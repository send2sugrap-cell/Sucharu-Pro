package com.sucharu.sucharupro.domain.service.businesscostcontrol

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.datasource.businesscostcontrol.*
import com.sucharu.sucharupro.domain.model.businesscostcontrol.*
import com.sucharu.sucharupro.domain.model.common.DomainResult
import java.math.BigDecimal

// --- Command Dataclasses ---

data class CreateCostCommitmentCommand(
    val commitmentNumber: String? = null,
    val vendorId: String? = null,
    val jobId: String? = null,
    val costCenterId: String? = null,
    val costCategoryId: String,
    val description: String,
    val committedAmount: BigDecimal,
    val currency: String = "BDT",
    val commitmentDate: Long = System.currentTimeMillis(),
    val expectedDate: Long? = null,
    val periodId: String? = null,
    val sourceType: BusinessCostCommitmentSourceType = BusinessCostCommitmentSourceType.MANUAL,
    val sourceId: String? = null,
    val idempotencyKey: String? = null,
    val correlationId: String? = null
)

data class UpdateCostCommitmentCommand(
    val commitmentId: String,
    val vendorId: String? = null,
    val jobId: String? = null,
    val costCenterId: String? = null,
    val costCategoryId: String? = null,
    val description: String? = null,
    val committedAmount: BigDecimal? = null,
    val currency: String? = null,
    val expectedDate: Long? = null,
    val periodId: String? = null
)

data class ConsumeCostCommitmentCommand(
    val commitmentId: String,
    val amount: BigDecimal,
    val sourceType: BusinessCostCommitmentSourceType = BusinessCostCommitmentSourceType.MANUAL,
    val sourceId: String,
    val currency: String = "BDT",
    val notes: String? = null,
    val idempotencyKey: String? = null,
    val correlationId: String? = null
)

data class CreateCostAccrualCommand(
    val accrualNumber: String? = null,
    val vendorId: String? = null,
    val jobId: String? = null,
    val costCenterId: String? = null,
    val costCategoryId: String,
    val description: String,
    val accrualAmount: BigDecimal,
    val currency: String = "BDT",
    val accountingPeriodId: String,
    val accrualDate: Long = System.currentTimeMillis(),
    val sourceCommitmentId: String? = null,
    val sourceType: BusinessCostCommitmentSourceType = BusinessCostCommitmentSourceType.MANUAL,
    val sourceId: String? = null,
    val idempotencyKey: String? = null,
    val correlationId: String? = null
)

data class ReverseCostAccrualCommand(
    val accrualId: String,
    val reversalAmount: BigDecimal,
    val reason: String,
    val accountingPeriodId: String? = null,
    val currency: String = "BDT",
    val idempotencyKey: String? = null,
    val correlationId: String? = null
)

data class CreateFinancialPeriodCommand(
    val periodCode: String,
    val periodName: String,
    val startDate: Long,
    val endDate: Long
)

// --- Service Interfaces ---

interface BusinessCostCommitmentService {
    suspend fun createCommitment(principal: AuthenticatedPrincipal, command: CreateCostCommitmentCommand): DomainResult<BusinessCostCommitment>
    suspend fun updateCommitment(principal: AuthenticatedPrincipal, command: UpdateCostCommitmentCommand): DomainResult<BusinessCostCommitment>
    suspend fun submitCommitment(principal: AuthenticatedPrincipal, commitmentId: String): DomainResult<BusinessCostCommitment>
    suspend fun approveCommitment(principal: AuthenticatedPrincipal, commitmentId: String): DomainResult<BusinessCostCommitment>
    suspend fun activateCommitment(principal: AuthenticatedPrincipal, commitmentId: String): DomainResult<BusinessCostCommitment>
    suspend fun consumeCommitment(principal: AuthenticatedPrincipal, command: ConsumeCostCommitmentCommand): DomainResult<BusinessCostCommitmentConsumption>
    suspend fun cancelCommitment(principal: AuthenticatedPrincipal, commitmentId: String, reason: String): DomainResult<BusinessCostCommitment>
    suspend fun closeCommitment(principal: AuthenticatedPrincipal, commitmentId: String, reason: String): DomainResult<BusinessCostCommitment>
    suspend fun getCommitmentById(principal: AuthenticatedPrincipal, commitmentId: String): DomainResult<BusinessCostCommitment>
    suspend fun listCommitments(principal: AuthenticatedPrincipal, filter: BusinessCostCommitmentFilter = BusinessCostCommitmentFilter()): DomainResult<List<BusinessCostCommitment>>
    suspend fun listConsumptions(principal: AuthenticatedPrincipal, commitmentId: String): DomainResult<List<BusinessCostCommitmentConsumption>>
}

