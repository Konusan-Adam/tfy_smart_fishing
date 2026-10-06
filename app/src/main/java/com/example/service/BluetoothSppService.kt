package com.example.service

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.util.UUID

class BluetoothSppService(
    private val context: Context,
    private val onStatusChanged: (Boolean, String) -> Unit,
    private val onMessageReceived: (String) -> Unit
) {
    companion object {
        // Standard SPP (Serial Port Profile) UUID for HC-05, HC-06, ESP32 SPP, Arduino
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        private const val TAG = "BluetoothSppService"
        private const val PREFS_NAME = "balik_zili_bt_prefs"
        private const val KEY_LAST_DEVICE_MAC = "last_connected_mac"
        private const val KEY_LAST_DEVICE_NAME = "last_connected_name"
        private const val KEY_AUTOCONNECT_ENABLED = "autoconnect_enabled"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    val isAutoConnectEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTOCONNECT_ENABLED, true)

    fun setAutoConnectEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTOCONNECT_ENABLED, enabled).apply()
        if (!enabled) {
            isUserExplicitDisconnect = true
            aggressiveReconnectJob?.cancel()
            aggressiveReconnectJob = null
            infiniteWatchdogJob?.cancel()
            infiniteWatchdogJob = null
            disconnectInternal()
            onStatusChanged(false, "Otomatik Arama Kapalı (Dinlenme Modu)")
            FishingWatchdogService.stop(context)
        } else {
            isUserExplicitDisconnect = false
            startInfiniteAutoReconnectWatchdog()
            triggerImmediateAutoConnect()
        }
    }

    private var socket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private var readJob: Job? = null
    private var aggressiveReconnectJob: Job? = null
    private var infiniteWatchdogJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private var targetDevice: BluetoothDevice? = null
    private var isUserExplicitDisconnect = false

    private val btStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            if (intent?.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                if (state == BluetoothAdapter.STATE_ON) {
                    Log.d(TAG, "Bluetooth açıldı, agresif otomatik bağlantı tetikleniyor!")
                    triggerImmediateAutoConnect()
                } else if (state == BluetoothAdapter.STATE_OFF) {
                    onStatusChanged(false, "Bluetooth Kapalı")
                }
            }
        }
    }

    init {
        try {
            context.registerReceiver(
                btStateReceiver,
                IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
            )
        } catch (e: Exception) {
            Log.w(TAG, "Register btStateReceiver failed: ${e.message}")
        }
        startInfiniteAutoReconnectWatchdog()
    }

    val isBluetoothSupported: Boolean
        get() = bluetoothAdapter != null

    val isBluetoothEnabled: Boolean
        get() = bluetoothAdapter?.isEnabled == true

    val isConnected: Boolean
        get() = socket?.isConnected == true

    val isConnecting: Boolean
        get() = aggressiveReconnectJob?.isActive == true && !isConnected

    @SuppressLint("MissingPermission")
    fun getPairedDevices(): List<BluetoothDevice> {
        return try {
            bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error reading paired devices", e)
            emptyList()
        }
    }

    @SuppressLint("MissingPermission")
    private fun findCandidateDevice(): BluetoothDevice? {
        val adapter = bluetoothAdapter ?: return null
        if (!adapter.isEnabled) return null

        // 1. targetDevice belirlenmişse
        targetDevice?.let { return it }

        // 2. Tercihlerdeki kayıtlı MAC
        val savedMac = prefs.getString(KEY_LAST_DEVICE_MAC, null)
        if (savedMac != null) {
            try {
                val dev = adapter.getRemoteDevice(savedMac)
                if (dev != null) return dev
            } catch (_: Exception) {}
        }

        // 3. Eşleşmiş cihazlar listesinde filtreleme
        try {
            val paired = adapter.bondedDevices?.toList() ?: emptyList()
            val target = paired.firstOrNull {
                val n = (it.name ?: "").uppercase()
                n.contains("BALIK") || n.contains("ALARM") || n.contains("C3") || 
                n.contains("MERKEZ") || n.contains("ESP32") || n.contains("PAGER") || 
                n.contains("TFY") || n.contains("BT") || n.contains("HC-05") || n.contains("HC-06")
            } ?: paired.firstOrNull()
            return target
        } catch (_: Exception) {
            return null
        }
    }

    fun startInfiniteAutoReconnectWatchdog() {
        if (!isAutoConnectEnabled) return
        infiniteWatchdogJob?.cancel()
        infiniteWatchdogJob = scope.launch {
            while (isActive) {
                try {
                    if (isAutoConnectEnabled && !isConnected && !isUserExplicitDisconnect && isBluetoothEnabled) {
                        val candidate = findCandidateDevice()
                        if (candidate != null && (aggressiveReconnectJob == null || aggressiveReconnectJob?.isActive != true)) {
                            Log.d(TAG, "Daimi Gözlemci: Cihaz aranıyor -> ${candidate.name} (${candidate.address})")
                            startAggressiveReconnectLoop(candidate)
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Watchdog error: ${e.message}")
                }
                delay(2500L)
            }
        }
    }

    fun triggerImmediateAutoConnect() {
        isUserExplicitDisconnect = false
        autoConnectLastSavedDevice()
    }

    @SuppressLint("MissingPermission")
    fun autoConnectLastSavedDevice() {
        isUserExplicitDisconnect = false
        val candidate = findCandidateDevice()
        if (candidate != null) {
            connectToDevice(candidate)
        } else {
            startInfiniteAutoReconnectWatchdog()
        }
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(device: BluetoothDevice) {
        isUserExplicitDisconnect = false
        targetDevice = device

        // Tercihlere kaydet
        prefs.edit()
            .putString(KEY_LAST_DEVICE_MAC, device.address)
            .putString(KEY_LAST_DEVICE_NAME, device.name ?: "ESP32 Alıcı")
            .apply()

        startAggressiveReconnectLoop(device)
    }

    @SuppressLint("MissingPermission")
    private fun startAggressiveReconnectLoop(device: BluetoothDevice) {
        aggressiveReconnectJob?.cancel()
        aggressiveReconnectJob = scope.launch {
            disconnectInternal()

            var attemptCount = 0
            while (isActive && !isUserExplicitDisconnect) {
                if (bluetoothAdapter?.isEnabled != true) {
                    withContext(Dispatchers.Main) {
                        onStatusChanged(false, "Bluetooth Kapalı")
                    }
                    delay(2000L)
                    continue
                }

                attemptCount++
                val devName = device.name ?: device.address

                withContext(Dispatchers.Main) {
                    onStatusChanged(false, "Bağlanıyor: $devName (#$attemptCount)...")
                }

                try {
                    try {
                        bluetoothAdapter?.cancelDiscovery()
                    } catch (_: Exception) {}

                    var btSocket: BluetoothSocket? = null

                    // 1. Insecure RFCOMM (Şifresiz SPP - Hızlı deneme, max 3sn)
                    try {
                        val s = device.createInsecureRfcommSocketToServiceRecord(SPP_UUID)
                        try {
                            kotlinx.coroutines.withTimeout(3000L) {
                                s.connect()
                            }
                            btSocket = s
                        } catch (e1: Exception) {
                            try { s.close() } catch (_: Exception) {}
                            throw e1
                        }
                    } catch (e1: Exception) {
                        try {
                            // 2. Port 1 Reflection (Hızlı deneme, max 3sn)
                            val m = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                            val s = m.invoke(device, 1) as? BluetoothSocket
                            if (s != null) {
                                try {
                                    kotlinx.coroutines.withTimeout(3000L) {
                                        s.connect()
                                    }
                                    btSocket = s
                                } catch (e2: Exception) {
                                    try { s.close() } catch (_: Exception) {}
                                    throw e2
                                }
                            }
                        } catch (e2: Exception) {
                            // 3. Standart Güvenli RFCOMM (max 3sn)
                            try {
                                val s = device.createRfcommSocketToServiceRecord(SPP_UUID)
                                try {
                                    kotlinx.coroutines.withTimeout(3000L) {
                                        s.connect()
                                    }
                                    btSocket = s
                                } catch (e3: Exception) {
                                    try { s.close() } catch (_: Exception) {}
                                }
                            } catch (_: Exception) {}
                        }
                    }

                    val connectedSock = btSocket
                    if (connectedSock != null && connectedSock.isConnected) {
                        socket = connectedSock
                        outputStream = connectedSock.outputStream

                        withContext(Dispatchers.Main) {
                            onStatusChanged(true, devName)
                        }

                        // Foreground Servisini Başlat
                        FishingWatchdogService.start(context)

                        // Okuma akışını başlat
                        startReading(connectedSock, device)
                        break
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Bağlantı denemesi #$attemptCount başarısız ($devName): ${e.message}")
                    disconnectInternal()
                }

                delay(1500L)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startReading(btSocket: BluetoothSocket, device: BluetoothDevice) {
        readJob = scope.launch {
            try {
                val reader = BufferedReader(InputStreamReader(btSocket.inputStream))
                while (isActive && btSocket.isConnected) {
                    val line = reader.readLine() ?: break
                    val trimmed = line.trim()
                    if (trimmed.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            onMessageReceived(trimmed)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Bluetooth akışı kesildi: ${e.message}")
            } finally {
                withContext(Dispatchers.Main) {
                    onStatusChanged(false, "Bağlantı koptu. Agresif yeniden aranıyor...")
                }
                disconnectInternal()

                if (!isUserExplicitDisconnect) {
                    startAggressiveReconnectLoop(device)
                }
            }
        }
    }

    private val writeLock = Any()

    fun sendCommand(command: String) {
        scope.launch {
            try {
                val payload = if (command.endsWith("\n")) command else "$command\n"
                val bytes = payload.toByteArray(Charsets.UTF_8)
                synchronized(writeLock) {
                    outputStream?.write(bytes)
                    outputStream?.flush()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send command: $command", e)
            }
        }
    }

    private fun disconnectInternal() {
        readJob?.cancel()
        readJob = null
        try {
            outputStream?.close()
        } catch (_: Exception) {}
        try {
            socket?.close()
        } catch (_: Exception) {}
        socket = null
        outputStream = null
    }

    fun disconnect() {
        isUserExplicitDisconnect = true
        targetDevice = null
        aggressiveReconnectJob?.cancel()
        aggressiveReconnectJob = null
        infiniteWatchdogJob?.cancel()
        infiniteWatchdogJob = null
        disconnectInternal()
        onStatusChanged(false, "Bağlantı kesildi")
        FishingWatchdogService.stop(context)
    }

    fun release() {
        try {
            context.unregisterReceiver(btStateReceiver)
        } catch (_: Exception) {}
        disconnect()
    }
}
