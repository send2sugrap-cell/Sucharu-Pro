package com.sucharu.sucharupro.data.datasource.businesscost

import com.sucharu.sucharupro.domain.model.businesscost.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * In-memory thread-safe Fake implementation for Business Cost Management DataSource.
 */
class FakeBusinessCostManagementDataSource : BusinessCostManagementDataSource {

    private val mutex = Mutex()

    private val costCenters = mutableListOf<BusinessCostCenter>()
    private val costCategories = mutableListOf<BusinessCostCategory>()
    private val costTrackingList = mutableListOf<BusinessCostTracking>()
    private val auditEvents = mutableListOf<BusinessCostClassificationAuditEvent>()

    // --- Cost Centers ---

    override suspend fun createCostCenter(center: BusinessCostCenter): BusinessCostCenter = mutex.withLock {
        if (costCenters.any { it.tenantId == center.tenantId && it.projectId == center.projectId && it.code.equals(center.code, ignoreCase = true) }) {
            throw IllegalStateException("Cost center code '${center.code}' already exists for this tenant.")
        }
        costCenters.add(center)
        center
    }

    override suspend fun findCostCenterById(id: String, tenantId: String, projectId: String): BusinessCostCenter? = mutex.withLock {
        costCenters.find { it.id == id && it.tenantId == tenantId && it.projectId == projectId }
    }

    override suspend fun findCostCenterByCode(code: String, tenantId: String, projectId: String): BusinessCostCenter? = mutex.withLock {
        costCenters.find { it.code.equals(code, ignoreCase = true) && it.tenantId == tenantId && it.projectId == projectId }
    }

    override suspend fun updateCostCenter(center: BusinessCostCenter): BusinessCostCenter = mutex.withLock {
        val idx = costCenters.indexOfFirst { it.id == center.id && it.tenantId == center.tenantId && it.projectId == center.projectId }
        if (idx == -1) throw NoSuchElementException("Cost center '${center.id}' not found.")
        val updated = center.copy(version = costCenters[idx].version + 1, updatedAt = System.currentTimeMillis())
        costCenters[idx] = updated
        updated
    }

    override suspend fun listCostCenters(tenantId: String, projectId: String, activeOnly: Boolean?): List<BusinessCostCenter> = mutex.withLock {
        costCenters.filter { c ->
            c.tenantId == tenantId && c.projectId == projectId &&
            (activeOnly == null || c.isActive == activeOnly)
        }.sortedBy { it.code }
    }

    override suspend fun getCostCenterChildren(parentCostCenterId: String, tenantId: String, projectId: String): List<BusinessCostCenter> = mutex.withLock {
        costCenters.filter { it.parentCostCenterId == parentCostCenterId && it.tenantId == tenantId && it.projectId == projectId }
            .sortedBy { it.code }
    }

    // --- Cost Categories ---

    override suspend fun createCostCategory(category: BusinessCostCategory): BusinessCostCategory = mutex.withLock {
        if (costCategories.any { it.tenantId == category.tenantId && it.projectId == category.projectId && it.code.equals(category.code, ignoreCase = true) }) {
            throw IllegalStateException("Cost category code '${category.code}' already exists for this tenant.")
        }
        costCategories.add(category)
        category
    }

    override suspend fun findCostCategoryById(id: String, tenantId: String, projectId: String): BusinessCostCategory? = mutex.withLock {
        costCategories.find { it.id == id && it.tenantId == tenantId && it.projectId == projectId }
    }

    override suspend fun findCostCategoryByCode(code: String, tenantId: String, projectId: String): BusinessCostCategory? = mutex.withLock {
        costCategories.find { it.code.equals(code, ignoreCase = true) && it.tenantId == tenantId && it.projectId == projectId }
    }

    override suspend fun updateCostCategory(category: BusinessCostCategory): BusinessCostCategory = mutex.withLock {
        val idx = costCategories.indexOfFirst { it.id == category.id && it.tenantId == category.tenantId && it.projectId == category.projectId }
        if (idx == -1) throw NoSuchElementException("Cost category '${category.id}' not found.")
        val updated = category.copy(version = costCategories[idx].version + 1, updatedAt = System.currentTimeMillis())
        costCategories[idx] = updated
        updated
    }

    override suspend fun listCostCategories(tenantId: String, projectId: String, activeOnly: Boolean?): List<BusinessCostCategory> = mutex.withLock {
        costCategories.filter { c ->
            c.tenantId == tenantId && c.projectId == projectId &&
            (activeOnly == null || c.isActive == activeOnly)
        }.sortedBy { it.code }
    }

