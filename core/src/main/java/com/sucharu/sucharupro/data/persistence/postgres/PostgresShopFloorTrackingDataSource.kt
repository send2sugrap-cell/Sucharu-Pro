package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.datasource.shopfloortracking.ShopFloorTrackingDataSource
import com.sucharu.sucharupro.domain.model.production.ProductionStageType
import com.sucharu.sucharupro.domain.model.shopfloortracking.*
import java.sql.ResultSet

class PostgresShopFloorTrackingDataSource(
    private val transactionManager: TransactionManager
) : ShopFloorTrackingDataSource {

    override suspend fun saveOperatorTimeRecord(tenantId: String, record: OperatorTimeTrackingRecord) {
        transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = """
                INSERT INTO operator_time_tracking (
                    record_id, tenant_id, work_order_id, execution_job_id, order_id,
                    sequence_number, stage_type, machine_id, machine_name, operator_id,
                    operator_name, current_state, started_at, setup_minutes, run_minutes,
                    downtime_minutes, good_quantity_produced, scrap_quantity_produced,
                    paused_at, pause_reason, completed_at, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (record_id) DO UPDATE SET
                    current_state = EXCLUDED.current_state,
                    started_at = EXCLUDED.started_at,
                    setup_minutes = EXCLUDED.setup_minutes,
                    run_minutes = EXCLUDED.run_minutes,
                    downtime_minutes = EXCLUDED.downtime_minutes,
                    good_quantity_produced = EXCLUDED.good_quantity_produced,
                    scrap_quantity_produced = EXCLUDED.scrap_quantity_produced,
                    paused_at = EXCLUDED.paused_at,
                    pause_reason = EXCLUDED.pause_reason,
                    completed_at = EXCLUDED.completed_at,
                    updated_at = EXCLUDED.updated_at
            """.trimIndent()
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, record.recordId)
                ps.setString(2, tenantId)
                ps.setString(3, record.workOrderId)
                ps.setString(4, record.executionJobId)
                ps.setString(5, record.orderId)
                ps.setInt(6, record.sequenceNumber)
                ps.setString(7, record.stageType.name)
                ps.setString(8, record.machineId)
                ps.setString(9, record.machineName)
                ps.setString(10, record.operatorId)
                ps.setString(11, record.operatorName)
                ps.setString(12, record.currentState.name)
                ps.setObject(13, record.startedAt)
                ps.setInt(14, record.setupMinutes)
                ps.setInt(15, record.runMinutes)
                ps.setInt(16, record.downtimeMinutes)
                ps.setBigDecimal(17, record.goodQuantityProduced)
                ps.setBigDecimal(18, record.scrapQuantityProduced)
                ps.setObject(19, record.pausedAt)
                ps.setString(20, record.pauseReason)
                ps.setObject(21, record.completedAt)
                ps.setLong(22, record.createdAt)
                ps.setLong(23, record.updatedAt)
                ps.executeUpdate()
            }
        }
    }

    override suspend fun getOperatorTimeRecord(tenantId: String, workOrderId: String): OperatorTimeTrackingRecord? {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM operator_time_tracking WHERE tenant_id = ? AND work_order_id = ?"
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, workOrderId)
                ps.executeQuery().use { rs ->
                    if (rs.next()) mapOperatorTimeRecord(rs) else null
                }
            }
        }
    }

    override suspend fun listOperatorTimeRecordsByJob(tenantId: String, executionJobId: String): List<OperatorTimeTrackingRecord> {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM operator_time_tracking WHERE tenant_id = ? AND execution_job_id = ? ORDER BY sequence_number ASC"
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, executionJobId)
                ps.executeQuery().use { rs ->
                    val list = mutableListOf<OperatorTimeTrackingRecord>()
                    while (rs.next()) list.add(mapOperatorTimeRecord(rs))
                    list
                }
            }
        }
    }

    override suspend fun listOperatorTimeRecordsByOperator(tenantId: String, operatorId: String): List<OperatorTimeTrackingRecord> {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM operator_time_tracking WHERE tenant_id = ? AND operator_id = ? ORDER BY updated_at DESC"
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, operatorId)
                ps.executeQuery().use { rs ->
                    val list = mutableListOf<OperatorTimeTrackingRecord>()
                    while (rs.next()) list.add(mapOperatorTimeRecord(rs))
                    list
                }
            }
        }
    }

    override suspend fun saveMaterialConsumptionRecord(tenantId: String, record: ProductionMaterialConsumptionRecord) {
        transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = """
                INSERT INTO production_material_consumption (
                    consumption_id, tenant_id, work_order_id, execution_job_id, stage_type,
                    material_code, material_name, unit_of_measure, planned_quantity,
                    actual_quantity_consumed, scrap_quantity, variance_quantity,
                    variance_percentage, batch_lot_number, status, recorded_by, recorded_at, notes
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (consumption_id) DO UPDATE SET
                    actual_quantity_consumed = EXCLUDED.actual_quantity_consumed,
                    scrap_quantity = EXCLUDED.scrap_quantity,
                    variance_quantity = EXCLUDED.variance_quantity,
                    variance_percentage = EXCLUDED.variance_percentage,
                    batch_lot_number = EXCLUDED.batch_lot_number,
                    status = EXCLUDED.status,
                    notes = EXCLUDED.notes
            """.trimIndent()
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, record.consumptionId)
                ps.setString(2, tenantId)
                ps.setString(3, record.workOrderId)
                ps.setString(4, record.executionJobId)
                ps.setString(5, record.stageType.name)
                ps.setString(6, record.materialCode)
                ps.setString(7, record.materialName)
                ps.setString(8, record.unitOfMeasure)
                ps.setBigDecimal(9, record.plannedQuantity)
                ps.setBigDecimal(10, record.actualQuantityConsumed)
                ps.setBigDecimal(11, record.scrapQuantity)
                ps.setBigDecimal(12, record.varianceQuantity)
                ps.setBigDecimal(13, record.variancePercentage)
                ps.setString(14, record.batchLotNumber)
                ps.setString(15, record.status.name)
                ps.setString(16, record.recordedBy)
                ps.setLong(17, record.recordedAt)
                ps.setString(18, record.notes)
                ps.executeUpdate()
            }
        }
    }

    override suspend fun listMaterialConsumptionsByJob(tenantId: String, executionJobId: String): List<ProductionMaterialConsumptionRecord> {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM production_material_consumption WHERE tenant_id = ? AND execution_job_id = ? ORDER BY recorded_at ASC"
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, executionJobId)
                ps.executeQuery().use { rs ->
                    val list = mutableListOf<ProductionMaterialConsumptionRecord>()
                    while (rs.next()) list.add(mapMaterialConsumption(rs))
                    list
                }
            }
        }
    }

    override suspend fun listMaterialConsumptionsByWorkOrder(tenantId: String, workOrderId: String): List<ProductionMaterialConsumptionRecord> {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM production_material_consumption WHERE tenant_id = ? AND work_order_id = ? ORDER BY recorded_at ASC"
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, workOrderId)
                ps.executeQuery().use { rs ->
                    val list = mutableListOf<ProductionMaterialConsumptionRecord>()
                    while (rs.next()) list.add(mapMaterialConsumption(rs))
                    list
                }
            }
        }
    }

    override suspend fun saveMachineTelemetryLog(tenantId: String, log: MachineTelemetryLog) {
        transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = """
                INSERT INTO machine_telemetry_logs (
                    log_id, tenant_id, machine_id, machine_name, work_order_id, execution_job_id,
                    recorded_speed_units_per_hour, rated_speed_units_per_hour, speed_efficiency_percentage,
                    total_impressions, current_downtime_category, downtime_minutes, temperature_celsius,
                    is_running, logged_at, logged_by
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, log.logId)
                ps.setString(2, tenantId)
                ps.setString(3, log.machineId)
                ps.setString(4, log.machineName)
                ps.setString(5, log.workOrderId)
                ps.setString(6, log.executionJobId)
                ps.setBigDecimal(7, log.recordedSpeedUnitsPerHour)
                ps.setBigDecimal(8, log.ratedSpeedUnitsPerHour)
                ps.setBigDecimal(9, log.speedEfficiencyPercentage)
                ps.setLong(10, log.totalImpressions)
                ps.setString(11, log.currentDowntimeCategory?.name)
                ps.setInt(12, log.downtimeMinutes)
                ps.setBigDecimal(13, log.temperatureCelsius)
                ps.setBoolean(14, log.isRunning)
                ps.setLong(15, log.loggedAt)
                ps.setString(16, log.loggedBy)
                ps.executeUpdate()
            }
        }
    }

    override suspend fun listMachineTelemetryLogsByMachine(tenantId: String, machineId: String, limit: Int): List<MachineTelemetryLog> {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM machine_telemetry_logs WHERE tenant_id = ? AND machine_id = ? ORDER BY logged_at DESC LIMIT ?"
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, machineId)
                ps.setInt(3, limit)
                ps.executeQuery().use { rs ->
                    val list = mutableListOf<MachineTelemetryLog>()
                    while (rs.next()) list.add(mapTelemetryLog(rs))
                    list
                }
            }
        }
    }

    override suspend fun listMachineTelemetryLogsByJob(tenantId: String, executionJobId: String): List<MachineTelemetryLog> {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM machine_telemetry_logs WHERE tenant_id = ? AND execution_job_id = ? ORDER BY logged_at DESC"
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, executionJobId)
                ps.executeQuery().use { rs ->
                    val list = mutableListOf<MachineTelemetryLog>()
                    while (rs.next()) list.add(mapTelemetryLog(rs))
                    list
                }
            }
        }
    }

    override suspend fun saveStageHandoverRecord(tenantId: String, handover: StageOutputHandoverRecord) {
        transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = """
                INSERT INTO stage_output_handovers (
                    handover_id, tenant_id, execution_job_id, from_work_order_id, from_stage,
                    to_work_order_id, to_stage, planned_output_quantity, actual_good_quantity,
                    scrap_quantity, yield_percentage, handed_over_by, handed_over_at,
                    accepted_by, accepted_at, status, discrepancy_notes, integrity_hash
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (handover_id) DO UPDATE SET
                    accepted_by = EXCLUDED.accepted_by,
                    accepted_at = EXCLUDED.accepted_at,
                    status = EXCLUDED.status,
                    discrepancy_notes = EXCLUDED.discrepancy_notes
            """.trimIndent()
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, handover.handoverId)
                ps.setString(2, tenantId)
                ps.setString(3, handover.executionJobId)
                ps.setString(4, handover.fromWorkOrderId)
                ps.setString(5, handover.fromStage.name)
                ps.setString(6, handover.toWorkOrderId)
                ps.setString(7, handover.toStage?.name)
                ps.setBigDecimal(8, handover.plannedOutputQuantity)
                ps.setBigDecimal(9, handover.actualGoodQuantity)
                ps.setBigDecimal(10, handover.scrapQuantity)
                ps.setBigDecimal(11, handover.yieldPercentage)
                ps.setString(12, handover.handedOverBy)
                ps.setLong(13, handover.handedOverAt)
                ps.setString(14, handover.acceptedBy)
                ps.setObject(15, handover.acceptedAt)
                ps.setString(16, handover.status.name)
                ps.setString(17, handover.discrepancyNotes)
                ps.setString(18, handover.integrityHash)
                ps.executeUpdate()
            }
        }
    }

    override suspend fun getStageHandoverRecord(tenantId: String, handoverId: String): StageOutputHandoverRecord? {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM stage_output_handovers WHERE tenant_id = ? AND handover_id = ?"
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, handoverId)
                ps.executeQuery().use { rs ->
                    if (rs.next()) mapStageHandover(rs) else null
                }
            }
        }
    }

    override suspend fun listStageHandoversByJob(tenantId: String, executionJobId: String): List<StageOutputHandoverRecord> {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM stage_output_handovers WHERE tenant_id = ? AND execution_job_id = ? ORDER BY handed_over_at ASC"
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, executionJobId)
                ps.executeQuery().use { rs ->
                    val list = mutableListOf<StageOutputHandoverRecord>()
                    while (rs.next()) list.add(mapStageHandover(rs))
                    list
                }
            }
        }
    }

    override suspend fun saveShopFloorEvent(tenantId: String, event: ShopFloorTrackingEvent) {
        transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = """
                INSERT INTO shop_floor_tracking_events (
                    event_id, tenant_id, work_order_id, execution_job_id, event_type, actor, payload, timestamp
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, event.eventId)
                ps.setString(2, tenantId)
                ps.setString(3, event.workOrderId)
                ps.setString(4, event.executionJobId)
                ps.setString(5, event.eventType.name)
                ps.setString(6, event.actor)
                ps.setString(7, event.payload)
                ps.setLong(8, event.timestamp)
                ps.executeUpdate()
            }
        }
    }

    override suspend fun listShopFloorEventsByJob(tenantId: String, executionJobId: String): List<ShopFloorTrackingEvent> {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM shop_floor_tracking_events WHERE tenant_id = ? AND execution_job_id = ? ORDER BY timestamp ASC"
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, executionJobId)
                ps.executeQuery().use { rs ->
                    val list = mutableListOf<ShopFloorTrackingEvent>()
                    while (rs.next()) {
                        list.add(
                            ShopFloorTrackingEvent(
                                eventId = rs.getString("event_id"),
                                tenantId = rs.getString("tenant_id"),
                                workOrderId = rs.getString("work_order_id"),
                                executionJobId = rs.getString("execution_job_id"),
                                eventType = ShopFloorTrackingEventType.valueOf(rs.getString("event_type")),
                                actor = rs.getString("actor"),
                                payload = rs.getString("payload"),
                                timestamp = rs.getLong("timestamp")
                            )
                        )
                    }
                    list
                }
            }
        }
    }

    private fun mapOperatorTimeRecord(rs: ResultSet): OperatorTimeTrackingRecord {
        return OperatorTimeTrackingRecord(
            recordId = rs.getString("record_id"),
            tenantId = rs.getString("tenant_id"),
            workOrderId = rs.getString("work_order_id"),
            executionJobId = rs.getString("execution_job_id"),
            orderId = rs.getString("order_id"),
            sequenceNumber = rs.getInt("sequence_number"),
            stageType = ProductionStageType.valueOf(rs.getString("stage_type")),
            machineId = rs.getString("machine_id"),
            machineName = rs.getString("machine_name"),
            operatorId = rs.getString("operator_id"),
            operatorName = rs.getString("operator_name"),
            currentState = OperatorTrackingState.valueOf(rs.getString("current_state")),
            startedAt = rs.getObject("started_at") as? Long,
            setupMinutes = rs.getInt("setup_minutes"),
            runMinutes = rs.getInt("run_minutes"),
            downtimeMinutes = rs.getInt("downtime_minutes"),
            goodQuantityProduced = rs.getBigDecimal("good_quantity_produced"),
            scrapQuantityProduced = rs.getBigDecimal("scrap_quantity_produced"),
            pausedAt = rs.getObject("paused_at") as? Long,
            pauseReason = rs.getString("pause_reason"),
            completedAt = rs.getObject("completed_at") as? Long,
            createdAt = rs.getLong("created_at"),
            updatedAt = rs.getLong("updated_at")
        )
    }

    private fun mapMaterialConsumption(rs: ResultSet): ProductionMaterialConsumptionRecord {
        return ProductionMaterialConsumptionRecord(
            consumptionId = rs.getString("consumption_id"),
            tenantId = rs.getString("tenant_id"),
            workOrderId = rs.getString("work_order_id"),
            executionJobId = rs.getString("execution_job_id"),
            stageType = ProductionStageType.valueOf(rs.getString("stage_type")),
            materialCode = rs.getString("material_code"),
            materialName = rs.getString("material_name"),
            unitOfMeasure = rs.getString("unit_of_measure"),
            plannedQuantity = rs.getBigDecimal("planned_quantity"),
            actualQuantityConsumed = rs.getBigDecimal("actual_quantity_consumed"),
            scrapQuantity = rs.getBigDecimal("scrap_quantity"),
            varianceQuantity = rs.getBigDecimal("variance_quantity"),
            variancePercentage = rs.getBigDecimal("variance_percentage"),
            batchLotNumber = rs.getString("batch_lot_number"),
            status = MaterialConsumptionStatus.valueOf(rs.getString("status")),
            recordedBy = rs.getString("recorded_by"),
            recordedAt = rs.getLong("recorded_at"),
            notes = rs.getString("notes")
        )
    }

    private fun mapTelemetryLog(rs: ResultSet): MachineTelemetryLog {
        return MachineTelemetryLog(
            logId = rs.getString("log_id"),
            tenantId = rs.getString("tenant_id"),
            machineId = rs.getString("machine_id"),
            machineName = rs.getString("machine_name"),
            workOrderId = rs.getString("work_order_id"),
            executionJobId = rs.getString("execution_job_id"),
            recordedSpeedUnitsPerHour = rs.getBigDecimal("recorded_speed_units_per_hour"),
            ratedSpeedUnitsPerHour = rs.getBigDecimal("rated_speed_units_per_hour"),
            speedEfficiencyPercentage = rs.getBigDecimal("speed_efficiency_percentage"),
            totalImpressions = rs.getLong("total_impressions"),
            currentDowntimeCategory = rs.getString("current_downtime_category")?.let { DowntimeCategory.valueOf(it) },
            downtimeMinutes = rs.getInt("downtime_minutes"),
            temperatureCelsius = rs.getBigDecimal("temperature_celsius"),
            isRunning = rs.getBoolean("is_running"),
            loggedAt = rs.getLong("logged_at"),
            loggedBy = rs.getString("logged_by")
        )
    }

    private fun mapStageHandover(rs: ResultSet): StageOutputHandoverRecord {
        return StageOutputHandoverRecord(
            handoverId = rs.getString("handover_id"),
            tenantId = rs.getString("tenant_id"),
            executionJobId = rs.getString("execution_job_id"),
            fromWorkOrderId = rs.getString("from_work_order_id"),
            fromStage = ProductionStageType.valueOf(rs.getString("from_stage")),
            toWorkOrderId = rs.getString("to_work_order_id"),
            toStage = rs.getString("to_stage")?.let { ProductionStageType.valueOf(it) },
            plannedOutputQuantity = rs.getBigDecimal("planned_output_quantity"),
            actualGoodQuantity = rs.getBigDecimal("actual_good_quantity"),
            scrapQuantity = rs.getBigDecimal("scrap_quantity"),
            yieldPercentage = rs.getBigDecimal("yield_percentage"),
            handedOverBy = rs.getString("handed_over_by"),
            handedOverAt = rs.getLong("handed_over_at"),
            acceptedBy = rs.getString("accepted_by"),
            acceptedAt = rs.getObject("accepted_at") as? Long,
            status = HandoverStatus.valueOf(rs.getString("status")),
            discrepancyNotes = rs.getString("discrepancy_notes"),
            integrityHash = rs.getString("integrity_hash")
        )
    }
}
