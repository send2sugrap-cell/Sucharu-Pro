package com.sucharu.sucharupro.data.api.model.businessintegrity

import com.sucharu.sucharupro.domain.model.businessintegrity.*
import java.math.BigDecimal

data class ExecuteIntegrityRunRequestDto(
    val periodId: String = "",
    val notes: String? = null,
    val idempotencyKey: String? = null
)

data class FinalizePeriodCloseRequestDto(
    val requesterId: String = "",
    val notes: String? = null,
    val idempotencyKey: String? = null
)

data class FinancialControlAssertionDto(
    val id: String = "",
    val tenantId: String = "",
    val projectId: String = "",
    val runId: String = "",
    val periodId: String = "",
    val assertionType: String = "",
    val assertionName: String = "",
    val status: String = "",
    val severity: String = "",
    val expectedValue: String = "",
    val actualValue: String = "",
    val varianceValue: String? = null,
    val explanation: String = "",
    val recommendedAction: String? = null,
    val sourceEntitiesCount: Int = 0,
    val evaluatedAt: Long = 0L
) {
    companion object {
        fun fromDomain(domain: FinancialControlAssertion) = FinancialControlAssertionDto(
            id = domain.id,
            tenantId = domain.tenantId,
            projectId = domain.projectId,
            runId = domain.runId,
            periodId = domain.periodId,
            assertionType = domain.assertionType.name,
            assertionName = domain.assertionName,
            status = domain.status.name,
            severity = domain.severity.name,
            expectedValue = domain.expectedValue,
            actualValue = domain.actualValue,
            varianceValue = domain.varianceValue,
            explanation = domain.explanation,
            recommendedAction = domain.recommendedAction,
            sourceEntitiesCount = domain.sourceEntitiesCount,
            evaluatedAt = domain.evaluatedAt
        )
    }

    fun toDomain() = FinancialControlAssertion(
        id = id,
        tenantId = tenantId,
        projectId = projectId,
        runId = runId,
        periodId = periodId,
        assertionType = FinancialAssertionType.valueOf(assertionType),
        assertionName = assertionName,
        status = FinancialIntegrityStatus.valueOf(status),
        severity = AssertionSeverity.valueOf(severity),
        expectedValue = expectedValue,
        actualValue = actualValue,
        varianceValue = varianceValue,
        explanation = explanation,
        recommendedAction = recommendedAction,
        sourceEntitiesCount = sourceEntitiesCount,
        evaluatedAt = evaluatedAt
    )
}

data class FinancialIntegrityRunDto(
    val id: String = "",
    val tenantId: String = "",
    val projectId: String = "",
    val periodId: String = "",
    val runNumber: String = "",
    val status: String = "",
    val executedBy: String = "",
    val startedAt: Long = 0L,
    val completedAt: Long? = null,
    val totalAssertionsCount: Int = 18,
    val passedAssertionsCount: Int = 0,
    val warningAssertionsCount: Int = 0,
    val failedAssertionsCount: Int = 0,
    val blockedAssertionsCount: Int = 0,
    val integrityChecksum: String = "",
    val notes: String? = null,
    val assertions: List<FinancialControlAssertionDto> = emptyList()
) {
    companion object {
        fun fromDomain(domain: FinancialIntegrityRun) = FinancialIntegrityRunDto(
            id = domain.id,
            tenantId = domain.tenantId,
            projectId = domain.projectId,
            periodId = domain.periodId,
            runNumber = domain.runNumber,
            status = domain.status.name,
            executedBy = domain.executedBy,
            startedAt = domain.startedAt,
            completedAt = domain.completedAt,
            totalAssertionsCount = domain.totalAssertionsCount,
            passedAssertionsCount = domain.passedAssertionsCount,
            warningAssertionsCount = domain.warningAssertionsCount,
            failedAssertionsCount = domain.failedAssertionsCount,
            blockedAssertionsCount = domain.blockedAssertionsCount,
            integrityChecksum = domain.integrityChecksum,
            notes = domain.notes,
            assertions = domain.assertions.map { FinancialControlAssertionDto.fromDomain(it) }
        )
    }

    fun toDomain() = FinancialIntegrityRun(
        id = id,
        tenantId = tenantId,
        projectId = projectId,
        periodId = periodId,
        runNumber = runNumber,
        status = FinancialIntegrityStatus.valueOf(status),
        executedBy = executedBy,
        startedAt = startedAt,
        completedAt = completedAt,
        totalAssertionsCount = totalAssertionsCount,
        passedAssertionsCount = passedAssertionsCount,
        warningAssertionsCount = warningAssertionsCount,
        failedAssertionsCount = failedAssertionsCount,
        blockedAssertionsCount = blockedAssertionsCount,
        integrityChecksum = integrityChecksum,
        notes = notes,
        assertions = assertions.map { it.toDomain() }
    )
}

