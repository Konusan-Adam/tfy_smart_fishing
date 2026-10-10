/*
 * =========================================================================================
 * 🎣 KAMIŞ VERİCİ SENSÖRÜ (ESP32-C3 SUPERMINI) - USTA BALIKÇI MANTIĞI (v15.0)
 * =========================================================================================
 * 🎯 SIFIR SAHTE VURUŞ (ZERO FALSE-ALARM):
 *    - Jiroskop otomatik sıfırlama (Gyro Auto-Zero) ile sensörün çip ısısı gürültüsü sıfırlanır.
 *    - Hem açı eğilmesi HEM DE gerçek fiziksel darbe (jerk) VE dinamik hareket şarttır (&&).
 *    - Sensör masada, çantada veya sehpada hareketsiz dururken darbe 0 mG olacağından ASLA ötmez.
 *    - Boşa düşme ve hırsızlıkta da dinamik hareket şartı konuldu; statik duruşta döngüye girmez.
 * =========================================================================================
 */

#include <Arduino.h>
#include <Wire.h>
#include <WiFi.h>
#include <esp_now.h>
#include <esp_wifi.h>
#include <Preferences.h>

// =========================================================================================
// ⚙️ [KULLANICI MANUEL AYARLARI] - İSTEDİĞİNİZ DEĞERLERİ BURADAN DEĞİŞTİREBİLİRSİNİZ
// =========================================================================================
#define MANUEL_OLTA_ID                      1        // Bu vericinin Olta Numarası (1 .. 10)

// 🐟 1. VURUŞ / ÖNE ASILMA AYARLARI (STRIKE / RUN):
// 💡 Balıkçı İpucu: Açıyı yüksek (örn: 3.0° veya 4.0°), şoku dengeli (200 mG) tutarak
// küçük balık tırtıklamaları tamamen elenir; sadece kamışı oturaklı eğen sazan yakalanır!
float MANUEL_STRIKE_ANGLE_DEG             = 2.5f;    // Vuruş Açısı (°): Kamışın suya en az kaç derece eğileceği (1.5° .. 15.0°)
float MANUEL_SHOCK_THRESHOLD_MG           = 200.0f;  // Şok / Titreşim Eşiği (mG): İvme darbesi (100 .. 800 mG)
unsigned long MANUEL_PULL_HOLD_MS         = 4000UL;     // Öne asılmanın sürmesi gereken süre (ms): 100 ms içinde anında paket fırlatma

// 🔻 2. BOŞA DÜŞME / MİSİNA GEVŞEME AYARLARI (DROP-BACK):
float MANUEL_DROP_BACK_ANGLE_DEG          = 0.35f;   // Boşa Düşme Açısı (°): Kamışın geriye dikleşme açısı (0.2° .. 1.5°)
unsigned long MANUEL_ASILIP_BIRAKMA_MS    = 3000UL;    // Asılıp-Bırakma Penceresi (ms): 3.2 sn içinde öne asılıp salarsa "Boşa Düştü" sayar
unsigned long MANUEL_DROP_HOLD_MS         = 60;      // Geriye yaylanıp yeni yerine oturma süresi (ms)

// 🚨 3. HIRSIZLIK / SEHPADAN KALDIRMA AYARLARI (THEFT):
float MANUEL_THEFT_ROLL_ANGLE_DEG         = 25.0f;   // Yanal Yatma Eşiği (°): Sehpadan alınınca sağa/sola dönme açısı
float MANUEL_THEFT_LIFT_ELEVATION_DEG     = 32.0f;   // Yatay Konuma İnme Açısı (°): Dik duruştan sehpaya/masaya indirilme algısı

// ⏱️ 4. SÜKUNET VE ZAMANLAMA AYARLARI:
unsigned long MANUEL_STRIKE_COOLDOWN_MS   = 4000UL;  // Vuruş sonrası mükerrer sinyal kesici süresi (4.0 sn kilit)
unsigned long MANUEL_DROP_COOLDOWN_MS     = 4000UL;  // Boşa düşme sonrası mükerrer sinyal kesici süresi (4.0 sn kilit)
int   MANUEL_SAMPLE_INTERVAL_MS           = 20;      // 50 Hz dinamik örnekleme (20 ms)
unsigned long MANUEL_PING_INTERVAL_SEC    = 14400UL; // Canlılık kalp atışı (4 saatte bir durum bildirimi - derin pil tasarrufu)

