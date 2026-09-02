package com.sucharu.sucharupro.data.workflow

import com.sucharu.sucharupro.data.event.MockPostgresEventDatabase
import com.sucharu.sucharupro.data.job.postgres.PostgresJobRepository
import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.data.workflow.integration.EventToWorkflowTrigger
import com.sucharu.sucharupro.data.workflow.integration.WorkflowJobStepAdapter
import com.sucharu.sucharupro.data.workflow.postgres.PostgresWorkflowDefinitionRepository
import com.sucharu.sucharupro.data.workflow.postgres.PostgresWorkflowInstanceRepository
import com.sucharu.sucharupro.domain.event.model.EventActor
import com.sucharu.sucharupro.domain.event.model.EventEnvelope
import com.sucharu.sucharupro.domain.event.model.events.OrderCreatedEvent
import com.sucharu.sucharupro.domain.workflow.engine.OrchestratorResult
import com.sucharu.sucharupro.domain.workflow.engine.WorkflowOrchestrator
import com.sucharu.sucharupro.domain.workflow.model.WorkflowDefinition
import com.sucharu.sucharupro.domain.workflow.model.WorkflowInstance
import com.sucharu.sucharupro.domain.workflow.model.WorkflowStatus
import com.sucharu.sucharupro.domain.workflow.model.WorkflowStepDefinition
import com.sucharu.sucharupro.domain.workflow.model.WorkflowStepType
import com.sucharu.sucharupro.domain.workflow.model.WorkflowVersion
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class WorkflowOrchestratorAndEventBridgeTest {

    private lateinit var mockDb: MockPostgresEventDatabase
    private lateinit var defRepo: PostgresWorkflowDefinitionRepository
    private lateinit var instanceRepo: PostgresWorkflowInstanceRepository
    private lateinit var jobRepo: PostgresJobRepository
    private lateinit var orchestrator: WorkflowOrchestrator
    private lateinit var eventTrigger: EventToWorkflowTrigger
    private lateinit var jobStepAdapter: WorkflowJobStepAdapter

    @Before
    fun setUp() {
        mockDb = MockPostgresEventDatabase()
        defRepo = PostgresWorkflowDefinitionRepository(mockDb)
        instanceRepo = PostgresWorkflowInstanceRepository(mockDb)
        jobRepo = PostgresJobRepository(mockDb)
        orchestrator = WorkflowOrchestrator()
        eventTrigger = EventToWorkflowTrigger(defRepo, instanceRepo, orchestrator)
        jobStepAdapter = WorkflowJobStepAdapter(jobRepo)
    }

    @Test
    fun testEventToWorkflowTriggerCreatesRunningInstance() {
        runBlocking {
            val tenant = TenantContext("tenant_alpha")

            val step1 = WorkflowStepDefinition(
                stepId = "step-validate",
                definitionId = "def-order",
                versionId = "v1",
                projectId = "tenant_alpha",
                stepName = "Validate Specs",
                stepType = WorkflowStepType.ACTION,
                sequenceOrder = 1
            )
            val step2 = WorkflowStepDefinition(
                stepId = "step-plate",
                definitionId = "def-order",
                versionId = "v1",
                projectId = "tenant_alpha",
                stepName = "Create Plate",
                stepType = WorkflowStepType.JOB,
                sequenceOrder = 2,
                config = mapOf("jobType" to "production.laser_plate")
            )

            defRepo.saveDefinition(
                WorkflowDefinition(
                    definitionId = "def-order",
                    projectId = "tenant_alpha",
                    workflowName = "Order Workflow",
                    createdBy = "admin-1"
                ),
                tenant
            )

            defRepo.saveVersion(
                WorkflowVersion(
                    definitionId = "def-order",
                    projectId = "tenant_alpha",
                    versionId = "v1",
                    steps = listOf(step1, step2),
                    publishedBy = "admin-1"
                ),
                tenant
            )

            val envelope = EventEnvelope.create(
                payload = OrderCreatedEvent(
                    orderId = "ord-555",
                    customerId = "cust-1",
                    totalAmount = BigDecimal("15000.00"),
                    itemCount = 10
                ),
                projectId = "tenant_alpha",
                actor = EventActor.human("user-staff")
            )

            val workflowId = eventTrigger.triggerWorkflowFromEvent(
                envelope = envelope,
                definitionId = "def-order",
                versionId = "v1",
                tenantContext = tenant
            )

            assertNotNull(workflowId)

            val instance = instanceRepo.getInstanceById(workflowId!!, tenant)
            assertNotNull(instance)
            assertEquals(WorkflowStatus.RUNNING, instance?.status)
            assertEquals("step-validate", instance?.currentStepId)
        }
    }

    @Test
    fun testOrchestratorAdvancesStepsAndTransitionsToCompleted() {
        runBlocking {
            val tenant = TenantContext("tenant_alpha")

            val step1 = WorkflowStepDefinition(
                stepId = "step-1",
                definitionId = "def-simple",
                versionId = "v1",
                projectId = "tenant_alpha",
                stepName = "Step 1",
                stepType = WorkflowStepType.ACTION,
                sequenceOrder = 1
            )
            val step2 = WorkflowStepDefinition(
                stepId = "step-2",
                definitionId = "def-simple",
                versionId = "v1",
                projectId = "tenant_alpha",
                stepName = "Step 2",
                stepType = WorkflowStepType.ACTION,
                sequenceOrder = 2
            )

            val version = WorkflowVersion(
                definitionId = "def-simple",
                projectId = "tenant_alpha",
                versionId = "v1",
                steps = listOf(step1, step2),
                publishedBy = "admin-1"
            )

            val instance = WorkflowInstance(
                workflowId = "wf-simple-1",
                projectId = "tenant_alpha",
                definitionId = "def-simple",
                versionId = "v1",
                actorId = "u1"
            )

            val (started, firstStep) = orchestrator.startWorkflow(instance, version)
            assertEquals("step-1", firstStep)
            assertEquals(WorkflowStatus.RUNNING, started.status)

            // Advance step 1 -> step 2
            val res1 = orchestrator.advanceWorkflow(started, version, tenantContext = tenant)
            assertTrue(res1 is OrchestratorResult.Advanced)
            val advanced = (res1 as OrchestratorResult.Advanced).instance
            assertEquals("step-2", advanced.currentStepId)

            // Advance step 2 -> Completed
            val res2 = orchestrator.advanceWorkflow(advanced, version, tenantContext = tenant)
            assertTrue(res2 is OrchestratorResult.Completed)
            val completed = (res2 as OrchestratorResult.Completed).instance
            assertEquals(WorkflowStatus.COMPLETED, completed.status)
        }
    }

    @Test
    fun testJobStepAdapterEnqueuesBackgroundJob() {
        runBlocking {
            val tenant = TenantContext("tenant_alpha")

            val step = WorkflowStepDefinition(
                stepId = "step-job-1",
                definitionId = "def-job",
                versionId = "v1",
                projectId = "tenant_alpha",
                stepName = "Async PDF Render",
                stepType = WorkflowStepType.JOB,
                sequenceOrder = 1
            )

            val instance = WorkflowInstance(
                workflowId = "wf-job-10",
                projectId = "tenant_alpha",
                definitionId = "def-job",
                versionId = "v1",
                actorId = "u1"
            )

            val jobId = jobStepAdapter.enqueueStepJob(
                step = step,
                instance = instance,
                jobType = "pdf.render_plates",
                payloadJson = "{\"dpi\":300}",
                tenantContext = tenant
            )

            assertNotNull(jobId)

            val job = jobRepo.getJobById(jobId, tenant)
            assertNotNull(job)
            assertEquals("pdf.render_plates", job?.jobType)
            assertEquals("wf-job-10", job?.metadata?.get("workflowId"))
        }
    }
}
