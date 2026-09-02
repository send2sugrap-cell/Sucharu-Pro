package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.datasource.substratereservation.SubstrateReplenishmentDataSource
import com.sucharu.sucharupro.domain.model.printingcalculator.MeasurementUnit
import com.sucharu.sucharupro.domain.model.printingcalculator.PaperStockType
import com.sucharu.sucharupro.domain.model.printingcalculator.PrintingDimension
import com.sucharu.sucharupro.domain.model.substratereservation.*
import java.math.BigDecimal
import java.sql.ResultSet

class PostgresSubstrateReplenishmentDataSource(
    private val transactionManager: TransactionManager
) : SubstrateReplenishmentDataSource {

    override suspend fun saveEvaluation(evaluation: SubstrateReplenishmentEvaluation): SubstrateReplenishmentEvaluation {
        return transactionManager.inTransaction(TenantContext(evaluation.tenantId)) { ctx ->
            val conn = ctx.connection

            val sqlEval = """
                INSERT INTO substrate_replenishment_evaluations (
                    evaluation_id, tenant_id, product_id, sku, material_name, stock_type, gsm,
                    sheet_width_mm, sheet_height_mm, warehouse_id, warehouse_name, on_hand_physical_sheets,
                    active_reserved_sheets, available_sheets, pending_inbound_sheets, planned_demand_sheets,
                    net_projected_availability_sheets, safety_stock_sheets, reorder_point_sheets,
                    target_stock_sheets, is_reorder_required, projected_shortfall_sheets,
                    recommended_reorder_sheets, recommended_reorder_reams, trigger_state, priority,
                    primary_reason, policy_id, policy_version, primary_vendor_id, primary_vendor_name,
                    deduplication_fingerprint, master_integrity_hash, evaluated_by, evaluated_at, notes, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())
                ON CONFLICT (evaluation_id) DO UPDATE SET
                    on_hand_physical_sheets = EXCLUDED.on_hand_physical_sheets,
                    active_reserved_sheets = EXCLUDED.active_reserved_sheets,
                    available_sheets = EXCLUDED.available_sheets,
                    pending_inbound_sheets = EXCLUDED.pending_inbound_sheets,
                    planned_demand_sheets = EXCLUDED.planned_demand_sheets,
                    net_projected_availability_sheets = EXCLUDED.net_projected_availability_sheets,
                    is_reorder_required = EXCLUDED.is_reorder_required,
                    projected_shortfall_sheets = EXCLUDED.projected_shortfall_sheets,
                    recommended_reorder_sheets = EXCLUDED.recommended_reorder_sheets,
                    recommended_reorder_reams = EXCLUDED.recommended_reorder_reams,
                    trigger_state = EXCLUDED.trigger_state,
                    priority = EXCLUDED.priority,
                    primary_reason = EXCLUDED.primary_reason,
                    primary_vendor_id = EXCLUDED.primary_vendor_id,
                    primary_vendor_name = EXCLUDED.primary_vendor_name,
                    deduplication_fingerprint = EXCLUDED.deduplication_fingerprint,
                    master_integrity_hash = EXCLUDED.master_integrity_hash,
                    notes = EXCLUDED.notes,
                    updated_at = NOW()
            """.trimIndent()

            conn.prepareStatement(sqlEval).use { stmt ->
                stmt.setString(1, evaluation.evaluationId)
                stmt.setString(2, evaluation.tenantId)
                stmt.setString(3, evaluation.productId)
                stmt.setString(4, evaluation.sku)
                stmt.setString(5, evaluation.materialName)
                stmt.setString(6, evaluation.stockType.name)
                stmt.setBigDecimal(7, evaluation.gsm)
                stmt.setBigDecimal(8, evaluation.sheetDimension.width)
                stmt.setBigDecimal(9, evaluation.sheetDimension.height)
                stmt.setString(10, evaluation.warehouseId)
                stmt.setString(11, evaluation.warehouseName)
                stmt.setLong(12, evaluation.onHandPhysicalSheets)
                stmt.setLong(13, evaluation.activeReservedSheets)
                stmt.setLong(14, evaluation.availableSheets)
                stmt.setLong(15, evaluation.pendingInboundSheets)
                stmt.setLong(16, evaluation.plannedDemandSheets)
                stmt.setLong(17, evaluation.netProjectedAvailabilitySheets)
                stmt.setLong(18, evaluation.safetyStockSheets)
                stmt.setLong(19, evaluation.reorderPointSheets)
                stmt.setLong(20, evaluation.targetStockSheets)
                stmt.setBoolean(21, evaluation.isReorderRequired)
                stmt.setLong(22, evaluation.projectedShortfallSheets)
                stmt.setLong(23, evaluation.recommendedReorderSheets)
                stmt.setBigDecimal(24, evaluation.recommendedReorderReams)
                stmt.setString(25, evaluation.triggerState.name)
                stmt.setString(26, evaluation.priority.name)
                stmt.setString(27, evaluation.primaryReason.name)
                stmt.setString(28, evaluation.policyId)
                stmt.setString(29, evaluation.policyVersion)
                stmt.setString(30, evaluation.primaryVendorId)
                stmt.setString(31, evaluation.primaryVendorName)
                stmt.setString(32, evaluation.deduplicationFingerprint)
                stmt.setString(33, evaluation.masterIntegrityHash)
                stmt.setString(34, evaluation.evaluatedBy)
                stmt.setLong(35, evaluation.evaluatedAt)
                stmt.setString(36, evaluation.notes)
                stmt.executeUpdate()
            }

            // Save recommended suppliers
            val sqlDelSuppliers = "DELETE FROM substrate_replenishment_supplier_recommendations WHERE evaluation_id = ? AND tenant_id = ?"
            conn.prepareStatement(sqlDelSuppliers).use { stmt ->
                stmt.setString(1, evaluation.evaluationId)
                stmt.setString(2, evaluation.tenantId)
                stmt.executeUpdate()
            }

            val sqlInsertSupplier = """
                INSERT INTO substrate_replenishment_supplier_recommendations (
                    candidate_id, evaluation_id, tenant_id, vendor_id, vendor_code, vendor_name, rank,
                    suitability_score, estimated_lead_time_days, quoted_cost_per_sheet, minimum_order_quantity_sheets,
                    standard_pack_size, primary_contact_email, primary_contact_phone, is_approved_supplier,
                    selection_rationale
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()

            conn.prepareStatement(sqlInsertSupplier).use { stmt ->
                for (supplier in evaluation.recommendedSuppliers) {
                    stmt.setString(1, supplier.candidateId)
                    stmt.setString(2, evaluation.evaluationId)
                    stmt.setString(3, evaluation.tenantId)
                    stmt.setString(4, supplier.vendorId)
                    stmt.setString(5, supplier.vendorCode)
                    stmt.setString(6, supplier.vendorName)
                    stmt.setInt(7, supplier.rank)
                    stmt.setBigDecimal(8, supplier.suitabilityScore)
                    stmt.setInt(9, supplier.estimatedLeadTimeDays)
                    stmt.setBigDecimal(10, supplier.quotedCostPerSheet)
                    stmt.setLong(11, supplier.minimumOrderQuantitySheets)
                    stmt.setInt(12, supplier.standardPackSize)
                    stmt.setString(13, supplier.primaryContactEmail)
                    stmt.setString(14, supplier.primaryContactPhone)
                    stmt.setBoolean(15, supplier.isApprovedSupplier)
                    stmt.setString(16, supplier.selectionRationale)
                    stmt.addBatch()
                }
                stmt.executeBatch()
            }

            evaluation
        }
    }

    override suspend fun findEvaluationById(tenantId: String, evaluationId: String): SubstrateReplenishmentEvaluation? {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM substrate_replenishment_evaluations WHERE evaluation_id = ? AND tenant_id = ?"
            var eval: SubstrateReplenishmentEvaluation? = null
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, evaluationId)
                stmt.setString(2, tenantId)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) {
                        eval = mapEvaluation(rs)
                    }
                }
            }
            eval?.let { e ->
                val suppliers = loadSuppliersForEvaluation(conn, tenantId, evaluationId)
                e.copy(recommendedSuppliers = suppliers)
            }
        }
    }

    override suspend fun findLatestEvaluationByFingerprint(tenantId: String, fingerprint: String): SubstrateReplenishmentEvaluation? {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM substrate_replenishment_evaluations WHERE tenant_id = ? AND deduplication_fingerprint = ? ORDER BY evaluated_at DESC LIMIT 1"
            var eval: SubstrateReplenishmentEvaluation? = null
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, tenantId)
                stmt.setString(2, fingerprint)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) {
                        eval = mapEvaluation(rs)
                    }
                }
            }
            eval?.let { e ->
                val suppliers = loadSuppliersForEvaluation(conn, tenantId, e.evaluationId)
                e.copy(recommendedSuppliers = suppliers)
            }
        }
    }

    override suspend fun listEvaluationsBySku(tenantId: String, sku: String): List<SubstrateReplenishmentEvaluation> {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM substrate_replenishment_evaluations WHERE tenant_id = ? AND sku = ? ORDER BY evaluated_at DESC"
            val list = mutableListOf<SubstrateReplenishmentEvaluation>()
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, tenantId)
                stmt.setString(2, sku)
                stmt.executeQuery().use { rs ->
                    while (rs.next()) {
                        list.add(mapEvaluation(rs))
                    }
                }
            }
            list.map { it.copy(recommendedSuppliers = loadSuppliersForEvaluation(conn, tenantId, it.evaluationId)) }
        }
    }

    override suspend fun listEvaluationsByState(tenantId: String, state: ReplenishmentTriggerState): List<SubstrateReplenishmentEvaluation> {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM substrate_replenishment_evaluations WHERE tenant_id = ? AND trigger_state = ? ORDER BY evaluated_at DESC"
            val list = mutableListOf<SubstrateReplenishmentEvaluation>()
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, tenantId)
                stmt.setString(2, state.name)
                stmt.executeQuery().use { rs ->
                    while (rs.next()) {
                        list.add(mapEvaluation(rs))
                    }
                }
            }
            list.map { it.copy(recommendedSuppliers = loadSuppliersForEvaluation(conn, tenantId, it.evaluationId)) }
        }
    }

    override suspend fun listAllEvaluations(tenantId: String, limit: Int): List<SubstrateReplenishmentEvaluation> {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM substrate_replenishment_evaluations WHERE tenant_id = ? ORDER BY evaluated_at DESC LIMIT ?"
            val list = mutableListOf<SubstrateReplenishmentEvaluation>()
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, tenantId)
                stmt.setInt(2, limit)
                stmt.executeQuery().use { rs ->
                    while (rs.next()) {
                        list.add(mapEvaluation(rs))
                    }
                }
            }
            list.map { it.copy(recommendedSuppliers = loadSuppliersForEvaluation(conn, tenantId, it.evaluationId)) }
        }
    }

    override suspend fun updateEvaluationStatus(
        tenantId: String,
        evaluationId: String,
        newState: ReplenishmentTriggerState,
        actor: String
    ): Boolean {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "UPDATE substrate_replenishment_evaluations SET trigger_state = ?, updated_at = NOW() WHERE evaluation_id = ? AND tenant_id = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, newState.name)
                stmt.setString(2, evaluationId)
                stmt.setString(3, tenantId)
                stmt.executeUpdate() > 0
            }
        }
    }

    override suspend fun saveSupplierAlert(alert: SupplierReorderAlert): SupplierReorderAlert {
        return transactionManager.inTransaction(TenantContext(alert.tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = """
                INSERT INTO substrate_supplier_reorder_alerts (
                    alert_id, evaluation_id, tenant_id, vendor_id, vendor_code, vendor_name, sku,
                    material_name, requested_sheets, requested_reams, target_delivery_timestamp,
                    priority, status, alert_payload_json, dispatched_by, dispatched_at,
                    acknowledged_at, purchase_requisition_id
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (alert_id) DO UPDATE SET
                    status = EXCLUDED.status,
                    acknowledged_at = EXCLUDED.acknowledged_at,
                    purchase_requisition_id = EXCLUDED.purchase_requisition_id
            """.trimIndent()

            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, alert.alertId)
                stmt.setString(2, alert.evaluationId)
                stmt.setString(3, alert.tenantId)
                stmt.setString(4, alert.vendorId)
                stmt.setString(5, alert.vendorCode)
                stmt.setString(6, alert.vendorName)
                stmt.setString(7, alert.sku)
                stmt.setString(8, alert.materialName)
                stmt.setLong(9, alert.requestedSheets)
                stmt.setBigDecimal(10, alert.requestedReams)
                if (alert.targetDeliveryTimestamp != null) stmt.setLong(11, alert.targetDeliveryTimestamp) else stmt.setNull(11, java.sql.Types.BIGINT)
                stmt.setString(12, alert.priority.name)
                stmt.setString(13, alert.status.name)
                stmt.setString(14, alert.alertPayloadJson)
                stmt.setString(15, alert.dispatchedBy)
                stmt.setLong(16, alert.dispatchedAt)
                if (alert.acknowledgedAt != null) stmt.setLong(17, alert.acknowledgedAt) else stmt.setNull(17, java.sql.Types.BIGINT)
                stmt.setString(18, alert.purchaseRequisitionId)
                stmt.executeUpdate()
            }
            alert
        }
    }

    override suspend fun findAlertById(tenantId: String, alertId: String): SupplierReorderAlert? {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM substrate_supplier_reorder_alerts WHERE alert_id = ? AND tenant_id = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, alertId)
                stmt.setString(2, tenantId)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) mapAlert(rs) else null
                }
            }
        }
    }

    override suspend fun listAlertsByEvaluation(tenantId: String, evaluationId: String): List<SupplierReorderAlert> {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM substrate_supplier_reorder_alerts WHERE tenant_id = ? AND evaluation_id = ? ORDER BY dispatched_at DESC"
            val list = mutableListOf<SupplierReorderAlert>()
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, tenantId)
                stmt.setString(2, evaluationId)
                stmt.executeQuery().use { rs ->
                    while (rs.next()) list.add(mapAlert(rs))
                }
            }
            list
        }
    }

    override suspend fun listAllAlerts(tenantId: String, limit: Int): List<SupplierReorderAlert> {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM substrate_supplier_reorder_alerts WHERE tenant_id = ? ORDER BY dispatched_at DESC LIMIT ?"
            val list = mutableListOf<SupplierReorderAlert>()
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, tenantId)
                stmt.setInt(2, limit)
                stmt.executeQuery().use { rs ->
                    while (rs.next()) list.add(mapAlert(rs))
                }
            }
            list
        }
    }

    override suspend fun saveAuditEvent(event: SubstrateReplenishmentAuditEvent) {
        transactionManager.inTransaction(TenantContext(event.tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = """
                INSERT INTO substrate_replenishment_audit_events (
                    audit_id, evaluation_id, tenant_id, previous_state, new_state,
                    trigger_action, actor, timestamp, details
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, event.auditId)
                stmt.setString(2, event.evaluationId)
                stmt.setString(3, event.tenantId)
                stmt.setString(4, event.previousState.name)
                stmt.setString(5, event.newState.name)
                stmt.setString(6, event.triggerAction)
                stmt.setString(7, event.actor)
                stmt.setLong(8, event.timestamp)
                stmt.setString(9, event.details)
                stmt.executeUpdate()
            }
        }
    }

    override suspend fun listAuditEvents(tenantId: String, evaluationId: String): List<SubstrateReplenishmentAuditEvent> {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM substrate_replenishment_audit_events WHERE tenant_id = ? AND evaluation_id = ? ORDER BY timestamp ASC"
            val list = mutableListOf<SubstrateReplenishmentAuditEvent>()
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, tenantId)
                stmt.setString(2, evaluationId)
                stmt.executeQuery().use { rs ->
                    while (rs.next()) {
                        list.add(
                            SubstrateReplenishmentAuditEvent(
                                auditId = rs.getString("audit_id"),
                                evaluationId = rs.getString("evaluation_id"),
                                tenantId = rs.getString("tenant_id"),
                                previousState = ReplenishmentTriggerState.valueOf(rs.getString("previous_state")),
                                newState = ReplenishmentTriggerState.valueOf(rs.getString("new_state")),
                                triggerAction = rs.getString("trigger_action"),
                                actor = rs.getString("actor"),
                                timestamp = rs.getLong("timestamp"),
                                details = rs.getString("details")
                            )
                        )
                    }
                }
            }
            list
        }
    }

    private fun loadSuppliersForEvaluation(conn: java.sql.Connection, tenantId: String, evaluationId: String): List<SupplierReorderCandidate> {
        val sql = "SELECT * FROM substrate_replenishment_supplier_recommendations WHERE evaluation_id = ? AND tenant_id = ? ORDER BY rank ASC"
        val list = mutableListOf<SupplierReorderCandidate>()
        conn.prepareStatement(sql).use { stmt ->
            stmt.setString(1, evaluationId)
            stmt.setString(2, tenantId)
            stmt.executeQuery().use { rs ->
                while (rs.next()) {
                    list.add(
                        SupplierReorderCandidate(
                            candidateId = rs.getString("candidate_id"),
                            vendorId = rs.getString("vendor_id"),
                            vendorCode = rs.getString("vendor_code"),
                            vendorName = rs.getString("vendor_name"),
                            rank = rs.getInt("rank"),
                            suitabilityScore = rs.getBigDecimal("suitability_score"),
                            estimatedLeadTimeDays = rs.getInt("estimated_lead_time_days"),
                            quotedCostPerSheet = rs.getBigDecimal("quoted_cost_per_sheet"),
                            minimumOrderQuantitySheets = rs.getLong("minimum_order_quantity_sheets"),
                            standardPackSize = rs.getInt("standard_pack_size"),
                            primaryContactEmail = rs.getString("primary_contact_email"),
                            primaryContactPhone = rs.getString("primary_contact_phone"),
                            isApprovedSupplier = rs.getBoolean("is_approved_supplier"),
                            selectionRationale = rs.getString("selection_rationale")
                        )
                    )
                }
            }
        }
        return list
    }

    private fun mapEvaluation(rs: ResultSet): SubstrateReplenishmentEvaluation {
        return SubstrateReplenishmentEvaluation(
            evaluationId = rs.getString("evaluation_id"),
            tenantId = rs.getString("tenant_id"),
            productId = rs.getString("product_id"),
            sku = rs.getString("sku"),
            materialName = rs.getString("material_name"),
            stockType = PaperStockType.valueOf(rs.getString("stock_type")),
            gsm = rs.getBigDecimal("gsm"),
            sheetDimension = PrintingDimension(
                width = rs.getBigDecimal("sheet_width_mm"),
                height = rs.getBigDecimal("sheet_height_mm"),
                unit = MeasurementUnit.MILLIMETERS
            ),
            warehouseId = rs.getString("warehouse_id"),
            warehouseName = rs.getString("warehouse_name"),
            onHandPhysicalSheets = rs.getLong("on_hand_physical_sheets"),
            activeReservedSheets = rs.getLong("active_reserved_sheets"),
            availableSheets = rs.getLong("available_sheets"),
            pendingInboundSheets = rs.getLong("pending_inbound_sheets"),
            plannedDemandSheets = rs.getLong("planned_demand_sheets"),
            netProjectedAvailabilitySheets = rs.getLong("net_projected_availability_sheets"),
            safetyStockSheets = rs.getLong("safety_stock_sheets"),
            reorderPointSheets = rs.getLong("reorder_point_sheets"),
            targetStockSheets = rs.getLong("target_stock_sheets"),
            isReorderRequired = rs.getBoolean("is_reorder_required"),
            projectedShortfallSheets = rs.getLong("projected_shortfall_sheets"),
            recommendedReorderSheets = rs.getLong("recommended_reorder_sheets"),
            recommendedReorderReams = rs.getBigDecimal("recommended_reorder_reams"),
            triggerState = ReplenishmentTriggerState.valueOf(rs.getString("trigger_state")),
            priority = ReplenishmentPriority.valueOf(rs.getString("priority")),
            primaryReason = ReplenishmentReason.valueOf(rs.getString("primary_reason")),
            policyId = rs.getString("policy_id"),
            policyVersion = rs.getString("policy_version"),
            primaryVendorId = rs.getString("primary_vendor_id"),
            primaryVendorName = rs.getString("primary_vendor_name"),
            deduplicationFingerprint = rs.getString("deduplication_fingerprint"),
            masterIntegrityHash = rs.getString("master_integrity_hash"),
            evaluatedBy = rs.getString("evaluated_by"),
            evaluatedAt = rs.getLong("evaluated_at"),
            notes = rs.getString("notes")
        )
    }

    private fun mapAlert(rs: ResultSet): SupplierReorderAlert {
        return SupplierReorderAlert(
            alertId = rs.getString("alert_id"),
            evaluationId = rs.getString("evaluation_id"),
            tenantId = rs.getString("tenant_id"),
            vendorId = rs.getString("vendor_id"),
            vendorCode = rs.getString("vendor_code"),
            vendorName = rs.getString("vendor_name"),
            sku = rs.getString("sku"),
            materialName = rs.getString("material_name"),
            requestedSheets = rs.getLong("requested_sheets"),
            requestedReams = rs.getBigDecimal("requested_reams"),
            targetDeliveryTimestamp = rs.getObject("target_delivery_timestamp") as? Long,
            priority = ReplenishmentPriority.valueOf(rs.getString("priority")),
            status = ReplenishmentTriggerState.valueOf(rs.getString("status")),
            alertPayloadJson = rs.getString("alert_payload_json"),
            dispatchedBy = rs.getString("dispatched_by"),
            dispatchedAt = rs.getLong("dispatched_at"),
            acknowledgedAt = rs.getObject("acknowledged_at") as? Long,
            purchaseRequisitionId = rs.getString("purchase_requisition_id")
        )
    }
}
