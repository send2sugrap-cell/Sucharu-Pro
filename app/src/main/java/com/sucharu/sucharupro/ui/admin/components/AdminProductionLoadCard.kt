package com.sucharu.sucharupro.ui.admin.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.ui.navigation.AppDestination

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
 * Production Load Progress Bar & Breakdown Widget matching Image 2.
 */
@Composable
fun AdminProductionLoadCard(
    modifier: Modifier = Modifier,
    totalJobs: Int = 63,
    runningJobs: Int = 28,
    completeJobs: Int = 18,
    preparingJobs: Int = 12,
    delayedJobs: Int = 5,
    onNavigateToDestination: (AppDestination) -> Unit = {}
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF0F172A),
        border = BorderStroke(1.dp, Color(0xFF00B4D8).copy(alpha = 0.4f)),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onNavigateToDestination(AppDestination.Staff.Production) }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00B4D8).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Engineering, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(text = "প্রোডাকশন লোড", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                        Text(text = "প্রিন্টিং এখন চলমান", fontSize = 9.5.sp, color = Color(0xFF94A3B8))
                    }
                }

                Text(text = "বিস্তারিত >", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "মোট কাজ", fontSize = 10.sp, color = Color(0xFF94A3B8))
                    Text(text = totalJobs.toBengaliDigits(), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                }

                // Segmented Multi-Color Progress Bar
                Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(Color(0xFF1E293B))
                    ) {
                        Row(modifier = Modifier.fillMaxHeight()) {
                            val total = totalJobs.coerceAtLeast(1).toFloat()
                            Box(modifier = Modifier.weight(runningJobs / total).fillMaxHeight().background(Color(0xFF10B981)))
                            Box(modifier = Modifier.weight(completeJobs / total).fillMaxHeight().background(Color(0xFF3B82F6)))
                            Box(modifier = Modifier.weight(preparingJobs / total).fillMaxHeight().background(Color(0xFFF59E0B)))
                            Box(modifier = Modifier.weight(delayedJobs / total).fillMaxHeight().background(Color(0xFFEF4444)))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Breakdown Status Badges
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF10B981)))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "চলমান", fontSize = 9.5.sp, color = Color.White)
                            }
                            Text(text = runningJobs.toBengaliDigits(), fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF3B82F6)))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "সম্পন্ন", fontSize = 9.5.sp, color = Color.White)
                            }
                            Text(text = completeJobs.toBengaliDigits(), fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
