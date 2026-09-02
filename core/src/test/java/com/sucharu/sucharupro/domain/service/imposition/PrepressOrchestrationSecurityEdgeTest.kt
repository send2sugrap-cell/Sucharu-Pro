package com.sucharu.sucharupro.domain.service.imposition

import com.sucharu.sucharupro.data.datasource.imposition.FakePrepressOrchestrationDataSource
import com.sucharu.sucharupro.data.repository.imposition.PrepressOrchestrationRepositoryImpl
import com.sucharu.sucharupro.domain.model.imposition.PrepressPlanStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Security, Multi-Tenant Isolation & Edge Case Tests for Prepress Orchestration.
 * Module 18 Step 06.
 */
class PrepressOrchestrationSecurityEdgeTest {

    private val tenantA = "tenant_alpha_18"
    private val tenantB = "tenant_bravo_18"
    private lateinit var dataSource: FakePrepressOrchestrationDataSource
    private lateinit var repository: PrepressOrchestrationRepositoryImpl
    private lateinit var service: PrepressOrchestrationServiceImpl

    @Before
    fun setUp() {
        dataSource = FakePrepressOrchestrationDataSource()
        repository = PrepressOrchestrationRepositoryImpl(dataSource)
        service = PrepressOrchestrationServiceImpl(orchestrationRepository = repository)
    }

    @Test
    fun testMultiTenantIsolation_NoCrossTenantAccess() = runBlocking {
        val planA = service.orchestrateAndSavePlan(
            tenantId = tenantA,
            planName = "Tenant A Plan",
            jobId = "JOB-A-01",
            orderId = "ORD-A-01",
            orderItemId = "ITEM-01",
            productName = "Product A",
            requiredQuantity = 1000L,
            actor = "user_a"
        )

        // Tenant B attempting to access Tenant A's plan
        val crossTenantPlan = service.getPlan(tenantB, planA.planId)
        assertNull("Tenant B must not see Tenant A's prepress orchestration plan.", crossTenantPlan)

        val listTenantB = service.listPlansByJob(tenantB, "JOB-A-01")
        assertTrue("Tenant B must not see Tenant A's plans via job query.", listTenantB.isEmpty())
    }

    @Test
    fun testBlankTenantId_ThrowsException() {
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                service.orchestrateAndSavePlan(
                    tenantId = "",
                    planName = "Invalid Plan",
                    jobId = "JOB-01",
                    orderId = "ORD-01",
                    orderItemId = "ITEM-01",
                    productName = "Product",
                    requiredQuantity = 1000L,
                    actor = "user"
                )
            }
        }
    }

    @Test
    fun testNegativeOrZeroQuantity_ThrowsException() {
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                service.orchestrateAndSavePlan(
                    tenantId = tenantA,
                    planName = "Zero Quantity Plan",
                    jobId = "JOB-01",
                    orderId = "ORD-01",
                    orderItemId = "ITEM-01",
                    productName = "Product",
                    requiredQuantity = 0L,
                    actor = "user"
                )
            }
        }
    }
}
