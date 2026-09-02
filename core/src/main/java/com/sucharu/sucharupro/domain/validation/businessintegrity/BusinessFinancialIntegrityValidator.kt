package com.sucharu.sucharupro.domain.validation.businessintegrity

import com.sucharu.sucharupro.domain.model.businessintegrity.*
import com.sucharu.sucharupro.domain.model.common.DomainResult
import java.security.MessageDigest

/**
 * Domain validators for Module 15 Step 10 Financial Integrity and Period Finalization.
 */
object BusinessFinancialIntegrityValidator {

    fun validateRunCreation(
        tenantId: String,
        projectId: String,
        periodId: String,
        runNumber: String,
        actorId: String
    ): DomainResult<Unit> {
        if (tenantId.isBlank()) return DomainResult.Error(message = "Tenant ID cannot be blank.")
        if (projectId.isBlank()) return DomainResult.Error(message = "Project ID cannot be blank.")
        if (periodId.isBlank()) return DomainResult.Error(message = "Period ID cannot be blank.")
        if (runNumber.isBlank()) return DomainResult.Error(message = "Run Number cannot be blank.")
        if (actorId.isBlank()) return DomainResult.Error(message = "Actor ID cannot be blank.")
        return DomainResult.Success(Unit)
    }

    fun validatePeriodFinalization(
        periodId: String,
        readiness: PeriodFinalizationReadiness,
        requesterId: String,
        finalizerId: String,
        finalizerRole: String
    ): DomainResult<Unit> {
        if (periodId.isBlank()) return DomainResult.Error(message = "Period ID cannot be blank.")
        if (finalizerId.isBlank()) return DomainResult.Error(message = "Finalizer ID cannot be blank.")

        // Role authorization
        if (finalizerRole != "ADMIN" && finalizerRole != "MANAGER") {
            return DomainResult.Error(message = "Only ADMIN or MANAGER roles are authorized to finalize period closure (current: $finalizerRole).")
        }

        // Separation of Duties (SoD): Requester cannot self-approve / finalize
        if (requesterId.isNotBlank() && requesterId == finalizerId) {
            return DomainResult.Error(message = "Separation of Duties violation: The creator/requester of period closing cannot finalize the closure.")
        }

        // Readiness verification
        if (!readiness.isReadyForClose) {
            val issuesStr = readiness.blockingReasons.joinToString("; ")
            return DomainResult.Error(message = "Period '$periodId' is not ready for final closure. Blocking issues: $issuesStr")
        }

        return DomainResult.Success(Unit)
    }

    fun verifySnapshotChecksum(
        payloadJson: String,
        expectedChecksum: String
    ): Boolean {
        if (payloadJson.isBlank() || expectedChecksum.isBlank()) return false
        val computed = calculateSha256(payloadJson)
        return computed.equals(expectedChecksum, ignoreCase = true)
    }

    fun calculateSha256(input: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