    override suspend fun getCostCategoryChildren(parentCategoryId: String, tenantId: String, projectId: String): List<BusinessCostCategory> = mutex.withLock {
        costCategories.filter { it.parentCategoryId == parentCategoryId && it.tenantId == tenantId && it.projectId == projectId }
            .sortedBy { it.code }
    }

    // --- Cost Tracking ---

    override suspend fun createCostTracking(tracking: BusinessCostTracking): BusinessCostTracking = mutex.withLock {
        costTrackingList.add(tracking)
        tracking
    }

    override suspend fun findCostTrackingById(id: String, tenantId: String, projectId: String): BusinessCostTracking? = mutex.withLock {
        costTrackingList.find { it.id == id && it.tenantId == tenantId && it.projectId == projectId }
    }

    override suspend fun findCostTrackingBySource(
        sourceType: BusinessCostTrackingSourceType,
        sourceId: String,
        tenantId: String,
        projectId: String
    ): List<BusinessCostTracking> = mutex.withLock {
        costTrackingList.filter { it.sourceType == sourceType && it.sourceId == sourceId && it.tenantId == tenantId && it.projectId == projectId }
    }

    override suspend fun updateCostTracking(tracking: BusinessCostTracking): BusinessCostTracking = mutex.withLock {
        val idx = costTrackingList.indexOfFirst { it.id == tracking.id && it.tenantId == tracking.tenantId && it.projectId == tracking.projectId }
        if (idx == -1) throw NoSuchElementException("Cost tracking '${tracking.id}' not found.")
        val updated = tracking.copy(version = costTrackingList[idx].version + 1, updatedAt = System.currentTimeMillis())
        costTrackingList[idx] = updated
        updated
    }

    override suspend fun listCostTracking(tenantId: String, projectId: String, filter: BusinessCostTrackingFilter): List<BusinessCostTracking> = mutex.withLock {
        costTrackingList.filter { t ->
            t.tenantId == tenantId && t.projectId == projectId &&
            (filter.sourceType == null || t.sourceType == filter.sourceType) &&
            (filter.sourceId == null || t.sourceId == filter.sourceId) &&
            (filter.costCenterId == null || t.costCenterId == filter.costCenterId) &&
            (filter.costCategoryId == null || t.costCategoryId == filter.costCategoryId) &&
            (filter.jobId == null || t.jobId == filter.jobId) &&
            (filter.allocationStatus == null || t.allocationStatus == filter.allocationStatus) &&
            (filter.classificationStatus == null || t.classificationStatus == filter.classificationStatus) &&
            (filter.fromDate == null || t.createdAt >= filter.fromDate) &&
            (filter.toDate == null || t.createdAt <= filter.toDate)
        }.sortedByDescending { it.createdAt }
            .drop(filter.offset)
            .take(filter.limit)
    }

    // --- Audits ---

    override suspend fun recordAuditEvent(event: BusinessCostClassificationAuditEvent) {
        mutex.withLock {
            auditEvents.add(event)
        }
    }

    override suspend fun listAuditEvents(tenantId: String, projectId: String, trackingId: String?): List<BusinessCostClassificationAuditEvent> = mutex.withLock {
        auditEvents.filter { a ->
            a.tenantId == tenantId && a.projectId == projectId &&
            (trackingId == null || a.trackingId == trackingId)
        }.sortedByDescending { it.timestamp }
    }

    // --- Rollup Projections ---

    override suspend fun calculateCostCenterSummary(costCenterId: String, tenantId: String, projectId: String): BusinessCostCenterSummary = mutex.withLock {
        val center = costCenters.find { it.id == costCenterId && it.tenantId == tenantId && it.projectId == projectId }
            ?: throw NoSuchElementException("Cost center '$costCenterId' not found.")

        val items = costTrackingList.filter { it.costCenterId == costCenterId && it.tenantId == tenantId && it.projectId == projectId }
        var total = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
        var allocated = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)

        for (item in items) {
            total = total.add(item.amount)
            if (item.allocationStatus in setOf(BusinessCostAllocationStatus.FULLY_ALLOCATED, BusinessCostAllocationStatus.PARTIALLY_ALLOCATED)) {
                allocated = allocated.add(item.amount)
            }
        }

        val unallocated = total.subtract(allocated).setScale(4, RoundingMode.HALF_UP)

