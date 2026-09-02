package com.sucharu.sucharupro.businessledger

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.datasource.FakeCustomerFinancialAccountDataSource
import com.sucharu.sucharupro.data.datasource.businessexpense.FakeBusinessExpenseDataSource
import com.sucharu.sucharupro.data.datasource.businessledger.FakeBusinessLedgerDataSource
import com.sucharu.sucharupro.data.datasource.vendorpayable.FakeVendorPayableDataSource
import com.sucharu.sucharupro.data.repository.CustomerFinancialAccountRepositoryImpl
import com.sucharu.sucharupro.data.repository.businessexpense.BusinessExpenseRepositoryImpl
import com.sucharu.sucharupro.data.repository.businessledger.BusinessLedgerRepositoryImpl
import com.sucharu.sucharupro.data.repository.vendorpayable.VendorPayableRepositoryImpl
import com.sucharu.sucharupro.domain.model.businessexpense.BusinessExpense
import com.sucharu.sucharupro.domain.model.businessexpense.BusinessExpenseStatus
import com.sucharu.sucharupro.domain.model.businessledger.BusinessLedgerAccountCategory
import com.sucharu.sucharupro.domain.model.businessledger.BusinessLedgerSourceType
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccount
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccountStatus
import com.sucharu.sucharupro.domain.model.vendorpayable.VendorPayable
import com.sucharu.sucharupro.domain.model.vendorpayable.VendorPayableStatus
import com.sucharu.sucharupro.domain.service.businessledger.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class BusinessLedgerConsistencyTest {

    private lateinit var ledgerDataSource: FakeBusinessLedgerDataSource
    private lateinit var ledgerRepository: BusinessLedgerRepositoryImpl
    private lateinit var expenseDataSource: FakeBusinessExpenseDataSource
    private lateinit var expenseRepository: BusinessExpenseRepositoryImpl
    private lateinit var payableDataSource: FakeVendorPayableDataSource
    private lateinit var payableRepository: VendorPayableRepositoryImpl
    private lateinit var customerAccountDataSource: FakeCustomerFinancialAccountDataSource
    private lateinit var customerAccountRepository: CustomerFinancialAccountRepositoryImpl
    private lateinit var service: BusinessLedgerServiceImpl

    private val tenantId = "TENANT-001"
    private val projectId = "PRJ-001"

    private val adminPrincipal = AuthenticatedPrincipal(
        userId = "USER-ADMIN-1",
        projectId = projectId,
        username = "admin1",
        role = UserRole.ADMIN
    )

    @Before
    fun setup() {
        ledgerDataSource = FakeBusinessLedgerDataSource()
        ledgerRepository = BusinessLedgerRepositoryImpl(ledgerDataSource)
        expenseDataSource = FakeBusinessExpenseDataSource()
        expenseRepository = BusinessExpenseRepositoryImpl(expenseDataSource)
        payableDataSource = FakeVendorPayableDataSource()
        payableRepository = VendorPayableRepositoryImpl(payableDataSource)
        customerAccountDataSource = FakeCustomerFinancialAccountDataSource()
        customerAccountRepository = CustomerFinancialAccountRepositoryImpl(customerAccountDataSource)

        service = BusinessLedgerServiceImpl(
            repository = ledgerRepository,
            expenseRepository = expenseRepository,
            payableRepository = payableRepository,
            defaultTenantId = tenantId
        )
    }

    @Test
    fun testMathematicalAndDomainConsistencyAcrossModules() = runBlocking {
        // Setup initial Customer Account to verify zero mutation
        val initialCustomerAccount = CustomerFinancialAccount(
            financialAccountId = "CFA-101",
            customerId = "CUST-001",
            accountNumber = "ACC-101",
            currency = "BDT",
            status = CustomerFinancialAccountStatus.ACTIVE,
            tenantId = tenantId,
            projectId = projectId
        )
        customerAccountRepository.createAccount(initialCustomerAccount)

        // 1. Business Expense = Ledger Recognition
        val expenseAmount = BigDecimal("8500.0000")
        val expense = BusinessExpense(
            expenseId = "EXP-CONSIST-1",
            tenantId = tenantId,
            projectId = projectId,
            expenseNumber = "EXP-2026-CONSIST",
            expenseCategoryId = "CAT-001",
            amount = expenseAmount,
            status = BusinessExpenseStatus.APPROVED,
            description = "Spot UV Chemical Supply",
            createdBy = "USER-1"
        )
        expenseRepository.createExpense(expense)

        val expPost = (service.postApprovedExpense(adminPrincipal, PostApprovedExpenseCommand(expenseId = "EXP-CONSIST-1")) as DomainResult.Success).data
        assertEquals(expenseAmount, expPost.debitAmount)

        // 2. Vendor Payable = Recognized Liability
        val payableAmount = BigDecimal("35000.0000")
        val payable = VendorPayable(
            payableId = "PAY-CONSIST-1",
            tenantId = tenantId,
            projectId = projectId,
            payableNumber = "BILL-CONSIST",
            vendorId = "VEND-001",
            description = "Kraft Paper Board Rolls",
            originalAmount = payableAmount,
            paidAmount = BigDecimal.ZERO,
            issueDate = System.currentTimeMillis(),
            dueDate = System.currentTimeMillis() + 86400000L,
            status = VendorPayableStatus.APPROVED,
            createdBy = "USER-1"
        )
        payableRepository.createPayable(payable)

        val payPost = (service.postApprovedPayable(adminPrincipal, PostApprovedPayableCommand(payableId = "PAY-CONSIST-1")) as DomainResult.Success).data
        assertEquals(payableAmount, payPost.debitAmount)

        // 3. Vendor Payment Allocation = Vendor Payment Ledger Movement
        val paymentAmount = BigDecimal("15000.0000")
        val pmtPost = (service.postVendorPayment(
            adminPrincipal,
            PostVendorPaymentCommand(
                payableId = "PAY-CONSIST-1",
                allocationId = "ALLOC-CONSIST-1",
                amount = paymentAmount
            )
        ) as DomainResult.Success).data
        assertEquals(paymentAmount, pmtPost.creditAmount)

        // 4. Source Amount >= Total Cost Allocation
        val allocRes1 = service.allocateCost(
            adminPrincipal,
            AllocateCostCommand(
                sourceType = BusinessLedgerSourceType.BUSINESS_EXPENSE,
                sourceId = "EXP-CONSIST-1",
                jobId = "JOB-01",
                allocatedAmount = BigDecimal("5000.0000")
            )
        )
        assertTrue(allocRes1 is DomainResult.Success)

        val allocRes2 = service.allocateCost(
            adminPrincipal,
            AllocateCostCommand(
                sourceType = BusinessLedgerSourceType.BUSINESS_EXPENSE,
                sourceId = "EXP-CONSIST-1",
                jobId = "JOB-02",
                allocatedAmount = BigDecimal("3500.0000")
            )
        )
        assertTrue(allocRes2 is DomainResult.Success)

        // Unallocated summary check: 8500 - (5000 + 3500) = 0 remaining
        val unalloc = (service.getUnallocatedCostSummary(adminPrincipal, BusinessLedgerSourceType.BUSINESS_EXPENSE, "EXP-CONSIST-1") as DomainResult.Success).data
        assertEquals(BigDecimal("8500.0000"), unalloc.totalSourceAmount)
        assertEquals(BigDecimal("8500.0000"), unalloc.allocatedAmount)
        assertEquals(BigDecimal("0.0000"), unalloc.unallocatedAmount)
        assertEquals(BigDecimal("100.00"), unalloc.allocationPercentage)

        // 5. Customer Financial Data = 100% UNCHANGED
        val customerAccountAfter = (customerAccountRepository.getAccountById(tenantId, projectId, "CFA-101") as DomainResult.Success).data
        assertNotNull(customerAccountAfter)
        assertEquals(initialCustomerAccount.financialAccountId, customerAccountAfter.financialAccountId)
        assertEquals(initialCustomerAccount.status, customerAccountAfter.status)
        assertEquals(initialCustomerAccount.currency, customerAccountAfter.currency)
    }
}
