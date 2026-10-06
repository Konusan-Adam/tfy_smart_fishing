/*
 * =========================================================================================
 * 📟 2. PAKET: ÇADIR İÇİ SES VE IŞIK ÇALARI (ESP32 ALICI PAGER & WI-FI SUNUCU) - v9.0
 * =========================================================================================
 * 🎯 ULTRA-STABİL WI-FI SOFTAP (KANAL 1) + SIFIR SESLİ PARAZİT + TELSİZ SES KALINTILARI TEMİZLENDİ
 *
 * GERÇEK DEVRE BAĞLANTILARI:
 *  - Buzzer Modülü      -> GPIO 25
 *  - Harici LED         -> GPIO 4
 *  - ESP32 Dahili LED   -> GPIO 2  (Mavi Dahili Çip LED'i)
 *  - Type-C / 5V Güç
 *
 * YAPILAN KRİTİK GÜNCELLEMELER:
 *  1. Kanal 1 Uyumu: Wi-Fi SoftAP ve ESP-NOW kanalı Kanal 1'e sabitlendi (Kamışlarla %100 senkron).
 *  2. Rahatsız Edici Ötüş İptal: Sinyal koptuğunda çadır kutusundaki buzzer'ın anlamsız ötmesi kapatıldı.
 *  3. Telsiz Ses Kalıntıları Silindi: Eski telsiz ses aktarımı tamponları ve röleleri kaldırılarak RAM ve Wi-Fi rahatlatıldı.
 *  4. Anında Wi-Fi El Sıkışması: TCP NoDelay ile telefon bağlandığı an anında ALIVE yanıtı verilir.
 * =========================================================================================
 */

#include <Arduino.h>
#include <WiFi.h>
#include <esp_now.h>
#include <esp_wifi.h>
#include <BluetoothSerial.h>

#define BUZZER_PIN           25      // Buzzer Pini
#define LED_PIN              4       // Harici LED Pini (GPIO 4)
#define ONBOARD_LED_PIN      2       // ESP32 Kart Üstü Dahili Mavi LED (GPIO 2)

#define BT_DEVICE_NAME       "T_F_Y_SMART_FISHING"
#define WATCHDOG_TIMEOUT_MS  (5UL * 60UL * 60UL * 1000UL) // 5 Saat - Kamışın 4 saatlik uyku moduna tam uyumlu

// 🌐 1. YÖNTEM: Wi-Fi Erişim Noktası (AP) ve TCP Sunucu Ayarları
#define WIFI_AP_SSID         "T_F_Y_SMART_FISHING"
#define WIFI_AP_PASS         "1234567890"    // WPA2 Şifresi (10 Karakter - Android el sıkışma uyumlu)
#define WIFI_TCP_PORT        8080
#define MAX_WIFI_CLIENTS     4

// 📱 2. YÖNTEM: Telefon Hotspot (Kişisel Erişim Noktası) İstemci Ayarları (İsteğe Bağlı)
#define PHONE_HOTSPOT_SSID        ""              // Telefon Hotspot Adınız
#define PHONE_HOTSPOT_PASS        ""              // Telefon Hotspot Şifreniz
#define PHONE_HOTSPOT_SERVER_IP   "192.168.43.1"  // Android telefon varsayılan Hotspot IP'si
#define PHONE_HOTSPOT_SERVER_PORT 8080            // Uygulamadaki Hotspot dinleyici portu

WiFiServer wifiServer(WIFI_TCP_PORT);
WiFiClient wifiClients[MAX_WIFI_CLIENTS];
String wifiBuffers[MAX_WIFI_CLIENTS];

WiFiClient phoneHotspotClient;
String phoneHotspotBuffer = "";
unsigned long lastHotspotConnectAttempt = 0;

// Gelen Sensör Paketi (Kamıştan gelen)
typedef struct __attribute__((packed)) {
    uint8_t  rodId;
    uint8_t  strikeType;      // 0: Ping, 1: Vuruş, 2: Boşa Düşme, 3: Hırsızlık, 4: Eşik Onayı (ACK)
    uint8_t  intensity;
    int16_t  pitchAngleTenths;
    uint8_t  batteryPercent;
    uint16_t packetCounter;
} RodSensorPacket;

