package com.sucharu.sucharupro.ui.features.demo

import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.auth.model.LoginRequestDto
import com.sucharu.sucharupro.data.auth.session.AppEntryState
import com.sucharu.sucharupro.data.composition.DemoRole
import com.sucharu.sucharupro.data.composition.DevelopmentDemoRuntimeComposition
import com.sucharu.sucharupro.ui.features.auth.PostLoginRouter
import com.sucharu.sucharupro.ui.navigation.AppDestination
import com.sucharu.sucharupro.ui.navigation.AppNavigationManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

/**
 * UI and Navigation Integration Test Suite for All-Role Development Demo Mode (INFRA-06).
 */
class DevelopmentDemoAllRolesUiTest {

    @Test
    fun testCustomerDemo_postLoginRouting() = runBlocking {
        val composition = DevelopmentDemoRuntimeComposition(initialRole = DemoRole.CUSTOMER)
        val sessionManager = composition.createSessionManager()
        val navManager = AppNavigationManager(sessionManager)

        sessionManager.confirmVerification("123456")
        sessionManager.login(LoginRequestDto(identifier = "demo_customer", password = "demoPassword123!"))

        val state = sessionManager.entryState.value
        assertTrue(state is AppEntryState.Authenticated)
        val principal = (state as AppEntryState.Authenticated).principal
        assertEquals(UserRole.CUSTOMER, principal.role)

        navManager.syncWithPostLoginRouter(principal)
        assertEquals(AppDestination.Customer.Home, navManager.currentDestination.value)
    }

    @Test
    fun testAffiliateDemo_postLoginRouting() = runBlocking {
        val composition = DevelopmentDemoRuntimeComposition(initialRole = DemoRole.AFFILIATE)
        val sessionManager = composition.createSessionManager()
        val navManager = AppNavigationManager(sessionManager)

        sessionManager.confirmVerification("123456")
        sessionManager.login(LoginRequestDto(identifier = "demo_affiliate", password = "demoPassword123!"))

        val state = sessionManager.entryState.value
        assertTrue(state is AppEntryState.Authenticated)
        val principal = (state as AppEntryState.Authenticated).principal
        assertEquals(UserRole.AFFILIATE, principal.role)

        navManager.syncWithPostLoginRouter(principal)
        assertEquals(AppDestination.Affiliate.Home, navManager.currentDestination.value)
    }

    @Test
    fun testStaffDemo_postLoginRouting() = runBlocking {
        val composition = DevelopmentDemoRuntimeComposition(initialRole = DemoRole.STAFF)
        val sessionManager = composition.createSessionManager()
        val navManager = AppNavigationManager(sessionManager)

        sessionManager.confirmVerification("123456")
        sessionManager.login(LoginRequestDto(identifier = "demo_staff", password = "demoPassword123!"))

        val state = sessionManager.entryState.value
        assertTrue(state is AppEntryState.Authenticated)
        val principal = (state as AppEntryState.Authenticated).principal
        assertEquals(UserRole.STAFF, principal.role)

        navManager.syncWithPostLoginRouter(principal)
        assertEquals(AppDestination.Staff.AssignedWork, navManager.currentDestination.value)
    }

    @Test
    fun testManagerDemo_postLoginRouting() = runBlocking {
        val composition = DevelopmentDemoRuntimeComposition(initialRole = DemoRole.MANAGER)
        val sessionManager = composition.createSessionManager()
        val navManager = AppNavigationManager(sessionManager)

        sessionManager.confirmVerification("123456")
        sessionManager.login(LoginRequestDto(identifier = "demo_manager", password = "demoPassword123!"))

        val state = sessionManager.entryState.value
        assertTrue(state is AppEntryState.Authenticated)
        val principal = (state as AppEntryState.Authenticated).principal
        assertEquals(UserRole.MANAGER, principal.role)

        navManager.syncWithPostLoginRouter(principal)
        assertEquals(AppDestination.Manager.Operations, navManager.currentDestination.value)
    }

    @Test
    fun testAdminDemo_postLoginRouting() = runBlocking {
        val composition = DevelopmentDemoRuntimeComposition(initialRole = DemoRole.ADMIN)
        val sessionManager = composition.createSessionManager()
        val navManager = AppNavigationManager(sessionManager)

        sessionManager.confirmVerification("123456")
        sessionManager.login(LoginRequestDto(identifier = "demo_admin", password = "demoPassword123!"))

        val state = sessionManager.entryState.value
        assertTrue(state is AppEntryState.Authenticated)
        val principal = (state as AppEntryState.Authenticated).principal
        assertEquals(UserRole.ADMIN, principal.role)

        navManager.syncWithPostLoginRouter(principal)
        assertEquals(AppDestination.Admin.FullAdministration, navManager.currentDestination.value)
    }

    @Test
    fun testDemoRoleSelector_allRolesMappedToCanonicalUserRoles() {
        assertEquals(UserRole.CUSTOMER, DemoRole.CUSTOMER.userRole)
        assertEquals(UserRole.AFFILIATE, DemoRole.AFFILIATE.userRole)
        assertEquals(UserRole.STAFF, DemoRole.STAFF.userRole)
        assertEquals(UserRole.MANAGER, DemoRole.MANAGER.userRole)
        assertEquals(UserRole.ADMIN, DemoRole.ADMIN.userRole)
    }
}