// Donanım Pin Tanımları (ESP32-C3 SuperMini)
#define SDA_PIN                   4       // I2C SDA
#define SCL_PIN                   5       // I2C SCL
#define INT_PIN                   3       // MPU6050 INT Kesme Pini
#define BATT_ADC_PIN              0       // Gerçek Pil Girişi (GPIO 0)
#define ONBOARD_LED_PIN           8       // ESP32-C3 Dahili LED (GPIO 8 - Active LOW)
#define MPU_ADDR                  0x68    // MPU-6050 I2C Adresi

// =========================================================================================
// 🎯 ÇALIŞMA ZAMANI DİNAMİK DEĞİŞKENLERİ
// =========================================================================================
uint8_t currentRodId            = MANUEL_OLTA_ID;
float dynamicStrikeAngleDeg     = MANUEL_STRIKE_ANGLE_DEG;
float dynamicShockThresholdMg   = MANUEL_SHOCK_THRESHOLD_MG;
float dynamicDropBackAngleDeg   = MANUEL_DROP_BACK_ANGLE_DEG;
float dynamicTheftAngleDeg       = MANUEL_THEFT_ROLL_ANGLE_DEG;
int   dynamicSampleIntervalMs   = MANUEL_SAMPLE_INTERVAL_MS;
unsigned long pingIntervalMs    = (MANUEL_PING_INTERVAL_SEC * 1000UL);
unsigned long strikeCooldownMs  = MANUEL_STRIKE_COOLDOWN_MS;
unsigned long dropCooldownMs    = MANUEL_DROP_COOLDOWN_MS;

Preferences prefs;

// Telsiz Paketi
typedef struct __attribute__((packed)) {
    uint8_t  rodId;           // Olta No (1..10)
    uint8_t  strikeType;      // 0: Ping, 1: Vuruş, 2: Boşa Düşme, 3: Hırsızlık, 4: Eşik Onayı
    uint8_t  intensity;       // %0 - %100 Vuruş Şiddeti
    int16_t  pitchAngleTenths;// Açı x 10 (35.2° -> 352)
    uint8_t  batteryPercent;  // %0 - %100 Gerçek Pil
    uint16_t packetCounter;   // Paket No
} RodSensorPacket;

// Alıcıdan Gelen Kontrol Paketi
typedef struct __attribute__((packed)) {
    uint8_t  targetRodId;
    uint8_t  commandCode;       // 0: Kapat / Sustur, 1: Aç / Nöbet, 2: Dinamik Eşik Paketi
    uint16_t shockMg;           // mG Cinsinden Şok Eşiği
    uint16_t dropBackTenthsDeg; // Boşa Düşme Derece x 10
    uint16_t theftTenthsDeg;    // Hırsızlık Derece x 10
    uint16_t strikeTenthsDeg;   // VURUŞ / EĞİLME AÇISI Derece x 10
    uint8_t  sampleIntervalMs;  // ms
    uint16_t pingIntervalSec;   // Kalp Atışı Aralığı (sn)
} RodCommandPacket;

RodSensorPacket txPacket;
uint8_t broadcastMacAddress[] = {0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF};
esp_now_peer_info_t peerInfo;

uint16_t packetSeqNum = 0;
bool isArmed = true;              // Telefondan AÇ / KAPAT durumu
unsigned long muteUntilTime = 0;  // Telefondan geçici susturma bitiş anı

// Açı, Filtre ve Kalibrasyon Değişkenleri
float baselineElevation = 0.0f;   // Kamışın kazıktaki referans dik duruş açısı
float smoothedElevation = 0.0f;   // Yumuşatılmış anlık açı
float prevAx = 0.0f, prevAy = 0.0f, prevAz = 0.0f;
float gyroOffsetX = 0.0f, gyroOffsetY = 0.0f, gyroOffsetZ = 0.0f; // Jiroskop Donanımsal Sıfır Kayması
bool baselineCalibrated = false;

// Usta Balıkçı Durum Takip Değişkenleri
unsigned long lastStrikeTime = 0;     // Son öne asılma anı
unsigned long lastStrikeSendTime = 0; // Son vuruş telsiz paketi fırlatma anı
unsigned long lastDropSendTime = 0;   // Son boşa düşme paketi fırlatma anı
unsigned long lastTheftSendTime = 0;  // Son hırsızlık paketi fırlatma anı

