/*
 * =========================================================================================
 * 🎣 1. PAKET: KAMIŞ VERİCİ SENSÖRÜ (ESP32-C3 SUPERMINI) - KESİNTİSİZ AKILLI GÜÇ & VURUŞ SENSÖRÜ (v11.0)
 * =========================================================================================
 * 🎯 45 SANİYE AKILLI KALP ATIŞI (PING) + 0 MS ANLIK BALIK VURUŞ REFLEKSİ
 *
 * DONANIM BAĞLANTILARI:
 *  - MPU-6050 VCC -> 3.3V
 *  - MPU-6050 GND -> GND
 *  - MPU-6050 SDA -> ESP32-C3 GPIO 4
 *  - MPU-6050 SCL -> ESP32-C3 GPIO 5
 *  - MPU-6050 INT -> ESP32-C3 GPIO 3  (Opsiyonel Donanımsal Kesme)
 *  - Pil Girişi   -> ESP32-C3 GPIO 0  (ADC0)
 *  - Dahili LED   -> ESP32-C3 GPIO 8  (Active LOW - Tamamen SÖNDÜRÜLDÜ)
 *
 * 🎯 VURUŞ VE FİZİK AYARLARI:
 *  - 50 Hz Dinamik Örnekleme (20 ms)
 *  - 75 mG Şok Vuruş Eşiği (Balık dokunma / asılma -> 0 ms ANINDA ALARM)
 *  - 2.5° Boşa Düşme (Drop-back / Gevşeme)
 *  - 18.0° Hırsızlık / Sehpadan Dikilme
 *  - 45 Saniyede Bir Canlı Kalp Atışı & Pil Raporu (Maksimum Pil Tasarrufu)
 *  - Wi-Fi Kanal 1 Kilidi (Bağlantı asla kopmaz)
 *
 * ℹ️ ÖNEMLİ MİMARİ NOTU (WIFI / HOTSPOT HAKKINDA):
 *  - Bu kamış verici kartı doğrudan telefonun Wi-Fi veya Hotspot ağına BAĞLANMAZ.
 *  - Çünkü kamışlar su kenarında açıktadır ve Wi-Fi modeme bağlanmak pili saatler içinde bitirir.
 *  - Bu kart ultra-düşük güç tüketen "ESP-NOW" telsiz protokolüyle çadırdaki ÇADIR ALICISI PAGER (ESP32)
 *    cihazına anlık sinyal fırlatır.
 *  - Telefona Bluetooth veya Wi-Fi / Hotspot bağlantısını ÇADIR ALICI KUTUSU (Pager) kurar!
 * =========================================================================================
 */

#include <Arduino.h>
#include <Wire.h>
#include <WiFi.h>
#include <esp_now.h>
#include <esp_wifi.h>

#define OLTA_ID                   1       // Bu vericinin Olta Numarası (1..10)
#define SDA_PIN                   4       // I2C SDA
#define SCL_PIN                   5       // I2C SCL
#define INT_PIN                   3       // MPU6050 INT Kesme Pini (Opsiyonel)
#define BATT_ADC_PIN              0       // Gerçek Pil Girişi (GPIO 0)
#define ONBOARD_LED_PIN           8       // ESP32-C3 Dahili LED (GPIO 8 - Active LOW)
#define MPU_ADDR                  0x68    // MPU-6050 I2C Adresi

// 🎯 DİNAMİK EŞİKLER VE FİLTRELER (Telefondan anlık güncellenir)
float dynamicShockThresholdMg  = 500.0f;  // Şok Vuruş Eşiği (mG) - Varsayılan: 500 mG (Dengeli)
float dynamicDropBackAngleDeg  = 1.5f;    // Boşa Düşme Eşiği (Derece) - Varsayılan: 1.5°
float dynamicTheftAngleDeg      = 18.0f;   // Hırsızlık Eşiği (Derece) - Varsayılan: 18.0°
int   dynamicSampleIntervalMs  = 20;      // 50 Hz Dinamik Örnekleme (20 ms)
unsigned long pingIntervalMs   = (4UL * 60UL * 60UL * 1000UL); // 4 Saat (14.400.000 ms) Varsayılan Derin Pil Tasarrufu

// Telsiz Paketi
typedef struct __attribute__((packed)) {
    uint8_t  rodId;           // Olta No (1..10)
    uint8_t  strikeType;      // 0: Ping, 1: Vuruş, 2: Boşa Düşme, 3: Hırsızlık, 4: Eşik Onayı (ACK)
    uint8_t  intensity;       // %0 - %100 Vuruş Şiddeti
    int16_t  pitchAngleTenths;// Açı x 10 (35.2° -> 352)
    uint8_t  batteryPercent;  // %0 - %100 Gerçek Pil
    uint16_t packetCounter;   // Paket No
} RodSensorPacket;

