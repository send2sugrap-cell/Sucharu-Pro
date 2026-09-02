package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.finance.FinancialReportFilter
import com.sucharu.sucharupro.domain.model.finance.FinancialReportType
import com.sucharu.sucharupro.domain.model.user.UserRole

/**
 * RBAC authorization validator for Financial Reporting & Analytics (Module 09 Step 09).
 *
 * Rules:
 * - ADMIN: Full access to all financial reports, exports, and audit snapshots.
 * - MANAGER: Access to management reports and operational summaries.
 * - ACCOUNTS: Full access to all financial reports, statements, and ledgers.
 * - STAFF: Restricted operational financial reports only (DASHBOARD, ACCOUNTS_RECEIVABLE, ACCOUNTS_PAYABLE, CUSTOMER_PAYMENT, SUPPLIER_PAYMENT).
 * - CUSTOMER: Own payments and receivables only (must match customerId). Blocked from P&L, Balance Sheet, Ledger, etc.
 * - VENDOR: Own payables and payments only (must match vendorId). Blocked from P&L, Balance Sheet, Ledger, etc.
 */
object FinancialReportAuthorizationValidator {

    fun validateAccess(
        reportType: FinancialReportType,
        filter: FinancialReportFilter,
        callerRole: UserRole,
        actorId: String? = null
    ): DomainResult<Unit> {
        when (callerRole) {
            UserRole.ADMIN,
            UserRole.MANAGER,
            UserRole.ACCOUNTS -> {
                return DomainResult.Success(Unit)
            }

            UserRole.STAFF -> {
                if (reportType in listOf(
                        FinancialReportType.DASHBOARD,
                        FinancialReportType.ACCOUNTS_RECEIVABLE,
                        FinancialReportType.ACCOUNTS_PAYABLE,
                        FinancialReportType.CUSTOMER_PAYMENT,
                        FinancialReportType.SUPPLIER_PAYMENT,
                        FinancialReportType.EXPENSE_ANALYSIS
                    )
                ) {
                    return DomainResult.Success(Unit)
                }
                return DomainResult.Error(
                    message = "Unauthorized: STAFF role does not have access to full financial report '${reportType.defaultLabel}'."
                )
            }

            UserRole.CUSTOMER -> {
                if (reportType != FinancialReportType.CUSTOMER_PAYMENT &&
                    reportType != FinancialReportType.ACCOUNTS_RECEIVABLE
                ) {
                    return DomainResult.Error(
                        message = "Unauthorized: CUSTOMER cannot access financial statement or management report '${reportType.defaultLabel}'."
                    )
                }
                if (filter.customerId.isNullOrBlank() || (actorId != null && filter.customerId != actorId)) {
                    return DomainResult.Error(
                        message = "Unauthorized: CUSTOMER can only query their own scoped financial data."
                    )
                }
                return DomainResult.Success(Unit)
            }

            UserRole.VENDOR -> {
                if (reportType != FinancialReportType.SUPPLIER_PAYMENT &&
                    reportType != FinancialReportType.ACCOUNTS_PAYABLE
                ) {
                    return DomainResult.Error(
                        message = "Unauthorized: VENDOR cannot access financial statement or management report '${reportType.defaultLabel}'."
                    )
                }
                if (filter.vendorId.isNullOrBlank() || (actorId != null && filter.vendorId != actorId)) {
                    return DomainResult.Error(
                        message = "Unauthorized: VENDOR can only query their own scoped financial data."
                    )
                }
                return DomainResult.Success(Unit)
            }

            else -> {
                return DomainResult.Error(
                    message = "Unauthorized: Role '$callerRole' does not have permission to view financial reports."
                )
            }
        }
    }

    fun validateSnapshotGeneration(callerRole: UserRole): DomainResult<Unit> {
        val allowed = callerRole == UserRole.ADMIN ||
                callerRole == UserRole.MANAGER ||
                callerRole == UserRole.ACCOUNTS
        return if (allowed) {
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(
                message = "Unauthorized: Role '$callerRole' cannot generate immutable financial report audit snapshots."
            )
        }
    }

    fun validateExport(callerRole: UserRole): DomainResult<Unit> {
        val allowed = callerRole == UserRole.ADMIN ||
                callerRole == UserRole.MANAGER ||
                callerRole == UserRole.ACCOUNTS
        return if (allowed) {
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(
                message = "Unauthorized: Role '$callerRole' cannot request financial report exports."
            )
        }
    }
}
