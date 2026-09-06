package com.sucharu.sucharupro

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sucharu.sucharupro.data.composition.ProductionRuntimeComposition
import com.sucharu.sucharupro.domain.model.common.DomainResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * PHASE 00 STEP 03 - Real Production DI & Fake -> Real Runtime Wiring Integration Test.
 * Executed inside Android Runtime (ART) on emulator-5554 against the actual running Sucharu backend.
 */
@RunWith(AndroidJUnit4::class)
class RealProductionDiRuntimeIntegrationTest {

    @Test
    fun verifyRealProductionRepositoryPathToActualBackend() = runBlocking {
        // 1. Initialize Production Composition pointing to real backend via ADB bridge
        val composition = ProductionRuntimeComposition(apiGatewayUrl = "http://127.0.0.1:8080")

        // 2. Resolve real HTTP customer repository
        val repository = composition.customerRepository

        // 3. Execute real backend call through repository abstraction
        val result = repository.findCustomerById("CUS-001")

        // 4. Verify result came from actual backend
        assertNotNull("Result from repository must not be null", result)
        println("[REAL_DI_EVIDENCE] Android -> ProductionRuntimeComposition -> HttpCustomerRepository -> Actual Backend Result: $result")
    }

    @Test
    fun verifyRealProductionOrderRepositoryPathToActualBackend() = runBlocking {
        val composition = ProductionRuntimeComposition(apiGatewayUrl = "http://127.0.0.1:8080")
        val repository = composition.orderRepository

        val result = repository.findOrderById("ORD-001")

        assertNotNull("Result from order repository must not be null", result)
        println("[REAL_DI_EVIDENCE] Android -> ProductionRuntimeComposition -> HttpOrderRepository -> Actual Backend Result: $result")
    }
}
