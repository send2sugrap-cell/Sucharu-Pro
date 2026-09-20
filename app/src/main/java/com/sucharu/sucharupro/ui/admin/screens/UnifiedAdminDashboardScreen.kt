package com.sucharu.sucharupro.ui.admin.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.ui.admin.components.AdminAffiliateNodeGraph
import com.sucharu.sucharupro.ui.admin.components.AdminButton
import com.sucharu.sucharupro.ui.admin.components.AdminButtonStyle
import com.sucharu.sucharupro.ui.admin.components.AdminCard
import com.sucharu.sucharupro.ui.admin.components.AdminCardSkeleton
import com.sucharu.sucharupro.ui.admin.components.AdminCashCollectionCard
import com.sucharu.sucharupro.ui.admin.components.AdminDonutCompletionChart
import com.sucharu.sucharupro.ui.admin.components.AdminIconContainer
import com.sucharu.sucharupro.ui.admin.components.AdminKpiGridSkeleton
import com.sucharu.sucharupro.ui.admin.components.AdminProductionLoadCard
import com.sucharu.sucharupro.ui.admin.components.AdminSalesTrendChart
import com.sucharu.sucharupro.ui.admin.components.AdminWorkflowSwiper
import com.sucharu.sucharupro.ui.admin.theme.AdminTheme
import com.sucharu.sucharupro.ui.features.dashboard.DashboardUiState
import com.sucharu.sucharupro.ui.features.dashboard.DashboardViewModel
import com.sucharu.sucharupro.ui.navigation.AppDestination

/**
 * Primary Unified ERP Command Center Dashboard Screen matching reference design image (Complete Implementation).
 */
@Composable
fun UnifiedAdminDashboardScreen(
    viewModel: DashboardViewModel,
    principal: AuthenticatedPrincipal?,
    modifier: Modifier = Modifier,
    onNavigateToDestination: (destination: AppDestination) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp

    val isDesktop = screenWidthDp >= 840

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 28.dp)
    ) {
        when (val state = uiState) {
            is DashboardUiState.Loading -> {
                AdminKpiGridSkeleton(count = 4)
                Spacer(modifier = Modifier.height(AdminTheme.spacing.md))
                AdminCardSkeleton(cardHeight = 220.dp)
            }

            is DashboardUiState.Error -> {
                AdminCard(accentBarColor = AdminTheme.colors.error) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AdminIconContainer(
                            icon = Icons.Default.Error,
                            iconTint = AdminTheme.colors.error,
                            containerColor = AdminTheme.colors.errorContainer
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = "Dashboard Data Unavailable", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(modifier = Modifier.height(12.dp))
                        AdminButton(text = "Retry", onClick = { viewModel.retry() }, style = AdminButtonStyle.SECONDARY, icon = Icons.Default.Refresh)
                    }
                }
            }

            else -> {
                // Renders full Admin Operations Dashboard for both Empty and Success states
                AdminOperationsDashboardBody(
                    isDesktop = isDesktop,
                    onNavigateToDestination = onNavigateToDestination
                )
            }
        }
    }
}

@Composable
private fun AdminOperationsDashboardBody(
    isDesktop: Boolean,
    onNavigateToDestination: (destination: AppDestination) -> Unit
) {
    // 1. TODAY'S OPERATIONS & ORDER HEALTH SECTION
    AdminTodayOperationsSection(
        isDesktop = isDesktop,
        onNavigateToDestination = onNavigateToDestination
    )

    Spacer(modifier = Modifier.height(14.dp))

    // 2. 4 STAT KPI CHIPS ROW
    AdminFourKpiChipsRow(
        isDesktop = isDesktop,
        onNavigateToDestination = onNavigateToDestination
    )

    Spacer(modifier = Modifier.height(16.dp))

    // 3. AFFILIATE INTELLIGENCE & CASH/PRODUCTION CARDS SECTION
    if (isDesktop) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(modifier = Modifier.weight(1.2f)) {
                AdminAffiliateIntelligenceCard(onNavigateToDestination = onNavigateToDestination)
            }
            Column(modifier = Modifier.weight(0.8f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                AdminCashCollectionCard(onNavigateToDestination = onNavigateToDestination)
                AdminProductionLoadCard(onNavigateToDestination = onNavigateToDestination)
            }
        }
    } else {
        AdminAffiliateIntelligenceCard(onNavigateToDestination = onNavigateToDestination)
        Spacer(modifier = Modifier.height(14.dp))
        AdminCashCollectionCard(onNavigateToDestination = onNavigateToDestination)
        Spacer(modifier = Modifier.height(14.dp))
        AdminProductionLoadCard(onNavigateToDestination = onNavigateToDestination)
    }

    Spacer(modifier = Modifier.height(16.dp))

    // 4. INTERACTIVE 13-STAGE WORKFLOW PIPELINE SWIPER
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF0F172A),
        border = BorderStroke(1.dp, Color(0xFF00B4D8).copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.padding(14.dp)) {
            AdminWorkflowSwiper(
                onStageClick = { stage ->
                    onNavigateToDestination(AppDestination.Staff.Production)
                }
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // 5. PRIORITY ALERTS & QUICK CONTROLS
    if (isDesktop) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(modifier = Modifier.weight(1.2f)) {
                AdminPriorityAlertsWidget(onNavigateToDestination = onNavigateToDestination)
            }
            Column(modifier = Modifier.weight(0.8f)) {
                AdminQuickControlsWidget(onNavigateToDestination = onNavigateToDestination)
            }
        }
    } else {
        AdminPriorityAlertsWidget(onNavigateToDestination = onNavigateToDestination)
        Spacer(modifier = Modifier.height(14.dp))
        AdminQuickControlsWidget(onNavigateToDestination = onNavigateToDestination)
    }

    Spacer(modifier = Modifier.height(16.dp))

    // 6. 24-MODULE MASTER CONTROL CENTER GRID WIDGET
    com.sucharu.sucharupro.ui.admin.components.AdminMasterModulesGridCard(
        onNavigateToDestination = onNavigateToDestination
    )
}

