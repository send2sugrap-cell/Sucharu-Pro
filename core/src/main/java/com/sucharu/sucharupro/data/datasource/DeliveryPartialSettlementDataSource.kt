package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.delivery.partial.DeliveryPartialSettlement
import com.sucharu.sucharupro.domain.model.delivery.partial.DeliveryPartialSettlementLine
import com.sucharu.sucharupro.domain.model.delivery.partial.DeliverySettlementEvent
import com.sucharu.sucharupro.domain.model.delivery.partial.DeliverySplitDispatch
import com.sucharu.sucharupro.domain.model.delivery.partial.DeliverySplitDispatchLine
import kotlinx.coroutines.flow.Flow

/**
 * Reactive data source interface for Delivery Partial Settlement & Split Dispatches (Module 08 Step 06).
 */
interface DeliveryPartialSettlementDataSource {
    fun observeSettlements(projectId: String): Flow<List<DeliveryPartialSettlement>>
    fun observeSettlement(settlementId: String): Flow<DeliveryPartialSettlement?>
    fun observeSettlementLines(settlementId: String): Flow<List<DeliveryPartialSettlementLine>>
    suspend fun getSettlement(settlementId: String): DeliveryPartialSettlement?
    suspend fun getSettlementByDeliveryOrder(deliveryOrderId: String): DeliveryPartialSettlement?
    suspend fun getSettlementLines(settlementId: String): List<DeliveryPartialSettlementLine>
    suspend fun insertSettlement(settlement: DeliveryPartialSettlement, lines: List<DeliveryPartialSettlementLine>)
    suspend fun updateSettlement(settlement: DeliveryPartialSettlement, lines: List<DeliveryPartialSettlementLine>)

    fun observeSplitDispatches(deliveryOrderId: String): Flow<List<DeliverySplitDispatch>>
    fun observeSplitDispatchLines(splitDispatchId: String): Flow<List<DeliverySplitDispatchLine>>
    suspend fun getSplitDispatch(splitDispatchId: String): DeliverySplitDispatch?
    suspend fun getSplitDispatches(deliveryOrderId: String): List<DeliverySplitDispatch>
    suspend fun getSplitDispatchLines(splitDispatchId: String): List<DeliverySplitDispatchLine>
    suspend fun insertSplitDispatch(split: DeliverySplitDispatch, lines: List<DeliverySplitDispatchLine>)
    suspend fun updateSplitDispatch(split: DeliverySplitDispatch)

    fun observeEvents(settlementId: String): Flow<List<DeliverySettlementEvent>>
    suspend fun getEvents(settlementId: String): List<DeliverySettlementEvent>
    suspend fun insertEvent(event: DeliverySettlementEvent)
}
