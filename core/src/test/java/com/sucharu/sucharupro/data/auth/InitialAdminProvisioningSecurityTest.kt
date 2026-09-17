package com.sucharu.sucharupro.data.auth

import com.sucharu.sucharupro.data.api.model.ConflictException
import com.sucharu.sucharupro.data.api.model.UserPermission
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.api.model.ValidationException
import com.sucharu.sucharupro.data.auth.model.ProvisionAdminRequestDto
import com.sucharu.sucharupro.data.auth.model.RegisterRequestDto
import com.sucharu.sucharupro.data.auth.persistence.FakeAuthAccountDataSource
import com.sucharu.sucharupro.data.auth.persistence.FakeAuthAuditDataSource
import com.sucharu.sucharupro.data.auth.persistence.FakeAuthProfileDataSource
import com.sucharu.sucharupro.data.auth.persistence.FakeAuthSessionDataSource
import com.sucharu.sucharupro.data.auth.service.AuthenticationService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class InitialAdminProvisioningSecurityTest {

    private lateinit var accountDs: FakeAuthAccountDataSource
    private lateinit var sessionDs: FakeAuthSessionDataSource
    private lateinit var auditDs: FakeAuthAuditDataSource
    private lateinit var profileDs: FakeAuthProfileDataSource
    private lateinit var authService: AuthenticationService

    @Before
    fun setUp() {
        accountDs = FakeAuthAccountDataSource()
        sessionDs = FakeAuthSessionDataSource()
        auditDs = FakeAuthAuditDataSource()
        profileDs = FakeAuthProfileDataSource()

        authService = AuthenticationService(
            accountDataSource = accountDs,
            sessionDataSource = sessionDs,
            auditDataSource = auditDs,
            profileDataSource = profileDs
        )
    }

    @Test
    fun test01_provisionInitialAdmin_noExistingAdmin_succeedsAndIssuesAdminToken() = runBlocking {
        val request = ProvisionAdminRequestDto(
            identifier = "owner_admin",
            password = "SecureOwnerPassword123!",
            email = "owner@sucharupro.com",
            displayName = "System Owner",
            requestedProjectId = "TENANT-001"
        )

        val response = authService.provisionInitialAdmin(
            request = request,
            correlationId = "test-corr-01"
        )

        assertNotNull(response)
        assertNotNull(response.accessToken)
        assertEquals(UserRole.ADMIN, response.user.role)
        assertTrue("ADMIN must resolve to ADMIN_ALL permission", response.user.permissions.contains(UserPermission.ADMIN_ALL))
        assertEquals("owner_admin", response.user.username)

        // Verify account is stored with ADMIN role in datasource
        val storedAccount = accountDs.getAccount("TENANT-001", "owner_admin")
        assertNotNull(storedAccount)
        assertEquals(UserRole.ADMIN, storedAccount?.role)
        assertNotEquals("Password must NOT be in plaintext", "SecureOwnerPassword123!", storedAccount?.passwordHash)
    }

    @Test
    fun test02_provisionInitialAdmin_whenAdminAlreadyExists_permanentlyDisabledWithConflict() = runBlocking {
        // First provisioning attempt succeeds
        authService.provisionInitialAdmin(
            request = ProvisionAdminRequestDto(
                identifier = "initial_owner",
                password = "OwnerPassword123!",
                requestedProjectId = "TENANT-001"
            ),
            correlationId = "corr-1"
        )

        // Second provisioning attempt for same project MUST be rejected with ConflictException!
        try {
            authService.provisionInitialAdmin(
                request = ProvisionAdminRequestDto(
                    identifier = "second_attacker",
                    password = "HackerPassword123!",
                    requestedProjectId = "TENANT-001"
                ),
                correlationId = "corr-2"
            )
            fail("Subsequent provisioning attempts MUST be rejected when an ADMIN already exists!")
        } catch (e: ConflictException) {
            assertTrue(e.message?.contains("Initial admin provisioning is unavailable") == true)
        }
    }

    @Test
    fun test03_publicRegistration_cannotInjectAdminRole() = runBlocking {
        try {
            authService.register(
                request = RegisterRequestDto(
                    displayName = "Malicious User",
                    email = "hacker@test.com",
                    password = "Password123!",
                    requestedRole = UserRole.ADMIN
                ),
                correlationId = "corr-pub-1"
            )
            fail("Public registration MUST reject requestedRole = ADMIN!")
        } catch (e: ValidationException) {
            assertTrue(e.message?.contains("Public registration cannot assign privileged role") == true)
        }
    }

    @Test
    fun test04_provisionedAdmin_canAuthenticateViaNormalLoginFlow() = runBlocking {
        // 1. Provision initial owner
        authService.provisionInitialAdmin(
            request = ProvisionAdminRequestDto(
                identifier = "owner_credentials",
                password = "OwnerMasterPassword123!",
                requestedProjectId = "TENANT-001"
            ),
            correlationId = "prov-1"
        )

        // 2. Log in through normal login(...) flow
        val loginResponse = authService.login(
            request = com.sucharu.sucharupro.data.auth.model.LoginRequestDto(
                identifier = "owner_credentials",
                password = "OwnerMasterPassword123!",
                requestedProjectId = "TENANT-001"
            ),
            correlationId = "login-1"
        )

        assertNotNull(loginResponse)
        assertEquals(UserRole.ADMIN, loginResponse.user.role)
        assertTrue(loginResponse.user.permissions.contains(UserPermission.ADMIN_ALL))
    }
}
