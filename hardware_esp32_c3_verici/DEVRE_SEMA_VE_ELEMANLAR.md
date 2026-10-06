# 🎣 KAMIŞ SENSÖR & VERİCİ ÜNİTESİ (ESP32 / ESP32-C3 + MPU-6050)
## Devre Elemanları, Doğrulanmış Pin Şeması ve Kurulum Kılavuzu (v3.1)

Bu kılavuz, olta kamışının üzerine takılacak olan kablosuz sensör ve verici ünitesinin donanım kurulumunu adım adım açıklar.

---

### 🧰 1. Gerekli Devre Elemanları Listesi

| Eleman Adı | Model / Özellik | Adet | Açıklama |
| :--- | :--- | :---: | :--- |
| **Mikrodenetleyici** | **ESP32 NodeMCU / ESP32-C3** | 1 | Dahili Wi-Fi/Bluetooth, Type-C/Micro-USB |
| **Hareket & İvme Sensörü**| **MPU-6050 (GY-521)** | 1 | 3 Eksen İvmeölçer + 3 Eksen Gyro (I2C) |
| **Şarj Modülü** | **TP4056 Korumalı (Type-C)** | 1 | 1A Lityum Pil Şarj ve Deşarj Koruma Devresi |
| **Batarya (Pil)** | **3.7V Li-Po (500mAh - 1200mAh)** | 1 | Kamışta hafiflik için yassı LiPo batarya |
| **Açma / Kapama Anahtarı**| Mini Sürgülü Switch (SPDT) | 1 | Pil ile devre arasına güç kesici anahtar |
| **Bildirim LED'i** | Dahili Mavi LED (GPIO 2) | 1 | Vuruş ve durum geri bildirimi |

---

### 🔌 2. Doğrulanmış Pin Bağlantı Tablosu

#### A) ESP32 ile MPU-6050 Arasındaki Bağlantı:
| MPU-6050 (GY-521) Pini | ESP32 Pini | Açıklama |
| :--- | :--- | :--- |
| **VCC** | **VIN (veya 5V / 3.3V)** | Sensör güç girişi (5V toleranslı) |
| **GND** | **GND** | Ortak Toprak (Şasi) |
| **SDA** | **GPIO 4 (D4)** | **Doğrulanan I2C Veri Hattı** |
| **SCL** | **GPIO 5 (D5)** | **Doğrulanan I2C Saat Sinyali Hattı** |
| **AD0** | *Boşta veya GND* | Varsayılan Adres `0x68` |
| **INT** | **GPIO 3** | **Donanımsal Wake-on-Motion / Kesme Hattı** |

#### B) Güç ve TP4056 Şarj Devresi Bağlantısı:
```text
[ 3.7V Li-Po Pil ] (+) ------> [ TP4056 B+ ]
[ 3.7V Li-Po Pil ] (-) ------> [ TP4056 B- ]

[ TP4056 OUT+ ] ----> [ Mini Switch ] ----> [ ESP32 VIN Pini (Sağ Üst 1. Pin) ]
[ TP4056 OUT- ] --------------------------> [ ESP32 GND Pini (Sağ Üst 2. Pin) ]
```

---

### 📡 3. Telsiz Haberleşme ve Fizik Parametreleri

* **Haberleşme:** ESP-NOW 2.4 GHz (Broadcast: `FF:FF:FF:FF:FF:FF`)
* **DLPF Filtresi:** 21 Hz Donanımsal Alçak Geçiren Filtre
* **Şok Vuruş Eşiği:** `75.0 mG`
* **Boşa Düşme (Drop-Back) Eşiği:** `2.5°`
* **Hırsızlık Eşiği:** `18.0°`
* **Kalp Atışı (Heartbeat / Pil):** Her 4 saniyede bir
