package com.sucharu.sucharupro.domain.job.workflow

import com.sucharu.sucharupro.domain.job.model.DependencyRequirement
import com.sucharu.sucharupro.domain.job.model.JobDependencyLink
import com.sucharu.sucharupro.domain.job.model.JobStatus

/**
 * Domain coordinator for managing workflow DAG dependency linkages and satisfaction (INFRA-04 Step 04).
 */
class JobDependencyManager {

    /**
     * Validates that adding a dependency link does not create a circular cycle.
     */
    fun validateNoCycle(
        existingLinks: List<JobDependencyLink>,
        newChildJobId: String,
        newParentJobId: String
    ) {
        val edgeMap = mutableMapOf<String, MutableSet<String>>()
        for (link in existingLinks) {
            edgeMap.computeIfAbsent(link.jobId) { mutableSetOf() }.add(link.dependsOnJobId)
        }

        if (DependencyCycleDetector.wouldCreateCycle(edgeMap, newChildJobId, newParentJobId)) {
            throw IllegalArgumentException(
                "Circular dependency detected: Job '$newChildJobId' cannot depend on '$newParentJobId' as it creates a cycle."
            )
        }
    }

    /**
     * Determines if a dependency link is satisfied given the parent job's current status.
     */
    fun isLinkSatisfied(link: JobDependencyLink, parentStatus: JobStatus): Boolean {
        return when (link.requirement) {
            DependencyRequirement.ON_SUCCESS -> parentStatus == JobStatus.SUCCEEDED
            DependencyRequirement.ON_FAILURE -> parentStatus == JobStatus.FAILED || parentStatus == JobStatus.DEAD_LETTER
            DependencyRequirement.ON_COMPLETION -> parentStatus.isTerminal
        }
    }

    /**
     * Evaluates whether all prerequisite dependencies for a child job are satisfied.
     */
    fun areAllDependenciesSatisfied(
        dependencies: List<JobDependencyLink>,
        parentStatuses: Map<String, JobStatus>
    ): Boolean {
        if (dependencies.isEmpty()) return true

        return dependencies.all { link ->
            val parentStatus = parentStatuses[link.dependsOnJobId] ?: return@all false
            isLinkSatisfied(link, parentStatus)
        }
    }
}
