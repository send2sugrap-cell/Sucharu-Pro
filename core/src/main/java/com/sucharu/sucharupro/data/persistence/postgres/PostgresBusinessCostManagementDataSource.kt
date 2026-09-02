package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.datasource.businesscost.BusinessCostManagementDataSource
import com.sucharu.sucharupro.data.datasource.businesscost.BusinessCostTrackingFilter
import com.sucharu.sucharupro.domain.model.businesscost.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.sql.ResultSet

/**
 * Production PostgreSQL JDBC Data Source for Cost Centers, Categories, Tracking & Audits.
 */
class PostgresBusinessCostManagementDataSource(
    private val transactionManager: TransactionManager
) : BusinessCostManagementDataSource {

    // --- Cost Centers ---

    override suspend fun createCostCenter(center: BusinessCostCenter): BusinessCostCenter {
        return transactionManager.inTransaction(TenantContext(center.projectId)) { tx ->
            val sql = """
                INSERT INTO business_cost_centers (
                    id, tenant_id, project_id, code, name, description,
                    parent_cost_center_id, is_active, created_at, created_by,
                    updated_at, updated_by, version
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, center.id)
                ps.setString(2, center.tenantId)
                ps.setString(3, center.projectId)
                ps.setString(4, center.code)
                ps.setString(5, center.name)
                ps.setString(6, center.description)
                ps.setString(7, center.parentCostCenterId)
                ps.setBoolean(8, center.isActive)
                ps.setLong(9, center.createdAt)
                ps.setString(10, center.createdBy)
                ps.setLong(11, center.updatedAt)
                ps.setString(12, center.updatedBy)
                ps.setLong(13, center.version)
                ps.executeUpdate()
            }
            center
        }
    }

    override suspend fun findCostCenterById(id: String, tenantId: String, projectId: String): BusinessCostCenter? {
        return transactionManager.inReadOnly(TenantContext(projectId)) { tx ->
            val sql = "SELECT * FROM business_cost_centers WHERE id = ? AND tenant_id = ? AND project_id = ?"
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, id)
                ps.setString(2, tenantId)
                ps.setString(3, projectId)
                ps.executeQuery().use { rs ->
                    if (rs.next()) mapCostCenter(rs) else null
                }
            }
        }
    }

    override suspend fun findCostCenterByCode(code: String, tenantId: String, projectId: String): BusinessCostCenter? {
        return transactionManager.inReadOnly(TenantContext(projectId)) { tx ->
            val sql = "SELECT * FROM business_cost_centers WHERE LOWER(code) = LOWER(?) AND tenant_id = ? AND project_id = ?"
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, code)
                ps.setString(2, tenantId)
                ps.setString(3, projectId)
                ps.executeQuery().use { rs ->
                    if (rs.next()) mapCostCenter(rs) else null
                }
            }
        }
    }

    override suspend fun updateCostCenter(center: BusinessCostCenter): BusinessCostCenter {
        return transactionManager.inTransaction(TenantContext(center.projectId)) { tx ->
            val sql = """
                UPDATE business_cost_centers SET
                    name = ?, description = ?, parent_cost_center_id = ?,
                    is_active = ?, updated_at = ?, updated_by = ?, version = version + 1
                WHERE id = ? AND tenant_id = ? AND project_id = ?
            """.trimIndent()
            val rows = tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, center.name)
                ps.setString(2, center.description)
                ps.setString(3, center.parentCostCenterId)
                ps.setBoolean(4, center.isActive)
                ps.setLong(5, System.currentTimeMillis())
                ps.setString(6, center.updatedBy)
                ps.setString(7, center.id)
                ps.setString(8, center.tenantId)
                ps.setString(9, center.projectId)
                ps.executeUpdate()
            }
            if (rows == 0) throw NoSuchElementException("Cost center '${center.id}' not found for update.")
            center.copy(version = center.version + 1, updatedAt = System.currentTimeMillis())
        }
    }

    override suspend fun listCostCenters(tenantId: String, projectId: String, activeOnly: Boolean?): List<BusinessCostCenter> {
        return transactionManager.inReadOnly(TenantContext(projectId)) { tx ->
            val sql = buildString {
                append("SELECT * FROM business_cost_centers WHERE tenant_id = ? AND project_id = ?")
                if (activeOnly != null) append(" AND is_active = ?")
                append(" ORDER BY code ASC")
            }
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, projectId)
                if (activeOnly != null) ps.setBoolean(3, activeOnly)
                ps.executeQuery().use { rs ->
                    val list = mutableListOf<BusinessCostCenter>()
                    while (rs.next()) list.add(mapCostCenter(rs))
                    list
                }
            }
        }
    }

    override suspend fun getCostCenterChildren(parentCostCenterId: String, tenantId: String, projectId: String): List<BusinessCostCenter> {
        return transactionManager.inReadOnly(TenantContext(projectId)) { tx ->
            val sql = "SELECT * FROM business_cost_centers WHERE parent_cost_center_id = ? AND tenant_id = ? AND project_id = ? ORDER BY code ASC"
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, parentCostCenterId)
                ps.setString(2, tenantId)
                ps.setString(3, projectId)
                ps.executeQuery().use { rs ->
                    val list = mutableListOf<BusinessCostCenter>()
                    while (rs.next()) list.add(mapCostCenter(rs))
                    list
                }
            }
        }
    }

    // --- Cost Categories ---

    override suspend fun createCostCategory(category: BusinessCostCategory): BusinessCostCategory {
        return transactionManager.inTransaction(TenantContext(category.projectId)) { tx ->
            val sql = """
                INSERT INTO business_cost_categories (
                    id, tenant_id, project_id, code, name, description,
                    parent_category_id, is_active, is_system_defined, created_at,
                    created_by, updated_at, updated_by, version
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, category.id)
                ps.setString(2, category.tenantId)
                ps.setString(3, category.projectId)
                ps.setString(4, category.code)
                ps.setString(5, category.name)
                ps.setString(6, category.description)
                ps.setString(7, category.parentCategoryId)
                ps.setBoolean(8, category.isActive)
                ps.setBoolean(9, category.isSystemDefined)
                ps.setLong(10, category.createdAt)
                ps.setString(11, category.createdBy)
                ps.setLong(12, category.updatedAt)
                ps.setString(13, category.updatedBy)
                ps.setLong(14, category.version)
                ps.executeUpdate()
            }
            category
        }
    }

    override suspend fun findCostCategoryById(id: String, tenantId: String, projectId: String): BusinessCostCategory? {
        return transactionManager.inReadOnly(TenantContext(projectId)) { tx ->
            val sql = "SELECT * FROM business_cost_categories WHERE id = ? AND tenant_id = ? AND project_id = ?"
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, id)
                ps.setString(2, tenantId)
                ps.setString(3, projectId)
                ps.executeQuery().use { rs ->
                    if (rs.next()) mapCostCategory(rs) else null
                }
            }
        }
    }

    override suspend fun findCostCategoryByCode(code: String, tenantId: String, projectId: String): BusinessCostCategory? {
        return transactionManager.inReadOnly(TenantContext(projectId)) { tx ->
            val sql = "SELECT * FROM business_cost_categories WHERE LOWER(code) = LOWER(?) AND tenant_id = ? AND project_id = ?"
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, code)
                ps.setString(2, tenantId)
                ps.setString(3, projectId)
                ps.executeQuery().use { rs ->
                    if (rs.next()) mapCostCategory(rs) else null
                }
            }
        }
    }

    override suspend fun updateCostCategory(category: BusinessCostCategory): BusinessCostCategory {
        return transactionManager.inTransaction(TenantContext(category.projectId)) { tx ->
            val sql = """
                UPDATE business_cost_categories SET
                    name = ?, description = ?, parent_category_id = ?,
                    is_active = ?, updated_at = ?, updated_by = ?, version = version + 1
                WHERE id = ? AND tenant_id = ? AND project_id = ?
            """.trimIndent()
            val rows = tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, category.name)
                ps.setString(2, category.description)
                ps.setString(3, category.parentCategoryId)
                ps.setBoolean(4, category.isActive)
                ps.setLong(5, System.currentTimeMillis())
                ps.setString(6, category.updatedBy)
                ps.setString(7, category.id)
                ps.setString(8, category.tenantId)
                ps.setString(9, category.projectId)
                ps.executeUpdate()
            }
            if (rows == 0) throw NoSuchElementException("Cost category '${category.id}' not found for update.")
            category.copy(version = category.version + 1, updatedAt = System.currentTimeMillis())
        }
    }

    override suspend fun listCostCategories(tenantId: String, projectId: String, activeOnly: Boolean?): List<BusinessCostCategory> {
        return transactionManager.inReadOnly(TenantContext(projectId)) { tx ->
            val sql = buildString {
                append("SELECT * FROM business_cost_categories WHERE tenant_id = ? AND project_id = ?")
                if (activeOnly != null) append(" AND is_active = ?")
                append(" ORDER BY code ASC")
            }
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, projectId)
                if (activeOnly != null) ps.setBoolean(3, activeOnly)
                ps.executeQuery().use { rs ->
                    val list = mutableListOf<BusinessCostCategory>()
                    while (rs.next()) list.add(mapCostCategory(rs))
                    list
                }
            }
        }
    }

    override suspend fun getCostCategoryChildren(parentCategoryId: String, tenantId: String, projectId: String): List<BusinessCostCategory> {
        return transactionManager.inReadOnly(TenantContext(projectId)) { tx ->
            val sql = "SELECT * FROM business_cost_categories WHERE parent_category_id = ? AND tenant_id = ? AND project_id = ? ORDER BY code ASC"
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, parentCategoryId)
                ps.setString(2, tenantId)
                ps.setString(3, projectId)
                ps.executeQuery().use { rs ->
                    val list = mutableListOf<BusinessCostCategory>()
                    while (rs.next()) list.add(mapCostCategory(rs))
                    list
                }
            }
        }
    }

    // --- Cost Tracking ---

    override suspend fun createCostTracking(tracking: BusinessCostTracking): BusinessCostTracking {
        return transactionManager.inTransaction(TenantContext(tracking.projectId)) { tx ->
            val sql = """
                INSERT INTO business_cost_tracking (
                    id, tenant_id, project_id, source_type, source_id,
                    ledger_posting_id, cost_center_id, cost_category_id,
                    job_id, amount, currency, allocation_status,
                    classification_status, notes, created_at, created_by,
                    updated_at, updated_by, version
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, tracking.id)
                ps.setString(2, tracking.tenantId)
                ps.setString(3, tracking.projectId)
                ps.setString(4, tracking.sourceType.name)
                ps.setString(5, tracking.sourceId)
                ps.setString(6, tracking.ledgerPostingId)
                ps.setString(7, tracking.costCenterId)
                ps.setString(8, tracking.costCategoryId)
                ps.setString(9, tracking.jobId)
                ps.setBigDecimal(10, tracking.amount)
                ps.setString(11, tracking.currency)
                ps.setString(12, tracking.allocationStatus.name)
                ps.setString(13, tracking.classificationStatus.name)
                ps.setString(14, tracking.notes)
                ps.setLong(15, tracking.createdAt)
                ps.setString(16, tracking.createdBy)
                ps.setLong(17, tracking.updatedAt)
                ps.setString(18, tracking.updatedBy)
                ps.setLong(19, tracking.version)
                ps.executeUpdate()
            }
            tracking
        }
    }

    override suspend fun findCostTrackingById(id: String, tenantId: String, projectId: String): BusinessCostTracking? {
        return transactionManager.inReadOnly(TenantContext(projectId)) { tx ->
            val sql = "SELECT * FROM business_cost_tracking WHERE id = ? AND tenant_id = ? AND project_id = ?"
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, id)
                ps.setString(2, tenantId)
                ps.setString(3, projectId)
                ps.executeQuery().use { rs ->
                    if (rs.next()) mapCostTracking(rs) else null
                }
            }
        }
    }

    override suspend fun findCostTrackingBySource(
        sourceType: BusinessCostTrackingSourceType,
        sourceId: String,
        tenantId: String,
        projectId: String
    ): List<BusinessCostTracking> {
        return transactionManager.inReadOnly(TenantContext(projectId)) { tx ->
            val sql = "SELECT * FROM business_cost_tracking WHERE source_type = ? AND source_id = ? AND tenant_id = ? AND project_id = ? ORDER BY created_at DESC"
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, sourceType.name)
                ps.setString(2, sourceId)
                ps.setString(3, tenantId)
                ps.setString(4, projectId)
                ps.executeQuery().use { rs ->
                    val list = mutableListOf<BusinessCostTracking>()
                    while (rs.next()) list.add(mapCostTracking(rs))
                    list
                }
            }
        }
    }

    override suspend fun updateCostTracking(tracking: BusinessCostTracking): BusinessCostTracking {
        return transactionManager.inTransaction(TenantContext(tracking.projectId)) { tx ->
            val sql = """
                UPDATE business_cost_tracking SET
                    cost_center_id = ?, cost_category_id = ?, job_id = ?,
                    amount = ?, allocation_status = ?, classification_status = ?,
                    notes = ?, updated_at = ?, updated_by = ?, version = version + 1
                WHERE id = ? AND tenant_id = ? AND project_id = ?
            """.trimIndent()
            val rows = tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, tracking.costCenterId)
                ps.setString(2, tracking.costCategoryId)
                ps.setString(3, tracking.jobId)
                ps.setBigDecimal(4, tracking.amount)
                ps.setString(5, tracking.allocationStatus.name)
                ps.setString(6, tracking.classificationStatus.name)
                ps.setString(7, tracking.notes)
                ps.setLong(8, System.currentTimeMillis())
                ps.setString(9, tracking.updatedBy)
                ps.setString(10, tracking.id)
                ps.setString(11, tracking.tenantId)
                ps.setString(12, tracking.projectId)
                ps.executeUpdate()
            }
            if (rows == 0) throw NoSuchElementException("Cost tracking '${tracking.id}' not found for update.")
            tracking.copy(version = tracking.version + 1, updatedAt = System.currentTimeMillis())
        }
    }

    override suspend fun listCostTracking(
        tenantId: String,
        projectId: String,
        filter: BusinessCostTrackingFilter
    ): List<BusinessCostTracking> {
        return transactionManager.inReadOnly(TenantContext(projectId)) { tx ->
            val params = mutableListOf<Any>()
            val sql = buildString {
                append("SELECT * FROM business_cost_tracking WHERE tenant_id = ? AND project_id = ?")
                params.add(tenantId)
                params.add(projectId)

                if (filter.sourceType != null) {
                    append(" AND source_type = ?")
                    params.add(filter.sourceType.name)
                }
                if (filter.sourceId != null) {
                    append(" AND source_id = ?")
                    params.add(filter.sourceId)
                }
                if (filter.costCenterId != null) {
                    append(" AND cost_center_id = ?")
                    params.add(filter.costCenterId)
                }
                if (filter.costCategoryId != null) {
                    append(" AND cost_category_id = ?")
                    params.add(filter.costCategoryId)
                }
                if (filter.jobId != null) {
                    append(" AND job_id = ?")
                    params.add(filter.jobId)
                }
                if (filter.allocationStatus != null) {
                    append(" AND allocation_status = ?")
                    params.add(filter.allocationStatus.name)
                }
                if (filter.classificationStatus != null) {
                    append(" AND classification_status = ?")
                    params.add(filter.classificationStatus.name)
                }
                if (filter.fromDate != null) {
                    append(" AND created_at >= ?")
                    params.add(filter.fromDate)
                }
                if (filter.toDate != null) {
                    append(" AND created_at <= ?")
                    params.add(filter.toDate)
                }
                append(" ORDER BY created_at DESC LIMIT ? OFFSET ?")
                params.add(filter.limit)
                params.add(filter.offset)
            }

            tx.connection.prepareStatement(sql).use { ps ->
                params.forEachIndexed { i, p ->
                    when (p) {
                        is String -> ps.setString(i + 1, p)
                        is Long -> ps.setLong(i + 1, p)
                        is Int -> ps.setInt(i + 1, p)
                    }
                }
                ps.executeQuery().use { rs ->
                    val list = mutableListOf<BusinessCostTracking>()
                    while (rs.next()) list.add(mapCostTracking(rs))
                    list
                }
            }
        }
    }

    // --- Audits ---

    override suspend fun recordAuditEvent(event: BusinessCostClassificationAuditEvent) {
        transactionManager.inTransaction(TenantContext(event.projectId)) { tx ->
            val sql = """
                INSERT INTO business_cost_classification_audit_events (
                    event_id, tenant_id, project_id, tracking_id, action,
                    actor_id, actor_role, previous_state_json, new_state_json,
                    reason, correlation_id, idempotency_key, timestamp
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, event.eventId)
                ps.setString(2, event.tenantId)
                ps.setString(3, event.projectId)
                ps.setString(4, event.trackingId)
                ps.setString(5, event.action)
                ps.setString(6, event.actorId)
                ps.setString(7, event.actorRole)
                ps.setString(8, event.previousStateJson)
                ps.setString(9, event.newStateJson)
                ps.setString(10, event.reason)
                ps.setString(11, event.correlationId)
                ps.setString(12, event.idempotencyKey)
                ps.setLong(13, event.timestamp)
                ps.executeUpdate()
            }
        }
    }

    override suspend fun listAuditEvents(
        tenantId: String,
        projectId: String,
        trackingId: String?
    ): List<BusinessCostClassificationAuditEvent> {
        return transactionManager.inReadOnly(TenantContext(projectId)) { tx ->
            val sql = buildString {
                append("SELECT * FROM business_cost_classification_audit_events WHERE tenant_id = ? AND project_id = ?")
                if (trackingId != null) append(" AND tracking_id = ?")
                append(" ORDER BY timestamp DESC")
            }
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, projectId)
                if (trackingId != null) ps.setString(3, trackingId)
                ps.executeQuery().use { rs ->
                    val list = mutableListOf<BusinessCostClassificationAuditEvent>()
                    while (rs.next()) {
                        list.add(
                            BusinessCostClassificationAuditEvent(
                                eventId = rs.getString("event_id"),
                                tenantId = rs.getString("tenant_id"),
                                projectId = rs.getString("project_id"),
                                trackingId = rs.getString("tracking_id"),
                                action = rs.getString("action"),
                                actorId = rs.getString("actor_id"),
                                actorRole = rs.getString("actor_role"),
                                previousStateJson = rs.getString("previous_state_json"),
                                newStateJson = rs.getString("new_state_json"),
                                reason = rs.getString("reason"),
                                correlationId = rs.getString("correlation_id"),
                                idempotencyKey = rs.getString("idempotency_key"),
                                timestamp = rs.getLong("timestamp")
                            )
                        )
                    }
                    list
                }
            }
        }
    }

    // --- Rollup Projections ---

    override suspend fun calculateCostCenterSummary(
        costCenterId: String,
        tenantId: String,
        projectId: String
    ): BusinessCostCenterSummary {
        val center = findCostCenterById(costCenterId, tenantId, projectId)
            ?: throw NoSuchElementException("Cost center '$costCenterId' not found.")
        val trackingList = listCostTracking(tenantId, projectId, BusinessCostTrackingFilter(costCenterId = costCenterId, limit = 5000))

        var total = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
        var allocated = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)

        for (item in trackingList) {
            total = total.add(item.amount)
            if (item.allocationStatus in setOf(BusinessCostAllocationStatus.FULLY_ALLOCATED, BusinessCostAllocationStatus.PARTIALLY_ALLOCATED)) {
                allocated = allocated.add(item.amount)
            }
        }
        val unallocated = total.subtract(allocated).setScale(4, RoundingMode.HALF_UP)

        return BusinessCostCenterSummary(
            costCenterId = center.id,
            code = center.code,
            name = center.name,
            parentCostCenterId = center.parentCostCenterId,
            isActive = center.isActive,
            totalCost = total,
            allocatedCost = allocated,
            unallocatedCost = unallocated,
            trackedItemCount = trackingList.size
        )
    }

    override suspend fun calculateCostCategorySummary(
        categoryId: String,
        tenantId: String,
        projectId: String
    ): BusinessCostCategorySummary {
        val cat = findCostCategoryById(categoryId, tenantId, projectId)
            ?: throw NoSuchElementException("Cost category '$categoryId' not found.")
        val trackingList = listCostTracking(tenantId, projectId, BusinessCostTrackingFilter(costCategoryId = categoryId, limit = 5000))

        var total = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
        val jobs = mutableSetOf<String>()

        for (item in trackingList) {
            total = total.add(item.amount)
            if (!item.jobId.isNullOrBlank()) jobs.add(item.jobId)
        }

        return BusinessCostCategorySummary(
            categoryId = cat.id,
            code = cat.code,
            name = cat.name,
            parentCategoryId = cat.parentCategoryId,
            isActive = cat.isActive,
            isSystemDefined = cat.isSystemDefined,
            totalCost = total,
            jobCount = jobs.size,
            trackedItemCount = trackingList.size
        )
    }

    override suspend fun calculateJobCostDetail(
        jobId: String,
        tenantId: String,
        projectId: String
    ): BusinessJobCostDetailSummary {
        val trackingList = listCostTracking(tenantId, projectId, BusinessCostTrackingFilter(jobId = jobId, limit = 5000))
        val categories = listCostCategories(tenantId, projectId)

        var total = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
        var production = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
        var vendor = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
        var expense = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
        var transport = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
        var labour = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
        var other = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
        var allocated = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)

        for (item in trackingList) {
            total = total.add(item.amount)
            val cat = categories.find { it.id == item.costCategoryId }
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

        return BusinessJobCostDetailSummary(
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
            itemCount = trackingList.size,
            items = trackingList
        )
    }

    override suspend fun calculateTrackingSummary(tenantId: String, projectId: String): BusinessCostTrackingSummary {
        val trackingList = listCostTracking(tenantId, projectId, BusinessCostTrackingFilter(limit = 10000))
        val costCenters = listCostCenters(tenantId, projectId)
        val costCategories = listCostCategories(tenantId, projectId)

        var total = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
        var allocated = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
        val jobs = mutableSetOf<String>()
        var reclassPending = 0

        for (item in trackingList) {
            total = total.add(item.amount)
            if (item.allocationStatus != BusinessCostAllocationStatus.UNALLOCATED) {
                allocated = allocated.add(item.amount)
            }
            if (!item.jobId.isNullOrBlank()) jobs.add(item.jobId)
            if (item.allocationStatus == BusinessCostAllocationStatus.RECLASSIFICATION_PENDING) reclassPending++
        }
        val unallocated = total.subtract(allocated).setScale(4, RoundingMode.HALF_UP)

        return BusinessCostTrackingSummary(
            totalTrackedCost = total,
            totalAllocatedCost = allocated,
            totalUnallocatedCost = unallocated,
            totalCostCenters = costCenters.size,
            totalActiveCategories = costCategories.count { it.isActive },
            jobsWithCostCount = jobs.size,
            reclassificationPendingCount = reclassPending
        )
    }

    // --- Mappers ---

    private fun mapCostCenter(rs: ResultSet): BusinessCostCenter {
        return BusinessCostCenter(
            id = rs.getString("id"),
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            code = rs.getString("code"),
            name = rs.getString("name"),
            description = rs.getString("description"),
            parentCostCenterId = rs.getString("parent_cost_center_id"),
            isActive = rs.getBoolean("is_active"),
            createdAt = rs.getLong("created_at"),
            createdBy = rs.getString("created_by"),
            updatedAt = rs.getLong("updated_at"),
            updatedBy = rs.getString("updated_by"),
            version = rs.getLong("version")
        )
    }

    private fun mapCostCategory(rs: ResultSet): BusinessCostCategory {
        return BusinessCostCategory(
            id = rs.getString("id"),
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            code = rs.getString("code"),
            name = rs.getString("name"),
            description = rs.getString("description"),
            parentCategoryId = rs.getString("parent_category_id"),
            isActive = rs.getBoolean("is_active"),
            isSystemDefined = rs.getBoolean("is_system_defined"),
            createdAt = rs.getLong("created_at"),
            createdBy = rs.getString("created_by"),
            updatedAt = rs.getLong("updated_at"),
            updatedBy = rs.getString("updated_by"),
            version = rs.getLong("version")
        )
    }

    private fun mapCostTracking(rs: ResultSet): BusinessCostTracking {
        return BusinessCostTracking(
            id = rs.getString("id"),
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            sourceType = BusinessCostTrackingSourceType.valueOf(rs.getString("source_type")),
            sourceId = rs.getString("source_id"),
            ledgerPostingId = rs.getString("ledger_posting_id"),
            costCenterId = rs.getString("cost_center_id"),
            costCategoryId = rs.getString("cost_category_id"),
            jobId = rs.getString("job_id"),
            amount = rs.getBigDecimal("amount").setScale(4, RoundingMode.HALF_UP),
            currency = rs.getString("currency"),
            allocationStatus = BusinessCostAllocationStatus.valueOf(rs.getString("allocation_status")),
            classificationStatus = BusinessCostClassificationStatus.valueOf(rs.getString("classification_status")),
            notes = rs.getString("notes"),
            createdAt = rs.getLong("created_at"),
            createdBy = rs.getString("created_by"),
            updatedAt = rs.getLong("updated_at"),
            updatedBy = rs.getString("updated_by"),
            version = rs.getLong("version")
        )
    }
}
