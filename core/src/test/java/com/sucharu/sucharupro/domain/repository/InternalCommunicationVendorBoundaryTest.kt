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

class InternalCommunicationVendorBoundaryTest {

    private lateinit var repository: InternalCommunicationRepositoryImpl

    @Before
    fun setUp() {
        val commDs = FakeInternalCommunicationDataSource()
        val notifDs = FakeNotificationDataSource()
        val notifRepo = NotificationRepositoryImpl(notifDs)
        repository = InternalCommunicationRepositoryImpl(commDs, notifRepo)
    }

    @Test
    fun `test vendor role is strictly prevented from reading or sending internal communications`() = runBlocking {
        val createRes = repository.createCommunication(
            projectId = "PRJ-01",
            senderUserId = "VEND-001",
            senderRole = UserRole.VENDOR,
            recipientType = InternalCommunicationRecipientType.USER,
            recipientUserIds = setOf("STAFF-01"),
            communicationType = InternalCommunicationType.DIRECT_MESSAGE,
            subject = "Vendor message attempt",
            message = "Unsafe internal access",
            actorId = "VEND-001",
            callerRole = UserRole.VENDOR
        )
        assertTrue(createRes is DomainResult.Error)
        assertTrue((createRes as DomainResult.Error).message.contains("VENDOR role is blocked"))

        val readRes = repository.getCommunications(
            projectId = "PRJ-01",
            targetUserId = "VEND-001",
            actorId = "VEND-001",
            callerRole = UserRole.VENDOR
        )
        assertTrue(readRes is DomainResult.Error)
        assertTrue((readRes as DomainResult.Error).message.contains("VENDOR role is blocked"))
    }
}
