package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecution
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecutionActivityEvent
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecutionLine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Thread-safe fake in-memory data source for Dispatch Executions (Module 08 Step 03).
 */
class FakeDispatchExecutionDataSource : DispatchExecutionDataSource {

    private val mutex = Mutex()

    private val dispatchesFlow = MutableStateFlow<List<DispatchExecution>>(emptyList())
    private val linesFlow = MutableStateFlow<List<DispatchExecutionLine>>(emptyList())
    private val activityEventsFlow = MutableStateFlow<List<DispatchExecutionActivityEvent>>(emptyList())

    // ──────────────────────────────────────────────────────────────
    // Dispatch Headers
    // ──────────────────────────────────────────────────────────────

    override fun observeDispatches(projectId: String): Flow<List<DispatchExecution>> {
        return dispatchesFlow.map { list -> list.filter { it.projectId == projectId } }
    }

    override fun observeDispatchesForChallan(deliveryChallanId: String): Flow<List<DispatchExecution>> {
        return dispatchesFlow.map { list -> list.filter { it.deliveryChallanId == deliveryChallanId } }
    }

    override fun observeDispatch(dispatchExecutionId: String): Flow<DispatchExecution?> {
        return dispatchesFlow.map { list -> list.find { it.dispatchExecutionId == dispatchExecutionId } }
    }

    override suspend fun getDispatch(dispatchExecutionId: String): DispatchExecution? = mutex.withLock {
        dispatchesFlow.value.find { it.dispatchExecutionId == dispatchExecutionId }
    }

    override suspend fun getDispatchByNo(projectId: String, dispatchNo: String): DispatchExecution? = mutex.withLock {
        dispatchesFlow.value.find { it.projectId == projectId && it.dispatchNo.equals(dispatchNo, ignoreCase = true) }
    }

    override suspend fun getDispatchesForChallan(deliveryChallanId: String): List<DispatchExecution> = mutex.withLock {
        dispatchesFlow.value.filter { it.deliveryChallanId == deliveryChallanId }
    }

    override suspend fun insertDispatch(dispatch: DispatchExecution, lines: List<DispatchExecutionLine>): Unit = mutex.withLock {
        val currentDispatches = dispatchesFlow.value.toMutableList()
        currentDispatches.add(dispatch)
        dispatchesFlow.value = currentDispatches

        val currentLines = linesFlow.value.toMutableList()
        currentLines.addAll(lines)
        linesFlow.value = currentLines
    }

    override suspend fun updateDispatch(dispatch: DispatchExecution): Unit = mutex.withLock {
        val current = dispatchesFlow.value.toMutableList()
        val index = current.indexOfFirst { it.dispatchExecutionId == dispatch.dispatchExecutionId }
        if (index != -1) {
            current[index] = dispatch
            dispatchesFlow.value = current
        }
    }

    override suspend fun updateDispatchWithLines(
        dispatch: DispatchExecution,
        lines: List<DispatchExecutionLine>
    ): Unit = mutex.withLock {
        val currentDispatches = dispatchesFlow.value.toMutableList()
        val index = currentDispatches.indexOfFirst { it.dispatchExecutionId == dispatch.dispatchExecutionId }
        if (index != -1) {
            currentDispatches[index] = dispatch
            dispatchesFlow.value = currentDispatches
        }

        val currentLines = linesFlow.value.toMutableList()
        currentLines.removeAll { it.dispatchExecutionId == dispatch.dispatchExecutionId }
        currentLines.addAll(lines)
        linesFlow.value = currentLines
    }

    // ──────────────────────────────────────────────────────────────
    // Dispatch Lines
    // ──────────────────────────────────────────────────────────────

    override fun observeDispatchLines(dispatchExecutionId: String): Flow<List<DispatchExecutionLine>> {
        return linesFlow.map { list -> list.filter { it.dispatchExecutionId == dispatchExecutionId } }
    }

    override suspend fun getDispatchLines(dispatchExecutionId: String): List<DispatchExecutionLine> = mutex.withLock {
        linesFlow.value.filter { it.dispatchExecutionId == dispatchExecutionId }
    }

    override suspend fun getDispatchLine(lineId: String): DispatchExecutionLine? = mutex.withLock {
        linesFlow.value.find { it.dispatchExecutionLineId == lineId }
    }

    // ──────────────────────────────────────────────────────────────
    // Activity Events
    // ──────────────────────────────────────────────────────────────

    override fun observeActivityEvents(dispatchExecutionId: String): Flow<List<DispatchExecutionActivityEvent>> {
        return activityEventsFlow.map { list ->
            list.filter { it.dispatchExecutionId == dispatchExecutionId }.sortedByDescending { it.performedAt }
        }
    }

    override suspend fun getActivityEvents(dispatchExecutionId: String): List<DispatchExecutionActivityEvent> = mutex.withLock {
        activityEventsFlow.value.filter { it.dispatchExecutionId == dispatchExecutionId }.sortedByDescending { it.performedAt }
    }

    override suspend fun insertActivityEvent(event: DispatchExecutionActivityEvent): Unit = mutex.withLock {
        val current = activityEventsFlow.value.toMutableList()
        current.add(event)
        activityEventsFlow.value = current
    }
}
