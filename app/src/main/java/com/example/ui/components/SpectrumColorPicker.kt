package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.RgbLedColor
import com.example.ui.theme.NeonGreen
import kotlin.math.roundToInt

@Composable
fun SpectrumColorPicker(
    strikeColor: RgbLedColor,
    dropBackColor: RgbLedColor,
    theftColor: RgbLedColor,
    rodId: Int,
    rodName: String = "Olta $rodId",
    onStrikeColorSelected: (RgbLedColor) -> Unit,
    onDropBackColorSelected: (RgbLedColor) -> Unit,
    onTheftColorSelected: (RgbLedColor) -> Unit,
    modifier: Modifier = Modifier
) {
    var showColorModal by remember { mutableStateOf(false) }
    var activeModeTab by remember { mutableIntStateOf(0) } // 0: Vuruş, 1: Boşa Düşme, 2: Hırsızlık

    val currentModeColor = when (activeModeTab) {
        1 -> dropBackColor
        2 -> theftColor
        else -> strikeColor
    }

    // KART ÜZERİ: 3 Ayrı Alarm Rengi Seçici Şeridi (Vuruş / Boşa / Hırsızlık)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF0C0F16))
            .border(1.dp, Color(0xFF1B2232), RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Üst Kısım: 3 Sekmeli Alarm Türü Seçici & Renk Önizlemeleri
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 3'lü Mod Hap Butonları
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Vuruş Sekmesi
                AlarmTypePill(
                    title = "Vuruş",
                    dotColor = strikeColor.color,
                    isSelected = activeModeTab == 0,
                    onClick = { activeModeTab = 0 }
                )

                // 2. Boşa Düşme Sekmesi
                AlarmTypePill(
                    title = "Boşa",
                    dotColor = dropBackColor.color,
                    isSelected = activeModeTab == 1,
                    onClick = { activeModeTab = 1 }
                )

                // 3. Hırsızlık Sekmesi
                AlarmTypePill(
                    title = "Hırsız",
                    dotColor = theftColor.color,
                    isSelected = activeModeTab == 2,
                    onClick = { activeModeTab = 2 }
                )
            }

            // Sağdaki Özel Renk Paleti Butonu
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1B202D))
                    .border(1.dp, Color(0xFF333D54), RoundedCornerShape(8.dp))
                    .clickable { showColorModal = true }
                    .testTag("open_color_picker_modal_$rodId"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = "Tüm Renkler & Palet",
                    tint = NeonGreen,
                    modifier = Modifier.size(15.dp)
                )
            }
        }

        // Alt Kısım: Aktif Mod İçin Hızlı Preset Renk Noktaları
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val quickPresets = RgbLedColor.PRESETS.take(7)
            quickPresets.forEach { preset ->
                val isSelected = currentModeColor.color == preset.color
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .shadow(if (isSelected) 6.dp else 1.dp, CircleShape, spotColor = preset.color)
                        .clip(CircleShape)
                        .background(preset.color)
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) Color.White else Color(0x33FFFFFF),
                            shape = CircleShape
                        )
                        .clickable {
                            when (activeModeTab) {
                                0 -> onStrikeColorSelected(preset)
                                1 -> onDropBackColorSelected(preset)
                                2 -> onTheftColorSelected(preset)
                            }
                        }
                        .testTag("preset_color_${rodId}_${activeModeTab}_${preset.name}"),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Seçili",
                            tint = if (preset == RgbLedColor.PRESET_YELLOW || preset == RgbLedColor.PRESET_WHITE) Color.Black else Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }

    // --- 3 AYRI ALARM İÇİN ÖZEL RGB RENK PALETİ MODAL DİYALOĞU ---
    if (showColorModal) {
        var modalActiveTab by remember { mutableIntStateOf(activeModeTab) }
        var tempStrikeColor by remember { mutableStateOf(strikeColor) }
        var tempDropBackColor by remember { mutableStateOf(dropBackColor) }
        var tempTheftColor by remember { mutableStateOf(theftColor) }

        val activeSelectedColor = when (modalActiveTab) {
            1 -> tempDropBackColor
            2 -> tempTheftColor
            else -> tempStrikeColor
        }

        val spectrumColors = remember {
            listOf(
                Color(0xFFFF0000), // Red
                Color(0xFFFF8800), // Orange
                Color(0xFFFFFF00), // Yellow
                Color(0xFF00FF00), // Green
                Color(0xFF00FFFF), // Cyan
                Color(0xFF0066FF), // Blue
                Color(0xFF9900FF), // Violet
                Color(0xFFFF00AA), // Pink
                Color(0xFFFF0000)  // Red
            )
        }

        AlertDialog(
            onDismissRequest = { showColorModal = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = NeonGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "$rodName LED Işıkları",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 1. MOD SEÇİM SEKMELERİ (VURUŞ / BOŞA / HIRSIZLIK)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0B0D14))
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ModalTabButton(
                            title = "🐟 Vuruş",
                            dotColor = tempStrikeColor.color,
                            isSelected = modalActiveTab == 0,
                            onClick = { modalActiveTab = 0 },
                            modifier = Modifier.weight(1f)
                        )
                        ModalTabButton(
                            title = "🔻 Boşa",
                            dotColor = tempDropBackColor.color,
                            isSelected = modalActiveTab == 1,
                            onClick = { modalActiveTab = 1 },
                            modifier = Modifier.weight(1f)
                        )
                        ModalTabButton(
                            title = "🚨 Hırsız",
                            dotColor = tempTheftColor.color,
                            isSelected = modalActiveTab == 2,
                            onClick = { modalActiveTab = 2 },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // 2. CANLI LED & RENK ÖNİZLEME HALKASI
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .shadow(16.dp, CircleShape, spotColor = activeSelectedColor.color)
                            .clip(CircleShape)
                            .background(Color(0xFF0A0D14))
                            .border(3.dp, activeSelectedColor.color, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(activeSelectedColor.color)
                        )
                    }

                    Text(
                        text = when (modalActiveTab) {
                            1 -> "🔻 Boşa Düşme Flaşör Rengi"
                            2 -> "🚨 Hırsızlık Alarmı Flaşör Rengi"
                            else -> "🐟 Balık Vuruşu Flaşör Rengi"
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFFCBD5E1),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    )

                    // 3. HAZIR PROFESYONEL PRESET TONLARI
                    Text(
                        text = "Hazır Renkler",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        ),
                        modifier = Modifier.align(Alignment.Start)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RgbLedColor.PRESETS.forEach { preset ->
                            val isSelected = activeSelectedColor.color == preset.color
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .shadow(if (isSelected) 8.dp else 2.dp, CircleShape, spotColor = preset.color)
                                    .clip(CircleShape)
                                    .background(preset.color)
                                    .border(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) Color.White else Color(0x33FFFFFF),
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        when (modalActiveTab) {
                                            0 -> tempStrikeColor = preset
                                            1 -> tempDropBackColor = preset
                                            2 -> tempTheftColor = preset
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = if (preset == RgbLedColor.PRESET_YELLOW || preset == RgbLedColor.PRESET_WHITE) Color.Black else Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    // 4. KESİNTİSİZ SPEKTRUM KAYDIRICI (GRADIENT COLOR PICKER)
                    Text(
                        text = "Özel Renk Spektrumu",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        ),
                        modifier = Modifier.align(Alignment.Start)
                    )

                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Brush.horizontalGradient(spectrumColors))
                            .border(1.5.dp, Color(0xFF333D54), RoundedCornerShape(18.dp))
                    ) {
                        val density = LocalDensity.current
                        val widthPx = constraints.maxWidth.toFloat()
                        var thumbPositionPx by remember { mutableFloatStateOf(widthPx / 2f) }

                        fun updateColorFromX(x: Float) {
                            val clampedX = x.coerceIn(0f, widthPx)
                            thumbPositionPx = clampedX
                            val fraction = (clampedX / widthPx).coerceIn(0f, 1f)

                            val numSegments = spectrumColors.size - 1
                            val segmentFrac = fraction * numSegments
                            val index = segmentFrac.toInt().coerceIn(0, numSegments - 1)
                            val subFrac = segmentFrac - index

                            val c1 = spectrumColors[index]
                            val c2 = spectrumColors[index + 1]

                            val r = (c1.red + (c2.red - c1.red) * subFrac).coerceIn(0f, 1f)
                            val g = (c1.green + (c2.green - c1.green) * subFrac).coerceIn(0f, 1f)
                            val b = (c1.blue + (c2.blue - c1.blue) * subFrac).coerceIn(0f, 1f)

                            val custom = RgbLedColor(Color(r, g, b), "Özel Renk")
                            when (modalActiveTab) {
                                0 -> tempStrikeColor = custom
                                1 -> tempDropBackColor = custom
                                2 -> tempTheftColor = custom
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                                .pointerInput(Unit) {
                                    detectTapGestures { offset ->
                                        updateColorFromX(offset.x)
                                    }
                                }
                                .pointerInput(Unit) {
                                    detectDragGestures { change, _ ->
                                        change.consume()
                                        updateColorFromX(change.position.x)
                                    }
                                }
                        ) {
                            // Kaydırıcı Başlığı (Thumb)
                            val thumbOffsetDp = with(density) { thumbPositionPx.toDp() } - 14.dp
                            Box(
                                modifier = Modifier
                                    .offset { IntOffset(with(density) { thumbOffsetDp.roundToPx() }, 0) }
                                    .size(28.dp)
                                    .shadow(6.dp, CircleShape, spotColor = Color.Black)
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .border(2.dp, activeSelectedColor.color, CircleShape)
                            )
                        }
                    }

                    // 5. HEX RENK KODU BİLGİSİ
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "HEX Değeri:",
                            style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF94A3B8))
                        )
                        Text(
                            text = activeSelectedColor.hexCode,
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = NeonGreen,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onStrikeColorSelected(tempStrikeColor)
                        onDropBackColorSelected(tempDropBackColor)
                        onTheftColorSelected(tempTheftColor)
                        showColorModal = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("confirm_colors_button_$rodId")
                ) {
                    Text(
                        text = "Kaydet ve Uygula",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                Button(
                    onClick = { showColorModal = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2433)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "Vazgeç",
                        color = Color.White
                    )
                }
            },
            containerColor = Color(0xFF141724),
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun AlarmTypePill(
    title: String,
    dotColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (isSelected) Color(0xFF161C26) else Color(0xFF0F121A),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, if (isSelected) NeonGreen.copy(alpha = 0.8f) else Color(0xFF222B3D)),
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(dotColor)
                    .shadow(if (isSelected) 4.dp else 0.dp, CircleShape, spotColor = dotColor)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = if (isSelected) Color.White else Color(0xFF94A3B8),
                    fontSize = 10.5.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            )
        }
    }
}

@Composable
private fun ModalTabButton(
    title: String,
    dotColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (isSelected) Color(0xFF1E2638) else Color.Transparent,
        shape = RoundedCornerShape(8.dp),
        border = if (isSelected) BorderStroke(1.dp, NeonGreen.copy(alpha = 0.6f)) else null,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(vertical = 7.dp, horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = if (isSelected) Color.White else Color(0xFF94A3B8),
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 11.sp
                )
            )
        }
    }
}
