package com.sucharu.sucharupro.data.repository.businesscost

import com.sucharu.sucharupro.data.datasource.businesscost.BusinessCostManagementDataSource
import com.sucharu.sucharupro.data.datasource.businesscost.BusinessCostTrackingFilter
import com.sucharu.sucharupro.domain.model.businesscost.*
import com.sucharu.sucharupro.domain.repository.businesscost.BusinessCostManagementRepository

/**
 * Production implementation of BusinessCostManagementRepository delegating to DataSource.
 */
class BusinessCostManagementRepositoryImpl(
    private val dataSource: BusinessCostManagementDataSource
) : BusinessCostManagementRepository {

    override suspend fun createCostCenter(center: BusinessCostCenter): BusinessCostCenter =
        dataSource.createCostCenter(center)

    override suspend fun findCostCenterById(id: String, tenantId: String, projectId: String): BusinessCostCenter? =
        dataSource.findCostCenterById(id, tenantId, projectId)

    override suspend fun findCostCenterByCode(code: String, tenantId: String, projectId: String): BusinessCostCenter? =
        dataSource.findCostCenterByCode(code, tenantId, projectId)

    override suspend fun updateCostCenter(center: BusinessCostCenter): BusinessCostCenter =
        dataSource.updateCostCenter(center)

    override suspend fun listCostCenters(tenantId: String, projectId: String, activeOnly: Boolean?): List<BusinessCostCenter> =
        dataSource.listCostCenters(tenantId, projectId, activeOnly)

    override suspend fun getCostCenterChildren(parentCostCenterId: String, tenantId: String, projectId: String): List<BusinessCostCenter> =
        dataSource.getCostCenterChildren(parentCostCenterId, tenantId, projectId)

    override suspend fun createCostCategory(category: BusinessCostCategory): BusinessCostCategory =
        dataSource.createCostCategory(category)

    override suspend fun findCostCategoryById(id: String, tenantId: String, projectId: String): BusinessCostCategory? =
        dataSource.findCostCategoryById(id, tenantId, projectId)

    override suspend fun findCostCategoryByCode(code: String, tenantId: String, projectId: String): BusinessCostCategory? =
        dataSource.findCostCategoryByCode(code, tenantId, projectId)

    override suspend fun updateCostCategory(category: BusinessCostCategory): BusinessCostCategory =
        dataSource.updateCostCategory(category)

    override suspend fun listCostCategories(tenantId: String, projectId: String, activeOnly: Boolean?): List<BusinessCostCategory> =
        dataSource.listCostCategories(tenantId, projectId, activeOnly)

    override suspend fun getCostCategoryChildren(parentCategoryId: String, tenantId: String, projectId: String): List<BusinessCostCategory> =
        dataSource.getCostCategoryChildren(parentCategoryId, tenantId, projectId)

    override suspend fun createCostTracking(tracking: BusinessCostTracking): BusinessCostTracking =
        dataSource.createCostTracking(tracking)

    override suspend fun findCostTrackingById(id: String, tenantId: String, projectId: String): BusinessCostTracking? =
        dataSource.findCostTrackingById(id, tenantId, projectId)

    override suspend fun findCostTrackingBySource(
        sourceType: BusinessCostTrackingSourceType,
        sourceId: String,
        tenantId: String,
        projectId: String
    ): List<BusinessCostTracking> =
        dataSource.findCostTrackingBySource(sourceType, sourceId, tenantId, projectId)

    override suspend fun updateCostTracking(tracking: BusinessCostTracking): BusinessCostTracking =
        dataSource.updateCostTracking(tracking)

    override suspend fun listCostTracking(
        tenantId: String,
        projectId: String,
        filter: BusinessCostTrackingFilter
    ): List<BusinessCostTracking> =
        dataSource.listCostTracking(tenantId, projectId, filter)

    override suspend fun recordAuditEvent(event: BusinessCostClassificationAuditEvent) =
        dataSource.recordAuditEvent(event)

    override suspend fun listAuditEvents(
        tenantId: String,
        projectId: String,
        trackingId: String?
    ): List<BusinessCostClassificationAuditEvent> =
        dataSource.listAuditEvents(tenantId, projectId, trackingId)

    override suspend fun calculateCostCenterSummary(
        costCenterId: String,
        tenantId: String,
        projectId: String
    ): BusinessCostCenterSummary =
        dataSource.calculateCostCenterSummary(costCenterId, tenantId, projectId)

    override suspend fun calculateCostCategorySummary(
        categoryId: String,
        tenantId: String,
        projectId: String
    ): BusinessCostCategorySummary =
        dataSource.calculateCostCategorySummary(categoryId, tenantId, projectId)

    override suspend fun calculateJobCostDetail(
        jobId: String,
        tenantId: String,
        projectId: String
    ): BusinessJobCostDetailSummary =
        dataSource.calculateJobCostDetail(jobId, tenantId, projectId)

    override suspend fun calculateTrackingSummary(
        tenantId: String,
        projectId: String
    ): BusinessCostTrackingSummary =
        dataSource.calculateTrackingSummary(tenantId, projectId)
}
