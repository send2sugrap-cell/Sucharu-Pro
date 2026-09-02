package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import org.junit.Assert.assertTrue
import org.junit.Test

class QcCostTimeCrossProjectIsolationTest {

    @Test
    fun `validateCrossProjectIsolation succeeds when project IDs match`() {
        assertTrue(QcCostEntryValidator.validateCrossProjectIsolation("PRJ-01", "PRJ-01") is DomainResult.Success)
        assertTrue(QcTimeEntryValidator.validateCrossProjectIsolation("PRJ-01", "PRJ-01") is DomainResult.Success)
    }

    @Test
    fun `validateCrossProjectIsolation fails when project IDs differ`() {
        assertTrue(QcCostEntryValidator.validateCrossProjectIsolation("PRJ-01", "PRJ-02") is DomainResult.Error)
        assertTrue(QcTimeEntryValidator.validateCrossProjectIsolation("PRJ-01", "PRJ-02") is DomainResult.Error)
    }
}
