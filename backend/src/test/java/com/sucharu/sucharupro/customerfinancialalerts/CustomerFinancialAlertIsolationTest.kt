package com.sucharu.sucharupro.customerfinancialalerts

import com.sucharu.sucharupro.data.datasource.customerfinancialreporting.FakeCustomerFinancialAlertDataSource
import com.sucharu.sucharupro.data.datasource.customerfinancialreporting.FakeCustomerFinancialReportScheduleDataSource
import com.sucharu.sucharupro.data.repository.customerfinancialreporting.CustomerFinancialAlertRepositoryImpl
import com.sucharu.sucharupro.data.repository.customerfinancialreporting.CustomerFinancialReportScheduleRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customerfinancialreporting.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CustomerFinancialAlertIsolationTest {

    private lateinit var alertRepo: CustomerFinancialAlertRepositoryImpl
    private lateinit var scheduleRepo: CustomerFinancialReportScheduleRepositoryImpl
    private val tenantA = "TENANT-AAA"
    private val tenantB = "TENANT-BBB"

    @Before
    fun setup() {
        val alertDs = FakeCustomerFinancialAlertDataSource()
        alertRepo = CustomerFinancialAlertRepositoryImpl(alertDs)
        val scheduleDs = FakeCustomerFinancialReportScheduleDataSource()
        scheduleRepo = CustomerFinancialReportScheduleRepositoryImpl(scheduleDs)

        runBlocking {
            alertRepo.saveAlert(
                CustomerFinancialAlert(
                    alertId = "ALT-A",
                    tenantId = tenantA,
                    projectId = tenantA,
                    customerId = "CUS-A",
                    alertType = CustomerFinancialAlertType.INVOICE_OVERDUE,
                    severity = CustomerFinancialAlertSeverity.HIGH,
                    title = "Overdue A",
                    safeMessage = "Invoice overdue A",
                    sourceType = "INVOICE",
                    sourceId = "INV-A",
                    deduplicationKey = "dedup_A"
                )
            )

            alertRepo.saveAlert(
                CustomerFinancialAlert(
                    alertId = "ALT-B",
                    tenantId = tenantB,
                    projectId = tenantB,
                    customerId = "CUS-B",
                    alertType = CustomerFinancialAlertType.INVOICE_OVERDUE,
                    severity = CustomerFinancialAlertSeverity.HIGH,
                    title = "Overdue B",
                    safeMessage = "Invoice overdue B",
                    sourceType = "INVOICE",
                    sourceId = "INV-B",
                    deduplicationKey = "dedup_B"
                )
            )

            scheduleRepo.saveSchedule(
                CustomerFinancialReportSchedule(
                    scheduleId = "SCH-A",
                    tenantId = tenantA,
                    projectId = tenantA,
                    customerId = "CUS-A",
                    reportType = CustomerFinancialReportType.CUSTOMER_STATEMENT,
                    frequency = CustomerFinancialScheduleFrequency.DAILY,
                    timezone = "Asia/Dhaka",
                    nextRunAt = 1000L,
                    createdBy = "admin"
                )
            )
        }
    }

    @Test
    fun testTenantIsolationOnAlertsAndSchedules() = runBlocking {
        // Query Tenant A alerts
        val listA = alertRepo.listAlerts(tenantA, tenantA)
        assertTrue(listA is DomainResult.Success)
        val alertsA = (listA as DomainResult.Success).data
        assertEquals(1, alertsA.size)
        assertEquals("ALT-A", alertsA.first().alertId)

        // Query Tenant B alerts
        val listB = alertRepo.listAlerts(tenantB, tenantB)
        assertTrue(listB is DomainResult.Success)
        val alertsB = (listB as DomainResult.Success).data
        assertEquals(1, alertsB.size)
        assertEquals("ALT-B", alertsB.first().alertId)

        // Tenant B cannot retrieve Tenant A alert by ID
        val crossAlert = alertRepo.getAlertById(tenantB, tenantB, "ALT-A")
        assertTrue(crossAlert is DomainResult.Success)
        assertEquals(null, (crossAlert as DomainResult.Success).data)

        // Tenant B cannot retrieve Tenant A schedule by ID
        val crossSchedule = scheduleRepo.getScheduleById(tenantB, tenantB, "SCH-A")
        assertTrue(crossSchedule is DomainResult.Success)
        assertEquals(null, (crossSchedule as DomainResult.Success).data)
    }
}
