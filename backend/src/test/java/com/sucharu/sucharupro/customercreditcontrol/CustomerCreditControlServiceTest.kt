package com.sucharu.sucharupro.customercreditcontrol

import com.sucharu.sucharupro.data.datasource.FakeCustomerDataSource
import com.sucharu.sucharupro.data.datasource.FakeCustomerFinancialAccountDataSource
import com.sucharu.sucharupro.data.datasource.FakeCustomerInvoiceDataSource
import com.sucharu.sucharupro.data.datasource.customercredit.FakeCustomerCreditDataSource
import com.sucharu.sucharupro.data.datasource.customercreditcontrol.FakeCustomerCreditControlDataSource
import com.sucharu.sucharupro.data.datasource.customerpayment.FakeCustomerPaymentDataSource
import com.sucharu.sucharupro.data.datasource.customersettlement.FakeCustomerPaymentAllocationDataSource
import com.sucharu.sucharupro.data.repository.CustomerFinancialAccountRepositoryImpl
import com.sucharu.sucharupro.data.repository.CustomerInvoiceRepositoryImpl
import com.sucharu.sucharupro.data.repository.CustomerRepositoryImpl
import com.sucharu.sucharupro.data.repository.customercredit.CustomerCreditRepositoryImpl
import com.sucharu.sucharupro.data.repository.customercreditcontrol.CustomerCreditControlRepositoryImpl
import com.sucharu.sucharupro.data.repository.customerpayment.CustomerPaymentRepositoryImpl
import com.sucharu.sucharupro.data.repository.customersettlement.CustomerPaymentAllocationRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customer.Customer
import com.sucharu.sucharupro.domain.model.customer.CustomerStatusType
import com.sucharu.sucharupro.domain.model.customer.CustomerType
import com.sucharu.sucharupro.domain.model.customercreditcontrol.CustomerCreditCheckRequest
import com.sucharu.sucharupro.domain.model.customercreditcontrol.CustomerCreditRiskStatus
import com.sucharu.sucharupro.domain.model.customercreditcontrol.CustomerPaymentTermsType
import com.sucharu.sucharupro.domain.model.customercreditcontrol.ReceivableAgingBucket
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccount
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccountStatus
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoice
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoiceStatus
import com.sucharu.sucharupro.domain.service.customercreditcontrol.CustomerCreditControlServiceImpl
import com.sucharu.sucharupro.domain.service.customersettlement.CustomerSettlementServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class CustomerCreditControlServiceTest {

    private lateinit var service: CustomerCreditControlServiceImpl
    private lateinit var customerRepo: CustomerRepositoryImpl
    private lateinit var invoiceRepo: CustomerInvoiceRepositoryImpl
    private lateinit var accountRepo: CustomerFinancialAccountRepositoryImpl

    private val tenantId = "TENANT-SRV-01"
    private val projectId = "PRJ-SRV-01"
    private val customerId = "CUS-SRV-01"
    private val accountId = "CFA-SRV-01"

    @Before
    fun setup() {
        val customerDs = FakeCustomerDataSource()
        customerRepo = CustomerRepositoryImpl(customerDs)
        val accountDs = FakeCustomerFinancialAccountDataSource()
        accountRepo = CustomerFinancialAccountRepositoryImpl(accountDs)
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

        val settlementService = CustomerSettlementServiceImpl(
            allocationRepo, paymentRepo, invoiceRepo, accountRepo, creditRepo
        )

        service = CustomerCreditControlServiceImpl(
            creditControlRepo, customerRepo, accountRepo, settlementService, invoiceRepo
        )

        runBlocking {
            customerRepo.addCustomer(
                Customer(
                    customerId = customerId,
                    customerCode = "CUS-01",
                    displayName = "Test Customer",
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
                    accountNumber = "CFA-01",
                    status = CustomerFinancialAccountStatus.ACTIVE
                )
            )
        }
    }

    @Test
    fun testUpdateCreditProfileAndCreditCheckApproval() = runBlocking {
        // Set credit limit 100,000
        val updateRes = service.updateCreditProfile(
            tenantId = tenantId,
            projectId = projectId,
            customerId = customerId,
            creditLimit = BigDecimal("100000.0000"),
            currency = "BDT",
            paymentTermsType = CustomerPaymentTermsType.NET_30,
            creditDays = 30,
            requiresAdvance = false,
            notes = "Corporate client",
            actorId = "staff_01",
            actorRole = "STAFF",
            reason = "Approved 100k credit"
        )
        assertTrue(updateRes is DomainResult.Success)

        // Evaluate credit request of 40,000 (should be APPROVED)
        val checkRes = service.evaluateCredit(
            tenantId = tenantId,
            projectId = projectId,
            request = CustomerCreditCheckRequest(
                customerId = customerId,
                requestedExposure = BigDecimal("40000.0000")
            )
        )
        assertTrue(checkRes is DomainResult.Success)
        val result = (checkRes as DomainResult.Success).data
        assertTrue(result.approved)
        assertEquals(BigDecimal("100000.0000"), result.creditLimit)
        assertEquals(BigDecimal("40000.0000"), result.projectedExposure)
        assertEquals(CustomerCreditRiskStatus.NORMAL, result.riskStatus)
    }

    @Test
    fun testCreditCheckRejectionWhenExceedingLimit() = runBlocking {
        // Set credit limit 50,000
        service.updateCreditProfile(
            tenantId = tenantId,
            projectId = projectId,
            customerId = customerId,
            creditLimit = BigDecimal("50000.0000"),
            currency = "BDT",
            paymentTermsType = CustomerPaymentTermsType.NET_15,
            creditDays = 15,
            requiresAdvance = false,
            notes = null,
            actorId = "staff_01",
            actorRole = "STAFF",
            reason = "Approved 50k credit"
        )

        // Existing invoice of 30,000 due
        invoiceRepo.createInvoice(
            CustomerInvoice(
                invoiceId = "INV-01",
                tenantId = tenantId,
                projectId = projectId,
                customerId = customerId,
                customerFinancialAccountId = accountId,
                invoiceNumber = "INV-01",
                grandTotal = BigDecimal("30000.0000"),
                dueAmount = BigDecimal("30000.0000"),
                status = CustomerInvoiceStatus.ISSUED
            )
        )

        // Request 30,000 (Current 30,000 + Request 30,000 = 60,000 > 50,000 -> REJECT)
        val checkRes = service.evaluateCredit(
            tenantId = tenantId,
            projectId = projectId,
            request = CustomerCreditCheckRequest(
                customerId = customerId,
                requestedExposure = BigDecimal("30000.0000")
            )
        )
        assertTrue(checkRes is DomainResult.Success)
        val result = (checkRes as DomainResult.Success).data
        assertFalse(result.approved)
        assertEquals("CREDIT_LIMIT_EXCEEDED", result.failureCode)
    }

    @Test
    fun testFinancialHoldLifecycle() = runBlocking {
        // Place financial hold
        val holdRes = service.placeFinancialHold(
            tenantId, projectId, customerId, "Disputed payment check", "manager_01", "MANAGER"
        )
        assertTrue(holdRes is DomainResult.Success)
        val profileOnHold = (holdRes as DomainResult.Success).data
        assertTrue(profileOnHold.financialHold)

        // Credit check while on hold must be rejected
        val checkRes = service.evaluateCredit(
            tenantId, projectId, CustomerCreditCheckRequest(customerId, BigDecimal("1000.0000"))
        )
        val checkData = (checkRes as DomainResult.Success).data
        assertFalse(checkData.approved)
        assertEquals("FINANCIAL_HOLD", checkData.failureCode)

        // Release hold
        val relRes = service.releaseFinancialHold(
            tenantId, projectId, customerId, "Dispute resolved", "manager_01", "MANAGER"
        )
        assertTrue(relRes is DomainResult.Success)
        assertFalse((relRes as DomainResult.Success).data.financialHold)
    }

    @Test
    fun testReceivableAgingReportCalculation() = runBlocking {
        val now = System.currentTimeMillis()
        val millisInDay = 86_400_000L

        // Current invoice: due 5 days in future
        invoiceRepo.createInvoice(
            CustomerInvoice(
                invoiceId = "INV-CURR",
                tenantId = tenantId,
                projectId = projectId,
                customerId = customerId,
                customerFinancialAccountId = accountId,
                invoiceNumber = "INV-CURR",
                dueDate = now + 5 * millisInDay,
                grandTotal = BigDecimal("10000.0000"),
                dueAmount = BigDecimal("10000.0000"),
                status = CustomerInvoiceStatus.ISSUED
            )
        )

        // Overdue 10 days
        invoiceRepo.createInvoice(
            CustomerInvoice(
                invoiceId = "INV-OVER-10",
                tenantId = tenantId,
                projectId = projectId,
                customerId = customerId,
                customerFinancialAccountId = accountId,
                invoiceNumber = "INV-OVER-10",
                dueDate = now - 10 * millisInDay,
                grandTotal = BigDecimal("5000.0000"),
                dueAmount = BigDecimal("5000.0000"),
                status = CustomerInvoiceStatus.ISSUED
            )
        )

        // Overdue 40 days
        invoiceRepo.createInvoice(
            CustomerInvoice(
                invoiceId = "INV-OVER-40",
                tenantId = tenantId,
                projectId = projectId,
                customerId = customerId,
                customerFinancialAccountId = accountId,
                invoiceNumber = "INV-OVER-40",
                dueDate = now - 40 * millisInDay,
                grandTotal = BigDecimal("7000.0000"),
                dueAmount = BigDecimal("7000.0000"),
                status = CustomerInvoiceStatus.ISSUED
            )
        )

        val reportRes = service.getReceivableAgingReport(tenantId, projectId, customerId, now)
        assertTrue(reportRes is DomainResult.Success)
        val report = (reportRes as DomainResult.Success).data

        assertEquals(BigDecimal("22000.0000"), report.totalOutstanding)

        val currentBucket = report.buckets.first { it.bucket == ReceivableAgingBucket.CURRENT }
        assertEquals(BigDecimal("10000.0000"), currentBucket.outstandingAmount)
        assertEquals(1, currentBucket.invoiceCount)

        val days8To30Bucket = report.buckets.first { it.bucket == ReceivableAgingBucket.DAYS_8_30 }
        assertEquals(BigDecimal("5000.0000"), days8To30Bucket.outstandingAmount)
        assertEquals(1, days8To30Bucket.invoiceCount)

        val days31To60Bucket = report.buckets.first { it.bucket == ReceivableAgingBucket.DAYS_31_60 }
        assertEquals(BigDecimal("7000.0000"), days31To60Bucket.outstandingAmount)
        assertEquals(1, days31To60Bucket.invoiceCount)
    }
}
