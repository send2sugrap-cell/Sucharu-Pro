package com.sucharu.sucharupro.domain.job.idempotency

/**
 * Interface for background job idempotency tracking (INFRA-04 Step 04).
 */
interface JobIdempotencyStore {
    suspend fun isIdempotencyKeyClaimed(projectId: String, idempotencyKey: String): Boolean
    suspend fun getJobIdByIdempotencyKey(projectId: String, idempotencyKey: String): String?
    suspend fun recordIdempotencyKey(projectId: String, idempotencyKey: String, jobId: String)
}
