package com.sucharu.sucharupro.data.api.model.profitability

import com.sucharu.sucharupro.domain.model.businessintegrity.Module16FinancialHandoffContract
import com.sucharu.sucharupro.domain.model.profitability.*
import com.sucharu.sucharupro.domain.service.profitability.ValidatedFinancialHandoff
import java.math.BigDecimal

data class GenerateProfitabilitySnapshotRequestDto(
    val scope: String,
    val targetEntityId: String? = null,
    val periodId: String? = null,
    val currency: String = "BDT",
    val customRevenue: BigDecimal? = null,
    val customDirectCost: BigDecimal? = null,
    val customIndirectCost: BigDecimal? = null,
    val baselineCost: BigDecimal? = null,
    val baselineRevenue: BigDecimal? = null,
    val revenueProvenances: List<RevenueProvenanceDto> = emptyList(),
    val costAttributions: List<CostAttributionReferenceDto> = emptyList(),
    val idempotencyKey: String? = null
)

data class RevenueProvenanceDto(
    val id: String = "",
    val tenantId: String = "",
    val projectId: String = "",
    val canonicalSourceType: String,
    val canonicalSourceId: String,
    val customerId: String? = null,
    val orderId: String? = null,
    val jobId: String? = null,
    val periodId: String? = null,
    val recognizedAmount: BigDecimal,
    val currency: String = "BDT",
    val recognitionState: String = "RECOGNIZED",
    val sourceTimestamp: Long = System.currentTimeMillis()
) {
    fun toDomain(tenant: String, project: String) = RevenueProvenance(
        id = if (id.isNotBlank()) id else "REV-${java.util.UUID.randomUUID()}",
        tenantId = if (tenantId.isNotBlank()) tenantId else tenant,
        projectId = if (projectId.isNotBlank()) projectId else project,
        canonicalSourceType = RevenueSourceType.valueOf(canonicalSourceType),
        canonicalSourceId = canonicalSourceId,
        customerId = customerId,
        orderId = orderId,
        jobId = jobId,
        periodId = periodId,
        recognizedAmount = recognizedAmount,
        currency = currency,
        recognitionState = recognitionState,
        sourceTimestamp = sourceTimestamp
    )

    companion object {
        fun fromDomain(d: RevenueProvenance) = RevenueProvenanceDto(
            id = d.id,
            tenantId = d.tenantId,
            projectId = d.projectId,
            canonicalSourceType = d.canonicalSourceType.name,
            canonicalSourceId = d.canonicalSourceId,
            customerId = d.customerId,
            orderId = d.orderId,
            jobId = d.jobId,
            periodId = d.periodId,
            recognizedAmount = d.recognizedAmount,
            currency = d.currency,
            recognitionState = d.recognitionState,
            sourceTimestamp = d.sourceTimestamp
        )
    }
}

data class CostAttributionReferenceDto(
    val id: String = "",
    val tenantId: String = "",
    val projectId: String = "",
    val sourceType: String,
    val sourceId: String,
    val componentType: String,
    val jobId: String? = null,
    val orderId: String? = null,
    val productId: String? = null,
    val customerId: String? = null,
    val vendorId: String? = null,
    val periodId: String? = null,
    val attributionBasis: String = "DIRECT",
    val sourceAmount: BigDecimal,
    val attributableAmount: BigDecimal,
    val currency: String = "BDT",
    val recordedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(tenant: String, project: String) = CostAttributionReference(
        id = if (id.isNotBlank()) id else "ATTR-${java.util.UUID.randomUUID()}",
        tenantId = if (tenantId.isNotBlank()) tenantId else tenant,
        projectId = if (projectId.isNotBlank()) projectId else project,
        sourceType = CostAttributionSourceType.valueOf(sourceType),
        sourceId = sourceId,
        componentType = CostComponentType.valueOf(componentType),
        jobId = jobId,
        orderId = orderId,
        productId = productId,
        customerId = customerId,
        vendorId = vendorId,
        periodId = periodId,
        attributionBasis = attributionBasis,
        sourceAmount = sourceAmount,
        attributableAmount = attributableAmount,
        currency = currency,
        recordedAt = recordedAt
    )

    companion object {
        fun fromDomain(d: CostAttributionReference) = CostAttributionReferenceDto(
            id = d.id,
            tenantId = d.tenantId,
            projectId = d.projectId,
            sourceType = d.sourceType.name,
            sourceId = d.sourceId,
            componentType = d.componentType.name,
            jobId = d.jobId,
            orderId = d.orderId,
            productId = d.productId,
            customerId = d.customerId,
            vendorId = d.vendorId,
            periodId = d.periodId,
            attributionBasis = d.attributionBasis,
            sourceAmount = d.sourceAmount,
            attributableAmount = d.attributableAmount,
            currency = d.currency,
            recordedAt = d.recordedAt
        )
    }
}

