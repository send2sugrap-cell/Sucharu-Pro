package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.returns.ReturnException
import kotlinx.coroutines.flow.Flow

/**
 * Data source interface for Return Governance Exceptions and Analytics persistence (Module 11 Step 06).
 */
interface ReturnAnalyticsDataSource {
    fun observeExceptions(projectId: String): Flow<List<ReturnException>>
    suspend fun getExceptions(projectId: String): List<ReturnException>
    suspend fun getExceptionById(exceptionId: String): ReturnException?
    suspend fun getExceptionByIdempotencyKey(key: String): ReturnException?
    suspend fun insertOrUpdateException(exception: ReturnException)
    suspend fun deleteException(exceptionId: String)
}
