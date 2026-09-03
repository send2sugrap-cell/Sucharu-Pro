package com.sucharu.sucharupro.ui.features.substratereservation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for SubstrateReleaseGovernanceViewModel.
 * Module 19 Step 05.
 */
class SubstrateReleaseGovernanceViewModelTest {

    private fun createTestViewModel(): SubstrateReleaseGovernanceViewModel {
        return SubstrateReleaseGovernanceViewModel(
            externalScope = CoroutineScope(Dispatchers.Unconfined)
        )
    }

    @Test
    fun testInitialStateAndTabSelection() {
        val viewModel = createTestViewModel()
        val state = viewModel.uiState.value

        assertNotNull(state.currentRecord)
        assertEquals(0, state.selectedTab)
        assertNull(state.errorMessage)
        assertNotNull(state.successMessage)
        assertEquals("ART-300-25X36", state.currentRecord?.sku)
        assertEquals(7000L, state.currentRecord?.releasableSheets)

        // Select Tab 3 (Release Execution)
        viewModel.selectTab(3)
        assertEquals(3, viewModel.uiState.value.selectedTab)
    }

    @Test
    fun testEvaluateCancellation_UpdatesState() {
        val viewModel = createTestViewModel()

        viewModel.evaluateCancellation(
            reservationId = "RES-NEW-01",
            orderId = "ORD-NEW-01",
            orderItemId = "ITEM-01",
            executionJobId = "JOB-NEW-01",
            sku = "COATED-250",
            materialName = "Coated 250 GSM",
            warehouseId = "WH-01",
            allocatedSheets = 5000L,
            consumedSheets = 0L,
            committedSheets = 0L,
            productionStatus = "READY"
        )

        val state = viewModel.uiState.value
        assertNotNull(state.currentRecord)
        assertEquals("COATED-250", state.currentRecord?.sku)
        assertEquals("RELEASE_ELIGIBLE", state.currentRecord?.decision)
        assertEquals(5000L, state.currentRecord?.releasableSheets)
    }

    @Test
    fun testEvaluateRevision_QuantityReduction_UpdatesState() {
        val viewModel = createTestViewModel()

        viewModel.evaluateRevision(
            reservationId = "RES-REV-01",
            orderId = "ORD-REV-01",
            orderItemId = "ITEM-01",
            executionJobId = "JOB-REV-01",
            sku = "BOND-90",
            materialName = "Bond Paper 90 GSM",
            warehouseId = "WH-01",
            previousRequiredSheets = 10000L,
            newRequiredSheets = 7000L,
            allocatedSheets = 10000L,
            consumedSheets = 0L,
            committedSheets = 0L,
            productionStatus = "READY"
        )

        val state = viewModel.uiState.value
        assertNotNull(state.currentRecord)
        assertEquals("BOND-90", state.currentRecord?.sku)
        assertEquals("PARTIAL_RELEASE_ELIGIBLE", state.currentRecord?.decision)
        assertEquals(3000L, state.currentRecord?.releasableSheets)
    }

    @Test
    fun testApproveAndExecuteWorkflow() {
        val viewModel = createTestViewModel()
        val govId = viewModel.uiState.value.currentRecord!!.governanceId

        // Approve
        viewModel.approveRelease(govId, "Supervisor approval granted")
        assertEquals("APPROVED", viewModel.uiState.value.currentRecord?.executionStatus)

        // Execute
        viewModel.executeRelease(govId)
        assertEquals("RELEASE_EXECUTED", viewModel.uiState.value.currentRecord?.executionStatus)
        assertTrue(viewModel.uiState.value.successMessage!!.contains("Substrate release executed!"))
    }

    @Test
    fun testHandoffJsonPreview_ContainsRequiredContractFields() {
        val viewModel = createTestViewModel()
        val preview = viewModel.uiState.value.jsonHandoffPreview

        assertNotNull(preview)
        assertTrue(preview!!.contains("\"contractVersion\": \"5.0.0\""))
        assertTrue(preview.contains("\"governanceId\":"))
        assertTrue(preview.contains("\"deduplicationFingerprint\":"))
        assertTrue(preview.contains("\"masterIntegrityHash\":"))
    }
}
