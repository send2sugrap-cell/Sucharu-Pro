package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.datasource.productionplanning.ProductionPlanningDataSource
import com.sucharu.sucharupro.domain.model.production.ProductionStageType
import com.sucharu.sucharupro.domain.model.productionplanning.*
import java.sql.ResultSet

class PostgresProductionPlanningDataSource(
    private val transactionManager: TransactionManager
) : ProductionPlanningDataSource {

    override suspend fun savePlanningSnapshot(
        snapshot: ProductionPlanningSnapshot,
        idempotencyKey: String?
    ): ProductionPlanningSnapshot {
        return transactionManager.inTransaction(TenantContext(snapshot.projectId)) { ctx ->
            // If current, mark previous versions for this order as not current
            if (snapshot.isCurrent) {
                val updateCurrentSql = "UPDATE production_planning_snapshots SET is_current = FALSE WHERE tenant_id = ? AND order_id = ? AND planning_id <> ?"
                ctx.connection.prepareStatement(updateCurrentSql).use { s ->
                    s.setString(1, snapshot.tenantId)
                    s.setString(2, snapshot.orderId)
                    s.setString(3, snapshot.planningId)
                    s.executeUpdate()
                }
            }

            val insertSnapshotSql = """
                INSERT INTO production_planning_snapshots (
                    planning_id, tenant_id, project_id, order_id, order_number, order_item_id,
                    commercial_commitment_id, quotation_id, quotation_version_number, customer_id,
                    status, version, is_current, readiness_score, feasibility_status,
                    order_requested_date, estimated_completion_date, planning_fingerprint,
                    integrity_hash, idempotency_key, created_at, updated_at, created_by
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (planning_id) DO UPDATE SET
                    status = EXCLUDED.status,
                    version = EXCLUDED.version,
                    is_current = EXCLUDED.is_current,
                    readiness_score = EXCLUDED.readiness_score,
                    feasibility_status = EXCLUDED.feasibility_status,
                    updated_at = EXCLUDED.updated_at
            """.trimIndent()

            ctx.connection.prepareStatement(insertSnapshotSql).use { s ->
                s.setString(1, snapshot.planningId)
                s.setString(2, snapshot.tenantId)
                s.setString(3, snapshot.projectId)
                s.setString(4, snapshot.orderId)
                s.setString(5, snapshot.orderNumber)
                s.setString(6, snapshot.orderItemId)
                s.setString(7, snapshot.commercialCommitmentId)
                s.setString(8, snapshot.quotationId)
                s.setObject(9, snapshot.quotationVersionNumber)
                s.setString(10, snapshot.customerId)
                s.setString(11, snapshot.status.name)
                s.setInt(12, snapshot.version)
                s.setBoolean(13, snapshot.isCurrent)
                s.setBigDecimal(14, snapshot.readinessScore)
                s.setString(15, snapshot.feasibilityStatus.name)
                s.setObject(16, snapshot.orderRequestedDate)
                s.setObject(17, snapshot.estimatedCompletionDate)
                s.setString(18, snapshot.planningFingerprint)
                s.setString(19, snapshot.integrityHash)
                s.setString(20, idempotencyKey)
                s.setLong(21, snapshot.createdAt)
                s.setLong(22, snapshot.updatedAt)
                s.setString(23, snapshot.createdBy)
                s.executeUpdate()
            }

            // Insert Specification
            val spec = snapshot.specification
            val insertSpecSql = """
                INSERT INTO production_job_specifications (
                    spec_id, planning_id, tenant_id, job_title, product_type, ordered_quantity, planned_quantity,
                    finished_width_mm, finished_height_mm, substrate_type, substrate_gsm, substrate_brand,
                    parent_sheet_width_mm, parent_sheet_height_mm, press_sheet_width_mm, press_sheet_height_mm,
                    printing_method, colors_front, colors_back, coating_front, coating_back, imposition_ups,
                    lamination, binding_method, folding_type, cutting_required, die_cutting_required,
                    packaging_method, artwork_url, special_instructions, spec_fingerprint
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (spec_id) DO NOTHING
            """.trimIndent()

            ctx.connection.prepareStatement(insertSpecSql).use { s ->
                s.setString(1, spec.specId)
                s.setString(2, snapshot.planningId)
                s.setString(3, snapshot.tenantId)
                s.setString(4, spec.jobTitle)
                s.setString(5, spec.productType)
                s.setLong(6, spec.orderedQuantity)
                s.setLong(7, spec.plannedQuantity)
                s.setBigDecimal(8, spec.finishedWidthMm)
                s.setBigDecimal(9, spec.finishedHeightMm)
                s.setString(10, spec.substrateType)
                s.setInt(11, spec.substrateGsm)
                s.setString(12, spec.substrateBrand)
                s.setBigDecimal(13, spec.parentSheetWidthMm)
                s.setBigDecimal(14, spec.parentSheetHeightMm)
                s.setBigDecimal(15, spec.pressSheetWidthMm)
                s.setBigDecimal(16, spec.pressSheetHeightMm)
                s.setString(17, spec.printingMethod)
                s.setInt(18, spec.colorsFront)
                s.setInt(19, spec.colorsBack)
                s.setString(20, spec.coatingFront)
                s.setString(21, spec.coatingBack)
                s.setInt(22, spec.impositionUps)
                s.setString(23, spec.lamination)
                s.setString(24, spec.bindingMethod)
                s.setString(25, spec.foldingType)
                s.setBoolean(26, spec.cuttingRequired)
                s.setBoolean(27, spec.dieCuttingRequired)
                s.setString(28, spec.packagingMethod)
                s.setString(29, spec.artworkUrl)
                s.setString(30, spec.specialInstructions)
                s.setString(31, spec.specFingerprint)
                s.executeUpdate()
            }

            // Insert Requirements
            for (req in snapshot.requirements) {
                val insertReqSql = """
                    INSERT INTO production_planning_requirements (
                        requirement_id, planning_id, tenant_id, category, item_code, description,
                        required_quantity, make_ready_quantity, waste_quantity, total_planned_quantity,
                        unit_of_measure, estimated_available, notes
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (requirement_id) DO NOTHING
                """.trimIndent()
                ctx.connection.prepareStatement(insertReqSql).use { s ->
                    s.setString(1, req.requirementId)
                    s.setString(2, snapshot.planningId)
                    s.setString(3, snapshot.tenantId)
                    s.setString(4, req.category)
                    s.setString(5, req.itemCode)
                    s.setString(6, req.description)
                    s.setBigDecimal(7, req.requiredQuantity)
                    s.setBigDecimal(8, req.makeReadyQuantity)
                    s.setBigDecimal(9, req.wasteQuantity)
                    s.setBigDecimal(10, req.totalPlannedQuantity)
                    s.setString(11, req.unitOfMeasure)
                    s.setBoolean(12, req.estimatedAvailable)
                    s.setString(13, req.notes)
                    s.executeUpdate()
                }
            }

            // Insert Operations
            for (op in snapshot.operations) {
                val insertOpSql = """
                    INSERT INTO production_planning_operations (
                        operation_id, planning_id, tenant_id, sequence_number, stage_type,
                        operation_code, operation_name, target_work_center, estimated_setup_minutes,
                        estimated_run_minutes, is_mandatory, is_qc_checkpoint, dependencies, notes
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (operation_id) DO NOTHING
                """.trimIndent()
                ctx.connection.prepareStatement(insertOpSql).use { s ->
                    s.setString(1, op.operationId)
                    s.setString(2, snapshot.planningId)
                    s.setString(3, snapshot.tenantId)
                    s.setInt(4, op.sequenceNumber)
                    s.setString(5, op.stageType.name)
                    s.setString(6, op.operationCode)
                    s.setString(7, op.operationName)
                    s.setString(8, op.targetWorkCenter)
                    s.setInt(9, op.estimatedSetupMinutes)
                    s.setInt(10, op.estimatedRunMinutes)
                    s.setBoolean(11, op.isMandatory)
                    s.setBoolean(12, op.isQcCheckpoint)
                    s.setString(13, op.dependencies.joinToString(","))
                    s.setString(14, op.notes)
                    s.executeUpdate()
                }
            }

            // Insert Diagnostics
            for (diag in snapshot.diagnostics) {
                val insertDiagSql = """
                    INSERT INTO production_planning_diagnostics (
                        diagnostic_id, planning_id, tenant_id, code, severity, category,
                        message, is_blocking, recommended_action
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (diagnostic_id) DO NOTHING
                """.trimIndent()
                ctx.connection.prepareStatement(insertDiagSql).use { s ->
                    s.setString(1, diag.diagnosticId)
                    s.setString(2, snapshot.planningId)
                    s.setString(3, snapshot.tenantId)
                    s.setString(4, diag.code)
                    s.setString(5, diag.severity.name)
                    s.setString(6, diag.category)
                    s.setString(7, diag.message)
                    s.setBoolean(8, diag.isBlocking)
                    s.setString(9, diag.recommendedAction)
                    s.executeUpdate()
                }
            }

            snapshot
        }
    }

    override suspend fun getPlanningSnapshotById(
        tenantId: String,
        planningId: String
    ): ProductionPlanningSnapshot? {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val sql = "SELECT * FROM production_planning_snapshots WHERE tenant_id = ? AND planning_id = ?"
            ctx.connection.prepareStatement(sql).use { s ->
                s.setString(1, tenantId)
                s.setString(2, planningId)
                s.executeQuery().use { rs ->
                    if (rs.next()) {
                        mapPlanningSnapshot(ctx, rs)
                    } else {
                        null
                    }
                }
            }
        }
    }

    override suspend fun getLatestPlanningSnapshotByOrder(
        tenantId: String,
        orderId: String
    ): ProductionPlanningSnapshot? {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val sql = "SELECT * FROM production_planning_snapshots WHERE tenant_id = ? AND order_id = ? ORDER BY version DESC LIMIT 1"
            ctx.connection.prepareStatement(sql).use { s ->
                s.setString(1, tenantId)
                s.setString(2, orderId)
                s.executeQuery().use { rs ->
                    if (rs.next()) {
                        mapPlanningSnapshot(ctx, rs)
                    } else {
                        null
                    }
                }
            }
        }
    }

    override suspend fun getPlanningSnapshotByIdempotencyKey(
        tenantId: String,
        idempotencyKey: String
    ): ProductionPlanningSnapshot? {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val sql = "SELECT * FROM production_planning_snapshots WHERE tenant_id = ? AND idempotency_key = ?"
            ctx.connection.prepareStatement(sql).use { s ->
                s.setString(1, tenantId)
                s.setString(2, idempotencyKey)
                s.executeQuery().use { rs ->
                    if (rs.next()) {
                        mapPlanningSnapshot(ctx, rs)
                    } else {
                        null
                    }
                }
            }
        }
    }

    override suspend fun listPlanningSnapshotsByOrder(
        tenantId: String,
        orderId: String
    ): List<ProductionPlanningSnapshot> {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val sql = "SELECT * FROM production_planning_snapshots WHERE tenant_id = ? AND order_id = ? ORDER BY version DESC"
            ctx.connection.prepareStatement(sql).use { s ->
                s.setString(1, tenantId)
                s.setString(2, orderId)
                s.executeQuery().use { rs ->
                    val list = mutableListOf<ProductionPlanningSnapshot>()
                    while (rs.next()) {
                        list.add(mapPlanningSnapshot(ctx, rs))
                    }
                    list
                }
            }
        }
    }

    override suspend fun savePlanningEvent(
        event: ProductionPlanningEvent
    ): ProductionPlanningEvent {
        return transactionManager.inTransaction(TenantContext(event.tenantId)) { ctx ->
            val sql = """
                INSERT INTO production_planning_events (
                    event_id, planning_id, tenant_id, event_type, from_status, to_status, event_payload, performed_by, performed_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            ctx.connection.prepareStatement(sql).use { s ->
                s.setString(1, event.eventId)
                s.setString(2, event.planningId)
                s.setString(3, event.tenantId)
                s.setString(4, event.eventType.name)
                s.setString(5, event.fromStatus?.name)
                s.setString(6, event.toStatus?.name)
                s.setString(7, event.eventPayload)
                s.setString(8, event.performedBy)
                s.setLong(9, event.performedAt)
                s.executeUpdate()
            }
            event
        }
    }

    override suspend fun listPlanningEvents(
        tenantId: String,
        planningId: String
    ): List<ProductionPlanningEvent> {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val sql = "SELECT * FROM production_planning_events WHERE tenant_id = ? AND planning_id = ? ORDER BY performed_at ASC"
            ctx.connection.prepareStatement(sql).use { s ->
                s.setString(1, tenantId)
                s.setString(2, planningId)
                s.executeQuery().use { rs ->
                    val list = mutableListOf<ProductionPlanningEvent>()
                    while (rs.next()) {
                        list.add(
                            ProductionPlanningEvent(
                                eventId = rs.getString("event_id"),
                                planningId = rs.getString("planning_id"),
                                tenantId = rs.getString("tenant_id"),
                                eventType = ProductionPlanningEventType.valueOf(rs.getString("event_type")),
                                fromStatus = rs.getString("from_status")?.let { PlanningStatus.valueOf(it) },
                                toStatus = rs.getString("to_status")?.let { PlanningStatus.valueOf(it) },
                                eventPayload = rs.getString("event_payload"),
                                performedBy = rs.getString("performed_by"),
                                performedAt = rs.getLong("performed_at")
                            )
                        )
                    }
                    list
                }
            }
        }
    }

    private fun mapPlanningSnapshot(ctx: TransactionContext, rs: ResultSet): ProductionPlanningSnapshot {
        val planningId = rs.getString("planning_id")
        val tenantId = rs.getString("tenant_id")

        // Load Spec
        val specSql = "SELECT * FROM production_job_specifications WHERE planning_id = ?"
        val spec = ctx.connection.prepareStatement(specSql).use { s ->
            s.setString(1, planningId)
            s.executeQuery().use { srs ->
                if (srs.next()) {
                    ProductionJobSpecification(
                        specId = srs.getString("spec_id"),
                        jobTitle = srs.getString("job_title"),
                        productType = srs.getString("product_type"),
                        orderedQuantity = srs.getLong("ordered_quantity"),
                        plannedQuantity = srs.getLong("planned_quantity"),
                        finishedWidthMm = srs.getBigDecimal("finished_width_mm"),
                        finishedHeightMm = srs.getBigDecimal("finished_height_mm"),
                        substrateType = srs.getString("substrate_type"),
                        substrateGsm = srs.getInt("substrate_gsm"),
                        substrateBrand = srs.getString("substrate_brand"),
                        parentSheetWidthMm = srs.getBigDecimal("parent_sheet_width_mm"),
                        parentSheetHeightMm = srs.getBigDecimal("parent_sheet_height_mm"),
                        pressSheetWidthMm = srs.getBigDecimal("press_sheet_width_mm"),
                        pressSheetHeightMm = srs.getBigDecimal("press_sheet_height_mm"),
                        printingMethod = srs.getString("printing_method"),
                        colorsFront = srs.getInt("colors_front"),
                        colorsBack = srs.getInt("colors_back"),
                        coatingFront = srs.getString("coating_front"),
                        coatingBack = srs.getString("coating_back"),
                        impositionUps = srs.getInt("imposition_ups"),
                        lamination = srs.getString("lamination"),
                        bindingMethod = srs.getString("binding_method"),
                        foldingType = srs.getString("folding_type"),
                        cuttingRequired = srs.getBoolean("cutting_required"),
                        dieCuttingRequired = srs.getBoolean("die_cutting_required"),
                        packagingMethod = srs.getString("packaging_method"),
                        artworkUrl = srs.getString("artwork_url"),
                        specialInstructions = srs.getString("special_instructions"),
                        specFingerprint = srs.getString("spec_fingerprint")
                    )
                } else {
                    throw IllegalStateException("Specification not found for planning snapshot $planningId")
                }
            }
        }

        // Load Requirements
        val reqs = mutableListOf<ProductionPlanningRequirement>()
        val reqSql = "SELECT * FROM production_planning_requirements WHERE planning_id = ?"
        ctx.connection.prepareStatement(reqSql).use { s ->
            s.setString(1, planningId)
            s.executeQuery().use { rrs ->
                while (rrs.next()) {
                    reqs.add(
                        ProductionPlanningRequirement(
                            requirementId = rrs.getString("requirement_id"),
                            planningId = planningId,
                            category = rrs.getString("category"),
                            itemCode = rrs.getString("item_code"),
                            description = rrs.getString("description"),
                            requiredQuantity = rrs.getBigDecimal("required_quantity"),
                            makeReadyQuantity = rrs.getBigDecimal("make_ready_quantity"),
                            wasteQuantity = rrs.getBigDecimal("waste_quantity"),
                            totalPlannedQuantity = rrs.getBigDecimal("total_planned_quantity"),
                            unitOfMeasure = rrs.getString("unit_of_measure"),
                            estimatedAvailable = rrs.getBoolean("estimated_available"),
                            notes = rrs.getString("notes")
                        )
                    )
                }
            }
        }

        // Load Operations
        val ops = mutableListOf<ProductionPlanningOperation>()
        val opSql = "SELECT * FROM production_planning_operations WHERE planning_id = ? ORDER BY sequence_number ASC"
        ctx.connection.prepareStatement(opSql).use { s ->
            s.setString(1, planningId)
            s.executeQuery().use { ors ->
                while (ors.next()) {
                    val depStr = ors.getString("dependencies")
                    val deps = if (depStr.isNullOrBlank()) emptyList() else depStr.split(",")
                    ops.add(
                        ProductionPlanningOperation(
                            operationId = ors.getString("operation_id"),
                            planningId = planningId,
                            sequenceNumber = ors.getInt("sequence_number"),
                            stageType = ProductionStageType.valueOf(ors.getString("stage_type")),
                            operationCode = ors.getString("operation_code"),
                            operationName = ors.getString("operation_name"),
                            targetWorkCenter = ors.getString("target_work_center"),
                            estimatedSetupMinutes = ors.getInt("estimated_setup_minutes"),
                            estimatedRunMinutes = ors.getInt("estimated_run_minutes"),
                            isMandatory = ors.getBoolean("is_mandatory"),
                            isQcCheckpoint = ors.getBoolean("is_qc_checkpoint"),
                            dependencies = deps,
                            notes = ors.getString("notes")
                        )
                    )
                }
            }
        }

        // Load Diagnostics
        val diags = mutableListOf<PlanningDiagnostic>()
        val diagSql = "SELECT * FROM production_planning_diagnostics WHERE planning_id = ?"
        ctx.connection.prepareStatement(diagSql).use { s ->
            s.setString(1, planningId)
            s.executeQuery().use { drs ->
                while (drs.next()) {
                    diags.add(
                        PlanningDiagnostic(
                            diagnosticId = drs.getString("diagnostic_id"),
                            planningId = planningId,
                            code = drs.getString("code"),
                            severity = DiagnosticSeverity.valueOf(drs.getString("severity")),
                            category = drs.getString("category"),
                            message = drs.getString("message"),
                            isBlocking = drs.getBoolean("is_blocking"),
                            recommendedAction = drs.getString("recommended_action")
                        )
                    )
                }
            }
        }

        return ProductionPlanningSnapshot(
            planningId = planningId,
            tenantId = tenantId,
            projectId = rs.getString("project_id"),
            orderId = rs.getString("order_id"),
            orderNumber = rs.getString("order_number"),
            orderItemId = rs.getString("order_item_id"),
            commercialCommitmentId = rs.getString("commercial_commitment_id"),
            quotationId = rs.getString("quotation_id"),
            quotationVersionNumber = rs.getObject("quotation_version_number") as? Int,
            customerId = rs.getString("customer_id"),
            status = PlanningStatus.valueOf(rs.getString("status")),
            version = rs.getInt("version"),
            isCurrent = rs.getBoolean("is_current"),
            readinessScore = rs.getBigDecimal("readiness_score"),
            feasibilityStatus = FeasibilityStatus.valueOf(rs.getString("feasibility_status")),
            specification = spec,
            requirements = reqs,
            operations = ops,
            diagnostics = diags,
            machineCompatibility = emptyList(),
            orderRequestedDate = rs.getObject("order_requested_date") as? Long,
            estimatedCompletionDate = rs.getObject("estimated_completion_date") as? Long,
            planningFingerprint = rs.getString("planning_fingerprint"),
            integrityHash = rs.getString("integrity_hash"),
            createdAt = rs.getLong("created_at"),
            updatedAt = rs.getLong("updated_at"),
            createdBy = rs.getString("created_by")
        )
    }
}
