package com.sucharu.sucharupro.ui.features.imposition

import com.sucharu.sucharupro.data.api.model.imposition.NestingCandidateItemDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

/**
 * Unit Tests for Dynamic Nesting Command Center ViewModel.
 * Module 18 Step 03.
 */
class NestingViewModelTest {

    private fun createTestViewModel(): NestingViewModel {
        return NestingViewModel(
            externalScope = CoroutineScope(Dispatchers.Unconfined)
        )
    }

    @Test
    fun `initial state runs optimization on default candidates and populates current specification`() {
        val viewModel = createTestViewModel()

        val state = viewModel.uiState.value
        assertNotNull("Specification should be calculated on init", state.currentSpecification)
        assertTrue(state.currentSpecification!!.totalItemsPlaced > 0)
        assertTrue(state.currentSpecification!!.usableYieldPercentage > BigDecimal.ZERO)
        assertNull(state.errorMessage)
    }

    @Test
    fun `addCandidate and removeCandidate should dynamically update candidate pool and re-optimize`() {
        val viewModel = createTestViewModel()

        val initialCount = viewModel.uiState.value.candidatePool.size

        val newItem = NestingCandidateItemDto(
            jobId = "JOB-SPECIAL-01",
            orderId = "ORD-SP",
            orderItemId = "ITEM-SP",
            productName = "Special Postcard",
            finishedWidthMm = BigDecimal("100.0000"),
            finishedHeightMm = BigDecimal("150.0000"),
            requiredQuantity = 500L,
            paperStockType = "ART_CARD",
            gsm = BigDecimal("300.0000")
        )

        viewModel.addCandidate(newItem)

        val stateAfterAdd = viewModel.uiState.value
        assertEquals(initialCount + 1, stateAfterAdd.candidatePool.size)
        assertTrue(stateAfterAdd.currentSpecification!!.placements.any { it.jobId == "JOB-SPECIAL-01" })

        viewModel.removeCandidate("JOB-SPECIAL-01")

        val stateAfterRemove = viewModel.uiState.value
        assertEquals(initialCount, stateAfterRemove.candidatePool.size)
        assertFalse(stateAfterRemove.currentSpecification!!.placements.any { it.jobId == "JOB-SPECIAL-01" })
    }

    @Test
    fun `tab selection and input change handlers should update state cleanly`() {
        val viewModel = createTestViewModel()
        viewModel.onTabSelected(2)
        assertEquals(2, viewModel.uiState.value.selectedTab)

        viewModel.onNameChanged("Custom Batch")
        assertEquals("Custom Batch", viewModel.uiState.value.name)

        viewModel.onSheetWidthChanged("700.0000")
        assertEquals("700.0000", viewModel.uiState.value.parentSheetWidthMm)
    }
}
