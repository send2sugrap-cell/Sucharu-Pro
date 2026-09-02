package com.sucharu.sucharupro.ui.features.returns

import com.sucharu.sucharupro.data.datasource.FakeReturnDataSource
import com.sucharu.sucharupro.data.repository.ReturnRepositoryImpl
import com.sucharu.sucharupro.domain.model.returns.ReturnDecision
import com.sucharu.sucharupro.domain.model.returns.ReturnItem
import com.sucharu.sucharupro.domain.model.returns.ReturnReason
import com.sucharu.sucharupro.domain.model.returns.ReturnRequest
import com.sucharu.sucharupro.domain.model.returns.ReturnStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ReturnInspectionViewModelTest {

    private lateinit var dataSource: FakeReturnDataSource
    private lateinit var repository: ReturnRepositoryImpl
    private val testScope = CoroutineScope(Dispatchers.Unconfined)

    private val projectId = "PRJ-VM"
    private val customerId = "CUST-VM"

    private val testReturn = ReturnRequest(
        returnId = "RET-INSP-VM-1",
        projectId = projectId,
        returnNo = "RN-INSP-100",
        customerId = customerId,
        originalChallanId = "CHAL-100",
        status = ReturnStatus.UNDER_INSPECTION,
        reason = ReturnReason.PRINTING_DEFECT,
        requestedBy = "STAFF-01",
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis(),
        version = 1L
    )

    private val testItem = ReturnItem(
        returnItemId = "RI-INSP-1",
        returnId = testReturn.returnId,
        productId = "PROD-INSP-1",
        originalChallanItemId = "CHI-100",
        requestedQuantity = 10,
        acceptedQuantity = 0,
        rejectedQuantity = 0
    )

    @Before
    fun setUp() = runBlocking {
        dataSource = FakeReturnDataSource()
        repository = ReturnRepositoryImpl(dataSource)
        dataSource.insertReturn(testReturn, listOf(testItem))
    }

    @Test
    fun `ReturnInspectionViewModel loads data, toggles checklist and saves draft`() = runBlocking {
        val viewModel = ReturnInspectionViewModel(repository, testScope)
        viewModel.loadInspection(testReturn.returnId, UserRole.QC_INSPECTOR, projectId)

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(testReturn.returnId, state.returnRequest?.returnId)
        assertTrue(state.checklist.isNotEmpty())

        val firstChecklistId = state.checklist.first().itemId
        viewModel.toggleChecklistItem(firstChecklistId)
        assertTrue(viewModel.uiState.value.checklist.first { it.itemId == firstChecklistId }.isPassed)

        viewModel.updateFindings("Defects confirmed on print surface")
        viewModel.saveDraftInspection("QC-USER-1", UserRole.QC_INSPECTOR, projectId)

        val updatedState = viewModel.uiState.value
        assertNotNull(updatedState.inspection)
        assertNotNull(updatedState.actionSuccessMessage)
    }

    @Test
    fun `ReturnInspectionViewModel approves return successfully`() = runBlocking {
        val viewModel = ReturnInspectionViewModel(repository, testScope)
        viewModel.loadInspection(testReturn.returnId, UserRole.ADMIN, projectId)

        viewModel.updateAcceptedQuantity(testItem.returnItemId, 10)
        viewModel.updateFindings("Inspection passed 100%")
        viewModel.approveReturn("ADMIN-01", UserRole.ADMIN, projectId)

        val state = viewModel.uiState.value
        assertEquals(ReturnStatus.APPROVED, state.returnRequest?.status)
        assertEquals(ReturnDecision.APPROVE, state.inspection?.decision)
        assertNotNull(state.actionSuccessMessage)
    }

    @Test
    fun `ReturnInspectionViewModel rejects return successfully`() = runBlocking {
        val viewModel = ReturnInspectionViewModel(repository, testScope)
        viewModel.loadInspection(testReturn.returnId, UserRole.MANAGER, projectId)

        viewModel.updateRejectedQuantity(testItem.returnItemId, 10)
        viewModel.rejectReturn("Defect caused by misuse", "MANAGER-01", UserRole.MANAGER, projectId)

        val state = viewModel.uiState.value
        assertEquals(ReturnStatus.REJECTED, state.returnRequest?.status)
        assertEquals(ReturnDecision.REJECT, state.inspection?.decision)
        assertNotNull(state.actionSuccessMessage)
    }
}
