package com.sucharu.sucharupro.domain.validation.profitability

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.ExecutiveProfitabilitySnapshot

/**
 * Domain Validator for Executive Profitability Engine.
 * Module 16 Step 10.
 */
object ExecutiveProfitabilityValidator {

    fun validateTenantContext(tenantId: String?, projectId: String?): DomainResult<Unit> {
        if (tenantId.isNullOrBlank()) {
            return DomainResult.Error(message = "Tenant ID is required and cannot be blank.")
        }
        if (projectId.isNullOrBlank()) {
            return DomainResult.Error(message = "Project ID is required and cannot be blank.")
        }
        return DomainResult.Success(Unit)
    }

    fun validatePeriodId(periodId: String?): DomainResult<Unit> {
        if (periodId == null) return DomainResult.Success(Unit)
        if (periodId.isBlank()) {
            return DomainResult.Error(message = "Period ID if provided must not be blank.")
        }
        val regex = Regex("^(\\d{4}-(M(0[1-9]|1[0-2])|Q[1-4]|Y))|(\\d{4}-\\d{2}-\\d{2})$")
        return if (regex.matches(periodId) || periodId.length in 4..32) {
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(message = "Period ID '$periodId' is not formatted in a recognized fiscal or calendar period standard.")
        }
    }

    fun validateSnapshotIntegrity(snapshot: ExecutiveProfitabilitySnapshot): DomainResult<Unit> {
        if (snapshot.snapshotId.isBlank()) {
            return DomainResult.Error(message = "Snapshot ID cannot be blank.")
        }
        if (snapshot.integrityHash.isBlank()) {
            return DomainResult.Error(message = "Snapshot integrity hash cannot be blank.")
        }
        if (snapshot.sourceFingerprint.isBlank()) {
            return DomainResult.Error(message = "Snapshot source fingerprint cannot be blank.")
        }
        return DomainResult.Success(Unit)
    }
}
