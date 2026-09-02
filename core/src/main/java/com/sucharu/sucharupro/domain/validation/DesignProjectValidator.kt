package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.design.DesignProject
import com.sucharu.sucharupro.domain.model.design.DesignStatus
import com.sucharu.sucharupro.domain.model.job.ProductionJob

/**
 * Domain integrity and creation validator for [DesignProject].
 */
object DesignProjectValidator {

    /**
     * Validates domain constraints on a [DesignProject].
     */
    fun validateProject(project: DesignProject): DomainResult<Unit> {
        if (project.projectId.isBlank()) {
            return DomainResult.Error(message = "Design Project ID cannot be blank.")
        }
        if (project.projectNumber.isBlank()) {
            return DomainResult.Error(message = "Design Project Number cannot be blank.")
        }
        if (project.productionJobId.isBlank()) {
            return DomainResult.Error(message = "Production Job ID cannot be blank.")
        }
        if (project.orderId.isBlank()) {
            return DomainResult.Error(message = "Order ID cannot be blank.")
        }
        if (project.customerId.isBlank()) {
            return DomainResult.Error(message = "Customer ID cannot be blank.")
        }
        if (project.title.isBlank()) {
            return DomainResult.Error(message = "Design Project title cannot be blank.")
        }
        if (project.createdAt.isBlank()) {
            return DomainResult.Error(message = "Creation timestamp cannot be blank.")
        }
        if (project.updatedAt.isBlank()) {
            return DomainResult.Error(message = "Update timestamp cannot be blank.")
        }

        return DomainResult.Success(Unit)
    }

    /**
     * Validates eligibility to create a new [DesignProject] for a given [ProductionJob].
     */
    fun validateCreation(
        job: ProductionJob,
        existingProjects: List<DesignProject>
    ): DomainResult<Unit> {
        if (job.status.isTerminal) {
            return DomainResult.Error(
                message = "Cannot create Design Project for a ${job.status.defaultLabel} production job."
            )
        }

        // Prevent duplicate active DesignProjects for the same ProductionJob
        val existingActive = existingProjects.find {
            it.productionJobId == job.jobId && it.status != DesignStatus.CANCELLED
        }
        if (existingActive != null) {
            return DomainResult.Error(
                message = "Active Design Project '${existingActive.projectNumber}' already exists for Production Job '${job.jobNumber}'."
            )
        }

        return DomainResult.Success(Unit)
    }
}
