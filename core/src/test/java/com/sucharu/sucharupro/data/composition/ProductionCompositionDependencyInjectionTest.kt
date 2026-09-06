package com.sucharu.sucharupro.data.composition

import com.sucharu.sucharupro.data.repository.HttpCustomerRepository
import com.sucharu.sucharupro.data.repository.HttpDashboardRepository
import com.sucharu.sucharupro.data.repository.HttpOrderRepository
import com.sucharu.sucharupro.data.repository.affiliate.HttpAffiliateRepository
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * PHASE 00 STEP 03 - Production Dependency Injection Unit Test Suite.
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
}
