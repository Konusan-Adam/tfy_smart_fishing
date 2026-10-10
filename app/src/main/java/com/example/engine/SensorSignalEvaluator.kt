package com.example.engine

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 3-EKSEN DİJİTAL SİNYAL VE YÜKSEK HASSASİYETLİ VURUŞ AYRIŞTIRMA MOTORU (DSP)
 *
 * MPU6050 / Gyro / İvmeölçer verilerini (Pitch, Roll, Jerk) işler.
 * Balıkçının 2 defa yanlış alarm almasını engellemek için:
 * 1. Gerilim Kalibrasyonu (Misina gerilince 0° referansı alma)
 * 2. Öne Asılma (Balık Vurdu): +Pitch & Dikey Jerk & Yanal Sapmasız
 * 3. Boşa Düşme (Gevşeme): -Pitch & Yanal Sapmasız
 * 4. Hırsızlık (Kaldırılma): Aşırı +Pitch veya Yanal +Roll
 * 5. Dalga/Rüzgar Sönümleme: Deadband (+-1.5°) & Bant-Geçiren Filtre
 */
enum class SensorEvaluationResult {
    NOISE_FILTERED,  // Rüzgar ve dalga hareketi (Sönümlendi, alarm yok)
    STRIKE_FORWARD,  // Balık oltayı öne asıldı (Düz Vuruş)
    DROP_BACK,       // Balık kıyıya yüzdü / misina gevşedi (Boşa Düşme)
    THEFT_REMOVAL    // Kamış sehpadan kaldırıldı / çalınıyor (Hırsızlık)
}

data class EvaluatedStrike(
    val result: SensorEvaluationResult,
    val rodId: Int,
    val deltaPitchDeg: Double,
    val deltaRollDeg: Double,
    val jerkMilliG: Int,
    val calculatedIntensity: Int, // 10..100
    val debugMessage: String
)

data class RodBaselineState(
    var isCalibrated: Boolean = false,
    var baselinePitchDeg: Double = 0.0,
    var baselineRollDeg: Double = 0.0,
    var lastRawPitchDeg: Double = 0.0,
    var lastRawRollDeg: Double = 0.0,
    var lastJerkMilliG: Int = 0,
    var lastEvaluationTime: Long = System.currentTimeMillis()
)

object SensorSignalEvaluator {

    private val rodBaselines = HashMap<Int, RodBaselineState>()

    /**
     * Olta kazığa dikilip misina 2-3 cm gerildiğinde basılan "GERİLİMİ SIFIRLA" işlemi
     */
    fun resetPreTensionBaseline(rodId: Int, currentPitchDeg: Double, currentRollDeg: Double): String {
        val state = rodBaselines.getOrPut(rodId) { RodBaselineState() }
        state.baselinePitchDeg = currentPitchDeg
        state.baselineRollDeg = currentRollDeg
        state.isCalibrated = true
        state.lastRawPitchDeg = currentPitchDeg
        state.lastRawRollDeg = currentRollDeg
        return "✓ Olta #$rodId gerilim referansı sıfırlandı: Pitch=${String.format("%.1f", currentPitchDeg)}°, Roll=${String.format("%.1f", currentRollDeg)}°"
    }

