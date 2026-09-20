package com.sucharu.sucharupro.ui.admin.shell

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.ui.navigation.AppDestination

/**
 * Reusable Nav Item Data Class for Custom Sidebar Items.
 */
data class CustomAdminSidebarItem(
    val destination: AppDestination,
    val title: String,
    val icon: ImageVector
)

/**
 * Reusable Admin Navigation Sidebar matching reference design image (Desktop Operations Shell).
 */
@Composable
fun AdminSidebar(
    currentDestination: AppDestination,
    principal: AuthenticatedPrincipal?,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false,
    onDestinationSelect: (destination: AppDestination) -> Unit,
    onToggleCompact: (() -> Unit)? = null
) {
    val navItems = remember {
        listOf(
            CustomAdminSidebarItem(AppDestination.Admin.FullAdministration, "Dashboard", Icons.Default.Dashboard),
            CustomAdminSidebarItem(AppDestination.Customer.Orders, "Orders", Icons.Default.Print),
            CustomAdminSidebarItem(AppDestination.Admin.PrepressOrchestration, "Production", Icons.Default.Print),
            CustomAdminSidebarItem(AppDestination.Admin.AffiliateManagement, "Affiliate", Icons.Default.Campaign),
            CustomAdminSidebarItem(AppDestination.Admin.Finance, "Finance", Icons.Default.AccountBalanceWallet),
            CustomAdminSidebarItem(AppDestination.Admin.Reports, "Reports", Icons.Default.Analytics),
            CustomAdminSidebarItem(AppDestination.Admin.Users, "Users", Icons.Default.Group),
            CustomAdminSidebarItem(AppDestination.Admin.Settings, "Settings", Icons.Default.ChevronRight)
        )
    }

    val sidebarWidth = if (isCompact) 72.dp else 240.dp

    Surface(
        modifier = modifier
            .width(sidebarWidth)
            .fillMaxHeight(),
        color = Color(0xFF070E1E),
        border = BorderStroke(1.dp, Color(0xFF00B4D8).copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(vertical = 12.dp)
        ) {
            // BRAND LOGO & TITLE
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (isCompact) Arrangement.Center else Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00B4D8).copy(alpha = 0.2f))
                            .border(1.5.dp, Color(0xFF38BDF8), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Print,
                            contentDescription = "Sucharu Graphics",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    if (!isCompact) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "SUCHARU GRAPHICS",
                                fontSize = 12.sp,
                                lineHeight = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                letterSpacing = 0.3.sp
                            )
                            Text(
                                text = "PRINTING IDEAS TO REALITY",
                                fontSize = 7.5.sp,
                                lineHeight = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF38BDF8),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                if (!isCompact && onToggleCompact != null) {
                    IconButton(onClick = onToggleCompact, modifier = Modifier.size(26.dp)) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Compact Sidebar",
                            tint = Color(0xFF94A3B8)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // MAIN NAVIGATION ITEMS
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                navItems.forEach { item ->
                    val isSelected = currentDestination.route == item.destination.route ||
                        (item.destination == AppDestination.Admin.FullAdministration && currentDestination == AppDestination.Admin.FullAdministration)

                    val containerBg = if (isSelected) {
                        Brush.horizontalGradient(
                            listOf(Color(0xFF00B4D8).copy(alpha = 0.3f), Color(0xFF0284C7).copy(alpha = 0.1f))
                        )
                    } else {
                        Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(containerBg)
                            .border(
                                border = BorderStroke(
                                    width = if (isSelected) 1.dp else 0.dp,
                                    color = if (isSelected) Color(0xFF38BDF8).copy(alpha = 0.8f) else Color.Transparent
                                ),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { onDestinationSelect(item.destination) }
                            .padding(horizontal = 10.dp, vertical = 9.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = if (isCompact) Arrangement.Center else Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title,
                                    tint = if (isSelected) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                                    modifier = Modifier.size(18.dp)
                                )

                                if (!isCompact) {
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = item.title,
                                        fontSize = 12.sp,
                                        lineHeight = 14.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else Color(0xFFCBD5E1),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            if (!isCompact && isSelected) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // BOTTOM WELCOME PROMO CARD (Matching Image 1)
            if (!isCompact) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF0F172A),
                    border = BorderStroke(1.dp, Color(0xFF00B4D8).copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp)
                    ) {
                        Text(
                            text = "স্বাগতম, অ্যাডমিন",
                            fontSize = 12.sp,
                            lineHeight = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "সবকিছু নিয়ন্ত্রণে, এগিয়ে যাচ্ছে সুচারু",
                            fontSize = 9.sp,
                            lineHeight = 11.sp,
                            color = Color(0xFF94A3B8),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(5.dp))
                        Text(
                            text = "QUALITY PRINT • STRONGER BRANDS",
                            fontSize = 7.5.sp,
                            lineHeight = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8),
                            letterSpacing = 0.3.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
