package com.sucharu.sucharupro.customerfinancialreporting

import com.sucharu.sucharupro.data.datasource.FakeCustomerDataSource
import com.sucharu.sucharupro.data.datasource.FakeCustomerFinancialAccountDataSource
import com.sucharu.sucharupro.data.datasource.FakeCustomerInvoiceDataSource
import com.sucharu.sucharupro.data.datasource.customercollection.FakeCustomerCollectionDataSource
import com.sucharu.sucharupro.data.datasource.customercredit.FakeCustomerCreditDataSource
import com.sucharu.sucharupro.data.datasource.customercreditcontrol.FakeCustomerCreditControlDataSource
import com.sucharu.sucharupro.data.datasource.customerledger.FakeCustomerLedgerDataSource
import com.sucharu.sucharupro.data.datasource.customerpayment.FakeCustomerPaymentDataSource
import com.sucharu.sucharupro.data.datasource.customersettlement.FakeCustomerPaymentAllocationDataSource
import com.sucharu.sucharupro.data.repository.CustomerFinancialAccountRepositoryImpl
import com.sucharu.sucharupro.data.repository.CustomerInvoiceRepositoryImpl
import com.sucharu.sucharupro.data.repository.CustomerRepositoryImpl
import com.sucharu.sucharupro.data.repository.customercollection.CustomerCollectionRepositoryImpl
import com.sucharu.sucharupro.data.repository.customercredit.CustomerCreditRepositoryImpl
import com.sucharu.sucharupro.data.repository.customercreditcontrol.CustomerCreditControlRepositoryImpl
import com.sucharu.sucharupro.data.repository.customerledger.CustomerLedgerRepositoryImpl
import com.sucharu.sucharupro.data.repository.customerpayment.CustomerPaymentRepositoryImpl
import com.sucharu.sucharupro.data.repository.customersettlement.CustomerPaymentAllocationRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customer.Customer
import com.sucharu.sucharupro.domain.model.customer.CustomerStatusType
import com.sucharu.sucharupro.domain.model.customer.CustomerType
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccount
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccountStatus
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoice
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoiceStatus
import com.sucharu.sucharupro.domain.service.customercollection.CustomerCollectionServiceImpl
import com.sucharu.sucharupro.domain.service.customercreditcontrol.CustomerCreditControlServiceImpl
import com.sucharu.sucharupro.domain.service.customerfinancialdashboard.CustomerFinancialDashboardServiceImpl
import com.sucharu.sucharupro.domain.service.customerfinancialreporting.CustomerFinancialReportingServiceImpl
import com.sucharu.sucharupro.domain.service.customerledger.CustomerLedgerServiceImpl
import com.sucharu.sucharupro.domain.service.customersettlement.CustomerSettlementServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class CustomerFinancialReportingConsistencyTest {

    private lateinit var reportingService: CustomerFinancialReportingServiceImpl
    private lateinit var settlementService: CustomerSettlementServiceImpl
    private lateinit var creditControlService: CustomerCreditControlServiceImpl
    private lateinit var ledgerService: CustomerLedgerServiceImpl

    private val tenantId = "TENANT-CONST"
    private val projectId = "PRJ-CONST"
    private val customerId = "CUS-CONST"
    private val accountId = "ACC-CONST"

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

        settlementService = CustomerSettlementServiceImpl(allocationRepo, paymentRepo, invoiceRepo, accountRepo, creditRepo)
        creditControlService = CustomerCreditControlServiceImpl(creditControlRepo, customerRepo, accountRepo, settlementService, invoiceRepo)
        val collectionService = CustomerCollectionServiceImpl(collectionRepo, customerRepo, accountRepo, invoiceRepo, settlementService, creditControlService)
        ledgerService = CustomerLedgerServiceImpl(ledgerRepo, accountRepo, invoiceRepo, paymentRepo, creditRepo, customerRepo)
        val dashboardService = CustomerFinancialDashboardServiceImpl(
            customerRepo, accountRepo, invoiceRepo, paymentRepo, creditRepo, collectionRepo,
            settlementService, creditControlService, collectionService, ledgerService
        )

        reportingService = CustomerFinancialReportingServiceImpl(
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

        runBlocking {
            customerRepo.addCustomer(Customer(customerId = customerId, customerCode = "CUS-CONST", displayName = "Consistency Customer", primaryPhone = "0", customerType = CustomerType.BUSINESS, status = CustomerStatusType.ACTIVE, createdAt = "2026-08-29", updatedAt = "2026-08-29"))
            accountRepo.createAccount(CustomerFinancialAccount(financialAccountId = accountId, tenantId = tenantId, projectId = projectId, customerId = customerId, accountNumber = "ACC-CONST", status = CustomerFinancialAccountStatus.ACTIVE))
            invoiceRepo.createInvoice(CustomerInvoice(invoiceId = "INV-01", tenantId = tenantId, projectId = projectId, customerId = customerId, customerFinancialAccountId = accountId, invoiceNumber = "INV-01", grandTotal = BigDecimal("50000.0000"), dueAmount = BigDecimal("50000.0000"), status = CustomerInvoiceStatus.ISSUED))
        }
    }

    @Test
    fun testReportOutstandingMatchesSettlementService() = runBlocking {
        val repRes = reportingService.getCustomerSettlementReport(tenantId, projectId, customerId)
        assertTrue(repRes is DomainResult.Success)
        val rep = (repRes as DomainResult.Success).data

        val canonicalRes = settlementService.getCustomerSettlementSummary(tenantId, projectId, customerId)
        assertTrue(canonicalRes is DomainResult.Success)
        val canonical = (canonicalRes as DomainResult.Success).data

        assertEquals(canonical.totalOutstanding, rep.totalOutstanding)
        assertEquals(canonical.totalInvoiced, rep.totalInvoiced)
        assertEquals(canonical.totalPaid, rep.totalPaid)
        assertEquals(canonical.totalAllocated, rep.totalAllocated)
        assertEquals(canonical.totalUnallocated, rep.totalUnallocated)
    }

    @Test
    fun testAgingReportMatchesCreditControlService() = runBlocking {
        val asOf = System.currentTimeMillis()
        val repRes = reportingService.getCustomerReceivableAgingReport(tenantId, projectId, customerId, asOf)
        assertTrue(repRes is DomainResult.Success)
        val rep = (repRes as DomainResult.Success).data

        val canonicalRes = creditControlService.getReceivableAgingReport(tenantId, projectId, customerId, asOf)
        assertTrue(canonicalRes is DomainResult.Success)
        val canonical = (canonicalRes as DomainResult.Success).data

        assertEquals(canonical.totalOutstanding, rep.totalOutstanding)
    }
}
