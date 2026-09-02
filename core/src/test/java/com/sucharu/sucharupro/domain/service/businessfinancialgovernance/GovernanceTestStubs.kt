package com.sucharu.sucharupro.domain.service.businessfinancialgovernance

import com.sucharu.sucharupro.data.datasource.businesscost.BusinessCostTrackingFilter
import com.sucharu.sucharupro.data.datasource.businesscostcontrol.BusinessCostAccrualFilter
import com.sucharu.sucharupro.data.datasource.businesscostcontrol.BusinessCostCommitmentFilter
import com.sucharu.sucharupro.data.datasource.businesscostcontrol.BusinessFinancialPeriodFilter
import com.sucharu.sucharupro.data.datasource.businessfinancialadjustment.AdjustmentFilter
import com.sucharu.sucharupro.data.datasource.businessfinancialadjustment.RefundFilter
import com.sucharu.sucharupro.data.datasource.businessfinancialadjustment.WriteOffFilter
import com.sucharu.sucharupro.data.datasource.businessledger.BusinessCostAllocationFilter
import com.sucharu.sucharupro.data.datasource.businessledger.BusinessLedgerPostingFilter
import com.sucharu.sucharupro.data.datasource.businessreconciliation.DiscrepancyFilter
import com.sucharu.sucharupro.data.datasource.businessreconciliation.ReconciliationRunFilter
import com.sucharu.sucharupro.data.repository.businesscostcontrol.BusinessCostControlRepository
import com.sucharu.sucharupro.data.repository.businessfinancialadjustment.BusinessFinancialAdjustmentRepository
import com.sucharu.sucharupro.data.repository.businessreconciliation.BusinessFinancialReconciliationRepository
import com.sucharu.sucharupro.domain.model.businesscost.*
import com.sucharu.sucharupro.domain.model.businesscostcontrol.*
import com.sucharu.sucharupro.domain.model.businessexpense.*
import com.sucharu.sucharupro.domain.model.businessfinancialadjustment.*
import com.sucharu.sucharupro.domain.model.businessledger.*
import com.sucharu.sucharupro.domain.model.businessreconciliation.*
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendorpayable.*
import com.sucharu.sucharupro.domain.repository.businesscost.BusinessCostManagementRepository
import com.sucharu.sucharupro.domain.repository.businessexpense.BusinessExpenseRepository
import com.sucharu.sucharupro.domain.repository.businessledger.BusinessLedgerRepository
import com.sucharu.sucharupro.domain.repository.vendorpayable.VendorPayableRepository
import java.math.BigDecimal

