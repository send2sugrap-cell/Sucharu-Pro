package com.sucharu.sucharupro.domain.model.vendorportal

/**
 * Lifecycle states of a vendor's primary portal account (Module 13 Step 01).
 */
enum class VendorPortalAccountStatus {
    INVITED,
    PENDING_ACTIVATION,
    ACTIVE,
    SUSPENDED,
    DISABLED,
    REVOKED
}

/**
 * Lifecycle states of an individual user's membership to a vendor portal.
 */
enum class VendorPortalMembershipStatus {
    INVITED,
    PENDING_ACTIVATION,
    ACTIVE,
    SUSPENDED,
    DISABLED,
    REVOKED
}

/**
 * Functional roles assigned to vendor portal users.
 */
enum class VendorPortalRole(val displayName: String) {
    VENDOR_ADMIN("Vendor Administrator"),
    VENDOR_OPERATOR("Vendor Operations Lead"),
    VENDOR_FINANCE("Vendor Billing & Finance"),
    VENDOR_QC("Vendor Quality Control"),
    VENDOR_LOGISTICS("Vendor Shipping & Logistics"),
    VENDOR_VIEWER("Vendor Read-Only Observer")
}

/**
 * Status of a vendor portal session.
 */
enum class VendorPortalSessionStatus {
    ACTIVE,
    EXPIRED,
    REVOKED,
    TERMINATED
}

/**
 * Security audit event classifications for vendor portal actions.
 */
enum class VendorPortalAuditEventType {
    ACCOUNT_CREATED,
    ACCOUNT_ACTIVATED,
    ACCOUNT_SUSPENDED,
    ACCOUNT_REVOKED,
    MEMBERSHIP_INVITED,
    MEMBERSHIP_ACTIVATED,
    MEMBERSHIP_SUSPENDED,
    MEMBERSHIP_REVOKED,
    LOGIN_SUCCESS,
    LOGIN_FAILURE,
    ACCESS_DENIED,
    SESSION_TERMINATED,
    POLICY_UPDATED
}
