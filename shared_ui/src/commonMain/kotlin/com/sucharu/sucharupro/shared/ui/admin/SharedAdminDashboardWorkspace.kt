package com.sucharu.sucharupro.shared.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private data class Tuple3<A, B, C>(val first: A, val second: B, val third: C)
private data class Tuple5<A, B, C, D, E>(val first: A, val second: B, val third: C, val fourth: D, val fifth: E)

/**
 * Task 13.1 & 13.2: Master Admin Operations Center ERP Dashboard for Web (Wasm), Desktop, and Mobile.
 * Implements Responsive Adaptive Dark Theme Layouts across Compact (<600dp), Medium (600-839dp),
 * and Expanded (>=840dp) screen dimensions with min 48dp touch targets and live operational cards.
 */
@Composable
fun SharedAdminDashboardWorkspace(
    modifier: Modifier = Modifier,
    onOpenCalculator: () -> Unit = {},
    onModuleClick: (moduleCode: String) -> Unit = {}
) {
    var activeNavTab by remember { mutableStateOf("Dashboard") }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF040914))
    ) {
        val isCompact = maxWidth < 600.dp
        val isExpanded = maxWidth >= 840.dp

        Row(modifier = Modifier.fillMaxSize()) {
            // 1. LEFT VERTICAL SIDEBAR (Shown on Medium & Expanded screens)
            if (!isCompact) {
                SidebarComponent(
                    activeNavTab = activeNavTab,
                    onTabSelected = { tab ->
                        activeNavTab = tab
                        when (tab) {
                            "Orders" -> onModuleClick("Module 03")
                            "Production" -> onModuleClick("Module 04")
                            "Finance" -> onModuleClick("Module 09/14")
                            "Affiliate" -> onModuleClick("Module 20")
                            "Reports" -> onModuleClick("Module 24")
                            "Users" -> onModuleClick("Module 01")
                            "Settings" -> onModuleClick("Module 00")
                            else -> { /* Dashboard tab */ }
                        }
                    }
                )

                // Vertical Divider Glow
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(Color(0xFF00B4D8).copy(alpha = 0.25f))
                )
            }

            // 2. MAIN OPERATIONS AREA (Adaptive Grid)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // A. TOP BAR & KPI HEADER
                TopHeaderComponent(
                    onOpenCalculator = onOpenCalculator,
                    onModuleClick = onModuleClick
                )

                // B. TWO-COLUMN ANALYTICS TOP SECTION
                if (isExpanded) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        TodayOperationsCard(
                            modifier = Modifier.weight(1.5f),
                            onModuleClick = onModuleClick
                        )
                        OrderHealthCard(
                            modifier = Modifier.weight(1f),
                            onModuleClick = onModuleClick
                        )
                    }
                } else {
                    TodayOperationsCard(
                        modifier = Modifier.fillMaxWidth(),
                        onModuleClick = onModuleClick
                    )
                    OrderHealthCard(
                        modifier = Modifier.fillMaxWidth(),
                        onModuleClick = onModuleClick
                    )
                }

                // C. 4 DYNAMIC TOP KPI CARDS
                if (isCompact) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        KpiCard("আজকের অর্ডার", "৮৪", "↑ +১২%", Icons.Default.ShoppingCart, Color(0xFF00F0FF), Modifier.fillMaxWidth()) { onModuleClick("Module 03") }
                        KpiCard("উৎপাদন লোড", "২৮", "↑ +৮%", Icons.Default.Settings, Color(0xFF00F0FF), Modifier.fillMaxWidth()) { onModuleClick("Module 04") }
                        KpiCard("নগদ আদায় (আজ)", "৳ ১,২২,৩০০", "↑ +১৮.০%", Icons.Default.Star, Color(0xFF00F0FF), Modifier.fillMaxWidth()) { onModuleClick("Module 09/14") }
                        KpiCard("কমিশন বাকি", "৳ ১২,৪৫০", "↑ +৬.২%", Icons.Default.Person, Color(0xFF00F0FF), Modifier.fillMaxWidth()) { onModuleClick("Module 23") }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        KpiCard("আজকের অর্ডার", "৮৪", "↑ +১২%", Icons.Default.ShoppingCart, Color(0xFF00F0FF), Modifier.weight(1f)) { onModuleClick("Module 03") }
                        KpiCard("উৎপাদন লোড", "২৮", "↑ +৮%", Icons.Default.Settings, Color(0xFF00F0FF), Modifier.weight(1f)) { onModuleClick("Module 04") }
                        KpiCard("নগদ আদায় (আজ)", "৳ ১,২২,৩০০", "↑ +১৮.০%", Icons.Default.Star, Color(0xFF00F0FF), Modifier.weight(1f)) { onModuleClick("Module 09/14") }
                        KpiCard("কমিশন বাকি", "৳ ১২,৪৫০", "↑ +৬.২%", Icons.Default.Person, Color(0xFF00F0FF), Modifier.weight(1f)) { onModuleClick("Module 23") }
                    }
                }

                // D. BOTTOM INTELLIGENCE & WORKFLOW ROW (ADAPTIVE GRID)
                if (isExpanded) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        AffiliateIntelligenceCard(
                            modifier = Modifier.weight(1.5f),
                            onModuleClick = onModuleClick
                        )

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            PriorityAlertsCard(onModuleClick = onModuleClick)
                            QuickControlsCard(onModuleClick = onModuleClick)
                        }
                    }
                } else {
                    AffiliateIntelligenceCard(
                        modifier = Modifier.fillMaxWidth(),
                        onModuleClick = onModuleClick
                    )
                    PriorityAlertsCard(onModuleClick = onModuleClick)
                    QuickControlsCard(onModuleClick = onModuleClick)
                }

                // E. ORDER-TO-DELIVERY 6-STEP WORKFLOW STEPPER BAR
                WorkflowStepperBar(onModuleClick = onModuleClick)
            }
        }
    }
}

