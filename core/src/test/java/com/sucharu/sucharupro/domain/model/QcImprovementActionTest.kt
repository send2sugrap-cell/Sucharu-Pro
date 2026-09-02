package com.sucharu.sucharupro.domain.model

import com.sucharu.sucharupro.domain.model.qc.governance.QcImprovementAction
import com.sucharu.sucharupro.domain.model.qc.governance.QcImprovementActionStatus
import com.sucharu.sucharupro.domain.model.qc.governance.QcImprovementActionType
import com.sucharu.sucharupro.domain.model.qc.governance.QcImprovementPriority
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QcImprovementActionTest {

    @Test
    fun `improvement action instantiates with proposed status and terminal verification flag`() {
        val action = QcImprovementAction(
            actionId = "ACT-01",
            projectId = "PRJ-01",
            proposedBy = "insp-01",
            actionType = QcImprovementActionType.CORRECTIVE_ACTION,
            priority = QcImprovementPriority.HIGH,
            title = "Replace Cyan Printhead Dampers",
            description = "Recurring streak issues due to worn dampers",
            createdAt = "2026-08-17T08:00:00Z",
            updatedAt = "2026-08-17T08:00:00Z"
        )
        assertEquals(QcImprovementActionStatus.PROPOSED, action.status)
        assertFalse(action.isTerminal)

        val verified = action.copy(status = QcImprovementActionStatus.VERIFIED)
        assertTrue(verified.isTerminal)
    }
}
