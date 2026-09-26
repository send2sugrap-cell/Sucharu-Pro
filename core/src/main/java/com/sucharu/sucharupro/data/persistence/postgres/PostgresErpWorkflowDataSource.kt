package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.persistence.postgres.RowMappers.getEnumByName
import com.sucharu.sucharupro.domain.model.erp.*
import java.sql.ResultSet
import java.time.Instant

/**
 * Production-grade PostgreSQL DataSource for Form 05 ERP Workflow Orchestration.
 */
class PostgresErpWorkflowDataSource(
    private val transactionManager: TransactionManager,
    private val defaultTenantId: String = "TENANT-001"
) {

    private fun mapOrchestration(rs: ResultSet): ErpWorkflowOrchestration {
        return ErpWorkflowOrchestration(
            orchestrationId = rs.getString("orchestration_id"),
            customerAction = rs.getEnumByName("customer_action", CustomerActionType.ORDER_NOW),
            customerId = rs.getString("customer_id"),
            productId = rs.getString("product_id"),
            offerId = rs.getString("offer_id"),
            priceConfigId = rs.getString("price_config_id"),

            workflowStatus = rs.getEnumByName("workflow_status", ErpWorkflowStatus.INITIATED),
            quotationId = rs.getString("quotation_id"),
            orderId = rs.getString("order_id"),
            jobCardId = rs.getString("job_card_id"),
            qcInspectionId = rs.getString("qc_inspection_id"),
            challanId = rs.getString("challan_id"),
            invoiceId = rs.getString("invoice_id"),

            orderQuantity = rs.getInt("order_quantity"),
            orderAmount = rs.getDouble("order_amount"),
            currency = rs.getString("currency") ?: "BDT",
            specialInstructions = rs.getString("special_instructions"),

            createdAt = rs.getTimestamp("created_at")?.toInstant()?.toString() ?: Instant.now().toString(),
            updatedAt = rs.getTimestamp("updated_at")?.toInstant()?.toString() ?: Instant.now().toString(),
            createdBy = rs.getString("created_by") ?: "SYSTEM",
            updatedBy = rs.getString("updated_by")
        )
    }

    suspend fun insertOrchestration(orch: ErpWorkflowOrchestration, tenantId: String = defaultTenantId): ErpWorkflowOrchestration {
        val tenant = TenantContext(tenantId)
        transactionManager.inTransaction(tenant) { ctx ->
            val sql = """
                INSERT INTO erp_workflow_orchestrations (
                    project_id, orchestration_id, customer_action, customer_id, product_id, offer_id, price_config_id,
                    workflow_status, quotation_id, order_id, job_card_id, qc_inspection_id, challan_id, invoice_id,
                    order_quantity, order_amount, currency, special_instructions,
                    created_at, updated_at, created_by, updated_by, version
                ) VALUES (
                    ?, ?, ?, ?, ?, ?, ?,
                    ?, ?, ?, ?, ?, ?, ?,
                    ?, ?, ?, ?,
                    NOW(), NOW(), ?, ?, 1
                )
            """.trimIndent()

            ctx.sqlExecutor.executeUpdate(
                sql,
                listOf(
                    tenant.projectId, orch.orchestrationId, orch.customerAction.name, orch.customerId, orch.productId, orch.offerId, orch.priceConfigId,
                    orch.workflowStatus.name, orch.quotationId, orch.orderId, orch.jobCardId, orch.qcInspectionId, orch.challanId, orch.invoiceId,
                    orch.orderQuantity, orch.orderAmount, orch.currency, orch.specialInstructions,
                    orch.createdBy, orch.updatedBy
                )
            )
        }
        return orch
    }

    suspend fun getOrchestrationByOrder(orderId: String, tenantId: String = defaultTenantId): ErpWorkflowOrchestration? {
        val tenant = TenantContext(tenantId)
        return transactionManager.inReadOnly(tenant) { ctx ->
            val sql = """
                SELECT * FROM erp_workflow_orchestrations
                WHERE project_id = ? AND order_id = ?
            """.trimIndent()

            ctx.sqlExecutor.queryList(sql, listOf(tenant.projectId, orderId)) { rs ->
                mapOrchestration(rs)
            }.firstOrNull()
        }
    }
}
