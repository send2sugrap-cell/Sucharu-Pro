package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.verification.DeliveryItemVerification
import com.sucharu.sucharupro.domain.model.delivery.verification.DeliveryItemVerificationActivityEvent
import com.sucharu.sucharupro.domain.model.delivery.verification.DeliveryItemVerificationLine
import com.sucharu.sucharupro.domain.model.delivery.verification.DeliveryItemVerificationSummary
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository contract for Delivery Item Verification (Module 08 Step 04).
 */
interface DeliveryItemVerificationRepository {

    // Queries
    fun observeVerifications(projectId: String): Flow<List<DeliveryItemVerification>>
    fun observeVerificationsForDispatch(dispatchExecutionId: String): Flow<List<DeliveryItemVerification>>
    fun observeVerification(verificationId: String): Flow<DeliveryItemVerification?>
    suspend fun getVerification(
        verificationId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DeliveryItemVerification>

    suspend fun getVerificationsForDispatch(
        dispatchExecutionId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<List<DeliveryItemVerification>>

    fun observeVerificationLines(verificationId: String): Flow<List<DeliveryItemVerificationLine>>
    suspend fun getVerificationLines(
        verificationId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<List<DeliveryItemVerificationLine>>

    fun observeActivityEvents(verificationId: String): Flow<List<DeliveryItemVerificationActivityEvent>>
    suspend fun getActivityEvents(
        verificationId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<List<DeliveryItemVerificationActivityEvent>>

    suspend fun getVerificationSummary(
        verificationId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DeliveryItemVerificationSummary>

    // Mutations
    suspend fun createVerification(
        verification: DeliveryItemVerification,
        lines: List<DeliveryItemVerificationLine>,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DeliveryItemVerification>

    suspend fun updateDraftVerification(
        verificationId: String,
        remarks: String?,
        lines: List<DeliveryItemVerificationLine>,
        actorId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DeliveryItemVerification>

    suspend fun submitVerification(
        verificationId: String,
        actorId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DeliveryItemVerification>

    suspend fun startVerification(
        verificationId: String,
        actorId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DeliveryItemVerification>

    suspend fun verifyLine(
        verificationId: String,
        verificationLineId: String,
        verifiedQuantity: Double,
        isDamaged: Boolean = false,
        damagedQuantity: Double = 0.0,
        isMissing: Boolean = false,
        isProductMismatch: Boolean = false,
        isBatchMismatch: Boolean = false,
        isLotMismatch: Boolean = false,
        remarks: String? = null,
        actorId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DeliveryItemVerificationLine>

    suspend fun completeVerification(
        verificationId: String,
        actorId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DeliveryItemVerification>

    suspend fun closeVerification(
        verificationId: String,
        actorId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DeliveryItemVerification>

    suspend fun cancelVerification(
        verificationId: String,
        actorId: String,
        reason: String?,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DeliveryItemVerification>
}
