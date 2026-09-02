package com.sucharu.sucharupro.data.api.model.printingcalculator

import com.sucharu.sucharupro.domain.model.printingcalculator.*
import com.sucharu.sucharupro.domain.model.product.ProductType
import java.math.BigDecimal

data class FinishingOperationInputDto(
    val operationType: String,
    val description: String? = null,
    val unitRate: String? = null,
    val setupRate: String? = null,
    val isOptional: Boolean = false
)

data class MachineSpecificationDto(
    val machineId: String? = null,
    val machineName: String? = null,
    val processType: String = "OFFSET",
    val hourlyRate: String? = null,
    val impressionsPerHour: Int? = null,
    val plateCostPerUnit: String? = null
)

data class PrintingCalculationRequestDto(
    val jobTitle: String = "Print Calculation Estimate",
    val productType: String = "PRINTING_JOB",
    val quantity: Long,
    val quantityUnit: String = "PIECES",
    val finishedWidth: String,
    val finishedHeight: String,
    val dimensionUnit: String = "MILLIMETERS",
    val materialName: String,
    val stockType: String = "ART_PAPER",
    val gsm: String? = null,
    val sheetWidth: String? = null,
    val sheetHeight: String? = null,
    val sheetDimensionUnit: String = "MILLIMETERS",
    val materialUnitPricePerSheet: String? = null,
    val processType: String = "OFFSET",
    val sides: String = "SINGLE_SIDED",
    val colorMode: String = "CMYK_FOUR_COLOR",
    val frontColorsCount: Int = 4,
    val backColorsCount: Int = 0,
    val spotColorsCount: Int = 0,
    val setupSheets: Long = 0L,
    val runningWastePercentage: String = "0.0000",
    val finishingWastePercentage: String = "0.0000",
    val finishingOperations: List<FinishingOperationInputDto> = emptyList(),
    val machine: MachineSpecificationDto? = null,
    val currency: String = "BDT",
    val idempotencyKey: String? = null
)

data class CalculationBreakdownItemDto(
    val componentCode: String,
    val description: String,
    val quantity: String,
    val unit: String,
    val unitRate: String?,
    val calculatedAmount: String?,
    val classification: String,
    val formulaReference: String,
    val diagnosticCode: String?
)

data class CalculationDiagnosticDto(
    val code: String,
    val severity: String,
    val message: String,
    val targetField: String?,
    val suggestedRemediation: String?
)

data class MaterialRequirementDto(
    val finishedItemsPerSheet: Int,
    val cutDirection: String,
    val productiveSheetsRequired: Long,
    val wasteSheetsRequired: Long,
    val totalSheetsRequired: Long,
    val totalReamsRequired: String,
    val totalWeightKg: String?,
    val estimatedMaterialCost: String?,
    val costStatus: String,
    val missingPriceReason: String?
)

data class PrintingRequirementDto(
    val totalImpressions: Long,
    val totalPasses: Int,
    val plateCount: Int,
    val estimatedPrintingCost: String?,
    val estimatedPlateCost: String?,
    val costStatus: String,
    val missingRateReason: String?
)

data class FinishingRequirementDto(
    val operations: List<CalculationBreakdownItemDto>,
    val totalEstimatedFinishingCost: String?,
    val costStatus: String
)

data class NormalizedSpecificationDto(
    val jobTitle: String,
    val productType: String,
    val finishedDimension: String,
    val normalizedDimensionMm: String,
    val orderedQuantity: Long,
    val quantityUnit: String,
    val materialName: String,
    val stockType: String,
    val gsm: String?,
    val sheetDimensionMm: String?,
    val processType: String,
    val sides: String,
    val colorMode: String,
    val totalColorsCount: Int,
    val wasteSummary: String
)

data class PrintingCalculationResponseDto(
    val calculationId: String,
    val tenantId: String,
    val projectId: String,
    val requestFingerprint: String,
    val requestedAt: Long,
    val calculatedAt: Long,
    val status: String,
    val classification: String,
    val normalizedSpecification: NormalizedSpecificationDto,
    val materialRequirement: MaterialRequirementDto,
    val printingRequirement: PrintingRequirementDto,
    val finishingRequirement: FinishingRequirementDto,
    val breakdownItems: List<CalculationBreakdownItemDto>,
    val totalEstimatedCost: String?,
    val estimatedUnitCost: String?,
    val currency: String,
    val diagnostics: List<CalculationDiagnosticDto>,
    val integrityHash: String,
    val calculationVersion: String
)

data class ValidationResponseDto(
    val isValid: Boolean,
    val hasErrors: Boolean,
    val diagnostics: List<CalculationDiagnosticDto>
)

