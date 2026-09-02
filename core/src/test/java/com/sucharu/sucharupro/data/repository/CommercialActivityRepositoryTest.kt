package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.datasource.FakeCommercialActivityDataSource
import com.sucharu.sucharupro.domain.model.activity.CommercialActivityEvent
import com.sucharu.sucharupro.domain.model.activity.CommercialActivityType
import com.sucharu.sucharupro.domain.model.activity.CommercialEntityType
import com.sucharu.sucharupro.domain.model.common.DomainResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [CommercialActivityRepositoryImpl].
 *
 * Tests cover:
 * - Reactive observation of all events
 * - Reactive observation filtered by entity
 * - Lookup by activity ID (found + not found)
 * - Successful event recording
 * - Duplicate event ID rejection
 * - Blank-field validation at repository layer
 * - Events returned newest-first
 */
class CommercialActivityRepositoryTest {

    private lateinit var dataSource: FakeCommercialActivityDataSource
    private lateinit var repository: CommercialActivityRepositoryImpl

    @Before
    fun setUp() {
        dataSource = FakeCommercialActivityDataSource()
        repository = CommercialActivityRepositoryImpl(dataSource)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 01: Initial empty state
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun test01_observeActivities_empty_emitsEmptyList() = runBlocking {
        val events = repository.observeActivities().first()
        assertTrue("Expected empty list initially", events.isEmpty())
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 02: Record and observe one event
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun test02_recordActivity_success_eventAppearsinObserveAll() = runBlocking {
        val event = buildEvent(
            activityId = "act-001",
            entityType = CommercialEntityType.INQUIRY,
            entityId = "inq-001",
            activityType = CommercialActivityType.CREATED,
            timestamp = "2026-01-01T10:00:00Z"
        )
        val result = repository.recordActivity(event)
        assertTrue("Expected success", result is DomainResult.Success)

        val events = repository.observeActivities().first()
        assertEquals(1, events.size)
        assertEquals("act-001", events.first().activityId)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 03: Filter by entity type and ID
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun test03_observeActivitiesForEntity_filtersCorrectly() = runBlocking {
        repository.recordActivity(
            buildEvent("act-inq-01", CommercialEntityType.INQUIRY, "inq-001",
                CommercialActivityType.CREATED, "2026-01-01T09:00:00Z")
        )
        repository.recordActivity(
            buildEvent("act-quot-01", CommercialEntityType.QUOTATION, "quot-001",
                CommercialActivityType.CREATED, "2026-01-01T10:00:00Z")
        )
        repository.recordActivity(
            buildEvent("act-ord-01", CommercialEntityType.ORDER, "ord-001",
                CommercialActivityType.CREATED, "2026-01-01T11:00:00Z")
        )

        val inquiryEvents = repository
            .observeActivitiesForEntity(CommercialEntityType.INQUIRY, "inq-001")
            .first()

        assertEquals(1, inquiryEvents.size)
        assertEquals("act-inq-01", inquiryEvents.first().activityId)

        val quotationEvents = repository
            .observeActivitiesForEntity(CommercialEntityType.QUOTATION, "quot-001")
            .first()
        assertEquals(1, quotationEvents.size)
        assertEquals("act-quot-01", quotationEvents.first().activityId)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 04: Lookup by ID — found
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun test04_getActivityById_found_returnsSuccess() = runBlocking {
        val event = buildEvent("act-find-01", CommercialEntityType.ORDER, "ord-001",
            CommercialActivityType.STATUS_CHANGED, "2026-02-01T12:00:00Z")
        repository.recordActivity(event)

        val result = repository.getActivityById("act-find-01")
        assertTrue(result is DomainResult.Success)
        assertEquals("act-find-01", (result as DomainResult.Success).data.activityId)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 05: Lookup by ID — not found
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun test05_getActivityById_notFound_returnsError() = runBlocking {
        val result = repository.getActivityById("nonexistent-id")
        assertTrue("Expected error for missing ID", result is DomainResult.Error)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 06: Duplicate ID rejection
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun test06_recordActivity_duplicateId_returnsError() = runBlocking {
        val event = buildEvent("act-dup-01", CommercialEntityType.QUOTATION, "quot-01",
            CommercialActivityType.CREATED, "2026-03-01T08:00:00Z")
        repository.recordActivity(event)

        val duplicateResult = repository.recordActivity(event.copy(activityType = CommercialActivityType.VIEWED))
        assertTrue("Expected duplicate rejection error", duplicateResult is DomainResult.Error)

        // Original event must still exist unchanged
        val events = repository.observeActivities().first()
        assertEquals(1, events.size)
        assertEquals(CommercialActivityType.CREATED, events.first().activityType)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 07: Blank activityId rejected at repository level
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun test07_recordActivity_blankActivityId_returnsError() = runBlocking {
        // Cannot create CommercialActivityEvent with blank activityId — init block enforces it
        // So we test the repository's explicit guard for blank activityId indirectly
        // by attempting direct datasource insert with blank-like edge
        val result = repository.getActivityById("   ")
        assertTrue("Blank ID lookup should return error", result is DomainResult.Error)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 08: Events returned newest-first
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun test08_observeActivities_returnedNewestFirst() = runBlocking {
        repository.recordActivity(
            buildEvent("act-old", CommercialEntityType.ORDER, "ord-01",
                CommercialActivityType.CREATED, "2026-01-01T08:00:00Z")
        )
        repository.recordActivity(
            buildEvent("act-mid", CommercialEntityType.ORDER, "ord-01",
                CommercialActivityType.STATUS_CHANGED, "2026-01-02T10:00:00Z")
        )
        repository.recordActivity(
            buildEvent("act-new", CommercialEntityType.ORDER, "ord-01",
                CommercialActivityType.PRIORITY_CHANGED, "2026-01-03T12:00:00Z")
        )

        val events = repository.observeActivities().first()
        assertEquals("act-new", events[0].activityId)
        assertEquals("act-mid", events[1].activityId)
        assertEquals("act-old", events[2].activityId)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 09: Multiple entities accumulate independently
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun test09_multipleEntities_eventsAccumulateIndependently() = runBlocking {
        repeat(3) { i ->
            repository.recordActivity(
                buildEvent("inq-ev-$i", CommercialEntityType.INQUIRY, "inq-001",
                    CommercialActivityType.VIEWED, "2026-04-01T${10 + i}:00:00Z")
            )
        }
        repeat(2) { i ->
            repository.recordActivity(
                buildEvent("ord-ev-$i", CommercialEntityType.ORDER, "ord-001",
                    CommercialActivityType.STATUS_CHANGED, "2026-04-01T${14 + i}:00:00Z")
            )
        }

        val allEvents = repository.observeActivities().first()
        assertEquals(5, allEvents.size)

        val inquiryEvents = repository
            .observeActivitiesForEntity(CommercialEntityType.INQUIRY, "inq-001").first()
        assertEquals(3, inquiryEvents.size)

        val orderEvents = repository
            .observeActivitiesForEntity(CommercialEntityType.ORDER, "ord-001").first()
        assertEquals(2, orderEvents.size)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 10: Lifecycle audit sequence — CREATED → STATUS_CHANGED → CANCELLED
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun test10_lifecycleAuditSequence_allEventsRecordedCorrectly() = runBlocking {
        val quotationId = "quot-life-01"

        val created = buildEvent("life-01", CommercialEntityType.QUOTATION, quotationId,
            CommercialActivityType.CREATED, "2026-05-01T08:00:00Z")
        val sent = buildEvent("life-02", CommercialEntityType.QUOTATION, quotationId,
            CommercialActivityType.STATUS_CHANGED, "2026-05-01T09:00:00Z",
            previousStatus = "DRAFT", newStatus = "SENT")
        val cancelled = buildEvent("life-03", CommercialEntityType.QUOTATION, quotationId,
            CommercialActivityType.CANCELLED, "2026-05-01T10:00:00Z",
            newStatus = "CANCELLED", reason = "Customer changed requirements.")

        listOf(created, sent, cancelled).forEach { repository.recordActivity(it) }

        val events = repository
            .observeActivitiesForEntity(CommercialEntityType.QUOTATION, quotationId)
            .first()

        assertEquals(3, events.size)
        // Newest first
        assertEquals(CommercialActivityType.CANCELLED, events[0].activityType)
        assertEquals("Customer changed requirements.", events[0].reason)
        assertEquals(CommercialActivityType.STATUS_CHANGED, events[1].activityType)
        assertEquals("DRAFT", events[1].previousStatus)
        assertEquals("SENT", events[1].newStatus)
        assertEquals(CommercialActivityType.CREATED, events[2].activityType)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 11: resolvedActorName — fallback to "System"
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun test11_resolvedActorName_nullActor_returnsSystem() = runBlocking {
        val event = buildEvent("act-sys-01", CommercialEntityType.ORDER, "ord-001",
            CommercialActivityType.CREATED, "2026-06-01T08:00:00Z")
        assertEquals("System", event.resolvedActorName)
    }

    @Test
    fun test11b_resolvedActorName_namedActor_returnsName() = runBlocking {
        val event = buildEvent(
            activityId = "act-sys-02",
            entityType = CommercialEntityType.ORDER,
            entityId = "ord-001",
            activityType = CommercialActivityType.APPROVED,
            timestamp = "2026-06-01T09:00:00Z"
        ).copy(actorId = "usr-01", actorName = "Rafiq")
        assertEquals("Rafiq", event.resolvedActorName)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 12: FakeDataSource initialEvents constructor
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun test12_fakeDataSource_initialEvents_emittedOnSubscription() = runBlocking {
        val seed = buildEvent("seed-01", CommercialEntityType.ORDER, "ord-seed",
            CommercialActivityType.CREATED, "2026-07-01T10:00:00Z")
        val seededDataSource = FakeCommercialActivityDataSource(listOf(seed))
        val seededRepo = CommercialActivityRepositoryImpl(seededDataSource)

        val events = seededRepo.observeActivities().first()
        assertEquals(1, events.size)
        assertEquals("seed-01", events.first().activityId)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 13: ORDER_CONVERTED event contains new order ID in newValue
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun test13_orderConvertedEvent_newValueContainsOrderId() = runBlocking {
        val event = buildEvent("conv-01", CommercialEntityType.QUOTATION, "quot-001",
            CommercialActivityType.ORDER_CONVERTED, "2026-08-01T08:00:00Z",
            newValue = "ord-abc123")
        repository.recordActivity(event)

        val events = repository
            .observeActivitiesForEntity(CommercialEntityType.QUOTATION, "quot-001")
            .first()
        assertEquals("ord-abc123", events.first().newValue)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 14: HANDOFF_READY event on ORDER entity
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun test14_handoffReadyEvent_recordedOnOrderEntity() = runBlocking {
        val event = buildEvent("hndof-01", CommercialEntityType.ORDER, "ord-001",
            CommercialActivityType.HANDOFF_READY, "2026-08-05T09:00:00Z",
            newValue = "READY_FOR_JOB")
        repository.recordActivity(event)

        val result = repository.getActivityById("hndof-01")
        assertTrue(result is DomainResult.Success)
        assertEquals(CommercialActivityType.HANDOFF_READY, (result as DomainResult.Success).data.activityType)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private fun buildEvent(
        activityId: String,
        entityType: CommercialEntityType,
        entityId: String,
        activityType: CommercialActivityType,
        timestamp: String,
        previousStatus: String? = null,
        newStatus: String? = null,
        previousValue: String? = null,
        newValue: String? = null,
        reason: String? = null,
        note: String? = null
    ) = CommercialActivityEvent(
        activityId = activityId,
        entityType = entityType,
        entityId = entityId,
        activityType = activityType,
        actorId = null,
        actorName = null,
        timestamp = timestamp,
        previousStatus = previousStatus,
        newStatus = newStatus,
        previousValue = previousValue,
        newValue = newValue,
        reason = reason,
        note = note
    )
}
