package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import com.example.model.AlarmType
import com.example.model.RgbLedColor
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonGreenLight
import kotlin.math.roundToInt

@Composable
fun RealisticFishingRodView(
    currentSensitivity: Int,
    rodId: Int,
    onSensitivityChanged: (Int) -> Unit,
    isAlarming: Boolean,
    ledColor: RgbLedColor,
    alarmType: AlarmType = AlarmType.NORMAL,
    strikeIntensity: Int = 75,
    modifier: Modifier = Modifier
) {
    // 1. DİNAMİK VURUŞ ŞİDDETİ FAKTÖRÜ (0.15 ile 1.0 arası)
    val intensityFrac = (strikeIntensity / 100f).coerceIn(0.15f, 1.0f)
    val intensitySquared = intensityFrac * intensityFrac

    // DEVASA BÜKÜLME SINIRLARI:
    // %30 Hafif Yoklama: 20f - 32f px (Uç kısmın hafifçe yaylanması)
    // %65 Orta Çekiş: 52f - 78f px (Derin, tok ve belirgin kavis)
    // %95 Canavar Asılması: 95f - 140f px! (Kamış ikiye katlanır, tepe suya doğru 40px çöker!)
    val baseMinBend = 12f + (85f * intensitySquared)
    val baseMaxBend = 20f + (122f * intensitySquared)

    val bendTransition = rememberInfiniteTransition(label = "RodBendingPhysics")

    // Ana bükülme animasyonu (Sert vuruşta daha hızlı ve agresif döngü)
    val deepBendAmount by bendTransition.animateFloat(
        initialValue = baseMinBend,
        targetValue = baseMaxBend,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (400 - (180 * intensityFrac)).toInt().coerceIn(170, 400),
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "DeepBend"
    )

    // Agresif Kafa Darbeleri (Head-shake shudder) - %95 vuruşta güçlü sarsıntı
    val headShakeShudder by bendTransition.animateFloat(
        initialValue = -3.5f * intensityFrac,
        targetValue = 6.5f * intensityFrac,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 105, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "HeadShakeShudder"
    )

    // Boşa düşme durumundaki misina gevşeme salınımı (Slack vibration)
    val slackTremble by bendTransition.animateFloat(
        initialValue = -4.0f,
        targetValue = 4.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 95, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "SlackTremble"
    )

    // 2. Alarm sensör flaşörü
    val strobePulse by bendTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (alarmType == AlarmType.DROP_BACK) 130 else (220 - (80 * intensityFrac)).toInt(),
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "StrobePulse"
    )

    // 3. Yumuşak hassasiyet animasyonu
    val levelFraction = ((currentSensitivity - 1) / 9f).coerceIn(0f, 1f)
    val animatedFraction by animateFloatAsState(
        targetValue = levelFraction,
        animationSpec = tween(durationMillis = 150),
        label = "AnimatedSensitivityFraction"
    )

    val activeColor = if (alarmType == AlarmType.DROP_BACK) Color(0xFFFFEA00) else ledColor.color

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
    ) {
        val totalWidth = constraints.maxWidth.toFloat()
        val totalHeight = constraints.maxHeight.toFloat()

        // Sınırlar (Kaydırma için)
        val topLimitY = totalHeight * 0.22f
        val bottomLimitY = totalHeight * 0.73f
        val travelDistance = (bottomLimitY - topLimitY).coerceAtLeast(1f)

        var dragProgress by remember(currentSensitivity) { mutableFloatStateOf(levelFraction) }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("interactive_rod_canvas_$rodId")
                .pointerInput(rodId) {
                    detectTapGestures { offset ->
                        val tapY = offset.y.coerceIn(topLimitY, bottomLimitY)
                        val fraction = (1f - ((tapY - topLimitY) / travelDistance)).coerceIn(0f, 1f)
                        val newLevel = (1 + (fraction * 9f).roundToInt()).coerceIn(1, 10)
                        if (newLevel != currentSensitivity) {
                            onSensitivityChanged(newLevel)
                        }
                    }
                }
                .pointerInput(rodId) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val currentY = offset.y.coerceIn(topLimitY, bottomLimitY)
                            dragProgress = (1f - ((currentY - topLimitY) / travelDistance)).coerceIn(0f, 1f)
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val deltaFraction = -dragAmount.y / travelDistance
                            val newProgress = (dragProgress + deltaFraction).coerceIn(0f, 1f)
                            dragProgress = newProgress
                            val newLevel = (1 + (newProgress * 9f).roundToInt()).coerceIn(1, 10)
                            if (newLevel != currentSensitivity) {
                                onSensitivityChanged(newLevel)
                            }
                        }
                    )
                }
        ) {
            val w = size.width
            val h = size.height

            // Kamışın taban merkezi sol tarafa yaslanır (sağa doğru devasa kavis çizebilmesi için)
            val centerX = w * 0.28f

            val tipY = h * 0.04f
            val baseY = h * 0.94f

            // DEVASA BÜKÜLME FORMÜLÜ (Parabolik Karbon Gövde Aksiyonu):
            fun getRodOffset(relY: Float): Offset {
                if (!isAlarming) {
                    return Offset(centerX, relY)
                }
                val u = ((baseY - relY) / (baseY - tipY)).coerceIn(0f, 1f)

                // Bükülmenin başladığı nokta:
                // %30 hafif vuruşta: u > 0.44 (orta-üst kısım)
                // %65 orta vuruşta: u > 0.32 (alt-orta gövde)
                // %95 canavar vuruşta: u > 0.23 (hemen makara yatağının üzeri, tüm kamış ikiye katlanır!)
                val bendAnchorU = 0.23f + (0.28f * (1f - intensityFrac))
                if (u <= bendAnchorU) {
                    return Offset(centerX, relY)
                }

                if (alarmType == AlarmType.DROP_BACK) {
                    val normU = (u - bendAnchorU) / (1f - bendAnchorU)
                    val wiggle = slackTremble * normU
                    return Offset(centerX + wiggle, relY)
                }

                // Normal vuruş: Parabolik Bézier bükülme + kafa atma sarsıntısı
                val normU = (u - bendAnchorU) / (1f - bendAnchorU)
                val curvePower = normU * normU * normU * 0.55f + normU * normU * 0.45f
                val totalBendX = (deepBendAmount + headShakeShudder) * curvePower
                val totalDropY = (deepBendAmount * 0.28f * intensityFrac) * curvePower
                return Offset(centerX + totalBendX, relY + totalDropY)
            }

            // 1. CARBON FIBER BLANK (İkiye Katlanan Karbon Kamış)
            val segments = 28
            val leftPoints = mutableListOf<Offset>()
            val rightPoints = mutableListOf<Offset>()

            for (i in 0..segments) {
                val stepFrac = i / segments.toFloat()
                val currentRelY = tipY + (stepFrac * (baseY - tipY))
                val centerOffset = getRodOffset(currentRelY)

                val halfThickness = 3.5f + (stepFrac * 6f)
                leftPoints.add(Offset(centerOffset.x - halfThickness, centerOffset.y))
                rightPoints.add(Offset(centerOffset.x + halfThickness, centerOffset.y))
            }

            val blankPath = Path().apply {
                if (leftPoints.isNotEmpty()) {
                    moveTo(leftPoints.first().x, leftPoints.first().y)
                    for (i in 1 until leftPoints.size) {
                        lineTo(leftPoints[i].x, leftPoints[i].y)
                    }
                    for (i in rightPoints.indices.reversed()) {
                        lineTo(rightPoints[i].x, rightPoints[i].y)
                    }
                    close()
                }
            }

            val maxCurvedX = getRodOffset(tipY).x
            val carbonBrush = Brush.horizontalGradient(
                colors = listOf(
                    Color(0xFF0F1218),
                    Color(0xFF262C38),
                    Color(0xFF3E4759),
                    Color(0xFF1B202B),
                    Color(0xFF0B0D12)
                ),
                startX = centerX - 14f,
                endX = maxOf(centerX + 24f, maxCurvedX + 10f)
            )
            drawPath(blankPath, brush = carbonBrush)

            // Kamış boğum halkaları
            val ferrulePositions = listOf(0.20f, 0.42f, 0.64f)
            for (fPos in ferrulePositions) {
                val fy = h * fPos
                val fCenter = getRodOffset(fy)
                val fWidth = 9f + (fPos * 7f)
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color(0xFFD97706), Color(0xFFFDE68A), Color(0xFF92400E))
                    ),
                    topLeft = Offset(fCenter.x - fWidth / 2, fCenter.y - 2.5f),
                    size = Size(fWidth, 5f),
                    cornerRadius = CornerRadius(1.5f, 1.5f)
                )
            }

            // 2. FUJI FUJİ PORSELEN HALKALARI (Bükülen Kavis Boyunca Takip Eder)
            val guidePositions = listOf(0.10f, 0.22f, 0.36f, 0.52f, 0.68f)
            val guideCenters = mutableListOf<Offset>()

            for (pos in guidePositions) {
                val gy = h * pos
                val gCenter = getRodOffset(gy)
                guideCenters.add(gCenter)
                val ringRadius = 6f + (pos * 5.5f)

                // Chrome frame
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFFFFFF), Color(0xFF94A3B8), Color(0xFF334155))
                    ),
                    radius = ringRadius,
                    center = Offset(gCenter.x + ringRadius + 3f, gCenter.y),
                    style = Stroke(width = 2.4f)
                )
                // Ceramic insert
                drawCircle(
                    color = Color(0xFF0F172A),
                    radius = ringRadius - 2f,
                    center = Offset(gCenter.x + ringRadius + 3f, gCenter.y),
                    style = Stroke(width = 1.5f)
                )
                // Thread wrap
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFFB45309), Color(0xFFFBBF24), Color(0xFF78350F))
                    ),
                    topLeft = Offset(gCenter.x - 4f, gCenter.y - 4f),
                    size = Size(10f, 8f),
                    cornerRadius = CornerRadius(1.5f, 1.5f)
                )
            }

            // 3. TEPE PORSELEN HALKASI
            val curvedTip = getRodOffset(tipY)
            drawCircle(
                color = Color(0xFFE2E8F0),
                radius = 5.5f,
                center = Offset(curvedTip.x + 6f, curvedTip.y),
                style = Stroke(width = 2.4f)
            )

            // 4. GERGİN MİSİNA (Bükülmeyle Yay Gibi Gerilir)
            val linePath = Path().apply {
                moveTo(curvedTip.x + 6f, curvedTip.y)
                for (i in guidePositions.indices) {
                    val gPos = guidePositions[i]
                    val gCenter = guideCenters[i]
                    val ringRadius = 6f + (gPos * 5.5f)
                    lineTo(gCenter.x + ringRadius + 3f, gCenter.y)
                }
                lineTo(centerX + 18f, h * 0.82f)
            }
            drawPath(
                path = linePath,
                color = if (isAlarming) activeColor.copy(alpha = 0.95f) else Color(0x99A7F3D0),
                style = Stroke(width = if (isAlarming) (1.4f + (1.3f * intensityFrac)) else 1.4f)
            )

            // 5. NEON YEŞİLİ IŞIK ÇİZGİSİ (Bükülen Kamışın Omurgası Boyunca)
            val ringRelTargetY = bottomLimitY - (animatedFraction * travelDistance)
            val ringCenter = getRodOffset(ringRelTargetY)

            val beamSegments = 18
            val beamLeft = mutableListOf<Offset>()
            val beamRight = mutableListOf<Offset>()

            for (i in 0..beamSegments) {
                val frac = i / beamSegments.toFloat()
                val currentY = ringRelTargetY + (frac * (bottomLimitY - ringRelTargetY))
                val centerPt = getRodOffset(currentY)
                val halfW = 4f
                beamLeft.add(Offset(centerPt.x - halfW, centerPt.y))
                beamRight.add(Offset(centerPt.x + halfW, centerPt.y))
            }

            val beamPath = Path().apply {
                if (beamLeft.isNotEmpty()) {
                    moveTo(beamLeft.first().x, beamLeft.first().y)
                    for (pt in beamLeft) lineTo(pt.x, pt.y)
                    for (i in beamRight.indices.reversed()) lineTo(beamRight[i].x, beamRight[i].y)
                    close()
                }
            }

            drawPath(
                path = beamPath,
                brush = Brush.verticalGradient(
                    colors = listOf(NeonGreen, NeonGreenLight, Color(0xFF059669)),
                    startY = ringCenter.y,
                    endY = bottomLimitY
                )
            )

            // 6. BÜYÜTÜLMÜŞ 3D METALİK ERGONOMİK AYAR TOPUZU
            val ringOuterRadius = 38f

            drawCircle(
                color = Color(0x88000000),
                radius = ringOuterRadius + 4f,
                center = ringCenter + Offset(0f, 4f)
            )

            drawCircle(
                color = NeonGreen.copy(alpha = 0.40f),
                radius = ringOuterRadius + 14f,
                center = ringCenter
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFFFFFF),
                        Color(0xFFE2E8F0),
                        Color(0xFF94A3B8),
                        Color(0xFF475569),
                        Color(0xFF1E293B)
                    ),
                    center = ringCenter - Offset(4f, 4f),
                    radius = ringOuterRadius
                ),
                radius = ringOuterRadius,
                center = ringCenter
            )

            drawCircle(
                color = Color(0xFF0F172A),
                radius = ringOuterRadius,
                center = ringCenter,
                style = Stroke(width = 2.5f)
            )

            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color(0xFFCBD5E1),
                        Color(0xFF64748B),
                        Color(0xFFCBD5E1),
                        Color(0xFF475569),
                        Color(0xFFCBD5E1)
                    ),
                    center = ringCenter
                ),
                radius = ringOuterRadius - 6f,
                center = ringCenter,
                style = Stroke(width = 3.5f)
            )

            drawCircle(
                color = Color(0xFF090B10),
                radius = ringOuterRadius - 12f,
                center = ringCenter
            )
            drawCircle(
                color = Color(0xFF222B3D),
                radius = ringOuterRadius - 12f,
                center = ringCenter,
                style = Stroke(width = 1.5f)
            )

            val jewelRadius = 15f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFFFFFF),
                        NeonGreenLight,
                        NeonGreen,
                        Color(0xFF047857)
                    ),
                    center = ringCenter - Offset(2f, 2f),
                    radius = jewelRadius
                ),
                radius = jewelRadius,
                center = ringCenter
            )

            drawCircle(
                color = Color.White.copy(alpha = 0.85f),
                radius = 3.2f,
                center = ringCenter - Offset(4.5f, 4.5f)
            )

            // 7. SENSOR MODULE (Kamışla Birlikte Bükülür)
            val sensorOffset = getRodOffset(h * 0.16f)
            val sensorBoxWidth = 28f
            val sensorBoxHeight = 38f

            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF1E2430), Color(0xFF0F1218), Color(0xFF1E2430))
                ),
                topLeft = Offset(sensorOffset.x - sensorBoxWidth / 2, sensorOffset.y - sensorBoxHeight / 2),
                size = Size(sensorBoxWidth, sensorBoxHeight),
                cornerRadius = CornerRadius(5f, 5f)
            )
            drawRoundRect(
                color = Color(0xFF3B4455),
                topLeft = Offset(sensorOffset.x - sensorBoxWidth / 2, sensorOffset.y - sensorBoxHeight / 2),
                size = Size(sensorBoxWidth, sensorBoxHeight),
                cornerRadius = CornerRadius(5f, 5f),
                style = Stroke(width = 1.2f)
            )

            val ledOffsetLeft = Offset(sensorOffset.x - 7f, sensorOffset.y - 8f)
            val ledOffsetRight = Offset(sensorOffset.x + 7f, sensorOffset.y - 8f)

            if (isAlarming) {
                drawCircle(
                    color = activeColor.copy(alpha = 0.5f * strobePulse),
                    radius = 16f * strobePulse,
                    center = ledOffsetLeft
                )
                drawCircle(
                    color = activeColor.copy(alpha = 0.5f * strobePulse),
                    radius = 16f * strobePulse,
                    center = ledOffsetRight
                )
                drawCircle(color = activeColor, radius = 5f, center = ledOffsetLeft)
                drawCircle(color = activeColor, radius = 5f, center = ledOffsetRight)
                drawCircle(color = Color.White, radius = 2.2f, center = ledOffsetLeft)
                drawCircle(color = Color.White, radius = 2.2f, center = ledOffsetRight)
            } else {
                drawCircle(color = activeColor.copy(alpha = 0.8f), radius = 4f, center = ledOffsetLeft)
                drawCircle(color = activeColor.copy(alpha = 0.8f), radius = 4f, center = ledOffsetRight)
            }

            drawRoundRect(
                color = Color(0xFF090A0D),
                topLeft = Offset(sensorOffset.x - 6f, sensorOffset.y + 4f),
                size = Size(12f, 11f),
                cornerRadius = CornerRadius(2f, 2f)
            )

            // 8. NATURAL CORK HANDLE (Sabit Sehpa Mantarı)
            val handleStartY = h * 0.84f
            val handleHeight = h * 0.13f
            val handleWidth = 20f

            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF854D0E),
                        Color(0xFFB45309),
                        Color(0xFFD97706),
                        Color(0xFFFDE68A),
                        Color(0xFFB45309),
                        Color(0xFF78350F)
                    )
                ),
                topLeft = Offset(centerX - handleWidth / 2, handleStartY),
                size = Size(handleWidth, handleHeight),
                cornerRadius = CornerRadius(4f, 4f)
            )

            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color(0xFF334155), Color(0xFF94A3B8), Color(0xFF334155))
                ),
                topLeft = Offset(centerX - handleWidth / 2 - 1f, handleStartY + handleHeight - 4f),
                size = Size(handleWidth + 2f, 7f),
                cornerRadius = CornerRadius(2f, 2f)
            )

            // 9. REEL & SEAT
            val reelSeatY = h * 0.80f

            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color(0xFF334155), Color(0xFFE2E8F0), Color(0xFF1E293B))
                ),
                topLeft = Offset(centerX - 9f, reelSeatY - 10f),
                size = Size(18f, 16f),
                cornerRadius = CornerRadius(3f, 3f)
            )

            drawRoundRect(
                color = Color(0xFF1E2330),
                topLeft = Offset(centerX + 7f, reelSeatY - 6f),
                size = Size(14f, 12f),
                cornerRadius = CornerRadius(3f, 3f)
            )

            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color(0xFFB45309), Color(0xFFFBBF24), Color(0xFFD97706))
                ),
                topLeft = Offset(centerX + 18f, reelSeatY - 14f),
                size = Size(20f, 24f),
                cornerRadius = CornerRadius(3f, 3f)
            )
        }
    }
}
