package com.sucharu.sucharupro.customerfinancialalerts

import com.sucharu.sucharupro.data.datasource.*
import com.sucharu.sucharupro.data.datasource.customercollection.FakeCustomerCollectionDataSource
import com.sucharu.sucharupro.data.datasource.customercredit.FakeCustomerCreditDataSource
import com.sucharu.sucharupro.data.datasource.customercreditcontrol.FakeCustomerCreditControlDataSource
import com.sucharu.sucharupro.data.datasource.customerfinancialreporting.FakeCustomerFinancialAlertDataSource
import com.sucharu.sucharupro.data.datasource.customerfinancialreporting.FakeCustomerFinancialDocumentDeliveryDataSource
import com.sucharu.sucharupro.data.datasource.customerfinancialreporting.FakeCustomerFinancialReportScheduleDataSource
import com.sucharu.sucharupro.data.datasource.customerledger.FakeCustomerLedgerDataSource
import com.sucharu.sucharupro.data.datasource.customerpayment.FakeCustomerPaymentDataSource
import com.sucharu.sucharupro.data.datasource.customersettlement.FakeCustomerPaymentAllocationDataSource
import com.sucharu.sucharupro.data.repository.*
import com.sucharu.sucharupro.data.repository.customercollection.CustomerCollectionRepositoryImpl
import com.sucharu.sucharupro.data.repository.customercredit.CustomerCreditRepositoryImpl
import com.sucharu.sucharupro.data.repository.customercreditcontrol.CustomerCreditControlRepositoryImpl
import com.sucharu.sucharupro.data.repository.customerfinancialreporting.CustomerFinancialAlertRepositoryImpl
import com.sucharu.sucharupro.data.repository.customerfinancialreporting.CustomerFinancialDocumentDeliveryRepositoryImpl
import com.sucharu.sucharupro.data.repository.customerfinancialreporting.CustomerFinancialReportScheduleRepositoryImpl
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
import com.sucharu.sucharupro.domain.service.customercollection.CustomerCollectionServiceImpl
import com.sucharu.sucharupro.domain.service.customercreditcontrol.CustomerCreditControlServiceImpl
import com.sucharu.sucharupro.domain.service.customerfinancialdashboard.CustomerFinancialDashboardServiceImpl
import com.sucharu.sucharupro.domain.service.customerfinancialreporting.CustomerFinancialDocumentDeliveryServiceImpl
import com.sucharu.sucharupro.domain.service.customerfinancialreporting.CustomerFinancialReportingServiceImpl
import com.sucharu.sucharupro.domain.service.customerfinancialreporting.CustomerFinancialScheduleServiceImpl
import com.sucharu.sucharupro.domain.service.customerledger.CustomerLedgerServiceImpl
import com.sucharu.sucharupro.domain.service.customersettlement.CustomerSettlementServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class CustomerFinancialScheduleServiceTest {

    private lateinit var scheduleService: CustomerFinancialScheduleServiceImpl
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
        val deliveryRepo = CustomerFinancialDocumentDeliveryRepositoryImpl(deliveryDs)
        val deliveryService = CustomerFinancialDocumentDeliveryServiceImpl(
            deliveryRepository = deliveryRepo,
            reportingService = reportingService,
            customerRepository = customerRepo,
            notificationRepository = null
        )

        val scheduleDs = FakeCustomerFinancialReportScheduleDataSource()
        val scheduleRepo = CustomerFinancialReportScheduleRepositoryImpl(scheduleDs)
        val alertDs = FakeCustomerFinancialAlertDataSource()
        val alertRepo = CustomerFinancialAlertRepositoryImpl(alertDs)

        scheduleService = CustomerFinancialScheduleServiceImpl(
            scheduleRepository = scheduleRepo,
            customerRepository = customerRepo,
            documentDeliveryService = deliveryService,
            alertRepository = alertRepo
        )

        runBlocking {
            customerRepo.addCustomer(
                Customer(
                    customerId = customerId,
                    customerCode = "CUS-1",
                    displayName = "Schedule Customer",
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
        }
    }

    @Test
    fun testScheduleLifecycleAndDueExecution() = runBlocking {
        val now = System.currentTimeMillis()

        // 1. Create Schedule
        val createRes = scheduleService.createSchedule(
            tenantId = tenantId,
            projectId = projectId,
            customerId = customerId,
            reportType = CustomerFinancialReportType.CUSTOMER_STATEMENT,
            format = CustomerFinancialReportFormat.CSV,
            frequency = CustomerFinancialScheduleFrequency.DAILY,
            timezone = "Asia/Dhaka",
            firstRunAt = now - 5000L, // Due immediately
            actorId = "admin",
            actorRole = "ADMIN"
        )
        assertTrue(createRes is DomainResult.Success)
        val schedule = (createRes as DomainResult.Success).data
        assertEquals(CustomerFinancialReportScheduleStatus.ACTIVE, schedule.status)

        // 2. Execute Due Schedules
        val execRes = scheduleService.executeDueSchedules(tenantId, projectId, now, "worker_1", "SYSTEM")
        assertTrue(execRes is DomainResult.Success)
        val executions = (execRes as DomainResult.Success).data
        assertEquals(1, executions.size)
        assertEquals(CustomerFinancialScheduleExecutionStatus.SUCCESS, executions.first().status)
        assertNotNull(executions.first().documentDeliveryId)

        // 3. Schedule nextRunAt advanced
        val updatedScheduleRes = scheduleService.getSchedule(tenantId, projectId, schedule.scheduleId)
        assertTrue(updatedScheduleRes is DomainResult.Success)
        val updatedSchedule = (updatedScheduleRes as DomainResult.Success).data
        assertTrue(updatedSchedule.nextRunAt > now)
        assertEquals("SUCCESS", updatedSchedule.lastRunStatus)

        // 4. Pause, Resume, Cancel
        val pauseRes = scheduleService.pauseSchedule(tenantId, projectId, schedule.scheduleId, "admin", "ADMIN")
        assertTrue(pauseRes is DomainResult.Success)
        assertEquals(CustomerFinancialReportScheduleStatus.PAUSED, (pauseRes as DomainResult.Success).data.status)

        val resumeRes = scheduleService.resumeSchedule(tenantId, projectId, schedule.scheduleId, "admin", "ADMIN")
        assertTrue(resumeRes is DomainResult.Success)
        assertEquals(CustomerFinancialReportScheduleStatus.ACTIVE, (resumeRes as DomainResult.Success).data.status)

        val cancelRes = scheduleService.cancelSchedule(tenantId, projectId, schedule.scheduleId, "admin", "ADMIN")
        assertTrue(cancelRes is DomainResult.Success)
        assertEquals(CustomerFinancialReportScheduleStatus.CANCELLED, (cancelRes as DomainResult.Success).data.status)
    }
}
