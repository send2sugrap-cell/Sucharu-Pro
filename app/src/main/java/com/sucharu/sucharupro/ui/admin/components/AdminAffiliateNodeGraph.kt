package com.sucharu.sucharupro.ui.admin.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

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
 * Custom Futuristic Affiliate Orbital Network Node Graph matching Image 1 & Image 2.
 */
@Composable
fun AdminAffiliateNodeGraph(
    modifier: Modifier = Modifier,
    partnerCount: Int = 28,
    lineColor: Color = Color(0xFF00B4D8),
    centerColor: Color = Color(0xFF0284C7)
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val orbitRadius = size.height * 0.38f

            // Draw Orbit Ring Line
            drawCircle(
                color = lineColor.copy(alpha = 0.25f),
                radius = orbitRadius,
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 1.5.dp.toPx(),
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )
            )

            // Draw 8 Satellite Node Connection Lines & Circles
            val satelliteCount = 8
            for (i in 0 until satelliteCount) {
                val angle = Math.toRadians((i * (360.0 / satelliteCount) - 90.0))
                val sx = center.x + (orbitRadius * cos(angle)).toFloat()
                val sy = center.y + (orbitRadius * sin(angle)).toFloat()
                val satPoint = Offset(sx, sy)

                // Connecting Ray Line
                drawLine(
                    color = lineColor.copy(alpha = 0.4f),
                    start = center,
                    end = satPoint,
                    strokeWidth = 1.2.dp.toPx()
                )

                // Outer Satellite Node Pulse
                drawCircle(
                    color = lineColor.copy(alpha = 0.3f),
                    radius = 12.dp.toPx(),
                    center = satPoint
                )

                // Satellite Node Core
                drawCircle(
                    color = Color(0xFF0F172A),
                    radius = 9.dp.toPx(),
                    center = satPoint
                )
                drawCircle(
                    color = lineColor,
                    radius = 7.dp.toPx(),
                    center = satPoint
                )
            }
        }

        // Central Hub Node Box
        Box(
            modifier = Modifier
                .size(70.dp)
                .clip(CircleShape)
                .background(centerColor)
                .border(2.dp, Color(0xFF38BDF8), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Group,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = partnerCount.toBengaliDigits(),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    text = "সক্রিয় পার্টনার",
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE0F2FE)
                )
            }
        }
    }
}
