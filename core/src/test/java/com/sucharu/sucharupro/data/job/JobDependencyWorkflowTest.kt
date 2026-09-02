package com.sucharu.sucharupro.data.job

import com.sucharu.sucharupro.data.event.MockPostgresEventDatabase
import com.sucharu.sucharupro.data.job.postgres.PostgresJobDependencyRepository
import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.domain.job.model.DependencyRequirement
import com.sucharu.sucharupro.domain.job.model.JobDependencyLink
import com.sucharu.sucharupro.domain.job.model.JobStatus
import com.sucharu.sucharupro.domain.job.workflow.DependencyCycleDetector
import com.sucharu.sucharupro.domain.job.workflow.JobDependencyManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class JobDependencyWorkflowTest {

    private lateinit var mockDb: MockPostgresEventDatabase
    private lateinit var dependencyRepo: PostgresJobDependencyRepository
    private lateinit var dependencyManager: JobDependencyManager

    @Before
    fun setUp() {
        mockDb = MockPostgresEventDatabase()
        dependencyRepo = PostgresJobDependencyRepository(mockDb)
        dependencyManager = JobDependencyManager()
    }

    @Test
    fun testDirectCycleDetectionRejectsSelfDependency() {
        val wouldCycle = DependencyCycleDetector.wouldCreateCycle(
            existingEdges = emptyMap(),
            newChildJobId = "job-A",
            newParentJobId = "job-A"
        )
        assertTrue(wouldCycle)
    }

    @Test
    fun testTransitiveCycleDetection() {
        // A depends on B, B depends on C
        val edges = mapOf(
            "job-A" to setOf("job-B"),
            "job-B" to setOf("job-C")
        )

        // Adding: C depends on A -> Cycle A -> B -> C -> A
        val wouldCycle = DependencyCycleDetector.wouldCreateCycle(edges, "job-C", "job-A")
        assertTrue(wouldCycle)

        // Adding: D depends on C -> Valid DAG
        val validDag = DependencyCycleDetector.wouldCreateCycle(edges, "job-D", "job-C")
        assertFalse(validDag)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testJobDependencyManagerRejectsCycle() {
        val existingLinks = listOf(
            JobDependencyLink(jobId = "job-A", dependsOnJobId = "job-B", projectId = "tenant_alpha"),
            JobDependencyLink(jobId = "job-B", dependsOnJobId = "job-C", projectId = "tenant_alpha")
        )

        dependencyManager.validateNoCycle(existingLinks, "job-C", "job-A")
    }

    @Test
    fun testDependencySatisfactionEvaluation() {
        val link1 = JobDependencyLink(
            jobId = "job-child",
            dependsOnJobId = "job-parent-1",
            requirement = DependencyRequirement.ON_SUCCESS,
            projectId = "tenant_alpha"
        )
        val link2 = JobDependencyLink(
            jobId = "job-child",
            dependsOnJobId = "job-parent-2",
            requirement = DependencyRequirement.ON_COMPLETION,
            projectId = "tenant_alpha"
        )

        val dependencies = listOf(link1, link2)

        // Parent 1 succeeded, Parent 2 still running
        val partial = mapOf("job-parent-1" to JobStatus.SUCCEEDED, "job-parent-2" to JobStatus.RUNNING)
        assertFalse(dependencyManager.areAllDependenciesSatisfied(dependencies, partial))

        // Parent 1 succeeded, Parent 2 dead-lettered (terminal)
        val complete = mapOf("job-parent-1" to JobStatus.SUCCEEDED, "job-parent-2" to JobStatus.DEAD_LETTER)
        assertTrue(dependencyManager.areAllDependenciesSatisfied(dependencies, complete))
    }

    @Test
    fun testPostgresJobDependencyRepositoryPersistence() {
        runBlocking {
            val tenant = TenantContext("tenant_alpha")
            val link = JobDependencyLink(
                dependencyId = "dep-1",
                projectId = "tenant_alpha",
                jobId = "child-job",
                dependsOnJobId = "parent-job",
                requirement = DependencyRequirement.ON_SUCCESS
            )

            dependencyRepo.addDependency(link, tenant)

            val deps = dependencyRepo.getDependenciesForJob("child-job", tenant)
            assertEquals(1, deps.size)
            assertEquals("parent-job", deps[0].dependsOnJobId)
            assertFalse(deps[0].isSatisfied)

            dependencyRepo.markDependencySatisfied("dep-1", tenant)

            val updated = dependencyRepo.getDependenciesForJob("child-job", tenant)
            assertTrue(updated[0].isSatisfied)
        }
    }
}
