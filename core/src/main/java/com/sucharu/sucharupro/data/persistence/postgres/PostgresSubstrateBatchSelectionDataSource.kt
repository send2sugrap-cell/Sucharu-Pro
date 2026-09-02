package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.datasource.substratereservation.SubstrateBatchSelectionDataSource
import com.sucharu.sucharupro.domain.model.printingcalculator.MeasurementUnit
import com.sucharu.sucharupro.domain.model.printingcalculator.PaperStockType
import com.sucharu.sucharupro.domain.model.printingcalculator.PrintingDimension
import com.sucharu.sucharupro.domain.model.substratereservation.*
import java.math.BigDecimal
import java.sql.ResultSet
import java.util.UUID

class PostgresSubstrateBatchSelectionDataSource(
    private val transactionManager: TransactionManager
) : SubstrateBatchSelectionDataSource {

    override suspend fun saveSelectionResult(result: BatchLotSelectionResult): BatchLotSelectionResult {
        return transactionManager.inTransaction(TenantContext(result.tenantId)) { ctx ->
            val conn = ctx.connection

            val sqlRecord = """
                INSERT INTO substrate_batch_selection_records (
                    selection_id, tenant_id, order_id, order_item_id, execution_job_id, work_order_id,
                    reservation_id, product_id, sku, requested_material_name, stock_type, target_gsm,
                    required_sheet_width_mm, required_sheet_height_mm, required_grain_direction,
                    required_sheets, allocated_sheets, deficit_sheets, allocated_reams, allocated_weight_kg,
                    allow_sheet_rotation, allow_multi_batch_fulfillment, selection_policy, status,
                    is_fully_satisfied, is_multi_batch_fulfillment, primary_selected_batch_number,
                    primary_selected_lot_number, primary_warehouse_id, overall_compatibility_score,
                    selection_explanation, master_integrity_hash, is_confirmed_and_allocated,
                    selected_by, selected_at, confirmed_at, confirmed_by, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())
                ON CONFLICT (selection_id) DO UPDATE SET
                    allocated_sheets = EXCLUDED.allocated_sheets,
                    deficit_sheets = EXCLUDED.deficit_sheets,
                    allocated_reams = EXCLUDED.allocated_reams,
                    allocated_weight_kg = EXCLUDED.allocated_weight_kg,
                    status = EXCLUDED.status,
                    is_fully_satisfied = EXCLUDED.is_fully_satisfied,
                    is_multi_batch_fulfillment = EXCLUDED.is_multi_batch_fulfillment,
                    primary_selected_batch_number = EXCLUDED.primary_selected_batch_number,
                    primary_selected_lot_number = EXCLUDED.primary_selected_lot_number,
                    primary_warehouse_id = EXCLUDED.primary_warehouse_id,
                    overall_compatibility_score = EXCLUDED.overall_compatibility_score,
                    selection_explanation = EXCLUDED.selection_explanation,
                    master_integrity_hash = EXCLUDED.master_integrity_hash,
                    is_confirmed_and_allocated = EXCLUDED.is_confirmed_and_allocated,
                    confirmed_at = EXCLUDED.confirmed_at,
                    confirmed_by = EXCLUDED.confirmed_by,
                    updated_at = NOW()
            """.trimIndent()

            conn.prepareStatement(sqlRecord).use { stmt ->
                stmt.setString(1, result.selectionId)
                stmt.setString(2, result.tenantId)
                stmt.setString(3, result.specification.orderId)
                stmt.setString(4, result.specification.orderItemId)
                stmt.setString(5, result.specification.executionJobId)
                stmt.setString(6, result.specification.workOrderId)
                stmt.setString(7, result.specification.reservationId)
                stmt.setString(8, result.specification.productId)
                stmt.setString(9, result.specification.sku)
                stmt.setString(10, result.specification.requestedMaterialName)
                stmt.setString(11, result.specification.stockType.name)
                stmt.setBigDecimal(12, result.specification.targetGsm)
                stmt.setBigDecimal(13, result.specification.requiredSheetDimension.width)
                stmt.setBigDecimal(14, result.specification.requiredSheetDimension.height)
                stmt.setString(15, result.specification.requiredGrainDirection.name)
                stmt.setLong(16, result.requiredSheets)
                stmt.setLong(17, result.allocatedSheets)
                stmt.setLong(18, result.deficitSheets)
                stmt.setBigDecimal(19, result.allocatedReams)
                stmt.setBigDecimal(20, result.allocatedWeightKg)
                stmt.setBoolean(21, result.specification.allowSheetRotation)
                stmt.setBoolean(22, result.specification.allowMultiBatchFulfillment)
                stmt.setString(23, result.specification.selectionPolicy.name)
                stmt.setString(24, result.status.name)
                stmt.setBoolean(25, result.isFullySatisfied)
                stmt.setBoolean(26, result.isMultiBatchFulfillment)
                stmt.setString(27, result.primarySelectedBatchNumber)
                stmt.setString(28, result.primarySelectedLotNumber)
                stmt.setString(29, result.primaryWarehouseId)
                stmt.setBigDecimal(30, result.overallCompatibilityScore)
                stmt.setString(31, result.selectionExplanation)
                stmt.setString(32, result.masterIntegrityHash)
                stmt.setBoolean(33, result.isConfirmedAndAllocated)
                stmt.setString(34, result.selectedBy)
                stmt.setLong(35, result.selectedAt)
                if (result.confirmedAt != null) stmt.setLong(36, result.confirmedAt) else stmt.setNull(36, java.sql.Types.BIGINT)
                stmt.setString(37, result.confirmedBy)
                stmt.executeUpdate()
            }

            // Delete existing allocations before re-inserting
            conn.prepareStatement("DELETE FROM substrate_batch_selection_allocations WHERE selection_id = ?").use { stmt ->
                stmt.setString(1, result.selectionId)
                stmt.executeUpdate()
            }

            val sqlAlloc = """
                INSERT INTO substrate_batch_selection_allocations (
                    allocation_id, selection_id, tenant_id, warehouse_id, warehouse_name, location_id,
                    batch_number, lot_number, sku, allocated_sheets, allocated_reams, allocated_weight_kg,
                    sheet_width_mm, sheet_height_mm, grain_direction, is_rotated, match_score
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()

            conn.prepareStatement(sqlAlloc).use { stmt ->
                for (alloc in result.selectedBatches) {
                    stmt.setString(1, alloc.allocationId)
                    stmt.setString(2, result.selectionId)
                    stmt.setString(3, result.tenantId)
                    stmt.setString(4, alloc.warehouseId)
                    stmt.setString(5, alloc.warehouseName)
                    stmt.setString(6, alloc.locationId)
                    stmt.setString(7, alloc.batchNumber)
                    stmt.setString(8, alloc.lotNumber)
                    stmt.setString(9, alloc.sku)
                    stmt.setLong(10, alloc.allocatedSheets)
                    stmt.setBigDecimal(11, alloc.allocatedReams)
                    stmt.setBigDecimal(12, alloc.allocatedWeightKg)
                    stmt.setBigDecimal(13, alloc.sheetDimension.width)
                    stmt.setBigDecimal(14, alloc.sheetDimension.height)
                    stmt.setString(15, alloc.grainDirection.name)
                    stmt.setBoolean(16, alloc.isRotated)
                    stmt.setBigDecimal(17, alloc.matchScore)
                    stmt.addBatch()
                }
                stmt.executeBatch()
            }

            // Record audit event
            val sqlAudit = """
                INSERT INTO substrate_batch_selection_audits (
                    audit_id, selection_id, tenant_id, event_type, actor, details, timestamp
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()

            conn.prepareStatement(sqlAudit).use { stmt ->
                stmt.setString(1, "SBAUD-${UUID.randomUUID().toString().take(8).uppercase()}")
                stmt.setString(2, result.selectionId)
                stmt.setString(3, result.tenantId)
                stmt.setString(4, "SELECTION_EVALUATED")
                stmt.setString(5, result.selectedBy)
                stmt.setString(6, "Status: ${result.status.name}, Allocated: ${result.allocatedSheets}/${result.requiredSheets}")
                stmt.setLong(7, System.currentTimeMillis())
                stmt.executeUpdate()
            }

            result
        }
    }

    override suspend fun findSelectionById(tenantId: String, selectionId: String): BatchLotSelectionResult? {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM substrate_batch_selection_records WHERE selection_id = ? AND tenant_id = ?"

            var result: BatchLotSelectionResult? = null
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, selectionId)
                stmt.setString(2, tenantId)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) {
                        val allocations = loadAllocations(conn, selectionId, tenantId)
                        result = mapRowToResult(rs, allocations)
                    }
                }
            }
            result
        }
    }

    override suspend fun findSelectionsByOrder(tenantId: String, orderId: String): List<BatchLotSelectionResult> {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM substrate_batch_selection_records WHERE order_id = ? AND tenant_id = ? ORDER BY selected_at DESC"
            val results = mutableListOf<BatchLotSelectionResult>()

            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, orderId)
                stmt.setString(2, tenantId)
                stmt.executeQuery().use { rs ->
                    while (rs.next()) {
                        val selId = rs.getString("selection_id")
                        val allocations = loadAllocations(conn, selId, tenantId)
                        results.add(mapRowToResult(rs, allocations))
                    }
                }
            }
            results
        }
    }

    override suspend fun findSelectionsByJob(tenantId: String, executionJobId: String): List<BatchLotSelectionResult> {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM substrate_batch_selection_records WHERE execution_job_id = ? AND tenant_id = ? ORDER BY selected_at DESC"
            val results = mutableListOf<BatchLotSelectionResult>()

            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, executionJobId)
                stmt.setString(2, tenantId)
                stmt.executeQuery().use { rs ->
                    while (rs.next()) {
                        val selId = rs.getString("selection_id")
                        val allocations = loadAllocations(conn, selId, tenantId)
                        results.add(mapRowToResult(rs, allocations))
                    }
                }
            }
            results
        }
    }

    override suspend fun listAllSelections(tenantId: String, limit: Int): List<BatchLotSelectionResult> {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM substrate_batch_selection_records WHERE tenant_id = ? ORDER BY selected_at DESC LIMIT ?"
            val results = mutableListOf<BatchLotSelectionResult>()

            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, tenantId)
                stmt.setInt(2, limit)
                stmt.executeQuery().use { rs ->
                    while (rs.next()) {
                        val selId = rs.getString("selection_id")
                        val allocations = loadAllocations(conn, selId, tenantId)
                        results.add(mapRowToResult(rs, allocations))
                    }
                }
            }
            results
        }
    }

    override suspend fun updateSelectionConfirmation(
        tenantId: String,
        selectionId: String,
        isConfirmed: Boolean,
        confirmedBy: String?,
        confirmedAt: Long?
    ): Boolean {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = """
                UPDATE substrate_batch_selection_records
                SET is_confirmed_and_allocated = ?, confirmed_by = ?, confirmed_at = ?, updated_at = NOW()
                WHERE selection_id = ? AND tenant_id = ?
            """.trimIndent()

            val updated = conn.prepareStatement(sql).use { stmt ->
                stmt.setBoolean(1, isConfirmed)
                stmt.setString(2, confirmedBy)
                if (confirmedAt != null) stmt.setLong(3, confirmedAt) else stmt.setNull(3, java.sql.Types.BIGINT)
                stmt.setString(4, selectionId)
                stmt.setString(5, tenantId)
                stmt.executeUpdate() > 0
            }

            if (updated) {
                val sqlAudit = """
                    INSERT INTO substrate_batch_selection_audits (
                        audit_id, selection_id, tenant_id, event_type, actor, details, timestamp
                    ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()

                conn.prepareStatement(sqlAudit).use { stmt ->
                    stmt.setString(1, "SBAUD-${UUID.randomUUID().toString().take(8).uppercase()}")
                    stmt.setString(2, selectionId)
                    stmt.setString(3, tenantId)
                    stmt.setString(4, "SELECTION_CONFIRMED")
                    stmt.setString(5, confirmedBy ?: "SYSTEM")
                    stmt.setString(6, "Selection confirmed and allocated to production job")
                    stmt.setLong(7, confirmedAt ?: System.currentTimeMillis())
                    stmt.executeUpdate()
                }
            }
            updated
        }
    }

    private fun loadAllocations(conn: java.sql.Connection, selectionId: String, tenantId: String): List<SelectedBatchAllocation> {
        val sql = "SELECT * FROM substrate_batch_selection_allocations WHERE selection_id = ? AND tenant_id = ?"
        val list = mutableListOf<SelectedBatchAllocation>()
        conn.prepareStatement(sql).use { stmt ->
            stmt.setString(1, selectionId)
            stmt.setString(2, tenantId)
            stmt.executeQuery().use { rs ->
                while (rs.next()) {
                    list.add(
                        SelectedBatchAllocation(
                            allocationId = rs.getString("allocation_id"),
                            selectionId = rs.getString("selection_id"),
                            tenantId = rs.getString("tenant_id"),
                            warehouseId = rs.getString("warehouse_id"),
                            warehouseName = rs.getString("warehouse_name"),
                            locationId = rs.getString("location_id"),
                            batchNumber = rs.getString("batch_number"),
                            lotNumber = rs.getString("lot_number"),
                            sku = rs.getString("sku"),
                            allocatedSheets = rs.getLong("allocated_sheets"),
                            allocatedReams = rs.getBigDecimal("allocated_reams"),
                            allocatedWeightKg = rs.getBigDecimal("allocated_weight_kg"),
                            sheetDimension = PrintingDimension(
                                width = rs.getBigDecimal("sheet_width_mm"),
                                height = rs.getBigDecimal("sheet_height_mm"),
                                unit = MeasurementUnit.MILLIMETERS
                            ),
                            grainDirection = PaperGrainDirection.fromString(rs.getString("grain_direction")),
                            isRotated = rs.getBoolean("is_rotated"),
                            matchScore = rs.getBigDecimal("match_score")
                        )
                    )
                }
            }
        }
        return list
    }

    private fun mapRowToResult(rs: ResultSet, allocations: List<SelectedBatchAllocation>): BatchLotSelectionResult {
        val stockType = try {
            PaperStockType.valueOf(rs.getString("stock_type"))
        } catch (_: Exception) {
            PaperStockType.ART_PAPER
        }

        val policy = try {
            BatchSelectionPolicy.valueOf(rs.getString("selection_policy"))
        } catch (_: Exception) {
            BatchSelectionPolicy.FIFO
        }

        val status = try {
            BatchLotSelectionStatus.valueOf(rs.getString("status"))
        } catch (_: Exception) {
            BatchLotSelectionStatus.FULLY_SATISFIED
        }

        val grain = PaperGrainDirection.fromString(rs.getString("required_grain_direction"))

        val spec = BatchLotSelectionSpecification(
            selectionId = rs.getString("selection_id"),
            tenantId = rs.getString("tenant_id"),
            orderId = rs.getString("order_id"),
            orderItemId = rs.getString("order_item_id"),
            executionJobId = rs.getString("execution_job_id"),
            workOrderId = rs.getString("work_order_id"),
            reservationId = rs.getString("reservation_id"),
            productId = rs.getString("product_id"),
            sku = rs.getString("sku"),
            requestedMaterialName = rs.getString("requested_material_name"),
            stockType = stockType,
            targetGsm = rs.getBigDecimal("target_gsm"),
            requiredSheetDimension = PrintingDimension(
                width = rs.getBigDecimal("required_sheet_width_mm"),
                height = rs.getBigDecimal("required_sheet_height_mm"),
                unit = MeasurementUnit.MILLIMETERS
            ),
            requiredGrainDirection = grain,
            requiredSheets = rs.getLong("required_sheets"),
            allowSheetRotation = rs.getBoolean("allow_sheet_rotation"),
            allowMultiBatchFulfillment = rs.getBoolean("allow_multi_batch_fulfillment"),
            selectionPolicy = policy,
            actor = rs.getString("selected_by")
        )

        return BatchLotSelectionResult(
            selectionId = rs.getString("selection_id"),
            tenantId = rs.getString("tenant_id"),
            specification = spec,
            status = status,
            requiredSheets = rs.getLong("required_sheets"),
            allocatedSheets = rs.getLong("allocated_sheets"),
            deficitSheets = rs.getLong("deficit_sheets"),
            allocatedReams = rs.getBigDecimal("allocated_reams"),
            allocatedWeightKg = rs.getBigDecimal("allocated_weight_kg"),
            isFullySatisfied = rs.getBoolean("is_fully_satisfied"),
            isMultiBatchFulfillment = rs.getBoolean("is_multi_batch_fulfillment"),
            selectedBatches = allocations,
            evaluatedCandidates = emptyList(),
            primarySelectedBatchNumber = rs.getString("primary_selected_batch_number"),
            primarySelectedLotNumber = rs.getString("primary_selected_lot_number"),
            primaryWarehouseId = rs.getString("primary_warehouse_id"),
            overallCompatibilityScore = rs.getBigDecimal("overall_compatibility_score"),
            selectionExplanation = rs.getString("selection_explanation"),
            masterIntegrityHash = rs.getString("master_integrity_hash"),
            selectedAt = rs.getLong("selected_at"),
            selectedBy = rs.getString("selected_by"),
            isConfirmedAndAllocated = rs.getBoolean("is_confirmed_and_allocated"),
            confirmedAt = rs.getLong("confirmed_at").takeIf { !rs.wasNull() },
            confirmedBy = rs.getString("confirmed_by")
        )
    }
}
