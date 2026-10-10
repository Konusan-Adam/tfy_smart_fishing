package com.example.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.LuxuryNeonBlue
import com.example.ui.theme.LuxuryNeonPurple
import com.example.ui.theme.LuxuryNeonRed
import com.example.ui.theme.LuxuryNeonYellow

data class RgbLedColor(
    val color: Color,
    val name: String = ""
) {
    val redInt: Int get() = ((color.red * 255f).toInt()).coerceIn(0, 255)
    val greenInt: Int get() = ((color.green * 255f).toInt()).coerceIn(0, 255)
    val blueInt: Int get() = ((color.blue * 255f).toInt()).coerceIn(0, 255)

    val hexCode: String get() = String.format("#%02X%02X%02X", redInt, greenInt, blueInt)

    companion object {
        val PRESET_CYAN = RgbLedColor(LuxuryNeonBlue, "Neon Mavi")
        val PRESET_RED = RgbLedColor(LuxuryNeonRed, "Neon Kırmızı")
        val PRESET_PURPLE = RgbLedColor(LuxuryNeonPurple, "Neon Mor")
        val PRESET_YELLOW = RgbLedColor(LuxuryNeonYellow, "Neon Sarı")
        val PRESET_GREEN = RgbLedColor(Color(0xFF00FF87), "Neon Yeşil")
        val PRESET_ORANGE = RgbLedColor(Color(0xFFFF8A00), "Kehribar Turuncu")
        val PRESET_PINK = RgbLedColor(Color(0xFFFF2DF1), "Neon Pembe")
        val PRESET_WHITE = RgbLedColor(Color(0xFFFFFFFF), "Kristal Beyaz")

        val PRESETS = listOf(
            PRESET_CYAN,
            PRESET_RED,
            PRESET_PURPLE,
            PRESET_YELLOW,
            PRESET_GREEN,
            PRESET_ORANGE,
            PRESET_PINK,
            PRESET_WHITE
        )

        fun defaultForRod(id: Int): RgbLedColor {
            return PRESETS[(id - 1) % PRESETS.size]
        }

        fun fromHex(hex: String, defaultName: String = "Özel Renk"): RgbLedColor {
            return try {
                val cleanHex = hex.removePrefix("#")
                val longVal = cleanHex.toLong(16)
                val color = if (cleanHex.length == 6) {
                    Color(0xFF000000 or longVal)
                } else if (cleanHex.length == 8) {
                    Color(longVal)
                } else {
                    PRESET_CYAN.color
                }
                PRESETS.find { it.hexCode.equals("#" + cleanHex.takeLast(6).uppercase(), ignoreCase = true) }
                    ?: RgbLedColor(color, defaultName)
            } catch (_: Exception) {
                PRESET_CYAN
            }
        }
    }
}

enum class AlarmType {
    NORMAL,    // Düz Vuruş / Balık Asılması (Run)
    DROP_BACK, // Boşa Düşme / Misina Gevşemesi (Drop-back)
    THEFT      // Hırsızlık / Sehpadan Dikilme (Theft)
}

enum class SoundStyle(val title: String, val desc: String) {
    VOICE_AND_SIREN("Türkçe Asistan + Siren", "İsminiz, olta adı ve siren birlikte çalar"),
    REEL_DRAG_AND_VOICE("Mekanik Kalama + Sesli Asistan", "Sazan makarası cırcır cırlaması ve Türkçe anons"),
    VOICE_ONLY("Sadece Sesli Asistan", "Siren olmadan sadece Türkçe sesli anons"),
    CLASSIC_BEEP("Klasik Elektronik Bip", "Geleneksel sazan alarmı kesik bip tonu"),
    HIGH_PITCH_SIREN("Keskin Tiz Siren", "Uykudan uyandırıcı yüksek frekanslı alarm"),
    POLICE_SIREN("Deniz Feneri & Polisiye Siren", "Yüksek güçlü polis/fener acil durum ikazı")
}

