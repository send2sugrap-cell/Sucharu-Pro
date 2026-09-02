package com.sucharu.sucharupro.data.event

import com.sucharu.sucharupro.data.event.postgres.PostgresEventIdempotencyStore
import com.sucharu.sucharupro.domain.event.idempotency.EventProcessingRecord
import com.sucharu.sucharupro.domain.event.idempotency.EventProcessingStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class PostgresEventIdempotencyTest {

    private lateinit var mockDb: MockPostgresEventDatabase
    private lateinit var idempotencyStore: PostgresEventIdempotencyStore

    @Before
    fun setUp() {
        mockDb = MockPostgresEventDatabase()
        idempotencyStore = PostgresEventIdempotencyStore(mockDb)
    }

    @Test
    fun test01_unprocessedEvent_returnsFalse() {
        runBlocking {
            val processed = idempotencyStore.isProcessed("EVT-1", "Consumer-A", "proj_1")
            assertFalse(processed)
        }
    }

    @Test
    fun test02_processedEvent_returnsTrue() {
        runBlocking {
            val record = EventProcessingRecord(
                eventId = "EVT-1",
                consumerId = "Consumer-A",
                projectId = "proj_1",
                processedAt = System.currentTimeMillis(),
                status = EventProcessingStatus.PROCESSED,
                executionDurationMs = 15L
            )

            idempotencyStore.recordProcessing(record)

            val processed = idempotencyStore.isProcessed("EVT-1", "Consumer-A", "proj_1")
            assertTrue(processed)

            // Different consumer or different project returns false
            assertFalse(idempotencyStore.isProcessed("EVT-1", "Consumer-B", "proj_1"))
            assertFalse(idempotencyStore.isProcessed("EVT-1", "Consumer-A", "proj_2"))
        }
    }

    @Test
    fun test03_getRecord_retrievesPersistedDetails() {
        runBlocking {
            val record = EventProcessingRecord(
                eventId = "EVT-99",
                consumerId = "AuditConsumer",
                projectId = "proj_1",
                processedAt = 1700000000000L,
                status = EventProcessingStatus.PROCESSED,
                executionDurationMs = 42L
            )

            idempotencyStore.recordProcessing(record)

            val fetched = idempotencyStore.getRecord("EVT-99", "AuditConsumer", "proj_1")
            assertNotNull(fetched)
            assertEquals("EVT-99", fetched?.eventId)
            assertEquals("AuditConsumer", fetched?.consumerId)
            assertEquals(EventProcessingStatus.PROCESSED, fetched?.status)
            assertEquals(42L, fetched?.executionDurationMs)
        }
    }
}
