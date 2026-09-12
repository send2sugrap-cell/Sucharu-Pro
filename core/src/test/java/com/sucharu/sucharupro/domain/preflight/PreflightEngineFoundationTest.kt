package com.sucharu.sucharupro.domain.preflight

import com.sucharu.sucharupro.data.datasource.preflight.FakePreflightDataSource
import com.sucharu.sucharupro.data.repository.preflight.PreflightRepositoryImpl
import com.sucharu.sucharupro.domain.engine.preflight.PreflightEngineImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.production.ProductionStageType
import com.sucharu.sucharupro.domain.service.preflight.PreflightServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class PreflightEngineFoundationTest {

    private lateinit var dataSource: FakePreflightDataSource
    private lateinit var repository: PreflightRepositoryImpl
    private lateinit var registry: PreflightRuleRegistry
    private lateinit var engine: PreflightEngineImpl
    private lateinit var service: PreflightServiceImpl

    private val tenantId = "TENANT-001"
    private val artworkId = "ARTWORK-101"

    // Dummy test rule for foundation verification
    class DummyPassRule(override val ruleCode: String = "RULE_001_DUMMY_PASS") : PreflightRule {
        override val ruleId: String = "R-001"
        override val ruleName: String = "Dummy Pass Rule"
        override val category: PreflightRuleCategory = PreflightRuleCategory.DOCUMENT
        override val defaultSeverity: PreflightRuleSeverity = PreflightRuleSeverity.INFO

        override fun isApplicable(context: PreflightExecutionContext): Boolean = true
        override suspend fun execute(context: PreflightExecutionContext): PreflightRuleExecutionResult {
            return PreflightRuleExecutionResult(
                result = PreflightExecutionResult.PASS,
                findings = emptyList()
            )
        }
    }

    class DummyWarningRule(override val ruleCode: String = "RULE_002_DUMMY_WARN") : PreflightRule {
        override val ruleId: String = "R-002"
        override val ruleName: String = "Dummy Warning Rule"
        override val category: PreflightRuleCategory = PreflightRuleCategory.SPECIFICATION
        override val defaultSeverity: PreflightRuleSeverity = PreflightRuleSeverity.WARNING

        override fun isApplicable(context: PreflightExecutionContext): Boolean = true
        override suspend fun execute(context: PreflightExecutionContext): PreflightRuleExecutionResult {
            return PreflightRuleExecutionResult(
                result = PreflightExecutionResult.WARNING,
                findings = listOf(
                    PreflightFinding(
                        findingId = "F-001",
                        tenantId = context.tenantId,
                        preflightRunId = "",
                        ruleCode = ruleCode,
                        category = category,
                        severity = PreflightRuleSeverity.WARNING,
                        message = "Dummy warning finding"
                    )
                )
            )
        }
    }

    @Before
    fun setUp() {
        dataSource = FakePreflightDataSource()
        repository = PreflightRepositoryImpl(dataSource)
        registry = PreflightRuleRegistry()
        engine = PreflightEngineImpl(registry, repository)
        service = PreflightServiceImpl(engine, repository)
    }

    @Test
    fun test01_ruleRegistry_duplicateRuleRegistration_fails() {
        val rule1 = DummyPassRule("RULE_DUP_01")
        val rule2 = DummyPassRule("RULE_DUP_01")

        registry.registerRule(rule1)
        try {
            registry.registerRule(rule2)
            fail("Expected IllegalArgumentException on duplicate rule registration.")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("already registered") == true)
        }
    }

    @Test
    fun test02_resultAggregator_logicVerification() {
        val mockRunId = "RUN-AGG-01"
        val mockExecPass = PreflightRuleExecution(
            executionId = "E-1", tenantId = tenantId, preflightRunId = mockRunId,
            ruleId = "R-1", ruleCode = "CODE1", result = PreflightExecutionResult.PASS
        )
        val mockExecWarn = PreflightRuleExecution(
            executionId = "E-2", tenantId = tenantId, preflightRunId = mockRunId,
            ruleId = "R-2", ruleCode = "CODE2", result = PreflightExecutionResult.WARNING
        )
        val mockFindingWarn = PreflightFinding(
            findingId = "F-1", tenantId = tenantId, preflightRunId = mockRunId,
            ruleCode = "CODE2", category = PreflightRuleCategory.DOCUMENT,
            severity = PreflightRuleSeverity.WARNING, message = "Warning"
        )
        val mockFindingErr = PreflightFinding(
            findingId = "F-2", tenantId = tenantId, preflightRunId = mockRunId,
            ruleCode = "CODE3", category = PreflightRuleCategory.SPECIFICATION,
            severity = PreflightRuleSeverity.ERROR, message = "Error"
        )

        // Empty -> NOT_EVALUATED
        assertEquals(PreflightOverallResult.NOT_EVALUATED, PreflightResultAggregator.aggregate(emptyList(), emptyList()))

        // Pass only -> PASS
        assertEquals(PreflightOverallResult.PASS, PreflightResultAggregator.aggregate(listOf(mockExecPass), emptyList()))

        // Warnings present -> WARNING
        assertEquals(PreflightOverallResult.WARNING, PreflightResultAggregator.aggregate(listOf(mockExecWarn), listOf(mockFindingWarn)))

        // Errors present -> ERROR
        assertEquals(PreflightOverallResult.ERROR, PreflightResultAggregator.aggregate(listOf(mockExecPass, mockExecWarn), listOf(mockFindingWarn, mockFindingErr)))
    }

    @Test
    fun test03_engineExecution_passAndWarningRules() = runBlocking {
        registry.registerRule(DummyPassRule())
        registry.registerRule(DummyWarningRule())

        val context = PreflightExecutionContext(
            tenantId = tenantId,
            artworkId = artworkId,
            jobId = "JOB-101"
        )

        val result = service.runPreflight(context, "STAFF-01")
        assertTrue(result is DomainResult.Success)
        val run = (result as DomainResult.Success).data

        assertEquals(PreflightRunStatus.COMPLETED, run.status)
        assertEquals(PreflightOverallResult.WARNING, run.overallResult)
        assertNotNull(run.completedAt)

        // Verify findings
        val findingsRes = service.listFindingsForRun(tenantId, run.preflightRunId)
        assertTrue(findingsRes is DomainResult.Success)
        val findings = (findingsRes as DomainResult.Success).data
        assertEquals(1, findings.size)
        assertEquals("RULE_002_DUMMY_WARN", findings.first().ruleCode)
    }

    @Test
    fun test04_idempotency_returnsExistingRun() = runBlocking {
        registry.registerRule(DummyPassRule())

        val context = PreflightExecutionContext(
            tenantId = tenantId,
            artworkId = artworkId
        )

        val res1 = service.runPreflight(context, "STAFF-01", idempotencyKey = "IDEMP-KEY-999")
        assertTrue(res1 is DomainResult.Success)
        val run1 = (res1 as DomainResult.Success).data

        val res2 = service.runPreflight(context, "STAFF-01", idempotencyKey = "IDEMP-KEY-999")
        assertTrue(res2 is DomainResult.Success)
        val run2 = (res2 as DomainResult.Success).data

        assertEquals(run1.preflightRunId, run2.preflightRunId)
    }

    @Test
    fun test05_canonicalProductionStageWorkflow_regressionCheck() {
        val expectedSequence = listOf(
            ProductionStageType.DESIGN,
            ProductionStageType.APPROVAL,
            ProductionStageType.QC,
            ProductionStageType.ITEM_APPROVAL,
            ProductionStageType.CTP,
            ProductionStageType.PRINTING,
            ProductionStageType.LAMINATION,
            ProductionStageType.FOLDING,
            ProductionStageType.BINDING,
            ProductionStageType.FINAL_QC,
            ProductionStageType.PACKAGING,
            ProductionStageType.READY,
            ProductionStageType.DELIVERED
        )
        assertEquals(13, ProductionStageType.entries.size)
        assertEquals(expectedSequence, ProductionStageType.entries)
    }
}
