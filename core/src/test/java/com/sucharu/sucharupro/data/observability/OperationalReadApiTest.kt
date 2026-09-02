package com.sucharu.sucharupro.data.observability

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.PrincipalType
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.observability.alert.OperationalAlertEngine
import com.sucharu.sucharupro.data.observability.capacity.CapacityMonitor
import com.sucharu.sucharupro.data.observability.health.*
import com.sucharu.sucharupro.data.observability.service.OperationalReadResult
import com.sucharu.sucharupro.data.observability.service.OperationalReadService
import com.sucharu.sucharupro.data.observability.slo.SloEngine
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Operational Read API endpoints and authorization integration test suite (INFRA-04 Step 09).
 */
class OperationalReadApiTest {

    private lateinit var readService: OperationalReadService

    private val manager = AuthenticatedPrincipal(
        userId = "mgr-01",
        projectId = "p-001",
        username = "human_mgr",
        role = UserRole.MANAGER,
        principalType = PrincipalType.HUMAN
    )

    @Before
    fun setUp() {
        val aggregator = SystemHealthAggregator(
            EventInfrastructureHealthEvaluator(),
            NotificationHealthEvaluator(),
            BackgroundJobHealthEvaluator(),
            WorkflowHealthEvaluator(),
            AiAgentHealthEvaluator(),
            N8nHealthEvaluator()
        )
        readService = OperationalReadService(
            healthAggregator = aggregator,
            alertEngine = OperationalAlertEngine(),
            sloEngine = SloEngine(),
            capacityMonitor = CapacityMonitor()
        )
    }

    @Test
    fun test01_managerCanGetSystemHealth() {
        val result = readService.getSystemHealth(manager)
        assertTrue("Manager should be able to get system health", result is OperationalReadResult.Success)
    }

    @Test
    fun test02_managerCanGetTenantHealth() {
        val result = readService.getTenantHealth(manager, "p-001")
        assertTrue("Manager should be able to get tenant health", result is OperationalReadResult.Success)
    }

    @Test
    fun test03_managerCannotGetOtherTenantHealth() {
        val result = readService.getTenantHealth(manager, "p-999")
        assertTrue("Manager should be denied access to other tenant's health", result is OperationalReadResult.Denied)
    }

    @Test
    fun test04_managerCanGetSloStatus() {
        val result = readService.getSloStatus(manager)
        assertTrue("Manager should be able to get SLO status", result is OperationalReadResult.Success)
        val measurements = (result as OperationalReadResult.Success).data
        assertTrue(measurements.isNotEmpty())
    }
}
