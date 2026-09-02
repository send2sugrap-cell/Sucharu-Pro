package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.datasource.customerledger.CustomerLedgerDataSource
import com.sucharu.sucharupro.data.persistence.postgres.RowMappers.getEnumByName
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customerledger.CustomerReceivableReconciliation
import com.sucharu.sucharupro.domain.model.customerledger.ReceivableReconciliationStatus
import com.sucharu.sucharupro.domain.model.customerledger.ReconciliationDiscrepancy
import java.math.BigDecimal
import java.sql.ResultSet

/**
 * Production PostgreSQL DataSource for Customer Ledger and Reconciliations (Module 14 Step 05).
 */
class PostgresCustomerLedgerDataSource(
    private val transactionManager: TransactionManager,
    private val defaultTenantId: String = "TENANT-001"
) : CustomerLedgerDataSource {

    private fun mapReconciliation(rs: ResultSet): CustomerReceivableReconciliation {
        return CustomerReceivableReconciliation(
            reconciliationId = rs.getString("reconciliation_id"),
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            customerId = rs.getString("customer_id"),
            customerFinancialAccountId = rs.getString("customer_financial_account_id"),
            reconciledAt = rs.getLong("reconciled_at"),
            reconciledBy = rs.getString("reconciled_by") ?: "system",
            status = rs.getEnumByName("status", ReceivableReconciliationStatus.CONSISTENT),
            invoiceTotalReceivable = rs.getBigDecimal("invoice_total_receivable") ?: BigDecimal.ZERO,
            ledgerCalculatedBalance = rs.getBigDecimal("ledger_calculated_balance") ?: BigDecimal.ZERO,
            availableCreditBalance = rs.getBigDecimal("available_credit_balance") ?: BigDecimal.ZERO,
            difference = rs.getBigDecimal("difference") ?: BigDecimal.ZERO,
            isConsistent = rs.getBoolean("is_consistent"),
            discrepancyCount = rs.getInt("discrepancy_count"),
            discrepancies = emptyList(), // Can be parsed from discrepancies_json if needed
            notes = rs.getString("notes"),
            createdAt = rs.getLong("created_at"),
            version = rs.getLong("version")
        )
    }

    override suspend fun insertReconciliation(
        reconciliation: CustomerReceivableReconciliation
    ): DomainResult<CustomerReceivableReconciliation> {
        val tenantContext = TenantContext(projectId = reconciliation.projectId)
        return try {
            transactionManager.inTransaction(tenantContext) { tx ->
                val sql = """
                    INSERT INTO customer_reconciliations (
                        reconciliation_id, tenant_id, project_id, customer_id, customer_financial_account_id,
                        reconciled_at, reconciled_by, status, invoice_total_receivable,
                        ledger_calculated_balance, available_credit_balance, difference,
                        is_consistent, discrepancy_count, discrepancies_json, notes,
                        created_at, version
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
                tx.connection.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, reconciliation.reconciliationId)
                    stmt.setString(2, reconciliation.tenantId)
                    stmt.setString(3, reconciliation.projectId)
                    stmt.setString(4, reconciliation.customerId)
                    stmt.setString(5, reconciliation.customerFinancialAccountId)
                    stmt.setLong(6, reconciliation.reconciledAt)
                    stmt.setString(7, reconciliation.reconciledBy)
                    stmt.setString(8, reconciliation.status.name)
                    stmt.setBigDecimal(9, reconciliation.invoiceTotalReceivable)
                    stmt.setBigDecimal(10, reconciliation.ledgerCalculatedBalance)
                    stmt.setBigDecimal(11, reconciliation.availableCreditBalance)
                    stmt.setBigDecimal(12, reconciliation.difference)
                    stmt.setBoolean(13, reconciliation.isConsistent)
                    stmt.setInt(14, reconciliation.discrepancyCount)
                    stmt.setString(15, null)
                    stmt.setString(16, reconciliation.notes)
                    stmt.setLong(17, reconciliation.createdAt)
                    stmt.setLong(18, reconciliation.version)
                    stmt.executeUpdate()
                }
                DomainResult.Success(reconciliation)
            }
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to insert customer reconciliation")
        }
    }

    override suspend fun findReconciliationById(
        tenantId: String,
        projectId: String,
        reconciliationId: String
    ): DomainResult<CustomerReceivableReconciliation> {
        val tenantContext = TenantContext(projectId = projectId)
        return try {
            transactionManager.inReadOnly(tenantContext) { tx ->
                val sql = "SELECT * FROM customer_reconciliations WHERE tenant_id = ? AND project_id = ? AND reconciliation_id = ?"
                val rec = tx.connection.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, tenantId)
                    stmt.setString(2, projectId)
                    stmt.setString(3, reconciliationId)
                    val rs = stmt.executeQuery()
                    if (rs.next()) mapReconciliation(rs) else null
                }
                if (rec != null) DomainResult.Success(rec) else DomainResult.Error(IllegalArgumentException("Reconciliation '$reconciliationId' not found"))
            }
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to find reconciliation by ID")
        }
    }

    override suspend fun listReconciliations(
        tenantId: String,
        projectId: String,
        customerId: String?,
        limit: Int,
        offset: Int
    ): DomainResult<List<CustomerReceivableReconciliation>> {
        val tenantContext = TenantContext(projectId = projectId)
        return try {
            transactionManager.inReadOnly(tenantContext) { tx ->
                val sb = StringBuilder("SELECT * FROM customer_reconciliations WHERE tenant_id = ? AND project_id = ?")
                val params = mutableListOf<Any>(tenantId, projectId)
                if (customerId != null) {
                    sb.append(" AND customer_id = ?")
                    params.add(customerId)
                }
                sb.append(" ORDER BY reconciled_at DESC LIMIT ? OFFSET ?")
                params.add(limit)
                params.add(offset)

                val list = tx.connection.prepareStatement(sb.toString()).use { stmt ->
                    params.forEachIndexed { i, p ->
                        when (p) {
                            is String -> stmt.setString(i + 1, p)
                            is Int -> stmt.setInt(i + 1, p)
                        }
                    }
                    val rs = stmt.executeQuery()
                    val res = mutableListOf<CustomerReceivableReconciliation>()
                    while (rs.next()) res.add(mapReconciliation(rs))
                    res
                }
                DomainResult.Success(list)
            }
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to list reconciliations")
        }
    }
}
