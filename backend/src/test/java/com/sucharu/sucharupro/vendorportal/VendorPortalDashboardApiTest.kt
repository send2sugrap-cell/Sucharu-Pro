package com.sucharu.sucharupro.vendorportal

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.PrincipalType
import com.sucharu.sucharupro.data.api.model.UserPermission
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.api.server.BackendUseCases
import com.sucharu.sucharupro.data.datasource.FakeVendorDataSource
import com.sucharu.sucharupro.data.datasource.FakeVendorPortalDataSource
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.data.persistence.postgres.TransactionContext
import com.sucharu.sucharupro.data.persistence.postgres.TransactionManager
import com.sucharu.sucharupro.data.repository.VendorPortalDashboardRepositoryImpl
import com.sucharu.sucharupro.data.repository.VendorPortalRepositoryImpl
import com.sucharu.sucharupro.data.repository.VendorRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.Vendor
import com.sucharu.sucharupro.domain.model.vendor.VendorStatus
import com.sucharu.sucharupro.domain.model.vendorportal.VendorPortalRole
import com.sucharu.sucharupro.domain.service.vendorportal.VendorPortalDashboardService
import com.sucharu.sucharupro.domain.service.vendorportal.VendorPortalDashboardServiceImpl
import com.sucharu.sucharupro.domain.service.vendorportal.VendorPortalService
import com.sucharu.sucharupro.domain.service.vendorportal.VendorPortalServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class VendorPortalDashboardApiTest {

    private lateinit var vendorDs: FakeVendorDataSource
    private lateinit var vendorRepo: VendorRepositoryImpl
    private lateinit var portalDs: FakeVendorPortalDataSource
    private lateinit var portalRepo: VendorPortalRepositoryImpl
    private lateinit var portalService: VendorPortalService
    private lateinit var dashboardRepo: VendorPortalDashboardRepositoryImpl
    private lateinit var dashboardService: VendorPortalDashboardService
    private lateinit var useCases: BackendUseCases

    @Before
    fun setUp() {
        runBlocking {
            vendorDs = FakeVendorDataSource()
            vendorRepo = VendorRepositoryImpl(vendorDs)
            portalDs = FakeVendorPortalDataSource()
            portalRepo = VendorPortalRepositoryImpl(portalDs)
            portalService = VendorPortalServiceImpl(portalRepo, vendorRepo)

            vendorRepo.createVendor(
                Vendor(
                    vendorId = "vnd_api_01",
                    projectId = "PROJ-ALPHA",
                    vendorCode = "VND-API",
                    vendorName = "API Matrix Corp",
                    status = VendorStatus.ACTIVE
                )
            )

            dashboardRepo = VendorPortalDashboardRepositoryImpl(
                vendorRepository = vendorRepo,
                portalRepository = portalRepo
            )
            dashboardService = VendorPortalDashboardServiceImpl(
                portalService = portalService,
                dashboardRepository = dashboardRepo
            )

            // Setup portal account & membership
            val acc = portalService.createOrInviteAccount("vnd_api_01", "API-01", null, null, "TENANT-001", "PROJ-ALPHA", "admin_001")
            val pId = (acc as DomainResult.Success).data.portalAccountId
            portalService.activateAccount(pId, "TENANT-001", "admin_001")

            val mem = portalService.inviteVendorUser(pId, "vnd_api_01", "usr_api_01", VendorPortalRole.VENDOR_ADMIN, "*", "TENANT-001", "admin_001")
            val token = (mem as DomainResult.Success).data.invitationToken!!
            portalService.activateMembership(token, "TENANT-001", "admin_001", true)

            val fakeTxManager = object : TransactionManager {
                override suspend fun <T> inTransaction(tenantContext: TenantContext, block: suspend (TransactionContext) -> T): T {
                    throw UnsupportedOperationException("Not required for read tests")
                }
                override suspend fun <T> inReadOnly(tenantContext: TenantContext, block: suspend (TransactionContext) -> T): T {
                    throw UnsupportedOperationException("Not required for read tests")
                }
            }

            val customRepoFactory = object : PostgresRepositoryFactory(fakeTxManager) {
                override fun createVendorPortalService(tenantId: String): VendorPortalService = portalService
                override fun createVendorPortalDashboardService(tenantId: String): VendorPortalDashboardService = dashboardService
            }

            useCases = BackendUseCases(fakeTxManager, customRepoFactory)
        }
    }

    @Test
    fun testGetDashboardUseCase() {
        runBlocking {
            val principal = AuthenticatedPrincipal(
                userId = "usr_api_01",
                projectId = "TENANT-001",
                username = "matrix-vendor",
                role = UserRole.VENDOR,
                principalType = PrincipalType.HUMAN,
                vendorId = "vnd_api_01",
                permissions = setOf(UserPermission.READ_VENDOR_PORTAL)
            )
            val dashboard = useCases.getVendorPortalDashboard(principal)
            assertNotNull(dashboard)
            assertEquals("vnd_api_01", dashboard.vendorId)
            assertEquals("API Matrix Corp", dashboard.vendorName)
        }
    }

    @Test
    fun testGetProfileUseCase() {
        runBlocking {
            val principal = AuthenticatedPrincipal(
                userId = "usr_api_01",
                projectId = "TENANT-001",
                username = "matrix-vendor",
                role = UserRole.VENDOR,
                principalType = PrincipalType.HUMAN,
                vendorId = "vnd_api_01",
                permissions = setOf(UserPermission.READ_VENDOR_PORTAL)
            )
            val profile = useCases.getVendorPortalProfile(principal)
            assertNotNull(profile)
            assertEquals("vnd_api_01", profile.vendorId)
        }
    }

    @Test
    fun testGetWorkspaceUseCase() {
        runBlocking {
            val principal = AuthenticatedPrincipal(
                userId = "usr_api_01",
                projectId = "TENANT-001",
                username = "matrix-vendor",
                role = UserRole.VENDOR,
                principalType = PrincipalType.HUMAN,
                vendorId = "vnd_api_01",
                permissions = setOf(UserPermission.READ_VENDOR_PORTAL)
            )
            val workspace = useCases.getVendorPortalWorkspace(principal)
            assertNotNull(workspace)
            assertEquals("vnd_api_01", workspace.vendorId)
        }
    }
}
