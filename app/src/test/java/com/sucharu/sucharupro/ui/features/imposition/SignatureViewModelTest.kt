package com.sucharu.sucharupro.ui.features.imposition

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

/**
 * Unit Tests for Multi-Page Signature Imposition ViewModel.
 * Module 18 Step 04.
 */
class SignatureViewModelTest {

    private fun createTestViewModel(): SignatureViewModel {
        return SignatureViewModel(
            externalScope = CoroutineScope(Dispatchers.Unconfined)
        )
    }

    @Test
    fun `initial state runs optimization on default 64pp booklet and populates specification`() {
        val viewModel = createTestViewModel()
        val state = viewModel.uiState.value

        assertNotNull("Specification should be calculated on init", state.currentSpecification)
        assertEquals(4, state.currentSpecification!!.totalSignaturesCount)
        assertEquals(16, state.currentSpecification!!.signaturePageCount)
        assertTrue(state.currentSpecification!!.sheetUtilizationPercentage > BigDecimal.ZERO)
        assertNull(state.errorMessage)
    }

    @Test
    fun `changing total pages and turning method triggers re-calculation`() {
        val viewModel = createTestViewModel()

        viewModel.onTotalPagesChanged("16")
        viewModel.onSheetTurningMethodChanged("WORK_AND_TURN")
        viewModel.optimizeSignature()

        val state = viewModel.uiState.value
        assertNotNull(state.currentSpecification)
        assertEquals(1, state.currentSpecification!!.totalSignaturesCount)
        assertEquals("WORK_AND_TURN", state.currentSpecification!!.sheetTurningMethod)
        assertEquals(1, state.currentSpecification!!.signatureForms.size)
    }

    @Test
    fun `exportHandoffContract should generate valid Module 19 handoff JSON`() {
        val viewModel = createTestViewModel()
        viewModel.exportHandoffContract()

        val state = viewModel.uiState.value
        assertNotNull("Handoff JSON must be populated", state.handoffExportedJson)
        assertTrue(state.handoffExportedJson!!.contains("contractVersion"))
        assertTrue(state.handoffExportedJson!!.contains("totalSignatures"))
    }
}