        BusinessCostCenterSummary(
            costCenterId = center.id,
            code = center.code,
            name = center.name,
            parentCostCenterId = center.parentCostCenterId,
            isActive = center.isActive,
            totalCost = total,
            allocatedCost = allocated,
            unallocatedCost = unallocated,
            trackedItemCount = items.size
        )
    }

    override suspend fun calculateCostCategorySummary(categoryId: String, tenantId: String, projectId: String): BusinessCostCategorySummary = mutex.withLock {
        val cat = costCategories.find { it.id == categoryId && it.tenantId == tenantId && it.projectId == projectId }
            ?: throw NoSuchElementException("Cost category '$categoryId' not found.")

        val items = costTrackingList.filter { it.costCategoryId == categoryId && it.tenantId == tenantId && it.projectId == projectId }
        var total = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
        val jobs = mutableSetOf<String>()

        for (item in items) {
            total = total.add(item.amount)
            if (!item.jobId.isNullOrBlank()) {
                jobs.add(item.jobId)
            }
        }

        BusinessCostCategorySummary(
            categoryId = cat.id,
            code = cat.code,
            name = cat.name,
            parentCategoryId = cat.parentCategoryId,
            isActive = cat.isActive,
            isSystemDefined = cat.isSystemDefined,
            totalCost = total,
            jobCount = jobs.size,
            trackedItemCount = items.size
        )
    }

    override suspend fun calculateJobCostDetail(jobId: String, tenantId: String, projectId: String): BusinessJobCostDetailSummary = mutex.withLock {
        val items = costTrackingList.filter { it.jobId == jobId && it.tenantId == tenantId && it.projectId == projectId }

        var total = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
        var production = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
        var vendor = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
        var expense = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
        var transport = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
        var labour = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
        var other = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
        var allocated = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)

        for (item in items) {
            total = total.add(item.amount)
            val cat = costCategories.find { it.id == item.costCategoryId }
            val catCode = cat?.code?.uppercase() ?: ""

            when {
                catCode.contains("PRINT") || catCode.contains("PAPER") || catCode.contains("CTP") || catCode.contains("LAM") || catCode.contains("FOIL") || catCode.contains("DIECUT") || catCode.contains("BIND") -> production = production.add(item.amount)
                catCode.contains("OUTSOURCE") || item.sourceType == BusinessCostTrackingSourceType.VENDOR_PAYABLE -> vendor = vendor.add(item.amount)
                catCode.contains("TRANSPORT") || catCode.contains("DELIV") -> transport = transport.add(item.amount)
                catCode.contains("LABOUR") -> labour = labour.add(item.amount)
                item.sourceType == BusinessCostTrackingSourceType.BUSINESS_EXPENSE -> expense = expense.add(item.amount)
                else -> other = other.add(item.amount)
            }

            if (item.allocationStatus != BusinessCostAllocationStatus.UNALLOCATED) {
                allocated = allocated.add(item.amount)
            }
        }

        val unallocated = total.subtract(allocated).setScale(4, RoundingMode.HALF_UP)

        BusinessJobCostDetailSummary(
            jobId = jobId,
            totalCost = total,
            productionCost = production,
            vendorCost = vendor,
            expenseCost = expense,
            transportCost = transport,
            labourCost = labour,
            otherCost = other,
            allocatedCost = allocated,
            unallocatedCost = unallocated,
            itemCount = items.size,
            items = items
        )
    }

    override suspend fun calculateTrackingSummary(tenantId: String, projectId: String): BusinessCostTrackingSummary = mutex.withLock {
        val items = costTrackingList.filter { it.tenantId == tenantId && it.projectId == projectId }
        var total = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
        var allocated = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
        val jobs = mutableSetOf<String>()
        var reclassPending = 0

        for (item in items) {
            total = total.add(item.amount)
            if (item.allocationStatus != BusinessCostAllocationStatus.UNALLOCATED) {
                allocated = allocated.add(item.amount)
            }
            if (!item.jobId.isNullOrBlank()) {
                jobs.add(item.jobId)
            }
            if (item.allocationStatus == BusinessCostAllocationStatus.RECLASSIFICATION_PENDING) {
                reclassPending++
            }
        }

        val unallocated = total.subtract(allocated).setScale(4, RoundingMode.HALF_UP)
        val centerCount = costCenters.count { it.tenantId == tenantId && it.projectId == projectId }
        val catCount = costCategories.count { it.tenantId == tenantId && it.projectId == projectId && it.isActive }

        BusinessCostTrackingSummary(
            totalTrackedCost = total,
            totalAllocatedCost = allocated,
            totalUnallocatedCost = unallocated,
            totalCostCenters = centerCount,
            totalActiveCategories = catCount,
            jobsWithCostCount = jobs.size,
            reclassificationPendingCount = reclassPending
        )
    }
}
