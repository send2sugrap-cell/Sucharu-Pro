package com.sucharu.sucharupro.data.datasource.businesscostcontrol

import com.sucharu.sucharupro.domain.model.businesscostcontrol.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class FakeBusinessCostControlDataSource : BusinessCostControlDataSource {

    private val mutex = Mutex()

    private val periods = mutableListOf<BusinessFinancialPeriod>()
    private val commitments = mutableListOf<BusinessCostCommitment>()
    private val consumptions = mutableListOf<BusinessCostCommitmentConsumption>()
    private val accruals = mutableListOf<BusinessCostAccrual>()
    private val reversals = mutableListOf<BusinessCostAccrualReversal>()
    private val auditEvents = mutableListOf<BusinessCostControlAuditEvent>()

    // Financial Periods
    override suspend fun createFinancialPeriod(period: BusinessFinancialPeriod): BusinessFinancialPeriod = mutex.withLock {
        periods.add(period)
        period
    }

    override suspend fun findFinancialPeriodById(id: String, tenantId: String, projectId: String): BusinessFinancialPeriod? = mutex.withLock {
        periods.find { it.id == id && it.tenantId == tenantId && it.projectId == projectId }
    }

    override suspend fun findFinancialPeriodByCode(periodCode: String, tenantId: String, projectId: String): BusinessFinancialPeriod? = mutex.withLock {
        periods.find { it.periodCode.equals(periodCode, ignoreCase = true) && it.tenantId == tenantId && it.projectId == projectId }
    }

    override suspend fun updateFinancialPeriod(period: BusinessFinancialPeriod): BusinessFinancialPeriod = mutex.withLock {
        val idx = periods.indexOfFirst { it.id == period.id && it.tenantId == period.tenantId && it.projectId == period.projectId }
        if (idx == -1) throw NoSuchElementException("Financial period '${period.id}' not found.")
        val updated = period.copy(version = periods[idx].version + 1, updatedAt = System.currentTimeMillis())
        periods[idx] = updated
        updated
    }

    override suspend fun listFinancialPeriods(tenantId: String, projectId: String, filter: BusinessFinancialPeriodFilter): List<BusinessFinancialPeriod> = mutex.withLock {
        periods.filter { p ->
            p.tenantId == tenantId && p.projectId == projectId &&
            (filter.status == null || p.status == filter.status)
        }.sortedByDescending { it.startDate }
    }

    // Commitments
    override suspend fun createCommitment(commitment: BusinessCostCommitment): BusinessCostCommitment = mutex.withLock {
        commitments.add(commitment)
        commitment
    }

    override suspend fun findCommitmentById(id: String, tenantId: String, projectId: String): BusinessCostCommitment? = mutex.withLock {
        commitments.find { it.id == id && it.tenantId == tenantId && it.projectId == projectId }
    }

    override suspend fun findCommitmentByNumber(commitmentNumber: String, tenantId: String, projectId: String): BusinessCostCommitment? = mutex.withLock {
        commitments.find { it.commitmentNumber.equals(commitmentNumber, ignoreCase = true) && it.tenantId == tenantId && it.projectId == projectId }
    }

    override suspend fun updateCommitment(commitment: BusinessCostCommitment): BusinessCostCommitment = mutex.withLock {
        val idx = commitments.indexOfFirst { it.id == commitment.id && it.tenantId == commitment.tenantId && it.projectId == commitment.projectId }
        if (idx == -1) throw NoSuchElementException("Commitment '${commitment.id}' not found.")
        val updated = commitment.copy(version = commitments[idx].version + 1, updatedAt = System.currentTimeMillis())
        commitments[idx] = updated
        updated
    }

    override suspend fun listCommitments(tenantId: String, projectId: String, filter: BusinessCostCommitmentFilter): List<BusinessCostCommitment> = mutex.withLock {
        commitments.filter { c ->
            c.tenantId == tenantId && c.projectId == projectId &&
            (filter.status == null || c.status == filter.status) &&
            (filter.vendorId == null || c.vendorId == filter.vendorId) &&
            (filter.jobId == null || c.jobId == filter.jobId) &&
            (filter.costCenterId == null || c.costCenterId == filter.costCenterId) &&
            (filter.costCategoryId == null || c.costCategoryId == filter.costCategoryId) &&
            (filter.periodId == null || c.periodId == filter.periodId) &&
            (filter.sourceType == null || c.sourceType == filter.sourceType)
        }.sortedByDescending { it.commitmentDate }
    }

    // Commitment Consumptions
    override suspend fun recordConsumption(consumption: BusinessCostCommitmentConsumption): BusinessCostCommitmentConsumption = mutex.withLock {
        consumptions.add(consumption)
        consumption
    }

    override suspend fun listConsumptions(tenantId: String, projectId: String, commitmentId: String): List<BusinessCostCommitmentConsumption> = mutex.withLock {
        consumptions.filter { it.tenantId == tenantId && it.projectId == projectId && it.commitmentId == commitmentId }
            .sortedByDescending { it.consumedAt }
    }

    // Accruals
    override suspend fun createAccrual(accrual: BusinessCostAccrual): BusinessCostAccrual = mutex.withLock {
        accruals.add(accrual)
        accrual
    }

    override suspend fun findAccrualById(id: String, tenantId: String, projectId: String): BusinessCostAccrual? = mutex.withLock {
        accruals.find { it.id == id && it.tenantId == tenantId && it.projectId == projectId }
    }

    override suspend fun findAccrualByNumber(accrualNumber: String, tenantId: String, projectId: String): BusinessCostAccrual? = mutex.withLock {
        accruals.find { it.accrualNumber.equals(accrualNumber, ignoreCase = true) && it.tenantId == tenantId && it.projectId == projectId }
    }

    override suspend fun updateAccrual(accrual: BusinessCostAccrual): BusinessCostAccrual = mutex.withLock {
        val idx = accruals.indexOfFirst { it.id == accrual.id && it.tenantId == accrual.tenantId && it.projectId == accrual.projectId }
        if (idx == -1) throw NoSuchElementException("Accrual '${accrual.id}' not found.")
        val updated = accrual.copy(version = accruals[idx].version + 1, updatedAt = System.currentTimeMillis())
        accruals[idx] = updated
        updated
    }

    override suspend fun listAccruals(tenantId: String, projectId: String, filter: BusinessCostAccrualFilter): List<BusinessCostAccrual> = mutex.withLock {
        accruals.filter { a ->
            a.tenantId == tenantId && a.projectId == projectId &&
            (filter.status == null || a.status == filter.status) &&
            (filter.vendorId == null || a.vendorId == filter.vendorId) &&
            (filter.jobId == null || a.jobId == filter.jobId) &&
            (filter.costCenterId == null || a.costCenterId == filter.costCenterId) &&
            (filter.costCategoryId == null || a.costCategoryId == filter.costCategoryId) &&
            (filter.accountingPeriodId == null || a.accountingPeriodId == filter.accountingPeriodId) &&
            (filter.sourceType == null || a.sourceType == filter.sourceType)
        }.sortedByDescending { it.accrualDate }
    }

    // Accrual Reversals
    override suspend fun recordReversal(reversal: BusinessCostAccrualReversal): BusinessCostAccrualReversal = mutex.withLock {
        reversals.add(reversal)
        reversal
    }

    override suspend fun listReversals(tenantId: String, projectId: String, accrualId: String): List<BusinessCostAccrualReversal> = mutex.withLock {
        reversals.filter { it.tenantId == tenantId && it.projectId == projectId && it.accrualId == accrualId }
            .sortedByDescending { it.reversalDate }
    }

    // Audit Events
    override suspend fun recordAuditEvent(event: BusinessCostControlAuditEvent): BusinessCostControlAuditEvent = mutex.withLock {
        auditEvents.add(event)
        event
    }

    override suspend fun listAuditEvents(tenantId: String, projectId: String, entityId: String?, entityType: String?): List<BusinessCostControlAuditEvent> = mutex.withLock {
        auditEvents.filter { e ->
            e.tenantId == tenantId && e.projectId == projectId &&
            (entityId == null || e.entityId == entityId) &&
            (entityType == null || e.entityType.equals(entityType, ignoreCase = true))
        }.sortedByDescending { it.timestamp }
    }
}
