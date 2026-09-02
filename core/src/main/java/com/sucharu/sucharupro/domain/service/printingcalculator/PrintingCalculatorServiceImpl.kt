package com.sucharu.sucharupro.domain.service.printingcalculator

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.printingcalculator.*
import com.sucharu.sucharupro.domain.repository.printingcalculator.PrintingCalculatorRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Implementation of PrintingCalculatorService.
 * Module 17 Step 01.
 */
class PrintingCalculatorServiceImpl(
    private val repository: PrintingCalculatorRepository
) : PrintingCalculatorService {

    private val mutex = Mutex()

    override suspend fun calculate(request: PrintingCalculationRequest): DomainResult<PrintingCalculationResult> {
        val validation = PrintingCalculatorValidator.validateRequest(request)
        if (validation.hasErrors) {
            return DomainResult.Error(
                message = "Calculation request validation failed: ${validation.errorMessages.joinToString("; ")}"
            )
        }

        val normalizedSpec = PrintingSpecificationNormalizer.normalize(request)
        val normalizedValidation = PrintingCalculatorValidator.validateNormalized(normalizedSpec)
        val allDiagnostics = validation.diagnostics + normalizedValidation.diagnostics

        val calculatedResult = PrintingCalculatorEngine.calculate(
            request = request,
            spec = normalizedSpec,
            initialDiagnostics = allDiagnostics
        )

        // Mutex & Idempotency check
        mutex.withLock {
            val existing = repository.findCalculationByFingerprint(request.tenantId, calculatedResult.requestFingerprint)
            if (existing is DomainResult.Success && existing.data != null) {
                return DomainResult.Success(existing.data!!)
            }
            repository.saveCalculation(calculatedResult)
        }

        return DomainResult.Success(calculatedResult)
    }

    override suspend fun getCalculationById(tenantId: String, calculationId: String): DomainResult<PrintingCalculationResult?> {
        return repository.findCalculationById(tenantId, calculationId)
    }

    override suspend fun getCalculationBreakdown(tenantId: String, calculationId: String): DomainResult<List<CalculationBreakdownItem>> {
        val calcRes = repository.findCalculationById(tenantId, calculationId)
        if (calcRes is DomainResult.Success && calcRes.data != null) {
            return DomainResult.Success(calcRes.data!!.breakdownItems)
        }
        return DomainResult.Error(message = "Calculation not found: $calculationId")
    }

    override suspend fun validateRequest(request: PrintingCalculationRequest): DomainResult<PrintingCalculatorValidator.ValidationResult> {
        val result = PrintingCalculatorValidator.validateRequest(request)
        return DomainResult.Success(result)
    }

    override suspend fun exportHandoffContract(tenantId: String, calculationId: String): DomainResult<Module17Step01PrintingCalculatorHandoffContract> {
        val calcRes = repository.findCalculationById(tenantId, calculationId)
        if (calcRes !is DomainResult.Success || calcRes.data == null) {
            return DomainResult.Error(message = "Calculation not found for handoff export: $calculationId")
        }

        val calc = calcRes.data!!
        val spec = calc.normalizedSpecification
        val now = System.currentTimeMillis()
        val handoffId = "handoff-calc-${calc.calculationId}".take(64)

        val handoffHash = PrintingCalculatorMathUtils.sha256(
            "HANDOFF-V1:${calc.calculationId}:${calc.tenantId}:${calc.requestFingerprint}:${calc.totalEstimatedCost ?: "NONE"}:${calc.integrityHash}:$now"
        )

        val contract = Module17Step01PrintingCalculatorHandoffContract(
            handoffId = handoffId,
            calculationId = calc.calculationId,
            tenantId = calc.tenantId,
            projectId = calc.projectId,
            generatedAt = now,
            contractVersion = "1.0.0",
            requestFingerprint = calc.requestFingerprint,
            calculationStatus = calc.status,
            classification = calc.classification,
            jobTitle = spec.jobTitle,
            orderedQuantity = spec.quantity.orderedQuantity,
            finishedDimensionsMm = "${spec.normalizedDimensionMm.width}mm x ${spec.normalizedDimensionMm.height}mm",
            substrateDetails = "${spec.material.materialName} (${spec.material.stockType.displayName}${spec.material.gsm?.let { " $it GSM" } ?: ""})",
            totalSheetsRequired = calc.materialRequirement.totalSheetsRequired,
            totalImpressions = calc.printingRequirement.totalImpressions,
            totalEstimatedCost = calc.totalEstimatedCost,
            estimatedUnitCost = calc.estimatedUnitCost,
            currency = calc.currency,
            diagnosticsSummary = calc.diagnostics.map { "[${it.severity}] ${it.code}: ${it.message}" },
            breakdownSummary = calc.breakdownItems,
            isReadOnly = true,
            handoffIntegrityHash = handoffHash
        )

        return DomainResult.Success(contract)
    }

    override suspend fun listCalculations(tenantId: String, limit: Int): DomainResult<List<PrintingCalculationResult>> {
        return repository.listCalculations(tenantId, limit)
    }
}
