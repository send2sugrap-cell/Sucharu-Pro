package com.sucharu.sucharupro.domain.model.design

import com.sucharu.sucharupro.domain.model.job.ProductionJob

/**
 * Core domain entity representing a Design Project in Sucharu Pro ERP (Module 05).
 *
 * Belongs strictly to an existing [ProductionJob] without duplicating Customer or Order aggregates.
 * Tracks designer assignment and the creative design lifecycle.
 */
data class DesignProject(
    val projectId: String,
    val projectNumber: String,
    val productionJobId: String,
    val orderId: String,
    val orderNumber: String,
    val customerId: String,
    val title: String,
    val status: DesignStatus = DesignStatus.NOT_STARTED,
    val assignedDesignerId: String? = null,
    val assignedDesignerName: String? = null,
    val notes: String? = null,
    val startedAt: String? = null,
    val completedAt: String? = null,
    val createdAt: String,
    val createdBy: String? = null,
    val updatedAt: String,
    val updatedBy: String? = null
) {
    init {
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(projectNumber.isNotBlank()) { "Project Number cannot be blank." }
        require(productionJobId.isNotBlank()) { "Production Job ID cannot be blank." }
        require(orderId.isNotBlank()) { "Order ID cannot be blank." }
        require(customerId.isNotBlank()) { "Customer ID cannot be blank." }
        require(title.isNotBlank()) { "Title cannot be blank." }
        require(createdAt.isNotBlank()) { "Creation timestamp cannot be blank." }
        require(updatedAt.isNotBlank()) { "Update timestamp cannot be blank." }
    }

    /** Convenience indicator whether a designer is actively assigned. */
    val isAssigned: Boolean get() = !assignedDesignerId.isNullOrBlank()

    /** Convenience indicator whether the project is actively in design phase. */
    val isInProgress: Boolean get() = status == DesignStatus.IN_DESIGN

    /** Convenience indicator whether the project is terminal. */
    val isTerminal: Boolean get() = status.isTerminal

    companion object {
        /**
         * Factory function to create a [DesignProject] from an existing [ProductionJob].
         */
        fun fromProductionJob(
            projectId: String,
            projectNumber: String,
            job: ProductionJob,
            title: String? = null,
            notes: String? = null,
            createdBy: String? = null,
            timestamp: String
        ): DesignProject {
            return DesignProject(
                projectId = projectId,
                projectNumber = projectNumber,
                productionJobId = job.jobId,
                orderId = job.orderId,
                orderNumber = job.orderNumber,
                customerId = job.customerId,
                title = title?.takeIf { it.isNotBlank() } ?: "Design for Job ${job.jobNumber}",
                status = DesignStatus.NOT_STARTED,
                assignedDesignerId = null,
                assignedDesignerName = null,
                notes = notes,
                startedAt = null,
                completedAt = null,
                createdAt = timestamp,
                createdBy = createdBy,
                updatedAt = timestamp,
                updatedBy = createdBy
            )
        }
    }
}
