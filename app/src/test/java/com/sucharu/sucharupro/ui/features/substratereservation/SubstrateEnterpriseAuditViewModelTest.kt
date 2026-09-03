package com.sucharu.sucharupro.ui.features.substratereservation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for SubstrateEnterpriseAuditViewModel (Module 19 Step 06).
 */
class SubstrateEnterpriseAuditViewModelTest {

    private fun createTestViewModel(): SubstrateEnterpriseAuditViewModel {
        return SubstrateEnterpriseAuditViewModel(
            externalScope = CoroutineScope(Dispatchers.Unconfined)
        )
    }

    @Test
    fun testInitialStateAndTabSelection() {
        val viewModel = createTestViewModel()
        val state = viewModel.uiState.value

        assertEquals(EnterpriseAuditTab.OVERVIEW, state.selectedTab)
        assertNotNull(state.governanceSummary)
        assertTrue(state.auditEvents.isNotEmpty())
        assertNotNull(state.activeReconciliation)
        assertNotNull(state.integrityResult)
        assertNotNull(state.aiHandoffContract)
        assertEquals("6.0.0", state.aiHandoffContract?.contractVersion)

        // Select Tab
        viewModel.selectTab(EnterpriseAuditTab.AUDIT_TRAIL)
        assertEquals(EnterpriseAuditTab.AUDIT_TRAIL, viewModel.uiState.value.selectedTab)
    }

    @Test
    fun testReconciliationExecution_UpdatesState() {
        val viewModel = createTestViewModel()
        viewModel.runReconciliation("RES-TEST-01")

        val state = viewModel.uiState.value
        assertNotNull(state.activeReconciliation)
        assertEquals("HEALTHY", state.activeReconciliation?.status)
        assertTrue(state.successMessage!!.contains("Reconciliation completed"))
    }

    @Test
    fun testIntegrityVerification_UpdatesState() {
        val viewModel = createTestViewModel()
        viewModel.verifyAuditIntegrity("RES-TEST-01")

        val state = viewModel.uiState.value
        assertNotNull(state.integrityResult)
        assertTrue(state.integrityResult!!.isValidChain)
        assertEquals("INTACT", state.integrityResult?.status)
        assertTrue(state.successMessage!!.contains("Integrity verified"))
    }

    @Test
    fun testAiHandoffSynthesis_GeneratesV6Contract() {
        val viewModel = createTestViewModel()
        viewModel.generateAiHandoff("RES-TEST-01")

        val state = viewModel.uiState.value
        val contract = state.aiHandoffContract
        assertNotNull(contract)
        assertEquals("6.0.0", contract?.contractVersion)
        assertTrue(contract!!.isReadOnly)
        assertTrue(contract.forbiddenActions.contains("MUTATE_RESERVATION_STATE"))
        assertTrue(state.successMessage!!.contains("v6.0.0"))
    }
}
