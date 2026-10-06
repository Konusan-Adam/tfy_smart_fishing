package com.example.engine

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

enum class RodCategory(val title: String, val icon: String) {
    ALL("Tümü", "🌐"),
    POLE_LAKE("Göl & Şamandıra (5-9m)", "🎋"),
    CARP("Sazan Kamışları", "🎣"),
    SURF("Surf & Kıyı Deniz", "🌊"),
    CATFISH("Ağır Yayın", "🦈"),
    FEEDER("Feeder & Method", "🌾"),
    CUSTOM("Özel Kayıtlarım", "⭐")
}

enum class RodMaterial(
    val title: String,
    val youngsModulusGPa: Double, // GPa (Young Modülü - Esneklik/Sertlik Direnci)
    val dampingRatio: Double,    // Sönümleme katsayısı
    val description: String
) {
    HIGH_MODULUS_CARBON(
        title = "Yüksek Modüllü Karbon (30T-40T / Graphene)",
        youngsModulusGPa = 145.0,
        dampingRatio = 0.035,
        description = "Çok hızlı toparlanma, yüksek rezonans frekansı, en ufak dokunuşları anında iletir"
    ),
    STANDARD_CARBON_COMPOSITE(
        title = "Karbon Kompozit / Karışım (24T)",
        youngsModulusGPa = 85.0,
        dampingRatio = 0.055,
        description = "Dengeli esneklik, orta frekanslı vuruş iletimi"
    ),
    FIBERGLASS(
        title = "Fiberglas / Dolgu Fiber",
        youngsModulusGPa = 38.0,
        dampingRatio = 0.095,
        description = "Yumuşak ve esnek yapı, düşük frekanslı geniş salınım genliği"
    )
}

enum class RodAction(
    val title: String,
    val actionFactor: Double, // Aksiyon katsayısı (Kiriş bükülme noktası oranı)
    val naturalFreqFactor: Double, // Doğal frekans katsayısı (Hz çarpanı)
    val description: String
) {
    FAST_ACTION(
        title = "Sert / Fast (Uç Aksiyonu)",
        actionFactor = 1.40,
        naturalFreqFactor = 12.5,
        description = "Kamışın sadece uç %30'luk kısmı bükülür. Şok vuruşlar yüksek frekansta iletilir."
    ),
    MEDIUM_FAST_ACTION(
        title = "Orta Sert / Medium-Fast",
        actionFactor = 1.15,
        naturalFreqFactor = 9.0,
        description = "Kamışın üst %45'lik kısmı bükülür. Hem uzağa atış hem hassas vuruş algılama dengesi."
    ),
    MEDIUM_PARABOLIC(
        title = "Orta / Medium Parabolik",
        actionFactor = 0.95,
        naturalFreqFactor = 6.2,
        description = "Kamışın üst %60'ı bükülür. Düzgün yay hareketi ve orta frekanslı salınım."
    ),
    SLOW_FULL_PARABOLIC(
        title = "Yumuşak / Slow (Tam Parabolik)",
        actionFactor = 0.70,
        naturalFreqFactor = 3.8,
        description = "Tüm gövde yay gibi eğilir. Büyük balık kafa vuruşlarında geniş açısal sapma üretir."
    )
}

enum class WaterEnvironment(
    val title: String,
    val noiseFactor: Double, // Rüzgar ve dalga gürültü katsayısı
    val minCutoffHz: Double
) {
    CALM_LAKE(
        title = "Durgun Göl / Baraj",
        noiseFactor = 1.0,
        minCutoffHz = 1.2
    ),
    RIVER_FLOW(
        title = "Akıntılı Nehir / Çay",
        noiseFactor = 1.35,
        minCutoffHz = 2.0
    ),
    WAVY_SEA(
        title = "Dalgalı Deniz / Kıyı Surf",
        noiseFactor = 1.80,
        minCutoffHz = 3.2
    )
}

