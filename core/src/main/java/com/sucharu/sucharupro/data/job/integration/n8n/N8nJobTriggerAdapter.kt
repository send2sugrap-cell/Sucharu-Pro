package com.sucharu.sucharupro.data.job.integration.n8n

import com.sucharu.sucharupro.data.api.model.PrincipalType
import com.sucharu.sucharupro.data.event.integration.n8n.N8nConfig
import com.sucharu.sucharupro.data.job.postgres.JobRepository
import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.domain.job.model.JobDefinition
import com.sucharu.sucharupro.domain.job.model.JobPriority
import com.sucharu.sucharupro.domain.job.model.JobStatus
import com.sucharu.sucharupro.domain.job.model.JobTriggerType
import java.nio.charset.StandardCharsets
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.abs

/**
 * Result of validating an incoming n8n webhook job trigger.
 */
sealed class N8nJobTriggerResult {
    data class Accepted(val jobId: String) : N8nJobTriggerResult()
    data class Rejected(val reason: String, val isSecurityViolation: Boolean = false) : N8nJobTriggerResult()
}

/**
 * Controlled adapter for n8n-triggered asynchronous background workflows (INFRA-04 Step 04).
 */
class N8nJobTriggerAdapter(
    private val config: N8nConfig,
    private val jobRepository: JobRepository
) {
    /**
     * Authenticates and enqueues an n8n-triggered background job.
     */
    suspend fun triggerJobFromN8n(
        payloadJson: String,
        signatureHeader: String?,
        timestampHeader: String?,
        jobType: String,
        idempotencyKey: String?,
        tenantContext: TenantContext
    ): N8nJobTriggerResult {
        // 1. Validate signature header
        if (signatureHeader.isNullOrBlank()) {
            return N8nJobTriggerResult.Rejected("Missing X-Sucharu-Signature header", isSecurityViolation = true)
        }

        // 2. Validate timestamp replay window (5 minutes max)
        val timestamp = timestampHeader?.toLongOrNull()
        if (timestamp == null || abs(System.currentTimeMillis() - timestamp) > 300000L) {
            return N8nJobTriggerResult.Rejected("Invalid or expired timestamp header", isSecurityViolation = true)
        }

        // 3. Verify HMAC signature
        val expectedSignature = computeHmac(payloadJson, config.signingSecret)
        if (!secureEquals(expectedSignature, signatureHeader)) {
            return N8nJobTriggerResult.Rejected("Invalid HMAC signature verification", isSecurityViolation = true)
        }

        // 4. Validate permitted job type
        if (jobType.startsWith("security.") || jobType.startsWith("auth.")) {
            return N8nJobTriggerResult.Rejected("n8n cannot trigger security jobType '$jobType'", isSecurityViolation = true)
        }

        // 5. Enqueue background job with server-authoritative tenant
        val jobId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        val job = JobDefinition(
            jobId = jobId,
            projectId = tenantContext.projectId,
            jobType = jobType,
            jobVersion = "v1",
            triggerType = JobTriggerType.N8N,
            priority = JobPriority.NORMAL,
            status = JobStatus.QUEUED,
            payloadJson = payloadJson,
            correlationId = UUID.randomUUID().toString(),
            actorType = PrincipalType.SYSTEM,
            actorId = "N8N_WEBHOOK",
            principalType = PrincipalType.SYSTEM,
            source = "n8n:${config.webhookBaseUrl}",
            idempotencyKey = idempotencyKey ?: "n8n:$jobId",
            createdAt = now,
            updatedAt = now
        )

        jobRepository.enqueueJob(job, tenantContext)
        return N8nJobTriggerResult.Accepted(jobId)
    }

    private fun computeHmac(data: String, secret: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(StandardCharsets.UTF_8), "HmacSHA256"))
        val hash = mac.doFinal(data.toByteArray(StandardCharsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }

    private fun secureEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var result = 0
        for (i in a.indices) {
            result = result or (a[i].code xor b[i].code)
        }
        return result == 0
    }
}
