package com.example.service

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.TimeUnit

class WifiWebSocketService(
    private val onStatusChanged: (Boolean, String) -> Unit,
    private val onMessageReceived: (String) -> Unit
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private var webSocket: WebSocket? = null
    private var okHttpClient: OkHttpClient? = null

    // For Phone-as-Hotspot direct TCP socket fallback server
    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private val clientSockets = mutableListOf<Socket>()

    // For Direct TCP Connection to ESP32 Wi-Fi AP (192.168.4.1:8080)
    private var tcpClientSocket: Socket? = null
    private var tcpClientJob: Job? = null
    private var isUserExplicitDisconnect = false

    val isConnected: Boolean
        get() = (tcpClientSocket != null && tcpClientSocket?.isClosed == false && tcpClientSocket?.isConnected == true) ||
                (webSocket != null) ||
                (serverSocket != null && clientSockets.isNotEmpty())

    val isConnecting: Boolean
        get() = (tcpClientJob?.isActive == true && !isConnected) ||
                (serverJob?.isActive == true && clientSockets.isEmpty())

    fun connectWebSocket(serverUrl: String) {
        isUserExplicitDisconnect = false
        disconnectInternal()
        onStatusChanged(false, "Bağlanıyor: $serverUrl...")

        // Eğer ws:// veya wss:// ile başlıyorsa WebSocket kullan
        if (serverUrl.startsWith("ws://") || serverUrl.startsWith("wss://")) {
            scope.launch {
                try {
                    val client = OkHttpClient.Builder()
                        .readTimeout(10, TimeUnit.SECONDS)
                        .connectTimeout(5, TimeUnit.SECONDS)
                        .build()
                    okHttpClient = client

                    val request = Request.Builder().url(serverUrl).build()
                    webSocket = client.newWebSocket(request, object : WebSocketListener() {
                        override fun onOpen(webSocket: WebSocket, response: Response) {
                            scope.launch(Dispatchers.Main) {
                                onStatusChanged(true, "Wi-Fi WS: $serverUrl")
                            }
                        }

                        override fun onMessage(webSocket: WebSocket, text: String) {
                            scope.launch(Dispatchers.Main) {
                                onMessageReceived(text.trim())
                            }
                        }

                        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                            scope.launch(Dispatchers.Main) {
                                onStatusChanged(false, "Kapanıyor: $reason")
                            }
                        }

                        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                            scope.launch(Dispatchers.Main) {
                                onStatusChanged(false, "Bağlantı hatası: ${t.localizedMessage ?: "Sunucuya ulaşılamadı"}")
                            }
                        }
                    })
                } catch (e: Exception) {
                    Log.e("WifiWebSocketService", "Error initiating WebSocket", e)
                    withContext(Dispatchers.Main) {
                        onStatusChanged(false, "Hata: ${e.message}")
                    }
                }
            }
        } else {
            // Standart ESP32 TCP Soket Bağlantısı (192.168.4.1:8080)
            val clean = serverUrl.removePrefix("tcp://").removePrefix("http://")
            val host: String
            val port: Int
            if (clean.contains(":")) {
                val parts = clean.split(":")
                host = parts[0]
                port = parts[1].substringBefore("/").toIntOrNull() ?: 8080
            } else {
                host = clean.substringBefore("/")
                port = 8080
            }

            tcpClientJob = scope.launch {
                var attempt = 0
                while (isActive && !isUserExplicitDisconnect) {
                    attempt++
                    var socket: Socket? = null
                    try {
                        val s = Socket()
                        s.tcpNoDelay = true
                        s.keepAlive = true
                        s.sendBufferSize = 65536
                        s.receiveBufferSize = 65536
                        s.connect(InetSocketAddress(host, port), 3000)
                        socket = s
                        tcpClientSocket = s
                        withContext(Dispatchers.Main) {
                            onStatusChanged(true, "$host:$port")
                        }
                        val reader = BufferedReader(InputStreamReader(s.getInputStream()))
                        while (isActive && !s.isClosed) {
                            val line = reader.readLine() ?: break
                            val trimmed = line.trim()
                            if (trimmed.isNotEmpty()) {
                                withContext(Dispatchers.Main) {
                                    onMessageReceived(trimmed)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.d("WifiWebSocketService", "Wi-Fi TCP denemesi #$attempt ($host:$port): ${e.message}")
                    } finally {
                        try { socket?.close() } catch (_: Exception) {}
                        try { tcpClientSocket?.close() } catch (_: Exception) {}
                        tcpClientSocket = null
                        withContext(Dispatchers.Main) {
                            onStatusChanged(false, "Wi-Fi Çevrimdışı")
                        }
                    }
                    kotlinx.coroutines.delay(2000L)
                }
            }
        }
    }

    // Starts a local TCP server on port 8080 when the phone operates as Hotspot
    fun startLocalHotspotServer(port: Int = 8080) {
        isUserExplicitDisconnect = false
        disconnectInternal()
        onStatusChanged(false, "Hotspot Dinleyici Başlatılıyor (Port $port)...")

        serverJob = scope.launch {
            try {
                val ss = ServerSocket(port)
                serverSocket = ss
                withContext(Dispatchers.Main) {
                    onStatusChanged(true, "Hotspot Sunucusu Aktif (Port $port)")
                }

                while (isActive && !ss.isClosed) {
                    val client = ss.accept()
                    synchronized(clientSockets) {
                        clientSockets.add(client)
                    }
                    launch {
                        handleClient(client)
                    }
                }
            } catch (e: Exception) {
                Log.w("WifiWebSocketService", "Local server stopped: ${e.message}")
                withContext(Dispatchers.Main) {
                    onStatusChanged(false, "Sunucu kapandı")
                }
            }
        }
    }

    private suspend fun handleClient(client: Socket) {
        try {
            val reader = BufferedReader(InputStreamReader(client.getInputStream()))
            while (scope.isActive && !client.isClosed) {
                val line = reader.readLine() ?: break
                val trimmed = line.trim()
                if (trimmed.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        onMessageReceived(trimmed)
                    }
                    // Eğer telsiz ses paketi ise sunucuya bağlı diğer telefonlara da aktar
                    if (trimmed.startsWith("TELSİZ:") || trimmed.startsWith("TELSIZ:") ||
                        trimmed.startsWith("TELSİZ_SON:") || trimmed.startsWith("TELSIZ_SON:")) {
                        scope.launch {
                            synchronized(clientSockets) {
                                clientSockets.filter { it != client && !it.isClosed }.forEach { otherSock ->
                                    try {
                                        val writer = PrintWriter(otherSock.getOutputStream(), true)
                                        writer.println(trimmed)
                                    } catch (_: Exception) {}
                                }
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {
        } finally {
            synchronized(clientSockets) {
                clientSockets.remove(client)
            }
            try { client.close() } catch (_: Exception) {}
        }
    }

    private val tcpWriteLock = Any()

    fun sendCommand(command: String) {
        val payload = if (command.endsWith("\n")) command else "$command\n"
        // 1. WebSocket send
        webSocket?.send(command)

        val bytes = payload.toByteArray(Charsets.UTF_8)

        // 2. Direct TCP Client send (ESP32 AP moduna bağlıyken)
        scope.launch {
            try {
                synchronized(tcpWriteLock) {
                    tcpClientSocket?.let { sock ->
                        if (!sock.isClosed && sock.isConnected) {
                            val os = sock.getOutputStream()
                            os.write(bytes)
                            os.flush()
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        // 3. Local hotspot TCP clients broadcast
        scope.launch {
            synchronized(clientSockets) {
                val iterator = clientSockets.iterator()
                while (iterator.hasNext()) {
                    val socket = iterator.next()
                    try {
                        if (!socket.isClosed && socket.isConnected) {
                            val os = socket.getOutputStream()
                            os.write(bytes)
                            os.flush()
                        }
                    } catch (e: Exception) {
                        iterator.remove()
                        try { socket.close() } catch (_: Exception) {}
                    }
                }
            }
        }
    }

    private fun disconnectInternal() {
        try {
            webSocket?.close(1000, "Disconnect")
            webSocket = null
        } catch (_: Exception) {}

        tcpClientJob?.cancel()
        tcpClientJob = null
        try {
            tcpClientSocket?.close()
            tcpClientSocket = null
        } catch (_: Exception) {}

        serverJob?.cancel()
        serverJob = null
        try {
            serverSocket?.close()
            serverSocket = null
        } catch (_: Exception) {}

        synchronized(clientSockets) {
            clientSockets.forEach { try { it.close() } catch (_: Exception) {} }
            clientSockets.clear()
        }
    }

    fun disconnect() {
        isUserExplicitDisconnect = true
        disconnectInternal()
        onStatusChanged(false, "Wi-Fi Bağlantısı Kesildi")
    }
}
