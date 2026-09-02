package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.datasource.imposition.DynamicNestingDataSource
import com.sucharu.sucharupro.domain.model.imposition.*
import com.sucharu.sucharupro.domain.model.printingcalculator.*
import java.math.BigDecimal
import java.sql.ResultSet
import java.util.UUID

/**
 * PostgreSQL Implementation for Dynamic Nesting Specifications with TransactionManager and RLS.
 * Module 18 Step 03.
 */
class PostgresDynamicNestingDataSource(
    private val transactionManager: TransactionManager
) : DynamicNestingDataSource {

    override suspend fun saveNestingSpecification(specification: DynamicNestingSpecification): DynamicNestingSpecification {
        return transactionManager.inTransaction(TenantContext(specification.tenantId)) { ctx ->
            val conn = ctx.connection

            val insertSpecSql = """
                INSERT INTO dynamic_nesting_specifications (
                    nesting_id, tenant_id, name, paper_stock_type, gsm,
                    color_mode, printing_side_option, sheet_width_mm, sheet_height_mm,
                    margin_top_mm, margin_bottom_mm, margin_left_mm, margin_right_mm,
                    bleed_mm, horizontal_gutter_mm, vertical_gutter_mm,
                    orientation_policy, placement_strategy, usable_width_mm, usable_height_mm,
                    total_items_placed, common_required_sheets, total_produced_items, total_overage_items,
                    total_sheet_area_mm2, usable_area_mm2, occupied_area_mm2, waste_area_mm2,
                    recoverable_offcut_area_mm2, sheet_utilization_percentage, usable_yield_percentage,
                    offcut_recovery_percentage, version, status, integrity_hash, notes, created_at, created_by
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (nesting_id) DO UPDATE SET
                    version = dynamic_nesting_specifications.version + 1,
                    status = EXCLUDED.status,
                    notes = EXCLUDED.notes
            """.trimIndent()

            conn.prepareStatement(insertSpecSql).use { ps ->
                var idx = 1
                ps.setString(idx++, specification.nestingId)
                ps.setString(idx++, specification.tenantId)
                ps.setString(idx++, specification.name)
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
                ps.setString(idx++, specification.orientationPolicy.name)
                ps.setString(idx++, specification.placementStrategy.name)
                ps.setBigDecimal(idx++, specification.usableWidthMm)
                ps.setBigDecimal(idx++, specification.usableHeightMm)
                ps.setInt(idx++, specification.totalItemsPlaced)
                ps.setLong(idx++, specification.commonRequiredSheets)
                ps.setLong(idx++, specification.totalProducedItems)
                ps.setLong(idx++, specification.totalOverageItems)
                ps.setBigDecimal(idx++, specification.totalSheetAreaMm2)
                ps.setBigDecimal(idx++, specification.usableAreaMm2)
                ps.setBigDecimal(idx++, specification.occupiedAreaMm2)
                ps.setBigDecimal(idx++, specification.wasteAreaMm2)
                ps.setBigDecimal(idx++, specification.recoverableOffcutAreaMm2)
                ps.setBigDecimal(idx++, specification.sheetUtilizationPercentage)
                ps.setBigDecimal(idx++, specification.usableYieldPercentage)
                ps.setBigDecimal(idx++, specification.offcutRecoveryPercentage)
                ps.setInt(idx++, specification.version)
                ps.setString(idx++, specification.status.name)
                ps.setString(idx++, specification.integrityHash)
                ps.setString(idx++, specification.notes)
                ps.setLong(idx++, specification.createdAt)
                ps.setString(idx++, specification.createdBy)
                ps.executeUpdate()
            }

            // Insert line placements
            val insertPlacementSql = """
                INSERT INTO dynamic_nesting_placements (
                    placement_id, nesting_id, tenant_id, slot_index, job_id, order_id, order_item_id,
                    product_name, x_mm, y_mm, placed_width_mm, placed_height_mm, orientation, occupied_area_mm2
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()

            conn.prepareStatement(insertPlacementSql).use { ps ->
                for (placement in specification.placements) {
                    var idx = 1
                    ps.setString(idx++, placement.placementId)
                    ps.setString(idx++, specification.nestingId)
                    ps.setString(idx++, specification.tenantId)
                    ps.setInt(idx++, placement.slotIndex)
                    ps.setString(idx++, placement.jobId)
                    ps.setString(idx++, placement.orderId)
                    ps.setString(idx++, placement.orderItemId)
                    ps.setString(idx++, placement.productName)
                    ps.setBigDecimal(idx++, placement.xMm)
                    ps.setBigDecimal(idx++, placement.yMm)
                    ps.setBigDecimal(idx++, placement.placedWidthMm)
                    ps.setBigDecimal(idx++, placement.placedHeightMm)
                    ps.setString(idx++, placement.orientation.name)
                    ps.setBigDecimal(idx++, placement.occupiedAreaMm2)
                    ps.addBatch()
                }
                ps.executeBatch()
            }

            // Insert offcuts
            val insertOffcutSql = """
                INSERT INTO dynamic_nesting_offcuts (
                    offcut_id, nesting_id, tenant_id, x_mm, y_mm, width_mm, height_mm, area_mm2, is_recoverable
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()

            conn.prepareStatement(insertOffcutSql).use { ps ->
                for (offcut in specification.offcutRemnants) {
                    var idx = 1
                    ps.setString(idx++, offcut.offcutId)
                    ps.setString(idx++, specification.nestingId)
                    ps.setString(idx++, specification.tenantId)
                    ps.setBigDecimal(idx++, offcut.xMm)
                    ps.setBigDecimal(idx++, offcut.yMm)
                    ps.setBigDecimal(idx++, offcut.widthMm)
                    ps.setBigDecimal(idx++, offcut.heightMm)
                    ps.setBigDecimal(idx++, offcut.areaMm2)
                    ps.setBoolean(idx++, offcut.isRecoverable)
                    ps.addBatch()
                }
                ps.executeBatch()
            }

            specification
        }
    }

    override suspend fun getNestingSpecification(tenantId: String, nestingId: String): DynamicNestingSpecification? {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection

            val specSql = "SELECT * FROM dynamic_nesting_specifications WHERE tenant_id = ? AND nesting_id = ?"
            val spec = conn.prepareStatement(specSql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, nestingId)
                ps.executeQuery().use { rs ->
                    if (rs.next()) mapSpecificationRow(rs) else null
                }
            } ?: return@inTransaction null

            val placementSql = "SELECT * FROM dynamic_nesting_placements WHERE tenant_id = ? AND nesting_id = ? ORDER BY slot_index ASC"
            val placements = conn.prepareStatement(placementSql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, nestingId)
                ps.executeQuery().use { rs ->
                    val list = mutableListOf<NestingItemPlacement>()
                    while (rs.next()) {
                        list.add(mapPlacementRow(rs))
                    }
                    list
                }
            }

            val offcutSql = "SELECT * FROM dynamic_nesting_offcuts WHERE tenant_id = ? AND nesting_id = ? ORDER BY area_mm2 DESC"
            val offcuts = conn.prepareStatement(offcutSql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, nestingId)
                ps.executeQuery().use { rs ->
                    val list = mutableListOf<NestingOffcutRemnant>()
                    while (rs.next()) {
                        list.add(mapOffcutRow(rs))
                    }
                    list
                }
            }

            // Reconstruct job summaries from placements
            val jobCopies = placements.groupBy { it.jobId }
            val jobSummaries = jobCopies.map { (jobId, jobPlacements) ->
                val first = jobPlacements.first()
                val assignedSlots = jobPlacements.size
                val itemArea = first.placedWidthMm.multiply(first.placedHeightMm).setScale(4, java.math.RoundingMode.HALF_UP)
                val totalArea = itemArea.multiply(BigDecimal(assignedSlots)).setScale(4, java.math.RoundingMode.HALF_UP)
                val produced = spec.commonRequiredSheets * assignedSlots
                val relYield = if (spec.occupiedAreaMm2 > BigDecimal.ZERO) {
                    totalArea.multiply(BigDecimal("100.0000")).divide(spec.occupiedAreaMm2, 4, java.math.RoundingMode.HALF_UP)
                } else BigDecimal.ZERO

                NestingJobAllocationSummary(
                    jobId = jobId,
                    orderId = first.orderId,
                    orderItemId = first.orderItemId,
                    productName = first.productName,
                    assignedCopiesOnSheet = assignedSlots,
                    requiredQuantity = 0L,
                    producedQuantity = produced,
                    overageQuantity = 0L,
                    totalOccupiedAreaMm2 = totalArea,
                    relativeYieldPercentage = relYield
                )
            }

            spec.copy(
                placements = placements,
                offcutRemnants = offcuts,
                jobSummaries = jobSummaries
            )
        }
    }

    override suspend fun listNestingSpecifications(tenantId: String, limit: Int, offset: Int): List<DynamicNestingSpecification> {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM dynamic_nesting_specifications WHERE tenant_id = ? ORDER BY created_at DESC LIMIT ? OFFSET ?"
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setInt(2, limit)
                ps.setInt(3, offset)
                ps.executeQuery().use { rs ->
                    val list = mutableListOf<DynamicNestingSpecification>()
                    while (rs.next()) {
                        list.add(mapSpecificationRow(rs))
                    }
                    list
                }
            }
        }
    }

    override suspend fun updateNestingStatus(
        tenantId: String,
        nestingId: String,
        status: NestingStatus,
        actor: String,
        notes: String?
    ): Boolean {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection

            val currentStatusSql = "SELECT status FROM dynamic_nesting_specifications WHERE tenant_id = ? AND nesting_id = ?"
            val prevStatus = conn.prepareStatement(currentStatusSql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, nestingId)
                ps.executeQuery().use { rs ->
                    if (rs.next()) rs.getString("status") else null
                }
            } ?: return@inTransaction false

            val updateSql = "UPDATE dynamic_nesting_specifications SET status = ?, notes = COALESCE(?, notes) WHERE tenant_id = ? AND nesting_id = ?"
            val updated = conn.prepareStatement(updateSql).use { ps ->
                ps.setString(1, status.name)
                ps.setString(2, notes)
                ps.setString(3, tenantId)
                ps.setString(4, nestingId)
                ps.executeUpdate() > 0
            }

            if (updated) {
                val auditSql = """
                    INSERT INTO dynamic_nesting_audit_events (
                        event_id, nesting_id, tenant_id, action_type, previous_status, new_status, actor, details, created_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
                conn.prepareStatement(auditSql).use { ps ->
                    var idx = 1
                    ps.setString(idx++, "AUDIT-${UUID.randomUUID().toString().take(12)}")
                    ps.setString(idx++, nestingId)
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

    private fun mapSpecificationRow(rs: ResultSet): DynamicNestingSpecification {
        return DynamicNestingSpecification(
            nestingId = rs.getString("nesting_id"),
            tenantId = rs.getString("tenant_id"),
            name = rs.getString("name"),
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
            orientationPolicy = NestingOrientationPolicy.valueOf(rs.getString("orientation_policy")),
            placementStrategy = NestingPlacementStrategy.valueOf(rs.getString("placement_strategy")),
            usableWidthMm = rs.getBigDecimal("usable_width_mm"),
            usableHeightMm = rs.getBigDecimal("usable_height_mm"),
            placements = emptyList(),
            offcutRemnants = emptyList(),
            jobSummaries = emptyList(),
            totalItemsPlaced = rs.getInt("total_items_placed"),
            commonRequiredSheets = rs.getLong("common_required_sheets"),
            totalProducedItems = rs.getLong("total_produced_items"),
            totalOverageItems = rs.getLong("total_overage_items"),
            totalSheetAreaMm2 = rs.getBigDecimal("total_sheet_area_mm2"),
            usableAreaMm2 = rs.getBigDecimal("usable_area_mm2"),
            occupiedAreaMm2 = rs.getBigDecimal("occupied_area_mm2"),
            wasteAreaMm2 = rs.getBigDecimal("waste_area_mm2"),
            recoverableOffcutAreaMm2 = rs.getBigDecimal("recoverable_offcut_area_mm2"),
            sheetUtilizationPercentage = rs.getBigDecimal("sheet_utilization_percentage"),
            usableYieldPercentage = rs.getBigDecimal("usable_yield_percentage"),
            offcutRecoveryPercentage = rs.getBigDecimal("offcut_recovery_percentage"),
            version = rs.getInt("version"),
            status = NestingStatus.valueOf(rs.getString("status")),
            integrityHash = rs.getString("integrity_hash"),
            notes = rs.getString("notes"),
            createdAt = rs.getLong("created_at"),
            createdBy = rs.getString("created_by")
        )
    }

    private fun mapPlacementRow(rs: ResultSet): NestingItemPlacement {
        return NestingItemPlacement(
            placementId = rs.getString("placement_id"),
            slotIndex = rs.getInt("slot_index"),
            jobId = rs.getString("job_id"),
            orderId = rs.getString("order_id"),
            orderItemId = rs.getString("order_item_id"),
            productName = rs.getString("product_name"),
            xMm = rs.getBigDecimal("x_mm"),
            yMm = rs.getBigDecimal("y_mm"),
            placedWidthMm = rs.getBigDecimal("placed_width_mm"),
            placedHeightMm = rs.getBigDecimal("placed_height_mm"),
            orientation = ImpositionLayoutOrientation.valueOf(rs.getString("orientation")),
            occupiedAreaMm2 = rs.getBigDecimal("occupied_area_mm2")
        )
    }

    private fun mapOffcutRow(rs: ResultSet): NestingOffcutRemnant {
        return NestingOffcutRemnant(
            offcutId = rs.getString("offcut_id"),
            xMm = rs.getBigDecimal("x_mm"),
            yMm = rs.getBigDecimal("y_mm"),
            widthMm = rs.getBigDecimal("width_mm"),
            heightMm = rs.getBigDecimal("height_mm"),
            areaMm2 = rs.getBigDecimal("area_mm2"),
            isRecoverable = rs.getBoolean("is_recoverable")
        )
    }
}
