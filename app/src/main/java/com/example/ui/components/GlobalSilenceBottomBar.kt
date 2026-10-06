package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AlertRedMatte
import com.example.ui.theme.MatteBlackBg

@Composable
fun GlobalSilenceBottomBar(
    hasActiveAlarm: Boolean,
    onSilenceAndReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "AlarmSilencePulse")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 350, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseGlow"
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MatteBlackBg,
        border = BorderStroke(1.dp, Color(0xFF1E222F))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            // 5. GLOBAL SUSTURMA: Ekrana lüks bir şekilde gömülmüş, tüm alarmları sakinleştiren
            // ve sesi anında kesen büyük, mat bir 'Alarmı Sustur ve Sıfırla' butonu
            Button(
                onClick = onSilenceAndReset,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .shadow(
                        elevation = if (hasActiveAlarm) 10.dp else 2.dp,
                        shape = RoundedCornerShape(12.dp),
                        spotColor = if (hasActiveAlarm) AlertRedMatte else Color.Black
                    )
                    .testTag("silence_and_reset_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (hasActiveAlarm) AlertRedMatte else Color(0xFF141720),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    width = if (hasActiveAlarm) (2.dp * pulseGlow) else 1.dp,
                    color = if (hasActiveAlarm) Color.White else Color(0xFF282E3E)
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (hasActiveAlarm) Icons.Default.VolumeOff else Icons.Default.NotificationsOff,
                        contentDescription = "Sustur",
                        modifier = Modifier.size(24.dp),
                        tint = if (hasActiveAlarm) Color.White else Color(0xFF94A3B8)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Alarmı Sustur ve Sıfırla",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp, // 3sp artırıldı
                            letterSpacing = 0.5.sp,
                            color = Color.White
                        )
                    )
                }
            }
        }
    }
}
