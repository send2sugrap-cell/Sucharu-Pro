package com.sucharu.sucharupro.domain.service.productionscheduling

import com.sucharu.sucharupro.domain.model.productionscheduling.*

interface ProductionSchedulingService {

    /**
     * Creates and calculates an initial [ProductionSchedule] for a given [executionJobId].
     */
    suspend fun createScheduleForJob(
        tenantId: String,
        executionJobId: String,
        baseStartTime: Long? = null,
        requestedDueDate: Long? = null,
        actor: String,
        idempotencyKey: String? = null
    ): ProductionSchedule

    suspend fun getScheduleById(
        tenantId: String,
        scheduleId: String
    ): ProductionSchedule?

    suspend fun listSchedulesForJob(
        tenantId: String,
        executionJobId: String
    ): List<ProductionSchedule>

    suspend fun listSchedules(
        tenantId: String,
        limit: Int = 50
    ): List<ProductionSchedule>

    /**
     * Approves a schedule and automatically populates the active dispatch queue.
     */
    suspend fun approveSchedule(
        tenantId: String,
        scheduleId: String,
        actor: String
    ): ProductionSchedule

    /**
     * Supersedes an existing schedule and produces a new version with reason and audit.
     */
    suspend fun supersedeSchedule(
        tenantId: String,
        scheduleId: String,
        reason: String,
        newStartTime: Long? = null,
        requestedDueDate: Long? = null,
        actor: String
    ): ProductionSchedule

    /**
     * Dispatches a specific queue item to the shop floor.
     */
    suspend fun dispatchQueueItem(
        tenantId: String,
        queueItemId: String,
        actor: String
    ): ProductionDispatchQueueItem

    /**
     * Acknowledges a dispatched item on the shop floor.
     */
    suspend fun acknowledgeQueueItem(
        tenantId: String,
        queueItemId: String,
        actor: String
    ): ProductionDispatchQueueItem

    suspend fun listDispatchQueue(
        tenantId: String,
        scheduleId: String? = null,
        limit: Int = 100
    ): List<ProductionDispatchQueueItem>

    suspend fun listCapacityWindows(
        tenantId: String,
        machineId: String? = null,
        shiftDate: String? = null
    ): List<ProductionCapacityWindow>

    suspend fun getScheduleConflicts(
        tenantId: String,
        scheduleId: String
    ): List<ProductionScheduleConflict>

    suspend fun reconcileSchedule(
        tenantId: String,
        scheduleId: String
    ): ProductionScheduleReconciliationResult

    suspend fun getAiHandoffContract(
        tenantId: String,
        scheduleId: String
    ): Module17Step06ProductionSchedulingHandoffContract
}
