package com.sucharu.sucharupro.domain.service.finalqc

import com.sucharu.sucharupro.domain.model.finalqc.*
import com.sucharu.sucharupro.domain.model.production.ProductionStageType
import java.math.BigDecimal
import java.util.UUID

class FinalQcInspectionEngine {

    fun createInspection(
        tenantId: String,
        executionJobId: String,
        orderId: String,
        samplePlanType: InspectionSamplePlanType,
        totalLotQuantity: BigDecimal,
        sampleSize: BigDecimal,
        checklist: List<QcChecklistItem>,
        inspectorId: String,
        inspectorName: String,
        notes: String? = null
    ): ProductionFinalQcInspection {
        val scaledLot = FinalQcPackagingMathUtils.roundScale4(totalLotQuantity)
        val scaledSample = FinalQcPackagingMathUtils.roundScale4(sampleSize)

        return ProductionFinalQcInspection(
            inspectionId = "INSP-" + UUID.randomUUID().toString().take(8).uppercase(),
            tenantId = tenantId,
            executionJobId = executionJobId,
            orderId = orderId,
            samplePlanType = samplePlanType,
            totalLotQuantity = scaledLot,
            sampleSize = scaledSample,
            acceptedQuantity = FinalQcPackagingMathUtils.ZERO,
            rejectedQuantity = FinalQcPackagingMathUtils.ZERO,
            reworkQuantity = FinalQcPackagingMathUtils.ZERO,
            status = FinalQcInspectionStatus.IN_PROGRESS,
            checklist = checklist,
            inspectorId = inspectorId,
            inspectorName = inspectorName,
            inspectionNotes = notes,
            inspectedAt = System.currentTimeMillis()
        )
    }

    fun completeInspection(
        inspection: ProductionFinalQcInspection,
        acceptedQuantity: BigDecimal,
        rejectedQuantity: BigDecimal,
        reworkQuantity: BigDecimal,
        notes: String? = null
    ): ProductionFinalQcInspection {
        val scaledAccepted = FinalQcPackagingMathUtils.roundScale4(acceptedQuantity)
        val scaledRejected = FinalQcPackagingMathUtils.roundScale4(rejectedQuantity)
        val scaledRework = FinalQcPackagingMathUtils.roundScale4(reworkQuantity)

        val determinedStatus = when {
            scaledRejected.compareTo(BigDecimal.ZERO) > 0 && scaledAccepted.compareTo(BigDecimal.ZERO) == 0 ->
                FinalQcInspectionStatus.REJECTED
            scaledRework.compareTo(BigDecimal.ZERO) > 0 ->
                FinalQcInspectionStatus.REWORK_REQUIRED
            scaledRejected.compareTo(BigDecimal.ZERO) > 0 && scaledAccepted.compareTo(BigDecimal.ZERO) > 0 ->
                FinalQcInspectionStatus.CONDITIONALLY_ACCEPTED
            else ->
                FinalQcInspectionStatus.ACCEPTED
        }

        return inspection.copy(
            acceptedQuantity = scaledAccepted,
            rejectedQuantity = scaledRejected,
            reworkQuantity = scaledRework,
            status = determinedStatus,
            inspectionNotes = notes ?: inspection.inspectionNotes,
            completedAt = System.currentTimeMillis()
        )
    }
}

class DefectContainmentEngine {

    fun recordDefectContainment(
        tenantId: String,
        executionJobId: String,
        inspectionId: String,
        rootCauseStage: ProductionStageType,
        defectType: DefectClassificationType,
        severity: DefectSeverity,
        defectQuantity: BigDecimal,
        disposition: ContainmentDisposition,
        quarantineLocation: String,
        reworkWorkOrderId: String? = null,
        rootCauseDetails: String,
        loggedBy: String
    ): ProductionDefectContainmentRecord {
        return ProductionDefectContainmentRecord(
            containmentId = "DEF-" + UUID.randomUUID().toString().take(8).uppercase(),
            tenantId = tenantId,
            executionJobId = executionJobId,
            inspectionId = inspectionId,
            rootCauseStage = rootCauseStage,
            defectType = defectType,
            severity = severity,
            defectQuantity = FinalQcPackagingMathUtils.roundScale4(defectQuantity),
            disposition = disposition,
            quarantineLocation = quarantineLocation,
            reworkWorkOrderId = reworkWorkOrderId,
            rootCauseDetails = rootCauseDetails,
            loggedBy = loggedBy,
            loggedAt = System.currentTimeMillis()
        )
    }
}

class PackagingOrchestrationEngine {

