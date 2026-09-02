package com.sucharu.sucharupro.ui.features.inventory.substratereservation

import com.sucharu.sucharupro.data.datasource.substratereservation.FakeSubstrateReservationDataSource
import com.sucharu.sucharupro.data.repository.substratereservation.SubstrateReservationRepositoryImpl
import com.sucharu.sucharupro.domain.service.substratereservation.SubstrateReservationService
import com.sucharu.sucharupro.domain.service.substratereservation.SubstrateReservationServiceImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class SubstrateReservationViewModelTest {

    private lateinit var viewModel: SubstrateReservationViewModel
    private lateinit var service: SubstrateReservationService

    @Before
    fun setup() {
        val fakeReservationDs = FakeSubstrateReservationDataSource()
        val reservationRepo = SubstrateReservationRepositoryImpl(fakeReservationDs)
        service = SubstrateReservationServiceImpl(reservationRepo)

        viewModel = SubstrateReservationViewModel(
            reservationService = service,
            defaultTenantId = "TENANT-001",
            externalScope = CoroutineScope(Dispatchers.Unconfined)
        )
    }

    @Test
    fun `test initial state and tab selection`() {
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(SubstrateReservationTab.ACTIVE_RESERVATIONS, state.selectedTab)

        viewModel.selectTab(SubstrateReservationTab.REQUIREMENT_RESOLVER)
        assertEquals(SubstrateReservationTab.REQUIREMENT_RESOLVER, viewModel.uiState.value.selectedTab)
    }

    @Test
    fun `test create reservation and load flow in ViewModel`() = runBlocking {
        viewModel.createReservation(
            orderId = "ORD-201",
            orderItemId = "ITEM-01",
            executionJobId = "JOB-EXEC-001",
            productId = "PROD-01",
            sku = "ART-300-25X36",
            productName = "Art Card 300 GSM",
            totalSheets = 5000L,
            isHardAllocation = true,
            notes = "Test reservation"
        )

        val state = viewModel.uiState.value
        assertEquals(1, state.activeReservations.size)
        assertEquals(5000L, state.totalReservedSheets)
        assertEquals("ORD-201", state.activeReservations[0].orderId)
        assertEquals("ALLOCATED_HARD", state.activeReservations[0].status)
    }

    @Test
    fun `test soft reservation creation and promote to hard in ViewModel`() = runBlocking {
        viewModel.createSoftReservation(
            orderId = "ORD-501",
            orderItemId = "ITEM-01",
            productId = "PROD-01",
            sku = "ART-300-25X36",
            productName = "Art Card 300 GSM",
            totalSheets = 2000L,
            softHoldDurationMinutes = 60L,
            notes = "Soft quote hold"
        )

        val softState = viewModel.uiState.value
        assertEquals(1, softState.activeReservations.size)
        assertEquals("RESERVED_SOFT", softState.activeReservations[0].status)
        val reservationId = softState.activeReservations[0].reservationId

        viewModel.promoteSoftToHard(
            reservationId = reservationId,
            executionJobId = "JOB-EXEC-501",
            workOrderId = "WO-501",
            warehouseId = "WH-MAIN-01",
            locationId = "BAY-01",
            batchNumber = "BATCH-501"
        )

        val hardState = viewModel.uiState.value
        assertEquals(1, hardState.activeReservations.size)
        assertEquals("ALLOCATED_HARD", hardState.activeReservations[0].status)
        assertEquals("JOB-EXEC-501", hardState.activeReservations[0].executionJobId)
    }

    @Test
    fun `test resolve requirement updates resolutionResult`() = runBlocking {
        viewModel.resolveRequirement(
            orderId = "ORD-201",
            orderItemId = "ITEM-01",
            materialCode = "ART-300-25X36",
            materialName = "Art Card 300 GSM",
            gsm = BigDecimal("300.0000"),
            sheetWidthMm = BigDecimal("635.0000"),
            sheetHeightMm = BigDecimal("914.4000"),
            productiveSheets = 4500L,
            wasteSheets = 500L
        )

        val state = viewModel.uiState.value
        assertNotNull(state.resolutionResult)
        assertEquals(5000L, state.resolutionResult?.requirement?.totalSheetsRequired)
        assertTrue(state.resolutionResult?.isSufficientStockAvailable == true)
    }
}