data class CostComponentBreakdownDto(
    val componentType: String,
    val totalAmount: BigDecimal,
    val percentageOfTotalCost: BigDecimal,
    val itemCount: Int
) {
    companion object {
        fun fromDomain(d: CostComponentBreakdown) = CostComponentBreakdownDto(
            componentType = d.componentType.name,
            totalAmount = d.totalAmount,
            percentageOfTotalCost = d.percentageOfTotalCost,
            itemCount = d.itemCount
        )
    }
}

data class ProfitabilityMetricDto(
    val revenue: BigDecimal,
    val directCost: BigDecimal,
    val indirectCost: BigDecimal,
    val totalCost: BigDecimal,
    val grossProfit: BigDecimal,
    val grossMarginPercentage: BigDecimal,
    val baselineCost: BigDecimal? = null,
    val costVariance: BigDecimal? = null,
    val revenueVariance: BigDecimal? = null,
    val marginVariance: BigDecimal? = null
) {
    companion object {
        fun fromDomain(d: ProfitabilityMetric) = ProfitabilityMetricDto(
            revenue = d.revenue,
            directCost = d.directCost,
            indirectCost = d.indirectCost,
            totalCost = d.totalCost,
            grossProfit = d.grossProfit,
            grossMarginPercentage = d.grossMarginPercentage,
            baselineCost = d.baselineCost,
            costVariance = d.costVariance,
            revenueVariance = d.revenueVariance,
            marginVariance = d.marginVariance
        )
    }
}

data class ProfitabilitySnapshotDto(
    val id: String,
    val tenantId: String,
    val projectId: String,
    val scope: String,
    val targetEntityId: String?,
    val periodId: String?,
    val currency: String,
    val metrics: ProfitabilityMetricDto,
    val costBreakdowns: List<CostComponentBreakdownDto>,
    val revenueProvenances: List<RevenueProvenanceDto>,
    val costAttributions: List<CostAttributionReferenceDto>,
    val calculationVersion: String,
    val sourceIntegrityStatus: String,
    val financialHandoffVerified: Boolean,
    val handoffChecksum: String?,
    val integrityNotes: List<String>,
    val generatedBy: String,
    val generatedAt: Long
) {
    companion object {
        fun fromDomain(d: ProfitabilitySnapshot) = ProfitabilitySnapshotDto(
            id = d.id,
            tenantId = d.tenantId,
            projectId = d.projectId,
            scope = d.scope.name,
            targetEntityId = d.targetEntityId,
            periodId = d.periodId,
            currency = d.currency,
            metrics = ProfitabilityMetricDto.fromDomain(d.metrics),
            costBreakdowns = d.costBreakdowns.map { CostComponentBreakdownDto.fromDomain(it) },
            revenueProvenances = d.revenueProvenances.map { RevenueProvenanceDto.fromDomain(it) },
            costAttributions = d.costAttributions.map { CostAttributionReferenceDto.fromDomain(it) },
            calculationVersion = d.calculationVersion,
            sourceIntegrityStatus = d.sourceIntegrityStatus.name,
            financialHandoffVerified = d.financialHandoffVerified,
            handoffChecksum = d.handoffChecksum,
            integrityNotes = d.integrityNotes,
            generatedBy = d.generatedBy,
            generatedAt = d.generatedAt
        )
    }
}

data class ProfitabilitySourceReadinessDto(
    val tenantId: String,
    val projectId: String,
    val periodId: String?,
    val module15HandoffStatus: String,
    val isLedgerBalanced: Boolean,
    val directExpensesAvailable: Boolean,
    val vendorPayablesAvailable: Boolean,
    val recognizedRevenueAvailable: Boolean,
    val costAllocationsAvailable: Boolean,
    val activeCommitmentsCount: Int,
    val outstandingAccrualsCount: Int,
    val periodClosed: Boolean,
    val warnings: List<String>,
    val evaluatedAt: Long
) {
    companion object {
        fun fromDomain(d: ProfitabilitySourceReadiness) = ProfitabilitySourceReadinessDto(
            tenantId = d.tenantId,
            projectId = d.projectId,
            periodId = d.periodId,
            module15HandoffStatus = d.module15HandoffStatus.name,
            isLedgerBalanced = d.isLedgerBalanced,
            directExpensesAvailable = d.directExpensesAvailable,
            vendorPayablesAvailable = d.vendorPayablesAvailable,
            recognizedRevenueAvailable = d.recognizedRevenueAvailable,
            costAllocationsAvailable = d.costAllocationsAvailable,
            activeCommitmentsCount = d.activeCommitmentsCount,
            outstandingAccrualsCount = d.outstandingAccrualsCount,
            periodClosed = d.periodClosed,
            warnings = d.warnings,
            evaluatedAt = d.evaluatedAt
        )
    }
}

