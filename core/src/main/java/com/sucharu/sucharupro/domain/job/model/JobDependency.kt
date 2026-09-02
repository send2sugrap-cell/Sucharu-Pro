package com.sucharu.sucharupro.domain.job.model

import java.util.UUID

/**
 * Required status condition for a parent job to satisfy dependency.
 */
enum class DependencyRequirement {
    ON_SUCCESS,
    ON_FAILURE,
    ON_COMPLETION // Either Success or Dead-Letter
}

/**
 * Explicit workflow dependency link (INFRA-04 Step 04).
 */
data class JobDependencyLink(
    val dependencyId: String = UUID.randomUUID().toString(),
    val projectId: String,
    val jobId: String,                 // Dependent child job
    val dependsOnJobId: String,         // Prerequisite parent job
    val requirement: DependencyRequirement = DependencyRequirement.ON_SUCCESS,
    val isSatisfied: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    init {
        require(dependencyId.isNotBlank()) { "dependencyId cannot be blank" }
        require(projectId.isNotBlank()) { "projectId cannot be blank" }
        require(jobId.isNotBlank()) { "jobId cannot be blank" }
        require(dependsOnJobId.isNotBlank()) { "dependsOnJobId cannot be blank" }
        require(jobId != dependsOnJobId) { "Job cannot depend on itself ($jobId)" }
    }
}
