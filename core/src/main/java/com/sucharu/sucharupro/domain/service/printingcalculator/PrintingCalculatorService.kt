package com.sucharu.sucharupro.domain.service.printingcalculator

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.printingcalculator.CalculationBreakdownItem
import com.sucharu.sucharupro.domain.model.printingcalculator.Module17Step01PrintingCalculatorHandoffContract
import com.sucharu.sucharupro.domain.model.printingcalculator.PrintingCalculationRequest
import com.sucharu.sucharupro.domain.model.printingcalculator.PrintingCalculationResult

/**
 * Domain Service contract for the Smart Printing Calculator.
 * Module 17 Step 01.
 */
interface PrintingCalculatorService {
    suspend fun calculate(request: PrintingCalculationRequest): DomainResult<PrintingCalculationResult>
    suspend fun getCalculationById(tenantId: String, calculationId: String): DomainResult<PrintingCalculationResult?>
    suspend fun getCalculationBreakdown(tenantId: String, calculationId: String): DomainResult<List<CalculationBreakdownItem>>
    suspend fun validateRequest(request: PrintingCalculationRequest): DomainResult<PrintingCalculatorValidator.ValidationResult>
    suspend fun exportHandoffContract(tenantId: String, calculationId: String): DomainResult<Module17Step01PrintingCalculatorHandoffContract>
    suspend fun listCalculations(tenantId: String, limit: Int = 50): DomainResult<List<PrintingCalculationResult>>
}