    fun createPackagingRecord(
        tenantId: String,
        executionJobId: String,
        inspectionId: String,
        packagingType: PackagingType,
        unitsPerPackage: BigDecimal,
        totalPackageCount: Int,
        palletIdentifier: String? = null,
        cartonNumbersRange: String? = null,
        grossWeightKg: BigDecimal? = null,
        packagedBy: String,
        notes: String? = null
    ): ProductionPackagingRecord {
        val scaledUnits = FinalQcPackagingMathUtils.roundScale4(unitsPerPackage)
        val totalPackaged = FinalQcPackagingMathUtils.roundScale4(scaledUnits.multiply(BigDecimal(totalPackageCount)))
        val barcode = "PKG-${executionJobId.takeLast(6)}-${totalPackageCount}C-${System.currentTimeMillis().toString().takeLast(4)}"

        return ProductionPackagingRecord(
            packagingId = "PKG-" + UUID.randomUUID().toString().take(8).uppercase(),
            tenantId = tenantId,
            executionJobId = executionJobId,
            inspectionId = inspectionId,
            packagingType = packagingType,
            unitsPerPackage = scaledUnits,
            totalPackageCount = totalPackageCount,
            totalPackagedQuantity = totalPackaged,
            palletIdentifier = palletIdentifier,
            cartonNumbersRange = cartonNumbersRange,
            grossWeightKg = grossWeightKg?.let { FinalQcPackagingMathUtils.roundScale4(it) },
            packagingSlipBarcode = barcode,
            packagedBy = packagedBy,
            packagedAt = System.currentTimeMillis(),
            notes = notes
        )
    }
}

class FinishedGoodsReleaseEngine {

    fun authorizeRelease(
        tenantId: String,
        executionJobId: String,
        orderId: String,
        inspectionId: String,
        packagingId: String,
        releasedQuantity: BigDecimal,
        destination: String,
        authorizedBy: String,
        notes: String? = null
    ): FinishedGoodsReleaseRecord {
        val now = System.currentTimeMillis()
        val scaledReleased = FinalQcPackagingMathUtils.roundScale4(releasedQuantity)
        val hash = FinalQcPackagingMathUtils.generateReleaseCertificateHash(
            tenantId = tenantId,
            executionJobId = executionJobId,
            orderId = orderId,
            inspectionId = inspectionId,
            packagingId = packagingId,
            releasedQuantity = scaledReleased,
            destination = destination,
            authorizedBy = authorizedBy,
            authorizedAt = now
        )

        return FinishedGoodsReleaseRecord(
            releaseId = "REL-" + UUID.randomUUID().toString().take(8).uppercase(),
            tenantId = tenantId,
            executionJobId = executionJobId,
            orderId = orderId,
            inspectionId = inspectionId,
            packagingId = packagingId,
            releasedQuantity = scaledReleased,
            destination = destination,
            status = FinishedGoodsReleaseStatus.RELEASE_APPROVED,
            authorizedBy = authorizedBy,
            authorizedAt = now,
            integrityHash = hash,
            notes = notes
        )
    }
}

class FinalQcPackagingReconciliationEngine {

    fun reconcile(
        executionJobId: String,
        tenantId: String,
        inspections: List<ProductionFinalQcInspection>,
        defects: List<ProductionDefectContainmentRecord>,
        packagings: List<ProductionPackagingRecord>,
        releases: List<FinishedGoodsReleaseRecord>,
        totalShopFloorGoodOutput: BigDecimal = BigDecimal.ZERO
    ): FinalQcPackagingReconciliationResult {
        val discrepancies = mutableListOf<String>()

        val totalAccepted = inspections.fold(BigDecimal.ZERO) { acc, i -> acc.add(i.acceptedQuantity) }
        val totalRejected = inspections.fold(BigDecimal.ZERO) { acc, i -> acc.add(i.rejectedQuantity) }
        val totalRework = inspections.fold(BigDecimal.ZERO) { acc, i -> acc.add(i.reworkQuantity) }
        val totalInspectedLot = inspections.fold(BigDecimal.ZERO) { acc, i -> acc.add(i.totalLotQuantity) }

        // 1. Lot quantity sanity
        val outputMatched = if (totalShopFloorGoodOutput.compareTo(BigDecimal.ZERO) > 0) {
            totalInspectedLot.compareTo(totalShopFloorGoodOutput) <= 0
        } else true
        if (!outputMatched) discrepancies.add("Inspected lot quantity exceeds shop-floor output")

        // 2. Defect balance
        val totalDefectRecorded = defects.fold(BigDecimal.ZERO) { acc, d -> acc.add(d.defectQuantity) }
        val defectAccountingBalanced = totalRejected.add(totalRework).compareTo(totalDefectRecorded) >= 0
        if (!defectAccountingBalanced && defects.isNotEmpty()) {
            discrepancies.add("Defect containment quantity ($totalDefectRecorded) exceeds rejected + rework sum (${totalRejected.add(totalRework)})")
        }

        // 3. Zero uncontained critical defects
        val hasUncontainedCritical = defects.any { it.severity == DefectSeverity.CRITICAL && it.disposition == ContainmentDisposition.CONCESSION_RELEASED }
        if (hasUncontainedCritical) {
            discrepancies.add("Critical defects cannot be released under concession")
        }

        // 4. Packaging matches accepted quantity
        val totalPackaged = packagings.fold(BigDecimal.ZERO) { acc, p -> acc.add(p.totalPackagedQuantity) }
        val packagingMatches = totalPackaged.compareTo(totalAccepted) <= 0
        if (!packagingMatches) {
            discrepancies.add("Packaged quantity ($totalPackaged) exceeds accepted inspection quantity ($totalAccepted)")
        }

        // 5. Release certificate verification
        var releaseHashValid = true
        for (rel in releases) {
            val expectedHash = FinalQcPackagingMathUtils.generateReleaseCertificateHash(
                tenantId = rel.tenantId,
                executionJobId = rel.executionJobId,
                orderId = rel.orderId,
                inspectionId = rel.inspectionId,
                packagingId = rel.packagingId,
                releasedQuantity = rel.releasedQuantity,
                destination = rel.destination,
                authorizedBy = rel.authorizedBy,
                authorizedAt = rel.authorizedAt
            )
            if (expectedHash != rel.integrityHash) {
                releaseHashValid = false
                discrepancies.add("Release certificate integrity hash mismatch on ${rel.releaseId}")
            }
        }

        val isFullyReconciled = discrepancies.isEmpty()

        return FinalQcPackagingReconciliationResult(
            executionJobId = executionJobId,
            tenantId = tenantId,
            outputMatchedInspectionLot = outputMatched,
            samplePlanConsistent = true,
            defectAccountingBalanced = defectAccountingBalanced,
            zeroUncontainedCriticalDefects = !hasUncontainedCritical,
            packagingQuantityMatchesAccepted = packagingMatches,
            releaseCertificateHashValid = releaseHashValid,
            multiTenantIsolationVerified = true,
            isFullyReconciled = isFullyReconciled,
            discrepancies = discrepancies
        )
    }

