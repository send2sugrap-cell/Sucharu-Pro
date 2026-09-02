package com.sucharu.sucharupro.vendor

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.CorrectiveActionPriority
import com.sucharu.sucharupro.domain.model.vendor.VendorCorrectiveAction
import com.sucharu.sucharupro.domain.validation.vendor.VendorPerformanceValidator
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class VendorCorrectiveActionDomainTest {

    @Test
    fun testValidCorrectiveAction() {
        val action = VendorCorrectiveAction(
            actionId = "CAPA-001",
            projectId = "PRJ-01",
            tenantId = "PRJ-01",
            vendorId = "VND-01",
            sourceType = "KPI",
            sourceId = "KPI-OTD",
            issueDescription = "Delivery delay exceeding 15 days on PO-998",
            rootCause = "Raw material shortage at factory",
            actionPlan = "Procured alternative supply lines",
            assignedTo = "manager_01",
            assignedToName = "Manager One",
            priority = CorrectiveActionPriority.HIGH,
            dueDate = Instant.now().plusSeconds(86400 * 14),
            createdBy = "qc_lead"
        )
        val res = VendorPerformanceValidator.validateCorrectiveAction(action)
        assertTrue(res is DomainResult.Success)
    }

    @Test
    fun testInvalidCorrectiveAction() {
        val action = VendorCorrectiveAction(
            actionId = "CAPA-001",
            projectId = "PRJ-01",
            tenantId = "PRJ-01",
            vendorId = "",
            sourceType = "KPI",
            issueDescription = "",
            actionPlan = "",
            assignedTo = "",
            assignedToName = "",
            dueDate = Instant.now().minusSeconds(86400),
            createdBy = ""
        )
        val res = VendorPerformanceValidator.validateCorrectiveAction(action)
        assertTrue(res is DomainResult.Error)
    }
}
