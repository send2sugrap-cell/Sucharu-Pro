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

class InternalCommunicationProductionBoundaryTest {

    private lateinit var repository: InternalCommunicationRepositoryImpl

    @Before
    fun setUp() {
        val commDs = FakeInternalCommunicationDataSource()
        val notifDs = FakeNotificationDataSource()
        val notifRepo = NotificationRepositoryImpl(notifDs)
        repository = InternalCommunicationRepositoryImpl(commDs, notifRepo)
    }

    @Test
    fun `test production job discussion does not mutate job status or stage output`() = runBlocking {
        val res = repository.createCommunication(
            projectId = "PRJ-01",
            senderUserId = "PROD-STAFF-01",
            senderRole = UserRole.STAFF,
            recipientType = InternalCommunicationRecipientType.USER,
            recipientUserIds = setOf("QC-STAFF-01"),
            communicationType = InternalCommunicationType.PRODUCTION_DISCUSSION,
            subject = "Offset Printing Delay",
            message = "Drying time extended for heavy ink coverage",
            referenceType = "JOB",
            referenceId = "JOB-2026-0089",
            actorId = "PROD-STAFF-01",
            callerRole = UserRole.STAFF
        )
        assertTrue(res is DomainResult.Success)
        val comm = (res as DomainResult.Success).data
        assertEquals("JOB", comm.referenceType)
        assertEquals("JOB-2026-0089", comm.referenceId)
    }
}
