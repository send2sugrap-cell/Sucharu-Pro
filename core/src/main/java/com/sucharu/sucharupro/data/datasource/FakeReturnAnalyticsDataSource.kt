package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.returns.ReturnException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Thread-safe, in-memory fake implementation of [ReturnAnalyticsDataSource] (Module 11 Step 06).
 */
class FakeReturnAnalyticsDataSource : ReturnAnalyticsDataSource {

    private val mutex = Mutex()
    private val exceptionsState = MutableStateFlow<Map<String, ReturnException>>(emptyMap())

    override fun observeExceptions(projectId: String): Flow<List<ReturnException>> {
        return exceptionsState.map { map ->
            map.values.filter { it.projectId == projectId }.sortedByDescending { it.detectedAt }
        }
    }

    override suspend fun getExceptions(projectId: String): List<ReturnException> = mutex.withLock {
        exceptionsState.value.values
            .filter { it.projectId == projectId }
            .sortedByDescending { it.detectedAt }
    }

    override suspend fun getExceptionById(exceptionId: String): ReturnException? = mutex.withLock {
        exceptionsState.value[exceptionId]
    }

    override suspend fun getExceptionByIdempotencyKey(key: String): ReturnException? = mutex.withLock {
        exceptionsState.value.values.find { it.idempotencyKey == key }
    }

    override suspend fun insertOrUpdateException(exception: ReturnException) = mutex.withLock {
        val current = exceptionsState.value.toMutableMap()
        current[exception.exceptionId] = exception
        exceptionsState.value = current
    }

    override suspend fun deleteException(exceptionId: String) = mutex.withLock {
        val current = exceptionsState.value.toMutableMap()
        current.remove(exceptionId)
        exceptionsState.value = current
    }

    suspend fun clear() = mutex.withLock {
        exceptionsState.value = emptyMap()
    }
}
