package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import org.junit.Assert.assertTrue
import org.junit.Test

class QcCostTimeCrossJobIsolationTest {

    @Test
    fun `validateCrossJobIsolation succeeds when job IDs match`() {
        assertTrue(QcCostEntryValidator.validateCrossJobIsolation("JOB-01", "JOB-01") is DomainResult.Success)
        assertTrue(QcTimeEntryValidator.validateCrossJobIsolation("JOB-01", "JOB-01") is DomainResult.Success)
    }

    @Test
    fun `validateCrossJobIsolation fails when job IDs differ`() {
        assertTrue(QcCostEntryValidator.validateCrossJobIsolation("JOB-01", "JOB-02") is DomainResult.Error)
        assertTrue(QcTimeEntryValidator.validateCrossJobIsolation("JOB-01", "JOB-02") is DomainResult.Error)
    }
}
