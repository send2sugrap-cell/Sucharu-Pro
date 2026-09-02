package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.QcTimeEntry
import com.sucharu.sucharupro.domain.model.qc.QcTimeEntryType
import com.sucharu.sucharupro.domain.model.qc.QcTimeStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QcTimeEntryValidationTest {

    @Test
    fun `validateCreation succeeds with valid parameters`() {
        val result = QcTimeEntryValidator.validateCreation(
            projectId = "PRJ-01",
            productionJobId = "JOB-01",
            entryType = QcTimeEntryType.INSPECTION,
            actorId = "insp-01",
            startedAt = "2026-08-17T09:00:00Z",
            endedAt = "2026-08-17T09:45:00Z",
            durationMinutes = 45L
        )
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun `validateCreation fails when duration is negative`() {
        val result = QcTimeEntryValidator.validateCreation(
            projectId = "PRJ-01",
            productionJobId = "JOB-01",
            entryType = QcTimeEntryType.INSPECTION,
            actorId = "insp-01",
            startedAt = "2026-08-17T09:00:00Z",
            endedAt = "2026-08-17T09:45:00Z",
            durationMinutes = -15L
        )
        assertTrue(result is DomainResult.Error)
    }

    @Test
    fun `validateCreation fails when endedAt is earlier than startedAt`() {
        val result = QcTimeEntryValidator.validateCreation(
            projectId = "PRJ-01",
            productionJobId = "JOB-01",
            entryType = QcTimeEntryType.INSPECTION,
            actorId = "insp-01",
            startedAt = "2026-08-17T10:00:00Z",
            endedAt = "2026-08-17T09:00:00Z",
            durationMinutes = 60L
        )
        assertTrue(result is DomainResult.Error)
    }

    @Test
    fun `validateStatusTransition follows valid state machine`() {
        assertTrue(QcTimeEntryValidator.validateStatusTransition(QcTimeStatus.OPEN, QcTimeStatus.RECORDED) is DomainResult.Success)
        assertTrue(QcTimeEntryValidator.validateStatusTransition(QcTimeStatus.RECORDED, QcTimeStatus.RECONCILED) is DomainResult.Success)
        assertTrue(QcTimeEntryValidator.validateStatusTransition(QcTimeStatus.RECONCILED, QcTimeStatus.LOCKED) is DomainResult.Success)
        assertTrue(QcTimeEntryValidator.validateStatusTransition(QcTimeStatus.LOCKED, QcTimeStatus.OPEN) is DomainResult.Error)
    }

    @Test
    fun `validateImmutability rejects modification on locked time entry`() {
        val entry = QcTimeEntry(
            id = "qct-1",
            productionJobId = "JOB-01",
            projectId = "PRJ-01",
            entryType = QcTimeEntryType.INSPECTION,
            actorId = "user-1",
            startedAt = "2026-08-17T09:00:00Z",
            endedAt = "2026-08-17T09:30:00Z",
            durationMinutes = 30L,
            status = QcTimeStatus.LOCKED,
            createdAt = "2026-08-17T09:00:00Z",
            updatedAt = "2026-08-17T09:30:00Z"
        )
        val result = QcTimeEntryValidator.validateImmutability(entry)
        assertTrue(result is DomainResult.Error)
    }
}
