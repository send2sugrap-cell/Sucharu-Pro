package com.sucharu.sucharupro.ui.customer.wall.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.R
import com.sucharu.sucharupro.data.prayer.PrayerTimesCalculator
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Premium Live Prayer & Fasting Times Utility Widget (Hanafi Madhhab Standard).
 *
 * Compact 2-Row Grid Layout with reduced line spacing and tight vertical padding:
 * - Far-Left Artistic Mosque Image (R.drawable.ic_prayer_art)
 * - Left-Aligned Waqt & Countdown Info ("এখন", Waqt Name, "সময় শেষ হতে বাকি", "00:00:00")
 * - Top Row: 5 Prayer Waqts Chips (Fajr, Dhuhr, Asr, Maghrib, Isha) with compact line spacing
 * - Bottom Row: Sahri Start, Vertical Divider, Time Remaining, Iftar Time, Full Schedule Button + 3D Arrow (R.drawable.ic_prayer_arrow)
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
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCalendarClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B132B)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00B4D8).copy(alpha = 0.5f))
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // FAR-LEFT: Artistic Mosque Image & Left Info Column (Shifted Left for High Visibility)
                Row(
                    modifier = Modifier.weight(1.4f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(56.dp)
                            .height(92.dp)
                            .clip(RoundedCornerShape(10.dp))
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_prayer_art),
                            contentDescription = "Prayer Artwork",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Left-Aligned Waqt & Countdown Info
                    Column(
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        Text(
                            text = "এখন",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFCBD5E1),
                            maxLines = 1,
                            lineHeight = 12.sp
                        )
                        Text(
                            text = liveState.currentWaqtName,
                            fontSize = 21.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            maxLines = 1,
                            lineHeight = 21.sp
                        )
                        Text(
                            text = "সময় শেষ হতে বাকি",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF94A3B8),
                            maxLines = 1,
                            lineHeight = 11.sp
                        )
                        Text(
                            text = liveState.remainingCountdownText,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            maxLines = 1,
                            softWrap = false,
                            lineHeight = 19.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // RIGHT SIDE: 2-Row Compact Grid Layout
                Column(
                    modifier = Modifier.weight(2.2f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // ROW 1: 5 Prayer Waqts Chips (Compact Height & Tight Line Spacing)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        prayerTimes.forEach { (name, time) ->
                            val isCurrent = name == liveState.currentWaqtName
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isCurrent) Color(0xFF047857) else Color(0xFF1E293B))
                                    .border(1.dp, if (isCurrent) Color(0xFF10B981) else Color(0xFF334155), RoundedCornerShape(8.dp))
                                    .padding(vertical = 4.dp, horizontal = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(0.dp)
                                ) {
                                    Text(
                                        text = name,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCurrent) Color.White else Color(0xFF38BDF8),
                                        lineHeight = 12.sp
                                    )
                                    Text(
                                        text = time,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        lineHeight = 13.sp
                                    )
                                }
                            }
                        }
                    }

                    // ROW 2: Sahri Start, Divider, Remaining, Iftar, Full Schedule Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Sahri End Countdown Box ("সেহরী শেষ হতে বাকী")
                        Card(
                            modifier = Modifier.weight(2f),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(0.dp)
                            ) {
                                Text(
                                    text = "সেহরী শেষ হতে বাকী",
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF38BDF8),
                                    lineHeight = 9.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = liveState.remainingCountdownText,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    lineHeight = 11.sp
                                )
                            }
                        }

                        // Iftar Time Box
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp, horizontal = 2.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(0.dp)
                            ) {
                                Text(
                                    text = "ইফতার",
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF38BDF8),
                                    lineHeight = 9.sp
                                )
                                Text(
                                    text = schedule.maghribFormatted,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    lineHeight = 11.sp
                                )
                            }
                        }

                        // Full Schedule Action Button with 3D Arrow Image (R.drawable.ic_prayer_arrow)
                        Card(
                            modifier = Modifier
                                .weight(2f)
                                .clickable { onCalendarClick() },
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0284C7))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "নামাজের",
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF38BDF8),
                                        lineHeight = 9.sp
                                    )
                                    Text(
                                        text = "পূর্ণাঙ্গ সময়সূচি",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        lineHeight = 10.sp
                                    )
                                }

                                Image(
                                    painter = painterResource(id = R.drawable.ic_prayer_arrow),
                                    contentDescription = "Full Schedule Arrow",
                                    modifier = Modifier.size(23.dp),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
