package com.sucharu.sucharupro.ui.features.production.tracking

import com.sucharu.sucharupro.data.datasource.shopfloortracking.FakeShopFloorTrackingDataSource
import com.sucharu.sucharupro.data.repository.shopfloortracking.ShopFloorTrackingRepositoryImpl
import com.sucharu.sucharupro.domain.model.production.ProductionStageType
import com.sucharu.sucharupro.domain.service.shopfloortracking.ShopFloorTrackingServiceImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class ShopFloorTrackingViewModelTest {

    private lateinit var viewModel: ShopFloorTrackingViewModel
    private val tenantId = "TENANT-001"
    private val jobId = "JOB-001"

    @Before
    fun setup() {
        val fakeDs = FakeShopFloorTrackingDataSource()
        val repo = ShopFloorTrackingRepositoryImpl(fakeDs)
        val service = ShopFloorTrackingServiceImpl(repo)
        viewModel = ShopFloorTrackingViewModel(
            trackingService = service,
            defaultTenantId = tenantId,
            externalScope = CoroutineScope(Dispatchers.Unconfined)
        )
    }

    @Test
    fun `test start work order and fetch live tracking data updates UI state`() = runBlocking {
        viewModel.startWorkOrder(
            workOrderId = "WO-101",
            jobId = jobId,
            orderId = "ORD-101",
            sequenceNumber = 1,
            stageType = ProductionStageType.PRINTING,
            machineId = "PRESS-01",
            machineName = "Heidelberg 4C",
            operatorId = "OP-1",
            operatorName = "Rahim",
            isSetup = true,
            actor = "operator",
            tenantId = tenantId
        )

        val state = viewModel.uiState.value
        assertEquals("Work order WO-101 started.", state.successMessage)
        assertEquals(1, state.operatorTimeRecords.size)
        assertEquals("SETUP", state.operatorTimeRecords[0].currentState)
    }

    @Test
    fun `test recording output and material consumption updates state`() = runBlocking {
        viewModel.startWorkOrder(
            workOrderId = "WO-101",
            jobId = jobId,
            orderId = "ORD-101",
            sequenceNumber = 1,
            stageType = ProductionStageType.PRINTING,
            machineId = "PRESS-01",
            machineName = "Heidelberg 4C",
            operatorId = "OP-1",
            operatorName = "Rahim",
            isSetup = false,
            actor = "operator",
            tenantId = tenantId
        )

        viewModel.recordWorkOrderOutput(
            workOrderId = "WO-101",
            goodQty = BigDecimal("5000.0000"),
            scrapQty = BigDecimal("100.0000"),
            setupMins = 20,
            runMins = 80,
            downtimeMins = 10,
            isCompleted = true,
            actor = "operator",
            tenantId = tenantId
        )

        viewModel.recordMaterialConsumption(
            workOrderId = "WO-101",
            jobId = jobId,
            stageType = ProductionStageType.PRINTING,
            materialCode = "PAPER-01",
            materialName = "Paper",
            unitOfMeasure = "SHEETS",
            plannedQty = BigDecimal("5000.0000"),
            actualQty = BigDecimal("5100.0000"),
            scrapQty = BigDecimal("100.0000"),
            actor = "operator",
            tenantId = tenantId
        )

        val state = viewModel.uiState.value
        assertEquals(1, state.materialConsumptions.size)
        assertEquals(BigDecimal("100.0000"), state.materialConsumptions[0].varianceQuantity)
        assertEquals("COMPLETED", state.operatorTimeRecords[0].currentState)
    }
}
