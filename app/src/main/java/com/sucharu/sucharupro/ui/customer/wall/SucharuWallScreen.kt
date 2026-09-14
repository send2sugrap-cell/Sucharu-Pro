package com.sucharu.sucharupro.ui.customer.wall

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.ui.customer.components.CardSkeletonLoader
import com.sucharu.sucharupro.ui.customer.components.CustomerBottomNavigation
import com.sucharu.sucharupro.ui.customer.components.CustomerBottomTab
import com.sucharu.sucharupro.ui.customer.components.CustomerEmptyState
import com.sucharu.sucharupro.ui.customer.components.CustomerErrorState
import com.sucharu.sucharupro.ui.customer.theme.CustomerColors
import com.sucharu.sucharupro.ui.customer.theme.CustomerTheme
import com.sucharu.sucharupro.ui.customer.wall.components.HomeActivityCard
import com.sucharu.sucharupro.ui.customer.wall.components.HomeBannerPager
import com.sucharu.sucharupro.ui.customer.wall.components.HomeHeader
import com.sucharu.sucharupro.ui.customer.wall.components.HomeHeroCard
import com.sucharu.sucharupro.ui.customer.wall.components.HomeSection
import com.sucharu.sucharupro.ui.customer.wall.components.HomeToolCard
import com.sucharu.sucharupro.ui.customer.wall.components.ProductGridCard
import com.sucharu.sucharupro.ui.customer.wall.components.ServiceGridCard
import com.sucharu.sucharupro.ui.navigation.AppDestination

