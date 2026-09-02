package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeInternalCommunicationDataSource
import com.sucharu.sucharupro.data.datasource.FakeNotificationDataSource
import com.sucharu.sucharupro.data.repository.InternalCommunicationRepositoryImpl
import com.sucharu.sucharupro.data.repository.NotificationRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.communication.internal.InternalCommunicationRecipientType
import com.sucharu.sucharupro.domain.model.communication.internal.InternalCommunicationType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class InternalCommunicationConcurrencyTest {

    private lateinit var repository: InternalCommunicationRepositoryImpl

    @Before
    fun setUp() {
        val commDs = FakeInternalCommunicationDataSource()
        val notifDs = FakeNotificationDataSource()
        val notifRepo = NotificationRepositoryImpl(notifDs)
        repository = InternalCommunicationRepositoryImpl(commDs, notifRepo)
    }

    @Test
    fun `test 20 concurrent communication creations yield distinct IDs and communication numbers`() = runBlocking {
        val count = 20
        val jobs = (1..count).map { i ->
            async {
                repository.createCommunication(
                    projectId = "PRJ-CONCURRENCY",
                    senderUserId = "USER-$i",
                    senderRole = UserRole.STAFF,
                    recipientType = InternalCommunicationRecipientType.USER,
                    recipientUserIds = setOf("RECIPIENT-$i"),
                    communicationType = InternalCommunicationType.DIRECT_MESSAGE,
                    subject = "Concurrent Subject $i",
                    message = "Concurrent Message $i",
                    actorId = "USER-$i",
                    callerRole = UserRole.STAFF
                )
            }
        }
        val results = jobs.awaitAll()
        assertEquals(count, results.size)
        assertTrue(results.all { it is DomainResult.Success })

        val comms = results.map { (it as DomainResult.Success).data }
        val ids = comms.map { it.communicationId }.toSet()
        val nos = comms.map { it.communicationNo }.toSet()

        assertEquals(count, ids.size)
        assertEquals(count, nos.size)
    }
}
