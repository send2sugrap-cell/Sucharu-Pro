package com.sucharu.sucharupro.ui.features.printing

import com.sucharu.sucharupro.data.api.model.printingcalculator.PrintingCalculationRequestDto
import com.sucharu.sucharupro.data.datasource.printingcalculator.FakePrintingCalculatorDataSource
import com.sucharu.sucharupro.data.repository.printingcalculator.PrintingCalculatorRepositoryImpl
import com.sucharu.sucharupro.domain.service.printingcalculator.PrintingCalculatorServiceImpl
import com.sucharu.sucharupro.ui.features.printing.calculator.PrintingCalculatorViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PrintingCalculatorViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: PrintingCalculatorViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val dataSource = FakePrintingCalculatorDataSource()
        val repository = PrintingCalculatorRepositoryImpl(dataSource)
        val service = PrintingCalculatorServiceImpl(repository)
        viewModel = PrintingCalculatorViewModel(calculatorService = service)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testCalculate_updatesUiStateWithResult() = runTest {
        val reqDto = PrintingCalculationRequestDto(
            jobTitle = "Test Business Card",
            quantity = 1000L,
            finishedWidth = "90",
            finishedHeight = "54",
            materialName = "Art Card 300 GSM",
            materialUnitPricePerSheet = "12.0000"
        )

        assertNull(viewModel.uiState.value.calculationResult)
        assertFalse(viewModel.uiState.value.isLoading)

        viewModel.calculate(reqDto)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertNotNull(state.calculationResult)
        assertEquals("1000", state.calculationResult?.normalizedSpecification?.orderedQuantity?.toString())
    }

    @Test
    fun testDuplicateSubmissionProtection_ignoresSubsequentCallsWhileLoading() = runTest {
        var callCount = 0
        val baseService = com.sucharu.sucharupro.domain.service.printingcalculator.PrintingCalculatorServiceImpl(PrintingCalculatorRepositoryImpl(FakePrintingCalculatorDataSource()))
        val delayingService = object : com.sucharu.sucharupro.domain.service.printingcalculator.PrintingCalculatorService by baseService {
            override suspend fun calculate(request: com.sucharu.sucharupro.domain.model.printingcalculator.PrintingCalculationRequest): com.sucharu.sucharupro.domain.model.common.DomainResult<com.sucharu.sucharupro.domain.model.printingcalculator.PrintingCalculationResult> {
                callCount++
                kotlinx.coroutines.delay(500)
                return baseService.calculate(request)
            }
        }
        val customVm = PrintingCalculatorViewModel(calculatorService = delayingService)

        val reqDto = PrintingCalculationRequestDto(
            jobTitle = "Test Poster",
            quantity = 500L,
            finishedWidth = "297",
            finishedHeight = "420",
            materialName = "Art Paper 150 GSM"
        )

        customVm.calculate(reqDto)
        testDispatcher.scheduler.runCurrent()
        assertTrue(customVm.uiState.value.isLoading)

        // Rapid duplicate call while in-flight
        customVm.calculate(reqDto)
        assertEquals(1, callCount)

        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(customVm.uiState.value.isLoading)
        assertNotNull(customVm.uiState.value.calculationResult)
    }
}