// Alıcıdan Gelen Kontrol ve Dinamik Eşik Paketi
typedef struct __attribute__((packed)) {
    uint8_t  targetRodId;
    uint8_t  commandCode;       // 0: Kapat / Sustur, 1: Aç / Nöbet, 2: Dinamik Eşik Paketi
    uint16_t shockMg;           // mG Cinsinden Şok Eşiği (Örn: 250, 500, 750)
    uint16_t dropBackTenthsDeg; // Derece x 10 (Örn: 1.5° -> 15)
    uint16_t theftTenthsDeg;    // Derece x 10 (Örn: 18.0° -> 180)
    uint8_t  sampleIntervalMs;  // 10 ms (100 Hz) veya 20 ms (50 Hz)
    uint16_t pingIntervalSec;   // Kalp Atışı Aralığı (Saniye) - Varsayılan: 14400 (4 saat)
} RodCommandPacket;

RodSensorPacket txPacket;
uint8_t broadcastMacAddress[] = {0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF};
esp_now_peer_info_t peerInfo;

uint16_t packetSeqNum = 0;
float baselinePitch = 0.0f;
float baselineAx = 0.0f, baselineAy = 0.0f, baselineAz = 0.0f;
bool baselineCalibrated = false;
bool theftTriggeredState = false;
float smoothedTiltDev = 0.0f;
bool isArmed = true;              // Telefondan AÇ / KAPAT durumu

float prevAccZ = 0.0f, currentPitch = 0.0f, smoothedPitch = 0.0f;
unsigned long lastSampleTime = 0, lastPingTime = 0, lastTriggerTime = 0;

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

// ======================= MPU-6050 VERİ OKUMA =======================
bool readMPU6050(float &ax, float &ay, float &az, float &pitch) {
    Wire.beginTransmission(MPU_ADDR);
    Wire.write(0x3B);
    if (Wire.endTransmission(false) != 0) return false;

    if (Wire.requestFrom((uint8_t)MPU_ADDR, (size_t)6, true) == 6) {
        int16_t raw_x = (Wire.read() << 8) | Wire.read();
        int16_t raw_y = (Wire.read() << 8) | Wire.read();
        int16_t raw_z = (Wire.read() << 8) | Wire.read();

        ax = (raw_x / 16384.0f) * 9.80665f;
        ay = (raw_y / 16384.0f) * 9.80665f;
        az = (raw_z / 16384.0f) * 9.80665f;

        pitch = atan2f(-ax, sqrtf(ay * ay + az * az)) * 57.2957795f;
        return true;
    }
    return false;
}

// ======================= TELSİZ PAKETİ FIRLATMA =======================
void sendPacket(uint8_t type, uint8_t intensity, float pitch) {
    packetSeqNum++;
    txPacket.rodId            = OLTA_ID;
    txPacket.strikeType       = type;
    txPacket.intensity        = intensity;
    txPacket.pitchAngleTenths = (int16_t)(pitch * 10.0f);
    txPacket.batteryPercent   = readRealBatteryPercent();
    txPacket.packetCounter    = packetSeqNum;

    esp_now_send(broadcastMacAddress, (uint8_t *)&txPacket, sizeof(txPacket));
    
    if (type == 1) Serial.printf("🐟 [VURUŞ] Şiddet: %%%d | Açı: %.1f° | Pil: %%%d\n", intensity, pitch, txPacket.batteryPercent);
    else if (type == 2) Serial.printf("🔻 [BOŞA DÜŞME] Açı: %.1f° | Pil: %%%d\n", pitch, txPacket.batteryPercent);
    else if (type == 3) Serial.printf("🚨 [HIRSIZLIK] Açı: %.1f° | Pil: %%%d\n", pitch, txPacket.batteryPercent);
    else if (type == 4) Serial.printf("⚡ [EŞİK ONAYI ACK] Şok: %.0f mG uygulandı | Pil: %%%d\n", dynamicShockThresholdMg, txPacket.batteryPercent);
    else Serial.printf("💓 [KALP ATIŞI PING] Olta #%d Hayatta | Pil: %%%d\n", OLTA_ID, txPacket.batteryPercent);
}