data class RodPhysicsInput(
    val rodName: String = "Sazan Kamışı",
    val lengthMeters: Double = 3.60,       // L (Kamış Tam Boyu: 1.80 - 10.00 m)
    val sensorDistanceMeters: Double = 0.60, // x (Makine/Sap noktasından sensör mesafesi: 0.15 - 3.50 m)
    val testCurveLbs: Double = 3.50,       // TC (Test Curve: 1.0 - 7.0 lbs veya gramaj karşılığı)
    val material: RodMaterial = RodMaterial.HIGH_MODULUS_CARBON,
    val action: RodAction = RodAction.FAST_ACTION,
    val environment: WaterEnvironment = WaterEnvironment.CALM_LAKE,
    val category: RodCategory = RodCategory.CARP
)

data class CalculatedSensorCalibration(
    val input: RodPhysicsInput,
    val accelThresholdMilliG: Int,        // İvmeölçer eşik değeri (10..300 mG)
    val gyroThresholdDegSec: Double,       // Açısal hız eşik değeri (°/sn)
    val pitchAngleDropThresholdDeg: Double,// Boşa düşme açısal eşiği (0.5°..8.0°)
    val filterCutoffHz: Double,            // Sayısal Bant-Geçiren Filtre Cutoff (Hz)
    val resonanceFrequencyHz: Double,      // Kamışın teorik doğal rezonans frekansı (Hz)
    val normalizedSensitivityLevel: Int,   // Balıkçıya gösterilen 1..10 arası nihai hassasiyet
    val esp32CommandPayload: String,       // Sensör donanımına gönderilen seri paket
    val rfEfficiencyPercent: Int = 98,     // RF Sinyal Verim Yüzdesi (%30 - %100)
    val rfEfficiencyDescription: String = "Maksimum Menzil (250m+)"
) {
    val formulaSummary: String
        get() = buildString {
            append("• Moment Kolu Oranı (x/L): %${(input.sensorDistanceMeters / input.lengthMeters * 100).roundToInt()}\n")
            append("• Kiriş Rijitliği (EI): ${(input.material.youngsModulusGPa * 0.45 * input.testCurveLbs).roundToInt()} N·m²\n")
            append("• Doğal Rezonans Frekansı (fn): ${String.format("%.1f", resonanceFrequencyHz)} Hz\n")
            append("• İvme Eşik Değeri (Th_acc): $accelThresholdMilliG mG\n")
            append("• Boşa Düşme Açısı (Th_pitch): ${String.format("%.1f", pitchAngleDropThresholdDeg)}°\n")
            append("• Sayısal Gürültü Filtresi: ${String.format("%.1f", filterCutoffHz)} Hz\n")
            append("• RF Sinyal Verimi: %$rfEfficiencyPercent ($rfEfficiencyDescription)")
        }
}

object RodPhysicsEngine {

