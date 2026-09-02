package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.activity.CommercialActivityEvent
import com.sucharu.sucharupro.domain.model.activity.CommercialEntityType
import com.sucharu.sucharupro.domain.model.common.DomainResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * In-memory, reactive implementation of [CommercialActivityDataSource] for development and testing.
 *
 * - Events are stored as an append-only list in a [MutableStateFlow].
 * - All mutations are guarded by a [Mutex] for atomic, thread-safe updates.
 * - Events are emitted newest-first (sorted descending by [CommercialActivityEvent.timestamp]).
 * - Duplicate [activityId] insertion is rejected atomically.
 * - No Room, DAO, network, or database dependencies.
 */
class FakeCommercialActivityDataSource(
    initialEvents: List<CommercialActivityEvent> = emptyList()
) : CommercialActivityDataSource {

    private val mutex = Mutex()
    private val _events = MutableStateFlow<List<CommercialActivityEvent>>(
        initialEvents.sortedByDescending { it.timestamp }
    )

    override fun observeActivities(): Flow<List<CommercialActivityEvent>> =
        _events.asStateFlow()

    override fun observeActivitiesForEntity(
        entityType: CommercialEntityType,
        entityId: String
    ): Flow<List<CommercialActivityEvent>> =
        _events.asStateFlow().map { events ->
            events.filter { it.entityType == entityType && it.entityId == entityId }
        }

    override suspend fun fetchActivityById(
        activityId: String
    ): DomainResult<CommercialActivityEvent> = mutex.withLock {
        val event = _events.value.find { it.activityId == activityId }
        return if (event != null) {
            DomainResult.Success(event)
        } else {
            DomainResult.Error(message = "Activity event not found with ID: $activityId")
        }
    }

    override suspend fun insertActivity(
        event: CommercialActivityEvent
    ): DomainResult<CommercialActivityEvent> = mutex.withLock {
        // Duplicate ID rejection
        if (_events.value.any { it.activityId == event.activityId }) {
            return DomainResult.Error(
                message = "Activity event with ID '${event.activityId}' already exists. Duplicate events are not permitted."
            )
        }

        // Append and re-sort newest-first
        val updated = (_events.value + event).sortedByDescending { it.timestamp }
        _events.value = updated
        DomainResult.Success(event)
    }
}
