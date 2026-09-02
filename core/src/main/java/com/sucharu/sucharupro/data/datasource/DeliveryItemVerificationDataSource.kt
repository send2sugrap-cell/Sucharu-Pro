package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.delivery.verification.DeliveryItemVerification
import com.sucharu.sucharupro.domain.model.delivery.verification.DeliveryItemVerificationActivityEvent
import com.sucharu.sucharupro.domain.model.delivery.verification.DeliveryItemVerificationLine
import kotlinx.coroutines.flow.Flow

/**
 * Reactive data source interface for Delivery Item Verification (Module 08 Step 04).
 */
interface DeliveryItemVerificationDataSource {
    fun observeVerifications(projectId: String): Flow<List<DeliveryItemVerification>>
    fun observeVerificationsForDispatch(dispatchExecutionId: String): Flow<List<DeliveryItemVerification>>
    fun observeVerification(verificationId: String): Flow<DeliveryItemVerification?>
    suspend fun getVerification(verificationId: String): DeliveryItemVerification?
    suspend fun getVerificationByNo(projectId: String, verificationNo: String): DeliveryItemVerification?
    suspend fun getVerificationsForDispatch(dispatchExecutionId: String): List<DeliveryItemVerification>
    suspend fun insertVerification(verification: DeliveryItemVerification, lines: List<DeliveryItemVerificationLine>)
    suspend fun updateVerification(verification: DeliveryItemVerification)
    suspend fun updateVerificationWithLines(verification: DeliveryItemVerification, lines: List<DeliveryItemVerificationLine>)

    fun observeVerificationLines(verificationId: String): Flow<List<DeliveryItemVerificationLine>>
    suspend fun getVerificationLines(verificationId: String): List<DeliveryItemVerificationLine>
    suspend fun updateVerificationLine(line: DeliveryItemVerificationLine)

    fun observeActivityEvents(verificationId: String): Flow<List<DeliveryItemVerificationActivityEvent>>
    suspend fun getActivityEvents(verificationId: String): List<DeliveryItemVerificationActivityEvent>
    suspend fun insertActivityEvent(event: DeliveryItemVerificationActivityEvent)
}
