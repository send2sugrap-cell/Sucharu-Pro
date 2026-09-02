package com.sucharu.sucharupro.data.datasource.businesscostcontrol

import com.sucharu.sucharupro.domain.model.businesscostcontrol.*

data class BusinessCostCommitmentFilter(
    val status: BusinessCostCommitmentStatus? = null,
    val vendorId: String? = null,
    val jobId: String? = null,
    val costCenterId: String? = null,
    val costCategoryId: String? = null,
    val periodId: String? = null,
    val sourceType: BusinessCostCommitmentSourceType? = null
)

data class BusinessCostAccrualFilter(
    val status: BusinessCostAccrualStatus? = null,
    val vendorId: String? = null,
    val jobId: String? = null,
    val costCenterId: String? = null,
    val costCategoryId: String? = null,
    val accountingPeriodId: String? = null,
    val sourceType: BusinessCostCommitmentSourceType? = null
)

data class BusinessFinancialPeriodFilter(
    val status: BusinessFinancialPeriodStatus? = null
)

interface BusinessCostControlDataSource {
    // Financial Periods
    suspend fun createFinancialPeriod(period: BusinessFinancialPeriod): BusinessFinancialPeriod
    suspend fun findFinancialPeriodById(id: String, tenantId: String, projectId: String): BusinessFinancialPeriod?
    suspend fun findFinancialPeriodByCode(periodCode: String, tenantId: String, projectId: String): BusinessFinancialPeriod?
    suspend fun updateFinancialPeriod(period: BusinessFinancialPeriod): BusinessFinancialPeriod
    suspend fun listFinancialPeriods(tenantId: String, projectId: String, filter: BusinessFinancialPeriodFilter): List<BusinessFinancialPeriod>

    // Commitments
    suspend fun createCommitment(commitment: BusinessCostCommitment): BusinessCostCommitment
    suspend fun findCommitmentById(id: String, tenantId: String, projectId: String): BusinessCostCommitment?
    suspend fun findCommitmentByNumber(commitmentNumber: String, tenantId: String, projectId: String): BusinessCostCommitment?
    suspend fun updateCommitment(commitment: BusinessCostCommitment): BusinessCostCommitment
    suspend fun listCommitments(tenantId: String, projectId: String, filter: BusinessCostCommitmentFilter): List<BusinessCostCommitment>

    // Commitment Consumptions
    suspend fun recordConsumption(consumption: BusinessCostCommitmentConsumption): BusinessCostCommitmentConsumption
    suspend fun listConsumptions(tenantId: String, projectId: String, commitmentId: String): List<BusinessCostCommitmentConsumption>

    // Accruals
    suspend fun createAccrual(accrual: BusinessCostAccrual): BusinessCostAccrual
    suspend fun findAccrualById(id: String, tenantId: String, projectId: String): BusinessCostAccrual?
    suspend fun findAccrualByNumber(accrualNumber: String, tenantId: String, projectId: String): BusinessCostAccrual?
    suspend fun updateAccrual(accrual: BusinessCostAccrual): BusinessCostAccrual
    suspend fun listAccruals(tenantId: String, projectId: String, filter: BusinessCostAccrualFilter): List<BusinessCostAccrual>

    // Accrual Reversals
    suspend fun recordReversal(reversal: BusinessCostAccrualReversal): BusinessCostAccrualReversal
    suspend fun listReversals(tenantId: String, projectId: String, accrualId: String): List<BusinessCostAccrualReversal>

    // Audit Events
    suspend fun recordAuditEvent(event: BusinessCostControlAuditEvent): BusinessCostControlAuditEvent
    suspend fun listAuditEvents(tenantId: String, projectId: String, entityId: String? = null, entityType: String? = null): List<BusinessCostControlAuditEvent>
}
