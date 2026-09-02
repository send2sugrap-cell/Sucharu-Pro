package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.governance.QcAlertStatus
import org.junit.Assert.assertTrue
import org.junit.Test

class QcQualityAlertLifecycleTest {

    @Test
    fun `valid alert transitions pass`() {
        assertTrue(QcGovernanceValidator.validateAlertTransition(QcAlertStatus.DETECTED, QcAlertStatus.ACKNOWLEDGED) is DomainResult.Success)
        assertTrue(QcGovernanceValidator.validateAlertTransition(QcAlertStatus.ACKNOWLEDGED, QcAlertStatus.INVESTIGATING) is DomainResult.Success)
        assertTrue(QcGovernanceValidator.validateAlertTransition(QcAlertStatus.INVESTIGATING, QcAlertStatus.ACTION_REQUIRED) is DomainResult.Success)
        assertTrue(QcGovernanceValidator.validateAlertTransition(QcAlertStatus.ACTION_REQUIRED, QcAlertStatus.RESOLVED) is DomainResult.Success)
    }

    @Test
    fun `terminal alert cannot transition`() {
        val result = QcGovernanceValidator.validateAlertTransition(QcAlertStatus.RESOLVED, QcAlertStatus.INVESTIGATING)
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("terminal status", ignoreCase = true))

        val dismissResult = QcGovernanceValidator.validateAlertTransition(QcAlertStatus.DISMISSED, QcAlertStatus.ACKNOWLEDGED)
        assertTrue(dismissResult is DomainResult.Error)
    }
}