// Kamışa Gönderilecek Emir ve Dinamik Eşik Paketi (ESP32-C3 ile %100 Birebir Eşleşir)
typedef struct __attribute__((packed)) {
    uint8_t  targetRodId;
    uint8_t  commandCode;       // 0: Kapat / Sustur, 1: Aç / Nöbet, 2: Dinamik Eşik Paketi
    uint16_t shockMg;           // mG Cinsinden Şok Eşiği (Örn: 250, 500, 750)
    uint16_t dropBackTenthsDeg; // Derece x 10 (Örn: 1.5° -> 15)
    uint16_t theftTenthsDeg;    // Derece x 10 (Örn: 18.0° -> 180)
    uint8_t  sampleIntervalMs;  // 10 ms (100 Hz) veya 20 ms (50 Hz)
    uint16_t pingIntervalSec;   // Kalp Atışı Aralığı (Saniye) - Varsayılan: 14400 (4 saat)
} RodCommandPacket;

RodSensorPacket rxPacket;
RodCommandPacket txCmd;
BluetoothSerial SerialBT;
uint8_t broadcastMac[] = {0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF};
esp_now_peer_info_t peerInfo;

unsigned long lastRodPacketTime[11] = {0};
bool isRodOnline[11] = {false};
unsigned long lastKeepAliveTime = 0;
unsigned long lastWatchdogCheckTime = 0;
volatile bool shouldMuteNow = false;
String btBuffer = "";

// ======================= TÜM SES VE LED'LERİ SÖNDÜR =======================
void blackoutAllAlerts() {
    shouldMuteNow = true;
    digitalWrite(BUZZER_PIN, LOW);
    digitalWrite(LED_PIN, LOW);
    digitalWrite(ONBOARD_LED_PIN, LOW);
    Serial.println("🔇 [BLACKOUT] Tüm uyarılar söndürüldü.");
}

// 🎯 BALIK VURUNCA 5 DEFA ÇAK VE ÖT
void triggerFishStrike5Times() {
    shouldMuteNow = false;
    Serial.println("🚨 [ALARM BAŞLADI] Balık Vurdu!");

    for (int i = 0; i < 5; i++) {
        if (shouldMuteNow) {
            blackoutAllAlerts();
            return;
        }

        digitalWrite(BUZZER_PIN, HIGH);
        digitalWrite(LED_PIN, HIGH);
        digitalWrite(ONBOARD_LED_PIN, HIGH);
        delay(130);

        digitalWrite(BUZZER_PIN, LOW);
        digitalWrite(LED_PIN, LOW);
        digitalWrite(ONBOARD_LED_PIN, LOW);
        delay(100);
    }
}

// Boşa düşme uyarısı
void triggerDropBackAlert() {
    shouldMuteNow = false;
    Serial.println("🔻 [BOŞA DÜŞTÜ!]");

    for (int i = 0; i < 3; i++) {
        if (shouldMuteNow) {
            blackoutAllAlerts();
            return;
        }

        digitalWrite(BUZZER_PIN, HIGH);
        digitalWrite(LED_PIN, HIGH);
        digitalWrite(ONBOARD_LED_PIN, HIGH);
        delay(200);

        digitalWrite(BUZZER_PIN, LOW);
        digitalWrite(LED_PIN, LOW);
        digitalWrite(ONBOARD_LED_PIN, LOW);
        delay(120);
    }
}

// ⚠️ KAMIŞ SİNYALİ KESİLDİ BİLGİSİ (Buzzer ötüşü kapatıldı)
void triggerDisconnectAlertBeep() {
    // SESSİZE ALINDI: Kullanıcıyı rahatsız eden gereksiz buzzer ötüşü tamamen kapatıldı.
    Serial.println("ℹ️ [BİLGİ] Kamış sinyali kesildi (Sesli ikaz sessize alındı).");
}

// 🚨 HIRSIZLIK / SEHPADAN ALINMA ACİL DURUM PANİK SİRENİ
void triggerTheftPanicSiren() {
    shouldMuteNow = false;
    Serial.println("🚨🚨 [ACİL HIRSIZLIK] OLTALAR ÇALINIYOR! KESİNTİSİZ PANİK SİRENİ!");

    for (int i = 0; i < 20; i++) {
        if (shouldMuteNow) {
            blackoutAllAlerts();
            return;
        }

        digitalWrite(BUZZER_PIN, HIGH);
        digitalWrite(LED_PIN, HIGH);
        digitalWrite(ONBOARD_LED_PIN, HIGH);
        delay(70);

        digitalWrite(BUZZER_PIN, LOW);
        digitalWrite(LED_PIN, LOW);
        digitalWrite(ONBOARD_LED_PIN, LOW);
        delay(50);
    }
}

