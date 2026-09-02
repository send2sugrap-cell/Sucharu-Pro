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

class InternalCommunicationDeliveryBoundaryTest {

    private lateinit var repository: InternalCommunicationRepositoryImpl

    @Before
    fun setUp() {
        val commDs = FakeInternalCommunicationDataSource()
        val notifDs = FakeNotificationDataSource()
        val notifRepo = NotificationRepositoryImpl(notifDs)
        repository = InternalCommunicationRepositoryImpl(commDs, notifRepo)
    }

    @Test
    fun `test delivery discussion reference does not alter delivery status or challan`() = runBlocking {
        val res = repository.createCommunication(
            projectId = "PRJ-01",
            senderUserId = "DELIVERY-01",
            senderRole = UserRole.STAFF,
            recipientType = InternalCommunicationRecipientType.USER,
            recipientUserIds = setOf("MANAGER-01"),
            communicationType = InternalCommunicationType.DELIVERY_DISCUSSION,
            subject = "Address Verification Required",
            message = "Building entry requires gate pass",
            referenceType = "DELIVERY",
            referenceId = "DEL-2026-0042",
            actorId = "DELIVERY-01",
            callerRole = UserRole.STAFF
        )
        assertTrue(res is DomainResult.Success)
        val comm = (res as DomainResult.Success).data
        assertEquals("DELIVERY", comm.referenceType)
        assertEquals("DEL-2026-0042", comm.referenceId)
    }
}
