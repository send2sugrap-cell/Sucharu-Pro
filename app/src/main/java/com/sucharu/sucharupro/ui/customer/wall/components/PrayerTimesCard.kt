package com.sucharu.sucharupro.ui.customer.wall.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.data.prayer.PrayerTimesCalculator
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Premium Live Prayer Times Utility Widget (নামাজের সময়সূচি).
 * Automatically highlights ONLY the current active Waqt based on astronomical local time calculations
 * and updates countdown continuously.
 */
@Composable
fun PrayerTimesCard(
    modifier: Modifier = Modifier,
    latitude: Double = 23.8103,
    longitude: Double = 90.4125,
    onCalendarClick: () -> Unit = {}
) {
    val schedule = remember(latitude, longitude) {
        PrayerTimesCalculator.calculateSchedule(latitude = latitude, longitude = longitude)
    }

    var liveState by remember(schedule) {
        mutableStateOf(PrayerTimesCalculator.determineLiveWaqtState(schedule))
    }

    // Continuous 1-second live ticker
    LaunchedEffect(schedule) {
        while (isActive) {
            liveState = PrayerTimesCalculator.determineLiveWaqtState(schedule)
            delay(1000L)
        }
    }

    val prayerTimes = listOf(
        "ফজর" to schedule.fajrFormatted,
        "যোহর" to schedule.dhuhrFormatted,
        "আসর" to schedule.asrFormatted,
        "মাগরিব" to schedule.maghribFormatted,
        "এশা" to schedule.ishaFormatted
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B132B)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00B4D8).copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Mosque Icon & Dynamic Current Waqt Highlight Box
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1.1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF00B4D8).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mosque,
                        contentDescription = "Prayer Widget",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "এখন ",
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8)
                        )
                        Text(
                            text = "${liveState.currentWaqtName} ${liveState.currentWaqtTimeFormatted}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981)
                        )
                    }
                    Text(
                        text = "পরবর্তী: ${liveState.nextWaqtName} ${liveState.nextWaqtTimeFormatted}",
                        fontSize = 9.sp,
                        color = Color(0xFFCBD5E1)
                    )
                    Text(
                        text = "বাকি: ${liveState.remainingCountdownText}",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8)
                    )
                }
            }

            // Center: 5 Waqts Chips - ONLY CURRENT WAQT GETS GREEN HIGHLIGHT
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(2f)
            ) {
                prayerTimes.forEach { (name, time) ->
                    val isCurrent = name == liveState.currentWaqtName
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isCurrent) Color(0xFF047857) else Color(0xFF1E293B))
                            .border(1.dp, if (isCurrent) Color(0xFF10B981) else Color(0xFF334155), RoundedCornerShape(6.dp))
                            .padding(vertical = 4.dp, horizontal = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = name,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCurrent) Color.White else Color(0xFF94A3B8)
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                text = time,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isCurrent) Color.White else Color(0xFFCBD5E1)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Right: Full Schedule Action Button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0284C7).copy(alpha = 0.2f))
                    .border(1.dp, Color(0xFF0284C7), RoundedCornerShape(8.dp))
                    .clickable { onCalendarClick() }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "সময়সূচী >",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8)
                    )
                }
            }
        }
    }
}
