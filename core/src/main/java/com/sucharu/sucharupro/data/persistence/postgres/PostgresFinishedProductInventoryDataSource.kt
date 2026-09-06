package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.datasource.FinishedProductInventoryDataSource
import com.sucharu.sucharupro.domain.model.inventory.FinishedProductInventoryReceipt
import com.sucharu.sucharupro.domain.model.inventory.InventoryUnit
import java.math.BigDecimal
import java.sql.ResultSet

class PostgresFinishedProductInventoryDataSource(
    private val transactionManager: TransactionManager
) : FinishedProductInventoryDataSource {

    override suspend fun saveReceipt(tenantId: String, receipt: FinishedProductInventoryReceipt) {
        transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = """
                INSERT INTO finished_product_inventory_receipts (
                    receipt_id, project_id, execution_job_id, order_id, product_id,
                    warehouse_id, bin_id, received_quantity, unit, unit_cost,
                    qc_inspection_id, release_id, status, received_by, received_at,
                    notes, version
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, to_timestamp(? / 1000.0), ?, ?)
                ON CONFLICT (project_id, execution_job_id) DO NOTHING
            """.trimIndent()

            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, receipt.receiptId)
                ps.setString(2, tenantId)
                ps.setString(3, receipt.executionJobId)
                ps.setString(4, receipt.orderId)
                ps.setString(5, receipt.productId)
                ps.setString(6, receipt.warehouseId)
                ps.setString(7, receipt.binId)
                ps.setBigDecimal(8, receipt.receivedQuantity)
                ps.setString(9, receipt.unit.name)
                ps.setBigDecimal(10, receipt.unitCost)
                ps.setString(11, receipt.qcInspectionId)
                ps.setString(12, receipt.releaseId)
                ps.setString(13, receipt.status)
                ps.setString(14, receipt.receivedBy)
                ps.setLong(15, receipt.receivedAt)
                ps.setString(16, receipt.notes)
                ps.setLong(17, receipt.version)
                ps.executeUpdate()
            }
        }
    }

    override suspend fun getReceiptByJob(
        tenantId: String,
        executionJobId: String
    ): FinishedProductInventoryReceipt? {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = """
                SELECT * FROM finished_product_inventory_receipts
                WHERE project_id = ? AND execution_job_id = ?
            """.trimIndent()

            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, executionJobId)
                ps.executeQuery().use { rs ->
                    if (rs.next()) mapReceipt(rs) else null
                }
            }
        }
    }

    override suspend fun listReceipts(tenantId: String): List<FinishedProductInventoryReceipt> {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = """
                SELECT * FROM finished_product_inventory_receipts
                WHERE project_id = ?
                ORDER BY received_at DESC
            """.trimIndent()

            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.executeQuery().use { rs ->
                    val list = mutableListOf<FinishedProductInventoryReceipt>()
                    while (rs.next()) {
                        list.add(mapReceipt(rs))
                    }
                    list
                }
            }
        }
    }

    private fun mapReceipt(rs: ResultSet): FinishedProductInventoryReceipt {
        val receivedAtTs = rs.getTimestamp("received_at")
        return FinishedProductInventoryReceipt(
            receiptId = rs.getString("receipt_id"),
            projectId = rs.getString("project_id"),
            executionJobId = rs.getString("execution_job_id"),
            orderId = rs.getString("order_id"),
            productId = rs.getString("product_id"),
            warehouseId = rs.getString("warehouse_id"),
            binId = rs.getString("bin_id"),
            receivedQuantity = rs.getBigDecimal("received_quantity") ?: BigDecimal.ZERO,
            unit = runCatching { InventoryUnit.valueOf(rs.getString("unit")) }.getOrDefault(InventoryUnit.PCS),
            unitCost = rs.getBigDecimal("unit_cost") ?: BigDecimal.ZERO,
            qcInspectionId = rs.getString("qc_inspection_id"),
            releaseId = rs.getString("release_id"),
            status = rs.getString("status") ?: "COMPLETED",
            receivedBy = rs.getString("received_by") ?: "SYSTEM",
            receivedAt = receivedAtTs?.time ?: System.currentTimeMillis(),
            notes = rs.getString("notes"),
            version = rs.getLong("version")
        )
    }
}
