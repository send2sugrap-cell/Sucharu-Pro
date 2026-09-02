package com.sucharu.sucharupro.ui.features.production.jobclosure

import com.sucharu.sucharupro.data.datasource.jobclosure.FakeProductionJobClosureDataSource
import com.sucharu.sucharupro.data.repository.jobclosure.ProductionJobClosureRepositoryImpl
import com.sucharu.sucharupro.domain.model.jobclosure.JobClosureStatus
import com.sucharu.sucharupro.domain.service.jobclosure.ProductionJobClosureServiceImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class ProductionJobClosureViewModelTest {

    private lateinit var viewModel: ProductionJobClosureViewModel
    private lateinit var fakeDs: FakeProductionJobClosureDataSource
    private val tenantId = "TENANT-001"
    private val jobId = "JOB-101"

    @Before
    fun setup() {
        fakeDs = FakeProductionJobClosureDataSource()
        val repo = ProductionJobClosureRepositoryImpl(fakeDs)
        val service = ProductionJobClosureServiceImpl(repo)

        viewModel = ProductionJobClosureViewModel(
            closureService = service,
            defaultTenantId = tenantId,
            externalScope = CoroutineScope(Dispatchers.Unconfined)
        )
    }

    @Test
    fun `test pre-closure audit updates UI state`() = runBlocking {
        viewModel.auditReadiness(jobId = jobId, orderId = "ORD-101")

        val state = viewModel.uiState.value
        assertNotNull(state.readinessAudit)
        assertTrue(state.successMessage?.contains("Pre-closure audit complete") == true)
    }

    @Test
    fun `test close and seal job updates UI state with master hash`() = runBlocking {
        viewModel.closeAndSealJob(
            jobId = jobId,
            orderId = "ORD-101",
            orderQuantity = BigDecimal("5000.0000"),
            goodUnitsReleased = BigDecimal("5000.0000"),
            estimatedTotalCost = BigDecimal("20000.0000"),
            actualTotalCost = BigDecimal("20000.0000"),
            totalCostVariance = BigDecimal.ZERO
        )

        val state = viewModel.uiState.value
        assertNotNull(state.closureRecord)
        assertEquals("GOVERNANCE_SEALED", state.closureRecord?.closureStatus)
        assertEquals(64, state.closureRecord?.masterCertificate?.masterSealHash?.length)
        assertFalse(state.isCloseJobDialogOpen)
        assertTrue(state.successMessage?.contains("Job sealed & closed successfully") == true)
    }
}
