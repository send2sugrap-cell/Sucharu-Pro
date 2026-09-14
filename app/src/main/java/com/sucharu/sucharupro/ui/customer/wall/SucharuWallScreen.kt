package com.sucharu.sucharupro.ui.customer.wall

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
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.ui.customer.components.ActivityCard
import com.sucharu.sucharupro.ui.customer.components.AnnouncementCard
import com.sucharu.sucharupro.ui.customer.components.CardSkeletonLoader
import com.sucharu.sucharupro.ui.customer.components.CustomerBottomNavigation
import com.sucharu.sucharupro.ui.customer.components.CustomerBottomTab
import com.sucharu.sucharupro.ui.customer.components.CustomerEmptyState
import com.sucharu.sucharupro.ui.customer.components.CustomerErrorState
import com.sucharu.sucharupro.ui.customer.components.FeaturedOfferCard
import com.sucharu.sucharupro.ui.customer.components.MobileTopBar
import com.sucharu.sucharupro.ui.customer.components.ProductCard
import com.sucharu.sucharupro.ui.customer.components.ProfileHeader
import com.sucharu.sucharupro.ui.customer.components.QuickActionCard
import com.sucharu.sucharupro.ui.customer.components.ServiceCard
import com.sucharu.sucharupro.ui.customer.theme.CustomerTheme
import com.sucharu.sucharupro.ui.navigation.AppDestination

/**
 * Primary Authenticated Home Experience — Sucharu Wall / Universal Mobile Home.
 *
 * Renders general business content + relevant personal activity + touch-friendly quick actions:
 * - Profile Header & Notifications
 * - Personal Activity Timeline (Capability & Role Guarded)
 * - Featured Offers & Discount Banners
 * - Services & Products Showcase
 * - Sucharu Updates & Announcements
 * - Role-Aware Quick Actions (Customer vs Affiliate)
 */
@Composable
fun SucharuWallScreen(
    viewModel: SucharuWallViewModel,
    principal: AuthenticatedPrincipal?,
    modifier: Modifier = Modifier,
    onNavigateToDestination: (destination: AppDestination) -> Unit = {},
    onNavigateBack: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(CustomerBottomTab.HOME) }

    LaunchedEffect(principal) {
        viewModel.loadWallFeed(principal)
    }

    CustomerTheme {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = CustomerTheme.colors.background,
            topBar = {
                MobileTopBar(
                    title = "Sucharu Wall",
                    onBackClick = onNavigateBack
                )
            },
            bottomBar = {
                CustomerBottomNavigation(
                    selectedTab = selectedTab,
                    onTabSelect = { tab ->
                        selectedTab = tab
                        when (tab) {
                            CustomerBottomTab.HOME -> { /* Stay on Wall */ }
                            CustomerBottomTab.SERVICES -> onNavigateToDestination(AppDestination.Public.PrintingServices)
                            CustomerBottomTab.OFFERS -> onNavigateToDestination(AppDestination.Public.Offers)
                            CustomerBottomTab.ACTIVITY -> onNavigateToDestination(AppDestination.Customer.Orders)
                            CustomerBottomTab.ACCOUNT -> onNavigateToDestination(AppDestination.Customer.Profile)
                        }
                    },
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

                // 1. Profile Avatar Header & Notification Bell
                ProfileHeader(
                    principal = principal,
                    notificationCount = 0,
                    onNotificationsClick = {
                        onNavigateToDestination(AppDestination.Customer.Notifications)
                    }
                )

                Spacer(modifier = Modifier.height(CustomerTheme.spacing.lg))

                when (val state = uiState) {
                    is SucharuWallUiState.Loading -> {
                        CardSkeletonLoader()
                        Spacer(modifier = Modifier.height(CustomerTheme.spacing.md))
                        CardSkeletonLoader()
                    }

                    is SucharuWallUiState.Error -> {
                        CustomerErrorState(
                            errorMessage = state.errorMessage,
                            onRetry = { viewModel.refresh(principal) }
                        )
                    }

                    is SucharuWallUiState.Empty -> {
                        CustomerEmptyState(
                            message = state.message,
                            subtitle = "Check back soon for new offers and printing service updates."
                        )
                    }

                    is SucharuWallUiState.Success -> {
                        val feed = state.feedData

                        // 2. Role-Aware Touch-Friendly Quick Action Command Bar
                        WallQuickActionsSection(
                            role = principal?.role ?: UserRole.CUSTOMER,
                            onNavigateToDestination = onNavigateToDestination
                        )

                        Spacer(modifier = Modifier.height(CustomerTheme.spacing.lg))

                        // 3. Relevant Personal Activity Section
                        if (feed.personalActivities.isNotEmpty()) {
                            WallSectionHeader(title = "Personal Activity Timeline", subtitle = "Your recent order & account updates")
                            Spacer(modifier = Modifier.height(CustomerTheme.spacing.sm))
                            feed.personalActivities.forEach { act ->
                                ActivityCard(
                                    title = act.title,
                                    timestamp = act.timestamp,
                                    icon = Icons.Default.History,
                                    statusLabel = act.statusLabel
                                )
                                Spacer(modifier = Modifier.height(CustomerTheme.spacing.sm))
                            }
                            Spacer(modifier = Modifier.height(CustomerTheme.spacing.md))
                        }

                        // 4. Featured / Today's Offer Section
                        if (feed.featuredOffer != null) {
                            WallSectionHeader(title = "Featured Offer & Promotions", subtitle = "Exclusive discounts for bulk printing")
                            Spacer(modifier = Modifier.height(CustomerTheme.spacing.sm))
                            FeaturedOfferCard(
                                title = feed.featuredOffer.title,
                                description = feed.featuredOffer.description,
                                discountTag = feed.featuredOffer.discountTag,
                                actionLabel = "Order Now",
                                onActionClick = { onNavigateToDestination(AppDestination.Public.Offers) }
                            )
                            Spacer(modifier = Modifier.height(CustomerTheme.spacing.lg))
                        }

                        // 5. Featured Printing Services Section
                        if (feed.services.isNotEmpty()) {
                            WallSectionHeader(title = "Printing & Packaging Services", subtitle = "Commercial offset, digital, and custom packaging")
                            Spacer(modifier = Modifier.height(CustomerTheme.spacing.sm))
                            feed.services.forEach { srv ->
                                ServiceCard(
                                    title = srv.title,
                                    description = srv.description,
                                    icon = Icons.Default.Print,
                                    onClick = { onNavigateToDestination(AppDestination.Public.PrintingServices) }
                                )
                                Spacer(modifier = Modifier.height(CustomerTheme.spacing.sm))
                            }
                            Spacer(modifier = Modifier.height(CustomerTheme.spacing.md))
                        }

                        // 6. Featured Products Showcase Section
                        if (feed.products.isNotEmpty()) {
                            WallSectionHeader(title = "Featured Product Catalogue", subtitle = "Religious books, diaries, calendars, and corporate gifts")
                            Spacer(modifier = Modifier.height(CustomerTheme.spacing.sm))
                            feed.products.forEach { prod ->
                                ProductCard(
                                    title = prod.title,
                                    category = prod.category,
                                    priceFormatted = prod.priceFormatted,
                                    onClick = { onNavigateToDestination(AppDestination.Public.Products) }
                                )
                                Spacer(modifier = Modifier.height(CustomerTheme.spacing.sm))
                            }
                            Spacer(modifier = Modifier.height(CustomerTheme.spacing.md))
                        }

                        // 7. Sucharu Updates & Announcements Section
                        if (feed.announcements.isNotEmpty()) {
                            WallSectionHeader(title = "Sucharu Pro News & Updates", subtitle = "Equipment additions & service announcements")
                            Spacer(modifier = Modifier.height(CustomerTheme.spacing.sm))
                            feed.announcements.forEach { ann ->
                                AnnouncementCard(
                                    title = ann.title,
                                    message = ann.message,
                                    dateFormatted = ann.dateFormatted,
                                    tag = ann.tag
                                )
                                Spacer(modifier = Modifier.height(CustomerTheme.spacing.sm))
                            }
                        }

                        Spacer(modifier = Modifier.height(CustomerTheme.spacing.xxl))
                    }
                }
            }
        }
    }
}

