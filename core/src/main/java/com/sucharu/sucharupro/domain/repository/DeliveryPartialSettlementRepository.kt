package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.partial.DeliveryPartialSettlement
import com.sucharu.sucharupro.domain.model.delivery.partial.DeliveryPartialSettlementLine
import com.sucharu.sucharupro.domain.model.delivery.partial.DeliveryPartialSettlementSummary
import com.sucharu.sucharupro.domain.model.delivery.partial.DeliverySettlementEvent
import com.sucharu.sucharupro.domain.model.delivery.partial.DeliverySplitDispatch
import com.sucharu.sucharupro.domain.model.delivery.partial.DeliverySplitDispatchLine
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository contract for Partial Delivery, Split Dispatch & Settlement Management (Module 08 Step 06).
 */
interface DeliveryPartialSettlementRepository {

    // Queries
    fun observeSettlements(projectId: String): Flow<List<DeliveryPartialSettlement>>
    fun observeSettlement(settlementId: String): Flow<DeliveryPartialSettlement?>
    fun observeSettlementLines(settlementId: String): Flow<List<DeliveryPartialSettlementLine>>

    suspend fun getSettlement(
        settlementId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DeliveryPartialSettlement>

    suspend fun getSettlementByDeliveryOrder(
        deliveryOrderId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DeliveryPartialSettlement>

    suspend fun getSettlementLines(
        settlementId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<List<DeliveryPartialSettlementLine>>

    fun observeSplitDispatches(deliveryOrderId: String): Flow<List<DeliverySplitDispatch>>

    suspend fun getSplitDispatches(
        deliveryOrderId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<List<DeliverySplitDispatch>>

    suspend fun getSplitDispatch(
        splitDispatchId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DeliverySplitDispatch>

    suspend fun getSplitDispatchLines(
        splitDispatchId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<List<DeliverySplitDispatchLine>>

    fun observeEvents(settlementId: String): Flow<List<DeliverySettlementEvent>>

    suspend fun getEvents(
        settlementId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<List<DeliverySettlementEvent>>

    suspend fun getSettlementSummary(
        projectId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DeliveryPartialSettlementSummary>

    // Mutations
    suspend fun initializeSettlementForDeliveryOrder(
        deliveryOrderId: String,
        actorId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DeliveryPartialSettlement>

    suspend fun createSplitDispatch(
        deliveryOrderId: String,
        lines: List<DeliverySplitDispatchLine>,
        deliveryChallanId: String? = null,
        dispatchExecutionId: String? = null,
        shipmentId: String? = null,
        notes: String? = null,
        actorId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DeliverySplitDispatch>

    suspend fun recordPartialDelivery(
        settlementId: String,
        deliveryOrderLineId: String,
        deliveredQuantity: Double,
        actorId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DeliveryPartialSettlement>

    suspend fun recalculateSettlement(
        settlementId: String,
        actorId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DeliveryPartialSettlement>

    suspend fun finalizeSettlement(
        settlementId: String,
        notes: String? = null,
        actorId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DeliveryPartialSettlement>

    suspend fun disputeSettlement(
        settlementId: String,
        reason: String,
        actorId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DeliveryPartialSettlement>

    suspend fun cancelSettlement(
        settlementId: String,
        reason: String? = null,
        actorId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DeliveryPartialSettlement>
}