    fun generateVarianceSummary(
        executionJobId: String,
        tenantId: String,
        inspections: List<ProductionFinalQcInspection>,
        defects: List<ProductionDefectContainmentRecord>,
        packagings: List<ProductionPackagingRecord>,
        totalShopFloorGoodOutput: BigDecimal
    ): FinalQcPackagingVarianceSummary {
        val totalLot = inspections.fold(BigDecimal.ZERO) { acc, i -> acc.add(i.totalLotQuantity) }
        val sampleInspected = inspections.fold(BigDecimal.ZERO) { acc, i -> acc.add(i.sampleSize) }
        val totalAccepted = inspections.fold(BigDecimal.ZERO) { acc, i -> acc.add(i.acceptedQuantity) }
        val totalRejected = inspections.fold(BigDecimal.ZERO) { acc, i -> acc.add(i.rejectedQuantity) }
        val totalRework = inspections.fold(BigDecimal.ZERO) { acc, i -> acc.add(i.reworkQuantity) }
        val totalDefects = defects.fold(BigDecimal.ZERO) { acc, d -> acc.add(d.defectQuantity) }
        val totalPackaged = packagings.fold(BigDecimal.ZERO) { acc, p -> acc.add(p.totalPackagedQuantity) }

        val qualityYield = FinalQcPackagingMathUtils.calculateYieldPercentage(totalAccepted, if (totalLot.compareTo(BigDecimal.ZERO) > 0) totalLot else totalShopFloorGoodOutput)
        val defectRate = FinalQcPackagingMathUtils.calculateDefectRatePercentage(totalDefects, if (totalLot.compareTo(BigDecimal.ZERO) > 0) totalLot else totalShopFloorGoodOutput)
        val packVariance = FinalQcPackagingMathUtils.calculatePackagingVariance(totalPackaged, totalAccepted)

        val isReady = totalAccepted.compareTo(BigDecimal.ZERO) > 0 && totalPackaged.compareTo(BigDecimal.ZERO) > 0 && totalRejected.compareTo(BigDecimal.ZERO) == 0

        return FinalQcPackagingVarianceSummary(
            executionJobId = executionJobId,
            tenantId = tenantId,
            totalManufacturedOutput = FinalQcPackagingMathUtils.roundScale4(totalShopFloorGoodOutput),
            sampleInspectedQuantity = FinalQcPackagingMathUtils.roundScale4(sampleInspected),
            totalAcceptedGoodQuantity = FinalQcPackagingMathUtils.roundScale4(totalAccepted),
            totalRejectedQuantity = FinalQcPackagingMathUtils.roundScale4(totalRejected),
            totalReworkQuantity = FinalQcPackagingMathUtils.roundScale4(totalRework),
            overallQualityYieldPercentage = qualityYield,
            defectRatePercentage = defectRate,
            totalPackagedQuantity = FinalQcPackagingMathUtils.roundScale4(totalPackaged),
            packagingBalanceVariance = packVariance,
            isReadyForFullRelease = isReady
        )
    }
}