/**
 * Today's Operations Header with Sales Trend Chart & Donut Chart.
 */
@Composable
private fun AdminTodayOperationsSection(
    isDesktop: Boolean,
    onNavigateToDestination: (AppDestination) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF0F172A),
        border = BorderStroke(1.dp, Color(0xFF00B4D8).copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "আজকের অপারেশন", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    Text(text = "সব অর্ডার, সব ডিপার্টমেন্ট, এক নজরে", fontSize = 10.5.sp, color = Color(0xFF94A3B8))
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF1E293B),
                    border = BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Text(
                        text = "আজকের ∨",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isDesktop) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1.2f)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                            Column {
                                Text(text = "মোট বিক্রি (আজ)", fontSize = 10.sp, color = Color(0xFF94A3B8))
                                Text(text = "৳ ১,৩৭,৫০০", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                                Text(text = "↑ +১২.৮% গতকালের তুলনায়", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                            }
                            Column {
                                Text(text = "মোট অর্ডার", fontSize = 10.sp, color = Color(0xFF94A3B8))
                                Text(text = "৮৪", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                                Text(text = "↑ +১২% গতকালের তুলনায়", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        AdminSalesTrendChart()
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    AdminDonutCompletionChart()
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "মোট বিক্রি (আজ)", fontSize = 10.sp, color = Color(0xFF94A3B8))
                        Text(text = "৳ ১,৩৭,৫০০", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                        Text(text = "↑ +১২.৮% গতকালের তুলনায়", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                    }
                    AdminDonutCompletionChart(modifier = Modifier.size(90.dp))
                }
                Spacer(modifier = Modifier.height(8.dp))
                AdminSalesTrendChart()
            }
        }
    }
}

/**
 * 4 Stat KPI Chips Row matching Image 1.
 */
