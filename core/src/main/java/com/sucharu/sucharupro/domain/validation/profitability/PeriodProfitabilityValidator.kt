package com.sucharu.sucharupro.domain.validation.profitability

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.PeriodProfitabilityProvenanceRecord
import java.math.BigDecimal

/**
 * Domain validation for Period Profitability requests, boundaries, and provenances.
 * Module 16 Step 06.
 */
object PeriodProfitabilityValidator {

    fun validateCalculateRequest(
        tenantId: String,
        projectId: String,
        periodId: String,
        periodStart: Long,
        periodEnd: Long,
        customBaselineRevenue: BigDecimal? = null,
        customBaselineCost: BigDecimal? = null
    ): DomainResult<Unit> {
        val errors = mutableListOf<String>()

        if (tenantId.isBlank()) errors.add("Tenant ID must not be blank")
        if (projectId.isBlank()) errors.add("Project ID must not be blank")
        if (periodId.isBlank()) errors.add("Period ID must not be blank")
        if (periodStart >= periodEnd) errors.add("Period start must be strictly before period end [periodStart, periodEnd)")
        if (customBaselineRevenue != null && customBaselineRevenue < BigDecimal.ZERO) {
            errors.add("Custom baseline revenue cannot be negative")
        }
        if (customBaselineCost != null && customBaselineCost < BigDecimal.ZERO) {
            errors.add("Custom baseline cost cannot be negative")
        }

        return if (errors.isEmpty()) {
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(message = errors.joinToString("; "))
        }
    }

    fun validateProvenanceRecord(record: PeriodProfitabilityProvenanceRecord): DomainResult<Unit> {
        val errors = mutableListOf<String>()

        if (record.provenanceId.isBlank()) errors.add("Provenance ID must not be blank")
        if (record.tenantId.isBlank()) errors.add("Tenant ID must not be blank")
        if (record.projectId.isBlank()) errors.add("Project ID must not be blank")
        if (record.periodId.isBlank()) errors.add("Period ID must not be blank")
        if (record.amount < BigDecimal.ZERO) errors.add("Provenance amount cannot be negative")
        if (record.sourceEntityId.isBlank()) errors.add("Source entity ID must not be blank")
        if (record.fingerprint.isBlank()) errors.add("Provenance fingerprint must not be blank")

        return if (errors.isEmpty()) {
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(message = errors.joinToString("; "))
        }
    }
}