    /**
     * EULER-BERNOULLI KİRİŞ & TİTREŞİM MEKANİĞİ HESAPLAYICISI (1.80m - 10.00m)
     */
    fun calculate(input: RodPhysicsInput, rodId: Int): CalculatedSensorCalibration {
        val L = input.lengthMeters.coerceIn(1.80, 10.00)
        val x = input.sensorDistanceMeters.coerceIn(0.15, (L * 0.85).coerceAtLeast(0.30))
        val tc = input.testCurveLbs.coerceIn(1.0, 7.0)

        // 1. Kamışın Sertlik / Rijitlik Katsayısı (Rigidity Index - EI)
        val materialE = input.material.youngsModulusGPa // 38..145 GPa
        val actionFactor = input.action.actionFactor     // 0.70..1.40
        val envNoise = input.environment.noiseFactor     // 1.0..1.8

        // Efektif bükülme rijitliği (N.m^2)
        val beamStiffness = (materialE / 100.0) * tc * (actionFactor.pow(1.5))

        // 2. Sensörün Konum Katsayısı (Moment Kolu Transfer Fonksiyonu)
        // 7-8m göl kamışlarında x/L oranı küçülür ama uzun kol ivmeyi iletir
        val relativePosition = x / L // 0.02..0.85
        val positionMultiplier = (1.0 - (1.0 - relativePosition).pow(2.2)).coerceIn(0.08, 1.0)

        // 3. Doğal Rezonans Frekansı
        // Uzun kamışlarda (7-8m) rezonans frekansı düşer, kısa ve sert kamışlarda yükselir
        val rawResonanceFreq = (input.action.naturalFreqFactor * (sqrt(materialE / 80.0) / (L / 3.60).pow(1.25)))
            .coerceIn(1.5, 22.0)

        // 4. İvme Eşik Değeri (milli-G) Hesabı
        val baseAccelMilliG = (45.0 * positionMultiplier * (beamStiffness / 3.5) * envNoise)
        val clampedAccelMilliG = baseAccelMilliG.roundToInt().coerceIn(10, 290)

        // 5. Açısal Hız (Gyro) ve Boşa Düşme (Drop-Back) Eşiği (°/s ve Derece)
        val pitchDropDeg = (3.8 * (1.0 / (positionMultiplier.coerceAtLeast(0.15))) * (tc / 3.5) * (1.0 / actionFactor) * (3.60 / L).pow(0.5))
            .coerceIn(0.6, 8.5)

        val gyroThresholdDegSec = (pitchDropDeg * rawResonanceFreq * 0.45).coerceIn(1.5, 25.0)

        // 6. Sayısal Bant Geçiren Filtre Frekansı (Cutoff Hz)
        val filterCutoffHz = max(input.environment.minCutoffHz, rawResonanceFreq * 0.65)

        // 7. Balıkçı İçin Normalize Edilmiş 1..10 Arası Hassasiyet Skalası
        val normalizedSensLevel = when {
            clampedAccelMilliG <= 25 -> 10
            clampedAccelMilliG <= 40 -> 9
            clampedAccelMilliG <= 60 -> 8
            clampedAccelMilliG <= 85 -> 7
            clampedAccelMilliG <= 115 -> 6
            clampedAccelMilliG <= 150 -> 5
            clampedAccelMilliG <= 190 -> 4
            clampedAccelMilliG <= 230 -> 3
            clampedAccelMilliG <= 260 -> 2
            else -> 1
        }

        // 8. RF Sinyal İletim & Yer Yansıması Verimi (Fresnel Zone & Yerden Yükseklik)
        val rfEfficiencyPercent = when {
            x >= 2.20 -> 98
            x >= 1.80 -> 90
            x >= 1.40 -> 78
            x >= 1.00 -> 65
            x >= 0.60 -> 50
            else -> 35
        }

        val rfEfficiencyDesc = when {
            x >= 2.20 -> "Maksimum Menzil (250m+) • Zemin Kaybı Sıfır • Altın Konum"
            x >= 1.80 -> "Yüksek Menzil (180-220m) • Çok İyi Görüş Hattı"
            x >= 1.20 -> "Orta Menzil (100-140m) • Normal İletim"
            else -> "Kısa Menzil (30-60m) • Zemin/Su Emilimi Yüksek (2.20m Önerilir)"
        }

        // 9. Sensör Donanımı Kalibrasyon Komut Paketi:
        val pitchTenths = (pitchDropDeg * 10).roundToInt()
        val filterTenths = (filterCutoffHz * 10).roundToInt()
        val commandPayload = "KALIBRE:$rodId:$clampedAccelMilliG:$pitchTenths:$filterTenths:$normalizedSensLevel"

        return CalculatedSensorCalibration(
            input = input,
            accelThresholdMilliG = clampedAccelMilliG,
            gyroThresholdDegSec = (gyroThresholdDegSec * 10.0).roundToInt() / 10.0,
            pitchAngleDropThresholdDeg = (pitchDropDeg * 10.0).roundToInt() / 10.0,
            filterCutoffHz = (filterCutoffHz * 10.0).roundToInt() / 10.0,
            resonanceFrequencyHz = (rawResonanceFreq * 10.0).roundToInt() / 10.0,
            normalizedSensitivityLevel = normalizedSensLevel,
            esp32CommandPayload = commandPayload,
            rfEfficiencyPercent = rfEfficiencyPercent,
            rfEfficiencyDescription = rfEfficiencyDesc
        )
    }
}