// Telefona Bluetooth ve Wi-Fi ile Veri Gönderme (Dual-Mode)
void sendToAndroid(const char* message) {
    // 1. Bluetooth Üzerinden Gönder
    if (SerialBT.hasClient()) {
        SerialBT.print(message);
    }

    // 2. Wi-Fi AP'ye Bağlı TCP İstemcilerine Gönder
    for (int i = 0; i < MAX_WIFI_CLIENTS; i++) {
        if (wifiClients[i] && wifiClients[i].connected()) {
            wifiClients[i].print(message);
        }
    }

    // 3. Telefon Hotspot Soket Sunucusuna Gönder
    if (phoneHotspotClient && phoneHotspotClient.connected()) {
        phoneHotspotClient.print(message);
    }

    Serial.print("[TX] "); 
    Serial.print(message);
}

// Kamış Vericisine Güç / Durum Emri Fırlat
void forwardCommandToSensor(uint8_t rodId, uint8_t commandCode) {
    txCmd.targetRodId = rodId;
    txCmd.commandCode = commandCode;
    txCmd.shockMg = 500;
    txCmd.dropBackTenthsDeg = 15;
    txCmd.theftTenthsDeg = 180;
    txCmd.sampleIntervalMs = 20;
    txCmd.pingIntervalSec = 14400; // 4 saat
    esp_now_send(broadcastMac, (uint8_t *)&txCmd, sizeof(txCmd));
    Serial.printf("📡 [EMİR] Olta #%d -> Kod: %d fırlatıldı.\n", rodId, commandCode);
}

// ⚡ Kamış Vericisine Dinamik Eşik ve Filtre Parametrelerini Fırlat
void forwardThresholdToSensor(uint8_t rodId, uint16_t shockMg, uint16_t dropBackTenths, uint16_t theftTenths, uint8_t sampleMs, uint16_t pingSec) {
    txCmd.targetRodId = rodId;
    txCmd.commandCode = 2; // 2: Dinamik Eşik Paketi
    txCmd.shockMg = shockMg;
    txCmd.dropBackTenthsDeg = dropBackTenths;
    txCmd.theftTenthsDeg = theftTenths;
    txCmd.sampleIntervalMs = sampleMs;
    txCmd.pingIntervalSec = (pingSec > 0) ? pingSec : 14400;

    esp_now_send(broadcastMac, (uint8_t *)&txCmd, sizeof(txCmd));
    Serial.printf("⚡ [DİNAMİK EŞİK TX] Olta #%d -> Şok: %d mG | Boşa: %.1f° | Hırsızlık: %.1f° | Hız: %d ms | Ping: %d sn\n",
                  rodId, shockMg, dropBackTenths / 10.0f, theftTenths / 10.0f, sampleMs, txCmd.pingIntervalSec);

    // Telefona anında kabul edildi bilgisi ver
    char buf[32];
    snprintf(buf, sizeof(buf), "ESIK_OK_%d_%d\n", rodId, shockMg);
    sendToAndroid(buf);
}

void processIncomingTelemetry(int rodId, int strikeType, int intensity, int battery) {
    char buf[64];

    // 1. Pil Bilgisi
    if (battery > 0) {
        snprintf(buf, sizeof(buf), "PIL_%d_%d\n", rodId, battery);
        sendToAndroid(buf);
    }

    // 2. Vuruş Bilgisi
    if (strikeType == 1) {
        snprintf(buf, sizeof(buf), "OLTA_%d_SIDDET_%d\n", rodId, intensity);
        sendToAndroid(buf);
        triggerFishStrike5Times();
    } else if (strikeType == 2) {
        snprintf(buf, sizeof(buf), "BOSA_DUSTU_%d\n", rodId);
        sendToAndroid(buf);
        triggerDropBackAlert();
    } else if (strikeType == 3) {
        snprintf(buf, sizeof(buf), "HIRSIZLIK_%d\n", rodId);
        sendToAndroid(buf);
        triggerTheftPanicSiren();
    } else if (strikeType == 4) {
        // Eşik Onayı (Kamıştan gelen ACK yanıtı)
        snprintf(buf, sizeof(buf), "ESIK_OK_%d_%d\n", rodId, intensity * 10);
        sendToAndroid(buf);
        Serial.printf("⚡ [KAMIŞ EŞİK ONAYI] Olta #%d eşiği güncelledi.\n", rodId);
    } else {
        snprintf(buf, sizeof(buf), "PING_%d\n", rodId);
        sendToAndroid(buf);
    }
}

