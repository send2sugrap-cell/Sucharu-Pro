package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.datasource.imposition.ImpositionDataSource
import com.sucharu.sucharupro.domain.model.imposition.*
import com.sucharu.sucharupro.domain.model.printingcalculator.MeasurementUnit
import com.sucharu.sucharupro.domain.model.printingcalculator.PrintingDimension
import java.sql.ResultSet
import java.util.UUID

class PostgresImpositionDataSource(
    private val transactionManager: TransactionManager
) : ImpositionDataSource {

    override suspend fun saveSpecification(spec: ImpositionSpecification): ImpositionSpecification {
        return transactionManager.inTransaction(TenantContext(spec.tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = """
                INSERT INTO imposition_specifications (
                    imposition_id, tenant_id, job_id, order_id, order_item_id, calculation_id, product_name,
                    item_width_mm, item_height_mm, sheet_width_mm, sheet_height_mm, usable_width_mm, usable_height_mm,
                    margin_top_mm, margin_bottom_mm, margin_left_mm, margin_right_mm,
                    bleed_mm, horizontal_gutter_mm, vertical_gutter_mm,
                    orientation_policy, selected_orientation, columns_count, rows_count, copies_per_sheet,
                    required_quantity, required_sheets, total_produced_capacity, overage_quantity,
                    occupied_area_mm2, usable_area_mm2, waste_area_mm2, yield_percentage,
                    version, status, integrity_hash, notes, created_at, created_by
                ) VALUES (
                    ?, ?, ?, ?, ?, ?, ?,
                    ?, ?, ?, ?, ?, ?,
                    ?, ?, ?, ?,
                    ?, ?, ?,
                    ?, ?, ?, ?, ?,
                    ?, ?, ?, ?,
                    ?, ?, ?, ?,
                    ?, ?, ?, ?, ?, ?
                )
                ON CONFLICT (imposition_id) DO UPDATE SET
                    status = EXCLUDED.status,
                    version = EXCLUDED.version,
                    notes = EXCLUDED.notes
            """.trimIndent()

            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, spec.impositionId)
                ps.setString(2, spec.tenantId)
                ps.setString(3, spec.jobId)
                ps.setString(4, spec.orderId)
                ps.setString(5, spec.orderItemId)
                ps.setString(6, spec.calculationId)
                ps.setString(7, spec.productName)

                ps.setBigDecimal(8, spec.finishedItemDimension.width)
                ps.setBigDecimal(9, spec.finishedItemDimension.height)
                ps.setBigDecimal(10, spec.parentSheetDimension.width)
                ps.setBigDecimal(11, spec.parentSheetDimension.height)
                ps.setBigDecimal(12, spec.usableWidthMm)
                ps.setBigDecimal(13, spec.usableHeightMm)

                ps.setBigDecimal(14, spec.marginSpec.topMm)
                ps.setBigDecimal(15, spec.marginSpec.bottomMm)
                ps.setBigDecimal(16, spec.marginSpec.leftMm)
                ps.setBigDecimal(17, spec.marginSpec.rightMm)

                ps.setBigDecimal(18, spec.spacingSpec.bleedMm)
                ps.setBigDecimal(19, spec.spacingSpec.horizontalGutterMm)
                ps.setBigDecimal(20, spec.spacingSpec.verticalGutterMm)

                ps.setString(21, spec.orientationPolicy.name)
                ps.setString(22, spec.selectedOrientation.name)
                ps.setInt(23, spec.columns)
                ps.setInt(24, spec.rows)
                ps.setInt(25, spec.copiesPerSheet)

                ps.setLong(26, spec.requiredQuantity)
                ps.setLong(27, spec.requiredSheets)
                ps.setLong(28, spec.totalProducedCapacity)
                ps.setLong(29, spec.overageQuantity)

                ps.setBigDecimal(30, spec.occupiedAreaMm2)
                ps.setBigDecimal(31, spec.usableAreaMm2)
                ps.setBigDecimal(32, spec.wasteAreaMm2)
                ps.setBigDecimal(33, spec.yieldPercentage)

                ps.setInt(34, spec.version)
                ps.setString(35, spec.status.name)
                ps.setString(36, spec.integrityHash)
                ps.setString(37, spec.notes)
                ps.setLong(38, spec.createdAt)
                ps.setString(39, spec.createdBy)

                ps.executeUpdate()
            }

            // Insert audit event
            val auditSql = """
                INSERT INTO imposition_audit_events (
                    event_id, imposition_id, tenant_id, action_type, previous_status, new_status, actor, details, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            conn.prepareStatement(auditSql).use { ps ->
                ps.setString(1, "EVT-${UUID.randomUUID().toString().take(12)}")
                ps.setString(2, spec.impositionId)
                ps.setString(3, spec.tenantId)
                ps.setString(4, "IMPOSITION_OPTIMIZED")
                ps.setString(5, null)
                ps.setString(6, spec.status.name)
                ps.setString(7, spec.createdBy)
                ps.setString(8, "Created imposition specification with ${spec.copiesPerSheet} up (${spec.columns}x${spec.rows})")
                ps.setLong(9, System.currentTimeMillis())
                ps.executeUpdate()
            }

            spec
        }
    }

    override suspend fun getSpecificationById(tenantId: String, impositionId: String): ImpositionSpecification? {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM imposition_specifications WHERE tenant_id = ? AND imposition_id = ?"
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, impositionId)
                ps.executeQuery().use { rs ->
                    if (rs.next()) mapRow(rs) else null
                }
            }
        }
    }

    override suspend fun listSpecificationsByJob(tenantId: String, jobId: String): List<ImpositionSpecification> {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM imposition_specifications WHERE tenant_id = ? AND job_id = ? ORDER BY created_at DESC"
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, jobId)
                ps.executeQuery().use { rs ->
                    val list = mutableListOf<ImpositionSpecification>()
                    while (rs.next()) {
                        list.add(mapRow(rs))
                    }
                    list
                }
            }
        }
    }

    override suspend fun listSpecificationsByOrder(tenantId: String, orderId: String): List<ImpositionSpecification> {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM imposition_specifications WHERE tenant_id = ? AND order_id = ? ORDER BY created_at DESC"
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, orderId)
                ps.executeQuery().use { rs ->
                    val list = mutableListOf<ImpositionSpecification>()
                    while (rs.next()) {
                        list.add(mapRow(rs))
                    }
                    list
                }
            }
        }
    }

    override suspend fun listAllSpecifications(tenantId: String, limit: Int): List<ImpositionSpecification> {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM imposition_specifications WHERE tenant_id = ? ORDER BY created_at DESC LIMIT ?"
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setInt(2, limit)
                ps.executeQuery().use { rs ->
                    val list = mutableListOf<ImpositionSpecification>()
                    while (rs.next()) {
                        list.add(mapRow(rs))
                    }
                    list
                }
            }
        }
    }

    override suspend fun updateStatus(
        tenantId: String,
        impositionId: String,
        status: String,
        actor: String,
        notes: String?
    ): Boolean {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val currentSql = "SELECT status FROM imposition_specifications WHERE tenant_id = ? AND imposition_id = ?"
            var prevStatus: String? = null
            conn.prepareStatement(currentSql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, impositionId)
                ps.executeQuery().use { rs ->
                    if (rs.next()) prevStatus = rs.getString("status")
                }
            }

            if (prevStatus == null) return@inTransaction false

            val updateSql = """
                UPDATE imposition_specifications 
                SET status = ?, notes = COALESCE(?, notes), version = version + 1
                WHERE tenant_id = ? AND imposition_id = ?
            """.trimIndent()
            val updated = conn.prepareStatement(updateSql).use { ps ->
                ps.setString(1, status)
                ps.setString(2, notes)
                ps.setString(3, tenantId)
                ps.setString(4, impositionId)
                ps.executeUpdate() > 0
            }

            if (updated) {
                val auditSql = """
                    INSERT INTO imposition_audit_events (
                        event_id, imposition_id, tenant_id, action_type, previous_status, new_status, actor, details, created_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
                conn.prepareStatement(auditSql).use { ps ->
                    ps.setString(1, "EVT-${UUID.randomUUID().toString().take(12)}")
                    ps.setString(2, impositionId)
                    ps.setString(3, tenantId)
                    ps.setString(4, "IMPOSITION_STATUS_UPDATED")
                    ps.setString(5, prevStatus)
                    ps.setString(6, status)
                    ps.setString(7, actor)
                    ps.setString(8, notes ?: "Status changed from $prevStatus to $status")
                    ps.setLong(9, System.currentTimeMillis())
                    ps.executeUpdate()
                }
            }

            updated
        }
    }

    private fun mapRow(rs: ResultSet): ImpositionSpecification {
        val itemDim = PrintingDimension(
            width = rs.getBigDecimal("item_width_mm"),
            height = rs.getBigDecimal("item_height_mm"),
            unit = MeasurementUnit.MILLIMETERS
        )
        val sheetDim = PrintingDimension(
            width = rs.getBigDecimal("sheet_width_mm"),
            height = rs.getBigDecimal("sheet_height_mm"),
            unit = MeasurementUnit.MILLIMETERS
        )
        val margins = ImpositionMarginSpec(
            topMm = rs.getBigDecimal("margin_top_mm"),
            bottomMm = rs.getBigDecimal("margin_bottom_mm"),
            leftMm = rs.getBigDecimal("margin_left_mm"),
            rightMm = rs.getBigDecimal("margin_right_mm")
        )
        val spacing = ImpositionSpacingSpec(
            bleedMm = rs.getBigDecimal("bleed_mm"),
            horizontalGutterMm = rs.getBigDecimal("horizontal_gutter_mm"),
            verticalGutterMm = rs.getBigDecimal("vertical_gutter_mm")
        )

        return ImpositionSpecification(
            impositionId = rs.getString("imposition_id"),
            tenantId = rs.getString("tenant_id"),
            jobId = rs.getString("job_id"),
            orderId = rs.getString("order_id"),
            orderItemId = rs.getString("order_item_id"),
            calculationId = rs.getString("calculation_id"),
            productName = rs.getString("product_name"),
            finishedItemDimension = itemDim,
            parentSheetDimension = sheetDim,
            marginSpec = margins,
            spacingSpec = spacing,
            orientationPolicy = try { ImpositionOrientationPolicy.valueOf(rs.getString("orientation_policy")) } catch (e: Exception) { ImpositionOrientationPolicy.AUTO_OPTIMAL },
            selectedOrientation = try { ImpositionLayoutOrientation.valueOf(rs.getString("selected_orientation")) } catch (e: Exception) { ImpositionLayoutOrientation.STANDARD },
            columns = rs.getInt("columns_count"),
            rows = rs.getInt("rows_count"),
            copiesPerSheet = rs.getInt("copies_per_sheet"),
            requiredQuantity = rs.getLong("required_quantity"),
            requiredSheets = rs.getLong("required_sheets"),
            totalProducedCapacity = rs.getLong("total_produced_capacity"),
            overageQuantity = rs.getLong("overage_quantity"),
            usableWidthMm = rs.getBigDecimal("usable_width_mm"),
            usableHeightMm = rs.getBigDecimal("usable_height_mm"),
            occupiedAreaMm2 = rs.getBigDecimal("occupied_area_mm2"),
            usableAreaMm2 = rs.getBigDecimal("usable_area_mm2"),
            wasteAreaMm2 = rs.getBigDecimal("waste_area_mm2"),
            yieldPercentage = rs.getBigDecimal("yield_percentage"),
            version = rs.getInt("version"),
            status = try { ImpositionStatus.valueOf(rs.getString("status")) } catch (e: Exception) { ImpositionStatus.OPTIMIZED },
            integrityHash = rs.getString("integrity_hash"),
            notes = rs.getString("notes"),
            candidateBreakdown = emptyList(),
            createdAt = rs.getLong("created_at"),
            createdBy = rs.getString("created_by")
        )
    }
}
