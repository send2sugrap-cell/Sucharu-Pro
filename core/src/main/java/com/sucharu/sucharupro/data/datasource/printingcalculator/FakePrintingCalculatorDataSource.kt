package com.sucharu.sucharupro.data.datasource.printingcalculator

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.printingcalculator.PrintingCalculationResult
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe In-Memory Data Source for Printing Calculator.
 * Module 17 Step 01.
 */
class FakePrintingCalculatorDataSource : PrintingCalculatorDataSource {

    private val calculationsMap = ConcurrentHashMap<String, PrintingCalculationResult>()

    override suspend fun saveCalculation(result: PrintingCalculationResult): DomainResult<PrintingCalculationResult> {
        calculationsMap[result.calculationId] = result
        return DomainResult.Success(result)
    }

    override suspend fun findCalculationById(tenantId: String, calculationId: String): DomainResult<PrintingCalculationResult?> {
        val found = calculationsMap[calculationId]?.takeIf { it.tenantId == tenantId }
        return DomainResult.Success(found)
    }

    override suspend fun findCalculationByFingerprint(tenantId: String, fingerprint: String): DomainResult<PrintingCalculationResult?> {
        val found = calculationsMap.values.find { it.tenantId == tenantId && it.requestFingerprint == fingerprint }
        return DomainResult.Success(found)
    }

    override suspend fun listCalculations(tenantId: String, limit: Int): DomainResult<List<PrintingCalculationResult>> {
        val list = calculationsMap.values
            .filter { it.tenantId == tenantId }
            .sortedByDescending { it.calculatedAt }
            .take(limit)
        return DomainResult.Success(list)
    }

    fun clear() {
        calculationsMap.clear()
    }
}
