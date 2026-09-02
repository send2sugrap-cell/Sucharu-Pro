package com.sucharu.sucharupro.data.event

import com.sucharu.sucharupro.data.event.model.OutboxStatus
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
import java.sql.Timestamp

class PostgresTransactionalOutboxTest {

    private lateinit var mockDb: MockPostgresEventDatabase
    private lateinit var outboxStore: PostgresTransactionalOutboxStore
    private val tenant = TenantContext("sucharu_main")

    @Before
    fun setUp() {
        mockDb = MockPostgresEventDatabase()
        outboxStore = PostgresTransactionalOutboxStore(mockDb)
    }

    @Test
    fun test01_enqueueAndClaimLifecycle() {
        runBlocking {
            val payload = OrderCreatedEvent("ORD-1", "CUST-1", BigDecimal("100.00"), 1)
            val envelope = EventEnvelope.create(
                payload = payload,
                projectId = "sucharu_main",
                actor = EventActor.human("U-1")
            )

            outboxStore.enqueue(tenant, envelope)

            // Pending outbox records can be claimed
            val claimed = outboxStore.claimPendingRecords(
                tenantContext = tenant,
                workerId = "WORKER-01",
                limit = 10,
                leaseDurationMs = 30000L
            )

            assertEquals(1, claimed.size)
            val record = claimed[0]
            assertEquals(envelope.eventId, record.eventId)
            assertEquals("WORKER-01", record.claimedByWorker)
            assertEquals(OutboxStatus.PROCESSING, record.status)
            assertEquals(1, record.attemptCount)

            // Second worker cannot claim the same record (SKIP LOCKED)
            val secondClaim = outboxStore.claimPendingRecords(
                tenantContext = tenant,
                workerId = "WORKER-02",
                limit = 10
            )
            assertTrue(secondClaim.isEmpty())
        }
    }

    @Test
    fun test02_markPublished_setsTerminalStatus() {
        runBlocking {
            val envelope = EventEnvelope.create(
                payload = OrderCreatedEvent("ORD-1", "CUST-1", BigDecimal("100.00"), 1),
                projectId = "sucharu_main",
                actor = EventActor.human("U-1")
            )
            outboxStore.enqueue(tenant, envelope)
            val claimed = outboxStore.claimPendingRecords(tenant, "W1")
            val outboxId = claimed[0].outboxId

            outboxStore.markPublished(tenant, outboxId)

            val row = mockDb.outboxTable.first { it["outbox_id"] == outboxId }
            assertEquals("PUBLISHED", row["status"])
            assertNotNull(row["published_at"])
            assertNull(row["claimed_by_worker"])
        }
    }

    @Test
    fun test03_expiredLeaseRecovery_reclaimsForAnotherWorker() {
        runBlocking {
            val envelope = EventEnvelope.create(
                payload = OrderCreatedEvent("ORD-1", "CUST-1", BigDecimal("100.00"), 1),
                projectId = "sucharu_main",
                actor = EventActor.human("U-1")
            )
            outboxStore.enqueue(tenant, envelope)

            // First worker claims with short lease
            val claimed = outboxStore.claimPendingRecords(tenant, "CRASHED_WORKER", leaseDurationMs = 1000L)
            val outboxId = claimed[0].outboxId

            // Manually simulate lease expiration in DB
            val row = mockDb.outboxTable.first { it["outbox_id"] == outboxId }
            row["lease_expires_at"] = Timestamp(System.currentTimeMillis() - 5000L)

            // New worker claims expired lease
            val reClaimed = outboxStore.claimPendingRecords(tenant, "RECOVERY_WORKER")
            assertEquals(1, reClaimed.size)
            assertEquals("RECOVERY_WORKER", reClaimed[0].claimedByWorker)
            assertEquals(2, reClaimed[0].attemptCount)
        }
    }

    @Test
    fun test04_moveToDeadLetter_persistsFailureDiagnostics() {
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
                errorCode = "SCHEMA_ERROR",
                errorMessage = "Corrupt payload schema"
            )

            val outboxRow = mockDb.outboxTable.first { it["outbox_id"] == outboxId }
            assertEquals("DEAD_LETTER", outboxRow["status"])

            val deadLetterRow = mockDb.deadLetterTable.first { it["outbox_id"] == outboxId }
            assertEquals("sucharu_main", deadLetterRow["project_id"])
            assertEquals(envelope.eventId, deadLetterRow["event_id"])
            assertEquals("NON_RETRYABLE", deadLetterRow["failure_classification"])
            assertEquals("SCHEMA_ERROR", deadLetterRow["error_code"])
            assertEquals("Corrupt payload schema", deadLetterRow["error_message"])
        }
    }
}
