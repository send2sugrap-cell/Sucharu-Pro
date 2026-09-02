package com.sucharu.sucharupro.customerfinancialalerts

import com.sucharu.sucharupro.data.datasource.*
import com.sucharu.sucharupro.data.datasource.customercollection.FakeCustomerCollectionDataSource
import com.sucharu.sucharupro.data.datasource.customercredit.FakeCustomerCreditDataSource
import com.sucharu.sucharupro.data.datasource.customercreditcontrol.FakeCustomerCreditControlDataSource
import com.sucharu.sucharupro.data.datasource.customerfinancialreporting.FakeCustomerFinancialAlertDataSource
import com.sucharu.sucharupro.data.datasource.customerfinancialreporting.FakeCustomerFinancialDocumentDeliveryDataSource
import com.sucharu.sucharupro.data.datasource.customerledger.FakeCustomerLedgerDataSource
import com.sucharu.sucharupro.data.datasource.customerpayment.FakeCustomerPaymentDataSource
import com.sucharu.sucharupro.data.datasource.customersettlement.FakeCustomerPaymentAllocationDataSource
import com.sucharu.sucharupro.data.repository.*
import com.sucharu.sucharupro.data.repository.customercollection.CustomerCollectionRepositoryImpl
import com.sucharu.sucharupro.data.repository.customercredit.CustomerCreditRepositoryImpl
import com.sucharu.sucharupro.data.repository.customercreditcontrol.CustomerCreditControlRepositoryImpl
import com.sucharu.sucharupro.data.repository.customerfinancialreporting.CustomerFinancialAlertRepositoryImpl
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
import com.sucharu.sucharupro.domain.model.customerfinancialreporting.CustomerFinancialAlertStatus
import com.sucharu.sucharupro.domain.model.customerfinancialreporting.CustomerFinancialAlertType
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoice
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoiceStatus
import com.sucharu.sucharupro.domain.service.customercollection.CustomerCollectionServiceImpl
import com.sucharu.sucharupro.domain.service.customercreditcontrol.CustomerCreditControlServiceImpl
import com.sucharu.sucharupro.domain.service.customerfinancialdashboard.CustomerFinancialDashboardServiceImpl
import com.sucharu.sucharupro.domain.service.customerfinancialreporting.CustomerFinancialAlertServiceImpl
import com.sucharu.sucharupro.domain.service.customerfinancialreporting.CustomerFinancialDocumentDeliveryServiceImpl
import com.sucharu.sucharupro.domain.service.customerfinancialreporting.CustomerFinancialReportingServiceImpl
import com.sucharu.sucharupro.domain.service.customerledger.CustomerLedgerServiceImpl
import com.sucharu.sucharupro.domain.service.customersettlement.CustomerSettlementServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class CustomerFinancialAlertServiceTest {

    private lateinit var alertService: CustomerFinancialAlertServiceImpl
    private lateinit var invoiceRepo: CustomerInvoiceRepositoryImpl
    private lateinit var customerRepo: CustomerRepositoryImpl

    private val tenantId = "TENANT-1"
    private val projectId = "PRJ-1"
    private val customerId = "CUS-1"
    private val accountId = "CFA-1"

    @Before
    fun setup() {
        val customerDs = FakeCustomerDataSource()
        customerRepo = CustomerRepositoryImpl(customerDs)
        val accountDs = FakeCustomerFinancialAccountDataSource()
        val accountRepo = CustomerFinancialAccountRepositoryImpl(accountDs)
        val invoiceDs = FakeCustomerInvoiceDataSource()
        invoiceRepo = CustomerInvoiceRepositoryImpl(invoiceDs)
        val paymentDs = FakeCustomerPaymentDataSource()
        val paymentRepo = CustomerPaymentRepositoryImpl(paymentDs)
        val creditDs = FakeCustomerCreditDataSource()
        val creditRepo = CustomerCreditRepositoryImpl(creditDs)
        val allocationDs = FakeCustomerPaymentAllocationDataSource()
        val allocationRepo = CustomerPaymentAllocationRepositoryImpl(allocationDs)
        val creditControlDs = FakeCustomerCreditControlDataSource()
        val creditControlRepo = CustomerCreditControlRepositoryImpl(creditControlDs)
        val collectionDs = FakeCustomerCollectionDataSource()
        val collectionRepo = CustomerCollectionRepositoryImpl(collectionDs)
        val ledgerDs = FakeCustomerLedgerDataSource()
        val ledgerRepo = CustomerLedgerRepositoryImpl(ledgerDs)

        val settlementService = CustomerSettlementServiceImpl(allocationRepo, paymentRepo, invoiceRepo, accountRepo, creditRepo)
        val creditControlService = CustomerCreditControlServiceImpl(creditControlRepo, customerRepo, accountRepo, settlementService, invoiceRepo)
        val collectionService = CustomerCollectionServiceImpl(collectionRepo, customerRepo, accountRepo, invoiceRepo, settlementService, creditControlService)
        val ledgerService = CustomerLedgerServiceImpl(ledgerRepo, accountRepo, invoiceRepo, paymentRepo, creditRepo, customerRepo)
        val dashboardService = CustomerFinancialDashboardServiceImpl(
            customerRepo, accountRepo, invoiceRepo, paymentRepo, creditRepo, collectionRepo,
            settlementService, creditControlService, collectionService, ledgerService
        )

        val alertDs = FakeCustomerFinancialAlertDataSource()
        val alertRepo = CustomerFinancialAlertRepositoryImpl(alertDs)

        alertService = CustomerFinancialAlertServiceImpl(
            alertRepository = alertRepo,
            customerRepository = customerRepo,
            accountRepository = accountRepo,
            invoiceRepository = invoiceRepo,
            paymentRepository = paymentRepo,
            creditControlService = creditControlService,
            collectionService = collectionService,
            dashboardService = dashboardService,
            notificationRepository = null
        )

        runBlocking {
            customerRepo.addCustomer(
                Customer(
                    customerId = customerId,
                    customerCode = "CUS-1",
                    displayName = "Alert Customer",
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
                    accountNumber = "ACC-1",
                    status = CustomerFinancialAccountStatus.ACTIVE
                )
            )

            // Overdue Invoice
            invoiceRepo.createInvoice(
                CustomerInvoice(
                    invoiceId = "INV-OVERDUE",
                    tenantId = tenantId,
                    projectId = projectId,
                    customerId = customerId,
                    customerFinancialAccountId = accountId,
                    invoiceNumber = "INV-001",
                    dueDate = System.currentTimeMillis() - 86400000L * 5, // 5 days overdue
                    grandTotal = BigDecimal("1000.00"),
                    paidAmount = BigDecimal("0.00"),
                    dueAmount = BigDecimal("1000.00"),
                    status = CustomerInvoiceStatus.ISSUED
                )
            )
        }
    }

    @Test
    fun testAlertEvaluationDeduplicationAndLifecycle() = runBlocking {
        // 1. Evaluate Alerts
        val evalRes1 = alertService.evaluateCustomerFinancialAlerts(tenantId, projectId, customerId, "admin", "ADMIN")
        assertTrue(evalRes1 is DomainResult.Success)
        val alerts1 = (evalRes1 as DomainResult.Success).data
        assertEquals(1, alerts1.size)
        val alert = alerts1.first()
        assertEquals(CustomerFinancialAlertType.INVOICE_OVERDUE, alert.alertType)
        assertEquals(CustomerFinancialAlertStatus.OPEN, alert.status)

        // 2. Repeated Evaluation Does NOT Create Duplicates (Idempotency)
        val evalRes2 = alertService.evaluateCustomerFinancialAlerts(tenantId, projectId, customerId, "admin", "ADMIN")
        assertTrue(evalRes2 is DomainResult.Success)
        val alerts2 = (evalRes2 as DomainResult.Success).data
        assertEquals(1, alerts2.size)
        assertEquals(alert.alertId, alerts2.first().alertId)

        // 3. Acknowledge Alert
        val ackRes = alertService.acknowledgeAlert(tenantId, projectId, alert.alertId, "staff_1", "STAFF")
        assertTrue(ackRes is DomainResult.Success)
        val ackAlert = (ackRes as DomainResult.Success).data
        assertEquals(CustomerFinancialAlertStatus.ACKNOWLEDGED, ackAlert.status)

        // 4. Resolve Alert
        val resRes = alertService.resolveAlert(tenantId, projectId, alert.alertId, "Paid in cash", "staff_1", "STAFF")
        assertTrue(resRes is DomainResult.Success)
        val resAlert = (resRes as DomainResult.Success).data
        assertEquals(CustomerFinancialAlertStatus.RESOLVED, resAlert.status)

        // 5. Summary Check
        val summaryRes = alertService.getAlertSummary(tenantId, projectId, customerId)
        assertTrue(summaryRes is DomainResult.Success)
        val summary = (summaryRes as DomainResult.Success).data
        assertEquals(0, summary.totalOpen)
        assertEquals(1, summary.resolvedCount)

        // 6. Audit Trail Check
        val auditRes = alertService.getAlertAuditHistory(tenantId, projectId, alert.alertId)
        assertTrue(auditRes is DomainResult.Success)
        val audits = (auditRes as DomainResult.Success).data
        assertEquals(3, audits.size) // CREATED, ACKNOWLEDGED, RESOLVED
    }
}
