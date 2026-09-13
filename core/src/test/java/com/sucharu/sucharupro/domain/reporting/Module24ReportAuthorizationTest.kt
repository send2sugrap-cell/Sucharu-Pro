package com.sucharu.sucharupro.domain.reporting

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.report.*
import com.sucharu.sucharupro.domain.validation.Module24ReportAuthorizationValidator
import org.junit.Assert.*
import org.junit.Test

class Module24ReportAuthorizationTest {

    private val tenantId = "TENANT-ALPHA"

    private val admin = AuthenticatedPrincipal(
        userId = "admin-1",
        projectId = tenantId,
        username = "admin",
        role = UserRole.ADMIN
    )

    private val manager = AuthenticatedPrincipal(
        userId = "manager-1",
        projectId = tenantId,
        username = "manager",
        role = UserRole.MANAGER
    )

    private val staff = AuthenticatedPrincipal(
        userId = "staff-1",
        projectId = tenantId,
        username = "staff",
        role = UserRole.STAFF
    )

    private val customer = AuthenticatedPrincipal(
        userId = "cust-100",
        projectId = tenantId,
        username = "customer",
        role = UserRole.CUSTOMER,
        customerId = "CUST-100"
    )

    private val affiliate = AuthenticatedPrincipal(
        userId = "aff-200",
        projectId = tenantId,
        username = "affiliate",
        role = UserRole.AFFILIATE,
        affiliateId = "AFF-200"
    )

    @Test
    fun testAdmin_hasAccessToAllReportCategories() {
        ReportCategory.entries.forEach { category ->
            val req = ReportRequest(
                reportCategory = category,
                reportType = "${category.name}_REPORT",
                tenantId = tenantId,
                projectId = tenantId
            )
            val result = Module24ReportAuthorizationValidator.validateRequest(admin, req)
            assertTrue("Admin should have access to category $category", result is DomainResult.Success)
        }
    }

    @Test
    fun testStaff_allowedForOperational_deniedForFinancialAndExecutive() {
        val prodReq = ReportRequest(ReportCategory.PRODUCTION, "PRODUCTION_SUMMARY", tenantId, tenantId)
        val prodVal = Module24ReportAuthorizationValidator.validateRequest(staff, prodReq)
        assertTrue("Staff allowed for production report", prodVal is DomainResult.Success)

        val finReq = ReportRequest(ReportCategory.FINANCE, "FINANCE_EXECUTIVE_SUMMARY", tenantId, tenantId)
        val finVal = Module24ReportAuthorizationValidator.validateRequest(staff, finReq)
        assertTrue("Staff denied for finance report", finVal is DomainResult.Error)

        val auditReq = ReportRequest(ReportCategory.AUDIT, "SYSTEM_AUDIT_LOGS", tenantId, tenantId)
        val auditVal = Module24ReportAuthorizationValidator.validateRequest(staff, auditReq)
        assertTrue("Staff denied for audit report", auditVal is DomainResult.Error)
    }

    @Test
    fun testCustomer_identityScopeEnforcement() {
        val ownReq = ReportRequest(
            reportCategory = ReportCategory.CUSTOMER,
            reportType = "CUSTOMER_RECEIVABLE_AGING",
            tenantId = tenantId,
            projectId = tenantId,
            filters = mapOf("customerId" to "CUST-100")
        )
        val ownVal = Module24ReportAuthorizationValidator.validateRequest(customer, ownReq)
        assertTrue("Customer allowed for own customer ID", ownVal is DomainResult.Success)

        val otherReq = ReportRequest(
            reportCategory = ReportCategory.CUSTOMER,
            reportType = "CUSTOMER_RECEIVABLE_AGING",
            tenantId = tenantId,
            projectId = tenantId,
            filters = mapOf("customerId" to "CUST-999")
        )
        val otherVal = Module24ReportAuthorizationValidator.validateRequest(customer, otherReq)
        assertTrue("Customer denied for another customer ID", otherVal is DomainResult.Error)
        val err = otherVal as DomainResult.Error
        assertTrue(err.message.contains("Customer accounts can only access"))
    }

    @Test
    fun testAffiliate_identityScopeEnforcement() {
        val ownReq = ReportRequest(
            reportCategory = ReportCategory.AFFILIATE,
            reportType = "AFFILIATE_COMMISSIONS",
            tenantId = tenantId,
            projectId = tenantId,
            filters = mapOf("affiliateId" to "AFF-200")
        )
        val ownVal = Module24ReportAuthorizationValidator.validateRequest(affiliate, ownReq)
        assertTrue("Affiliate allowed for own affiliate ID", ownVal is DomainResult.Success)

        val otherReq = ReportRequest(
            reportCategory = ReportCategory.AFFILIATE,
            reportType = "AFFILIATE_COMMISSIONS",
            tenantId = tenantId,
            projectId = tenantId,
            filters = mapOf("affiliateId" to "AFF-999")
        )
        val otherVal = Module24ReportAuthorizationValidator.validateRequest(affiliate, otherReq)
        assertTrue("Affiliate denied for another affiliate ID", otherVal is DomainResult.Error)
    }
}
