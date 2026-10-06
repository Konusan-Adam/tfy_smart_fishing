package com.example.ui.components

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FishingRod
import com.example.ui.theme.NeonGreen
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NightTentModeOverlay(
    rods: List<FishingRod>,
    onSilenceAlarm: () -> Unit,
    onExitNightMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentTimeFormatted by remember { mutableStateOf("") }

    // Canlı saat sayacı
    LaunchedEffect(Unit) {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        while (true) {
            currentTimeFormatted = sdf.format(Date())
            delay(1000)
        }
    }

    val alarmingRod = rods.find { it.isAlarming }
    val isAnyAlarming = alarmingRod != null

    // Vuruş anında ekranı o oltanın rengiyle flaşlatacak parlak arkaplan
    val infiniteTransition = rememberInfiniteTransition(label = "tent_alarm")
    val flashColor by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.90f,
        animationSpec = infiniteRepeatable(
            animation = tween(380),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tent_flash"
    )

    val backgroundColor = if (isAnyAlarming) {
        alarmingRod!!.currentAlarmColor.color.copy(alpha = flashColor)
    } else {
        Color(0xFF020305) // Saf OLED Siyah
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 22.dp)
    ) {
        // --- 1. ÜST BAR (Status Bar ile Asla Çakışmayan Güvenli Alan) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.NightlightRound,
                    contentDescription = null,
                    tint = Color(0xFFFFB703),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "ÇADIR GECE MODU (OLED HUD)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color(0xFFCBD5E1),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        fontSize = 11.sp
                    )
                )
            }

            Surface(
                color = Color(0xFF161A26),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Color(0xFF2E384D)),
                modifier = Modifier
                    .clickable { onExitNightMode() }
                    .testTag("exit_night_mode_btn")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Kapat",
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "Gündüz Moduna Dön",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }

        // --- 2. MERKEZ İÇERİK (Gece Saati, Hava/Sıcaklık veya Flaşör Alarmı) ---
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
        ) {
            if (isAnyAlarming) {
                // --- ALARM MODU: ODA AYDINLATMA & DEV İKAZ ---
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .shadow(35.dp, CircleShape, spotColor = alarmingRod!!.ledColor.color)
                        .clip(CircleShape)
                        .background(Color(0xFF0F121C))
                        .border(4.dp, alarmingRod.ledColor.color, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = "Vuruş Var!",
                        tint = alarmingRod.ledColor.color,
                        modifier = Modifier.size(70.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "🚨 ${alarmingRod!!.name.uppercase()} VURUYOR!",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 1.2.sp
                    ),
                    textAlign = TextAlign.Center
                )

                if (alarmingRod.baitNote.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        color = Color(0x33000000),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0x44FFFFFF))
                    ) {
                        Text(
                            text = "Takılı Yem: ${alarmingRod.baitNote}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color(0xFFFFD166),
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(26.dp))

                // Büyük Sustur ve Avla Butonu
                Button(
                    onClick = onSilenceAlarm,
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(64.dp)
                        .shadow(20.dp, RoundedCornerShape(16.dp), spotColor = Color(0xFFFF2A55))
                        .testTag("tent_silence_alarm_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF2A55),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeOff,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "ALARMI SUSTUR & AVLA",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    )
                }
            } else {
                // --- SAKİN GECE MODU: LOŞ OLED DİJİTAL SAAT & HAVA/BASINÇ HUD ---
                Text(
                    text = currentTimeFormatted,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        fontSize = 62.sp,
                        color = Color(0xFF38BDF8),
                        letterSpacing = 2.sp
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Tüm Oltalar Nöbette • Ekran Koruma Açık",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF94A3B8),
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // HAVA SICAKLIĞI & BAROMETRE & AY DURUMU BİLGİ KAPSÜLÜ
                Surface(
                    color = Color(0xFF0F1422),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFF222D42))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Sıcaklık
                        Text(
                            text = "🌡️ 18°C (Açık)",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        )

                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF475569))
                        )

                        // Rüzgar
                        Text(
                            text = "💨 8 km/s",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = Color(0xFFCBD5E1),
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp
                            )
                        )

                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF475569))
                        )

                        // Basınç
                        Text(
                            text = "📊 1014 hPa",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = NeonGreen,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                        )

                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF475569))
                        )

                        // Ay / Av Şansı
                        Text(
                            text = "🌖 %88 Av",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = Color(0xFFFFD166),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        )
                    }
                }
            }
        }

        // --- 3. ALT BAR (Ferah, Net, Yüksek Kontrastlı ve Android Barından Yukarıda) ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AKTİF OLTALAR & NÖBET DURUMLARI",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                )
                Text(
                    text = "${rods.size} Olta Aktif",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = NeonGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(rods) { rod ->
                    val isCritBattery = rod.batteryPercent <= 15
                    val rodBorderColor = when {
                        rod.isAlarming -> Color(0xFFFF2A55)
                        isCritBattery -> Color(0xFFFF4D6D)
                        else -> Color(0xFF263044)
                    }
                    val rodBgColor = when {
                        rod.isAlarming -> Color(0xFF3B0F18)
                        else -> Color(0xFF101420)
                    }

                    Surface(
                        color = rodBgColor,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.5.dp, rodBorderColor),
                        shadowElevation = if (rod.isAlarming) 8.dp else 2.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Parlayan LED Işığı
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .shadow(6.dp, CircleShape, spotColor = rod.ledColor.color)
                                    .clip(CircleShape)
                                    .background(rod.ledColor.color)
                            )

                            Column {
                                Text(
                                    text = rod.name,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                )
                                if (rod.baitNote.isNotBlank()) {
                                    Text(
                                        text = "🪱 ${rod.baitNote}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color(0xFFFFD166),
                                            fontSize = 9.sp
                                        ),
                                        maxLines = 1
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(2.dp))

                            // Pil Rozeti
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = if (isCritBattery) Icons.Default.BatteryAlert else Icons.Default.BatteryFull,
                                    contentDescription = null,
                                    tint = if (isCritBattery) Color(0xFFFF4D6D) else NeonGreen,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "%${rod.batteryPercent}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (isCritBattery) Color(0xFFFF4D6D) else NeonGreen,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
