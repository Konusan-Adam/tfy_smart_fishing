package com.example.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.google.firebase.messaging.FirebaseMessaging
import java.util.UUID

/**
 * 🔔 CANLI BİLDİRİM VE PUSH NOTIFICATION YÖNETİCİSİ
 * - Firebase Cloud Messaging (FCM) ve Sistem Bildirim Kanallarını yapılandırır
 * - Duyuru, Güncelleme ve Fırtına/Hava durumu kanallarını yüksek öncelikli olarak yönetir
 * - Cihaz FCM Token'ını güvenle kaydeder
 */
object TfyNotificationManager {

    const val CHANNEL_ANNOUNCEMENTS = "tfy_announcements"
    const val CHANNEL_UPDATES = "tfy_updates"
    const val CHANNEL_WEATHER_ALERTS = "tfy_weather_alerts"

    private const val PREFS_NAME = "tfy_notification_prefs"
    private const val KEY_FCM_TOKEN = "fcm_push_token"
    private const val KEY_NOTIF_ANNOUNCEMENTS = "notif_announcements_enabled"
    private const val KEY_NOTIF_UPDATES = "notif_updates_enabled"
    private const val KEY_NOTIF_WEATHER = "notif_weather_enabled"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun fetchRealFcmToken(context: Context) {
        try {
            // Clear any stale FCM topic sync queue in SharedPreferences
            try {
                context.getSharedPreferences("com.google.android.gms.appid", Context.MODE_PRIVATE).edit().clear().apply()
                context.getSharedPreferences("com.google.firebase.messaging", Context.MODE_PRIVATE).edit().clear().apply()
            } catch (_: Exception) {}

            // Check if GMS is installed on device
            try {
                context.packageManager.getPackageInfo("com.google.android.gms", 0)
            } catch (_: Exception) {
                android.util.Log.i("TfyNotificationManager", "GMS package not present. FCM skipped.")
                return
            }

            val availability = com.google.android.gms.common.GoogleApiAvailability.getInstance()
                .isGooglePlayServicesAvailable(context)
            if (availability != com.google.android.gms.common.ConnectionResult.SUCCESS) {
                android.util.Log.i("TfyNotificationManager", "Google Play Services status: $availability. FCM skipped.")
                return
            }

            if (com.google.firebase.FirebaseApp.getApps(context).isEmpty()) {
                com.google.firebase.FirebaseApp.initializeApp(context)
            }

            // Execute token fetch safely without enabling auto-init
            val fm = FirebaseMessaging.getInstance()
            fm.isAutoInitEnabled = false
            fm.token.addOnCompleteListener { task ->
                try {
                    if (task.isSuccessful && task.result != null) {
                        val token = task.result
                        saveFcmToken(context, token)
                        android.util.Log.d("TfyNotificationManager", "Real FCM Token fetched: $token")
                    } else {
                        val exc = task.exception
                        android.util.Log.w("TfyNotificationManager", "FCM Token retrieval requires SHA-1 and FCM enabled in Firebase Console: ${exc?.message}")
                    }
                } catch (e: Exception) {
                    android.util.Log.w("TfyNotificationManager", "FCM token processing skipped: ${e.message}")
                }
            }
        } catch (e: Exception) {
            android.util.Log.i("TfyNotificationManager", "FCM initialization skipped: ${e.message}")
        }
    }

    fun saveFcmToken(context: Context, token: String) {
        getPrefs(context).edit().putString(KEY_FCM_TOKEN, token.trim()).apply()
        android.util.Log.d("TfyNotificationManager", "FCM Token Saved: $token")
    }

    fun getFcmToken(context: Context): String {
        val saved = getPrefs(context).getString(KEY_FCM_TOKEN, "")
        if (!saved.isNullOrBlank()) return saved

        // Token henüz oluşmadıysa örnek bir id türet
        val newToken = "tfy_fcm_${UUID.randomUUID().toString().take(16)}"
        saveFcmToken(context, newToken)
        return newToken
    }

    fun setCategoryEnabled(context: Context, key: String, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(key, enabled).apply()
    }

    fun isCategoryEnabled(context: Context, key: String): Boolean {
        return getPrefs(context).getBoolean(key, true)
    }

    /**
     * Bildirim Kanallarını (Notification Channels) Android 8.0+ için oluşturur
     */
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .build()

            // 1. Duyuru & Trofe Kanalı
            val channelAnnounce = NotificationChannel(
                CHANNEL_ANNOUNCEMENTS,
                "📢 Canlı Duyuru & Trofe Bildirimleri",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Balıkçılık haberleri, trofe paylaşımları ve canlı duyurular"
                enableLights(true)
                lightColor = Color.GREEN
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 150, 300)
                setSound(alarmSound, audioAttributes)
            }

            // 2. Sürüm Güncellemeleri Kanalı
            val channelUpdates = NotificationChannel(
                CHANNEL_UPDATES,
                "🚀 Sürüm & YAZILIM Güncellemeleri",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Yeni APK güncellemeleri ve ESP32 yazılım duyuruları"
                enableLights(true)
                lightColor = Color.CYAN
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 200, 100, 200)
            }

            // 3. Fırtına & Hava Uyarısı Kanalı
            val channelWeather = NotificationChannel(
                CHANNEL_WEATHER_ALERTS,
                "⛈️ Fırtına & Av Hava Uyarısı",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Deniz fırtınası, ani basınç düşüşü ve av koşulları uyarıları"
                enableLights(true)
                lightColor = Color.YELLOW
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
            }

            manager.createNotificationChannel(channelAnnounce)
            manager.createNotificationChannel(channelUpdates)
            manager.createNotificationChannel(channelWeather)
        }
    }

    /**
     * Yüksek Öncelikli Canlı Bildirimi Ekrana Yansıtır
     */
    fun showNotification(
        context: Context,
        title: String,
        body: String,
        channelId: String = CHANNEL_ANNOUNCEMENTS
    ) {
        createNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(System.currentTimeMillis().toInt(), builder.build())
    }
}