@Composable
private fun AdminFourKpiChipsRow(
    isDesktop: Boolean,
    onNavigateToDestination: (AppDestination) -> Unit
) {
    val chips = listOf(
        KpiChipData("আজকের অর্ডার", "৮৪", "↑ +১২%", Icons.Default.ShoppingCart, Color(0xFF38BDF8)) { onNavigateToDestination(AppDestination.Customer.Orders) },
        KpiChipData("উৎপাদন লোড", "২৮", "↑ +৮%", Icons.Default.Engineering, Color(0xFF10B981)) { onNavigateToDestination(AppDestination.Staff.Production) },
        KpiChipData("নগদ আদায় (আজ)", "৳ ১,২২,৩০০", "↑ +১৮.০%", Icons.Default.MonetizationOn, Color(0xFFF59E0B)) { onNavigateToDestination(AppDestination.Admin.Finance) },
        KpiChipData("কমিশন বাকি", "৳ ১২,৪৫০", "↑ +৬.২%", Icons.Default.AccountBalanceWallet, Color(0xFFA855F7)) { onNavigateToDestination(AppDestination.Admin.AffiliateManagement) }
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        chips.forEach { chip ->
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { chip.onClick() },
                color = Color(0xFF0F172A),
                border = BorderStroke(1.dp, chip.accentColor.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = chip.title, fontSize = 9.5.sp, color = Color(0xFF94A3B8))
                        Text(text = chip.value, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                        Text(text = chip.subtext, fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = chip.accentColor)
                    }

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(chip.accentColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = chip.icon, contentDescription = null, tint = chip.accentColor, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

private data class KpiChipData(
    val title: String,
    val value: String,
    val subtext: String,
    val icon: ImageVector,
    val accentColor: Color,
    val onClick: () -> Unit
)

/**
 * Affiliate Intelligence Network Orbital Graph Card matching Image 1 & Image 2.
 */
@Composable
private fun AdminAffiliateIntelligenceCard(
    onNavigateToDestination: (AppDestination) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF0F172A),
        border = BorderStroke(1.dp, Color(0xFF00B4D8).copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00B4D8).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Campaign, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(text = "Affiliate Intelligence", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                        Text(text = "আমাদের পার্টনার, আমাদের শক্তি", fontSize = 10.sp, color = Color(0xFF94A3B8))
                    }
                }

                Text(
                    text = "বিস্তারিত দেখুন >",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF38BDF8),
                    modifier = Modifier.clickable { onNavigateToDestination(AppDestination.Admin.AffiliateManagement) }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Orbital Node Graph
            AdminAffiliateNodeGraph(partnerCount = 28)

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "সক্রিয় পার্টনার", fontSize = 9.5.sp, color = Color(0xFF94A3B8))
                    Text(text = "২৮ জন", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    Text(text = "↑ +৮ জন গত মাসে", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                }
                Column {
                    Text(text = "রেফারেল বিক্রি", fontSize = 9.5.sp, color = Color(0xFF94A3B8))
                    Text(text = "৳ ১,৭৩,২০০", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    Text(text = "↑ +৩২.৫%", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                }
                Column {
                    Text(text = "কমিশন প্রদান", fontSize = 9.5.sp, color = Color(0xFF94A3B8))
                    Text(text = "৳ ১২,৪৫০", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    Text(text = "৫ জন পার্টনার", fontSize = 9.sp, color = Color(0xFF38BDF8))
                }
            }
        }
    }
}

/**
 * Priority Alerts Widget (অগ্রাধিকার সতর্কতা).
 */
@Composable
private fun AdminPriorityAlertsWidget(
    onNavigateToDestination: (AppDestination) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF0F172A),
        border = BorderStroke(1.dp, Color(0xFF00B4D8).copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Notifications, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "অগ্রাধিকার সতর্কতা (Priority Alerts)", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                }
                Text(text = "সব দেখুন >", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
            }

            Spacer(modifier = Modifier.height(10.dp))

            val alerts = listOf(
                "পেমেন্ট বকেয়া (২ দিনের বেশি)" to "১২",
                "ডেলিভারি দেরি হওয়ার ঝুঁকি" to "৫",
                "ডিজাইন অনুমোদন অপেক্ষমাণ" to "৮",
                "কাঁচামাল কম আছে" to "৩",
                "উৎপাদনে আটকে আছে" to "৮",
                "আজ সম্পন্ন হওয়ার লক্ষ্যমাত্রা" to "১৮"
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                alerts.forEach { (label, count) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1E293B))
                            .clickable { onNavigateToDestination(AppDestination.Admin.Notifications) }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = label, fontSize = 11.sp, color = Color.White)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = count, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFEF4444))
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Quick Controls Widget (কন্ট্রোল শর্টকাট).
 */
@Composable
private fun AdminQuickControlsWidget(
    onNavigateToDestination: (AppDestination) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF0F172A),
        border = BorderStroke(1.dp, Color(0xFF00B4D8).copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(text = "কন্ট্রোল শর্টকাট (Quick Controls)", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Text(text = "দ্রুত অ্যাক্সেস, দ্রুত অগ্রগতি", fontSize = 10.sp, color = Color(0xFF94A3B8))

            Spacer(modifier = Modifier.height(12.dp))

            val controls = listOf(
                Triple("নতুন অর্ডার", Icons.Default.Add, AppDestination.Customer.Quotations),
                Triple("উৎপাদন নিয়ন্ত্রণ", Icons.Default.Engineering, AppDestination.Staff.Production),
                Triple("আর্থিক ব্যবস্থাপনা", Icons.Default.MonetizationOn, AppDestination.Admin.Finance),
                Triple("অ্যাফিলিয়েট ব্যবস্থাপনা", Icons.Default.Campaign, AppDestination.Admin.AffiliateManagement),
                Triple("রিপোর্ট এবং বিশ্লেষণ", Icons.Default.Analytics, AppDestination.Admin.Reports),
                Triple("সিস্টেম সেটিংস", Icons.Default.Category, AppDestination.Admin.Settings)
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                controls.chunked(2).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { (label, icon, dest) ->
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onNavigateToDestination(dest) },
                                color = Color(0xFF1E293B),
                                border = BorderStroke(1.dp, Color(0xFF00B4D8).copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = icon, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(text = label, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                    Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
