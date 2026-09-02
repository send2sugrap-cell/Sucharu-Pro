package com.sucharu.sucharupro.domain.service.jobcosting

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.ForbiddenException
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.api.model.jobcosting.*
import com.sucharu.sucharupro.data.api.server.BackendUseCases
import com.sucharu.sucharupro.data.datasource.finalqc.FakeFinalQcPackagingDataSource
import com.sucharu.sucharupro.data.datasource.jobcosting.FakeProductionJobCostingDataSource
import com.sucharu.sucharupro.data.datasource.shopfloortracking.FakeShopFloorTrackingDataSource
import com.sucharu.sucharupro.data.event.MockPostgresEventDatabase
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import com.sucharu.sucharupro.data.repository.finalqc.FinalQcPackagingRepositoryImpl
import com.sucharu.sucharupro.data.repository.jobcosting.ProductionJobCostingRepositoryImpl
import com.sucharu.sucharupro.data.repository.shopfloortracking.ShopFloorTrackingRepositoryImpl
import com.sucharu.sucharupro.domain.service.finalqc.FinalQcPackagingServiceImpl
import com.sucharu.sucharupro.domain.service.shopfloortracking.ShopFloorTrackingServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class ProductionJobCostingSecurityEdgeTest {

    private lateinit var useCases: BackendUseCases
    private val tenantAlpha = "TENANT-ALPHA"
    private val tenantBeta = "TENANT-BETA"

    private val adminPrincipal = AuthenticatedPrincipal(
        userId = "admin-1",
        username = "admin_user",
        role = UserRole.ADMIN,
        projectId = tenantAlpha
    )

    private val staffCostAccountantPrincipal = AuthenticatedPrincipal(
        userId = "staff-cost-1",
        username = "cost_accountant_user",
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
        val fakeCostDs = FakeProductionJobCostingDataSource()
        val fakeTrackingDs = FakeShopFloorTrackingDataSource()
        val fakeQcDs = FakeFinalQcPackagingDataSource()

        val costRepo = ProductionJobCostingRepositoryImpl(fakeCostDs)
        val trackingRepo = ShopFloorTrackingRepositoryImpl(fakeTrackingDs)
        val qcRepo = FinalQcPackagingRepositoryImpl(fakeQcDs)

        val costService = ProductionJobCostingServiceImpl(costRepo)
        val trackingService = ShopFloorTrackingServiceImpl(trackingRepo)
        val qcService = FinalQcPackagingServiceImpl(qcRepo)

        val mockDb = MockPostgresEventDatabase()

        val factory = object : PostgresRepositoryFactory(
            transactionManager = mockDb,
            defaultTenantId = tenantAlpha
        ) {
            override fun createProductionJobCostingDataSource(tenantId: String) = fakeCostDs
            override fun createProductionJobCostingRepository(tenantId: String) = costRepo
            override fun createProductionJobCostingService(tenantId: String) = costService

            override fun createShopFloorTrackingDataSource(tenantId: String) = fakeTrackingDs
            override fun createShopFloorTrackingRepository(tenantId: String) = trackingRepo
            override fun createShopFloorTrackingService(tenantId: String) = trackingService

            override fun createFinalQcPackagingDataSource(tenantId: String) = fakeQcDs
            override fun createFinalQcPackagingRepository(tenantId: String) = qcRepo
            override fun createFinalQcPackagingService(tenantId: String) = qcService
        }

        useCases = BackendUseCases(mockDb, factory)
    }

    @Test
    fun `test staff cost accountant can calculate actual job cost`() = runBlocking {
        val req = CalculateActualJobCostRequestDto(
            orderId = "ORD-ALPHA",
            manufacturedGoodQuantity = BigDecimal("5000.0000")
        )

        val res = useCases.calculateActualJobCost(staffCostAccountantPrincipal, "JOB-001", req)
        assertEquals("ACTUAL_COSTED", res.costStatus)
    }

    @Test
    fun `test customer role is strictly forbidden from calculating job cost`() = runBlocking {
        val req = CalculateActualJobCostRequestDto(
            orderId = "ORD-ALPHA",
            manufacturedGoodQuantity = BigDecimal("5000.0000")
        )

        try {
            useCases.calculateActualJobCost(customerPrincipal, "JOB-001", req)
            fail("Expected ForbiddenException for Customer role")
        } catch (e: ForbiddenException) {
            assertTrue(e.message?.contains("Access denied") == true || e.message?.contains("Forbidden") == true || e.message != null)
        }
    }

    @Test
    fun `test vendor role is strictly forbidden from job cost variance analysis`() = runBlocking {
        val req = CalculateJobCostVarianceRequestDto(
            quotedSellingPrice = BigDecimal("20000.0000"),
            estimatedTotalCost = BigDecimal("15000.0000"),
            estimatedMaterialCost = BigDecimal("10000.0000"),
            estimatedLaborCost = BigDecimal("3000.0000"),
            estimatedMachineCost = BigDecimal("2000.0000"),
            orderQuantity = BigDecimal("5000.0000")
        )

        try {
            useCases.calculateJobCostVariance(vendorPrincipal, "JOB-001", req)
            fail("Expected ForbiddenException for Vendor role")
        } catch (e: ForbiddenException) {
            assertTrue(e.message?.contains("Access denied") == true || e.message?.contains("Forbidden") == true || e.message != null)
        }
    }

    @Test
    fun `test cross tenant cannot view actual job cost of another tenant`() = runBlocking {
        val req = CalculateActualJobCostRequestDto(
            orderId = "ORD-ALPHA",
            manufacturedGoodQuantity = BigDecimal("5000.0000")
        )
        useCases.calculateActualJobCost(adminPrincipal, "JOB-ALPHA", req)

        val betaRes = useCases.getActualJobCostByJob(crossTenantPrincipal, "JOB-ALPHA")
        assertNull("Cross-tenant user must receive null for other tenant's cost record", betaRes)
    }
}
