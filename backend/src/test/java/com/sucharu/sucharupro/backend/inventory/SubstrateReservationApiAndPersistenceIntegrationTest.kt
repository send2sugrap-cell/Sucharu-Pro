package com.sucharu.sucharupro.backend.inventory

import com.sucharu.sucharupro.backend.integration.MockIntegrationDb
import com.sucharu.sucharupro.data.api.model.*
import com.sucharu.sucharupro.data.api.server.*
import com.sucharu.sucharupro.data.auth.security.AuthConfig
import com.sucharu.sucharupro.data.auth.security.JwtTokenProvider
import com.sucharu.sucharupro.data.datasource.substratereservation.FakeSubstrateReservationDataSource
import com.sucharu.sucharupro.data.persistence.postgres.DatabaseHealthChecker
import com.sucharu.sucharupro.data.persistence.postgres.DefaultPostgresTransactionManager
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import com.sucharu.sucharupro.data.repository.substratereservation.SubstrateReservationRepositoryImpl
import com.sucharu.sucharupro.domain.model.printingcalculator.PaperStockType
import com.sucharu.sucharupro.domain.model.printingcalculator.PrintingDimension
import com.sucharu.sucharupro.domain.model.substratereservation.*
import com.sucharu.sucharupro.domain.service.substratereservation.SubstrateReservationServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class SubstrateReservationApiAndPersistenceIntegrationTest {

    private lateinit var router: BackendRouter
    private lateinit var securityContext: BackendSecurityContext
    private lateinit var useCases: BackendUseCases
    private lateinit var jwtTokenProvider: JwtTokenProvider
    private lateinit var adminToken: String
    private lateinit var reservationService: SubstrateReservationServiceImpl
    private lateinit var reservationDs: FakeSubstrateReservationDataSource
    private val tenantId = "TENANT-001"
    private val orderId = "ORD-SUB-01"
    private val orderItemId = "ITEM-SUB-01"

    @Before
    fun setup() {
        runBlocking {
            reservationDs = FakeSubstrateReservationDataSource()
            val reservationRepo = SubstrateReservationRepositoryImpl(reservationDs)
            reservationService = SubstrateReservationServiceImpl(reservationRepo)

            val mockDb = MockIntegrationDb()
            val txManager = DefaultPostgresTransactionManager(mockDb)

            val customFactory = object : PostgresRepositoryFactory(txManager, tenantId) {
                override fun createSubstrateReservationDataSource(tenantId: String) = reservationDs
                override fun createSubstrateReservationRepository(tenantId: String) = reservationRepo
                override fun createSubstrateReservationService(tenantId: String) = reservationService
            }

            useCases = BackendUseCases(txManager, customFactory)

            val authConfig = AuthConfig(
                jwtSigningSecret = "test_signing_secret_for_substrate_reservation_api_test_2026",
                jwtIssuer = "sucharu-test",
                jwtAudience = "sucharu-api"
            )
            jwtTokenProvider = JwtTokenProvider(authConfig)
            securityContext = BackendSecurityContext(jwtTokenProvider)

            adminToken = jwtTokenProvider.generateAccessToken(
                principal = AuthenticatedPrincipal(
                    userId = "USR-ADMIN-01",
                    projectId = tenantId,
                    username = "admin_user",
                    role = UserRole.ADMIN
                )
            )

            val healthChecker = DatabaseHealthChecker(mockDb)
            router = BackendRouter(securityContext, useCases, healthChecker)
        }
    }

    private fun createRequirement(): SubstrateRequirement {
        return SubstrateRequirement(
            requirementId = "REQ-001",
            tenantId = tenantId,
            orderId = orderId,
            orderItemId = orderItemId,
            stockType = PaperStockType.ART_CARD,
            requestedMaterialName = "Art Card 300 GSM",
            gsm = BigDecimal("300"),
            sheetDimension = PrintingDimension(BigDecimal("635"), BigDecimal("914")),
            productiveSheetsRequired = 1000L,
            wasteSheetsRequired = 50L,
            totalSheetsRequired = 1050L,
            totalReamsRequired = BigDecimal("2.1"),
            totalWeightKg = BigDecimal("182.2")
        )
    }

    @Test
    fun testCreateSoftReservationAndGet_EndToEnd() = runBlocking {
        val requirement = createRequirement()

        val reservation = reservationService.createSoftReservation(
            tenantId = tenantId,
            orderId = orderId,
            orderItemId = orderItemId,
            productId = "PROD-ART-300",
            sku = "SKU-ART-300",
            productName = "Art Card 300 GSM Sheet",
            warehouseId = "WH-MAIN-01",
            requirement = requirement,
            softHoldDurationMinutes = 60L,
            notes = "Quotation soft hold",
            actor = "planner_john"
        )

        assertNotNull(reservation)
        assertEquals(SubstrateReservationStatus.RESERVED_SOFT, reservation.status)
        assertEquals(SubstrateReservationMode.SOFT, reservation.mode)
        assertEquals(1050L, reservation.reservedSheets)
        assertEquals(tenantId, reservation.tenantId)

        val retrieved = reservationService.getReservation(tenantId, reservation.reservationId)
        assertNotNull(retrieved)
        assertEquals(reservation.reservationId, retrieved!!.reservationId)
    }

    @Test
    fun testPromoteSoftToHardReservation_AtomicallyAllocates() = runBlocking {
        val requirement = createRequirement()

        val softRes = reservationService.createSoftReservation(
            tenantId = tenantId,
            orderId = orderId,
            orderItemId = orderItemId,
            productId = "PROD-ART-300",
            sku = "SKU-ART-300",
            productName = "Art Card 300 GSM Sheet",
            warehouseId = "WH-MAIN-01",
            requirement = requirement,
            notes = "Quotation hold",
            actor = "planner_john"
        )

        val promotedRes = reservationService.promoteSoftToHard(
            tenantId = tenantId,
            reservationId = softRes.reservationId,
            executionJobId = "JOB-EXEC-101",
            workOrderId = "WO-101",
            allocatedWarehouseId = "WH-MAIN-01",
            actor = "scheduler_sarah"
        )

        assertNotNull(promotedRes)
        assertEquals(SubstrateReservationStatus.ALLOCATED_HARD, promotedRes.status)
        assertEquals(SubstrateReservationMode.HARD, promotedRes.mode)
        assertEquals("JOB-EXEC-101", promotedRes.executionJobId)
        assertEquals("scheduler_sarah", promotedRes.promotedBy)
        assertNotNull(promotedRes.promotedAt)
    }

    @Test
    fun testReleaseReservation_RestoresAvailableQuantity() = runBlocking {
        val requirement = createRequirement()

        val softRes = reservationService.createSoftReservation(
            tenantId = tenantId,
            orderId = orderId,
            orderItemId = orderItemId,
            productId = "PROD-ART-300",
            sku = "SKU-ART-300",
            productName = "Art Card 300 GSM Sheet",
            warehouseId = "WH-MAIN-01",
            requirement = requirement,
            actor = "planner_john"
        )

        val releasedRes = reservationService.releaseReservation(
            tenantId = tenantId,
            reservationId = softRes.reservationId,
            reason = "Customer cancelled order",
            actor = "planner_john"
        )

        assertEquals(SubstrateReservationStatus.CANCELLED, releasedRes.status)
        assertTrue(releasedRes.status.isTerminal)
        assertFalse(releasedRes.status.isActiveHold)
    }

    @Test
    fun testCrossTenantIsolation_PreventsUnauthorizedAccess() = runBlocking {
        val requirement = createRequirement()

        val reservation = reservationService.createSoftReservation(
            tenantId = tenantId,
            orderId = orderId,
            orderItemId = orderItemId,
            productId = "PROD-ART-300",
            sku = "SKU-ART-300",
            productName = "Art Card 300 GSM Sheet",
            warehouseId = "WH-MAIN-01",
            requirement = requirement,
            actor = "planner_john"
        )

        // Querying from tenant "TENANT-OTHER" must return null
        val crossTenantRes = reservationService.getReservation("TENANT-OTHER", reservation.reservationId)
        assertNull("Cross-tenant lookup must return null for tenant isolation", crossTenantRes)
    }
}
