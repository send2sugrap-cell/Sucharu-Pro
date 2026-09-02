package com.sucharu.sucharupro.customerfinancialalerts

import com.sucharu.sucharupro.data.datasource.customerfinancialreporting.FakeCustomerFinancialAlertDataSource
import com.sucharu.sucharupro.data.repository.customerfinancialreporting.CustomerFinancialAlertRepositoryImpl
import com.sucharu.sucharupro.domain.model.customerfinancialreporting.CustomerFinancialAlertAuditEvent
import com.sucharu.sucharupro.domain.model.customerfinancialreporting.CustomerFinancialAlertEventType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class CustomerFinancialAlertConcurrencyTest {

    @Test
    fun testConcurrentAuditRecordingAndAlertReads() = runBlocking {
        val alertDs = FakeCustomerFinancialAlertDataSource()
        val alertRepo = CustomerFinancialAlertRepositoryImpl(alertDs)
        val tenantId = "TENANT-CONCUR"
        val alertId = "ALT-CONCUR"

        val jobs = (1..50).map { i ->
            async(Dispatchers.Default) {
                alertRepo.recordAuditEvent(
                    CustomerFinancialAlertAuditEvent(
                        tenantId = tenantId,
                        projectId = tenantId,
                        alertId = alertId,
                        eventType = CustomerFinancialAlertEventType.ALERT_CREATED,
                        actorId = "user_$i",
                        actorRole = "STAFF",
                        detailsJson = "{\"seq\":$i}"
                    )
                )
            }
        }
        jobs.awaitAll()

        val listRes = alertRepo.listAuditEvents(tenantId, tenantId, alertId)
        val events = (listRes as com.sucharu.sucharupro.domain.model.common.DomainResult.Success).data
        assertEquals(50, events.size)
    }
}
