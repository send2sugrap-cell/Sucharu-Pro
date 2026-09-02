package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.*
import java.math.BigDecimal

/**
 * Result container of collecting and attributing canonical revenue, cost, orders, jobs, and products for a Customer.
 */
data class CustomerSourceCollectionResult(
    val revenueAttributions: List<CustomerRevenueAttribution>,
    val costAttributions: List<CustomerCostAttribution>,
    val unattributedItems: List<UnattributedProfitabilityItem>,
    val totalRevenue: BigDecimal,
    val totalCost: BigDecimal,
    val variableCost: BigDecimal,
    val fixedCost: BigDecimal,
    val orderSummaries: List<CustomerOrderProfitabilitySummary>,
    val jobSummaries: List<CustomerJobProfitabilitySummary>,
    val productSummaries: List<CustomerProductContributionSummary>,
    val costBreakdown: List<CustomerCostBreakdownItem>,
    val operationalMetrics: CustomerOperationalMetrics,
    val provenanceFingerprints: List<String>,
    val sourceIntegrity: ProductSourceIntegrityStatus,
    val warnings: List<String>
)

/**
 * Source Collector & Attribution Service Interface for Customer Profitability (Module 16 Step 04).
 */
interface CustomerProfitabilitySourceCollector {

    suspend fun collectCustomerData(
        tenantId: String,
        projectId: String,
        customerId: String,
        customRevenue: List<CustomerRevenueAttribution>? = null,
        customCosts: List<CustomerCostAttribution>? = null,
        periodStart: Long? = null,
        periodEnd: Long? = null
    ): DomainResult<CustomerSourceCollectionResult>
}