data class PeriodCloseCertificateDto(
    val id: String = "",
    val tenantId: String = "",
    val projectId: String = "",
    val periodId: String = "",
    val periodCode: String = "",
    val finalRunId: String = "",
    val closedBy: String = "",
    val closedAt: Long = 0L,
    val approvedBy: String = "",
    val approvedAt: Long = 0L,
    val status: String = "FINALIZED",
    val totalRecognizedExpenses: BigDecimal = BigDecimal.ZERO,
    val totalSettledPayables: BigDecimal = BigDecimal.ZERO,
    val totalLedgerDebit: BigDecimal = BigDecimal.ZERO,
    val totalLedgerCredit: BigDecimal = BigDecimal.ZERO,
    val netRecognizedAdjustments: BigDecimal = BigDecimal.ZERO,
    val certificateChecksum: String = "",
    val snapshotPayloadJson: String = "",
    val notes: String? = null
) {
    companion object {
        fun fromDomain(domain: PeriodCloseCertificate) = PeriodCloseCertificateDto(
            id = domain.id,
            tenantId = domain.tenantId,
            projectId = domain.projectId,
            periodId = domain.periodId,
            periodCode = domain.periodCode,
            finalRunId = domain.finalRunId,
            closedBy = domain.closedBy,
            closedAt = domain.closedAt,
            approvedBy = domain.approvedBy,
            approvedAt = domain.approvedAt,
            status = domain.status,
            totalRecognizedExpenses = domain.totalRecognizedExpenses,
            totalSettledPayables = domain.totalSettledPayables,
            totalLedgerDebit = domain.totalLedgerDebit,
            totalLedgerCredit = domain.totalLedgerCredit,
            netRecognizedAdjustments = domain.netRecognizedAdjustments,
            certificateChecksum = domain.certificateChecksum,
            snapshotPayloadJson = domain.snapshotPayloadJson,
            notes = domain.notes
        )
    }

    fun toDomain() = PeriodCloseCertificate(
        id = id,
        tenantId = tenantId,
        projectId = projectId,
        periodId = periodId,
        periodCode = periodCode,
        finalRunId = finalRunId,
        closedBy = closedBy,
        closedAt = closedAt,
        approvedBy = approvedBy,
        approvedAt = approvedAt,
        status = status,
        totalRecognizedExpenses = totalRecognizedExpenses,
        totalSettledPayables = totalSettledPayables,
        totalLedgerDebit = totalLedgerDebit,
        totalLedgerCredit = totalLedgerCredit,
        netRecognizedAdjustments = netRecognizedAdjustments,
        certificateChecksum = certificateChecksum,
        snapshotPayloadJson = snapshotPayloadJson,
        notes = notes
    )
}

data class PeriodFinalizationReadinessDto(
    val periodId: String = "",
    val periodCode: String = "",
    val status: String = "",
    val isReadyForClose: Boolean = false,
    val blockingReasons: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
    val latestRunId: String? = null,
    val latestRunStatus: String? = null,
    val totalAssertionsCount: Int = 18,
    val failedAssertionsCount: Int = 0,
    val warningAssertionsCount: Int = 0,
    val evaluatedAt: Long = 0L
) {
    companion object {
        fun fromDomain(domain: PeriodFinalizationReadiness) = PeriodFinalizationReadinessDto(
            periodId = domain.periodId,
            periodCode = domain.periodCode,
            status = domain.status.name,
            isReadyForClose = domain.isReadyForClose,
            blockingReasons = domain.blockingReasons,
            warnings = domain.warnings,
            latestRunId = domain.latestRunId,
            latestRunStatus = domain.latestRunStatus?.name,
            totalAssertionsCount = domain.totalAssertionsCount,
            failedAssertionsCount = domain.failedAssertionsCount,
            warningAssertionsCount = domain.warningAssertionsCount,
            evaluatedAt = domain.evaluatedAt
        )
    }

    fun toDomain() = PeriodFinalizationReadiness(
        periodId = periodId,
        periodCode = periodCode,
        status = PeriodClosureStatus.valueOf(status),
        isReadyForClose = isReadyForClose,
        blockingReasons = blockingReasons,
        warnings = warnings,
        latestRunId = latestRunId,
        latestRunStatus = latestRunStatus?.let { FinancialIntegrityStatus.valueOf(it) },
        totalAssertionsCount = totalAssertionsCount,
        failedAssertionsCount = failedAssertionsCount,
        warningAssertionsCount = warningAssertionsCount,
        evaluatedAt = evaluatedAt
    )
}

