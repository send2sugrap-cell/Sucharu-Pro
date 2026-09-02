package com.sucharu.sucharupro.ui.features.governance

import com.sucharu.sucharupro.data.api.model.businessintegrity.FinancialControlAssertionDto
import com.sucharu.sucharupro.data.api.model.businessintegrity.FinancialIntegrityRunDto
import com.sucharu.sucharupro.data.api.model.businessintegrity.PeriodCloseCertificateDto
import com.sucharu.sucharupro.data.api.model.businessintegrity.PeriodFinalizationReadinessDto
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class FinancialGovernanceUiTest {

    @Test
    fun `test FinancialIntegrityRunDto and assertions data mapping`() {
        val assertion = FinancialControlAssertionDto(
            id = "A-1",
            tenantId = "T1",
            projectId = "P1",
            runId = "R1",
            periodId = "PER-1",
            assertionType = "ASSERTION_01_LEDGER_BALANCE",
            assertionName = "Ledger Debit/Credit Balance Integrity",
            status = "PASSED",
            severity = "CRITICAL",
            expectedValue = "Debits == Credits",
            actualValue = "Debits: 5000, Credits: 5000",
            explanation = "Balanced ledger",
            sourceEntitiesCount = 2
        )

        val run = FinancialIntegrityRunDto(
            id = "R1",
            tenantId = "T1",
            projectId = "P1",
            periodId = "PER-1",
            runNumber = "IRUN-1001",
            status = "PASSED",
            executedBy = "ADMIN-1",
            totalAssertionsCount = 18,
            passedAssertionsCount = 18,
            integrityChecksum = "abcd1234checksum",
            assertions = listOf(assertion)
        )

        assertEquals("PASSED", run.status)
        assertEquals(18, run.passedAssertionsCount)
        assertEquals(1, run.assertions.size)
        assertEquals("ASSERTION_01_LEDGER_BALANCE", run.assertions[0].assertionType)
    }

    @Test
    fun `test PeriodFinalizationReadinessDto data structure`() {
        val readiness = PeriodFinalizationReadinessDto(
            periodId = "PER-1",
            periodCode = "2026-M08",
            status = "READY",
            isReadyForClose = true,
            blockingReasons = emptyList(),
            warnings = listOf("1 warning found"),
            latestRunStatus = "PASSED"
        )

        assertTrue(readiness.isReadyForClose)
        assertEquals("READY", readiness.status)
        assertEquals(1, readiness.warnings.size)
    }

    @Test
    fun `test PeriodCloseCertificateDto data structure`() {
        val cert = PeriodCloseCertificateDto(
            id = "CERT-1",
            tenantId = "T1",
            projectId = "P1",
            periodId = "PER-1",
            periodCode = "2026-M08",
            finalRunId = "R1",
            closedBy = "ADMIN-1",
            approvedBy = "ADMIN-1",
            status = "FINALIZED",
            totalRecognizedExpenses = BigDecimal("10000.0000"),
            totalSettledPayables = BigDecimal("5000.0000"),
            totalLedgerDebit = BigDecimal("15000.0000"),
            totalLedgerCredit = BigDecimal("15000.0000"),
            certificateChecksum = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
        )

        assertEquals("FINALIZED", cert.status)
        assertEquals(BigDecimal("10000.0000"), cert.totalRecognizedExpenses)
        assertEquals(BigDecimal("15000.0000"), cert.totalLedgerDebit)
        assertEquals(64, cert.certificateChecksum.length)
    }
}
