package com.sucharu.sucharupro.ui.features.category

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sucharu.sucharupro.data.repository.category.CategoryItemDetail
import com.sucharu.sucharupro.ui.customer.components.CardSkeletonLoader
import com.sucharu.sucharupro.ui.customer.components.CustomerEmptyState
import com.sucharu.sucharupro.ui.customer.components.CustomerErrorState
import com.sucharu.sucharupro.ui.customer.components.MobileTopBar
import com.sucharu.sucharupro.ui.navigation.AppDestination

/**
 * Clean 2-Column Material 3 Grid Category Details Screen for Products Catalogue.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsScreen(
    categoryKey: String = "ALL",
    viewModel: CategoryDetailsViewModel = viewModel(),
    onNavigate: (AppDestination) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedItemForDetail by remember { mutableStateOf<CategoryItemDetail?>(null) }

    LaunchedEffect(categoryKey) {
        viewModel.loadProductsCategory(categoryKey)
    }

    val displayTitle = when (categoryKey.uppercase()) {
        "CARD", "ভিজিটিং কার্ড" -> "ভিজিটিং কার্ড ভ্যারিয়েন্ট"
        "BROCHURE", "ব্রোশিওর" -> "ব্রোশিওর ও ক্যাটালগ"
        "RIGIDBOX", "রিজিড বক্স" -> "রিজিড প্যাকেজিং বক্সেস"
        "TAG", "ট্যাগ / লেবেল" -> "ট্যাগ ও প্রোডাক্ট লেবেল"
        "CHALLAN", "চালান বই" -> "চালান ও ক্যাশ মেমো"
        "3D", "৩ডি লেটার" -> "৩ডি সাইন ও চ্যানেল লেটার"
        "STICKER", "স্টিকার" -> "ডাই-কাট ভিনাইল স্টিকার"
        else -> "জনপ্রিয় প্রোডাক্টস ক্যাটালগ"
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
                        onRetry = { viewModel.loadProductsCategory(categoryKey) }
                    )
                }
            }

            is CategoryDetailsUiState.Empty -> {
                Column(modifier = Modifier.padding(16.dp)) {
                    CustomerEmptyState(
                        message = "কোনো নির্দিষ্ট প্রোডাক্ট পাওয়া যায়নি",
                        subtitle = "অন্য কোনো প্রোডাক্ট ক্যাটাগরি থেকে অনুসন্ধান করুন।"
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