data class SensorThresholdConfig(
    val shockAccelThresholdMg: Float = 250.0f,  // Şok Vuruş Eşiği (mG) (50.0 .. 1500.0)
    val dropBackAngleDeg: Float = 0.35f,       // Boşa Düşme Eşiği (Derece) (0.2 .. 3.0)
    val theftAngleDeg: Float = 25.0f,          // Hırsızlık Eşiği (Derece) (10.0 .. 60.0)
    val strikeAngleDeg: Float = 2.5f,          // Vuruş / Öne Eğilme Açısı (Derece) (1.0 .. 25.0)
    val sampleIntervalMs: Int = 20,            // Örnekleme Aralığı (ms) (10ms=100Hz, 20ms=50Hz, 40ms=25Hz)
    val pingIntervalSec: Int = 14400           // Canlı Kalp Atışı (sn) (14400 sn = 4 saat - balık vurana kadar derin uyku)
) {
    fun toCommandString(rodId: Int): String {
        return "ESIK:${rodId}:${String.format(java.util.Locale.US, "%.1f", shockAccelThresholdMg)}:${String.format(java.util.Locale.US, "%.1f", dropBackAngleDeg)}:${String.format(java.util.Locale.US, "%.1f", theftAngleDeg)}:${String.format(java.util.Locale.US, "%.1f", strikeAngleDeg)}:${sampleIntervalMs}:${pingIntervalSec}"
    }

    companion object {
        val DEFAULT = SensorThresholdConfig()

        // 🎯 Popüler Dinamik Eşik Hazır Butonları
        const val PRESET_750_MG = 750f  // Fırtınalı / Dalgalı / İri Balık Eşiği
        const val PRESET_500_MG = 500f  // Standart / Dengeli Sazan Eşiği
        const val PRESET_250_MG = 250f  // Yüksek Hassasiyet Eşiği
        const val PRESET_150_MG = 150f  // Ultra Hassas / İnce Takım Eşiği

        // 1..10 Hassasiyet Seviyesiyle Senkronize Eşik Hesaplama (1: 750 mG, 5: ~483 mG, 8: ~283 mG, 10: 150 mG)
        fun fromSensitivity(sensitivity: Int): SensorThresholdConfig {
            val clamped = sensitivity.coerceIn(1, 10)
            val shock = 750f - (clamped - 1) * (600f / 9f)
            val dropBack = 0.8f - (clamped - 1) * 0.055f // 1: 0.8°, 5: 0.58°, 8: 0.41°, 10: 0.30°
            val theft = 35.0f - (clamped - 1) * 1.5f
            val strike = 4.5f - (clamped - 1) * 0.3f // 1: 4.5° (sadece basan balık), 10: 1.8° (hassas)
            return SensorThresholdConfig(
                shockAccelThresholdMg = shock.coerceIn(100f, 1500f),
                dropBackAngleDeg = dropBack.coerceIn(0.2f, 3.0f),
                theftAngleDeg = theft.coerceIn(15f, 60f),
                strikeAngleDeg = strike.coerceIn(1.0f, 15f),
                sampleIntervalMs = if (clamped >= 8) 10 else 20,
                pingIntervalSec = 14400 // 4 saat
            )
        }

        // Doğrudan mG Değerinden Eşik Oluşturma
        fun fromShockMg(shockMg: Float): SensorThresholdConfig {
            val clamped = shockMg.coerceIn(50f, 1500f)
            val sens = toSensitivity(clamped)
            return fromSensitivity(sens).copy(shockAccelThresholdMg = clamped)
        }

        // Şok eşiğinden 1..10 hassasiyet seviyesini geri hesaplama
        fun toSensitivity(shockMg: Float): Int {
            val level = kotlin.math.round(1 + ((750f - shockMg) / (600f / 9f))).toInt()
            return level.coerceIn(1, 10)
        }
    }
}

data class FishingRod(
    val id: Int,
    val name: String = "Olta $id",
    val baitNote: String = "",
    val sensitivity: Int = 5,
    val assignedRodProfileName: String? = null,
    val thresholdConfig: SensorThresholdConfig = SensorThresholdConfig.DEFAULT,
    val ledColor: RgbLedColor = RgbLedColor.defaultForRod(id), // 🐟 Balık Vuruşu LED Rengi
    val dropBackLedColor: RgbLedColor = RgbLedColor.PRESET_WHITE, // 🔻 Boşa Düşme LED Rengi (Varsayılan: Beyaz)
    val theftLedColor: RgbLedColor = RgbLedColor.PRESET_RED, // 🚨 Hırsızlık / Dikilme LED Rengi (Varsayılan: Kırmızı)
    val batteryPercent: Int = 0,
    val isOnline: Boolean = false,
    val isSleeping: Boolean = false,
    val lastSeenTime: Long = 0L,
    val isAlarming: Boolean = false,
    val alarmType: AlarmType = AlarmType.NORMAL,
    val strikeIntensity: Int = 75, // 1..100 (Sensörden gelen vuruş şiddeti/çekiş hızı)
    val lastTriggeredTime: Long? = null,
    val isArmed: Boolean = true,
    val macAddress: String = "",
    val rssi: Int = -65,
    val isFriendRod: Boolean = false,
    val ownerName: String = "",
    val isWatchActive: Boolean = true
) {
    val currentAlarmColor: RgbLedColor
        get() = when (alarmType) {
            AlarmType.DROP_BACK -> dropBackLedColor
            AlarmType.THEFT -> theftLedColor
            AlarmType.NORMAL -> ledColor
        }

    val currentShockMg: Float
        get() = thresholdConfig.shockAccelThresholdMg

    val strikeIntensityLabel: String
        get() = when {
            alarmType == AlarmType.DROP_BACK -> "Boşa Düşme"
            alarmType == AlarmType.THEFT -> "Hırsızlık Alarmı"
            strikeIntensity <= 35 -> "Hafif Yoklama"
            strikeIntensity <= 70 -> "Orta Çekiş"
            else -> "Sert Asılma"
        }
}

enum class ConnectionMode {
    BLUETOOTH,
    WIFI_HOTSPOT
}

sealed class ConnectionStatus {
    object Disconnected : ConnectionStatus()
    data class Connecting(val target: String) : ConnectionStatus()
    data class Connected(val target: String) : ConnectionStatus()
}

data class PacketLog(
    val id: Long = System.currentTimeMillis() + (0..999).random(),
    val timeFormatted: String,
    val isOutbound: Boolean,
    val message: String
)