// ======================= TELSİZDEN EMİR ALMA (AÇ / KAPAT / DİNAMİK EŞİK) =======================
#if defined(ESP_ARDUINO_VERSION_MAJOR) && ESP_ARDUINO_VERSION_MAJOR >= 3
void onDataRecv(const esp_now_recv_info_t *recv_info, const uint8_t *incomingData, int len) {
#else
void onDataRecv(const uint8_t *mac, const uint8_t *incomingData, int len) {
#endif
    if (len >= 2) {
        RodCommandPacket *cmd = (RodCommandPacket *)incomingData;
        if (cmd->targetRodId == OLTA_ID || cmd->targetRodId == 0) {
            if (cmd->commandCode == 0) {
                // KAPAT / SUSTUR: Alarmları kes, ama telsizi açık tut
                Serial.println("🛑 Telefondan KAPAT emri alındı! Alarmlar susturuldu.");
                isArmed = false;
            } else if (cmd->commandCode == 1) {
                // AÇ / NÖBETE GEÇ
                Serial.println("🟢 Telefondan AÇ emri alındı! Nöbet aktifleşti.");
                isArmed = true;
                baselineCalibrated = false;
            } else if (cmd->commandCode == 2 && len >= 8) {
                // 🎯 DİNAMİK EŞİK PAKETİ: PARAMETRELER TELEFONDAN ANLIK GÜNCELLENİR
                if (cmd->shockMg >= 50 && cmd->shockMg <= 1500) {
                    dynamicShockThresholdMg = (float)cmd->shockMg;
                }
                if (cmd->dropBackTenthsDeg > 0 && cmd->dropBackTenthsDeg <= 200) {
                    dynamicDropBackAngleDeg = cmd->dropBackTenthsDeg / 10.0f;
                }
                if (cmd->theftTenthsDeg > 0 && cmd->theftTenthsDeg <= 600) {
                    dynamicTheftAngleDeg = cmd->theftTenthsDeg / 10.0f;
                }
                if (cmd->sampleIntervalMs >= 5 && cmd->sampleIntervalMs <= 100) {
                    dynamicSampleIntervalMs = cmd->sampleIntervalMs;
                }
                if (len >= (int)sizeof(RodCommandPacket) && cmd->pingIntervalSec >= 10) {
                    pingIntervalMs = ((unsigned long)cmd->pingIntervalSec) * 1000UL;
                }
                Serial.printf("⚡ [DİNAMİK EŞİK GÜNCELLENDİ] Şok: %.0f mG | Boşa: %.1f° | Hırsızlık: %.1f° | Hız: %d ms | Ping: %lu ms\n",
                              dynamicShockThresholdMg, dynamicDropBackAngleDeg, dynamicTheftAngleDeg, dynamicSampleIntervalMs, pingIntervalMs);
                
                // Telefona ve Alıcıya Eşik Kabul Edildi (ACK) Yanıtı Gönder
                sendPacket(4, (uint8_t)min((int)(dynamicShockThresholdMg / 10), 255), smoothedPitch);
            }
        }
    }
}

// ======================= KURULUM (SETUP) =======================
void setup() {
    setCpuFrequencyMhz(80); // 80 MHz düşük güç tasarrufu modu
    Serial.begin(115200);
    delay(200);

    // Dahili LED: SÖNDÜR (Active LOW: HIGH = KAPALI)
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

    // 21 Hz Donanımsal DLPF Filtresi (Rüzgar/dalga süzgeci)
    Wire.beginTransmission(MPU_ADDR);
    Wire.write(0x1A);
    Wire.write(0x04);
    Wire.endTransmission();

    // Wi-Fi & ESP-NOW Telsiz Başlat (Kanal 1'e sabitle)
    WiFi.mode(WIFI_STA);
    WiFi.disconnect();
    esp_wifi_set_channel(1, WIFI_SECOND_CHAN_NONE);
    esp_wifi_set_ps(WIFI_PS_NONE); // Kesintisiz anlık telsiz yanıtı
    esp_now_init();
    esp_now_register_recv_cb(onDataRecv);

    memcpy(peerInfo.peer_addr, broadcastMacAddress, 6);
    peerInfo.channel = 1;
    peerInfo.encrypt = false;
    esp_now_add_peer(&peerInfo);

    delay(100);
    float ax, ay, az;
    readMPU6050(ax, ay, az, currentPitch);
    baselineAx = ax;
    baselineAy = ay;
    baselineAz = az;
    baselinePitch = currentPitch;
    smoothedPitch = currentPitch;
    prevAccZ = az;
    baselineCalibrated = true;

    Serial.printf("\n🎣 KAMIŞ SENSÖRÜ #%d HAZIR! Referans Açı: %.1f°\n", OLTA_ID, baselinePitch);
    Serial.println("📡 50 Hz Dinamik Vuruş Algılama + 45 Saniye Kalp Atışı Başlatıldı.");

    // İlk açılış PING'i
    sendPacket(0, 0, baselinePitch);
}

// ======================= ANA DÖNGÜ (50 HZ VURUŞ + 45 SN PING) =======================
void loop() {
    unsigned long now = millis();

    // 🎯 DİNAMİK ÖRNEKLEME (10 ms veya 20 ms)
    if (now - lastSampleTime >= dynamicSampleIntervalMs) {
        lastSampleTime = now;

        float ax, ay, az;
        if (readMPU6050(ax, ay, az, currentPitch)) {
            smoothedPitch = (smoothedPitch * 0.85f) + (currentPitch * 0.15f);
            float deltaAccZ = fabsf(az - prevAccZ) * 101.9716f;
            prevAccZ = az;

            if (!baselineCalibrated) {
                baselineAx = ax;
                baselineAy = ay;
                baselineAz = az;
                baselinePitch = smoothedPitch;
                baselineCalibrated = true;
            }

            // Universal 3D Tilt / Theft Angle Deviation (Vector Dot Product - Orientation Independent)
            float dot = (baselineAx * ax) + (baselineAy * ay) + (baselineAz * az);
            float mag1 = sqrtf(baselineAx * baselineAx + baselineAy * baselineAy + baselineAz * baselineAz);
            float mag2 = sqrtf(ax * ax + ay * ay + az * az);
            float cosAngle = dot / (mag1 * mag2 + 0.00001f);
            if (cosAngle > 1.0f) cosAngle = 1.0f;
            if (cosAngle < -1.0f) cosAngle = -1.0f;
            float totalTiltDevDeg = acosf(cosAngle) * 57.2957795f;
            
            smoothedTiltDev = (smoothedTiltDev * 0.8f) + (totalTiltDevDeg * 0.2f);

            float angleDev = smoothedPitch - baselinePitch;

            // Sadece AÇIK (isArmed) iken alarm fırlat
            if (isArmed) {
                // 1. GERÇEK DİNAMİK VURUŞ ALARMI (Telefondan gelen dynamicShockThresholdMg eşiği)
                if (deltaAccZ >= dynamicShockThresholdMg && (now - lastTriggerTime > 400)) {
                    lastTriggerTime = now;
                    uint8_t intensity = (uint8_t)constrain(map((long)deltaAccZ, (long)dynamicShockThresholdMg, (long)(dynamicShockThresholdMg * 3.5f), 40, 100), 30, 100);
                    sendPacket(1, intensity, smoothedPitch);
                    lastPingTime = now; // Ping sayacını sıfırla
                }
                // 2. BOŞA DÜŞME (DROP-BACK / GEVŞEME - Dinamik Açı Eşiği)
                else if (angleDev <= -dynamicDropBackAngleDeg && (now - lastTriggerTime > 600)) {
                    lastTriggerTime = now;
                    sendPacket(2, 85, smoothedPitch);
                    baselinePitch = (baselinePitch * 0.60f) + (smoothedPitch * 0.40f);
                    lastPingTime = now;
                }
                
                // 3. HIRSIZLIK / SEHPADAN DİKİLME (Dinamik Açı Eşiği)
                if (smoothedTiltDev >= dynamicTheftAngleDeg) {
                    if (!theftTriggeredState) {
                        theftTriggeredState = true;
                        sendPacket(3, 100, smoothedPitch);
                        lastPingTime = now;
                    }
                } else {
                    if (smoothedTiltDev < (dynamicTheftAngleDeg * 0.4f)) {
                        theftTriggeredState = false; // Kamış yerine konunca sıfırlanır
                    }
                }

                // 4. RÜZGAR VE DALGA ADAPTASYONU
                if (deltaAccZ < 25.0f && smoothedTiltDev < (dynamicTheftAngleDeg * 0.3f)) {
                    baselineAx = (baselineAx * 0.99f) + (ax * 0.01f);
                    baselineAy = (baselineAy * 0.99f) + (ay * 0.01f);
                    baselineAz = (baselineAz * 0.99f) + (az * 0.01f);
                    baselinePitch = (baselinePitch * 0.99f) + (smoothedPitch * 0.01f);
                }
            }
        }
    }

    // 📡 Kalp Atışı (Ping) - Balık vurana kadar derin sessizlik & süper pil tasarrufu (Varsayılan 4 Saat)
    if (now - lastPingTime >= pingIntervalMs) {
        lastPingTime = now;
        sendPacket(0, 0, smoothedPitch);
    }

    delay(2);
}
