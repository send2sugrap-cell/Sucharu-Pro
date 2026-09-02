package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecution
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecutionActivityEvent
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecutionLine
import kotlinx.coroutines.flow.Flow

/**
 * Data source interface for Dispatch Executions (Module 08 Step 03).
 */
interface DispatchExecutionDataSource {

    // Dispatch Headers
    fun observeDispatches(projectId: String): Flow<List<DispatchExecution>>
    fun observeDispatchesForChallan(deliveryChallanId: String): Flow<List<DispatchExecution>>
    fun observeDispatch(dispatchExecutionId: String): Flow<DispatchExecution?>
    suspend fun getDispatch(dispatchExecutionId: String): DispatchExecution?
    suspend fun getDispatchByNo(projectId: String, dispatchNo: String): DispatchExecution?
    suspend fun getDispatchesForChallan(deliveryChallanId: String): List<DispatchExecution>
    suspend fun insertDispatch(dispatch: DispatchExecution, lines: List<DispatchExecutionLine>)
    suspend fun updateDispatch(dispatch: DispatchExecution)
    suspend fun updateDispatchWithLines(dispatch: DispatchExecution, lines: List<DispatchExecutionLine>)

    // Dispatch Lines
    fun observeDispatchLines(dispatchExecutionId: String): Flow<List<DispatchExecutionLine>>
    suspend fun getDispatchLines(dispatchExecutionId: String): List<DispatchExecutionLine>
    suspend fun getDispatchLine(lineId: String): DispatchExecutionLine?

    // Activity Events
    fun observeActivityEvents(dispatchExecutionId: String): Flow<List<DispatchExecutionActivityEvent>>
    suspend fun getActivityEvents(dispatchExecutionId: String): List<DispatchExecutionActivityEvent>
    suspend fun insertActivityEvent(event: DispatchExecutionActivityEvent)
}
