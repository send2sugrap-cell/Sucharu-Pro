package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.activity.CommercialActivityEvent
import com.sucharu.sucharupro.domain.model.activity.CommercialEntityType
import com.sucharu.sucharupro.domain.model.common.DomainResult
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface contract for the Commercial Activity Audit Trail in Sucharu Pro.
 *
 * Audit events are append-only and immutable. No event can be edited or deleted
 * after it has been recorded. Events are returned newest-first by default.
 *
 * No Room, DAO, or network/API dependencies are permitted in implementations.
 */
interface CommercialActivityRepository {

    /**
     * Reactive stream of ALL audit events across all entity types, newest-first.
     */
    fun observeActivities(): Flow<List<CommercialActivityEvent>>

    /**
     * Reactive stream of audit events for a specific entity, newest-first.
     *
     * @param entityType  The type of commercial entity (INQUIRY, QUOTATION, ORDER).
     * @param entityId    The primary key of the entity.
     */
    fun observeActivitiesForEntity(
        entityType: CommercialEntityType,
        entityId: String
    ): Flow<List<CommercialActivityEvent>>

    /**
     * One-shot lookup of a single audit event by its unique [activityId].
     */
    suspend fun getActivityById(activityId: String): DomainResult<CommercialActivityEvent>

    /**
     * Records a new audit event atomically.
     *
     * Returns [DomainResult.Error] if:
     * - An event with the same [activityId] already exists (duplicate rejection).
     * - Any required field is blank.
     *
     * @param event  The immutable audit event to record.
     */
    suspend fun recordActivity(event: CommercialActivityEvent): DomainResult<CommercialActivityEvent>
}
