package com.sucharu.sucharupro.ui.admin.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private fun Int.toBengaliDigits(): String {
    val enDigits = arrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')
    val bnDigits = arrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
    var str = this.toString()
    for (i in 0..9) {
        str = str.replace(enDigits[i], bnDigits[i])
    }
    return str
}

/**
 * Custom High-Tech 24-Hour Sales Trend Line Graph matching reference design.
 */
@Composable
fun AdminSalesTrendChart(
    modifier: Modifier = Modifier,
    lineColor: Color = Color(0xFF00B4D8),
    glowColor: Color = Color(0xFF38BDF8)
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(130.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Normalized point coordinates for 24-hour curve
            val points = listOf(
                Offset(0f, height * 0.85f),
                Offset(width * 0.15f, height * 0.70f),
                Offset(width * 0.35f, height * 0.65f),
                Offset(width * 0.50f, height * 0.25f),
                Offset(width * 0.68f, height * 0.15f),
                Offset(width * 0.85f, height * 0.55f),
                Offset(width, height * 0.80f)
            )

            // Draw Area Fill Gradient
            val fillPath = Path().apply {
                moveTo(points.first().x, points.first().y)
                for (i in 0 until points.size - 1) {
                    val p1 = points[i]
                    val p2 = points[i + 1]
                    val cx = (p1.x + p2.x) / 2f
                    cubicTo(cx, p1.y, cx, p2.y, p2.x, p2.y)
                }
                lineTo(width, height)
                lineTo(0f, height)
                close()
            }

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(glowColor.copy(alpha = 0.35f), Color.Transparent)
                )
            )

            // Draw Smooth Curve Line
            val strokePath = Path().apply {
                moveTo(points.first().x, points.first().y)
                for (i in 0 until points.size - 1) {
                    val p1 = points[i]
                    val p2 = points[i + 1]
                    val cx = (p1.x + p2.x) / 2f
                    cubicTo(cx, p1.y, cx, p2.y, p2.x, p2.y)
                }
            }

            drawPath(
                path = strokePath,
                color = lineColor,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            // Draw Highlight Nodes
            points.forEach { pt ->
                drawCircle(
                    color = Color(0xFF070E1E),
                    radius = 5.dp.toPx(),
                    center = pt
                )
                drawCircle(
                    color = glowColor,
                    radius = 3.5.dp.toPx(),
                    center = pt
                )
            }
        }
    }
}

/**
 * Custom 72% Order Completion Donut Chart matching reference design.
 */
@Composable
fun AdminDonutCompletionChart(
    modifier: Modifier = Modifier,
    percentage: Int = 72,
    activeColor: Color = Color(0xFF00B4D8),
    trackColor: Color = Color(0xFF1E293B)
) {
    Box(
        modifier = modifier.size(110.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 10.dp.toPx()
            val sweepAngle = (percentage / 100f) * 360f

            // Draw Track Circle
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Draw Active Arc
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(activeColor, Color(0xFF38BDF8), activeColor)
                ),
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${percentage.toBengaliDigits()}%",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Text(
                text = "কাজ সম্পন্ন",
                fontSize = 8.5.sp,
                color = Color(0xFF94A3B8)
            )
        }
    }
}
