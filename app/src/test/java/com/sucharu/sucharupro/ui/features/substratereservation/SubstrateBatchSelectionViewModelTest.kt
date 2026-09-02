package com.sucharu.sucharupro.ui.features.substratereservation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

/**
 * Unit tests for SubstrateBatchSelectionViewModel (Module 19 Step 03).
 */
class SubstrateBatchSelectionViewModelTest {

    private fun createTestViewModel(): SubstrateBatchSelectionViewModel {
        return SubstrateBatchSelectionViewModel(
            externalScope = CoroutineScope(Dispatchers.Unconfined)
        )
    }

    @Test
    fun testInitialStateAndTabSelection() {
        val viewModel = createTestViewModel()
        val state = viewModel.uiState.value

        assertNotNull(state.currentSelection)
        assertEquals(0, state.selectedTab)
        assertNull(state.errorMessage)
        assertNotNull(state.successMessage)
        assertEquals("SBS-DEMO-01", state.currentSelection?.selectionId)

        // Select Tab 2 (2D Visualizer)
        viewModel.selectTab(2)
        assertEquals(2, viewModel.uiState.value.selectedTab)
    }

    @Test
    fun testEvaluateSelection_UpdatesState() {
        val viewModel = createTestViewModel()

        viewModel.evaluateSampleSelection(
            orderId = "ORD-TEST-99",
            requiredSheets = 2000L,
            targetGsm = BigDecimal("300.0000")
        )

        val state = viewModel.uiState.value
        assertNotNull(state.currentSelection)
        assertEquals("ORD-TEST-99", state.currentSelection?.orderId)
        assertEquals(2000L, state.currentSelection?.requiredSheets)
        assertTrue(state.currentSelection?.allocatedSheets ?: 0L > 0L)
    }

    @Test
    fun testConfirmSelection_UpdatesConfirmationStatus() {
        val viewModel = createTestViewModel()
        viewModel.confirmSelection()

        val state = viewModel.uiState.value
        assertNotNull(state.currentSelection)
        assertTrue(state.currentSelection!!.isConfirmedAndAllocated)
        assertEquals("SYSTEM_SUPERVISOR", state.currentSelection!!.confirmedBy)
    }

    @Test
    fun testExportHandoffJson_PopulatesJsonString() {
        val viewModel = createTestViewModel()
        viewModel.exportHandoffJson()

        val state = viewModel.uiState.value
        assertNotNull(state.jsonHandoffPreview)
        assertTrue(state.jsonHandoffPreview!!.contains("contractVersion"))
        assertTrue(state.jsonHandoffPreview!!.contains("3.0.0"))
        assertTrue(state.jsonHandoffPreview!!.contains("targetGrainDirection"))
    }
}
