package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.datasource.businesscostcontrol.*
import com.sucharu.sucharupro.domain.model.businesscostcontrol.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.sql.ResultSet

/**
 * Production PostgreSQL JDBC Data Source for Cost Commitments, Consumptions, Accruals, Reversals, Financial Periods & Audits.
 */
class PostgresBusinessCostControlDataSource(
    private val transactionManager: TransactionManager
) : BusinessCostControlDataSource {

    // --- Financial Periods ---

    override suspend fun createFinancialPeriod(period: BusinessFinancialPeriod): BusinessFinancialPeriod {
        return transactionManager.inTransaction(TenantContext(period.projectId)) { tx ->
            val sql = """
                INSERT INTO business_financial_periods (
                    id, tenant_id, project_id, period_code, period_name, start_date, end_date,
                    status, closed_by, closed_at, close_reason, created_at, created_by,
                    updated_at, updated_by, version
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, period.id)
                ps.setString(2, period.tenantId)
                ps.setString(3, period.projectId)
                ps.setString(4, period.periodCode)
                ps.setString(5, period.periodName)
                ps.setLong(6, period.startDate)
                ps.setLong(7, period.endDate)
                ps.setString(8, period.status.name)
                ps.setString(9, period.closedBy)
                if (period.closedAt != null) ps.setLong(10, period.closedAt) else ps.setNull(10, java.sql.Types.BIGINT)
                ps.setString(11, period.closeReason)
                ps.setLong(12, period.createdAt)
                ps.setString(13, period.createdBy)
                ps.setLong(14, period.updatedAt)
                ps.setString(15, period.updatedBy)
                ps.setLong(16, period.version)
                ps.executeUpdate()
            }
            period
        }
    }

    override suspend fun findFinancialPeriodById(id: String, tenantId: String, projectId: String): BusinessFinancialPeriod? {
        return transactionManager.inReadOnly(TenantContext(projectId)) { tx ->
            val sql = "SELECT * FROM business_financial_periods WHERE id = ? AND tenant_id = ? AND project_id = ?"
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, id)
                ps.setString(2, tenantId)
                ps.setString(3, projectId)
                ps.executeQuery().use { rs ->
                    if (rs.next()) mapFinancialPeriod(rs) else null
                }
            }
        }
    }

    override suspend fun findFinancialPeriodByCode(periodCode: String, tenantId: String, projectId: String): BusinessFinancialPeriod? {
        return transactionManager.inReadOnly(TenantContext(projectId)) { tx ->
            val sql = "SELECT * FROM business_financial_periods WHERE LOWER(period_code) = LOWER(?) AND tenant_id = ? AND project_id = ?"
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, periodCode)
                ps.setString(2, tenantId)
                ps.setString(3, projectId)
                ps.executeQuery().use { rs ->
                    if (rs.next()) mapFinancialPeriod(rs) else null
                }
            }
        }
    }

    override suspend fun updateFinancialPeriod(period: BusinessFinancialPeriod): BusinessFinancialPeriod {
        return transactionManager.inTransaction(TenantContext(period.projectId)) { tx ->
            val sql = """
                UPDATE business_financial_periods SET
                    period_name = ?, start_date = ?, end_date = ?, status = ?,
                    closed_by = ?, closed_at = ?, close_reason = ?,
                    updated_at = ?, updated_by = ?, version = version + 1
                WHERE id = ? AND tenant_id = ? AND project_id = ?
            """.trimIndent()
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, period.periodName)
                ps.setLong(2, period.startDate)
                ps.setLong(3, period.endDate)
                ps.setString(4, period.status.name)
                ps.setString(5, period.closedBy)
                if (period.closedAt != null) ps.setLong(6, period.closedAt) else ps.setNull(6, java.sql.Types.BIGINT)
                ps.setString(7, period.closeReason)
                ps.setLong(8, System.currentTimeMillis())
                ps.setString(9, period.updatedBy)
                ps.setString(10, period.id)
                ps.setString(11, period.tenantId)
                ps.setString(12, period.projectId)
                val rows = ps.executeUpdate()
                if (rows == 0) throw NoSuchElementException("Financial period '${period.id}' not found for update.")
            }
            findFinancialPeriodById(period.id, period.tenantId, period.projectId)
                ?: period.copy(version = period.version + 1)
        }
    }

    override suspend fun listFinancialPeriods(tenantId: String, projectId: String, filter: BusinessFinancialPeriodFilter): List<BusinessFinancialPeriod> {
        return transactionManager.inReadOnly(TenantContext(projectId)) { tx ->
            val sb = StringBuilder("SELECT * FROM business_financial_periods WHERE tenant_id = ? AND project_id = ?")
            val params = mutableListOf<Any>(tenantId, projectId)

            if (filter.status != null) {
                sb.append(" AND status = ?")
                params.add(filter.status.name)
            }
            sb.append(" ORDER BY start_date DESC")

            tx.connection.prepareStatement(sb.toString()).use { ps ->
                params.forEachIndexed { i, p -> ps.setObject(i + 1, p) }
                ps.executeQuery().use { rs ->
                    val list = mutableListOf<BusinessFinancialPeriod>()
                    while (rs.next()) {
                        list.add(mapFinancialPeriod(rs))
                    }
                    list
                }
            }
        }
    }

    // --- Commitments ---

    override suspend fun createCommitment(commitment: BusinessCostCommitment): BusinessCostCommitment {
        return transactionManager.inTransaction(TenantContext(commitment.projectId)) { tx ->
            val sql = """
                INSERT INTO business_cost_commitments (
                    id, tenant_id, project_id, commitment_number, vendor_id, job_id,
                    cost_center_id, cost_category_id, description, committed_amount,
                    consumed_amount, remaining_amount, currency, commitment_date,
                    expected_date, period_id, status, source_type, source_id,
                    created_by, approved_by, created_at, updated_at, version
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, commitment.id)
                ps.setString(2, commitment.tenantId)
                ps.setString(3, commitment.projectId)
                ps.setString(4, commitment.commitmentNumber)
                ps.setString(5, commitment.vendorId)
                ps.setString(6, commitment.jobId)
                ps.setString(7, commitment.costCenterId)
                ps.setString(8, commitment.costCategoryId)
                ps.setString(9, commitment.description)
                ps.setBigDecimal(10, commitment.committedAmount)
                ps.setBigDecimal(11, commitment.consumedAmount)
                ps.setBigDecimal(12, commitment.remainingAmount)
                ps.setString(13, commitment.currency)
                ps.setLong(14, commitment.commitmentDate)
                if (commitment.expectedDate != null) ps.setLong(15, commitment.expectedDate) else ps.setNull(15, java.sql.Types.BIGINT)
                ps.setString(16, commitment.periodId)
                ps.setString(17, commitment.status.name)
                ps.setString(18, commitment.sourceType.name)
                ps.setString(19, commitment.sourceId)
                ps.setString(20, commitment.createdBy)
                ps.setString(21, commitment.approvedBy)
                ps.setLong(22, commitment.createdAt)
                ps.setLong(23, commitment.updatedAt)
                ps.setLong(24, commitment.version)
                ps.executeUpdate()
            }
            commitment
        }
    }

    override suspend fun findCommitmentById(id: String, tenantId: String, projectId: String): BusinessCostCommitment? {
        return transactionManager.inReadOnly(TenantContext(projectId)) { tx ->
            val sql = "SELECT * FROM business_cost_commitments WHERE id = ? AND tenant_id = ? AND project_id = ?"
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, id)
                ps.setString(2, tenantId)
                ps.setString(3, projectId)
                ps.executeQuery().use { rs ->
                    if (rs.next()) mapCommitment(rs) else null
                }
            }
        }
    }

    override suspend fun findCommitmentByNumber(commitmentNumber: String, tenantId: String, projectId: String): BusinessCostCommitment? {
        return transactionManager.inReadOnly(TenantContext(projectId)) { tx ->
            val sql = "SELECT * FROM business_cost_commitments WHERE LOWER(commitment_number) = LOWER(?) AND tenant_id = ? AND project_id = ?"
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, commitmentNumber)
                ps.setString(2, tenantId)
                ps.setString(3, projectId)
                ps.executeQuery().use { rs ->
                    if (rs.next()) mapCommitment(rs) else null
                }
            }
        }
    }

    override suspend fun updateCommitment(commitment: BusinessCostCommitment): BusinessCostCommitment {
        return transactionManager.inTransaction(TenantContext(commitment.projectId)) { tx ->
            val sql = """
                UPDATE business_cost_commitments SET
                    vendor_id = ?, job_id = ?, cost_center_id = ?, cost_category_id = ?,
                    description = ?, committed_amount = ?, consumed_amount = ?, remaining_amount = ?,
                    currency = ?, expected_date = ?, period_id = ?, status = ?,
                    approved_by = ?, updated_at = ?, version = version + 1
                WHERE id = ? AND tenant_id = ? AND project_id = ?
            """.trimIndent()
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, commitment.vendorId)
                ps.setString(2, commitment.jobId)
                ps.setString(3, commitment.costCenterId)
                ps.setString(4, commitment.costCategoryId)
                ps.setString(5, commitment.description)
                ps.setBigDecimal(6, commitment.committedAmount)
                ps.setBigDecimal(7, commitment.consumedAmount)
                ps.setBigDecimal(8, commitment.remainingAmount)
                ps.setString(9, commitment.currency)
                if (commitment.expectedDate != null) ps.setLong(10, commitment.expectedDate) else ps.setNull(10, java.sql.Types.BIGINT)
                ps.setString(11, commitment.periodId)
                ps.setString(12, commitment.status.name)
                ps.setString(13, commitment.approvedBy)
                ps.setLong(14, System.currentTimeMillis())
                ps.setString(15, commitment.id)
                ps.setString(16, commitment.tenantId)
                ps.setString(17, commitment.projectId)
                val rows = ps.executeUpdate()
                if (rows == 0) throw NoSuchElementException("Commitment '${commitment.id}' not found for update.")
            }
            findCommitmentById(commitment.id, commitment.tenantId, commitment.projectId)
                ?: commitment.copy(version = commitment.version + 1)
        }
    }

    override suspend fun listCommitments(tenantId: String, projectId: String, filter: BusinessCostCommitmentFilter): List<BusinessCostCommitment> {
        return transactionManager.inReadOnly(TenantContext(projectId)) { tx ->
            val sb = StringBuilder("SELECT * FROM business_cost_commitments WHERE tenant_id = ? AND project_id = ?")
            val params = mutableListOf<Any>(tenantId, projectId)

            if (filter.status != null) {
                sb.append(" AND status = ?")
                params.add(filter.status.name)
            }
            if (filter.vendorId != null) {
                sb.append(" AND vendor_id = ?")
                params.add(filter.vendorId)
            }
            if (filter.jobId != null) {
                sb.append(" AND job_id = ?")
                params.add(filter.jobId)
            }
            if (filter.costCenterId != null) {
                sb.append(" AND cost_center_id = ?")
                params.add(filter.costCenterId)
            }
            if (filter.costCategoryId != null) {
                sb.append(" AND cost_category_id = ?")
                params.add(filter.costCategoryId)
            }
            if (filter.periodId != null) {
                sb.append(" AND period_id = ?")
                params.add(filter.periodId)
            }
            if (filter.sourceType != null) {
                sb.append(" AND source_type = ?")
                params.add(filter.sourceType.name)
            }
            sb.append(" ORDER BY commitment_date DESC")

            tx.connection.prepareStatement(sb.toString()).use { ps ->
                params.forEachIndexed { i, p -> ps.setObject(i + 1, p) }
                ps.executeQuery().use { rs ->
                    val list = mutableListOf<BusinessCostCommitment>()
                    while (rs.next()) {
                        list.add(mapCommitment(rs))
                    }
                    list
                }
            }
        }
    }

    // --- Consumptions ---

    override suspend fun recordConsumption(consumption: BusinessCostCommitmentConsumption): BusinessCostCommitmentConsumption {
        return transactionManager.inTransaction(TenantContext(consumption.projectId)) { tx ->
            val sql = """
                INSERT INTO business_cost_commitment_consumptions (
                    id, commitment_id, tenant_id, project_id, source_type, source_id,
                    amount, currency, consumed_at, created_by, idempotency_key, notes
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, consumption.id)
                ps.setString(2, consumption.commitmentId)
                ps.setString(3, consumption.tenantId)
                ps.setString(4, consumption.projectId)
                ps.setString(5, consumption.sourceType.name)
                ps.setString(6, consumption.sourceId)
                ps.setBigDecimal(7, consumption.amount)
                ps.setString(8, consumption.currency)
                ps.setLong(9, consumption.consumedAt)
                ps.setString(10, consumption.createdBy)
                ps.setString(11, consumption.idempotencyKey)
                ps.setString(12, consumption.notes)
                ps.executeUpdate()
            }
            consumption
        }
    }

    override suspend fun listConsumptions(tenantId: String, projectId: String, commitmentId: String): List<BusinessCostCommitmentConsumption> {
        return transactionManager.inReadOnly(TenantContext(projectId)) { tx ->
            val sql = "SELECT * FROM business_cost_commitment_consumptions WHERE tenant_id = ? AND project_id = ? AND commitment_id = ? ORDER BY consumed_at DESC"
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, projectId)
                ps.setString(3, commitmentId)
                ps.executeQuery().use { rs ->
                    val list = mutableListOf<BusinessCostCommitmentConsumption>()
                    while (rs.next()) {
                        list.add(mapConsumption(rs))
                    }
                    list
                }
            }
        }
    }

    // --- Accruals ---

    override suspend fun createAccrual(accrual: BusinessCostAccrual): BusinessCostAccrual {
        return transactionManager.inTransaction(TenantContext(accrual.projectId)) { tx ->
            val sql = """
                INSERT INTO business_cost_accruals (
                    id, tenant_id, project_id, accrual_number, vendor_id, job_id,
                    cost_center_id, cost_category_id, description, accrual_amount,
                    reversed_amount, currency, accounting_period_id, accrual_date,
                    source_commitment_id, source_type, source_id, status,
                    ledger_posting_id, reversal_posting_id, created_by, reviewed_by,
                    approved_by, posted_by, reversed_by, created_at, updated_at, version
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, accrual.id)
                ps.setString(2, accrual.tenantId)
                ps.setString(3, accrual.projectId)
                ps.setString(4, accrual.accrualNumber)
                ps.setString(5, accrual.vendorId)
                ps.setString(6, accrual.jobId)
                ps.setString(7, accrual.costCenterId)
                ps.setString(8, accrual.costCategoryId)
                ps.setString(9, accrual.description)
                ps.setBigDecimal(10, accrual.accrualAmount)
                ps.setBigDecimal(11, accrual.reversedAmount)
                ps.setString(12, accrual.currency)
                ps.setString(13, accrual.accountingPeriodId)
                ps.setLong(14, accrual.accrualDate)
                ps.setString(15, accrual.sourceCommitmentId)
                ps.setString(16, accrual.sourceType.name)
                ps.setString(17, accrual.sourceId)
                ps.setString(18, accrual.status.name)
                ps.setString(19, accrual.ledgerPostingId)
                ps.setString(20, accrual.reversalPostingId)
                ps.setString(21, accrual.createdBy)
                ps.setString(22, accrual.reviewedBy)
                ps.setString(23, accrual.approvedBy)
                ps.setString(24, accrual.postedBy)
                ps.setString(25, accrual.reversedBy)
                ps.setLong(26, accrual.createdAt)
                ps.setLong(27, accrual.updatedAt)
                ps.setLong(28, accrual.version)
                ps.executeUpdate()
            }
            accrual
        }
    }

    override suspend fun findAccrualById(id: String, tenantId: String, projectId: String): BusinessCostAccrual? {
        return transactionManager.inReadOnly(TenantContext(projectId)) { tx ->
            val sql = "SELECT * FROM business_cost_accruals WHERE id = ? AND tenant_id = ? AND project_id = ?"
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, id)
                ps.setString(2, tenantId)
                ps.setString(3, projectId)
                ps.executeQuery().use { rs ->
                    if (rs.next()) mapAccrual(rs) else null
                }
            }
        }
    }

    override suspend fun findAccrualByNumber(accrualNumber: String, tenantId: String, projectId: String): BusinessCostAccrual? {
        return transactionManager.inReadOnly(TenantContext(projectId)) { tx ->
            val sql = "SELECT * FROM business_cost_accruals WHERE LOWER(accrual_number) = LOWER(?) AND tenant_id = ? AND project_id = ?"
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, accrualNumber)
                ps.setString(2, tenantId)
                ps.setString(3, projectId)
                ps.executeQuery().use { rs ->
                    if (rs.next()) mapAccrual(rs) else null
                }
            }
        }
    }

    override suspend fun updateAccrual(accrual: BusinessCostAccrual): BusinessCostAccrual {
        return transactionManager.inTransaction(TenantContext(accrual.projectId)) { tx ->
            val sql = """
                UPDATE business_cost_accruals SET
                    vendor_id = ?, job_id = ?, cost_center_id = ?, cost_category_id = ?,
                    description = ?, accrual_amount = ?, reversed_amount = ?, currency = ?,
                    accounting_period_id = ?, source_commitment_id = ?, status = ?,
                    ledger_posting_id = ?, reversal_posting_id = ?, reviewed_by = ?,
                    approved_by = ?, posted_by = ?, reversed_by = ?, updated_at = ?,
                    version = version + 1
                WHERE id = ? AND tenant_id = ? AND project_id = ?
            """.trimIndent()
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, accrual.vendorId)
                ps.setString(2, accrual.jobId)
                ps.setString(3, accrual.costCenterId)
                ps.setString(4, accrual.costCategoryId)
                ps.setString(5, accrual.description)
                ps.setBigDecimal(6, accrual.accrualAmount)
                ps.setBigDecimal(7, accrual.reversedAmount)
                ps.setString(8, accrual.currency)
                ps.setString(9, accrual.accountingPeriodId)
                ps.setString(10, accrual.sourceCommitmentId)
                ps.setString(11, accrual.status.name)
                ps.setString(12, accrual.ledgerPostingId)
                ps.setString(13, accrual.reversalPostingId)
                ps.setString(14, accrual.reviewedBy)
                ps.setString(15, accrual.approvedBy)
                ps.setString(16, accrual.postedBy)
                ps.setString(17, accrual.reversedBy)
                ps.setLong(18, System.currentTimeMillis())
                ps.setString(19, accrual.id)
                ps.setString(20, accrual.tenantId)
                ps.setString(21, accrual.projectId)
                val rows = ps.executeUpdate()
                if (rows == 0) throw NoSuchElementException("Accrual '${accrual.id}' not found for update.")
            }
            findAccrualById(accrual.id, accrual.tenantId, accrual.projectId)
                ?: accrual.copy(version = accrual.version + 1)
        }
    }

    override suspend fun listAccruals(tenantId: String, projectId: String, filter: BusinessCostAccrualFilter): List<BusinessCostAccrual> {
        return transactionManager.inReadOnly(TenantContext(projectId)) { tx ->
            val sb = StringBuilder("SELECT * FROM business_cost_accruals WHERE tenant_id = ? AND project_id = ?")
            val params = mutableListOf<Any>(tenantId, projectId)

            if (filter.status != null) {
                sb.append(" AND status = ?")
                params.add(filter.status.name)
            }
            if (filter.vendorId != null) {
                sb.append(" AND vendor_id = ?")
                params.add(filter.vendorId)
            }
            if (filter.jobId != null) {
                sb.append(" AND job_id = ?")
                params.add(filter.jobId)
            }
            if (filter.costCenterId != null) {
                sb.append(" AND cost_center_id = ?")
                params.add(filter.costCenterId)
            }
            if (filter.costCategoryId != null) {
                sb.append(" AND cost_category_id = ?")
                params.add(filter.costCategoryId)
            }
            if (filter.accountingPeriodId != null) {
                sb.append(" AND accounting_period_id = ?")
                params.add(filter.accountingPeriodId)
            }
            if (filter.sourceType != null) {
                sb.append(" AND source_type = ?")
                params.add(filter.sourceType.name)
            }
            sb.append(" ORDER BY accrual_date DESC")

            tx.connection.prepareStatement(sb.toString()).use { ps ->
                params.forEachIndexed { i, p -> ps.setObject(i + 1, p) }
                ps.executeQuery().use { rs ->
                    val list = mutableListOf<BusinessCostAccrual>()
                    while (rs.next()) {
                        list.add(mapAccrual(rs))
                    }
                    list
                }
            }
        }
    }

    // --- Reversals ---

    override suspend fun recordReversal(reversal: BusinessCostAccrualReversal): BusinessCostAccrualReversal {
        return transactionManager.inTransaction(TenantContext(reversal.projectId)) { tx ->
            val sql = """
                INSERT INTO business_cost_accrual_reversals (
                    id, tenant_id, project_id, accrual_id, reversal_amount,
                    currency, reversal_date, accounting_period_id, reason,
                    ledger_posting_id, created_by, created_at, idempotency_key
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, reversal.id)
                ps.setString(2, reversal.tenantId)
                ps.setString(3, reversal.projectId)
                ps.setString(4, reversal.accrualId)
                ps.setBigDecimal(5, reversal.reversalAmount)
                ps.setString(6, reversal.currency)
                ps.setLong(7, reversal.reversalDate)
                ps.setString(8, reversal.accountingPeriodId)
                ps.setString(9, reversal.reason)
                ps.setString(10, reversal.ledgerPostingId)
                ps.setString(11, reversal.createdBy)
                ps.setLong(12, reversal.createdAt)
                ps.setString(13, reversal.idempotencyKey)
                ps.executeUpdate()
            }
            reversal
        }
    }

    override suspend fun listReversals(tenantId: String, projectId: String, accrualId: String): List<BusinessCostAccrualReversal> {
        return transactionManager.inReadOnly(TenantContext(projectId)) { tx ->
            val sql = "SELECT * FROM business_cost_accrual_reversals WHERE tenant_id = ? AND project_id = ? AND accrual_id = ? ORDER BY reversal_date DESC"
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, projectId)
                ps.setString(3, accrualId)
                ps.executeQuery().use { rs ->
                    val list = mutableListOf<BusinessCostAccrualReversal>()
                    while (rs.next()) {
                        list.add(mapReversal(rs))
                    }
                    list
                }
            }
        }
    }

    // --- Audit Events ---

    override suspend fun recordAuditEvent(event: BusinessCostControlAuditEvent): BusinessCostControlAuditEvent {
        return transactionManager.inTransaction(TenantContext(event.projectId)) { tx ->
            val sql = """
                INSERT INTO business_cost_control_audit_events (
                    id, tenant_id, project_id, entity_type, entity_id, event_type,
                    actor_user_id, actor_role, timestamp, correlation_id, idempotency_key,
                    previous_state, new_state, amount, currency, reason, metadata
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, event.id)
                ps.setString(2, event.tenantId)
                ps.setString(3, event.projectId)
                ps.setString(4, event.entityType)
                ps.setString(5, event.entityId)
                ps.setString(6, event.eventType)
                ps.setString(7, event.actorUserId)
                ps.setString(8, event.actorRole)
                ps.setLong(9, event.timestamp)
                ps.setString(10, event.correlationId)
                ps.setString(11, event.idempotencyKey)
                ps.setString(12, event.previousState)
                ps.setString(13, event.newState)
                if (event.amount != null) ps.setBigDecimal(14, event.amount) else ps.setNull(14, java.sql.Types.NUMERIC)
                ps.setString(15, event.currency)
                ps.setString(16, event.reason)
                ps.setString(17, event.metadata)
                ps.executeUpdate()
            }
            event
        }
    }

    override suspend fun listAuditEvents(tenantId: String, projectId: String, entityId: String?, entityType: String?): List<BusinessCostControlAuditEvent> {
        return transactionManager.inReadOnly(TenantContext(projectId)) { tx ->
            val sb = StringBuilder("SELECT * FROM business_cost_control_audit_events WHERE tenant_id = ? AND project_id = ?")
            val params = mutableListOf<Any>(tenantId, projectId)

            if (entityId != null) {
                sb.append(" AND entity_id = ?")
                params.add(entityId)
            }
            if (entityType != null) {
                sb.append(" AND LOWER(entity_type) = LOWER(?)")
                params.add(entityType)
            }
            sb.append(" ORDER BY timestamp DESC")

            tx.connection.prepareStatement(sb.toString()).use { ps ->
                params.forEachIndexed { i, p -> ps.setObject(i + 1, p) }
                ps.executeQuery().use { rs ->
                    val list = mutableListOf<BusinessCostControlAuditEvent>()
                    while (rs.next()) {
                        list.add(mapAuditEvent(rs))
                    }
                    list
                }
            }
        }
    }

    // --- ResultSet Mappers ---

    private fun mapFinancialPeriod(rs: ResultSet): BusinessFinancialPeriod {
        return BusinessFinancialPeriod(
            id = rs.getString("id"),
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            periodCode = rs.getString("period_code"),
            periodName = rs.getString("period_name"),
            startDate = rs.getLong("start_date"),
            endDate = rs.getLong("end_date"),
            status = BusinessFinancialPeriodStatus.valueOf(rs.getString("status")),
            closedBy = rs.getString("closed_by"),
            closedAt = rs.getObject("closed_at") as? Long,
            closeReason = rs.getString("close_reason"),
            createdAt = rs.getLong("created_at"),
            createdBy = rs.getString("created_by"),
            updatedAt = rs.getLong("updated_at"),
            updatedBy = rs.getString("updated_by"),
            version = rs.getLong("version")
        )
    }

    private fun mapCommitment(rs: ResultSet): BusinessCostCommitment {
        return BusinessCostCommitment(
            id = rs.getString("id"),
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            commitmentNumber = rs.getString("commitment_number"),
            vendorId = rs.getString("vendor_id"),
            jobId = rs.getString("job_id"),
            costCenterId = rs.getString("cost_center_id"),
            costCategoryId = rs.getString("cost_category_id"),
            description = rs.getString("description"),
            committedAmount = rs.getBigDecimal("committed_amount").setScale(4, RoundingMode.HALF_UP),
            consumedAmount = rs.getBigDecimal("consumed_amount").setScale(4, RoundingMode.HALF_UP),
            remainingAmount = rs.getBigDecimal("remaining_amount").setScale(4, RoundingMode.HALF_UP),
            currency = rs.getString("currency"),
            commitmentDate = rs.getLong("commitment_date"),
            expectedDate = rs.getObject("expected_date") as? Long,
            periodId = rs.getString("period_id"),
            status = BusinessCostCommitmentStatus.valueOf(rs.getString("status")),
            sourceType = BusinessCostCommitmentSourceType.valueOf(rs.getString("source_type")),
            sourceId = rs.getString("source_id"),
            createdBy = rs.getString("created_by"),
            approvedBy = rs.getString("approved_by"),
            createdAt = rs.getLong("created_at"),
            updatedAt = rs.getLong("updated_at"),
            version = rs.getLong("version")
        )
    }

    private fun mapConsumption(rs: ResultSet): BusinessCostCommitmentConsumption {
        return BusinessCostCommitmentConsumption(
            id = rs.getString("id"),
            commitmentId = rs.getString("commitment_id"),
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            sourceType = BusinessCostCommitmentSourceType.valueOf(rs.getString("source_type")),
            sourceId = rs.getString("source_id"),
            amount = rs.getBigDecimal("amount").setScale(4, RoundingMode.HALF_UP),
            currency = rs.getString("currency"),
            consumedAt = rs.getLong("consumed_at"),
            createdBy = rs.getString("created_by"),
            idempotencyKey = rs.getString("idempotency_key"),
            notes = rs.getString("notes")
        )
    }

    private fun mapAccrual(rs: ResultSet): BusinessCostAccrual {
        return BusinessCostAccrual(
            id = rs.getString("id"),
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            accrualNumber = rs.getString("accrual_number"),
            vendorId = rs.getString("vendor_id"),
            jobId = rs.getString("job_id"),
            costCenterId = rs.getString("cost_center_id"),
            costCategoryId = rs.getString("cost_category_id"),
            description = rs.getString("description"),
            accrualAmount = rs.getBigDecimal("accrual_amount").setScale(4, RoundingMode.HALF_UP),
            reversedAmount = rs.getBigDecimal("reversed_amount").setScale(4, RoundingMode.HALF_UP),
            currency = rs.getString("currency"),
            accountingPeriodId = rs.getString("accounting_period_id"),
            accrualDate = rs.getLong("accrual_date"),
            sourceCommitmentId = rs.getString("source_commitment_id"),
            sourceType = BusinessCostCommitmentSourceType.valueOf(rs.getString("source_type")),
            sourceId = rs.getString("source_id"),
            status = BusinessCostAccrualStatus.valueOf(rs.getString("status")),
            ledgerPostingId = rs.getString("ledger_posting_id"),
            reversalPostingId = rs.getString("reversal_posting_id"),
            createdBy = rs.getString("created_by"),
            reviewedBy = rs.getString("reviewed_by"),
            approvedBy = rs.getString("approved_by"),
            postedBy = rs.getString("posted_by"),
            reversedBy = rs.getString("reversed_by"),
            createdAt = rs.getLong("created_at"),
            updatedAt = rs.getLong("updated_at"),
            version = rs.getLong("version")
        )
    }

    private fun mapReversal(rs: ResultSet): BusinessCostAccrualReversal {
        return BusinessCostAccrualReversal(
            id = rs.getString("id"),
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            accrualId = rs.getString("accrual_id"),
            reversalAmount = rs.getBigDecimal("reversal_amount").setScale(4, RoundingMode.HALF_UP),
            currency = rs.getString("currency"),
            reversalDate = rs.getLong("reversal_date"),
            accountingPeriodId = rs.getString("accounting_period_id"),
            reason = rs.getString("reason"),
            ledgerPostingId = rs.getString("ledger_posting_id"),
            createdBy = rs.getString("created_by"),
            createdAt = rs.getLong("created_at"),
            idempotencyKey = rs.getString("idempotency_key")
        )
    }

    private fun mapAuditEvent(rs: ResultSet): BusinessCostControlAuditEvent {
        return BusinessCostControlAuditEvent(
            id = rs.getString("id"),
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            entityType = rs.getString("entity_type"),
            entityId = rs.getString("entity_id"),
            eventType = rs.getString("event_type"),
            actorUserId = rs.getString("actor_user_id"),
            actorRole = rs.getString("actor_role"),
            timestamp = rs.getLong("timestamp"),
            correlationId = rs.getString("correlation_id"),
            idempotencyKey = rs.getString("idempotency_key"),
            previousState = rs.getString("previous_state"),
            newState = rs.getString("new_state"),
            amount = rs.getBigDecimal("amount")?.setScale(4, RoundingMode.HALF_UP),
            currency = rs.getString("currency"),
            reason = rs.getString("reason"),
            metadata = rs.getString("metadata")
        )
    }
}
