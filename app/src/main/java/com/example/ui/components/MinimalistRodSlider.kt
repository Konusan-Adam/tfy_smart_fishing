package com.example.ui.components

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MetallicSilver
import com.example.ui.theme.NeonGreen

@Composable
fun MinimalistRodSlider(
    currentLevel: Int,
    rodId: Int,
    currentShockMg: Float? = null,
    onLevelChanged: (Int) -> Unit,
    onShockMgSelected: ((Float) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // Dinamik hesaplanan mG değeri
    val approxMg = kotlin.math.round(750f - (currentLevel - 1) * (600f / 9f)).toInt()
    val displayMg = currentShockMg?.toInt() ?: approxMg

    Column(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "HASSASİYET",
            style = MaterialTheme.typography.labelSmall.copy(
                color = Color(0xFF6B7280),
                fontSize = 15.sp,
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Bold
            )
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Dijital Büyük Gösterge: 5 / 10
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "$currentLevel",
                style = MaterialTheme.typography.displayMedium.copy(
                    color = NeonGreen,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.SansSerif
                )
            )
            Text(
                text = "/10",
                modifier = Modifier.padding(bottom = 6.dp, start = 2.dp),
                style = MaterialTheme.typography.titleMedium.copy(
                    color = Color(0xFF4B5563),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }

        // ⚡ Gerçek Dinamik mG İvme Rozeti (Karta Anlık İletilen Eşik)
        Surface(
            color = Color(0xFF10241A),
            shape = RoundedCornerShape(6.dp),
            border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.6f)),
            modifier = Modifier.padding(vertical = 3.dp)
        ) {
            Text(
                text = "⚡ $displayMg mG Dinamik",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = NeonGreen,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    letterSpacing = 0.3.sp
                ),
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 10 Kademeli Hassasiyet Işık Segmenti
        Row(
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 1..10) {
                val isLit = i <= currentLevel
                Box(
                    modifier = Modifier
                        .size(width = 10.dp, height = 3.5.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (isLit) NeonGreen else Color(0xFF222634))
                        .shadow(if (isLit) 3.dp else 0.dp, spotColor = NeonGreen)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 🎯 HIZLI EŞİK BUTONLARI: 750 mG | 500 mG | 250 mG
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            listOf(
                750f to "750 mG",
                500f to "500 mG",
                250f to "250 mG"
            ).forEach { (mg, label) ->
                val isMatch = (displayMg in (mg - 35).toInt()..(mg + 35).toInt())
                Surface(
                    color = if (isMatch) NeonGreen.copy(alpha = 0.25f) else Color(0xFF141824),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(
                        1.dp,
                        if (isMatch) NeonGreen else Color(0xFF2A3448)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            if (onShockMgSelected != null) {
                                onShockMgSelected(mg)
                            } else {
                                val targetLevel = when (mg) {
                                    750f -> 1
                                    500f -> 5
                                    250f -> 8
                                    else -> 5
                                }
                                onLevelChanged(targetLevel)
                            }
                        }
                        .testTag("preset_btn_${rodId}_${mg.toInt()}")
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (isMatch) NeonGreen else Color(0xFF94A3B8),
                            fontWeight = if (isMatch) FontWeight.Black else FontWeight.Bold,
                            fontSize = 10.5.sp
                        ),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(vertical = 4.dp, horizontal = 2.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Dokunmatik Senkronize Eksi (-) ve Artı (+) Butonları
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Eksi (-) butonu
            IconButton(
                onClick = { if (currentLevel > 1) onLevelChanged(currentLevel - 1) },
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF161922))
                    .border(1.5.dp, Color(0xFF2D3546), CircleShape)
                    .shadow(5.dp, CircleShape)
                    .testTag("slider_minus_$rodId")
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Hassasiyeti Azalt",
                    tint = MetallicSilver,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Artı (+) butonu
            IconButton(
                onClick = { if (currentLevel < 10) onLevelChanged(currentLevel + 1) },
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF161922))
                    .border(1.5.dp, Color(0xFF2D3546), CircleShape)
                    .shadow(5.dp, CircleShape)
                    .testTag("slider_plus_$rodId")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Hassasiyeti Artır",
                    tint = MetallicSilver,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
