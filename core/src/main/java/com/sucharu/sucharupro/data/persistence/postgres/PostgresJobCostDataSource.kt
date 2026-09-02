package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.datasource.profitability.JobCostDataSource
import com.sucharu.sucharupro.domain.model.profitability.*
import java.math.BigDecimal
import java.sql.Connection
import java.sql.ResultSet

class PostgresJobCostDataSource(
    private val transactionManager: TransactionManager
) : JobCostDataSource {

    override suspend fun saveSnapshot(snapshot: JobCostSnapshot): JobCostSnapshot {
        return transactionManager.inTransaction(TenantContext(snapshot.projectId)) { tx ->
            val sqlSnapshot = """
                INSERT INTO job_cost_snapshots (
                    snapshot_id, tenant_id, project_id, job_id, job_number, customer_id, product_id,
                    job_quantity, calculation_version, calculation_timestamp, currency,
                    total_actual_cost, total_direct_cost, total_indirect_cost,
                    estimated_cost, cost_variance, cost_variance_percentage,
                    variance_classification, readiness_status, is_reconciled,
                    source_count, duplicate_source_count, unresolved_source_count,
                    warnings_json, integrity_hash, generated_by
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (snapshot_id) DO UPDATE SET
                    total_actual_cost = EXCLUDED.total_actual_cost,
                    total_direct_cost = EXCLUDED.total_direct_cost,
                    total_indirect_cost = EXCLUDED.total_indirect_cost,
                    estimated_cost = EXCLUDED.estimated_cost,
                    cost_variance = EXCLUDED.cost_variance,
                    cost_variance_percentage = EXCLUDED.cost_variance_percentage,
                    variance_classification = EXCLUDED.variance_classification,
                    readiness_status = EXCLUDED.readiness_status,
                    is_reconciled = EXCLUDED.is_reconciled,
                    source_count = EXCLUDED.source_count,
                    duplicate_source_count = EXCLUDED.duplicate_source_count,
                    unresolved_source_count = EXCLUDED.unresolved_source_count,
                    warnings_json = EXCLUDED.warnings_json,
                    integrity_hash = EXCLUDED.integrity_hash,
                    generated_by = EXCLUDED.generated_by
            """.trimIndent()

            tx.connection.prepareStatement(sqlSnapshot).use { ps ->
                ps.setString(1, snapshot.snapshotId)
                ps.setString(2, snapshot.tenantId)
                ps.setString(3, snapshot.projectId)
                ps.setString(4, snapshot.jobId)
                ps.setString(5, snapshot.jobNumber)
                ps.setString(6, snapshot.customerId)
                ps.setString(7, snapshot.productId)
                ps.setInt(8, snapshot.jobQuantity)
                ps.setString(9, snapshot.calculationVersion)
                ps.setLong(10, snapshot.calculationTimestamp)
                ps.setString(11, snapshot.currency)
                ps.setBigDecimal(12, snapshot.totalActualCost)
                ps.setBigDecimal(13, snapshot.totalDirectCost)
                ps.setBigDecimal(14, snapshot.totalIndirectCost)
                ps.setBigDecimal(15, snapshot.estimatedCost)
                ps.setBigDecimal(16, snapshot.costVariance)
                ps.setBigDecimal(17, snapshot.costVariancePercentage)
                ps.setString(18, snapshot.varianceClassification.name)
                ps.setString(19, snapshot.readinessStatus.name)
                ps.setBoolean(20, snapshot.isReconciled)
                ps.setInt(21, snapshot.sourceCount)
                ps.setInt(22, snapshot.duplicateSourceCount)
                ps.setInt(23, snapshot.unresolvedSourceCount)
                ps.setString(24, snapshot.warnings.joinToString("||"))
                ps.setString(25, snapshot.integrityHash)
                ps.setString(26, snapshot.generatedBy)
                ps.executeUpdate()
            }

            // Save Components
            if (snapshot.costComponents.isNotEmpty()) {
                val sqlComp = """
                    INSERT INTO job_cost_components (
                        component_id, snapshot_id, tenant_id, project_id, job_id,
                        component_type, directness, quantity, unit_rate, original_amount,
                        attributed_amount, percentage_of_total_cost, currency, attribution_basis,
                        source_item_count, calculation_explanation
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (component_id) DO NOTHING
                """.trimIndent()

                tx.connection.prepareStatement(sqlComp).use { ps ->
                    for (comp in snapshot.costComponents) {
                        ps.setString(1, comp.componentId)
                        ps.setString(2, snapshot.snapshotId)
                        ps.setString(3, snapshot.tenantId)
                        ps.setString(4, snapshot.projectId)
                        ps.setString(5, snapshot.jobId)
                        ps.setString(6, comp.componentType.name)
                        ps.setString(7, comp.directness.name)
                        ps.setBigDecimal(8, comp.quantity)
                        ps.setBigDecimal(9, comp.unitRate)
                        ps.setBigDecimal(10, comp.originalAmount)
                        ps.setBigDecimal(11, comp.attributedAmount)
                        ps.setBigDecimal(12, comp.percentageOfTotalCost)
                        ps.setString(13, comp.currency)
                        ps.setString(14, comp.attributionBasis)
                        ps.setInt(15, comp.sourceItemCount)
                        ps.setString(16, comp.calculationExplanation)
                        ps.addBatch()
                    }
                    ps.executeBatch()
                }
            }

            // Save Provenances
            if (snapshot.provenances.isNotEmpty()) {
                val sqlProv = """
                    INSERT INTO job_cost_provenance_records (
                        provenance_id, snapshot_id, tenant_id, project_id, job_id,
                        source_module, source_entity_type, source_entity_id, source_transaction_id,
                        source_reference, vendor_id, operation_id, inventory_movement_id,
                        expense_id, payable_id, qc_cost_id, rework_id,
                        cost_component_type, directness, original_amount, attributed_amount,
                        currency, attribution_basis, calculation_explanation, fingerprint_hash
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (provenance_id) DO NOTHING
                """.trimIndent()

                tx.connection.prepareStatement(sqlProv).use { ps ->
                    for (p in snapshot.provenances) {
                        ps.setString(1, p.provenanceId)
                        ps.setString(2, snapshot.snapshotId)
                        ps.setString(3, snapshot.tenantId)
                        ps.setString(4, snapshot.projectId)
                        ps.setString(5, snapshot.jobId)
                        ps.setString(6, p.sourceModule)
                        ps.setString(7, p.sourceEntityType)
                        ps.setString(8, p.sourceEntityId)
                        ps.setString(9, p.sourceTransactionId)
                        ps.setString(10, p.sourceReference)
                        ps.setString(11, p.vendorId)
                        ps.setString(12, p.operationId)
                        ps.setString(13, p.inventoryMovementId)
                        ps.setString(14, p.expenseId)
                        ps.setString(15, p.payableId)
                        ps.setString(16, p.qcCostId)
                        ps.setString(17, p.reworkId)
                        ps.setString(18, p.costComponentType.name)
                        ps.setString(19, p.directness.name)
                        ps.setBigDecimal(20, p.originalAmount)
                        ps.setBigDecimal(21, p.attributedAmount)
                        ps.setString(22, p.currency)
                        ps.setString(23, p.attributionBasis)
                        ps.setString(24, p.calculationExplanation)
                        ps.setString(25, p.fingerprintHash)
                        ps.addBatch()
                    }
                    ps.executeBatch()
                }
            }

            snapshot
        }
    }

    override suspend fun findSnapshotById(
        tenantId: String,
        projectId: String,
        snapshotId: String
    ): JobCostSnapshot? {
        return transactionManager.inTransaction(TenantContext(projectId)) { tx ->
            val sql = """
                SELECT * FROM job_cost_snapshots
                WHERE tenant_id = ? AND project_id = ? AND snapshot_id = ?
            """.trimIndent()

            val snapshot = tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, projectId)
                ps.setString(3, snapshotId)
                ps.executeQuery().use { rs ->
                    if (rs.next()) mapSnapshot(rs) else null
                }
            } ?: return@inTransaction null

            val components = loadComponentsForSnapshot(tx.connection, tenantId, projectId, snapshotId)
            val provenances = loadProvenancesForSnapshot(tx.connection, tenantId, projectId, snapshotId)

            snapshot.copy(
                costComponents = components,
                provenances = provenances
            )
        }
    }

    override suspend fun findLatestSnapshotByJobId(
        tenantId: String,
        projectId: String,
        jobId: String
    ): JobCostSnapshot? {
        return transactionManager.inTransaction(TenantContext(projectId)) { tx ->
            val sql = """
                SELECT * FROM job_cost_snapshots
                WHERE tenant_id = ? AND project_id = ? AND job_id = ?
                ORDER BY calculation_timestamp DESC
                LIMIT 1
            """.trimIndent()

            val snapshot = tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, projectId)
                ps.setString(3, jobId)
                ps.executeQuery().use { rs ->
                    if (rs.next()) mapSnapshot(rs) else null
                }
            } ?: return@inTransaction null

            val components = loadComponentsForSnapshot(tx.connection, tenantId, projectId, snapshot.snapshotId)
            val provenances = loadProvenancesForSnapshot(tx.connection, tenantId, projectId, snapshot.snapshotId)

            snapshot.copy(
                costComponents = components,
                provenances = provenances
            )
        }
    }

    override suspend fun listSnapshots(
        tenantId: String,
        projectId: String,
        jobId: String?,
        limit: Int,
        offset: Int
    ): List<JobCostSnapshot> {
        return transactionManager.inTransaction(TenantContext(projectId)) { tx ->
            val sql = buildString {
                append("SELECT * FROM job_cost_snapshots WHERE tenant_id = ? AND project_id = ? ")
                if (jobId != null) append("AND job_id = ? ")
                append("ORDER BY calculation_timestamp DESC LIMIT ? OFFSET ?")
            }

            tx.connection.prepareStatement(sql).use { ps ->
                var idx = 1
                ps.setString(idx++, tenantId)
                ps.setString(idx++, projectId)
                if (jobId != null) ps.setString(idx++, jobId)
                ps.setInt(idx++, limit)
                ps.setInt(idx++, offset)

                ps.executeQuery().use { rs ->
                    val list = mutableListOf<JobCostSnapshot>()
                    while (rs.next()) {
                        list.add(mapSnapshot(rs))
                    }
                    list
                }
            }
        }
    }

    override suspend fun saveReconciliationEvent(event: JobCostReconciliationEvent): JobCostReconciliationEvent {
        return transactionManager.inTransaction(TenantContext(event.projectId)) { tx ->
            val sql = """
                INSERT INTO job_cost_reconciliation_events (
                    reconciliation_id, snapshot_id, tenant_id, project_id, job_id,
                    is_reconciled, component_total_cost, snapshot_total_cost, provenance_total_cost,
                    component_difference, provenance_difference, duplicate_count, missing_source_count,
                    discrepancies_json, checked_by, checked_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (reconciliation_id) DO NOTHING
            """.trimIndent()

            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, event.reconciliationId)
                ps.setString(2, event.snapshotId)
                ps.setString(3, event.tenantId)
                ps.setString(4, event.projectId)
                ps.setString(5, event.jobId)
                ps.setBoolean(6, event.isReconciled)
                ps.setBigDecimal(7, event.componentTotalCost)
                ps.setBigDecimal(8, event.snapshotTotalCost)
                ps.setBigDecimal(9, event.provenanceTotalCost)
                ps.setBigDecimal(10, event.componentDifference)
                ps.setBigDecimal(11, event.provenanceDifference)
                ps.setInt(12, event.duplicateCount)
                ps.setInt(13, event.missingSourceCount)
                ps.setString(14, event.discrepancies.joinToString("||"))
                ps.setString(15, event.checkedBy)
                ps.setLong(16, event.checkedAt)
                ps.executeUpdate()
            }
            event
        }
    }

    override suspend fun listReconciliationEvents(
        tenantId: String,
        projectId: String,
        jobId: String?,
        snapshotId: String?,
        limit: Int,
        offset: Int
    ): List<JobCostReconciliationEvent> {
        return transactionManager.inTransaction(TenantContext(projectId)) { tx ->
            val sql = buildString {
                append("SELECT * FROM job_cost_reconciliation_events WHERE tenant_id = ? AND project_id = ? ")
                if (jobId != null) append("AND job_id = ? ")
                if (snapshotId != null) append("AND snapshot_id = ? ")
                append("ORDER BY checked_at DESC LIMIT ? OFFSET ?")
            }

            tx.connection.prepareStatement(sql).use { ps ->
                var idx = 1
                ps.setString(idx++, tenantId)
                ps.setString(idx++, projectId)
                if (jobId != null) ps.setString(idx++, jobId)
                if (snapshotId != null) ps.setString(idx++, snapshotId)
                ps.setInt(idx++, limit)
                ps.setInt(idx++, offset)

                ps.executeQuery().use { rs ->
                    val list = mutableListOf<JobCostReconciliationEvent>()
                    while (rs.next()) {
                        list.add(
                            JobCostReconciliationEvent(
                                reconciliationId = rs.getString("reconciliation_id"),
                                snapshotId = rs.getString("snapshot_id"),
                                tenantId = rs.getString("tenant_id"),
                                projectId = rs.getString("project_id"),
                                jobId = rs.getString("job_id"),
                                isReconciled = rs.getBoolean("is_reconciled"),
                                componentTotalCost = rs.getBigDecimal("component_total_cost") ?: BigDecimal.ZERO,
                                snapshotTotalCost = rs.getBigDecimal("snapshot_total_cost") ?: BigDecimal.ZERO,
                                provenanceTotalCost = rs.getBigDecimal("provenance_total_cost") ?: BigDecimal.ZERO,
                                componentDifference = rs.getBigDecimal("component_difference") ?: BigDecimal.ZERO,
                                provenanceDifference = rs.getBigDecimal("provenance_difference") ?: BigDecimal.ZERO,
                                duplicateCount = rs.getInt("duplicate_count"),
                                missingSourceCount = rs.getInt("missing_source_count"),
                                discrepancies = rs.getString("discrepancies_json")?.split("||")?.filter { it.isNotBlank() } ?: emptyList(),
                                checkedBy = rs.getString("checked_by") ?: "SYSTEM",
                                checkedAt = rs.getLong("checked_at")
                            )
                        )
                    }
                    list
                }
            }
        }
    }

    override suspend fun recordAuditEvent(event: JobCostAuditEvent): JobCostAuditEvent {
        return transactionManager.inTransaction(TenantContext(event.projectId)) { tx ->
            val sql = """
                INSERT INTO job_cost_audit_events (
                    event_id, tenant_id, project_id, job_id, snapshot_id, action, actor, actor_role,
                    outcome, details, correlation_id, timestamp
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (event_id) DO NOTHING
            """.trimIndent()

            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, event.eventId)
                ps.setString(2, event.tenantId)
                ps.setString(3, event.projectId)
                ps.setString(4, event.jobId)
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
            event
        }
    }

    override suspend fun listAuditEvents(
        tenantId: String,
        projectId: String,
        jobId: String?,
        limit: Int,
        offset: Int
    ): List<JobCostAuditEvent> {
        return transactionManager.inTransaction(TenantContext(projectId)) { tx ->
            val sql = buildString {
                append("SELECT * FROM job_cost_audit_events WHERE tenant_id = ? AND project_id = ? ")
                if (jobId != null) append("AND job_id = ? ")
                append("ORDER BY timestamp DESC LIMIT ? OFFSET ?")
            }

            tx.connection.prepareStatement(sql).use { ps ->
                var idx = 1
                ps.setString(idx++, tenantId)
                ps.setString(idx++, projectId)
                if (jobId != null) ps.setString(idx++, jobId)
                ps.setInt(idx++, limit)
                ps.setInt(idx++, offset)

                ps.executeQuery().use { rs ->
                    val list = mutableListOf<JobCostAuditEvent>()
                    while (rs.next()) {
                        list.add(
                            JobCostAuditEvent(
                                eventId = rs.getString("event_id"),
                                tenantId = rs.getString("tenant_id"),
                                projectId = rs.getString("project_id"),
                                jobId = rs.getString("job_id"),
                                snapshotId = rs.getString("snapshot_id"),
                                action = rs.getString("action"),
                                actor = rs.getString("actor"),
                                actorRole = rs.getString("actor_role") ?: "STAFF",
                                outcome = rs.getString("outcome") ?: "SUCCESS",
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
    }

    private fun loadComponentsForSnapshot(
        conn: Connection,
        tenantId: String,
        projectId: String,
        snapshotId: String
    ): List<JobCostComponent> {
        val sql = """
            SELECT * FROM job_cost_components
            WHERE tenant_id = ? AND project_id = ? AND snapshot_id = ?
        """.trimIndent()

        return conn.prepareStatement(sql).use { ps ->
            ps.setString(1, tenantId)
            ps.setString(2, projectId)
            ps.setString(3, snapshotId)
            ps.executeQuery().use { rs ->
                val list = mutableListOf<JobCostComponent>()
                while (rs.next()) {
                    list.add(
                        JobCostComponent(
                            componentId = rs.getString("component_id"),
                            tenantId = rs.getString("tenant_id"),
                            projectId = rs.getString("project_id"),
                            jobId = rs.getString("job_id"),
                            componentType = JobCostComponentType.valueOf(rs.getString("component_type")),
                            directness = CostDirectness.valueOf(rs.getString("directness")),
                            quantity = rs.getBigDecimal("quantity") ?: BigDecimal.ZERO,
                            unitRate = rs.getBigDecimal("unit_rate") ?: BigDecimal.ZERO,
                            originalAmount = rs.getBigDecimal("original_amount") ?: BigDecimal.ZERO,
                            attributedAmount = rs.getBigDecimal("attributed_amount") ?: BigDecimal.ZERO,
                            percentageOfTotalCost = rs.getBigDecimal("percentage_of_total_cost") ?: BigDecimal.ZERO,
                            currency = rs.getString("currency") ?: "BDT",
                            attributionBasis = rs.getString("attribution_basis") ?: "CANONICAL",
                            sourceItemCount = rs.getInt("source_item_count"),
                            calculationExplanation = rs.getString("calculation_explanation") ?: ""
                        )
                    )
                }
                list
            }
        }
    }

    private fun loadProvenancesForSnapshot(
        conn: Connection,
        tenantId: String,
        projectId: String,
        snapshotId: String
    ): List<JobCostProvenance> {
        val sql = """
            SELECT * FROM job_cost_provenance_records
            WHERE tenant_id = ? AND project_id = ? AND snapshot_id = ?
        """.trimIndent()

        return conn.prepareStatement(sql).use { ps ->
            ps.setString(1, tenantId)
            ps.setString(2, projectId)
            ps.setString(3, snapshotId)
            ps.executeQuery().use { rs ->
                val list = mutableListOf<JobCostProvenance>()
                while (rs.next()) {
                    list.add(
                        JobCostProvenance(
                            provenanceId = rs.getString("provenance_id"),
                            tenantId = rs.getString("tenant_id"),
                            projectId = rs.getString("project_id"),
                            jobId = rs.getString("job_id"),
                            sourceModule = rs.getString("source_module"),
                            sourceEntityType = rs.getString("source_entity_type"),
                            sourceEntityId = rs.getString("source_entity_id"),
                            sourceTransactionId = rs.getString("source_transaction_id"),
                            sourceReference = rs.getString("source_reference"),
                            vendorId = rs.getString("vendor_id"),
                            operationId = rs.getString("operation_id"),
                            inventoryMovementId = rs.getString("inventory_movement_id"),
                            expenseId = rs.getString("expense_id"),
                            payableId = rs.getString("payable_id"),
                            qcCostId = rs.getString("qc_cost_id"),
                            reworkId = rs.getString("rework_id"),
                            costComponentType = JobCostComponentType.valueOf(rs.getString("cost_component_type")),
                            directness = CostDirectness.valueOf(rs.getString("directness")),
                            originalAmount = rs.getBigDecimal("original_amount") ?: BigDecimal.ZERO,
                            attributedAmount = rs.getBigDecimal("attributed_amount") ?: BigDecimal.ZERO,
                            currency = rs.getString("currency") ?: "BDT",
                            attributionBasis = rs.getString("attribution_basis") ?: "",
                            calculationExplanation = rs.getString("calculation_explanation") ?: "",
                            fingerprintHash = rs.getString("fingerprint_hash") ?: ""
                        )
                    )
                }
                list
            }
        }
    }

    private fun mapSnapshot(rs: ResultSet): JobCostSnapshot {
        return JobCostSnapshot(
            snapshotId = rs.getString("snapshot_id"),
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            jobId = rs.getString("job_id"),
            jobNumber = rs.getString("job_number"),
            customerId = rs.getString("customer_id"),
            productId = rs.getString("product_id"),
            jobQuantity = rs.getInt("job_quantity"),
            calculationVersion = rs.getString("calculation_version") ?: "JOB_COST_ENGINE_V1",
            calculationTimestamp = rs.getLong("calculation_timestamp"),
            currency = rs.getString("currency") ?: "BDT",
            totalActualCost = rs.getBigDecimal("total_actual_cost") ?: BigDecimal.ZERO,
            totalDirectCost = rs.getBigDecimal("total_direct_cost") ?: BigDecimal.ZERO,
            totalIndirectCost = rs.getBigDecimal("total_indirect_cost") ?: BigDecimal.ZERO,
            estimatedCost = rs.getBigDecimal("estimated_cost"),
            costVariance = rs.getBigDecimal("cost_variance"),
            costVariancePercentage = rs.getBigDecimal("cost_variance_percentage"),
            varianceClassification = CostVarianceClassification.valueOf(rs.getString("variance_classification")),
            readinessStatus = JobCostReadinessStatus.valueOf(rs.getString("readiness_status")),
            isReconciled = rs.getBoolean("is_reconciled"),
            sourceCount = rs.getInt("source_count"),
            duplicateSourceCount = rs.getInt("duplicate_source_count"),
            unresolvedSourceCount = rs.getInt("unresolved_source_count"),
            warnings = rs.getString("warnings_json")?.split("||")?.filter { it.isNotBlank() } ?: emptyList(),
            integrityHash = rs.getString("integrity_hash") ?: "",
            generatedBy = rs.getString("generated_by") ?: "SYSTEM"
        )
    }
}
