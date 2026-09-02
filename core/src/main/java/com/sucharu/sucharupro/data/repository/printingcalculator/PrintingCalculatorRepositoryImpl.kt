package com.sucharu.sucharupro.data.repository.printingcalculator

import com.sucharu.sucharupro.data.datasource.printingcalculator.PrintingCalculatorDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.printingcalculator.PrintingCalculationResult
import com.sucharu.sucharupro.domain.repository.printingcalculator.PrintingCalculatorRepository

/**
 * Implementation of PrintingCalculatorRepository delegating to DataSource.
 * Module 17 Step 01.
 */
class PrintingCalculatorRepositoryImpl(
    private val dataSource: PrintingCalculatorDataSource
) : PrintingCalculatorRepository {

    override suspend fun saveCalculation(result: PrintingCalculationResult): DomainResult<PrintingCalculationResult> {
        return dataSource.saveCalculation(result)
    }

    override suspend fun findCalculationById(tenantId: String, calculationId: String): DomainResult<PrintingCalculationResult?> {
        return dataSource.findCalculationById(tenantId, calculationId)
    }

    override suspend fun findCalculationByFingerprint(tenantId: String, fingerprint: String): DomainResult<PrintingCalculationResult?> {
        return dataSource.findCalculationByFingerprint(tenantId, fingerprint)
    }

    override suspend fun listCalculations(tenantId: String, limit: Int): DomainResult<List<PrintingCalculationResult>> {
        return dataSource.listCalculations(tenantId, limit)
    }
}
