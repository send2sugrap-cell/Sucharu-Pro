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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class CustomerFinancialReportingIsolationTest {

    private lateinit var reportingService: CustomerFinancialReportingServiceImpl

    private val tenantA = "TENANT-A"
    private val tenantB = "TENANT-B"
    private val projectA = "PRJ-A"
    private val projectB = "PRJ-B"
    private val customerA = "CUS-A"
    private val customerB = "CUS-B"

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
            customerRepo.addCustomer(Customer(customerId = customerA, customerCode = "CUS-A", displayName = "Customer A", primaryPhone = "1", customerType = CustomerType.BUSINESS, status = CustomerStatusType.ACTIVE, createdAt = "2026-08-29", updatedAt = "2026-08-29"))
            customerRepo.addCustomer(Customer(customerId = customerB, customerCode = "CUS-B", displayName = "Customer B", primaryPhone = "2", customerType = CustomerType.BUSINESS, status = CustomerStatusType.ACTIVE, createdAt = "2026-08-29", updatedAt = "2026-08-29"))

            accountRepo.createAccount(CustomerFinancialAccount(financialAccountId = "ACC-A", tenantId = tenantA, projectId = projectA, customerId = customerA, accountNumber = "ACC-01", status = CustomerFinancialAccountStatus.ACTIVE))
            accountRepo.createAccount(CustomerFinancialAccount(financialAccountId = "ACC-B", tenantId = tenantB, projectId = projectB, customerId = customerB, accountNumber = "ACC-02", status = CustomerFinancialAccountStatus.ACTIVE))

            invoiceRepo.createInvoice(CustomerInvoice(invoiceId = "INV-A", tenantId = tenantA, projectId = projectA, customerId = customerA, customerFinancialAccountId = "ACC-A", invoiceNumber = "INV-A", grandTotal = BigDecimal("5000.0000"), dueAmount = BigDecimal("5000.0000"), status = CustomerInvoiceStatus.ISSUED))
            invoiceRepo.createInvoice(CustomerInvoice(invoiceId = "INV-B", tenantId = tenantB, projectId = projectB, customerId = customerB, customerFinancialAccountId = "ACC-B", invoiceNumber = "INV-B", grandTotal = BigDecimal("8000.0000"), dueAmount = BigDecimal("8000.0000"), status = CustomerInvoiceStatus.ISSUED))
        }
    }

    @Test
    fun testCrossTenantReportAccessRejected() = runBlocking {
        val res = reportingService.getCustomerStatementReport(tenantA, projectA, customerB)
        assertTrue(res is DomainResult.Error)
    }

    @Test
    fun testValidTenantReportAccessGranted() = runBlocking {
        val res = reportingService.getCustomerStatementReport(tenantA, projectA, customerA)
        assertTrue(res is DomainResult.Success)
    }
}
