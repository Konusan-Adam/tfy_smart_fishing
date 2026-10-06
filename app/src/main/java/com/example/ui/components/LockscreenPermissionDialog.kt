package com.example.ui.components

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.PermissionHelper
import com.example.ui.theme.LuxuryCardBg
import com.example.ui.theme.NeonGreen

@Composable
fun LockscreenPermissionDialog(
    onRequestNotificationPermission: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isNotificationGranted = PermissionHelper.isNotificationPermissionGranted(context)
    val isBatteryIgnored = PermissionHelper.isIgnoringBatteryOptimizations(context)
    val isFullScreenAllowed = PermissionHelper.canUseFullScreenIntent(context)
    val isAllGranted = isNotificationGranted && isBatteryIgnored && isFullScreenAllowed

    val isXiaomi = Build.MANUFACTURER.contains("xiaomi", ignoreCase = true) ||
                    Build.MANUFACTURER.contains("redmi", ignoreCase = true) ||
                    Build.MANUFACTURER.contains("poco", ignoreCase = true)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0D1117),
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF059669), Color(0xFF10B981))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LockOpen,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column {
                    Text(
                        text = "Kilit Ekranı ve Gece Nöbeti",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "Ekran kapalıyken anında uyanma kurulumu",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Telefon kilitliyken veya cebindeyken balık vurduğunda ekranın anında parlayıp oltayı önüne getirmesi için aşağıdaki onayları tek dokunuşla verin:",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFFCBD5E1),
                        lineHeight = 16.sp
                    )
                )

                // 1. BİLDİRİM İZNİ
                PermissionItemCard(
                    icon = Icons.Default.NotificationsActive,
                    title = "1. Acil Durum Bildirimi & Ses",
                    description = "Vuruş anında telefonun yüksek sesle ötmesi için gereklidir.",
                    isGranted = isNotificationGranted,
                    buttonText = "İzin Ver",
                    onClick = onRequestNotificationPermission
                )

                // 2. PİL TASARRUFU MUAFİYETİ
                PermissionItemCard(
                    icon = Icons.Default.BatteryChargingFull,
                    title = "2. Kesintisiz Nöbet (Pil Tasarrufu Muafiyeti)",
                    description = "Telefon kilitliyken Bluetooth bağlantısının ve işlemcinin uyumasını engeller.",
                    isGranted = isBatteryIgnored,
                    buttonText = "Muaf Tut",
                    onClick = {
                        PermissionHelper.requestIgnoreBatteryOptimization(context)
                    }
                )

                // 3. ANDROID 14+ KİLİT EKRANI TAM EKRAN İZNİ
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    PermissionItemCard(
                        icon = Icons.Default.LockOpen,
                        title = "3. Kilit Ekranını Delme İzni",
                        description = "Balık vurduğunda uygulamanın kilit ekranının üstüne tam ekran açılmasını sağlar.",
                        isGranted = isFullScreenAllowed,
                        buttonText = "Etkinleştir",
                        onClick = {
                            PermissionHelper.requestFullScreenIntentPermission(context)
                        }
                    )
                }

                // 4. XIAOMI / SAMSUNG ÖZEL KİLİT EKRANI AYARI
                if (isXiaomi) {
                    Surface(
                        color = Color(0xFF161B26),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFFFB703).copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFFFB703),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Xiaomi / Redmi Cihazlar İçin:",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = Color(0xFFFFB703),
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Açılan sayfada 'Kilit ekranında göster' ve 'Açılır pencere' izinlerini 'Her Zaman İzin Ver' yapın.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFFE2E8F0),
                                    fontSize = 11.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { PermissionHelper.openOemOrAppSettings(context) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB703)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "⚙️ Xiaomi İzin Sayfasını Aç",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isAllGranted) NeonGreen else Color(0xFF238636)
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("confirm_lockscreen_permissions_btn")
            ) {
                Text(
                    text = if (isAllGranted) "✓ Tüm İzinler Hazır" else "Tamam / Kapat",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
}

@Composable
private fun PermissionItemCard(
    icon: ImageVector,
    title: String,
    description: String,
    isGranted: Boolean,
    buttonText: String,
    onClick: () -> Unit
) {
    Surface(
        color = LuxuryCardBg,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            1.dp,
            if (isGranted) NeonGreen.copy(alpha = 0.5f) else Color(0xFF30363D)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isGranted) NeonGreen else Color(0xFF94A3B8),
                    modifier = Modifier.size(20.dp)
                )
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFF8B949E),
                            fontSize = 11.sp,
                            lineHeight = 14.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (isGranted) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = NeonGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Aktif",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = NeonGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    )
                }
            } else {
                Button(
                    onClick = onClick,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = buttonText,
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
