package com.sucharu.sucharupro.data.communication.customer

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.communication.customer.CustomerCommunicationScheduler
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Local/In-Memory implementation of CustomerCommunicationScheduler for development and testing (Module 10 Step 02).
 */
class InMemoryCustomerCommunicationScheduler : CustomerCommunicationScheduler {

    private val mutex = Mutex()
    private val scheduledMap = mutableMapOf<String, Long>()

    override suspend fun schedule(
        projectId: String,
        communicationId: String,
        scheduledAt: Long
    ): DomainResult<Unit> = mutex.withLock {
        scheduledMap["$projectId:$communicationId"] = scheduledAt
        DomainResult.Success(Unit)
    }

    override suspend fun cancelScheduled(
        projectId: String,
        communicationId: String
    ): DomainResult<Unit> = mutex.withLock {
        scheduledMap.remove("$projectId:$communicationId")
        DomainResult.Success(Unit)
    }
}
