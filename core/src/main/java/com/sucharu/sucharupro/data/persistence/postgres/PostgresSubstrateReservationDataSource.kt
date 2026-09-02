package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.datasource.substratereservation.SubstrateReservationDataSource
import com.sucharu.sucharupro.domain.model.printingcalculator.MeasurementUnit
import com.sucharu.sucharupro.domain.model.printingcalculator.PaperStockType
import com.sucharu.sucharupro.domain.model.printingcalculator.PrintingDimension
import com.sucharu.sucharupro.domain.model.substratereservation.SubstrateAllocationSource
import com.sucharu.sucharupro.domain.model.substratereservation.SubstrateReservation
import com.sucharu.sucharupro.domain.model.substratereservation.SubstrateReservationAuditEvent
import com.sucharu.sucharupro.domain.model.substratereservation.SubstrateReservationMode
import com.sucharu.sucharupro.domain.model.substratereservation.SubstrateReservationStatus
import java.math.BigDecimal
import java.sql.ResultSet

class PostgresSubstrateReservationDataSource(
    private val transactionManager: TransactionManager
) : SubstrateReservationDataSource {

    override suspend fun saveReservation(reservation: SubstrateReservation): SubstrateReservation {
        return transactionManager.inTransaction(TenantContext(reservation.tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = """
                INSERT INTO substrate_reservations (
                    reservation_id, tenant_id, order_id, order_item_id, execution_job_id, work_order_id,
                    product_id, sku, product_name, warehouse_id, location_id, stock_type, gsm,
                    sheet_width_mm, sheet_height_mm, reserved_sheets, reserved_reams, reserved_weight_kg,
                    status, reservation_mode, idempotency_key, expiry_timestamp, soft_hold_expires_at,
                    promoted_at, promoted_by, reserved_by, reserved_at, updated_at, notes
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (reservation_id) DO UPDATE SET
                    execution_job_id = EXCLUDED.execution_job_id,
                    work_order_id = EXCLUDED.work_order_id,
                    status = EXCLUDED.status,
                    reservation_mode = EXCLUDED.reservation_mode,
                    reserved_sheets = EXCLUDED.reserved_sheets,
                    reserved_reams = EXCLUDED.reserved_reams,
                    reserved_weight_kg = EXCLUDED.reserved_weight_kg,
                    soft_hold_expires_at = EXCLUDED.soft_hold_expires_at,
                    promoted_at = EXCLUDED.promoted_at,
                    promoted_by = EXCLUDED.promoted_by,
                    updated_at = EXCLUDED.updated_at,
                    notes = EXCLUDED.notes
            """.trimIndent()

            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, reservation.reservationId)
                stmt.setString(2, reservation.tenantId)
                stmt.setString(3, reservation.orderId)
                stmt.setString(4, reservation.orderItemId)
                stmt.setString(5, reservation.executionJobId)
                stmt.setString(6, reservation.workOrderId)
                stmt.setString(7, reservation.productId)
                stmt.setString(8, reservation.sku)
                stmt.setString(9, reservation.productName)
                stmt.setString(10, reservation.warehouseId)
                stmt.setString(11, reservation.locationId)
                stmt.setString(12, reservation.stockType.name)
                stmt.setBigDecimal(13, reservation.gsm)
                stmt.setBigDecimal(14, reservation.sheetDimension.width)
                stmt.setBigDecimal(15, reservation.sheetDimension.height)
                stmt.setLong(16, reservation.reservedSheets)
                stmt.setBigDecimal(17, reservation.reservedReams)
                stmt.setBigDecimal(18, reservation.reservedWeightKg)
                stmt.setString(19, reservation.status.name)
                stmt.setString(20, reservation.mode.name)
                stmt.setString(21, reservation.idempotencyKey)
                if (reservation.expiryTimestamp != null) stmt.setLong(22, reservation.expiryTimestamp) else stmt.setNull(22, java.sql.Types.BIGINT)
                if (reservation.softHoldExpiresAt != null) stmt.setLong(23, reservation.softHoldExpiresAt) else stmt.setNull(23, java.sql.Types.BIGINT)
                if (reservation.promotedAt != null) stmt.setLong(24, reservation.promotedAt) else stmt.setNull(24, java.sql.Types.BIGINT)
                stmt.setString(25, reservation.promotedBy)
                stmt.setString(26, reservation.reservedBy)
                stmt.setLong(27, reservation.reservedAt)
                stmt.setLong(28, reservation.updatedAt)
                stmt.setString(29, reservation.notes)
                stmt.executeUpdate()
            }
            reservation
        }
    }

    override suspend fun getReservationById(tenantId: String, reservationId: String): SubstrateReservation? {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM substrate_reservations WHERE tenant_id = ? AND reservation_id = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, tenantId)
                stmt.setString(2, reservationId)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) rs.toSubstrateReservation() else null
                }
            }
        }
    }

    override suspend fun getReservationByIdempotencyKey(tenantId: String, idempotencyKey: String): SubstrateReservation? {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM substrate_reservations WHERE tenant_id = ? AND idempotency_key = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, tenantId)
                stmt.setString(2, idempotencyKey)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) rs.toSubstrateReservation() else null
                }
            }
        }
    }

    override suspend fun listReservationsByOrder(tenantId: String, orderId: String): List<SubstrateReservation> {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM substrate_reservations WHERE tenant_id = ? AND order_id = ? ORDER BY reserved_at DESC"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, tenantId)
                stmt.setString(2, orderId)
                stmt.executeQuery().use { rs ->
                    val list = mutableListOf<SubstrateReservation>()
                    while (rs.next()) list.add(rs.toSubstrateReservation())
                    list
                }
            }
        }
    }

    override suspend fun listReservationsByJob(tenantId: String, executionJobId: String): List<SubstrateReservation> {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM substrate_reservations WHERE tenant_id = ? AND execution_job_id = ? ORDER BY reserved_at DESC"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, tenantId)
                stmt.setString(2, executionJobId)
                stmt.executeQuery().use { rs ->
                    val list = mutableListOf<SubstrateReservation>()
                    while (rs.next()) list.add(rs.toSubstrateReservation())
                    list
                }
            }
        }
    }

    override suspend fun listReservationsBySku(tenantId: String, sku: String): List<SubstrateReservation> {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM substrate_reservations WHERE tenant_id = ? AND sku = ? ORDER BY reserved_at DESC"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, tenantId)
                stmt.setString(2, sku)
                stmt.executeQuery().use { rs ->
                    val list = mutableListOf<SubstrateReservation>()
                    while (rs.next()) list.add(rs.toSubstrateReservation())
                    list
                }
            }
        }
    }

    override suspend fun listAllReservations(tenantId: String, limit: Int): List<SubstrateReservation> {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM substrate_reservations WHERE tenant_id = ? ORDER BY reserved_at DESC LIMIT ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, tenantId)
                stmt.setInt(2, limit)
                stmt.executeQuery().use { rs ->
                    val list = mutableListOf<SubstrateReservation>()
                    while (rs.next()) list.add(rs.toSubstrateReservation())
                    list
                }
            }
        }
    }

    override suspend fun saveAuditEvent(event: SubstrateReservationAuditEvent) {
        transactionManager.inTransaction(TenantContext(event.tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = """
                INSERT INTO substrate_reservation_audit_events (
                    event_id, reservation_id, tenant_id, previous_status, new_status,
                    quantity_change_sheets, actor, reason, timestamp
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, event.eventId)
                stmt.setString(2, event.reservationId)
                stmt.setString(3, event.tenantId)
                stmt.setString(4, event.previousStatus?.name)
                stmt.setString(5, event.newStatus.name)
                stmt.setLong(6, event.quantityChangeSheets)
                stmt.setString(7, event.actor)
                stmt.setString(8, event.reason)
                stmt.setLong(9, event.timestamp)
                stmt.executeUpdate()
            }
        }
    }

    override suspend fun listAuditEventsByReservation(tenantId: String, reservationId: String): List<SubstrateReservationAuditEvent> {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM substrate_reservation_audit_events WHERE tenant_id = ? AND reservation_id = ? ORDER BY timestamp ASC"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, tenantId)
                stmt.setString(2, reservationId)
                stmt.executeQuery().use { rs ->
                    val list = mutableListOf<SubstrateReservationAuditEvent>()
                    while (rs.next()) {
                        list.add(
                            SubstrateReservationAuditEvent(
                                eventId = rs.getString("event_id"),
                                reservationId = rs.getString("reservation_id"),
                                tenantId = rs.getString("tenant_id"),
                                previousStatus = rs.getString("previous_status")?.let { SubstrateReservationStatus.valueOf(it) },
                                newStatus = SubstrateReservationStatus.valueOf(rs.getString("new_status")),
                                quantityChangeSheets = rs.getLong("quantity_change_sheets"),
                                actor = rs.getString("actor"),
                                reason = rs.getString("reason"),
                                timestamp = rs.getLong("timestamp")
                            )
                        )
                    }
                    list
                }
            }
        }
    }

    override suspend fun saveAllocationSource(source: SubstrateAllocationSource): SubstrateAllocationSource {
        return transactionManager.inTransaction(TenantContext(source.tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = """
                INSERT INTO substrate_reservation_allocations (
                    allocation_id, reservation_id, tenant_id, warehouse_id, location_id,
                    batch_number, allocated_sheets, allocated_reams, allocated_weight_kg,
                    allocated_at, allocated_by
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (allocation_id) DO UPDATE SET
                    location_id = EXCLUDED.location_id,
                    batch_number = EXCLUDED.batch_number,
                    allocated_sheets = EXCLUDED.allocated_sheets,
                    allocated_reams = EXCLUDED.allocated_reams,
                    allocated_weight_kg = EXCLUDED.allocated_weight_kg,
                    allocated_at = EXCLUDED.allocated_at,
                    allocated_by = EXCLUDED.allocated_by
            """.trimIndent()
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, source.allocationId)
                stmt.setString(2, source.reservationId)
                stmt.setString(3, source.tenantId)
                stmt.setString(4, source.warehouseId)
                stmt.setString(5, source.locationId)
                stmt.setString(6, source.batchNumber)
                stmt.setLong(7, source.allocatedSheets)
                stmt.setBigDecimal(8, source.allocatedReams)
                stmt.setBigDecimal(9, source.allocatedWeightKg)
                stmt.setLong(10, source.allocatedAt)
                stmt.setString(11, source.allocatedBy)
                stmt.executeUpdate()
            }
            source
        }
    }

    override suspend fun listAllocationsByReservation(tenantId: String, reservationId: String): List<SubstrateAllocationSource> {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM substrate_reservation_allocations WHERE tenant_id = ? AND reservation_id = ? ORDER BY allocated_at ASC"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, tenantId)
                stmt.setString(2, reservationId)
                stmt.executeQuery().use { rs ->
                    val list = mutableListOf<SubstrateAllocationSource>()
                    while (rs.next()) {
                        list.add(
                            SubstrateAllocationSource(
                                allocationId = rs.getString("allocation_id"),
                                reservationId = rs.getString("reservation_id"),
                                tenantId = rs.getString("tenant_id"),
                                warehouseId = rs.getString("warehouse_id"),
                                locationId = rs.getString("location_id"),
                                batchNumber = rs.getString("batch_number"),
                                allocatedSheets = rs.getLong("allocated_sheets"),
                                allocatedReams = rs.getBigDecimal("allocated_reams"),
                                allocatedWeightKg = rs.getBigDecimal("allocated_weight_kg"),
                                allocatedAt = rs.getLong("allocated_at"),
                                allocatedBy = rs.getString("allocated_by")
                            )
                        )
                    }
                    list
                }
            }
        }
    }

    override suspend fun deleteAllocationsByReservation(tenantId: String, reservationId: String) {
        transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "DELETE FROM substrate_reservation_allocations WHERE tenant_id = ? AND reservation_id = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, tenantId)
                stmt.setString(2, reservationId)
                stmt.executeUpdate()
            }
        }
    }

    private fun ResultSet.toSubstrateReservation(): SubstrateReservation {
        val modeStr = getString("reservation_mode")
        val mode = if (modeStr != null) {
            try { SubstrateReservationMode.valueOf(modeStr) } catch (e: Exception) { SubstrateReservationMode.SOFT }
        } else SubstrateReservationMode.SOFT

        return SubstrateReservation(
            reservationId = getString("reservation_id"),
            tenantId = getString("tenant_id"),
            orderId = getString("order_id"),
            orderItemId = getString("order_item_id"),
            executionJobId = getString("execution_job_id"),
            workOrderId = getString("work_order_id"),
            productId = getString("product_id"),
            sku = getString("sku"),
            productName = getString("product_name"),
            warehouseId = getString("warehouse_id"),
            locationId = getString("location_id"),
            stockType = PaperStockType.valueOf(getString("stock_type")),
            gsm = getBigDecimal("gsm"),
            sheetDimension = PrintingDimension(
                width = getBigDecimal("sheet_width_mm"),
                height = getBigDecimal("sheet_height_mm"),
                unit = MeasurementUnit.MILLIMETERS
            ),
            reservedSheets = getLong("reserved_sheets"),
            reservedReams = getBigDecimal("reserved_reams"),
            reservedWeightKg = getBigDecimal("reserved_weight_kg"),
            status = SubstrateReservationStatus.valueOf(getString("status")),
            mode = mode,
            idempotencyKey = getString("idempotency_key"),
            expiryTimestamp = getLong("expiry_timestamp").let { if (it == 0L && wasNull()) null else it },
            softHoldExpiresAt = getLong("soft_hold_expires_at").let { if (it == 0L && wasNull()) null else it },
            promotedAt = getLong("promoted_at").let { if (it == 0L && wasNull()) null else it },
            promotedBy = getString("promoted_by"),
            reservedBy = getString("reserved_by"),
            reservedAt = getLong("reserved_at"),
            updatedAt = getLong("updated_at"),
            notes = getString("notes")
        )
    }
}
