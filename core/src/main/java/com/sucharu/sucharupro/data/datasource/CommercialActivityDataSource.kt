package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.activity.CommercialActivityEvent
import com.sucharu.sucharupro.domain.model.activity.CommercialEntityType
import com.sucharu.sucharupro.domain.model.common.DomainResult
import kotlinx.coroutines.flow.Flow

/**
 * Data source abstraction for the Commercial Activity Audit Trail in Sucharu Pro.
 *
 * Implementations must store events as an append-only, immutable log.
 * No editing or deletion of events is permitted.
 */
interface CommercialActivityDataSource {

    /** Continuous reactive stream of all recorded audit events, newest-first. */
    fun observeActivities(): Flow<List<CommercialActivityEvent>>

    /**
     * Reactive stream of audit events filtered to a specific entity, newest-first.
     *
     * @param entityType  The commercial entity type.
     * @param entityId    The primary key of the entity.
     */
    fun observeActivitiesForEntity(
        entityType: CommercialEntityType,
        entityId: String
    ): Flow<List<CommercialActivityEvent>>

    /**
     * One-shot lookup of a single audit event by [activityId].
     */
    suspend fun fetchActivityById(activityId: String): DomainResult<CommercialActivityEvent>

    /**
     * Appends a new audit event to the store.
     *
     * Returns [DomainResult.Error] if an event with the same [activityId] already exists.
     */
    suspend fun insertActivity(event: CommercialActivityEvent): DomainResult<CommercialActivityEvent>
}
