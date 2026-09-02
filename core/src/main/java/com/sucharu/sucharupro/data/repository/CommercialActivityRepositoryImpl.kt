package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.datasource.CommercialActivityDataSource
import com.sucharu.sucharupro.domain.model.activity.CommercialActivityEvent
import com.sucharu.sucharupro.domain.model.activity.CommercialEntityType
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.repository.CommercialActivityRepository
import kotlinx.coroutines.flow.Flow

/**
 * Production-ready implementation of [CommercialActivityRepository] delegating
 * to [CommercialActivityDataSource].
 *
 * Enforces:
 * - Blank ID rejection before delegating to the data source.
 * - Duplicate event ID rejection (enforced at data-source level).
 * - Append-only audit log semantics — no update or delete operations.
 */
class CommercialActivityRepositoryImpl(
    private val dataSource: CommercialActivityDataSource
) : CommercialActivityRepository {

    override fun observeActivities(): Flow<List<CommercialActivityEvent>> =
        dataSource.observeActivities()

    override fun observeActivitiesForEntity(
        entityType: CommercialEntityType,
        entityId: String
    ): Flow<List<CommercialActivityEvent>> =
        dataSource.observeActivitiesForEntity(entityType, entityId)

    override suspend fun getActivityById(
        activityId: String
    ): DomainResult<CommercialActivityEvent> {
        if (activityId.isBlank()) {
            return DomainResult.Error(message = "Activity ID cannot be blank.")
        }
        return dataSource.fetchActivityById(activityId)
    }

    override suspend fun recordActivity(
        event: CommercialActivityEvent
    ): DomainResult<CommercialActivityEvent> {
        if (event.activityId.isBlank()) {
            return DomainResult.Error(message = "Activity ID cannot be blank.")
        }
        if (event.entityId.isBlank()) {
            return DomainResult.Error(message = "Entity ID cannot be blank.")
        }
        if (event.timestamp.isBlank()) {
            return DomainResult.Error(message = "Timestamp cannot be blank.")
        }
        return dataSource.insertActivity(event)
    }
}
