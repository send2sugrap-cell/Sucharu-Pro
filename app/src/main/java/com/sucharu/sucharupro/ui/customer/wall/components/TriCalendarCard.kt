package com.sucharu.sucharupro.ui.customer.wall.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Tri-Calendar & Special Occasion Notice Bar.
 * Displays Day Name (বার), Gregorian, Hijri, Bangla dates, and status notice badge.
 */
@Composable
fun TriCalendarCard(
    modifier: Modifier = Modifier,
    dayName: String = "মঙ্গলবার",
    gregorianDate: String = "১৫ সেপ্টেম্বর ২০২৬",
    hijriDate: String = "২৩ রবিউল আউয়াল ১৪৪৮ হিজরি",
    banglaDate: String = "৩০ ভাদ্র ১৪৩৩ বঙ্গাব্দ",
    occasionNotice: String = "বাণিজ্যিক মুদ্রণ, কাস্টম প্যাকেজিং ও গ্রাহক সেবা সক্রিয় রানিং"
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            // Day & Gregorian
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dayName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0284C7)
                )

                Text(
                    text = gregorianDate,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Hijri & Bangla
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "হিজরি: $hijriDate",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )

                Text(
                    text = "বাংলা: $banglaDate",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )
            }

            if (occasionNotice.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFE0F2FE), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "স্ট্যাটাস: ",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0369A1)
                    )
                    Text(
                        text = occasionNotice,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF0369A1)
                    )
                }
            }
        }
    }
}