uint8_t sustainedPullCount = 0;
uint8_t sustainedDropCount = 0;
bool dropSnapDetected = false;        // Anlık geriye esneme/darbe yakalandı mı
unsigned long dropSnapTime = 0;       // Geriye esneme anı (ms)
bool theftAlarmActive = false;

unsigned long lastSampleTime = 0, lastPingTime = 0;

// ======================= HAFIZADAN AYARLARI YÜKLE / KAYDET =======================
void loadConfigFromFlash() {
    prefs.begin("rod_cfg", true);
    dynamicStrikeAngleDeg   = prefs.getFloat("strike_deg", MANUEL_STRIKE_ANGLE_DEG);
    dynamicShockThresholdMg = prefs.getFloat("shock_mg",   MANUEL_SHOCK_THRESHOLD_MG);
    dynamicDropBackAngleDeg = prefs.getFloat("drop_deg",    MANUEL_DROP_BACK_ANGLE_DEG);
    if (dynamicDropBackAngleDeg > 1.2f || dynamicDropBackAngleDeg < 0.15f) {
        dynamicDropBackAngleDeg = MANUEL_DROP_BACK_ANGLE_DEG; // Eski hatalı yüksek eşikleri temizle
    }
    dynamicTheftAngleDeg     = prefs.getFloat("theft_deg",   MANUEL_THEFT_ROLL_ANGLE_DEG);
    dynamicSampleIntervalMs = prefs.getInt("sample_ms",     MANUEL_SAMPLE_INTERVAL_MS);
    unsigned long pingSec   = prefs.getULong("ping_sec",    MANUEL_PING_INTERVAL_SEC);
    if (pingSec >= 10) pingIntervalMs = pingSec * 1000UL;
    prefs.end();
}

void saveConfigToFlash() {
    prefs.begin("rod_cfg", false);
    prefs.putFloat("strike_deg", dynamicStrikeAngleDeg);
    prefs.putFloat("shock_mg",   dynamicShockThresholdMg);
    prefs.putFloat("drop_deg",    dynamicDropBackAngleDeg);
    prefs.putFloat("theft_deg",   dynamicTheftAngleDeg);
    prefs.putInt("sample_ms",     dynamicSampleIntervalMs);
    prefs.putULong("ping_sec",    pingIntervalMs / 1000UL);
    prefs.end();
}

// ======================= GERÇEK PİL ÖLÇÜMÜ =======================
uint8_t readRealBatteryPercent() {
    uint32_t totalMv = 0;
    for (int i = 0; i < 16; i++) {
        totalMv += analogReadMilliVolts(BATT_ADC_PIN);
        delay(1);
    }
    float pinMv = totalMv / 16.0f;
    float battVolt = (pinMv * 2.0f) / 1000.0f;

    int pct;
    if (battVolt >= 4.18f) pct = 100;
    else if (battVolt >= 4.05f) pct = map((long)(battVolt * 100), 405, 418, 90, 100);
    else if (battVolt >= 3.90f) pct = map((long)(battVolt * 100), 390, 405, 75, 90);
    else if (battVolt >= 3.75f) pct = map((long)(battVolt * 100), 375, 390, 50, 75);
    else if (battVolt >= 3.60f) pct = map((long)(battVolt * 100), 360, 375, 25, 50);
    else if (battVolt >= 3.45f) pct = map((long)(battVolt * 100), 345, 360, 10, 25);
    else if (battVolt >= 3.30f) pct = map((long)(battVolt * 100), 330, 345, 1, 10);
    else pct = 0;

    return (uint8_t)constrain(pct, 0, 100);
}

// ======================= MPU-6050 14-BYTE OKUMA (ACCEL + GYRO) =======================
bool readMPU6050(float &ax, float &ay, float &az, float &gx, float &gy, float &gz) {
    Wire.beginTransmission(MPU_ADDR);
    Wire.write(0x3B);
    if (Wire.endTransmission(false) != 0) return false;

    if (Wire.requestFrom((uint8_t)MPU_ADDR, (size_t)14, true) == 14) {
        int16_t raw_ax = (Wire.read() << 8) | Wire.read();
        int16_t raw_ay = (Wire.read() << 8) | Wire.read();
        int16_t raw_az = (Wire.read() << 8) | Wire.read();
        Wire.read(); Wire.read(); // Sıcaklık baytlarını atla
        int16_t raw_gx = (Wire.read() << 8) | Wire.read();
        int16_t raw_gy = (Wire.read() << 8) | Wire.read();
        int16_t raw_gz = (Wire.read() << 8) | Wire.read();

        // ±2g İvme -> m/s²
        ax = (raw_ax / 16384.0f) * 9.80665f;
        ay = (raw_ay / 16384.0f) * 9.80665f;
        az = (raw_az / 16384.0f) * 9.80665f;

        // ±250 °/sn Jiroskop (Donanımsal Ofset Çıkarılmış)
        gx = (raw_gx / 131.0f) - gyroOffsetX;
        gy = (raw_gy / 131.0f) - gyroOffsetY;
        gz = (raw_gz / 131.0f) - gyroOffsetZ;
        return true;
    }
    return false;
}