// ============================================================================
// 1. LEFT SIDEBAR COMPONENT
// ============================================================================
@Composable
private fun SidebarComponent(
    activeNavTab: String,
    onTabSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .width(250.dp)
            .fillMaxHeight()
            .background(Color(0xFF060D1A))
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo Block
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(Color(0xFF00F0FF).copy(alpha = 0.3f), Color(0xFF0284C7).copy(alpha = 0.1f))
                        )
                    )
                    .border(1.5.dp, Color(0xFF00F0FF), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(38.dp)) {
                    val radius = size.minDimension / 2f
                    val centerPt = Offset(size.width / 2f, size.height / 2f)
                    drawCircle(color = Color(0xFF00F0FF), radius = radius, center = centerPt, style = Stroke(width = 2.dp.toPx()))
                    drawCircle(color = Color(0xFF38BDF8), radius = radius * 0.65f, center = centerPt, style = Stroke(width = 1.5.dp.toPx()))
                    drawCircle(color = Color.White, radius = radius * 0.3f, center = centerPt)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "SUCHARU GRAPHICS",
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 1.sp
            )
            Text(
                text = "PRINTING IDEAS TO REALITY",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF00F0FF),
                letterSpacing = 1.2.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 8 Navigation Tabs
            val navItems = listOf(
                "Dashboard" to Icons.Default.Home,
                "Orders" to Icons.Default.ShoppingCart,
                "Production" to Icons.Default.Settings,
                "Affiliate" to Icons.Default.Person,
                "Finance" to Icons.Default.Star,
                "Reports" to Icons.AutoMirrored.Filled.List,
                "Users" to Icons.Default.Person,
                "Settings" to Icons.Default.Settings
            )

            navItems.forEach { (title, icon) ->
                val isActive = activeNavTab == title
                SidebarNavTile(
                    title = title,
                    icon = icon,
                    isActive = isActive,
                    onClick = { onTabSelected(title) }
                )
                Spacer(modifier = Modifier.height(6.dp))
            }
        }

        // Bottom Admin Welcome Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF0A162B),
            border = BorderStroke(1.dp, Color(0xFF00B4D8).copy(alpha = 0.4f))
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(70.dp)
                        .align(Alignment.BottomCenter)
                ) {
                    val path = Path().apply {
                        moveTo(0f, size.height * 0.7f)
                        cubicTo(
                            size.width * 0.3f, size.height * 0.2f,
                            size.width * 0.7f, size.height * 1.1f,
                            size.width, size.height * 0.5f
                        )
                        lineTo(size.width, size.height)
                        lineTo(0f, size.height)
                        close()
                    }
                    drawPath(
                        path = path,
                        brush = Brush.verticalGradient(
                            listOf(Color(0xFF00F0FF).copy(alpha = 0.25f), Color(0xFF0284C7).copy(alpha = 0.05f))
                        )
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Text(
                        text = "স্বাগতম, অ্যাডমিন",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "সবকিছু নিয়ন্ত্রণে,\nএগিয়ে যাচ্ছে সুচারু",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8),
                        lineHeight = 15.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "QUALITY PRINT • STRONGER BRANDS",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00F0FF).copy(alpha = 0.8f),
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SidebarNavTile(
    title: String,
    icon: ImageVector,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        color = if (isActive) Color(0xFF0284C7) else Color.Transparent,
        border = if (isActive) BorderStroke(1.dp, Color(0xFF00F0FF)) else null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isActive) Color.White else Color(0xFF94A3B8),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                    color = if (isActive) Color.White else Color(0xFF94A3B8)
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = if (isActive) Color.White else Color(0xFF475569),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// ============================================================================
// 2. TOP HEADER COMPONENT
// ============================================================================
@Composable
private fun TopHeaderComponent(
    onOpenCalculator: () -> Unit,
    onModuleClick: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF0F172A),
                border = BorderStroke(1.dp, Color(0xFF00F0FF).copy(alpha = 0.4f)),
                modifier = Modifier
                    .size(48.dp)
                    .clickable { onOpenCalculator() }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = null,
                        tint = Color(0xFF00F0FF),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = "ADMIN OPERATIONS CENTER",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF00F0FF),
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.clickable { onModuleClick("Module 24") }
                )
                Text(
                    text = "নিয়ন্ত্রণ • সমন্বয় • উৎপাদন • উন্নত ভবিষ্যৎ",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF94A3B8)
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Date Picker Pill
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF0F172A),
                border = BorderStroke(1.dp, Color(0xFF1E293B)),
                modifier = Modifier.heightIn(min = 48.dp).clickable { onModuleClick("Module 24") }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        tint = Color(0xFF00F0FF),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "২২ এপ্রিল ২০২৬ - ২২ এপ্রিল ২০২৬",
                        fontSize = 12.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Notification Badge Pill
            Box(modifier = Modifier.clickable { onModuleClick("Module 10") }) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF0F172A),
                    border = BorderStroke(1.dp, Color(0xFF1E293B)),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEF4444))
                        .align(Alignment.TopEnd),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "3", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            // Profile Pill
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF0F172A),
                border = BorderStroke(1.dp, Color(0xFF1E293B)),
                modifier = Modifier.heightIn(min = 48.dp).clickable { onModuleClick("Module 01") }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0284C7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(text = "Admin", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(text = "Super Admin", fontSize = 9.sp, color = Color(0xFF94A3B8))
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// ============================================================================
// 3. TODAY'S OPERATIONS CARD & 24H TREND GRAPH
// ============================================================================
@Composable
private fun TodayOperationsCard(
    modifier: Modifier = Modifier,
    onModuleClick: (String) -> Unit
) {
    Surface(
        modifier = modifier.clickable { onModuleClick("Module 04") },
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF0A1224),
        border = BorderStroke(1.dp, Color(0xFF00B4D8).copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(18.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFF00F0FF))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(text = "আজকের অপারেশন", fontSize = 17.sp, fontWeight = FontWeight.Black, color = Color.White)
                        Text(text = "সব অর্ডার, সব ডিপার্টমেন্ট, এক নজরে", fontSize = 11.sp, color = Color(0xFF94A3B8))
                    }
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF132038),
                    border = BorderStroke(1.dp, Color(0xFF1E293B)),
                    modifier = Modifier.clickable { onModuleClick("Module 04") }
                ) {
                    Text(
                        text = "আজকের ∨",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00F0FF),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Body Layout: Stats + 24H Bézier Curve + Performance Trophy
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "মোট বিক্রি (আজ)", fontSize = 11.sp, color = Color(0xFF94A3B8))
                    Text(text = "৳ ১,৩৭,৫০০", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color.White)
                    Text(text = "↑ +১২.৮% গতকালের তুলনায়", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(text = "মোট অর্ডার", fontSize = 11.sp, color = Color(0xFF94A3B8))
                    Text(text = "৮৪", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color.White)
                    Text(text = "↑ +১২% গতকালের তুলনায়", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                }

                // 24H Trend Bézier Graph
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(260.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "বিক্রয় প্রবণতা (২৪ ঘণ্টা)", fontSize = 10.sp, color = Color(0xFF94A3B8))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF00F0FF).copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, Color(0xFF00F0FF))
                        ) {
                            Text(
                                text = "৳ ১,৩৭,৫০০\n6 PM",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00F0FF),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            verticalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .height(80.dp)
                                .padding(end = 6.dp)
                        ) {
                            Text(text = "200K", fontSize = 8.sp, color = Color(0xFF64748B))
                            Text(text = "100K", fontSize = 8.sp, color = Color(0xFF64748B))
                            Text(text = "0", fontSize = 8.sp, color = Color(0xFF64748B))
                        }

                        Canvas(
                            modifier = Modifier
                                .weight(1f)
                                .height(80.dp)
                        ) {
                            val width = size.width
                            val height = size.height

                            val points = listOf(
                                Offset(0f, height * 0.75f),
                                Offset(width * 0.25f, height * 0.65f),
                                Offset(width * 0.5f, height * 0.35f),
                                Offset(width * 0.75f, height * 0.1f),
                                Offset(width, height * 0.45f)
                            )

                            val path = Path().apply {
                                moveTo(points[0].x, points[0].y)
                                for (i in 0 until points.size - 1) {
                                    val p1 = points[i]
                                    val p2 = points[i + 1]
                                    val cx = (p1.x + p2.x) / 2f
                                    cubicTo(cx, p1.y, cx, p2.y, p2.x, p2.y)
                                }
                            }

                            val fillPath = Path().apply {
                                addPath(path)
                                lineTo(width, height)
                                lineTo(0f, height)
                                close()
                            }

                            drawPath(
                                path = fillPath,
                                brush = Brush.verticalGradient(
                                    listOf(Color(0xFF00F0FF).copy(alpha = 0.35f), Color.Transparent)
                                )
                            )

                            drawPath(
                                path = path,
                                color = Color(0xFF00F0FF),
                                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                            )

                            points.forEachIndexed { index, _ ->
                                val radius = if (index == 3) 5.dp.toPx() else 3.dp.toPx()
                                val color = if (index == 3) Color.White else Color(0xFF00F0FF)
                                drawCircle(color = Color(0xFF00F0FF), radius = radius + 2.dp.toPx(), center = points[index])
                                drawCircle(color = color, radius = radius, center = points[index])
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf("12 AM", "6 AM", "12 PM", "6 PM", "12 AM").forEach { label ->
                            Text(text = label, fontSize = 8.sp, color = Color(0xFF64748B))
                        }
                    }
                }

                // Performance Trophy Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF132038),
                    border = BorderStroke(1.dp, Color(0xFF00F0FF).copy(alpha = 0.3f)),
                    modifier = Modifier.padding(start = 12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF59E0B).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "আজকের পারফরম্যান্স", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        Text(text = "↑ +১২.৮%", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color(0xFF10B981))
                        Text(text = "লক্ষ্যমাত্রার তুলনায়", fontSize = 9.sp, color = Color(0xFF94A3B8))
                    }
                }
            }
        }
    }
}

