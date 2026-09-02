package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Cross-project isolation tests for Final QC (Module 06 Step 07).
 */
class FinalQcCrossProjectIsolationTest {

    @Test
    fun matchingProjects_succeeds() {
        val result = FinalQcValidator.validateCrossProjectIsolation("proj-101", "proj-101")
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun mismatchedProjects_rejected() {
        val result = FinalQcValidator.validateCrossProjectIsolation("proj-101", "proj-999")
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("Cross-project reference violation"))
    }
}
