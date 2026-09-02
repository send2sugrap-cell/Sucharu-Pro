package com.sucharu.sucharupro.domain.service.substratereservation

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.ForbiddenException
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.api.model.substratereservation.CreateSubstrateReservationRequestDto
import com.sucharu.sucharupro.data.api.server.BackendUseCases
import com.sucharu.sucharupro.data.datasource.substratereservation.FakeSubstrateReservationDataSource
import com.sucharu.sucharupro.data.event.MockPostgresEventDatabase
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import com.sucharu.sucharupro.data.repository.substratereservation.SubstrateReservationRepositoryImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class SubstrateReservationSecurityEdgeTest {

    private lateinit var useCases: BackendUseCases
    private val tenantAlpha = "TENANT-ALPHA"
    private val tenantBeta = "TENANT-BETA"

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

    private val managerPrincipal = AuthenticatedPrincipal(
        userId = "mgr-1",
        username = "manager_user",
        role = UserRole.MANAGER,
        projectId = tenantAlpha
    )

    @Before
    fun setup() {
        val fakeReservationDs = FakeSubstrateReservationDataSource()
        val reservationRepo = SubstrateReservationRepositoryImpl(fakeReservationDs)
        val reservationService = SubstrateReservationServiceImpl(reservationRepo)

        val mockDb = MockPostgresEventDatabase()

        val factory = object : PostgresRepositoryFactory(
            transactionManager = mockDb,
            defaultTenantId = tenantAlpha
        ) {
            override fun createSubstrateReservationDataSource(tenantId: String) = fakeReservationDs
            override fun createSubstrateReservationRepository(tenantId: String) = reservationRepo
            override fun createSubstrateReservationService(tenantId: String) = reservationService
        }

        useCases = BackendUseCases(mockDb, factory)
    }


    @Test
    fun `test Customer and Vendor roles are strictly rejected from creating substrate reservations`() {
        val reqDto = CreateSubstrateReservationRequestDto(
            orderId = "ORD-001",
            orderItemId = "ITEM-01",
            productId = "PROD-01",
            sku = "ART-300-25X36",
            productName = "Art Card 300",
            totalSheetsRequired = 5000L
        )

        // Customer rejected (403 ForbiddenException)
        assertThrows(ForbiddenException::class.java) {
            runBlocking {
                useCases.createSubstrateReservation(customerPrincipal, reqDto)
            }
        }

        // Vendor rejected (403 ForbiddenException)
        assertThrows(ForbiddenException::class.java) {
            runBlocking {
                useCases.createSubstrateReservation(vendorPrincipal, reqDto)
            }
        }
    }

    @Test
    fun `test Manager and Staff roles are authorized to create substrate reservations`() = runBlocking {
        val reqDto = CreateSubstrateReservationRequestDto(
            orderId = "ORD-001",
            orderItemId = "ITEM-01",
            productId = "PROD-01",
            sku = "ART-300-25X36",
            productName = "Art Card 300",
            totalSheetsRequired = 3000L
        )

        val res = useCases.createSubstrateReservation(managerPrincipal, reqDto)
        assertNotNull(res)
        assertEquals("ORD-001", res.orderId)
        assertEquals(3000L, res.reservedSheets)
    }
}
