package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.*

/**
 * Customer Profitability & Contribution Analysis Engine Service Interface (Module 16 Step 04).
 */
interface CustomerProfitabilityService {

    suspend fun calculateCustomerProfitability(
        tenantId: String,
        projectId: String,
        customerId: String,
        customerName: String? = null,
        customerCode: String? = null,
        periodType: ProfitabilityPeriodType = ProfitabilityPeriodType.ALL_TIME,
        periodStart: Long? = null,
        periodEnd: Long? = null,
        customRevenue: List<CustomerRevenueAttribution>? = null,
        customCosts: List<CustomerCostAttribution>? = null,
        previousPeriodMargin: java.math.BigDecimal? = null,
        idempotencyKey: String? = null,
        actor: String = "SYSTEM"
    ): DomainResult<CustomerProfitabilitySnapshot>

    suspend fun getLatestSnapshot(tenantId: String, projectId: String, customerId: String): DomainResult<CustomerProfitabilitySnapshot?>

    suspend fun getSnapshotById(tenantId: String, projectId: String, snapshotId: String): DomainResult<CustomerProfitabilitySnapshot>

    suspend fun listSnapshots(tenantId: String, projectId: String, filter: CustomerProfitabilityFilter): DomainResult<List<CustomerProfitabilitySnapshot>>

    suspend fun getCostBreakdown(tenantId: String, projectId: String, customerId: String): DomainResult<List<CustomerCostBreakdownItem>>

    suspend fun getProvenance(tenantId: String, projectId: String, customerId: String): DomainResult<Pair<List<CustomerRevenueAttribution>, List<CustomerCostAttribution>>>

    suspend fun getOrderProfitabilities(tenantId: String, projectId: String, customerId: String): DomainResult<List<CustomerOrderProfitabilitySummary>>

    suspend fun getJobProfitabilities(tenantId: String, projectId: String, customerId: String): DomainResult<List<CustomerJobProfitabilitySummary>>

    suspend fun getProductContributions(tenantId: String, projectId: String, customerId: String): DomainResult<List<CustomerProductContributionSummary>>

    suspend fun getTrend(tenantId: String, projectId: String, customerId: String): DomainResult<CustomerProfitabilityTrend>

    suspend fun reconcileCustomerProfitability(
        tenantId: String,
        projectId: String,
        customerId: String,
        snapshotId: String? = null,
        actor: String = "SYSTEM"
    ): DomainResult<CustomerProfitabilityReconciliationEvent>

    suspend fun getAuditHistory(tenantId: String, projectId: String, customerId: String): DomainResult<List<CustomerProfitabilityAuditEvent>>

    suspend fun rankCustomers(
        tenantId: String,
        projectId: String,
        criteria: CustomerRankingCriteria = CustomerRankingCriteria.GROSS_PROFIT
    ): DomainResult<List<CustomerProfitabilityRankingItem>>

    suspend fun analyzeConcentration(tenantId: String, projectId: String): DomainResult<CustomerConcentrationAnalysis>

    suspend fun compareCustomers(
        tenantId: String,
        projectId: String,
        customerIds: List<String>
    ): DomainResult<List<CustomerProfitabilityComparisonItem>>

    suspend fun getUnattributedDiagnostics(tenantId: String, projectId: String): DomainResult<List<UnattributedProfitabilityItem>>
}