open class TestExpenseRepository(
    val expenses: MutableList<BusinessExpense> = mutableListOf()
) : BusinessExpenseRepository {
    override suspend fun createExpense(expense: BusinessExpense): DomainResult<BusinessExpense> {
        expenses.add(expense)
        return DomainResult.Success(expense)
    }
    override suspend fun updateExpense(expense: BusinessExpense): DomainResult<BusinessExpense> {
        val idx = expenses.indexOfFirst { it.expenseId == expense.expenseId }
        if (idx >= 0) expenses[idx] = expense else expenses.add(expense)
        return DomainResult.Success(expense)
    }
    override suspend fun getExpenseById(tenantId: String, projectId: String, expenseId: String): DomainResult<BusinessExpense?> =
        DomainResult.Success(expenses.find { it.tenantId == tenantId && it.projectId == projectId && it.expenseId == expenseId })
    override suspend fun getExpenseByNumber(tenantId: String, projectId: String, expenseNumber: String): DomainResult<BusinessExpense?> =
        DomainResult.Success(expenses.find { it.tenantId == tenantId && it.projectId == projectId && it.expenseNumber == expenseNumber })
    override suspend fun getExpenseByIdempotencyKey(tenantId: String, projectId: String, idempotencyKey: String): DomainResult<BusinessExpense?> =
        DomainResult.Success(expenses.find { it.tenantId == tenantId && it.projectId == projectId && it.idempotencyKey == idempotencyKey })
    override suspend fun listExpenses(
        tenantId: String, projectId: String, status: BusinessExpenseStatus?, categoryId: String?,
        vendorId: String?, jobId: String?, fromDate: Long?, toDate: Long?, limit: Int, offset: Int
    ): DomainResult<List<BusinessExpense>> {
        val filtered = expenses.filter {
            it.tenantId == tenantId && it.projectId == projectId &&
            (status == null || it.status == status) &&
            (categoryId == null || it.expenseCategoryId == categoryId) &&
            (vendorId == null || it.vendorId == vendorId) &&
            (jobId == null || it.jobId == jobId)
        }
        return DomainResult.Success(filtered.drop(offset).take(limit))
    }
    override suspend fun countExpenses(
        tenantId: String, projectId: String, status: BusinessExpenseStatus?, categoryId: String?,
        vendorId: String?, jobId: String?, fromDate: Long?, toDate: Long?
    ): DomainResult<Long> = DomainResult.Success(expenses.size.toLong())
    override suspend fun generateNextExpenseNumber(tenantId: String, projectId: String): String = "EXP-${System.currentTimeMillis()}"
    override suspend fun createCategory(category: BusinessExpenseCategory): DomainResult<BusinessExpenseCategory> = DomainResult.Success(category)
    override suspend fun updateCategory(category: BusinessExpenseCategory): DomainResult<BusinessExpenseCategory> = DomainResult.Success(category)
    override suspend fun getCategoryById(tenantId: String, projectId: String, categoryId: String): DomainResult<BusinessExpenseCategory?> = DomainResult.Success(null)
    override suspend fun getCategoryByCode(tenantId: String, projectId: String, code: String): DomainResult<BusinessExpenseCategory?> = DomainResult.Success(null)
    override suspend fun listCategories(tenantId: String, projectId: String, activeOnly: Boolean): DomainResult<List<BusinessExpenseCategory>> = DomainResult.Success(emptyList())
    override suspend fun recordAuditEvent(event: BusinessExpenseAuditEvent): DomainResult<Unit> = DomainResult.Success(Unit)
    override suspend fun getAuditEvents(tenantId: String, projectId: String, expenseId: String): DomainResult<List<BusinessExpenseAuditEvent>> = DomainResult.Success(emptyList())
}

