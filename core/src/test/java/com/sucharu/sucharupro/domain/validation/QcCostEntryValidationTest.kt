package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.QcCostEntry
import com.sucharu.sucharupro.domain.model.qc.QcCostStatus
import com.sucharu.sucharupro.domain.model.qc.QcCostType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QcCostEntryValidationTest {

    @Test
    fun `validateCreation succeeds with valid parameters`() {
        val result = QcCostEntryValidator.validateCreation(
            projectId = "PRJ-01",
            productionJobId = "JOB-01",
            costType = QcCostType.INSPECTION,
            description = "Visual inspection consumables",
            quantity = 2.0,
            unitCost = 150.0,
            currency = "BDT",
            recordedBy = "insp-01",
            recordedAt = "2026-08-17T10:00:00Z"
        )
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun `validateCreation fails when project ID is blank`() {
        val result = QcCostEntryValidator.validateCreation(
            projectId = "",
            productionJobId = "JOB-01",
            costType = QcCostType.INSPECTION,
            description = "Visual inspection",
            quantity = 1.0,
            unitCost = 100.0,
            currency = "BDT",
            recordedBy = "insp-01",
            recordedAt = "2026-08-17T10:00:00Z"
        )
        assertTrue(result is DomainResult.Error)
        assertEquals("Project ID cannot be blank.", (result as DomainResult.Error).message)
    }

    @Test
    fun `validateCreation fails when quantity is zero or negative`() {
        val resZero = QcCostEntryValidator.validateCreation(
            projectId = "PRJ-01",
            productionJobId = "JOB-01",
            costType = QcCostType.INSPECTION,
            description = "Visual inspection",
            quantity = 0.0,
            unitCost = 100.0,
            currency = "BDT",
            recordedBy = "insp-01",
            recordedAt = "2026-08-17T10:00:00Z"
        )
        assertTrue(resZero is DomainResult.Error)

        val resNeg = QcCostEntryValidator.validateCreation(
            projectId = "PRJ-01",
            productionJobId = "JOB-01",
            costType = QcCostType.INSPECTION,
            description = "Visual inspection",
            quantity = -1.5,
            unitCost = 100.0,
            currency = "BDT",
            recordedBy = "insp-01",
            recordedAt = "2026-08-17T10:00:00Z"
        )
        assertTrue(resNeg is DomainResult.Error)
    }

    @Test
    fun `validateCreation fails when unit cost is negative`() {
        val res = QcCostEntryValidator.validateCreation(
            projectId = "PRJ-01",
            productionJobId = "JOB-01",
            costType = QcCostType.INSPECTION,
            description = "Visual inspection",
            quantity = 1.0,
            unitCost = -50.0,
            currency = "BDT",
            recordedBy = "insp-01",
            recordedAt = "2026-08-17T10:00:00Z"
        )
        assertTrue(res is DomainResult.Error)
    }

    @Test
    fun `validateStatusTransition follows valid state machine`() {
        assertTrue(QcCostEntryValidator.validateStatusTransition(QcCostStatus.DRAFT, QcCostStatus.RECORDED) is DomainResult.Success)
        assertTrue(QcCostEntryValidator.validateStatusTransition(QcCostStatus.RECORDED, QcCostStatus.RECONCILED) is DomainResult.Success)
        assertTrue(QcCostEntryValidator.validateStatusTransition(QcCostStatus.RECONCILED, QcCostStatus.LOCKED) is DomainResult.Success)
        assertTrue(QcCostEntryValidator.validateStatusTransition(QcCostStatus.LOCKED, QcCostStatus.RECORDED) is DomainResult.Error)
        assertTrue(QcCostEntryValidator.validateStatusTransition(QcCostStatus.CANCELLED, QcCostStatus.RECORDED) is DomainResult.Error)
    }

    @Test
    fun `validateImmutability rejects modification on locked cost entry`() {
        val entry = QcCostEntry(
            id = "qcc-1",
            productionJobId = "JOB-01",
            projectId = "PRJ-01",
            costType = QcCostType.INSPECTION,
            description = "Consumables",
            quantity = 1.0,
            unitCost = 100.0,
            status = QcCostStatus.LOCKED,
            recordedBy = "user-1",
            recordedAt = "2026-08-17T10:00:00Z",
            createdAt = "2026-08-17T10:00:00Z",
            updatedAt = "2026-08-17T10:00:00Z"
        )
        val result = QcCostEntryValidator.validateImmutability(entry)
        assertTrue(result is DomainResult.Error)
    }
}
