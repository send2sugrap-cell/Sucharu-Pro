package com.sucharu.sucharupro.domain.service.businessintegrity

import com.sucharu.sucharupro.domain.validation.businessintegrity.BusinessFinancialIntegrityValidator
import org.junit.Assert.*
import org.junit.Test

class FinancialGovernanceSnapshotIntegrityTest {

    @Test
    fun `calculateSha256 produces deterministic lowercase 64-character hex hash`() {
        val payload = """{"periodId":"PER-2026-M08","totalExpenses":"5000.0000","totalDebit":"5000.0000"}"""
        val hash1 = BusinessFinancialIntegrityValidator.calculateSha256(payload)
        val hash2 = BusinessFinancialIntegrityValidator.calculateSha256(payload)

        assertNotNull(hash1)
        assertEquals(64, hash1.length)
        assertEquals(hash1, hash2)
        assertTrue(hash1.matches(Regex("^[a-f0-9]{64}$")))
    }

    @Test
    fun `verifySnapshotChecksum returns true for matching payload and false for tampered payload`() {
        val validPayload = """{"periodId":"PER-2026-M08","totalExpenses":"5000.0000","totalDebit":"5000.0000"}"""
        val validHash = BusinessFinancialIntegrityValidator.calculateSha256(validPayload)

        // Valid verification
        assertTrue(BusinessFinancialIntegrityValidator.verifySnapshotChecksum(validPayload, validHash))

        // Tampered payload verification
        val tamperedPayload = """{"periodId":"PER-2026-M08","totalExpenses":"9999.0000","totalDebit":"5000.0000"}"""
        assertFalse(BusinessFinancialIntegrityValidator.verifySnapshotChecksum(tamperedPayload, validHash))
    }

    @Test
    fun `verifySnapshotChecksum handles invalid inputs gracefully`() {
        assertFalse(BusinessFinancialIntegrityValidator.verifySnapshotChecksum("", "hash"))
        assertFalse(BusinessFinancialIntegrityValidator.verifySnapshotChecksum("payload", ""))
    }
}
