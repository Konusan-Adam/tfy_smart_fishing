package com.example.ui.components

import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.model.ConnectionMode
import com.example.model.FishingRod
import com.example.model.SensorThresholdConfig
import com.example.model.SoundStyle
import com.example.ui.theme.NeonGreen

@Composable
fun AudioSettingsDialog(
    currentUserName: String,
    currentSoundStyle: SoundStyle,
    currentAppVolume: Float = 1.0f,
    onVolumeChangeLive: (Float) -> Unit = {},
    isMasterSystemActive: Boolean = true,
    onToggleMasterSystemPower: (Boolean) -> Unit = {},
    currentMode: ConnectionMode = ConnectionMode.BLUETOOTH,
    onModeSelect: (ConnectionMode) -> Unit = {},
    pairedDevices: List<BluetoothDevice> = emptyList(),
    onSelectBluetoothDevice: (BluetoothDevice) -> Unit = {},
    onConnectWifi: (String) -> Unit = {},
    onStartWifiHotspotServer: (Int) -> Unit = {},
    rods: List<FishingRod> = emptyList(),
    onToggleRodPower: (rodId: Int, isArmed: Boolean) -> Unit = { _, _ -> },
    onUpdateThresholds: (rodId: Int, config: SensorThresholdConfig) -> Unit = { _, _ -> },
    onDismiss: () -> Unit,
    onSave: (userName: String, soundStyle: SoundStyle, appVolume: Float) -> Unit,
    onTerminateFishing: () -> Unit = {},
    onOpenLogs: () -> Unit = {},
    onOpenPermissions: () -> Unit = {},
    onToggleNightMode: () -> Unit = {},
    onTestAlarmSound: () -> Unit = {},
    onTestAlarmSoundWithStyle: (SoundStyle) -> Unit = {},
    onTestDropBackSound: () -> Unit = {},
    onTestTheftSound: () -> Unit = {},
    onTestReelDragSound: () -> Unit = {},
    customNormalSoundUri: String? = null,
    customNormalSoundName: String? = null,
    customDropBackSoundUri: String? = null,
    customDropBackSoundName: String? = null,
    customTheftSoundUri: String? = null,
    customTheftSoundName: String? = null,
    customDragSoundUri: String? = null,
    customDragSoundName: String? = null,
    onSetCustomSound: (scenarioKey: String, uri: String?, displayName: String?) -> Unit = { _, _, _ -> },
    currentAppVersion: String = "1.0.0",
    onCheckForUpdates: () -> Unit = {}
) {
    var nameText by remember { mutableStateOf(currentUserName) }
    var selectedStyle by remember { mutableStateOf(currentSoundStyle) }
    var appVolumeState by remember { mutableFloatStateOf(currentAppVolume) }
    var showDeviceList by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var activeScenarioKeyForPicker by remember { mutableStateOf<String?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        val key = activeScenarioKeyForPicker
        if (uri != null && key != null) {
            var name = "Özel MP3"
            try {
                context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx != -1) {
                            val displayName = cursor.getString(idx)
                            if (!displayName.isNullOrBlank()) name = displayName
                        }
                    }
                }
            } catch (_: Exception) {}
            onSetCustomSound(key, uri.toString(), name)
        }
    }

    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val key = activeScenarioKeyForPicker
        if (result.resultCode == Activity.RESULT_OK && key != null) {
            val pickedUri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }
            if (pickedUri != null) {
                var name = "Sistem Zil Sesi"
                try {
                    val ringtone = RingtoneManager.getRingtone(context, pickedUri)
                    val title = ringtone?.getTitle(context)
                    if (!title.isNullOrBlank()) name = title
                } catch (_: Exception) {}
                onSetCustomSound(key, pickedUri.toString(), name)
            }
        }
    }
    var isSavedToSensor by remember { mutableStateOf(false) }
    var isSyncing by remember { mutableStateOf(false) }

    // Seçili Olta Eşik Ayarları State
    var targetRodId by remember { mutableIntStateOf(rods.firstOrNull()?.id ?: 1) }
    val activeRod = rods.find { it.id == targetRodId } ?: rods.firstOrNull()
    val initialConfig = activeRod?.thresholdConfig ?: SensorThresholdConfig.DEFAULT

    var strikeAngleDeg by remember(targetRodId, activeRod?.thresholdConfig) {
        mutableFloatStateOf(initialConfig.strikeAngleDeg)
    }
    var shockMg by remember(targetRodId, activeRod?.thresholdConfig) {
        mutableFloatStateOf(initialConfig.shockAccelThresholdMg)
    }
    var dropBackDeg by remember(targetRodId, activeRod?.thresholdConfig) {
        mutableFloatStateOf(initialConfig.dropBackAngleDeg)
    }
    var theftDeg by remember(targetRodId, activeRod?.thresholdConfig) {
        mutableFloatStateOf(initialConfig.theftAngleDeg)
    }
    var sampleMs by remember(targetRodId, activeRod?.thresholdConfig) {
        mutableIntStateOf(initialConfig.sampleIntervalMs)
    }
    var pingSec by remember(targetRodId, activeRod?.thresholdConfig) {
        mutableIntStateOf(initialConfig.pingIntervalSec)
    }

    val hasUnsavedChanges = (initialConfig.shockAccelThresholdMg != shockMg) ||
        (initialConfig.dropBackAngleDeg != dropBackDeg) ||
        (initialConfig.theftAngleDeg != theftDeg) ||
        (initialConfig.strikeAngleDeg != strikeAngleDeg) ||
        (initialConfig.sampleIntervalMs != sampleMs) ||
        (initialConfig.pingIntervalSec != pingSec)

    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = NeonGreen,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Ayarlar & Kontrol Paneli",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 🔥 0. GENEL SİSTEM ŞALTERİ (AÇIK / KAPALI - TÜM SİSTEMİ, BAĞLANTILARI VE OLTALARI TEK TUŞLA YÖNETİR)
                Surface(
                    color = if (isMasterSystemActive) Color(0xFF0D2818) else Color(0xFF281216),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.5.dp, if (isMasterSystemActive) NeonGreen else Color(0xFFFF3B30)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(if (isMasterSystemActive) NeonGreen.copy(alpha = 0.2f) else Color(0xFFFF3B30).copy(alpha = 0.2f))
                                    .border(1.dp, if (isMasterSystemActive) NeonGreen else Color(0xFFFF3B30), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PowerSettingsNew,
                                    contentDescription = null,
                                    tint = if (isMasterSystemActive) NeonGreen else Color(0xFFFF3B30),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = if (isMasterSystemActive) "SİSTEM: AÇIK (AV MODU)" else "SİSTEM: KAPALI (DİNLENME)",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Black,
                                        color = if (isMasterSystemActive) NeonGreen else Color(0xFFFF453A),
                                        fontSize = 14.5.sp,
                                        letterSpacing = 0.5.sp
                                    )
                                )
                                Text(
                                    text = if (isMasterSystemActive)
                                        "⚡ Bluetooth arama, Wi-Fi ve oltalar aktif."
                                    else
                                        "💤 Tüm aramalar ve bağlantılar durduruldu, Bluetooth serbest.",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color(0xFFCBD5E1),
                                        fontSize = 11.5.sp
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Switch(
                            checked = isMasterSystemActive,
                            onCheckedChange = { onToggleMasterSystemPower(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = NeonGreen,
                                checkedTrackColor = Color(0xFF004D25),
                                uncheckedThumbColor = Color(0xFFFF453A),
                                uncheckedTrackColor = Color(0xFF4A151C)
                            ),
                            modifier = Modifier.testTag("master_system_power_switch")
                        )
                    }
                }

                // 1. BAĞLANTI TÜRÜ VE CİHAZ YÖNETİMİ
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "BAĞLANTI TÜRÜ (ÇADIR MERKEZİ):",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF0F121C),
                        border = BorderStroke(1.dp, Color(0xFF222838)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val isBt = currentMode == ConnectionMode.BLUETOOTH
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isBt) Color(0xFF1B2E24) else Color.Transparent,
                                border = BorderStroke(
                                    1.dp,
                                    if (isBt) NeonGreen.copy(alpha = 0.5f) else Color.Transparent
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onModeSelect(ConnectionMode.BLUETOOTH) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Bluetooth,
                                        contentDescription = null,
                                        tint = if (isBt) NeonGreen else Color(0xFF7E8799),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Bluetooth (Kablosuz)",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (isBt) Color.White else Color(0xFF94A3B8),
                                            fontWeight = if (isBt) FontWeight.Bold else FontWeight.Normal
                                        )
                                    )
                                }
                            }

                            val isWifi = currentMode == ConnectionMode.WIFI_HOTSPOT
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isWifi) Color(0xFF1B2E24) else Color.Transparent,
                                border = BorderStroke(
                                    1.dp,
                                    if (isWifi) NeonGreen.copy(alpha = 0.5f) else Color.Transparent
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onModeSelect(ConnectionMode.WIFI_HOTSPOT) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Wifi,
                                        contentDescription = null,
                                        tint = if (isWifi) NeonGreen else Color(0xFF7E8799),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Çadır Wi-Fi Ağı",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (isWifi) Color.White else Color(0xFF94A3B8),
                                            fontWeight = if (isWifi) FontWeight.Bold else FontWeight.Normal
                                        )
                                    )
                                }
                            }
                        }
                    }

                    if (currentMode == ConnectionMode.BLUETOOTH) {
                        OutlinedButton(
                            onClick = { showDeviceList = !showDeviceList },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFF2A344A)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (showDeviceList) "▲ Eşleşmiş Cihaz Listesini Kapat" else "▼ Eşleşmiş Bluetooth Cihazı Seç",
                                color = Color(0xFFCBD5E1),
                                fontSize = 11.sp
                            )
                        }

                        if (showDeviceList) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF0D1017),
                                border = BorderStroke(1.dp, Color(0xFF252D3F)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(6.dp)) {
                                    if (pairedDevices.isEmpty()) {
                                        Text(
                                            text = "Eşleşmiş Bluetooth cihazı bulunamadı. Telefon ayarlarından ESP32'yi eşleştirin.",
                                            style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF94A3B8)),
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    } else {
                                        pairedDevices.forEach { device ->
                                            @Suppress("MissingPermission")
                                            val name = device.name ?: device.address
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        onSelectBluetoothDevice(device)
                                                        showDeviceList = false
                                                    }
                                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text(
                                                        text = name,
                                                        color = Color.White,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        text = device.address,
                                                        color = Color(0xFF64748B),
                                                        fontSize = 10.sp
                                                    )
                                                }
                                                Text(
                                                    text = "Bağlan",
                                                    color = NeonGreen,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else if (currentMode == ConnectionMode.WIFI_HOTSPOT) {
                        var wsUrlInput by remember { mutableStateOf("192.168.4.1:8080") }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF0F1420),
                            border = BorderStroke(1.dp, Color(0xFF223048)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Wifi,
                                        contentDescription = null,
                                        tint = NeonGreen,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Wi-Fi & Hotspot Bağlantı Paneli",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = NeonGreen,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.5.sp
                                        )
                                    )
                                }

                                OutlinedTextField(
                                    value = wsUrlInput,
                                    onValueChange = { wsUrlInput = it },
                                    singleLine = true,
                                    label = { Text("ESP32 TCP IP:Port", color = Color(0xFF94A3B8), fontSize = 11.sp) },
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
                                        onConnectWifi(wsUrlInput)
                                        onDismiss()
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Wifi,
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "ESP32 AP'ye Bağlan (192.168.4.1:8080)",
                                        color = Color.Black,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.5.sp
                                    )
                                }

                                OutlinedButton(
                                    onClick = {
                                        onStartWifiHotspotServer(8080)
                                        onDismiss()
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.6f))
                                ) {
                                    Text(
                                        text = "📱 Telefon Hotspot Sunucusu Başlat (Port 8080)",
                                        color = Color.White,
                                        fontSize = 11.5.sp
                                    )
                                }

                                Surface(
                                    color = Color(0xFF131826),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "💡 Bağlantı Yöntemleri:\n1. Kolay Yol (Tavsiye): Telefonunuzdan 'T_F_Y_SMART_FISHING' ağına (Şifre: 1234567890) bağlanıp 'ESP32 AP'ye Bağlan' butonuna basın. (İnternet kotası harcamaz)\n2. Hotspot Yolu: Telefonun Hotspot'unu açıp 'Hotspot Sunucusu Başlat' butonuna basın.",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Color(0xFF94A3B8),
                                            fontSize = 10.sp,
                                            lineHeight = 14.sp
                                        ),
                                        modifier = Modifier.padding(7.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 1. SENSÖR GÜÇ VE NÖBET KONTROLÜ (AÇ / KAPAT TOGGLE SWITCH)
                // YALNIZCA TANIMLI OLAN OLTALAR GÖSTERİLİR
                if (rods.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.PowerSettingsNew,
                                    contentDescription = null,
                                    tint = NeonGreen,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "SENSÖR GÜÇ KONTROLÜ (AÇ / KAPAT):",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color(0xFF94A3B8),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                )
                            }
                            Text(
                                text = "${rods.count { it.isArmed }}/${rods.size} Aktif",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = NeonGreen,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black
                                )
                            )
                        }

                        rods.forEach { rod ->
                            Surface(
                                color = if (rod.isArmed) Color(0xFF131924) else Color(0xFF0F1118),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (rod.isArmed) NeonGreen.copy(alpha = 0.4f) else Color(0xFF222838)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clip(CircleShape)
                                                .background(if (rod.isArmed) rod.ledColor.color else Color(0xFF475569))
                                                .shadow(if (rod.isArmed) 6.dp else 0.dp, CircleShape, spotColor = rod.ledColor.color)
                                        )
                                        Column {
                                            Text(
                                                text = rod.name,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    color = if (rod.isArmed) Color.White else Color(0xFF64748B),
                                                    fontWeight = FontWeight.Bold
                                                )
                                            )
                                            Text(
                                                text = if (rod.isArmed) "● Nöbette / Alarm Aktif" else "○ Kapalı / Kış Uykusunda",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = if (rod.isArmed) NeonGreen else Color(0xFF64748B),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            )
                                        }
                                    }

                                    // Sağa / sola çekilen Toggle Switch
                                    Switch(
                                        checked = rod.isArmed,
                                        onCheckedChange = { isChecked ->
                                            onToggleRodPower(rod.id, isChecked)
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.Black,
                                            checkedTrackColor = NeonGreen,
                                            checkedBorderColor = NeonGreen,
                                            uncheckedThumbColor = Color(0xFF64748B),
                                            uncheckedTrackColor = Color(0xFF1E2433),
                                            uncheckedBorderColor = Color(0xFF334155)
                                        ),
                                        modifier = Modifier.testTag("rod_power_toggle_${rod.id}")
                                    )
                                }
                            }
                        }
                    }
                }

                // Balıkçı İsmi (Sesli Asistan hitabı)
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    label = { Text("Balıkçı İsminiz") },
                    placeholder = { Text("Örn: Fikret, Reis") },
                    singleLine = true,
                    supportingText = {
                        Text(
                            text = "Sesli asistan vuruş anında size bu isimle seslenir.",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("user_name_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonGreen,
                        unfocusedBorderColor = Color(0xFF283042),
                        focusedLabelColor = NeonGreen,
                        unfocusedLabelColor = Color(0xFF94A3B8),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                // 🔊 UYGULAMA MÜSTAKİL SES SEVİYESİ (MASTER VOLUME CONTROL)
                Surface(
                    color = Color(0xFF0F141F),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFF223048)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = null,
                                    tint = NeonGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "🔊 UYGULAMA SES SEVİYESİ",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = NeonGreen,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                )
                            }

                            val volPercent = (appVolumeState * 100).toInt()
                            val volBadgeText = when {
                                volPercent == 0 -> "SESSİZ (%0)"
                                volPercent < 40 -> "GECE DÜŞÜK (%$volPercent)"
                                volPercent < 80 -> "DENGELİ (%$volPercent)"
                                else -> "MAKSİMUM GÜÇ (%$volPercent)"
                            }
                            Surface(
                                color = if (volPercent == 0) Color(0xFF331B1B) else Color(0xFF162529),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, if (volPercent == 0) Color(0xFFEF4444) else NeonGreen.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = volBadgeText,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (volPercent == 0) Color(0xFFFCA5A5) else NeonGreen,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        // Volume Slider
                        Slider(
                            value = appVolumeState,
                            onValueChange = { newVol ->
                                appVolumeState = newVol
                                onVolumeChangeLive(newVol)
                            },
                            valueRange = 0f..1f,
                            steps = 20,
                            colors = SliderDefaults.colors(
                                thumbColor = NeonGreen,
                                activeTrackColor = NeonGreen,
                                inactiveTrackColor = Color(0xFF1E293B)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("app_volume_slider")
                        )

                        // Quick Presets Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val presets = listOf(
                                "🌙 Sessiz" to 0.0f,
                                "☕ Gece (%30)" to 0.3f,
                                "🎣 Standart (%60)" to 0.6f,
                                "📢 Max (%100)" to 1.0f
                            )
                            presets.forEach { (label, targetVol) ->
                                val isSelected = Math.abs(appVolumeState - targetVol) < 0.05f
                                Surface(
                                    color = if (isSelected) NeonGreen.copy(alpha = 0.2f) else Color(0xFF161B26),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, if (isSelected) NeonGreen else Color(0xFF2D3748)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            appVolumeState = targetVol
                                            onVolumeChangeLive(targetVol)
                                        }
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (isSelected) Color.White else Color(0xFF94A3B8),
                                            fontSize = 9.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        modifier = Modifier.padding(vertical = 6.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }

                // 🎵 SENARYO BAZLI ÖZEL MP3 VE ZİL SESİ ATAMA PANELDEN KULLANICI SEÇİMLERİ
                Surface(
                    color = Color(0xFF0F141F),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = null,
                                tint = NeonGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "📁 SENARYO BAZLI ÖZEL MP3 / ZİL SESİ SEÇİMİ",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = NeonGreen,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            )
                        }

                        Text(
                            text = "Telefonunuzdaki istediğiniz MP3, müzik veya zil sesini her bir uyarı senaryosu için ayrı ayrı atayabilirsiniz.",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFFCBD5E1),
                                fontSize = 10.5.sp
                            )
                        )

                        val scenarioItems = listOf(
                            Triple("normal", "1. Normal Vuruş (Sazan Deparı / Run)", Pair(customNormalSoundUri, customNormalSoundName)),
                            Triple("dropback", "2. Boşa Düşme (Misina Gevşemesi)", Pair(customDropBackSoundUri, customDropBackSoundName)),
                            Triple("theft", "3. Hırsızlık İkazı (Sehpadan Dikilme)", Pair(customTheftSoundUri, customTheftSoundName)),
                            Triple("drag", "4. Makara Kalama / Cırcır Sesi", Pair(customDragSoundUri, customDragSoundName))
                        )

                        scenarioItems.forEach { (key, title, soundPair) ->
                            val uri = soundPair.first
                            val name = soundPair.second
                            val hasCustom = !uri.isNullOrBlank()

                            Surface(
                                color = if (hasCustom) Color(0xFF14221A) else Color(0xFF111520),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, if (hasCustom) NeonGreen else Color(0xFF232B3E)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = title,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.5.sp
                                            )
                                        )

                                        if (hasCustom) {
                                            Surface(
                                                color = NeonGreen.copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(4.dp),
                                                border = BorderStroke(0.8.dp, NeonGreen)
                                            ) {
                                                Text(
                                                    text = "Özel Ses Aktif",
                                                    color = NeonGreen,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                )
                                            }
                                        } else {
                                            Text(
                                                text = "Varsayılan Sentez",
                                                color = Color(0xFF64748B),
                                                fontSize = 9.5.sp
                                            )
                                        }
                                    }

                                    // Seçili Ses Etiketi
                                    Text(
                                        text = if (hasCustom) "🎵 ${name ?: "Özel Ses Dosyası"}" else "🔊 Varsayılan (Türkçe Asistan / Siren / Kalama)",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (hasCustom) NeonGreen else Color(0xFF94A3B8),
                                            fontSize = 10.5.sp,
                                            fontWeight = if (hasCustom) FontWeight.Bold else FontWeight.Normal
                                        )
                                    )

                                    // İşlem Butonları
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        // 📁 MP3 Dosyası Seç
                                        Surface(
                                            color = Color(0xFF1E293B),
                                            shape = RoundedCornerShape(6.dp),
                                            border = BorderStroke(1.dp, Color(0xFF334155)),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable {
                                                    activeScenarioKeyForPicker = key
                                                    filePickerLauncher.launch("audio/*")
                                                }
                                        ) {
                                            Text(
                                                text = "📁 MP3 Seç",
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                                modifier = Modifier.padding(vertical = 6.dp)
                                            )
                                        }

                                        // 🔔 Zil Sesi Seç
                                        Surface(
                                            color = Color(0xFF1E293B),
                                            shape = RoundedCornerShape(6.dp),
                                            border = BorderStroke(1.dp, Color(0xFF334155)),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable {
                                                    activeScenarioKeyForPicker = key
                                                    val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                                        putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALL)
                                                        putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "$title İçin Zil Sesi Seç")
                                                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                                                    }
                                                    ringtonePickerLauncher.launch(intent)
                                                }
                                        ) {
                                            Text(
                                                text = "🔔 Zil Sesi Seç",
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                                modifier = Modifier.padding(vertical = 6.dp)
                                            )
                                        }

                                        // 🔊 Test Et
                                        Surface(
                                            color = Color(0xFF0F2D1E),
                                            shape = RoundedCornerShape(6.dp),
                                            border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.6f)),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable {
                                                    when (key) {
                                                        "normal" -> onTestAlarmSound()
                                                        "dropback" -> onTestDropBackSound()
                                                        "theft" -> onTestTheftSound()
                                                        "drag" -> onTestReelDragSound()
                                                    }
                                                }
                                        ) {
                                            Text(
                                                text = "🔊 Test Et",
                                                color = NeonGreen,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                                modifier = Modifier.padding(vertical = 6.dp)
                                            )
                                        }

                                        // ❌ Sıfırla (Sadece özel ses varsa gösterilir)
                                        if (hasCustom) {
                                            Surface(
                                                color = Color(0xFF3B1818),
                                                shape = RoundedCornerShape(6.dp),
                                                border = BorderStroke(1.dp, Color(0xFFEF4444)),
                                                modifier = Modifier
                                                    .clickable { onSetCustomSound(key, null, null) }
                                            ) {
                                                Text(
                                                    text = "❌",
                                                    color = Color(0xFFFCA5A5),
                                                    fontSize = 10.sp,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 🎼 Alarm Tonu ve Tarzı Seçimi (Tüm Bildirim Sesleri)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "🎼 TÜM BİLDİRİM & ALARM SES TEMASI:",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )

                    SoundStyle.values().forEach { style ->
                        val isSelected = selectedStyle == style
                        Surface(
                            color = if (isSelected) Color(0xFF1B2333) else Color(0xFF0F121A),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, if (isSelected) NeonGreen else Color(0xFF222838)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedStyle = style }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { selectedStyle = style },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = NeonGreen,
                                            unselectedColor = Color(0xFF55607A)
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = style.title,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = if (isSelected) Color.White else Color(0xFFCBD5E1),
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 12.5.sp
                                            )
                                        )
                                        Text(
                                            text = style.desc,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = Color(0xFF7E8B9B),
                                                fontSize = 10.sp
                                            )
                                        )
                                    }
                                }

                                Surface(
                                    color = Color(0xFF10281F),
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.5f)),
                                    modifier = Modifier.clickable {
                                        selectedStyle = style
                                        onTestAlarmSoundWithStyle(style)
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = NeonGreen,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(
                                            text = "Dinle",
                                            color = NeonGreen,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 3.5. ⚡ DONANIM SENSÖR EŞİK & FİLTRE AYARLARI
                Surface(
                    color = Color(0xFF0F141F),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFF223048)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "⚡ SENSÖR EŞİK & FİLTRE AYARLARI",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = NeonGreen,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                )
                                Text(
                                    text = "Olta ucu sensörünün vuruş, açı ve örnekleme filtreleri",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color(0xFF94A3B8),
                                        fontSize = 10.sp
                                    )
                                )
                            }

                            val calculatedSens = SensorThresholdConfig.toSensitivity(shockMg)
                            Surface(
                                color = Color(0xFF162529),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = "Kart Hassasiyeti: $calculatedSens / 10",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = NeonGreen,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }

                        // Olta Seçimi (Chips)
                        if (rods.size > 1) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                rods.forEach { rod ->
                                    val isSelected = targetRodId == rod.id
                                    Surface(
                                        color = if (isSelected) Color(0xFF1E2B3E) else Color(0xFF131824),
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(1.dp, if (isSelected) NeonGreen else Color(0xFF28344A)),
                                        modifier = Modifier.clickable { targetRodId = rod.id }
                                    ) {
                                        Text(
                                            text = rod.name,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = if (isSelected) Color.White else Color(0xFF94A3B8),
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 10.sp
                                            ),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // 0. VURUŞ / ÖNE BÜKÜLME AÇISI (Derece)
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "Vuruş / Öne Bükülme Açısı",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    )
                                    Text(
                                        text = "Kamış suya eğilme eşiği (Yüksek açı küçük balığı eler)",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color(0xFF7E8B9B),
                                            fontSize = 9.5.sp
                                        )
                                    )
                                }
                                Text(
                                    text = String.format(java.util.Locale.US, "%.1f°", strikeAngleDeg),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFF00E5FF),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                            Slider(
                                value = strikeAngleDeg,
                                onValueChange = { strikeAngleDeg = it },
                                valueRange = 1.0f..15.0f,
                                steps = 27,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF00E5FF),
                                    activeTrackColor = Color(0xFF00E5FF),
                                    inactiveTrackColor = Color(0xFF253046)
                                )
                            )

                            // 🎯 Hızlı Vuruş Açısı Presetleri (1.5° / 2.5° / 4.0°)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                listOf(1.5f to "1.5° (Hassas)", 2.5f to "2.5° (Standart)", 4.0f to "4.0° (Sazan / İri)").forEach { (presetDeg, label) ->
                                    val isMatch = kotlin.math.abs(strikeAngleDeg - presetDeg) < 0.2f
                                    Surface(
                                        color = if (isMatch) Color(0xFF00E5FF).copy(alpha = 0.25f) else Color(0xFF141926),
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(1.dp, if (isMatch) Color(0xFF00E5FF) else Color(0xFF28344A)),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { strikeAngleDeg = presetDeg }
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = if (isMatch) Color(0xFF00E5FF) else Color(0xFF94A3B8),
                                                fontWeight = if (isMatch) FontWeight.Black else FontWeight.Bold,
                                                fontSize = 9.5.sp
                                            ),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                            modifier = Modifier.padding(vertical = 4.dp, horizontal = 2.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // 1. ŞOK VURUŞ EŞİĞİ (mG)
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "Şok Vuruş / Asılma Eşiği",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    )
                                    Text(
                                        text = "Balık vurduğunda algılanacak ivme eşiği",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color(0xFF7E8B9B),
                                            fontSize = 9.5.sp
                                        )
                                    )
                                }
                                Text(
                                    text = "${shockMg.toInt()} mG",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        color = NeonGreen,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                            Slider(
                                value = shockMg,
                                onValueChange = { shockMg = it },
                                valueRange = 100f..1000f,
                                steps = 35,
                                colors = SliderDefaults.colors(
                                    thumbColor = NeonGreen,
                                    activeTrackColor = NeonGreen,
                                    inactiveTrackColor = Color(0xFF253046)
                                )
                            )

                            // 🎯 Hızlı Şok Eşiği Presetleri (750 mG / 500 mG / 250 mG)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                listOf(750f to "750 mG (Sert/Dalgalı)", 500f to "500 mG (Standart)", 250f to "250 mG (Hassas)").forEach { (presetMg, label) ->
                                    val isMatch = kotlin.math.abs(shockMg - presetMg) < 25f
                                    Surface(
                                        color = if (isMatch) NeonGreen.copy(alpha = 0.25f) else Color(0xFF141926),
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(1.dp, if (isMatch) NeonGreen else Color(0xFF28344A)),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { shockMg = presetMg }
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = if (isMatch) NeonGreen else Color(0xFF94A3B8),
                                                fontWeight = if (isMatch) FontWeight.Black else FontWeight.Bold,
                                                fontSize = 9.5.sp
                                            ),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                            modifier = Modifier.padding(vertical = 4.dp, horizontal = 2.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // 2. BOŞA DÜŞME AÇISI (Derece)
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "Boşa Düşme Açısı",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    )
                                    Text(
                                        text = "Misina boşaldığında kamışın geriye dikleşme açısı",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color(0xFF7E8B9B),
                                            fontSize = 9.5.sp
                                        )
                                    )
                                }
                                Text(
                                    text = String.format(java.util.Locale.US, "%.1f°", dropBackDeg),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFFFFD166),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                            Slider(
                                value = dropBackDeg,
                                onValueChange = { dropBackDeg = it },
                                valueRange = 0.2f..3.0f,
                                steps = 27,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFFFFD166),
                                    activeTrackColor = Color(0xFFFFD166),
                                    inactiveTrackColor = Color(0xFF253046)
                                )
                            )

                            // 🎯 Hızlı Boşa Düşme Presetleri (0.3° / 0.5° / 0.8°)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                listOf(0.3f to "0.3° (Hassas)", 0.5f to "0.5° (Standart)", 0.8f to "0.8° (Dirençli)").forEach { (presetDeg, label) ->
                                    val isMatch = kotlin.math.abs(dropBackDeg - presetDeg) < 0.08f
                                    Surface(
                                        color = if (isMatch) Color(0xFFFFD166).copy(alpha = 0.25f) else Color(0xFF141926),
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(1.dp, if (isMatch) Color(0xFFFFD166) else Color(0xFF28344A)),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { dropBackDeg = presetDeg }
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = if (isMatch) Color(0xFFFFD166) else Color(0xFF94A3B8),
                                                fontWeight = if (isMatch) FontWeight.Black else FontWeight.Bold,
                                                fontSize = 9.5.sp
                                            ),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                            modifier = Modifier.padding(vertical = 4.dp, horizontal = 2.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // 3. HIRSIZLIK / DİKİLME AÇISI (Derece)
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "Hırsızlık / Sehpadan Dikilme Açısı",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    )
                                    Text(
                                        text = "Kamış sehpadan havaya kaldırıldığında tetiklenen açı",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color(0xFF7E8B9B),
                                            fontSize = 9.5.sp
                                        )
                                    )
                                }
                                Text(
                                    text = String.format(java.util.Locale.US, "%.1f°", theftDeg),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFFFF5252),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                            Slider(
                                value = theftDeg,
                                onValueChange = { theftDeg = it },
                                valueRange = 8.0f..40.0f,
                                steps = 31,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFFFF5252),
                                    activeTrackColor = Color(0xFFFF5252),
                                    inactiveTrackColor = Color(0xFF253046)
                                )
                            )
                        }

                        // 4. ÖRNEKLEME ARALIĞI (ms)
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Sensör Dinamik Örnekleme Hızı",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    )
                                    Text(
                                        text = "Sensörün hareket analiz sıklığı",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color(0xFF7E8B9B),
                                            fontSize = 9.5.sp
                                        )
                                    )
                                }
                                Surface(
                                    color = Color(0xFF132032),
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, Color(0xFF2E486D))
                                ) {
                                    Text(
                                        text = "$sampleMs ms (${1000 / sampleMs} Hz)",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            color = Color(0xFF64B5F6),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.5.sp
                                        ),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                listOf(
                                    10 to "10 ms (Ultra Hızlı / 100 Hz)",
                                    20 to "20 ms (Standart / 50 Hz)",
                                    40 to "40 ms (Pil Tasarrufu / 25 Hz)"
                                ).forEach { (ms, label) ->
                                    val isSel = sampleMs == ms
                                    Surface(
                                        color = if (isSel) Color(0xFF18283E) else Color(0xFF131722),
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(1.dp, if (isSel) Color(0xFF64B5F6) else Color(0xFF26334A)),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { sampleMs = ms }
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = if (isSel) Color.White else Color(0xFF94A3B8),
                                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 9.5.sp
                                            ),
                                            modifier = Modifier.padding(vertical = 5.dp, horizontal = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // 5. CANLI KALP ATIŞI (PING_INTERVAL_MS)
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Canlılık Sinyali / Pil Tasarrufu",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    )
                                    Text(
                                        text = "Sensörün çadıra 'çevrimiçiyim' deme sıklığı",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color(0xFF7E8B9B),
                                            fontSize = 9.5.sp
                                        )
                                    )
                                }
                                Surface(
                                    color = Color(0xFF27132F),
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, Color(0xFF5A2570))
                                ) {
                                    Text(
                                        text = if (pingSec >= 3600) "${pingSec / 3600} Saat" else if (pingSec >= 60) "${pingSec / 60} Dk" else "$pingSec sn",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            color = Color(0xFFBA68C8),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.5.sp
                                        ),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                listOf(
                                    45 to "45 sn (Test/Yakın)",
                                    3600 to "1 Saat (Dengeli)",
                                    14400 to "4 Saat (Derin Uyku)"
                                ).forEach { (sec, label) ->
                                    val isMatch = pingSec == sec
                                    Surface(
                                        color = if (isMatch) Color(0xFF381D42) else Color(0xFF131722),
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(1.dp, if (isMatch) Color(0xFFBA68C8) else Color(0xFF26334A)),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { pingSec = sec }
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = if (isMatch) Color.White else Color(0xFF94A3B8),
                                                fontWeight = if (isMatch) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 9.5.sp
                                            ),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                            modifier = Modifier.padding(vertical = 5.dp, horizontal = 2.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Butonlar: Otomatik Eşitle & Sensöre Gönder
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val synced = SensorThresholdConfig.fromSensitivity(activeRod?.sensitivity ?: 5)
                                    strikeAngleDeg = synced.strikeAngleDeg
                                    shockMg = synced.shockAccelThresholdMg
                                    dropBackDeg = synced.dropBackAngleDeg
                                    theftDeg = synced.theftAngleDeg
                                    sampleMs = synced.sampleIntervalMs
                                    pingSec = synced.pingIntervalSec
                                },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFF33425E)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "🔄 Hassasiyete Eşitle",
                                    color = Color(0xFFCBD5E1),
                                    fontSize = 10.5.sp
                                )
                            }

                            Button(
                                onClick = {
                                    val newConfig = SensorThresholdConfig(
                                        shockAccelThresholdMg = shockMg,
                                        dropBackAngleDeg = dropBackDeg,
                                        theftAngleDeg = theftDeg,
                                        strikeAngleDeg = strikeAngleDeg,
                                        sampleIntervalMs = sampleMs,
                                        pingIntervalSec = pingSec
                                    )
                                    onUpdateThresholds(targetRodId, newConfig)
                                    isSavedToSensor = true
                                    coroutineScope.launch {
                                        delay(1500)
                                        isSavedToSensor = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSavedToSensor) Color(0xFF00C853) else NeonGreen
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("save_to_sensor_btn")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isSavedToSensor) Icons.Default.Check else Icons.Default.Sensors,
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = if (isSavedToSensor) "✓ Kaydedildi!" else "📡 Sensöre Kaydet",
                                        color = Color.Black,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // 🔔 CANLI BİLDİRİM VE PUSH PANELİ (FIREBASE FCM)
                NotificationSettingsCard()

                // 4. HIZLI ARAÇLAR & ALARM SESİNİ TEST ET
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "HIZLI ARAÇLAR & TEST:",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )

                    // Alarm Sesini Güvenli Test Et (Normal Vuruş)
                    Surface(
                        color = Color(0xFF101F18),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTestAlarmSound() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = NeonGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "🚨 Düz Vuruş Alarmını Test Et (Siren + Kalama)",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Sazan asılmasında çalan ana alarm anonsu",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    // Boşa Düşme (Drop-Back) Test Et
                    Surface(
                        color = Color(0xFF1B1A12),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFFEAB308).copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTestDropBackSound() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color(0xFFEAB308),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "📉 Boşa Düşme (Swinger) Alarmını Test Et",
                                    color = Color(0xFFFEF08A),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Misina gevşemesinde telaşlı kesik ses uyarısı",
                                    color = Color(0xFFA1A1AA),
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    // Hırsızlık Alarmını Test Et
                    Surface(
                        color = Color(0xFF221213),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTestTheftSound() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "🔒 Hırsızlık / Sehpadan Dikilme Alarmını Test Et",
                                    color = Color(0xFFFCA5A5),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Olta yerinden söküldüğünde acil çakar uyarısı",
                                    color = Color(0xFFA1A1AA),
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    // 🎣 Kalama Açılma / Makara Cırlaması Test Et Butonu
                    Surface(
                        color = Color(0xFF1E170F),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFFFFB703).copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTestReelDragSound() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color(0xFFFFB703),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "🎣 Kalama (Makara Cırlama) Sesini Test Et",
                                    color = Color(0xFFFFE082),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Mekanik kalama boşalma efektini (CZZZT!) çalar",
                                    color = Color(0xFFBCAAA4),
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    // 📲 Güncellemeleri Denetle (GitHub OTA)
                    Surface(
                        color = Color(0xFF131926),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onDismiss()
                                onCheckForUpdates()
                            }
                            .testTag("check_updates_settings_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudDownload,
                                    contentDescription = null,
                                    tint = Color(0xFF60A5FA),
                                    modifier = Modifier.size(18.dp)
                                )
                                Column {
                                    Text(
                                        text = "📲 Güncellemeleri Denetle (OTA)",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Mevcut Sürüm: v$currentAppVersion (GitHub OTA)",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 10.sp
                                    )
                                }
                            }
                            Text(
                                text = "Denetle ➔",
                                color = Color(0xFF60A5FA),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Gece Çadır Modu
                        Surface(
                            color = Color(0xFF1B1812),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFFD97706).copy(alpha = 0.5f)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    onDismiss()
                                    onToggleNightMode()
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NightlightRound,
                                    contentDescription = null,
                                    tint = Color(0xFFFFB703),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Gece Modu",
                                    color = Color(0xFFFFE082),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Kilit İzinleri
                        Surface(
                            color = Color(0xFF121B16),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    onDismiss()
                                    onOpenPermissions()
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LockOpen,
                                    contentDescription = null,
                                    tint = NeonGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Kilit İzni",
                                    color = Color(0xFF86EFAC),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Ham Veri Log
                        Surface(
                            color = Color(0xFF141622),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFF2E3852)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    onDismiss()
                                    onOpenLogs()
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Terminal,
                                    contentDescription = null,
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Terminal",
                                    color = Color(0xFFCBD5E1),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val currentConfig = SensorThresholdConfig(
                        shockAccelThresholdMg = shockMg,
                        dropBackAngleDeg = dropBackDeg,
                        theftAngleDeg = theftDeg,
                        strikeAngleDeg = strikeAngleDeg,
                        sampleIntervalMs = sampleMs,
                        pingIntervalSec = pingSec
                    )
                    onUpdateThresholds(targetRodId, currentConfig)
                    onSave(nameText.trim(), selectedStyle, appVolumeState)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("save_audio_settings_btn")
            ) {
                Text(
                    text = "Kaydet",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2433)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = "İptal",
                    color = Color.White
                )
            }
        },
        containerColor = Color(0xFF141724),
        shape = RoundedCornerShape(20.dp)
    )
}