data class Module16FinancialHandoffContractDto(
    val tenantId: String = "",
    val projectId: String = "",
    val periodId: String = "",
    val periodCode: String = "",
    val currency: String = "BDT",
    val isPeriodClosed: Boolean = false,
    val closureCertificateChecksum: String? = null,
    val totalRecognizedRevenue: BigDecimal = BigDecimal.ZERO,
    val totalDirectExpenses: BigDecimal = BigDecimal.ZERO,
    val totalVendorPayablesSettled: BigDecimal = BigDecimal.ZERO,
    val totalRecognizedCostAllocations: BigDecimal = BigDecimal.ZERO,
    val totalActiveCommitmentExposure: BigDecimal = BigDecimal.ZERO,
    val totalOutstandingAccruals: BigDecimal = BigDecimal.ZERO,
    val netFinancialAdjustments: BigDecimal = BigDecimal.ZERO,
    val ledgerTotalDebit: BigDecimal = BigDecimal.ZERO,
    val ledgerTotalCredit: BigDecimal = BigDecimal.ZERO,
    val isLedgerBalanced: Boolean = false,
    val allocatedJobCostsCount: Int = 0,
    val verifiedAt: Long = 0L
) {
    companion object {
        fun fromDomain(domain: Module16FinancialHandoffContract) = Module16FinancialHandoffContractDto(
            tenantId = domain.tenantId,
            projectId = domain.projectId,
            periodId = domain.periodId,
            periodCode = domain.periodCode,
            currency = domain.currency,
            isPeriodClosed = domain.isPeriodClosed,
            closureCertificateChecksum = domain.closureCertificateChecksum,
            totalRecognizedRevenue = domain.totalRecognizedRevenue,
            totalDirectExpenses = domain.totalDirectExpenses,
            totalVendorPayablesSettled = domain.totalVendorPayablesSettled,
            totalRecognizedCostAllocations = domain.totalRecognizedCostAllocations,
            totalActiveCommitmentExposure = domain.totalActiveCommitmentExposure,
            totalOutstandingAccruals = domain.totalOutstandingAccruals,
            netFinancialAdjustments = domain.netFinancialAdjustments,
            ledgerTotalDebit = domain.ledgerTotalDebit,
            ledgerTotalCredit = domain.ledgerTotalCredit,
            isLedgerBalanced = domain.isLedgerBalanced,
            allocatedJobCostsCount = domain.allocatedJobCostsCount,
            verifiedAt = domain.verifiedAt
        )
    }

    fun toDomain() = Module16FinancialHandoffContract(
        tenantId = tenantId,
        projectId = projectId,
        periodId = periodId,
        periodCode = periodCode,
        currency = currency,
        isPeriodClosed = isPeriodClosed,
        closureCertificateChecksum = closureCertificateChecksum,
        totalRecognizedRevenue = totalRecognizedRevenue,
        totalDirectExpenses = totalDirectExpenses,
        totalVendorPayablesSettled = totalVendorPayablesSettled,
        totalRecognizedCostAllocations = totalRecognizedCostAllocations,
        totalActiveCommitmentExposure = totalActiveCommitmentExposure,
        totalOutstandingAccruals = totalOutstandingAccruals,
        netFinancialAdjustments = netFinancialAdjustments,
        ledgerTotalDebit = ledgerTotalDebit,
        ledgerTotalCredit = ledgerTotalCredit,
        isLedgerBalanced = isLedgerBalanced,
        allocatedJobCostsCount = allocatedJobCostsCount,
        verifiedAt = verifiedAt
    )
}
