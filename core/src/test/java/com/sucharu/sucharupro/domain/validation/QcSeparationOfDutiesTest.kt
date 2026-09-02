package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import org.junit.Assert.assertTrue
import org.junit.Test

class QcSeparationOfDutiesTest {

    @Test
    fun `proposer cannot self-approve improvement action`() {
        val result = QcGovernanceValidator.validateSeparationOfDuties(
            proposedBy = "user-01",
            approverId = "user-01"
        )
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("cannot self-approve", ignoreCase = true))
    }

    @Test
    fun `action owner cannot self-verify improvement action`() {
        val result = QcGovernanceValidator.validateSeparationOfDuties(
            proposedBy = "user-01",
            approverId = "mgr-01",
            actionOwnerId = "tech-01",
            verifierId = "tech-01"
        )
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("cannot self-verify", ignoreCase = true))
    }

    @Test
    fun `distinct proposer, approver, owner, and verifier pass validation`() {
        val result = QcGovernanceValidator.validateSeparationOfDuties(
            proposedBy = "insp-01",
            approverId = "mgr-01",
            actionOwnerId = "tech-01",
            verifierId = "admin-01"
        )
        assertTrue(result is DomainResult.Success)
    }
}
