package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.auth.authorization.AuthorizationCapability
import com.sucharu.sucharupro.data.auth.authorization.RoleCapabilityMatrix
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.report.ExportReportRequest
import com.sucharu.sucharupro.domain.model.report.ReportCategory
import com.sucharu.sucharupro.domain.model.report.ReportRequest
import com.sucharu.sucharupro.domain.service.report.Module24ReportCatalogueRegistry

/**
 * Capability and Tenant Authorization Validator for Module 24 Reporting Subsystem.
 */
object Module24ReportAuthorizationValidator {

    fun validateRequest(
        principal: AuthenticatedPrincipal,
        request: ReportRequest
    ): DomainResult<Unit> {
        // 1. Tenant Isolation Verification
        if (request.tenantId.isNotBlank() && request.tenantId != principal.projectId) {
            return DomainResult.Error(
                message = "Tenant isolation violation: Tenant ID in request ('${request.tenantId}') does not match principal tenant context ('${principal.projectId}')."
            )
        }

        // 2. Report Definition & Capability Resolution
        val definition = Module24ReportCatalogueRegistry.findDefinition(request.reportType)
        val requiredCapability = definition?.requiredCapability ?: resolveCategoryCapability(request.reportCategory)

        if (!RoleCapabilityMatrix.hasCapability(principal.role, requiredCapability)) {
            return DomainResult.Error(
                message = "Unauthorized: Role '${principal.role}' does not have capability '${requiredCapability.name}' required for report '${request.reportType}'."
            )
        }

        // 3. Identity Scoping Constraints for External Roles
        when (principal.role) {
            UserRole.CUSTOMER -> {
                val filterCust = request.filters["customerId"]
                if (filterCust != null && filterCust.isNotBlank() && filterCust != principal.effectiveCustomerId) {
                    return DomainResult.Error(
                        message = "Unauthorized: Customer accounts can only access financial and order reports scoped to their own customer identity ('${principal.effectiveCustomerId}')."
                    )
                }
            }

            UserRole.AFFILIATE -> {
                val filterAff = request.filters["affiliateId"]
                if (filterAff != null && filterAff.isNotBlank() && filterAff != principal.effectiveAffiliateId) {
                    return DomainResult.Error(
                        message = "Unauthorized: Affiliate accounts can only access commission and wallet reports scoped to their own affiliate identity ('${principal.effectiveAffiliateId}')."
                    )
                }
            }

            UserRole.VENDOR -> {
                val filterVendor = request.filters["vendorId"]
                if (filterVendor != null && filterVendor.isNotBlank() && filterVendor != principal.effectiveVendorId) {
                    return DomainResult.Error(
                        message = "Unauthorized: Vendor accounts can only access reports scoped to their own vendor identity ('${principal.effectiveVendorId}')."
                    )
                }
            }

            else -> { /* Internal roles pass identity scope */ }
        }

        return DomainResult.Success(Unit)
    }

    fun validateExport(
        principal: AuthenticatedPrincipal,
        exportRequest: ExportReportRequest
    ): DomainResult<Unit> {
        val baseVal = validateRequest(principal, exportRequest.queryRequest)
        if (baseVal is DomainResult.Error) return baseVal

        if (!RoleCapabilityMatrix.hasCapability(principal.role, AuthorizationCapability.REPORT_EXPORT)) {
            return DomainResult.Error(
                message = "Unauthorized: Role '${principal.role}' does not have capability 'REPORT_EXPORT' required to export reports."
            )
        }

        return DomainResult.Success(Unit)
    }

    private fun resolveCategoryCapability(category: ReportCategory): AuthorizationCapability {
        return when (category) {
            ReportCategory.SALES -> AuthorizationCapability.REPORT_VIEW_SALES
            ReportCategory.CUSTOMER -> AuthorizationCapability.REPORT_VIEW_CUSTOMER
            ReportCategory.ORDER -> AuthorizationCapability.REPORT_VIEW_ORDER
            ReportCategory.PRODUCTION -> AuthorizationCapability.REPORT_VIEW_PRODUCTION
            ReportCategory.QUALITY -> AuthorizationCapability.REPORT_VIEW_QUALITY
            ReportCategory.INVENTORY -> AuthorizationCapability.REPORT_VIEW_INVENTORY
            ReportCategory.DELIVERY -> AuthorizationCapability.REPORT_VIEW_DELIVERY
            ReportCategory.FINANCE -> AuthorizationCapability.REPORT_VIEW_FINANCE
            ReportCategory.PROFITABILITY -> AuthorizationCapability.REPORT_VIEW_PROFITABILITY
            ReportCategory.AFFILIATE -> AuthorizationCapability.REPORT_VIEW_AFFILIATE
            ReportCategory.WALLET_PAYOUT -> AuthorizationCapability.REPORT_VIEW_WALLET_PAYOUT
            ReportCategory.MACHINE_OPERATIONS -> AuthorizationCapability.REPORT_VIEW_MACHINE_OPERATIONS
            ReportCategory.PREFLIGHT -> AuthorizationCapability.REPORT_VIEW_PREFLIGHT
            ReportCategory.AUDIT -> AuthorizationCapability.REPORT_VIEW_AUDIT
            ReportCategory.EXECUTIVE_ANALYTICS -> AuthorizationCapability.REPORT_VIEW_EXECUTIVE_ANALYTICS
        }
    }
}
