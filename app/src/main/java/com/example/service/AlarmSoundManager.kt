package com.example.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

import com.example.model.SoundStyle

/**
 * BALIK ZİLİ AKILLI ALARM VE DİNAMİK METALİK KALAMA MOTORU
 * - 0 ms Gecikmeli, 16-bit PCM AudioTrack ile sentezlenen gerçekçi paslanmaz çelik kalama tırnak sesi (Pawl Clicker)
 * - Balık vuruşunda hızlanan, yavaşlayan, kafa atan sazan koşusu (run) dinamiği
 * - Sustur butonuna basılana kadar kesintisiz akış
 * - Türkçe sesli asistan, siren ve kamera flaşör eşgüdümü
 */
class AlarmSoundManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var repeatingJob: Job? = null
    private var dragStreamJob: Job? = null
    private var dragAudioTrack: AudioTrack? = null
    @Volatile private var isDragStreaming = false

    private var toneGenerator: ToneGenerator? = null
    @Volatile var currentVolume: Float = 1.0f
    private val scope = CoroutineScope(Dispatchers.Default)

    // 📁 KULLANICI SEÇİMLİ ÖZEL MP3 / ZİL SESİ ADRESLERİ (TÜM SENARYOLAR İÇİN)
    @Volatile var customNormalSoundUri: String? = null
    @Volatile var customDropBackSoundUri: String? = null
    @Volatile var customTheftSoundUri: String? = null
    @Volatile var customDragSoundUri: String? = null

    private var customMediaPlayer: MediaPlayer? = null

    private fun playCustomAudioLoop(uriString: String) {
        stopCustomMediaPlayer()
        try {
            val uri = Uri.parse(uriString)
            customMediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setDataSource(context, uri)
                isLooping = true
                prepare()
                setVolume(currentVolume, currentVolume)
                start()
            }
        } catch (e: Exception) {
            Log.e("AlarmSoundManager", "Error playing custom audio $uriString", e)
        }
    }

    private fun stopCustomMediaPlayer() {
        try {
            customMediaPlayer?.let { mp ->
                if (mp.isPlaying) {
                    mp.stop()
                }
                mp.release()
            }
        } catch (e: Exception) {
            Log.w("AlarmSoundManager", "Error stopping custom media player", e)
        }
        customMediaPlayer = null
    }

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    init {
        try {
            tts = TextToSpeech(context.applicationContext, this)
            toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 100)
        } catch (e: Exception) {
            Log.e("AlarmSoundManager", "Error initializing audio tools", e)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("tr", "TR"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale.getDefault())
            }
            tts?.setSpeechRate(1.06f)
            tts?.setPitch(1.15f)
            isTtsReady = true
        }
    }

    /**
     * DİNAMİK METALİK KALAMA AKIŞI (SUSTURANA KADAR KESİNTİSİZ ÇALAR)
     * Yalnızca balık açığa doğru asılıp makarayı boşalttığında (Düz Vuruş / Run) devreye girer.
     * - Gerçek paslanmaz çelik tırnak vuruşu (3750 Hz & 5600 Hz metal rezonansı)
     * - Sazan koşusuna göre değişen hızlanma & yavaşlama dalgaları (15 Hz - 88 Hz tık frekansı)
     * - Misinanın porselen halkalardan sürtünme fısıltısı
     */
    fun startContinuousMetallicDrag() {
        stopContinuousMetallicDrag()

        isDragStreaming = true
        dragStreamJob = scope.launch(Dispatchers.IO) {
            val sampleRate = 44100
            val chunkSize = 2205 // 50 ms'lik akış blokları
            val minBufSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(chunkSize * 4)

            try {
                dragAudioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(minBufSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                dragAudioTrack?.play()

                val buffer = ShortArray(chunkSize)
                var totalSampleCount = 0L
                var clickPhaseSamples = 0
                var currentPeriodSamples = 1000

                while (isActive && isDragStreaming) {
                    val timeSec = totalSampleCount / sampleRate.toDouble()

                    // Sazan Depar Dinamiği (Clicks Per Second / Tık Hızı):
                    val clicksPerSec = when {
                        timeSec < 0.7 -> {
                            // 1. Aşama: İlk Kapış & Kamış Eğilmesi (15 Hz -> 45 Hz hızlanma)
                            15.0 + (timeSec / 0.7) * 30.0
                        }
                        timeSec < 3.8 -> {
                            // 2. Aşama: İLK ÇILGIN DEPAR! (Makara Ağlar: 75 Hz - 88 Hz)
                            75.0 + 12.0 * sin(timeSec * 3.8) + (Random.nextDouble() * 6.0 - 3.0)
                        }
                        timeSec < 5.6 -> {
                            // 3. Aşama: Kısa Kafa Atışı & Yavaşlama (30 Hz - 42 Hz)
                            34.0 + 10.0 * cos(timeSec * 2.2)
                        }
                        timeSec < 9.0 -> {
                            // 4. Aşama: İKİNCİ ÖFKELİ DEPAR! (Tekrar Fişekleme: 68 Hz - 84 Hz)
                            76.0 + 10.0 * sin(timeSec * 4.2)
                        }
                        else -> {
                            // 5. Aşama: Sürekli Adrenalin Dalgalanması (40 Hz - 75 Hz)
                            54.0 + 20.0 * sin(timeSec * 0.85) + 6.0 * sin(timeSec * 2.7)
                        }
                    }

                    currentPeriodSamples = (sampleRate / clicksPerSec).toInt().coerceIn(400, 44100)

                    for (i in 0 until chunkSize) {
                        val currentSampleIndex = totalSampleCount + i
                        var sampleVal = 0.0

                        clickPhaseSamples++
                        if (clickPhaseSamples >= currentPeriodSamples) {
                            clickPhaseSamples = 0
                            // Mikro rastgelelik (Gerçek mekanik dişli tırnağı toleransı)
                            val jitter = (Random.nextDouble() * 0.12 - 0.06)
                            currentPeriodSamples = ((sampleRate / clicksPerSec) * (1.0 + jitter)).toInt().coerceIn(380, 44100)
                        }

                        // Tırnak Çarpması & Metal Çınlama Modeli (3750 Hz, 5600 Hz, 1350 Hz):
                        val clickTime = clickPhaseSamples / sampleRate.toDouble()
                        if (clickTime < 0.022) { // 22 ms metalik tırnak çınlaması
                            val decay = exp(-clickTime * 340.0) // Hızlı metalik sönümleme
                            val metalHarmonic1 = sin(2.0 * PI * 3750.0 * clickTime) * 0.65
                            val metalHarmonic2 = sin(2.0 * PI * 5600.0 * clickTime) * 0.45
                            val bodyResonance = sin(2.0 * PI * 1350.0 * clickTime) * 0.30
                            val clickImpulse = (Random.nextDouble() * 2.0 - 1.0) * 0.40

                            sampleVal += (metalHarmonic1 + metalHarmonic2 + bodyResonance + clickImpulse) * decay
                        }

                        // Hızlı akışta porselen halka misina sürtünme hışırtısı (Line Friction):
                        val speedFactor = (clicksPerSec / 85.0).coerceIn(0.0, 1.0)
                        val lineHiss = (Random.nextDouble() * 2.0 - 1.0) * (0.04 + speedFactor * 0.09)
                        sampleVal += lineHiss

                        // Yumuşak Başlangıç (0-50 ms fade in)
                        val masterEnvelope = if (currentSampleIndex < 2205) currentSampleIndex / 2205.0 else 1.0

                        buffer[i] = (sampleVal * 29000.0 * masterEnvelope * currentVolume).toInt().coerceIn(-32767, 32767).toShort()
                    }

                    dragAudioTrack?.write(buffer, 0, chunkSize)
                    totalSampleCount += chunkSize
                }
            } catch (e: Exception) {
                Log.e("AlarmSoundManager", "Error in continuous drag stream", e)
            } finally {
                cleanupDragTrack()
            }
        }
    }

    fun stopContinuousMetallicDrag() {
        isDragStreaming = false
        dragStreamJob?.cancel()
        dragStreamJob = null
        cleanupDragTrack()
    }

    private fun cleanupDragTrack() {
        try {
            dragAudioTrack?.let { track ->
                if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    track.pause()
                    track.flush()
                }
                track.release()
            }
        } catch (_: Exception) {}
        dragAudioTrack = null
    }

    /**
     * AYARLAR MENÜSÜ İÇİN KALAMA SESİ TESTİ (4 Saniyelik Hızlanan/Yavaşlayan Canlı Önizleme)
     */
    fun playReelDragClickerSound(durationMs: Int = 3800) {
        scope.launch {
            applyVolume()
            if (!customDragSoundUri.isNullOrBlank()) {
                playCustomAudioLoop(customDragSoundUri!!)
                delay(durationMs.toLong())
                if (repeatingJob == null) {
                    stopCustomMediaPlayer()
                }
            } else {
                startContinuousMetallicDrag()
                delay(durationMs.toLong())
                if (repeatingJob == null) {
                    stopContinuousMetallicDrag()
                }
            }
        }
    }

    /**
     * NORMAL VURUŞ ALARMI:
     * 1. SUSTURANA KADAR KESİNTİSİZ GERÇEKÇİ KALAMA CIRLAMASI VEYA ÖZEL MP3!
     * 2. Eşzamanlı Türkçe sesli asistan (Kendi oltası veya Arkadaş oltası anonsu).
     * 3. Siren ve titreşim darbeleri.
     */
    fun triggerAlarm(
        rodId: Int,
        rodName: String = "Olta $rodId",
        userName: String = "Fikret",
        soundStyle: SoundStyle = SoundStyle.VOICE_AND_SIREN,
        isFriendRod: Boolean = false,
        ownerName: String = ""
    ) {
        stopAlarm()

        repeatingJob = scope.launch {
            applyVolume()

            if (!customNormalSoundUri.isNullOrBlank()) {
                playCustomAudioLoop(customNormalSoundUri!!)
            } else {
                val startDrag = (soundStyle == SoundStyle.REEL_DRAG_AND_VOICE) || (soundStyle == SoundStyle.VOICE_AND_SIREN)
                if (startDrag) {
                    if (!customDragSoundUri.isNullOrBlank()) {
                        playCustomAudioLoop(customDragSoundUri!!)
                    } else {
                        startContinuousMetallicDrag()
                    }
                } else {
                    stopContinuousMetallicDrag()
                }
            }

            val alarmSentence = if (isFriendRod && ownerName.isNotBlank()) {
                if (userName.isNotBlank()) "$userName! Dikkat! $ownerName'nin $rodName oltasına balık vurdu, koş müdahale et!"
                else "Dikkat! $ownerName'nin $rodName oltasına balık vurdu, koş müdahale et!"
            } else {
                if (userName.isNotBlank()) "$userName, balık var! $rodName oltasında!" else "Balık var! $rodName oltasında!"
            }

            while (isActive) {
                // Siren/Ton çalma (Sadece özel ses tanımlı değilse)
                if (customNormalSoundUri.isNullOrBlank() && soundStyle != SoundStyle.VOICE_ONLY) {
                    try {
                        val toneType = when (soundStyle) {
                            SoundStyle.HIGH_PITCH_SIREN -> ToneGenerator.TONE_SUP_ERROR
                            SoundStyle.CLASSIC_BEEP -> ToneGenerator.TONE_PROP_BEEP2
                            else -> ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK
                        }
                        toneGenerator?.startTone(toneType, if (soundStyle == SoundStyle.CLASSIC_BEEP) 220 else 380)
                    } catch (e: Exception) {
                        Log.w("AlarmSoundManager", "Tone error: ${e.message}")
                    }
                }

                vibratePulse()

                // Sesli asistan
                if (isTtsReady && soundStyle != SoundStyle.CLASSIC_BEEP && soundStyle != SoundStyle.HIGH_PITCH_SIREN) {
                    tts?.setSpeechRate(1.08f)
                    tts?.setPitch(1.15f)
                    val params = android.os.Bundle()
                    params.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_ALARM)
                    tts?.speak(alarmSentence, TextToSpeech.QUEUE_FLUSH, params, "rod_alarm_$rodId")
                }

                delay(if (soundStyle == SoundStyle.CLASSIC_BEEP) 1800 else 3400)
            }
        }
    }

    /**
     * 3. TERSİNE VURUŞ (BALIK KAÇIRMA / BOŞA DÜŞME / DROP-BACK) ALARMI:
     */
    fun triggerDropBackAlarm(
        rodId: Int,
        rodName: String = "Olta $rodId",
        userName: String = "Fikret",
        soundStyle: SoundStyle = SoundStyle.VOICE_AND_SIREN,
        isFriendRod: Boolean = false,
        ownerName: String = ""
    ) {
        stopAlarm()

        repeatingJob = scope.launch {
            applyVolume()

            stopContinuousMetallicDrag()

            if (!customDropBackSoundUri.isNullOrBlank()) {
                playCustomAudioLoop(customDropBackSoundUri!!)
            }

            val urgentSentence = if (isFriendRod && ownerName.isNotBlank()) {
                if (userName.isNotBlank()) "$userName! Dikkat! $ownerName'nin $rodName oltasında boşa düşme var, misina gevşedi, acele et boşluğu al!"
                else "Dikkat! $ownerName'nin $rodName oltasında boşa düşme var, misina gevşedi, acele et boşluğu al!"
            } else {
                if (userName.isNotBlank()) "$userName! $rodName oltasında boşa düşme var, misina gevşedi, acele et boşluğu al!"
                else "$rodName oltasında boşa düşme var, misina gevşedi, acele et boşluğu al!"
            }

            while (isActive) {
                if (customDropBackSoundUri.isNullOrBlank() && soundStyle != SoundStyle.VOICE_ONLY) {
                    try {
                        toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ABBR_ALERT, 180)
                        delay(140)
                        toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ABBR_ALERT, 180)
                    } catch (e: Exception) {
                        Log.w("AlarmSoundManager", "Tone error: ${e.message}")
                    }
                }

                vibrateStaccato()

                if (isTtsReady && soundStyle != SoundStyle.CLASSIC_BEEP && soundStyle != SoundStyle.HIGH_PITCH_SIREN) {
                    tts?.setSpeechRate(1.22f)
                    tts?.setPitch(1.25f)
                    val params = android.os.Bundle()
                    params.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_ALARM)
                    tts?.speak(urgentSentence, TextToSpeech.QUEUE_FLUSH, params, "dropback_$rodId")
                }

                delay(2800)
            }
        }
    }

    /**
     * 5. HIRSIZLIK / SEHPADAN DİKİLME / KALDIRILMA ALARMI:
     */
    fun triggerTheftAlarm(
        rodId: Int,
        rodName: String = "Olta $rodId",
        userName: String = "Fikret",
        soundStyle: SoundStyle = SoundStyle.VOICE_AND_SIREN,
        isFriendRod: Boolean = false,
        ownerName: String = ""
    ) {
        stopAlarm()

        repeatingJob = scope.launch {
            applyVolume()
            stopContinuousMetallicDrag()

            if (!customTheftSoundUri.isNullOrBlank()) {
                playCustomAudioLoop(customTheftSoundUri!!)
            }

            val theftSentence = if (userName.isNotBlank()) {
                "Tehlike! $userName, $rodName sehpadan kaldırıldı veya dikildi! Hırsızlık uyarısı!"
            } else {
                "Tehlike! $rodName sehpadan kaldırıldı veya dikildi! Hırsızlık uyarısı!"
            }

            while (isActive) {
                if (customTheftSoundUri.isNullOrBlank() && soundStyle != SoundStyle.VOICE_ONLY) {
                    try {
                        toneGenerator?.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 450)
                    } catch (e: Exception) {
                        Log.w("AlarmSoundManager", "Theft tone error", e)
                    }
                }

                vibratePulse()

                if (isTtsReady && soundStyle != SoundStyle.CLASSIC_BEEP && soundStyle != SoundStyle.HIGH_PITCH_SIREN) {
                    tts?.setSpeechRate(1.15f)
                    tts?.setPitch(1.30f)
                    val params = android.os.Bundle()
                    params.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_ALARM)
                    tts?.speak(theftSentence, TextToSpeech.QUEUE_FLUSH, params, "theft_$rodId")
                }

                delay(2400)
            }
        }
    }

    /**
     * 2. DÜŞÜK PİL UYARISI:
     * "[Olta Adı/Numarası] pili kritik seviyede, yüzde [15]!"
     */
    fun speakBatteryWarning(rodId: Int, rodName: String = "Olta $rodId", batteryPercent: Int = 15) {
        if (!isTtsReady) return
        val sentence = "$rodName pili kritik seviyede, yüzde $batteryPercent. Lütfen şarj edin."
        val params = android.os.Bundle()
        params.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_NOTIFICATION)
        tts?.setSpeechRate(1.02f)
        tts?.setPitch(1.1f)
        tts?.speak(sentence, TextToSpeech.QUEUE_FLUSH, params, "battery_warn_$rodId")
        vibrateStaccato()
    }

    /**
     * 4. KRİTİK BAĞLANTI KOPTU (BLUETOOTH VEYA TELSİZ KOPUKLUĞU) SESLİ ALARMI:
     */
    fun triggerConnectionLostAlarm(title: String, speechText: String) {
        scope.launch {
            applyVolume()

            try {
                toneGenerator?.startTone(ToneGenerator.TONE_SUP_ERROR, 350)
                delay(400)
                toneGenerator?.startTone(ToneGenerator.TONE_SUP_ERROR, 350)
            } catch (e: Exception) {
                Log.w("AlarmSoundManager", "Disconnect tone error", e)
            }

            vibrateStaccato()

            if (isTtsReady) {
                tts?.setSpeechRate(1.1f)
                tts?.setPitch(1.2f)
                val params = android.os.Bundle()
                params.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_ALARM)
                tts?.speak(speechText, TextToSpeech.QUEUE_FLUSH, params, "conn_lost_${System.currentTimeMillis()}")
            }
        }
    }

    fun setVolume(volume: Float) {
        currentVolume = volume.coerceIn(0f, 1f)
        try {
            toneGenerator?.release()
            val volInt = (100 * currentVolume).toInt().coerceIn(0, 100)
            toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, volInt)
        } catch (e: Exception) {
            Log.w("AlarmSoundManager", "Re-init ToneGenerator error", e)
        }
        applyVolume()
    }

    private fun applyVolume() {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            audioManager?.let { am ->
                val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                val targetVol = (maxVol * currentVolume).toInt().coerceIn(0, maxVol)
                am.setStreamVolume(AudioManager.STREAM_ALARM, targetVol, 0)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                dragAudioTrack?.setVolume(currentVolume)
            }
        } catch (e: Exception) {
            Log.w("AlarmSoundManager", "applyVolume error: ${e.message}")
        }
    }

    private fun vibratePulse() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 250, 100, 250)
                val amplitudes = intArrayOf(0, 255, 0, 255)
                vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 250, 100, 250), -1)
            }
        } catch (_: Exception) {}
    }

    private fun vibrateStaccato() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 100, 80, 100, 80, 100)
                val amplitudes = intArrayOf(0, 255, 0, 255, 0, 255)
                vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 100, 80, 100, 80, 100), -1)
            }
        } catch (_: Exception) {}
    }

    /**
     * SUSTUR BUTONUNA BASILDIĞINDA:
     * - Kesintisiz akan kalama sesi anında kesilir
     * - Siren, konuşma ve titreşimler anında durur
     */
    fun stopAlarm() {
        repeatingJob?.cancel()
        repeatingJob = null
        stopContinuousMetallicDrag()
        stopCustomMediaPlayer()
        try {
            toneGenerator?.stopTone()
            tts?.stop()
            vibrator?.cancel()
        } catch (_: Exception) {}
    }

    fun release() {
        stopAlarm()
        try {
            toneGenerator?.release()
            tts?.shutdown()
        } catch (_: Exception) {}
    }
}
