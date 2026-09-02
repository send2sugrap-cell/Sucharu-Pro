package com.sucharu.sucharupro.customerfinancialalerts

import com.sucharu.sucharupro.domain.model.customerfinancialreporting.*
import com.sucharu.sucharupro.domain.validation.customerfinancialreporting.CustomerFinancialAlertValidator
import org.junit.Assert.*
import org.junit.Test

class CustomerFinancialAlertDomainTest {

    @Test
    fun testAlertModelPropertiesAndDefaults() {
        val alert = CustomerFinancialAlert(
            alertId = "ALT-001",
            tenantId = "TENANT-1",
            projectId = "PRJ-1",
            customerId = "CUS-1",
            alertType = CustomerFinancialAlertType.INVOICE_OVERDUE,
            severity = CustomerFinancialAlertSeverity.HIGH,
            title = "Overdue Invoice",
            safeMessage = "Invoice #101 is overdue by 5 days",
            sourceType = "INVOICE",
            sourceId = "INV-101",
            deduplicationKey = "TENANT-1:PRJ-1:CUS-1:INVOICE_OVERDUE:INVOICE:INV-101"
        )

        assertEquals("ALT-001", alert.alertId)
        assertTrue(alert.isOpen)
        assertFalse(alert.isAcknowledged)
        assertFalse(alert.isResolved)
        assertFalse(alert.isDismissed)
        assertFalse(alert.isExpired)
    }

    @Test
    fun testStatusTransitionsAndValidation() {
        // Valid transitions
        assertTrue(CustomerFinancialAlertStatus.OPEN.canTransitionTo(CustomerFinancialAlertStatus.ACKNOWLEDGED))
        assertTrue(CustomerFinancialAlertStatus.OPEN.canTransitionTo(CustomerFinancialAlertStatus.RESOLVED))
        assertTrue(CustomerFinancialAlertStatus.OPEN.canTransitionTo(CustomerFinancialAlertStatus.DISMISSED))
        assertTrue(CustomerFinancialAlertStatus.ACKNOWLEDGED.canTransitionTo(CustomerFinancialAlertStatus.RESOLVED))
        assertTrue(CustomerFinancialAlertStatus.ACKNOWLEDGED.canTransitionTo(CustomerFinancialAlertStatus.DISMISSED))

        // Invalid transitions from terminal state
        assertFalse(CustomerFinancialAlertStatus.RESOLVED.canTransitionTo(CustomerFinancialAlertStatus.OPEN))
        assertFalse(CustomerFinancialAlertStatus.DISMISSED.canTransitionTo(CustomerFinancialAlertStatus.ACKNOWLEDGED))

        val alert = CustomerFinancialAlert(
            alertId = "ALT-002",
            tenantId = "TENANT-1",
            projectId = "PRJ-1",
            customerId = "CUS-1",
            alertType = CustomerFinancialAlertType.CREDIT_LIMIT_EXCEEDED,
            severity = CustomerFinancialAlertSeverity.CRITICAL,
            title = "Limit Exceeded",
            safeMessage = "Exceeded limit",
            sourceType = "CREDIT",
            sourceId = "CUS-1",
            deduplicationKey = "dedup"
        )

        val errRes = CustomerFinancialAlertValidator.validateDismissal(alert, "")
        assertTrue(errRes is com.sucharu.sucharupro.domain.model.common.DomainResult.Error)

        val okRes = CustomerFinancialAlertValidator.validateDismissal(alert, "Customer agreed to pay")
        assertTrue(okRes is com.sucharu.sucharupro.domain.model.common.DomainResult.Success)
    }

    @Test
    fun testScheduleFrequencyAndTimezoneValidation() {
        val validSchedule = CustomerFinancialReportSchedule(
            scheduleId = "SCH-001",
            tenantId = "TENANT-1",
            projectId = "PRJ-1",
            customerId = "CUS-1",
            reportType = CustomerFinancialReportType.CUSTOMER_STATEMENT,
            frequency = CustomerFinancialScheduleFrequency.MONTHLY,
            timezone = "Asia/Dhaka",
            nextRunAt = System.currentTimeMillis() + 86400000L,
            createdBy = "admin"
        )
        val validRes = CustomerFinancialAlertValidator.validateSchedule(validSchedule)
        assertTrue(validRes is com.sucharu.sucharupro.domain.model.common.DomainResult.Success)

        val invalidTzSchedule = validSchedule.copy(timezone = "Invalid/Timezone_XYZ")
        val invalidTzRes = CustomerFinancialAlertValidator.validateSchedule(invalidTzSchedule)
        assertTrue(invalidTzRes is com.sucharu.sucharupro.domain.model.common.DomainResult.Error)
    }
}
