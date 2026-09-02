package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.datasource.profitability.CustomerProfitabilityDataSource
import com.sucharu.sucharupro.domain.model.profitability.*
import java.math.BigDecimal
import java.sql.ResultSet

/**
 * PostgreSQL Implementation of CustomerProfitabilityDataSource with Row-Level Security enforcement.
 */
class PostgresCustomerProfitabilityDataSource(
    private val transactionManager: TransactionManager
) : CustomerProfitabilityDataSource {

    override suspend fun saveSnapshot(snapshot: CustomerProfitabilitySnapshot) {
        transactionManager.inTransaction(TenantContext(snapshot.projectId)) { tx ->
            val sql = """
                INSERT INTO customer_profitability_snapshots (
                    snapshot_id, tenant_id, project_id, customer_id, customer_name, customer_code,
                    period_type, period_start, period_end, recognized_revenue, total_actual_cost,
                    gross_profit, gross_margin_percentage, attributable_variable_cost, attributable_fixed_cost,
                    contribution_amount, contribution_margin_percentage, cost_to_revenue_percentage,
                    order_count, job_count, product_count, total_quantity_sold, average_order_value,
                    average_job_value, average_revenue_per_unit, average_cost_per_unit, average_profit_per_unit,
                    unit_economics_status, profitability_classification, trend, concentration_risk,
                    is_loss_making, is_low_margin, source_integrity_status, is_reconciled,
                    reconciliation_discrepancy, calculation_version, generated_at, generated_by, integrity_hash
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (snapshot_id) DO UPDATE SET
                    recognized_revenue = EXCLUDED.recognized_revenue,
                    total_actual_cost = EXCLUDED.total_actual_cost,
                    gross_profit = EXCLUDED.gross_profit,
                    gross_margin_percentage = EXCLUDED.gross_margin_percentage,
                    attributable_variable_cost = EXCLUDED.attributable_variable_cost,
                    attributable_fixed_cost = EXCLUDED.attributable_fixed_cost,
                    contribution_amount = EXCLUDED.contribution_amount,
                    contribution_margin_percentage = EXCLUDED.contribution_margin_percentage,
                    cost_to_revenue_percentage = EXCLUDED.cost_to_revenue_percentage,
                    order_count = EXCLUDED.order_count,
                    job_count = EXCLUDED.job_count,
                    product_count = EXCLUDED.product_count,
                    total_quantity_sold = EXCLUDED.total_quantity_sold,
                    average_order_value = EXCLUDED.average_order_value,
                    average_job_value = EXCLUDED.average_job_value,
                    average_revenue_per_unit = EXCLUDED.average_revenue_per_unit,
                    average_cost_per_unit = EXCLUDED.average_cost_per_unit,
                    average_profit_per_unit = EXCLUDED.average_profit_per_unit,
                    profitability_classification = EXCLUDED.profitability_classification,
                    trend = EXCLUDED.trend,
                    concentration_risk = EXCLUDED.concentration_risk,
                    is_loss_making = EXCLUDED.is_loss_making,
                    is_low_margin = EXCLUDED.is_low_margin,
                    source_integrity_status = EXCLUDED.source_integrity_status,
                    is_reconciled = EXCLUDED.is_reconciled,
                    reconciliation_discrepancy = EXCLUDED.reconciliation_discrepancy,
                    generated_at = EXCLUDED.generated_at,
                    integrity_hash = EXCLUDED.integrity_hash
            """.trimIndent()

            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, snapshot.snapshotId)
                ps.setString(2, snapshot.tenantId)
                ps.setString(3, snapshot.projectId)
                ps.setString(4, snapshot.customerId)
                ps.setString(5, snapshot.customerName)
                ps.setString(6, snapshot.customerCode)
                ps.setString(7, snapshot.periodType.name)
                if (snapshot.periodStart != null) ps.setLong(8, snapshot.periodStart) else ps.setNull(8, java.sql.Types.BIGINT)
                if (snapshot.periodEnd != null) ps.setLong(9, snapshot.periodEnd) else ps.setNull(9, java.sql.Types.BIGINT)
                ps.setBigDecimal(10, snapshot.recognizedRevenue)
                ps.setBigDecimal(11, snapshot.totalActualCost)
                ps.setBigDecimal(12, snapshot.grossProfit)
                ps.setBigDecimal(13, snapshot.grossMarginPercentage)
                ps.setBigDecimal(14, snapshot.contributionMetrics.attributableVariableCost)
                ps.setBigDecimal(15, snapshot.contributionMetrics.attributableFixedCost)
                ps.setBigDecimal(16, snapshot.contributionMetrics.contributionAmount)
                ps.setBigDecimal(17, snapshot.contributionMetrics.contributionMarginPercentage)
                ps.setBigDecimal(18, snapshot.contributionMetrics.costToRevenuePercentage)
                ps.setInt(19, snapshot.operationalMetrics.orderCount)
                ps.setInt(20, snapshot.operationalMetrics.jobCount)
                ps.setInt(21, snapshot.operationalMetrics.productCount)
                ps.setInt(22, snapshot.operationalMetrics.totalQuantitySold)
                ps.setBigDecimal(23, snapshot.operationalMetrics.averageOrderValue)
                ps.setBigDecimal(24, snapshot.operationalMetrics.averageJobValue)
                ps.setBigDecimal(25, snapshot.operationalMetrics.averageRevenuePerUnit)
                ps.setBigDecimal(26, snapshot.operationalMetrics.averageCostPerUnit)
                ps.setBigDecimal(27, snapshot.operationalMetrics.averageProfitPerUnit)
                ps.setString(28, snapshot.operationalMetrics.unitEconomicsStatus)
                ps.setString(29, snapshot.profitabilityClassification.name)
                ps.setString(30, snapshot.trend.name)
                ps.setString(31, snapshot.concentrationRisk.name)
                ps.setBoolean(32, snapshot.isLossMaking)
                ps.setBoolean(33, snapshot.isLowMargin)
                ps.setString(34, snapshot.sourceIntegrityStatus.name)
                ps.setBoolean(35, snapshot.isReconciled)
                ps.setBigDecimal(36, snapshot.reconciliationDiscrepancy)
                ps.setString(37, snapshot.calculationVersion)
                ps.setLong(38, snapshot.generatedAt)
                ps.setString(39, snapshot.generatedBy)
                ps.setString(40, snapshot.integrityHash)
                ps.executeUpdate()
            }

            // Save Cost Components Breakdown
            if (snapshot.costBreakdown.isNotEmpty()) {
                val compSql = """
                    INSERT INTO customer_profitability_components (
                        snapshot_id, tenant_id, project_id, customer_id, component_type,
                        amount, percentage_of_total_cost, is_variable_cost, source_count,
                        allocation_basis, provenance_fingerprints
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (snapshot_id, component_type) DO UPDATE SET
                        amount = EXCLUDED.amount,
                        percentage_of_total_cost = EXCLUDED.percentage_of_total_cost,
                        is_variable_cost = EXCLUDED.is_variable_cost,
                        source_count = EXCLUDED.source_count,
                        allocation_basis = EXCLUDED.allocation_basis,
                        provenance_fingerprints = EXCLUDED.provenance_fingerprints
                """.trimIndent()

                tx.connection.prepareStatement(compSql).use { ps ->
                    for (comp in snapshot.costBreakdown) {
                        ps.setString(1, snapshot.snapshotId)
                        ps.setString(2, snapshot.tenantId)
                        ps.setString(3, snapshot.projectId)
                        ps.setString(4, snapshot.customerId)
                        ps.setString(5, comp.componentType.name)
                        ps.setBigDecimal(6, comp.amount)
                        ps.setBigDecimal(7, comp.percentageOfTotalCost)
                        ps.setBoolean(8, comp.isVariableCost)
                        ps.setInt(9, comp.sourceCount)
                        ps.setString(10, comp.allocationBasis)
                        ps.setString(11, comp.provenanceFingerprints.joinToString(","))
                        ps.addBatch()
                    }
                    ps.executeBatch()
                }
            }
        }
    }

    override suspend fun getSnapshotById(tenantId: String, projectId: String, snapshotId: String): CustomerProfitabilitySnapshot? {
        return transactionManager.inTransaction(TenantContext(projectId)) { tx ->
            val sql = "SELECT * FROM customer_profitability_snapshots WHERE tenant_id = ? AND project_id = ? AND snapshot_id = ?"
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, projectId)
                ps.setString(3, snapshotId)
                val rs = ps.executeQuery()
                if (rs.next()) {
                    val snap = mapSnapshot(rs)
                    val comps = loadComponents(tx.connection, tenantId, projectId, snap.snapshotId)
                    snap.copy(costBreakdown = comps)
                } else null
            }
        }
    }

    override suspend fun getLatestSnapshotByCustomer(tenantId: String, projectId: String, customerId: String): CustomerProfitabilitySnapshot? {
        return transactionManager.inTransaction(TenantContext(projectId)) { tx ->
            val sql = "SELECT * FROM customer_profitability_snapshots WHERE tenant_id = ? AND project_id = ? AND customer_id = ? ORDER BY generated_at DESC LIMIT 1"
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, projectId)
                ps.setString(3, customerId)
                val rs = ps.executeQuery()
                if (rs.next()) {
                    val snap = mapSnapshot(rs)
                    val comps = loadComponents(tx.connection, tenantId, projectId, snap.snapshotId)
                    snap.copy(costBreakdown = comps)
                } else null
            }
        }
    }

    override suspend fun listSnapshots(tenantId: String, projectId: String, filter: CustomerProfitabilityFilter): List<CustomerProfitabilitySnapshot> {
        return transactionManager.inTransaction(TenantContext(projectId)) { tx ->
            val conditions = mutableListOf("tenant_id = ?", "project_id = ?")
            val params = mutableListOf<Any>(tenantId, projectId)

            if (filter.customerId != null) {
                conditions.add("customer_id = ?")
                params.add(filter.customerId)
            }
            if (filter.classification != null) {
                conditions.add("profitability_classification = ?")
                params.add(filter.classification.name)
            }
            if (filter.isLossMaking != null) {
                conditions.add("is_loss_making = ?")
                params.add(filter.isLossMaking)
            }
            if (filter.isLowMargin != null) {
                conditions.add("is_low_margin = ?")
                params.add(filter.isLowMargin)
            }

            val sql = "SELECT * FROM customer_profitability_snapshots WHERE ${conditions.joinToString(" AND ")} ORDER BY generated_at DESC LIMIT ? OFFSET ?"
            tx.connection.prepareStatement(sql).use { ps ->
                var idx = 1
                for (param in params) {
                    when (param) {
                        is String -> ps.setString(idx++, param)
                        is Boolean -> ps.setBoolean(idx++, param)
                        is Int -> ps.setInt(idx++, param)
                    }
                }
                ps.setInt(idx++, filter.limit)
                ps.setInt(idx, filter.offset)

                val rs = ps.executeQuery()
                val list = mutableListOf<CustomerProfitabilitySnapshot>()
                while (rs.next()) {
                    list.add(mapSnapshot(rs))
                }
                list
            }
        }
    }

    override suspend fun saveRevenueAttributions(attributions: List<CustomerRevenueAttribution>) {
        if (attributions.isEmpty()) return
        transactionManager.inTransaction(TenantContext(attributions.first().projectId)) { tx ->
            val sql = """
                INSERT INTO customer_profitability_revenue_attributions (
                    revenue_attribution_id, tenant_id, project_id, customer_id, order_id,
                    invoice_id, invoice_line_id, product_id, quantity, recognized_revenue,
                    currency, source_module, source_entity_type, source_entity_id,
                    source_transaction_id, provenance_fingerprint
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (revenue_attribution_id) DO NOTHING
            """.trimIndent()

            tx.connection.prepareStatement(sql).use { ps ->
                for (rev in attributions) {
                    ps.setString(1, rev.revenueAttributionId)
                    ps.setString(2, rev.tenantId)
                    ps.setString(3, rev.projectId)
                    ps.setString(4, rev.customerId)
                    ps.setString(5, rev.orderId)
                    ps.setString(6, rev.invoiceId)
                    ps.setString(7, rev.invoiceLineId)
                    ps.setString(8, rev.productId)
                    ps.setInt(9, rev.quantity)
                    ps.setBigDecimal(10, rev.recognizedRevenue)
                    ps.setString(11, rev.currency)
                    ps.setString(12, rev.sourceModule)
                    ps.setString(13, rev.sourceEntityType)
                    ps.setString(14, rev.sourceEntityId)
                    ps.setString(15, rev.sourceTransactionId)
                    ps.setString(16, rev.provenanceFingerprint)
                    ps.addBatch()
                }
                ps.executeBatch()
            }
        }
    }

    override suspend fun getRevenueAttributions(tenantId: String, projectId: String, customerId: String): List<CustomerRevenueAttribution> {
        return transactionManager.inTransaction(TenantContext(projectId)) { tx ->
            val sql = "SELECT * FROM customer_profitability_revenue_attributions WHERE tenant_id = ? AND project_id = ? AND customer_id = ?"
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, projectId)
                ps.setString(3, customerId)
                val rs = ps.executeQuery()
                val list = mutableListOf<CustomerRevenueAttribution>()
                while (rs.next()) {
                    list.add(
                        CustomerRevenueAttribution(
                            revenueAttributionId = rs.getString("revenue_attribution_id"),
                            tenantId = rs.getString("tenant_id"),
                            projectId = rs.getString("project_id"),
                            customerId = rs.getString("customer_id"),
                            orderId = rs.getString("order_id"),
                            invoiceId = rs.getString("invoice_id"),
                            invoiceLineId = rs.getString("invoice_line_id"),
                            productId = rs.getString("product_id"),
                            quantity = rs.getInt("quantity"),
                            recognizedRevenue = rs.getBigDecimal("recognized_revenue") ?: BigDecimal.ZERO,
                            currency = rs.getString("currency") ?: "BDT",
                            sourceModule = rs.getString("source_module"),
                            sourceEntityType = rs.getString("source_entity_type"),
                            sourceEntityId = rs.getString("source_entity_id"),
                            sourceTransactionId = rs.getString("source_transaction_id"),
                            provenanceFingerprint = rs.getString("provenance_fingerprint") ?: ""
                        )
                    )
                }
                list
            }
        }
    }

    override suspend fun saveCostAttributions(attributions: List<CustomerCostAttribution>) {
        if (attributions.isEmpty()) return
        transactionManager.inTransaction(TenantContext(attributions.first().projectId)) { tx ->
            val sql = """
                INSERT INTO customer_profitability_cost_attributions (
                    cost_attribution_id, tenant_id, project_id, customer_id, order_id,
                    job_id, product_id, component_type, directness, is_variable_cost,
                    attributed_amount, allocation_basis, numerator, denominator,
                    allocation_ratio, priority, source_module, source_entity_type,
                    source_entity_id, source_transaction_id, provenance_fingerprint
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (cost_attribution_id) DO NOTHING
            """.trimIndent()

            tx.connection.prepareStatement(sql).use { ps ->
                for (cost in attributions) {
                    ps.setString(1, cost.costAttributionId)
                    ps.setString(2, cost.tenantId)
                    ps.setString(3, cost.projectId)
                    ps.setString(4, cost.customerId)
                    ps.setString(5, cost.orderId)
                    ps.setString(6, cost.jobId)
                    ps.setString(7, cost.productId)
                    ps.setString(8, cost.componentType.name)
                    ps.setString(9, cost.directness.name)
                    ps.setBoolean(10, cost.isVariableCost)
                    ps.setBigDecimal(11, cost.attributedAmount)
                    ps.setString(12, cost.allocationBasis)
                    ps.setBigDecimal(13, cost.numerator)
                    ps.setBigDecimal(14, cost.denominator)
                    ps.setBigDecimal(15, cost.allocationRatio)
                    ps.setString(16, cost.priority.name)
                    ps.setString(17, cost.sourceModule)
                    ps.setString(18, cost.sourceEntityType)
                    ps.setString(19, cost.sourceEntityId)
                    ps.setString(20, cost.sourceTransactionId)
                    ps.setString(21, cost.provenanceFingerprint)
                    ps.addBatch()
                }
                ps.executeBatch()
            }
        }
    }

    override suspend fun getCostAttributions(tenantId: String, projectId: String, customerId: String): List<CustomerCostAttribution> {
        return transactionManager.inTransaction(TenantContext(projectId)) { tx ->
            val sql = "SELECT * FROM customer_profitability_cost_attributions WHERE tenant_id = ? AND project_id = ? AND customer_id = ?"
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, projectId)
                ps.setString(3, customerId)
                val rs = ps.executeQuery()
                val list = mutableListOf<CustomerCostAttribution>()
                while (rs.next()) {
                    list.add(
                        CustomerCostAttribution(
                            costAttributionId = rs.getString("cost_attribution_id"),
                            tenantId = rs.getString("tenant_id"),
                            projectId = rs.getString("project_id"),
                            customerId = rs.getString("customer_id"),
                            orderId = rs.getString("order_id"),
                            jobId = rs.getString("job_id"),
                            productId = rs.getString("product_id"),
                            componentType = JobCostComponentType.valueOf(rs.getString("component_type")),
                            directness = CostDirectness.valueOf(rs.getString("directness") ?: "DIRECT"),
                            isVariableCost = rs.getBoolean("is_variable_cost"),
                            attributedAmount = rs.getBigDecimal("attributed_amount") ?: BigDecimal.ZERO,
                            allocationBasis = rs.getString("allocation_basis") ?: "DIRECT",
                            numerator = rs.getBigDecimal("numerator"),
                            denominator = rs.getBigDecimal("denominator"),
                            allocationRatio = rs.getBigDecimal("allocation_ratio"),
                            priority = CustomerCostAttributionPriority.valueOf(rs.getString("priority") ?: "PRIORITY_1_DIRECT_CUSTOMER"),
                            sourceModule = rs.getString("source_module"),
                            sourceEntityType = rs.getString("source_entity_type"),
                            sourceEntityId = rs.getString("source_entity_id"),
                            sourceTransactionId = rs.getString("source_transaction_id"),
                            provenanceFingerprint = rs.getString("provenance_fingerprint") ?: ""
                        )
                    )
                }
                list
            }
        }
    }

    override suspend fun saveReconciliationEvent(event: CustomerProfitabilityReconciliationEvent) {
        transactionManager.inTransaction(TenantContext(event.projectId)) { tx ->
            val sql = """
                INSERT INTO customer_profitability_reconciliation_events (
                    reconciliation_id, snapshot_id, tenant_id, project_id, customer_id,
                    is_reconciled, revenue_reconciled, cost_reconciled, profit_reconciled,
                    contribution_reconciled, expected_revenue, actual_revenue, expected_cost,
                    actual_cost, expected_gross_profit, actual_gross_profit, discrepancies_json,
                    checked_at, checked_by
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()

            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, event.reconciliationId)
                ps.setString(2, event.snapshotId)
                ps.setString(3, event.tenantId)
                ps.setString(4, event.projectId)
                ps.setString(5, event.customerId)
                ps.setBoolean(6, event.isReconciled)
                ps.setBoolean(7, event.revenueReconciled)
                ps.setBoolean(8, event.costReconciled)
                ps.setBoolean(9, event.profitReconciled)
                ps.setBoolean(10, event.contributionReconciled)
                ps.setBigDecimal(11, event.expectedRevenue)
                ps.setBigDecimal(12, event.actualRevenue)
                ps.setBigDecimal(13, event.expectedCost)
                ps.setBigDecimal(14, event.actualCost)
                ps.setBigDecimal(15, event.expectedGrossProfit)
                ps.setBigDecimal(16, event.actualGrossProfit)
                ps.setString(17, event.discrepancies.joinToString("||"))
                ps.setLong(18, event.checkedAt)
                ps.setString(19, event.checkedBy)
                ps.executeUpdate()
            }
        }
    }

    override suspend fun getReconciliationEvents(tenantId: String, projectId: String, customerId: String): List<CustomerProfitabilityReconciliationEvent> {
        return transactionManager.inTransaction(TenantContext(projectId)) { tx ->
            val sql = "SELECT * FROM customer_profitability_reconciliation_events WHERE tenant_id = ? AND project_id = ? AND customer_id = ? ORDER BY checked_at DESC"
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, projectId)
                ps.setString(3, customerId)
                val rs = ps.executeQuery()
                val list = mutableListOf<CustomerProfitabilityReconciliationEvent>()
                while (rs.next()) {
                    val disc = rs.getString("discrepancies_json") ?: ""
                    list.add(
                        CustomerProfitabilityReconciliationEvent(
                            reconciliationId = rs.getString("reconciliation_id"),
                            snapshotId = rs.getString("snapshot_id"),
                            tenantId = rs.getString("tenant_id"),
                            projectId = rs.getString("project_id"),
                            customerId = rs.getString("customer_id"),
                            isReconciled = rs.getBoolean("is_reconciled"),
                            revenueReconciled = rs.getBoolean("revenue_reconciled"),
                            costReconciled = rs.getBoolean("cost_reconciled"),
                            profitReconciled = rs.getBoolean("profit_reconciled"),
                            contributionReconciled = rs.getBoolean("contribution_reconciled"),
                            expectedRevenue = rs.getBigDecimal("expected_revenue") ?: BigDecimal.ZERO,
                            actualRevenue = rs.getBigDecimal("actual_revenue") ?: BigDecimal.ZERO,
                            expectedCost = rs.getBigDecimal("expected_cost") ?: BigDecimal.ZERO,
                            actualCost = rs.getBigDecimal("actual_cost") ?: BigDecimal.ZERO,
                            expectedGrossProfit = rs.getBigDecimal("expected_gross_profit") ?: BigDecimal.ZERO,
                            actualGrossProfit = rs.getBigDecimal("actual_gross_profit") ?: BigDecimal.ZERO,
                            discrepancies = if (disc.isBlank()) emptyList() else disc.split("||"),
                            checkedAt = rs.getLong("checked_at"),
                            checkedBy = rs.getString("checked_by")
                        )
                    )
                }
                list
            }
        }
    }

    override suspend fun recordAuditEvent(event: CustomerProfitabilityAuditEvent) {
        transactionManager.inTransaction(TenantContext(event.projectId)) { tx ->
            val sql = """
                INSERT INTO customer_profitability_audit_events (
                    event_id, tenant_id, project_id, customer_id, snapshot_id,
                    action, actor, actor_role, outcome, details, correlation_id, timestamp
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()

            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, event.eventId)
                ps.setString(2, event.tenantId)
                ps.setString(3, event.projectId)
                ps.setString(4, event.customerId)
                ps.setString(5, event.snapshotId)
                ps.setString(6, event.action)
                ps.setString(7, event.actor)
                ps.setString(8, event.actorRole)
                ps.setString(9, event.outcome)
                ps.setString(10, event.details)
                ps.setString(11, event.correlationId)
                ps.setLong(12, event.timestamp)
                ps.executeUpdate()
            }
        }
    }

    override suspend fun getAuditEvents(tenantId: String, projectId: String, customerId: String): List<CustomerProfitabilityAuditEvent> {
        return transactionManager.inTransaction(TenantContext(projectId)) { tx ->
            val sql = "SELECT * FROM customer_profitability_audit_events WHERE tenant_id = ? AND project_id = ? AND customer_id = ? ORDER BY timestamp DESC"
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, projectId)
                ps.setString(3, customerId)
                val rs = ps.executeQuery()
                val list = mutableListOf<CustomerProfitabilityAuditEvent>()
                while (rs.next()) {
                    list.add(
                        CustomerProfitabilityAuditEvent(
                            eventId = rs.getString("event_id"),
                            tenantId = rs.getString("tenant_id"),
                            projectId = rs.getString("project_id"),
                            customerId = rs.getString("customer_id"),
                            snapshotId = rs.getString("snapshot_id"),
                            action = rs.getString("action"),
                            actor = rs.getString("actor"),
                            actorRole = rs.getString("actor_role"),
                            outcome = rs.getString("outcome"),
                            details = rs.getString("details") ?: "",
                            correlationId = rs.getString("correlation_id"),
                            timestamp = rs.getLong("timestamp")
                        )
                    )
                }
                list
            }
        }
    }

    override suspend fun saveUnattributedItems(items: List<UnattributedProfitabilityItem>) {
        if (items.isEmpty()) return
        transactionManager.inTransaction(TenantContext(items.first().projectId)) { tx ->
            val sql = """
                INSERT INTO customer_profitability_unattributed_items (
                    item_id, tenant_id, project_id, item_type, amount, source_module, source_entity_id, reason, timestamp
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (item_id) DO NOTHING
            """.trimIndent()

            tx.connection.prepareStatement(sql).use { ps ->
                for (item in items) {
                    ps.setString(1, item.itemId)
                    ps.setString(2, item.tenantId)
                    ps.setString(3, item.projectId)
                    ps.setString(4, item.itemType)
                    ps.setBigDecimal(5, item.amount)
                    ps.setString(6, item.sourceModule)
                    ps.setString(7, item.sourceEntityId)
                    ps.setString(8, item.reason)
                    ps.setLong(9, item.timestamp)
                    ps.addBatch()
                }
                ps.executeBatch()
            }
        }
    }

    override suspend fun getUnattributedItems(tenantId: String, projectId: String): List<UnattributedProfitabilityItem> {
        return transactionManager.inTransaction(TenantContext(projectId)) { tx ->
            val sql = "SELECT * FROM customer_profitability_unattributed_items WHERE tenant_id = ? AND project_id = ? ORDER BY timestamp DESC"
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, projectId)
                val rs = ps.executeQuery()
                val list = mutableListOf<UnattributedProfitabilityItem>()
                while (rs.next()) {
                    list.add(
                        UnattributedProfitabilityItem(
                            itemId = rs.getString("item_id"),
                            tenantId = rs.getString("tenant_id"),
                            projectId = rs.getString("project_id"),
                            itemType = rs.getString("item_type"),
                            amount = rs.getBigDecimal("amount") ?: BigDecimal.ZERO,
                            sourceModule = rs.getString("source_module"),
                            sourceEntityId = rs.getString("source_entity_id"),
                            reason = rs.getString("reason") ?: "",
                            timestamp = rs.getLong("timestamp")
                        )
                    )
                }
                list
            }
        }
    }

    private fun mapSnapshot(rs: ResultSet): CustomerProfitabilitySnapshot {
        val startLong = rs.getLong("period_start")
        val periodStart = if (rs.wasNull()) null else startLong
        val endLong = rs.getLong("period_end")
        val periodEnd = if (rs.wasNull()) null else endLong

        return CustomerProfitabilitySnapshot(
            snapshotId = rs.getString("snapshot_id"),
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            customerId = rs.getString("customer_id"),
            customerName = rs.getString("customer_name"),
            customerCode = rs.getString("customer_code"),
            periodType = ProfitabilityPeriodType.valueOf(rs.getString("period_type")),
            periodStart = periodStart,
            periodEnd = periodEnd,
            recognizedRevenue = rs.getBigDecimal("recognized_revenue") ?: BigDecimal.ZERO,
            totalActualCost = rs.getBigDecimal("total_actual_cost") ?: BigDecimal.ZERO,
            grossProfit = rs.getBigDecimal("gross_profit") ?: BigDecimal.ZERO,
            grossMarginPercentage = rs.getBigDecimal("gross_margin_percentage"),
            contributionMetrics = CustomerContributionMetrics(
                attributableVariableCost = rs.getBigDecimal("attributable_variable_cost") ?: BigDecimal.ZERO,
                attributableFixedCost = rs.getBigDecimal("attributable_fixed_cost") ?: BigDecimal.ZERO,
                contributionAmount = rs.getBigDecimal("contribution_amount") ?: BigDecimal.ZERO,
                contributionMarginPercentage = rs.getBigDecimal("contribution_margin_percentage"),
                costToRevenuePercentage = rs.getBigDecimal("cost_to_revenue_percentage")
            ),
            operationalMetrics = CustomerOperationalMetrics(
                orderCount = rs.getInt("order_count"),
                jobCount = rs.getInt("job_count"),
                productCount = rs.getInt("product_count"),
                totalQuantitySold = rs.getInt("total_quantity_sold"),
                averageOrderValue = rs.getBigDecimal("average_order_value"),
                averageJobValue = rs.getBigDecimal("average_job_value"),
                averageRevenuePerUnit = rs.getBigDecimal("average_revenue_per_unit"),
                averageCostPerUnit = rs.getBigDecimal("average_cost_per_unit"),
                averageProfitPerUnit = rs.getBigDecimal("average_profit_per_unit"),
                unitEconomicsStatus = rs.getString("unit_economics_status") ?: "AVAILABLE"
            ),
            profitabilityClassification = CustomerProfitabilityClassification.valueOf(rs.getString("profitability_classification")),
            trend = CustomerProfitabilityTrend.valueOf(rs.getString("trend")),
            concentrationRisk = CustomerConcentrationRisk.valueOf(rs.getString("concentration_risk")),
            isLossMaking = rs.getBoolean("is_loss_making"),
            isLowMargin = rs.getBoolean("is_low_margin"),
            sourceIntegrityStatus = ProductSourceIntegrityStatus.valueOf(rs.getString("source_integrity_status")),
            isReconciled = rs.getBoolean("is_reconciled"),
            reconciliationDiscrepancy = rs.getBigDecimal("reconciliation_discrepancy") ?: BigDecimal.ZERO,
            calculationVersion = rs.getString("calculation_version"),
            generatedAt = rs.getLong("generated_at"),
            generatedBy = rs.getString("generated_by"),
            integrityHash = rs.getString("integrity_hash")
        )
    }

    private fun loadComponents(conn: java.sql.Connection, tenantId: String, projectId: String, snapshotId: String): List<CustomerCostBreakdownItem> {
        val sql = "SELECT * FROM customer_profitability_components WHERE tenant_id = ? AND project_id = ? AND snapshot_id = ?"
        conn.prepareStatement(sql).use { ps ->
            ps.setString(1, tenantId)
            ps.setString(2, projectId)
            ps.setString(3, snapshotId)
            val rs = ps.executeQuery()
            val list = mutableListOf<CustomerCostBreakdownItem>()
            while (rs.next()) {
                val fps = rs.getString("provenance_fingerprints") ?: ""
                list.add(
                    CustomerCostBreakdownItem(
                        componentType = JobCostComponentType.valueOf(rs.getString("component_type")),
                        amount = rs.getBigDecimal("amount") ?: BigDecimal.ZERO,
                        percentageOfTotalCost = rs.getBigDecimal("percentage_of_total_cost") ?: BigDecimal.ZERO,
                        isVariableCost = rs.getBoolean("is_variable_cost"),
                        sourceCount = rs.getInt("source_count"),
                        allocationBasis = rs.getString("allocation_basis") ?: "DIRECT",
                        provenanceFingerprints = if (fps.isBlank()) emptyList() else fps.split(",")
                    )
                )
            }
            return list
        }
    }
}
