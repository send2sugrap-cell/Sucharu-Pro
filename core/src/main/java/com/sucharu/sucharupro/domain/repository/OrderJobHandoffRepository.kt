package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.handoff.OrderJobHandoff
import com.sucharu.sucharupro.domain.model.handoff.OrderJobHandoffStatus
import com.sucharu.sucharupro.domain.model.order.Order
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface contract for managing Order → Job Handoff records in Sucharu Pro.
 */
interface OrderJobHandoffRepository {

    /** Reactive stream of all handoff records. */
    fun getHandoffs(): Flow<List<OrderJobHandoff>>

    /** Reactive stream observing a single handoff by its ID. */
    fun getHandoffById(handoffId: String): Flow<OrderJobHandoff?>

    /** Direct lookup of a handoff record by its ID. */
    suspend fun findHandoffById(handoffId: String): DomainResult<OrderJobHandoff>

    /** Reactive stream observing the handoff record for a specific [orderId]. */
    fun getHandoffForOrder(orderId: String): Flow<OrderJobHandoff?>

    /** Direct lookup of the handoff record for a specific [orderId]. */
    suspend fun findHandoffForOrder(orderId: String): DomainResult<OrderJobHandoff>

    /**
     * Validates commercial eligibility and creates an [OrderJobHandoff] snapshot record.
     */
    suspend fun createHandoff(
        handoffId: String,
        order: Order,
        createdBy: String? = null,
        notes: String? = null,
        timestamp: String
    ): DomainResult<OrderJobHandoff>

    /**
     * Confirms the handoff and transitions status from [OrderJobHandoffStatus.READY_FOR_HANDOFF] to [OrderJobHandoffStatus.HANDED_OFF].
     */
    suspend fun confirmHandoff(
        handoffId: String,
        confirmedBy: String? = null,
        timestamp: String
    ): DomainResult<OrderJobHandoff>

    /**
     * Marks the handoff as [OrderJobHandoffStatus.READY_FOR_PRODUCTION].
     */
    suspend fun markReadyForProduction(
        handoffId: String,
        timestamp: String
    ): DomainResult<OrderJobHandoff>

    /**
     * Cancels an active handoff before production intake.
     */
    suspend fun cancelHandoff(
        handoffId: String,
        reason: String? = null
    ): DomainResult<OrderJobHandoff>
}
