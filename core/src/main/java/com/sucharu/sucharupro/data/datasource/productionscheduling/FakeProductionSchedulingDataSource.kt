package com.sucharu.sucharupro.data.datasource.productionscheduling

import com.sucharu.sucharupro.domain.model.productionscheduling.*

class FakeProductionSchedulingDataSource : ProductionSchedulingDataSource {

    private val schedules = mutableListOf<ProductionSchedule>()
    private val idempotencyMap = mutableMapOf<String, String>() // tenantId+key -> scheduleId
    private val dispatchQueue = mutableListOf<ProductionDispatchQueueItem>()
    private val capacityWindows = mutableListOf<ProductionCapacityWindow>()
    private val events = mutableListOf<ProductionScheduleEvent>()

    override suspend fun saveSchedule(
        schedule: ProductionSchedule,
        idempotencyKey: String?
    ): ProductionSchedule {
        schedules.removeIf { it.scheduleId == schedule.scheduleId }
        schedules.add(schedule)

        if (idempotencyKey != null) {
            idempotencyMap["${schedule.tenantId}:$idempotencyKey"] = schedule.scheduleId
        }

        return schedule
    }

    override suspend fun getScheduleById(
        tenantId: String,
        scheduleId: String
    ): ProductionSchedule? {
        return schedules.firstOrNull { it.tenantId == tenantId && it.scheduleId == scheduleId }
    }

    override suspend fun getScheduleByIdempotencyKey(
        tenantId: String,
        idempotencyKey: String
    ): ProductionSchedule? {
        val scheduleId = idempotencyMap["$tenantId:$idempotencyKey"] ?: return null
        return getScheduleById(tenantId, scheduleId)
    }

    override suspend fun listSchedulesByJob(
        tenantId: String,
        executionJobId: String
    ): List<ProductionSchedule> {
        return schedules.filter { it.tenantId == tenantId && it.executionJobId == executionJobId }
            .sortedByDescending { it.version }
    }

    override suspend fun listSchedules(
        tenantId: String,
        limit: Int
    ): List<ProductionSchedule> {
        return schedules.filter { it.tenantId == tenantId }
            .sortedByDescending { it.createdAt }
            .take(limit)
    }

    override suspend fun saveDispatchQueueItems(
        items: List<ProductionDispatchQueueItem>
    ): List<ProductionDispatchQueueItem> {
        items.forEach { item ->
            dispatchQueue.removeIf { it.queueItemId == item.queueItemId }
            dispatchQueue.add(item)
        }
        return items
    }

    override suspend fun updateDispatchQueueItem(
        item: ProductionDispatchQueueItem
    ): ProductionDispatchQueueItem {
        dispatchQueue.removeIf { it.queueItemId == item.queueItemId }
        dispatchQueue.add(item)
        return item
    }

    override suspend fun getDispatchQueueItemById(
        tenantId: String,
        queueItemId: String
    ): ProductionDispatchQueueItem? {
        return dispatchQueue.firstOrNull { it.tenantId == tenantId && it.queueItemId == queueItemId }
    }

    override suspend fun listDispatchQueue(
        tenantId: String,
        scheduleId: String?,
        limit: Int
    ): List<ProductionDispatchQueueItem> {
        return dispatchQueue.filter { item ->
            item.tenantId == tenantId && (scheduleId == null || item.scheduleId == scheduleId)
        }.sortedByDescending { it.priorityScore }.take(limit)
    }

    override suspend fun saveCapacityWindows(
        windows: List<ProductionCapacityWindow>
    ): List<ProductionCapacityWindow> {
        windows.forEach { win ->
            capacityWindows.removeIf { it.windowId == win.windowId }
            capacityWindows.add(win)
        }
        return windows
    }

    override suspend fun listCapacityWindows(
        tenantId: String,
        machineId: String?,
        shiftDate: String?
    ): List<ProductionCapacityWindow> {
        return capacityWindows.filter { win ->
            win.tenantId == tenantId &&
                    (machineId == null || win.machineId == machineId) &&
                    (shiftDate == null || win.shiftDate == shiftDate)
        }
    }

    override suspend fun saveScheduleEvent(
        event: ProductionScheduleEvent
    ): ProductionScheduleEvent {
        events.add(event)
        return event
    }

    override suspend fun listScheduleEvents(
        tenantId: String,
        scheduleId: String
    ): List<ProductionScheduleEvent> {
        return events.filter { it.tenantId == tenantId && it.scheduleId == scheduleId }
            .sortedBy { it.performedAt }
    }
}
