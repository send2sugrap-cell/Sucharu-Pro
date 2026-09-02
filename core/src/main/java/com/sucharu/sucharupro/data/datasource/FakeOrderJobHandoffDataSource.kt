package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.handoff.OrderJobHandoff
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Thread-safe in-memory implementation of [OrderJobHandoffDataSource] using [MutableStateFlow] and [Mutex].
 */
class FakeOrderJobHandoffDataSource(
    initialHandoffs: List<OrderJobHandoff> = emptyList()
) : OrderJobHandoffDataSource {

    private val mutex = Mutex()
    private val _handoffs = MutableStateFlow(initialHandoffs)

    override fun observeHandoffs(): Flow<List<OrderJobHandoff>> = _handoffs.asStateFlow()

    override suspend fun fetchHandoffById(handoffId: String): DomainResult<OrderJobHandoff> = mutex.withLock {
        val found = _handoffs.value.find { it.handoffId == handoffId }
        return if (found != null) {
            DomainResult.Success(found)
        } else {
            DomainResult.Error(message = "Handoff not found: $handoffId")
        }
    }

    override suspend fun fetchHandoffForOrder(orderId: String): DomainResult<OrderJobHandoff> = mutex.withLock {
        val found = _handoffs.value.find { it.orderId == orderId }
        return if (found != null) {
            DomainResult.Success(found)
        } else {
            DomainResult.Error(message = "No handoff record found for order: $orderId")
        }
    }

    override suspend fun insertHandoff(handoff: OrderJobHandoff): DomainResult<OrderJobHandoff> = mutex.withLock {
        if (_handoffs.value.any { it.handoffId == handoff.handoffId }) {
            return DomainResult.Error(message = "Handoff with ID '${handoff.handoffId}' already exists.")
        }
        val currentList = _handoffs.value.toMutableList()
        currentList.add(handoff)
        _handoffs.value = currentList.toList()
        DomainResult.Success(handoff)
    }

    override suspend fun updateHandoff(handoff: OrderJobHandoff): DomainResult<OrderJobHandoff> = mutex.withLock {
        val index = _handoffs.value.indexOfFirst { it.handoffId == handoff.handoffId }
        if (index == -1) {
            return DomainResult.Error(message = "Handoff not found: ${handoff.handoffId}")
        }
        val currentList = _handoffs.value.toMutableList()
        currentList[index] = handoff
        _handoffs.value = currentList.toList()
        DomainResult.Success(handoff)
    }
}
