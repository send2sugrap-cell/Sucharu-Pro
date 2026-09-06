package com.sucharu.sucharupro.ui.features.printing.calculator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.data.api.model.printingcalculator.PrintingCalculationRequestDto
import com.sucharu.sucharupro.data.api.model.printingcalculator.PrintingCalculationResponseDto
import com.sucharu.sucharupro.data.api.model.printingcalculator.ValidationResponseDto
import com.sucharu.sucharupro.data.api.model.printingcalculator.toDomain
import com.sucharu.sucharupro.data.api.model.printingcalculator.toDto
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.service.printingcalculator.PrintingCalculatorService
import com.sucharu.sucharupro.domain.service.printingcalculator.PrintingCalculatorServiceImpl
import com.sucharu.sucharupro.data.repository.printingcalculator.PrintingCalculatorRepositoryImpl
import com.sucharu.sucharupro.data.datasource.printingcalculator.FakePrintingCalculatorDataSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PrintingCalculatorUiState(
    val calculationResult: PrintingCalculationResponseDto? = null,
    val validationResult: ValidationResponseDto? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class PrintingCalculatorViewModel(
    private val calculatorService: PrintingCalculatorService = PrintingCalculatorServiceImpl(
        PrintingCalculatorRepositoryImpl(FakePrintingCalculatorDataSource())
    )
) : ViewModel() {

    private val _uiState = MutableStateFlow(PrintingCalculatorUiState())
    val uiState: StateFlow<PrintingCalculatorUiState> = _uiState.asStateFlow()

    fun calculate(requestDto: PrintingCalculationRequestDto) {
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val domainReq = requestDto.toDomain(
                tenantId = "TENANT-001",
                projectId = "PROJECT-001",
                actorId = "USER-001"
            )
            when (val result = calculatorService.calculate(domainReq)) {
                is DomainResult.Success -> {
                    val resDto = result.data.toDto()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            calculationResult = resDto,
                            errorMessage = null
                        )
                    }
                }
                is DomainResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.message
                        )
                    }
                }
                else -> {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }
}
