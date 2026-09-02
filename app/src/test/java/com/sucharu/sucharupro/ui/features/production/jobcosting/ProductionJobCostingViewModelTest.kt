package com.sucharu.sucharupro.ui.features.production.jobcosting

import com.sucharu.sucharupro.data.datasource.jobcosting.FakeProductionJobCostingDataSource
import com.sucharu.sucharupro.data.repository.jobcosting.ProductionJobCostingRepositoryImpl
import com.sucharu.sucharupro.domain.model.jobcosting.*
import com.sucharu.sucharupro.domain.model.production.ProductionStageType
import com.sucharu.sucharupro.domain.model.shopfloortracking.ProductionMaterialConsumptionRecord
import com.sucharu.sucharupro.domain.service.jobcosting.ProductionJobCostingServiceImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class ProductionJobCostingViewModelTest {

    private lateinit var viewModel: ProductionJobCostingViewModel
    private lateinit var fakeDs: FakeProductionJobCostingDataSource
    private val tenantId = "TENANT-001"
    private val jobId = "JOB-101"

    @Before
    fun setup() {
        fakeDs = FakeProductionJobCostingDataSource()
        val repo = ProductionJobCostingRepositoryImpl(fakeDs)
        val service = ProductionJobCostingServiceImpl(repo)

        viewModel = ProductionJobCostingViewModel(
            costingService = service,
            defaultTenantId = tenantId,
            externalScope = CoroutineScope(Dispatchers.Unconfined)
        )
    }

    @Test
    fun `test calculating actual job cost updates UI state`() = runBlocking {
        viewModel.calculateActualJobCost(
            jobId = jobId,
            orderId = "ORD-101",
            manufacturedGoodQuantity = BigDecimal("5000.0000"),
            packagingUnitRate = BigDecimal("25.0000"),
            overheadAllocationRate = BigDecimal("0.1000")
        )

        val state = viewModel.uiState.value
        assertNotNull(state.actualCostRecord)
        assertEquals("ACTUAL_COSTED", state.actualCostRecord?.costStatus)
        assertFalse(state.isCalculateCostDialogOpen)
        assertTrue(state.successMessage?.contains("Actual manufacturing cost calculated") == true)
    }

    @Test
    fun `test variance analysis and 8-way reconciliation updates UI state`() = runBlocking {
        // 1. Calculate actual cost
        viewModel.calculateActualJobCost(
            jobId = jobId,
            orderId = "ORD-101",
            manufacturedGoodQuantity = BigDecimal("5000.0000")
        )

        // 2. Calculate variance
        viewModel.calculateJobCostVariance(
            jobId = jobId,
            quotedSellingPrice = BigDecimal("30000.0000"),
            estimatedTotalCost = BigDecimal("20000.0000"),
            estimatedMaterialCost = BigDecimal("15000.0000"),
            estimatedLaborCost = BigDecimal("3000.0000"),
            estimatedMachineCost = BigDecimal("2000.0000"),
            orderQuantity = BigDecimal("5000.0000")
        )

        val varianceState = viewModel.uiState.value
        assertNotNull(varianceState.varianceSummary)

        // 3. Reconcile
        viewModel.reconcileJobCosting(jobId = jobId)

        val finalState = viewModel.uiState.value
        assertNotNull(finalState.reconciliationResult)
        assertEquals(64, finalState.reconciliationResult?.certificateHash?.length)
    }
}