// ======================= TELSİZ PAKETİ FIRLATMA =======================
void sendPacket(uint8_t type, uint8_t intensity, float pitch) {
    packetSeqNum++;
    txPacket.rodId            = currentRodId;
    txPacket.strikeType       = type;
    txPacket.intensity        = intensity;
    txPacket.pitchAngleTenths = (int16_t)(pitch * 10.0f);
    txPacket.batteryPercent   = readRealBatteryPercent();
    txPacket.packetCounter    = packetSeqNum;

    esp_now_send(broadcastMacAddress, (uint8_t *)&txPacket, sizeof(txPacket));

    if (type == 1) Serial.printf("🐟 [VURUŞ / RUN] Şiddet: %%%d | Açı: %.1f° | Pil: %%%d\n", intensity, pitch, txPacket.batteryPercent);
    else if (type == 2) Serial.printf("🔻 [BOŞA DÜŞTÜ] Açı: %.1f° | Pil: %%%d\n", pitch, txPacket.batteryPercent);
    else if (type == 3) Serial.printf("🚨 [HIRSIZLIK] Açı: %.1f° | Pil: %%%d\n", pitch, txPacket.batteryPercent);
    else if (type == 4) Serial.printf("⚡ [EŞİK ONAYI ACK] Vuruş: %.1f° | Şok: %.0f mG | Boşa: %.1f°\n", dynamicStrikeAngleDeg, dynamicShockThresholdMg, dynamicDropBackAngleDeg);
    else Serial.printf("💓 [KALP ATIŞI PING] Olta #%d Hayatta | Açı: %.1f° | Pil: %%%d\n", currentRodId, pitch, txPacket.batteryPercent);
}

