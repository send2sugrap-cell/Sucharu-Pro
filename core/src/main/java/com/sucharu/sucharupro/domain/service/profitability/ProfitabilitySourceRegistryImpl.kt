package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.*
import java.math.BigDecimal

/**
 * Production implementation of ProfitabilitySourceRegistry.
 */
class ProfitabilitySourceRegistryImpl(
    private val financialHandoffAdapter: Module16FinancialHandoffAdapter
) : ProfitabilitySourceRegistry {

    override suspend fun evaluateSourceReadiness(
        tenantId: String,
        projectId: String,
        periodId: String?
    ): DomainResult<ProfitabilitySourceReadiness> {
        if (tenantId.isBlank()) return DomainResult.Error(message = "Tenant ID cannot be blank")
        if (projectId.isBlank()) return DomainResult.Error(message = "Project ID cannot be blank")

        val warnings = mutableListOf<String>()
        var handoffStatus = SourceIntegrityStatus.VERIFIED
        var isLedgerBalanced = true
        var periodClosed = false
        var activeCommitmentsCount = 0
        var outstandingAccrualsCount = 0

        if (!periodId.isNullOrBlank()) {
            when (val handoffRes = financialHandoffAdapter.getVerifiedFinancialHandoff(tenantId, projectId, periodId)) {
                is DomainResult.Success -> {
                    val validated = handoffRes.data
                    handoffStatus = validated.integrityStatus
                    isLedgerBalanced = validated.isLedgerBalanced
                    periodClosed = validated.isPeriodClosed
                    warnings.addAll(validated.validationNotes)
                    if (validated.contract.totalActiveCommitmentExposure.compareTo(BigDecimal.ZERO) > 0) {
                        activeCommitmentsCount = 1
                    }
                    if (validated.contract.totalOutstandingAccruals.compareTo(BigDecimal.ZERO) > 0) {
                        outstandingAccrualsCount = 1
                    }
                }
                is DomainResult.Error -> {
                    handoffStatus = SourceIntegrityStatus.SOURCE_MISSING
                    warnings.add("Financial handoff could not be retrieved from Module 15: ${handoffRes.message}")
                }
                DomainResult.Loading -> {
                    handoffStatus = SourceIntegrityStatus.SOURCE_MISSING
                    warnings.add("Financial handoff operation is still loading")
                }
            }
        }

        val readiness = ProfitabilitySourceReadiness(
            tenantId = tenantId,
            projectId = projectId,
            periodId = periodId,
            module15HandoffStatus = handoffStatus,
            isLedgerBalanced = isLedgerBalanced,
            directExpensesAvailable = true,
            vendorPayablesAvailable = true,
            recognizedRevenueAvailable = true,
            costAllocationsAvailable = true,
            activeCommitmentsCount = activeCommitmentsCount,
            outstandingAccrualsCount = outstandingAccrualsCount,
            periodClosed = periodClosed,
            warnings = warnings
        )

        return DomainResult.Success(readiness)
    }

    override fun validateRevenueProvenance(
        provenance: RevenueProvenance
    ): DomainResult<Boolean> {
        if (provenance.tenantId.isBlank()) return DomainResult.Error(message = "Tenant ID is required for revenue provenance")
        if (provenance.projectId.isBlank()) return DomainResult.Error(message = "Project ID is required for revenue provenance")
        if (provenance.canonicalSourceId.isBlank()) return DomainResult.Error(message = "Canonical Source ID is required for revenue provenance")
        if (provenance.recognizedAmount.compareTo(BigDecimal.ZERO) < 0) {
            return DomainResult.Error(message = "Recognized revenue amount cannot be negative")
        }
        return DomainResult.Success(true)
    }

    override fun validateCostAttribution(
        attribution: CostAttributionReference
    ): DomainResult<Boolean> {
        if (attribution.tenantId.isBlank()) return DomainResult.Error(message = "Tenant ID is required for cost attribution")
        if (attribution.projectId.isBlank()) return DomainResult.Error(message = "Project ID is required for cost attribution")
        if (attribution.sourceId.isBlank()) return DomainResult.Error(message = "Source ID is required for cost attribution")
        if (attribution.attributableAmount.compareTo(BigDecimal.ZERO) < 0) {
            return DomainResult.Error(message = "Attributable cost amount cannot be negative")
        }
        return DomainResult.Success(true)
    }

    override fun detectDuplicateSources(
        revenues: List<RevenueProvenance>,
        costs: List<CostAttributionReference>
    ): List<String> {
        val duplicates = mutableListOf<String>()

        val revKeys = mutableSetOf<String>()
        revenues.forEach { rev ->
            val key = "${rev.canonicalSourceType}:${rev.canonicalSourceId}"
            if (!revKeys.add(key)) {
                duplicates.add("Duplicate revenue source reference detected: $key")
            }
        }

        val costKeys = mutableSetOf<String>()
        costs.forEach { cost ->
            val key = "${cost.sourceType}:${cost.sourceId}:${cost.componentType}:${cost.jobId.orEmpty()}"
            if (!costKeys.add(key)) {
                duplicates.add("Duplicate cost attribution reference detected: $key")
            }
        }

        return duplicates
    }
}
