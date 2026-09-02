package com.sucharu.sucharupro.domain.model.communication.customer

import com.sucharu.sucharupro.domain.model.common.DomainResult

/**
 * Provider-neutral interface for scheduling and dispatching delayed customer communications (Module 10 Step 02).
 */
interface CustomerCommunicationScheduler {

    suspend fun schedule(
        projectId: String,
        communicationId: String,
        scheduledAt: Long
    ): DomainResult<Unit>

    suspend fun cancelScheduled(
        projectId: String,
        communicationId: String
    ): DomainResult<Unit>
}
