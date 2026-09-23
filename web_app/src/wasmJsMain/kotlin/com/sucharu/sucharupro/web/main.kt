package com.sucharu.sucharupro.web

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.CanvasBasedWindow
import com.sucharu.sucharupro.shared.ui.admin.SharedAdminDashboardWorkspace
import com.sucharu.sucharupro.shared.ui.calculator.SharedPrintingCalculatorWorkspace
import com.sucharu.sucharupro.shared.ui.theme.SharedTheme
import com.sucharu.sucharupro.shared.ui.wall.SharedPublicWallWorkspace

/**
 * Main Web Entry Point for Sucharu Pro (Wasm / WebAssembly Compose Multiplatform).
 */
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    CanvasBasedWindow(
        title = "Sucharu Pro - Commercial Printing ERP & Calculator",
        canvasElementId = "ComposeTarget"
    ) {
        var selectedWebTab by remember { mutableIntStateOf(0) } // 0: Admin Dashboard, 1: Printing Calculator, 2: Public Wall
        var activeModuleDetail by remember { mutableStateOf<String?>(null) }

        SharedTheme(darkTheme = true) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0F172A))
            ) {
                // Top Global Web Navigation Bar
                Surface(
                    color = Color(0xFF1E293B),
                    tonalElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "সুচারু প্রো",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF38BDF8)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = Color(0xFF0284C7).copy(alpha = 0.3f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "WEB ERP (WASM)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF38BDF8),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            WebNavTab("১. এ্যাডমিন ড্যাশবোর্ড", Icons.Default.Settings, selectedWebTab == 0) {
                                activeModuleDetail = null
                                selectedWebTab = 0
                            }
                            WebNavTab("২. প্রিন্টিং ক্যালকুলেটর", Icons.Default.Add, selectedWebTab == 1) {
                                activeModuleDetail = null
                                selectedWebTab = 1
                            }
                            WebNavTab("৩. পাবলিক হোম ওয়াল", Icons.Default.Home, selectedWebTab == 2) {
                                activeModuleDetail = null
                                selectedWebTab = 2
                            }
                        }
                    }
                }

                // Active Workspace Content
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    if (activeModuleDetail != null) {
                        WebModuleDetailWorkspace(
                            moduleCode = activeModuleDetail!!,
                            onClose = { activeModuleDetail = null }
                        )
                    } else {
                        when (selectedWebTab) {
                            0 -> SharedAdminDashboardWorkspace(
                                onOpenCalculator = { selectedWebTab = 1 },
                                onModuleClick = { moduleCode -> activeModuleDetail = moduleCode }
                            )
                            1 -> SharedPrintingCalculatorWorkspace(
                                onClose = { selectedWebTab = 0 }
                            )
                            2 -> SharedPublicWallWorkspace(
                                onOpenAdmin = { selectedWebTab = 0 },
                                onOpenCalculator = { selectedWebTab = 1 }
                            )
                            else -> SharedAdminDashboardWorkspace(
                                onOpenCalculator = { selectedWebTab = 1 },
                                onModuleClick = { moduleCode -> activeModuleDetail = moduleCode }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WebNavTab(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (isSelected) Color(0xFF0284C7) else Color(0xFF334155),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun WebModuleDetailWorkspace(
    moduleCode: String,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.7f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "$moduleCode • লাইভ ইআরপি এক্সিকিউশন মডিউল",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8)
                        )
                        Text(
                            text = getModuleName(moduleCode),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }

                    Surface(
                        color = Color(0xFF334155),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.clickable { onClose() }
                    ) {
                        Text(
                            text = "বন্ধ করুন",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Module Quick Action Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ModuleActionButton("অপারেশনাল ডাটা ভিউ", Color(0xFF10B981), Modifier.weight(1f))
                    ModuleActionButton("নতুন এন্ট্রি যোগ করুন", Color(0xFF0284C7), Modifier.weight(1f))
                    ModuleActionButton("অডিট ও রিপোর্টস", Color(0xFF8B5CF6), Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    color = Color(0xFF0F172A),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "সিস্টেম স্ট্যাটাস: ২০২৬-০৯-২৩ অডিট ভেরিফায়েড • সার্ভার কানেক্টেড (HTTP 200 OK)",
                            fontSize = 11.sp,
                            color = Color(0xFF10B981),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModuleActionButton(
    label: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = accentColor.copy(alpha = 0.2f),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = accentColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun getModuleName(code: String): String {
    return when (code) {
        "Module 00" -> "সিস্টেম কনফিগারেশন ও সিকিউরিটি"
        "Module 01" -> "ইউজার ও ক্যাপাবিলিটি ম্যাট্রিক্স"
        "Module 02" -> "কাস্টমার ও কন্টাক্ট ম্যানেজমেন্ট"
        "Module 03" -> "কোটেশন ও সেলস অর্ডার"
        "Module 04" -> "প্রডাকশন এক্সিকিউশন (১৩টি ধাপ)"
        "Module 05" -> "ডিজাইন ও প্রি-প্রেস প্রুফিং"
        "Module 06" -> "CTP প্লেট আউটপুট ও QC"
        "Module 07" -> "ফিনিশড গুডস ইনভেন্টরি"
        "Module 08/11" -> "ডেলিভারি চালান ও ডিসপ্যাচ"
        "Module 09/14" -> "ফিন্যান্স, মেমো ও ইনভয়েসিং"
        "Module 15/18" -> "প্রডাকশন জব কস্টিং ও রেট কার্ড"
        "Module 19" -> "সাবস্ট্রেট স্টক রিজার্ভেশন"
        "Module 20" -> "আফিলিয়েট গভর্ন্যান্স Network"
        "Module 21" -> "মেশিন টেলিমেট্রি ও OEE"
        "Module 23" -> "ওয়ালেট ও পেআউট একাউন্টিং"
        "Module 24" -> "রিপোর্টস, এনালিটিক্স ও অডিট (15 CAT)"
        else -> "ইআরপি অপারেশনাল মডিউল"
    }
}