open class TestCostControlRepository(
    val periods: MutableList<BusinessFinancialPeriod> = mutableListOf(),
    val commitments: MutableList<BusinessCostCommitment> = mutableListOf(),
    val accruals: MutableList<BusinessCostAccrual> = mutableListOf()
) : BusinessCostControlRepository {
    override suspend fun createFinancialPeriod(period: BusinessFinancialPeriod): BusinessFinancialPeriod {
        periods.add(period)
        return period
    }
    override suspend fun findFinancialPeriodById(id: String, tenantId: String, projectId: String): BusinessFinancialPeriod? =
        periods.find { it.id == id && it.tenantId == tenantId && it.projectId == projectId }
    override suspend fun findFinancialPeriodByCode(periodCode: String, tenantId: String, projectId: String): BusinessFinancialPeriod? =
        periods.find { it.periodCode == periodCode && it.tenantId == tenantId && it.projectId == projectId }
    override suspend fun updateFinancialPeriod(period: BusinessFinancialPeriod): BusinessFinancialPeriod {
        val idx = periods.indexOfFirst { it.id == period.id }
        if (idx >= 0) periods[idx] = period else periods.add(period)
        return period
    }
    override suspend fun listFinancialPeriods(tenantId: String, projectId: String, filter: BusinessFinancialPeriodFilter): List<BusinessFinancialPeriod> =
        periods.filter { it.tenantId == tenantId && it.projectId == projectId }

    override suspend fun createCommitment(commitment: BusinessCostCommitment): BusinessCostCommitment {
        commitments.add(commitment)
        return commitment
    }
    override suspend fun findCommitmentById(id: String, tenantId: String, projectId: String): BusinessCostCommitment? =
        commitments.find { it.id == id && it.tenantId == tenantId && it.projectId == projectId }
    override suspend fun findCommitmentByNumber(commitmentNumber: String, tenantId: String, projectId: String): BusinessCostCommitment? =
        commitments.find { it.commitmentNumber == commitmentNumber && it.tenantId == tenantId && it.projectId == projectId }
    override suspend fun updateCommitment(commitment: BusinessCostCommitment): BusinessCostCommitment {
        val idx = commitments.indexOfFirst { it.id == commitment.id }
        if (idx >= 0) commitments[idx] = commitment else commitments.add(commitment)
        return commitment
    }
    override suspend fun listCommitments(tenantId: String, projectId: String, filter: BusinessCostCommitmentFilter): List<BusinessCostCommitment> =
        commitments.filter { it.tenantId == tenantId && it.projectId == projectId && (filter.status == null || it.status == filter.status) }

    override suspend fun recordConsumption(consumption: BusinessCostCommitmentConsumption): BusinessCostCommitmentConsumption = consumption
    override suspend fun listConsumptions(tenantId: String, projectId: String, commitmentId: String): List<BusinessCostCommitmentConsumption> = emptyList()

    override suspend fun createAccrual(accrual: BusinessCostAccrual): BusinessCostAccrual {
        accruals.add(accrual)
        return accrual
    }
    override suspend fun findAccrualById(id: String, tenantId: String, projectId: String): BusinessCostAccrual? =
        accruals.find { it.id == id && it.tenantId == tenantId && it.projectId == projectId }
    override suspend fun findAccrualByNumber(accrualNumber: String, tenantId: String, projectId: String): BusinessCostAccrual? =
        accruals.find { it.accrualNumber == accrualNumber && it.tenantId == tenantId && it.projectId == projectId }
    override suspend fun updateAccrual(accrual: BusinessCostAccrual): BusinessCostAccrual {
        val idx = accruals.indexOfFirst { it.id == accrual.id }
        if (idx >= 0) accruals[idx] = accrual else accruals.add(accrual)
        return accrual
    }
    override suspend fun listAccruals(tenantId: String, projectId: String, filter: BusinessCostAccrualFilter): List<BusinessCostAccrual> =
        accruals.filter { it.tenantId == tenantId && it.projectId == projectId && (filter.accountingPeriodId == null || it.accountingPeriodId == filter.accountingPeriodId) }

    override suspend fun recordReversal(reversal: BusinessCostAccrualReversal): BusinessCostAccrualReversal = reversal
    override suspend fun listReversals(tenantId: String, projectId: String, accrualId: String): List<BusinessCostAccrualReversal> = emptyList()

    override suspend fun recordAuditEvent(event: BusinessCostControlAuditEvent): BusinessCostControlAuditEvent = event
    override suspend fun listAuditEvents(tenantId: String, projectId: String, entityId: String?, entityType: String?): List<BusinessCostControlAuditEvent> = emptyList()
}

open class TestPayableRepository(
    val payables: MutableList<VendorPayable> = mutableListOf()
) : VendorPayableRepository {
    override suspend fun createPayable(payable: VendorPayable): DomainResult<VendorPayable> {
        payables.add(payable)
        return DomainResult.Success(payable)
    }
    override suspend fun updatePayable(payable: VendorPayable): DomainResult<VendorPayable> {
        val idx = payables.indexOfFirst { it.payableId == payable.payableId }
        if (idx >= 0) payables[idx] = payable else payables.add(payable)
        return DomainResult.Success(payable)
    }
    override suspend fun getPayableById(tenantId: String, projectId: String, payableId: String): DomainResult<VendorPayable?> =
        DomainResult.Success(payables.find { it.tenantId == tenantId && it.projectId == projectId && it.payableId == payableId })
    override suspend fun getPayableByNumber(tenantId: String, projectId: String, payableNumber: String): DomainResult<VendorPayable?> =
        DomainResult.Success(payables.find { it.tenantId == tenantId && it.projectId == projectId && it.payableNumber == payableNumber })
    override suspend fun getPayableByIdempotencyKey(tenantId: String, projectId: String, idempotencyKey: String): DomainResult<VendorPayable?> =
        DomainResult.Success(payables.find { it.tenantId == tenantId && it.projectId == projectId && it.idempotencyKey == idempotencyKey })
    override suspend fun listPayables(
        tenantId: String, projectId: String, vendorId: String?, status: VendorPayableStatus?,
        jobId: String?, isOverdueOnly: Boolean, fromDate: Long?, toDate: Long?, limit: Int, offset: Int
    ): DomainResult<List<VendorPayable>> {
        val filtered = payables.filter {
            it.tenantId == tenantId && it.projectId == projectId &&
            (vendorId == null || it.vendorId == vendorId) &&
            (status == null || it.status == status) &&
            (jobId == null || it.jobId == jobId)
        }
        return DomainResult.Success(filtered.drop(offset).take(limit))
    }
    override suspend fun countPayables(
        tenantId: String, projectId: String, vendorId: String?, status: VendorPayableStatus?,
        jobId: String?, isOverdueOnly: Boolean, fromDate: Long?, toDate: Long?
    ): DomainResult<Long> = DomainResult.Success(payables.size.toLong())

    override suspend fun generateNextPayableNumber(tenantId: String, projectId: String): String = "VP-${System.currentTimeMillis()}"

    override suspend fun recordPaymentAllocation(allocation: VendorPayablePaymentAllocation): DomainResult<VendorPayablePaymentAllocation> = DomainResult.Success(allocation)
    override suspend fun getPaymentAllocations(tenantId: String, projectId: String, payableId: String): DomainResult<List<VendorPayablePaymentAllocation>> = DomainResult.Success(emptyList())

    override suspend fun recordAuditEvent(event: VendorPayableAuditEvent): DomainResult<Unit> = DomainResult.Success(Unit)
    override suspend fun getAuditEvents(tenantId: String, projectId: String, payableId: String): DomainResult<List<VendorPayableAuditEvent>> = DomainResult.Success(emptyList())
}

