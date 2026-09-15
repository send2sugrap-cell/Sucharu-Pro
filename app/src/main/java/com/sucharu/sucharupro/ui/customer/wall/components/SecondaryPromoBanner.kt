package com.sucharu.sucharupro.ui.customer.wall.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Star
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
 * Wide Secondary Promotional Banner (Matching Hand-Drawn Blueprint).
 * Displays Sucharu Graphics logo badge, corporate gift awards, t-shirts, and offset press graphics.
 */
@Composable
fun SecondaryPromoBanner(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(110.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0284C7).copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Sample Items Display Badges
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BannerItemBadge("সম্মাননা স্মারক", Icons.Default.Star, Color(0xFFFEF08A), Color(0xFFB45309))
                BannerItemBadge("ব্র্যান্ডেড টি-শার্ট", Icons.Default.CardGiftcard, Color(0xFFE0F2FE), Color(0xFF0369A1))
                BannerItemBadge("অফসেট প্রেস", Icons.Default.Print, Color(0xFFDCFCE7), Color(0xFF15803D))
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Right: Sucharu Graphics Circular Logo & Title
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color.White)
                        .border(2.dp, Color(0xFF0284C7), RoundedCornerShape(28.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF0284C7),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "সুচারু",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0284C7)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Text(
                        text = "সুচারু গ্রাফিক্স",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "এন্ড প্রিন্টিং",
                        fontSize = 10.sp,
                        color = Color(0xFF38BDF8)
                    )
                }
            }
        }
    }
}

@Composable
private fun BannerItemBadge(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    bgColor: Color,
    iconColor: Color
) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = iconColor,
                maxLines = 1
            )
        }
    }
}
