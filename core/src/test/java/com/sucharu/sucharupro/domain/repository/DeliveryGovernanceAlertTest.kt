package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.delivery.governance.DeliveryGovernanceAlert
import com.sucharu.sucharupro.domain.model.delivery.governance.DeliveryGovernanceAlertCategory
import com.sucharu.sucharupro.domain.model.delivery.governance.DeliveryGovernanceAlertSeverity
import com.sucharu.sucharupro.domain.model.delivery.governance.DeliveryGovernanceAlertStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class DeliveryGovernanceAlertTest {

    @Test
    fun `alert initializes with open status and valid fields`() {
        val alert = DeliveryGovernanceAlert(
            alertId = "ALT-1",
            projectId = "PRJ-01",
            category = DeliveryGovernanceAlertCategory.MISSING_POD,
            severity = DeliveryGovernanceAlertSeverity.WARNING,
            referenceType = "SHIPMENT",
            referenceId = "SH-1",
            title = "Missing POD",
            description = "POD is missing for delivered shipment",
            detectedAt = 1000L,
            createdAt = 1000L,
            updatedAt = 1000L
        )

        assertEquals(DeliveryGovernanceAlertStatus.OPEN, alert.status)
        assertEquals(DeliveryGovernanceAlertCategory.MISSING_POD, alert.category)
        assertEquals(DeliveryGovernanceAlertSeverity.WARNING, alert.severity)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `acknowledged alert without actor throws exception`() {
        DeliveryGovernanceAlert(
            alertId = "ALT-1",
            projectId = "PRJ-01",
            category = DeliveryGovernanceAlertCategory.MISSING_POD,
            severity = DeliveryGovernanceAlertSeverity.WARNING,
            referenceType = "SHIPMENT",
            referenceId = "SH-1",
            title = "Missing POD",
            description = "POD is missing",
            status = DeliveryGovernanceAlertStatus.ACKNOWLEDGED,
            acknowledgedBy = null,
            acknowledgedAt = null,
            detectedAt = 1000L,
            createdAt = 1000L,
            updatedAt = 1000L
        )
    }
}
