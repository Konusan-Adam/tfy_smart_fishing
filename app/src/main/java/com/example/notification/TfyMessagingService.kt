package com.example.notification

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * 🛰️ FIREBASE CLOUD MESSAGING (FCM) CANLI MESAJ BİLDİRİM SERVİSİ
 * - Firebase Console üzerinden gönderilen bildirimleri yakalar
 * - Arka planda veya kapalıyken bile telefon ekranına yüksek öncelikli düşürür
 */
class TfyMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        android.util.Log.d("TfyMessagingService", "Refreshed FCM Token: $token")
        TfyNotificationManager.saveFcmToken(this, token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: "🎣 TFY Smart Fishing"

        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: "Yeni bir canlı duyuru veya bildirim aldınız."

        val channelId = remoteMessage.data["channel_id"]
            ?: TfyNotificationManager.CHANNEL_ANNOUNCEMENTS

        android.util.Log.d("TfyMessagingService", "Incoming FCM Push: $title - $body")

        TfyNotificationManager.showNotification(
            context = this,
            title = title,
            body = body,
            channelId = channelId
        )
    }
}
