package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.ProductionQc
import com.sucharu.sucharupro.domain.model.qc.QcStatus
import com.sucharu.sucharupro.domain.model.qc.QcType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Cross-job isolation tests for QC domain (Module 06 Step 01).
 */
class QcCrossJobIsolationTest {

    @Test
    fun qcRecord_isBoundToSpecificJob() {
        val qc1 = ProductionQc(
            qcId = "qc-job1",
            productionJobId = "job-001",
            qcType = QcType.PRE_PRODUCTION,
            status = QcStatus.DRAFT,
            createdAt = "2026-08-16T10:00:00Z",
            updatedAt = "2026-08-16T10:00:00Z"
        )

        val qc2 = ProductionQc(
            qcId = "qc-job2",
            productionJobId = "job-002",
            qcType = QcType.FINAL,
            status = QcStatus.DRAFT,
            createdAt = "2026-08-16T10:00:00Z",
            updatedAt = "2026-08-16T10:00:00Z"
        )

        assertEquals("job-001", qc1.productionJobId)
        assertEquals("job-002", qc2.productionJobId)
        assertTrue(ProductionQcValidator.validateQc(qc1) is DomainResult.Success)
        assertTrue(ProductionQcValidator.validateQc(qc2) is DomainResult.Success)
    }
}