// ============================================================================
// 4. ORDER HEALTH CARD & DONUT RING
// ============================================================================
@Composable
private fun OrderHealthCard(
    modifier: Modifier = Modifier,
    onModuleClick: (String) -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF0A1224),
        border = BorderStroke(1.dp, Color(0xFF00B4D8).copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Order Health", fontSize = 17.sp, fontWeight = FontWeight.Black, color = Color.White)
                Text(
                    text = "বিস্তারিত দেখুন >",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00F0FF),
                    modifier = Modifier.clickable { onModuleClick("Module 03") }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Donut Circular Chart
                Box(
                    modifier = Modifier
                        .size(125.dp)
                        .clickable { onModuleClick("Module 03") },
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 14.dp.toPx()
                        val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                        val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)

                        // Base track
                        drawArc(
                            color = Color(0xFF1E293B),
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth)
                        )

                        // Arc 1: Completed 72% (259.2 deg)
                        drawArc(
                            color = Color(0xFF00F0FF),
                            startAngle = -90f,
                            sweepAngle = 259.2f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )

                        // Arc 2: In Progress 21% (75.6 deg)
                        drawArc(
                            color = Color(0xFFF59E0B),
                            startAngle = 175f,
                            sweepAngle = 60f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )

                        // Arc 3: Cancelled 7% (25.2 deg)
                        drawArc(
                            color = Color(0xFFEF4444),
                            startAngle = 240f,
                            sweepAngle = 25f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "72%", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color.White)
                        Text(text = "সম্পন্ন", fontSize = 11.sp, color = Color(0xFF00F0FF), fontWeight = FontWeight.Bold)
                    }
                }

                // Legend List
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(start = 12.dp)
                ) {
                    LegendItem(color = Color(0xFF00F0FF), label = "মোট অর্ডার", value = "৮৪", onClick = { onModuleClick("Module 03") })
                    LegendItem(color = Color(0xFF38BDF8), label = "সম্পন্ন", value = "৬১", onClick = { onModuleClick("Module 03") })
                    LegendItem(color = Color(0xFFF59E0B), label = "প্রক্রিয়াধীন", value = "১৮", onClick = { onModuleClick("Module 04") })
                    LegendItem(color = Color(0xFFEF4444), label = "বাতিল", value = "৫", onClick = { onModuleClick("Module 12") })
                }
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String, value: String, onClick: () -> Unit = {}) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .width(130.dp)
            .clickable { onClick() }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = label, fontSize = 11.sp, color = Color(0xFF94A3B8))
        }
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

