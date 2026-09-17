package com.sucharu.sucharupro.ui.customer.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.ui.customer.components.ActivityCard
import com.sucharu.sucharupro.ui.customer.components.AnnouncementCard
import com.sucharu.sucharupro.ui.customer.components.CardSkeletonLoader
import com.sucharu.sucharupro.ui.customer.components.CustomerBottomNavigation
import com.sucharu.sucharupro.ui.customer.components.CustomerBottomTab
import com.sucharu.sucharupro.ui.customer.components.CustomerEmptyState
import com.sucharu.sucharupro.ui.customer.components.FeaturedOfferCard
import com.sucharu.sucharupro.ui.customer.components.MobileTopBar
import com.sucharu.sucharupro.ui.customer.components.ProfileHeader
import com.sucharu.sucharupro.ui.customer.components.QuickActionCard
import com.sucharu.sucharupro.ui.customer.components.ServiceCard
import com.sucharu.sucharupro.ui.customer.theme.CustomerTheme

/**
 * Mobile-First Foundation Workspace Screen for Sucharu Pro Customer & Affiliate User Experiences.
 *
 * Demonstrates and verifies the shared UI/UX foundation:
 * - CustomerTheme (CustomerColors, CustomerTypography, CustomerSpacing)
 * - MobileTopBar & CustomerBottomNavigation
 * - ProfileHeader with principal identity
 * - FeaturedOfferCard, QuickActionCard, ServiceCard, ActivityCard, and AnnouncementCard
 */
@Composable
fun CustomerAffiliateFoundationScreen(
    principal: AuthenticatedPrincipal?,
    modifier: Modifier = Modifier,
    onNavigateBack: (() -> Unit)? = null
) {
    var selectedTab by remember { mutableStateOf(CustomerBottomTab.AI_ASSISTANT) }
    var isLoading by remember { mutableStateOf(false) }
    var showEmptyState by remember { mutableStateOf(false) }

    CustomerTheme {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = CustomerTheme.colors.background,
            topBar = {
                MobileTopBar(
                    title = "Sucharu Pro Services",
                    onBackClick = onNavigateBack
                )
            },
            bottomBar = {
                CustomerBottomNavigation(
                    selectedTab = selectedTab,
                    onTabSelect = { selectedTab = it },
                    userRole = principal?.role
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = CustomerTheme.spacing.lg)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(CustomerTheme.spacing.md))

                // 1. Profile Header
                ProfileHeader(
                    principal = principal,
                    notificationCount = 0,
                    onNotificationsClick = { }
                )

                Spacer(modifier = Modifier.height(CustomerTheme.spacing.lg))

                if (isLoading) {
                    CardSkeletonLoader()
                    Spacer(modifier = Modifier.height(CustomerTheme.spacing.md))
                    CardSkeletonLoader()
                } else if (showEmptyState) {
                    CustomerEmptyState(
                        message = "No Active Offers or Orders Found",
                        subtitle = "Check back soon for new printing discounts and service updates."
                    )
                } else {
                    // 2. Featured Offer Banner
                    FeaturedOfferCard(
                        title = "Eid Special Bulk Printing Offer",
                        description = "Get 15% discount on custom business cards and brochures.",
                        discountTag = "15% DISCOUNT",
                        actionLabel = "Order Now",
                        onActionClick = { }
                    )

                    Spacer(modifier = Modifier.height(CustomerTheme.spacing.lg))

                    // 3. Quick Actions Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(CustomerTheme.spacing.sm)
                    ) {
                        QuickActionCard(
                            label = "New Order",
                            icon = Icons.Default.ShoppingCart,
                            onClick = { },
                            modifier = Modifier.weight(1f)
                        )
                        QuickActionCard(
                            label = "My Orders",
                            icon = Icons.Default.ShoppingCart,
                            onClick = { },
                            modifier = Modifier.weight(1f)
                        )
                        QuickActionCard(
                            label = "Special Offers",
                            icon = Icons.Default.LocalOffer,
                            onClick = { },
                            modifier = Modifier.weight(1f)
                        )
                        QuickActionCard(
                            label = "Rewards",
                            icon = Icons.Default.CardGiftcard,
                            onClick = { },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(CustomerTheme.spacing.lg))

                    // 4. Featured Printing Services
                    ServiceCard(
                        title = "Custom Business Cards & Stationery",
                        description = "Premium 300GSM art card printing with matte/gloss lamination",
                        icon = Icons.Default.Print
                    )

                    Spacer(modifier = Modifier.height(CustomerTheme.spacing.md))

                    ServiceCard(
                        title = "Corporate Brochure & Packaging",
                        description = "Full color offset printing with precision fold & die-cut packaging",
                        icon = Icons.Default.Palette
                    )

                    Spacer(modifier = Modifier.height(CustomerTheme.spacing.lg))

                    // 5. Personal Activity Feed
                    ActivityCard(
                        title = "Commercial Order #ORD-000001 Confirmed",
                        timestamp = "Today, 10:30 AM",
                        icon = Icons.Default.History,
                        statusLabel = "CONFIRMED"
                    )

                    Spacer(modifier = Modifier.height(CustomerTheme.spacing.md))

                    // 6. System Announcement
                    AnnouncementCard(
                        title = "New Digital Press Equipment Added",
                        message = "We have added a high-speed HP Indigo Digital Press for faster same-day order deliveries.",
                        dateFormatted = "12 Sept 2026",
                        tag = "EQUIPMENT UPGRADE"
                    )
                }

                Spacer(modifier = Modifier.height(CustomerTheme.spacing.xxl))
            }
        }
    }
}
