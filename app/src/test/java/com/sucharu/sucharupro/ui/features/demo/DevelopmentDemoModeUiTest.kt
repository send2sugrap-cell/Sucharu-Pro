package com.sucharu.sucharupro.ui.features.demo

import com.sucharu.sucharupro.data.composition.DevelopmentDemoRuntimeComposition
import com.sucharu.sucharupro.data.composition.ProductionRuntimeComposition
import com.sucharu.sucharupro.data.composition.AppRuntimeMode
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.auth.session.AppEntryState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

/**
 * UI / Composition integration tests for Development Demo Mode.
 */
class DevelopmentDemoModeUiTest {

    @Test
    fun testDemoComposition_instantiatesDemoSessionManager() = runBlocking {
        val composition = DevelopmentDemoRuntimeComposition()
        assertEquals(AppRuntimeMode.DEVELOPMENT, composition.mode)

        val sessionManager = composition.createSessionManager()
        assertNotNull(sessionManager)

        val state = sessionManager.restoreSession()
        assertEquals(AppEntryState.Public, state)
    }

    @Test
    fun testProductionComposition_blocksUnconfiguredGateway() {
        val prodComposition = ProductionRuntimeComposition(apiGatewayUrl = null)
        assertEquals(AppRuntimeMode.PRODUCTION, prodComposition.mode)

        assertThrows(IllegalStateException::class.java) {
            prodComposition.createSessionManager()
        }
    }

    @Test
    fun testDemoUser_isCustomerRoleOnly() = runBlocking {
        val composition = DevelopmentDemoRuntimeComposition()
        val sessionManager = composition.createSessionManager()

        // Confirm OTP
        sessionManager.confirmVerification("123456")
        sessionManager.login(com.sucharu.sucharupro.data.auth.model.LoginRequestDto(identifier = "demo", password = "demoPassword123!"))

        val state = sessionManager.entryState.value
        assertTrue(state is AppEntryState.Authenticated)
        val principal = (state as AppEntryState.Authenticated).principal

        assertEquals(UserRole.CUSTOMER, principal.role)
        assertNotEquals(UserRole.ADMIN, principal.role)
        assertNotEquals(UserRole.MANAGER, principal.role)
        assertNotEquals(UserRole.STAFF, principal.role)
    }
}
