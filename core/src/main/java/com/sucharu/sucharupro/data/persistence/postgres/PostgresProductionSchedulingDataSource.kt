package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.datasource.productionscheduling.ProductionSchedulingDataSource
import com.sucharu.sucharupro.domain.model.production.ProductionStageType
import com.sucharu.sucharupro.domain.model.productionscheduling.*
import java.math.BigDecimal
import java.sql.Connection
import java.sql.ResultSet

class PostgresProductionSchedulingDataSource(
    private val transactionManager: TransactionManager
) : ProductionSchedulingDataSource {

    override suspend fun saveSchedule(
        schedule: ProductionSchedule,
        idempotencyKey: String?
    ): ProductionSchedule {
        return transactionManager.inTransaction(TenantContext(schedule.projectId)) { ctx ->
            val sql = """
                INSERT INTO production_schedules (
                    schedule_id, tenant_id, project_id, execution_job_id, order_id,
                    order_number, version, is_current, status, planned_start_at,
                    planned_end_at, total_setup_minutes, total_run_minutes,
                    slots_json, capacity_windows_json, conflicts_json,
                    schedule_fingerprint, integrity_hash, superseded_by_schedule_id,
                    superseding_reason, approved_at, approved_by, idempotency_key,
                    created_at, created_by, updated_at, updated_by
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (schedule_id) DO UPDATE SET
                    is_current = EXCLUDED.is_current,
                    status = EXCLUDED.status,
                    planned_start_at = EXCLUDED.planned_start_at,
                    planned_end_at = EXCLUDED.planned_end_at,
                    total_setup_minutes = EXCLUDED.total_setup_minutes,
                    total_run_minutes = EXCLUDED.total_run_minutes,
                    slots_json = EXCLUDED.slots_json,
                    capacity_windows_json = EXCLUDED.capacity_windows_json,
                    conflicts_json = EXCLUDED.conflicts_json,
                    superseded_by_schedule_id = EXCLUDED.superseded_by_schedule_id,
                    superseding_reason = EXCLUDED.superseding_reason,
                    approved_at = EXCLUDED.approved_at,
                    approved_by = EXCLUDED.approved_by,
                    updated_at = EXCLUDED.updated_at,
                    updated_by = EXCLUDED.updated_by
            """.trimIndent()

            ctx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, schedule.scheduleId)
                ps.setString(2, schedule.tenantId)
                ps.setString(3, schedule.projectId)
                ps.setString(4, schedule.executionJobId)
                ps.setString(5, schedule.orderId)
                ps.setString(6, schedule.orderNumber)
                ps.setInt(7, schedule.version)
                ps.setBoolean(8, schedule.isCurrent)
                ps.setString(9, schedule.status.name)
                ps.setLong(10, schedule.plannedStartAt)
                ps.setLong(11, schedule.plannedEndAt)
                ps.setInt(12, schedule.totalSetupMinutes)
                ps.setInt(13, schedule.totalRunMinutes)
                ps.setString(14, serializeSlots(schedule.slots))
                ps.setString(15, serializeCapacityWindows(schedule.capacityWindows))
                ps.setString(16, serializeConflicts(schedule.conflicts))
                ps.setString(17, schedule.scheduleFingerprint)
                ps.setString(18, schedule.integrityHash)
                ps.setString(19, schedule.supersededByScheduleId)
                ps.setString(20, schedule.supersedingReason)
                ps.setObject(21, schedule.approvedAt)
                ps.setString(22, schedule.approvedBy)
                ps.setString(23, idempotencyKey)
                ps.setLong(24, schedule.createdAt)
                ps.setString(25, schedule.createdBy)
                ps.setLong(26, schedule.updatedAt)
                ps.setString(27, schedule.updatedBy)
                ps.executeUpdate()
            }

            // Save individual slots
            schedule.slots.forEach { slot ->
                val slotSql = """
                    INSERT INTO production_schedule_slots (
                        slot_id, schedule_id, work_order_id, execution_job_id, tenant_id,
                        sequence_number, stage_type, operation_code, operation_name,
                        machine_id, machine_name, operator_id, operator_name,
                        scheduled_start_timestamp, scheduled_end_timestamp,
                        setup_minutes, run_minutes, priority_score, status, notes
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (slot_id) DO UPDATE SET
                        status = EXCLUDED.status,
                        notes = EXCLUDED.notes
                """.trimIndent()

                ctx.connection.prepareStatement(slotSql).use { ps ->
                    ps.setString(1, slot.slotId)
                    ps.setString(2, slot.scheduleId)
                    ps.setString(3, slot.workOrderId)
                    ps.setString(4, slot.executionJobId)
                    ps.setString(5, schedule.tenantId)
                    ps.setInt(6, slot.sequenceNumber)
                    ps.setString(7, slot.stageType.name)
                    ps.setString(8, slot.operationCode)
                    ps.setString(9, slot.operationName)
                    ps.setString(10, slot.machineId)
                    ps.setString(11, slot.machineName)
                    ps.setString(12, slot.operatorId)
                    ps.setString(13, slot.operatorName)
                    ps.setLong(14, slot.scheduledStartTimestamp)
                    ps.setLong(15, slot.scheduledEndTimestamp)
                    ps.setInt(16, slot.setupMinutes)
                    ps.setInt(17, slot.runMinutes)
                    ps.setBigDecimal(18, slot.priorityScore)
                    ps.setString(19, slot.status.name)
                    ps.setString(20, slot.notes)
                    ps.executeUpdate()
                }
            }

            // Save individual conflicts
            schedule.conflicts.forEach { conflict ->
                val confSql = """
                    INSERT INTO production_schedule_conflicts (
                        conflict_id, schedule_id, tenant_id, conflict_type, severity,
                        work_order_id, machine_id, operator_id, message, is_blocking, recommended_action
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (conflict_id) DO NOTHING
                """.trimIndent()

                ctx.connection.prepareStatement(confSql).use { ps ->
                    ps.setString(1, conflict.conflictId)
                    ps.setString(2, conflict.scheduleId)
                    ps.setString(3, schedule.tenantId)
                    ps.setString(4, conflict.conflictType.name)
                    ps.setString(5, conflict.severity.name)
                    ps.setString(6, conflict.workOrderId)
                    ps.setString(7, conflict.machineId)
                    ps.setString(8, conflict.operatorId)
                    ps.setString(9, conflict.message)
                    ps.setBoolean(10, conflict.isBlocking)
                    ps.setString(11, conflict.recommendedAction)
                    ps.executeUpdate()
                }
            }

            // Save capacity windows
            saveCapacityWindowsInternal(ctx.connection, schedule.capacityWindows)

            schedule
        }
    }

    override suspend fun getScheduleById(tenantId: String, scheduleId: String): ProductionSchedule? {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val sql = "SELECT * FROM production_schedules WHERE tenant_id = ? AND schedule_id = ?"
            ctx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, scheduleId)
                ps.executeQuery().use { rs ->
                    if (rs.next()) mapSchedule(rs) else null
                }
            }
        }
    }

    override suspend fun getScheduleByIdempotencyKey(tenantId: String, idempotencyKey: String): ProductionSchedule? {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val sql = "SELECT * FROM production_schedules WHERE tenant_id = ? AND idempotency_key = ?"
            ctx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, idempotencyKey)
                ps.executeQuery().use { rs ->
                    if (rs.next()) mapSchedule(rs) else null
                }
            }
        }
    }

    override suspend fun listSchedulesByJob(tenantId: String, executionJobId: String): List<ProductionSchedule> {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val sql = "SELECT * FROM production_schedules WHERE tenant_id = ? AND execution_job_id = ? ORDER BY version DESC"
            ctx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, executionJobId)
                ps.executeQuery().use { rs ->
                    val list = mutableListOf<ProductionSchedule>()
                    while (rs.next()) {
                        list.add(mapSchedule(rs))
                    }
                    list
                }
            }
        }
    }

    override suspend fun listSchedules(tenantId: String, limit: Int): List<ProductionSchedule> {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val sql = "SELECT * FROM production_schedules WHERE tenant_id = ? ORDER BY created_at DESC LIMIT ?"
            ctx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setInt(2, limit)
                ps.executeQuery().use { rs ->
                    val list = mutableListOf<ProductionSchedule>()
                    while (rs.next()) {
                        list.add(mapSchedule(rs))
                    }
                    list
                }
            }
        }
    }

    override suspend fun saveDispatchQueueItems(items: List<ProductionDispatchQueueItem>): List<ProductionDispatchQueueItem> {
        if (items.isEmpty()) return emptyList()
        val tenantId = items.first().tenantId
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val sql = """
                INSERT INTO production_dispatch_queue (
                    queue_item_id, tenant_id, schedule_id, schedule_version, work_order_id,
                    execution_job_id, order_id, order_number, sequence_number, stage_type,
                    operation_code, operation_name, target_work_center, machine_id, machine_name,
                    operator_id, operator_name, dispatch_status, priority_score, planned_quantity,
                    estimated_setup_minutes, estimated_run_minutes, scheduled_start_timestamp,
                    scheduled_end_timestamp, queued_at, ready_at, dispatched_at, acknowledged_at,
                    completed_at, notes
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (queue_item_id) DO UPDATE SET
                    dispatch_status = EXCLUDED.dispatch_status,
                    ready_at = EXCLUDED.ready_at,
                    dispatched_at = EXCLUDED.dispatched_at,
                    acknowledged_at = EXCLUDED.acknowledged_at,
                    completed_at = EXCLUDED.completed_at,
                    notes = EXCLUDED.notes
            """.trimIndent()

            ctx.connection.prepareStatement(sql).use { ps ->
                items.forEach { item ->
                    ps.setString(1, item.queueItemId)
                    ps.setString(2, item.tenantId)
                    ps.setString(3, item.scheduleId)
                    ps.setInt(4, item.scheduleVersion)
                    ps.setString(5, item.workOrderId)
                    ps.setString(6, item.executionJobId)
                    ps.setString(7, item.orderId)
                    ps.setString(8, item.orderNumber)
                    ps.setInt(9, item.sequenceNumber)
                    ps.setString(10, item.stageType.name)
                    ps.setString(11, item.operationCode)
                    ps.setString(12, item.operationName)
                    ps.setString(13, item.targetWorkCenter)
                    ps.setString(14, item.machineId)
                    ps.setString(15, item.machineName)
                    ps.setString(16, item.operatorId)
                    ps.setString(17, item.operatorName)
                    ps.setString(18, item.dispatchStatus.name)
                    ps.setBigDecimal(19, item.priorityScore)
                    ps.setBigDecimal(20, item.plannedQuantity)
                    ps.setInt(21, item.estimatedSetupMinutes)
                    ps.setInt(22, item.estimatedRunMinutes)
                    ps.setLong(23, item.scheduledStartTimestamp)
                    ps.setLong(24, item.scheduledEndTimestamp)
                    ps.setLong(25, item.queuedAt)
                    ps.setObject(26, item.readyAt)
                    ps.setObject(27, item.dispatchedAt)
                    ps.setObject(28, item.acknowledgedAt)
                    ps.setObject(29, item.completedAt)
                    ps.setString(30, item.notes)
                    ps.addBatch()
                }
                ps.executeBatch()
            }
            items
        }
    }

    override suspend fun updateDispatchQueueItem(item: ProductionDispatchQueueItem): ProductionDispatchQueueItem {
        return transactionManager.inTransaction(TenantContext(item.tenantId)) { ctx ->
            val sql = """
                UPDATE production_dispatch_queue SET
                    dispatch_status = ?,
                    ready_at = ?,
                    dispatched_at = ?,
                    acknowledged_at = ?,
                    completed_at = ?,
                    notes = ?
                WHERE tenant_id = ? AND queue_item_id = ?
            """.trimIndent()

            ctx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, item.dispatchStatus.name)
                ps.setObject(2, item.readyAt)
                ps.setObject(3, item.dispatchedAt)
                ps.setObject(4, item.acknowledgedAt)
                ps.setObject(5, item.completedAt)
                ps.setString(6, item.notes)
                ps.setString(7, item.tenantId)
                ps.setString(8, item.queueItemId)
                ps.executeUpdate()
            }
            item
        }
    }

    override suspend fun getDispatchQueueItemById(tenantId: String, queueItemId: String): ProductionDispatchQueueItem? {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val sql = "SELECT * FROM production_dispatch_queue WHERE tenant_id = ? AND queue_item_id = ?"
            ctx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, queueItemId)
                ps.executeQuery().use { rs ->
                    if (rs.next()) mapDispatchQueueItem(rs) else null
                }
            }
        }
    }

    override suspend fun listDispatchQueue(
        tenantId: String,
        scheduleId: String?,
        limit: Int
    ): List<ProductionDispatchQueueItem> {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val sql = if (scheduleId != null) {
                "SELECT * FROM production_dispatch_queue WHERE tenant_id = ? AND schedule_id = ? ORDER BY priority_score DESC LIMIT ?"
            } else {
                "SELECT * FROM production_dispatch_queue WHERE tenant_id = ? ORDER BY priority_score DESC LIMIT ?"
            }

            ctx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                if (scheduleId != null) {
                    ps.setString(2, scheduleId)
                    ps.setInt(3, limit)
                } else {
                    ps.setInt(2, limit)
                }
                ps.executeQuery().use { rs ->
                    val list = mutableListOf<ProductionDispatchQueueItem>()
                    while (rs.next()) {
                        list.add(mapDispatchQueueItem(rs))
                    }
                    list
                }
            }
        }
    }

    override suspend fun saveCapacityWindows(windows: List<ProductionCapacityWindow>): List<ProductionCapacityWindow> {
        if (windows.isEmpty()) return emptyList()
        val tenantId = windows.first().tenantId
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            saveCapacityWindowsInternal(ctx.connection, windows)
            windows
        }
    }

    private fun saveCapacityWindowsInternal(conn: Connection, windows: List<ProductionCapacityWindow>) {
        val sql = """
            INSERT INTO production_capacity_windows (
                window_id, tenant_id, machine_id, machine_name, shift_date, shift_type,
                start_timestamp, end_timestamp, total_capacity_minutes, allocated_minutes,
                available_minutes, utilization_rate
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (window_id) DO UPDATE SET
                allocated_minutes = EXCLUDED.allocated_minutes,
                available_minutes = EXCLUDED.available_minutes,
                utilization_rate = EXCLUDED.utilization_rate
        """.trimIndent()

        conn.prepareStatement(sql).use { ps ->
            windows.forEach { win ->
                ps.setString(1, win.windowId)
                ps.setString(2, win.tenantId)
                ps.setString(3, win.machineId)
                ps.setString(4, win.machineName)
                ps.setString(5, win.shiftDate)
                ps.setString(6, win.shiftType.name)
                ps.setLong(7, win.startTimestamp)
                ps.setLong(8, win.endTimestamp)
                ps.setBigDecimal(9, win.totalCapacityMinutes)
                ps.setBigDecimal(10, win.allocatedMinutes)
                ps.setBigDecimal(11, win.availableMinutes)
                ps.setBigDecimal(12, win.utilizationRate)
                ps.addBatch()
            }
            ps.executeBatch()
        }
    }

    override suspend fun listCapacityWindows(tenantId: String, machineId: String?, shiftDate: String?): List<ProductionCapacityWindow> {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val sql = "SELECT * FROM production_capacity_windows WHERE tenant_id = ?" +
                    (if (machineId != null) " AND machine_id = ?" else "") +
                    (if (shiftDate != null) " AND shift_date = ?" else "")

            ctx.connection.prepareStatement(sql).use { ps ->
                var idx = 1
                ps.setString(idx++, tenantId)
                if (machineId != null) ps.setString(idx++, machineId)
                if (shiftDate != null) ps.setString(idx++, shiftDate)

                ps.executeQuery().use { rs ->
                    val list = mutableListOf<ProductionCapacityWindow>()
                    while (rs.next()) {
                        list.add(mapCapacityWindow(rs))
                    }
                    list
                }
            }
        }
    }

    override suspend fun saveScheduleEvent(event: ProductionScheduleEvent): ProductionScheduleEvent {
        return transactionManager.inTransaction(TenantContext(event.tenantId)) { ctx ->
            val sql = """
                INSERT INTO production_schedule_events (
                    event_id, schedule_id, tenant_id, event_type, from_status,
                    to_status, payload, performed_by, performed_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()

            ctx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, event.eventId)
                ps.setString(2, event.scheduleId)
                ps.setString(3, event.tenantId)
                ps.setString(4, event.eventType.name)
                ps.setString(5, event.fromStatus?.name)
                ps.setString(6, event.toStatus?.name)
                ps.setString(7, event.payload)
                ps.setString(8, event.performedBy)
                ps.setLong(9, event.performedAt)
                ps.executeUpdate()
            }
            event
        }
    }

    override suspend fun listScheduleEvents(tenantId: String, scheduleId: String): List<ProductionScheduleEvent> {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val sql = "SELECT * FROM production_schedule_events WHERE tenant_id = ? AND schedule_id = ? ORDER BY performed_at ASC"
            ctx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, scheduleId)
                ps.executeQuery().use { rs ->
                    val list = mutableListOf<ProductionScheduleEvent>()
                    while (rs.next()) {
                        list.add(
                            ProductionScheduleEvent(
                                eventId = rs.getString("event_id"),
                                scheduleId = rs.getString("schedule_id"),
                                tenantId = rs.getString("tenant_id"),
                                eventType = ProductionSchedulingEventType.valueOf(rs.getString("event_type")),
                                fromStatus = rs.getString("from_status")?.let { ScheduleStatus.valueOf(it) },
                                toStatus = rs.getString("to_status")?.let { ScheduleStatus.valueOf(it) },
                                payload = rs.getString("payload"),
                                performedBy = rs.getString("performed_by"),
                                performedAt = rs.getLong("performed_at")
                            )
                        )
                    }
                    list
                }
            }
        }
    }

    // Mapping helpers
    private fun mapSchedule(rs: ResultSet): ProductionSchedule {
        val slotsJson = rs.getString("slots_json")
        val capJson = rs.getString("capacity_windows_json")
        val confJson = rs.getString("conflicts_json")

        return ProductionSchedule(
            scheduleId = rs.getString("schedule_id"),
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            executionJobId = rs.getString("execution_job_id"),
            orderId = rs.getString("order_id"),
            orderNumber = rs.getString("order_number"),
            version = rs.getInt("version"),
            isCurrent = rs.getBoolean("is_current"),
            status = ScheduleStatus.valueOf(rs.getString("status")),
            plannedStartAt = rs.getLong("planned_start_at"),
            plannedEndAt = rs.getLong("planned_end_at"),
            totalSetupMinutes = rs.getInt("total_setup_minutes"),
            totalRunMinutes = rs.getInt("total_run_minutes"),
            slots = deserializeSlots(slotsJson),
            capacityWindows = deserializeCapacityWindows(capJson),
            conflicts = deserializeConflicts(confJson),
            scheduleFingerprint = rs.getString("schedule_fingerprint"),
            integrityHash = rs.getString("integrity_hash"),
            supersededByScheduleId = rs.getString("superseded_by_schedule_id"),
            supersedingReason = rs.getString("superseding_reason"),
            approvedAt = rs.getObject("approved_at") as? Long,
            approvedBy = rs.getString("approved_by"),
            createdAt = rs.getLong("created_at"),
            createdBy = rs.getString("created_by"),
            updatedAt = rs.getLong("updated_at"),
            updatedBy = rs.getString("updated_by")
        )
    }

    private fun mapDispatchQueueItem(rs: ResultSet): ProductionDispatchQueueItem {
        return ProductionDispatchQueueItem(
            queueItemId = rs.getString("queue_item_id"),
            tenantId = rs.getString("tenant_id"),
            scheduleId = rs.getString("schedule_id"),
            scheduleVersion = rs.getInt("schedule_version"),
            workOrderId = rs.getString("work_order_id"),
            executionJobId = rs.getString("execution_job_id"),
            orderId = rs.getString("order_id"),
            orderNumber = rs.getString("order_number"),
            sequenceNumber = rs.getInt("sequence_number"),
            stageType = ProductionStageType.valueOf(rs.getString("stage_type")),
            operationCode = rs.getString("operation_code"),
            operationName = rs.getString("operation_name"),
            targetWorkCenter = rs.getString("target_work_center"),
            machineId = rs.getString("machine_id"),
            machineName = rs.getString("machine_name"),
            operatorId = rs.getString("operator_id"),
            operatorName = rs.getString("operator_name"),
            dispatchStatus = DispatchStatus.valueOf(rs.getString("dispatch_status")),
            priorityScore = rs.getBigDecimal("priority_score"),
            plannedQuantity = rs.getBigDecimal("planned_quantity"),
            estimatedSetupMinutes = rs.getInt("estimated_setup_minutes"),
            estimatedRunMinutes = rs.getInt("estimated_run_minutes"),
            scheduledStartTimestamp = rs.getLong("scheduled_start_timestamp"),
            scheduledEndTimestamp = rs.getLong("scheduled_end_timestamp"),
            queuedAt = rs.getLong("queued_at"),
            readyAt = rs.getObject("ready_at") as? Long,
            dispatchedAt = rs.getObject("dispatched_at") as? Long,
            acknowledgedAt = rs.getObject("acknowledged_at") as? Long,
            completedAt = rs.getObject("completed_at") as? Long,
            notes = rs.getString("notes")
        )
    }

    private fun mapCapacityWindow(rs: ResultSet): ProductionCapacityWindow {
        return ProductionCapacityWindow(
            windowId = rs.getString("window_id"),
            tenantId = rs.getString("tenant_id"),
            machineId = rs.getString("machine_id"),
            machineName = rs.getString("machine_name"),
            shiftDate = rs.getString("shift_date"),
            shiftType = ShiftType.valueOf(rs.getString("shift_type")),
            startTimestamp = rs.getLong("start_timestamp"),
            endTimestamp = rs.getLong("end_timestamp"),
            totalCapacityMinutes = rs.getBigDecimal("total_capacity_minutes"),
            allocatedMinutes = rs.getBigDecimal("allocated_minutes"),
            availableMinutes = rs.getBigDecimal("available_minutes"),
            utilizationRate = rs.getBigDecimal("utilization_rate")
        )
    }

    // Lightweight serializations
    private fun serializeSlots(slots: List<ProductionScheduleSlot>): String {
        return slots.joinToString(";") { s ->
            "${s.slotId}|${s.scheduleId}|${s.workOrderId}|${s.executionJobId}|${s.sequenceNumber}|${s.stageType.name}|${s.operationCode}|${s.operationName}|${s.machineId}|${s.machineName}|${s.operatorId ?: ""}|${s.operatorName ?: ""}|${s.scheduledStartTimestamp}|${s.scheduledEndTimestamp}|${s.setupMinutes}|${s.runMinutes}|${s.priorityScore}|${s.status.name}|${s.notes ?: ""}"
        }
    }

    private fun deserializeSlots(data: String?): List<ProductionScheduleSlot> {
        if (data.isNullOrBlank()) return emptyList()
        return data.split(";").filter { it.isNotBlank() }.map { row ->
            val p = row.split("|")
            ProductionScheduleSlot(
                slotId = p[0],
                scheduleId = p[1],
                workOrderId = p[2],
                executionJobId = p[3],
                sequenceNumber = p[4].toInt(),
                stageType = ProductionStageType.valueOf(p[5]),
                operationCode = p[6],
                operationName = p[7],
                machineId = p[8],
                machineName = p[9],
                operatorId = p[10].ifBlank { null },
                operatorName = p[11].ifBlank { null },
                scheduledStartTimestamp = p[12].toLong(),
                scheduledEndTimestamp = p[13].toLong(),
                setupMinutes = p[14].toInt(),
                runMinutes = p[15].toInt(),
                priorityScore = BigDecimal(p[16]),
                status = DispatchStatus.valueOf(p[17]),
                notes = if (p.size > 18 && p[18].isNotBlank()) p[18] else null
            )
        }
    }

    private fun serializeCapacityWindows(windows: List<ProductionCapacityWindow>): String {
        return windows.joinToString(";") { w ->
            "${w.windowId}|${w.tenantId}|${w.machineId}|${w.machineName}|${w.shiftDate}|${w.shiftType.name}|${w.startTimestamp}|${w.endTimestamp}|${w.totalCapacityMinutes}|${w.allocatedMinutes}|${w.availableMinutes}|${w.utilizationRate}"
        }
    }

    private fun deserializeCapacityWindows(data: String?): List<ProductionCapacityWindow> {
        if (data.isNullOrBlank()) return emptyList()
        return data.split(";").filter { it.isNotBlank() }.map { row ->
            val p = row.split("|")
            ProductionCapacityWindow(
                windowId = p[0],
                tenantId = p[1],
                machineId = p[2],
                machineName = p[3],
                shiftDate = p[4],
                shiftType = ShiftType.valueOf(p[5]),
                startTimestamp = p[6].toLong(),
                endTimestamp = p[7].toLong(),
                totalCapacityMinutes = BigDecimal(p[8]),
                allocatedMinutes = BigDecimal(p[9]),
                availableMinutes = BigDecimal(p[10]),
                utilizationRate = BigDecimal(p[11])
            )
        }
    }

    private fun serializeConflicts(conflicts: List<ProductionScheduleConflict>): String {
        return conflicts.joinToString(";") { c ->
            "${c.conflictId}|${c.scheduleId}|${c.conflictType.name}|${c.severity.name}|${c.workOrderId ?: ""}|${c.machineId ?: ""}|${c.operatorId ?: ""}|${c.message.replace("|", "_")}|${c.isBlocking}|${c.recommendedAction.replace("|", "_")}"
        }
    }

    private fun deserializeConflicts(data: String?): List<ProductionScheduleConflict> {
        if (data.isNullOrBlank()) return emptyList()
        return data.split(";").filter { it.isNotBlank() }.map { row ->
            val p = row.split("|")
            ProductionScheduleConflict(
                conflictId = p[0],
                scheduleId = p[1],
                conflictType = ScheduleConflictType.valueOf(p[2]),
                severity = ConflictSeverity.valueOf(p[3]),
                workOrderId = p[4].ifBlank { null },
                machineId = p[5].ifBlank { null },
                operatorId = p[6].ifBlank { null },
                message = p[7],
                isBlocking = p[8].toBoolean(),
                recommendedAction = p[9]
            )
        }
    }
}
