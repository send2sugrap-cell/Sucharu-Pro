package com.sucharu.sucharupro.data.event

import com.sucharu.sucharupro.data.event.postgres.PostgresEventStore
import com.sucharu.sucharupro.data.event.postgres.PostgresTransactionalOutboxStore
import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.domain.event.model.EventActor
import com.sucharu.sucharupro.domain.event.model.EventEnvelope
import com.sucharu.sucharupro.domain.event.model.events.OrderCreatedEvent
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class OutboxTransactionalAtomicityTest {

    private lateinit var mockDb: MockPostgresEventDatabase
    private lateinit var eventStore: PostgresEventStore
    private lateinit var outboxStore: PostgresTransactionalOutboxStore
    private val tenant = TenantContext("sucharu_main")

    @Before
    fun setUp() {
        mockDb = MockPostgresEventDatabase()
        eventStore = PostgresEventStore(mockDb)
        outboxStore = PostgresTransactionalOutboxStore(mockDb)
    }

    @Test
    fun test01_businessStateAndOutbox_commitAtomicallyTogether() = runBlocking {
        val envelope = EventEnvelope.create(
            payload = OrderCreatedEvent("ORD-1", "CUST-1", BigDecimal("100"), 1),
            projectId = "sucharu_main",
            actor = EventActor.human("U-1")
        )

        mockDb.inTransaction(tenant) { txContext ->
            // Simulate business state save + event store append + outbox enqueue in single transaction
            eventStore.appendInTransaction(txContext, envelope)
            outboxStore.enqueueInTransaction(txContext, envelope)
        }

        // Verify both tables have the committed records
        assertEquals(1, mockDb.eventStoreTable.size)
        assertEquals(1, mockDb.outboxTable.size)
    }

    @Test
    fun test02_transactionRollback_leavesZeroRecords() = runBlocking {
        val envelope = EventEnvelope.create(
            payload = OrderCreatedEvent("ORD-1", "CUST-1", BigDecimal("100"), 1),
            projectId = "sucharu_main",
            actor = EventActor.human("U-1")
        )

        try {
            mockDb.inTransaction(tenant) { txContext ->
                eventStore.appendInTransaction(txContext, envelope)
                outboxStore.enqueueInTransaction(txContext, envelope)
                // Trigger exception before commit
                throw IllegalStateException("Business validation failed, rolling back!")
            }
        } catch (_: IllegalStateException) {
            // Expected
        }

        // Verify rollback left zero dirty records
        assertEquals(0, mockDb.eventStoreTable.size)
        assertEquals(0, mockDb.outboxTable.size)
    }
}
