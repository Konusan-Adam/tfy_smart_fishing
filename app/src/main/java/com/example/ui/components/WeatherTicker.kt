package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.MarqueeSpacing
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.WeatherDisplayData
import com.example.ui.theme.NeonGreen
import kotlinx.coroutines.delay

@Composable
fun WeatherTicker(
    weatherData: WeatherDisplayData,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp),
        color = Color(0xFF0F141F).copy(alpha = 0.8f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .basicMarquee(
                    iterations = Int.MAX_VALUE,
                    velocity = 45.dp,
                    initialDelayMillis = 1200,
                    repeatDelayMillis = 0,
                    spacing = MarqueeSpacing.fractionOfContainer(1f / 6f)
                )
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(40.dp)
        ) {
            // İçeriği 3 kez tekrar ederek her zaman ekrandan taşmasını ve kaymasını sağlıyoruz
            repeat(3) {
                TickerContent(weatherData)
            }
        }
    }
}

@Composable
private fun TickerContent(weatherData: WeatherDisplayData) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        if (weatherData.noNetwork) {
            TickerItem(
                icon = Icons.Default.WifiOff,
                text = "İNTERNET YOK - ÇEVRİMDIŞI VERİ",
                color = Color(0xFFFF3B30)
            )
        }

        Text(
            text = "📍 ${weatherData.cityName.uppercase()}",
            style = MaterialTheme.typography.labelSmall.copy(
                color = NeonGreen,
                fontWeight = FontWeight.Black,
                fontSize = 13.sp,
                letterSpacing = 1.sp
            )
        )

        TickerItem(
            icon = Icons.Default.Cloud,
            text = weatherData.description,
            color = Color.White
        )

        TickerItem(
            icon = Icons.Default.DeviceThermostat,
            text = weatherData.temperature,
            color = Color(0xFFFF9500)
        )

        TickerItem(
            icon = Icons.Default.Speed,
            text = "BAROMETRE: ${weatherData.pressure}",
            color = Color(0xFF5AC8FA)
        )

        TickerItem(
            icon = Icons.Default.WaterDrop,
            text = "NEM: ${weatherData.humidity}",
            color = Color(0xFF007AFF)
        )

        TickerItem(
            icon = Icons.Default.Speed,
            text = "RÜZGAR: ${weatherData.windSpeed}",
            color = Color(0xFF34C759)
        )
        
        if (weatherData.isFromCache && !weatherData.noNetwork) {
            Text(
                text = "[ÖNBELLEK]",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color.Gray,
                    fontSize = 10.sp
                )
            )
        }
    }
}

@Composable
private fun TickerItem(
    icon: ImageVector,
    text: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                color = color,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                letterSpacing = 0.5.sp
            )
        )
    }
}
