package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.governance.QcImprovementActionStatus
import org.junit.Assert.assertTrue
import org.junit.Test

class QcImprovementActionLifecycleTest {

    @Test
    fun `valid improvement action workflow passes validation`() {
        assertTrue(QcGovernanceValidator.validateActionTransition(QcImprovementActionStatus.PROPOSED, QcImprovementActionStatus.APPROVED) is DomainResult.Success)
        assertTrue(QcGovernanceValidator.validateActionTransition(QcImprovementActionStatus.APPROVED, QcImprovementActionStatus.ASSIGNED) is DomainResult.Success)
        assertTrue(QcGovernanceValidator.validateActionTransition(QcImprovementActionStatus.ASSIGNED, QcImprovementActionStatus.IN_PROGRESS) is DomainResult.Success)
        assertTrue(QcGovernanceValidator.validateActionTransition(QcImprovementActionStatus.IN_PROGRESS, QcImprovementActionStatus.COMPLETED) is DomainResult.Success)
        assertTrue(QcGovernanceValidator.validateActionTransition(QcImprovementActionStatus.COMPLETED, QcImprovementActionStatus.VERIFIED) is DomainResult.Success)
    }

    @Test
    fun `terminal verified, rejected or cancelled actions cannot transition`() {
        val verifiedResult = QcGovernanceValidator.validateActionTransition(QcImprovementActionStatus.VERIFIED, QcImprovementActionStatus.IN_PROGRESS)
        assertTrue(verifiedResult is DomainResult.Error)
        assertTrue((verifiedResult as DomainResult.Error).message.contains("terminal status", ignoreCase = true))

        val rejectedResult = QcGovernanceValidator.validateActionTransition(QcImprovementActionStatus.REJECTED, QcImprovementActionStatus.APPROVED)
        assertTrue(rejectedResult is DomainResult.Error)
    }
}
