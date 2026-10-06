# 📟 ÇADIR & CEP ALICISI (PAGER) VE BLUETOOTH KÖPRÜSÜ
## Devre Elemanları, Bağlantı Şeması ve Kurulum Kılavuzu

Bu kılavuz, çadırda, cebinizde veya kampta yanınızda duracak olan ve 4 ayrı kamıştan gelen telsiz sinyallerini toplayıp hem **Buzzer/Titreşim** ile uyaran hem de **Bluetooth üzerinden Android uygulamamıza aktaran** ana merkezin kurulumunu içerir.

---

### 🧰 1. Gerekli Devre Elemanları Listesi

| Eleman Adı | Model / Özellik | Adet | Açıklama |
| :--- | :--- | :---: | :--- |
| **Mikrodenetleyici** | **ESP32 (WROOM-32 / NodeMCU 30-Pin)** | 1 | Dahili Bluetooth Classic SPP + Wi-Fi + ESP-NOW |
| **Sesli Uyarıcı** | **Aktif veya Pasif Buzzer (5V / 3.3V)** | 1 | Vuruşlarda kesik bip, hırsızlıkta siren çalar |
| **Titreşim Motoru** | **Mini Coin Titreşim Motoru (3V)** | 1 | Cepte veya yastık altında güçlü titreşim |
| **Sürücü Transistör**| **2N2222 veya BC547 (NPN)** | 1 | Titreşim motorunu güvenle sürmek için |
| **Diyot** | **1N4148 veya 1N4007** | 1 | Titreşim motoru ters gerilim koruması (Flyback) |
| **Dirençler** | **1 kΩ (Transistör tabanı için)** | 1 | GPIO19 pini akım sınırlama |
| **Dirençler** | **220 Ω veya 330 Ω (LED'ler için)** | 4 | 4 adet olta gösterge LED'i için |
| **Gösterge LED'leri**| 5mm veya 3mm Renkli LED'ler | 4 | 1: Mavi, 2: Sarı, 3: Turuncu, 4: Kırmızı |
| **Sessize Alma Butonu**| Mini Push Button (Tact Switch) | 1 | Çalan alarmı anında susturmak için |
| **Şarj & Batarya** | **18650 Li-Ion Pil (2500-3400mAh) + TP4056**| 1 | Günlerce kesintisiz çalışma sağlar |

---

### 🔌 2. Pin Bağlantı Tablosu

| Bileşen | ESP32 Pini | Bağlantı Detayı |
| :--- | :--- | :--- |
| **Buzzer (+)** | **GPIO 18** | Pozitif bacak (Eksi bacak GND'ye) |
| **Titreşim Motoru** | **GPIO 19** | 1kΩ direnç üzerinden **2N2222 Base** bacağına |
| **Sessize Alma Butonu** | **GPIO 4** | Bir bacağı GPIO4'e, diğer bacağı GND'ye (Dahili Pull-Up) |
| **1. Olta LED'i (Mavi)** | **GPIO 21** | 220Ω seri direnç üzerinden Anot (+), Katot GND'ye |
| **2. Olta LED'i (Sarı)** | **GPIO 22** | 220Ω seri direnç üzerinden Anot (+), Katot GND'ye |
| **3. Olta LED'i (Turuncu)**| **GPIO 23**| 220Ω seri direnç üzerinden Anot (+), Katot GND'ye |
| **4. Olta LED'i (Kırmızı)**| **GPIO 25**| 220Ω seri direnç üzerinden Anot (+), Katot GND'ye |

---

### 📐 3. Görsel Devre Şeması (Titreşim Motoru & Buzzer Sürücüsü)

```text
                                       +3.3V / 5V
                                           |
                                           +-----+------------+
                                           |     |            |
                                         [Buzzer]  [Titreşim] |
                                           |       [Motoru]   |
                                           |         |        |
                                           |         +--[|<]--+  (1N4007 Diyot)
                                           |         |  (Katot)
                                           |       Collector
                       (1kΩ)               |         |
  ESP32 [GPIO 19] ----[====]------------------------>|  2N2222 (NPN)
                                                     |
                                                   Emitter
                                                     |
  ESP32 [GPIO 18] -------------------------+         |
                                           |         |
  ESP32 [GND] -----------------------------+---------+---------> [ORTAK GND]
```

#### 4x Olta LED Bağlantısı:
```text
  ESP32 [GPIO 21] ----[ 220Ω ]---->| (Mavi LED)    ----+
  ESP32 [GPIO 22] ----[ 220Ω ]---->| (Sarı LED)    ----+
  ESP32 [GPIO 23] ----[ 220Ω ]---->| (Turuncu LED) ----+-----> [GND]
  ESP32 [GPIO 25] ----[ 220Ω ]---->| (Kırmızı LED) ----+
```

---

### 📱 4. Android Telefon ile Bağlantı Seçenekleri (Dual-Mode: Bluetooth & Wi-Fi)

Alıcı cihazınız hem **Bluetooth Classic** hem de **Wi-Fi** üzerinden aynı anda telefona veri aktarabilir. İki yöntemden dilediğinizi kullanabilirsiniz:

#### A Seçeneği: Bluetooth ile Bağlanma
1. Alıcı kartınıza güç verin.
2. Android telefonunuzun **Ayarlar -> Bluetooth** menüsünü açın.
3. Yeni cihaz tara diyerek **`T_F_Y_SMART_FISHING`** cihazını bulun ve eşleştirin.
4. Android uygulamasını açıp sağ üstteki **Bluetooth İkonuna** dokunun ve `T_F_Y_SMART_FISHING` cihazını seçin.

#### B Seçeneği: Wi-Fi ile Bağlanma (Modem/İnternet Gerekmez!)
1. Alıcı kartınız açıldığında otomatik olarak kendi Wi-Fi ağını yayınlar:
   - **Wi-Fi Adı (SSID):** `T_F_Y_SMART_FISHING`
   - **Şifre:** `1234567890`
   - **IP & Port:** `192.168.4.1:8080`
2. Telefonunuzun Wi-Fi ayarlarına girip **`T_F_Y_SMART_FISHING`** ağına bağlanın (Şifre: 1234567890).
3. Android uygulamamızı açıp üst bardaki **Wi-Fi / Çevrimdışı** butonuna dokunun.
4. Karşınıza gelen pencerede **`192.168.4.1:8080`** yazıp **"ESP32 AP'ye Bağlan"** butonuna basın. Anında tüm kamışların canlı verisi Wi-Fi üzerinden akmaya başlar!

#### C Seçeneği: Telefon Hotspot'u ile Bağlanma
1. Telefonunuzun **Kişisel Erişim Noktası'nı (Hotspot)** açın.
2. ESP32 alıcı kodunun başındaki `PHONE_HOTSPOT_SSID` ve `PHONE_HOTSPOT_PASS` alanlarına telefonunuzun Hotspot adını ve şifresini yazıp yükleyin.
3. Uygulamada **Wi-Fi Modu** penceresini açıp **"Telefon Hotspot Sunucusu Başlat (Port 8080)"** butonuna basın. ESP32 otomatik olarak telefonun Hotspot'una bağlanır ve veri aktarır.

---

### ⚙️ 5. Arduino IDE Kart Ayarları ve Yükleme

1. **Kart:** `Tools -> Board -> ESP32 Arduino -> ESP32 Dev Module`
2. **Upload Speed:** `921600` veya `115200`
3. **CPU Frequency:** `240MHz`
4. **Flash Frequency:** `80MHz`
5. **Partition Scheme:** `Huge APP (3MB No OTA/1MB SPIFFS)` *(Bluetooth kütüphanesi için önerilir)*
6. **Port:** Kartın takılı olduğu COM portunu seçin ve Yükle butonuna basın.