data class Module17Step01PrintingCalculatorHandoffContractDto(
    val handoffId: String,
    val calculationId: String,
    val tenantId: String,
    val projectId: String,
    val generatedAt: Long,
    val contractVersion: String,
    val requestFingerprint: String,
    val calculationStatus: String,
    val classification: String,
    val jobTitle: String,
    val orderedQuantity: Long,
    val finishedDimensionsMm: String,
    val substrateDetails: String,
    val totalSheetsRequired: Long,
    val totalImpressions: Long,
    val totalEstimatedCost: String?,
    val estimatedUnitCost: String?,
    val currency: String,
    val diagnosticsSummary: List<String>,
    val breakdownSummary: List<CalculationBreakdownItemDto>,
    val isReadOnly: Boolean,
    val handoffIntegrityHash: String
)

// Mapping helpers
fun CalculationBreakdownItem.toDto() = CalculationBreakdownItemDto(
    componentCode = componentCode,
    description = description,
    quantity = quantity.toPlainString(),
    unit = unit,
    unitRate = unitRate?.toPlainString(),
    calculatedAmount = calculatedAmount?.toPlainString(),
    classification = classification.name,
    formulaReference = formulaReference,
    diagnosticCode = diagnosticCode?.name
)

fun CalculationDiagnostic.toDto() = CalculationDiagnosticDto(
    code = code.name,
    severity = severity.name,
    message = message,
    targetField = targetField,
    suggestedRemediation = suggestedRemediation
)

fun MaterialRequirementResult.toDto() = MaterialRequirementDto(
    finishedItemsPerSheet = finishedItemsPerSheet,
    cutDirection = cutDirection,
    productiveSheetsRequired = productiveSheetsRequired,
    wasteSheetsRequired = wasteSheetsRequired,
    totalSheetsRequired = totalSheetsRequired,
    totalReamsRequired = totalReamsRequired.toPlainString(),
    totalWeightKg = totalWeightKg?.toPlainString(),
    estimatedMaterialCost = estimatedMaterialCost?.toPlainString(),
    costStatus = costStatus.name,
    missingPriceReason = missingPriceReason
)

fun PrintingRequirementResult.toDto() = PrintingRequirementDto(
    totalImpressions = totalImpressions,
    totalPasses = totalPasses,
    plateCount = plateCount,
    estimatedPrintingCost = estimatedPrintingCost?.toPlainString(),
    estimatedPlateCost = estimatedPlateCost?.toPlainString(),
    costStatus = costStatus.name,
    missingRateReason = missingRateReason
)

fun FinishingRequirementResult.toDto() = FinishingRequirementDto(
    operations = operations.map { it.toDto() },
    totalEstimatedFinishingCost = totalEstimatedFinishingCost?.toPlainString(),
    costStatus = costStatus.name
)

fun NormalizedPrintingSpecification.toDto() = NormalizedSpecificationDto(
    jobTitle = jobTitle,
    productType = productType.name,
    finishedDimension = "${finishedDimension.width} x ${finishedDimension.height} ${finishedDimension.unit.name}",
    normalizedDimensionMm = "${normalizedDimensionMm.width}mm x ${normalizedDimensionMm.height}mm",
    orderedQuantity = quantity.orderedQuantity,
    quantityUnit = quantity.unit.name,
    materialName = material.materialName,
    stockType = material.stockType.name,
    gsm = material.gsm?.toPlainString(),
    sheetDimensionMm = material.sheetDimension?.let { "${it.width}mm x ${it.height}mm" },
    processType = processType.name,
    sides = sides.name,
    colorMode = color.colorMode.name,
    totalColorsCount = color.totalColorsCount,
    wasteSummary = "Setup: ${waste.setupSheets} sh, Run: ${waste.runningWastePercentage}%, Finish: ${waste.finishingWastePercentage}%"
)

fun PrintingCalculationResult.toDto() = PrintingCalculationResponseDto(
    calculationId = calculationId,
    tenantId = tenantId,
    projectId = projectId,
    requestFingerprint = requestFingerprint,
    requestedAt = requestedAt,
    calculatedAt = calculatedAt,
    status = status.name,
    classification = classification.name,
    normalizedSpecification = normalizedSpecification.toDto(),
    materialRequirement = materialRequirement.toDto(),
    printingRequirement = printingRequirement.toDto(),
    finishingRequirement = finishingRequirement.toDto(),
    breakdownItems = breakdownItems.map { it.toDto() },
    totalEstimatedCost = totalEstimatedCost?.toPlainString(),
    estimatedUnitCost = estimatedUnitCost?.toPlainString(),
    currency = currency,
    diagnostics = diagnostics.map { it.toDto() },
    integrityHash = integrityHash,
    calculationVersion = calculationVersion
)

