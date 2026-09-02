package com.sucharu.sucharupro.data.observability

import com.sucharu.sucharupro.data.observability.postgres.InMemoryOperationalAlertRepository
import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.domain.observability.AlertSeverity
import com.sucharu.sucharupro.domain.observability.AlertStatus
import com.sucharu.sucharupro.domain.observability.OperationalAlert
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Persistence, tenant-scoping, and alert resolution test suite (INFRA-04 Step 09).
 */
class OperationalPersistenceSecurityTest {

    private lateinit var alertRepo: InMemoryOperationalAlertRepository
    private val tenantA = TenantContext("tenant-A")
    private val tenantB = TenantContext("tenant-B")

    @Before
    fun setUp() {
        alertRepo = InMemoryOperationalAlertRepository()
    }

    @Test
    fun test01_alertPersistence_tenantIsolation() = runBlocking {
        val alertA = OperationalAlert(
            alertId = "alt-01",
            projectId = "tenant-A",
            alertKey = "KEY_A",
            deduplicationKey = "tenant-A:SUB:KEY_A",
            title = "Alert A",
            summary = "Summary A",
            severity = AlertSeverity.WARNING,
            subsystem = "SUB"
        )
        alertRepo.saveAlert(alertA, tenantA)

        val alertB = OperationalAlert(
            alertId = "alt-02",
            projectId = "tenant-B",
            alertKey = "KEY_B",
            deduplicationKey = "tenant-B:SUB:KEY_B",
            title = "Alert B",
            summary = "Summary B",
            severity = AlertSeverity.CRITICAL,
            subsystem = "SUB"
        )
        alertRepo.saveAlert(alertB, tenantB)

        val listA = alertRepo.listActiveAlerts(tenantA)
        assertEquals(1, listA.size)
        assertEquals("tenant-A", listA.first().projectId)

        val listB = alertRepo.listActiveAlerts(tenantB)
        assertEquals(1, listB.size)
        assertEquals("tenant-B", listB.first().projectId)
    }

    @Test
    fun test02_resolveAlert_updatesStatus() = runBlocking {
        val alertA = OperationalAlert(
            alertId = "alt-res-01",
            projectId = "tenant-A",
            alertKey = "KEY_RES",
            deduplicationKey = "tenant-A:SUB:KEY_RES",
            title = "Alert Res",
            summary = "Summary",
            severity = AlertSeverity.WARNING,
            subsystem = "SUB"
        )
        alertRepo.saveAlert(alertA, tenantA)

        val resolved = alertRepo.resolveAlert("alt-res-01", "Fixed manually", tenantA)
        assertTrue(resolved)

        val activeList = alertRepo.listActiveAlerts(tenantA)
        assertEquals(0, activeList.size)
    }
}
