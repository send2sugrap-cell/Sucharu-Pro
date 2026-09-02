package com.sucharu.sucharupro.domain.model

import com.sucharu.sucharupro.domain.model.qc.governance.QcGovernanceKpi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QcGovernanceKpiTest {

    @Test
    fun `all 16 canonical KPIs are defined with labels and units`() {
        assertEquals(16, QcGovernanceKpi.entries.size)

        val firstPass = QcGovernanceKpi.FIRST_PASS_RATE
        assertEquals("First-Pass QC Rate", firstPass.defaultLabel)
        assertEquals("%", firstPass.unit)
        assertTrue(firstPass.isHigherBetter)

        val defectRate = QcGovernanceKpi.DEFECT_RATE
        assertEquals("%", defectRate.unit)
        assertEquals(false, defectRate.isHigherBetter)
    }

    @Test
    fun `fromString matches correctly and returns null for invalid`() {
        assertEquals(QcGovernanceKpi.QUALITY_EFFICIENCY_SCORE, QcGovernanceKpi.fromString("QUALITY_EFFICIENCY_SCORE"))
        assertEquals(QcGovernanceKpi.CRITICAL_DEFECT_RATE, QcGovernanceKpi.fromString("critical_defect_rate"))
        assertNull(QcGovernanceKpi.fromString("INVALID_KPI"))
        assertNull(QcGovernanceKpi.fromString(null))
    }
}
