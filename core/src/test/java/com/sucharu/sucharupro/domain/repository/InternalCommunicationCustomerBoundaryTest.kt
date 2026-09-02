package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeInternalCommunicationDataSource
import com.sucharu.sucharupro.data.datasource.FakeNotificationDataSource
import com.sucharu.sucharupro.data.repository.InternalCommunicationRepositoryImpl
import com.sucharu.sucharupro.data.repository.NotificationRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.communication.internal.InternalCommunicationRecipientType
import com.sucharu.sucharupro.domain.model.communication.internal.InternalCommunicationType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class InternalCommunicationCustomerBoundaryTest {

    private lateinit var repository: InternalCommunicationRepositoryImpl

    @Before
    fun setUp() {
        val commDs = FakeInternalCommunicationDataSource()
        val notifDs = FakeNotificationDataSource()
        val notifRepo = NotificationRepositoryImpl(notifDs)
        repository = InternalCommunicationRepositoryImpl(commDs, notifRepo)
    }

    @Test
    fun `test customer role is strictly prevented from reading or sending internal communications`() = runBlocking {
        val createRes = repository.createCommunication(
            projectId = "PRJ-01",
            senderUserId = "CUST-001",
            senderRole = UserRole.CUSTOMER,
            recipientType = InternalCommunicationRecipientType.USER,
            recipientUserIds = setOf("STAFF-01"),
            communicationType = InternalCommunicationType.DIRECT_MESSAGE,
            subject = "Customer message attempt",
            message = "Unsafe internal access",
            actorId = "CUST-001",
            callerRole = UserRole.CUSTOMER
        )
        assertTrue(createRes is DomainResult.Error)
        assertTrue((createRes as DomainResult.Error).message.contains("external and strictly prohibited"))

        val readRes = repository.getCommunications(
            projectId = "PRJ-01",
            targetUserId = "CUST-001",
            actorId = "CUST-001",
            callerRole = UserRole.CUSTOMER
        )
        assertTrue(readRes is DomainResult.Error)
        assertTrue((readRes as DomainResult.Error).message.contains("external and strictly prohibited"))
    }
}
