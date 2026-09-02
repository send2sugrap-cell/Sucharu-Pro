package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.datasource.productionexecution.ProductionExecutionDataSource
import com.sucharu.sucharupro.domain.model.order.OrderPriority
import com.sucharu.sucharupro.domain.model.production.ProductionStageType
import com.sucharu.sucharupro.domain.model.productionexecution.*
import com.sucharu.sucharupro.domain.model.productionplanning.ProductionJobSpecification
import java.math.BigDecimal
import java.sql.Connection
import java.sql.ResultSet

class PostgresProductionExecutionDataSource(
    private val transactionManager: TransactionManager
) : ProductionExecutionDataSource {

    override suspend fun saveJobExecution(
        job: ProductionJobExecution,
        idempotencyKey: String?
    ): ProductionJobExecution {
        return transactionManager.inTransaction(TenantContext(job.projectId)) { ctx ->
            val sql = """
                INSERT INTO production_job_executions (
                    execution_job_id, tenant_id, project_id, order_id, order_number,
                    order_item_id, customer_id, quotation_id, quotation_version_number,
                    commercial_commitment_id, planning_id, planning_version, title,
                    priority, status, planned_quantity,
                    started_quantity, completed_quantity, rejected_quantity,
                    wastage_quantity, rework_quantity, remaining_quantity,
                    current_stage_type, is_completed, completed_at,
                    completion_summary, job_fingerprint, integrity_hash, version,
                    idempotency_key, created_at, created_by, updated_at, updated_by
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (execution_job_id) DO UPDATE SET
                    status = EXCLUDED.status,
                    started_quantity = EXCLUDED.started_quantity,
                    completed_quantity = EXCLUDED.completed_quantity,
                    rejected_quantity = EXCLUDED.rejected_quantity,
                    wastage_quantity = EXCLUDED.wastage_quantity,
                    rework_quantity = EXCLUDED.rework_quantity,
                    remaining_quantity = EXCLUDED.remaining_quantity,
                    current_stage_type = EXCLUDED.current_stage_type,
                    is_completed = EXCLUDED.is_completed,
                    completed_at = EXCLUDED.completed_at,
                    completion_summary = EXCLUDED.completion_summary,
                    version = EXCLUDED.version,
                    updated_at = EXCLUDED.updated_at,
                    updated_by = EXCLUDED.updated_by
            """.trimIndent()

            ctx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, job.executionJobId)
                ps.setString(2, job.tenantId)
                ps.setString(3, job.projectId)
                ps.setString(4, job.orderId)
                ps.setString(5, job.orderNumber)
                ps.setString(6, job.orderItemId)
                ps.setString(7, job.customerId)
                ps.setString(8, job.quotationId)
                ps.setObject(9, job.quotationVersionNumber)
                ps.setString(10, job.commercialCommitmentId)
                ps.setString(11, job.planningId)
                ps.setInt(12, job.planningVersion)
                ps.setString(13, job.title)
                ps.setString(14, job.priority.name)
                ps.setString(15, job.status.name)
                ps.setBigDecimal(16, job.plannedQuantity)
                ps.setBigDecimal(17, job.startedQuantity)
                ps.setBigDecimal(18, job.completedQuantity)
                ps.setBigDecimal(19, job.rejectedQuantity)
                ps.setBigDecimal(20, job.wastageQuantity)
                ps.setBigDecimal(21, job.reworkQuantity)
                ps.setBigDecimal(22, job.remainingQuantity)
                ps.setString(23, job.currentStageType?.name)
                ps.setBoolean(24, job.isCompleted)
                ps.setObject(25, job.completedAt)
                ps.setString(26, job.completionSummary)
                ps.setString(27, job.jobFingerprint)
                ps.setString(28, job.integrityHash)
                ps.setInt(29, job.version)
                ps.setString(30, idempotencyKey)
                ps.setLong(31, job.createdAt)
                ps.setString(32, job.createdBy)
                ps.setLong(33, job.updatedAt)
                ps.setString(34, job.updatedBy)
                ps.executeUpdate()
            }

            // Save work orders
            job.workOrders.forEach { wo ->
                val woSql = """
                    INSERT INTO production_work_orders (
                        work_order_id, execution_job_id, tenant_id, sequence_number,
                        stage_type, operation_code, operation_name, target_work_center,
                        status, assigned_machine_id, assigned_machine_name,
                        assigned_operator_id, assigned_operator_name,
                        estimated_setup_minutes, estimated_run_minutes,
                        actual_setup_minutes, actual_run_minutes,
                        planned_quantity, completed_quantity, rejected_quantity,
                        wastage_quantity, is_mandatory, is_qc_checkpoint,
                        predecessors_json, started_at, paused_at, completed_at, notes
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (work_order_id) DO UPDATE SET
                        status = EXCLUDED.status,
                        assigned_machine_id = EXCLUDED.assigned_machine_id,
                        assigned_machine_name = EXCLUDED.assigned_machine_name,
                        assigned_operator_id = EXCLUDED.assigned_operator_id,
                        assigned_operator_name = EXCLUDED.assigned_operator_name,
                        actual_setup_minutes = EXCLUDED.actual_setup_minutes,
                        actual_run_minutes = EXCLUDED.actual_run_minutes,
                        completed_quantity = EXCLUDED.completed_quantity,
                        rejected_quantity = EXCLUDED.rejected_quantity,
                        wastage_quantity = EXCLUDED.wastage_quantity,
                        started_at = EXCLUDED.started_at,
                        paused_at = EXCLUDED.paused_at,
                        completed_at = EXCLUDED.completed_at,
                        notes = EXCLUDED.notes
                """.trimIndent()
                ctx.connection.prepareStatement(woSql).use { ps ->
                    ps.setString(1, wo.workOrderId)
                    ps.setString(2, wo.executionJobId)
                    ps.setString(3, wo.tenantId)
                    ps.setInt(4, wo.sequenceNumber)
                    ps.setString(5, wo.stageType.name)
                    ps.setString(6, wo.operationCode)
                    ps.setString(7, wo.operationName)
                    ps.setString(8, wo.targetWorkCenter)
                    ps.setString(9, wo.status.name)
                    ps.setString(10, wo.assignedMachineId)
                    ps.setString(11, wo.assignedMachineName)
                    ps.setString(12, wo.assignedOperatorId)
                    ps.setString(13, wo.assignedOperatorName)
                    ps.setInt(14, wo.estimatedSetupMinutes)
                    ps.setInt(15, wo.estimatedRunMinutes)
                    ps.setInt(16, wo.actualSetupMinutes)
                    ps.setInt(17, wo.actualRunMinutes)
                    ps.setBigDecimal(18, wo.plannedQuantity)
                    ps.setBigDecimal(19, wo.completedQuantity)
                    ps.setBigDecimal(20, wo.rejectedQuantity)
                    ps.setBigDecimal(21, wo.wastageQuantity)
                    ps.setBoolean(22, wo.isMandatory)
                    ps.setBoolean(23, wo.isQcCheckpoint)
                    ps.setString(24, wo.predecessorWorkOrderIds.joinToString(","))
                    ps.setObject(25, wo.startedAt)
                    ps.setObject(26, wo.pausedAt)
                    ps.setObject(27, wo.completedAt)
                    ps.setString(28, wo.notes)
                    ps.executeUpdate()
                }
            }

            job
        }
    }

    override suspend fun getJobExecutionById(
        tenantId: String,
        executionJobId: String
    ): ProductionJobExecution? {
        return transactionManager.inReadOnly(TenantContext(tenantId)) { ctx ->
            val sql = "SELECT * FROM production_job_executions WHERE tenant_id = ? AND execution_job_id = ?"
            ctx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, executionJobId)
                ps.executeQuery().use { rs ->
                    if (rs.next()) {
                        mapJob(ctx.connection, rs, tenantId)
                    } else null
                }
            }
        }
    }

    override suspend fun getJobExecutionByIdempotencyKey(
        tenantId: String,
        idempotencyKey: String
    ): ProductionJobExecution? {
        return transactionManager.inReadOnly(TenantContext(tenantId)) { ctx ->
            val sql = "SELECT * FROM production_job_executions WHERE tenant_id = ? AND idempotency_key = ?"
            ctx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, idempotencyKey)
                ps.executeQuery().use { rs ->
                    if (rs.next()) {
                        mapJob(ctx.connection, rs, tenantId)
                    } else null
                }
            }
        }
    }

    override suspend fun listJobExecutionsByOrder(
        tenantId: String,
        orderId: String
    ): List<ProductionJobExecution> {
        return transactionManager.inReadOnly(TenantContext(tenantId)) { ctx ->
            val sql = "SELECT * FROM production_job_executions WHERE tenant_id = ? AND order_id = ? ORDER BY created_at DESC"
            ctx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, orderId)
                ps.executeQuery().use { rs ->
                    val list = mutableListOf<ProductionJobExecution>()
                    while (rs.next()) {
                        list.add(mapJob(ctx.connection, rs, tenantId))
                    }
                    list
                }
            }
        }
    }

    override suspend fun listJobExecutions(
        tenantId: String,
        limit: Int
    ): List<ProductionJobExecution> {
        return transactionManager.inReadOnly(TenantContext(tenantId)) { ctx ->
            val sql = "SELECT * FROM production_job_executions WHERE tenant_id = ? ORDER BY created_at DESC LIMIT ?"
            ctx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setInt(2, limit)
                ps.executeQuery().use { rs ->
                    val list = mutableListOf<ProductionJobExecution>()
                    while (rs.next()) {
                        list.add(mapJob(ctx.connection, rs, tenantId))
                    }
                    list
                }
            }
        }
    }

    override suspend fun updateWorkOrder(
        workOrder: ProductionWorkOrder
    ): ProductionWorkOrder {
        return transactionManager.inTransaction(TenantContext(workOrder.tenantId)) { ctx ->
            val woSql = """
                UPDATE production_work_orders SET
                    status = ?,
                    assigned_machine_id = ?,
                    assigned_machine_name = ?,
                    assigned_operator_id = ?,
                    assigned_operator_name = ?,
                    actual_setup_minutes = ?,
                    actual_run_minutes = ?,
                    completed_quantity = ?,
                    rejected_quantity = ?,
                    wastage_quantity = ?,
                    started_at = ?,
                    paused_at = ?,
                    completed_at = ?,
                    notes = ?
                WHERE work_order_id = ? AND tenant_id = ?
            """.trimIndent()
            ctx.connection.prepareStatement(woSql).use { ps ->
                ps.setString(1, workOrder.status.name)
                ps.setString(2, workOrder.assignedMachineId)
                ps.setString(3, workOrder.assignedMachineName)
                ps.setString(4, workOrder.assignedOperatorId)
                ps.setString(5, workOrder.assignedOperatorName)
                ps.setInt(6, workOrder.actualSetupMinutes)
                ps.setInt(7, workOrder.actualRunMinutes)
                ps.setBigDecimal(8, workOrder.completedQuantity)
                ps.setBigDecimal(9, workOrder.rejectedQuantity)
                ps.setBigDecimal(10, workOrder.wastageQuantity)
                ps.setObject(11, workOrder.startedAt)
                ps.setObject(12, workOrder.pausedAt)
                ps.setObject(13, workOrder.completedAt)
                ps.setString(14, workOrder.notes)
                ps.setString(15, workOrder.workOrderId)
                ps.setString(16, workOrder.tenantId)
                ps.executeUpdate()
            }
            workOrder
        }
    }

    override suspend fun listWorkOrders(
        tenantId: String,
        executionJobId: String
    ): List<ProductionWorkOrder> {
        return transactionManager.inReadOnly(TenantContext(tenantId)) { ctx ->
            listWorkOrdersInternal(ctx.connection, tenantId, executionJobId)
        }
    }

    private fun listWorkOrdersInternal(
        conn: Connection,
        tenantId: String,
        executionJobId: String
    ): List<ProductionWorkOrder> {
        val sql = "SELECT * FROM production_work_orders WHERE tenant_id = ? AND execution_job_id = ? ORDER BY sequence_number ASC"
        return conn.prepareStatement(sql).use { ps ->
            ps.setString(1, tenantId)
            ps.setString(2, executionJobId)
            ps.executeQuery().use { rs ->
                val list = mutableListOf<ProductionWorkOrder>()
                while (rs.next()) {
                    val predStr = rs.getString("predecessors_json")
                    val preds = if (predStr.isNullOrBlank()) emptyList() else predStr.split(",")

                    list.add(
                        ProductionWorkOrder(
                            workOrderId = rs.getString("work_order_id"),
                            executionJobId = rs.getString("execution_job_id"),
                            tenantId = rs.getString("tenant_id"),
                            sequenceNumber = rs.getInt("sequence_number"),
                            stageType = ProductionStageType.valueOf(rs.getString("stage_type")),
                            operationCode = rs.getString("operation_code"),
                            operationName = rs.getString("operation_name"),
                            targetWorkCenter = rs.getString("target_work_center"),
                            status = WorkOrderStatus.valueOf(rs.getString("status")),
                            assignedMachineId = rs.getString("assigned_machine_id"),
                            assignedMachineName = rs.getString("assigned_machine_name"),
                            assignedOperatorId = rs.getString("assigned_operator_id"),
                            assignedOperatorName = rs.getString("assigned_operator_name"),
                            estimatedSetupMinutes = rs.getInt("estimated_setup_minutes"),
                            estimatedRunMinutes = rs.getInt("estimated_run_minutes"),
                            actualSetupMinutes = rs.getInt("actual_setup_minutes"),
                            actualRunMinutes = rs.getInt("actual_run_minutes"),
                            plannedQuantity = rs.getBigDecimal("planned_quantity"),
                            completedQuantity = rs.getBigDecimal("completed_quantity"),
                            rejectedQuantity = rs.getBigDecimal("rejected_quantity"),
                            wastageQuantity = rs.getBigDecimal("wastage_quantity"),
                            isMandatory = rs.getBoolean("is_mandatory"),
                            isQcCheckpoint = rs.getBoolean("is_qc_checkpoint"),
                            predecessorWorkOrderIds = preds,
                            startedAt = rs.getObject("started_at") as? Long,
                            pausedAt = rs.getObject("paused_at") as? Long,
                            completedAt = rs.getObject("completed_at") as? Long,
                            notes = rs.getString("notes")
                        )
                    )
                }
                list
            }
        }
    }

    override suspend fun saveActual(
        actual: ProductionExecutionActual
    ): ProductionExecutionActual {
        return transactionManager.inTransaction(TenantContext(actual.tenantId)) { ctx ->
            val sql = """
                INSERT INTO production_execution_actuals (
                    actual_id, execution_job_id, work_order_id, tenant_id, stage_type,
                    machine_id, operator_id, started_at, completed_at, duration_seconds,
                    good_quantity, scrap_quantity, rework_quantity, remarks
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            ctx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, actual.actualId)
                ps.setString(2, actual.executionJobId)
                ps.setString(3, actual.workOrderId)
                ps.setString(4, actual.tenantId)
                ps.setString(5, actual.stageType.name)
                ps.setString(6, actual.machineId)
                ps.setString(7, actual.operatorId)
                ps.setLong(8, actual.startedAt)
                ps.setObject(9, actual.completedAt)
                ps.setObject(10, actual.durationSeconds)
                ps.setBigDecimal(11, actual.goodQuantity)
                ps.setBigDecimal(12, actual.scrapQuantity)
                ps.setBigDecimal(13, actual.reworkQuantity)
                ps.setString(14, actual.remarks)
                ps.executeUpdate()
            }
            actual
        }
    }

    override suspend fun listActuals(
        tenantId: String,
        executionJobId: String
    ): List<ProductionExecutionActual> {
        return transactionManager.inReadOnly(TenantContext(tenantId)) { ctx ->
            val sql = "SELECT * FROM production_execution_actuals WHERE tenant_id = ? AND execution_job_id = ? ORDER BY started_at DESC"
            ctx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, executionJobId)
                ps.executeQuery().use { rs ->
                    val list = mutableListOf<ProductionExecutionActual>()
                    while (rs.next()) {
                        list.add(
                            ProductionExecutionActual(
                                actualId = rs.getString("actual_id"),
                                executionJobId = rs.getString("execution_job_id"),
                                workOrderId = rs.getString("work_order_id"),
                                tenantId = rs.getString("tenant_id"),
                                stageType = ProductionStageType.valueOf(rs.getString("stage_type")),
                                machineId = rs.getString("machine_id"),
                                operatorId = rs.getString("operator_id"),
                                startedAt = rs.getLong("started_at"),
                                completedAt = rs.getObject("completed_at") as? Long,
                                durationSeconds = rs.getObject("duration_seconds") as? Long,
                                goodQuantity = rs.getBigDecimal("good_quantity"),
                                scrapQuantity = rs.getBigDecimal("scrap_quantity"),
                                reworkQuantity = rs.getBigDecimal("rework_quantity"),
                                remarks = rs.getString("remarks")
                            )
                        )
                    }
                    list
                }
            }
        }
    }

    override suspend fun saveHold(
        hold: ProductionHold
    ): ProductionHold {
        return transactionManager.inTransaction(TenantContext(hold.tenantId)) { ctx ->
            val sql = """
                INSERT INTO production_execution_holds (
                    hold_id, execution_job_id, work_order_id, tenant_id, category,
                    reason, held_at, held_by, is_resolved, resolved_at, resolved_by, resolution_notes
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (hold_id) DO UPDATE SET
                    is_resolved = EXCLUDED.is_resolved,
                    resolved_at = EXCLUDED.resolved_at,
                    resolved_by = EXCLUDED.resolved_by,
                    resolution_notes = EXCLUDED.resolution_notes
            """.trimIndent()
            ctx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, hold.holdId)
                ps.setString(2, hold.executionJobId)
                ps.setString(3, hold.workOrderId)
                ps.setString(4, hold.tenantId)
                ps.setString(5, hold.category.name)
                ps.setString(6, hold.reason)
                ps.setLong(7, hold.heldAt)
                ps.setString(8, hold.heldBy)
                ps.setBoolean(9, hold.isResolved)
                ps.setObject(10, hold.resolvedAt)
                ps.setString(11, hold.resolvedBy)
                ps.setString(12, hold.resolutionNotes)
                ps.executeUpdate()
            }
            hold
        }
    }

    override suspend fun saveWastage(
        wastage: ProductionWastageRecord
    ): ProductionWastageRecord {
        return transactionManager.inTransaction(TenantContext(wastage.tenantId)) { ctx ->
            val sql = """
                INSERT INTO production_execution_wastages (
                    wastage_id, execution_job_id, work_order_id, tenant_id, material_code,
                    quantity, unit_of_measure, reason, stage_type, recorded_by, recorded_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            ctx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, wastage.wastageId)
                ps.setString(2, wastage.executionJobId)
                ps.setString(3, wastage.workOrderId)
                ps.setString(4, wastage.tenantId)
                ps.setString(5, wastage.materialCode)
                ps.setBigDecimal(6, wastage.quantity)
                ps.setString(7, wastage.unitOfMeasure)
                ps.setString(8, wastage.reason)
                ps.setString(9, wastage.stageType.name)
                ps.setString(10, wastage.recordedBy)
                ps.setLong(11, wastage.recordedAt)
                ps.executeUpdate()
            }
            wastage
        }
    }

    override suspend fun listWastages(
        tenantId: String,
        executionJobId: String
    ): List<ProductionWastageRecord> {
        return transactionManager.inReadOnly(TenantContext(tenantId)) { ctx ->
            val sql = "SELECT * FROM production_execution_wastages WHERE tenant_id = ? AND execution_job_id = ? ORDER BY recorded_at DESC"
            ctx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, executionJobId)
                ps.executeQuery().use { rs ->
                    val list = mutableListOf<ProductionWastageRecord>()
                    while (rs.next()) {
                        list.add(
                            ProductionWastageRecord(
                                wastageId = rs.getString("wastage_id"),
                                executionJobId = rs.getString("execution_job_id"),
                                workOrderId = rs.getString("work_order_id"),
                                tenantId = rs.getString("tenant_id"),
                                materialCode = rs.getString("material_code"),
                                quantity = rs.getBigDecimal("quantity"),
                                unitOfMeasure = rs.getString("unit_of_measure"),
                                reason = rs.getString("reason"),
                                stageType = ProductionStageType.valueOf(rs.getString("stage_type")),
                                recordedBy = rs.getString("recorded_by"),
                                recordedAt = rs.getLong("recorded_at")
                            )
                        )
                    }
                    list
                }
            }
        }
    }

    override suspend fun saveRework(
        rework: ProductionReworkRecord
    ): ProductionReworkRecord {
        return transactionManager.inTransaction(TenantContext(rework.tenantId)) { ctx ->
            val sql = """
                INSERT INTO production_execution_reworks (
                    rework_id, execution_job_id, source_work_order_id, target_work_order_id,
                    tenant_id, quantity, defect_code, reason, status, requested_by, requested_at, resolved_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            ctx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, rework.reworkId)
                ps.setString(2, rework.executionJobId)
                ps.setString(3, rework.sourceWorkOrderId)
                ps.setString(4, rework.targetWorkOrderId)
                ps.setString(5, rework.tenantId)
                ps.setBigDecimal(6, rework.quantity)
                ps.setString(7, rework.defectCode)
                ps.setString(8, rework.reason)
                ps.setString(9, rework.status)
                ps.setString(10, rework.requestedBy)
                ps.setLong(11, rework.requestedAt)
                ps.setObject(12, rework.resolvedAt)
                ps.executeUpdate()
            }
            rework
        }
    }

    override suspend fun listReworks(
        tenantId: String,
        executionJobId: String
    ): List<ProductionReworkRecord> {
        return transactionManager.inReadOnly(TenantContext(tenantId)) { ctx ->
            val sql = "SELECT * FROM production_execution_reworks WHERE tenant_id = ? AND execution_job_id = ? ORDER BY requested_at DESC"
            ctx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, executionJobId)
                ps.executeQuery().use { rs ->
                    val list = mutableListOf<ProductionReworkRecord>()
                    while (rs.next()) {
                        list.add(
                            ProductionReworkRecord(
                                reworkId = rs.getString("rework_id"),
                                executionJobId = rs.getString("execution_job_id"),
                                sourceWorkOrderId = rs.getString("source_work_order_id"),
                                targetWorkOrderId = rs.getString("target_work_order_id"),
                                tenantId = rs.getString("tenant_id"),
                                quantity = rs.getBigDecimal("quantity"),
                                defectCode = rs.getString("defect_code"),
                                reason = rs.getString("reason"),
                                status = rs.getString("status"),
                                requestedBy = rs.getString("requested_by"),
                                requestedAt = rs.getLong("requested_at"),
                                resolvedAt = rs.getObject("resolved_at") as? Long
                            )
                        )
                    }
                    list
                }
            }
        }
    }

    override suspend fun saveExecutionEvent(
        event: ProductionExecutionEvent
    ): ProductionExecutionEvent {
        return transactionManager.inTransaction(TenantContext(event.tenantId)) { ctx ->
            val sql = """
                INSERT INTO production_execution_events (
                    event_id, execution_job_id, work_order_id, tenant_id,
                    event_type, from_status, to_status, payload, performed_by, performed_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            ctx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, event.eventId)
                ps.setString(2, event.executionJobId)
                ps.setString(3, event.workOrderId)
                ps.setString(4, event.tenantId)
                ps.setString(5, event.eventType.name)
                ps.setString(6, event.fromStatus)
                ps.setString(7, event.toStatus)
                ps.setString(8, event.payload)
                ps.setString(9, event.performedBy)
                ps.setLong(10, event.performedAt)
                ps.executeUpdate()
            }
            event
        }
    }

    override suspend fun listExecutionEvents(
        tenantId: String,
        executionJobId: String
    ): List<ProductionExecutionEvent> {
        return transactionManager.inReadOnly(TenantContext(tenantId)) { ctx ->
            val sql = "SELECT * FROM production_execution_events WHERE tenant_id = ? AND execution_job_id = ? ORDER BY performed_at ASC"
            ctx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, executionJobId)
                ps.executeQuery().use { rs ->
                    val list = mutableListOf<ProductionExecutionEvent>()
                    while (rs.next()) {
                        list.add(
                            ProductionExecutionEvent(
                                eventId = rs.getString("event_id"),
                                executionJobId = rs.getString("execution_job_id"),
                                workOrderId = rs.getString("work_order_id"),
                                tenantId = rs.getString("tenant_id"),
                                eventType = ProductionExecutionEventType.valueOf(rs.getString("event_type")),
                                fromStatus = rs.getString("from_status"),
                                toStatus = rs.getString("to_status"),
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

    private fun mapJob(conn: Connection, rs: ResultSet, tenantId: String): ProductionJobExecution {
        val planningId = rs.getString("planning_id")
        val executionJobId = rs.getString("execution_job_id")

        // Load Spec from production_job_specifications
        val specSql = "SELECT * FROM production_job_specifications WHERE planning_id = ?"
        val spec = conn.prepareStatement(specSql).use { s ->
            s.setString(1, planningId)
            s.executeQuery().use { srs ->
                if (srs.next()) {
                    ProductionJobSpecification(
                        specId = srs.getString("spec_id"),
                        jobTitle = srs.getString("job_title"),
                        productType = srs.getString("product_type"),
                        orderedQuantity = srs.getLong("ordered_quantity"),
                        plannedQuantity = srs.getLong("planned_quantity"),
                        finishedWidthMm = srs.getBigDecimal("finished_width_mm"),
                        finishedHeightMm = srs.getBigDecimal("finished_height_mm"),
                        substrateType = srs.getString("substrate_type"),
                        substrateGsm = srs.getInt("substrate_gsm"),
                        substrateBrand = srs.getString("substrate_brand"),
                        parentSheetWidthMm = srs.getBigDecimal("parent_sheet_width_mm"),
                        parentSheetHeightMm = srs.getBigDecimal("parent_sheet_height_mm"),
                        pressSheetWidthMm = srs.getBigDecimal("press_sheet_width_mm"),
                        pressSheetHeightMm = srs.getBigDecimal("press_sheet_height_mm"),
                        printingMethod = srs.getString("printing_method"),
                        colorsFront = srs.getInt("colors_front"),
                        colorsBack = srs.getInt("colors_back"),
                        coatingFront = srs.getString("coating_front"),
                        coatingBack = srs.getString("coating_back"),
                        impositionUps = srs.getInt("imposition_ups"),
                        lamination = srs.getString("lamination"),
                        bindingMethod = srs.getString("binding_method"),
                        foldingType = srs.getString("folding_type"),
                        cuttingRequired = srs.getBoolean("cutting_required"),
                        dieCuttingRequired = srs.getBoolean("die_cutting_required"),
                        packagingMethod = srs.getString("packaging_method"),
                        artworkUrl = srs.getString("artwork_url"),
                        specialInstructions = srs.getString("special_instructions"),
                        specFingerprint = srs.getString("spec_fingerprint")
                    )
                } else {
                    ProductionJobSpecification(
                        specId = "SPEC-$planningId",
                        jobTitle = rs.getString("title"),
                        productType = "GENERAL_PRINT",
                        orderedQuantity = rs.getBigDecimal("planned_quantity").toLong(),
                        plannedQuantity = rs.getBigDecimal("planned_quantity").toLong(),
                        finishedWidthMm = BigDecimal("210.0000"),
                        finishedHeightMm = BigDecimal("297.0000"),
                        substrateType = "ART_PAPER",
                        substrateGsm = 150,
                        parentSheetWidthMm = BigDecimal("640.0000"),
                        parentSheetHeightMm = BigDecimal("900.0000"),
                        pressSheetWidthMm = BigDecimal("640.0000"),
                        pressSheetHeightMm = BigDecimal("450.0000"),
                        printingMethod = "OFFSET",
                        colorsFront = 4,
                        colorsBack = 4,
                        impositionUps = 1,
                        specFingerprint = "FP-$planningId"
                    )
                }
            }
        }

        // Load active hold if any
        val holdSql = "SELECT * FROM production_execution_holds WHERE execution_job_id = ? AND is_resolved = FALSE LIMIT 1"
        val hold = conn.prepareStatement(holdSql).use { s ->
            s.setString(1, executionJobId)
            s.executeQuery().use { hrs ->
                if (hrs.next()) {
                    ProductionHold(
                        holdId = hrs.getString("hold_id"),
                        executionJobId = executionJobId,
                        workOrderId = hrs.getString("work_order_id"),
                        tenantId = hrs.getString("tenant_id"),
                        category = HoldCategory.valueOf(hrs.getString("category")),
                        reason = hrs.getString("reason"),
                        heldAt = hrs.getLong("held_at"),
                        heldBy = hrs.getString("held_by"),
                        isResolved = hrs.getBoolean("is_resolved"),
                        resolvedAt = hrs.getObject("resolved_at") as? Long,
                        resolvedBy = hrs.getString("resolved_by"),
                        resolutionNotes = hrs.getString("resolution_notes")
                    )
                } else null
            }
        }

        val stageType = rs.getString("current_stage_type")?.let { ProductionStageType.valueOf(it) }
        val wos = listWorkOrdersInternal(conn, tenantId, executionJobId)

        return ProductionJobExecution(
            executionJobId = executionJobId,
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            orderId = rs.getString("order_id"),
            orderNumber = rs.getString("order_number"),
            orderItemId = rs.getString("order_item_id"),
            customerId = rs.getString("customer_id"),
            quotationId = rs.getString("quotation_id"),
            quotationVersionNumber = rs.getObject("quotation_version_number") as? Int,
            commercialCommitmentId = rs.getString("commercial_commitment_id"),
            planningId = planningId,
            planningVersion = rs.getInt("planning_version"),
            title = rs.getString("title"),
            priority = OrderPriority.valueOf(rs.getString("priority")),
            status = ProductionJobExecutionStatus.valueOf(rs.getString("status")),
            specification = spec,
            plannedQuantity = rs.getBigDecimal("planned_quantity"),
            startedQuantity = rs.getBigDecimal("started_quantity"),
            completedQuantity = rs.getBigDecimal("completed_quantity"),
            rejectedQuantity = rs.getBigDecimal("rejected_quantity"),
            wastageQuantity = rs.getBigDecimal("wastage_quantity"),
            reworkQuantity = rs.getBigDecimal("rework_quantity"),
            remainingQuantity = rs.getBigDecimal("remaining_quantity"),
            workOrders = wos,
            currentHold = hold,
            currentStageType = stageType,
            isCompleted = rs.getBoolean("is_completed"),
            completedAt = rs.getObject("completed_at") as? Long,
            completionSummary = rs.getString("completion_summary"),
            jobFingerprint = rs.getString("job_fingerprint"),
            integrityHash = rs.getString("integrity_hash"),
            version = rs.getInt("version"),
            createdAt = rs.getLong("created_at"),
            createdBy = rs.getString("created_by"),
            updatedAt = rs.getLong("updated_at"),
            updatedBy = rs.getString("updated_by")
        )
    }
}
