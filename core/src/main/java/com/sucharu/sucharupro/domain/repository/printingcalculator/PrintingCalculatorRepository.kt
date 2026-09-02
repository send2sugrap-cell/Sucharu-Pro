package com.sucharu.sucharupro.domain.repository.printingcalculator

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.printingcalculator.PrintingCalculationResult

/**
 * Domain Repository interface for Smart Printing Calculator.
 * Module 17 Step 01.
 */
interface PrintingCalculatorRepository {
    suspend fun saveCalculation(result: PrintingCalculationResult): DomainResult<PrintingCalculationResult>
    suspend fun findCalculationById(tenantId: String, calculationId: String): DomainResult<PrintingCalculationResult?>
    suspend fun findCalculationByFingerprint(tenantId: String, fingerprint: String): DomainResult<PrintingCalculationResult?>
    suspend fun listCalculations(tenantId: String, limit: Int = 50): DomainResult<List<PrintingCalculationResult>>
}
