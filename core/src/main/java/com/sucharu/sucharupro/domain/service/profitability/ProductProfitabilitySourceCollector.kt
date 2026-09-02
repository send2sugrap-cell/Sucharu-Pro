package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.*
import java.math.BigDecimal

/**
 * Result container of aggregating canonical revenue and cost sources for a Product.
 */
data class ProductSourceCollectionResult(
    val revenueAttributions: List<ProductRevenueAttribution>,
    val costAttributions: List<ProductCostAttribution>,
    val totalQuantity: Int,
    val totalRecognizedRevenue: BigDecimal,
    val totalActualCost: BigDecimal,
    val components: List<ProductCostBreakdownItem>,
    val provenanceFingerprints: List<String>,
    val sourceIntegrity: ProductSourceIntegrityStatus,
    val warnings: List<String>
)

/**
 * Canonical Source Collector and Attribution Interface for Product Profitability (Module 16 Step 03).
 */
interface ProductProfitabilitySourceCollector {

    suspend fun collectProductData(
        tenantId: String,
        projectId: String,
        productId: String,
        customRevenue: List<ProductRevenueAttribution>? = null,
        customCosts: List<ProductCostAttribution>? = null
    ): DomainResult<ProductSourceCollectionResult>
}
