package com.sucharu.sucharupro.data.repository.productionscheduling

import com.sucharu.sucharupro.data.datasource.productionscheduling.ProductionSchedulingDataSource
import com.sucharu.sucharupro.domain.model.productionscheduling.*
import com.sucharu.sucharupro.domain.repository.productionscheduling.ProductionSchedulingRepository

class ProductionSchedulingRepositoryImpl(
    private val dataSource: ProductionSchedulingDataSource
) : ProductionSchedulingRepository {

    override suspend fun saveSchedule(schedule: ProductionSchedule, idempotencyKey: String?): ProductionSchedule {
        return dataSource.saveSchedule(schedule, idempotencyKey)
    }

    override suspend fun getScheduleById(tenantId: String, scheduleId: String): ProductionSchedule? {
        return dataSource.getScheduleById(tenantId, scheduleId)
    }

    override suspend fun getScheduleByIdempotencyKey(tenantId: String, idempotencyKey: String): ProductionSchedule? {
        return dataSource.getScheduleByIdempotencyKey(tenantId, idempotencyKey)
    }

    override suspend fun listSchedulesByJob(tenantId: String, executionJobId: String): List<ProductionSchedule> {
        return dataSource.listSchedulesByJob(tenantId, executionJobId)
    }

    override suspend fun listSchedules(tenantId: String, limit: Int): List<ProductionSchedule> {
        return dataSource.listSchedules(tenantId, limit)
    }

    override suspend fun saveDispatchQueueItems(items: List<ProductionDispatchQueueItem>): List<ProductionDispatchQueueItem> {
        return dataSource.saveDispatchQueueItems(items)
    }

    override suspend fun updateDispatchQueueItem(item: ProductionDispatchQueueItem): ProductionDispatchQueueItem {
        return dataSource.updateDispatchQueueItem(item)
    }

    override suspend fun getDispatchQueueItemById(tenantId: String, queueItemId: String): ProductionDispatchQueueItem? {
        return dataSource.getDispatchQueueItemById(tenantId, queueItemId)
    }

    override suspend fun listDispatchQueue(tenantId: String, scheduleId: String?, limit: Int): List<ProductionDispatchQueueItem> {
        return dataSource.listDispatchQueue(tenantId, scheduleId, limit)
    }

    override suspend fun saveCapacityWindows(windows: List<ProductionCapacityWindow>): List<ProductionCapacityWindow> {
        return dataSource.saveCapacityWindows(windows)
    }

    override suspend fun listCapacityWindows(tenantId: String, machineId: String?, shiftDate: String?): List<ProductionCapacityWindow> {
        return dataSource.listCapacityWindows(tenantId, machineId, shiftDate)
    }

    override suspend fun saveScheduleEvent(event: ProductionScheduleEvent): ProductionScheduleEvent {
        return dataSource.saveScheduleEvent(event)
    }

    override suspend fun listScheduleEvents(tenantId: String, scheduleId: String): List<ProductionScheduleEvent> {
        return dataSource.listScheduleEvents(tenantId, scheduleId)
    }
}
