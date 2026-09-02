package com.sucharu.sucharupro.businessreconciliation

import com.sucharu.sucharupro.domain.model.businessreconciliation.BusinessFinancialReconciliationDiscrepancy
import com.sucharu.sucharupro.domain.model.businessreconciliation.FinancialDiscrepancyType
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.validation.businessreconciliation.BusinessFinancialReconciliationValidators
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode

class BusinessFinancialReconciliationPrecisionTest {

    @Test
    fun testMonetaryPrecisionAndScale() {
        val amount = BigDecimal("150000.5000")
        assertEquals(4, amount.scale())

        val scaled = BigDecimal("150000.5").setScale(4, RoundingMode.HALF_UP)
        assertEquals("150000.5000", scaled.toPlainString())

        val valRes = BusinessFinancialReconciliationValidators.validatePrecision(scaled)
        assertTrue(valRes is DomainResult.Success)

        val invalid = BigDecimal("150000.55555")
        val invRes = BusinessFinancialReconciliationValidators.validatePrecision(invalid)
        assertTrue(invRes is DomainResult.Error)
    }

    @Test
    fun testDiscrepancyDifferencePrecision() {
        val expected = BigDecimal("100000.0000")
        val actual = BigDecimal("95000.5555")
        val diff = (expected - actual).setScale(4, RoundingMode.HALF_UP)
        assertEquals(BigDecimal("4999.4445"), diff)

        val disc = BusinessFinancialReconciliationDiscrepancy(
            id = "DISC-PREC-01",
            tenantId = "TENANT-001",
            projectId = "PRJ-001",
            reconciliationRunId = "RUN-01",
            periodId = "PER-01",
            discrepancyType = FinancialDiscrepancyType.AMOUNT_MISMATCH,
            sourceType = "EXPENSE",
            sourceId = "EXP-01",
            expectedAmount = expected,
            actualAmount = actual,
            differenceAmount = diff,
            description = "Precision check"
        )
        assertEquals(4, disc.expectedAmount.scale())
        assertEquals(4, disc.actualAmount.scale())
        assertEquals(4, disc.differenceAmount.scale())
    }
}
