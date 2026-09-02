package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.datasource.imposition.CtpOutputDataSource
import com.sucharu.sucharupro.domain.model.imposition.*
import java.math.BigDecimal
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

/**
 * PostgreSQL Implementation for CTP Prepress Output Specifications with RLS and Audit Trail.
 * Module 18 Step 05.
 */
class PostgresCtpOutputDataSource(
    private val transactionManager: TransactionManager
) : CtpOutputDataSource {

    override suspend fun save(specification: CtpOutputSpecification): CtpOutputSpecification {
        return transactionManager.inTransaction(TenantContext(specification.tenantId)) { ctx ->
            val conn = ctx.connection

            val insertSpecSql = """
                INSERT INTO ctp_output_specifications (
                    ctp_output_id, tenant_id, name, job_id, order_id, order_item_id,
                    product_name, source_imposition_type, source_imposition_id, source_imposition_hash,
                    status, package_version, resolution_dpi, screening_method, default_screen_ruling_lpi,
                    plate_width_mm, plate_height_mm, plate_thickness_mm,
                    gripper_margin_mm, tail_margin_mm, side_guide_margin_left_mm, side_guide_margin_right_mm,
                    total_plates_count, front_plates_count, back_plates_count, spot_colors_count,
                    press_sheet_width_mm, press_sheet_height_mm, rip_instructions, validation_summary,
                    integrity_hash, notes, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (ctp_output_id) DO UPDATE SET
                    package_version = EXCLUDED.package_version,
                    status = EXCLUDED.status,
                    notes = EXCLUDED.notes,
                    updated_at = EXCLUDED.updated_at
            """.trimIndent()

            conn.prepareStatement(insertSpecSql).use { ps ->
                var idx = 1
                ps.setString(idx++, specification.ctpOutputId)
                ps.setString(idx++, specification.tenantId)
                ps.setString(idx++, specification.name)
                ps.setString(idx++, specification.jobId)
                ps.setString(idx++, specification.orderId)
                ps.setString(idx++, specification.orderItemId)
                ps.setString(idx++, specification.productName)
                ps.setString(idx++, specification.sourceImpositionType)
                ps.setString(idx++, specification.sourceImpositionId)
                ps.setString(idx++, specification.sourceImpositionHash)
                ps.setString(idx++, specification.status.name)
                ps.setInt(idx++, specification.packageVersion)
                ps.setInt(idx++, specification.resolutionDpi.dpi)
                ps.setString(idx++, specification.screeningMethod.name)
                ps.setBigDecimal(idx++, specification.defaultScreenRulingLpi)
                ps.setBigDecimal(idx++, specification.plateDimensionSpec.plateWidthMm)
                ps.setBigDecimal(idx++, specification.plateDimensionSpec.plateHeightMm)
                ps.setBigDecimal(idx++, specification.plateDimensionSpec.plateThicknessMm)
                ps.setBigDecimal(idx++, specification.plateDimensionSpec.gripperMarginMm)
                ps.setBigDecimal(idx++, specification.plateDimensionSpec.tailMarginMm)
                ps.setBigDecimal(idx++, specification.plateDimensionSpec.sideGuideMarginLeftMm)
                ps.setBigDecimal(idx++, specification.plateDimensionSpec.sideGuideMarginRightMm)
                ps.setInt(idx++, specification.outputPackage.totalPlatesCount)
                ps.setInt(idx++, specification.outputPackage.frontPlatesCount)
                ps.setInt(idx++, specification.outputPackage.backPlatesCount)
                ps.setInt(idx++, specification.outputPackage.spotColorsCount)
                ps.setBigDecimal(idx++, specification.outputPackage.pressSheetWidthMm)
                ps.setBigDecimal(idx++, specification.outputPackage.pressSheetHeightMm)
                ps.setString(idx++, specification.outputPackage.ripInstructions)
                ps.setString(idx++, specification.outputPackage.validationSummary)
                ps.setString(idx++, specification.integrityHash)
                ps.setString(idx++, specification.notes)
                ps.setTimestamp(idx++, Timestamp.from(specification.createdAt))
                ps.setTimestamp(idx++, Timestamp.from(specification.updatedAt))
                ps.executeUpdate()
            }

            // Insert Plates
            val insertPlateSql = """
                INSERT INTO ctp_output_plates (
                    plate_id, ctp_output_id, tenant_id, plate_name, form_reference_id, signature_number,
                    plate_side, color_separation, spot_color_name, plate_width_mm, plate_height_mm,
                    plate_thickness_mm, resolution_dpi, screening_method, screen_ruling_lpi,
                    screen_angle_degrees, dot_shape, sheet_offset_x_mm, sheet_offset_y_mm,
                    plate_area_mm2, plate_integrity_hash, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (plate_id) DO NOTHING
            """.trimIndent()

            conn.prepareStatement(insertPlateSql).use { ps ->
                for (plate in specification.outputPackage.plates) {
                    var idx = 1
                    ps.setString(idx++, plate.plateId)
                    ps.setString(idx++, specification.ctpOutputId)
                    ps.setString(idx++, specification.tenantId)
                    ps.setString(idx++, plate.plateName)
                    ps.setString(idx++, plate.formReferenceId)
                    ps.setInt(idx++, plate.signatureNumber)
                    ps.setString(idx++, plate.plateSide.name)
                    ps.setString(idx++, plate.colorSeparation.name)
                    ps.setString(idx++, plate.spotColorName)
                    ps.setBigDecimal(idx++, plate.plateWidthMm)
                    ps.setBigDecimal(idx++, plate.plateHeightMm)
                    ps.setBigDecimal(idx++, plate.plateThicknessMm)
                    ps.setInt(idx++, plate.resolutionDpi.dpi)
                    ps.setString(idx++, plate.screeningMethod.name)
                    ps.setBigDecimal(idx++, plate.screenRulingLpi)
                    ps.setBigDecimal(idx++, plate.screenAngleDegrees)
                    ps.setString(idx++, plate.dotShape)
                    ps.setBigDecimal(idx++, plate.sheetOffsetXMm)
                    ps.setBigDecimal(idx++, plate.sheetOffsetYMm)
                    ps.setBigDecimal(idx++, plate.plateAreaMm2)
                    ps.setString(idx++, plate.plateIntegrityHash)
                    ps.setTimestamp(idx++, Timestamp.from(specification.createdAt))
                    ps.addBatch()
                }
                ps.executeBatch()
            }

            // Insert Marks
            val insertMarkSql = """
                INSERT INTO ctp_prepress_marks (
                    mark_id, ctp_output_id, tenant_id, mark_type, plate_side,
                    x_position_mm, y_position_mm, width_mm, height_mm,
                    rotation_degrees, label_text, target_color_separation, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (mark_id) DO NOTHING
            """.trimIndent()

            conn.prepareStatement(insertMarkSql).use { ps ->
                for (mark in specification.outputPackage.globalMarks) {
                    var idx = 1
                    ps.setString(idx++, mark.markId)
                    ps.setString(idx++, specification.ctpOutputId)
                    ps.setString(idx++, specification.tenantId)
                    ps.setString(idx++, mark.markType.name)
                    ps.setString(idx++, mark.plateSide.name)
                    ps.setBigDecimal(idx++, mark.xPositionMm)
                    ps.setBigDecimal(idx++, mark.yPositionMm)
                    ps.setBigDecimal(idx++, mark.widthMm)
                    ps.setBigDecimal(idx++, mark.heightMm)
                    ps.setBigDecimal(idx++, mark.rotationDegrees)
                    ps.setString(idx++, mark.labelText)
                    ps.setString(idx++, mark.targetColorSeparation?.name)
                    ps.setTimestamp(idx++, Timestamp.from(specification.createdAt))
                    ps.addBatch()
                }
                ps.executeBatch()
            }

            // Record Audit Trail
            val insertAuditSql = """
                INSERT INTO ctp_output_audits (audit_id, ctp_output_id, tenant_id, actor, action, details, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            conn.prepareStatement(insertAuditSql).use { ps ->
                var idx = 1
                ps.setString(idx++, "AUD-${UUID.randomUUID().toString().take(8).uppercase()}")
                ps.setString(idx++, specification.ctpOutputId)
                ps.setString(idx++, specification.tenantId)
                ps.setString(idx++, "system")
                ps.setString(idx++, "CTP_OUTPUT_SAVED")
                ps.setString(idx++, "Saved CTP specification with ${specification.outputPackage.totalPlatesCount} plates and version ${specification.packageVersion}")
                ps.setTimestamp(idx++, Timestamp.from(Instant.now()))
                ps.executeUpdate()
            }

            specification
        }
    }

    override suspend fun findById(tenantId: String, ctpOutputId: String): CtpOutputSpecification? {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection

            val specSql = "SELECT * FROM ctp_output_specifications WHERE tenant_id = ? AND ctp_output_id = ?"
            var spec: CtpOutputSpecification? = null

            conn.prepareStatement(specSql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, ctpOutputId)
                ps.executeQuery().use { rs ->
                    if (rs.next()) {
                        spec = mapSpecification(rs, conn)
                    }
                }
            }
            spec
        }
    }

    override suspend fun findByJobId(tenantId: String, jobId: String): List<CtpOutputSpecification> {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val list = mutableListOf<CtpOutputSpecification>()

            val sql = "SELECT * FROM ctp_output_specifications WHERE tenant_id = ? AND job_id = ? ORDER BY created_at DESC"
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, jobId)
                ps.executeQuery().use { rs ->
                    while (rs.next()) {
                        list.add(mapSpecification(rs, conn))
                    }
                }
            }
            list
        }
    }

    override suspend fun findBySourceImpositionId(tenantId: String, sourceImpositionId: String): List<CtpOutputSpecification> {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val list = mutableListOf<CtpOutputSpecification>()

            val sql = "SELECT * FROM ctp_output_specifications WHERE tenant_id = ? AND source_imposition_id = ? ORDER BY created_at DESC"
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, sourceImpositionId)
                ps.executeQuery().use { rs ->
                    while (rs.next()) {
                        list.add(mapSpecification(rs, conn))
                    }
                }
            }
            list
        }
    }

    override suspend fun listAll(tenantId: String): List<CtpOutputSpecification> {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val list = mutableListOf<CtpOutputSpecification>()

            val sql = "SELECT * FROM ctp_output_specifications WHERE tenant_id = ? ORDER BY created_at DESC"
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.executeQuery().use { rs ->
                    while (rs.next()) {
                        list.add(mapSpecification(rs, conn))
                    }
                }
            }
            list
        }
    }

    override suspend fun updateStatus(
        tenantId: String,
        ctpOutputId: String,
        newStatus: String,
        actor: String,
        reason: String?
    ): CtpOutputSpecification? {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection

            val updateSql = """
                UPDATE ctp_output_specifications
                SET status = ?, updated_at = NOW(), notes = COALESCE(notes || E'\n', '') || ?
                WHERE tenant_id = ? AND ctp_output_id = ?
            """.trimIndent()

            val statusNote = "Status updated to $newStatus by $actor" + (if (reason != null) ": $reason" else "")

            conn.prepareStatement(updateSql).use { ps ->
                ps.setString(1, newStatus)
                ps.setString(2, statusNote)
                ps.setString(3, tenantId)
                ps.setString(4, ctpOutputId)
                ps.executeUpdate()
            }

            val insertAuditSql = """
                INSERT INTO ctp_output_audits (audit_id, ctp_output_id, tenant_id, actor, action, details, created_at)
                VALUES (?, ?, ?, ?, ?, ?, NOW())
            """.trimIndent()
            conn.prepareStatement(insertAuditSql).use { ps ->
                ps.setString(1, "AUD-${UUID.randomUUID().toString().take(8).uppercase()}")
                ps.setString(2, ctpOutputId)
                ps.setString(3, tenantId)
                ps.setString(4, actor)
                ps.setString(5, "STATUS_CHANGE_$newStatus")
                ps.setString(6, reason ?: "Status updated to $newStatus")
                ps.executeUpdate()
            }

            findById(tenantId, ctpOutputId)
        }
    }

    private fun mapSpecification(rs: ResultSet, conn: java.sql.Connection): CtpOutputSpecification {
        val ctpOutputId = rs.getString("ctp_output_id")
        val tenantId = rs.getString("tenant_id")

        val plateDimensionSpec = PlateDimensionSpec(
            plateWidthMm = rs.getBigDecimal("plate_width_mm"),
            plateHeightMm = rs.getBigDecimal("plate_height_mm"),
            plateThicknessMm = rs.getBigDecimal("plate_thickness_mm"),
            gripperMarginMm = rs.getBigDecimal("gripper_margin_mm"),
            tailMarginMm = rs.getBigDecimal("tail_margin_mm"),
            sideGuideMarginLeftMm = rs.getBigDecimal("side_guide_margin_left_mm"),
            sideGuideMarginRightMm = rs.getBigDecimal("side_guide_margin_right_mm")
        )

        val resolutionDpi = when (rs.getInt("resolution_dpi")) {
            1200 -> OutputResolutionDpi.DPI_1200
            2400 -> OutputResolutionDpi.DPI_2400
            4000 -> OutputResolutionDpi.DPI_4000
            else -> OutputResolutionDpi.DPI_2540
        }

        val screeningMethod = ScreeningMethod.valueOf(rs.getString("screening_method"))
        val defaultScreenRulingLpi = rs.getBigDecimal("default_screen_ruling_lpi")

        // Fetch Plates
        val plates = mutableListOf<PlateSpecification>()
        val platesSql = "SELECT * FROM ctp_output_plates WHERE ctp_output_id = ? AND tenant_id = ? ORDER BY signature_number ASC, plate_side ASC, color_separation ASC"
        conn.prepareStatement(platesSql).use { ps ->
            ps.setString(1, ctpOutputId)
            ps.setString(2, tenantId)
            ps.executeQuery().use { prs ->
                while (prs.next()) {
                    plates.add(
                        PlateSpecification(
                            plateId = prs.getString("plate_id"),
                            plateName = prs.getString("plate_name"),
                            formReferenceId = prs.getString("form_reference_id"),
                            signatureNumber = prs.getInt("signature_number"),
                            plateSide = PlateSide.valueOf(prs.getString("plate_side")),
                            colorSeparation = PlateColorSeparation.valueOf(prs.getString("color_separation")),
                            spotColorName = prs.getString("spot_color_name"),
                            plateWidthMm = prs.getBigDecimal("plate_width_mm"),
                            plateHeightMm = prs.getBigDecimal("plate_height_mm"),
                            plateThicknessMm = prs.getBigDecimal("plate_thickness_mm"),
                            resolutionDpi = resolutionDpi,
                            screeningMethod = screeningMethod,
                            screenRulingLpi = prs.getBigDecimal("screen_ruling_lpi"),
                            screenAngleDegrees = prs.getBigDecimal("screen_angle_degrees"),
                            dotShape = prs.getString("dot_shape"),
                            sheetOffsetXMm = prs.getBigDecimal("sheet_offset_x_mm"),
                            sheetOffsetYMm = prs.getBigDecimal("sheet_offset_y_mm"),
                            marks = emptyList(),
                            plateAreaMm2 = prs.getBigDecimal("plate_area_mm2"),
                            plateIntegrityHash = prs.getString("plate_integrity_hash")
                        )
                    )
                }
            }
        }

        // Fetch Marks
        val marks = mutableListOf<PrepressMarkPlacement>()
        val marksSql = "SELECT * FROM ctp_prepress_marks WHERE ctp_output_id = ? AND tenant_id = ?"
        conn.prepareStatement(marksSql).use { ps ->
            ps.setString(1, ctpOutputId)
            ps.setString(2, tenantId)
            ps.executeQuery().use { mrs ->
                while (mrs.next()) {
                    val sepStr = mrs.getString("target_color_separation")
                    marks.add(
                        PrepressMarkPlacement(
                            markId = mrs.getString("mark_id"),
                            markType = PrepressMarkType.valueOf(mrs.getString("mark_type")),
                            plateSide = PlateSide.valueOf(mrs.getString("plate_side")),
                            xPositionMm = mrs.getBigDecimal("x_position_mm"),
                            yPositionMm = mrs.getBigDecimal("y_position_mm"),
                            widthMm = mrs.getBigDecimal("width_mm"),
                            heightMm = mrs.getBigDecimal("height_mm"),
                            rotationDegrees = mrs.getBigDecimal("rotation_degrees"),
                            labelText = mrs.getString("label_text"),
                            targetColorSeparation = if (sepStr != null) PlateColorSeparation.valueOf(sepStr) else null
                        )
                    )
                }
            }
        }

        val outputPackage = CtpOutputPackage(
            packageId = "PKG-$ctpOutputId",
            packageVersion = rs.getInt("package_version"),
            sourceImpositionType = rs.getString("source_imposition_type"),
            sourceImpositionId = rs.getString("source_imposition_id"),
            sourceIntegrityHash = rs.getString("source_imposition_hash"),
            totalPlatesCount = rs.getInt("total_plates_count"),
            frontPlatesCount = rs.getInt("front_plates_count"),
            backPlatesCount = rs.getInt("back_plates_count"),
            spotColorsCount = rs.getInt("spot_colors_count"),
            pressSheetWidthMm = rs.getBigDecimal("press_sheet_width_mm"),
            pressSheetHeightMm = rs.getBigDecimal("press_sheet_height_mm"),
            plateWidthMm = plateDimensionSpec.plateWidthMm,
            plateHeightMm = plateDimensionSpec.plateHeightMm,
            gripperMarginMm = plateDimensionSpec.gripperMarginMm,
            tailMarginMm = plateDimensionSpec.tailMarginMm,
            sideGuideMarginLeftMm = plateDimensionSpec.sideGuideMarginLeftMm,
            sideGuideMarginRightMm = plateDimensionSpec.sideGuideMarginRightMm,
            plates = plates,
            globalMarks = marks,
            ripInstructions = rs.getString("rip_instructions"),
            validationSummary = rs.getString("validation_summary"),
            integrityHash = rs.getString("integrity_hash")
        )

        return CtpOutputSpecification(
            ctpOutputId = ctpOutputId,
            tenantId = tenantId,
            name = rs.getString("name"),
            jobId = rs.getString("job_id"),
            orderId = rs.getString("order_id"),
            orderItemId = rs.getString("order_item_id"),
            productName = rs.getString("product_name"),
            sourceImpositionType = rs.getString("source_imposition_type"),
            sourceImpositionId = rs.getString("source_imposition_id"),
            sourceImpositionHash = rs.getString("source_imposition_hash"),
            status = CtpOutputStatus.valueOf(rs.getString("status")),
            packageVersion = rs.getInt("package_version"),
            resolutionDpi = resolutionDpi,
            screeningMethod = screeningMethod,
            defaultScreenRulingLpi = defaultScreenRulingLpi,
            markPolicy = PrepressMarkPolicy(),
            plateDimensionSpec = plateDimensionSpec,
            outputPackage = outputPackage,
            integrityHash = rs.getString("integrity_hash"),
            notes = rs.getString("notes"),
            createdAt = rs.getTimestamp("created_at").toInstant(),
            updatedAt = rs.getTimestamp("updated_at").toInstant()
        )
    }
}
