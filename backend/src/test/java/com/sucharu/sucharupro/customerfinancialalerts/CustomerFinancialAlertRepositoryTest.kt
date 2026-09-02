package com.sucharu.sucharupro.customerfinancialalerts

import com.sucharu.sucharupro.data.datasource.customerfinancialreporting.FakeCustomerFinancialAlertDataSource
import com.sucharu.sucharupro.data.datasource.customerfinancialreporting.FakeCustomerFinancialReportScheduleDataSource
import com.sucharu.sucharupro.data.repository.customerfinancialreporting.CustomerFinancialAlertRepositoryImpl
import com.sucharu.sucharupro.data.repository.customerfinancialreporting.CustomerFinancialReportScheduleRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customerfinancialreporting.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class CustomerFinancialAlertRepositoryTest {

    private lateinit var alertRepo: CustomerFinancialAlertRepositoryImpl
    private lateinit var scheduleRepo: CustomerFinancialReportScheduleRepositoryImpl
    private val tenantId = "TENANT-1"
    private val projectId = "PRJ-1"
    private val customerId = "CUS-1"

    @Before
    fun setup() {
        val alertDs = FakeCustomerFinancialAlertDataSource()
        alertRepo = CustomerFinancialAlertRepositoryImpl(alertDs)
        val scheduleDs = FakeCustomerFinancialReportScheduleDataSource()
        scheduleRepo = CustomerFinancialReportScheduleRepositoryImpl(scheduleDs)
    }

    @Test
    fun testAlertSaveRetrieveAndDedup() = runBlocking {
        val dedupKey = CustomerFinancialAlert.buildDeduplicationKey(
            tenantId, projectId, customerId, CustomerFinancialAlertType.INVOICE_OVERDUE, "INVOICE", "INV-1"
        )
        val alert = CustomerFinancialAlert(
            alertId = "ALT-1",
            tenantId = tenantId,
            projectId = projectId,
            customerId = customerId,
            alertType = CustomerFinancialAlertType.INVOICE_OVERDUE,
            severity = CustomerFinancialAlertSeverity.HIGH,
            title = "Overdue",
            safeMessage = "Invoice is overdue",
            sourceType = "INVOICE",
            sourceId = "INV-1",
            deduplicationKey = dedupKey
        )

        val saveRes = alertRepo.saveAlert(alert)
        assertTrue(saveRes is DomainResult.Success)

        val retrievedRes = alertRepo.getAlertById(tenantId, projectId, "ALT-1")
        assertTrue(retrievedRes is DomainResult.Success)
        assertEquals("ALT-1", (retrievedRes as DomainResult.Success).data?.alertId)

        val dedupRes = alertRepo.getActiveAlertByDedupKey(tenantId, projectId, dedupKey)
        assertTrue(dedupRes is DomainResult.Success)
        assertEquals("ALT-1", (dedupRes as DomainResult.Success).data?.alertId)

        // List
        val listRes = alertRepo.listAlerts(tenantId, projectId, customerId)
        assertTrue(listRes is DomainResult.Success)
        assertEquals(1, (listRes as DomainResult.Success).data.size)
    }

    @Test
    fun testScheduleSaveRetrieveAndDueQuery() = runBlocking {
        val now = System.currentTimeMillis()
        val schedule = CustomerFinancialReportSchedule(
            scheduleId = "SCH-1",
            tenantId = tenantId,
            projectId = projectId,
            customerId = customerId,
            reportType = CustomerFinancialReportType.CUSTOMER_STATEMENT,
            frequency = CustomerFinancialScheduleFrequency.DAILY,
            timezone = "Asia/Dhaka",
            nextRunAt = now - 1000L,
            createdBy = "admin"
        )

        val saveRes = scheduleRepo.saveSchedule(schedule)
        assertTrue(saveRes is DomainResult.Success)

        val dueRes = scheduleRepo.getDueSchedules(tenantId, projectId, now)
        assertTrue(dueRes is DomainResult.Success)
        assertEquals(1, (dueRes as DomainResult.Success).data.size)
    }
}
