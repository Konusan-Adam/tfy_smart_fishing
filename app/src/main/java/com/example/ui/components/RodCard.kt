package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AlarmType
import com.example.model.FishingRod
import com.example.model.RgbLedColor
import com.example.ui.theme.LuxuryCardBg
import com.example.ui.theme.LuxuryCardBorder
import com.example.ui.theme.LuxuryNeonBlue
import com.example.ui.theme.NeonGreen

@Composable
fun RodCard(
    rod: FishingRod,
    onSensitivityChanged: (Int) -> Unit,
    onShockMgChanged: (Float) -> Unit = {},
    onColorSelected: (RgbLedColor) -> Unit,
    onDropBackColorSelected: (RgbLedColor) -> Unit = {},
    onTheftColorSelected: (RgbLedColor) -> Unit = {},
    onTestTriggerClick: () -> Unit,
    onEditRodClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    onToggleFriendWatch: () -> Unit = {},
    onCalibrateClick: () -> Unit = {},
    onTogglePower: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // 1 & 3. Alarm Strobe Flash Animation
    val infiniteTransition = rememberInfiniteTransition(label = "LuxuryStrobe")
    val strobeFlash by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (rod.alarmType == AlarmType.DROP_BACK) 140 else 200,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "StrobeAlpha"
    )

    // 🎯 3 AYRI ALARM TÜRÜ İÇİN ÖZEL SEÇİLEN RENK KULLANILIR:
    // - Vuruş: rod.ledColor
    // - Boşa Düşme: rod.dropBackLedColor
    // - Hırsızlık: rod.theftLedColor
    val activeAlarmColor = rod.currentAlarmColor.color

    // Strobe border and ambient glow
    val borderStroke = if (rod.isAlarming) {
        BorderStroke(
            width = (3.5.dp * strobeFlash) + 1.dp,
            brush = Brush.radialGradient(
                colors = listOf(Color.White, activeAlarmColor, Color.Transparent)
            )
        )
    } else {
        BorderStroke(1.dp, NeonGreen)
    }

    val cardBg = if (rod.isAlarming) {
        activeAlarmColor.copy(alpha = 0.25f + (0.50f * strobeFlash))
    } else {
        LuxuryCardBg
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (rod.isAlarming) 22.dp else 8.dp,
                shape = RoundedCornerShape(26.dp),
                spotColor = if (rod.isAlarming) activeAlarmColor else Color.Black
            )
            .testTag("rod_card_${rod.id}"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = borderStroke
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // CARD HEADER: Olta Adı, Akıllı Pil Göstergesi & Durum Butonu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sol: Olta adı, düzenleme ikonu ve yem etiketi
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .padding(end = 6.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onEditRodClick() }
                        .testTag("edit_rod_info_btn_${rod.id}")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(activeAlarmColor)
                                .shadow(8.dp, CircleShape, spotColor = activeAlarmColor)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = rod.name.uppercase(),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 24.sp, // 3sp artırıldı
                                letterSpacing = 1.2.sp,
                                color = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Düzenle",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    if (rod.baitNote.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Surface(
                            color = Color(0xFF161C26),
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.6f))
                        ) {
                            Text(
                                text = "🪱 ${rod.baitNote}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFFFFD166),
                                    fontSize = 16.sp, // 3sp artırıldı
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }

                    if (!rod.isArmed) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Surface(
                            color = Color(0xFF2A1517),
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(1.dp, Color(0xFFFF3B30).copy(alpha = 0.6f))
                        ) {
                            Text(
                                text = "🛑 PASİF",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFFFF453A),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // 🎣 OTOMATİK KALİBRE EDİLEN KAMIŞ PROFİLİ ROZETİ (Yalnızca profile varsa gösterilir)
                    if (!rod.assignedRodProfileName.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Surface(
                            color = Color(0xFF0F1A1B),
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = "🎣 ${rod.assignedRodProfileName}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = NeonGreen,
                                        fontSize = 15.5.sp, // 3sp artırıldı
                                        fontWeight = FontWeight.Bold
                                    ),
                                    maxLines = 1
                                )
                                Text(
                                    text = "• Oto-Kalibre ⚡",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color(0xFF68D391),
                                        fontSize = 15.sp, // 3sp artırıldı
                                        fontWeight = FontWeight.Normal
                                    )
                                )
                            }
                        }
                    }

                    if (rod.isFriendRod) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Surface(
                            color = if (rod.isWatchActive) Color(0xFF281338) else Color(0xFF1A1A22),
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(
                                1.dp,
                                if (rod.isWatchActive) Color(0xFFB388FF).copy(alpha = 0.8f) else Color(0xFF444455)
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { onToggleFriendWatch() }
                        ) {
                            Text(
                                text = if (rod.isWatchActive)
                                    "👥 ${rod.ownerName.ifBlank { "Arkadaş" }} (Nöbettesin 🔔)"
                                else
                                    "👥 ${rod.ownerName.ifBlank { "Arkadaş" }} (Nöbet Kapalı 🔕)",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (rod.isWatchActive) Color(0xFFD1C4E9) else Color(0xFF888899),
                                    fontSize = 16.sp, // 3sp artırıldı
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Sağ: 2. KART ÜZERİNE AKILLI GÜÇ ANAHTARI (AÇIK/KAPALI) & Sensör Pil Göstergesi & Alarm Rozeti
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 🔋 SENSÖR PİL ŞARJ GÖSTERGESİ (AÇIK BUTONUNUN YANINDA)
                    SensorBatteryIndicator(
                        batteryPercent = rod.batteryPercent,
                        isOnline = rod.isOnline,
                        isSleeping = rod.isSleeping,
                        rodName = rod.name,
                        rodId = rod.id
                    )

                    // GÜÇ ANAHTARI (AÇIK / KAPALI)
                    val isArmed = rod.isArmed
                    Surface(
                        color = if (isArmed) Color(0xFF00381B) else Color(0xFF5A0E16),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(
                            1.5.dp,
                            if (isArmed) NeonGreen else Color(0xFFFF3B30)
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onTogglePower(!isArmed) }
                            .testTag("rod_power_toggle_${rod.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (isArmed) "AÇIK" else "KAPALI",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (isArmed) NeonGreen else Color(0xFFFF453A),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp, // 3sp artırıldı
                                    letterSpacing = 0.5.sp
                                )
                            )

                            // Mikro Switch Kapsülü
                            Box(
                                modifier = Modifier
                                    .width(28.dp)
                                    .height(16.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isArmed) NeonGreen.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.3f))
                                    .padding(2.dp),
                                contentAlignment = if (isArmed) Alignment.CenterEnd else Alignment.CenterStart
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(if (isArmed) NeonGreen else Color(0xFFFF3B30))
                                )
                            }
                        }
                    }

                    // Durum rozeti (VURUŞ VAR! / BOŞA DÜŞTÜ! / TEST ET)
                    if (rod.isAlarming) {
                        Surface(
                            color = activeAlarmColor,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (rod.alarmType == AlarmType.DROP_BACK) Icons.Default.Warning else Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (rod.alarmType == AlarmType.DROP_BACK) {
                                        "BOŞA DÜŞTÜ!"
                                    } else {
                                        "%${rod.strikeIntensity} ${rod.strikeIntensityLabel.uppercase()}"
                                    },
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color.Black,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 17.sp, // 3sp artırıldı
                                        letterSpacing = 0.5.sp
                                    )
                                )
                            }
                        }
                    }

                    // 1. LÜKS VE YÜKSEK KONTRASTLI 'ÇÖP KUTUSU' (SİL) BUTONU
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(Color(0xFF261214))
                            .border(1.dp, Color(0xFF68262B), RoundedCornerShape(9.dp))
                            .clickable { onDeleteClick() }
                            .testTag("delete_rod_button_${rod.id}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = "${rod.name} Sil",
                            tint = Color(0xFFFF6B6B),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 1. GERÇEKÇİ DİKEY OLTA KAMIŞI & METALİK HALKA SLIDER ENTEGRASYONU
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF0C0E14))
                    .border(1.dp, NeonGreen.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                    .padding(vertical = 10.dp, horizontal = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(0.48f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        RealisticFishingRodView(
                            currentSensitivity = rod.sensitivity,
                            rodId = rod.id,
                            onSensitivityChanged = onSensitivityChanged,
                            isAlarming = rod.isAlarming,
                            ledColor = rod.ledColor,
                            alarmType = rod.alarmType,
                            strikeIntensity = rod.strikeIntensity
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(0.52f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        MinimalistRodSlider(
                            currentLevel = rod.sensitivity,
                            rodId = rod.id,
                            currentShockMg = rod.currentShockMg,
                            onLevelChanged = onSensitivityChanged,
                            onShockMgSelected = onShockMgChanged
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3. AYRI AYRI SEÇİLEBİLEN 3 ALARM LED RENK SEÇİCİSİ (Vuruş / Boşa / Hırsızlık)
            SpectrumColorPicker(
                strikeColor = rod.ledColor,
                dropBackColor = rod.dropBackLedColor,
                theftColor = rod.theftLedColor,
                rodId = rod.id,
                rodName = rod.name,
                onStrikeColorSelected = onColorSelected,
                onDropBackColorSelected = onDropBackColorSelected,
                onTheftColorSelected = onTheftColorSelected
            )
        }
    }
}

/**
 * 🔋 SENSÖR PİL ŞARJ GÖSTERGESİ (AÇIK BUTONUNUN YANINDA)
 * - Sensörden gelen gerçek Li-ion pil şarj yüzdesini gösterir
 * - %15 altı kritik seviyede flaşörlü kırmızı uyarı
 * - %16-35 arası sarı/amber uyarı
 * - %35 üzeri dolu neon yeşil gösterge
 * - Dokunulduğunda olta adı ve şarj tavsiyesi bildirir
 */
@Composable
fun SensorBatteryIndicator(
    batteryPercent: Int,
    isOnline: Boolean,
    isSleeping: Boolean = false,
    rodName: String,
    rodId: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showSleepDialog by remember { mutableStateOf(false) }

    val isCritical = batteryPercent in 1..15
    val isLow = batteryPercent in 16..35
    val isGood = batteryPercent > 35

    // Kritik pil seviyesinde balıkçıyı uyarmak için yumuşak nabız (pulse) animasyonu
    val infiniteTransition = rememberInfiniteTransition(label = "BatteryPulse_$rodId")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    val (baseColor, bgColor) = when {
        isSleeping -> Color(0xFFBA68C8) to Color(0xFF27132F)
        !isOnline -> Color(0xFF94A3B8) to Color(0xFF141824)
        isCritical -> Color(0xFFFF3B30) to Color(0xFF381014)
        isLow -> Color(0xFFFFB703) to Color(0xFF2E2210)
        isGood -> NeonGreen to Color(0xFF00381B)
        else -> Color(0xFF94A3B8) to Color(0xFF141824)
    }

    val stateColor = if (isCritical && !isSleeping) baseColor.copy(alpha = pulseAlpha) else baseColor

    val icon = when {
        isSleeping -> Icons.Default.NightlightRound
        !isOnline -> Icons.Default.BatteryAlert
        isCritical -> Icons.Default.BatteryAlert
        isLow -> Icons.Default.BatteryAlert
        isGood -> Icons.Default.BatteryFull
        else -> Icons.Default.BatteryAlert
    }

    val displayText = when {
        isSleeping -> "UYKUDA"
        !isOnline -> "--%"
        batteryPercent > 0 -> "%$batteryPercent"
        else -> "%0"
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.5.dp, stateColor),
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable {
                if (isSleeping) {
                    showSleepDialog = true
                } else {
                    val statusMsg = when {
                        isCritical -> "⚠️ $rodName Sensör Pili: %$batteryPercent (KRİTİK - Lütfen Şarj Edin!)"
                        isLow -> "🔋 $rodName Sensör Pili: %$batteryPercent (Düşük Şarj)"
                        isGood -> "⚡ $rodName Sensör Pili: %$batteryPercent (Dolu & Stabil)"
                        else -> "📡 $rodName Sensör Pili: Sinyal Bekleniyor"
                    }
                    Toast.makeText(context, statusMsg, Toast.LENGTH_SHORT).show()
                }
            }
            .testTag("rod_battery_indicator_$rodId")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "Sensör Pil Şarjı: $displayText",
                tint = stateColor,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = displayText,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = stateColor,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    fontSize = if (isSleeping) 11.5.sp else 14.sp,
                    letterSpacing = 0.5.sp
                )
            )
        }
    }

    if (showSleepDialog) {
        AlertDialog(
            onDismissRequest = { showSleepDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.NightlightRound,
                        contentDescription = null,
                        tint = Color(0xFFBA68C8),
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "$rodName Sensör Uykuda",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Sensör 30 saniyedir sinyal göndermediği için güç tasarrufu amacıyla uyku modundadır.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFFCBD5E1))
                    )
                    Surface(
                        color = Color(0xFF1B1324),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFF7B1FA2))
                    ) {
                        Text(
                            text = "💡 Sensör Uykuda. Pili öğrenmek / sensörü uyandırmak için sensörü sallayın!",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFFE1BEE7),
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showSleepDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF9C27B0)
                    )
                ) {
                    Text("Anladım", color = Color.White)
                }
            },
            containerColor = Color(0xFF130D1A),
            shape = RoundedCornerShape(16.dp)
        )
    }
}

