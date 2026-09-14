package com.sucharu.sucharupro.data.repository.category

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.coroutines.flow.Flow

/**
 * Category Item Detail Model for Category-Specific Grids.
 */
data class CategoryItemDetail(
    val id: String,
    val categoryKey: String,
    val title: String,
    val subtitleGsm: String,
    val icon: ImageVector,
    val bgColor: Color,
    val iconColor: Color,
    val estimatedPriceFormatted: String = "৳150.00 / Pc"
)

/**
 * Repository interface contract for category-specific printing services & products.
 * Easily switchable between Local Mock Data and Remote Admin Service API.
 */
interface CategoryDetailsRepository {
    fun getCategoryServices(categoryKey: String): Flow<List<CategoryItemDetail>>
    fun getCategoryProducts(categoryKey: String): Flow<List<CategoryItemDetail>>
}