open class TestLedgerRepository : BusinessLedgerRepository {
    override suspend fun createPosting(posting: BusinessLedgerPosting): BusinessLedgerPosting = posting
    override suspend fun findPostingById(id: String, tenantId: String, projectId: String): BusinessLedgerPosting? = null
    override suspend fun findPostingByNumber(postingNumber: String, tenantId: String, projectId: String): BusinessLedgerPosting? = null
    override suspend fun findPostingByIdempotencyKey(key: String, tenantId: String, projectId: String): BusinessLedgerPosting? = null
    override suspend fun findPostingsBySource(sourceType: BusinessLedgerSourceType, sourceId: String, tenantId: String, projectId: String): List<BusinessLedgerPosting> = emptyList()
    override suspend fun findPostingBySourceAndType(sourceType: BusinessLedgerSourceType, sourceId: String, postingType: BusinessLedgerPostingType, tenantId: String, projectId: String): BusinessLedgerPosting? = null
    override suspend fun listPostings(tenantId: String, projectId: String, filter: BusinessLedgerPostingFilter): List<BusinessLedgerPosting> = emptyList()
    override suspend fun countPostings(tenantId: String, projectId: String, filter: BusinessLedgerPostingFilter): Long = 0L
    override suspend fun markPostingReversed(id: String, reversalReason: String, reversedBy: String, reversedAt: Long, reversalPostingId: String): Boolean = true

    override suspend fun createCostAllocation(allocation: BusinessCostAllocation): BusinessCostAllocation = allocation
    override suspend fun findCostAllocationById(id: String, tenantId: String, projectId: String): BusinessCostAllocation? = null
    override suspend fun findCostAllocationByIdempotencyKey(key: String, tenantId: String, projectId: String): BusinessCostAllocation? = null
    override suspend fun listCostAllocations(tenantId: String, projectId: String, filter: BusinessCostAllocationFilter): List<BusinessCostAllocation> = emptyList()
    override suspend fun markCostAllocationReversed(id: String, reversalReason: String, reversedBy: String, reversedAt: Long): Boolean = true

    override suspend fun recordAuditEvent(event: BusinessLedgerAuditEvent) {}
    override suspend fun listAuditEvents(tenantId: String, projectId: String, sourceId: String?, postingId: String?, allocationId: String?): List<BusinessLedgerAuditEvent> = emptyList()

    override suspend fun calculateBalanceSummary(tenantId: String, projectId: String, asOfTimestamp: Long): BusinessLedgerBalanceSummary =
        BusinessLedgerBalanceSummary(tenantId, projectId)
    override suspend fun calculatePeriodSummary(tenantId: String, projectId: String, fromDate: Long, toDate: Long): BusinessLedgerPeriodSummary =
        BusinessLedgerPeriodSummary(tenantId, projectId, fromDate, toDate, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0)
}