interface BusinessCostAccrualService {
    suspend fun createAccrual(principal: AuthenticatedPrincipal, command: CreateCostAccrualCommand): DomainResult<BusinessCostAccrual>
    suspend fun reviewAccrual(principal: AuthenticatedPrincipal, accrualId: String): DomainResult<BusinessCostAccrual>
    suspend fun approveAccrual(principal: AuthenticatedPrincipal, accrualId: String): DomainResult<BusinessCostAccrual>
    suspend fun postAccrual(principal: AuthenticatedPrincipal, accrualId: String, correlationId: String? = null, idempotencyKey: String? = null): DomainResult<BusinessCostAccrual>
    suspend fun reverseAccrual(principal: AuthenticatedPrincipal, command: ReverseCostAccrualCommand): DomainResult<BusinessCostAccrualReversal>
    suspend fun cancelAccrual(principal: AuthenticatedPrincipal, accrualId: String, reason: String): DomainResult<BusinessCostAccrual>
    suspend fun getAccrualById(principal: AuthenticatedPrincipal, accrualId: String): DomainResult<BusinessCostAccrual>
    suspend fun listAccruals(principal: AuthenticatedPrincipal, filter: BusinessCostAccrualFilter = BusinessCostAccrualFilter()): DomainResult<List<BusinessCostAccrual>>
    suspend fun listReversals(principal: AuthenticatedPrincipal, accrualId: String): DomainResult<List<BusinessCostAccrualReversal>>
}

interface BusinessFinancialPeriodService {
    suspend fun createFinancialPeriod(principal: AuthenticatedPrincipal, command: CreateFinancialPeriodCommand): DomainResult<BusinessFinancialPeriod>
    suspend fun softCloseFinancialPeriod(principal: AuthenticatedPrincipal, periodId: String, reason: String? = null): DomainResult<BusinessFinancialPeriod>
    suspend fun closeFinancialPeriod(principal: AuthenticatedPrincipal, periodId: String, reason: String): DomainResult<BusinessFinancialPeriod>
    suspend fun reopenFinancialPeriod(principal: AuthenticatedPrincipal, periodId: String, reason: String): DomainResult<BusinessFinancialPeriod>
    suspend fun getFinancialPeriodById(principal: AuthenticatedPrincipal, periodId: String): DomainResult<BusinessFinancialPeriod>
    suspend fun listFinancialPeriods(principal: AuthenticatedPrincipal, filter: BusinessFinancialPeriodFilter = BusinessFinancialPeriodFilter()): DomainResult<List<BusinessFinancialPeriod>>
}

interface BusinessCostControlService :
    BusinessCostCommitmentService,
    BusinessCostAccrualService,
    BusinessFinancialPeriodService {

    suspend fun getControlDashboard(principal: AuthenticatedPrincipal): DomainResult<BusinessCostControlDashboard>
    suspend fun getReconciliationSummary(principal: AuthenticatedPrincipal, vendorId: String? = null, jobId: String? = null): DomainResult<BusinessCostReconciliationSummary>
    suspend fun listExceptions(principal: AuthenticatedPrincipal): DomainResult<List<BusinessCostControlException>>
    suspend fun getPeriodEndReport(principal: AuthenticatedPrincipal, periodId: String): DomainResult<BusinessCostPeriodEndReport>
    suspend fun listAuditEvents(principal: AuthenticatedPrincipal, entityId: String? = null, entityType: String? = null): DomainResult<List<BusinessCostControlAuditEvent>>
}
