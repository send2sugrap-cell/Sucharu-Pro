package com.sucharu.sucharupro.ui.features.category

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sucharu.sucharupro.data.repository.category.CategoryItemDetail
import com.sucharu.sucharupro.ui.customer.components.CardSkeletonLoader
import com.sucharu.sucharupro.ui.customer.components.CustomerEmptyState
import com.sucharu.sucharupro.ui.customer.components.CustomerErrorState
import com.sucharu.sucharupro.ui.customer.components.MobileTopBar
import com.sucharu.sucharupro.ui.navigation.AppDestination

/**
 * Clean 2-Column Material 3 Grid Category Details Screen for Printing Services.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrintingServicesScreen(
    categoryKey: String = "ALL",
    viewModel: CategoryDetailsViewModel = viewModel(),
    onNavigate: (AppDestination) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedItemForDetail by remember { mutableStateOf<CategoryItemDetail?>(null) }

    LaunchedEffect(categoryKey) {
        viewModel.loadServicesCategory(categoryKey)
    }

    val displayTitle = when (categoryKey.uppercase()) {
        "OFFSET", "অফসেট" -> "অফসেট প্রিন্টিং সেবা"
        "DIGITAL", "ডিজিটাল" -> "ডিজিটাল ফাস্ট প্রিন্ট"
        "PACKAGING", "প্যাকেজিং" -> "কাস্টম প্যাকেজিং বক্সেস"
        "BANNER", "ব্যানার" -> "পিভিসি ব্যানার ও সাইনেজ"
        else -> "প্রিন্টিং সার্ভিসেস ক্যাটালগ"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF6F8FA))
    ) {
        MobileTopBar(
            title = displayTitle,
            onBackClick = { onNavigate(AppDestination.Public.Home) }
        )

        when (val state = uiState) {
            is CategoryDetailsUiState.Loading -> {
                Column(modifier = Modifier.padding(16.dp)) {
                    CardSkeletonLoader()
                    Spacer(modifier = Modifier.height(12.dp))
                    CardSkeletonLoader()
                }
            }

            is CategoryDetailsUiState.Error -> {
                Column(modifier = Modifier.padding(16.dp)) {
                    CustomerErrorState(
                        errorMessage = state.message,
                        onRetry = { viewModel.loadServicesCategory(categoryKey) }
                    )
                }
            }

            is CategoryDetailsUiState.Empty -> {
                Column(modifier = Modifier.padding(16.dp)) {
                    CustomerEmptyState(
                        message = "কোনো নির্দিষ্ট প্রিন্টিং সেবা পাওয়া যায়নি",
                        subtitle = "অন্য কোনো ক্যাটাগরি বা হোম স্ক্রিন থেকে সার্ভিস সিলেক্ট করুন।"
                    )
                }
            }

            is CategoryDetailsUiState.Success -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.items) { item ->
                        CategoryGridItemCard(
                            item = item,
                            onClick = {
                                // Open Item Detail Spec Sheet
                                selectedItemForDetail = item
                            }
                        )
                    }
                }
            }
        }
    }

    // Modal Spec Sheet for selected item
    if (selectedItemForDetail != null) {
        CategoryItemDetailSheet(
            item = selectedItemForDetail,
            onDismiss = { selectedItemForDetail = null },
            onOrderRequestClick = {
                onNavigate(AppDestination.Customer.Quotations)
            }
        )
    }
}

/**
 * Reusable 2-Column Material 3 Category Grid Item Card.
 *
 * Dynamic height (min 160.dp) to eliminate text clipping on small/normal mobile displays.
 */
@Composable
fun CategoryGridItemCard(
    item: CategoryItemDetail,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 160.dp)
            .wrapContentHeight()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(item.bgColor, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.title,
                    tint = item.iconColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = item.title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF263238),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = item.subtitleGsm,
                fontSize = 10.sp,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = item.estimatedPriceFormatted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2E7D32),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
