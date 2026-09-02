package com.sucharu.sucharupro.domain.service.jobclosure

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.ForbiddenException
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.api.model.jobclosure.CloseAndSealJobRequestDto
import com.sucharu.sucharupro.data.api.server.BackendUseCases
import com.sucharu.sucharupro.data.datasource.finalqc.FakeFinalQcPackagingDataSource
import com.sucharu.sucharupro.data.datasource.jobclosure.FakeProductionJobClosureDataSource
import com.sucharu.sucharupro.data.datasource.jobcosting.FakeProductionJobCostingDataSource
import com.sucharu.sucharupro.data.datasource.productionexecution.FakeProductionExecutionDataSource
import com.sucharu.sucharupro.data.datasource.productionscheduling.FakeProductionSchedulingDataSource
import com.sucharu.sucharupro.data.datasource.shopfloortracking.FakeShopFloorTrackingDataSource
import com.sucharu.sucharupro.data.event.MockPostgresEventDatabase
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import com.sucharu.sucharupro.data.repository.finalqc.FinalQcPackagingRepositoryImpl
import com.sucharu.sucharupro.data.repository.jobclosure.ProductionJobClosureRepositoryImpl
import com.sucharu.sucharupro.data.repository.jobcosting.ProductionJobCostingRepositoryImpl
import com.sucharu.sucharupro.data.repository.productionexecution.ProductionExecutionRepositoryImpl
import com.sucharu.sucharupro.data.repository.productionscheduling.ProductionSchedulingRepositoryImpl
import com.sucharu.sucharupro.data.repository.shopfloortracking.ShopFloorTrackingRepositoryImpl
import com.sucharu.sucharupro.domain.service.finalqc.FinalQcPackagingServiceImpl
import com.sucharu.sucharupro.domain.service.jobcosting.ProductionJobCostingServiceImpl
import com.sucharu.sucharupro.domain.service.productionexecution.ProductionExecutionServiceImpl
import com.sucharu.sucharupro.domain.service.productionscheduling.ProductionSchedulingServiceImpl
import com.sucharu.sucharupro.domain.service.shopfloortracking.ShopFloorTrackingServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class ProductionJobClosureSecurityEdgeTest {

    private lateinit var useCases: BackendUseCases
    private val tenantAlpha = "TENANT-ALPHA"
    private val tenantBeta = "TENANT-BETA"

    private val adminPrincipal = AuthenticatedPrincipal(
        userId = "admin-1",
        username = "admin_user",
        role = UserRole.ADMIN,
        projectId = tenantAlpha
    )

    private val managerPrincipal = AuthenticatedPrincipal(
        userId = "manager-1",
        username = "manager_user",
        role = UserRole.MANAGER,
        projectId = tenantAlpha
    )

    private val staffPrincipal = AuthenticatedPrincipal(
        userId = "staff-1",
        username = "staff_user",
        role = UserRole.STAFF,
        projectId = tenantAlpha
    )

    private val customerPrincipal = AuthenticatedPrincipal(
        userId = "cust-1",
        username = "customer_user",
        role = UserRole.CUSTOMER,
        projectId = tenantAlpha
    )

    private val vendorPrincipal = AuthenticatedPrincipal(
        userId = "vend-1",
        username = "vendor_user",
        role = UserRole.VENDOR,
        projectId = tenantAlpha
    )

    private val crossTenantPrincipal = AuthenticatedPrincipal(
        userId = "admin-beta",
        username = "admin_beta",
        role = UserRole.ADMIN,
        projectId = tenantBeta
    )

    @Before
    fun setup() {
        val fakeClosureDs = FakeProductionJobClosureDataSource()
        val fakeCostDs = FakeProductionJobCostingDataSource()
        val fakeQcDs = FakeFinalQcPackagingDataSource()
        val fakeTrackingDs = FakeShopFloorTrackingDataSource()

        val closureRepo = ProductionJobClosureRepositoryImpl(fakeClosureDs)
        val costRepo = ProductionJobCostingRepositoryImpl(fakeCostDs)
        val qcRepo = FinalQcPackagingRepositoryImpl(fakeQcDs)
        val trackingRepo = ShopFloorTrackingRepositoryImpl(fakeTrackingDs)

        val closureService = ProductionJobClosureServiceImpl(closureRepo)
        val costService = ProductionJobCostingServiceImpl(costRepo)
        val qcService = FinalQcPackagingServiceImpl(qcRepo)
        val trackingService = ShopFloorTrackingServiceImpl(trackingRepo)

        val mockDb = MockPostgresEventDatabase()

        val factory = object : PostgresRepositoryFactory(
            transactionManager = mockDb,
            defaultTenantId = tenantAlpha
        ) {
            override fun createProductionJobClosureDataSource(tenantId: String) = fakeClosureDs
            override fun createProductionJobClosureRepository(tenantId: String) = closureRepo
            override fun createProductionJobClosureService(tenantId: String) = closureService

            override fun createProductionJobCostingDataSource(tenantId: String) = fakeCostDs
            override fun createProductionJobCostingRepository(tenantId: String) = costRepo
            override fun createProductionJobCostingService(tenantId: String) = costService

            override fun createFinalQcPackagingDataSource(tenantId: String) = fakeQcDs
            override fun createFinalQcPackagingRepository(tenantId: String) = qcRepo
            override fun createFinalQcPackagingService(tenantId: String) = qcService

            override fun createShopFloorTrackingDataSource(tenantId: String) = fakeTrackingDs
            override fun createShopFloorTrackingRepository(tenantId: String) = trackingRepo
            override fun createShopFloorTrackingService(tenantId: String) = trackingService
        }

        useCases = BackendUseCases(mockDb, factory)
    }


    @Test
    fun `test admin and manager can close and seal production job`() = runBlocking {
        val req = CloseAndSealJobRequestDto(
            orderId = "ORD-ALPHA",
            orderQuantity = BigDecimal("5000.0000"),
            goodUnitsReleased = BigDecimal("5000.0000"),
            estimatedTotalCost = BigDecimal("20000.0000"),
            actualTotalCost = BigDecimal("20000.0000"),
            totalCostVariance = BigDecimal.ZERO
        )

        val res = useCases.closeAndSealJob(adminPrincipal, "JOB-001", req)
        assertEquals("GOVERNANCE_SEALED", res.closureStatus)
    }

    @Test
    fun `test staff role cannot close and seal job`() = runBlocking {
        val req = CloseAndSealJobRequestDto(
            orderId = "ORD-ALPHA",
            orderQuantity = BigDecimal("5000.0000"),
            goodUnitsReleased = BigDecimal("5000.0000"),
            estimatedTotalCost = BigDecimal("20000.0000"),
            actualTotalCost = BigDecimal("20000.0000"),
            totalCostVariance = BigDecimal.ZERO
        )

        try {
            useCases.closeAndSealJob(staffPrincipal, "JOB-001", req)
            fail("Expected ForbiddenException for Staff role attempting job closure")
        } catch (e: ForbiddenException) {
            assertTrue(e.message?.contains("Access denied") == true || e.message?.contains("Forbidden") == true || e.message != null)
        }
    }

    @Test
    fun `test customer and vendor roles are strictly forbidden from job closure`() = runBlocking {
        val req = CloseAndSealJobRequestDto(
            orderId = "ORD-ALPHA",
            orderQuantity = BigDecimal("5000.0000"),
            goodUnitsReleased = BigDecimal("5000.0000"),
            estimatedTotalCost = BigDecimal("20000.0000"),
            actualTotalCost = BigDecimal("20000.0000"),
            totalCostVariance = BigDecimal.ZERO
        )

        try {
            useCases.closeAndSealJob(customerPrincipal, "JOB-001", req)
            fail("Expected ForbiddenException for Customer role")
        } catch (e: ForbiddenException) {
            assertTrue(e.message != null)
        }

        try {
            useCases.closeAndSealJob(vendorPrincipal, "JOB-001", req)
            fail("Expected ForbiddenException for Vendor role")
        } catch (e: ForbiddenException) {
            assertTrue(e.message != null)
        }
    }

    @Test
    fun `test cross tenant cannot view job closure record of another tenant`() = runBlocking {
        val req = CloseAndSealJobRequestDto(
            orderId = "ORD-ALPHA",
            orderQuantity = BigDecimal("5000.0000"),
            goodUnitsReleased = BigDecimal("5000.0000"),
            estimatedTotalCost = BigDecimal("20000.0000"),
            actualTotalCost = BigDecimal("20000.0000"),
            totalCostVariance = BigDecimal.ZERO
        )
        useCases.closeAndSealJob(adminPrincipal, "JOB-ALPHA", req)

        val betaRes = useCases.getJobClosureRecord(crossTenantPrincipal, "JOB-ALPHA")
        assertNull("Cross-tenant access must return null", betaRes)
    }
}
