package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.delivery.partial.DeliveryPartialSettlement
import com.sucharu.sucharupro.domain.model.delivery.partial.DeliveryPartialSettlementLine
import com.sucharu.sucharupro.domain.model.delivery.partial.DeliverySettlementEvent
import com.sucharu.sucharupro.domain.model.delivery.partial.DeliverySplitDispatch
import com.sucharu.sucharupro.domain.model.delivery.partial.DeliverySplitDispatchLine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Thread-safe in-memory Fake implementation of [DeliveryPartialSettlementDataSource] (Module 08 Step 06).
 */
class FakeDeliveryPartialSettlementDataSource : DeliveryPartialSettlementDataSource {

    private val mutex = Mutex()
    private val settlementsFlow = MutableStateFlow<Map<String, DeliveryPartialSettlement>>(emptyMap())
    private val settlementLinesFlow = MutableStateFlow<Map<String, List<DeliveryPartialSettlementLine>>>(emptyMap())
    private val splitDispatchesFlow = MutableStateFlow<Map<String, List<DeliverySplitDispatch>>>(emptyMap())
    private val splitDispatchLinesFlow = MutableStateFlow<Map<String, List<DeliverySplitDispatchLine>>>(emptyMap())
    private val eventsFlow = MutableStateFlow<Map<String, List<DeliverySettlementEvent>>>(emptyMap())

    override fun observeSettlements(projectId: String): Flow<List<DeliveryPartialSettlement>> {
        return settlementsFlow.map { map ->
            map.values.filter { it.projectId == projectId }.sortedByDescending { it.createdAt }
        }
    }

    override fun observeSettlement(settlementId: String): Flow<DeliveryPartialSettlement?> {
        return settlementsFlow.map { it[settlementId] }
    }

    override fun observeSettlementLines(settlementId: String): Flow<List<DeliveryPartialSettlementLine>> {
        return settlementLinesFlow.map { it[settlementId] ?: emptyList() }
    }

    override suspend fun getSettlement(settlementId: String): DeliveryPartialSettlement? = mutex.withLock {
        settlementsFlow.value[settlementId]
    }

    override suspend fun getSettlementByDeliveryOrder(deliveryOrderId: String): DeliveryPartialSettlement? = mutex.withLock {
        settlementsFlow.value.values.firstOrNull { it.deliveryOrderId == deliveryOrderId }
    }

    override suspend fun getSettlementLines(settlementId: String): List<DeliveryPartialSettlementLine> = mutex.withLock {
        settlementLinesFlow.value[settlementId] ?: emptyList()
    }

    override suspend fun insertSettlement(
        settlement: DeliveryPartialSettlement,
        lines: List<DeliveryPartialSettlementLine>
    ) = mutex.withLock {
        settlementsFlow.update { it + (settlement.settlementId to settlement) }
        settlementLinesFlow.update { it + (settlement.settlementId to lines) }
    }

    override suspend fun updateSettlement(
        settlement: DeliveryPartialSettlement,
        lines: List<DeliveryPartialSettlementLine>
    ) = mutex.withLock {
        settlementsFlow.update { it + (settlement.settlementId to settlement) }
        settlementLinesFlow.update { it + (settlement.settlementId to lines) }
    }

    override fun observeSplitDispatches(deliveryOrderId: String): Flow<List<DeliverySplitDispatch>> {
        return splitDispatchesFlow.map { it[deliveryOrderId] ?: emptyList() }
    }

    override fun observeSplitDispatchLines(splitDispatchId: String): Flow<List<DeliverySplitDispatchLine>> {
        return splitDispatchLinesFlow.map { it[splitDispatchId] ?: emptyList() }
    }

    override suspend fun getSplitDispatch(splitDispatchId: String): DeliverySplitDispatch? = mutex.withLock {
        splitDispatchesFlow.value.values.flatten().firstOrNull { it.splitDispatchId == splitDispatchId }
    }

    override suspend fun getSplitDispatches(deliveryOrderId: String): List<DeliverySplitDispatch> = mutex.withLock {
        splitDispatchesFlow.value[deliveryOrderId] ?: emptyList()
    }

    override suspend fun getSplitDispatchLines(splitDispatchId: String): List<DeliverySplitDispatchLine> = mutex.withLock {
        splitDispatchLinesFlow.value[splitDispatchId] ?: emptyList()
    }

    override suspend fun insertSplitDispatch(
        split: DeliverySplitDispatch,
        lines: List<DeliverySplitDispatchLine>
    ) = mutex.withLock {
        splitDispatchesFlow.update { current ->
            val list = current[split.deliveryOrderId]?.toMutableList() ?: mutableListOf()
            list.add(split)
            current + (split.deliveryOrderId to list)
        }
        splitDispatchLinesFlow.update { current ->
            current + (split.splitDispatchId to lines)
        }
    }

    override suspend fun updateSplitDispatch(split: DeliverySplitDispatch) = mutex.withLock {
        splitDispatchesFlow.update { current ->
            val list = current[split.deliveryOrderId]?.toMutableList() ?: mutableListOf()
            val index = list.indexOfFirst { it.splitDispatchId == split.splitDispatchId }
            if (index >= 0) {
                list[index] = split
            } else {
                list.add(split)
            }
            current + (split.deliveryOrderId to list)
        }
    }

    override fun observeEvents(settlementId: String): Flow<List<DeliverySettlementEvent>> {
        return eventsFlow.map { it[settlementId] ?: emptyList() }
    }

    override suspend fun getEvents(settlementId: String): List<DeliverySettlementEvent> = mutex.withLock {
        eventsFlow.value[settlementId] ?: emptyList()
    }

    override suspend fun insertEvent(event: DeliverySettlementEvent) = mutex.withLock {
        eventsFlow.update { current ->
            val list = current[event.settlementId]?.toMutableList() ?: mutableListOf()
            list.add(event)
            current + (event.settlementId to list)
        }
    }
}
