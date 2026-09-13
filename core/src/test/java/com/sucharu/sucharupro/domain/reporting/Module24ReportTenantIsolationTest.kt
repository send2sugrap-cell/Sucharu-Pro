package com.sucharu.sucharupro.domain.reporting

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.report.ReportCategory
import com.sucharu.sucharupro.domain.model.report.ReportRequest
import com.sucharu.sucharupro.domain.validation.Module24ReportAuthorizationValidator
import org.junit.Assert.*
import org.junit.Test

class Module24ReportTenantIsolationTest {

    private val tenantId = "TENANT-ORIGINAL"

    private val principal = AuthenticatedPrincipal(
        userId = "manager-1",
        projectId = tenantId,
        username = "manager",
        role = UserRole.MANAGER
    )

    @Test
    fun testTenantMatch_passesValidation() {
        val req = ReportRequest(
            reportCategory = ReportCategory.SALES,
            reportType = "SALES_SUMMARY",
            tenantId = tenantId,
            projectId = tenantId
        )

        val result = Module24ReportAuthorizationValidator.validateRequest(principal, req)
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun testCrossTenantRequest_rejected() {
        val req = ReportRequest(
            reportCategory = ReportCategory.SALES,
            reportType = "SALES_SUMMARY",
            tenantId = "TENANT-MALICIOUS-OVERRIDE",
            projectId = "TENANT-MALICIOUS-OVERRIDE"
        )

        val result = Module24ReportAuthorizationValidator.validateRequest(principal, req)
        assertTrue("Cross-tenant attempt must be rejected", result is DomainResult.Error)

        val err = result as DomainResult.Error
        assertTrue(err.message.contains("Tenant isolation violation"))
    }
}
