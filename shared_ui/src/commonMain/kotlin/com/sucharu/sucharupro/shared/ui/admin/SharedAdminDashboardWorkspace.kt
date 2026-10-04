package com.sucharu.sucharupro.shared.ui.admin

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
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
 * Shared Multiplatform Master Admin ERP Operations Center for Web (Wasm), Desktop, and Mobile.
 * Renders full 25-Module Master Control Center, KPI Cards, Pipeline Swiper, and Intelligence Widgets.
 */
@Composable
fun SharedAdminDashboardWorkspace(
    modifier: Modifier = Modifier,
    onOpenCalculator: () -> Unit = {},
    onModuleClick: (moduleCode: String) -> Unit = {}
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070E1E))
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // 1. TOP HEADER BANNER
        Surface(
            color = Color(0xFF0F172A),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFF00B4D8).copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
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
                            text = "ADMIN OPERATIONS CENTER • SYSTEM ONLINE",
                            color = Color(0xFF10B981),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
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

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. 4 STAT KPI CHIPS ROW
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            KpiChip("আজকের অর্ডার", "৮৪", "↑ +১২%", Icons.Default.Add, Color(0xFF38BDF8), Modifier.weight(1f))
            KpiChip("উৎপাদন লোড", "২৮", "↑ +৮%", Icons.Default.Settings, Color(0xFF10B981), Modifier.weight(1f))
            KpiChip("নগদ আদায় (আজ)", "৳ ১,২২,৩০০", "↑ +১৮.০%", Icons.Default.Star, Color(0xFFF59E0B), Modifier.weight(1f))
            KpiChip("কমিশন বাকি", "৳ ১২,৪৫০", "↑ +৬.২%", Icons.Default.Person, Color(0xFFA855F7), Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. TODAY'S OPERATIONS & SALES TREND SECTION
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF0F172A),
            border = BorderStroke(1.dp, Color(0xFF00B4D8).copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "আজকের অপারেশন", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                        Text(text = "সব অর্ডার, সব ডিপার্টমেন্ট, এক নজরে", fontSize = 11.sp, color = Color(0xFF94A3B8))
                    }

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF1E293B),
                        border = BorderStroke(1.dp, Color(0xFF334155))
                    ) {
                        Text(
                            text = "আজকের ∨",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "মোট বিক্রি (আজ)", fontSize = 11.sp, color = Color(0xFF94A3B8))
                        Text(text = "৳ ১,৩৭,৫০০", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                        Text(text = "↑ +১২.৮% গতকালের তুলনায়", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "মোট অর্ডার", fontSize = 11.sp, color = Color(0xFF94A3B8))
                        Text(text = "৮৪টি অর্ডার", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                        Text(text = "↑ +১২% নতুন ইনটেক", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Order Health Score", fontSize = 11.sp, color = Color(0xFF94A3B8))
                        Text(text = "৭২% সম্পন্ন", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF38BDF8))
                        Text(text = "৬১ সম্পন্ন • ১৮ প্রসেসিং", fontSize = 11.sp, color = Color(0xFF94A3B8))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. 13-STAGE PRODUCTION WORKFLOW PIPELINE
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF0F172A),
            border = BorderStroke(1.dp, Color(0xFF00B4D8).copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "অর্ডার থেকে ডেলিভারি (১৩-স্টেপ ওয়ার্কফ্লো)",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    text = "প্রতিটি ধাপে নজরদারি, সময়মতো নিখুঁত ডেলিভারি",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )

                Spacer(modifier = Modifier.height(14.dp))

                val stages = listOf(
                    "অর্ডার গ্রহণ" to "৮৪",
                    "ডিজাইন" to "৬১",
                    "প্রিন্টিং" to "৩৮",
                    "ফিনিশিং" to "২২",
                    "প্যাকেজিং" to "১৮",
                    "ডেলিভারি" to "১২"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    stages.forEach { (label, count) ->
                        Surface(
                            color = Color(0xFF1E293B),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.3f)),
                            modifier = Modifier.weight(1f).padding(horizontal = 3.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(text = count, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF38BDF8))
                                Text(text = label, fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 5. 25-MODULE MASTER ERP CONTROL CENTER (M00 to M24)
        Text(
            text = "২৪টি ক্যানোনিকাল মডিউল কন্ট্রোল হাব (Modules 00–24)",
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White
        )
        Text(
            text = "মডিউল ০০ থেকে ২৪ — সরাসরি ১-ট্যাপ ডাইরেক্ট এক্সেস",
            fontSize = 11.sp,
            color = Color(0xFF94A3B8)
        )

        Spacer(modifier = Modifier.height(12.dp))

        val modules = listOf(
            Triple("Module 00", "সিস্টেম কনফিগারেশন ও সিকিউরিটি", Icons.Default.Settings),
            Triple("Module 01", "ইউজার একাউন্ট ও পারমিশন ম্যাট্রিক্স", Icons.Default.Person),
            Triple("Module 02", "কাস্টমার সিআরএম ও চুক্তি", Icons.Default.Person),
            Triple("Module 03", "কোটেশন ও সেলস অর্ডার ইনটেক", Icons.Default.Add),
            Triple("Module 04", "প্রোডাকশন প্ল্যানিং ও ১৩-স্টেপ জব কার্ড", Icons.Default.Settings),
            Triple("Module 05", "ডিজাইন ফাইল প্রি-প্রেস ও প্রুফিং", Icons.Default.Star),
            Triple("Module 06", "সিটিপি (CTP) প্লেট আউটপুট ও ইন-প্রসেস QC", Icons.Default.CheckCircle),
            Triple("Module 07", "তৈরি পণ্য ওয়ারহাউস ইনভেন্টরি", Icons.Default.Info),
            Triple("Module 08", "চালান ও লজিস্টিকস ডিসপ্যাচ", Icons.Default.Refresh),
            Triple("Module 09", "কাস্টমার ইনভয়েসিং ও রিসিভেবল", Icons.Default.Search),
            Triple("Module 10", "সিস্টেম অ্যালার্ট ও জরুরী নোটিফিকেশন", Icons.Default.Notifications),
            Triple("Module 11", "প্রুফ অফ ডেলিভারি (POD) কনফার্মেশন", Icons.Default.CheckCircle),
            Triple("Module 12", "রিটার্ন গুডস (RMA) ও ড্যামেজ ইনস্পেকশন", Icons.Default.Info),
            Triple("Module 13", "জেনারেল লেজার ও বিজনেস জার্নাল", Icons.Default.Star),
            Triple("Module 14", "অটো বিলিং ও ট্যাক্স/ভ্যাট ইনভয়েস", Icons.Default.CheckCircle),
            Triple("Module 15", "জব কস্টিং ও ম্যানুফ্যাকচারিং ভ্যারিয়েন্স", Icons.Default.Search),
            Triple("Module 16", "ইম্পোজিশন ও নেস্টিং লেআউট", Icons.Default.Settings),
            Triple("Module 17", "মাল্টি-লেভেল ক্রেডিট অনুমোদন", Icons.Default.CheckCircle),
            Triple("Module 18", "প্রফিটেবিলিটি ও কস্ট অ্যানালিটিক্স", Icons.Default.Star),
            Triple("Module 19", "সাবস্ট্রেট স্টক র-মেটেরিয়াল রিজার্ভেশন", Icons.Default.Info),
            Triple("Module 20", "অ্যাফিলিয়েট পার্টনার ও গভর্ন্যান্স", Icons.Default.Person),
            Triple("Module 21", "মেশিন টেলিমেট্রি ও কারখানা OEE", Icons.Default.Settings),
            Triple("Module 22", "ভেন্ডর পারচেজ অর্ডার ও রিপ্রেনিশমেন্ট", Icons.Default.Home),
            Triple("Module 23", "অ্যাফিলিয়েট কমিশন ওয়ালেট ও উইথড্রয়াল", Icons.Default.Person),
            Triple("Module 24", "সিইও এক্সিকিউティブ রিপোর্টস ও অ্যানালিটিক্স", Icons.Default.Search)
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
private fun KpiChip(
    title: String,
    value: String,
    subtext: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF0F172A),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = title, fontSize = 10.sp, color = Color(0xFF94A3B8))
                Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                Text(text = subtext, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = accentColor)
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
            }
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
        border = BorderStroke(1.dp, Color(0xFF00B4D8).copy(alpha = 0.3f))
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
                tint = Color(0xFF38BDF8),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
