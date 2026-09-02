package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeCustomerReceivableDataSource
import com.sucharu.sucharupro.data.datasource.FakeFinancialAdjustmentDataSource
import com.sucharu.sucharupro.data.datasource.FakeFinancialTransactionDataSource
import com.sucharu.sucharupro.data.datasource.FakeVendorPayableDataSource
import com.sucharu.sucharupro.data.repository.CustomerReceivableRepositoryImpl
import com.sucharu.sucharupro.data.repository.FinancialAdjustmentRepositoryImpl
import com.sucharu.sucharupro.data.repository.FinancialTransactionRepositoryImpl
import com.sucharu.sucharupro.data.repository.VendorPayableRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.finance.FinancialAdjustmentType
import com.sucharu.sucharupro.domain.model.finance.FinancialReferenceType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class FinancialAdjustmentProjectIsolationTest {

    private lateinit var adjustmentDataSource: FakeFinancialAdjustmentDataSource
    private lateinit var financialTransactionDataSource: FakeFinancialTransactionDataSource
    private lateinit var receivableDataSource: FakeCustomerReceivableDataSource
    private lateinit var payableDataSource: FakeVendorPayableDataSource

    private lateinit var financialTransactionRepository: FinancialTransactionRepository
    private lateinit var customerReceivableRepository: CustomerReceivableRepository
    private lateinit var vendorPayableRepository: VendorPayableRepository
    private lateinit var adjustmentRepository: FinancialAdjustmentRepository

    @Before
    fun setUp() {
        adjustmentDataSource = FakeFinancialAdjustmentDataSource()
        financialTransactionDataSource = FakeFinancialTransactionDataSource()
        receivableDataSource = FakeCustomerReceivableDataSource()
        payableDataSource = FakeVendorPayableDataSource()

        financialTransactionRepository = FinancialTransactionRepositoryImpl(financialTransactionDataSource)
        customerReceivableRepository = CustomerReceivableRepositoryImpl(receivableDataSource)
        vendorPayableRepository = VendorPayableRepositoryImpl(payableDataSource)

        adjustmentRepository = FinancialAdjustmentRepositoryImpl(
            adjustmentDataSource,
            financialTransactionRepository,
            customerReceivableRepository,
            vendorPayableRepository
        )
    }

    @Test
    fun `adjustments in Project A are strictly isolated from Project B`() = runBlocking {
        adjustmentRepository.createAdjustment(
            projectId = "PRJ-A",
            adjustmentType = FinancialAdjustmentType.CUSTOMER_CREDIT_NOTE,
            amount = Money(BigDecimal("1000.00")),
            customerId = "CUST-001",
            referenceType = FinancialReferenceType.INVOICE,
            referenceId = "INV-A-1",
            reasonCode = "REBATE",
            reason = "Rebate",
            description = "Project A Adj",
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )

        adjustmentRepository.createAdjustment(
            projectId = "PRJ-B",
            adjustmentType = FinancialAdjustmentType.CUSTOMER_CREDIT_NOTE,
            amount = Money(BigDecimal("2000.00")),
            customerId = "CUST-001",
            referenceType = FinancialReferenceType.INVOICE,
            referenceId = "INV-B-1",
            reasonCode = "REBATE",
            reason = "Rebate",
            description = "Project B Adj",
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )

        val listA = adjustmentRepository.observeAdjustments("PRJ-A", UserRole.ACCOUNTS).first()
        val listB = adjustmentRepository.observeAdjustments("PRJ-B", UserRole.ACCOUNTS).first()

        assertEquals(1, listA.size)
        assertEquals("PRJ-A", listA[0].projectId)

        assertEquals(1, listB.size)
        assertEquals("PRJ-B", listB[0].projectId)
    }
}