data class ValidatedFinancialHandoffDto(
    val tenantId: String,
    val projectId: String,
    val periodId: String,
    val periodCode: String,
    val currency: String,
    val isPeriodClosed: Boolean,
    val closureCertificateChecksum: String?,
    val totalRecognizedRevenue: BigDecimal,
    val totalDirectExpenses: BigDecimal,
    val totalVendorPayablesSettled: BigDecimal,
    val totalRecognizedCostAllocations: BigDecimal,
    val totalActiveCommitmentExposure: BigDecimal,
    val totalOutstandingAccruals: BigDecimal,
    val netFinancialAdjustments: BigDecimal,
    val ledgerTotalDebit: BigDecimal,
    val ledgerTotalCredit: BigDecimal,
    val isLedgerBalanced: Boolean,
    val integrityStatus: String,
    val hasValidClosureCertificate: Boolean,
    val validationNotes: List<String>
) {
    companion object {
        fun fromDomain(d: ValidatedFinancialHandoff) = ValidatedFinancialHandoffDto(
            tenantId = d.contract.tenantId,
            projectId = d.contract.projectId,
            periodId = d.contract.periodId,
            periodCode = d.contract.periodCode,
            currency = d.contract.currency,
            isPeriodClosed = d.isPeriodClosed,
            closureCertificateChecksum = d.contract.closureCertificateChecksum,
            totalRecognizedRevenue = d.contract.totalRecognizedRevenue,
            totalDirectExpenses = d.contract.totalDirectExpenses,
            totalVendorPayablesSettled = d.contract.totalVendorPayablesSettled,
            totalRecognizedCostAllocations = d.contract.totalRecognizedCostAllocations,
            totalActiveCommitmentExposure = d.contract.totalActiveCommitmentExposure,
            totalOutstandingAccruals = d.contract.totalOutstandingAccruals,
            netFinancialAdjustments = d.contract.netFinancialAdjustments,
            ledgerTotalDebit = d.contract.ledgerTotalDebit,
            ledgerTotalCredit = d.contract.ledgerTotalCredit,
            isLedgerBalanced = d.isLedgerBalanced,
            integrityStatus = d.integrityStatus.name,
            hasValidClosureCertificate = d.hasValidClosureCertificate,
            validationNotes = d.validationNotes
        )
    }
}

data class ProfitabilityReconciliationEventDto(
    val id: String,
    val tenantId: String,
    val projectId: String,
    val snapshotId: String,
    val scope: String,
    val targetEntityId: String?,
    val periodId: String?,
    val isReconciled: Boolean,
    val canonicalRevenueTotal: BigDecimal,
    val snapshotRevenueTotal: BigDecimal,
    val revenueDifference: BigDecimal,
    val canonicalCostTotal: BigDecimal,
    val snapshotCostTotal: BigDecimal,
    val costDifference: BigDecimal,
    val discrepancies: List<String>,
    val checkedBy: String,
    val checkedAt: Long
) {
    companion object {
        fun fromDomain(d: ProfitabilityReconciliationEvent) = ProfitabilityReconciliationEventDto(
            id = d.id,
            tenantId = d.tenantId,
            projectId = d.projectId,
            snapshotId = d.snapshotId,
            scope = d.scope.name,
            targetEntityId = d.targetEntityId,
            periodId = d.periodId,
            isReconciled = d.isReconciled,
            canonicalRevenueTotal = d.canonicalRevenueTotal,
            snapshotRevenueTotal = d.snapshotRevenueTotal,
            revenueDifference = d.revenueDifference,
            canonicalCostTotal = d.canonicalCostTotal,
            snapshotCostTotal = d.snapshotCostTotal,
            costDifference = d.costDifference,
            discrepancies = d.discrepancies,
            checkedBy = d.checkedBy,
            checkedAt = d.checkedAt
        )
    }
}

data class ProfitabilityAuditEventDto(
    val id: String,
    val tenantId: String,
    val projectId: String,
    val snapshotId: String?,
    val action: String,
    val scope: String?,
    val targetEntityId: String?,
    val outcome: String,
    val details: String?,
    val actor: String,
    val timestamp: Long,
    val correlationId: String?
) {
    companion object {
        fun fromDomain(d: ProfitabilityAuditEvent) = ProfitabilityAuditEventDto(
            id = d.id,
            tenantId = d.tenantId,
            projectId = d.projectId,
            snapshotId = d.snapshotId,
            action = d.action,
            scope = d.scope?.name,
            targetEntityId = d.targetEntityId,
            outcome = d.outcome,
            details = d.details,
            actor = d.actor,
            timestamp = d.timestamp,
            correlationId = d.correlationId
        )
    }
}
