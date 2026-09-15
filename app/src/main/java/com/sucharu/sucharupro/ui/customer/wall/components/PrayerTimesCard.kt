package com.sucharu.sucharupro.ui.customer.wall.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
 * Premium Live Prayer Times Utility Widget (নামাজের সময়সূচি).
 * Deep teal/navy identity with active Waqt highlight and progress indicator.
 */
@Composable
fun PrayerTimesCard(
    modifier: Modifier = Modifier,
    currentWaqt: String = "যোহর",
    currentWaqtTime: String = "১২:০৫ PM",
    nextWaqt: String = "আসর",
    nextWaqtTime: String = "৪:২৫ PM",
    remainingTimeText: String = "২ ঘণ্টা ৩১ মিনিট বাকি"
) {
    val prayerTimes = listOf(
        "ফজর" to "৪:৩৫ AM",
        "যোহর" to "১২:০৫ PM",
        "আসর" to "৪:২৫ PM",
        "মাগরিব" to "৬:১০ PM",
        "এশা" to "৭:২৫ PM"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0F766E).copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header
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
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "আজকের নামাজের সময়সূচি (ঢাকা)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF059669))
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "চলমান: $currentWaqt",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF059669)
                    )
                }
            }

            // 5 Waqts Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                prayerTimes.forEach { (name, time) ->
                    val isCurrent = name == currentWaqt
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isCurrent) Color(0xFFCCFBF1) else Color(0xFFF8FAFC))
                            .border(1.dp, if (isCurrent) Color(0xFF0F766E) else Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                            .padding(vertical = 8.dp, horizontal = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = name,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCurrent) Color(0xFF0F766E) else Color(0xFF475569)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = time,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isCurrent) Color(0xFF115E59) else Color(0xFF64748B)
                            )
                        }
                    }
                }
            }
        }
    }
}