// ============================================================================
// 5. DYNAMIC KPI CARD
// ============================================================================
@Composable
private fun KpiCard(
    title: String,
    value: String,
    trend: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.heightIn(min = 48.dp).clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF0A1224),
        border = BorderStroke(1.dp, Color(0xFF00B4D8).copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = title, fontSize = 12.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Medium)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = trend, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
            }

            Column(horizontalAlignment = Alignment.End) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color(0xFF00F0FF),
                    modifier = Modifier.size(18.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Mini Sparkline Bars
                Canvas(modifier = Modifier.size(width = 36.dp, height = 20.dp)) {
                    val barWidth = 4.dp.toPx()
                    val heights = listOf(0.4f, 0.7f, 0.5f, 0.9f, 1.0f)
                    heights.forEachIndexed { idx, h ->
                        drawRect(
                            color = if (idx == 4) accentColor else accentColor.copy(alpha = 0.4f),
                            topLeft = Offset(idx * (barWidth + 2.dp.toPx()), size.height * (1f - h)),
                            size = Size(barWidth, size.height * h)
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// 6. AFFILIATE INTELLIGENCE CARD
// ============================================================================
@Composable
private fun AffiliateIntelligenceCard(
    modifier: Modifier = Modifier,
    onModuleClick: (String) -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF0A1224),
        border = BorderStroke(1.dp, Color(0xFF00B4D8).copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(18.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFF00F0FF))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(text = "Affiliate Intelligence", fontSize = 17.sp, fontWeight = FontWeight.Black, color = Color.White)
                        Text(text = "আমাদের পার্টনার, আমাদের শক্তি", fontSize = 11.sp, color = Color(0xFF94A3B8))
                    }
                }

                Text(
                    text = "বিস্তারিত দেখুন >",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00F0FF),
                    modifier = Modifier.clickable { onModuleClick("Module 20") }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Stats Column
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    AffiliateStatRow(title = "সক্রিয় পার্টনার", value = "২৮", trend = "↑ +৮ জন", icon = Icons.Default.Person, onClick = { onModuleClick("Module 20") })
                    AffiliateStatRow(title = "রেফারেল বিক্রি", value = "৳ ১,৭৩,২০০", trend = "↑ +৩২.৫%", icon = Icons.Default.ShoppingCart, onClick = { onModuleClick("Module 20") })
                    AffiliateStatRow(title = "কমিশন প্রদান", value = "৳ ১২,৪৫০", trend = "৫ জন পার্টনারের", icon = Icons.Default.Star, onClick = { onModuleClick("Module 23") })
                }

                // Center Topology Network Canvas
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .clickable { onModuleClick("Module 20") },
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val centerPt = Offset(size.width / 2f, size.height / 2f)
                        val radius = size.width * 0.38f
                        val nodes = 7

                        for (i in 0 until nodes) {
                            val angle = (2 * PI / nodes * i)
                            val x = centerPt.x + radius * cos(angle).toFloat()
                            val y = centerPt.y + radius * sin(angle).toFloat()

                            // Connecting glowing line
                            drawLine(
                                color = Color(0xFF00F0FF).copy(alpha = 0.4f),
                                start = centerPt,
                                end = Offset(x, y),
                                strokeWidth = 1.5.dp.toPx()
                            )

                            // Satellite node
                            drawCircle(color = Color(0xFF0284C7), radius = 10.dp.toPx(), center = Offset(x, y))
                            drawCircle(color = Color(0xFF00F0FF), radius = 10.dp.toPx(), center = Offset(x, y), style = Stroke(width = 1.5.dp.toPx()))
                        }

                        // Central Hub Node
                        drawCircle(color = Color(0xFF0A1224), radius = 32.dp.toPx(), center = centerPt)
                        drawCircle(color = Color(0xFF00F0FF), radius = 32.dp.toPx(), center = centerPt, style = Stroke(width = 2.dp.toPx()))
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = Color(0xFF00F0FF), modifier = Modifier.size(18.dp))
                        Text(text = "২৮", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color.White)
                        Text(text = "সক্রিয় পার্টনার", fontSize = 8.sp, color = Color(0xFF94A3B8))
                    }
                }

                // Right Commission Trend Sparkline Chart
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(180.dp)
                        .clickable { onModuleClick("Module 23") }
                ) {
                    Text(text = "কমিশন প্রবণতা (গত ৭ দিন)", fontSize = 10.sp, color = Color(0xFF94A3B8))
                    Spacer(modifier = Modifier.height(6.dp))

                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(70.dp)
                    ) {
                        val w = size.width
                        val h = size.height
                        val points = listOf(
                            Offset(0f, h * 0.8f),
                            Offset(w * 0.16f, h * 0.7f),
                            Offset(w * 0.33f, h * 0.5f),
                            Offset(w * 0.5f, h * 0.55f),
                            Offset(w * 0.66f, h * 0.3f),
                            Offset(w * 0.83f, h * 0.35f),
                            Offset(w, h * 0.15f)
                        )

                        // Draw background bars
                        val barW = 8.dp.toPx()
                        points.forEach { pt ->
                            drawRect(
                                color = Color(0xFF8B5CF6).copy(alpha = 0.25f),
                                topLeft = Offset(pt.x - barW / 2f, pt.y),
                                size = Size(barW, h - pt.y)
                            )
                        }

                        val path = Path().apply {
                            moveTo(points[0].x, points[0].y)
                            for (i in 0 until points.size - 1) {
                                val p1 = points[i]
                                val p2 = points[i + 1]
                                val cx = (p1.x + p2.x) / 2f
                                cubicTo(cx, p1.y, cx, p2.y, p2.x, p2.y)
                            }
                        }

                        drawPath(path = path, color = Color(0xFFA855F7), style = Stroke(width = 2.dp.toPx()))

                        points.forEach { pt ->
                            drawCircle(color = Color.White, radius = 2.5.dp.toPx(), center = pt)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf("১৬", "১৭", "১৮", "১৯", "২০", "২১", "২২").forEach { day ->
                            Text(text = "$day এপ্রি", fontSize = 7.sp, color = Color(0xFF64748B))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AffiliateStatRow(title: String, value: String, trend: String, icon: ImageVector, onClick: () -> Unit = {}) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(Color(0xFF0284C7).copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = Color(0xFF00F0FF), modifier = Modifier.size(15.dp))
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(text = title, fontSize = 10.sp, color = Color(0xFF94A3B8))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = trend, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
            }
        }
    }
}

