package com.sucharu.sucharupro.vendorportal

import com.sucharu.sucharupro.data.api.model.*
import com.sucharu.sucharupro.data.api.server.BackendUseCases
import com.sucharu.sucharupro.data.datasource.*
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.data.persistence.postgres.TransactionContext
import com.sucharu.sucharupro.data.persistence.postgres.TransactionManager
import com.sucharu.sucharupro.data.repository.*
import com.sucharu.sucharupro.domain.model.vendor.*
import com.sucharu.sucharupro.domain.service.vendor.VendorPerformanceServiceImpl
import com.sucharu.sucharupro.domain.service.vendorportal.VendorPortalPerformanceComplianceService
import com.sucharu.sucharupro.domain.service.vendorportal.VendorPortalPerformanceComplianceServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class VendorPortalPerformanceSecurityEdgeTest {

    private lateinit var useCases: BackendUseCases
    private lateinit var vendorRepo: VendorRepositoryImpl

    @Before
    fun setup() {
        runBlocking {
            val vendorDs = FakeVendorDataSource()
            val perfDs = FakeVendorPerformanceDataSource()
            val portalDs = FakeVendorPortalPerformanceComplianceDataSource()

            vendorRepo = VendorRepositoryImpl(vendorDs)
            val perfRepo = VendorPerformanceRepositoryImpl(perfDs)
            val portalRepo = VendorPortalPerformanceComplianceRepositoryImpl(portalDs)

            val canonicalService = VendorPerformanceServiceImpl(perfRepo, vendorRepo)
            val portalService = VendorPortalPerformanceComplianceServiceImpl(
                portalRepository = portalRepo,
                canonicalPerformanceService = canonicalService,
                vendorRepository = vendorRepo
            )

            val fakeTxManager = object : TransactionManager {
                override suspend fun <T> inTransaction(tenantContext: TenantContext, block: suspend (TransactionContext) -> T): T {
                    throw UnsupportedOperationException("Not required for mock tests")
                }
                override suspend fun <T> inReadOnly(tenantContext: TenantContext, block: suspend (TransactionContext) -> T): T {
                    throw UnsupportedOperationException("Not required for mock tests")
                }
            }

            val customFactory = object : PostgresRepositoryFactory(fakeTxManager) {
                override fun createVendorPortalPerformanceComplianceService(tenantId: String): VendorPortalPerformanceComplianceService {
                    return portalService
                }
            }

            useCases = BackendUseCases(fakeTxManager, customFactory)

            // Seed suspended vendor
            vendorRepo.createVendor(
                Vendor(
                    vendorId = "VND-SUSPENDED",
                    projectId = "PRJ-001",
                    vendorCode = "VND-SUSP-CODE",
                    vendorName = "Suspended Vendor Ltd.",
                    vendorCategory = VendorCategory.OTHER,
                    status = VendorStatus.SUSPENDED
                )
            )
        }
    }

    @Test
    fun testSuspendedVendorCannotAccessPerformanceWorkspace() {
        val suspendedPrincipal = AuthenticatedPrincipal(
            userId = "user-susp",
            projectId = "PRJ-001",
            username = "vendor_susp",
            role = UserRole.VENDOR,
            vendorId = "VND-SUSPENDED"
        )

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                useCases.getVendorPortalPerformanceOverview(suspendedPrincipal)
            }
        }
    }

    @Test
    fun testPrincipalWithoutVendorIdCannotAccessVendorPerformance() {
        val noVendorPrincipal = AuthenticatedPrincipal(
            userId = "user-orphan",
            projectId = "PRJ-001",
            username = "vendor_orphan",
            role = UserRole.VENDOR,
            vendorId = null
        )

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                useCases.getVendorPortalPerformanceOverview(noVendorPrincipal)
            }
        }
    }
}
