package com.sucharu.sucharupro.domain.model.vendor

/**
 * State machine and classification enums for Vendor Settlement, Analytics & Integration (Module 12 Step 10).
 */
enum class VendorSettlementStatus {
    DRAFT,
    ELIGIBLE,
    APPROVED,
    PROCESSING,
    SETTLED,
    REJECTED,
    CANCELLED,
    FAILED,
    RECONCILIATION_REQUIRED
}

enum class SettlementMethod {
    BANK_TRANSFER,
    CHEQUE,
    CASH,
    MOBILE_MONEY,
    ELECTRONIC_FUNDS_TRANSFER,
    CREDIT_ADJUSTMENT,
    OTHER
}

enum class SettlementEligibility {
    ELIGIBLE,
    INELIGIBLE_UNAPPROVED_INVOICE,
    INELIGIBLE_UNMATCHED,
    INELIGIBLE_DISPUTE_BLOCKED,
    INELIGIBLE_ALREADY_SETTLED,
    INELIGIBLE_VENDOR_SUSPENDED,
    INELIGIBLE_ZERO_BALANCE,
    INELIGIBLE_NON_EXISTENT_PAYABLE
}

enum class ReconciliationStatus {
    MATCHED,
    DISCREPANCY_DETECTED,
    UNRECONCILED,
    PENDING_INVESTIGATION,
    RESOLVED
}

enum class AnalyticsPeriod {
    DAILY,
    WEEKLY,
    MONTHLY,
    QUARTERLY,
    YEARLY,
    CUSTOM
}

enum class VendorSettlementAuditEventType {
    SETTLEMENT_CREATED,
    ELIGIBILITY_EVALUATED,
    SETTLEMENT_APPROVED,
    SETTLEMENT_PROCESSED,
    ALLOCATION_CREATED,
    RECONCILIATION_PERFORMED,
    RECONCILIATION_MISMATCH_DETECTED,
    SETTLEMENT_FAILED,
    SETTLEMENT_CANCELLED,
    SNAPSHOT_GENERATED
}
