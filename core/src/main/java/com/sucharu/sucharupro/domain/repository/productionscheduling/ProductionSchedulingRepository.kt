package com.sucharu.sucharupro.domain.repository.productionscheduling

import com.sucharu.sucharupro.domain.model.productionscheduling.*

interface ProductionSchedulingRepository {

    suspend fun saveSchedule(
        schedule: ProductionSchedule,
        idempotencyKey: String? = null
    ): ProductionSchedule

    suspend fun getScheduleById(
        tenantId: String,
        scheduleId: String
    ): ProductionSchedule?

    suspend fun getScheduleByIdempotencyKey(
        tenantId: String,
        idempotencyKey: String
    ): ProductionSchedule?

    suspend fun listSchedulesByJob(
        tenantId: String,
        executionJobId: String
    ): List<ProductionSchedule>

    suspend fun listSchedules(
        tenantId: String,
        limit: Int = 50
    ): List<ProductionSchedule>

    suspend fun saveDispatchQueueItems(
        items: List<ProductionDispatchQueueItem>
    ): List<ProductionDispatchQueueItem>

    suspend fun updateDispatchQueueItem(
        item: ProductionDispatchQueueItem
    ): ProductionDispatchQueueItem

    suspend fun getDispatchQueueItemById(
        tenantId: String,
        queueItemId: String
    ): ProductionDispatchQueueItem?

    suspend fun listDispatchQueue(
        tenantId: String,
        scheduleId: String? = null,
        limit: Int = 100
    ): List<ProductionDispatchQueueItem>

    suspend fun saveCapacityWindows(
        windows: List<ProductionCapacityWindow>
    ): List<ProductionCapacityWindow>

    suspend fun listCapacityWindows(
        tenantId: String,
        machineId: String? = null,
        shiftDate: String? = null
    ): List<ProductionCapacityWindow>

    suspend fun saveScheduleEvent(
        event: ProductionScheduleEvent
    ): ProductionScheduleEvent

    suspend fun listScheduleEvents(
        tenantId: String,
        scheduleId: String
    ): List<ProductionScheduleEvent>
}
