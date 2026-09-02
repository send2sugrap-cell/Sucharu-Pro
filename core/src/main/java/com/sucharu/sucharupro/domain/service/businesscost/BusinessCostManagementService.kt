package com.sucharu.sucharupro.domain.service.businesscost

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.datasource.businesscost.BusinessCostTrackingFilter
import com.sucharu.sucharupro.domain.model.businesscost.*
import com.sucharu.sucharupro.domain.model.common.DomainResult
import java.math.BigDecimal

// --- Command Models ---

data class CreateCostCenterCommand(
    val code: String,
    val name: String,
    val description: String? = null,
    val parentCostCenterId: String? = null
)

data class UpdateCostCenterCommand(
    val name: String,
    val description: String? = null,
    val parentCostCenterId: String? = null,
    val isActive: Boolean = true
)

data class CreateCostCategoryCommand(
    val code: String,
    val name: String,
    val description: String? = null,
    val parentCategoryId: String? = null,
    val isSystemDefined: Boolean = false
)

data class UpdateCostCategoryCommand(
    val name: String,
    val description: String? = null,
    val parentCategoryId: String? = null,
    val isActive: Boolean = true
)

data class TrackOperationalCostCommand(
    val sourceType: BusinessCostTrackingSourceType,
    val sourceId: String,
    val ledgerPostingId: String? = null,
    val costCenterId: String,
    val costCategoryId: String,
    val jobId: String? = null,
    val amount: BigDecimal? = null,
    val currency: String = "BDT",
    val notes: String? = null,
    val idempotencyKey: String? = null,
    val correlationId: String? = null
)

data class ClassifyCostCommand(
    val trackingId: String,
    val costCenterId: String,
    val costCategoryId: String,
    val jobId: String? = null,
    val notes: String? = null,
    val idempotencyKey: String? = null,
    val correlationId: String? = null
)

data class ReclassifyCostCommand(
    val trackingId: String,
    val newCostCenterId: String,
    val newCostCategoryId: String,
    val newJobId: String? = null,
    val reason: String,
    val idempotencyKey: String? = null,
    val correlationId: String? = null
)

/**
 * Service orchestrator for Cost Center, Cost Category, and Cost Tracking management.
 */
interface BusinessCostManagementService {

    // --- Cost Center Governance ---
    suspend fun createCostCenter(principal: AuthenticatedPrincipal, command: CreateCostCenterCommand): DomainResult<BusinessCostCenter>
    suspend fun getCostCenterById(principal: AuthenticatedPrincipal, id: String): DomainResult<BusinessCostCenter>
    suspend fun updateCostCenter(principal: AuthenticatedPrincipal, id: String, command: UpdateCostCenterCommand): DomainResult<BusinessCostCenter>
    suspend fun activateCostCenter(principal: AuthenticatedPrincipal, id: String): DomainResult<BusinessCostCenter>
    suspend fun deactivateCostCenter(principal: AuthenticatedPrincipal, id: String): DomainResult<BusinessCostCenter>
    suspend fun listCostCenters(principal: AuthenticatedPrincipal, activeOnly: Boolean? = null): DomainResult<List<BusinessCostCenter>>
    suspend fun getCostCenterHierarchy(principal: AuthenticatedPrincipal, parentCostCenterId: String): DomainResult<List<BusinessCostCenter>>

    // --- Cost Category Governance ---
    suspend fun createCostCategory(principal: AuthenticatedPrincipal, command: CreateCostCategoryCommand): DomainResult<BusinessCostCategory>
    suspend fun getCostCategoryById(principal: AuthenticatedPrincipal, id: String): DomainResult<BusinessCostCategory>
    suspend fun updateCostCategory(principal: AuthenticatedPrincipal, id: String, command: UpdateCostCategoryCommand): DomainResult<BusinessCostCategory>
    suspend fun activateCostCategory(principal: AuthenticatedPrincipal, id: String): DomainResult<BusinessCostCategory>
    suspend fun deactivateCostCategory(principal: AuthenticatedPrincipal, id: String): DomainResult<BusinessCostCategory>
    suspend fun listCostCategories(principal: AuthenticatedPrincipal, activeOnly: Boolean? = null): DomainResult<List<BusinessCostCategory>>
    suspend fun getCostCategoryHierarchy(principal: AuthenticatedPrincipal, parentCategoryId: String): DomainResult<List<BusinessCostCategory>>

    // --- Operational Cost Tracking & Ingestion ---
    suspend fun trackOperationalCost(principal: AuthenticatedPrincipal, command: TrackOperationalCostCommand): DomainResult<BusinessCostTracking>
    suspend fun getCostTrackingById(principal: AuthenticatedPrincipal, id: String): DomainResult<BusinessCostTracking>
    suspend fun listCostTracking(principal: AuthenticatedPrincipal, filter: BusinessCostTrackingFilter): DomainResult<List<BusinessCostTracking>>
    suspend fun getCostTrackingBySource(principal: AuthenticatedPrincipal, sourceType: BusinessCostTrackingSourceType, sourceId: String): DomainResult<List<BusinessCostTracking>>

    // --- Classification & Reclassification ---
    suspend fun classifyCost(principal: AuthenticatedPrincipal, command: ClassifyCostCommand): DomainResult<BusinessCostTracking>
    suspend fun reclassifyCost(principal: AuthenticatedPrincipal, command: ReclassifyCostCommand): DomainResult<BusinessCostTracking>

    // --- Summaries & Analytical Projections ---
    suspend fun getCostCenterSummary(principal: AuthenticatedPrincipal, costCenterId: String): DomainResult<BusinessCostCenterSummary>
    suspend fun getCostCategorySummary(principal: AuthenticatedPrincipal, categoryId: String): DomainResult<BusinessCostCategorySummary>
    suspend fun getJobCostDetail(principal: AuthenticatedPrincipal, jobId: String): DomainResult<BusinessJobCostDetailSummary>
    suspend fun getTrackingSummary(principal: AuthenticatedPrincipal): DomainResult<BusinessCostTrackingSummary>
    suspend fun getAuditTrail(principal: AuthenticatedPrincipal, trackingId: String? = null): DomainResult<List<BusinessCostClassificationAuditEvent>>
}