fun Module17Step01PrintingCalculatorHandoffContract.toDto() = Module17Step01PrintingCalculatorHandoffContractDto(
    handoffId = handoffId,
    calculationId = calculationId,
    tenantId = tenantId,
    projectId = projectId,
    generatedAt = generatedAt,
    contractVersion = contractVersion,
    requestFingerprint = requestFingerprint,
    calculationStatus = calculationStatus.name,
    classification = classification.name,
    jobTitle = jobTitle,
    orderedQuantity = orderedQuantity,
    finishedDimensionsMm = finishedDimensionsMm,
    substrateDetails = substrateDetails,
    totalSheetsRequired = totalSheetsRequired,
    totalImpressions = totalImpressions,
    totalEstimatedCost = totalEstimatedCost?.toPlainString(),
    estimatedUnitCost = estimatedUnitCost?.toPlainString(),
    currency = currency,
    diagnosticsSummary = diagnosticsSummary,
    breakdownSummary = breakdownSummary.map { it.toDto() },
    isReadOnly = isReadOnly,
    handoffIntegrityHash = handoffIntegrityHash
)

fun PrintingCalculationRequestDto.toDomain(tenantId: String, projectId: String, actorId: String): PrintingCalculationRequest {
    val dimUnit = try { MeasurementUnit.valueOf(dimensionUnit.uppercase()) } catch (_: Exception) { MeasurementUnit.MILLIMETERS }
    val sDimUnit = try { MeasurementUnit.valueOf(sheetDimensionUnit.uppercase()) } catch (_: Exception) { MeasurementUnit.MILLIMETERS }
    val qUnit = try { QuantityUnit.valueOf(quantityUnit.uppercase()) } catch (_: Exception) { QuantityUnit.PIECES }
    val pType = try { ProductType.valueOf(productType.uppercase()) } catch (_: Exception) { ProductType.PRINTING_JOB }
    val sType = try { PaperStockType.valueOf(stockType.uppercase()) } catch (_: Exception) { PaperStockType.ART_PAPER }
    val prcType = try { PrintingProcessType.valueOf(processType.uppercase()) } catch (_: Exception) { PrintingProcessType.OFFSET }
    val sideOpt = try { PrintingSideOption.valueOf(sides.uppercase()) } catch (_: Exception) { PrintingSideOption.SINGLE_SIDED }
    val cMode = try { ColorMode.valueOf(colorMode.uppercase()) } catch (_: Exception) { ColorMode.CMYK_FOUR_COLOR }

    val finOps = finishingOperations.map { fDto ->
        val opType = try { FinishingOperationType.valueOf(fDto.operationType.uppercase()) } catch (_: Exception) { FinishingOperationType.CUTTING_TRIMMING }
        FinishingOperationSpecification(
            operationType = opType,
            description = fDto.description ?: opType.displayName,
            unitRate = fDto.unitRate?.let { BigDecimal(it) },
            setupRate = fDto.setupRate?.let { BigDecimal(it) },
            isOptional = fDto.isOptional
        )
    }

    val machineSpec = machine?.let { mDto ->
        val mProc = try { PrintingProcessType.valueOf(mDto.processType.uppercase()) } catch (_: Exception) { PrintingProcessType.OFFSET }
        MachineSpecification(
            machineId = mDto.machineId,
            machineName = mDto.machineName,
            processType = mProc,
            hourlyRate = mDto.hourlyRate?.let { BigDecimal(it) },
            impressionsPerHour = mDto.impressionsPerHour,
            plateCostPerUnit = mDto.plateCostPerUnit?.let { BigDecimal(it) }
        )
    }

    return PrintingCalculationRequest(
        tenantId = tenantId,
        projectId = projectId,
        jobTitle = jobTitle,
        productType = pType,
        quantity = quantity,
        quantityUnit = qUnit,
        finishedWidth = BigDecimal(finishedWidth),
        finishedHeight = BigDecimal(finishedHeight),
        dimensionUnit = dimUnit,
        materialName = materialName,
        stockType = sType,
        gsm = gsm?.let { BigDecimal(it) },
        sheetWidth = sheetWidth?.let { BigDecimal(it) },
        sheetHeight = sheetHeight?.let { BigDecimal(it) },
        sheetDimensionUnit = sDimUnit,
        materialUnitPricePerSheet = materialUnitPricePerSheet?.let { BigDecimal(it) },
        processType = prcType,
        sides = sideOpt,
        colorMode = cMode,
        frontColorsCount = frontColorsCount,
        backColorsCount = backColorsCount,
        spotColorsCount = spotColorsCount,
        setupSheets = setupSheets,
        runningWastePercentage = BigDecimal(runningWastePercentage),
        finishingWastePercentage = BigDecimal(finishingWastePercentage),
        finishingOperations = finOps,
        machine = machineSpec,
        currency = currency,
        requestedBy = actorId,
        idempotencyKey = idempotencyKey
    )
}
