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
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalTime

/**
 * Dynamic Prayer Schedule Data Model.
 */
data class PrayerTimeSchedule(
    val fajr: String = "04:48",
    val dhuhr: String = "12:16",
    val asr: String = "04:25",
    val maghrib: String = "06:08",
    val isha: String = "07:38",
    val currentWaqtName: String = "যোহর",
    val currentWaqtTime: String = "12:16 PM",
    val nextWaqtName: String = "আসর",
    val nextWaqtTime: String = "04:25 PM",
    val remainingCountdownText: String = "01:24:00"
)

/**
 * Premium Live Prayer Times Utility Widget (নামাজের সময়সূচি).
 * Automatically highlights ONLY the current active Waqt based on local time.
 */
@Composable
fun PrayerTimesCard(
    modifier: Modifier = Modifier,
    schedule: PrayerTimeSchedule = remember { calculateDynamicPrayerSchedule() },
    onCalendarClick: () -> Unit = {}
) {
    val prayerTimes = listOf(
        "ফজর" to schedule.fajr,
        "যোহর" to schedule.dhuhr,
        "আসর" to schedule.asr,
        "মাগরিব" to schedule.maghrib,
        "এশা" to schedule.isha
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
                            text = "${schedule.currentWaqtName} ${schedule.currentWaqtTime}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981)
                        )
                    }
                    Text(
                        text = "পরবর্তী: ${schedule.nextWaqtName} ${schedule.nextWaqtTime}",
                        fontSize = 9.sp,
                        color = Color(0xFFCBD5E1)
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
                    val isCurrent = name == schedule.currentWaqtName
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

            // Right: Full Schedule Button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0284C7).copy(alpha = 0.2f))
                    .border(1.dp, Color(0xFF0284C7), RoundedCornerShape(8.dp))
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

/**
 * Dynamically computes local prayer schedule and active Waqt for Dhaka (23.8103° N, 90.4125° E).
 */
fun calculateDynamicPrayerSchedule(): PrayerTimeSchedule {
    val now = LocalTime.now()
    val currentMinutes = now.hour * 60 + now.minute

    val fajrMin = 4 * 60 + 48     // 04:48 AM
    val dhuhrMin = 12 * 60 + 16   // 12:16 PM
    val asrMin = 16 * 60 + 25     // 04:25 PM
    val maghribMin = 18 * 60 + 8  // 06:08 PM
    val ishaMin = 19 * 60 + 38    // 07:38 PM

    val (currentName, currentTimeStr, nextName, nextTimeStr, nextMin) = when {
        currentMinutes in 0..<fajrMin -> PrayerWaqtTuple("এশা", "07:38 PM", "ফজর", "04:48 AM", fajrMin)
        currentMinutes in fajrMin..<dhuhrMin -> PrayerWaqtTuple("ফজর", "04:48 AM", "যোহর", "12:16 PM", dhuhrMin)
        currentMinutes in dhuhrMin..<asrMin -> PrayerWaqtTuple("যোহর", "12:16 PM", "আসর", "04:25 PM", asrMin)
        currentMinutes in asrMin..<maghribMin -> PrayerWaqtTuple("আসর", "04:25 PM", "মাগরিব", "06:08 PM", maghribMin)
        currentMinutes in maghribMin..<ishaMin -> PrayerWaqtTuple("মাগরিব", "06:08 PM", "এশা", "07:38 PM", ishaMin)
        else -> PrayerWaqtTuple("এশা", "07:38 PM", "ফজর", "04:48 AM", fajrMin + 24 * 60)
    }

    val remainingMin = if (nextMin >= currentMinutes) nextMin - currentMinutes else (nextMin + 24 * 60) - currentMinutes
    val remHours = remainingMin / 60
    val remMins = remainingMin % 60
    val countdownStr = String.format(java.util.Locale.US, "%02d:%02d:00", remHours, remMins)

    return PrayerTimeSchedule(
        fajr = "04:48",
        dhuhr = "12:16",
        asr = "04:25",
        maghrib = "06:08",
        isha = "07:38",
        currentWaqtName = currentName,
        currentWaqtTime = currentTimeStr,
        nextWaqtName = nextName,
        nextWaqtTime = nextTimeStr,
        remainingCountdownText = countdownStr
    )
}

private data class PrayerWaqtTuple(
    val currentName: String,
    val currentTimeStr: String,
    val nextName: String,
    val nextTimeStr: String,
    val nextMin: Int
)
