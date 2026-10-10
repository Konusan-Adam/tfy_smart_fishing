package com.example.model

/**
 * 📢 ÖZEL SPONSOR VE YERLİ REKLAM BANNER MODELİ
 * - Üçüncü taraf reklam ağlarına bağımlı olmadan doğrudan yönetilen sponsor afişleri
 */
enum class SponsorActionType {
    OPEN_URL,         // Web sitesi veya e-ticaret mağazası aç
    OPEN_WHATSAPP,    // Doğrudan WhatsApp hattına yönlendir
    CALL_PHONE,       // Müşteri hizmetlerini / bayiyi ara
    COPY_PROMO_CODE   // Özel indirim kuponunu panoya kopyala
}

data class SponsorBanner(
    val id: String,
    val title: String,
    val description: String,
    val badgeText: String = "ÖZEL SPONSOR",
    val imageUrl: String? = null,
    val actionType: SponsorActionType = SponsorActionType.OPEN_URL,
    val actionTarget: String = "https://tfysmartfishing.com",
    val buttonText: String = "Sponsoru İncele",
    val isActive: Boolean = true
)
