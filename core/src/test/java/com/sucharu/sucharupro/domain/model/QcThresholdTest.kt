package com.sucharu.sucharupro.domain.model

import com.sucharu.sucharupro.domain.model.qc.governance.QcGovernanceKpi
import com.sucharu.sucharupro.domain.model.qc.governance.QcKpiThreshold
import com.sucharu.sucharupro.domain.model.qc.governance.QcThresholdSeverity
import com.sucharu.sucharupro.domain.model.qc.governance.QcThresholdStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QcThresholdTest {

    @Test
    fun `threshold evaluation record instantiates with correct variance`() {
        val threshold = QcKpiThreshold(
            kpiType = QcGovernanceKpi.FIRST_PASS_RATE,
            currentValue = 92.0,
            targetValue = 95.0,
            status = QcThresholdStatus.WARNING,
            severity = QcThresholdSeverity.WARNING,
            message = "First-Pass rate below target"
        )
        assertEquals(-3.0, threshold.varianceFromTarget, 0.001)
        assertEquals(QcThresholdStatus.WARNING, threshold.status)
        assertTrue(threshold.status.isBreached)
    }
}
