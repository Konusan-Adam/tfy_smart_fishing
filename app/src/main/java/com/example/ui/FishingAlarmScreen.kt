package com.example.ui

import android.Manifest
import android.os.Build
import android.widget.Toast
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import kotlinx.coroutines.delay
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FishingRod
import com.example.service.PermissionHelper
import com.example.ui.components.AddRodPairingDialog
import com.example.ui.components.AppUpdateDialog
import com.example.ui.components.AudioSettingsDialog
import com.example.ui.components.CalibrationDialog
import com.example.ui.components.ConnectionHeader
import com.example.ui.components.CriticalBatteryDialog
import com.example.ui.components.EditRodDialog
import com.example.ui.components.GlobalSilenceBottomBar
import com.example.ui.components.LockscreenPermissionDialog
import com.example.ui.components.NightTentModeOverlay
import com.example.ui.components.PacketLogDialog
import com.example.ui.components.PowerSaveWarningDialog
import com.example.ui.components.RodCard
import com.example.ui.components.TrophyLogDialog
import com.example.ui.components.WeatherTicker
import com.example.ui.theme.MatteBlackBg
import com.example.ui.theme.NeonGreen
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@Composable
fun FishingAlarmScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val rods by viewModel.rods.collectAsState()
    val connectionMode by viewModel.connectionMode.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val btConnected by viewModel.btConnected.collectAsState()
    val btTarget by viewModel.btTarget.collectAsState()
    val wifiConnected by viewModel.wifiConnected.collectAsState()
    val wifiTarget by viewModel.wifiTarget.collectAsState()
    val calibratingRod by viewModel.calibratingRod.collectAsState()
    val rodProfiles by viewModel.rodProfiles.collectAsState()
    val logs by viewModel.logs.collectAsState()
    val trophies by viewModel.trophies.collectAsState()
    val criticalBatteryDialogRod by viewModel.criticalBatteryDialogRod.collectAsState()
    val isNightModeEnabled by viewModel.isNightModeEnabled.collectAsState()
    val soundStyle by viewModel.soundStyle.collectAsState()
    val discoveredSensor by viewModel.discoveredSensor.collectAsState()
    val isPowerSaveModeActive by viewModel.isPowerSaveModeActive.collectAsState()
    val showPowerSaveWarning by viewModel.showPowerSaveWarning.collectAsState()
    val isMasterSystemActive by viewModel.isMasterSystemActive.collectAsState()

    val availableUpdate by viewModel.availableUpdate.collectAsState()
    val updateDownloadProgress by viewModel.updateDownloadProgress.collectAsState()
    val isDownloadingUpdate by viewModel.isDownloadingUpdate.collectAsState()
    val updateError by viewModel.updateError.collectAsState()
    val appVolume by viewModel.appVolume.collectAsState()

    val customNormalSoundUri by viewModel.customNormalSoundUri.collectAsState()
    val customNormalSoundName by viewModel.customNormalSoundName.collectAsState()
    val customDropBackSoundUri by viewModel.customDropBackSoundUri.collectAsState()
    val customDropBackSoundName by viewModel.customDropBackSoundName.collectAsState()
    val customTheftSoundUri by viewModel.customTheftSoundUri.collectAsState()
    val customTheftSoundName by viewModel.customTheftSoundName.collectAsState()
    val customDragSoundUri by viewModel.customDragSoundUri.collectAsState()
    val customDragSoundName by viewModel.customDragSoundName.collectAsState()

    var showAddRodPairingDialog by remember { mutableStateOf(false) }
    var showLogsDialog by remember { mutableStateOf(false) }
    var showTrophyDialog by remember { mutableStateOf(false) }
    var showAudioSettingsDialog by remember { mutableStateOf(false) }
    var showLockscreenPermissionDialog by remember { mutableStateOf(false) }
    var rodToDelete by remember { mutableStateOf<FishingRod?>(null) }
    var rodToEdit by remember { mutableStateOf<FishingRod?>(null) }

    val weatherData by viewModel.weatherData.collectAsState()

    val hasActiveAlarm = rods.any { it.isAlarming }

    // Toast/Snackbar notifications: Asılı kalmaz, kuyruk yapmaz, 1.2 sn içinde silinir!
    LaunchedEffect(Unit) {
        viewModel.toastEvent.collect { message ->
            snackbarHostState.currentSnackbarData?.dismiss()
            val job = launch {
                snackbarHostState.showSnackbar(
                    message = message,
                    duration = SnackbarDuration.Indefinite
                )
            }
            delay(1200)
            job.cancel()
            snackbarHostState.currentSnackbarData?.dismiss()
        }
    }

    // Horizontal Pager state: Yan yana duran kartlar
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { rods.size }
    )

    // Alarm çalan oltayı anında ekranın merkezine getirme (Otomatik Odaklanma)
    // Örneğin 1. oltadayken 5. olta çaldığı an hemen 5. oltaya kaydırır!
    LaunchedEffect(Unit) {
        viewModel.alarmingRodFocusEvent.collect { rodId ->
            val targetIndex = rods.indexOfFirst { it.id == rodId }
            if (targetIndex != -1) {
                pagerState.animateScrollToPage(targetIndex)
            }
        }
    }

    // Fallback: Alarm durumu değiştiğinde aktif çalan oltaya odaklan
    LaunchedEffect(rods) {
        val alarmingIndex = rods.indexOfLast { it.isAlarming }
        if (alarmingIndex != -1 && alarmingIndex != pagerState.currentPage) {
            pagerState.animateScrollToPage(alarmingIndex)
        }
    }

    // Permissions: Bluetooth, Camera Flashlight, Notifications
    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        // Konum izni verildiyse hava durumunu hemen tazele
        if (result[Manifest.permission.ACCESS_COARSE_LOCATION] == true || 
            result[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            viewModel.fetchWeather()
        }
    }

    LaunchedEffect(Unit) {
        val perms = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            perms.add(Manifest.permission.CAMERA)
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            perms.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            perms.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                perms.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                perms.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                perms.add(Manifest.permission.BLUETOOTH_SCAN)
            }
        } else {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                perms.add(Manifest.permission.BLUETOOTH)
            }
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
                perms.add(Manifest.permission.BLUETOOTH_ADMIN)
            }
        }
        if (perms.isNotEmpty()) {
            permissionsLauncher.launch(perms.toTypedArray())
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(MatteBlackBg),
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .padding(bottom = 85.dp)
                    .clickable { snackbarHostState.currentSnackbarData?.dismiss() }
            ) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = Color(0xFF1E293B),
                    contentColor = Color.White,
                    shape = RoundedCornerShape(8.dp)
                )
            }
        },
        containerColor = MatteBlackBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(top = 0.dp, bottom = 4.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // 2. ÜST BAR: 'Balık Zili' Başlığı, 'Av Günlüğü' İkonu, [+ Olta], [📻 Telsiz] ve [⚙️ Ayarlar]
            ConnectionHeader(
                currentMode = connectionMode,
                status = connectionStatus,
                btConnected = btConnected,
                btTarget = btTarget,
                wifiConnected = wifiConnected,
                wifiTarget = wifiTarget,
                sensorCount = rods.size,
                maxSensors = 10,
                onModeSelect = { mode -> viewModel.setConnectionMode(mode) },
                onAddNewSensor = { showAddRodPairingDialog = true },
                pairedDevices = viewModel.getPairedBluetoothDevices(),
                onSelectBluetoothDevice = { dev -> viewModel.connectBluetoothDevice(dev) },
                onConnectWifi = { url -> viewModel.connectWifiWebSocket(url) },
                onStartWifiHotspotServer = { port -> viewModel.startWifiHotspotServer(port) },
                onOpenLogs = { showLogsDialog = true },
                onOpenTrophyLog = { showTrophyDialog = true },
                onOpenAudioSettings = { showAudioSettingsDialog = true },
                onOpenPermissions = { showLockscreenPermissionDialog = true },
                onToggleNightMode = { viewModel.toggleNightMode(true) },
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            // ⚠️ GÜÇ TASARRUFU UYARI BANNERI
            if (isPowerSaveModeActive) {
                Surface(
                    color = Color(0xFF381216),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFFF3B30)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clickable { viewModel.openPowerSaveWarning() }
                        .testTag("power_save_warning_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFFF453A),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "⚠️ GÜÇ TASARRUFU MODU AÇIK: Kilit ekranında bağlantı kopabilir! Dokunun.",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFFFFEAEA),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // YAN YANA DURAN KARTLAR (YUKARI YASLANDI)
            HorizontalPager(
                state = pagerState,
                contentPadding = PaddingValues(horizontal = 12.dp),
                pageSpacing = 10.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { pageIndex ->
                val rod = rods.getOrNull(pageIndex)
                if (rod != null) {
                    RodCard(
                        rod = rod,
                        onSensitivityChanged = { newLevel ->
                            viewModel.updateSensitivity(rod.id, newLevel)
                        },
                        onShockMgChanged = { newShockMg ->
                            viewModel.updateShockMg(rod.id, newShockMg)
                        },
                        onColorSelected = { color ->
                            viewModel.updateLedColor(rod.id, color)
                        },
                        onDropBackColorSelected = { color ->
                            viewModel.updateDropBackLedColor(rod.id, color)
                        },
                        onTheftColorSelected = { color ->
                            viewModel.updateTheftLedColor(rod.id, color)
                        },

                        onEditRodClick = {
                            rodToEdit = rod
                        },
                        onDeleteClick = {
                            rodToDelete = rod
                        },
                        onToggleFriendWatch = {
                            viewModel.toggleFriendWatch(rod.id)
                        },
                        onTogglePower = { isArmed ->
                            viewModel.toggleRodPower(rod.id, isArmed)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // PAGER İNDİKATÖRÜ (OLTA 1/1 & NOKTALAR) - KARTIN ALTINA ALINDI
            val currentRod = rods.getOrNull(pagerState.currentPage)
            val isCurrentRodArmed = currentRod?.isArmed ?: true

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 0.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = if (isCurrentRodArmed) Color(0xFF10131B) else Color(0xFF200B10),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(
                        1.5.dp,
                        if (isCurrentRodArmed) Color(0xFF222838) else Color(0xFFFF3B30)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Sayaç Metni (Kullanıcının Verdiği Özel Ad ile Canlı Senkronize)
                        val rodDisplayName = currentRod?.name?.uppercase() ?: "OLTA ${pagerState.currentPage + 1}"
                        val pageCounterText = "$rodDisplayName (${pagerState.currentPage + 1}/${rods.size})"

                        Text(
                            text = pageCounterText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (isCurrentRodArmed) NeonGreen else Color(0xFF9E9E9E),
                                fontWeight = FontWeight.Black,
                                fontSize = 15.5.sp,
                                letterSpacing = 0.8.sp
                            ),
                            maxLines = 1
                        )

                        // İkinci Dikey Ayırıcı
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(12.dp)
                                .background(Color(0xFF283042))
                        )

                        // Dairesel İndikatör Noktaları
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            repeat(rods.size) { index ->
                                val isSelected = pagerState.currentPage == index
                                val targetRod = rods.getOrNull(index)
                                val isPageAlarming = targetRod?.isAlarming == true
                                val isTargetArmed = targetRod?.isArmed ?: true

                                val dotColor = when {
                                    isPageAlarming -> Color(0xFFFF2A55)
                                    !isTargetArmed -> Color(0xFF4A4A5A)
                                    isSelected -> NeonGreen
                                    else -> Color(0xFF333A4D)
                                }

                                Box(
                                    modifier = Modifier
                                        .size(if (isSelected) 14.dp else 5.dp, 5.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(dotColor)
                                        .shadow(if (isSelected && isTargetArmed) 4.dp else 0.dp, spotColor = dotColor)
                                        .clickable {
                                            scope.launch { pagerState.animateScrollToPage(index) }
                                        }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 5. GLOBAL SUSTURMA (ALARMI SUSTUR) - İNDİKATÖRÜN ALTINA ALINDI
            GlobalSilenceBottomBar(
                hasActiveAlarm = hasActiveAlarm,
                onSilenceAndReset = {
                    viewModel.silenceAndResetAllAlarms()
                }
            )

            // 6. HAVA DURUMU & BAROMETRE TICKER
            WeatherTicker(
                weatherData = weatherData,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }

    // 2. SENSÖR SİLME ONAY PENCERESİ:
    // Balıkçı 'Sil' ikonuna bastığında ekranda şık bir onay penceresi açılır:
    // "[OltaAdı] sistemden tamamen silinecek. Emin misiniz?"
    // Altında kırmızı renkli bir 'SİL' bir de 'İPTAL' butonu olur.
    rodToDelete?.let { targetRod ->
        AlertDialog(
            onDismissRequest = { rodToDelete = null },
            containerColor = Color(0xFF131620),
            titleContentColor = Color.White,
            textContentColor = Color(0xFFCBD5E1),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .border(1.dp, Color(0xFF2A3144), RoundedCornerShape(24.dp))
                .testTag("delete_confirmation_dialog"),
            icon = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0x26EF4444)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = null,
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(26.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "Sensör Silme",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            },
            text = {
                Text(
                    text = "${targetRod.name} sistemden tamamen silinecek. Emin misiniz?",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = Color(0xFFCBD5E1),
                        lineHeight = 22.sp
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val idToDelete = targetRod.id
                        rodToDelete = null
                        viewModel.deleteSensor(idToDelete)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFDC2626),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("confirm_delete_button")
                ) {
                    Text(
                        text = "SİL",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    )
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { rodToDelete = null },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF94A3B8)
                    ),
                    border = BorderStroke(1.dp, Color(0xFF333D52)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("cancel_delete_button")
                ) {
                    Text(
                        text = "İPTAL",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        )
    }

    // 4. 'Av Günlüğü' Dijital Karne Görüntüleyici
    if (showTrophyDialog) {
        TrophyLogDialog(
            trophies = trophies,
            onClearAll = { viewModel.clearTrophies() },
            onDismiss = { showTrophyDialog = false }
        )
    }

    // Kritik Pil Uyarısı Modalı (%15 altında açılır, 'Bir Daha Uyarma' / '5 Dk Sonra Hatırlat')
    criticalBatteryDialogRod?.let { rod ->
        CriticalBatteryDialog(
            rod = rod,
            onDismiss = { dontWarnAgain ->
                viewModel.dismissCriticalBatteryDialog(dontWarnAgain)
            }
        )
    }

    // 'Kalibre Et' Pop-up penceresi: Kamış Kütüphanesi & Özel Teknik Veri Girişi Modalı
    calibratingRod?.let { rod ->
        CalibrationDialog(
            rod = rod,
            rodProfiles = rodProfiles,
            onDismiss = { viewModel.dismissCalibrationDialog() },
            onApplyCalibration = { rodId, profile, result ->
                viewModel.applyCalibration(rodId, profile, result)
            },
            onSaveNewProfile = { profile ->
                viewModel.saveNewRodProfile(profile)
            },
            onResetPreTension = { rodId ->
                viewModel.resetPreTensionBaseline(rodId)
            }
        )
    }

    // Konsol / Simülasyon paneli (Düz vuruş, boşa düşme ve düşük pil testi)
    if (showLogsDialog) {
        PacketLogDialog(
            logs = logs,
            rods = rods,
            onDismiss = { showLogsDialog = false }
        )
    }

    // 1. Olta Adı, Kamış Modeli ve Yem Düzenleme Modalı (Otomatik Kalibrasyon & Özel Kamış)
    rodToEdit?.let { targetRod ->
        EditRodDialog(
            rod = targetRod,
            availableProfiles = rodProfiles,
            onDismiss = { rodToEdit = null },
            onSaveCustomProfile = { newProfile ->
                viewModel.saveNewRodProfile(newProfile)
            },
            onSave = { newName, newBait, newProfile ->
                viewModel.updateRodCustomInfo(targetRod.id, newName, newBait, newProfile)
            }
        )
    }

    // 2. Ses, Ayarlar ve Sensör Güç Yönetimi Modalı
    if (showAudioSettingsDialog) {
        AudioSettingsDialog(
            currentUserName = viewModel.userName,
            currentSoundStyle = soundStyle,
            currentAppVolume = appVolume,
            onVolumeChangeLive = { vol -> viewModel.setAppVolumeLive(vol) },
            isMasterSystemActive = isMasterSystemActive,
            onToggleMasterSystemPower = { active -> viewModel.setMasterSystemPower(active) },
            currentMode = connectionMode,
            onModeSelect = { mode -> viewModel.setConnectionMode(mode) },
            pairedDevices = viewModel.getPairedBluetoothDevices(),
            onSelectBluetoothDevice = { dev -> viewModel.connectBluetoothDevice(dev) },
            onConnectWifi = { url -> viewModel.connectWifiWebSocket(url) },
            onStartWifiHotspotServer = { port -> viewModel.startWifiHotspotServer(port) },
            rods = rods,
            onToggleRodPower = { rodId, isArmed ->
                viewModel.toggleRodPower(rodId, isArmed)
            },
            onUpdateThresholds = { rodId, config ->
                viewModel.updateRodThresholds(rodId, config)
            },
            onDismiss = { showAudioSettingsDialog = false },
            onSave = { newName, newStyle, newVolume ->
                viewModel.updateAudioSettings(newName, newStyle, newVolume)
            },
            onTerminateFishing = {
                viewModel.terminateFishingSession()
            },
            onOpenLogs = { showLogsDialog = true },
            onOpenPermissions = { showLockscreenPermissionDialog = true },
            onToggleNightMode = { viewModel.toggleNightMode(true) },
            onTestAlarmSound = { viewModel.testAlarmSound() },
            onTestAlarmSoundWithStyle = { style -> viewModel.testAlarmSound(style) },
            onTestDropBackSound = { viewModel.testDropBackSound() },
            onTestTheftSound = { viewModel.testTheftSound() },
            onTestReelDragSound = { viewModel.testReelDragSound() },
            customNormalSoundUri = customNormalSoundUri,
            customNormalSoundName = customNormalSoundName,
            customDropBackSoundUri = customDropBackSoundUri,
            customDropBackSoundName = customDropBackSoundName,
            customTheftSoundUri = customTheftSoundUri,
            customTheftSoundName = customTheftSoundName,
            customDragSoundUri = customDragSoundUri,
            customDragSoundName = customDragSoundName,
            onSetCustomSound = { key, uri, name -> viewModel.setCustomSound(key, uri, name) },
            currentAppVersion = viewModel.currentAppVersion,
            onCheckForUpdates = { viewModel.checkForAppUpdates(isManualCheck = true) }
        )
    }

    // 📲 GITHUB OTA GÜNCELLEME DİYALOĞU
    availableUpdate?.let { updateInfo ->
        AppUpdateDialog(
            updateInfo = updateInfo,
            currentVersion = viewModel.currentAppVersion,
            downloadProgress = updateDownloadProgress,
            isDownloading = isDownloadingUpdate,
            errorMessage = updateError,
            onStartDownload = { viewModel.startDownloadUpdate(updateInfo) },
            onInstallApk = { viewModel.installDownloadedUpdate() },
            onDismiss = { viewModel.dismissUpdateDialog() }
        )
    }

    // Kilit Ekranı & Gece Nöbeti İzinleri Sihirbazı
    if (showLockscreenPermissionDialog) {
        LockscreenPermissionDialog(
            onRequestNotificationPermission = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissionsLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
                }
            },
            onDismiss = { showLockscreenPermissionDialog = false }
        )
    }

    // 3. ÇADIR GECE MODU (OLED HUD & Vuruş Strobe Ekranı)
    if (isNightModeEnabled) {
        NightTentModeOverlay(
            rods = rods,
            onSilenceAlarm = { viewModel.silenceAndResetAllAlarms() },
            onExitNightMode = { viewModel.toggleNightMode(false) }
        )
    }

    // 4. AKILLI TELSİZ EŞLEŞTİRME SİHİRBAZI MODALI (Canlı Sinyal Doğrulama)
    if (showAddRodPairingDialog) {
        AddRodPairingDialog(
            suggestedRodId = viewModel.getNextAvailableRodId(),
            autoDiscoveredSensor = discoveredSensor,
            onVerifyManualInput = { input -> viewModel.verifyManualSensorInput(input) },
            onSaveAndPair = { name, mac, id, color, bait, batt, isFriendRod, ownerName ->
                val paired = viewModel.pairAndRegisterSensor(
                    name = name,
                    macAddress = mac,
                    sensorId = id,
                    color = color,
                    baitNote = bait,
                    batteryPercent = batt,
                    isFriendRod = isFriendRod,
                    ownerName = ownerName
                )
                if (paired) {
                    scope.launch {
                        pagerState.animateScrollToPage(rods.size)
                    }
                }
            },
            onDismiss = {
                showAddRodPairingDialog = false
                viewModel.clearDiscoveredSensor()
            }
        )
    }

    // 5. GÜÇ TASARRUFU UYARI MODALI (Kilit ekranında kesilme riski uyarısı)
    if (showPowerSaveWarning) {
        PowerSaveWarningDialog(
            onDismiss = { viewModel.dismissPowerSaveWarning() }
        )
    }
}
