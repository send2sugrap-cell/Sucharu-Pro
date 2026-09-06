package com.sucharu.sucharupro.data.repository.printingcalculator

import com.sucharu.sucharupro.data.api.client.BackendApiClient
import com.sucharu.sucharupro.data.api.model.ApiResult
import com.sucharu.sucharupro.data.api.model.printingcalculator.*
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.printingcalculator.*
import com.sucharu.sucharupro.domain.service.printingcalculator.PrintingCalculatorService
import com.sucharu.sucharupro.domain.service.printingcalculator.PrintingCalculatorValidator

/**
 * Production HTTP REST API implementation of [PrintingCalculatorService] (Phase 03 Step 01).
 * Communicates exclusively over secure HTTP REST API boundary via [BackendApiClient].
 * Guarantees zero local/demo fallbacks in production.
 */
class HttpPrintingCalculatorService(
    private val client: BackendApiClient
) : PrintingCalculatorService {

    override suspend fun calculate(request: PrintingCalculationRequest): DomainResult<PrintingCalculationResult> {
        val reqDto = PrintingCalculationRequestDto(
            jobTitle = request.jobTitle,
            productType = request.productType.name,
            quantity = request.quantity,
            quantityUnit = request.quantityUnit.name,
            finishedWidth = request.finishedWidth.toPlainString(),
            finishedHeight = request.finishedHeight.toPlainString(),
            dimensionUnit = request.dimensionUnit.name,
            materialName = request.materialName,
            stockType = request.stockType.name,
            gsm = request.gsm?.toPlainString(),
            sheetWidth = request.sheetWidth?.toPlainString(),
            sheetHeight = request.sheetHeight?.toPlainString(),
            sheetDimensionUnit = request.sheetDimensionUnit.name,
            materialUnitPricePerSheet = request.materialUnitPricePerSheet?.toPlainString(),
            processType = request.processType.name,
            sides = request.sides.name,
            colorMode = request.colorMode.name,
            frontColorsCount = request.frontColorsCount,
            backColorsCount = request.backColorsCount,
            spotColorsCount = request.spotColorsCount,
            setupSheets = request.setupSheets,
            runningWastePercentage = request.runningWastePercentage.toPlainString(),
            finishingWastePercentage = request.finishingWastePercentage.toPlainString(),
            finishingOperations = request.finishingOperations.map { f ->
                FinishingOperationInputDto(
                    operationType = f.operationType.name,
                    description = f.description,
                    unitRate = f.unitRate?.toPlainString(),
                    setupRate = f.setupRate?.toPlainString(),
                    isOptional = f.isOptional
                )
            },
            machine = request.machine?.let { m ->
                MachineSpecificationDto(
                    machineId = m.machineId,
                    machineName = m.machineName,
                    processType = m.processType.name,
                    hourlyRate = m.hourlyRate?.toPlainString(),
                    impressionsPerHour = m.impressionsPerHour,
                    plateCostPerUnit = m.plateCostPerUnit?.toPlainString()
                )
            },
            currency = request.currency,
            idempotencyKey = request.idempotencyKey
        )

        return when (val res = client.calculatePrintingCost(reqDto)) {
            is ApiResult.Success -> DomainResult.Success(res.data.toDomain())
            is ApiResult.Error -> DomainResult.Error(message = res.errorResponse.message)
        }
    }

    override suspend fun getCalculationById(tenantId: String, calculationId: String): DomainResult<PrintingCalculationResult?> {
        return when (val res = client.getPrintingCalculationById(calculationId)) {
            is ApiResult.Success -> DomainResult.Success(res.data.toDomain())
            is ApiResult.Error -> DomainResult.Error(message = res.errorResponse.message)
        }
    }

    override suspend fun getCalculationBreakdown(tenantId: String, calculationId: String): DomainResult<List<CalculationBreakdownItem>> {
        return when (val res = client.getPrintingCalculationBreakdown(calculationId)) {
            is ApiResult.Success -> DomainResult.Success(res.data.map { it.toDomain() })
            is ApiResult.Error -> DomainResult.Error(message = res.errorResponse.message)
        }
    }

    override suspend fun validateRequest(request: PrintingCalculationRequest): DomainResult<PrintingCalculatorValidator.ValidationResult> {
        val reqDto = PrintingCalculationRequestDto(
            jobTitle = request.jobTitle,
            productType = request.productType.name,
            quantity = request.quantity,
            finishedWidth = request.finishedWidth.toPlainString(),
            finishedHeight = request.finishedHeight.toPlainString(),
            materialName = request.materialName
        )
        return when (val res = client.validatePrintingCalculation(reqDto)) {
            is ApiResult.Success -> {
                val data = res.data
                val diagnostics = data.diagnostics.map { it.toDomain() }
                val valResult = PrintingCalculatorValidator.ValidationResult(isValid = data.isValid, diagnostics = diagnostics)
                DomainResult.Success(valResult)
            }
            is ApiResult.Error -> DomainResult.Error(message = res.errorResponse.message)
        }
    }

    override suspend fun exportHandoffContract(tenantId: String, calculationId: String): DomainResult<Module17Step01PrintingCalculatorHandoffContract> {
        return when (val res = client.getPrintingCalculatorHandoffContract(calculationId)) {
            is ApiResult.Success -> DomainResult.Success(res.data.toDomain())
            is ApiResult.Error -> DomainResult.Error(message = res.errorResponse.message)
        }
    }

    override suspend fun listCalculations(tenantId: String, limit: Int): DomainResult<List<PrintingCalculationResult>> {
        return when (val res = client.listPrintingCalculations()) {
            is ApiResult.Success -> DomainResult.Success(res.data.map { it.toDomain() })
            is ApiResult.Error -> DomainResult.Error(message = res.errorResponse.message)
        }
    }
}
