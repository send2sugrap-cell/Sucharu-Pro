package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.datasource.profitability.ProductProfitabilityDataSource
import com.sucharu.sucharupro.domain.model.profitability.*
import java.math.BigDecimal
import java.sql.ResultSet

/**
 * PostgreSQL Implementation of ProductProfitabilityDataSource with Row-Level Security enforcement.
 */
class PostgresProductProfitabilityDataSource(
    private val transactionManager: TransactionManager
) : ProductProfitabilityDataSource {

    override suspend fun saveSnapshot(snapshot: ProductProfitabilitySnapshot) {
        transactionManager.inTransaction(TenantContext(snapshot.projectId)) { tx ->
            val sqlSnapshot = """
                INSERT INTO product_profitability_snapshots (
                    snapshot_id, tenant_id, project_id, product_id, sku, product_name,
                    edition_id, version_id, period_id, customer_id, total_quantity,
                    recognized_revenue, total_actual_cost, gross_profit, gross_margin_percentage,
                    unit_revenue, unit_actual_cost, unit_gross_profit, unit_metric_status,
                    profitability_classification, variance_classification,
                    baseline_cost, cost_variance, cost_variance_percentage,
                    source_integrity_status, is_reconciled, reconciliation_discrepancy,
                    calculation_version, generated_at, generated_by, integrity_hash
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (snapshot_id) DO UPDATE SET
                    total_quantity = EXCLUDED.total_quantity,
                    recognized_revenue = EXCLUDED.recognized_revenue,
                    total_actual_cost = EXCLUDED.total_actual_cost,
                    gross_profit = EXCLUDED.gross_profit,
                    gross_margin_percentage = EXCLUDED.gross_margin_percentage,
                    unit_revenue = EXCLUDED.unit_revenue,
                    unit_actual_cost = EXCLUDED.unit_actual_cost,
                    unit_gross_profit = EXCLUDED.unit_gross_profit,
                    unit_metric_status = EXCLUDED.unit_metric_status,
                    profitability_classification = EXCLUDED.profitability_classification,
                    variance_classification = EXCLUDED.variance_classification,
                    baseline_cost = EXCLUDED.baseline_cost,
                    cost_variance = EXCLUDED.cost_variance,
                    cost_variance_percentage = EXCLUDED.cost_variance_percentage,
                    source_integrity_status = EXCLUDED.source_integrity_status,
                    is_reconciled = EXCLUDED.is_reconciled,
                    reconciliation_discrepancy = EXCLUDED.reconciliation_discrepancy,
                    generated_at = EXCLUDED.generated_at,
                    integrity_hash = EXCLUDED.integrity_hash
            """.trimIndent()

            tx.connection.prepareStatement(sqlSnapshot).use { ps ->
                ps.setString(1, snapshot.snapshotId)
                ps.setString(2, snapshot.tenantId)
                ps.setString(3, snapshot.projectId)
                ps.setString(4, snapshot.productId)
                ps.setString(5, snapshot.sku)
                ps.setString(6, snapshot.productName)
                ps.setString(7, snapshot.editionId)
                ps.setString(8, snapshot.versionId)
                ps.setString(9, snapshot.periodId)
                ps.setString(10, snapshot.customerId)
                ps.setInt(11, snapshot.totalQuantity)
                ps.setBigDecimal(12, snapshot.recognizedRevenue)
                ps.setBigDecimal(13, snapshot.totalActualCost)
                ps.setBigDecimal(14, snapshot.grossProfit)
                ps.setBigDecimal(15, snapshot.grossMarginPercentage)
                ps.setBigDecimal(16, snapshot.unitEconomics.unitRevenue)
                ps.setBigDecimal(17, snapshot.unitEconomics.unitActualCost)
                ps.setBigDecimal(18, snapshot.unitEconomics.unitGrossProfit)
                ps.setString(19, snapshot.unitEconomics.unitMetricStatus)
                ps.setString(20, snapshot.profitabilityClassification.name)
                ps.setString(21, snapshot.varianceClassification.name)
                ps.setBigDecimal(22, snapshot.baselineCost)
                ps.setBigDecimal(23, snapshot.costVariance)
                ps.setBigDecimal(24, snapshot.costVariancePercentage)
                ps.setString(25, snapshot.sourceIntegrityStatus.name)
                ps.setBoolean(26, snapshot.isReconciled)
                ps.setBigDecimal(27, snapshot.reconciliationDiscrepancy)
                ps.setString(28, snapshot.calculationVersion)
                ps.setLong(29, snapshot.generatedAt)
                ps.setString(30, snapshot.generatedBy)
                ps.setString(31, snapshot.integrityHash)
                ps.executeUpdate()
            }

            // Save Cost Breakdown Components
            if (snapshot.costBreakdown.isNotEmpty()) {
                val sqlComp = """
                    INSERT INTO product_profitability_components (
                        snapshot_id, tenant_id, project_id, product_id, component_type,
                        amount, unit_amount, percentage_of_total_cost, source_count,
                        allocation_basis, provenance_fingerprints
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (snapshot_id, component_type) DO UPDATE SET
                        amount = EXCLUDED.amount,
                        unit_amount = EXCLUDED.unit_amount,
                        percentage_of_total_cost = EXCLUDED.percentage_of_total_cost,
                        source_count = EXCLUDED.source_count,
                        allocation_basis = EXCLUDED.allocation_basis,
                        provenance_fingerprints = EXCLUDED.provenance_fingerprints
                """.trimIndent()

                tx.connection.prepareStatement(sqlComp).use { ps ->
                    for (comp in snapshot.costBreakdown) {
                        ps.setString(1, snapshot.snapshotId)
                        ps.setString(2, snapshot.tenantId)
                        ps.setString(3, snapshot.projectId)
                        ps.setString(4, snapshot.productId)
                        ps.setString(5, comp.componentType.name)
                        ps.setBigDecimal(6, comp.amount)
                        ps.setBigDecimal(7, comp.unitAmount)
                        ps.setBigDecimal(8, comp.percentageOfTotalCost)
                        ps.setInt(9, comp.sourceCount)
                        ps.setString(10, comp.allocationBasis.name)
                        ps.setString(11, comp.provenanceFingerprints.joinToString(","))
                        ps.addBatch()
                    }
                    ps.executeBatch()
                }
            }
        }
    }

    override suspend fun getSnapshotById(tenantId: String, projectId: String, snapshotId: String): ProductProfitabilitySnapshot? {
        return transactionManager.inTransaction(TenantContext(projectId)) { tx ->
            val sql = "SELECT * FROM product_profitability_snapshots WHERE tenant_id = ? AND project_id = ? AND snapshot_id = ?"
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, projectId)
                ps.setString(3, snapshotId)
                val rs = ps.executeQuery()
                if (rs.next()) {
                    val snap = mapSnapshot(rs)
                    val components = loadComponents(tx.connection, tenantId, projectId, snap.snapshotId)
                    snap.copy(costBreakdown = components)
                } else null
            }
        }
    }

    override suspend fun getLatestSnapshotByProduct(tenantId: String, projectId: String, productId: String): ProductProfitabilitySnapshot? {
        return transactionManager.inTransaction(TenantContext(projectId)) { tx ->
            val sql = "SELECT * FROM product_profitability_snapshots WHERE tenant_id = ? AND project_id = ? AND product_id = ? ORDER BY generated_at DESC LIMIT 1"
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, projectId)
                ps.setString(3, productId)
                val rs = ps.executeQuery()
                if (rs.next()) {
                    val snap = mapSnapshot(rs)
                    val components = loadComponents(tx.connection, tenantId, projectId, snap.snapshotId)
                    snap.copy(costBreakdown = components)
                } else null
            }
        }
    }

    override suspend fun listSnapshots(tenantId: String, projectId: String, filter: ProductProfitabilityFilter): List<ProductProfitabilitySnapshot> {
        return transactionManager.inTransaction(TenantContext(projectId)) { tx ->
            val conditions = mutableListOf("tenant_id = ?", "project_id = ?")
            val params = mutableListOf<Any>(tenantId, projectId)

            if (filter.productId != null) {
                conditions.add("product_id = ?")
                params.add(filter.productId)
            }
            if (filter.sku != null) {
                conditions.add("sku = ?")
                params.add(filter.sku)
            }
            if (filter.classification != null) {
                conditions.add("profitability_classification = ?")
                params.add(filter.classification.name)
            }

            val sql = "SELECT * FROM product_profitability_snapshots WHERE ${conditions.joinToString(" AND ")} ORDER BY generated_at DESC LIMIT ? OFFSET ?"
            tx.connection.prepareStatement(sql).use { ps ->
                var idx = 1
                for (param in params) {
                    when (param) {
                        is String -> ps.setString(idx++, param)
                        is Int -> ps.setInt(idx++, param)
                    }
                }
                ps.setInt(idx++, filter.limit)
                ps.setInt(idx, filter.offset)

                val rs = ps.executeQuery()
                val list = mutableListOf<ProductProfitabilitySnapshot>()
                while (rs.next()) {
                    list.add(mapSnapshot(rs))
                }
                list
            }
        }
    }

    override suspend fun saveRevenueAttributions(attributions: List<ProductRevenueAttribution>) {
        if (attributions.isEmpty()) return
        transactionManager.inTransaction(TenantContext(attributions.first().projectId)) { tx ->
            val sql = """
                INSERT INTO product_profitability_revenue_attributions (
                    revenue_attribution_id, tenant_id, project_id, product_id, sku,
                    edition_id, version_id, invoice_id, order_id, customer_id,
                    quantity, recognized_revenue, attribution_ratio, source_module,
                    source_entity_type, source_entity_id, source_transaction_id,
                    attribution_method, provenance_fingerprint
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (revenue_attribution_id) DO NOTHING
            """.trimIndent()

            tx.connection.prepareStatement(sql).use { ps ->
                for (rev in attributions) {
                    ps.setString(1, rev.revenueAttributionId)
                    ps.setString(2, rev.tenantId)
                    ps.setString(3, rev.projectId)
                    ps.setString(4, rev.productId)
                    ps.setString(5, rev.sku)
                    ps.setString(6, rev.editionId)
                    ps.setString(7, rev.versionId)
                    ps.setString(8, rev.invoiceId)
                    ps.setString(9, rev.orderId)
                    ps.setString(10, rev.customerId)
                    ps.setInt(11, rev.quantity)
                    ps.setBigDecimal(12, rev.recognizedRevenue)
                    ps.setBigDecimal(13, rev.attributionRatio)
                    ps.setString(14, rev.sourceModule)
                    ps.setString(15, rev.sourceEntityType)
                    ps.setString(16, rev.sourceEntityId)
                    ps.setString(17, rev.sourceTransactionId)
                    ps.setString(18, rev.attributionMethod)
                    ps.setString(19, rev.provenanceFingerprint)
                    ps.addBatch()
                }
                ps.executeBatch()
            }
        }
    }

    override suspend fun getRevenueAttributions(tenantId: String, projectId: String, productId: String): List<ProductRevenueAttribution> {
        return transactionManager.inTransaction(TenantContext(projectId)) { tx ->
            val sql = "SELECT * FROM product_profitability_revenue_attributions WHERE tenant_id = ? AND project_id = ? AND product_id = ?"
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, projectId)
                ps.setString(3, productId)
                val rs = ps.executeQuery()
                val list = mutableListOf<ProductRevenueAttribution>()
                while (rs.next()) {
                    list.add(
                        ProductRevenueAttribution(
                            revenueAttributionId = rs.getString("revenue_attribution_id"),
                            tenantId = rs.getString("tenant_id"),
                            projectId = rs.getString("project_id"),
                            productId = rs.getString("product_id"),
                            sku = rs.getString("sku"),
                            editionId = rs.getString("edition_id"),
                            versionId = rs.getString("version_id"),
                            invoiceId = rs.getString("invoice_id"),
                            orderId = rs.getString("order_id"),
                            customerId = rs.getString("customer_id"),
                            quantity = rs.getInt("quantity"),
                            recognizedRevenue = rs.getBigDecimal("recognized_revenue") ?: BigDecimal.ZERO,
                            attributionRatio = rs.getBigDecimal("attribution_ratio") ?: BigDecimal.ONE,
                            sourceModule = rs.getString("source_module"),
                            sourceEntityType = rs.getString("source_entity_type"),
                            sourceEntityId = rs.getString("source_entity_id"),
                            sourceTransactionId = rs.getString("source_transaction_id"),
                            attributionMethod = rs.getString("attribution_method"),
                            provenanceFingerprint = rs.getString("provenance_fingerprint") ?: ""
                        )
                    )
                }
                list
            }
        }
    }

    override suspend fun saveCostAttributions(attributions: List<ProductCostAttribution>) {
        if (attributions.isEmpty()) return
        transactionManager.inTransaction(TenantContext(attributions.first().projectId)) { tx ->
            val sql = """
                INSERT INTO product_profitability_cost_attributions (
                    cost_attribution_id, tenant_id, project_id, product_id, sku,
                    edition_id, version_id, job_id, component_type, directness,
                    attributed_amount, allocation_basis, numerator, denominator,
                    allocation_ratio, source_module, source_entity_type, source_entity_id,
                    source_transaction_id, provenance_fingerprint
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (cost_attribution_id) DO NOTHING
            """.trimIndent()

            tx.connection.prepareStatement(sql).use { ps ->
                for (cost in attributions) {
                    ps.setString(1, cost.costAttributionId)
                    ps.setString(2, cost.tenantId)
                    ps.setString(3, cost.projectId)
                    ps.setString(4, cost.productId)
                    ps.setString(5, cost.sku)
                    ps.setString(6, cost.editionId)
                    ps.setString(7, cost.versionId)
                    ps.setString(8, cost.jobId)
                    ps.setString(9, cost.componentType.name)
                    ps.setString(10, cost.directness.name)
                    ps.setBigDecimal(11, cost.attributedAmount)
                    ps.setString(12, cost.allocationBasis.name)
                    ps.setBigDecimal(13, cost.numerator)
                    ps.setBigDecimal(14, cost.denominator)
                    ps.setBigDecimal(15, cost.allocationRatio)
                    ps.setString(16, cost.sourceModule)
                    ps.setString(17, cost.sourceEntityType)
                    ps.setString(18, cost.sourceEntityId)
                    ps.setString(19, cost.sourceTransactionId)
                    ps.setString(20, cost.provenanceFingerprint)
                    ps.addBatch()
                }
                ps.executeBatch()
            }
        }
    }

    override suspend fun getCostAttributions(tenantId: String, projectId: String, productId: String): List<ProductCostAttribution> {
        return transactionManager.inTransaction(TenantContext(projectId)) { tx ->
            val sql = "SELECT * FROM product_profitability_cost_attributions WHERE tenant_id = ? AND project_id = ? AND product_id = ?"
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, projectId)
                ps.setString(3, productId)
                val rs = ps.executeQuery()
                val list = mutableListOf<ProductCostAttribution>()
                while (rs.next()) {
                    list.add(
                        ProductCostAttribution(
                            costAttributionId = rs.getString("cost_attribution_id"),
                            tenantId = rs.getString("tenant_id"),
                            projectId = rs.getString("project_id"),
                            productId = rs.getString("product_id"),
                            sku = rs.getString("sku"),
                            editionId = rs.getString("edition_id"),
                            versionId = rs.getString("version_id"),
                            jobId = rs.getString("job_id"),
                            componentType = JobCostComponentType.valueOf(rs.getString("component_type")),
                            directness = CostDirectness.valueOf(rs.getString("directness") ?: "DIRECT"),
                            attributedAmount = rs.getBigDecimal("attributed_amount") ?: BigDecimal.ZERO,
                            allocationBasis = ProductCostAllocationBasis.valueOf(rs.getString("allocation_basis") ?: "DIRECT"),
                            numerator = rs.getBigDecimal("numerator"),
                            denominator = rs.getBigDecimal("denominator"),
                            allocationRatio = rs.getBigDecimal("allocation_ratio"),
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

    override suspend fun saveReconciliationEvent(event: ProductProfitabilityReconciliationEvent) {
        transactionManager.inTransaction(TenantContext(event.projectId)) { tx ->
            val sql = """
                INSERT INTO product_profitability_reconciliation_events (
                    reconciliation_id, snapshot_id, tenant_id, project_id, product_id,
                    is_reconciled, revenue_reconciled, cost_reconciled, unit_economics_reconciled,
                    expected_revenue, actual_revenue, expected_cost, actual_cost,
                    gross_profit_discrepancy, discrepancies_json, checked_at, checked_by
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()

            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, event.reconciliationId)
                ps.setString(2, event.snapshotId)
                ps.setString(3, event.tenantId)
                ps.setString(4, event.projectId)
                ps.setString(5, event.productId)
                ps.setBoolean(6, event.isReconciled)
                ps.setBoolean(7, event.revenueReconciled)
                ps.setBoolean(8, event.costReconciled)
                ps.setBoolean(9, event.unitEconomicsReconciled)
                ps.setBigDecimal(10, event.expectedRevenue)
                ps.setBigDecimal(11, event.actualRevenue)
                ps.setBigDecimal(12, event.expectedCost)
                ps.setBigDecimal(13, event.actualCost)
                ps.setBigDecimal(14, event.grossProfitDiscrepancy)
                ps.setString(15, event.discrepancies.joinToString("||"))
                ps.setLong(16, event.checkedAt)
                ps.setString(17, event.checkedBy)
                ps.executeUpdate()
            }
        }
    }

    override suspend fun getReconciliationEvents(tenantId: String, projectId: String, productId: String): List<ProductProfitabilityReconciliationEvent> {
        return transactionManager.inTransaction(TenantContext(projectId)) { tx ->
            val sql = "SELECT * FROM product_profitability_reconciliation_events WHERE tenant_id = ? AND project_id = ? AND product_id = ? ORDER BY checked_at DESC"
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, projectId)
                ps.setString(3, productId)
                val rs = ps.executeQuery()
                val list = mutableListOf<ProductProfitabilityReconciliationEvent>()
                while (rs.next()) {
                    val discStr = rs.getString("discrepancies_json") ?: ""
                    val discList = if (discStr.isBlank()) emptyList() else discStr.split("||")
                    list.add(
                        ProductProfitabilityReconciliationEvent(
                            reconciliationId = rs.getString("reconciliation_id"),
                            snapshotId = rs.getString("snapshot_id"),
                            tenantId = rs.getString("tenant_id"),
                            projectId = rs.getString("project_id"),
                            productId = rs.getString("product_id"),
                            isReconciled = rs.getBoolean("is_reconciled"),
                            revenueReconciled = rs.getBoolean("revenue_reconciled"),
                            costReconciled = rs.getBoolean("cost_reconciled"),
                            unitEconomicsReconciled = rs.getBoolean("unit_economics_reconciled"),
                            expectedRevenue = rs.getBigDecimal("expected_revenue") ?: BigDecimal.ZERO,
                            actualRevenue = rs.getBigDecimal("actual_revenue") ?: BigDecimal.ZERO,
                            expectedCost = rs.getBigDecimal("expected_cost") ?: BigDecimal.ZERO,
                            actualCost = rs.getBigDecimal("actual_cost") ?: BigDecimal.ZERO,
                            grossProfitDiscrepancy = rs.getBigDecimal("gross_profit_discrepancy") ?: BigDecimal.ZERO,
                            discrepancies = discList,
                            checkedAt = rs.getLong("checked_at"),
                            checkedBy = rs.getString("checked_by")
                        )
                    )
                }
                list
            }
        }
    }

    override suspend fun recordAuditEvent(event: ProductProfitabilityAuditEvent) {
        transactionManager.inTransaction(TenantContext(event.projectId)) { tx ->
            val sql = """
                INSERT INTO product_profitability_audit_events (
                    event_id, tenant_id, project_id, product_id, snapshot_id,
                    action, actor, actor_role, outcome, details, correlation_id, timestamp
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()

            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, event.eventId)
                ps.setString(2, event.tenantId)
                ps.setString(3, event.projectId)
                ps.setString(4, event.productId)
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

    override suspend fun getAuditEvents(tenantId: String, projectId: String, productId: String): List<ProductProfitabilityAuditEvent> {
        return transactionManager.inTransaction(TenantContext(projectId)) { tx ->
            val sql = "SELECT * FROM product_profitability_audit_events WHERE tenant_id = ? AND project_id = ? AND product_id = ? ORDER BY timestamp DESC"
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, projectId)
                ps.setString(3, productId)
                val rs = ps.executeQuery()
                val list = mutableListOf<ProductProfitabilityAuditEvent>()
                while (rs.next()) {
                    list.add(
                        ProductProfitabilityAuditEvent(
                            eventId = rs.getString("event_id"),
                            tenantId = rs.getString("tenant_id"),
                            projectId = rs.getString("project_id"),
                            productId = rs.getString("product_id"),
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

    private fun mapSnapshot(rs: ResultSet): ProductProfitabilitySnapshot {
        val qty = rs.getInt("total_quantity")
        val unitRev = rs.getBigDecimal("unit_revenue")
        val unitCost = rs.getBigDecimal("unit_actual_cost")
        val unitGp = rs.getBigDecimal("unit_gross_profit")
        val unitStatus = rs.getString("unit_metric_status") ?: "AVAILABLE"

        return ProductProfitabilitySnapshot(
            snapshotId = rs.getString("snapshot_id"),
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            productId = rs.getString("product_id"),
            sku = rs.getString("sku"),
            productName = rs.getString("product_name"),
            editionId = rs.getString("edition_id"),
            versionId = rs.getString("version_id"),
            periodId = rs.getString("period_id"),
            customerId = rs.getString("customer_id"),
            totalQuantity = qty,
            recognizedRevenue = rs.getBigDecimal("recognized_revenue") ?: BigDecimal.ZERO,
            totalActualCost = rs.getBigDecimal("total_actual_cost") ?: BigDecimal.ZERO,
            grossProfit = rs.getBigDecimal("gross_profit") ?: BigDecimal.ZERO,
            grossMarginPercentage = rs.getBigDecimal("gross_margin_percentage"),
            unitEconomics = ProductUnitEconomics(
                quantity = qty,
                unitRevenue = unitRev,
                unitActualCost = unitCost,
                unitGrossProfit = unitGp,
                unitMetricStatus = unitStatus
            ),
            profitabilityClassification = ProductProfitabilityClassification.valueOf(rs.getString("profitability_classification")),
            varianceClassification = ProductVarianceClassification.valueOf(rs.getString("variance_classification")),
            baselineCost = rs.getBigDecimal("baseline_cost"),
            costVariance = rs.getBigDecimal("cost_variance"),
            costVariancePercentage = rs.getBigDecimal("cost_variance_percentage"),
            sourceIntegrityStatus = ProductSourceIntegrityStatus.valueOf(rs.getString("source_integrity_status")),
            isReconciled = rs.getBoolean("is_reconciled"),
            reconciliationDiscrepancy = rs.getBigDecimal("reconciliation_discrepancy") ?: BigDecimal.ZERO,
            calculationVersion = rs.getString("calculation_version"),
            generatedAt = rs.getLong("generated_at"),
            generatedBy = rs.getString("generated_by"),
            integrityHash = rs.getString("integrity_hash")
        )
    }

    private fun loadComponents(conn: java.sql.Connection, tenantId: String, projectId: String, snapshotId: String): List<ProductCostBreakdownItem> {
        val sql = "SELECT * FROM product_profitability_components WHERE tenant_id = ? AND project_id = ? AND snapshot_id = ?"
        conn.prepareStatement(sql).use { ps ->
            ps.setString(1, tenantId)
            ps.setString(2, projectId)
            ps.setString(3, snapshotId)
            val rs = ps.executeQuery()
            val list = mutableListOf<ProductCostBreakdownItem>()
            while (rs.next()) {
                val fps = rs.getString("provenance_fingerprints") ?: ""
                list.add(
                    ProductCostBreakdownItem(
                        componentType = JobCostComponentType.valueOf(rs.getString("component_type")),
                        amount = rs.getBigDecimal("amount") ?: BigDecimal.ZERO,
                        unitAmount = rs.getBigDecimal("unit_amount"),
                        percentageOfTotalCost = rs.getBigDecimal("percentage_of_total_cost") ?: BigDecimal.ZERO,
                        sourceCount = rs.getInt("source_count"),
                        allocationBasis = ProductCostAllocationBasis.valueOf(rs.getString("allocation_basis") ?: "DIRECT"),
                        provenanceFingerprints = if (fps.isBlank()) emptyList() else fps.split(",")
                    )
                )
            }
            return list
        }
    }
}
