package com.sucharu.sucharupro

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sucharu.sucharupro.data.api.client.HttpBackendApiClient
import com.sucharu.sucharupro.data.api.model.ApiResult
import com.sucharu.sucharupro.data.composition.ProductionRuntimeComposition
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * PHASE 00 STEP 02 - Real Android Emulator → Actual Backend Runtime Verification Test.
 * Executed on Android Runtime (ART) on physical device or emulator-5554.
 * Communicates with actual backend listening at http://10.0.2.2:8080 (emulator host loopback).
 */
@RunWith(AndroidJUnit4::class)
class RealBackendRuntimeIntegrationTest {

    @Test
    fun verifyAndroidToActualBackendLivenessCommunication() = runBlocking {
        // Target backend via ADB reverse port forwarding (http://127.0.0.1:8080)
        val backendUrl = "http://127.0.0.1:8080"
        val client = HttpBackendApiClient(baseUrl = backendUrl)

        // Execute real HTTP Liveness call from Android runtime
        val result = client.checkHealthLive()

        // Verify real response received from actual backend
        assertTrue("Expected ApiResult.Success from actual backend liveness, got: $result", result is ApiResult.Success)
        val data = (result as ApiResult.Success).data
        assertNotNull("Response map from backend should not be null", data)
        println("[REAL_RUNTIME_EVIDENCE] Android -> Actual Backend HTTP Liveness Successful: data=$data, correlationId=${result.correlationId}")
    }

    @Test
    fun verifyAndroidToActualBackendReadinessCommunication() = runBlocking {
        val backendUrl = "http://127.0.0.1:8080"
        val client = HttpBackendApiClient(baseUrl = backendUrl)

        val result = client.checkHealthReady()

        // Readiness returns success or error response structure from actual backend
        assertTrue("Expected ApiResult from actual backend readiness call, got: $result", result is ApiResult.Success || result is ApiResult.Error)
        println("[REAL_RUNTIME_EVIDENCE] Android -> Actual Backend HTTP Readiness Response Received: $result")
    }

    @Test
    fun verifyProductionRuntimeCompositionInstantiation() {
        val composition = ProductionRuntimeComposition(apiGatewayUrl = "http://127.0.0.1:8080")
        val sessionManager = composition.createSessionManager()
        assertNotNull("SessionManager must be instantiated with HttpBackendApiClient", sessionManager)
        println("[REAL_RUNTIME_EVIDENCE] ProductionRuntimeComposition initialized session manager successfully.")
    }
}
