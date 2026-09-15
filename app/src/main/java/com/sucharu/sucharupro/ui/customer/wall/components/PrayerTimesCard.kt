package com.sucharu.sucharupro.ui.customer.wall.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Right Column Prayer Times Card (Matching Hand-Drawn Blueprint).
 * Shows current Waqt (যোহর), next Waqt (আসর), live progress bar, and remaining time.
 */
@Composable
fun PrayerTimesCard(
    modifier: Modifier = Modifier,
    currentWaqt: String = "যোহর",
    currentWaqtTime: String = "১১:৫৪ মি.",
    nextWaqt: String = "আসর",
    nextWaqtTime: String = "৪:১৯ মি.",
    remainingTime: String = "২ ঘণ্টা ৩১ মিনিট বাকি",
    progress: Float = 0.65f
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Header: Sun Icon & Times
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.WbSunny,
                        contentDescription = null,
                        tint = Color(0xFFD97706),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$currentWaqt $currentWaqtTime",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                }

                Text(
                    text = "$nextWaqt $nextWaqtTime",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B)
                )
            }

            // Current Waqt Name
            Text(
                text = currentWaqt,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0284C7)
            )

            // Progress Bar
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = Color(0xFF16A34A),
                trackColor = Color(0xFFE2E8F0)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "১১:৫৪ - ০৪:১৮",
                    fontSize = 9.sp,
                    color = Color(0xFF64748B)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF16A34A))
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "চলমান",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF16A34A)
                    )
                }
            }

            // Remaining Time Badge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF1F5F9), RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    text = remainingTime,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF334155)
                )
            }
        }
    }
}
