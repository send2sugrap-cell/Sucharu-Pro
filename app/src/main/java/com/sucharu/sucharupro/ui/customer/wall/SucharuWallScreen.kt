package com.sucharu.sucharupro.ui.customer.wall

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.ui.customer.components.CardSkeletonLoader
import com.sucharu.sucharupro.ui.customer.components.CustomerBottomNavigation
import com.sucharu.sucharupro.ui.customer.components.CustomerBottomTab
import com.sucharu.sucharupro.ui.customer.components.CustomerEmptyState
import com.sucharu.sucharupro.ui.customer.components.CustomerErrorState
import com.sucharu.sucharupro.ui.customer.theme.CustomerColors
import com.sucharu.sucharupro.ui.customer.theme.CustomerTheme
import com.sucharu.sucharupro.ui.customer.wall.components.DailyWisdomCard
import com.sucharu.sucharupro.ui.customer.wall.components.HomeBannerPager
import com.sucharu.sucharupro.ui.customer.wall.components.HomeHeader
import com.sucharu.sucharupro.ui.customer.wall.components.PrayerTimesCard
import com.sucharu.sucharupro.ui.customer.wall.components.RunningOffersCarousel
import com.sucharu.sucharupro.ui.navigation.AppDestination

/**
 * Sucharu Normal Public Wall (Final Locked Composition Order & Design).
 *
 * FINAL LOCKED SECTION ORDER:
 * 1. HEADER (HomeHeader)
 * 2. MONISHIR BANI + DATE (DailyWisdomCard + 3-Calendar Columns)
 * 3. RUNNING PRAYER-TIME WIDGET (PrayerTimesCard - LIVE DATA)
 * 4. HERO SIGNBOARD / BANNER (HomeBannerPager)
 * 5. চলমান অফার (RunningOffersCarousel)
 * 6. PRINTING SERVICES (2x2 Large Branded Cards)
 * 7. POPULAR PRODUCTS (Popular Products)
 * 8. BOTTOM NAVIGATION (CustomerBottomNavigation - Home Only)
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
    var selectedTab by remember { mutableStateOf(CustomerBottomTab.AI_ASSISTANT) }
    var showFullPrayerCalendar by remember { mutableStateOf(false) }

    if (showFullPrayerCalendar) {
        com.sucharu.sucharupro.ui.customer.wall.components.FullPrayerCalendarSheet(
            onDismiss = { showFullPrayerCalendar = false }
        )
    }

    LaunchedEffect(principal) {
        viewModel.loadWallFeed(principal)
    }

    CustomerTheme(colors = CustomerColors.light()) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = Color(0xFF0B132B),
            topBar = {
                HomeHeader(
                    principal = principal,
                    notificationCount = 0,
                    onLogoClick = {
                        /* Logo click action (e.g. reload feed or stay home) */
                    },
                    onProfileClick = {
                        if (principal != null) {
                            onNavigateToDestination(AppDestination.Customer.Profile)
                        } else {
                            onNavigateToDestination(AppDestination.Public.Login)
                        }
                    },
                    onNotificationClick = {
                        if (principal != null) {
                            onNavigateToDestination(AppDestination.Customer.Notifications)
                        } else {
                            onNavigateToDestination(AppDestination.Public.Login)
                        }
                    }
                )
            },
            bottomBar = {
                CustomerBottomNavigation(
                    selectedTab = selectedTab,
                    onTabSelect = { tab ->
                        selectedTab = tab
                        when (tab) {
                            CustomerBottomTab.AI_ASSISTANT -> {
                                when (principal?.role) {
                                    UserRole.CUSTOMER -> onNavigateToDestination(AppDestination.Customer.AiAssistant)
                                    UserRole.AFFILIATE -> onNavigateToDestination(AppDestination.Affiliate.AiAssistant)
                                    UserRole.STAFF, UserRole.MANAGER, UserRole.ADMIN -> onNavigateToDestination(AppDestination.Admin.PrepressOrchestration)
                                    else -> onNavigateToDestination(AppDestination.Public.PublicAiAssistant)
                                }
                            }
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
                            .padding(horizontal = 16.dp)
                    ) {
                        Spacer(modifier = Modifier.height(12.dp))
                        CardSkeletonLoader()
                        Spacer(modifier = Modifier.height(12.dp))
                        CardSkeletonLoader()
                    }
                }

                is SucharuWallUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(horizontal = 16.dp)
                    ) {
                        Spacer(modifier = Modifier.height(12.dp))
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
                            .padding(horizontal = 16.dp)
                    ) {
                        Spacer(modifier = Modifier.height(12.dp))
                        CustomerEmptyState(
                            message = state.message,
                            subtitle = "Check back soon for new offers and printing service updates."
                        )
                    }
                }

                is SucharuWallUiState.Success -> {
                    val feed = state.feedData

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 12.dp,
                            end = 12.dp,
                            top = innerPadding.calculateTopPadding() + 4.dp,
                            bottom = innerPadding.calculateBottomPadding() + 4.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // ১. বাণী চিরন্তন + কারেন্ট ৩-ক্যালেন্ডার ডেট (DailyWisdomCard)
                        item {
                            DailyWisdomCard()
                        }

                        // ২. রানিং প্লেয়ার নামাজের সময়সূচি (PrayerTimesCard - LIVE DATA DYNAMIC WAQT)
                        item {
                            PrayerTimesCard(
                                onCalendarClick = { showFullPrayerCalendar = true }
                            )
                        }

                        // ৩. হিরো ব্যানার স্লাইডার (Hero Banner Pager - IMMEDIATELY AFTER PRAYER)
                        if (feed.offers.isNotEmpty()) {
                            item {
                                HomeBannerPager(
                                    offers = feed.offers,
                                    onOfferClick = { onNavigateToDestination(AppDestination.Public.ProductGallery("Card", "আজকের বিশেষ অফার")) }
                                )
                            }
                        }

                        // ৪. চলমান অফার (Running Offers Carousel)
                        if (feed.offers.isNotEmpty()) {
                            item {
                                RunningOffersCarousel(
                                    offers = feed.offers,
                                    onOfferClick = { onNavigateToDestination(AppDestination.Public.ProductGallery("Card", "আজকের বিশেষ অফার")) }
                                )
                            }
                        }

                        // ৫. আমাদের সেবা সমূহ (2x2 Large Branded Printing Services Cards)
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = "👤", fontSize = 16.sp)
                                    Spacer(modifier = Modifier.padding(horizontal = 3.dp))
                                    Text(
                                        text = "আমাদের সেবা সমূহ",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                Text(
                                    text = "সব দেখুন >",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF38BDF8),
                                    modifier = Modifier.clickable {
                                        onNavigateToDestination(AppDestination.Public.PrintingServices)
                                    }
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            ServiceBrandedCardGrid(onNavigateToDestination = onNavigateToDestination)
                        }

                        // ৬. জনপ্রিয় পণ্যসমূহ (Popular Products Grid Rows)
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = "⭐", fontSize = 16.sp)
                                    Spacer(modifier = Modifier.padding(horizontal = 3.dp))
                                    Text(
                                        text = "জনপ্রিয় পণ্যসমূহ",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                Text(
                                    text = "সব দেখুন >",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF38BDF8),
                                    modifier = Modifier.clickable {
                                        onNavigateToDestination(AppDestination.Public.Products)
                                    }
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            val productsRow1 = listOf(
                                GridMenuItem("ভিজিটিং কার্ড", Icons.Default.Badge, Color(0xFF1E293B), Color(0xFF38BDF8)) {
                                    onNavigateToDestination(AppDestination.Public.ProductGallery("Card", "ভিজিটিং কার্ড"))
                                },
                                GridMenuItem("ব্রোশিওর", Icons.Default.Book, Color(0xFF1E293B), Color(0xFFF97316)) {
                                    onNavigateToDestination(AppDestination.Public.ProductGallery("Brochure", "ব্রোশিওর ও ফ্লায়ার"))
                                },
                                GridMenuItem("রিজিড বক্স", Icons.Default.Archive, Color(0xFF1E293B), Color(0xFFEAB308)) {
                                    onNavigateToDestination(AppDestination.Public.ProductGallery("RigidBox", "রিজিড বক্স"))
                                },
                                GridMenuItem("ট্যাগ / লেবেল", Icons.Default.Sell, Color(0xFF1E293B), Color(0xFFA855F7)) {
                                    onNavigateToDestination(AppDestination.Public.ProductGallery("Tag", "ট্যাগ ও লেবেল"))
                                }
                            )
                            val productsRow2 = listOf(
                                GridMenuItem("চালান বই", Icons.Default.Receipt, Color(0xFF1E293B), Color(0xFF3B82F6)) {
                                    onNavigateToDestination(AppDestination.Public.ProductGallery("Challan", "চালান ও ক্যাশ মেমো"))
                                },
                                GridMenuItem("৩ডি লেটার", Icons.Default.Build, Color(0xFF1E293B), Color(0xFFF59E0B)) {
                                    onNavigateToDestination(AppDestination.Public.ProductGallery("3D", "৩ডি সাইন লেটার"))
                                },
                                GridMenuItem("স্টিকার", Icons.Default.ThumbUp, Color(0xFF1E293B), Color(0xFF10B981)) {
                                    onNavigateToDestination(AppDestination.Public.ProductGallery("Sticker", "ডাই-কাট স্টিকার"))
                                },
                                GridMenuItem("অন্যান্য", Icons.Default.Category, Color(0xFF1E293B), Color(0xFF94A3B8)) {
                                    onNavigateToDestination(AppDestination.Public.ProductGallery("Others", "অন্যান্য সামগ্রী"))
                                }
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                StandardGridRow(items = productsRow1)
                                StandardGridRow(items = productsRow2)
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
            }
        }
    }
}