// ============================================================================
// 7. PRIORITY ALERTS CARD
// ============================================================================
@Composable
private fun PriorityAlertsCard(onModuleClick: (String) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF0A1224),
        border = BorderStroke(1.dp, Color(0xFF00B4D8).copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Priority Alerts", fontSize = 15.sp, fontWeight = FontWeight.Black, color = Color.White)
                }

                Text(
                    text = "সব দেখুন >",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00F0FF),
                    modifier = Modifier.clickable { onModuleClick("Module 10") }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            val alerts = listOf(
                Tuple5("পেমেন্ট বকেয়া (২ দিনের বেশি)", "১২", Color(0xFFEF4444), Icons.Default.Warning, "Module 09/14"),
                Tuple5("ডেলিভারি দেরি হওয়ার ঝুঁকি", "৫", Color(0xFFF59E0B), Icons.Default.Info, "Module 08/11"),
                Tuple5("ডিজাইন অনুমোদন অপেক্ষমান", "৮", Color(0xFF00F0FF), Icons.Default.Info, "Module 05"),
                Tuple5("কাঁচামাল কম আছে", "৩", Color(0xFFA855F7), Icons.Default.Warning, "Module 19"),
                Tuple5("উৎপাদনে আটকে আছে", "৮", Color(0xFFFF8800), Icons.Default.Settings, "Module 04"),
                Tuple5("আজ সম্পন্ন হওয়ার লক্ষ্যমাত্রা", "১৮", Color(0xFF10B981), Icons.Default.CheckCircle, "Module 04")
            )

            alerts.forEach { (title, count, color, icon, moduleCode) ->
                AlertItemRow(
                    title = title,
                    count = count,
                    color = color,
                    icon = icon,
                    onClick = { onModuleClick(moduleCode) }
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun AlertItemRow(title: String, count: String, color: Color, icon: ImageVector, onClick: () -> Unit = {}) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF111C33),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = title, fontSize = 11.sp, color = Color.White)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = count, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color(0xFF475569), modifier = Modifier.size(14.dp))
            }
        }
    }
}

