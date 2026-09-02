package com.sucharu.sucharupro.customercollection

import com.sucharu.sucharupro.data.datasource.FakeCustomerDataSource
import com.sucharu.sucharupro.data.datasource.FakeCustomerFinancialAccountDataSource
import com.sucharu.sucharupro.data.datasource.FakeCustomerInvoiceDataSource
import com.sucharu.sucharupro.data.datasource.customercollection.FakeCustomerCollectionDataSource
import com.sucharu.sucharupro.data.datasource.customercredit.FakeCustomerCreditDataSource
import com.sucharu.sucharupro.data.datasource.customercreditcontrol.FakeCustomerCreditControlDataSource
import com.sucharu.sucharupro.data.datasource.customerpayment.FakeCustomerPaymentDataSource
import com.sucharu.sucharupro.data.datasource.customersettlement.FakeCustomerPaymentAllocationDataSource
import com.sucharu.sucharupro.data.repository.CustomerFinancialAccountRepositoryImpl
import com.sucharu.sucharupro.data.repository.CustomerInvoiceRepositoryImpl
import com.sucharu.sucharupro.data.repository.CustomerRepositoryImpl
import com.sucharu.sucharupro.data.repository.customercollection.CustomerCollectionRepositoryImpl
import com.sucharu.sucharupro.data.repository.customercredit.CustomerCreditRepositoryImpl
import com.sucharu.sucharupro.data.repository.customercreditcontrol.CustomerCreditControlRepositoryImpl
import com.sucharu.sucharupro.data.repository.customerpayment.CustomerPaymentRepositoryImpl
import com.sucharu.sucharupro.data.repository.customersettlement.CustomerPaymentAllocationRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customer.Customer
import com.sucharu.sucharupro.domain.model.customer.CustomerStatusType
import com.sucharu.sucharupro.domain.model.customer.CustomerType
import com.sucharu.sucharupro.domain.model.customercollection.*
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccount
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccountStatus
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoice
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoiceStatus
import com.sucharu.sucharupro.domain.service.customercollection.CustomerCollectionServiceImpl
import com.sucharu.sucharupro.domain.service.customercreditcontrol.CustomerCreditControlServiceImpl
import com.sucharu.sucharupro.domain.service.customersettlement.CustomerSettlementServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class CustomerCollectionServiceTest {

    private lateinit var service: CustomerCollectionServiceImpl
    private lateinit var invoiceRepo: CustomerInvoiceRepositoryImpl

    private val tenantId = "TENANT-SRV-01"
    private val projectId = "PRJ-SRV-01"
    private val customerId = "CUS-SRV-01"
    private val accountId = "CFA-SRV-01"

    @Before
    fun setup() {
        val customerDs = FakeCustomerDataSource()
        val customerRepo = CustomerRepositoryImpl(customerDs)
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

        val settlementService = CustomerSettlementServiceImpl(
            allocationRepo, paymentRepo, invoiceRepo, accountRepo, creditRepo
        )
        val creditControlService = CustomerCreditControlServiceImpl(
            creditControlRepo, customerRepo, accountRepo, settlementService, invoiceRepo
        )

        service = CustomerCollectionServiceImpl(
            collectionRepository = collectionRepo,
            customerRepository = customerRepo,
            accountRepository = accountRepo,
            invoiceRepository = invoiceRepo,
            settlementService = settlementService,
            creditControlService = creditControlService
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

            invoiceRepo.createInvoice(
                CustomerInvoice(
                    invoiceId = "INV-01",
                    tenantId = tenantId,
                    projectId = projectId,
                    customerId = customerId,
                    customerFinancialAccountId = accountId,
                    invoiceNumber = "INV-01",
                    grandTotal = BigDecimal("50000.0000"),
                    dueAmount = BigDecimal("50000.0000"),
                    dueDate = System.currentTimeMillis() - (15 * 86400000L), // 15 days overdue
                    status = CustomerInvoiceStatus.ISSUED
                )
            )
        }
    }

    @Test
    fun testCollectionActionLifecycle() = runBlocking {
        // 1. Create collection action
        val createRes = service.createCollectionAction(
            tenantId = tenantId,
            projectId = projectId,
            customerId = customerId,
            invoiceId = "INV-01",
            actionType = CollectionActionType.PHONE_FOLLOW_UP,
            scheduledAt = System.currentTimeMillis() + 86400000,
            assignedUserId = "staff_01",
            notes = "Call customer regarding 15 days overdue",
            actorId = "admin_01",
            actorRole = "ADMIN"
        )
        assertTrue(createRes is DomainResult.Success)
        val action = (createRes as DomainResult.Success).data
        assertEquals(CollectionActionStatus.SCHEDULED, action.status)

        // 2. Reschedule
        val newSchedule = System.currentTimeMillis() + 172800000
        val reschedRes = service.rescheduleAction(
            tenantId, projectId, action.actionId, newSchedule, null, "Customer requested call back later", "staff_01", "STAFF"
        )
        assertTrue(reschedRes is DomainResult.Success)
        assertEquals(newSchedule, (reschedRes as DomainResult.Success).data.scheduledAt)

        // 3. Complete with outcome
        val completeRes = service.completeAction(
            tenantId, projectId, action.actionId, CollectionOutcomeType.PAYMENT_PROMISED, "Customer promised check next week", null, "staff_01", "STAFF"
        )
        assertTrue(completeRes is DomainResult.Success)
        val completed = (completeRes as DomainResult.Success).data
        assertEquals(CollectionActionStatus.COMPLETED, completed.status)
        assertEquals(CollectionOutcomeType.PAYMENT_PROMISED, completed.outcome)
    }

    @Test
    fun testPaymentPromiseCreation() = runBlocking {
        val promiseRes = service.createPaymentPromise(
            tenantId = tenantId,
            projectId = projectId,
            customerId = customerId,
            invoiceId = "INV-01",
            promisedAmount = BigDecimal("30000.0000"),
            promisedDate = System.currentTimeMillis() + 86400000,
            notes = "Partial payment promise",
            actorId = "staff_01",
            actorRole = "STAFF"
        )
        assertTrue(promiseRes is DomainResult.Success)
        val promise = (promiseRes as DomainResult.Success).data
        assertEquals(BigDecimal("30000.0000"), promise.promisedAmount)
        assertEquals(PaymentPromiseStatus.PENDING, promise.status)
    }

    @Test
    fun testReceivableDueScheduleAndCollectionSummary() = runBlocking {
        val scheduleRes = service.getReceivableDueSchedule(tenantId, projectId, customerId)
        assertTrue(scheduleRes is DomainResult.Success)
        val schedule = (scheduleRes as DomainResult.Success).data
        assertEquals(1, schedule.size)
        assertEquals(15, schedule[0].daysOverdue)

        val summaryRes = service.getCustomerCollectionSummary(tenantId, projectId, customerId)
        assertTrue(summaryRes is DomainResult.Success)
        val summary = (summaryRes as DomainResult.Success).data
        assertEquals(BigDecimal("50000.0000"), summary.totalOutstanding)
        assertEquals(BigDecimal("50000.0000"), summary.overdueAmount)
        assertEquals(1, summary.overdueInvoiceCount)
    }
}
