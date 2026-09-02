package com.sucharu.sucharupro.domain.validation.profitability

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.JobCostComponent
import com.sucharu.sucharupro.domain.model.profitability.JobCostProvenance

/**
 * Validation rules for Job Actual Cost Engine (Module 16 Step 02).
 */
object JobCostValidator {

    fun validateJobCostCalculationRequest(
        tenantId: String,
        projectId: String,
        jobId: String,
        currency: String
    ): DomainResult<Boolean> {
        if (tenantId.isBlank()) {
            return DomainResult.Error(message = "Tenant ID cannot be blank for Job cost calculation")
        }
        if (projectId.isBlank()) {
            return DomainResult.Error(message = "Project ID cannot be blank for Job cost calculation")
        }
        if (jobId.isBlank()) {
            return DomainResult.Error(message = "Job ID cannot be blank for Job cost calculation")
        }
        if (currency.isBlank()) {
            return DomainResult.Error(message = "Currency cannot be blank")
        }
        return DomainResult.Success(true)
    }

    fun detectDuplicateProvenances(provenances: List<JobCostProvenance>): List<String> {
        val duplicates = mutableListOf<String>()
        val seenFingerprints = mutableSetOf<String>()

        provenances.forEach { p ->
            val fp = p.fingerprintHash.ifBlank {
                "${p.sourceModule}:${p.sourceEntityType}:${p.sourceEntityId}:${p.sourceTransactionId.orEmpty()}:${p.costComponentType.name}"
            }
            if (!seenFingerprints.add(fp)) {
                duplicates.add("Duplicate cost source fingerprint detected: $fp (entityId: ${p.sourceEntityId})")
            }
        }
        return duplicates
    }

    fun validateComponentProvenanceIntegrity(component: JobCostComponent): List<String> {
        val errors = mutableListOf<String>()
        if (component.tenantId.isBlank()) errors.add("Component ${component.componentId} missing tenantId")
        if (component.projectId.isBlank()) errors.add("Component ${component.componentId} missing projectId")
        if (component.jobId.isBlank()) errors.add("Component ${component.componentId} missing jobId")
        return errors
    }
}