open class TestCostManagementRepository : BusinessCostManagementRepository {
    override suspend fun createCostCenter(center: BusinessCostCenter): BusinessCostCenter = center
    override suspend fun findCostCenterById(id: String, tenantId: String, projectId: String): BusinessCostCenter? = null
    override suspend fun findCostCenterByCode(code: String, tenantId: String, projectId: String): BusinessCostCenter? = null
    override suspend fun updateCostCenter(center: BusinessCostCenter): BusinessCostCenter = center
    override suspend fun listCostCenters(tenantId: String, projectId: String, activeOnly: Boolean?): List<BusinessCostCenter> = emptyList()
    override suspend fun getCostCenterChildren(parentCostCenterId: String, tenantId: String, projectId: String): List<BusinessCostCenter> = emptyList()

    override suspend fun createCostCategory(category: BusinessCostCategory): BusinessCostCategory = category
    override suspend fun findCostCategoryById(id: String, tenantId: String, projectId: String): BusinessCostCategory? = null
    override suspend fun findCostCategoryByCode(code: String, tenantId: String, projectId: String): BusinessCostCategory? = null
    override suspend fun updateCostCategory(category: BusinessCostCategory): BusinessCostCategory = category
    override suspend fun listCostCategories(tenantId: String, projectId: String, activeOnly: Boolean?): List<BusinessCostCategory> = emptyList()
    override suspend fun getCostCategoryChildren(parentCategoryId: String, tenantId: String, projectId: String): List<BusinessCostCategory> = emptyList()

    override suspend fun createCostTracking(tracking: BusinessCostTracking): BusinessCostTracking = tracking
    override suspend fun findCostTrackingById(id: String, tenantId: String, projectId: String): BusinessCostTracking? = null
    override suspend fun findCostTrackingBySource(sourceType: BusinessCostTrackingSourceType, sourceId: String, tenantId: String, projectId: String): List<BusinessCostTracking> = emptyList()
    override suspend fun updateCostTracking(tracking: BusinessCostTracking): BusinessCostTracking = tracking
    override suspend fun listCostTracking(tenantId: String, projectId: String, filter: BusinessCostTrackingFilter): List<BusinessCostTracking> = emptyList()

    override suspend fun recordAuditEvent(event: BusinessCostClassificationAuditEvent) {}
    override suspend fun listAuditEvents(tenantId: String, projectId: String, trackingId: String?): List<BusinessCostClassificationAuditEvent> = emptyList()

    override suspend fun calculateCostCenterSummary(costCenterId: String, tenantId: String, projectId: String): BusinessCostCenterSummary =
        BusinessCostCenterSummary(costCenterId = costCenterId, code = "CC-01", name = "Center", parentCostCenterId = null, isActive = true)
    override suspend fun calculateCostCategorySummary(categoryId: String, tenantId: String, projectId: String): BusinessCostCategorySummary =
        BusinessCostCategorySummary(categoryId = categoryId, code = "CAT-01", name = "Category", parentCategoryId = null, isActive = true, isSystemDefined = false)
    override suspend fun calculateJobCostDetail(jobId: String, tenantId: String, projectId: String): BusinessJobCostDetailSummary =
        BusinessJobCostDetailSummary(jobId = jobId)
    override suspend fun calculateTrackingSummary(tenantId: String, projectId: String): BusinessCostTrackingSummary =
        BusinessCostTrackingSummary()
}

open class TestReconciliationRepository : BusinessFinancialReconciliationRepository {
    override suspend fun createRun(run: BusinessFinancialReconciliationRun): BusinessFinancialReconciliationRun = run
    override suspend fun updateRun(run: BusinessFinancialReconciliationRun): BusinessFinancialReconciliationRun = run
    override suspend fun findRunById(id: String, tenantId: String, projectId: String): BusinessFinancialReconciliationRun? = null
    override suspend fun findRunByNumber(runNumber: String, tenantId: String, projectId: String): BusinessFinancialReconciliationRun? = null
    override suspend fun listRuns(tenantId: String, projectId: String, filter: ReconciliationRunFilter): List<BusinessFinancialReconciliationRun> = emptyList()
    override suspend fun countRuns(tenantId: String, projectId: String, filter: ReconciliationRunFilter): Long = 0L

