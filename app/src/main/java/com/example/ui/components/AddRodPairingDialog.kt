package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.RgbLedColor
import com.example.ui.theme.NeonGreen

data class DiscoveredSensorInfo(
    val rodId: Int,
    val macAddress: String,
    val batteryPercent: Int = 90,
    val rssi: Int = -65
)

enum class PairingMode {
    AUTO_RF,
    MANUAL_MAC
}

@Composable
fun AddRodPairingDialog(
    suggestedRodId: Int,
    autoDiscoveredSensor: DiscoveredSensorInfo?,
    onVerifyManualInput: (String) -> DiscoveredSensorInfo?,
    onSaveAndPair: (
        name: String,
        macAddress: String,
        sensorId: Int,
        color: RgbLedColor,
        baitNote: String,
        batteryPercent: Int,
        isFriendRod: Boolean,
        ownerName: String
    ) -> Unit,
    onDismiss: () -> Unit
) {
    var pairingMode by remember { mutableStateOf(PairingMode.AUTO_RF) }
    var manualMacInput by remember { mutableStateOf("") }
    var rodName by remember { mutableStateOf("${suggestedRodId}. Olta") }
    var baitNote by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(RgbLedColor.defaultForRod(suggestedRodId)) }
    var isFriendRod by remember { mutableStateOf(false) }
    var ownerName by remember { mutableStateOf("") }

    var verifiedSensor by remember { mutableStateOf<DiscoveredSensorInfo?>(null) }
    var verificationError by remember { mutableStateOf<String?>(null) }
    var isCheckingManual by remember { mutableStateOf(false) }

    // Otomatik modda gelen canlı telsiz paketini anında yakala
    LaunchedEffect(autoDiscoveredSensor) {
        if (autoDiscoveredSensor != null && verifiedSensor == null) {
            verifiedSensor = autoDiscoveredSensor
            rodName = "${autoDiscoveredSensor.rodId}. Olta"
            verificationError = null
        }
    }

    // Radar animasyonu
    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF132219))
                        .border(1.dp, NeonGreen, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Sensors,
                        contentDescription = null,
                        tint = NeonGreen,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column {
                    Text(
                        text = "Yeni Kamış Sensörü Eşleştir",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    )
                    Text(
                        text = "Kanal #${suggestedRodId} için canlı telsiz doğrulaması",
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
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. EŞLEŞTİRME MODU SEÇİCİ [ Otomatik Telsiz | Manuel MAC ]
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF0F121C),
                    border = BorderStroke(1.dp, Color(0xFF222838)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val isAuto = pairingMode == PairingMode.AUTO_RF
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isAuto) Color(0xFF1B2E24) else Color.Transparent,
                            border = BorderStroke(
                                1.dp,
                                if (isAuto) NeonGreen.copy(alpha = 0.6f) else Color.Transparent
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    pairingMode = PairingMode.AUTO_RF
                                    verificationError = null
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Radar,
                                    contentDescription = null,
                                    tint = if (isAuto) NeonGreen else Color(0xFF7E8799),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Otomatik Telsiz",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (isAuto) Color.White else Color(0xFF94A3B8),
                                        fontWeight = if (isAuto) FontWeight.Bold else FontWeight.Normal
                                    )
                                )
                            }
                        }

                        val isManual = pairingMode == PairingMode.MANUAL_MAC
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isManual) Color(0xFF1B2E24) else Color.Transparent,
                            border = BorderStroke(
                                1.dp,
                                if (isManual) NeonGreen.copy(alpha = 0.6f) else Color.Transparent
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    pairingMode = PairingMode.MANUAL_MAC
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = if (isManual) NeonGreen else Color(0xFF7E8799),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Manuel MAC / ID",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (isManual) Color.White else Color(0xFF94A3B8),
                                        fontWeight = if (isManual) FontWeight.Bold else FontWeight.Normal
                                    )
                                )
                            }
                        }
                    }
                }

                // 2. OTOMATİK MOD GÖRÜNÜMÜ: Telsiz Dinleme Radarı
                if (pairingMode == PairingMode.AUTO_RF) {
                    if (verifiedSensor == null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF0D121B),
                            border = BorderStroke(1.dp, Color(0xFF222838)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .scale(pulseScale)
                                        .clip(CircleShape)
                                        .background(Color(0xFF142E1F))
                                        .border(2.dp, NeonGreen.copy(alpha = 0.8f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.WifiTethering,
                                        contentDescription = null,
                                        tint = NeonGreen,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Telsiz Sinyali Aranıyor...",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Lütfen eşleştirmek istediğiniz kamış sensörünü 1 kez sallayın, misinayı çekin veya sensörü açın.",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color(0xFF94A3B8),
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp
                                    ),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(10.dp))
                                CircularProgressIndicator(
                                    color = NeonGreen,
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                    }
                } else {
                    // 3. MANUEL MOD GÖRÜNÜMÜ: MAC veya Kanal No Girişi
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "SENSÖR MAC ADRESİ VEYA KANAL NO:",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )

                        OutlinedTextField(
                            value = manualMacInput,
                            onValueChange = {
                                manualMacInput = it
                                verificationError = null
                            },
                            placeholder = {
                                Text(
                                    text = "Örn: 24:DC:C3:5B:82:1A veya 2",
                                    color = Color(0xFF55607A),
                                    fontSize = 12.sp
                                )
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = NeonGreen,
                                unfocusedBorderColor = Color(0xFF2C3244)
                            )
                        )

                        Button(
                            onClick = {
                                isCheckingManual = true
                                val verified = onVerifyManualInput(manualMacInput)
                                isCheckingManual = false
                                if (verified != null) {
                                    verifiedSensor = verified
                                    rodName = "${verified.rodId}. Olta"
                                    verificationError = null
                                } else {
                                    verificationError = "Bu sensörden canlı telsiz sinyali alınamadı! Sensörün açık ve çadır alıcısına yakın olduğundan emin olun."
                                    verifiedSensor = null
                                }
                            },
                            enabled = manualMacInput.isNotBlank() && !isCheckingManual,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B2E24)),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.NetworkCheck,
                                contentDescription = null,
                                tint = NeonGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isCheckingManual) "Sinyal Doğrulanıyor..." else "Bağlantıyı Doğrula & Test Sinyali İste",
                                color = NeonGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // 4. BAĞLANTI HATASI (Doğrulanamadıysa kırmızı uyarı)
                AnimatedVisibility(visible = verificationError != null) {
                    Surface(
                        color = Color(0xFF281316),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFFFF3366).copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = Color(0xFFFF3366),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = verificationError ?: "",
                                color = Color(0xFFFF99AA),
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }

                // 5. DOĞRULANMIŞ SENSÖR BİLGİ KARTI (Sadece telsizle doğrulandıysa açılır)
                AnimatedVisibility(visible = verifiedSensor != null) {
                    verifiedSensor?.let { sensor ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF0F1E16),
                            border = BorderStroke(1.5.dp, NeonGreen),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = NeonGreen,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "SENSÖR DOĞRULANDI & BAĞLANDI!",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = NeonGreen,
                                                fontWeight = FontWeight.Black,
                                                letterSpacing = 0.5.sp
                                            )
                                        )
                                    }

                                    // Tekrar Tara
                                    Text(
                                        text = "Değiştir",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 11.sp,
                                        modifier = Modifier.clickable {
                                            verifiedSensor = null
                                            verificationError = null
                                        }
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = "Donanım MAC:",
                                            style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF7E8B9B), fontSize = 10.sp)
                                        )
                                        Text(
                                            text = sensor.macAddress,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = Color.White,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "Telsiz Sinyali / Pil:",
                                            style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF7E8B9B), fontSize = 10.sp)
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "${sensor.rssi} dBm",
                                                color = NeonGreen,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(
                                                imageVector = Icons.Default.BatteryFull,
                                                contentDescription = null,
                                                tint = NeonGreen,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text = "%${sensor.batteryPercent}",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 6. KAMIŞ ÖZELLEŞTİRME (Ad, Renk, Yem Notu)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "KAMIŞ SAHİBİ VE ALARM AYARLARI:",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )

                    // 👥 ARKADAŞ / EMANET NÖBETÇİ KAMIŞI KARTI
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isFriendRod) Color(0xFF261536) else Color(0xFF131722),
                        border = BorderStroke(1.dp, if (isFriendRod) Color(0xFFB388FF) else Color(0xFF252D3D)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                isFriendRod = !isFriendRod
                                if (isFriendRod && ownerName.isNotBlank()) {
                                    rodName = "${ownerName}'ın ${suggestedRodId}. Oltası"
                                }
                            }
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Group,
                                        contentDescription = null,
                                        tint = if (isFriendRod) Color(0xFFB388FF) else Color(0xFF7E8B9B),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Arkadaş / Emanet Kamışı (Nöbet)",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (isFriendRod) Color.White else Color(0xFFB0B9C6),
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                                Switch(
                                    checked = isFriendRod,
                                    onCheckedChange = {
                                        isFriendRod = it
                                        if (isFriendRod && ownerName.isNotBlank()) {
                                            rodName = "${ownerName}'ın ${suggestedRodId}. Oltası"
                                        }
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color(0xFFB388FF),
                                        checkedTrackColor = Color(0xFF4A148C)
                                    )
                                )
                            }

                            AnimatedVisibility(visible = isFriendRod) {
                                Column(modifier = Modifier.padding(top = 8.dp)) {
                                    OutlinedTextField(
                                        value = ownerName,
                                        onValueChange = {
                                            ownerName = it
                                            if (it.isNotBlank()) {
                                                rodName = "${it}'ın ${suggestedRodId}. Oltası"
                                            }
                                        },
                                        label = { Text("Arkadaşının Adı (Kimin Oltası?)") },
                                        placeholder = { Text("Örn: Ahmet Abi, Murat") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedBorderColor = Color(0xFFB388FF),
                                            unfocusedBorderColor = Color(0xFF5B4278)
                                        )
                                    )
                                    Text(
                                        text = "📢 Balık vurduğunda asistan seni uyarır: 'Ali! Dikkat! ${ownerName.ifBlank { "Ahmet" }}'ın oltasına balık bindi, koş!'",
                                        color = Color(0xFFD1C4E9),
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = rodName,
                        onValueChange = { rodName = it },
                        label = { Text("Olta Adı") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = NeonGreen,
                            unfocusedBorderColor = Color(0xFF2C3244)
                        )
                    )

                    OutlinedTextField(
                        value = baitNote,
                        onValueChange = { baitNote = it },
                        label = { Text("Yem / Not (Opsiyonel)") },
                        placeholder = { Text("Örn: Mısır & Pop-up") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = NeonGreen,
                            unfocusedBorderColor = Color(0xFF2C3244)
                        )
                    )

                    // LED Renk Seçimi
                    Text(
                        text = "Alarm / LED Rengi:",
                        style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF7E8B9B), fontSize = 11.sp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        RgbLedColor.PRESETS.take(6).forEach { colorPreset ->
                            val isSelected = selectedColor.name == colorPreset.name
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(colorPreset.color)
                                    .border(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) Color.White else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColor = colorPreset }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            // KRİTİK KURAL: Sadece bağlantı kuruldu ve doğrulandıysa aktif olur!
            val canSave = verifiedSensor != null && rodName.isNotBlank()
            Button(
                onClick = {
                    val s = verifiedSensor ?: return@Button
                    onSaveAndPair(
                        rodName.trim(),
                        s.macAddress,
                        s.rodId,
                        selectedColor,
                        baitNote.trim(),
                        s.batteryPercent,
                        isFriendRod,
                        ownerName.trim()
                    )
                    onDismiss()
                },
                enabled = canSave,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonGreen,
                    disabledContainerColor = Color(0xFF1E2822)
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("confirm_pair_button")
            ) {
                Text(
                    text = if (canSave) "Oltayı Sisteme Kaydet & Eşleştir" else "Önce Bağlantıyı Doğrulayın",
                    color = if (canSave) Color.Black else Color(0xFF55655E),
                    fontWeight = FontWeight.Black
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("İptal", color = Color(0xFF94A3B8))
            }
        },
        containerColor = Color(0xFF141724),
        shape = RoundedCornerShape(20.dp)
    )
}