/**
 * 2x2 Large Branded Printing Services Cards Grid.
 */
@Composable
fun ServiceBrandedCardGrid(
    onNavigateToDestination: (destination: AppDestination) -> Unit
) {
    val services = listOf(
        ServiceBrandedItem(
            title = "অফসেট প্রিন্টিং",
            subtitle = "কমার্শিয়াল বই, ক্যাটালগ ও ফ্লায়ার প্রিন্টিং",
            icon = Icons.Default.Print,
            bgColor = Color(0xFF1E3A8A),
            accentColor = Color(0xFF38BDF8),
            destination = AppDestination.Public.ProductGallery("Offset", "অফসেট প্রিন্টিং")
        ),
        ServiceBrandedItem(
            title = "ডিজিটাল প্রিন্ট",
            subtitle = "জরুরি সেম-ডে ফাস্ট প্রিন্টিং সমাধান",
            icon = Icons.Default.PrecisionManufacturing,
            bgColor = Color(0xFF581C87),
            accentColor = Color(0xFFC084FC),
            destination = AppDestination.Public.ProductGallery("Digital", "ডিজিটাল প্রিন্ট")
        ),
        ServiceBrandedItem(
            title = "কাস্টম প্যাকেজিং",
            subtitle = "রিজিড বক্স, পেপার কার্টুন ও ডাই-কাট বক্স",
            icon = Icons.Default.Inventory,
            bgColor = Color(0xFF7C2D12),
            accentColor = Color(0xFFFB923C),
            destination = AppDestination.Public.ProductGallery("Packaging", "কাস্টম প্যাকেজিং")
        ),
        ServiceBrandedItem(
            title = "পিভিসি ব্যানার",
            subtitle = "বড় সাইনবোর্ড, এক্স-স্ট্যান্ড ও ব্যানার",
            icon = Icons.Default.Star,
            bgColor = Color(0xFF064E3B),
            accentColor = Color(0xFF34D399),
            destination = AppDestination.Public.ProductGallery("Banner", "পিভিসি ব্যানার")
        )
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ServiceBrandedCard(item = services[0], onClick = { onNavigateToDestination(services[0].destination) }, modifier = Modifier.weight(1f))
            ServiceBrandedCard(item = services[1], onClick = { onNavigateToDestination(services[1].destination) }, modifier = Modifier.weight(1f))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ServiceBrandedCard(item = services[2], onClick = { onNavigateToDestination(services[2].destination) }, modifier = Modifier.weight(1f))
            ServiceBrandedCard(item = services[3], onClick = { onNavigateToDestination(services[3].destination) }, modifier = Modifier.weight(1f))
        }
    }
}

data class ServiceBrandedItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val bgColor: Color,
    val accentColor: Color,
    val destination: AppDestination
)

@Composable
fun ServiceBrandedCard(
    item: ServiceBrandedItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(130.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = item.bgColor),
        border = androidx.compose.foundation.BorderStroke(1.dp, item.accentColor.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(item.accentColor.copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.title,
                    tint = item.accentColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column {
                Text(
                    text = item.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.subtitle,
                    fontSize = 10.sp,
                    color = Color(0xFFCBD5E1),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Grid menu item model for standard 4-column utility rows.
 */
data class GridMenuItem(
    val title: String,
    val icon: ImageVector,
    val bgColor: Color,
    val iconColor: Color,
    val onClick: () -> Unit
)

/**
 * Standard 4-item card grid row composable.
 */
@Composable
fun StandardGridRow(items: List<GridMenuItem>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { item ->
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(90.dp)
                    .clickable { item.onClick() },
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = item.bgColor),
                border = androidx.compose.foundation.BorderStroke(1.dp, item.iconColor.copy(alpha = 0.3f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(item.iconColor.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            tint = item.iconColor,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = item.title,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
