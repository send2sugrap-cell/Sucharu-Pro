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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.ShoppingCart
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
import com.sucharu.sucharupro.ui.customer.components.CardSkeletonLoader
import com.sucharu.sucharupro.ui.customer.components.CustomerBottomNavigation
import com.sucharu.sucharupro.ui.customer.components.CustomerBottomTab
import com.sucharu.sucharupro.ui.customer.components.CustomerEmptyState
import com.sucharu.sucharupro.ui.customer.components.CustomerErrorState
import com.sucharu.sucharupro.ui.customer.theme.CustomerColors
import com.sucharu.sucharupro.ui.customer.theme.CustomerTheme
import com.sucharu.sucharupro.ui.customer.wall.components.HomeActivityCard
import com.sucharu.sucharupro.ui.customer.wall.components.HomeHeader
import com.sucharu.sucharupro.ui.navigation.AppDestination

/**
 * Clean, Light-Theme Mobile-First Home / Sucharu Wall.
 *
 * 1. Primary top view opens directly with the light notice banner (Color(0xFFFFF3E0))
 * 2. Followed immediately by the 4-column card grid (অফসেট, ডিজিটাল, প্যাকেজিং, ব্যানার) at the top
 * 3. All old dark UI blocks and dark hero cards removed
 * 4. Preserves ViewModels, API calls, Navigation callbacks, and Data models
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

    // Light/Neutral Modern Commercial Utility Theme
    CustomerTheme(colors = CustomerColors.light()) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = Color(0xFFF6F8FA),
            topBar = {
                HomeHeader(
                    principal = principal,
                    notificationCount = 0,
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
                            CustomerBottomTab.HOME -> { /* Stay on Home */ }
                            CustomerBottomTab.SERVICES -> onNavigateToDestination(AppDestination.Public.PrintingServices)
                            CustomerBottomTab.OFFERS -> onNavigateToDestination(AppDestination.Public.Offers)
                            CustomerBottomTab.ACTIVITY -> {
                                if (principal != null) {
                                    onNavigateToDestination(AppDestination.Customer.Orders)
                                } else {
                                    onNavigateToDestination(AppDestination.Public.Login)
                                }
                            }
                            CustomerBottomTab.ACCOUNT -> {
                                if (principal != null) {
                                    onNavigateToDestination(AppDestination.Customer.Profile)
                                } else {
                                    onNavigateToDestination(AppDestination.Public.Login)
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
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // ১. ব্যানার / নোটিশ বক্স (RIGHT AT THE TOP OF THE MOBILE SCREEN)
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFE0B2))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = null,
                                        tint = Color(0xFFE65100),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = feed.featuredOffer?.title ?: "দশ মিলি করি কাজ: অফসেট ও প্যাকেজিং-এ বিশেষ ছাড় চলছে!",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFFE65100),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        // ২. প্রিন্টিং সার্ভিস (৪ কলাম গ্রিড) - AT THE TOP OF THE MOBILE SCREEN
                        item {
                            Text(
                                text = "প্রিন্টিং সার্ভিস",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF263238),
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            val services = listOf(
                                GridMenuItem("অফসেট", Icons.Default.Print, Color(0xFFE8F5E9), Color(0xFF2E7D32)) {
                                    onNavigateToDestination(AppDestination.Public.PrintingServices("Offset"))
                                },
                                GridMenuItem("ডিজিটাল", Icons.Default.PrecisionManufacturing, Color(0xFFE3F2FD), Color(0xFF1565C0)) {
                                    onNavigateToDestination(AppDestination.Public.PrintingServices("Digital"))
                                },
                                GridMenuItem("প্যাকেজিং", Icons.Default.Inventory, Color(0xFFFFF3E0), Color(0xFFEF6C00)) {
                                    onNavigateToDestination(AppDestination.Public.PrintingServices("Packaging"))
                                },
                                GridMenuItem("ব্যানার", Icons.Default.Star, Color(0xFFF3E5F5), Color(0xFF7B1FA2)) {
                                    onNavigateToDestination(AppDestination.Public.PrintingServices("Banner"))
                                }
                            )
                            StandardGridRow(items = services)
                        }

                        // ৩. জনপ্রিয় প্রোডাক্টস (দুই সারিতে ৪টি করে কার্ড)
                        item {
                            Text(
                                text = "জনপ্রিয় প্রোডাক্টস",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF263238),
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            val productsRow1 = listOf(
                                GridMenuItem("ভিজিটিং কার্ড", Icons.Default.Badge, Color(0xFFE0F7FA), Color(0xFF00838F)) {
                                    onNavigateToDestination(AppDestination.Public.Products("Card"))
                                },
                                GridMenuItem("ব্রোশিওর", Icons.Default.Book, Color(0xFFFBE9E7), Color(0xFFD84315)) {
                                    onNavigateToDestination(AppDestination.Public.Products("Brochure"))
                                },
                                GridMenuItem("রিজিড বক্স", Icons.Default.Archive, Color(0xFFEFEBE9), Color(0xFF4E342E)) {
                                    onNavigateToDestination(AppDestination.Public.Products("RigidBox"))
                                },
                                GridMenuItem("ট্যাগ / লেবেল", Icons.Default.Sell, Color(0xFFEDE7F6), Color(0xFF512DA8)) {
                                    onNavigateToDestination(AppDestination.Public.Products("Tag"))
                                }
                            )
                            val productsRow2 = listOf(
                                GridMenuItem("চালান বই", Icons.Default.Receipt, Color(0xFFE8EAF6), Color(0xFF283593)) {
                                    onNavigateToDestination(AppDestination.Public.Products("Challan"))
                                },
                                GridMenuItem("৩ডি লেটার", Icons.Default.Build, Color(0xFFFFF8E1), Color(0xFFF57F17)) {
                                    onNavigateToDestination(AppDestination.Public.Products("3D"))
                                },
                                GridMenuItem("স্টিকার", Icons.Default.ThumbUp, Color(0xFFF1F8E9), Color(0xFF33691E)) {
                                    onNavigateToDestination(AppDestination.Public.Products("Sticker"))
                                },
                                GridMenuItem("অন্যান্য", Icons.Default.Category, Color(0xFFECEFF1), Color(0xFF455A64)) {
                                    onNavigateToDestination(AppDestination.Public.Products("Others"))
                                }
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                StandardGridRow(items = productsRow1)
                                StandardGridRow(items = productsRow2)
                            }
                        }

                        // ৪. এআই ও স্মার্ট টুলস
                        item {
                            Text(
                                text = "স্মার্ট টুলস",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF263238),
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            val tools = listOf(
                                GridMenuItem("AI সহকারী", Icons.Default.AutoAwesome, Color(0xFFFFF9C4), Color(0xFFF57F17)) {
                                    onNavigateToDestination(AppDestination.Public.PublicAiAssistant)
                                },
                                GridMenuItem("দর হিসাব", Icons.Default.Calculate, Color(0xFFDCEDC8), Color(0xFF33691E)) {
                                    onNavigateToDestination(AppDestination.Customer.Quotations)
                                },
                                GridMenuItem("ট্র্যাকিং", Icons.Default.LocalShipping, Color(0xFFB2EBF2), Color(0xFF006064)) {
                                    onNavigateToDestination(AppDestination.Customer.Orders)
                                },
                                GridMenuItem("সাপোর্ট", Icons.Default.HeadsetMic, Color(0xFFFFCDD2), Color(0xFFC62828)) {
                                    onNavigateToDestination(AppDestination.Customer.Support)
                                }
                            )
                            StandardGridRow(items = tools)
                        }

                        // ৫. ব্যাক্তিগত অ্যাক্টিভিটি টাইমলাইন (যদি ডাটা থাকে)
                        if (feed.personalActivities.isNotEmpty()) {
                            item {
                                Text(
                                    text = "ব্যাক্তিগত অ্যাক্টিভিটি",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF263238),
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    feed.personalActivities.forEach { act ->
                                        HomeActivityCard(
                                            item = act,
                                            onClick = { onNavigateToDestination(AppDestination.Customer.Orders) }
                                        )
                                    }
                                }
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
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                            .background(item.bgColor, RoundedCornerShape(10.dp)),
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
                        color = Color(0xFF263238),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Helper composable matching exact requested function signature.
 */
@Composable
fun SucharuMuslimBanglaStyleHome(
    onServiceClick: (String) -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF6F8FA)),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ১. ব্যানার / নোটিশ বক্স
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFE0B2))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = Color(0xFFE65100),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "দশ মিলি করি কাজ: অফসেট ও প্যাকেজিং-এ বিশেষ ছাড় চলছে!",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFE65100),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // ২. প্রিন্টিং সার্ভিস (৪ কলাম গ্রিড)
        item {
            Text(
                text = "প্রিন্টিং সার্ভিস",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF263238),
                modifier = Modifier.padding(bottom = 6.dp)
            )
            val services = listOf(
                GridMenuItem("অফসেট", Icons.Default.Print, Color(0xFFE8F5E9), Color(0xFF2E7D32)) { onServiceClick("Offset") },
                GridMenuItem("ডিজিটাল", Icons.Default.PrecisionManufacturing, Color(0xFFE3F2FD), Color(0xFF1565C0)) { onServiceClick("Digital") },
                GridMenuItem("প্যাকেজিং", Icons.Default.Inventory, Color(0xFFFFF3E0), Color(0xFFEF6C00)) { onServiceClick("Packaging") },
                GridMenuItem("ব্যানার", Icons.Default.Star, Color(0xFFF3E5F5), Color(0xFF7B1FA2)) { onServiceClick("Banner") }
            )
            StandardGridRow(items = services)
        }

        // ৩. প্রোডাক্টস (দুই সারিতে ৪টি করে কার্ড)
        item {
            Text(
                text = "জনপ্রিয় প্রোডাক্টস",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF263238),
                modifier = Modifier.padding(bottom = 6.dp)
            )
            val productsRow1 = listOf(
                GridMenuItem("ভিজিটিং কার্ড", Icons.Default.Badge, Color(0xFFE0F7FA), Color(0xFF00838F)) { onServiceClick("Card") },
                GridMenuItem("ব্রোশিওর", Icons.Default.Book, Color(0xFFFBE9E7), Color(0xFFD84315)) { onServiceClick("Brochure") },
                GridMenuItem("রিজিড বক্স", Icons.Default.Archive, Color(0xFFEFEBE9), Color(0xFF4E342E)) { onServiceClick("RigidBox") },
                GridMenuItem("ট্যাগ / লেবেল", Icons.Default.Sell, Color(0xFFEDE7F6), Color(0xFF512DA8)) { onServiceClick("Tag") }
            )
            val productsRow2 = listOf(
                GridMenuItem("চালান বই", Icons.Default.Receipt, Color(0xFFE8EAF6), Color(0xFF283593)) { onServiceClick("Challan") },
                GridMenuItem("৩ডি লেটার", Icons.Default.Build, Color(0xFFFFF8E1), Color(0xFFF57F17)) { onServiceClick("3D") },
                GridMenuItem("স্টিকার", Icons.Default.ThumbUp, Color(0xFFF1F8E9), Color(0xFF33691E)) { onServiceClick("Sticker") },
                GridMenuItem("অন্যান্য", Icons.Default.Category, Color(0xFFECEFF1), Color(0xFF455A64)) { onServiceClick("Others") }
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StandardGridRow(items = productsRow1)
                StandardGridRow(items = productsRow2)
            }
        }

        // ৪. এআই ও স্মার্ট টুলস
        item {
            Text(
                text = "স্মার্ট টুলস",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF263238),
                modifier = Modifier.padding(bottom = 6.dp)
            )
            val tools = listOf(
                GridMenuItem("AI সহকারী", Icons.Default.AutoAwesome, Color(0xFFFFF9C4), Color(0xFFF57F17)) { onServiceClick("AI") },
                GridMenuItem("দর হিসাব", Icons.Default.Calculate, Color(0xFFDCEDC8), Color(0xFF33691E)) { onServiceClick("Calculator") },
                GridMenuItem("ট্র্যাকিং", Icons.Default.LocalShipping, Color(0xFFB2EBF2), Color(0xFF006064)) { onServiceClick("Track") },
                GridMenuItem("সাপোর্ট", Icons.Default.HeadsetMic, Color(0xFFFFCDD2), Color(0xFFC62828)) { onServiceClick("Support") }
            )
            StandardGridRow(items = tools)
        }
    }
}
