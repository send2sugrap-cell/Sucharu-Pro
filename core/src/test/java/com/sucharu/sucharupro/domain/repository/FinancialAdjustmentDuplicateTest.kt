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
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class FinancialAdjustmentDuplicateTest {

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
    fun `distinct adjustments for different business reasons produce unique adjustment records`() = runBlocking {
        val adj1 = (adjustmentRepository.createAdjustment(
            projectId = "PRJ-01",
            adjustmentType = FinancialAdjustmentType.CUSTOMER_CREDIT_NOTE,
            amount = Money(BigDecimal("1000.00")),
            customerId = "CUST-001",
            referenceType = FinancialReferenceType.INVOICE,
            referenceId = "INV-1001",
            reasonCode = "DAMAGE",
            reason = "Packaging damaged",
            description = "Damage rebate",
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        ) as DomainResult.Success).data

        val adj2 = (adjustmentRepository.createAdjustment(
            projectId = "PRJ-01",
            adjustmentType = FinancialAdjustmentType.CUSTOMER_CREDIT_NOTE,
            amount = Money(BigDecimal("500.00")),
            customerId = "CUST-001",
            referenceType = FinancialReferenceType.INVOICE,
            referenceId = "INV-1001",
            reasonCode = "DELAY",
            reason = "Late delivery compensation",
            description = "Delay rebate",
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        ) as DomainResult.Success).data

        assertNotEquals(adj1.adjustmentId, adj2.adjustmentId)
        assertNotEquals(adj1.adjustmentNo, adj2.adjustmentNo)
    }
}
