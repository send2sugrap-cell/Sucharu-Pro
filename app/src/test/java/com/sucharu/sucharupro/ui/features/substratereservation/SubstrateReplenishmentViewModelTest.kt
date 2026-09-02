package com.sucharu.sucharupro.ui.features.substratereservation

import com.sucharu.sucharupro.domain.model.printingcalculator.PaperStockType
import com.sucharu.sucharupro.domain.model.substratereservation.ReplenishmentTriggerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

/**
 * Unit tests for SubstrateReplenishmentViewModel (Module 19 Step 04).
 */
class SubstrateReplenishmentViewModelTest {

    private fun createTestViewModel(): SubstrateReplenishmentViewModel {
        return SubstrateReplenishmentViewModel(
            externalScope = CoroutineScope(Dispatchers.Unconfined)
        )
    }

    @Test
    fun testInitialStateAndTabSelection() {
        val viewModel = createTestViewModel()
        val state = viewModel.uiState.value

        assertNotNull(state.currentEvaluation)
        assertEquals(0, state.selectedTab)
        assertNull(state.errorMessage)
        assertNotNull(state.successMessage)
        assertEquals("ART-300-25X36", state.currentEvaluation?.sku)

        // Select Tab 2 (Recommendations)
        viewModel.selectTab(2)
        assertEquals(2, viewModel.uiState.value.selectedTab)
    }

    @Test
    fun testEvaluateReplenishment_UpdatesState() {
        val viewModel = createTestViewModel()

        viewModel.evaluateReplenishment(
            sku = "MAP-120-23X36",
            materialName = "Maplitho 120 GSM",
            stockType = PaperStockType.OFFSET_PAPER,
            gsm = BigDecimal("120.0000"),
            sheetWidthMm = BigDecimal("584.0000"),
            sheetHeightMm = BigDecimal("914.4000"),
            warehouseId = "WH-SUB-02",
            warehouseName = "Auxiliary Warehouse",
            onHandSheets = 4000L,
            reservedSheets = 2000L,
            pendingInboundSheets = 500L,
            plannedDemandSheets = 1000L,
            minStockSheets = 1500L,
            safetyStockSheets = 3000L,
            reorderPointSheets = 8000L,
            targetStockSheets = 20000L,
            moqSheets = 5000L
        )

        val state = viewModel.uiState.value
        assertNotNull(state.currentEvaluation)
        assertEquals("MAP-120-23X36", state.currentEvaluation?.sku)
        assertTrue(state.currentEvaluation!!.isReorderRequired)
        assertEquals(ReplenishmentTriggerState.REORDER_TRIGGERED.name, state.currentEvaluation?.triggerState)
    }

    @Test
    fun testTriggerSupplierAlert_DispatchesAlertAndUpdatesState() {
        val viewModel = createTestViewModel()
        val evalId = viewModel.uiState.value.currentEvaluation!!.evaluationId

        viewModel.triggerSupplierAlert(evalId)

        val state = viewModel.uiState.value
        assertEquals(1, state.alerts.size)
        assertEquals(evalId, state.alerts[0].evaluationId)
        assertEquals(ReplenishmentTriggerState.SUPPLIER_ALERT_SENT.name, state.alerts[0].status)
        assertEquals(ReplenishmentTriggerState.SUPPLIER_ALERT_SENT.name, state.currentEvaluation?.triggerState)
    }

    @Test
    fun testDialogStateAndClearMessages() {
        val viewModel = createTestViewModel()
        viewModel.setShowEvaluateDialog(true)
        assertTrue(viewModel.uiState.value.showEvaluateDialog)

        viewModel.setShowAlertConfirmDialog(true)
        assertTrue(viewModel.uiState.value.showAlertConfirmDialog)

        viewModel.clearMessages()
        assertNull(viewModel.uiState.value.errorMessage)
        assertNull(viewModel.uiState.value.successMessage)
    }
}
