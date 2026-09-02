package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.datasource.imposition.SignatureImpositionDataSource
import com.sucharu.sucharupro.domain.model.imposition.*
import com.sucharu.sucharupro.domain.model.printingcalculator.*
import java.math.BigDecimal
import java.sql.ResultSet
import java.util.UUID

/**
 * PostgreSQL Implementation for Signature Imposition Specifications with TransactionManager and RLS.
 * Module 18 Step 04.
 */
class PostgresSignatureImpositionDataSource(
    private val transactionManager: TransactionManager
) : SignatureImpositionDataSource {

    override suspend fun saveSpecification(
        tenantId: String,
        specification: SignatureImpositionSpecification
    ): SignatureImpositionSpecification {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection

            val insertSpecSql = """
                INSERT INTO signature_imposition_specifications (
                    signature_imposition_id, tenant_id, name, job_id, order_id, order_item_id,
                    product_name, total_pages, padded_total_pages, signature_page_count,
                    total_signatures_count, binding_method, sheet_turning_method, folding_scheme,
                    paper_stock_type, gsm, page_width_mm, page_height_mm,
                    parent_sheet_width_mm, parent_sheet_height_mm,
                    margin_top_mm, margin_bottom_mm, margin_left_mm, margin_right_mm,
                    spine_gutter_mm, head_gutter_mm, foot_gutter_mm, face_trim_mm, bleed_mm,
                    creep_is_enabled, paper_caliper_mm, total_creep_mm, creep_per_sheet_mm, innermost_page_shift_mm,
                    common_required_sheets, total_parent_sheets_required, total_produced_copies, overage_copies,
                    total_sheet_area_mm2, usable_area_mm2, occupied_area_mm2, waste_area_mm2,
                    sheet_utilization_percentage, usable_yield_percentage,
                    version, status, integrity_hash, notes, created_at, created_by
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (signature_imposition_id) DO UPDATE SET
                    version = signature_imposition_specifications.version + 1,
                    status = EXCLUDED.status,
                    notes = EXCLUDED.notes
            """.trimIndent()

            conn.prepareStatement(insertSpecSql).use { ps ->
                var idx = 1
                ps.setString(idx++, specification.signatureImpositionId)
                ps.setString(idx++, specification.tenantId)
                ps.setString(idx++, specification.name)
                ps.setString(idx++, specification.jobId)
                ps.setString(idx++, specification.orderId)
                ps.setString(idx++, specification.orderItemId)
                ps.setString(idx++, specification.productName)
                ps.setInt(idx++, specification.totalPages)
                ps.setInt(idx++, specification.paddedTotalPages)
                ps.setInt(idx++, specification.signaturePageCount)
                ps.setInt(idx++, specification.totalSignaturesCount)
                ps.setString(idx++, specification.bindingMethod.name)
                ps.setString(idx++, specification.sheetTurningMethod.name)
                ps.setString(idx++, specification.foldingScheme.name)
                ps.setString(idx++, specification.paperStockType.name)
                ps.setBigDecimal(idx++, specification.gsm)
                ps.setBigDecimal(idx++, specification.pageDimension.width)
                ps.setBigDecimal(idx++, specification.pageDimension.height)
                ps.setBigDecimal(idx++, specification.parentSheetDimension.width)
                ps.setBigDecimal(idx++, specification.parentSheetDimension.height)
                ps.setBigDecimal(idx++, specification.marginSpec.topMm)
                ps.setBigDecimal(idx++, specification.marginSpec.bottomMm)
                ps.setBigDecimal(idx++, specification.marginSpec.leftMm)
                ps.setBigDecimal(idx++, specification.marginSpec.rightMm)
                ps.setBigDecimal(idx++, specification.gutterSpec.spineGutterMm)
                ps.setBigDecimal(idx++, specification.gutterSpec.headGutterMm)
                ps.setBigDecimal(idx++, specification.gutterSpec.footGutterMm)
                ps.setBigDecimal(idx++, specification.gutterSpec.faceTrimMm)
                ps.setBigDecimal(idx++, specification.gutterSpec.bleedMm)
                ps.setBoolean(idx++, specification.creepSummary.isEnabled)
                ps.setBigDecimal(idx++, specification.creepSummary.paperCaliperMm)
                ps.setBigDecimal(idx++, specification.creepSummary.totalCreepMm)
                ps.setBigDecimal(idx++, specification.creepSummary.creepPerSheetMm)
                ps.setBigDecimal(idx++, specification.creepSummary.innermostPageShiftMm)
                ps.setLong(idx++, specification.commonRequiredSheets)
                ps.setLong(idx++, specification.totalParentSheetsRequired)
                ps.setLong(idx++, specification.totalProducedCopies)
                ps.setLong(idx++, specification.overageCopies)
                ps.setBigDecimal(idx++, specification.totalSheetAreaMm2)
                ps.setBigDecimal(idx++, specification.usableAreaMm2)
                ps.setBigDecimal(idx++, specification.occupiedAreaMm2)
                ps.setBigDecimal(idx++, specification.wasteAreaMm2)
                ps.setBigDecimal(idx++, specification.sheetUtilizationPercentage)
                ps.setBigDecimal(idx++, specification.usableYieldPercentage)
                ps.setInt(idx++, specification.version)
                ps.setString(idx++, specification.status.name)
                ps.setString(idx++, specification.integrityHash)
                ps.setString(idx++, specification.notes)
                ps.setLong(idx++, specification.createdAt)
                ps.setString(idx++, specification.createdBy)
                ps.executeUpdate()
            }

            // Clean old child records
            conn.prepareStatement("DELETE FROM signature_forms WHERE signature_imposition_id = ?").use { ps ->
                ps.setString(1, specification.signatureImpositionId)
                ps.executeUpdate()
            }

            // Insert Signature Forms & Page Allocations
            val insertFormSql = """
                INSERT INTO signature_forms (
                    form_id, signature_imposition_id, tenant_id, signature_number,
                    form_side, pages_per_side, columns, rows,
                    form_sheet_width_mm, form_sheet_height_mm,
                    occupied_area_mm2, usable_area_mm2, yield_percentage
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()

            val insertPageSql = """
                INSERT INTO signature_page_allocations (
                    placement_id, form_id, signature_imposition_id, tenant_id,
                    page_number, slot_index, grid_row, grid_column,
                    x_mm, y_mm, width_mm, height_mm,
                    head_orientation, creep_shift_x_mm, creep_shift_y_mm, is_blank_page
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()

            conn.prepareStatement(insertFormSql).use { psForm ->
                conn.prepareStatement(insertPageSql).use { psPage ->
                    for (form in specification.signatureForms) {
                        var fIdx = 1
                        psForm.setString(fIdx++, form.formId)
                        psForm.setString(fIdx++, specification.signatureImpositionId)
                        psForm.setString(fIdx++, specification.tenantId)
                        psForm.setInt(fIdx++, form.signatureNumber)
                        psForm.setString(fIdx++, form.formSide.name)
                        psForm.setInt(fIdx++, form.pagesPerSide)
                        psForm.setInt(fIdx++, form.columns)
                        psForm.setInt(fIdx++, form.rows)
                        psForm.setBigDecimal(fIdx++, form.formSheetWidthMm)
                        psForm.setBigDecimal(fIdx++, form.formSheetHeightMm)
                        psForm.setBigDecimal(fIdx++, form.occupiedAreaMm2)
                        psForm.setBigDecimal(fIdx++, form.usableAreaMm2)
                        psForm.setBigDecimal(fIdx++, form.yieldPercentage)
                        psForm.executeUpdate()

                        for (p in form.pagePlacements) {
                            var pIdx = 1
                            psPage.setString(pIdx++, p.placementId)
                            psPage.setString(pIdx++, form.formId)
                            psPage.setString(pIdx++, specification.signatureImpositionId)
                            psPage.setString(pIdx++, specification.tenantId)
                            psPage.setInt(pIdx++, p.pageNumber)
                            psPage.setInt(pIdx++, p.slotIndex)
                            psPage.setInt(pIdx++, p.row)
                            psPage.setInt(pIdx++, p.column)
                            psPage.setBigDecimal(pIdx++, p.xMm)
                            psPage.setBigDecimal(pIdx++, p.yMm)
                            psPage.setBigDecimal(pIdx++, p.widthMm)
                            psPage.setBigDecimal(pIdx++, p.heightMm)
                            psPage.setString(pIdx++, p.headOrientation.name)
                            psPage.setBigDecimal(pIdx++, p.creepShiftXMm)
                            psPage.setBigDecimal(pIdx++, p.creepShiftYMm)
                            psPage.setBoolean(pIdx++, p.isBlankPage)
                            psPage.addBatch()
                        }
                        psPage.executeBatch()
                    }
                }
            }

            // Insert audit record
            val auditSql = """
                INSERT INTO signature_imposition_audit_events (
                    event_id, signature_imposition_id, tenant_id, action,
                    from_status, to_status, actor, notes, timestamp
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            conn.prepareStatement(auditSql).use { ps ->
                ps.setString(1, "AUDIT-${UUID.randomUUID().toString().take(8).uppercase()}")
                ps.setString(2, specification.signatureImpositionId)
                ps.setString(3, specification.tenantId)
                ps.setString(4, "OPTIMIZE_SIGNATURE_IMPOSITION")
                ps.setString(5, null)
                ps.setString(6, specification.status.name)
                ps.setString(7, specification.createdBy)
                ps.setString(8, specification.notes)
                ps.setLong(9, System.currentTimeMillis())
                ps.executeUpdate()
            }

            specification
        }
    }

    override suspend fun getSpecificationById(
        tenantId: String,
        signatureImpositionId: String
    ): SignatureImpositionSpecification? {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection

            val specSql = "SELECT * FROM signature_imposition_specifications WHERE signature_imposition_id = ?"
            val spec = conn.prepareStatement(specSql).use { ps ->
                ps.setString(1, signatureImpositionId)
                ps.executeQuery().use { rs ->
                    if (rs.next()) mapSpecificationRow(rs) else null
                }
            } ?: return@inTransaction null

            // Query forms
            val formsSql = "SELECT * FROM signature_forms WHERE signature_imposition_id = ? ORDER BY signature_number ASC, form_side ASC"
            val forms = conn.prepareStatement(formsSql).use { ps ->
                ps.setString(1, signatureImpositionId)
                ps.executeQuery().use { rs ->
                    val list = mutableListOf<SignatureForm>()
                    while (rs.next()) {
                        list.add(mapFormRow(rs, conn))
                    }
                    list
                }
            }

            spec.copy(signatureForms = forms)
        }
    }

    override suspend fun listSpecifications(tenantId: String): List<SignatureImpositionSpecification> {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM signature_imposition_specifications ORDER BY created_at DESC"
            conn.prepareStatement(sql).use { ps ->
                ps.executeQuery().use { rs ->
                    val list = mutableListOf<SignatureImpositionSpecification>()
                    while (rs.next()) {
                        list.add(mapSpecificationRow(rs))
                    }
                    list
                }
            }
        }
    }

    override suspend fun listSpecificationsByJob(
        tenantId: String,
        jobId: String
    ): List<SignatureImpositionSpecification> {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM signature_imposition_specifications WHERE job_id = ? ORDER BY created_at DESC"
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, jobId)
                ps.executeQuery().use { rs ->
                    val list = mutableListOf<SignatureImpositionSpecification>()
                    while (rs.next()) {
                        list.add(mapSpecificationRow(rs))
                    }
                    list
                }
            }
        }
    }

    override suspend fun updateStatus(
        tenantId: String,
        signatureImpositionId: String,
        status: SignatureStatus,
        actor: String,
        notes: String?
    ): Boolean {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection

            val currentStatusSql = "SELECT status FROM signature_imposition_specifications WHERE signature_imposition_id = ?"
            val currentStatus = conn.prepareStatement(currentStatusSql).use { ps ->
                ps.setString(1, signatureImpositionId)
                ps.executeQuery().use { rs ->
                    if (rs.next()) rs.getString("status") else null
                }
            } ?: return@inTransaction false

            val updateSql = """
                UPDATE signature_imposition_specifications
                SET status = ?, notes = COALESCE(?, notes), version = version + 1
                WHERE signature_imposition_id = ?
            """.trimIndent()

            val rowsUpdated = conn.prepareStatement(updateSql).use { ps ->
                ps.setString(1, status.name)
                ps.setString(2, notes)
                ps.setString(3, signatureImpositionId)
                ps.executeUpdate()
            }

            if (rowsUpdated > 0) {
                val auditSql = """
                    INSERT INTO signature_imposition_audit_events (
                        event_id, signature_imposition_id, tenant_id, action,
                        from_status, to_status, actor, notes, timestamp
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
                conn.prepareStatement(auditSql).use { ps ->
                    ps.setString(1, "AUDIT-${UUID.randomUUID().toString().take(8).uppercase()}")
                    ps.setString(2, signatureImpositionId)
                    ps.setString(3, tenantId)
                    ps.setString(4, "UPDATE_SIGNATURE_STATUS")
                    ps.setString(5, currentStatus)
                    ps.setString(6, status.name)
                    ps.setString(7, actor)
                    ps.setString(8, notes)
                    ps.setLong(9, System.currentTimeMillis())
                    ps.executeUpdate()
                }
                true
            } else {
                false
            }
        }
    }

    private fun mapSpecificationRow(rs: ResultSet): SignatureImpositionSpecification {
        return SignatureImpositionSpecification(
            signatureImpositionId = rs.getString("signature_imposition_id"),
            tenantId = rs.getString("tenant_id"),
            name = rs.getString("name"),
            jobId = rs.getString("job_id"),
            orderId = rs.getString("order_id"),
            orderItemId = rs.getString("order_item_id"),
            productName = rs.getString("product_name"),
            totalPages = rs.getInt("total_pages"),
            paddedTotalPages = rs.getInt("padded_total_pages"),
            signaturePageCount = rs.getInt("signature_page_count"),
            totalSignaturesCount = rs.getInt("total_signatures_count"),
            bindingMethod = BindingMethod.valueOf(rs.getString("binding_method")),
            sheetTurningMethod = SheetTurningMethod.valueOf(rs.getString("sheet_turning_method")),
            foldingScheme = FoldingScheme.valueOf(rs.getString("folding_scheme")),
            paperStockType = PaperStockType.valueOf(rs.getString("paper_stock_type")),
            gsm = rs.getBigDecimal("gsm"),
            pageDimension = PrintingDimension(
                rs.getBigDecimal("page_width_mm"),
                rs.getBigDecimal("page_height_mm"),
                MeasurementUnit.MILLIMETERS
            ),
            parentSheetDimension = PrintingDimension(
                rs.getBigDecimal("parent_sheet_width_mm"),
                rs.getBigDecimal("parent_sheet_height_mm"),
                MeasurementUnit.MILLIMETERS
            ),
            marginSpec = ImpositionMarginSpec(
                topMm = rs.getBigDecimal("margin_top_mm"),
                bottomMm = rs.getBigDecimal("margin_bottom_mm"),
                leftMm = rs.getBigDecimal("margin_left_mm"),
                rightMm = rs.getBigDecimal("margin_right_mm")
            ),
            gutterSpec = SignatureGutterSpec(
                spineGutterMm = rs.getBigDecimal("spine_gutter_mm"),
                headGutterMm = rs.getBigDecimal("head_gutter_mm"),
                footGutterMm = rs.getBigDecimal("foot_gutter_mm"),
                faceTrimMm = rs.getBigDecimal("face_trim_mm"),
                bleedMm = rs.getBigDecimal("bleed_mm")
            ),
            creepSummary = CreepCompensationSummary(
                isEnabled = rs.getBoolean("creep_is_enabled"),
                paperCaliperMm = rs.getBigDecimal("paper_caliper_mm"),
                totalCreepMm = rs.getBigDecimal("total_creep_mm"),
                creepPerSheetMm = rs.getBigDecimal("creep_per_sheet_mm"),
                innermostPageShiftMm = rs.getBigDecimal("innermost_page_shift_mm")
            ),
            signatureForms = emptyList(),
            commonRequiredSheets = rs.getLong("common_required_sheets"),
            totalParentSheetsRequired = rs.getLong("total_parent_sheets_required"),
            totalProducedCopies = rs.getLong("total_produced_copies"),
            overageCopies = rs.getLong("overage_copies"),
            totalSheetAreaMm2 = rs.getBigDecimal("total_sheet_area_mm2"),
            usableAreaMm2 = rs.getBigDecimal("usable_area_mm2"),
            occupiedAreaMm2 = rs.getBigDecimal("occupied_area_mm2"),
            wasteAreaMm2 = rs.getBigDecimal("waste_area_mm2"),
            sheetUtilizationPercentage = rs.getBigDecimal("sheet_utilization_percentage"),
            usableYieldPercentage = rs.getBigDecimal("usable_yield_percentage"),
            version = rs.getInt("version"),
            status = SignatureStatus.valueOf(rs.getString("status")),
            integrityHash = rs.getString("integrity_hash"),
            notes = rs.getString("notes") ?: "",
            createdAt = rs.getLong("created_at"),
            createdBy = rs.getString("created_by")
        )
    }

    private fun mapFormRow(rs: ResultSet, conn: java.sql.Connection): SignatureForm {
        val formId = rs.getString("form_id")
        val pagesSql = "SELECT * FROM signature_page_allocations WHERE form_id = ? ORDER BY slot_index ASC"
        val pages = conn.prepareStatement(pagesSql).use { ps ->
            ps.setString(1, formId)
            ps.executeQuery().use { pRs ->
                val list = mutableListOf<SignaturePagePlacement>()
                while (pRs.next()) {
                    list.add(
                        SignaturePagePlacement(
                            placementId = pRs.getString("placement_id"),
                            pageNumber = pRs.getInt("page_number"),
                            slotIndex = pRs.getInt("slot_index"),
                            row = pRs.getInt("grid_row"),
                            column = pRs.getInt("grid_column"),
                            xMm = pRs.getBigDecimal("x_mm"),
                            yMm = pRs.getBigDecimal("y_mm"),
                            widthMm = pRs.getBigDecimal("width_mm"),
                            heightMm = pRs.getBigDecimal("height_mm"),
                            headOrientation = PageHeadOrientation.valueOf(pRs.getString("head_orientation")),
                            creepShiftXMm = pRs.getBigDecimal("creep_shift_x_mm"),
                            creepShiftYMm = pRs.getBigDecimal("creep_shift_y_mm"),
                            isBlankPage = pRs.getBoolean("is_blank_page")
                        )
                    )
                }
                list
            }
        }

        return SignatureForm(
            formId = formId,
            signatureNumber = rs.getInt("signature_number"),
            formSide = SignatureFormSide.valueOf(rs.getString("form_side")),
            pagesPerSide = rs.getInt("pages_per_side"),
            columns = rs.getInt("columns"),
            rows = rs.getInt("rows"),
            pagePlacements = pages,
            formSheetWidthMm = rs.getBigDecimal("form_sheet_width_mm"),
            formSheetHeightMm = rs.getBigDecimal("form_sheet_height_mm"),
            occupiedAreaMm2 = rs.getBigDecimal("occupied_area_mm2"),
            usableAreaMm2 = rs.getBigDecimal("usable_area_mm2"),
            yieldPercentage = rs.getBigDecimal("yield_percentage")
        )
    }
}