/**
 * Redesigned Front-Facing Home / Sucharu Wall Presentation Layer.
 *
 * Clean, light/neutral, modern, mobile-first, card-based, vertically scrollable Home experience.
 * Orchestrates general business content + relevant personal activity + tools & support:
 * - Clean Top Header (Branding, Notifications, Profile Avatar)
 * - Hero Quick Overview Card (Welcome, Active Order Tracking, CTA)
 * - Promotional Banner Carousel (HorizontalPager / LazyRow)
 * - Section 1: প্রিন্টিং সার্ভিস (Responsive Printing Service Grid Cards)
 * - Section 2: প্রোডাক্টস (Responsive Product Grid Cards)
 * - Section 3: টুলস ও সাপোর্ট (AI Assistant & Cost Estimator Tool Cards)
 * - Section 4: ব্যাক্তিগত অ্যাক্টিভিটি (Personal Activity Timeline)
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

    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val gridColumns = when {
        screenWidthDp >= 840 -> 4
        screenWidthDp >= 600 -> 3
        else -> 2
    }

    LaunchedEffect(principal) {
        viewModel.loadWallFeed(principal)
    }

    // Light/Neutral Modern Commercial App Theme
    CustomerTheme(colors = CustomerColors.light()) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = CustomerTheme.colors.background,
            topBar = {
                HomeHeader(
                    principal = principal,
                    notificationCount = 0,
                    onProfileClick = { onNavigateToDestination(AppDestination.Customer.Profile) },
                    onNotificationClick = { onNavigateToDestination(AppDestination.Customer.Notifications) }
                )
            },
            bottomBar = {
                CustomerBottomNavigation(
                    selectedTab = selectedTab,
                    onTabSelect = { tab ->
                        selectedTab = tab
                        when (tab) {
                            CustomerBottomTab.HOME -> { /* Stay on Home */ }
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
            when (val state = uiState) {
                is SucharuWallUiState.Loading -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(horizontal = CustomerTheme.spacing.lg)
                    ) {
                        Spacer(modifier = Modifier.height(CustomerTheme.spacing.md))
                        CardSkeletonLoader()
                        Spacer(modifier = Modifier.height(CustomerTheme.spacing.md))
                        CardSkeletonLoader()
                    }
                }

                is SucharuWallUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(horizontal = CustomerTheme.spacing.lg)
                    ) {
                        Spacer(modifier = Modifier.height(CustomerTheme.spacing.md))
                        CustomerErrorState(
                            errorMessage = state.errorMessage,
                            onRetry = { viewModel.refresh(principal) }
                        )
                    }
                }

                is SucharuWallUiState.Empty -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(horizontal = CustomerTheme.spacing.lg)
                    ) {
                        Spacer(modifier = Modifier.height(CustomerTheme.spacing.md))
                        CustomerEmptyState(
                            message = state.message,
                            subtitle = "Check back soon for new offers and printing service updates."
                        )
                    }
                }

                is SucharuWallUiState.Success -> {
                    val feed = state.feedData

                    // Single LazyColumn Vertical Scroll Architecture
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentPadding = PaddingValues(horizontal = CustomerTheme.spacing.lg, vertical = CustomerTheme.spacing.md),
                        verticalArrangement = Arrangement.spacedBy(CustomerTheme.spacing.lg)
                    ) {
                        // 1. Hero Overview Card
                        item {
                            HomeHeroCard(
                                principal = principal,
                                activeOrderNumber = "ORD-000001",
                                activeOrderStatus = "IN PRODUCTION",
                                onTrackOrderClick = { onNavigateToDestination(AppDestination.Customer.Orders) },
                                onNewOrderClick = { onNavigateToDestination(AppDestination.Customer.Quotations) }
                            )
                        }

                        // 2. Promotional Banner Carousel
                        if (feed.offers.isNotEmpty()) {
                            item {
                                HomeBannerPager(
                                    offers = feed.offers,
                                    onOfferClick = { onNavigateToDestination(AppDestination.Public.Offers) }
                                )
                            }
                        }

                        // 3. Section: প্রিন্টিং সার্ভিস (Printing Services)
                        if (feed.services.isNotEmpty()) {
                            item {
                                HomeSection(
                                    title = "প্রিন্টিং সার্ভিস",
                                    subtitle = "কমার্শিয়াল অফসেট, ডিজিটাল ও কাস্টম প্যাকেজিং সার্ভিস",
                                    actionLabel = "সব দেখুন",
                                    onActionClick = { onNavigateToDestination(AppDestination.Public.PrintingServices) }
                                ) {
                                    val chunkedServices = feed.services.chunked(gridColumns)
                                    Column(verticalArrangement = Arrangement.spacedBy(CustomerTheme.spacing.sm)) {
                                        chunkedServices.forEach { rowItems ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(CustomerTheme.spacing.sm)
                                            ) {
                                                rowItems.forEach { srv ->
                                                    Box(modifier = Modifier.weight(1f)) {
                                                        ServiceGridCard(
                                                            item = srv,
                                                            onClick = { onNavigateToDestination(AppDestination.Public.PrintingServices) }
                                                        )
                                                    }
                                                }
                                                for (i in 0 until (gridColumns - rowItems.size)) {
                                                    Spacer(modifier = Modifier.weight(1f))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 4. Section: প্রোডাক্টস (Products)
                        if (feed.products.isNotEmpty()) {
                            item {
                                HomeSection(
                                    title = "প্রোডাক্টস ক্যাটালগ",
                                    subtitle = "ধর্মীয় বই, ডায়েরি, ক্যালেন্ডার ও কর্পোরেট গিফট প্রোডাক্টস",
                                    actionLabel = "সব দেখুন",
                                    onActionClick = { onNavigateToDestination(AppDestination.Public.Products) }
                                ) {
                                    val chunkedProducts = feed.products.chunked(gridColumns)
                                    Column(verticalArrangement = Arrangement.spacedBy(CustomerTheme.spacing.sm)) {
                                        chunkedProducts.forEach { rowItems ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(CustomerTheme.spacing.sm)
                                            ) {
                                                rowItems.forEach { prod ->
                                                    Box(modifier = Modifier.weight(1f)) {
                                                        ProductGridCard(
                                                            item = prod,
                                                            onClick = { onNavigateToDestination(AppDestination.Public.Products) }
                                                        )
                                                    }
                                                }
                                                for (i in 0 until (gridColumns - rowItems.size)) {
                                                    Spacer(modifier = Modifier.weight(1f))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 5. Section: টুলস ও সাপোর্ট (Tools & Support)
                        item {
                            HomeSection(
                                title = "টুলস ও সাপোর্ট",
                                subtitle = "এআই অ্যাসিস্ট্যান্ট, কস্ট ক্যালকুলেটর ও কুইক সাপোর্ট"
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(CustomerTheme.spacing.sm)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(CustomerTheme.spacing.sm)
                                    ) {
                                        Box(modifier = Modifier.weight(1f)) {
                                            HomeToolCard(
                                                title = "AI Assistant",
                                                description = "স্মার্ট প্রিন্টিং এআই এডভাইজর",
                                                icon = Icons.Default.AutoAwesome,
                                                accentColor = CustomerTheme.colors.accentPurple,
                                                onClick = { onNavigateToDestination(AppDestination.Public.PublicAiAssistant) }
                                            )
                                        }
                                        Box(modifier = Modifier.weight(1f)) {
                                            HomeToolCard(
                                                title = "Cost Estimator",
                                                description = "প্রিন্টিং খরচ ও প্রাইস ক্যালকুলেটর",
                                                icon = Icons.Default.Calculate,
                                                accentColor = CustomerTheme.colors.accentPrimary,
                                                onClick = { onNavigateToDestination(AppDestination.Customer.Quotations) }
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(CustomerTheme.spacing.sm)
                                    ) {
                                        Box(modifier = Modifier.weight(1f)) {
                                            HomeToolCard(
                                                title = "কোটেসন রিকুয়েস্ট",
                                                description = "কাস্টম প্রিন্টিং এর দাম জানুন",
                                                icon = Icons.Default.ShoppingCart,
                                                accentColor = CustomerTheme.colors.success,
                                                onClick = { onNavigateToDestination(AppDestination.Customer.Quotations) }
                                            )
                                        }
                                        Box(modifier = Modifier.weight(1f)) {
                                            HomeToolCard(
                                                title = "হেল্পলাইন সাপোর্ট",
                                                description = "কাস্টমার কেয়ার ও মেসেজিং",
                                                icon = Icons.Default.Call,
                                                accentColor = CustomerTheme.colors.warning,
                                                onClick = { onNavigateToDestination(AppDestination.Customer.Support) }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 6. Section: ব্যাক্তিগত অ্যাক্টিভিটি (Personal Activity Timeline)
                        if (feed.personalActivities.isNotEmpty()) {
                            item {
                                HomeSection(
                                    title = "ব্যাক্তিগত অ্যাক্টিভিটি",
                                    subtitle = "আপনার সাম্প্রতিক অর্ডার ও ওয়ালেট আপডেট"
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(CustomerTheme.spacing.sm)) {
                                        feed.personalActivities.forEach { act ->
                                            HomeActivityCard(
                                                item = act,
                                                onClick = { onNavigateToDestination(AppDestination.Customer.Orders) }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(CustomerTheme.spacing.xl))
                        }
                    }
                }
            }
        }
    }
}
