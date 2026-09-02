package com.sucharu.sucharupro.data.job.observability

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.domain.job.model.JobDefinition
import com.sucharu.sucharupro.domain.job.model.JobStatus
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Structured audit entry for background job operations (INFRA-04 Step 04).
 */
data class JobAuditEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val action: String,
    val projectId: String,
    val jobId: String,
    val jobType: String,
    val actorId: String,
    val actorType: String,
    val status: JobStatus?,
    val details: Map<String, String> = emptyMap()
)

/**
 * Immutable audit logger for job operations with automatic credential stripping.
 */
class JobAuditLogger {
    private val entries = CopyOnWriteArrayList<JobAuditEntry>()

    private val sensitiveKeys = setOf("password", "token", "secret", "authorization", "apiKey", "key")

    fun logAction(
        action: String,
        job: JobDefinition,
        principal: AuthenticatedPrincipal?,
        details: Map<String, String> = emptyMap()
    ) {
        val sanitizedDetails = details.filterKeys { k -> !sensitiveKeys.any { k.contains(it, ignoreCase = true) } }

        val entry = JobAuditEntry(
            action = action,
            projectId = job.projectId,
            jobId = job.jobId,
            jobType = job.jobType,
            actorId = principal?.userId ?: job.actorId,
            actorType = principal?.principalType?.name ?: job.actorType.name,
            status = job.status,
            details = sanitizedDetails
        )
        entries.add(entry)
    }

    fun getEntriesForTenant(projectId: String): List<JobAuditEntry> {
        return entries.filter { it.projectId == projectId }
    }

    fun getEntriesForJob(jobId: String): List<JobAuditEntry> {
        return entries.filter { it.jobId == jobId }
    }

    fun clear() = entries.clear()
}