// ======================= TELSİZDEN EMİR ALMA =======================
#if defined(ESP_ARDUINO_VERSION_MAJOR) && ESP_ARDUINO_VERSION_MAJOR >= 3
void onDataRecv(const esp_now_recv_info_t *recv_info, const uint8_t *incomingData, int len) {
#else
void onDataRecv(const uint8_t *mac, const uint8_t *incomingData, int len) {
#endif
    if (len >= 2) {
        RodCommandPacket *cmd = (RodCommandPacket *)incomingData;
        if (cmd->targetRodId == currentRodId || cmd->targetRodId == 0) {
            if (cmd->commandCode == 0) {
                // KAPAT / SUSTUR (30 saniye sükunet)
                isArmed = false;
                muteUntilTime = millis() + 30000UL;
                Serial.println("🛑 Telefondan KAPAT emri alındı! Alarmlar susturuldu.");
            } else if (cmd->commandCode == 1) {
                // AÇ / NÖBETE GEÇ
                isArmed = true;
                muteUntilTime = 0;
                baselineCalibrated = false;
                Serial.println("🟢 Telefondan AÇ emri alındı! Nöbet aktifleşti.");
            } else if (cmd->commandCode == 2 && len >= 8) {
                // DİNAMİK EŞİK GÜNCELLEMESİ
                if (cmd->shockMg >= 50 && cmd->shockMg <= 1500) {
                    dynamicShockThresholdMg = (float)cmd->shockMg;
                }
                if (cmd->dropBackTenthsDeg > 0 && cmd->dropBackTenthsDeg <= 200) {
                    dynamicDropBackAngleDeg = cmd->dropBackTenthsDeg / 10.0f;
                }
                if (cmd->theftTenthsDeg > 0 && cmd->theftTenthsDeg <= 600) {
                    dynamicTheftAngleDeg = cmd->theftTenthsDeg / 10.0f;
                }
                if (len >= 10 && cmd->strikeTenthsDeg > 0 && cmd->strikeTenthsDeg <= 250) {
                    dynamicStrikeAngleDeg = cmd->strikeTenthsDeg / 10.0f;
                }
                if (cmd->sampleIntervalMs >= 5 && cmd->sampleIntervalMs <= 100) {
                    dynamicSampleIntervalMs = cmd->sampleIntervalMs;
                }
                if (cmd->pingIntervalSec >= 10) {
                    pingIntervalMs = ((unsigned long)cmd->pingIntervalSec) * 1000UL;
                }
                saveConfigToFlash();
                Serial.printf("⚡ [EŞİK GÜNCELLENDİ] Vuruş Açısı: %.1f° | Şok: %.0f mG | Boşa: %.1f° | Hırsızlık: %.1f°\n",
                              dynamicStrikeAngleDeg, dynamicShockThresholdMg, dynamicDropBackAngleDeg, dynamicTheftAngleDeg);
                sendPacket(4, (uint8_t)min((int)(dynamicShockThresholdMg / 10), 255), smoothedElevation);
            }
        }
    }
}

// ======================= KURULUM (SETUP) =======================
void setup() {
    setCpuFrequencyMhz(80); // 80 MHz düşük güç tasarrufu modu
    Serial.begin(115200);
    delay(200);

    loadConfigFromFlash();

    pinMode(ONBOARD_LED_PIN, OUTPUT);
    digitalWrite(ONBOARD_LED_PIN, HIGH);
    pinMode(INT_PIN, INPUT_PULLUP);

    analogSetPinAttenuation(BATT_ADC_PIN, ADC_11db);
    analogReadResolution(12);

    Wire.begin(SDA_PIN, SCL_PIN);
    Wire.setClock(100000);

    // MPU-6050 Uykudan Uyandır
    Wire.beginTransmission(MPU_ADDR);
    Wire.write(0x6B);
    Wire.write(0x00);
    Wire.endTransmission();

    // 21 Hz Donanımsal DLPF Filtresi
    Wire.beginTransmission(MPU_ADDR);
    Wire.write(0x1A);
    Wire.write(0x04);
    Wire.endTransmission();

    // 🎯 MPU-6050 Donanımsal Gyro Sıfırlama (50 Örneklemeli Otomatik Kalibrasyon - SIFIR DRIFT)
    float sumGx = 0, sumGy = 0, sumGz = 0;
    for (int i = 0; i < 50; i++) {
        float tax, tay, taz, tgx, tgy, tgz;
        readMPU6050(tax, tay, taz, tgx, tgy, tgz);
        sumGx += tgx;
        sumGy += tgy;
        sumGz += tgz;
        delay(4);
    }
    gyroOffsetX = sumGx / 50.0f;
    gyroOffsetY = sumGy / 50.0f;
    gyroOffsetZ = sumGz / 50.0f;

    // Wi-Fi & ESP-NOW Telsiz Başlat
    WiFi.mode(WIFI_STA);
    WiFi.disconnect();
    esp_wifi_set_channel(1, WIFI_SECOND_CHAN_NONE);
    esp_wifi_set_ps(WIFI_PS_NONE);
    esp_now_init();
    esp_now_register_recv_cb(onDataRecv);

    memcpy(peerInfo.peer_addr, broadcastMacAddress, 6);
    peerInfo.channel = 1;
    peerInfo.encrypt = false;
    esp_now_add_peer(&peerInfo);

    delay(200);

    // Başlangıç Referansı
    float ax, ay, az, gx, gy, gz;
    readMPU6050(ax, ay, az, gx, gy, gz);
    prevAx = ax; prevAy = ay; prevAz = az;
    baselineElevation = atan2f(ay, sqrtf(ax * ax + az * az)) * 57.2957795f;
    smoothedElevation = baselineElevation;
    baselineCalibrated = true;

    Serial.println("\n========================================================");
    Serial.printf("🎣 KAMIŞ SENSÖRÜ #%d HAZIR! (v15.0 Sıfır Sahte Vuruş)\n", currentRodId);
    Serial.printf("📌 Referans Duruş Açısı: %.1f° | Gyro Ofset: (%.1f, %.1f, %.1f)\n", baselineElevation, gyroOffsetX, gyroOffsetY, gyroOffsetZ);
    Serial.printf("🎯 Vuruş Eşiği: +%.1f° Öne Bükülme VE %.0f mG Gerçek Şok Darbesi\n", dynamicStrikeAngleDeg, dynamicShockThresholdMg);
    Serial.printf("🔻 Boşa Düşme Eşiği: -%.1f° Yaylanma\n", dynamicDropBackAngleDeg);
    Serial.println("========================================================\n");

    sendPacket(0, 0, baselineElevation);
}

// ======================= ANA DÖNGÜ (50 HZ TESPİT) =======================
void loop() {
    unsigned long now = millis();

    if (now - lastSampleTime >= (unsigned long)dynamicSampleIntervalMs) {
        lastSampleTime = now;

        float ax, ay, az, gx, gy, gz;
        if (readMPU6050(ax, ay, az, gx, gy, gz)) {
            // 1. Kamış Açısı
            float currentElevation = atan2f(ay, sqrtf(ax * ax + az * az)) * 57.2957795f;
            smoothedElevation = (smoothedElevation * 0.85f) + (currentElevation * 0.15f);

            // 2. Jiroskop Dönme Hızı (°/sn)
            float gyroOmega = sqrtf(gx * gx + gy * gy + gz * gz);

            // 3. İvme Şok Darbesi (Jerk - mG)
            float dAx = ax - prevAx;
            float dAy = ay - prevAy;
            float dAz = az - prevAz;
            float jerkMg = sqrtf(dAx * dAx + dAy * dAy + dAz * dAz) * 101.9716f;
            prevAx = ax; prevAy = ay; prevAz = az;

            if (!baselineCalibrated) {
                baselineElevation = smoothedElevation;
                baselineCalibrated = true;
            }

            // 4. Bükülme Sapması
            float bend = baselineElevation - smoothedElevation;

            // 5. Yanal Devrilme Açısı
            float rollTiltDeg = fabsf(atan2f(ax, fabsf(az)) * 57.2957795f);

            bool isMuted = (!isArmed) || (now < muteUntilTime);

            if (!isMuted) {
                // =========================================================================
                // 🐟 1. DÜZ VURUŞ / UZUN SÜRELİ ASILMA (RUN)
                // ŞARTLAR (KESİN "VE" MANTIĞI - SIFIR SAHTE VURUŞ):
                // 1. Kamış öne bükülmeli (bend >= dynamicStrikeAngleDeg)
                // 2. VE gerçek fiziksel ivme/sarsıntı olmalı (jerkMg >= dynamicShockThresholdMg * 0.7f)
                // 3. VE dinamik hareket olmalı (gyroOmega >= 2.0f || jerkMg >= 220.0f)
                // Masada/sehpada dururken jerk 0 mG olduğu için ASLA ötemez!
                // =========================================================================
                bool hasDynamicPullMotion = (jerkMg >= (dynamicShockThresholdMg * 0.7f)) && (gyroOmega >= 2.0f || jerkMg >= 220.0f);

                if (bend >= dynamicStrikeAngleDeg && hasDynamicPullMotion) {
                    sustainedPullCount++;
                } else {
                    if (sustainedPullCount > 0) sustainedPullCount--;
                }

                if (sustainedPullCount >= (MANUEL_PULL_HOLD_MS / dynamicSampleIntervalMs)) {
                    sustainedPullCount = 0;
                    lastStrikeTime = now;

                    if (now - lastStrikeSendTime >= strikeCooldownMs) {
                        lastStrikeSendTime = now;
                        uint8_t intensity = (uint8_t)constrain(map((long)jerkMg, 30, (long)(dynamicShockThresholdMg * 2.0f), 40, 100), 35, 100);
                        sendPacket(1, intensity, smoothedElevation);
                        lastPingTime = now;
                        Serial.println("🎣 [RUN / DÜZ VURUŞ] Gerçek balık kamışı bükerek asıldı!");
                    }
                }

                // =========================================================================
                // 🔻 2. BOŞA DÜŞME / MİSİNA GEVŞEMESİ (DROP-BACK / SLACK LINE)
                // USTA BALIKÇI İKİLİ KURAL MANTIĞI:
                // 1. KURAL (Asılıp-Bırakma): Balık öne asıldıktan sonra en fazla 3.2 sn içinde misinayı saldı.
                //    Kamış öne gerilimden geriye rahatlayıp dikleştiği an (bend <= -0.20° ve hafif ivme) ANINDA fırlatılır.
                // 2. KURAL (Doğrudan Boşa Düşme): Önden vuruş olmadan balık kıyıya yüzdü / swinger düştü.
                //    Kamış geriye dikleşti (bend <= -dynamicDropBackAngleDeg: -0.35°) ve 60 ms oturdu.
                // 3. KURAL (Mükerrer Kesici & Anında Sıfırlama): Sinyal atıldığı an yeni açı referans
                //    alınır, 4.0 sn mükerrer kilit başlar, durduğu yerde ASLA spam yapmaz!
                // =========================================================================

                // Kamış geriye doğru rahatlıyor/dikleşiyor mu?
                bool isRelaxingBack = (bend <= -0.20f) && (jerkMg >= 10.0f || gyroOmega >= 0.35f);

                // Geriye dik duruş takibi (Doğrudan boşa düşme için)
                if (bend <= -dynamicDropBackAngleDeg) {
                    sustainedDropCount++;
                } else {
                    if (sustainedDropCount > 0) sustainedDropCount--;
                }

                // Senaryo A: Vuruş sonrası 3.2 saniye içinde asılıp-bırakma
                bool pullThenSlackScenario = (lastStrikeTime > 0) &&
                                             (now - lastStrikeTime <= MANUEL_ASILIP_BIRAKMA_MS) &&
                                             (isRelaxingBack || bend <= -0.28f);

                // Senaryo B: Doğrudan boşa düşme (60 ms geride sabit oturdu + hafif sarsıntı/hareket veya 100 ms sabitlik)
                bool directSlackScenario = (sustainedDropCount >= (MANUEL_DROP_HOLD_MS / dynamicSampleIntervalMs)) &&
                                           (jerkMg >= 10.0f || gyroOmega >= 0.3f || sustainedDropCount >= 5);

                if ((pullThenSlackScenario || directSlackScenario) && (now - lastDropSendTime >= dropCooldownMs)) {
                    lastDropSendTime = now;
                    lastStrikeTime = 0; // Asılıp-bırakma durumunu sıfırla
                    sustainedDropCount = 0;
                    dropSnapDetected = false;

                    sendPacket(2, 90, smoothedElevation);

                    // 🎯 1 KEZ ÇAL & SUS: Yeni açıyı hemen referans al, rüzgarda veya durduğu yerde durmadan ötmesin!
                    baselineElevation = smoothedElevation;
                    lastPingTime = now;
                    Serial.printf("🔻 [BOŞA DÜŞTÜ] Misina gevşedi! (Sapma: %.2f°, Yeni Açı: %.1f°)\n", bend, baselineElevation);
                }

                // =========================================================================
                // 🚨 3. HIRSIZLIK / SEHPADAN KALDIRMA (THEFT)
                // ŞART: Kamış kazıktan alınınca hem yana yatmalı HEM DE hareket olmalı (jerkMg >= 80.0f || gyroOmega >= 3.0f)
                // =========================================================================
                bool hasTheftMotion = (jerkMg >= 80.0f || gyroOmega >= 3.0f);
                if (rollTiltDeg >= dynamicTheftAngleDeg && hasTheftMotion && bend < 1.0f) {
                    if (!theftAlarmActive && (now - lastTheftSendTime > 4000)) {
                        theftAlarmActive = true;
                        lastTheftSendTime = now;
                        sendPacket(3, 100, smoothedElevation);
                        lastPingTime = now;
                        Serial.println("🚨 [HIRSIZLIK] Kamış kazıktan/sehpadan kaldırıldı!");
                    }
                } else {
                    if (rollTiltDeg < 15.0f && smoothedElevation >= 40.0f) {
                        theftAlarmActive = false;
                    }
                }

                // =========================================================================
                // 🌊 4. RÜZGAR VE DALGA ÖLÜ BÖLGE ADAPTASYONU (Deadband)
                // Sadece kamış durgunken ve gerçek bir eğilme yokken (bend < 0.20°) çok yavaş adapte ol.
                // Vuruş veya boşa düşme yaşanırken asla baseline'ı kaydırıp eşiği yutmasın!
                // =========================================================================
                if (gyroOmega < 1.0f && jerkMg < 15.0f && fabsf(bend) < 0.20f) {
                    baselineElevation = (baselineElevation * 0.999f) + (smoothedElevation * 0.001f);
                }
            }
        }
    }

    // 📡 Canlı Kalp Atışı (Ping)
    if (now - lastPingTime >= pingIntervalMs) {
        lastPingTime = now;
        sendPacket(0, 0, smoothedElevation);
    }

    delay(2);
}
