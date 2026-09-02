package com.sucharu.sucharupro.data.event

import com.sucharu.sucharupro.data.event.postgres.PostgresDeadLetterRepository
import com.sucharu.sucharupro.data.event.postgres.PostgresTransactionalOutboxStore
import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.domain.event.consumer.EventFailureClassification
import com.sucharu.sucharupro.domain.event.model.EventActor
import com.sucharu.sucharupro.domain.event.model.EventEnvelope
import com.sucharu.sucharupro.domain.event.model.events.OrderCreatedEvent
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class PostgresDeadLetterRepositoryTest {

    private lateinit var mockDb: MockPostgresEventDatabase
    private lateinit var outboxStore: PostgresTransactionalOutboxStore
    private lateinit var deadLetterRepo: PostgresDeadLetterRepository
    private val tenant = TenantContext("sucharu_main")

    @Before
    fun setUp() {
        mockDb = MockPostgresEventDatabase()
        outboxStore = PostgresTransactionalOutboxStore(mockDb)
        deadLetterRepo = PostgresDeadLetterRepository(mockDb)
    }

    @Test
    fun test01_listDeadLetters_andMarkReplayed() {
        runBlocking {
            val envelope = EventEnvelope.create(
                payload = OrderCreatedEvent("ORD-1", "CUST-1", BigDecimal("100.00"), 1),
                projectId = "sucharu_main",
                actor = EventActor.human("U-1")
            )
            outboxStore.enqueue(tenant, envelope)
            val claimed = outboxStore.claimPendingRecords(tenant, "W1")
            val outboxId = claimed[0].outboxId

            outboxStore.moveToDeadLetter(
                tenantContext = tenant,
                outboxId = outboxId,
                classification = EventFailureClassification.NON_RETRYABLE,
                errorCode = "PERMANENT_ERROR",
                errorMessage = "Invalid business rule"
            )

            val deadLetters = deadLetterRepo.listDeadLetters(tenant)
            assertEquals(1, deadLetters.size)
            val dl = deadLetters[0]
            assertEquals(envelope.eventId, dl.eventId)
            assertEquals(EventFailureClassification.NON_RETRYABLE, dl.failureClassification)
            assertFalse(dl.isResolved)

            // Mark replayed
            deadLetterRepo.markReplayed(tenant, dl.deadLetterId, "ADMIN-01")

            val unresolvedAfterReplay = deadLetterRepo.listDeadLetters(tenant)
            assertTrue(unresolvedAfterReplay.isEmpty())
        }
    }
}
