package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.datasource.profitability.VendorProfitabilityDataSource
import com.sucharu.sucharupro.domain.model.profitability.*
import java.math.BigDecimal
import java.sql.ResultSet
import java.util.UUID

/**
 * PostgreSQL Implementation of VendorProfitabilityDataSource with Row-Level Security enforcement.
 * Module 16 Step 05.
 */
class PostgresVendorProfitabilityDataSource(
    private val transactionManager: TransactionManager
) : VendorProfitabilityDataSource {

    override suspend fun saveSnapshot(snapshot: VendorProfitabilitySnapshot): VendorProfitabilitySnapshot {
        transactionManager.inTransaction(TenantContext(snapshot.projectId)) { tx ->
            val sql = """
                INSERT INTO vendor_profitability_snapshots (
                    snapshot_id, tenant_id, project_id, vendor_id, vendor_name, vendor_code,
                    service_category, vendor_status, period_id, period_start, period_end,
                    currency, generated_at, total_vendor_cost, direct_vendor_cost, paid_vendor_cost,
                    outstanding_exposure, unbilled_estimate_cost, rework_cost, baseline_cost,
                    cost_variance, cost_variance_percentage, attributed_revenue_context,
                    attributed_total_job_cost, fulfillment_profitability_impact,
                    cost_to_revenue_context_percentage, vendor_cost_share_percentage,
                    attributed_work_order_count, attributed_job_count, attributed_product_count,
                    attributed_customer_count, total_attributed_quantity, cost_per_job,
                    cost_per_unit, quality_failure_count, rework_count, rejection_count,
                    dispute_count, quality_failure_rate, rework_rate, efficiency_score,
                    risk_classification, dependency_classification, dependency_share_percentage,
                    trend_direction, data_readiness, integrity_hash
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (snapshot_id) DO UPDATE SET
                    total_vendor_cost = EXCLUDED.total_vendor_cost,
                    direct_vendor_cost = EXCLUDED.direct_vendor_cost,
                    paid_vendor_cost = EXCLUDED.paid_vendor_cost,
                    outstanding_exposure = EXCLUDED.outstanding_exposure,
                    unbilled_estimate_cost = EXCLUDED.unbilled_estimate_cost,
                    rework_cost = EXCLUDED.rework_cost,
                    baseline_cost = EXCLUDED.baseline_cost,
                    cost_variance = EXCLUDED.cost_variance,
                    cost_variance_percentage = EXCLUDED.cost_variance_percentage,
                    attributed_revenue_context = EXCLUDED.attributed_revenue_context,
                    attributed_total_job_cost = EXCLUDED.attributed_total_job_cost,
                    fulfillment_profitability_impact = EXCLUDED.fulfillment_profitability_impact,
                    cost_to_revenue_context_percentage = EXCLUDED.cost_to_revenue_context_percentage,
                    vendor_cost_share_percentage = EXCLUDED.vendor_cost_share_percentage,
                    attributed_work_order_count = EXCLUDED.attributed_work_order_count,
                    attributed_job_count = EXCLUDED.attributed_job_count,
                    attributed_product_count = EXCLUDED.attributed_product_count,
                    attributed_customer_count = EXCLUDED.attributed_customer_count,
                    total_attributed_quantity = EXCLUDED.total_attributed_quantity,
                    cost_per_job = EXCLUDED.cost_per_job,
                    cost_per_unit = EXCLUDED.cost_per_unit,
                    quality_failure_count = EXCLUDED.quality_failure_count,
                    rework_count = EXCLUDED.rework_count,
                    rejection_count = EXCLUDED.rejection_count,
                    dispute_count = EXCLUDED.dispute_count,
                    quality_failure_rate = EXCLUDED.quality_failure_rate,
                    rework_rate = EXCLUDED.rework_rate,
                    efficiency_score = EXCLUDED.efficiency_score,
                    risk_classification = EXCLUDED.risk_classification,
                    dependency_classification = EXCLUDED.dependency_classification,
                    dependency_share_percentage = EXCLUDED.dependency_share_percentage,
                    trend_direction = EXCLUDED.trend_direction,
                    data_readiness = EXCLUDED.data_readiness,
                    integrity_hash = EXCLUDED.integrity_hash
            """.trimIndent()

            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, snapshot.snapshotId)
                ps.setString(2, snapshot.tenantId)
                ps.setString(3, snapshot.projectId)
                ps.setString(4, snapshot.vendorId)
                ps.setString(5, snapshot.vendorName)
                ps.setString(6, snapshot.vendorCode)
                ps.setString(7, snapshot.serviceCategory)
                ps.setString(8, snapshot.vendorStatus)
                ps.setString(9, snapshot.periodId)
                if (snapshot.periodStart != null) ps.setLong(10, snapshot.periodStart) else ps.setNull(10, java.sql.Types.BIGINT)
                if (snapshot.periodEnd != null) ps.setLong(11, snapshot.periodEnd) else ps.setNull(11, java.sql.Types.BIGINT)
                ps.setString(12, snapshot.currency)
                ps.setLong(13, snapshot.generatedAt)
                ps.setBigDecimal(14, snapshot.totalVendorCost)
                ps.setBigDecimal(15, snapshot.directVendorCost)
                ps.setBigDecimal(16, snapshot.paidVendorCost)
                ps.setBigDecimal(17, snapshot.outstandingExposure)
                ps.setBigDecimal(18, snapshot.unbilledEstimateCost)
                ps.setBigDecimal(19, snapshot.reworkCost)
                ps.setBigDecimal(20, snapshot.baselineCost)
                ps.setBigDecimal(21, snapshot.costVariance)
                ps.setBigDecimal(22, snapshot.costVariancePercentage)
                ps.setBigDecimal(23, snapshot.attributedRevenueContext)
                ps.setBigDecimal(24, snapshot.attributedTotalJobCost)
                ps.setBigDecimal(25, snapshot.fulfillmentProfitabilityImpact)
                ps.setBigDecimal(26, snapshot.costToRevenueContextPercentage)
                ps.setBigDecimal(27, snapshot.vendorCostSharePercentage)
                ps.setInt(28, snapshot.attributedWorkOrderCount)
                ps.setInt(29, snapshot.attributedJobCount)
                ps.setInt(30, snapshot.attributedProductCount)
                ps.setInt(31, snapshot.attributedCustomerCount)
                ps.setLong(32, snapshot.totalAttributedQuantity)
                ps.setBigDecimal(33, snapshot.costPerJob)
                ps.setBigDecimal(34, snapshot.costPerUnit)
                ps.setInt(35, snapshot.qualityFailureCount)
                ps.setInt(36, snapshot.reworkCount)
                ps.setInt(37, snapshot.rejectionCount)
                ps.setInt(38, snapshot.disputeCount)
                ps.setBigDecimal(39, snapshot.qualityFailureRate)
                ps.setBigDecimal(40, snapshot.reworkRate)
                ps.setBigDecimal(41, snapshot.efficiencyScore)
                ps.setString(42, snapshot.riskClassification.name)
                ps.setString(43, snapshot.dependencyClassification.name)
                ps.setBigDecimal(44, snapshot.dependencySharePercentage)
                ps.setString(45, snapshot.trendDirection.name)
                ps.setString(46, snapshot.dataReadiness.name)
                ps.setString(47, snapshot.integrityHash)
                ps.executeUpdate()
            }

            // Save components
            tx.connection.prepareStatement("DELETE FROM vendor_profitability_components WHERE snapshot_id = ?").use { ps ->
                ps.setString(1, snapshot.snapshotId)
                ps.executeUpdate()
            }

            val compSql = """
                INSERT INTO vendor_profitability_components (
                    component_id, snapshot_id, tenant_id, project_id, component_type, amount, percentage_of_total_cost
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()

            tx.connection.prepareStatement(compSql).use { ps ->
                for (comp in snapshot.costBreakdown) {
                    ps.setString(1, UUID.randomUUID().toString())
                    ps.setString(2, snapshot.snapshotId)
                    ps.setString(3, snapshot.tenantId)
                    ps.setString(4, snapshot.projectId)
                    ps.setString(5, comp.componentType.name)
                    ps.setBigDecimal(6, comp.amount)
                    ps.setBigDecimal(7, comp.percentageOfTotalCost)
                    ps.addBatch()
                }
                ps.executeBatch()
            }
        }
        return snapshot
    }

    override suspend fun findSnapshotById(tenantId: String, snapshotId: String): VendorProfitabilitySnapshot? {
        return transactionManager.inTransaction(TenantContext(tenantId)) { tx ->
            val sql = "SELECT * FROM vendor_profitability_snapshots WHERE snapshot_id = ? AND tenant_id = ?"
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, snapshotId)
                ps.setString(2, tenantId)
                ps.executeQuery().use { rs ->
                    if (rs.next()) {
                        val snapshot = mapSnapshot(rs)
                        val components = loadComponents(tx, snapshot.snapshotId)
                        snapshot.copy(costBreakdown = components)
                    } else null
                }
            }
        }
    }

    override suspend fun findLatestSnapshotByVendorId(tenantId: String, vendorId: String): VendorProfitabilitySnapshot? {
        return transactionManager.inTransaction(TenantContext(tenantId)) { tx ->
            val sql = "SELECT * FROM vendor_profitability_snapshots WHERE vendor_id = ? AND tenant_id = ? ORDER BY generated_at DESC LIMIT 1"
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, vendorId)
                ps.setString(2, tenantId)
                ps.executeQuery().use { rs ->
                    if (rs.next()) {
                        val snapshot = mapSnapshot(rs)
                        val components = loadComponents(tx, snapshot.snapshotId)
                        snapshot.copy(costBreakdown = components)
                    } else null
                }
            }
        }
    }

    override suspend fun listSnapshots(tenantId: String, filter: VendorProfitabilityFilter): List<VendorProfitabilitySnapshot> {
        return transactionManager.inTransaction(TenantContext(tenantId)) { tx ->
            val conditions = mutableListOf("tenant_id = ?")
            val params = mutableListOf<Any>(tenantId)

            if (filter.vendorId != null) {
                conditions.add("vendor_id = ?")
                params.add(filter.vendorId)
            }
            if (filter.serviceCategory != null) {
                conditions.add("service_category = ?")
                params.add(filter.serviceCategory)
            }
            if (filter.periodId != null) {
                conditions.add("period_id = ?")
                params.add(filter.periodId)
            }
            if (filter.riskClassification != null) {
                conditions.add("risk_classification = ?")
                params.add(filter.riskClassification.name)
            }
            if (filter.dependencyClassification != null) {
                conditions.add("dependency_classification = ?")
                params.add(filter.dependencyClassification.name)
            }
            if (filter.isHighRisk == true) {
                conditions.add("risk_classification IN ('HIGH_RISK', 'CRITICAL_RISK')")
            }
            if (filter.isOverBudget == true) {
                conditions.add("cost_variance_percentage > 0")
            }
            if (filter.minSpend != null) {
                conditions.add("total_vendor_cost >= ?")
                params.add(filter.minSpend)
            }
            if (filter.maxSpend != null) {
                conditions.add("total_vendor_cost <= ?")
                params.add(filter.maxSpend)
            }

            val sql = "SELECT * FROM vendor_profitability_snapshots WHERE ${conditions.joinToString(" AND ")} ORDER BY generated_at DESC LIMIT ? OFFSET ?"
            params.add(filter.limit)
            params.add(filter.offset)

            tx.connection.prepareStatement(sql).use { ps ->
                params.forEachIndexed { i, p ->
                    when (p) {
                        is String -> ps.setString(i + 1, p)
                        is BigDecimal -> ps.setBigDecimal(i + 1, p)
                        is Int -> ps.setInt(i + 1, p)
                        is Long -> ps.setLong(i + 1, p)
                    }
                }
                ps.executeQuery().use { rs ->
                    val result = mutableListOf<VendorProfitabilitySnapshot>()
                    while (rs.next()) {
                        result.add(mapSnapshot(rs))
                    }
                    result
                }
            }
        }
    }

    override suspend fun saveCostAttributions(attributions: List<VendorCostAttribution>) {
        if (attributions.isEmpty()) return
        transactionManager.inTransaction(TenantContext(attributions.first().projectId)) { tx ->
            val sql = """
                INSERT INTO vendor_profitability_cost_attributions (
                    cost_attribution_id, tenant_id, project_id, vendor_id, work_order_id,
                    job_id, product_id, customer_id, component_type, attributed_amount,
                    is_paid, source_module, source_entity_type, source_entity_id,
                    source_transaction_id, attribution_method, provenance_fingerprint, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (cost_attribution_id) DO NOTHING
            """.trimIndent()

            tx.connection.prepareStatement(sql).use { ps ->
                for (a in attributions) {
                    ps.setString(1, a.costAttributionId)
                    ps.setString(2, a.tenantId)
                    ps.setString(3, a.projectId)
                    ps.setString(4, a.vendorId)
                    ps.setString(5, a.workOrderId)
                    ps.setString(6, a.jobId)
                    ps.setString(7, a.productId)
                    ps.setString(8, a.customerId)
                    ps.setString(9, a.componentType.name)
                    ps.setBigDecimal(10, a.attributedAmount)
                    ps.setBoolean(11, a.isPaid)
                    ps.setString(12, a.sourceModule)
                    ps.setString(13, a.sourceEntityType)
                    ps.setString(14, a.sourceEntityId)
                    ps.setString(15, a.sourceTransactionId)
                    ps.setString(16, a.attributionMethod.name)
                    ps.setString(17, a.provenanceFingerprint)
                    ps.setLong(18, a.createdAt)
                    ps.addBatch()
                }
                ps.executeBatch()
            }
        }
    }

    override suspend fun listCostAttributionsByVendorId(tenantId: String, vendorId: String): List<VendorCostAttribution> {
        return transactionManager.inTransaction(TenantContext(tenantId)) { tx ->
            val sql = "SELECT * FROM vendor_profitability_cost_attributions WHERE tenant_id = ? AND vendor_id = ? ORDER BY created_at DESC"
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, vendorId)
                ps.executeQuery().use { rs ->
                    val result = mutableListOf<VendorCostAttribution>()
                    while (rs.next()) {
                        result.add(
                            VendorCostAttribution(
                                costAttributionId = rs.getString("cost_attribution_id"),
                                tenantId = rs.getString("tenant_id"),
                                projectId = rs.getString("project_id"),
                                vendorId = rs.getString("vendor_id"),
                                workOrderId = rs.getString("work_order_id"),
                                jobId = rs.getString("job_id"),
                                productId = rs.getString("product_id"),
                                customerId = rs.getString("customer_id"),
                                componentType = JobCostComponentType.valueOf(rs.getString("component_type")),
                                attributedAmount = rs.getBigDecimal("attributed_amount"),
                                isPaid = rs.getBoolean("is_paid"),
                                sourceModule = rs.getString("source_module"),
                                sourceEntityType = rs.getString("source_entity_type"),
                                sourceEntityId = rs.getString("source_entity_id"),
                                sourceTransactionId = rs.getString("source_transaction_id"),
                                attributionMethod = VendorAttributionMethod.valueOf(rs.getString("attribution_method")),
                                provenanceFingerprint = rs.getString("provenance_fingerprint"),
                                createdAt = rs.getLong("created_at")
                            )
                        )
                    }
                    result
                }
            }
        }
    }

    override suspend fun saveRevenueContextAttributions(attributions: List<VendorRevenueContextAttribution>) {
        if (attributions.isEmpty()) return
        transactionManager.inTransaction(TenantContext(attributions.first().projectId)) { tx ->
            val sql = """
                INSERT INTO vendor_profitability_revenue_context (
                    revenue_context_id, tenant_id, project_id, vendor_id, job_id,
                    product_id, customer_id, recognized_revenue_context, source_module,
                    source_entity_type, source_entity_id, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (revenue_context_id) DO NOTHING
            """.trimIndent()

            tx.connection.prepareStatement(sql).use { ps ->
                for (r in attributions) {
                    ps.setString(1, r.revenueContextId)
                    ps.setString(2, r.tenantId)
                    ps.setString(3, r.projectId)
                    ps.setString(4, r.vendorId)
                    ps.setString(5, r.jobId)
                    ps.setString(6, r.productId)
                    ps.setString(7, r.customerId)
                    ps.setBigDecimal(8, r.recognizedRevenueContext)
                    ps.setString(9, r.sourceModule)
                    ps.setString(10, r.sourceEntityType)
                    ps.setString(11, r.sourceEntityId)
                    ps.setLong(12, r.createdAt)
                    ps.addBatch()
                }
                ps.executeBatch()
            }
        }
    }

    override suspend fun listRevenueContextByVendorId(tenantId: String, vendorId: String): List<VendorRevenueContextAttribution> {
        return transactionManager.inTransaction(TenantContext(tenantId)) { tx ->
            val sql = "SELECT * FROM vendor_profitability_revenue_context WHERE tenant_id = ? AND vendor_id = ? ORDER BY created_at DESC"
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, vendorId)
                ps.executeQuery().use { rs ->
                    val result = mutableListOf<VendorRevenueContextAttribution>()
                    while (rs.next()) {
                        result.add(
                            VendorRevenueContextAttribution(
                                revenueContextId = rs.getString("revenue_context_id"),
                                tenantId = rs.getString("tenant_id"),
                                projectId = rs.getString("project_id"),
                                vendorId = rs.getString("vendor_id"),
                                jobId = rs.getString("job_id"),
                                productId = rs.getString("product_id"),
                                customerId = rs.getString("customer_id"),
                                recognizedRevenueContext = rs.getBigDecimal("recognized_revenue_context"),
                                sourceModule = rs.getString("source_module"),
                                sourceEntityType = rs.getString("source_entity_type"),
                                sourceEntityId = rs.getString("source_entity_id"),
                                createdAt = rs.getLong("created_at")
                            )
                        )
                    }
                    result
                }
            }
        }
    }

    override suspend fun saveReconciliationEvent(event: VendorProfitabilityReconciliationEvent): VendorProfitabilityReconciliationEvent {
        transactionManager.inTransaction(TenantContext(event.projectId)) { tx ->
            val sql = """
                INSERT INTO vendor_profitability_reconciliation_events (
                    event_id, tenant_id, project_id, vendor_id, snapshot_id, is_balanced,
                    total_cost_difference, component_difference, provenance_difference,
                    job_difference, product_difference, customer_difference, paid_vs_liability_valid,
                    error_details, timestamp
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()

            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, event.eventId)
                ps.setString(2, event.tenantId)
                ps.setString(3, event.projectId)
                ps.setString(4, event.vendorId)
                ps.setString(5, event.snapshotId)
                ps.setBoolean(6, event.isBalanced)
                ps.setBigDecimal(7, event.totalCostDifference)
                ps.setBigDecimal(8, event.componentDifference)
                ps.setBigDecimal(9, event.provenanceDifference)
                ps.setBigDecimal(10, event.jobDifference)
                ps.setBigDecimal(11, event.productDifference)
                ps.setBigDecimal(12, event.customerDifference)
                ps.setBoolean(13, event.paidVsLiabilityValid)
                ps.setString(14, event.errorDetails.joinToString(";"))
                ps.setLong(15, event.timestamp)
                ps.executeUpdate()
            }
        }
        return event
    }

    override suspend fun listReconciliationEventsByVendorId(tenantId: String, vendorId: String): List<VendorProfitabilityReconciliationEvent> {
        return transactionManager.inTransaction(TenantContext(tenantId)) { tx ->
            val sql = "SELECT * FROM vendor_profitability_reconciliation_events WHERE tenant_id = ? AND vendor_id = ? ORDER BY timestamp DESC"
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, vendorId)
                ps.executeQuery().use { rs ->
                    val result = mutableListOf<VendorProfitabilityReconciliationEvent>()
                    while (rs.next()) {
                        val details = rs.getString("error_details")?.split(";")?.filter { it.isNotBlank() } ?: emptyList()
                        result.add(
                            VendorProfitabilityReconciliationEvent(
                                eventId = rs.getString("event_id"),
                                tenantId = rs.getString("tenant_id"),
                                projectId = rs.getString("project_id"),
                                vendorId = rs.getString("vendor_id"),
                                snapshotId = rs.getString("snapshot_id"),
                                isBalanced = rs.getBoolean("is_balanced"),
                                totalCostDifference = rs.getBigDecimal("total_cost_difference"),
                                componentDifference = rs.getBigDecimal("component_difference"),
                                provenanceDifference = rs.getBigDecimal("provenance_difference"),
                                jobDifference = rs.getBigDecimal("job_difference"),
                                productDifference = rs.getBigDecimal("product_difference"),
                                customerDifference = rs.getBigDecimal("customer_difference"),
                                paidVsLiabilityValid = rs.getBoolean("paid_vs_liability_valid"),
                                errorDetails = details,
                                timestamp = rs.getLong("timestamp")
                            )
                        )
                    }
                    result
                }
            }
        }
    }

    override suspend fun saveAuditEvent(event: VendorProfitabilityAuditEvent): VendorProfitabilityAuditEvent {
        transactionManager.inTransaction(TenantContext(event.projectId)) { tx ->
            val sql = """
                INSERT INTO vendor_profitability_audit_events (
                    audit_id, tenant_id, project_id, vendor_id, action, actor_id,
                    actor_role, details, integrity_hash, timestamp
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()

            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, event.auditId)
                ps.setString(2, event.tenantId)
                ps.setString(3, event.projectId)
                ps.setString(4, event.vendorId)
                ps.setString(5, event.action)
                ps.setString(6, event.actorId)
                ps.setString(7, event.actorRole)
                ps.setString(8, event.details)
                ps.setString(9, event.integrityHash)
                ps.setLong(10, event.timestamp)
                ps.executeUpdate()
            }
        }
        return event
    }

    override suspend fun listAuditEventsByVendorId(tenantId: String, vendorId: String): List<VendorProfitabilityAuditEvent> {
        return transactionManager.inTransaction(TenantContext(tenantId)) { tx ->
            val sql = "SELECT * FROM vendor_profitability_audit_events WHERE tenant_id = ? AND vendor_id = ? ORDER BY timestamp DESC"
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, vendorId)
                ps.executeQuery().use { rs ->
                    val result = mutableListOf<VendorProfitabilityAuditEvent>()
                    while (rs.next()) {
                        result.add(
                            VendorProfitabilityAuditEvent(
                                auditId = rs.getString("audit_id"),
                                tenantId = rs.getString("tenant_id"),
                                projectId = rs.getString("project_id"),
                                vendorId = rs.getString("vendor_id"),
                                action = rs.getString("action"),
                                actorId = rs.getString("actor_id"),
                                actorRole = rs.getString("actor_role"),
                                details = rs.getString("details"),
                                integrityHash = rs.getString("integrity_hash"),
                                timestamp = rs.getLong("timestamp")
                            )
                        )
                    }
                    result
                }
            }
        }
    }

    override suspend fun saveUnattributedItems(items: List<VendorUnattributedItem>) {
        if (items.isEmpty()) return
        transactionManager.inTransaction(TenantContext(items.first().projectId)) { tx ->
            val sql = """
                INSERT INTO vendor_profitability_unattributed_items (
                    unattributed_id, tenant_id, project_id, vendor_id, source_module,
                    source_entity_type, source_entity_id, amount, reason, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (unattributed_id) DO NOTHING
            """.trimIndent()

            tx.connection.prepareStatement(sql).use { ps ->
                for (u in items) {
                    ps.setString(1, u.unattributedId)
                    ps.setString(2, u.tenantId)
                    ps.setString(3, u.projectId)
                    ps.setString(4, u.vendorId)
                    ps.setString(5, u.sourceModule)
                    ps.setString(6, u.sourceEntityType)
                    ps.setString(7, u.sourceEntityId)
                    ps.setBigDecimal(8, u.amount)
                    ps.setString(9, u.reason)
                    ps.setLong(10, u.createdAt)
                    ps.addBatch()
                }
                ps.executeBatch()
            }
        }
    }

    override suspend fun listUnattributedItems(tenantId: String, vendorId: String?): List<VendorUnattributedItem> {
        return transactionManager.inTransaction(TenantContext(tenantId)) { tx ->
            val sql = if (vendorId != null) {
                "SELECT * FROM vendor_profitability_unattributed_items WHERE tenant_id = ? AND vendor_id = ? ORDER BY created_at DESC"
            } else {
                "SELECT * FROM vendor_profitability_unattributed_items WHERE tenant_id = ? ORDER BY created_at DESC"
            }
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                if (vendorId != null) ps.setString(2, vendorId)
                ps.executeQuery().use { rs ->
                    val result = mutableListOf<VendorUnattributedItem>()
                    while (rs.next()) {
                        result.add(
                            VendorUnattributedItem(
                                unattributedId = rs.getString("unattributed_id"),
                                tenantId = rs.getString("tenant_id"),
                                projectId = rs.getString("project_id"),
                                vendorId = rs.getString("vendor_id"),
                                sourceModule = rs.getString("source_module"),
                                sourceEntityType = rs.getString("source_entity_type"),
                                sourceEntityId = rs.getString("source_entity_id"),
                                amount = rs.getBigDecimal("amount"),
                                reason = rs.getString("reason"),
                                createdAt = rs.getLong("created_at")
                            )
                        )
                    }
                    result
                }
            }
        }
    }

    private fun loadComponents(tx: TransactionContext, snapshotId: String): List<VendorCostBreakdownItem> {
        val sql = "SELECT * FROM vendor_profitability_components WHERE snapshot_id = ?"
        return tx.connection.prepareStatement(sql).use { ps ->
            ps.setString(1, snapshotId)
            ps.executeQuery().use { rs ->
                val list = mutableListOf<VendorCostBreakdownItem>()
                while (rs.next()) {
                    list.add(
                        VendorCostBreakdownItem(
                            componentType = JobCostComponentType.valueOf(rs.getString("component_type")),
                            amount = rs.getBigDecimal("amount"),
                            percentageOfTotalCost = rs.getBigDecimal("percentage_of_total_cost")
                        )
                    )
                }
                list
            }
        }
    }

    private fun mapSnapshot(rs: ResultSet): VendorProfitabilitySnapshot {
        return VendorProfitabilitySnapshot(
            snapshotId = rs.getString("snapshot_id"),
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            vendorId = rs.getString("vendor_id"),
            vendorName = rs.getString("vendor_name"),
            vendorCode = rs.getString("vendor_code"),
            serviceCategory = rs.getString("service_category"),
            vendorStatus = rs.getString("vendor_status"),
            periodId = rs.getString("period_id"),
            periodStart = rs.getObject("period_start") as? Long,
            periodEnd = rs.getObject("period_end") as? Long,
            currency = rs.getString("currency"),
            generatedAt = rs.getLong("generated_at"),
            totalVendorCost = rs.getBigDecimal("total_vendor_cost"),
            directVendorCost = rs.getBigDecimal("direct_vendor_cost"),
            paidVendorCost = rs.getBigDecimal("paid_vendor_cost"),
            outstandingExposure = rs.getBigDecimal("outstanding_exposure"),
            unbilledEstimateCost = rs.getBigDecimal("unbilled_estimate_cost") ?: BigDecimal.ZERO,
            reworkCost = rs.getBigDecimal("rework_cost") ?: BigDecimal.ZERO,
            baselineCost = rs.getBigDecimal("baseline_cost"),
            costVariance = rs.getBigDecimal("cost_variance"),
            costVariancePercentage = rs.getBigDecimal("cost_variance_percentage"),
            attributedRevenueContext = rs.getBigDecimal("attributed_revenue_context") ?: BigDecimal.ZERO,
            attributedTotalJobCost = rs.getBigDecimal("attributed_total_job_cost") ?: BigDecimal.ZERO,
            fulfillmentProfitabilityImpact = rs.getBigDecimal("fulfillment_profitability_impact") ?: BigDecimal.ZERO,
            costToRevenueContextPercentage = rs.getBigDecimal("cost_to_revenue_context_percentage"),
            vendorCostSharePercentage = rs.getBigDecimal("vendor_cost_share_percentage"),
            attributedWorkOrderCount = rs.getInt("attributed_work_order_count"),
            attributedJobCount = rs.getInt("attributed_job_count"),
            attributedProductCount = rs.getInt("attributed_product_count"),
            attributedCustomerCount = rs.getInt("attributed_customer_count"),
            totalAttributedQuantity = rs.getLong("total_attributed_quantity"),
            costPerJob = rs.getBigDecimal("cost_per_job"),
            costPerUnit = rs.getBigDecimal("cost_per_unit"),
            qualityFailureCount = rs.getInt("quality_failure_count"),
            reworkCount = rs.getInt("rework_count"),
            rejectionCount = rs.getInt("rejection_count"),
            disputeCount = rs.getInt("dispute_count"),
            qualityFailureRate = rs.getBigDecimal("quality_failure_rate"),
            reworkRate = rs.getBigDecimal("rework_rate"),
            efficiencyScore = rs.getBigDecimal("efficiency_score") ?: BigDecimal.ZERO,
            riskClassification = VendorRiskClassification.valueOf(rs.getString("risk_classification")),
            dependencyClassification = VendorDependencyClassification.valueOf(rs.getString("dependency_classification")),
            dependencySharePercentage = rs.getBigDecimal("dependency_share_percentage"),
            trendDirection = VendorTrendDirection.valueOf(rs.getString("trend_direction")),
            dataReadiness = VendorSourceReadiness.valueOf(rs.getString("data_readiness")),
            integrityHash = rs.getString("integrity_hash")
        )
    }
}
