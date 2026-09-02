package com.sucharu.sucharupro.data.observability

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.PrincipalType
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.observability.alert.OperationalAlertEngine
import com.sucharu.sucharupro.data.observability.capacity.CapacityMonitor
import com.sucharu.sucharupro.data.observability.health.*
import com.sucharu.sucharupro.data.observability.service.AiAgentObservabilityBoundary
import com.sucharu.sucharupro.data.observability.service.OperationalReadResult
import com.sucharu.sucharupro.data.observability.service.OperationalReadService
import com.sucharu.sucharupro.data.observability.slo.SloEngine
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * AI Agent boundary and data-minimization operational test suite (INFRA-04 Step 09).
 */
class AiAgentObservabilitySecurityTest {

    private lateinit var boundary: AiAgentObservabilityBoundary

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
        val readService = OperationalReadService(
            healthAggregator = aggregator,
            alertEngine = OperationalAlertEngine(),
            sloEngine = SloEngine(),
            capacityMonitor = CapacityMonitor()
        )
        boundary = AiAgentObservabilityBoundary(readService)
    }

    @Test
    fun test01_aiAgentReceivesDataMinimizedSummary() {
        val aiAgent = AuthenticatedPrincipal(
            userId = "ai-01",
            projectId = "p-001",
            username = "ai_agent",
            role = UserRole.AI_AGENT,
            principalType = PrincipalType.AI_AGENT
        )
        val result = boundary.getSafeHealthSummary(aiAgent)
        assertTrue("AI Agent should receive safe operational summary", result is OperationalReadResult.Success)
        val summary = (result as OperationalReadResult.Success).data
        assertTrue("System must be marked operational", summary.isSystemOperational)
        assertFalse("Message must not expose internal tables or credentials", summary.highLevelMessage.contains("password"))
    }

    @Test
    fun test02_humanUserDeniedFromAiBoundary() {
        val human = AuthenticatedPrincipal(
            userId = "mgr-01",
            projectId = "p-001",
            username = "human_mgr",
            role = UserRole.MANAGER,
            principalType = PrincipalType.HUMAN
        )
        val result = boundary.getSafeHealthSummary(human)
        assertTrue("Human principal should be denied from AI boundary endpoint", result is OperationalReadResult.Denied)
    }
}
