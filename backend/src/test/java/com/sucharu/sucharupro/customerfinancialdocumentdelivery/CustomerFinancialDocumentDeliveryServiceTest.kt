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
import com.sucharu.sucharupro.domain.model.customerfinancialreporting.*
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoice
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoiceStatus
import com.sucharu.sucharupro.domain.service.customercollection.CustomerCollectionServiceImpl
import com.sucharu.sucharupro.domain.service.customercreditcontrol.CustomerCreditControlServiceImpl
import com.sucharu.sucharupro.domain.service.customerfinancialdashboard.CustomerFinancialDashboardServiceImpl
import com.sucharu.sucharupro.domain.service.customerfinancialreporting.CustomerFinancialDocumentDeliveryServiceImpl
import com.sucharu.sucharupro.domain.service.customerfinancialreporting.CustomerFinancialReportingServiceImpl
import com.sucharu.sucharupro.domain.service.customerledger.CustomerLedgerServiceImpl
import com.sucharu.sucharupro.domain.service.customersettlement.CustomerSettlementServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class CustomerFinancialDocumentDeliveryServiceTest {

    private lateinit var deliveryService: CustomerFinancialDocumentDeliveryServiceImpl
    private lateinit var deliveryRepo: CustomerFinancialDocumentDeliveryRepositoryImpl
    private val tenantId = "TENANT-001"
    private val projectId = "PRJ-001"
    private val customerId = "CUS-001"
    private val accountId = "CFA-001"

    @Before
    fun setup() {
        val customerDs = FakeCustomerDataSource()
        val customerRepo = CustomerRepositoryImpl(customerDs)
        val accountDs = FakeCustomerFinancialAccountDataSource()
        val accountRepo = CustomerFinancialAccountRepositoryImpl(accountDs)
        val invoiceDs = FakeCustomerInvoiceDataSource()
        val invoiceRepo = CustomerInvoiceRepositoryImpl(invoiceDs)
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
        deliveryRepo = CustomerFinancialDocumentDeliveryRepositoryImpl(deliveryDs)
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
                    customerCode = "CUS-001",
                    displayName = "Acme Corp",
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
                    accountNumber = "ACC-001",
                    status = CustomerFinancialAccountStatus.ACTIVE
                )
            )

            invoiceRepo.createInvoice(
                CustomerInvoice(
                    invoiceId = "INV-001",
                    tenantId = tenantId,
                    projectId = projectId,
                    customerId = customerId,
                    customerFinancialAccountId = accountId,
                    invoiceNumber = "INV-2026-001",
                    dueDate = System.currentTimeMillis() + 86400000L,
                    grandTotal = BigDecimal("1000.00"),
                    paidAmount = BigDecimal("200.00"),
                    dueAmount = BigDecimal("800.00"),
                    status = CustomerInvoiceStatus.ISSUED
                )
            )
        }
    }

    @Test
    fun testGenerateAndRegisterDeliveryLifecycle() = runBlocking {
        val genRes = deliveryService.generateAndRegisterDelivery(
            tenantId = tenantId,
            projectId = projectId,
            customerId = customerId,
            reportType = CustomerFinancialReportType.CUSTOMER_STATEMENT,
            format = CustomerFinancialReportFormat.CSV,
            actorId = "admin",
            actorRole = "ADMIN",
            idempotencyKey = "IDEM-001"
        )
        assertTrue(genRes is DomainResult.Success)
        val delivery = (genRes as DomainResult.Success).data
        assertEquals(CustomerFinancialDeliveryStatus.READY, delivery.deliveryStatus)
        assertEquals(0, delivery.accessCount)
        assertTrue(delivery.checksum.startsWith("SHA256:"))

        // Access/Download Document
        val accRes = deliveryService.accessDocument(tenantId, projectId, delivery.deliveryId, "customer-user", "CUSTOMER")
        assertTrue(accRes is DomainResult.Success)
        val payload = (accRes as DomainResult.Success).data
        assertEquals(delivery.deliveryId, payload.deliveryId)
        assertTrue(payload.content.isNotEmpty())

        // Verify updated delivery access stats
        val getRes = deliveryService.getDelivery(tenantId, projectId, delivery.deliveryId)
        assertTrue(getRes is DomainResult.Success)
        val updated = (getRes as DomainResult.Success).data
        assertEquals(1, updated.accessCount)
        assertEquals(CustomerFinancialDeliveryStatus.ACCESSED, updated.deliveryStatus)

        // Notify customer
        val notifRes = deliveryService.notifyCustomer(tenantId, projectId, delivery.deliveryId, actorId = "admin", actorRole = "ADMIN")
        assertTrue(notifRes is DomainResult.Success)

        // Revoke document
        val revRes = deliveryService.revokeDelivery(tenantId, projectId, delivery.deliveryId, "Sent in error", "admin", "ADMIN")
        assertTrue(revRes is DomainResult.Success)
        val revoked = (revRes as DomainResult.Success).data
        assertTrue(revoked.isRevoked)
        assertEquals("Sent in error", revoked.revocationReason)

        // Accessing revoked document fails
        val blockedAcc = deliveryService.accessDocument(tenantId, projectId, delivery.deliveryId, "customer-user", "CUSTOMER")
        assertTrue(blockedAcc is DomainResult.Error)

        // Check Audit history
        val auditRes = deliveryService.getDeliveryAuditHistory(tenantId, projectId, delivery.deliveryId)
        assertTrue(auditRes is DomainResult.Success)
        val audits = (auditRes as DomainResult.Success).data
        assertTrue(audits.size >= 4) // READY, ACCESSED/DOWNLOADED, NOTIFIED, REVOKED
    }
}
