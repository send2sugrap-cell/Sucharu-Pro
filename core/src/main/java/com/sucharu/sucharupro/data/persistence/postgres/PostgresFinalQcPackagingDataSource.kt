package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.datasource.finalqc.FinalQcPackagingDataSource
import com.sucharu.sucharupro.domain.model.finalqc.*
import com.sucharu.sucharupro.domain.model.production.ProductionStageType
import java.sql.ResultSet

class PostgresFinalQcPackagingDataSource(
    private val transactionManager: TransactionManager
) : FinalQcPackagingDataSource {

    override suspend fun saveInspection(tenantId: String, inspection: ProductionFinalQcInspection) {
        transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = """
                INSERT INTO production_final_qc_inspections (
                    inspection_id, tenant_id, execution_job_id, order_id, sample_plan_type,
                    total_lot_quantity, sample_size, accepted_quantity, rejected_quantity,
                    rework_quantity, status, checklist_json, inspector_id, inspector_name,
                    inspection_notes, inspected_at, completed_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (inspection_id) DO UPDATE SET
                    accepted_quantity = EXCLUDED.accepted_quantity,
                    rejected_quantity = EXCLUDED.rejected_quantity,
                    rework_quantity = EXCLUDED.rework_quantity,
                    status = EXCLUDED.status,
                    checklist_json = EXCLUDED.checklist_json,
                    inspection_notes = EXCLUDED.inspection_notes,
                    completed_at = EXCLUDED.completed_at
            """.trimIndent()
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, inspection.inspectionId)
                ps.setString(2, tenantId)
                ps.setString(3, inspection.executionJobId)
                ps.setString(4, inspection.orderId)
                ps.setString(5, inspection.samplePlanType.name)
                ps.setBigDecimal(6, inspection.totalLotQuantity)
                ps.setBigDecimal(7, inspection.sampleSize)
                ps.setBigDecimal(8, inspection.acceptedQuantity)
                ps.setBigDecimal(9, inspection.rejectedQuantity)
                ps.setBigDecimal(10, inspection.reworkQuantity)
                ps.setString(11, inspection.status.name)
                ps.setString(12, serializeChecklist(inspection.checklist))
                ps.setString(13, inspection.inspectorId)
                ps.setString(14, inspection.inspectorName)
                ps.setString(15, inspection.inspectionNotes)
                ps.setLong(16, inspection.inspectedAt)
                ps.setObject(17, inspection.completedAt)
                ps.executeUpdate()
            }
        }
    }

    override suspend fun getInspection(tenantId: String, inspectionId: String): ProductionFinalQcInspection? {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM production_final_qc_inspections WHERE tenant_id = ? AND inspection_id = ?"
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, inspectionId)
                ps.executeQuery().use { rs ->
                    if (rs.next()) mapInspection(rs) else null
                }
            }
        }
    }

    override suspend fun listInspectionsByJob(tenantId: String, executionJobId: String): List<ProductionFinalQcInspection> {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM production_final_qc_inspections WHERE tenant_id = ? AND execution_job_id = ? ORDER BY inspected_at ASC"
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, executionJobId)
                ps.executeQuery().use { rs ->
                    val list = mutableListOf<ProductionFinalQcInspection>()
                    while (rs.next()) list.add(mapInspection(rs))
                    list
                }
            }
        }
    }

    override suspend fun saveDefectContainment(tenantId: String, defect: ProductionDefectContainmentRecord) {
        transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = """
                INSERT INTO production_defect_containments (
                    containment_id, tenant_id, execution_job_id, inspection_id, root_cause_stage,
                    defect_type, severity, defect_quantity, disposition, quarantine_location,
                    rework_work_order_id, root_cause_details, logged_by, logged_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (containment_id) DO UPDATE SET
                    disposition = EXCLUDED.disposition,
                    quarantine_location = EXCLUDED.quarantine_location,
                    rework_work_order_id = EXCLUDED.rework_work_order_id,
                    root_cause_details = EXCLUDED.root_cause_details
            """.trimIndent()
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, defect.containmentId)
                ps.setString(2, tenantId)
                ps.setString(3, defect.executionJobId)
                ps.setString(4, defect.inspectionId)
                ps.setString(5, defect.rootCauseStage.name)
                ps.setString(6, defect.defectType.name)
                ps.setString(7, defect.severity.name)
                ps.setBigDecimal(8, defect.defectQuantity)
                ps.setString(9, defect.disposition.name)
                ps.setString(10, defect.quarantineLocation)
                ps.setString(11, defect.reworkWorkOrderId)
                ps.setString(12, defect.rootCauseDetails)
                ps.setString(13, defect.loggedBy)
                ps.setLong(14, defect.loggedAt)
                ps.executeUpdate()
            }
        }
    }

    override suspend fun listDefectsByJob(tenantId: String, executionJobId: String): List<ProductionDefectContainmentRecord> {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM production_defect_containments WHERE tenant_id = ? AND execution_job_id = ? ORDER BY logged_at ASC"
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, executionJobId)
                ps.executeQuery().use { rs ->
                    val list = mutableListOf<ProductionDefectContainmentRecord>()
                    while (rs.next()) list.add(mapDefect(rs))
                    list
                }
            }
        }
    }

    override suspend fun savePackagingRecord(tenantId: String, packaging: ProductionPackagingRecord) {
        transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = """
                INSERT INTO production_packaging_records (
                    packaging_id, tenant_id, execution_job_id, inspection_id, packaging_type,
                    units_per_package, total_package_count, total_packaged_quantity,
                    pallet_identifier, carton_numbers_range, gross_weight_kg,
                    packaging_slip_barcode, packaged_by, packaged_at, notes
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, packaging.packagingId)
                ps.setString(2, tenantId)
                ps.setString(3, packaging.executionJobId)
                ps.setString(4, packaging.inspectionId)
                ps.setString(5, packaging.packagingType.name)
                ps.setBigDecimal(6, packaging.unitsPerPackage)
                ps.setInt(7, packaging.totalPackageCount)
                ps.setBigDecimal(8, packaging.totalPackagedQuantity)
                ps.setString(9, packaging.palletIdentifier)
                ps.setString(10, packaging.cartonNumbersRange)
                ps.setBigDecimal(11, packaging.grossWeightKg)
                ps.setString(12, packaging.packagingSlipBarcode)
                ps.setString(13, packaging.packagedBy)
                ps.setLong(14, packaging.packagedAt)
                ps.setString(15, packaging.notes)
                ps.executeUpdate()
            }
        }
    }

    override suspend fun getPackagingRecord(tenantId: String, packagingId: String): ProductionPackagingRecord? {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM production_packaging_records WHERE tenant_id = ? AND packaging_id = ?"
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, packagingId)
                ps.executeQuery().use { rs ->
                    if (rs.next()) mapPackaging(rs) else null
                }
            }
        }
    }

    override suspend fun listPackagingRecordsByJob(tenantId: String, executionJobId: String): List<ProductionPackagingRecord> {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM production_packaging_records WHERE tenant_id = ? AND execution_job_id = ? ORDER BY packaged_at ASC"
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, executionJobId)
                ps.executeQuery().use { rs ->
                    val list = mutableListOf<ProductionPackagingRecord>()
                    while (rs.next()) list.add(mapPackaging(rs))
                    list
                }
            }
        }
    }

    override suspend fun saveReleaseRecord(tenantId: String, release: FinishedGoodsReleaseRecord) {
        transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = """
                INSERT INTO finished_goods_release_records (
                    release_id, tenant_id, execution_job_id, order_id, inspection_id,
                    packaging_id, released_quantity, destination, status,
                    authorized_by, authorized_at, integrity_hash, notes
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (release_id) DO UPDATE SET
                    status = EXCLUDED.status,
                    notes = EXCLUDED.notes
            """.trimIndent()
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, release.releaseId)
                ps.setString(2, tenantId)
                ps.setString(3, release.executionJobId)
                ps.setString(4, release.orderId)
                ps.setString(5, release.inspectionId)
                ps.setString(6, release.packagingId)
                ps.setBigDecimal(7, release.releasedQuantity)
                ps.setString(8, release.destination)
                ps.setString(9, release.status.name)
                ps.setString(10, release.authorizedBy)
                ps.setLong(11, release.authorizedAt)
                ps.setString(12, release.integrityHash)
                ps.setString(13, release.notes)
                ps.executeUpdate()
            }
        }
    }

    override suspend fun getReleaseRecord(tenantId: String, releaseId: String): FinishedGoodsReleaseRecord? {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM finished_goods_release_records WHERE tenant_id = ? AND release_id = ?"
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, releaseId)
                ps.executeQuery().use { rs ->
                    if (rs.next()) mapRelease(rs) else null
                }
            }
        }
    }

    override suspend fun listReleaseRecordsByJob(tenantId: String, executionJobId: String): List<FinishedGoodsReleaseRecord> {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM finished_goods_release_records WHERE tenant_id = ? AND execution_job_id = ? ORDER BY authorized_at ASC"
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, executionJobId)
                ps.executeQuery().use { rs ->
                    val list = mutableListOf<FinishedGoodsReleaseRecord>()
                    while (rs.next()) list.add(mapRelease(rs))
                    list
                }
            }
        }
    }

    override suspend fun saveEvent(tenantId: String, event: FinalQcPackagingEvent) {
        transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = """
                INSERT INTO final_qc_packaging_events (
                    event_id, tenant_id, execution_job_id, event_type, actor, payload, timestamp
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, event.eventId)
                ps.setString(2, tenantId)
                ps.setString(3, event.executionJobId)
                ps.setString(4, event.eventType.name)
                ps.setString(5, event.actor)
                ps.setString(6, event.payload)
                ps.setLong(7, event.timestamp)
                ps.executeUpdate()
            }
        }
    }

    override suspend fun listEventsByJob(tenantId: String, executionJobId: String): List<FinalQcPackagingEvent> {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM final_qc_packaging_events WHERE tenant_id = ? AND execution_job_id = ? ORDER BY timestamp ASC"
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, executionJobId)
                ps.executeQuery().use { rs ->
                    val list = mutableListOf<FinalQcPackagingEvent>()
                    while (rs.next()) {
                        list.add(
                            FinalQcPackagingEvent(
                                eventId = rs.getString("event_id"),
                                tenantId = rs.getString("tenant_id"),
                                executionJobId = rs.getString("execution_job_id"),
                                eventType = FinalQcEventType.valueOf(rs.getString("event_type")),
                                actor = rs.getString("actor"),
                                payload = rs.getString("payload"),
                                timestamp = rs.getLong("timestamp")
                            )
                        )
                    }
                    list
                }
            }
        }
    }

    private fun mapInspection(rs: ResultSet): ProductionFinalQcInspection {
        return ProductionFinalQcInspection(
            inspectionId = rs.getString("inspection_id"),
            tenantId = rs.getString("tenant_id"),
            executionJobId = rs.getString("execution_job_id"),
            orderId = rs.getString("order_id"),
            samplePlanType = InspectionSamplePlanType.valueOf(rs.getString("sample_plan_type")),
            totalLotQuantity = rs.getBigDecimal("total_lot_quantity"),
            sampleSize = rs.getBigDecimal("sample_size"),
            acceptedQuantity = rs.getBigDecimal("accepted_quantity"),
            rejectedQuantity = rs.getBigDecimal("rejected_quantity"),
            reworkQuantity = rs.getBigDecimal("rework_quantity"),
            status = FinalQcInspectionStatus.valueOf(rs.getString("status")),
            checklist = parseChecklist(rs.getString("checklist_json")),
            inspectorId = rs.getString("inspector_id"),
            inspectorName = rs.getString("inspector_name"),
            inspectionNotes = rs.getString("inspection_notes"),
            inspectedAt = rs.getLong("inspected_at"),
            completedAt = rs.getObject("completed_at") as? Long
        )
    }

    private fun mapDefect(rs: ResultSet): ProductionDefectContainmentRecord {
        return ProductionDefectContainmentRecord(
            containmentId = rs.getString("containment_id"),
            tenantId = rs.getString("tenant_id"),
            executionJobId = rs.getString("execution_job_id"),
            inspectionId = rs.getString("inspection_id"),
            rootCauseStage = ProductionStageType.valueOf(rs.getString("root_cause_stage")),
            defectType = DefectClassificationType.valueOf(rs.getString("defect_type")),
            severity = DefectSeverity.valueOf(rs.getString("severity")),
            defectQuantity = rs.getBigDecimal("defect_quantity"),
            disposition = ContainmentDisposition.valueOf(rs.getString("disposition")),
            quarantineLocation = rs.getString("quarantine_location"),
            reworkWorkOrderId = rs.getString("rework_work_order_id"),
            rootCauseDetails = rs.getString("root_cause_details"),
            loggedBy = rs.getString("logged_by"),
            loggedAt = rs.getLong("logged_at")
        )
    }

    private fun mapPackaging(rs: ResultSet): ProductionPackagingRecord {
        return ProductionPackagingRecord(
            packagingId = rs.getString("packaging_id"),
            tenantId = rs.getString("tenant_id"),
            executionJobId = rs.getString("execution_job_id"),
            inspectionId = rs.getString("inspection_id"),
            packagingType = PackagingType.valueOf(rs.getString("packaging_type")),
            unitsPerPackage = rs.getBigDecimal("units_per_package"),
            totalPackageCount = rs.getInt("total_package_count"),
            totalPackagedQuantity = rs.getBigDecimal("total_packaged_quantity"),
            palletIdentifier = rs.getString("pallet_identifier"),
            cartonNumbersRange = rs.getString("carton_numbers_range"),
            grossWeightKg = rs.getBigDecimal("gross_weight_kg"),
            packagingSlipBarcode = rs.getString("packaging_slip_barcode"),
            packagedBy = rs.getString("packaged_by"),
            packagedAt = rs.getLong("packaged_at"),
            notes = rs.getString("notes")
        )
    }

    private fun mapRelease(rs: ResultSet): FinishedGoodsReleaseRecord {
        return FinishedGoodsReleaseRecord(
            releaseId = rs.getString("release_id"),
            tenantId = rs.getString("tenant_id"),
            executionJobId = rs.getString("execution_job_id"),
            orderId = rs.getString("order_id"),
            inspectionId = rs.getString("inspection_id"),
            packagingId = rs.getString("packaging_id"),
            releasedQuantity = rs.getBigDecimal("released_quantity"),
            destination = rs.getString("destination"),
            status = FinishedGoodsReleaseStatus.valueOf(rs.getString("status")),
            authorizedBy = rs.getString("authorized_by"),
            authorizedAt = rs.getLong("authorized_at"),
            integrityHash = rs.getString("integrity_hash"),
            notes = rs.getString("notes")
        )
    }

    private fun serializeChecklist(list: List<QcChecklistItem>): String {
        if (list.isEmpty()) return "[]"
        return list.joinToString(separator = "||") {
            "${it.checkCode};;${it.checkTitle};;${it.isPassed};;${it.measuredValue ?: ""};;${it.toleranceLimit ?: ""};;${it.remarks ?: ""}"
        }
    }

    private fun parseChecklist(raw: String?): List<QcChecklistItem> {
        if (raw.isNullOrBlank() || raw == "[]") return emptyList()
        return raw.split("||").mapNotNull { part ->
            val tokens = part.split(";;")
            if (tokens.size >= 3) {
                QcChecklistItem(
                    checkCode = tokens[0],
                    checkTitle = tokens[1],
                    isPassed = tokens[2].toBoolean(),
                    measuredValue = tokens.getOrNull(3)?.takeIf { it.isNotBlank() },
                    toleranceLimit = tokens.getOrNull(4)?.takeIf { it.isNotBlank() },
                    remarks = tokens.getOrNull(5)?.takeIf { it.isNotBlank() }
                )
            } else null
        }
    }
}
