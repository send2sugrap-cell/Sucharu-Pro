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
import com.sucharu.sucharupro.domain.model.finance.FinancialAdjustmentStatus
import com.sucharu.sucharupro.domain.model.finance.FinancialAdjustmentType
import com.sucharu.sucharupro.domain.model.finance.FinancialReferenceType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class FinancialAdjustmentLifecycleTest {

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
    fun `full lifecycle transitions from draft to submitted to approved to posted`() = runBlocking {
        val createRes = adjustmentRepository.createAdjustment(
            projectId = "PRJ-01",
            adjustmentType = FinancialAdjustmentType.CUSTOMER_CREDIT_NOTE,
            amount = Money(BigDecimal("2000.00")),
            customerId = "CUST-001",
            referenceType = FinancialReferenceType.INVOICE,
            referenceId = "INV-1001",
            reasonCode = "DISCOUNT",
            reason = "Bulk discount",
            description = "Approved bulk discount",
            actorId = "staff-1",
            callerRole = UserRole.STAFF
        )
        assertTrue(createRes is DomainResult.Success)
        val adjId = (createRes as DomainResult.Success).data.adjustmentId
        assertEquals(FinancialAdjustmentStatus.DRAFT, createRes.data.status)

        val submitRes = adjustmentRepository.submitAdjustment(adjId, "staff-1", UserRole.STAFF)
        assertTrue(submitRes is DomainResult.Success)
        assertEquals(FinancialAdjustmentStatus.PENDING, (submitRes as DomainResult.Success).data.status)

        val approveRes = adjustmentRepository.approveAdjustment(adjId, "mgr-1", UserRole.MANAGER)
        assertTrue(approveRes is DomainResult.Success)
        assertEquals(FinancialAdjustmentStatus.APPROVED, (approveRes as DomainResult.Success).data.status)

        val postRes = adjustmentRepository.postAdjustment(adjId, "SALES_RETURN", "acct-1", UserRole.ACCOUNTS)
        assertTrue(postRes is DomainResult.Success)
        assertEquals(FinancialAdjustmentStatus.POSTED, (postRes as DomainResult.Success).data.status)
    }
}
