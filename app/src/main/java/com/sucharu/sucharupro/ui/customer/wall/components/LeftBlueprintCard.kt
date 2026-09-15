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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Left Column Blueprint Card.
 * Top Box: AI Daily Wisdom / বাণী চিরন্তন Post
 * Bottom Box: Tri-Calendar (Gregorian, Hijri, Bangla) & Government Holiday / Status Badge
 */
@Composable
fun LeftBlueprintCard(
    modifier: Modifier = Modifier,
    dailyQuote: String = "এখানে আমাদের এআই এজেন্ট মুসলিম মনীষীদের বাণীচিরন্তন পোস্ট করবে",
    dayName: String = "মঙ্গল",
    gregorianDate: String = "১৫/০৯/২০২৬",
    hijriDate: String = "২৩/০৩/১৪৪৮ [হিজরী]",
    banglaDate: String = "৩০/০৫/১৪৩৩ [বঙ্গাব্দ]",
    holidayNotice: String = "বাণিজ্যিক সেবা সক্রিয় রানিং"
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // TOP BOX: AI Daily Wisdom Quote
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Default.FormatQuote,
                        contentDescription = null,
                        tint = Color(0xFF0284C7),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = dailyQuote,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF334155),
                        lineHeight = 14.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // BOTTOM BOX: Tri-Calendar & Notice
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "বার: $dayName",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0284C7)
                    )
                    Text(
                        text = gregorianDate,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = hijriDate,
                        fontSize = 9.sp,
                        color = Color(0xFF64748B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = banglaDate,
                        fontSize = 9.sp,
                        color = Color(0xFF64748B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (holidayNotice.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFE0F2FE), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = holidayNotice,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF0369A1),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
