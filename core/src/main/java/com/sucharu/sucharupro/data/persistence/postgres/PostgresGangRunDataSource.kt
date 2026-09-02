package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.datasource.imposition.GangRunDataSource
import com.sucharu.sucharupro.domain.model.imposition.*
import com.sucharu.sucharupro.domain.model.printingcalculator.*
import java.math.BigDecimal
import java.sql.ResultSet
import java.util.UUID

/**
 * PostgreSQL Implementation for Gang-Run Specifications with TransactionManager and RLS.
 * Module 18 Step 02.
 */
class PostgresGangRunDataSource(
    private val transactionManager: TransactionManager
) : GangRunDataSource {

    override suspend fun saveGangRunSpecification(specification: GangRunSpecification): GangRunSpecification {
        return transactionManager.inTransaction(TenantContext(specification.tenantId)) { ctx ->
            val conn = ctx.connection

            val insertSpecSql = """
                INSERT INTO gang_run_specifications (
                    gang_run_id, tenant_id, batch_name, paper_stock_type, gsm,
                    color_mode, printing_side_option, sheet_width_mm, sheet_height_mm,
                    margin_top_mm, margin_bottom_mm, margin_left_mm, margin_right_mm,
                    bleed_mm, horizontal_gutter_mm, vertical_gutter_mm,
                    total_available_slots, allocated_slots_count, common_required_sheets,
                    total_produced_items, total_overage_items,
                    usable_area_mm2, occupied_area_mm2, waste_area_mm2, sheet_yield_percentage,
                    version, status, integrity_hash, notes, created_at, created_by
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (gang_run_id) DO UPDATE SET
                    version = gang_run_specifications.version + 1,
                    status = EXCLUDED.status,
                    notes = EXCLUDED.notes
            """.trimIndent()

            conn.prepareStatement(insertSpecSql).use { ps ->
                var idx = 1
                ps.setString(idx++, specification.gangRunId)
                ps.setString(idx++, specification.tenantId)
                ps.setString(idx++, specification.batchName)
                ps.setString(idx++, specification.paperStockType.name)
                ps.setBigDecimal(idx++, specification.gsm)
                ps.setString(idx++, specification.colorMode.name)
                ps.setString(idx++, specification.printingSideOption.name)
                ps.setBigDecimal(idx++, specification.parentSheetDimension.width)
                ps.setBigDecimal(idx++, specification.parentSheetDimension.height)
                ps.setBigDecimal(idx++, specification.marginSpec.topMm)
                ps.setBigDecimal(idx++, specification.marginSpec.bottomMm)
                ps.setBigDecimal(idx++, specification.marginSpec.leftMm)
                ps.setBigDecimal(idx++, specification.marginSpec.rightMm)
                ps.setBigDecimal(idx++, specification.spacingSpec.bleedMm)
                ps.setBigDecimal(idx++, specification.spacingSpec.horizontalGutterMm)
                ps.setBigDecimal(idx++, specification.spacingSpec.verticalGutterMm)
                ps.setInt(idx++, specification.totalAvailableSlots)
                ps.setInt(idx++, specification.allocatedSlotsCount)
                ps.setLong(idx++, specification.commonRequiredSheets)
                ps.setLong(idx++, specification.totalProducedItems)
                ps.setLong(idx++, specification.totalOverageItems)
                ps.setBigDecimal(idx++, specification.usableAreaMm2)
                ps.setBigDecimal(idx++, specification.occupiedAreaMm2)
                ps.setBigDecimal(idx++, specification.wasteAreaMm2)
                ps.setBigDecimal(idx++, specification.sheetYieldPercentage)
                ps.setInt(idx++, specification.version)
                ps.setString(idx++, specification.status.name)
                ps.setString(idx++, specification.integrityHash)
                ps.setString(idx++, specification.notes)
                ps.setLong(idx++, specification.createdAt)
                ps.setString(idx++, specification.createdBy)
                ps.executeUpdate()
            }

            // Insert line-item allocations
            val insertAllocSql = """
                INSERT INTO gang_run_item_allocations (
                    allocation_id, gang_run_id, tenant_id, job_id, order_id, order_item_id,
                    product_name, assigned_slots, orientation, slot_item_width_mm, slot_item_height_mm,
                    required_quantity, produced_quantity, overage_quantity,
                    item_occupied_area_mm2, relative_yield_percentage
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()

            conn.prepareStatement(insertAllocSql).use { ps ->
                for (alloc in specification.allocations) {
                    var idx = 1
                    ps.setString(idx++, "ALLOC-${UUID.randomUUID().toString().take(12)}")
                    ps.setString(idx++, specification.gangRunId)
                    ps.setString(idx++, specification.tenantId)
                    ps.setString(idx++, alloc.jobId)
                    ps.setString(idx++, alloc.orderId)
                    ps.setString(idx++, alloc.orderItemId)
                    ps.setString(idx++, alloc.productName)
                    ps.setInt(idx++, alloc.assignedSlots)
                    ps.setString(idx++, alloc.orientation.name)
                    ps.setBigDecimal(idx++, alloc.slotItemWidthMm)
                    ps.setBigDecimal(idx++, alloc.slotItemHeightMm)
                    ps.setLong(idx++, alloc.requiredQuantity)
                    ps.setLong(idx++, alloc.producedQuantity)
                    ps.setLong(idx++, alloc.overageQuantity)
                    ps.setBigDecimal(idx++, alloc.itemOccupiedAreaMm2)
                    ps.setBigDecimal(idx++, alloc.relativeYieldPercentage)
                    ps.addBatch()
                }
                ps.executeBatch()
            }

            specification
        }
    }

    override suspend fun getGangRunSpecification(tenantId: String, gangRunId: String): GangRunSpecification? {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection

            val specSql = "SELECT * FROM gang_run_specifications WHERE tenant_id = ? AND gang_run_id = ?"
            val spec = conn.prepareStatement(specSql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, gangRunId)
                ps.executeQuery().use { rs ->
                    if (rs.next()) mapSpecificationRow(rs) else null
                }
            } ?: return@inTransaction null

            val allocSql = "SELECT * FROM gang_run_item_allocations WHERE tenant_id = ? AND gang_run_id = ? ORDER BY job_id ASC"
            val allocations = conn.prepareStatement(allocSql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, gangRunId)
                ps.executeQuery().use { rs ->
                    val list = mutableListOf<GangRunItemAllocation>()
                    while (rs.next()) {
                        list.add(mapAllocationRow(rs))
                    }
                    list
                }
            }

            spec.copy(allocations = allocations)
        }
    }

    override suspend fun listGangRunSpecifications(tenantId: String, limit: Int, offset: Int): List<GangRunSpecification> {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM gang_run_specifications WHERE tenant_id = ? ORDER BY created_at DESC LIMIT ? OFFSET ?"
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setInt(2, limit)
                ps.setInt(3, offset)
                ps.executeQuery().use { rs ->
                    val list = mutableListOf<GangRunSpecification>()
                    while (rs.next()) {
                        list.add(mapSpecificationRow(rs))
                    }
                    list
                }
            }
        }
    }

    override suspend fun updateGangRunStatus(
        tenantId: String,
        gangRunId: String,
        status: GangRunStatus,
        actor: String,
        notes: String?
    ): Boolean {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection

            val currentStatusSql = "SELECT status FROM gang_run_specifications WHERE tenant_id = ? AND gang_run_id = ?"
            val prevStatus = conn.prepareStatement(currentStatusSql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, gangRunId)
                ps.executeQuery().use { rs ->
                    if (rs.next()) rs.getString("status") else null
                }
            } ?: return@inTransaction false

            val updateSql = "UPDATE gang_run_specifications SET status = ?, notes = COALESCE(?, notes) WHERE tenant_id = ? AND gang_run_id = ?"
            val updated = conn.prepareStatement(updateSql).use { ps ->
                ps.setString(1, status.name)
                ps.setString(2, notes)
                ps.setString(3, tenantId)
                ps.setString(4, gangRunId)
                ps.executeUpdate() > 0
            }

            if (updated) {
                val auditSql = """
                    INSERT INTO gang_run_audit_events (
                        event_id, gang_run_id, tenant_id, action_type, previous_status, new_status, actor, details, created_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
                conn.prepareStatement(auditSql).use { ps ->
                    var idx = 1
                    ps.setString(idx++, "AUDIT-${UUID.randomUUID().toString().take(12)}")
                    ps.setString(idx++, gangRunId)
                    ps.setString(idx++, tenantId)
                    ps.setString(idx++, "STATUS_UPDATE")
                    ps.setString(idx++, prevStatus)
                    ps.setString(idx++, status.name)
                    ps.setString(idx++, actor)
                    ps.setString(idx++, notes)
                    ps.setLong(idx++, System.currentTimeMillis())
                    ps.executeUpdate()
                }
            }

            updated
        }
    }

    private fun mapSpecificationRow(rs: ResultSet): GangRunSpecification {
        return GangRunSpecification(
            gangRunId = rs.getString("gang_run_id"),
            tenantId = rs.getString("tenant_id"),
            batchName = rs.getString("batch_name"),
            paperStockType = PaperStockType.valueOf(rs.getString("paper_stock_type")),
            gsm = rs.getBigDecimal("gsm"),
            colorMode = ColorMode.valueOf(rs.getString("color_mode")),
            printingSideOption = PrintingSideOption.valueOf(rs.getString("printing_side_option")),
            parentSheetDimension = PrintingDimension(
                width = rs.getBigDecimal("sheet_width_mm"),
                height = rs.getBigDecimal("sheet_height_mm"),
                unit = MeasurementUnit.MILLIMETERS
            ),
            marginSpec = ImpositionMarginSpec(
                topMm = rs.getBigDecimal("margin_top_mm"),
                bottomMm = rs.getBigDecimal("margin_bottom_mm"),
                leftMm = rs.getBigDecimal("margin_left_mm"),
                rightMm = rs.getBigDecimal("margin_right_mm")
            ),
            spacingSpec = ImpositionSpacingSpec(
                bleedMm = rs.getBigDecimal("bleed_mm"),
                horizontalGutterMm = rs.getBigDecimal("horizontal_gutter_mm"),
                verticalGutterMm = rs.getBigDecimal("vertical_gutter_mm")
            ),
            totalAvailableSlots = rs.getInt("total_available_slots"),
            allocatedSlotsCount = rs.getInt("allocated_slots_count"),
            commonRequiredSheets = rs.getLong("common_required_sheets"),
            totalProducedItems = rs.getLong("total_produced_items"),
            totalOverageItems = rs.getLong("total_overage_items"),
            usableAreaMm2 = rs.getBigDecimal("usable_area_mm2"),
            occupiedAreaMm2 = rs.getBigDecimal("occupied_area_mm2"),
            wasteAreaMm2 = rs.getBigDecimal("waste_area_mm2"),
            sheetYieldPercentage = rs.getBigDecimal("sheet_yield_percentage"),
            allocations = emptyList(),
            version = rs.getInt("version"),
            status = GangRunStatus.valueOf(rs.getString("status")),
            integrityHash = rs.getString("integrity_hash"),
            notes = rs.getString("notes"),
            createdAt = rs.getLong("created_at"),
            createdBy = rs.getString("created_by")
        )
    }

    private fun mapAllocationRow(rs: ResultSet): GangRunItemAllocation {
        return GangRunItemAllocation(
            jobId = rs.getString("job_id"),
            orderId = rs.getString("order_id"),
            orderItemId = rs.getString("order_item_id"),
            productName = rs.getString("product_name"),
            assignedSlots = rs.getInt("assigned_slots"),
            orientation = ImpositionLayoutOrientation.valueOf(rs.getString("orientation")),
            slotItemWidthMm = rs.getBigDecimal("slot_item_width_mm"),
            slotItemHeightMm = rs.getBigDecimal("slot_item_height_mm"),
            requiredQuantity = rs.getLong("required_quantity"),
            producedQuantity = rs.getLong("produced_quantity"),
            overageQuantity = rs.getLong("overage_quantity"),
            itemOccupiedAreaMm2 = rs.getBigDecimal("item_occupied_area_mm2"),
            relativeYieldPercentage = rs.getBigDecimal("relative_yield_percentage")
        )
    }
}
