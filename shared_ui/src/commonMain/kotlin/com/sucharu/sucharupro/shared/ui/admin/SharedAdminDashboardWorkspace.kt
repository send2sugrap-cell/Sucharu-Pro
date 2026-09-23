package com.sucharu.sucharupro.shared.ui.admin

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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Shared Multiplatform Master Admin ERP Workspace for Web (Wasm), Desktop, and Android.
 */
@Composable
fun SharedAdminDashboardWorkspace(
    modifier: Modifier = Modifier,
    onOpenCalculator: () -> Unit = {},
    onModuleClick: (moduleName: String) -> Unit = {}
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Top Header Banner
        Surface(
            color = Color(0xFF1E293B),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SUCHARU PRO ERP • SYSTEM ONLINE",
                            color = Color(0xFF10B981),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "ইউনিফাইড এ্যাডমিন কমান্ড সেন্টার",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = "মডিউল ০০-২৪ • প্রডাকশন, ক্যাশ কালেকশন, জব কস্টিং ও ইন্টেলিজেন্স হাব",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
                }

                Surface(
                    color = Color(0xFF0284C7),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.clickable { onOpenCalculator() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "প্রিন্টিং ক্যালকুলেটর হাব",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Executive KPI Cards Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            KpiCard("দৈনিক আয় / সেলস", "৳ ২,৪৫,৮০০.০০", "+১২.৫%", Color(0xFF10B981), Modifier.weight(1f))
            KpiCard("সচল প্রডাকশন রান", "১৮টি অর্ডার", "১৩টি ধাপে রানিং", Color(0xFF38BDF8), Modifier.weight(1f))
            KpiCard("ক্যাশ কালেকশন", "৳ ১,৮৫,০০০.০০", "আজকের জমা", Color(0xFFF59E0B), Modifier.weight(1f))
            KpiCard("মেশিন OEE স্কোয়ার", "৮৮.৪%", "অপটিমাল স্টেট", Color(0xFF8B5CF6), Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Module Categories Title
        Text(
            text = "ইআরপি মাস্টার মডিউলসমূহ (Modules 00–24)",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Modules Grid
        val modules = listOf(
            Triple("Module 00", "সিস্টেম কনফিগারেশন ও সিকিউরিটি", Icons.Default.Settings),
            Triple("Module 01", "ইউজার ও ক্যাপাবিলিটি ম্যাট্রিক্স", Icons.Default.Person),
            Triple("Module 02", "কাস্টমার ও কন্টাক্ট ম্যানেজমেন্ট", Icons.Default.Home),
            Triple("Module 03", "কোটেশন ও সেলস অর্ডার", Icons.Default.ShoppingCart),
            Triple("Module 04", "প্রডাকশন এক্সিকিউশন (১৩টি ধাপ)", Icons.Default.Edit),
            Triple("Module 05", "ডিজাইন ও প্রি-প্রেস প্রুফিং", Icons.Default.Star),
            Triple("Module 06", "CTP প্লেট আউটপুট ও QC", Icons.Default.CheckCircle),
            Triple("Module 07", "ফিনিশড গুডস ইনভেন্টরি", Icons.Default.Info),
            Triple("Module 08/11", "ডেলিভারি চালান ও ডিসপ্যাচ", Icons.Default.Refresh),
            Triple("Module 09/14", "ফিন্যান্স, মেমো ও ইনভয়েসিং", Icons.Default.Search),
            Triple("Module 15/18", "প্রডাকশন জব কস্টিং ও রেট কার্ড", Icons.Default.Add),
            Triple("Module 19", "সাবস্ট্রেট স্টক রিজার্ভেশন", Icons.Default.Star),
            Triple("Module 20", "আফিলিয়েট গভর্ন্যান্স Network", Icons.Default.Notifications),
            Triple("Module 21", "মেশিন টেলিমেট্রি ও OEE", Icons.Default.Settings),
            Triple("Module 23", "ওয়ালেট ও পেআউট একাউন্টিং", Icons.Default.Person),
            Triple("Module 24", "রিপোর্টস, এনালিটিক্স ও অডিট (15 CAT)", Icons.Default.Info)
        )

        val columns = 2
        val rows = (modules.size + columns - 1) / columns

        for (rowIndex in 0 until rows) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                for (colIndex in 0 until columns) {
                    val index = rowIndex * columns + colIndex
                    if (index < modules.size) {
                        val (code, name, icon) = modules[index]
                        ModuleCard(
                            code = code,
                            name = name,
                            icon = icon,
                            modifier = Modifier.weight(1f),
                            onClick = { onModuleClick(code) }
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun KpiCard(
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(text = title, fontSize = 11.sp, color = Color(0xFF94A3B8))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, fontSize = 10.sp, color = accentColor, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ModuleCard(
    code: String,
    name: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0284C7).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = code, fontSize = 10.sp, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                Text(text = name, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = Color(0xFF64748B),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