// ============================================================================
// 8. QUICK CONTROLS CARD
// ============================================================================
@Composable
private fun QuickControlsCard(onModuleClick: (String) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF0A1224),
        border = BorderStroke(1.dp, Color(0xFF00B4D8).copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color(0xFF00F0FF), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Quick Controls", fontSize = 15.sp, fontWeight = FontWeight.Black, color = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "দ্রুত আকর্ষণ, দ্রুত অগ্রগতি", fontSize = 10.sp, color = Color(0xFF94A3B8))
            }

            Spacer(modifier = Modifier.height(10.dp))

            val controls = listOf(
                Tuple3("নতুন অর্ডার", Icons.Default.ShoppingCart, "Module 03"),
                Tuple3("উৎপাদন নিয়ন্ত্রণ", Icons.Default.Settings, "Module 04"),
                Tuple3("আর্থিক ব্যবস্থাপনা", Icons.Default.Star, "Module 09/14"),
                Tuple3("অ্যাফিলিয়েট ব্যবস্থাপনা", Icons.Default.Person, "Module 20"),
                Tuple3("রিপোর্ট এবং বিশ্লেষণ", Icons.AutoMirrored.Filled.List, "Module 24"),
                Tuple3("সিস্টেম সেটিংস", Icons.Default.Settings, "Module 00")
            )

            val cols = 2
            for (i in controls.indices step cols) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val ctrl1 = controls[i]
                    QuickControlPill(label = ctrl1.first, icon = ctrl1.second, modifier = Modifier.weight(1f)) {
                        onModuleClick(ctrl1.third)
                    }
                    if (i + 1 < controls.size) {
                        val ctrl2 = controls[i + 1]
                        QuickControlPill(label = ctrl2.first, icon = ctrl2.second, modifier = Modifier.weight(1f)) {
                            onModuleClick(ctrl2.third)
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun QuickControlPill(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.heightIn(min = 48.dp).clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF132038),
        border = BorderStroke(1.dp, Color(0xFF00B4D8).copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = Color(0xFF00F0FF), modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = label, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }
            Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color(0xFF00F0FF), modifier = Modifier.size(14.dp))
        }
    }
}

