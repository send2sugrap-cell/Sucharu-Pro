package com.sucharu.sucharupro.data.datasource.businesscost

import com.sucharu.sucharupro.domain.model.businesscost.*
import java.math.BigDecimal

/**
 * Filter criteria for querying operational cost tracking records.
 */
data class BusinessCostTrackingFilter(
    val sourceType: BusinessCostTrackingSourceType? = null,
    val sourceId: String? = null,
    val costCenterId: String? = null,
    val costCategoryId: String? = null,
    val jobId: String? = null,
    val allocationStatus: BusinessCostAllocationStatus? = null,
    val classificationStatus: BusinessCostClassificationStatus? = null,
    val fromDate: Long? = null,
    val toDate: Long? = null,
    val limit: Int = 50,
    val offset: Int = 0
)

/**
 * Low-level persistence interface for Cost Centers, Categories, Tracking, and Classification Audits.
 */
interface BusinessCostManagementDataSource {

    // --- Cost Centers ---
    suspend fun createCostCenter(center: BusinessCostCenter): BusinessCostCenter
    suspend fun findCostCenterById(id: String, tenantId: String, projectId: String): BusinessCostCenter?
    suspend fun findCostCenterByCode(code: String, tenantId: String, projectId: String): BusinessCostCenter?
    suspend fun updateCostCenter(center: BusinessCostCenter): BusinessCostCenter
    suspend fun listCostCenters(tenantId: String, projectId: String, activeOnly: Boolean? = null): List<BusinessCostCenter>
    suspend fun getCostCenterChildren(parentCostCenterId: String, tenantId: String, projectId: String): List<BusinessCostCenter>

    // --- Cost Categories ---
    suspend fun createCostCategory(category: BusinessCostCategory): BusinessCostCategory
    suspend fun findCostCategoryById(id: String, tenantId: String, projectId: String): BusinessCostCategory?
    suspend fun findCostCategoryByCode(code: String, tenantId: String, projectId: String): BusinessCostCategory?
    suspend fun updateCostCategory(category: BusinessCostCategory): BusinessCostCategory
    suspend fun listCostCategories(tenantId: String, projectId: String, activeOnly: Boolean? = null): List<BusinessCostCategory>
    suspend fun getCostCategoryChildren(parentCategoryId: String, tenantId: String, projectId: String): List<BusinessCostCategory>

    // --- Cost Tracking ---
    suspend fun createCostTracking(tracking: BusinessCostTracking): BusinessCostTracking
    suspend fun findCostTrackingById(id: String, tenantId: String, projectId: String): BusinessCostTracking?
    suspend fun findCostTrackingBySource(sourceType: BusinessCostTrackingSourceType, sourceId: String, tenantId: String, projectId: String): List<BusinessCostTracking>
    suspend fun updateCostTracking(tracking: BusinessCostTracking): BusinessCostTracking
    suspend fun listCostTracking(tenantId: String, projectId: String, filter: BusinessCostTrackingFilter): List<BusinessCostTracking>

    // --- Audits ---
    suspend fun recordAuditEvent(event: BusinessCostClassificationAuditEvent)
    suspend fun listAuditEvents(tenantId: String, projectId: String, trackingId: String? = null): List<BusinessCostClassificationAuditEvent>

    // --- Rollup Projections ---
    suspend fun calculateCostCenterSummary(costCenterId: String, tenantId: String, projectId: String): BusinessCostCenterSummary
    suspend fun calculateCostCategorySummary(categoryId: String, tenantId: String, projectId: String): BusinessCostCategorySummary
    suspend fun calculateJobCostDetail(jobId: String, tenantId: String, projectId: String): BusinessJobCostDetailSummary
    suspend fun calculateTrackingSummary(tenantId: String, projectId: String): BusinessCostTrackingSummary
}
