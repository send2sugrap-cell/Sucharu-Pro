package com.sucharu.sucharupro.ui.features.imposition

import com.sucharu.sucharupro.domain.model.imposition.CtpOutputStatus
import com.sucharu.sucharupro.domain.model.imposition.PlateColorSeparation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [CtpViewModel].
 * Module 18 Step 05.
 */
class CtpViewModelTest {

    private lateinit var viewModel: CtpViewModel

    @Before
    fun setUp() {
        viewModel = CtpViewModel(
            ctpService = null,
            tenantId = "tenant_test_vm",
            externalScope = CoroutineScope(Dispatchers.Unconfined)
        )
    }

    @Test
    fun testInit_generatesBaselineCtpPackage() {
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertNotNull(state.currentSpecification)

        val pkg = state.currentSpecification?.outputPackage
        assertNotNull(pkg)
        assertEquals(8, pkg?.totalPlatesCount) // 2 forms * 4 colors
        assertNotNull(state.handoffContractJson)
    }

    @Test
    fun testTabSelectionAndPlateToggles() {
        viewModel.selectTab(2)
        assertEquals(2, viewModel.uiState.value.selectedTab)

        viewModel.selectPlateIndex(3)
        assertEquals(3, viewModel.uiState.value.activePlateIndex)

        viewModel.selectColorChannel(PlateColorSeparation.CYAN)
        assertEquals(PlateColorSeparation.CYAN, viewModel.uiState.value.activeColorChannel)

        viewModel.toggleMarks(false)
        assertFalse(viewModel.uiState.value.showMarks)

        viewModel.toggleGripperZone(false)
        assertFalse(viewModel.uiState.value.showGripperZone)
    }

    @Test
    fun testApproveCtpPackage_updatesStatusToApproved() {
        viewModel.approveCtpPackage()

        val state = viewModel.uiState.value
        assertEquals(CtpOutputStatus.APPROVED.name, state.currentSpecification?.status)
    }
}
