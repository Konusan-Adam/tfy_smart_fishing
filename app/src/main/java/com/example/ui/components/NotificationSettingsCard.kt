package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.notification.TfyNotificationManager
import com.example.ui.theme.NeonGreen

@Composable
fun NotificationSettingsCard(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val fcmToken = remember { TfyNotificationManager.getFcmToken(context) }

    var isAnnouncementsEnabled by remember {
        mutableStateOf(TfyNotificationManager.isCategoryEnabled(context, "notif_announcements"))
    }
    var isUpdatesEnabled by remember {
        mutableStateOf(TfyNotificationManager.isCategoryEnabled(context, "notif_updates"))
    }
    var isWeatherAlertsEnabled by remember {
        mutableStateOf(TfyNotificationManager.isCategoryEnabled(context, "notif_weather"))
    }

    Surface(
        color = Color(0xFF0F1422),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFF26334A)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Başlık
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1B2A3A))
                        .border(1.dp, Color(0xFF38BDF8), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column {
                    Text(
                        text = "CANLI BİLDİRİM PANELİ (FIREBASE FCM)",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            fontSize = 12.5.sp
                        )
                    )
                    Text(
                        text = "Google Firebase Console üzerinden tüm kullanıcılara anlık mesaj",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFF94A3B8),
                            fontSize = 10.5.sp
                        )
                    )
                }
            }

            // Cihaz FCM Token Kopyalama Alanı
            Surface(
                color = Color(0xFF0B0D14),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "FCM CİHAZ PUSH TOKEN'INIZ:",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFF64748B),
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = fcmToken,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF38BDF8),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Surface(
                        color = Color(0xFF1E293B),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.clickable {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("FCM Token", fcmToken)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "📋 FCM Token Panoya Kopyalandı!", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Kopyala",
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "Kopyala",
                                color = Color.White,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Bildirim Kategorileri
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                // 1. Duyuru
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📢 Canlı Duyuru ve Sezon Haberleri",
                        style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFCBD5E1), fontSize = 11.5.sp)
                    )
                    Switch(
                        checked = isAnnouncementsEnabled,
                        onCheckedChange = {
                            isAnnouncementsEnabled = it
                            TfyNotificationManager.setCategoryEnabled(context, "notif_announcements", it)
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = NeonGreen, checkedTrackColor = Color(0xFF004D25)),
                        modifier = Modifier.testTag("notif_announcements_switch")
                    )
                }

                // 2. Güncelleme
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🚀 Yeni Sürüm & Güncelleme Bildirimleri",
                        style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFCBD5E1), fontSize = 11.5.sp)
                    )
                    Switch(
                        checked = isUpdatesEnabled,
                        onCheckedChange = {
                            isUpdatesEnabled = it
                            TfyNotificationManager.setCategoryEnabled(context, "notif_updates", it)
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = NeonGreen, checkedTrackColor = Color(0xFF004D25)),
                        modifier = Modifier.testTag("notif_updates_switch")
                    )
                }

                // 3. Fırtına & Hava Uyarısı
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⛈️ Fırtına ve Av Hava Durumu Uyarıları",
                        style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFCBD5E1), fontSize = 11.5.sp)
                    )
                    Switch(
                        checked = isWeatherAlertsEnabled,
                        onCheckedChange = {
                            isWeatherAlertsEnabled = it
                            TfyNotificationManager.setCategoryEnabled(context, "notif_weather", it)
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = NeonGreen, checkedTrackColor = Color(0xFF004D25)),
                        modifier = Modifier.testTag("notif_weather_switch")
                    )
                }
            }

            // Örnek Test Bildirimi Gönder Butonu
            Button(
                onClick = {
                    TfyNotificationManager.showNotification(
                        context = context,
                        title = "📢 TFY Smart Fishing Canlı Bildirim",
                        body = "Tebrikler! Firebase FCM canlı bildirim sistemi cihazınızda aktif ve sorunsuz çalışıyor.",
                        channelId = TfyNotificationManager.CHANNEL_ANNOUNCEMENTS
                    )
                    Toast.makeText(context, "🔔 Test bildirimi gönderildi!", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("test_push_notification_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "🧪 Örnek Canlı Bildirim Test Et",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.5.sp
                )
            }
        }
    }
}
