package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturn
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnActivityEvent
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnDisposition
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnLine
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnLineCondition
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnShipment
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnShipmentStatus
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnSummary
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository contract for Delivery Return & Reverse Logistics (Module 08 Step 07).
 */
interface DeliveryReturnRepository {

    fun observeReturns(projectId: String): Flow<List<DeliveryReturn>>

    fun observeReturn(returnId: String): Flow<DeliveryReturn?>

    fun observeReturnSummary(returnId: String): Flow<DeliveryReturnSummary?>

    suspend fun getReturn(
        returnId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DeliveryReturn>

    suspend fun getReturnLines(
        returnId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<List<DeliveryReturnLine>>

    suspend fun createReturn(
        ret: DeliveryReturn,
        lines: List<DeliveryReturnLine>,
        actorId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DeliveryReturn>

    suspend fun updateDraftReturn(
        ret: DeliveryReturn,
        lines: List<DeliveryReturnLine>,
        actorId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DeliveryReturn>

    suspend fun submitReturn(
        returnId: String,
        actorId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DeliveryReturn>

    suspend fun approveReturn(
        returnId: String,
        actorId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DeliveryReturn>

    suspend fun rejectReturn(
        returnId: String,
        rejectionReason: String,
        actorId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DeliveryReturn>

    suspend fun startReceiving(
        returnId: String,
        actorId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DeliveryReturn>

    suspend fun receiveReturn(
        returnId: String,
        receivedQuantitiesByLine: Map<String, Double>,
        actorId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DeliveryReturn>

    suspend fun startInspection(
        returnId: String,
        actorId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DeliveryReturn>

    suspend fun inspectReturnLine(
        returnId: String,
        returnLineId: String,
        acceptedQuantity: Double,
        rejectedQuantity: Double,
        condition: DeliveryReturnLineCondition,
        disposition: DeliveryReturnDisposition,
        inspectionNotes: String? = null,
        actorId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DeliveryReturnLine>

    suspend fun completeInspection(
        returnId: String,
        actorId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DeliveryReturn>

    suspend fun setDisposition(
        returnId: String,
        returnLineId: String,
        disposition: DeliveryReturnDisposition,
        actorId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DeliveryReturnLine>

    suspend fun processRestock(
        returnId: String,
        returnLineId: String,
        warehouseId: String,
        locationId: String,
        actorId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DeliveryReturnLine>

    suspend fun processAllRestock(
        returnId: String,
        defaultWarehouseId: String,
        defaultLocationId: String,
        actorId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DeliveryReturn>

    suspend fun completeReturn(
        returnId: String,
        actorId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DeliveryReturn>

    suspend fun cancelReturn(
        returnId: String,
        reason: String,
        actorId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DeliveryReturn>

    suspend fun getEligibleReturnQuantity(
        deliveryOrderId: String,
        deliveryOrderLineId: String,
        callerProjectId: String? = null
    ): DomainResult<Double>

    suspend fun getEvents(
        returnId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<List<DeliveryReturnActivityEvent>>

    fun observeReverseShipment(returnId: String): Flow<DeliveryReturnShipment?>

    suspend fun getReverseShipment(
        returnId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DeliveryReturnShipment>

    suspend fun createReverseShipment(
        shipment: DeliveryReturnShipment,
        actorId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DeliveryReturnShipment>

    suspend fun updateReverseShipmentStatus(
        returnId: String,
        newStatus: DeliveryReturnShipmentStatus,
        notes: String? = null,
        actorId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DeliveryReturnShipment>
}
