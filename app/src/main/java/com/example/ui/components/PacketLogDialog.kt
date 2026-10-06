package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FishingRod
import com.example.model.PacketLog
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonGreenLight

@Composable
fun PacketLogDialog(
    logs: List<PacketLog>,
    rods: List<FishingRod>,
    onSimulateTrigger: (Int) -> Unit,
    onSimulateDropBack: (Int) -> Unit,
    onSimulateBattery: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Terminal,
                    contentDescription = null,
                    tint = NeonGreen
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "İletişim Konsolu & Test",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Text(
                        text = "Bluetooth SPP / Wi-Fi WebSocket Paketleri",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = NeonGreenLight
                        )
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "1. Normal Vuruş Simülasyonu (Flaşör & Ses):",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = Color(0xFFFDE047),
                        fontWeight = FontWeight.Bold
                    )
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(rods) { rod ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF161A24),
                            border = BorderStroke(1.dp, Color(0xFF282F42)),
                            modifier = Modifier.clip(RoundedCornerShape(8.dp))
                        ) {
                            Button(
                                onClick = { onSimulateTrigger(rod.id) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Transparent,
                                    contentColor = Color.White
                                ),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                    horizontal = 8.dp,
                                    vertical = 4.dp
                                ),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FlashOn,
                                    contentDescription = null,
                                    tint = NeonGreen,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "OLTA_${rod.id}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "3. Tersine Vuruş (Boşa Düşme) Simülasyonu:",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = Color(0xFFFFD700),
                        fontWeight = FontWeight.Bold
                    )
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(rods) { rod ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF2A200F),
                            border = BorderStroke(1.dp, Color(0xFF6B4D16)),
                            modifier = Modifier.clip(RoundedCornerShape(8.dp))
                        ) {
                            Button(
                                onClick = { onSimulateDropBack(rod.id) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Transparent,
                                    contentColor = Color.White
                                ),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                    horizontal = 8.dp,
                                    vertical = 4.dp
                                ),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFFFD700),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "BOŞA_${rod.id}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "2. Düşük Pil Simülasyonu (%15):",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = Color(0xFFFF3366),
                        fontWeight = FontWeight.Bold
                    )
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(rods) { rod ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF261218),
                            border = BorderStroke(1.dp, Color(0xFF5E212D)),
                            modifier = Modifier.clip(RoundedCornerShape(8.dp))
                        ) {
                            Button(
                                onClick = { onSimulateBattery(rod.id, 15) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Transparent,
                                    contentColor = Color.White
                                ),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                    horizontal = 8.dp,
                                    vertical = 4.dp
                                ),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BatteryAlert,
                                    contentDescription = null,
                                    tint = Color(0xFFFF3366),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "PİL_${rod.id}_%15",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Giden ve Gelen Komut Günlüğü:",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color(0xFF7E8799)
                    )
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF090A0E))
                        .border(1.dp, Color(0xFF1F2433), RoundedCornerShape(12.dp))
                        .padding(8.dp)
                ) {
                    if (logs.isEmpty()) {
                        Text(
                            text = "Henüz paket akışı yok...",
                            color = Color(0xFF64748B),
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(logs, key = { it.id }) { log ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = log.timeFormatted,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 10.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = if (log.isOutbound) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                        contentDescription = null,
                                        tint = if (log.isOutbound) Color(0xFF60A5FA) else NeonGreen,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = log.message,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            fontWeight = if (log.message.contains("ALARM") || log.message.contains("OLTA") || log.message.contains("BOSA")) FontWeight.Bold else FontWeight.Normal,
                                            color = if (log.isOutbound) Color(0xFF93C5FD)
                                            else if (log.message.contains("BOSA")) Color(0xFFFFD700)
                                            else if (log.message.contains("ALARM") || log.message.contains("OLTA")) NeonGreen
                                            else Color(0xFFE2E8F0)
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(text = "Kapat", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = Color(0xFF141620),
        shape = RoundedCornerShape(20.dp)
    )
}
