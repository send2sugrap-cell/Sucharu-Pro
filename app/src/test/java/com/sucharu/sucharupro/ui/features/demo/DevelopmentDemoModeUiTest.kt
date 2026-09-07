package com.sucharu.sucharupro.ui.features.demo

import com.sucharu.sucharupro.data.composition.ProductionRuntimeComposition
import com.sucharu.sucharupro.data.composition.AppRuntimeMode
import org.junit.Assert.*
import org.junit.Test

/**
 * UI / Composition integration tests for Production Runtime Mode.
 */
class DevelopmentDemoModeUiTest {

    @Test
    fun testProductionComposition_blocksUnconfiguredGateway() {
        val prodComposition = ProductionRuntimeComposition(apiGatewayUrl = null)
        assertEquals(AppRuntimeMode.PRODUCTION, prodComposition.mode)

        assertThrows(IllegalStateException::class.java) {
            prodComposition.createSessionManager()
        }
    }

    @Test
    fun testProductionComposition_initializesWithGateway() {
        val prodComposition = ProductionRuntimeComposition(apiGatewayUrl = "http://127.0.0.1:8080")
        assertEquals(AppRuntimeMode.PRODUCTION, prodComposition.mode)
        assertNotNull(prodComposition.customerRepository)
        assertNotNull(prodComposition.orderRepository)
    }
}
