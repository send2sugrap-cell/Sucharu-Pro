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
import com.sucharu.sucharupro.domain.model.finance.FinancialAdjustmentDirection
import com.sucharu.sucharupro.domain.model.finance.FinancialAdjustmentType
import com.sucharu.sucharupro.domain.model.finance.FinancialReferenceType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class FinancialAdjustmentDirectionTest {

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
    fun `direction explicitly sets DEBIT or CREDIT without negative amounts`() = runBlocking {
        val creditAdj = (adjustmentRepository.createAdjustment(
            projectId = "PRJ-01",
            adjustmentType = FinancialAdjustmentType.CUSTOMER_CREDIT_NOTE,
            direction = FinancialAdjustmentDirection.CREDIT,
            amount = Money(BigDecimal("3000.00")),
            customerId = "CUST-001",
            referenceType = FinancialReferenceType.INVOICE,
            referenceId = "INV-1001",
            reasonCode = "REBATE",
            reason = "Customer rebate",
            description = "Credit adjustment",
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        ) as DomainResult.Success).data

        assertEquals(FinancialAdjustmentDirection.CREDIT, creditAdj.direction)
        assertTrue(creditAdj.amount.isPositive())

        val debitAdj = (adjustmentRepository.createAdjustment(
            projectId = "PRJ-01",
            adjustmentType = FinancialAdjustmentType.VENDOR_DEBIT_NOTE,
            direction = FinancialAdjustmentDirection.DEBIT,
            amount = Money(BigDecimal("1500.00")),
            vendorId = "VEND-001",
            referenceType = FinancialReferenceType.VENDOR_BILL,
            referenceId = "BILL-1001",
            reasonCode = "PENALTY",
            reason = "Late delivery penalty",
            description = "Debit adjustment",
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        ) as DomainResult.Success).data

        assertEquals(FinancialAdjustmentDirection.DEBIT, debitAdj.direction)
        assertTrue(debitAdj.amount.isPositive())
    }
}
