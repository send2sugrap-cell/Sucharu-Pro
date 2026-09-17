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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.chrono.HijrahDate

/**
 * Data class for synchronized Tri-Calendar calculation.
 */
data class TriCalendarData(
    val gregorianDay: String = "16",
    val gregorianMonthYear: String = "09/2026",
    val banglaDay: String = "01",
    val banglaMonthYear: String = "06/1433",
    val hijriDay: String = "04",
    val hijriMonthYear: String = "04/1448"
)

/**
 * Single Combined Editorial Wisdom & Tri-Calendar Card (Monishir Bani + 3 Calendar Columns).
 */
@Composable
fun DailyWisdomCard(
    quoteText: String = "ভালো কাজের মাধ্যমে মানুষের মনে বিশ্বাস তৈরি করাই আমাদের লক্ষ্য।",
    authorName: String = "সুচারু বাণী চিরন্তন",
    modifier: Modifier = Modifier
) {
    val calendarData = remember { calculateTriCalendarData() }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00B4D8).copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // LEFT: Monishir Bani Quote Section
            Column(
                modifier = Modifier.weight(1.4f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .background(Color(0xFF00B4D8).copy(alpha = 0.2f), RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FormatQuote,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = "মনিষীর বাণী",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "“$quoteText”",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    lineHeight = 16.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "— $authorName",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF38BDF8)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Main Vertical Divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(64.dp)
                    .background(Color(0xFF334155))
            )

            Spacer(modifier = Modifier.width(8.dp))

            // RIGHT: 3 Calendar Columns (English, Bangla, Hijri)
            Row(
                modifier = Modifier.weight(1.8f),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CalendarColumnItem("খ্রিস্টাব্দ", calendarData.gregorianDay, calendarData.gregorianMonthYear)

                Box(modifier = Modifier.width(1.dp).height(64.dp).background(Color(0xFF334155)))

                CalendarColumnItem("বঙ্গাব্দ", calendarData.banglaDay, calendarData.banglaMonthYear)

                Box(modifier = Modifier.width(1.dp).height(64.dp).background(Color(0xFF334155)))

                CalendarColumnItem("হিজরী", calendarData.hijriDay, calendarData.hijriMonthYear)
            }
        }
    }
}

@Composable
private fun CalendarColumnItem(
    label: String,
    largeDay: String,
    subMonthYear: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        Text(
            text = largeDay,
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF38BDF8),
            lineHeight = 22.sp
        )
        Text(
            text = subMonthYear,
            fontSize = 11.sp,
            fontWeight = FontWeight.Normal,
            color = Color.White,
            lineHeight = 12.sp
        )
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF94A3B8),
            textAlign = TextAlign.Center,
            lineHeight = 11.sp
        )
    }
}

/**
 * Calculates current Gregorian, Bangabda, and Hijri dates dynamically in English digits.
 */
private fun calculateTriCalendarData(): TriCalendarData {
    val now = LocalDate.now()
    val day = now.dayOfMonth
    val month = now.monthValue
    val year = now.year

    fun toEnglishDigits(number: Int): String {
        return String.format(java.util.Locale.US, "%02d", number)
    }

    val gregDayStr = toEnglishDigits(day)
    val gregMonthYearStr = "${toEnglishDigits(month)}/${year}"

    // Bangla / Bangabda calculation (Solar Calendar offset ~593 years)
    val banglaYear = year - 593
    val banglaMonth = if (month >= 4) month - 3 else month + 9
    val banglaDay = if (day >= 15) day - 14 else day + 16
    val banglaDayStr = toEnglishDigits(banglaDay)
    val banglaMonthYearStr = "${toEnglishDigits(banglaMonth)}/${banglaYear}"

    // Hijri calculation via HijrahDate
    val hijrahDate = HijrahDate.now()
    val hijriDayStr = toEnglishDigits(hijrahDate.get(java.time.temporal.ChronoField.DAY_OF_MONTH))
    val hijriMonth = hijrahDate.get(java.time.temporal.ChronoField.MONTH_OF_YEAR)
    val hijriYear = hijrahDate.get(java.time.temporal.ChronoField.YEAR)
    val hijriMonthYearStr = "${toEnglishDigits(hijriMonth)}/${hijriYear}"

    return TriCalendarData(
        gregorianDay = gregDayStr,
        gregorianMonthYear = gregMonthYearStr,
        banglaDay = banglaDayStr,
        banglaMonthYear = banglaMonthYearStr,
        hijriDay = hijriDayStr,
        hijriMonthYear = hijriMonthYearStr
    )
}