@Composable
private fun WallSectionHeader(title: String, subtitle: String) {
    Column {
        Text(text = title, style = CustomerTheme.typography.sectionHeader, color = CustomerTheme.colors.primaryText)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = subtitle, style = CustomerTheme.typography.caption, color = CustomerTheme.colors.secondaryText)
    }
}

@Composable
private fun WallQuickActionsSection(
    role: UserRole,
    onNavigateToDestination: (AppDestination) -> Unit
) {
    Column {
        Text(
            text = "Quick Actions",
            style = CustomerTheme.typography.sectionHeader,
            color = CustomerTheme.colors.primaryText
        )
        Spacer(modifier = Modifier.height(CustomerTheme.spacing.sm))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(CustomerTheme.spacing.sm)
        ) {
            if (role == UserRole.AFFILIATE) {
                QuickActionCard(
                    label = "Share Link",
                    icon = Icons.Default.Share,
                    onClick = { onNavigateToDestination(AppDestination.Affiliate.ReferralLinks) },
                    modifier = Modifier.weight(1f)
                )
                QuickActionCard(
                    label = "Referrals",
                    icon = Icons.Default.Campaign,
                    onClick = { onNavigateToDestination(AppDestination.Affiliate.Referrals) },
                    modifier = Modifier.weight(1f)
                )
                QuickActionCard(
                    label = "Commissions",
                    icon = Icons.Default.LocalOffer,
                    onClick = { onNavigateToDestination(AppDestination.Affiliate.Commission) },
                    modifier = Modifier.weight(1f)
                )
                QuickActionCard(
                    label = "Payouts",
                    icon = Icons.Default.Wallet,
                    onClick = { onNavigateToDestination(AppDestination.Affiliate.Payouts) },
                    modifier = Modifier.weight(1f)
                )
            } else {
                QuickActionCard(
                    label = "New Order",
                    icon = Icons.Default.ShoppingCart,
                    onClick = { onNavigateToDestination(AppDestination.Customer.Quotations) },
                    modifier = Modifier.weight(1f)
                )
                QuickActionCard(
                    label = "My Orders",
                    icon = Icons.Default.ShoppingCart,
                    onClick = { onNavigateToDestination(AppDestination.Customer.Orders) },
                    modifier = Modifier.weight(1f)
                )
                QuickActionCard(
                    label = "Special Offers",
                    icon = Icons.Default.LocalOffer,
                    onClick = { onNavigateToDestination(AppDestination.Public.Offers) },
                    modifier = Modifier.weight(1f)
                )
                QuickActionCard(
                    label = "Rewards",
                    icon = Icons.Default.CardGiftcard,
                    onClick = { onNavigateToDestination(AppDestination.Customer.Invoices) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