// ============================================================================
// 9. ORDER-TO-DELIVERY WORKFLOW STEPPER BAR
// ============================================================================
@Composable
private fun WorkflowStepperBar(onModuleClick: (String) -> Unit) {
    var activeWorkflowStep by remember { mutableStateOf("অর্ডার গ্রহণ") }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF0A1224),
        border = BorderStroke(1.dp, Color(0xFF00B4D8).copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(18.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFF00F0FF))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(text = "অর্ডার থেকে ডেলিভারি (ওয়ার্কফ্লো)", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color.White)
                        Text(text = "প্রতিটি ধাপে নজরদারি, সময়মতো ডেলিভারি", fontSize = 11.sp, color = Color(0xFF94A3B8))
                    }
                }

                Text(
                    text = "সব অর্ডারের অবস্থা দেখুন >",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00F0FF),
                    modifier = Modifier.clickable { onModuleClick("Module 04") }
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 6-Step Pipeline Nodes Row
            val steps = listOf(
                Tuple5("অর্ডার গ্রহণ", "৪৪", Color(0xFF10B981), Icons.AutoMirrored.Filled.List, "Module 03"),
                Tuple5("ডিজাইন", "৬১", Color(0xFF10B981), Icons.Default.Star, "Module 05"),
                Tuple5("প্রিন্টিং", "৩৮", Color(0xFF10B981), Icons.Default.Settings, "Module 04"),
                Tuple5("ফিনিশিং", "২২", Color(0xFFF59E0B), Icons.Default.Settings, "Module 04"),
                Tuple5("প্যাকেজিং", "১৮", Color(0xFF00F0FF), Icons.Default.Info, "Module 07"),
                Tuple5("ডেলিভারি", "১২", Color(0xFF38BDF8), Icons.Default.CheckCircle, "Module 08/11")
            )

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                // Background Glowing Line
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp)
                        .padding(horizontal = 40.dp)
                ) {
                    drawLine(
                        color = Color(0xFF00F0FF).copy(alpha = 0.5f),
                        start = Offset(0f, size.height / 2f),
                        end = Offset(size.width, size.height / 2f),
                        strokeWidth = 3.dp.toPx()
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    steps.forEach { (label, count, color, icon, moduleCode) ->
                        val isSelected = activeWorkflowStep == label

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable {
                                activeWorkflowStep = label
                                onModuleClick(moduleCode)
                            }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(if (isSelected) 52.dp else 46.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) Color(0xFF0284C7) else Color(0xFF0B182E))
                                    .border(if (isSelected) 2.5.dp else 1.5.dp, Color(0xFF00F0FF), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (isSelected) Color.White else Color(0xFF00F0FF),
                                    modifier = Modifier.size(if (isSelected) 24.dp else 22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                color = if (isSelected) Color(0xFF00F0FF) else Color.White
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = count,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