    override suspend fun createDiscrepancy(discrepancy: BusinessFinancialReconciliationDiscrepancy): BusinessFinancialReconciliationDiscrepancy = discrepancy
    override suspend fun createDiscrepanciesBatch(discrepancies: List<BusinessFinancialReconciliationDiscrepancy>): List<BusinessFinancialReconciliationDiscrepancy> = discrepancies
    override suspend fun updateDiscrepancy(discrepancy: BusinessFinancialReconciliationDiscrepancy): BusinessFinancialReconciliationDiscrepancy = discrepancy
    override suspend fun findDiscrepancyById(id: String, tenantId: String, projectId: String): BusinessFinancialReconciliationDiscrepancy? = null
    override suspend fun listDiscrepancies(tenantId: String, projectId: String, filter: DiscrepancyFilter): List<BusinessFinancialReconciliationDiscrepancy> = emptyList()
    override suspend fun countDiscrepancies(tenantId: String, projectId: String, filter: DiscrepancyFilter): Long = 0L

    override suspend fun saveSnapshot(snapshot: BusinessFinancialReconciliationSnapshot): BusinessFinancialReconciliationSnapshot = snapshot
    override suspend fun findSnapshotByRunId(runId: String, tenantId: String, projectId: String): BusinessFinancialReconciliationSnapshot? = null

    override suspend fun recordAuditEvent(event: BusinessFinancialReconciliationAuditEvent): BusinessFinancialReconciliationAuditEvent = event
    override suspend fun listAuditEvents(tenantId: String, projectId: String, runId: String?, discrepancyId: String?): List<BusinessFinancialReconciliationAuditEvent> = emptyList()
}

open class TestAdjustmentRepository : BusinessFinancialAdjustmentRepository {
    override suspend fun saveAdjustment(adjustment: BusinessFinancialAdjustment): BusinessFinancialAdjustment = adjustment
    override suspend fun updateAdjustment(adjustment: BusinessFinancialAdjustment): BusinessFinancialAdjustment = adjustment
    override suspend fun findAdjustmentById(id: String, tenantId: String, projectId: String): BusinessFinancialAdjustment? = null
    override suspend fun findAdjustmentByNumber(number: String, tenantId: String, projectId: String): BusinessFinancialAdjustment? = null
    override suspend fun listAdjustments(tenantId: String, projectId: String, filter: AdjustmentFilter): List<BusinessFinancialAdjustment> = emptyList()

    override suspend fun saveRefund(refund: BusinessFinancialRefund): BusinessFinancialRefund = refund
    override suspend fun updateRefund(refund: BusinessFinancialRefund): BusinessFinancialRefund = refund
    override suspend fun findRefundById(id: String, tenantId: String, projectId: String): BusinessFinancialRefund? = null
    override suspend fun findRefundByNumber(number: String, tenantId: String, projectId: String): BusinessFinancialRefund? = null
    override suspend fun listRefunds(tenantId: String, projectId: String, filter: RefundFilter): List<BusinessFinancialRefund> = emptyList()

    override suspend fun saveWriteOff(writeOff: BusinessFinancialWriteOff): BusinessFinancialWriteOff = writeOff
    override suspend fun updateWriteOff(writeOff: BusinessFinancialWriteOff): BusinessFinancialWriteOff = writeOff
    override suspend fun findWriteOffById(id: String, tenantId: String, projectId: String): BusinessFinancialWriteOff? = null
    override suspend fun findWriteOffByNumber(number: String, tenantId: String, projectId: String): BusinessFinancialWriteOff? = null
    override suspend fun listWriteOffs(tenantId: String, projectId: String, filter: WriteOffFilter): List<BusinessFinancialWriteOff> = emptyList()

    override suspend fun savePosting(posting: BusinessFinancialAdjustmentPosting): BusinessFinancialAdjustmentPosting = posting
    override suspend fun listPostingsByAdjustmentId(adjustmentId: String, tenantId: String, projectId: String): List<BusinessFinancialAdjustmentPosting> = emptyList()

    override suspend fun recordAuditEvent(event: BusinessFinancialAdjustmentAuditEvent): BusinessFinancialAdjustmentAuditEvent = event
    override suspend fun listAuditEvents(tenantId: String, projectId: String, entityId: String?, entityType: String?): List<BusinessFinancialAdjustmentAuditEvent> = emptyList()
}
