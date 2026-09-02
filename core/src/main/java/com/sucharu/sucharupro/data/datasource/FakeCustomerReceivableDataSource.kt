package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.finance.CustomerReceivable
import com.sucharu.sucharupro.domain.model.finance.CustomerReceivableActivityEvent
import com.sucharu.sucharupro.domain.model.finance.CustomerReceivableStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Thread-safe in-memory fake implementation of [CustomerReceivableDataSource] (Module 09 Step 02).
 */
class FakeCustomerReceivableDataSource : CustomerReceivableDataSource {

    private val mutex = Mutex()
    private val receivablesState = MutableStateFlow<Map<String, CustomerReceivable>>(emptyMap())
    private val activityEventsState = MutableStateFlow<List<CustomerReceivableActivityEvent>>(emptyList())

    override suspend fun insertReceivable(receivable: CustomerReceivable): Unit = mutex.withLock {
        val current = receivablesState.value.toMutableMap()
        current[receivable.receivableId] = receivable
        receivablesState.value = current
    }

    override suspend fun updateReceivable(receivable: CustomerReceivable): Unit = mutex.withLock {
        val current = receivablesState.value.toMutableMap()
        current[receivable.receivableId] = receivable
        receivablesState.value = current
    }

    override suspend fun getReceivableById(receivableId: String): CustomerReceivable? = mutex.withLock {
        receivablesState.value[receivableId]
    }

    override suspend fun getReceivableByNumber(projectId: String, receivableNo: String): CustomerReceivable? = mutex.withLock {
        receivablesState.value.values.firstOrNull { it.projectId == projectId && it.receivableNo == receivableNo }
    }

    override suspend fun getReceivablesByReference(projectId: String, referenceId: String): List<CustomerReceivable> = mutex.withLock {
        receivablesState.value.values.filter { it.projectId == projectId && it.referenceId == referenceId }
    }

    override fun observeReceivables(projectId: String): Flow<List<CustomerReceivable>> {
        return receivablesState.map { map ->
            map.values.filter { it.projectId == projectId }.sortedByDescending { it.createdAt }
        }
    }

    override fun observeReceivablesByCustomer(projectId: String, customerId: String): Flow<List<CustomerReceivable>> {
        return receivablesState.map { map ->
            map.values.filter { it.projectId == projectId && it.customerId == customerId }.sortedByDescending { it.createdAt }
        }
    }

    override suspend fun getReceivablesByStatus(
        projectId: String,
        status: CustomerReceivableStatus
    ): List<CustomerReceivable> = mutex.withLock {
        receivablesState.value.values
            .filter { it.projectId == projectId && it.status == status }
            .sortedByDescending { it.createdAt }
    }

    override suspend fun insertActivityEvent(event: CustomerReceivableActivityEvent): Unit = mutex.withLock {
        activityEventsState.value = activityEventsState.value + event
    }

    override suspend fun getActivityEvents(receivableId: String): List<CustomerReceivableActivityEvent> = mutex.withLock {
        activityEventsState.value.filter { it.receivableId == receivableId }.sortedBy { it.timestamp }
    }
}
