package com.abhi.madadwala_1.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.dp
import com.abhi.madadwala_1.ui.theme.MadadwalaColors
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun HelperCharacter(
    phoneDigitsCount: Int,
    isFocused: Boolean,
    isSuccess: Boolean = false
) {
    // Breathing animation
    val infiniteTransition = rememberInfiniteTransition(label = "Breathing")
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(1750, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BreathingScale"
    )

    // Hand animation for thumbs up
    val handOffset by animateFloatAsState(
        targetValue = if (isSuccess) 0f else 40f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "HandPop"
    )

    // Blinking animation
    var eyeHeightScale by remember { mutableStateOf(1f) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(Random.nextLong(2500, 5000))
            animate(
                initialValue = 1f,
                targetValue = 0.1f,
                animationSpec = tween(120)
            ) { value, _ -> eyeHeightScale = value }
            animate(
                initialValue = 0.1f,
                targetValue = 1f,
                animationSpec = tween(120)
            ) { value, _ -> eyeHeightScale = value }
        }
    }

    // Look at OTP boxes logic (looking down)
    val pupilShiftX = (phoneDigitsCount * 1.5f).coerceIn(0f, 8f) - 4f
    val pupilShiftY = if (isSuccess) 4f else 2f

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
    ) {
        val centerX = size.width / 2
        val centerY = size.height

        scale(breathingScale, pivot = Offset(centerX, centerY)) {
            // Body/Shoulders
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(centerX - 65.dp.toPx(), centerY - 45.dp.toPx()),
                size = Size(130.dp.toPx(), 90.dp.toPx()),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(24.dp.toPx(), 24.dp.toPx())
            )

            // ID Badge (Professional Detail)
            drawRoundRect(
                color = MadadwalaColors.Green.copy(alpha = 0.1f),
                topLeft = Offset(centerX + 25.dp.toPx(), centerY - 30.dp.toPx()),
                size = Size(20.dp.toPx(), 28.dp.toPx()),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )
            drawRect(
                color = MadadwalaColors.Green,
                topLeft = Offset(centerX + 28.dp.toPx(), centerY - 25.dp.toPx()),
                size = Size(14.dp.toPx(), 2.dp.toPx())
            )

            // Neckline/Collar
            val collarPath = Path().apply {
                moveTo(centerX - 18.dp.toPx(), centerY - 45.dp.toPx())
                lineTo(centerX + 18.dp.toPx(), centerY - 45.dp.toPx())
                lineTo(centerX, centerY - 28.dp.toPx())
                close()
            }
            drawPath(collarPath, MadadwalaColors.Green)

            // Head
            val headRadius = 48.dp.toPx()
            val headCenter = Offset(centerX, centerY - 90.dp.toPx())
            drawCircle(
                color = Color(0xFFFFDBAC), // Skin tone
                radius = headRadius,
                center = headCenter
            )

            // Cap
            drawArc(
                color = MadadwalaColors.Green,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = true,
                topLeft = Offset(headCenter.x - headRadius, headCenter.y - headRadius),
                size = Size(headRadius * 2, headRadius * 2)
            )
            // Cap Brim
            drawOval(
                color = MadadwalaColors.GreenDark,
                topLeft = Offset(headCenter.x - 25.dp.toPx(), headCenter.y - headRadius + 4.dp.toPx()),
                size = Size(70.dp.toPx(), 18.dp.toPx())
            )

            // Eyes - Improved visibility and expression
            val eyeWidth = 10.dp.toPx()
            val eyeHeight = if (isFocused) 4.dp.toPx() else 14.dp.toPx()
            val actualEyeHeight = eyeHeight * eyeHeightScale
            
            val leftEyePos = Offset(headCenter.x - 20.dp.toPx(), headCenter.y - 2.dp.toPx())
            val rightEyePos = Offset(headCenter.x + 10.dp.toPx(), headCenter.y - 2.dp.toPx())

            if (isSuccess && eyeHeightScale > 0.8f) {
                // Joyful eyes for success (arcs)
                val arcPathLeft = Path().apply {
                    addArc(
                        androidx.compose.ui.geometry.Rect(
                            leftEyePos.x, leftEyePos.y - 5.dp.toPx(),
                            leftEyePos.x + eyeWidth, leftEyePos.y + 5.dp.toPx()
                        ),
                        180f, 180f
                    )
                }
                val arcPathRight = Path().apply {
                    addArc(
                        androidx.compose.ui.geometry.Rect(
                            rightEyePos.x, rightEyePos.y - 5.dp.toPx(),
                            rightEyePos.x + eyeWidth, rightEyePos.y + 5.dp.toPx()
                        ),
                        180f, 180f
                    )
                }
                drawPath(arcPathLeft, MadadwalaColors.Ink, style = Stroke(width = 3.dp.toPx()))
                drawPath(arcPathRight, MadadwalaColors.Ink, style = Stroke(width = 3.dp.toPx()))
            } else {
                // Normal eyes
                drawRoundRect(
                    color = MadadwalaColors.Ink,
                    topLeft = Offset(leftEyePos.x, leftEyePos.y - actualEyeHeight / 2),
                    size = Size(eyeWidth, actualEyeHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx(), 6.dp.toPx())
                )
                drawRoundRect(
                    color = MadadwalaColors.Ink,
                    topLeft = Offset(rightEyePos.x, rightEyePos.y - actualEyeHeight / 2),
                    size = Size(eyeWidth, actualEyeHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx(), 6.dp.toPx())
                )

                // Pupils looking down
                if (!isFocused && eyeHeightScale > 0.5f) {
                    drawCircle(
                        color = Color.White,
                        radius = 2.dp.toPx(),
                        center = Offset(leftEyePos.x + eyeWidth / 2 + pupilShiftX, leftEyePos.y + pupilShiftY)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 2.dp.toPx(),
                        center = Offset(rightEyePos.x + eyeWidth / 2 + pupilShiftX, rightEyePos.y + pupilShiftY)
                    )
                }
            }

            // Smile
            drawArc(
                color = MadadwalaColors.Ink,
                startAngle = 20f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(headCenter.x - 18.dp.toPx(), headCenter.y + 12.dp.toPx()),
                size = Size(36.dp.toPx(), 18.dp.toPx()),
                style = Stroke(width = 2.5.dp.toPx())
            )

            // Thumbs Up Hand - Enhanced
            if (isSuccess || handOffset < 40f) {
                val handX = centerX + 55.dp.toPx()
                val handY = centerY - 55.dp.toPx() + handOffset.dp.toPx()
                
                // Hand base (fist)
                drawCircle(
                    color = Color(0xFFFFDBAC),
                    radius = 15.dp.toPx(),
                    center = Offset(handX, handY)
                )
                
                // Thumb pointing up
                val thumbPath = Path().apply {
                    moveTo(handX - 5.dp.toPx(), handY - 10.dp.toPx())
                    quadraticTo(
                        handX - 8.dp.toPx(), handY - 25.dp.toPx(),
                        handX + 2.dp.toPx(), handY - 28.dp.toPx()
                    )
                    lineTo(handX + 8.dp.toPx(), handY - 28.dp.toPx())
                    lineTo(handX + 5.dp.toPx(), handY - 10.dp.toPx())
                    close()
                }
                drawPath(thumbPath, Color(0xFFFFDBAC))
            }
        }
    }
}
