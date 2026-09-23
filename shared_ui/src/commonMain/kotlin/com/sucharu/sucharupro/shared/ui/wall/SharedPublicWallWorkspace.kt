package com.sucharu.sucharupro.shared.ui.wall

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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

/**
 * Shared Multiplatform Public Home Wall Workspace.
 */
@Composable
fun SharedPublicWallWorkspace(
    modifier: Modifier = Modifier,
    onOpenAdmin: () -> Unit = {},
    onOpenCalculator: () -> Unit = {}
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Hero Offer Banner
        Surface(
            color = Color(0xFF1E3A8A),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color(0xFFD97706),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "১৫% ছাড় • ঈদ স্পেশাল অফার",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    Text(
                        text = "SUCHARU SPECIAL",
                        color = Color(0xFF38BDF8),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "বুল্ক প্রিন্টিং ও কর্পোরেট গিফট স্পেশাল ডিল",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    text = "কাস্টম বিজনেস কার্ড, ব্রোশিয়ার, ডায়েরি ও ক্যালেন্ডার অর্ডারে দ্রুত ডেলিভারি",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8)
                )

                Spacer(modifier = Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        color = Color(0xFF0284C7),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.clickable { onOpenCalculator() }
                    ) {
                        Text(
                            text = "কোটেশন দেখুন",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }

                    Surface(
                        color = Color(0xFF334155),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.clickable { onOpenAdmin() }
                    ) {
                        Text(
                            text = "এ্যাডমিন প্যানেল",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Popular Services Grid
        Text(
            text = "জনপ্রিয় প্রিন্টিং সেবাসমূহ",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(10.dp))

        val services = listOf(
            "অফসেট কমার্শিয়াল প্রিন্টিং" to "বিজনেস কার্ড, লেটারহেড, প্যাড ও ব্রোশিয়ার",
            "ডিজিটাল ফাস্ট প্রিন্টিং" to "জরুরী অর্ডারের জন্য সেম-ডে ডেলিভারি",
            "প্যাকেজিং ও ডাই-কাটিং" to "কাস্টম কার্টন, পেপার ব্যাগ ও বক্স",
            "কর্পোরেট ডায়েরি ও ক্যালেন্ডার" to "২০২৭ নিউ ইয়ার কাস্টম ডায়েরি প্রাক-অর্ডার"
        )

        services.forEach { (title, desc) ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .clickable { onOpenCalculator() },
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(text = desc, fontSize = 11.sp, color = Color(0xFF94A3B8))
                    }
                }
            }
        }
    }
}
