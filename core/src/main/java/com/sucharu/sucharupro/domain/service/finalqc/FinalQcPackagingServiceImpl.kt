package com.sucharu.sucharupro.domain.service.finalqc

import com.sucharu.sucharupro.domain.model.finalqc.*
import com.sucharu.sucharupro.domain.model.production.ProductionStageType
import com.sucharu.sucharupro.domain.repository.finalqc.FinalQcPackagingRepository
import java.math.BigDecimal
import java.util.UUID

class FinalQcPackagingServiceImpl(
    private val repository: FinalQcPackagingRepository,
    private val inspectionEngine: FinalQcInspectionEngine = FinalQcInspectionEngine(),
    private val defectEngine: DefectContainmentEngine = DefectContainmentEngine(),
    private val packagingEngine: PackagingOrchestrationEngine = PackagingOrchestrationEngine(),
    private val releaseEngine: FinishedGoodsReleaseEngine = FinishedGoodsReleaseEngine(),
    private val reconciliationEngine: FinalQcPackagingReconciliationEngine = FinalQcPackagingReconciliationEngine()
) : FinalQcPackagingService {

    override suspend fun createFinalQcInspection(
        tenantId: String,
        executionJobId: String,
        orderId: String,
        samplePlanType: InspectionSamplePlanType,
        totalLotQuantity: BigDecimal,
        sampleSize: BigDecimal,
        checklist: List<QcChecklistItem>,
        inspectorId: String,
        inspectorName: String,
        notes: String?,
        actor: String
    ): ProductionFinalQcInspection {
        val inspection = inspectionEngine.createInspection(
            tenantId = tenantId,
            executionJobId = executionJobId,
            orderId = orderId,
            samplePlanType = samplePlanType,
            totalLotQuantity = totalLotQuantity,
            sampleSize = sampleSize,
            checklist = checklist,
            inspectorId = inspectorId,
            inspectorName = inspectorName,
            notes = notes
        )
        repository.saveInspection(tenantId, inspection)
        repository.saveEvent(
            tenantId,
            FinalQcPackagingEvent(
                eventId = "EVT-" + UUID.randomUUID().toString().take(8),
                tenantId = tenantId,
                executionJobId = executionJobId,
                eventType = FinalQcEventType.INSPECTION_STARTED,
                actor = actor,
                payload = "Final QC Inspection started for lot $totalLotQuantity (sample size: $sampleSize)"
            )
        )
        return inspection
    }

    override suspend fun completeFinalQcInspection(
        tenantId: String,
        inspectionId: String,
        acceptedQuantity: BigDecimal,
        rejectedQuantity: BigDecimal,
        reworkQuantity: BigDecimal,
        notes: String?,
        actor: String
    ): ProductionFinalQcInspection {
        val existing = repository.getInspection(tenantId, inspectionId)
            ?: throw IllegalArgumentException("Final QC Inspection $inspectionId not found for tenant $tenantId")

        val completed = inspectionEngine.completeInspection(
            inspection = existing,
            acceptedQuantity = acceptedQuantity,
            rejectedQuantity = rejectedQuantity,
            reworkQuantity = reworkQuantity,
            notes = notes
        )
        repository.saveInspection(tenantId, completed)
        repository.saveEvent(
            tenantId,
            FinalQcPackagingEvent(
                eventId = "EVT-" + UUID.randomUUID().toString().take(8),
                tenantId = tenantId,
                executionJobId = existing.executionJobId,
                eventType = FinalQcEventType.INSPECTION_COMPLETED,
                actor = actor,
                payload = "Final QC Inspection completed with status ${completed.status} (Accepted: $acceptedQuantity, Rejected: $rejectedQuantity, Rework: $reworkQuantity)"
            )
        )
        return completed
    }

    override suspend fun getInspection(tenantId: String, inspectionId: String): ProductionFinalQcInspection? {
        return repository.getInspection(tenantId, inspectionId)
    }

    override suspend fun listInspectionsByJob(tenantId: String, executionJobId: String): List<ProductionFinalQcInspection> {
        return repository.listInspectionsByJob(tenantId, executionJobId)
    }

    override suspend fun recordDefectContainment(
        tenantId: String,
        executionJobId: String,
        inspectionId: String,
        rootCauseStage: ProductionStageType,
        defectType: DefectClassificationType,
        severity: DefectSeverity,
        defectQuantity: BigDecimal,
        disposition: ContainmentDisposition,
        quarantineLocation: String,
        reworkWorkOrderId: String?,
        rootCauseDetails: String,
        actor: String
    ): ProductionDefectContainmentRecord {
        val record = defectEngine.recordDefectContainment(
            tenantId = tenantId,
            executionJobId = executionJobId,
            inspectionId = inspectionId,
            rootCauseStage = rootCauseStage,
            defectType = defectType,
            severity = severity,
            defectQuantity = defectQuantity,
            disposition = disposition,
            quarantineLocation = quarantineLocation,
            reworkWorkOrderId = reworkWorkOrderId,
            rootCauseDetails = rootCauseDetails,
            loggedBy = actor
        )
        repository.saveDefectContainment(tenantId, record)
        repository.saveEvent(
            tenantId,
            FinalQcPackagingEvent(
                eventId = "EVT-" + UUID.randomUUID().toString().take(8),
                tenantId = tenantId,
                executionJobId = executionJobId,
                eventType = FinalQcEventType.DEFECT_QUARANTINED,
                actor = actor,
                payload = "Defect contained: $defectType ($severity) qty: $defectQuantity at $quarantineLocation"
            )
        )
        return record
    }

    override suspend fun listDefectsByJob(tenantId: String, executionJobId: String): List<ProductionDefectContainmentRecord> {
        return repository.listDefectsByJob(tenantId, executionJobId)
    }

    override suspend fun createPackagingRecord(
        tenantId: String,
        executionJobId: String,
        inspectionId: String,
        packagingType: PackagingType,
        unitsPerPackage: BigDecimal,
        totalPackageCount: Int,
        palletIdentifier: String?,
        cartonNumbersRange: String?,
        grossWeightKg: BigDecimal?,
        packagedBy: String,
        notes: String?,
        actor: String
    ): ProductionPackagingRecord {
        val record = packagingEngine.createPackagingRecord(
            tenantId = tenantId,
            executionJobId = executionJobId,
            inspectionId = inspectionId,
            packagingType = packagingType,
            unitsPerPackage = unitsPerPackage,
            totalPackageCount = totalPackageCount,
            palletIdentifier = palletIdentifier,
            cartonNumbersRange = cartonNumbersRange,
            grossWeightKg = grossWeightKg,
            packagedBy = packagedBy,
            notes = notes
        )
        repository.savePackagingRecord(tenantId, record)
        repository.saveEvent(
            tenantId,
            FinalQcPackagingEvent(
                eventId = "EVT-" + UUID.randomUUID().toString().take(8),
                tenantId = tenantId,
                executionJobId = executionJobId,
                eventType = FinalQcEventType.PACKAGING_COMPLETED,
                actor = actor,
                payload = "Packaging complete: ${record.totalPackagedQuantity} units across $totalPackageCount packages (Barcode: ${record.packagingSlipBarcode})"
            )
        )
        return record
    }

    override suspend fun listPackagingRecordsByJob(tenantId: String, executionJobId: String): List<ProductionPackagingRecord> {
        return repository.listPackagingRecordsByJob(tenantId, executionJobId)
    }

    override suspend fun authorizeFinishedGoodsRelease(
        tenantId: String,
        executionJobId: String,
        orderId: String,
        inspectionId: String,
        packagingId: String,
        releasedQuantity: BigDecimal,
        destination: String,
        authorizedBy: String,
        notes: String?,
        actor: String
    ): FinishedGoodsReleaseRecord {
        val record = releaseEngine.authorizeRelease(
            tenantId = tenantId,
            executionJobId = executionJobId,
            orderId = orderId,
            inspectionId = inspectionId,
            packagingId = packagingId,
            releasedQuantity = releasedQuantity,
            destination = destination,
            authorizedBy = authorizedBy,
            notes = notes
        )
        repository.saveReleaseRecord(tenantId, record)
        repository.saveEvent(
            tenantId,
            FinalQcPackagingEvent(
                eventId = "EVT-" + UUID.randomUUID().toString().take(8),
                tenantId = tenantId,
                executionJobId = executionJobId,
                eventType = FinalQcEventType.RELEASE_CERTIFIED,
                actor = actor,
                payload = "Finished goods release certified: $releasedQuantity units to $destination (Hash: ${record.integrityHash})"
            )
        )
        return record
    }

    override suspend fun listReleaseRecordsByJob(tenantId: String, executionJobId: String): List<FinishedGoodsReleaseRecord> {
        return repository.listReleaseRecordsByJob(tenantId, executionJobId)
    }

    override suspend fun getQualityVarianceSummary(
        tenantId: String,
        executionJobId: String,
        totalShopFloorGoodOutput: BigDecimal
    ): FinalQcPackagingVarianceSummary {
        val inspections = repository.listInspectionsByJob(tenantId, executionJobId)
        val defects = repository.listDefectsByJob(tenantId, executionJobId)
        val packagings = repository.listPackagingRecordsByJob(tenantId, executionJobId)
        return reconciliationEngine.generateVarianceSummary(
            executionJobId = executionJobId,
            tenantId = tenantId,
            inspections = inspections,
            defects = defects,
            packagings = packagings,
            totalShopFloorGoodOutput = totalShopFloorGoodOutput
        )
    }

    override suspend fun reconcileFinalQcPackaging(
        tenantId: String,
        executionJobId: String,
        totalShopFloorGoodOutput: BigDecimal
    ): FinalQcPackagingReconciliationResult {
        val inspections = repository.listInspectionsByJob(tenantId, executionJobId)
        val defects = repository.listDefectsByJob(tenantId, executionJobId)
        val packagings = repository.listPackagingRecordsByJob(tenantId, executionJobId)
        val releases = repository.listReleaseRecordsByJob(tenantId, executionJobId)

        return reconciliationEngine.reconcile(
            executionJobId = executionJobId,
            tenantId = tenantId,
            inspections = inspections,
            defects = defects,
            packagings = packagings,
            releases = releases,
            totalShopFloorGoodOutput = totalShopFloorGoodOutput
        )
    }

    override suspend fun getAiHandoffContract(
        tenantId: String,
        executionJobId: String,
        orderId: String
    ): Module17Step08FinalQcPackagingHandoffContract {
        val inspections = repository.listInspectionsByJob(tenantId, executionJobId)
        val defects = repository.listDefectsByJob(tenantId, executionJobId)
        val packagings = repository.listPackagingRecordsByJob(tenantId, executionJobId)
        val releases = repository.listReleaseRecordsByJob(tenantId, executionJobId)
        val recon = reconcileFinalQcPackaging(tenantId, executionJobId)

        val latestInsp = inspections.lastOrNull()
        val latestPkg = packagings.lastOrNull()
        val latestRelease = releases.lastOrNull()

        val totalAccepted = inspections.fold(BigDecimal.ZERO) { acc, i -> acc.add(i.acceptedQuantity) }
        val totalDefects = defects.fold(BigDecimal.ZERO) { acc, d -> acc.add(d.defectQuantity) }
        val totalLot = inspections.fold(BigDecimal.ZERO) { acc, i -> acc.add(i.totalLotQuantity) }

        val qualityYield = FinalQcPackagingMathUtils.calculateYieldPercentage(totalAccepted, totalLot)
        val defectRate = FinalQcPackagingMathUtils.calculateDefectRatePercentage(totalDefects, totalLot)
        val totalPackaged = packagings.fold(BigDecimal.ZERO) { acc, p -> acc.add(p.totalPackagedQuantity) }
        val totalCartons = packagings.sumOf { it.totalPackageCount }

        return Module17Step08FinalQcPackagingHandoffContract(
            contractVersion = "1.0.0",
            tenantId = tenantId,
            executionJobId = executionJobId,
            orderId = if (orderId.isNotBlank()) orderId else (latestInsp?.orderId ?: ""),
            finalInspectionStatus = latestInsp?.status ?: FinalQcInspectionStatus.PENDING_INSPECTION,
            totalGoodQuantityAccepted = FinalQcPackagingMathUtils.roundScale4(totalAccepted),
            totalDefectQuantity = FinalQcPackagingMathUtils.roundScale4(totalDefects),
            qualityYieldPercentage = qualityYield,
            defectRatePercentage = defectRate,
            totalPackagedCartons = totalCartons,
            totalPackagedQuantity = FinalQcPackagingMathUtils.roundScale4(totalPackaged),
            packagingSlipBarcode = latestPkg?.packagingSlipBarcode ?: "NONE",
            releaseStatus = latestRelease?.status ?: FinishedGoodsReleaseStatus.DRAFT,
            releaseCertificateHash = latestRelease?.integrityHash ?: "UNRELEASED",
            isFullyReconciled = recon.isFullyReconciled,
            inspectionSummary = inspections.map { "${it.inspectionId}: ${it.status} (Pass: ${it.acceptedQuantity}, Fail: ${it.rejectedQuantity}, Rework: ${it.reworkQuantity})" },
            defectContainmentSummary = defects.map { "${it.containmentId}: ${it.defectType} [${it.severity}] - ${it.defectQuantity} units at ${it.quarantineLocation} (${it.disposition})" }
        )
    }
}
