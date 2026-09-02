package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.delivery.verification.DeliveryItemVerification
import com.sucharu.sucharupro.domain.model.delivery.verification.DeliveryItemVerificationActivityEvent
import com.sucharu.sucharupro.domain.model.delivery.verification.DeliveryItemVerificationLine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * In-memory thread-safe fake data source for Delivery Item Verification (Module 08 Step 04).
 */
class FakeDeliveryItemVerificationDataSource : DeliveryItemVerificationDataSource {

    private val mutex = Mutex()
    private val verificationsFlow = MutableStateFlow<Map<String, DeliveryItemVerification>>(emptyMap())
    private val linesFlow = MutableStateFlow<Map<String, List<DeliveryItemVerificationLine>>>(emptyMap())
    private val activityEventsFlow = MutableStateFlow<Map<String, List<DeliveryItemVerificationActivityEvent>>>(emptyMap())

    override fun observeVerifications(projectId: String): Flow<List<DeliveryItemVerification>> {
        return verificationsFlow.map { map ->
            map.values.filter { it.projectId == projectId }.sortedByDescending { it.createdAt }
        }
    }

    override fun observeVerificationsForDispatch(dispatchExecutionId: String): Flow<List<DeliveryItemVerification>> {
        return verificationsFlow.map { map ->
            map.values.filter { it.dispatchExecutionId == dispatchExecutionId }.sortedByDescending { it.createdAt }
        }
    }

    override fun observeVerification(verificationId: String): Flow<DeliveryItemVerification?> {
        return verificationsFlow.map { it[verificationId] }
    }

    override suspend fun getVerification(verificationId: String): DeliveryItemVerification? = mutex.withLock {
        verificationsFlow.value[verificationId]
    }

    override suspend fun getVerificationByNo(projectId: String, verificationNo: String): DeliveryItemVerification? = mutex.withLock {
        verificationsFlow.value.values.firstOrNull { it.projectId == projectId && it.verificationNo.equals(verificationNo, ignoreCase = true) }
    }

    override suspend fun getVerificationsForDispatch(dispatchExecutionId: String): List<DeliveryItemVerification> = mutex.withLock {
        verificationsFlow.value.values.filter { it.dispatchExecutionId == dispatchExecutionId }.sortedByDescending { it.createdAt }
    }

    override suspend fun insertVerification(
        verification: DeliveryItemVerification,
        lines: List<DeliveryItemVerificationLine>
    ) = mutex.withLock {
        verificationsFlow.update { it + (verification.verificationId to verification) }
        linesFlow.update { it + (verification.verificationId to lines) }
    }

    override suspend fun updateVerification(verification: DeliveryItemVerification) = mutex.withLock {
        verificationsFlow.update { it + (verification.verificationId to verification) }
    }

    override suspend fun updateVerificationWithLines(
        verification: DeliveryItemVerification,
        lines: List<DeliveryItemVerificationLine>
    ) = mutex.withLock {
        verificationsFlow.update { it + (verification.verificationId to verification) }
        linesFlow.update { it + (verification.verificationId to lines) }
    }

    override fun observeVerificationLines(verificationId: String): Flow<List<DeliveryItemVerificationLine>> {
        return linesFlow.map { it[verificationId] ?: emptyList() }
    }

    override suspend fun getVerificationLines(verificationId: String): List<DeliveryItemVerificationLine> = mutex.withLock {
        linesFlow.value[verificationId] ?: emptyList()
    }

    override suspend fun updateVerificationLine(line: DeliveryItemVerificationLine) = mutex.withLock {
        linesFlow.update { current ->
            val list = current[line.verificationId]?.toMutableList() ?: mutableListOf()
            val index = list.indexOfFirst { it.verificationLineId == line.verificationLineId }
            if (index >= 0) {
                list[index] = line
            } else {
                list.add(line)
            }
            current + (line.verificationId to list)
        }
    }

    override fun observeActivityEvents(verificationId: String): Flow<List<DeliveryItemVerificationActivityEvent>> {
        return activityEventsFlow.map { it[verificationId] ?: emptyList() }
    }

    override suspend fun getActivityEvents(verificationId: String): List<DeliveryItemVerificationActivityEvent> = mutex.withLock {
        activityEventsFlow.value[verificationId] ?: emptyList()
    }

    override suspend fun insertActivityEvent(event: DeliveryItemVerificationActivityEvent) = mutex.withLock {
        activityEventsFlow.update { current ->
            val list = current[event.verificationId]?.toMutableList() ?: mutableListOf()
            list.add(event)
            current + (event.verificationId to list)
        }
    }
}
