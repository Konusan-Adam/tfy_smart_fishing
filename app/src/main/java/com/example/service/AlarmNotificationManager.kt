package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.model.AlarmType

class AlarmNotificationManager(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "fishing_strike_alarm_channel_v2"
        const val NOTIFICATION_ID = 9991
    }

    init {
        createHighPriorityChannel()
    }

    private fun createHighPriorityChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val channel = NotificationChannel(
                CHANNEL_ID,
                "Acil Balık Alarmı (Kilit Ekranı Uyanma)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Balık vurduğunda kilit ekranını delip uygulamayı otomatik tam ekran açar"
                setSound(soundUri, audioAttributes)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 400, 200, 400, 200, 600)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                setBypassDnd(true)
            }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    /**
     * Kilit ekranındayken ekranı fiziksel olarak aydınlatır,
     * Full-Screen Intent fırlatır ve MainActivity'yi doğrudan kilit ekranının üstüne açar.
     */
    fun triggerFullScreenLockWakeup(rodId: Int, rodName: String, alarmType: AlarmType, intensity: Int = 80) {
        try {
            // 1. Fiziksel Ekran Panelini Aç (WakeLock)
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            @Suppress("DEPRECATION")
            val wakeLock = powerManager?.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
                PowerManager.ACQUIRE_CAUSES_WAKEUP or
                PowerManager.ON_AFTER_RELEASE,
                "BalikZili:FullScreenWakeLock"
            )
            wakeLock?.acquire(15000L) // 15 saniye açık tut

            // 2. Full-Screen Intent Hazırlama
            val fullScreenIntent = Intent(context, MainActivity::class.java).apply {
                action = "ACTION_FISHING_ALARM"
                putExtra("EXTRA_ROD_ID", rodId)
                putExtra("EXTRA_ALARM_TYPE", alarmType.name)
                putExtra("EXTRA_INTENSITY", intensity)
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                )
            }

            val fullScreenPendingIntent = PendingIntent.getActivity(
                context,
                rodId,
                fullScreenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            )

            val title = when (alarmType) {
                AlarmType.DROP_BACK -> "⚠️ BOŞA DÜŞTÜ: $rodName"
                AlarmType.THEFT -> "🚨 DİKKAT! OLTANIZ ÇALINIYOR: $rodName"
                else -> "🚨 BALIK VURUYOR: $rodName (Şiddet: %$intensity)"
            }

            val content = when (alarmType) {
                AlarmType.DROP_BACK -> "Olta misinası boşladı (Gevşeme)!"
                AlarmType.THEFT -> "Hırsız alarmı! Kamış sehpadan kaldırılıyor veya çekiliyor!"
                else -> "Dokun veya aç: Kamış çılgınca sallanıyor!"
            }

            // 3. Android Full-Screen Notification İnşası
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setFullScreenIntent(fullScreenPendingIntent, true) // ANDROID'İN KİLİT EKRANINI DELMESİNİ SAĞLAYAN ANAHTAR
                .setContentIntent(fullScreenPendingIntent)
                .setAutoCancel(true)
                .setOngoing(false)
                .build()

            val notificationManager = NotificationManagerCompat.from(context)
            try {
                notificationManager.notify(NOTIFICATION_ID, notification)
            } catch (e: SecurityException) {
                e.printStackTrace()
            }

            // 4. Doğrudan Activity'yi başlatmayı da dene (İzin verilmiş cihazlarda anında açar)
            try {
                context.startActivity(fullScreenIntent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
