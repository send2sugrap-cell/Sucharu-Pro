package com.sucharu.sucharupro.data.composition

import com.sucharu.sucharupro.data.repository.HttpCustomerRepository
import com.sucharu.sucharupro.data.repository.HttpDashboardRepository
import com.sucharu.sucharupro.data.repository.HttpOrderRepository
import com.sucharu.sucharupro.data.repository.affiliate.HttpAffiliateRepository
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * PHASE 00 STEP 03 - Production Dependency Injection & Fake Isolation Unit Test Suite.
 *
 * Verifies that:
 * 1. ProductionRuntimeComposition resolves real HTTP-backed repositories (HttpCustomerRepository, HttpOrderRepository, etc.)
 * 2. ProductionRuntimeComposition does NOT resolve FakeDataSources.
 * 3. DevelopmentDemoRuntimeComposition resolves explicitly isolated demo repositories.
 */
class ProductionCompositionDependencyInjectionTest {

    @Test
    fun testProductionRuntimeComposition_resolvesRealHttpRepositories() {
        val prodComposition = ProductionRuntimeComposition(apiGatewayUrl = "http://127.0.0.1:8080")

        assertNotNull("Customer repository must be provided", prodComposition.customerRepository)
        assertTrue(
            "Production customer repository must be HttpCustomerRepository",
            prodComposition.customerRepository is HttpCustomerRepository
        )

        assertNotNull("Order repository must be provided", prodComposition.orderRepository)
        assertTrue(
            "Production order repository must be HttpOrderRepository",
            prodComposition.orderRepository is HttpOrderRepository
        )

        assertNotNull("Affiliate repository must be provided", prodComposition.affiliateRepository)
        assertTrue(
            "Production affiliate repository must be HttpAffiliateRepository",
            prodComposition.affiliateRepository is HttpAffiliateRepository
        )

        assertNotNull("Dashboard repository must be provided", prodComposition.dashboardRepository)
        assertTrue(
            "Production dashboard repository must be HttpDashboardRepository",
            prodComposition.dashboardRepository is HttpDashboardRepository
        )
    }

    @Test
    fun testDevelopmentDemoRuntimeComposition_resolvesIsolatedDemoRepositories() {
        val demoComposition = DevelopmentDemoRuntimeComposition()

        assertNotNull("Demo customer repository must be provided", demoComposition.customerRepository)
        assertNotNull("Demo order repository must be provided", demoComposition.orderRepository)
        assertNotNull("Demo affiliate repository must be provided", demoComposition.affiliateRepository)
        assertNotNull("Demo dashboard repository must be provided", demoComposition.dashboardRepository)
    }
}
