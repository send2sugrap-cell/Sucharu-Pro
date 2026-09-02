package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallan
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallanActivityEvent
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallanLine
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallanType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository contract for Delivery Challan Management (Module 08 Step 02).
 */
interface DeliveryChallanRepository {

    // ──────────────────────────────────────────────────────────────
    // Challan Queries
    // ──────────────────────────────────────────────────────────────

    fun observeChallans(projectId: String): Flow<List<DeliveryChallan>>

    fun observeChallansForDeliveryOrder(deliveryOrderId: String): Flow<List<DeliveryChallan>>

    fun observeChallan(challanId: String): Flow<DeliveryChallan?>

    suspend fun getChallan(
        challanId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DeliveryChallan>

    suspend fun getChallansForDeliveryOrder(
        deliveryOrderId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<List<DeliveryChallan>>

    // ──────────────────────────────────────────────────────────────
    // Challan Line Queries
    // ──────────────────────────────────────────────────────────────

    fun observeChallanLines(challanId: String): Flow<List<DeliveryChallanLine>>

    suspend fun getChallanLines(
        challanId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<List<DeliveryChallanLine>>

    suspend fun getChallanLine(
        lineId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DeliveryChallanLine>

    suspend fun getAllocatedQuantityForDeliveryOrderLine(deliveryOrderLineId: String): Double

    // ──────────────────────────────────────────────────────────────
    // Activity Queries
    // ──────────────────────────────────────────────────────────────

    fun observeActivityEvents(challanId: String): Flow<List<DeliveryChallanActivityEvent>>

    suspend fun getActivityEvents(
        challanId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<List<DeliveryChallanActivityEvent>>

    // ──────────────────────────────────────────────────────────────
    // Challan Mutations
    // ──────────────────────────────────────────────────────────────

    suspend fun createChallan(
        challan: DeliveryChallan,
        lines: List<DeliveryChallanLine>,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DeliveryChallan>

    suspend fun updateDraftChallan(
        challanId: String,
        challanType: DeliveryChallanType,
        issueDate: Long,
        notes: String?,
        lines: List<DeliveryChallanLine>,
        actorId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DeliveryChallan>

    suspend fun submitChallan(
        challanId: String,
        actorId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DeliveryChallan>

    suspend fun approveChallan(
        challanId: String,
        actorId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DeliveryChallan>

    suspend fun markReadyForDispatch(
        challanId: String,
        actorId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DeliveryChallan>

    suspend fun cancelChallan(
        challanId: String,
        actorId: String,
        reason: String? = null,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DeliveryChallan>
}
