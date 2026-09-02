package com.sucharu.sucharupro.data.datasource.printingcalculator

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.printingcalculator.PrintingCalculationResult

/**
 * Data Source contract for Smart Printing Calculator.
 * Module 17 Step 01.
 */
interface PrintingCalculatorDataSource {
    suspend fun saveCalculation(result: PrintingCalculationResult): DomainResult<PrintingCalculationResult>
    suspend fun findCalculationById(tenantId: String, calculationId: String): DomainResult<PrintingCalculationResult?>
    suspend fun findCalculationByFingerprint(tenantId: String, fingerprint: String): DomainResult<PrintingCalculationResult?>
    suspend fun listCalculations(tenantId: String, limit: Int = 50): DomainResult<List<PrintingCalculationResult>>
}
