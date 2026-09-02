package com.sucharu.sucharupro.customerfinancialdocumentdelivery

import com.sucharu.sucharupro.data.datasource.*
import com.sucharu.sucharupro.data.datasource.customercollection.FakeCustomerCollectionDataSource
import com.sucharu.sucharupro.data.datasource.customercredit.FakeCustomerCreditDataSource
import com.sucharu.sucharupro.data.datasource.customercreditcontrol.FakeCustomerCreditControlDataSource
import com.sucharu.sucharupro.data.datasource.customerfinancialreporting.FakeCustomerFinancialDocumentDeliveryDataSource
import com.sucharu.sucharupro.data.datasource.customerledger.FakeCustomerLedgerDataSource
import com.sucharu.sucharupro.data.datasource.customerpayment.FakeCustomerPaymentDataSource
import com.sucharu.sucharupro.data.datasource.customersettlement.FakeCustomerPaymentAllocationDataSource
import com.sucharu.sucharupro.data.repository.*
import com.sucharu.sucharupro.data.repository.customercollection.CustomerCollectionRepositoryImpl
import com.sucharu.sucharupro.data.repository.customercredit.CustomerCreditRepositoryImpl
import com.sucharu.sucharupro.data.repository.customercreditcontrol.CustomerCreditControlRepositoryImpl
import com.sucharu.sucharupro.data.repository.customerfinancialreporting.CustomerFinancialDocumentDeliveryRepositoryImpl
import com.sucharu.sucharupro.data.repository.customerledger.CustomerLedgerRepositoryImpl
import com.sucharu.sucharupro.data.repository.customerpayment.CustomerPaymentRepositoryImpl
import com.sucharu.sucharupro.data.repository.customersettlement.CustomerPaymentAllocationRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customer.Customer
import com.sucharu.sucharupro.domain.model.customer.CustomerStatusType
import com.sucharu.sucharupro.domain.model.customer.CustomerType
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccount
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccountStatus
import com.sucharu.sucharupro.domain.model.customerfinancialreporting.CustomerFinancialReportFormat
import com.sucharu.sucharupro.domain.model.customerfinancialreporting.CustomerFinancialReportType
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoice
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoiceStatus
import com.sucharu.sucharupro.domain.model.customerpayment.CustomerPayment
import com.sucharu.sucharupro.domain.model.customerpayment.CustomerPaymentMethod
import com.sucharu.sucharupro.domain.model.customerpayment.CustomerPaymentStatus
import com.sucharu.sucharupro.domain.service.customercollection.CustomerCollectionServiceImpl
import com.sucharu.sucharupro.domain.service.customercreditcontrol.CustomerCreditControlServiceImpl
import com.sucharu.sucharupro.domain.service.customerfinancialdashboard.CustomerFinancialDashboardServiceImpl
import com.sucharu.sucharupro.domain.service.customerfinancialreporting.CustomerFinancialDocumentDeliveryServiceImpl
import com.sucharu.sucharupro.domain.service.customerfinancialreporting.CustomerFinancialReportingServiceImpl
import com.sucharu.sucharupro.domain.service.customerledger.CustomerLedgerServiceImpl
import com.sucharu.sucharupro.domain.service.customersettlement.CustomerSettlementServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class CustomerFinancialDocumentDeliveryConsistencyTest {

    private lateinit var deliveryService: CustomerFinancialDocumentDeliveryServiceImpl
    private lateinit var accountRepo: CustomerFinancialAccountRepositoryImpl
    private lateinit var invoiceRepo: CustomerInvoiceRepositoryImpl
    private lateinit var paymentRepo: CustomerPaymentRepositoryImpl
    private lateinit var creditRepo: CustomerCreditRepositoryImpl
    private lateinit var ledgerRepo: CustomerLedgerRepositoryImpl

    private val tenantId = "TENANT-CONST"
    private val projectId = "PRJ-CONST"
    private val customerId = "CUS-CONST"
    private val accountId = "CFA-CONST"

    @Before
    fun setup() {
        val customerDs = FakeCustomerDataSource()
        val customerRepo = CustomerRepositoryImpl(customerDs)
        val accountDs = FakeCustomerFinancialAccountDataSource()
        accountRepo = CustomerFinancialAccountRepositoryImpl(accountDs)
        val invoiceDs = FakeCustomerInvoiceDataSource()
        invoiceRepo = CustomerInvoiceRepositoryImpl(invoiceDs)
        val paymentDs = FakeCustomerPaymentDataSource()
        paymentRepo = CustomerPaymentRepositoryImpl(paymentDs)
        val creditDs = FakeCustomerCreditDataSource()
        creditRepo = CustomerCreditRepositoryImpl(creditDs)
        val allocationDs = FakeCustomerPaymentAllocationDataSource()
        val allocationRepo = CustomerPaymentAllocationRepositoryImpl(allocationDs)
        val creditControlDs = FakeCustomerCreditControlDataSource()
        val creditControlRepo = CustomerCreditControlRepositoryImpl(creditControlDs)
        val collectionDs = FakeCustomerCollectionDataSource()
        val collectionRepo = CustomerCollectionRepositoryImpl(collectionDs)
        val ledgerDs = FakeCustomerLedgerDataSource()
        ledgerRepo = CustomerLedgerRepositoryImpl(ledgerDs)

        val settlementService = CustomerSettlementServiceImpl(allocationRepo, paymentRepo, invoiceRepo, accountRepo, creditRepo)
        val creditControlService = CustomerCreditControlServiceImpl(creditControlRepo, customerRepo, accountRepo, settlementService, invoiceRepo)
        val collectionService = CustomerCollectionServiceImpl(collectionRepo, customerRepo, accountRepo, invoiceRepo, settlementService, creditControlService)
        val ledgerService = CustomerLedgerServiceImpl(ledgerRepo, accountRepo, invoiceRepo, paymentRepo, creditRepo, customerRepo)
        val dashboardService = CustomerFinancialDashboardServiceImpl(
            customerRepo, accountRepo, invoiceRepo, paymentRepo, creditRepo, collectionRepo,
            settlementService, creditControlService, collectionService, ledgerService
        )
        val reportingService = CustomerFinancialReportingServiceImpl(
            customerRepository = customerRepo,
            accountRepository = accountRepo,
            invoiceRepository = invoiceRepo,
            paymentRepository = paymentRepo,
            creditRepository = creditRepo,
            ledgerService = ledgerService,
            settlementService = settlementService,
            creditControlService = creditControlService,
            collectionService = collectionService,
            dashboardService = dashboardService
        )

        val deliveryDs = FakeCustomerFinancialDocumentDeliveryDataSource()
        val deliveryRepo = CustomerFinancialDocumentDeliveryRepositoryImpl(deliveryDs)
        deliveryService = CustomerFinancialDocumentDeliveryServiceImpl(
            deliveryRepository = deliveryRepo,
            reportingService = reportingService,
            customerRepository = customerRepo,
            notificationRepository = null
        )

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
                    grandTotal = BigDecimal("1500.00"),
                    paidAmount = BigDecimal("500.00"),
                    dueAmount = BigDecimal("1000.00"),
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
    fun testDocumentDeliveryOperationsNeverMutateUnderlyingFinancialLedgerBalances() = runBlocking {
        val initialAccount = (accountRepo.getAccountByCustomerId(tenantId, projectId, customerId) as DomainResult.Success).data!!
        val initialInvoice = (invoiceRepo.getInvoiceById(tenantId, projectId, "INV-CONST-1") as DomainResult.Success).data!!
        val initialPayment = (paymentRepo.getPaymentById(tenantId, projectId, "PAY-CONST-1") as DomainResult.Success).data!!

        // Generate delivery
        val genRes = deliveryService.generateAndRegisterDelivery(
            tenantId, projectId, customerId, CustomerFinancialReportType.CUSTOMER_STATEMENT, CustomerFinancialReportFormat.PDF,
            actorId = "admin", actorRole = "ADMIN"
        )
        assertTrue(genRes is DomainResult.Success)
        val delivery = (genRes as DomainResult.Success).data

        // Access and download
        deliveryService.accessDocument(tenantId, projectId, delivery.deliveryId, "admin", "ADMIN")

        // Notify
        deliveryService.notifyCustomer(tenantId, projectId, delivery.deliveryId, actorId = "admin", actorRole = "ADMIN")

        // Revoke
        deliveryService.revokeDelivery(tenantId, projectId, delivery.deliveryId, "Test revoke", "admin", "ADMIN")

        // Assert financial figures remain exactly identical
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
