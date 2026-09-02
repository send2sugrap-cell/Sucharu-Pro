package com.sucharu.sucharupro.vendorpayable

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.datasource.FakeCustomerDataSource
import com.sucharu.sucharupro.data.datasource.FakeCustomerFinancialAccountDataSource
import com.sucharu.sucharupro.data.datasource.FakeCustomerInvoiceDataSource
import com.sucharu.sucharupro.data.datasource.customerpayment.FakeCustomerPaymentDataSource
import com.sucharu.sucharupro.data.datasource.vendorpayable.FakeVendorPayableDataSource
import com.sucharu.sucharupro.data.repository.CustomerFinancialAccountRepositoryImpl
import com.sucharu.sucharupro.data.repository.CustomerInvoiceRepositoryImpl
import com.sucharu.sucharupro.data.repository.CustomerRepositoryImpl
import com.sucharu.sucharupro.data.repository.customerpayment.CustomerPaymentRepositoryImpl
import com.sucharu.sucharupro.data.repository.vendorpayable.VendorPayableRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customer.Customer
import com.sucharu.sucharupro.domain.model.customer.CustomerStatusType
import com.sucharu.sucharupro.domain.model.customer.CustomerType
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccount
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccountStatus
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoice
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoiceStatus
import com.sucharu.sucharupro.domain.model.customerpayment.CustomerPayment
import com.sucharu.sucharupro.domain.model.customerpayment.CustomerPaymentMethod
import com.sucharu.sucharupro.domain.model.customerpayment.CustomerPaymentStatus
import com.sucharu.sucharupro.domain.model.vendorpayable.VendorPayablePaymentMethod
import com.sucharu.sucharupro.domain.service.vendorpayable.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class VendorPayableConsistencyTest {

    private lateinit var payableService: VendorPayableServiceImpl
    private lateinit var customerRepo: CustomerRepositoryImpl
    private lateinit var accountRepo: CustomerFinancialAccountRepositoryImpl
    private lateinit var invoiceRepo: CustomerInvoiceRepositoryImpl
    private lateinit var paymentRepo: CustomerPaymentRepositoryImpl

    private val tenantId = "TENANT-CONST"
    private val projectId = "PRJ-CONST"
    private val vendorId = "VEND-CONST-01"
    private val customerId = "CUS-CONST-01"
    private val accountId = "CFA-CONST-01"

    private val adminPrincipal = AuthenticatedPrincipal(
        userId = "USER-ADMIN",
        projectId = projectId,
        username = "admin",
        role = UserRole.ADMIN
    )

    private val managerPrincipal = AuthenticatedPrincipal(
        userId = "USER-MGR",
        projectId = projectId,
        username = "manager",
        role = UserRole.MANAGER
    )

    @Before
    fun setup() {
        val payableDs = FakeVendorPayableDataSource()
        val payableRepo = VendorPayableRepositoryImpl(payableDs)
        payableService = VendorPayableServiceImpl(payableRepo, tenantId)

        val customerDs = FakeCustomerDataSource()
        customerRepo = CustomerRepositoryImpl(customerDs)

        val accountDs = FakeCustomerFinancialAccountDataSource()
        accountRepo = CustomerFinancialAccountRepositoryImpl(accountDs)

        val invoiceDs = FakeCustomerInvoiceDataSource()
        invoiceRepo = CustomerInvoiceRepositoryImpl(invoiceDs)

        val paymentDs = FakeCustomerPaymentDataSource()
        paymentRepo = CustomerPaymentRepositoryImpl(paymentDs)

        runBlocking {
            customerRepo.addCustomer(
                Customer(
                    customerId = customerId,
                    customerCode = "CUS-CONST",
                    displayName = "Const Customer",
                    primaryPhone = "+8801700000001",
                    customerType = CustomerType.BUSINESS,
                    status = CustomerStatusType.ACTIVE,
                    createdAt = "2026-08-29T00:00:00Z",
                    updatedAt = "2026-08-29T00:00:00Z"
                )
            )

            accountRepo.createAccount(
                CustomerFinancialAccount(
                    financialAccountId = accountId,
                    tenantId = tenantId,
                    projectId = projectId,
                    customerId = customerId,
                    accountNumber = "ACC-CONST",
                    status = CustomerFinancialAccountStatus.ACTIVE
                )
            )

            invoiceRepo.createInvoice(
                CustomerInvoice(
                    invoiceId = "INV-CONST-1",
                    tenantId = tenantId,
                    projectId = projectId,
                    customerId = customerId,
                    customerFinancialAccountId = accountId,
                    invoiceNumber = "INV-001",
                    dueDate = System.currentTimeMillis() + 86400000L,
                    grandTotal = BigDecimal("2000.00"),
                    paidAmount = BigDecimal("500.00"),
                    dueAmount = BigDecimal("1500.00"),
                    status = CustomerInvoiceStatus.ISSUED
                )
            )

            paymentRepo.createPayment(
                CustomerPayment(
                    paymentId = "PAY-CONST-1",
                    tenantId = tenantId,
                    projectId = projectId,
                    customerId = customerId,
                    customerFinancialAccountId = accountId,
                    paymentNumber = "PAY-001",
                    paymentMethod = CustomerPaymentMethod.BANK,
                    amount = BigDecimal("500.00"),
                    status = CustomerPaymentStatus.RECORDED
                )
            )
        }
    }

    @Test
    fun testMathematicalInvariantsAndZeroCustomerMutation() = runBlocking {
        // Initial customer snapshot
        val initialAccount = (accountRepo.getAccountByCustomerId(tenantId, projectId, customerId) as DomainResult.Success).data!!
        val initialInvoice = (invoiceRepo.getInvoiceById(tenantId, projectId, "INV-CONST-1") as DomainResult.Success).data!!
        val initialPayment = (paymentRepo.getPaymentById(tenantId, projectId, "PAY-CONST-1") as DomainResult.Success).data!!

        // Create 2 Vendor Payables:
        // 1. 10,000.00 -> Pay 4,000.00 (Outstanding = 6,000.00)
        // 2. 5,000.00  -> Pay 5,000.00 (Outstanding = 0.00)
        val p1Res = payableService.createPayable(
            adminPrincipal,
            CreateVendorPayableCommand(
                vendorId = vendorId,
                originalAmount = BigDecimal("10000.00"),
                description = "CTP Output",
                autoSubmit = true
            )
        )
        val p1 = (p1Res as DomainResult.Success).data
        payableService.approvePayable(managerPrincipal, p1.payableId, "Approved")
        payableService.allocatePayment(
            managerPrincipal,
            p1.payableId,
            AllocateVendorPayablePaymentCommand(
                amount = BigDecimal("4000.00"),
                paymentMethod = VendorPayablePaymentMethod.BANK,
                paymentReference = "TRX-101"
            )
        )

        val p2Res = payableService.createPayable(
            adminPrincipal,
            CreateVendorPayableCommand(
                vendorId = vendorId,
                originalAmount = BigDecimal("5000.00"),
                description = "Binding Services",
                autoSubmit = true
            )
        )
        val p2 = (p2Res as DomainResult.Success).data
        payableService.approvePayable(managerPrincipal, p2.payableId, "Approved")
        payableService.allocatePayment(
            managerPrincipal,
            p2.payableId,
            AllocateVendorPayablePaymentCommand(
                amount = BigDecimal("5000.00"),
                paymentMethod = VendorPayablePaymentMethod.BANK,
                paymentReference = "TRX-102"
            )
        )

        // 1. Verify Vendor Aggregate Invariants
        val summaryRes = payableService.getVendorPayableSummary(managerPrincipal, vendorId)
        assertTrue(summaryRes is DomainResult.Success)
        val summary = (summaryRes as DomainResult.Success).data

        // Total Approved = 15,000.00
        assertEquals(BigDecimal("15000.0000"), summary.totalApprovedLiability)
        // Total Paid = 9,000.00
        assertEquals(BigDecimal("9000.0000"), summary.totalPaid)
        // Total Outstanding = 6,000.00
        assertEquals(BigDecimal("6000.0000"), summary.totalOutstanding)
        // Total Approved = Total Paid + Total Outstanding
        assertEquals(summary.totalApprovedLiability, summary.totalPaid.add(summary.totalOutstanding))

        // 2. Verify Customer Financial Entities are 100% untouched
        val postAccount = (accountRepo.getAccountByCustomerId(tenantId, projectId, customerId) as DomainResult.Success).data!!
        val postInvoice = (invoiceRepo.getInvoiceById(tenantId, projectId, "INV-CONST-1") as DomainResult.Success).data!!
        val postPayment = (paymentRepo.getPaymentById(tenantId, projectId, "PAY-CONST-1") as DomainResult.Success).data!!

        assertEquals(initialAccount.financialAccountId, postAccount.financialAccountId)
        assertEquals(initialInvoice.dueAmount, postInvoice.dueAmount)
        assertEquals(initialPayment.amount, postPayment.amount)
    }
}