// ======================= ESP-NOW ALICI KESMESİ =======================
#if defined(ESP_ARDUINO_VERSION_MAJOR) && (ESP_ARDUINO_VERSION_MAJOR >= 3)
void onDataRecv(const esp_now_recv_info_t *info, const uint8_t *data, int len) {
    const uint8_t *senderMac = info->src_addr;
#else
void onDataRecv(const uint8_t *mac, const uint8_t *data, int len) {
    const uint8_t *senderMac = mac;
#endif
    if (len != sizeof(RodSensorPacket)) return;

    memcpy(&rxPacket, data, sizeof(rxPacket));

    int rodId      = rxPacket.rodId;
    int strikeType = rxPacket.strikeType;
    int intensity  = rxPacket.intensity;
    int battery    = rxPacket.batteryPercent;

    if (rodId >= 1 && rodId <= 10) {
        lastRodPacketTime[rodId] = millis();
        if (!isRodOnline[rodId]) {
            isRodOnline[rodId] = true;
            char macStr[18];
            snprintf(macStr, sizeof(macStr), "%02X:%02X:%02X:%02X:%02X:%02X",
                     senderMac[0], senderMac[1], senderMac[2], senderMac[3], senderMac[4], senderMac[5]);
            char onlineBuf[48];
            snprintf(onlineBuf, sizeof(onlineBuf), "MAC_%d_%s_%d\n", rodId, macStr, battery);
            sendToAndroid(onlineBuf);
        }
    }

    processIncomingTelemetry(rodId, strikeType, intensity, battery);
}

// ======================= TELEFONDAN GELEN KOMUTLARI İŞLE =======================
void processAndroidCommand(String rawCmd, int senderWifiIndex = -1) {
    rawCmd.trim();
    if (rawCmd.length() == 0) return;

    String cmd = rawCmd;
    cmd.toUpperCase();
    Serial.printf("📥 [CMD_RX] '%s'\n", cmd.c_str());

    // 1. ⚡ DİNAMİK EŞİK PAKETİ: ESIK:<rodId>:<shockMg>:<dropBackDeg>:<theftDeg>:<sampleMs>:<pingSec>
    if (cmd.startsWith("ESIK:") || cmd.startsWith("ESIK_")) {
        char delimiter = cmd.startsWith("ESIK:") ? ':' : '_';
        int p1 = cmd.indexOf(delimiter);
        int p2 = cmd.indexOf(delimiter, p1 + 1);
        int p3 = cmd.indexOf(delimiter, p2 + 1);
        int p4 = cmd.indexOf(delimiter, p3 + 1);
        int p5 = cmd.indexOf(delimiter, p4 + 1);
        int p6 = cmd.indexOf(delimiter, p5 + 1);

        uint8_t rodId = (p1 != -1 && p2 != -1) ? cmd.substring(p1 + 1, p2).toInt() : 1;
        float shock = (p2 != -1 && p3 != -1) ? cmd.substring(p2 + 1, p3).toFloat() : 500.0f;
        float dropBack = (p3 != -1 && p4 != -1) ? cmd.substring(p3 + 1, p4).toFloat() : 1.5f;
        float theft = (p4 != -1 && p5 != -1) ? cmd.substring(p4 + 1, p5).toFloat() : 18.0f;
        uint8_t sampleMs = (p5 != -1 && p6 != -1) ? cmd.substring(p5 + 1, p6).toInt() : (p5 != -1 ? cmd.substring(p5 + 1).toInt() : 20);
        uint16_t pingSec = (p6 != -1) ? cmd.substring(p6 + 1).toInt() : 14400;

        if (shock < 50.0f) shock = 50.0f;
        if (shock > 1500.0f) shock = 1500.0f;
        if (dropBack <= 0.0f) dropBack = 1.5f;
        if (theft <= 0.0f) theft = 18.0f;
        if (sampleMs < 5 || sampleMs > 100) sampleMs = 20;
        if (pingSec < 10) pingSec = 14400;

        forwardThresholdToSensor(rodId, (uint16_t)shock, (uint16_t)(dropBack * 10.0f), (uint16_t)(theft * 10.0f), sampleMs, pingSec);
    }
    // 2. ⚡ DOĞRUDAN HASSASİYET KOMUTU: HASSASIYET_<rodId>_<deger> veya HASSASIYET:<rodId>:<deger>
    else if (cmd.startsWith("HASSASIYET_") || cmd.startsWith("HASSASIYET:")) {
        char delimiter = cmd.startsWith("HASSASIYET:") ? ':' : '_';
        int p1 = cmd.indexOf(delimiter);
        int p2 = cmd.indexOf(delimiter, p1 + 1);
        if (p1 != -1 && p2 != -1) {
            uint8_t rodId = cmd.substring(p1 + 1, p2).toInt();
            int val = cmd.substring(p2 + 1).toInt();
            uint16_t shockMg = 500;
            if (val > 10) {
                // Doğrudan mG olarak gönderilmiş (örn: 250, 500, 750)
                shockMg = (uint16_t)constrain(val, 50, 1500);
            } else {
                // 1..10 kademe skalası (1: 750 mG, 5: 483 mG, 10: 150 mG)
                int clamped = constrain(val, 1, 10);
                shockMg = (uint16_t)(750 - (clamped - 1) * 66);
            }
            forwardThresholdToSensor(rodId, shockMg, 15, 180, 20, 14400);
        }
    }
    // 3. ⚡ KALİBRASYON MOTORU KOMUTU: KALIBRE:<rodId>:<shockMg>:<dropBackTenths>:<filterTenths>:<level>
    else if (cmd.startsWith("KALIBRE:") || cmd.startsWith("KALIBRE_")) {
        char delimiter = cmd.startsWith("KALIBRE:") ? ':' : '_';
        int p1 = cmd.indexOf(delimiter);
        int p2 = cmd.indexOf(delimiter, p1 + 1);
        int p3 = cmd.indexOf(delimiter, p2 + 1);
        int p4 = cmd.indexOf(delimiter, p3 + 1);
        int p5 = cmd.indexOf(delimiter, p4 + 1);

        uint8_t rodId = (p1 != -1 && p2 != -1) ? cmd.substring(p1 + 1, p2).toInt() : 1;
        uint16_t shockMg = (p2 != -1 && p3 != -1) ? cmd.substring(p2 + 1, p3).toInt() : 500;
        uint16_t dropBackTenths = (p3 != -1 && p4 != -1) ? cmd.substring(p3 + 1, p4).toInt() : 15;
        uint8_t sampleMs = 20;
        uint16_t pingSec = 14400;

        forwardThresholdToSensor(rodId, shockMg, dropBackTenths, 180, sampleMs, pingSec);
    }
    // 4. SENSÖR KAPAT / AÇ EMİRLERİ (GUC_1_0 veya GUC_1_1)
    else if (cmd.startsWith("GUC_") || cmd.startsWith("GUC:")) {
        char delimiter = cmd.startsWith("GUC:") ? ':' : '_';
        int firstUnderscore = cmd.indexOf(delimiter);
        int secondUnderscore = cmd.indexOf(delimiter, firstUnderscore + 1);
        if (firstUnderscore != -1 && secondUnderscore != -1) {
            int rodId = cmd.substring(firstUnderscore + 1, secondUnderscore).toInt();
            int state = cmd.substring(secondUnderscore + 1).toInt();

            if (state == 0) {
                blackoutAllAlerts();
            }

            forwardCommandToSensor((uint8_t)rodId, (uint8_t)state);
        }
    }
    // 5. SUSTURMA EMİRLERİ
    else if (cmd == "MUTE" || cmd == "SUSTUR" || cmd == "STOP_ALARM") {
        blackoutAllAlerts();
    }
    // 6. CANLILIK KONTROLÜ
    else if (cmd == "PING" || cmd == "ALIVE") {
        sendToAndroid("ALIVE\n");
    }
}

// ======================= KURULUM (SETUP) =======================
void setup() {
    Serial.begin(115200);

    // Tamponları standart boyutta ayır (Eski telsiz ses tamponları temizlendi)
    btBuffer.reserve(512);
    phoneHotspotBuffer.reserve(512);
    for (int i = 0; i < MAX_WIFI_CLIENTS; i++) {
        wifiBuffers[i].reserve(512);
    }

    pinMode(BUZZER_PIN, OUTPUT);
    pinMode(LED_PIN, OUTPUT);
    pinMode(ONBOARD_LED_PIN, OUTPUT);

    blackoutAllAlerts();

    delay(100);
    Serial.println("\n📟 ÇADIR MERKEZİ (ESP32) - HAZIRLANIYOR...");

    // 1. Bluetooth SPP'yi Başlat
    if (SerialBT.begin(BT_DEVICE_NAME)) {
        Serial.printf("✅ Bluetooth SPP Devrede: '%s'\n", BT_DEVICE_NAME);
    } else {
        Serial.println("⚠️ Bluetooth başlatılamadı!");
    }

    delay(100);

    // 2. Wi-Fi AP + STA (Erişim Noktası + ESP-NOW İstasyonu) Yapılandırması
    WiFi.persistent(false);               // NVS Flash bozulmasını engelle
    WiFi.disconnect(true, true);          // Eski NVS önbelleğini temizle

    // Hem AP (Telefonun bağlanması) hem STA (Kamışlarla ESP-NOW telsiz) için çift mod
    WiFi.mode(WIFI_AP_STA);

    // Sabit IP (192.168.4.1), Gateway ve Subnet Ayarla
    WiFi.softAPConfig(IPAddress(192, 168, 4, 1), IPAddress(192, 168, 4, 1), IPAddress(255, 255, 255, 0));

    // Wi-Fi Erişim Noktasını (SoftAP) Başlat: 'T_F_Y_SMART_FISHING', KANAL 1 (Kamış Vericileriyle 1:1 Eşzamanlı)
    if (strlen(WIFI_AP_PASS) >= 8) {
        WiFi.softAP(WIFI_AP_SSID, WIFI_AP_PASS, 1, 0, 4);
    } else {
        WiFi.softAP(WIFI_AP_SSID, NULL, 1, 0, 4);
    }

    // Wi-Fi Güç Tasarrufunu Kapat (Kopmaları ve Paket Düşmesini Önler)
    esp_wifi_set_ps(WIFI_PS_NONE);

    if (strlen(PHONE_HOTSPOT_SSID) > 0) {
        WiFi.begin(PHONE_HOTSPOT_SSID, PHONE_HOTSPOT_PASS);
        Serial.printf("📡 [HOTSPOT] Telefon Erişim Noktasına bağlanılıyor: '%s'...\n", PHONE_HOTSPOT_SSID);
    }

    // 3. ESP-NOW Ağı Başlat (Wi-Fi Kanal 1 ile 1:1 Eşzamanlı)
    if (esp_now_init() == ESP_OK) {
        esp_now_register_recv_cb(onDataRecv);
        memcpy(peerInfo.peer_addr, broadcastMac, 6);
        peerInfo.channel = 1; // Wi-Fi Kanal 1 ile Senkronize
        peerInfo.encrypt = false;
        esp_now_add_peer(&peerInfo);
        Serial.println("✅ ESP-NOW Sensör Ağı Devrede (Kanal 1).");
    } else {
        Serial.println("⚠️ ESP-NOW başlatılamadı!");
    }

    // 4. Wi-Fi TCP Soket Sunucusunu Başlat (192.168.4.1:8080)
    wifiServer.begin();
    wifiServer.setNoDelay(true);
    Serial.printf("✅ Wi-Fi Sunucusu Açıldı: '%s' (Kanal: 1, Port: %d)\n", WIFI_AP_SSID, WIFI_TCP_PORT);
}

// ======================= ANA DÖNGÜ (LOOP) =======================
void loop() {
    unsigned long now = millis();

    // Bluetooth & Wi-Fi Canlılık Koruması (Keep-Alive)
    if (now - lastKeepAliveTime >= 2000) {
        lastKeepAliveTime = now;
        if (SerialBT.hasClient()) {
            SerialBT.print("ALIVE\n");
        }
        for (int i = 0; i < MAX_WIFI_CLIENTS; i++) {
            if (wifiClients[i] && wifiClients[i].connected()) {
                wifiClients[i].print("ALIVE\n");
            }
        }
        if (phoneHotspotClient && phoneHotspotClient.connected()) {
            phoneHotspotClient.print("ALIVE\n");
        }
    }

    // Telsiz Kopma Takibi (5 Dakikalık Güvenli Eşik - Buzzer Ötüşü Kaldırıldı)
    if (now - lastWatchdogCheckTime >= 10000) {
        lastWatchdogCheckTime = now;
        for (int i = 1; i <= 10; i++) {
            if (isRodOnline[i] && (now - lastRodPacketTime[i] > WATCHDOG_TIMEOUT_MS)) {
                isRodOnline[i] = false;
                char buf[32];
                snprintf(buf, sizeof(buf), "KOPUK_%d\n", i);
                sendToAndroid(buf);
            }
        }
    }

    // 1. Telefondan gelen Bluetooth verilerini satır satır oku
    while (SerialBT.available()) {
        char c = (char)SerialBT.read();
        if (c == '\n' || c == '\r') {
            if (btBuffer.length() > 0) {
                processAndroidCommand(btBuffer, -1);
                btBuffer = "";
            }
        } else {
            btBuffer += c;
            if (btBuffer.length() > 300) btBuffer = "";
        }
        yield();
    }

    // 2. Wi-Fi Yeni İstemci Bağlantısı Kabulü (Anında ALIVE yanıtı ile sıfır gecikme)
    WiFiClient newClient = wifiServer.accept();
    if (newClient) {
        for (int i = 0; i < MAX_WIFI_CLIENTS; i++) {
            if (!wifiClients[i] || !wifiClients[i].connected()) {
                wifiClients[i] = newClient;
                wifiClients[i].setNoDelay(true);
                wifiBuffers[i] = "";
                Serial.printf("📱 [WIFI] Yeni Telefon Bağlandı (İstemci #%d)\n", i + 1);
                wifiClients[i].print("ALIVE\n");
                break;
            }
        }
    }

    // 3. Wi-Fi İstemcilerinden Gelen Verileri Oku
    for (int i = 0; i < MAX_WIFI_CLIENTS; i++) {
        if (wifiClients[i] && wifiClients[i].connected()) {
            while (wifiClients[i].available()) {
                char c = (char)wifiClients[i].read();
                if (c == '\n' || c == '\r') {
                    if (wifiBuffers[i].length() > 0) {
                        processAndroidCommand(wifiBuffers[i], i);
                        wifiBuffers[i] = "";
                    }
                } else {
                    wifiBuffers[i] += c;
                    if (wifiBuffers[i].length() > 300) wifiBuffers[i] = "";
                }
                yield();
            }
        }
    }

    // 4. Telefon Hotspot Soket Bağlantısı ve Veri Okuma (İstemci Modu)
    if (strlen(PHONE_HOTSPOT_SSID) > 0 && WiFi.status() == WL_CONNECTED) {
        if (!phoneHotspotClient.connected()) {
            if (now - lastHotspotConnectAttempt >= 4000) {
                lastHotspotConnectAttempt = now;
                Serial.printf("🔄 [HOTSPOT] Telefona (%s:%d) bağlanılıyor...\n", PHONE_HOTSPOT_SERVER_IP, PHONE_HOTSPOT_SERVER_PORT);
                if (phoneHotspotClient.connect(PHONE_HOTSPOT_SERVER_IP, PHONE_HOTSPOT_SERVER_PORT)) {
                    Serial.println("✅ [HOTSPOT] Telefonda kurulu yerel sunucuya bağlanıldı!");
                    phoneHotspotClient.print("ALIVE\n");
                }
            }
        } else {
            while (phoneHotspotClient.available()) {
                char c = (char)phoneHotspotClient.read();
                if (c == '\n' || c == '\r') {
                    if (phoneHotspotBuffer.length() > 0) {
                        processAndroidCommand(phoneHotspotBuffer, -2);
                        phoneHotspotBuffer = "";
                    }
                } else {
                    phoneHotspotBuffer += c;
                    if (phoneHotspotBuffer.length() > 300) phoneHotspotBuffer = "";
                }
            }
        }
    }

    delay(2);
}
