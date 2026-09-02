package com.sucharu.sucharupro.customerfinancialdashboard

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
import com.sucharu.sucharupro.domain.model.customerpayment.CustomerPayment
import com.sucharu.sucharupro.domain.model.customerpayment.CustomerPaymentMethod
import com.sucharu.sucharupro.domain.model.customerpayment.CustomerPaymentStatus
import com.sucharu.sucharupro.domain.service.customercollection.CustomerCollectionServiceImpl
import com.sucharu.sucharupro.domain.service.customercreditcontrol.CustomerCreditControlServiceImpl
import com.sucharu.sucharupro.domain.service.customerfinancialdashboard.CustomerFinancialDashboardServiceImpl
import com.sucharu.sucharupro.domain.service.customerledger.CustomerLedgerServiceImpl
import com.sucharu.sucharupro.domain.service.customersettlement.CustomerSettlementServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class CustomerFinancialConsistencyTest {

    private lateinit var dashboardService: CustomerFinancialDashboardServiceImpl
    private lateinit var settlementService: CustomerSettlementServiceImpl
    private lateinit var creditControlService: CustomerCreditControlServiceImpl

    private val tenantId = "TENANT-CONSIST-01"
    private val projectId = "PRJ-CONSIST-01"
    private val customerId = "CUS-CONSIST-01"
    private val accountId = "CFA-CONSIST-01"

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
        val ledgerService = CustomerLedgerServiceImpl(ledgerRepo, accountRepo, invoiceRepo, paymentRepo, creditRepo, customerRepo)

        dashboardService = CustomerFinancialDashboardServiceImpl(
            customerRepository = customerRepo,
            accountRepository = accountRepo,
            invoiceRepository = invoiceRepo,
            paymentRepository = paymentRepo,
            creditRepository = creditRepo,
            collectionRepository = collectionRepo,
            settlementService = settlementService,
            creditControlService = creditControlService,
            collectionService = collectionService,
            ledgerService = ledgerService
        )

        runBlocking {
            customerRepo.addCustomer(
                Customer(
                    customerId = customerId,
                    customerCode = "CUS-CONSIST",
                    displayName = "Consistent Customer",
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
                    accountNumber = "ACC-CONSIST-01",
                    status = CustomerFinancialAccountStatus.ACTIVE
                )
            )

            invoiceRepo.createInvoice(
                CustomerInvoice(
                    invoiceId = "INV-01",
                    tenantId = tenantId,
                    projectId = projectId,
                    customerId = customerId,
                    customerFinancialAccountId = accountId,
                    invoiceNumber = "INV-01",
                    grandTotal = BigDecimal("100000.0000"),
                    dueAmount = BigDecimal("60000.0000"),
                    paidAmount = BigDecimal("40000.0000"),
                    status = CustomerInvoiceStatus.PARTIALLY_PAID
                )
            )

            paymentRepo.createPayment(
                CustomerPayment(
                    paymentId = "PAY-01",
                    tenantId = tenantId,
                    projectId = projectId,
                    customerId = customerId,
                    customerFinancialAccountId = accountId,
                    paymentNumber = "PAY-01",
                    amount = BigDecimal("50000.0000"),
                    paymentMethod = CustomerPaymentMethod.CASH,
                    status = CustomerPaymentStatus.CONFIRMED,
                    paymentDate = System.currentTimeMillis()
                )
            )
        }
    }

    @Test
    fun testDashboardMatchesCanonicalSettlementAndCreditControl() = runBlocking {
        val dashRes = dashboardService.getCustomerFinancialDashboard(tenantId, projectId, customerId)
        assertTrue(dashRes is DomainResult.Success)
        val dash = (dashRes as DomainResult.Success).data

        val settleRes = settlementService.getCustomerSettlementSummary(tenantId, projectId, customerId)
        assertTrue(settleRes is DomainResult.Success)
        val settle = (settleRes as DomainResult.Success).data

        val riskRes = creditControlService.getReceivableRiskSummary(tenantId, projectId, customerId)
        assertTrue(riskRes is DomainResult.Success)
        val risk = (riskRes as DomainResult.Success).data

        // 1. Invariance check: Dashboard Outstanding == Settlement Outstanding
        assertEquals(settle.totalOutstanding, dash.outstandingReceivable)
        assertEquals(settle.totalInvoiced, dash.totalInvoiced)
        assertEquals(settle.totalPaid, dash.totalPaid)
        assertEquals(settle.totalAllocated, dash.totalAllocated)
        assertEquals(settle.totalUnallocated, dash.totalUnallocated)

        // 2. Invariance check: Dashboard Credit == Credit Control Risk Summary
        assertEquals(risk.creditLimit, dash.creditLimit)
        assertEquals(risk.netReceivableExposure, dash.currentCreditExposure)
        assertEquals(risk.availableCreditLimit, dash.availableCreditCapacity)
        assertEquals(risk.riskStatus, dash.riskStatus)
    }
}
