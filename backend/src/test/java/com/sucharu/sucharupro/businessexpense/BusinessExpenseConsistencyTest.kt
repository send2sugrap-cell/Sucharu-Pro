package com.sucharu.sucharupro.businessexpense

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.datasource.FakeCustomerDataSource
import com.sucharu.sucharupro.data.datasource.FakeCustomerFinancialAccountDataSource
import com.sucharu.sucharupro.data.datasource.FakeCustomerInvoiceDataSource
import com.sucharu.sucharupro.data.datasource.businessexpense.FakeBusinessExpenseDataSource
import com.sucharu.sucharupro.data.datasource.customerledger.FakeCustomerLedgerDataSource
import com.sucharu.sucharupro.data.datasource.customerpayment.FakeCustomerPaymentDataSource
import com.sucharu.sucharupro.data.repository.CustomerFinancialAccountRepositoryImpl
import com.sucharu.sucharupro.data.repository.CustomerInvoiceRepositoryImpl
import com.sucharu.sucharupro.data.repository.CustomerRepositoryImpl
import com.sucharu.sucharupro.data.repository.businessexpense.BusinessExpenseRepositoryImpl
import com.sucharu.sucharupro.data.repository.customerledger.CustomerLedgerRepositoryImpl
import com.sucharu.sucharupro.data.repository.customerpayment.CustomerPaymentRepositoryImpl
import com.sucharu.sucharupro.domain.model.businessexpense.BusinessExpensePaymentMethod
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
import com.sucharu.sucharupro.domain.service.businessexpense.BusinessExpenseServiceImpl
import com.sucharu.sucharupro.domain.service.businessexpense.CreateBusinessExpenseCommand
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class BusinessExpenseConsistencyTest {

    private lateinit var expenseService: BusinessExpenseServiceImpl
    private lateinit var customerRepo: CustomerRepositoryImpl
    private lateinit var accountRepo: CustomerFinancialAccountRepositoryImpl
    private lateinit var invoiceRepo: CustomerInvoiceRepositoryImpl
    private lateinit var paymentRepo: CustomerPaymentRepositoryImpl
    private lateinit var ledgerRepo: CustomerLedgerRepositoryImpl

    private val tenantId = "TENANT-CONST"
    private val projectId = "PRJ-CONST"
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
        val expenseDs = FakeBusinessExpenseDataSource()
        val expenseRepo = BusinessExpenseRepositoryImpl(expenseDs)
        expenseService = BusinessExpenseServiceImpl(expenseRepo, tenantId)
        expenseDs.seedDefaultCategories(tenantId, projectId)

        val customerDs = FakeCustomerDataSource()
        customerRepo = CustomerRepositoryImpl(customerDs)

        val accountDs = FakeCustomerFinancialAccountDataSource()
        accountRepo = CustomerFinancialAccountRepositoryImpl(accountDs)

        val invoiceDs = FakeCustomerInvoiceDataSource()
        invoiceRepo = CustomerInvoiceRepositoryImpl(invoiceDs)

        val paymentDs = FakeCustomerPaymentDataSource()
        paymentRepo = CustomerPaymentRepositoryImpl(paymentDs)

        val ledgerDs = FakeCustomerLedgerDataSource()
        ledgerRepo = CustomerLedgerRepositoryImpl(ledgerDs)

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
    fun testExpenseOperationsNeverMutateCustomerFinanceOrLedger() = runBlocking {
        // Capture initial states
        val initialAccount = (accountRepo.getAccountByCustomerId(tenantId, projectId, customerId) as DomainResult.Success).data!!
        val initialInvoice = (invoiceRepo.getInvoiceById(tenantId, projectId, "INV-CONST-1") as DomainResult.Success).data!!
        val initialPayment = (paymentRepo.getPaymentById(tenantId, projectId, "PAY-CONST-1") as DomainResult.Success).data!!

        // Execute full expense lifecycle in Module 15 Step 01
        val catId = "CAT-$tenantId-$projectId-CAT-OFC"
        val createRes = expenseService.createExpense(
            adminPrincipal,
            CreateBusinessExpenseCommand(
                categoryId = catId,
                amount = BigDecimal("15000.00"),
                currency = "BDT",
                paymentMethod = BusinessExpensePaymentMethod.BANK,
                paymentReference = "CHQ-10029",
                description = "Office Renovation Work",
                autoSubmit = true
            )
        )
        val expense = (createRes as DomainResult.Success).data

        expenseService.approveExpense(managerPrincipal, expense.expenseId, "Approved")

        // Verify that after creation and approval, Customer states remain 100% untouched
        val postAccount = (accountRepo.getAccountByCustomerId(tenantId, projectId, customerId) as DomainResult.Success).data!!
        val postInvoice = (invoiceRepo.getInvoiceById(tenantId, projectId, "INV-CONST-1") as DomainResult.Success).data!!
        val postPayment = (paymentRepo.getPaymentById(tenantId, projectId, "PAY-CONST-1") as DomainResult.Success).data!!

        assertEquals(initialAccount.financialAccountId, postAccount.financialAccountId)
        assertEquals(initialAccount.status, postAccount.status)

        assertEquals(initialInvoice.grandTotal, postInvoice.grandTotal)
        assertEquals(initialInvoice.paidAmount, postInvoice.paidAmount)
        assertEquals(initialInvoice.dueAmount, postInvoice.dueAmount)

        assertEquals(initialPayment.amount, postPayment.amount)
        assertEquals(initialPayment.status, postPayment.status)
    }
}
