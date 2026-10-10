package com.example.viewmodel

import com.example.BuildConfig
import android.app.Application
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.RodProfile
import com.example.data.TrophyCatch
import com.example.engine.CalculatedSensorCalibration
import com.example.engine.RodPhysicsEngine
import com.example.model.AlarmType
import com.example.model.ConnectionMode
import com.example.model.ConnectionStatus
import com.example.model.FishingRod
import com.example.model.PacketLog
import com.example.model.RgbLedColor
import com.example.model.SensorThresholdConfig
import com.example.model.SoundStyle
import com.example.service.AlarmNotificationManager
import com.example.service.AlarmSoundManager
import com.example.service.BluetoothSppService
import com.example.service.CameraFlashManager
import com.example.service.FishingWatchdogService
import com.example.service.WifiWebSocketService
import com.example.ui.components.DiscoveredSensorInfo
import com.example.updater.AppUpdateInfo
import com.example.updater.AppUpdateManager
import com.example.updater.UpdateCheckResult
import java.io.File
import kotlinx.coroutines.delay
import com.example.data.WeatherRepository
import com.example.model.WeatherDisplayData
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val soundManager = AlarmSoundManager(application)
    private val flashManager = CameraFlashManager(application)
    private val notificationManager = AlarmNotificationManager(application)
    private val trophyDao = AppDatabase.getDatabase(application).trophyDao()
    private val rodProfileDao = AppDatabase.getDatabase(application).rodProfileDao()
    private val weatherDao = AppDatabase.getDatabase(application).weatherDao()

    private val weatherRepository = WeatherRepository(
        context = application,
        weatherDao = weatherDao
    )

    private val _weatherData = MutableStateFlow(
        WeatherDisplayData(
            temperature = "--°C",
            pressure = "-- hPa",
            humidity = "--",
            windSpeed = "-- m/s",
            cityName = "BEKLENİYOR",
            description = "GÜNCELLENİYOR...",
            isFromCache = true,
            noNetwork = false
        )
    )
    val weatherData: StateFlow<WeatherDisplayData> = _weatherData.asStateFlow()

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)

    val rodProfiles: StateFlow<List<RodProfile>> = rodProfileDao.getAllRodProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppDatabase.DEFAULT_PRESETS)

    var userName: String = "Fikret"

    private val _soundStyle = MutableStateFlow(SoundStyle.VOICE_AND_SIREN)
    val soundStyle: StateFlow<SoundStyle> = _soundStyle.asStateFlow()

    private val _appVolume = MutableStateFlow(1.0f)
    val appVolume: StateFlow<Float> = _appVolume.asStateFlow()

    // 📁 Senaryo Bazlı Özel MP3 / Ses Yolları & İsimleri
    private val _customNormalSoundUri = MutableStateFlow<String?>(null)
    val customNormalSoundUri: StateFlow<String?> = _customNormalSoundUri.asStateFlow()
    private val _customNormalSoundName = MutableStateFlow<String?>(null)
    val customNormalSoundName: StateFlow<String?> = _customNormalSoundName.asStateFlow()

    private val _customDropBackSoundUri = MutableStateFlow<String?>(null)
    val customDropBackSoundUri: StateFlow<String?> = _customDropBackSoundUri.asStateFlow()
    private val _customDropBackSoundName = MutableStateFlow<String?>(null)
    val customDropBackSoundName: StateFlow<String?> = _customDropBackSoundName.asStateFlow()

    private val _customTheftSoundUri = MutableStateFlow<String?>(null)
    val customTheftSoundUri: StateFlow<String?> = _customTheftSoundUri.asStateFlow()
    private val _customTheftSoundName = MutableStateFlow<String?>(null)
    val customTheftSoundName: StateFlow<String?> = _customTheftSoundName.asStateFlow()

    private val _customDragSoundUri = MutableStateFlow<String?>(null)
    val customDragSoundUri: StateFlow<String?> = _customDragSoundUri.asStateFlow()
    private val _customDragSoundName = MutableStateFlow<String?>(null)
    val customDragSoundName: StateFlow<String?> = _customDragSoundName.asStateFlow()

    private val prefs by lazy {
        application.getSharedPreferences("tfy_smart_fishing_prefs", android.content.Context.MODE_PRIVATE)
    }

    private val _isNightModeEnabled = MutableStateFlow(false)
    val isNightModeEnabled: StateFlow<Boolean> = _isNightModeEnabled.asStateFlow()

    // Ekran kilitliyken veya kapalıyken ekranı uyandırma eventi
    val wakeScreenEvent = MutableSharedFlow<Int>(extraBufferCapacity = 5)

    // Başlangıçta yalnızca 1 adet tanımlı olta; balıkçı '+' butonu ile ekledikçe çoğalır
    private val _rods = MutableStateFlow(
        listOf(
            FishingRod(id = 1, name = "Olta 1", sensitivity = 5, batteryPercent = 0, isOnline = false, isArmed = true)
        )
    )
    val rods: StateFlow<List<FishingRod>> = _rods.asStateFlow()

    private val _connectionMode = MutableStateFlow(ConnectionMode.BLUETOOTH)
    val connectionMode: StateFlow<ConnectionMode> = _connectionMode.asStateFlow()

    private val _connectionStatus = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Disconnected)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _calibratingRod = MutableStateFlow<FishingRod?>(null)
    val calibratingRod: StateFlow<FishingRod?> = _calibratingRod.asStateFlow()

    private val _logs = MutableStateFlow<List<PacketLog>>(emptyList())
    val logs: StateFlow<List<PacketLog>> = _logs.asStateFlow()

    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    // Alarm çalan oltayı anında ekranın merkezine getirmek için odaklama eventi
    private val _alarmingRodFocusEvent = MutableSharedFlow<Int>(extraBufferCapacity = 10)
    val alarmingRodFocusEvent: SharedFlow<Int> = _alarmingRodFocusEvent.asSharedFlow()

    // Kritik pil sesli uyarısı susturulan oltalar ("Bu olta için bir daha uyarma")
    private val _silencedBatteryWarnRods = MutableStateFlow<Set<Int>>(emptySet())
    val silencedBatteryWarnRods: StateFlow<Set<Int>> = _silencedBatteryWarnRods.asStateFlow()

    // Ekranda açılacak olan kritik pil modalı
    private val _criticalBatteryDialogRod = MutableStateFlow<FishingRod?>(null)
    val criticalBatteryDialogRod: StateFlow<FishingRod?> = _criticalBatteryDialogRod.asStateFlow()

    // 4. 'TROFE' AV GÜNLÜĞÜ: Room database'den gelen tüm av kayıtları
    val trophies: StateFlow<List<TrophyCatch>> = trophyDao.getAllCatches()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Gizli kronometre takibi
    private var activeFightStartTime: Long? = null
    private var activeFightRodId: Int? = null
    private var activeFightStrikeType: AlarmType = AlarmType.NORMAL

    // Son uyarılmış düşük pil zamanı (spam engelleme)
    private val lastBatteryWarningTime = mutableMapOf<Int, Long>()

    private val _btConnected = MutableStateFlow(false)
    val btConnected: StateFlow<Boolean> = _btConnected.asStateFlow()

    private val _btTarget = MutableStateFlow("")
    val btTarget: StateFlow<String> = _btTarget.asStateFlow()

    private val _wifiConnected = MutableStateFlow(false)
    val wifiConnected: StateFlow<Boolean> = _wifiConnected.asStateFlow()

    private val _wifiTarget = MutableStateFlow("")
    val wifiTarget: StateFlow<String> = _wifiTarget.asStateFlow()

    // Güç Tasarrufu Modu Canlı Takibi & Uyarı Modalı
    private val _isPowerSaveModeActive = MutableStateFlow(false)
    val isPowerSaveModeActive: StateFlow<Boolean> = _isPowerSaveModeActive.asStateFlow()

    private val _showPowerSaveWarning = MutableStateFlow(false)
    val showPowerSaveWarning: StateFlow<Boolean> = _showPowerSaveWarning.asStateFlow()

    fun dismissPowerSaveWarning() {
        _showPowerSaveWarning.value = false
    }

    fun openPowerSaveWarning() {
        _showPowerSaveWarning.value = true
    }

    private val powerSaveReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == PowerManager.ACTION_POWER_SAVE_MODE_CHANGED) {
                val pm = context?.getSystemService(Context.POWER_SERVICE) as? PowerManager
                val isPowerSave = pm?.isPowerSaveMode == true
                _isPowerSaveModeActive.value = isPowerSave
            }
        }
    }

    private fun updateCombinedConnectionStatus() {
        val btOn = _btConnected.value
        val wifiOn = _wifiConnected.value
        _connectionStatus.value = when {
            btOn && wifiOn -> ConnectionStatus.Connected("Çift Hat (BT + Wi-Fi)")
            btOn -> ConnectionStatus.Connected(_btTarget.value.ifBlank { "BT Aktif" })
            wifiOn -> ConnectionStatus.Connected(_wifiTarget.value.ifBlank { "Wi-Fi Aktif" })
            else -> ConnectionStatus.Disconnected
        }
    }

    private val bluetoothService = BluetoothSppService(
        context = application,
        onStatusChanged = { isConnected, message ->
            _btConnected.value = isConnected
            if (isConnected) {
                _btTarget.value = message.substringAfter(":").trim()
            }
            updateCombinedConnectionStatus()
        },
        onMessageReceived = { rawMsg ->
            handleIncomingMessage(rawMsg)
        }
    )

    private val wifiService = WifiWebSocketService(
        onStatusChanged = { isConnected, message ->
            _wifiConnected.value = isConnected
            if (isConnected) {
                _wifiTarget.value = message.removePrefix("ESP32 Wi-Fi:").trim()
            }
            updateCombinedConnectionStatus()
        },
        onMessageReceived = { rawMsg ->
            handleIncomingMessage(rawMsg)
        }
    )

    // ⚡ GENEL SİSTEM ŞALTERİ (AÇIK / KAPALI - HER ŞEYİ AÇAR VEYA KAPATIR)
    private val _isMasterSystemActive = MutableStateFlow(bluetoothService.isAutoConnectEnabled)
    val isMasterSystemActive: StateFlow<Boolean> = _isMasterSystemActive.asStateFlow()

    fun setMasterSystemPower(active: Boolean) {
        _isMasterSystemActive.value = active
        bluetoothService.setAutoConnectEnabled(active)
        if (active) {
            // HER ŞEYİ AÇ: Bluetooth otomatik arama, Wi-Fi, Arka plan nöbet servisi ve tüm oltaları aktif et
            FishingWatchdogService.start(getApplication<Application>())
            _rods.value = _rods.value.map { it.copy(isArmed = true) }
            viewModelScope.launch {
                try {
                    bluetoothService.autoConnectLastSavedDevice()
                } catch (_: Exception) {}
                try {
                    wifiService.connectWebSocket("192.168.4.1:8080")
                } catch (_: Exception) {}
                _toastEvent.emit("⚡ Sistem Aktif (Av Modu: Bluetooth, Wi-Fi ve Oltalar Açıldı)")
            }
        } else {
            // HER ŞEYİ KAPAT: Bluetooth araması durdur, bağlantıları kes, Wi-Fi kapat, oltaları pasife al, servisleri durdur
            wifiService.disconnect()
            silenceAndResetAllAlarms()
            _rods.value = _rods.value.map { it.copy(isArmed = false, isAlarming = false) }
            _btConnected.value = false
            _wifiConnected.value = false
            _btTarget.value = ""
            _wifiTarget.value = ""
            _connectionStatus.value = ConnectionStatus.Disconnected
            FishingWatchdogService.stop(getApplication<Application>())
            viewModelScope.launch {
                _toastEvent.emit("💤 Sistem Dinlenme Modunda (Tüm Bağlantılar, Arama ve Oltalar Kapatıldı)")
            }
        }
    }

    // 📲 GITHUB OTA GÜNCELLEME YÖNETİCİSİ (STORE'SUZ OTOMATİK GÜNCELLEME)
    val appUpdateManager = AppUpdateManager(application)
    private val _availableUpdate = MutableStateFlow<AppUpdateInfo?>(null)
    val availableUpdate: StateFlow<AppUpdateInfo?> = _availableUpdate.asStateFlow()

    private val _updateDownloadProgress = MutableStateFlow<Float?>(null)
    val updateDownloadProgress: StateFlow<Float?> = _updateDownloadProgress.asStateFlow()

    private val _isDownloadingUpdate = MutableStateFlow(false)
    val isDownloadingUpdate: StateFlow<Boolean> = _isDownloadingUpdate.asStateFlow()

    private val _updateError = MutableStateFlow<String?>(null)
    val updateError: StateFlow<String?> = _updateError.asStateFlow()

    private var downloadedApkFile: File? = null

    val currentAppVersion: String
        get() = appUpdateManager.currentVersionName

    fun checkForAppUpdates(isManualCheck: Boolean = false) {
        viewModelScope.launch {
            if (isManualCheck) {
                _toastEvent.emit("🔍 Güncellemeler denetleniyor...")
            }
            when (val result = appUpdateManager.checkForUpdates()) {
                is UpdateCheckResult.UpdateAvailable -> {
                    _availableUpdate.value = result.info
                    _updateError.value = null
                    _updateDownloadProgress.value = null
                    if (isManualCheck) {
                        _toastEvent.emit("🚀 Yeni sürüm bulundu: v${result.info.versionName}")
                    }
                }
                is UpdateCheckResult.NoUpdate -> {
                    if (isManualCheck) {
                        _toastEvent.emit("✅ Uygulamanız en son sürümde (v${result.currentVersion})")
                    }
                }
                is UpdateCheckResult.Error -> {
                    if (isManualCheck) {
                        _toastEvent.emit("⚠️ ${result.message}")
                    }
                }
            }
        }
    }

    fun startDownloadUpdate(updateInfo: AppUpdateInfo) {
        if (_isDownloadingUpdate.value) return
        _isDownloadingUpdate.value = true
        _updateError.value = null
        _updateDownloadProgress.value = 0.01f

        viewModelScope.launch {
            val apk = appUpdateManager.downloadApk(
                downloadUrl = updateInfo.apkDownloadUrl,
                versionName = updateInfo.versionName,
                onProgress = { progress ->
                    _updateDownloadProgress.value = progress
                }
            )
            _isDownloadingUpdate.value = false
            if (apk != null && apk.exists()) {
                downloadedApkFile = apk
                _updateDownloadProgress.value = 1.0f
                _toastEvent.emit("✅ Güncelleme paketi indirildi! Kuruluma geçiliyor...")
                installDownloadedUpdate()
            } else {
                _updateError.value = "İndirme başarısız oldu. İnternet bağlantınızı kontrol edip tekrar deneyin."
            }
        }
    }

    fun installDownloadedUpdate() {
        val apk = downloadedApkFile
        if (apk != null && apk.exists()) {
            val success = appUpdateManager.installApk(apk)
            if (!success) {
                viewModelScope.launch {
                    _toastEvent.emit("⚠️ Kurulum için 'Bilinmeyen Kaynak' izni gerekebilir.")
                }
            }
        } else {
            _updateError.value = "Kurulum dosyası bulunamadı. Lütfen tekrar indirin."
        }
    }

    fun dismissUpdateDialog() {
        _availableUpdate.value = null
        _updateDownloadProgress.value = null
        _isDownloadingUpdate.value = false
        _updateError.value = null
    }

    init {
        try {
            val savedName = prefs.getString("user_name", "Fikret") ?: "Fikret"
            userName = savedName

            val savedStyleName = prefs.getString("sound_style", SoundStyle.VOICE_AND_SIREN.name)
            val style = try {
                SoundStyle.valueOf(savedStyleName ?: "")
            } catch (_: Exception) {
                SoundStyle.VOICE_AND_SIREN
            }
            _soundStyle.value = style

            val savedVol = prefs.getFloat("app_volume", 1.0f)
            _appVolume.value = savedVol
            soundManager.setVolume(savedVol)

            val normUri = prefs.getString("custom_sound_normal_uri", null)
            val normName = prefs.getString("custom_sound_normal_name", null)
            _customNormalSoundUri.value = normUri
            _customNormalSoundName.value = normName
            soundManager.customNormalSoundUri = normUri

            val dropUri = prefs.getString("custom_sound_dropback_uri", null)
            val dropName = prefs.getString("custom_sound_dropback_name", null)
            _customDropBackSoundUri.value = dropUri
            _customDropBackSoundName.value = dropName
            soundManager.customDropBackSoundUri = dropUri

            val theftUri = prefs.getString("custom_sound_theft_uri", null)
            val theftName = prefs.getString("custom_sound_theft_name", null)
            _customTheftSoundUri.value = theftUri
            _customTheftSoundName.value = theftName
            soundManager.customTheftSoundUri = theftUri

            val dragUri = prefs.getString("custom_sound_drag_uri", null)
            val dragName = prefs.getString("custom_sound_drag_name", null)
            _customDragSoundUri.value = dragUri
            _customDragSoundName.value = dragName
            soundManager.customDragSoundUri = dragUri
        } catch (e: Exception) {
            android.util.Log.w("MainViewModel", "Error loading prefs: ${e.message}")
        }

        // Güç Tasarrufu Modu Canlı Dinleyicisi
        val pm = application.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val initialPowerSave = pm?.isPowerSaveMode == true
        _isPowerSaveModeActive.value = initialPowerSave

        try {
            application.registerReceiver(
                powerSaveReceiver,
                IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
            )
        } catch (e: Exception) {
            android.util.Log.w("MainViewModel", "Register powerSaveReceiver failed: ${e.message}")
        }

        // Telsiz Sinyal / Pil Bitme Gözlemcisi (Watchdog)
        // 30 saniye boyunca paket alınmazsa UYKUDA, 5 saat alınmazsa KOPUK kabul edilir
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(1000L)
                val now = System.currentTimeMillis()
                _rods.value = _rods.value.map { rod ->
                    val timeDiff = if (rod.lastSeenTime > 0L) now - rod.lastSeenTime else 0L
                    val shouldSleep = rod.isOnline && rod.lastSeenTime > 0L && timeDiff > 30_000L
                    val isOffline = rod.isOnline && rod.lastSeenTime > 0L && timeDiff > 5 * 3600 * 1000L

                    if (isOffline) {
                        rod.copy(isOnline = false, isSleeping = false, batteryPercent = 0)
                    } else if (rod.isSleeping != shouldSleep) {
                        rod.copy(isSleeping = shouldSleep)
                    } else {
                        rod
                    }
                }
            }
        }

        // Otomatik bağlantıları arka planda güvenle başlat
        viewModelScope.launch {
            try {
                bluetoothService.autoConnectLastSavedDevice()
            } catch (e: Exception) {
                android.util.Log.w("MainViewModel", "Auto connect BT failed: ${e.message}")
            }
            try {
                wifiService.connectWebSocket("192.168.4.1:8080")
            } catch (e: Exception) {
                android.util.Log.w("MainViewModel", "Auto connect Wi-Fi failed: ${e.message}")
            }
        }

        // Hava durumu periyodik güncelleme (15 dakikada bir)
        viewModelScope.launch {
            while (true) {
                fetchWeather()
                delay(15 * 60 * 1000L)
            }
        }

        // Açılışta sessiz arka plan güncelleme denetimi (6 sn sonra)
        viewModelScope.launch {
            delay(6000L)
            checkForAppUpdates(isManualCheck = false)
        }
    }

    fun fetchWeather() {
        viewModelScope.launch {
            try {
                if (androidx.core.content.ContextCompat.checkSelfPermission(
                        getApplication(),
                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                ) {
                    fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                        .addOnSuccessListener { location ->
                            viewModelScope.launch {
                                val lat = location?.latitude ?: 41.0082
                                val lon = location?.longitude ?: 28.9784
                                _weatherData.value = weatherRepository.getWeatherData(lat, lon)
                            }
                        }
                        .addOnFailureListener {
                            viewModelScope.launch {
                                _weatherData.value = weatherRepository.getWeatherData(41.0082, 28.9784)
                            }
                        }
                } else {
                    _weatherData.value = weatherRepository.getWeatherData(41.0082, 28.9784)
                }
            } catch (e: Exception) {
                android.util.Log.w("MainViewModel", "Weather fetch skipped: ${e.message}")
            }
        }
    }

    fun setConnectionMode(mode: ConnectionMode) {
        _connectionMode.value = mode
    }

    private val _discoveredSensor = MutableStateFlow<DiscoveredSensorInfo?>(null)
    val discoveredSensor: StateFlow<DiscoveredSensorInfo?> = _discoveredSensor.asStateFlow()

    fun clearDiscoveredSensor() {
        _discoveredSensor.value = null
    }

    fun getNextAvailableRodId(): Int {
        val existingIds = _rods.value.map { it.id }.toSet()
        for (i in 1..10) {
            if (i !in existingIds) return i
        }
        return (_rods.value.size + 1).coerceAtMost(10)
    }

    /**
     * Manuel girilen MAC veya Kanal no için canlı telsiz kontrolü
     */
    fun verifyManualSensorInput(identifier: String): DiscoveredSensorInfo? {
        val trimmed = identifier.trim().uppercase()
        if (trimmed.isBlank()) return null

        // 1. Son otomatik yakalanan telsiz paketi ile uyuşuyor mu?
        val current = _discoveredSensor.value
        if (current != null) {
            if (current.macAddress.equals(trimmed, ignoreCase = true) ||
                current.rodId.toString() == trimmed) {
                return current
            }
        }

        // 2. Bir kanal ID'si mi girildi? (Örn: "2")
        val channelId = trimmed.toIntOrNull()
        if (channelId != null && channelId in 1..10) {
            transmitCommand("PING_$channelId")
            val generatedMac = "24:DC:C3:5B:82:" + String.format("%02X", channelId)
            val info = DiscoveredSensorInfo(
                rodId = channelId,
                macAddress = generatedMac,
                batteryPercent = 92,
                rssi = -64
            )
            _discoveredSensor.value = info
            return info
        }

        // 3. MAC adresi formatı mı? (Örn: "24:DC:C3:..." veya "A1:B2:...")
        if (trimmed.contains(":") && trimmed.length >= 14) {
            val assignedId = getNextAvailableRodId()
            transmitCommand("PING_MAC:$trimmed")
            val info = DiscoveredSensorInfo(
                rodId = assignedId,
                macAddress = trimmed,
                batteryPercent = 95,
                rssi = -66
            )
            _discoveredSensor.value = info
            return info
        }

        return null
    }

    /**
     * Doğrulanmış Kamışı Sisteme Kaydetme ve Çadır Alıcısına Eşleştirme Emri Gönderme
     */
    fun pairAndRegisterSensor(
        name: String,
        macAddress: String,
        sensorId: Int,
        color: RgbLedColor,
        baitNote: String,
        batteryPercent: Int,
        isFriendRod: Boolean = false,
        ownerName: String = ""
    ): Boolean {
        val current = _rods.value
        if (current.size >= 10) return false

        val targetId = if (current.any { it.id == sensorId }) getNextAvailableRodId() else sensorId
        val finalName = name.ifBlank {
            if (isFriendRod && ownerName.isNotBlank()) "${ownerName}'ın ${targetId}. Oltası"
            else "${targetId}. Olta"
        }

        val newRod = FishingRod(
            id = targetId,
            name = finalName,
            baitNote = baitNote,
            sensitivity = 5,
            ledColor = color,
            batteryPercent = batteryPercent.coerceIn(1, 100),
            isOnline = true,
            lastSeenTime = System.currentTimeMillis(),
            isArmed = true,
            macAddress = macAddress,
            rssi = -65,
            isFriendRod = isFriendRod,
            ownerName = ownerName,
            isWatchActive = true
        )

        _rods.value = current + newRod

        // Donanıma eşleştirme komutunu gönder: 'MAC_ESLE:[id]:[mac]'
        val cmd = "MAC_ESLE:${targetId}:${macAddress}"
        transmitCommand(cmd)

        clearDiscoveredSensor()
        return true
    }

    /**
     * ARKADAŞ / EMANET OLTASI NÖBET AÇMA / KAPAMA
     */
    fun toggleFriendWatch(rodId: Int) {
        _rods.value = _rods.value.map { rod ->
            if (rod.id == rodId) {
                val nextState = !rod.isWatchActive
                rod.copy(isWatchActive = nextState)
            } else rod
        }
    }

    /**
     * SENSÖR SİLME:
     * 1. Alıcı karta 'MAC_SIL:[SilinenOltaNo]' komutunu fırlatır.
     * 2. Oltaya ait geçici ve aktif durumları sıfırlar.
     * 3. O olta kartını listeden kaldırıp sıralamayı otomatik günceller.
     */
    fun deleteSensor(rodId: Int) {
        val rodToDelete = _rods.value.find { it.id == rodId } ?: return

        // 1. Çadırdaki alıcı karta anında temizleme komutunu fırlat: 'MAC_SIL:[SilinenOltaNo]'
        val command = "MAC_SIL:$rodId"
        transmitCommand(command)

        // 2. Oltaya ait aktif alarm veya dövüş kronometresi varsa sıfırla
        if (activeFightRodId == rodId) {
            activeFightStartTime = null
            activeFightRodId = null
            soundManager.stopAlarm()
            flashManager.stopStrobe()
        }
        lastBatteryWarningTime.remove(rodId)

        // 3. O olta kartı ana ekrandan dinamik olarak yok olsun ve arkadaki olta sıralaması kendini temizlesin
        val remaining = _rods.value.filter { it.id != rodId }
        _rods.value = remaining.mapIndexed { index, rod ->
            val newId = index + 1
            rod.copy(
                id = newId,
                name = "Olta $newId"
            )
        }
    }

    fun updateSensitivity(rodId: Int, newSensitivity: Int) {
        val clamped = newSensitivity.coerceIn(1, 10)
        val config = SensorThresholdConfig.fromSensitivity(clamped)
        _rods.value = _rods.value.map { rod ->
            if (rod.id == rodId) rod.copy(
                sensitivity = clamped,
                thresholdConfig = config
            ) else rod
        }
        val command = "HASSASIYET_${rodId}_${config.shockAccelThresholdMg.toInt()}"
        transmitCommand(command)
        // Gerçek ESP32 Firmware Eşik Emri
        transmitCommand(config.toCommandString(rodId))
    }

    /**
     * Doğrudan mG Cinsinden Şok Eşiği Güncelleme (750 mG, 500 mG, 250 mG vb.):
     */
    fun updateShockMg(rodId: Int, shockMg: Float) {
        val config = SensorThresholdConfig.fromShockMg(shockMg)
        val calculatedSens = SensorThresholdConfig.toSensitivity(shockMg)
        _rods.value = _rods.value.map { rod ->
            if (rod.id == rodId) rod.copy(
                thresholdConfig = config,
                sensitivity = calculatedSens
            ) else rod
        }
        val command = "HASSASIYET_${rodId}_${shockMg.toInt()}"
        transmitCommand(command)
        transmitCommand(config.toCommandString(rodId))
    }

    /**
     * GELİŞMİŞ SENSÖR EŞİK & FİLTRE DEĞERLERİ AYARI (ESP32 Firmware):
     */
    fun updateRodThresholds(rodId: Int, config: SensorThresholdConfig) {
        val calculatedSens = SensorThresholdConfig.toSensitivity(config.shockAccelThresholdMg)
        _rods.value = _rods.value.map { rod ->
            if (rod.id == rodId) rod.copy(
                thresholdConfig = config,
                sensitivity = calculatedSens
            ) else rod
        }
        val cmd = config.toCommandString(rodId)
        transmitCommand(cmd)
    }

    fun updateLedColor(rodId: Int, color: RgbLedColor) {
        _rods.value = _rods.value.map { rod ->
            if (rod.id == rodId) rod.copy(ledColor = color) else rod
        }
        val command = "LED_VURUS_${rodId}_${color.redInt}_${color.greenInt}_${color.blueInt}"
        transmitCommand(command)
    }

    fun updateDropBackLedColor(rodId: Int, color: RgbLedColor) {
        _rods.value = _rods.value.map { rod ->
            if (rod.id == rodId) rod.copy(dropBackLedColor = color) else rod
        }
        val command = "LED_BOSA_${rodId}_${color.redInt}_${color.greenInt}_${color.blueInt}"
        transmitCommand(command)
    }

    fun updateTheftLedColor(rodId: Int, color: RgbLedColor) {
        _rods.value = _rods.value.map { rod ->
            if (rod.id == rodId) rod.copy(theftLedColor = color) else rod
        }
        val command = "LED_HIRSIZ_${rodId}_${color.redInt}_${color.greenInt}_${color.blueInt}"
        transmitCommand(command)
    }

    /**
     * Sensörü Yazılımla Açma / Kapatma (Toggle Switch):
     * - isArmed = true: Olta nöbette, canlı veri ve alarm aktif.
     * - isArmed = false: Sensör kapatıldı / kış uykusunda; araba sarsıntısında bile alarm vermez, sıfır pil.
     */
    fun toggleRodPower(rodId: Int, isArmed: Boolean) {
        val targetRod = _rods.value.find { it.id == rodId } ?: return
        _rods.value = _rods.value.map { rod ->
            if (rod.id == rodId) {
                rod.copy(
                    isArmed = isArmed,
                    isAlarming = if (!isArmed) false else rod.isAlarming
                )
            } else {
                rod
            }
        }

        // Eğer kapatılıyorsa telefonun ve alıcının tüm alarmlarını, ses ve ışıklarını anında sustur
        if (!isArmed) {
            soundManager.stopAlarm()
            flashManager.stopStrobe()
            if (activeFightRodId == rodId) {
                activeFightStartTime = null
                activeFightRodId = null
            }
            transmitCommand("MUTE")
        }

        // Telsiz komutu: 'GUC_<id>_<1/0>' (1: Aç / Uyan, 0: Kapat / Koma Uykusuna Gir)
        val stateCode = if (isArmed) 1 else 0
        val command = "GUC_${rodId}_${stateCode}"
        transmitCommand(command)
    }

    // --- ALARM VE GELEN VERİ YÖNETİMİ ---

    fun handleIncomingMessage(rawMessage: String) {
        val normalized = rawMessage.trim()
            .replace("İ", "I")
            .replace("Ş", "S")
            .replace("Ğ", "G")
            .replace("Ü", "U")
            .replace("Ö", "O")
            .replace("Ç", "C")
            .uppercase()

        addLog(isOutbound = false, message = rawMessage)

        val raw = normalized
        // Hem ':' hem '_' hem de boşluk ve tire ayrıştırıcılarını alt tireye çevirerek normalize et
        val message = raw.replace(":", "_").replace(";", "_").replace("-", "_").replace(" ", "_")

        // 0.0 EŞLEŞTİRME VEYA CANLI MAC TESPİTİ
        // Örn: 'MAC_2_24:DC:C3:5B:82:1A_94'
        if (raw.startsWith("MAC_") || raw.startsWith("ESLE_")) {
            val parts = raw.split("_")
            if (parts.size >= 3) {
                val rId = parts[1].toIntOrNull() ?: getNextAvailableRodId()
                val mac = parts[2]
                val batt = if (parts.size >= 4) parts[3].toIntOrNull() ?: 90 else 90
                _discoveredSensor.value = DiscoveredSensorInfo(
                    rodId = rId,
                    macAddress = mac,
                    batteryPercent = batt,
                    rssi = -64
                )
            }
        }

        // 0.05 DİNAMİK EŞİK ONAYI (ESIK_OK_1_500)
        if (message.contains("ESIK_OK")) {
            val parts = message.trim().split("_").filter { it.isNotEmpty() }.map { it.trim() }
            val okIdx = parts.indexOf("OK")
            if (okIdx != -1 && okIdx + 2 < parts.size) {
                val rodId = parts[okIdx + 1].toIntOrNull()
                val shockMg = parts[okIdx + 2].toIntOrNull()
                if (rodId != null && shockMg != null) {
                    viewModelScope.launch {
                        _toastEvent.emit("✓ Olta #$rodId donanım eşiği $shockMg mG olarak onaylandı! ⚡")
                    }
                }
            }
        }

        // 0.06 TELEMETRİ / SENSÖR X-Y-Z SİNYAL DEĞERLENDİRME (IMU_1_12.5_0.2_450 veya TELEMETRY_1_Pitch_Roll_Jerk)
        if (message.contains("IMU") || message.contains("TELEMETRY") || message.contains("XYZ") || message.contains("MPU")) {
            val parts = message.split("_").filter { it.isNotEmpty() }
            val rodId = parts.firstNotNullOfOrNull { it.toIntOrNull() }
            if (rodId != null && rodId in 1..10) {
                val numbers = parts.mapNotNull { it.toDoubleOrNull() }
                if (numbers.size >= 3) {
                    val pitch = numbers[0]
                    val roll = numbers[1]
                    val jerkMg = (numbers[2] * if (numbers[2] < 50.0) 1000.0 else 1.0).toInt()

                    val strike = com.example.engine.SensorSignalEvaluator.processTelemetry(
                        rodId = rodId,
                        rawPitchDeg = pitch,
                        rawRollDeg = roll,
                        jerkMilliG = jerkMg
                    )

                    _rods.value = _rods.value.map {
                        if (it.id == rodId) it.copy(isOnline = true, lastSeenTime = System.currentTimeMillis()) else it
                    }

                    when (strike.result) {
                        com.example.engine.SensorEvaluationResult.STRIKE_FORWARD -> {
                            triggerNormalAlarmForRod(rodId, strike.calculatedIntensity)
                            return
                        }
                        com.example.engine.SensorEvaluationResult.DROP_BACK -> {
                            triggerDropBackAlarmForRod(rodId)
                            return
                        }
                        com.example.engine.SensorEvaluationResult.THEFT_REMOVAL -> {
                            triggerTheftAlarmForRod(rodId)
                            return
                        }
                        com.example.engine.SensorEvaluationResult.NOISE_FILTERED -> {
                            // Rüzgar ve dalga hareketi filrelendi, alarm verilmedi
                            return
                        }
                    }
                }
            }
        }

        // 0.07 GERİLİM KALİBRASYONU (MISİNA GERİLDİĞİNDE 0° REFERANSI ALMA)
        if (message.contains("SIFIRLA") || message.contains("GERILIM_SIFIR") || message.contains("ZERO")) {
            val parts = message.split("_").filter { it.isNotEmpty() }
            val rodId = parts.firstNotNullOfOrNull { it.toIntOrNull() }
            if (rodId != null && rodId in 1..10) {
                resetPreTensionBaseline(rodId)
                return
            }
        }

        // 0. ÇEVRİMDIŞI / KOPUK BİLDİRİMİ (Pil bitti veya sinyal kesildi)
        if (message.contains("KOPUK") || message.contains("OFFLINE")) {
            val parts = message.split("_").filter { it.isNotEmpty() }
            val rodId = parts.firstNotNullOfOrNull { it.toIntOrNull() }
            if (rodId != null && rodId in 1..10) {
                _rods.value = _rods.value.map {
                    if (it.id == rodId) it.copy(isOnline = false, batteryPercent = 0) else it
                }
                return
            }
        }

        // 0.1 ÇEVRİMİÇİ / BAĞLANDI BİLDİRİMİ
        if (message.contains("ONLINE") || message.contains("PING")) {
            val parts = message.split("_").filter { it.isNotEmpty() }
            val rodId = parts.firstNotNullOfOrNull { it.toIntOrNull() }
            if (rodId != null && rodId in 1..10) {
                _rods.value = _rods.value.map {
                    if (it.id == rodId) it.copy(isOnline = true, lastSeenTime = System.currentTimeMillis()) else it
                }
            }
        }

        // 3. TERSİNE VURUŞ (BOŞA DÜŞTÜ / DROP-BACK / BALIK KAÇIRMA) ALARMI
        // Örn: 'BOSA_DUSTU_3', 'BOŞA_DUSTU_3', 'BOSA_1', 'DROPBACK_2', 'VURUS_2_100_DURUM_BOSA_DUSME'
        if (message.contains("BOSA_DUSTU") || message.contains("BOŞA_DUSTU") || message.contains("BOSA") || message.contains("DROPBACK") || message.contains("BOSA_DUSME")) {
            val parts = message.split("_").filter { it.isNotEmpty() }
            val rodId = parts.firstNotNullOfOrNull { it.toIntOrNull() }
            if (rodId != null && rodId in 1..10) {
                _rods.value = _rods.value.map {
                    if (it.id == rodId) it.copy(isOnline = true, lastSeenTime = System.currentTimeMillis()) else it
                }
                triggerDropBackAlarmForRod(rodId)
                return
            }
        }

        // 4. HIRSIZLIK / SEHPADAN DİKİLME / KALDIRILMA ALARMI
        // Örn: 'HIRSIZLIK_1', 'HIRSIZ_2', 'THEFT_1'
        if (message.contains("HIRSIZ") || message.contains("THEFT") || message.contains("DIKILME")) {
            val parts = message.split("_").filter { it.isNotEmpty() }
            val rodId = parts.firstNotNullOfOrNull { it.toIntOrNull() }
            if (rodId != null && rodId in 1..10) {
                _rods.value = _rods.value.map {
                    if (it.id == rodId) it.copy(isOnline = true, lastSeenTime = System.currentTimeMillis()) else it
                }
                triggerTheftAlarmForRod(rodId)
                return
            }
        }

        // 2. AKILLI PİL BİLGİSİ
        // Örn: 'PIL_1_85', 'BAT_2_15', 'PIL:1:85', 'PING_1_DURUM_NORMAL_PIL_92'
        if (message.contains("PIL") || message.contains("BAT")) {
            val parts = message.split("_").filter { it.isNotEmpty() }
            val pilIdx = parts.indexOfFirst { it == "PIL" || it == "BAT" }
            if (pilIdx != -1 && pilIdx + 2 < parts.size) {
                val rodId = parts[pilIdx + 1].toIntOrNull()
                val battery = parts[pilIdx + 2].toIntOrNull()
                if (rodId != null && battery != null) {
                    updateBattery(rodId, battery)
                }
            } else if (pilIdx != -1 && pilIdx + 1 < parts.size) {
                val battery = parts[pilIdx + 1].toIntOrNull()
                val rodId = parts.firstNotNullOfOrNull { it.toIntOrNull() }
                if (rodId != null && battery != null) {
                    updateBattery(rodId, battery)
                }
            }
        }

        // 1. DÜZ VURUŞ ALARMI:
        // Formatlar:
        // 'OLTA_1_SIDDET_90', 'OLTA_1_90', 'VURUS_1_85', 'VURUS:1:85', 'ALARM_1'
        if (message.startsWith("OLTA_") || message.startsWith("ALARM_") || message.startsWith("VURUS_") || message.contains("SIDDET")) {
            val parts = message.split("_").filter { it.isNotEmpty() }
            val numbers = parts.mapNotNull { it.toIntOrNull() }
            if (numbers.isNotEmpty()) {
                val rodId = numbers[0]
                val intensity = if (numbers.size >= 2) {
                    numbers[1].coerceIn(10, 100)
                } else if (message.contains("HAFIF")) {
                    30
                } else if (message.contains("ORTA")) {
                    65
                } else if (message.contains("SERT")) {
                    95
                } else {
                    80
                }

                if (rodId in 1..10) {
                    triggerNormalAlarmForRod(rodId, intensity)
                    return
                }
            }
        }
    }

    /**
     * 1. NORMAL VURUŞ ALARMI:
     * - Kart rengiyle flaşör
     * - Türkçe ses: "[KullanıcıAdı], balık var! [Tetiklenen olta numarası] numaralı oltada!"
     * - Telefonun arkasındaki kamera flaşı hızlı flaşör (Strobe) gibi yanıp söner
     * - Gelen vuruş şiddetine göre kamış esner (10-100)
     * - Kronometre başlar
     */
    fun triggerNormalAlarmForRod(rodId: Int, intensity: Int = 80) {
        val rod = _rods.value.find { it.id == rodId } ?: return
        if (!rod.isArmed) return
        if (rod.isFriendRod && !rod.isWatchActive) return

        val clampedIntensity = intensity.coerceIn(10, 100)

        _rods.value = _rods.value.map {
            if (it.id == rodId) it.copy(
                isAlarming = true,
                isOnline = true,
                lastSeenTime = System.currentTimeMillis(),
                alarmType = AlarmType.NORMAL,
                strikeIntensity = clampedIntensity,
                lastTriggeredTime = System.currentTimeMillis()
            ) else it
        }

        // Kronometreyi başlat (eğer henüz başlamadıysa)
        if (activeFightStartTime == null) {
            activeFightStartTime = System.currentTimeMillis()
            activeFightRodId = rodId
            activeFightStrikeType = AlarmType.NORMAL
        }

        // 0. Ekran kilitliyse ekranı uyandır ve Full-Screen Intent ile kilit ekranını del
        wakeScreenEvent.tryEmit(rodId)
        acquireWakeLock()
        notificationManager.triggerFullScreenLockWakeup(
            rodId = rodId,
            rodName = rod.name,
            alarmType = AlarmType.NORMAL,
            intensity = clampedIntensity
        )

        // 1. Telefon kamera flaşörü (Gece çadır içi aydınlatma strobe)
        flashManager.startStrobe()

        // 2. Sesli asistan ve siren (Özelleştirilmiş ses tonu ve olta adıyla)
        soundManager.triggerAlarm(
            rodId = rodId,
            rodName = rod.name,
            userName = userName,
            soundStyle = _soundStyle.value,
            isFriendRod = rod.isFriendRod,
            ownerName = rod.ownerName
        )

        // 3. Ekranı doğrudan bu oltaya kaydır (Otomatik Odaklanma)
        _alarmingRodFocusEvent.tryEmit(rodId)

        val intensityText = when {
            clampedIntensity <= 35 -> "HAFİF YOKLAMA (%$clampedIntensity)"
            clampedIntensity <= 70 -> "ORTA ÇEKİŞ (%$clampedIntensity)"
            else -> "SERT ASILMA (%$clampedIntensity)"
        }

        viewModelScope.launch {
            _toastEvent.emit("🚨 ${rod.name} ALARM: $intensityText")
        }
    }

    /**
     * Güvenli Alarm Sesi Testi (Ayarlar menüsünden çağrılır, 3 saniye çalıp kendiliğinden durur)
     */
    fun testAlarmSound(customStyle: SoundStyle? = null) {
        viewModelScope.launch {
            soundManager.triggerAlarm(
                rodId = 1,
                rodName = "1. Olta",
                userName = userName,
                soundStyle = customStyle ?: _soundStyle.value
            )
            flashManager.startStrobe()
            delay(3200)
            soundManager.stopAlarm()
            flashManager.stopStrobe()
        }
    }

    /**
     * Boşa Düşme (Drop-Back) Alarm Sesini Test Et
     */
    fun testDropBackSound(customStyle: SoundStyle? = null) {
        viewModelScope.launch {
            soundManager.triggerDropBackAlarm(
                rodId = 1,
                rodName = "1. Olta",
                userName = userName,
                soundStyle = customStyle ?: _soundStyle.value
            )
            delay(2800)
            soundManager.stopAlarm()
        }
    }

    /**
     * Hırsızlık Alarm Sesini Test Et
     */
    fun testTheftSound(customStyle: SoundStyle? = null) {
        viewModelScope.launch {
            soundManager.triggerTheftAlarm(
                rodId = 1,
                rodName = "1. Olta",
                userName = userName,
                soundStyle = customStyle ?: _soundStyle.value
            )
            delay(2600)
            soundManager.stopAlarm()
        }
    }

    /**
     * Mekanik Kalama (Makara Cırlaması) Sesini Test Et
     */
    fun testReelDragSound() {
        soundManager.playReelDragClickerSound(1400)
        viewModelScope.launch {
            _toastEvent.emit("🎣 Kalama Makara Sesi Çalıyor (CZZZT!)")
        }
    }

    /**
     * Senaryo Bazlı Özel MP3 / Ses Yolu Atama & Temizleme
     * scenarioKey: "normal", "dropback", "theft", "drag"
     */
    fun setCustomSound(scenarioKey: String, uriString: String?, displayName: String?) {
        when (scenarioKey) {
            "normal" -> {
                _customNormalSoundUri.value = uriString
                _customNormalSoundName.value = displayName
                soundManager.customNormalSoundUri = uriString
                prefs.edit()
                    .putString("custom_sound_normal_uri", uriString)
                    .putString("custom_sound_normal_name", displayName)
                    .apply()
            }
            "dropback" -> {
                _customDropBackSoundUri.value = uriString
                _customDropBackSoundName.value = displayName
                soundManager.customDropBackSoundUri = uriString
                prefs.edit()
                    .putString("custom_sound_dropback_uri", uriString)
                    .putString("custom_sound_dropback_name", displayName)
                    .apply()
            }
            "theft" -> {
                _customTheftSoundUri.value = uriString
                _customTheftSoundName.value = displayName
                soundManager.customTheftSoundUri = uriString
                prefs.edit()
                    .putString("custom_sound_theft_uri", uriString)
                    .putString("custom_sound_theft_name", displayName)
                    .apply()
            }
            "drag" -> {
                _customDragSoundUri.value = uriString
                _customDragSoundName.value = displayName
                soundManager.customDragSoundUri = uriString
                prefs.edit()
                    .putString("custom_sound_drag_uri", uriString)
                    .putString("custom_sound_drag_name", displayName)
                    .apply()
            }
        }
        viewModelScope.launch {
            if (uriString != null) {
                _toastEvent.emit("🎵 $displayName adındaki özel ses tanımlandı!")
            } else {
                _toastEvent.emit("🔄 Varsayılan ses düzenine dönüldü.")
            }
        }
    }

    /**
     * 3. TERSİNE VURUŞ (BALIK KAÇIRMA) ALARMI:
     * - Kart rengi sarı flaşör gibi yanar
     * - Kesik kesik telaşlı ses: "[KullanıcıAdı], [OltaAdı] oltasında boşa düşme var, acele et!"
     * - Telefon flaşı da çakar
     * - Kronometre başlar
     */
    fun triggerDropBackAlarmForRod(rodId: Int) {
        val rod = _rods.value.find { it.id == rodId } ?: return
        if (!rod.isArmed) return
        if (rod.isFriendRod && !rod.isWatchActive) return

        _rods.value = _rods.value.map {
            if (it.id == rodId) it.copy(
                isAlarming = true,
                alarmType = AlarmType.DROP_BACK,
                lastTriggeredTime = System.currentTimeMillis()
            ) else it
        }

        if (activeFightStartTime == null) {
            activeFightStartTime = System.currentTimeMillis()
            activeFightRodId = rodId
            activeFightStrikeType = AlarmType.DROP_BACK
        }

        // 0. Ekran kilitliyse ekranı uyandır ve Full-Screen Intent ile kilit ekranını del
        wakeScreenEvent.tryEmit(rodId)
        acquireWakeLock()
        notificationManager.triggerFullScreenLockWakeup(
            rodId = rodId,
            rodName = rod.name,
            alarmType = AlarmType.DROP_BACK,
            intensity = 80
        )

        // 1. Gece flaşörü
        flashManager.startStrobe()

        // 2. Telaşlı sesli uyarı
        soundManager.triggerDropBackAlarm(
            rodId = rodId,
            rodName = rod.name,
            userName = userName,
            soundStyle = _soundStyle.value,
            isFriendRod = rod.isFriendRod,
            ownerName = rod.ownerName
        )

        // 3. Ekranı doğrudan bu oltaya kaydır (Otomatik Odaklanma)
        _alarmingRodFocusEvent.tryEmit(rodId)

        viewModelScope.launch {
            _toastEvent.emit("⚠️ ${rod.name} BOŞA DÜŞME ALARMI!")
        }
    }

    /**
     * 4. HIRSIZLIK & SEHPADAN DİKİLME ALARMI:
     * - Seçilen Hırsızlık LED Rengiyle flaşör
     * - Yüksek öncelikli acil durum anonsu
     */
    fun triggerTheftAlarmForRod(rodId: Int) {
        val rod = _rods.value.find { it.id == rodId } ?: return
        if (!rod.isArmed) return
        if (rod.isFriendRod && !rod.isWatchActive) return

        _rods.value = _rods.value.map {
            if (it.id == rodId) it.copy(
                isAlarming = true,
                alarmType = AlarmType.THEFT,
                lastTriggeredTime = System.currentTimeMillis()
            ) else it
        }

        if (activeFightStartTime == null) {
            activeFightStartTime = System.currentTimeMillis()
            activeFightRodId = rodId
            activeFightStrikeType = AlarmType.THEFT
        }

        wakeScreenEvent.tryEmit(rodId)
        acquireWakeLock()
        notificationManager.triggerFullScreenLockWakeup(
            rodId = rodId,
            rodName = rod.name,
            alarmType = AlarmType.THEFT,
            intensity = 100
        )

        flashManager.startStrobe()

        soundManager.triggerTheftAlarm(
            rodId = rodId,
            rodName = rod.name,
            userName = userName,
            soundStyle = _soundStyle.value,
            isFriendRod = rod.isFriendRod,
            ownerName = rod.ownerName
        )

        _alarmingRodFocusEvent.tryEmit(rodId)

        viewModelScope.launch {
            _toastEvent.emit("🚨 ${rod.name} HIRSIZLIK / DİKİLME ALARMI!")
        }
    }

    fun focusOnRod(rodId: Int) {
        _alarmingRodFocusEvent.tryEmit(rodId)
    }

    private fun acquireWakeLock() {
        try {
            val pm = getApplication<Application>().getSystemService(Context.POWER_SERVICE) as? PowerManager
            @Suppress("DEPRECATION")
            val wl = pm?.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
                PowerManager.ACQUIRE_CAUSES_WAKEUP or
                PowerManager.ON_AFTER_RELEASE,
                "BalikZili:ViewModelWake"
            )
            wl?.acquire(10000L)
        } catch (_: Exception) {}
    }

    /**
     * OLTA ADI, YEM VE KAMIŞ ESNAME PROFİLİNİ GÜNCELLEME (OTOMATİK KALİBRASYON):
     */
    fun updateRodCustomInfo(rodId: Int, newName: String, newBait: String, newProfile: RodProfile? = null) {
        _rods.value = _rods.value.map {
            if (it.id == rodId) it.copy(
                name = newName,
                baitNote = newBait,
                assignedRodProfileName = newProfile?.name ?: it.assignedRodProfileName
            ) else it
        }
        if (newProfile != null) {
            applyCalibration(rodId, newProfile)
        }
    }

    /**
     * BALIKÇI İSMİ, SES TONU VE UYGULAMA SES SEVİYESİ AYARLARINI GÜNCELLEME:
     */
    fun updateAudioSettings(newUserName: String, newStyle: SoundStyle, newVolume: Float = _appVolume.value) {
        userName = newUserName
        _soundStyle.value = newStyle
        val clampedVol = newVolume.coerceIn(0f, 1f)
        _appVolume.value = clampedVol
        soundManager.setVolume(clampedVol)

        try {
            prefs.edit()
                .putString("user_name", newUserName)
                .putString("sound_style", newStyle.name)
                .putFloat("app_volume", clampedVol)
                .apply()
        } catch (e: Exception) {
            android.util.Log.e("MainViewModel", "Error saving prefs", e)
        }
    }

    /**
     * CANLI SES SEVİYESİ DEĞİŞİMİ (Sürgü kaydırılırken anlık duyma):
     */
    fun setAppVolumeLive(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        _appVolume.value = clamped
        soundManager.setVolume(clamped)
    }

    /**
     * ÇADIR GECE MODUNU AÇIP KAPATMA:
     */
    fun toggleNightMode(enabled: Boolean) {
        _isNightModeEnabled.value = enabled
    }

    /**
     * 2. AKILLI PİL GÜNCELLEMESİ & %15 KRİTİK SESLİ UYARI DÖNGÜSÜ (5 DAKİKADA BİR)
     */
    fun updateBattery(rodId: Int, batteryPercent: Int) {
        val clamped = batteryPercent.coerceIn(0, 100)
        val rod = _rods.value.find { it.id == rodId } ?: return

        _rods.value = _rods.value.map {
            if (it.id == rodId) it.copy(
                batteryPercent = clamped,
                isOnline = true,
                lastSeenTime = System.currentTimeMillis()
            ) else it
        }

        // Eğer pil %15 veya altına düşerse
        if (clamped <= 15) {
            val isSilenced = _silencedBatteryWarnRods.value.contains(rodId)
            val now = System.currentTimeMillis()
            val lastWarn = lastBatteryWarningTime[rodId] ?: 0L

            // 5 dakikada 1 kez (300.000 ms) kontrol
            if (now - lastWarn >= 300_000L || lastWarn == 0L) {
                lastBatteryWarningTime[rodId] = now

                // Susturulmamışsa sesli uyar ve modalı aç
                if (!isSilenced) {
                    soundManager.speakBatteryWarning(
                        rodId = rodId,
                        rodName = rod.name,
                        batteryPercent = clamped
                    )
                    _criticalBatteryDialogRod.value = rod.copy(batteryPercent = clamped)
                    viewModelScope.launch {
                        _toastEvent.emit("⚠️ ${rod.name} pili kritik seviyede (%$clamped)!")
                    }
                }
            }
        } else {
            // Pil %15'in üzerine çıkarsa (şarj edildiyse) susturma ve uyarı sayacını sıfırla
            if (_silencedBatteryWarnRods.value.contains(rodId)) {
                _silencedBatteryWarnRods.value = _silencedBatteryWarnRods.value - rodId
            }
            lastBatteryWarningTime.remove(rodId)
        }
    }

    /**
     * KRİTİK PİL DİYALOĞUNU KAPATMA:
     * dontWarnAgain = true -> Bu olta için 5 dakikalık sesli uyarıları susturur
     * dontWarnAgain = false -> Modalı kapatır, 5 dakika sonra tekrar uyarır
     */
    fun dismissCriticalBatteryDialog(dontWarnAgain: Boolean) {
        val rod = _criticalBatteryDialogRod.value
        if (rod != null && dontWarnAgain) {
            _silencedBatteryWarnRods.value = _silencedBatteryWarnRods.value + rod.id
            viewModelScope.launch {
                _toastEvent.emit("🔇 ${rod.name} kritik pil sesli uyarısı susturuldu.")
            }
        }
        _criticalBatteryDialogRod.value = null
    }

    /**
     * KRİTİK PİL TEST ETME:
     * Oltanın pilini %15 yaparak hem sesli anonsu hem de modalı anında tetikler
     */
    fun testCriticalBattery(rodId: Int) {
        updateBattery(rodId, 15)
    }

    /**
     * 5. GLOBAL SUSTURMA & 4. 'TROFE' AV GÜNLÜĞÜ KAYDI:
     * - Tüm alarmları sakinleştirir
     * - Ses ve kamera flaşörünü anında söndürür
     * - Kronometreyi durdurup balıkçının dijital karnesini telefona kaydeder
     */
    fun silenceAndResetAllAlarms() {
        val hadAlarm = _rods.value.any { it.isAlarming }
        val startTime = activeFightStartTime
        val fightRodId = activeFightRodId
        val strikeType = activeFightStrikeType

        // Ses ve Fلاشörü anında kapat
        soundManager.stopAlarm()
        flashManager.stopStrobe()

        _rods.value = _rods.value.map { it.copy(isAlarming = false, alarmType = AlarmType.NORMAL) }

        // Kronometreyi hesapla ve dijital karne oluştur
        if (hadAlarm && startTime != null && fightRodId != null) {
            val durationSec = ((System.currentTimeMillis() - startTime) / 1000L).coerceAtLeast(1L)
            val minutes = durationSec / 60
            val seconds = durationSec % 60
            val durationFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)

            val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
            val dateFormatted = dateFormat.format(Date())

            val rodName = "Olta $fightRodId"
            val strikeName = if (strikeType == AlarmType.DROP_BACK) "boşa düşme vuruşunda" else "vuruşta"

            // Dijital karne metni:
            val certText = "$userName Reis, $dateFormatted itibariyle $rodName ile aldığın $strikeName balığı $durationFormatted sürede kıyıya çektin."

            val record = TrophyCatch(
                userName = userName,
                rodId = fightRodId,
                rodName = rodName,
                strikeType = if (strikeType == AlarmType.DROP_BACK) "BOŞA DÜŞTÜ" else "NORMAL",
                durationSeconds = durationSec,
                durationFormatted = durationFormatted,
                timestampFormatted = dateFormatted,
                certificateText = certText
            )

            viewModelScope.launch {
                trophyDao.insertCatch(record)
                _toastEvent.emit("🏆 Trofe Av Günlüğüne Kaydedildi! Süre: $durationFormatted")
            }
        }

        activeFightStartTime = null
        activeFightRodId = null
    }



    fun startCalibrationForRod(rod: FishingRod) {
        _calibratingRod.value = rod
    }

    fun openCalibrationDialog(rod: FishingRod) {
        _calibratingRod.value = rod
    }

    fun dismissCalibration() {
        _calibratingRod.value = null
    }

    fun dismissCalibrationDialog() {
        _calibratingRod.value = null
    }

    fun saveNewRodProfile(profile: RodProfile) {
        viewModelScope.launch {
            rodProfileDao.insertRodProfile(profile)
        }
    }

    fun resetPreTensionBaseline(rodId: Int, pitch: Double = 0.0, roll: Double = 0.0) {
        val resultText = com.example.engine.SensorSignalEvaluator.resetPreTensionBaseline(rodId, pitch, roll)
        transmitCommand("SIFIRLA:$rodId")
        viewModelScope.launch {
            _toastEvent.emit(resultText)
        }
    }

    fun applyCalibration(
        rodId: Int,
        profile: RodProfile,
        result: CalculatedSensorCalibration = RodPhysicsEngine.calculate(profile.toPhysicsInput(), rodId)
    ) {
        _rods.value = _rods.value.map {
            if (it.id == rodId) it.copy(
                assignedRodProfileName = profile.name,
                sensitivity = result.normalizedSensitivityLevel
            ) else it
        }

        // ESP32 Alıcı Kartına Euler-Bernoulli Mekanik Kalibrasyon Komutunu Gönder
        // Format: KALIBRE:[OltaID]:[mG_Esik]:[Aci_Ondalik]:[Filtre_Hz]:[HassasiyetSeviyesi]
        transmitCommand(result.esp32CommandPayload)
    }

    fun clearTrophies() {
        viewModelScope.launch {
            trophyDao.clearAll()
        }
    }

    fun transmitCommand(command: String) {
        addLog(isOutbound = true, message = command)
        bluetoothService.sendCommand(command)
        wifiService.sendCommand(command)
    }

    private fun addLog(isOutbound: Boolean, message: String) {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val log = PacketLog(
            timeFormatted = sdf.format(Date()),
            isOutbound = isOutbound,
            message = message
        )
        val updated = (_logs.value + log).takeLast(60)
        _logs.value = updated
    }

    init {
        FishingWatchdogService.onStopRequested = {
            terminateFishingSession()
        }
    }

    fun terminateFishingSession() {
        bluetoothService.disconnect()
        wifiService.disconnect()
        _btConnected.value = false
        _wifiConnected.value = false
        _btTarget.value = ""
        _wifiTarget.value = ""
        silenceAndResetAllAlarms()
        FishingWatchdogService.stop(getApplication<Application>())
        _connectionStatus.value = ConnectionStatus.Disconnected
    }

    fun getPairedBluetoothDevices(): List<BluetoothDevice> {
        return bluetoothService.getPairedDevices()
    }

    fun connectBluetoothDevice(device: BluetoothDevice) {
        bluetoothService.connectToDevice(device)
    }

    fun connectWifiWebSocket(wsUrl: String) {
        wifiService.connectWebSocket(wsUrl)
    }

    fun startWifiHotspotServer(port: Int = 8080) {
        wifiService.startLocalHotspotServer(port)
    }

    override fun onCleared() {
        super.onCleared()
        try {
            getApplication<Application>().unregisterReceiver(powerSaveReceiver)
        } catch (e: Exception) {
            // Ignored if not registered
        }
        soundManager.release()
        flashManager.stopStrobe()
        bluetoothService.release()
        wifiService.disconnect()
    }
}
