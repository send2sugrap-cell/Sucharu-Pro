package com.sucharu.sucharupro.ui.features.imposition

import com.sucharu.sucharupro.domain.model.imposition.PrepressPlanStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit Test Suite for Prepress Orchestration ViewModel.
 * Tests UI state lifecycle, tab selection, plan generation, approval, and finalization.
 * Module 18 Step 06.
 */
class PrepressOrchestrationViewModelTest {

    private fun createTestViewModel(): PrepressOrchestrationViewModel {
        return PrepressOrchestrationViewModel(
            externalScope = CoroutineScope(Dispatchers.Unconfined)
        )
    }

    @Test
    fun testInitialStateAndTabSelection() {
        val viewModel = createTestViewModel()
        val state = viewModel.uiState.value

        assertNotNull(state.currentPlan)
        assertEquals(0, state.selectedTab)
        assertNull(state.errorMessage)
        assertNotNull(state.successMessage)

        // Select Tab 2 (Reconciliation Matrix)
        viewModel.selectTab(2)
        assertEquals(2, viewModel.uiState.value.selectedTab)
    }

    @Test
    fun testApprovePlan_UpdatesStatus() {
        val viewModel = createTestViewModel()
        viewModel.approvePlan()
        val state = viewModel.uiState.value

        assertEquals(PrepressPlanStatus.APPROVED.name, state.currentPlan?.status)
        assertEquals("APPROVED", state.currentPlan?.approvalStatus)
    }

    @Test
    fun testFinalizePlan_UpdatesStatus() {
        val viewModel = createTestViewModel()
        viewModel.finalizePlan()
        val state = viewModel.uiState.value

        assertEquals(PrepressPlanStatus.FINALIZED.name, state.currentPlan?.status)
        assertEquals("FINALIZED", state.currentPlan?.approvalStatus)
    }
}