    /**
     * Ham Telekomünikasyon / IMU Paketini İşleme Engine
     */
    fun processTelemetry(
        rodId: Int,
        rawPitchDeg: Double,
        rawRollDeg: Double,
        jerkMilliG: Int,
        gyroDegSec: Double = 0.0,
        config: com.example.model.SensorThresholdConfig = com.example.model.SensorThresholdConfig.DEFAULT
    ): EvaluatedStrike {
        val state = rodBaselines.getOrPut(rodId) { RodBaselineState() }

        // Eğer henüz kalibre edilmediyse ilk gelen açıyı referans kabul et
        if (!state.isCalibrated) {
            state.baselinePitchDeg = rawPitchDeg
            state.baselineRollDeg = rawRollDeg
            state.isCalibrated = true
        }

        val deltaPitch = rawPitchDeg - state.baselinePitchDeg
        val deltaRoll = rawRollDeg - state.baselineRollDeg

        state.lastRawPitchDeg = rawPitchDeg
        state.lastRawRollDeg = rawRollDeg
        state.lastJerkMilliG = jerkMilliG
        state.lastEvaluationTime = System.currentTimeMillis()

        val absDeltaPitch = abs(deltaPitch)
        val absDeltaRoll = abs(deltaRoll)

        val theftThreshold = config.theftAngleDeg.toDouble()
        val strikeThreshold = config.strikeAngleDeg.toDouble()
        val dropBackThreshold = config.dropBackAngleDeg.toDouble()
        val shockThreshold = config.shockAccelThresholdMg.toInt()

        // --- 1. HIRSIZLIK / SEHPADAN SÖKÜLME DİKEY VEYA YANAL KALDIRILMA ---
        // Aşırı dikilme VEYA yana devrilme (Roll > theftThreshold)
        if (deltaPitch > theftThreshold || absDeltaRoll > theftThreshold || (deltaPitch > 15.0 && jerkMilliG > 850)) {
            return EvaluatedStrike(
                result = SensorEvaluationResult.THEFT_REMOVAL,
                rodId = rodId,
                deltaPitchDeg = deltaPitch,
                deltaRollDeg = deltaRoll,
                jerkMilliG = jerkMilliG,
                calculatedIntensity = 100,
                debugMessage = "🚨 HIRSIZLIK: Açı Değişimi Aşırı (Pitch: ${String.format("%.1f", deltaPitch)}°, Roll: ${String.format("%.1f", deltaRoll)}°)"
            )
        }

        // --- 2. TERSİNE VURUŞ / GEVŞEME (BOŞA DÜŞTÜ / DROP-BACK) ---
        // Sazan kıyıya doğru yüzdüğünde kamış dikleşir (deltaPitch negatif).
        // Rüzgar filtresine takılmaması için doğrudan değerlendirilir.
        if (deltaPitch <= -dropBackThreshold && absDeltaRoll < 12.0) {
            return EvaluatedStrike(
                result = SensorEvaluationResult.DROP_BACK,
                rodId = rodId,
                deltaPitchDeg = deltaPitch,
                deltaRollDeg = deltaRoll,
                jerkMilliG = jerkMilliG,
                calculatedIntensity = 80,
                debugMessage = "🔵 BOŞA DÜŞTÜ: Misina Gevşedi (ΔPitch: ${String.format("%.1f", deltaPitch)}°)"
            )
        }

        // --- 3. DALGA & RÜZGAR SÖNÜMLEME (DEADBAND) ---
        // Küçük açılı ve düşük ivmeli salınımlar rüzgardır, ALARM VERME!
        if (absDeltaPitch <= 1.0 && absDeltaRoll <= 2.5 && jerkMilliG < 150) {
            return EvaluatedStrike(
                result = SensorEvaluationResult.NOISE_FILTERED,
                rodId = rodId,
                deltaPitchDeg = deltaPitch,
                deltaRollDeg = deltaRoll,
                jerkMilliG = jerkMilliG,
                calculatedIntensity = 0,
                debugMessage = "🌊 SÖNÜMLENDİ: Rüzgar/Dalga Salınımı (Deadband İçinde)"
            )
        }

        // --- 4. DÜZ VURUŞ / ÖNE ASILMA (BALIK VURDU) ---
        // Pitch >= strikeThreshold VE Yanal Roll < 12.0° VE Dikey Jerk >= (shockThreshold * 0.70)
        if (deltaPitch >= strikeThreshold && absDeltaRoll < 12.0 && jerkMilliG >= (shockThreshold * 0.70).toInt()) {
            // Vuruş Şiddeti Hesabı (%10 - %100 arası ölçekleme)
            val pitchWeight = (deltaPitch / 10.0) * 50.0
            val jerkWeight = (jerkMilliG / 800.0) * 50.0
            val intensity = (pitchWeight + jerkWeight).roundToInt().coerceIn(15, 100)

            return EvaluatedStrike(
                result = SensorEvaluationResult.STRIKE_FORWARD,
                rodId = rodId,
                deltaPitchDeg = deltaPitch,
                deltaRollDeg = deltaRoll,
                jerkMilliG = jerkMilliG,
                calculatedIntensity = intensity,
                debugMessage = "🔴 BALIK VURDU: Öne Asılma %$intensity (ΔPitch: +${String.format("%.1f", deltaPitch)}°, Jerk: ${jerkMilliG}mG)"
            )
        }

        // Varsayılan gürültü filtresi
        return EvaluatedStrike(
            result = SensorEvaluationResult.NOISE_FILTERED,
            rodId = rodId,
            deltaPitchDeg = deltaPitch,
            deltaRollDeg = deltaRoll,
            jerkMilliG = jerkMilliG,
            calculatedIntensity = 0,
            debugMessage = "ℹ️ Sinyal Eşik Altı (Filtrelendi)"
        )
    }

    /**
     * Olta baselines bilgisini getir
     */
    fun getBaselineState(rodId: Int): RodBaselineState? = rodBaselines[rodId]
}
