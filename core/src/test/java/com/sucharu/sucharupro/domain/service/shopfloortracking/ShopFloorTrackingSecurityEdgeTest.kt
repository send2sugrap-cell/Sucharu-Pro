package com.sucharu.sucharupro.domain.service.shopfloortracking

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.ForbiddenException
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.api.model.shopfloortracking.*
import com.sucharu.sucharupro.data.api.server.BackendUseCases
import com.sucharu.sucharupro.data.datasource.shopfloortracking.FakeShopFloorTrackingDataSource
import com.sucharu.sucharupro.data.event.MockPostgresEventDatabase
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import com.sucharu.sucharupro.data.repository.shopfloortracking.ShopFloorTrackingRepositoryImpl
import com.sucharu.sucharupro.domain.repository.shopfloortracking.ShopFloorTrackingRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class ShopFloorTrackingSecurityEdgeTest {

    private lateinit var useCases: BackendUseCases
    private lateinit var trackingRepo: ShopFloorTrackingRepository
    private lateinit var fakeDs: FakeShopFloorTrackingDataSource

    private val tenantAlpha = "TENANT-ALPHA"
    private val tenantBeta = "TENANT-BETA"

    private val adminPrincipal = AuthenticatedPrincipal(
        userId = "admin-1",
        username = "admin_user",
        role = UserRole.ADMIN,
        projectId = tenantAlpha
    )

    private val staffOperatorPrincipal = AuthenticatedPrincipal(
        userId = "staff-1",
        username = "operator_user",
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
        fakeDs = FakeShopFloorTrackingDataSource()
        trackingRepo = ShopFloorTrackingRepositoryImpl(fakeDs)
        val mockDb = MockPostgresEventDatabase()

        val factory = object : PostgresRepositoryFactory(
            transactionManager = mockDb,
            defaultTenantId = tenantAlpha
        ) {
            override fun createShopFloorTrackingDataSource(tenantId: String) = fakeDs
            override fun createShopFloorTrackingRepository(tenantId: String) = trackingRepo
            override fun createShopFloorTrackingService(tenantId: String) = ShopFloorTrackingServiceImpl(trackingRepo)
        }

        useCases = BackendUseCases(mockDb, factory)
    }

    @Test
    fun `test staff operator can start work order and record output`() = runBlocking {
        val startReq = StartWorkOrderExecutionRequestDto(
            executionJobId = "JOB-001",
            orderId = "ORD-001",
            sequenceNumber = 1,
            stageType = "PRINTING",
            machineId = "PRESS-01",
            machineName = "Heidelberg 4C",
            operatorId = "OP-1",
            operatorName = "Rahim",
            isSetup = true
        )

        val started = useCases.startWorkOrderExecution(staffOperatorPrincipal, "WO-001", startReq)
        assertEquals("SETUP", started.currentState)

        val outputReq = RecordWorkOrderOutputRequestDto(
            additionalGoodQuantity = BigDecimal("1000.0000"),
            additionalScrapQuantity = BigDecimal("20.0000"),
            additionalSetupMinutes = 20,
            additionalRunMinutes = 40,
            isCompleted = false
        )
        val recorded = useCases.recordWorkOrderOutput(staffOperatorPrincipal, "WO-001", outputReq)
        assertEquals(BigDecimal("1000.0000"), recorded.goodQuantityProduced)
    }

    @Test
    fun `test customer role is strictly forbidden from shop floor tracking operations`() = runBlocking {
        val startReq = StartWorkOrderExecutionRequestDto(
            executionJobId = "JOB-001",
            orderId = "ORD-001",
            sequenceNumber = 1,
            stageType = "PRINTING",
            machineId = "PRESS-01",
            machineName = "Heidelberg 4C",
            operatorId = "OP-1",
            operatorName = "Rahim"
        )

        try {
            useCases.startWorkOrderExecution(customerPrincipal, "WO-001", startReq)
            fail("Expected ForbiddenException for Customer role")
        } catch (e: ForbiddenException) {
            assertTrue(e.message?.contains("Access denied") == true || e.message?.contains("Forbidden") == true || e.message != null)
        }
    }

    @Test
    fun `test vendor role is strictly forbidden from material consumption recording`() = runBlocking {
        val matReq = RecordMaterialConsumptionRequestDto(
            executionJobId = "JOB-001",
            stageType = "PRINTING",
            materialCode = "PAPER-01",
            materialName = "Paper",
            unitOfMeasure = "SHEETS",
            plannedQuantity = BigDecimal("1000.0000"),
            actualQuantity = BigDecimal("1050.0000")
        )

        try {
            useCases.recordMaterialConsumption(vendorPrincipal, "WO-001", matReq)
            fail("Expected ForbiddenException for Vendor role")
        } catch (e: ForbiddenException) {
            assertTrue(e.message?.contains("Access denied") == true || e.message?.contains("Forbidden") == true || e.message != null)
        }
    }

    @Test
    fun `test tenant isolation ensures cross tenant cannot view operator timers`() = runBlocking {
        // Create in TENANT-ALPHA
        val startReq = StartWorkOrderExecutionRequestDto(
            executionJobId = "JOB-ALPHA",
            orderId = "ORD-ALPHA",
            sequenceNumber = 1,
            stageType = "PRINTING",
            machineId = "PRESS-01",
            machineName = "Heidelberg 4C",
            operatorId = "OP-1",
            operatorName = "Rahim"
        )
        useCases.startWorkOrderExecution(adminPrincipal, "WO-ALPHA", startReq)

        // Query with TENANT-BETA principal
        val betaRecords = useCases.listOperatorTimeRecordsByJob(crossTenantPrincipal, "JOB-ALPHA")
        assertTrue("Cross-tenant user must see 0 records", betaRecords.isEmpty())
    }
}
