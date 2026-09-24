package com.sucharu.sucharupro.ui.customer.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.sucharu.sucharupro.ui.navigation.AppDestination

/**
 * Legacy Gallery Template Item Model for backwards compatibility.
 */
data class GalleryTemplateItem(
    val templateId: String,
    val templateCode: String,
    val title: String,
    val specsGsm: String,
    val estimatedPrice: String,
    val icon: ImageVector? = null,
    val bgColor: Color = Color.Transparent,
    val iconColor: Color = Color.Transparent
)

/**
 * Universal Product Gallery & Sample Showcase Screen.
 *
 * Renders the Feature Showcase Design Gallery dynamically customized for [categoryTitle] / [categoryId].
 */
@Composable
fun ProductGalleryScreen(
    categoryId: String = "ALL",
    categoryTitle: String = "ডিজাইন গ্যালারি",
    onNavigate: (AppDestination) -> Unit = {},
    modifier: Modifier = Modifier
) {
    SpecialOfferGalleryScreen(
        categoryTitle = categoryTitle,
        onNavigateBack = { onNavigate(AppDestination.Public.Home) },
        onOrderClick = { item -> onNavigate(AppDestination.Customer.Quotations) },
        onCustomizeClick = { item -> onNavigate(AppDestination.Public.ProductGallery(item.templateId, item.title)) },
        onCustomOrderClick = { onNavigate(AppDestination.Customer.Quotations) },
        modifier = modifier
    )
}
