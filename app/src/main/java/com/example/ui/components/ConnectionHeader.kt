package com.example.ui.components

import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.ConnectionMode
import com.example.model.ConnectionStatus
import com.example.ui.theme.MetallicSilver
import com.example.ui.theme.NeonGreen

@Composable
fun ConnectionHeader(
    currentMode: ConnectionMode,
    status: ConnectionStatus,
    btConnected: Boolean = false,
    btTarget: String = "",
    wifiConnected: Boolean = false,
    wifiTarget: String = "",
    sensorCount: Int,
    maxSensors: Int = 10,
    onModeSelect: (ConnectionMode) -> Unit,
    onAddNewSensor: () -> Unit,
    pairedDevices: List<BluetoothDevice>,
    onSelectBluetoothDevice: (BluetoothDevice) -> Unit,
    onConnectWifi: (String) -> Unit,
    onStartWifiHotspotServer: (Int) -> Unit,
    onOpenLogs: () -> Unit,
    onOpenTrophyLog: () -> Unit,
    onOpenAudioSettings: () -> Unit = {},
    onOpenPermissions: () -> Unit = {},
    onToggleNightMode: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showBtDialog by remember { mutableStateOf(false) }
    var showWifiDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
    ) {
        // 1. ÜST SATIR: TFY Logo & Av Günlüğü, TFY SMART FISHING Başlığı ve ⚙️ Ayarlar Çarkı
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // SOL: TFY Logo & Tam Başlık
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // TFY Siber Logo & Av Günlüğü Butonu
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF0F141F))
                        .border(1.5.dp, NeonGreen.copy(alpha = 0.8f), RoundedCornerShape(10.dp))
                        .clickable { onOpenTrophyLog() }
                        .testTag("trophy_log_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_tfy_logo),
                        contentDescription = "TFY Logo & Av Günlüğü",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                val isAnyConnected = btConnected || wifiConnected || status is ConnectionStatus.Connected
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isAnyConnected) NeonGreen else Color(0xFF6B7280))
                        .shadow(if (isAnyConnected) 6.dp else 0.dp, CircleShape, spotColor = NeonGreen)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "TFY SMART FISHING",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.1.sp,
                        fontSize = 17.5.sp,
                        color = Color.White
                    ),
                    maxLines = 1
                )
            }

            // SAĞ: ⚙️ Ayarlar (Dişli) Butonu
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF141724))
                    .border(1.2.dp, NeonGreen.copy(alpha = 0.6f), CircleShape)
                .clickable { onOpenAudioSettings() }
                    .testTag("open_audio_settings_btn"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Ayarlar & Kontrol",
                    tint = NeonGreen,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // 2. ALT SATIR: Orantılı, Eşit Boy ve Uzunlukta 3'lü Kontrol Paneli (Bluetooth, Wi-Fi, Olta Ekle)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Bluetooth Butonu & Durumu
            Surface(
                color = if (btConnected) Color(0xFF0F241A) else Color(0xFF141722),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(
                    1.dp,
                    if (btConnected) NeonGreen.copy(alpha = 0.8f) else Color(0xFF262C3E)
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { showBtDialog = true }
                    .testTag("header_bt_badge")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Bluetooth,
                        contentDescription = null,
                        tint = if (btConnected) NeonGreen else Color(0xFF7E8799),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (btConnected) "BT: ${btTarget.ifBlank { "Bağlı" }}" else "Bluetooth",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (btConnected) NeonGreen else Color(0xFF8E95A5),
                            fontWeight = if (btConnected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 11.5.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // 2. Wi-Fi Butonu & Durumu
            Surface(
                color = if (wifiConnected) Color(0xFF0F241A) else Color(0xFF141722),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(
                    1.dp,
                    if (wifiConnected) NeonGreen.copy(alpha = 0.8f) else Color(0xFF262C3E)
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { showWifiDialog = true }
                    .testTag("header_wifi_badge")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Wifi,
                        contentDescription = null,
                        tint = if (wifiConnected) NeonGreen else Color(0xFF7E8799),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (wifiConnected) "Wi-Fi: ${wifiTarget.ifBlank { "Bağlı" }}" else "Wi-Fi",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (wifiConnected) NeonGreen else Color(0xFF8E95A5),
                            fontWeight = if (wifiConnected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 11.5.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // 3. Olta Ekle Butonu
            Surface(
                color = if (sensorCount < maxSensors) Color(0xFF10241A) else Color(0xFF1B1E28),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(
                    1.dp,
                    if (sensorCount < maxSensors) NeonGreen.copy(alpha = 0.8f) else Color(0xFF333A4D)
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(enabled = sensorCount < maxSensors) { onAddNewSensor() }
                    .testTag("add_sensor_header_button")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Olta Ekle",
                        tint = if (sensorCount < maxSensors) NeonGreen else Color(0xFF7E8799),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "OLTA EKLE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (sensorCount < maxSensors) NeonGreen else Color(0xFF7E8799),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp
                        ),
                        maxLines = 1
                    )
                }
            }
        }
    }

    // --- BLUETOOTH EŞLEŞMİŞ CİHAZ LİSTESİ DİYALOĞU ---
    if (showBtDialog) {
        AlertDialog(
            onDismissRequest = { showBtDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Bluetooth,
                        contentDescription = null,
                        tint = NeonGreen
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Bluetooth Cihazı",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Eşleşmiş SPP olta alarmı seçin:",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8)),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    if (pairedDevices.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Eşleşmiş Bluetooth cihazı bulunamadı.\nTelefon Ayarlarından olta alarmını eşleştirin.",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFCBD5E1))
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(pairedDevices) { device ->
                                @android.annotation.SuppressLint("MissingPermission")
                                val devName = device.name ?: "Bilinmeyen Cihaz"
                                val devAddr = device.address

                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            onSelectBluetoothDevice(device)
                                            showBtDialog = false
                                        },
                                    color = Color(0xFF141722),
                                    border = BorderStroke(1.dp, Color(0xFF262C3E)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Router,
                                            contentDescription = null,
                                            tint = NeonGreen,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = devName,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                            )
                                            Text(
                                                text = devAddr,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = Color(0xFF7E8799),
                                                    fontSize = 11.sp
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showBtDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(text = "Kapat", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showBtDialog = false
                        showWifiDialog = true
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonGreen),
                    border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.5f))
                ) {
                    Icon(imageVector = Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Wi-Fi Ayarları", fontSize = 12.sp)
                }
            },
            containerColor = Color(0xFF141620),
            shape = RoundedCornerShape(20.dp)
        )
    }

    // --- WI-FI HOTSPOT AYARLARI DİYALOĞU ---
    if (showWifiDialog) {
        var wsUrl by remember { mutableStateOf("192.168.4.1:8080") }

        AlertDialog(
            onDismissRequest = { showWifiDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Wifi,
                        contentDescription = null,
                        tint = NeonGreen
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Wi-Fi Bağlantı Modu",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "ESP32 Wi-Fi TCP veya WebSocket Adresi:",
                        style = MaterialTheme.typography.labelMedium.copy(color = MetallicSilver)
                    )
                    OutlinedTextField(
                        value = wsUrl,
                        onValueChange = { wsUrl = it },
                        singleLine = true,
                        placeholder = { Text("192.168.4.1:8080", color = Color(0xFF64748B)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = NeonGreen,
                            unfocusedBorderColor = Color(0xFF2C3244)
                        )
                    )

                    Button(
                        onClick = {
                            onConnectWifi(wsUrl)
                            showWifiDialog = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Bağlan (ESP32 AP: 192.168.4.1:8080)", color = Color.Black, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            onStartWifiHotspotServer(8080)
                            showWifiDialog = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFF2C3244))
                    ) {
                        Text("Telefon Hotspot Sunucusu Başlat (Port 8080)", color = MetallicSilver)
                    }
                }
            },
            confirmButton = {
                OutlinedButton(
                    onClick = { showWifiDialog = false },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Text("Kapat")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showWifiDialog = false
                        showBtDialog = true
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonGreen),
                    border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.5f))
                ) {
                    Icon(imageVector = Icons.Default.Bluetooth, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Bluetooth Cihazları", fontSize = 12.sp)
                }
            },
            containerColor = Color(0xFF141620),
            shape = RoundedCornerShape(20.dp)
        )
    }
}
