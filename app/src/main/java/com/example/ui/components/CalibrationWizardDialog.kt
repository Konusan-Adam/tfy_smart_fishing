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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FishingRod
import com.example.ui.theme.LuxuryGold
import com.example.ui.theme.MetallicSilver
import com.example.ui.theme.NeonGreen

@Composable
fun CalibrationWizardDialog(
    rod: FishingRod,
    onDismiss: () -> Unit,
    onConfirmStart: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onConfirmStart,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonGreen,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("dialog_button_start")
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "BAŞLA",
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF2C3240)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MetallicSilver),
                modifier = Modifier.testTag("dialog_button_cancel")
            ) {
                Text(
                    text = "İPTAL",
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
            }
        },
        title = {
            Text(
                text = "${rod.name} Kalibrasyon",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 18.sp
                )
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0F1117))
                        .border(1.dp, Color(0xFF232736), RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Sensörü yerleştirin ve BAŞLA butonuna basın.",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = Color(0xFFE2E8F0),
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp,
                            lineHeight = 22.sp,
                            textAlign = TextAlign.Center
                        )
                    )
                }

                // 2.20m Altın Standart Tavsiyesi
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF1C190D))
                        .border(1.dp, LuxuryGold.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sensors,
                            contentDescription = null,
                            tint = LuxuryGold,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "4.20m'ye Kadar Olan Kamışlarda: En Az 2.20m",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = LuxuryGold,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            )
                            Text(
                                text = "4.20 metreye kadar olan kamışlarda maksimum RF sinyal menzili (250m+) ve en hassas vuruş tespiti için sensörü 2.20m (2. parçanın üstü) seviyesine takmanız önerilir.",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFFCBD5E1),
                                    fontSize = 10.sp,
                                    lineHeight = 13.sp
                                )
                            )
                        }
                    }
                }
            }
        },
        containerColor = Color(0xFF151822),
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 10.dp
    )
}
