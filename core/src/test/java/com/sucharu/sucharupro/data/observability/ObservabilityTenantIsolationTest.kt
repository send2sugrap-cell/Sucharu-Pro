package com.sucharu.sucharupro.data.observability

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.PrincipalType
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.observability.service.ObservabilityAuthDecision
import com.sucharu.sucharupro.data.observability.service.TenantObservabilityAuthorizationService
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Tenant isolation test suite for operational observability (INFRA-04 Step 09).
 */
class ObservabilityTenantIsolationTest {

    private lateinit var authService: TenantObservabilityAuthorizationService

    private fun staffPrincipal(projectId: String) = AuthenticatedPrincipal(
        userId = "staff-01",
        projectId = projectId,
        username = "staff01",
        role = UserRole.STAFF,
        principalType = PrincipalType.HUMAN
    )

    private fun managerPrincipal(projectId: String) = AuthenticatedPrincipal(
        userId = "mgr-01",
        projectId = projectId,
        username = "mgr01",
        role = UserRole.MANAGER,
        principalType = PrincipalType.HUMAN
    )

    @Before
    fun setUp() {
        authService = TenantObservabilityAuthorizationService()
    }

    @Test
    fun test01_sameTenantStaff_isAllowed() {
        val decision = authService.authorizeTenantAccess(staffPrincipal("tenant-A"), "tenant-A")
        assertTrue("Staff in same tenant should be allowed tenant observability", decision is ObservabilityAuthDecision.Allowed)
    }

    @Test
    fun test02_crossTenantStaff_isDenied() {
        val decision = authService.authorizeTenantAccess(staffPrincipal("tenant-A"), "tenant-B")
        assertTrue("Staff querying different tenant must be denied", decision is ObservabilityAuthDecision.Denied)
        val denied = decision as ObservabilityAuthDecision.Denied
        assertEquals("TENANT_MISMATCH", denied.code)
    }

    @Test
    fun test03_crossTenantManager_isDenied() {
        val decision = authService.authorizeTenantAccess(managerPrincipal("tenant-A"), "tenant-B")
        assertTrue("Manager querying different tenant must be denied", decision is ObservabilityAuthDecision.Denied)
        val denied = decision as ObservabilityAuthDecision.Denied
        assertEquals("TENANT_MISMATCH", denied.code)
    }

    @Test
    fun test04_adminCanAccessAnyTenant() {
        val admin = AuthenticatedPrincipal(
            userId = "admin-01",
            projectId = "system",
            username = "admin",
            role = UserRole.ADMIN,
            principalType = PrincipalType.HUMAN
        )
        val decision = authService.authorizeTenantAccess(admin, "tenant-A")
        assertTrue("Admin can access any tenant's observability", decision is ObservabilityAuthDecision.Allowed)
    }
}
