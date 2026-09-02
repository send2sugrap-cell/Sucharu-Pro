package com.sucharu.sucharupro.data.communication.internal

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.communication.internal.InternalCommunicationScheduler
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Local/In-Memory implementation of InternalCommunicationScheduler for development and testing (Module 10 Step 03).
 */
class InMemoryInternalCommunicationScheduler : InternalCommunicationScheduler {

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

    override suspend fun processDueCommunications(projectId: String): DomainResult<List<String>> = mutex.withLock {
        val now = System.currentTimeMillis()
        val dueKeys = scheduledMap.filter { (key, time) ->
            key.startsWith("$projectId:") && time <= now
        }.keys.toList()

        val dueIds = dueKeys.map { it.removePrefix("$projectId:") }
        dueKeys.forEach { scheduledMap.remove(it) }

        DomainResult.Success(dueIds)
    }
}
