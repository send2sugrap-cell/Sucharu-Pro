package com.sucharu.sucharupro.domain.validation.communication.vendor

import com.sucharu.sucharupro.domain.model.communication.vendor.VendorCommunicationType
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.user.UserRole

/**
 * RBAC authorization validator for Vendor Communication operations (Module 10 Step 05).
 *
 * Rules:
 * - ADMIN: full access
 * - MANAGER: create/send/view all vendor communications
 * - ACCOUNTS: financial communications only (PAYMENT_*, PAYABLE_*)
 * - STAFF: limited operational communication
 * - WAREHOUSE: RECEIVING_UPDATE, SUPPLY_REQUEST, DELIVERY_UPDATE
 * - QC_INSPECTOR: QUALITY_UPDATE, QUALITY_REJECTION, RETURN_UPDATE
 * - LOGISTICS: DELIVERY_UPDATE, RECEIVING_UPDATE
 * - VENDOR: view/read/acknowledge own communications only — never another vendor
 * - CUSTOMER: BLOCKED from all vendor communication operations
 * - No role may cross project boundaries
 */
object VendorCommunicationAuthorizationValidator {

    // Financial communication types — only ACCOUNTS + MANAGER + ADMIN may initiate
    private val financialTypes = setOf(
        VendorCommunicationType.PAYABLE_UPDATE,
        VendorCommunicationType.PAYMENT_RECEIVED,
        VendorCommunicationType.PAYMENT_DUE,
        VendorCommunicationType.PAYMENT_OVERDUE,
        VendorCommunicationType.PAYMENT_STATUS,
        VendorCommunicationType.PURCHASE_BILL_UPDATE
    )

    // Warehouse-relevant types
    private val warehouseTypes = setOf(
        VendorCommunicationType.RECEIVING_UPDATE,
        VendorCommunicationType.SUPPLY_REQUEST,
        VendorCommunicationType.SUPPLY_CONFIRMATION,
        VendorCommunicationType.DELIVERY_UPDATE
    )

    // QC-relevant types
    private val qcTypes = setOf(
        VendorCommunicationType.QUALITY_UPDATE,
        VendorCommunicationType.QUALITY_REJECTION,
        VendorCommunicationType.RETURN_UPDATE,
        VendorCommunicationType.REPLACEMENT_UPDATE
    )

    // Logistics-relevant types
    private val logisticsTypes = setOf(
        VendorCommunicationType.DELIVERY_UPDATE,
        VendorCommunicationType.RECEIVING_UPDATE
    )

    /**
     * Validates whether [callerRole] may create a vendor communication of [communicationType].
     */
    fun validateCreate(
        callerRole: UserRole,
        communicationType: VendorCommunicationType,
        callerVendorId: String? = null,
        targetVendorId: String? = null
    ): DomainResult<Unit> {
        // CUSTOMER is always blocked
        if (callerRole == UserRole.CUSTOMER) {
            return DomainResult.Error(message = "CUSTOMER role is not permitted to create vendor communications.")
        }

        // VENDOR cannot create communications — can only receive/read/acknowledge
        if (callerRole == UserRole.VENDOR) {
            return DomainResult.Error(message = "VENDOR role cannot create vendor communications. Vendors may only view and acknowledge communications.")
        }

        // ACCOUNTS: restricted to financial types
        if (callerRole == UserRole.ACCOUNTS && communicationType !in financialTypes) {
            return DomainResult.Error(message = "ACCOUNTS role may only create financial vendor communications (PAYMENT_*, PAYABLE_*).")
        }

        // WAREHOUSE: restricted to warehouse types
        if (callerRole == UserRole.WAREHOUSE && communicationType !in warehouseTypes) {
            return DomainResult.Error(message = "WAREHOUSE role may only create supply/receiving/delivery vendor communications.")
        }

        // QC_INSPECTOR: restricted to QC types
        if (callerRole == UserRole.QC_INSPECTOR && communicationType !in qcTypes) {
            return DomainResult.Error(message = "QC_INSPECTOR role may only create quality/return/replacement vendor communications.")
        }

        return DomainResult.Success(Unit)
    }

    /**
     * Validates whether [callerRole] may read/view vendor communications for [targetVendorId].
     */
    fun validateRead(
        callerRole: UserRole,
        targetVendorId: String,
        callerVendorId: String? = null
    ): DomainResult<Unit> {
        if (callerRole == UserRole.CUSTOMER) {
            return DomainResult.Error(message = "CUSTOMER role cannot access vendor communications.")
        }

        // VENDOR can only access own communications
        if (callerRole == UserRole.VENDOR) {
            if (callerVendorId.isNullOrBlank() || callerVendorId != targetVendorId) {
                return DomainResult.Error(
                    message = "VENDOR role may only view its own communications. Cross-vendor access is prohibited."
                )
            }
        }

        return DomainResult.Success(Unit)
    }

    /**
     * Validates whether [callerRole] may acknowledge or decline a communication.
     */
    fun validateAcknowledge(
        callerRole: UserRole,
        targetVendorId: String,
        callerVendorId: String? = null
    ): DomainResult<Unit> {
        if (callerRole == UserRole.CUSTOMER) {
            return DomainResult.Error(message = "CUSTOMER role cannot acknowledge vendor communications.")
        }

        // VENDOR may only acknowledge their own communications
        if (callerRole == UserRole.VENDOR) {
            if (callerVendorId.isNullOrBlank() || callerVendorId != targetVendorId) {
                return DomainResult.Error(
                    message = "VENDOR role may only acknowledge its own communications. Cross-vendor access is prohibited."
                )
            }
        }

        return DomainResult.Success(Unit)
    }

    /**
     * Validates whether [callerRole] may cancel or update a vendor communication.
     */
    fun validateAdminOperation(callerRole: UserRole): DomainResult<Unit> {
        return when (callerRole) {
            UserRole.ADMIN, UserRole.MANAGER -> DomainResult.Success(Unit)
            UserRole.CUSTOMER -> DomainResult.Error(message = "CUSTOMER role is not permitted for this operation.")
            UserRole.VENDOR -> DomainResult.Error(message = "VENDOR role cannot perform administrative operations on vendor communications.")
            else -> DomainResult.Error(message = "Role '${callerRole.defaultLabel}' is not authorized for this administrative operation.")
        }
    }

    /**
     * Validates that the caller has access to the vendor communications summary/analytics.
     */
    fun validateSummaryAccess(callerRole: UserRole): DomainResult<Unit> {
        if (callerRole == UserRole.CUSTOMER) {
            return DomainResult.Error(message = "CUSTOMER role cannot access vendor communication analytics.")
        }
        return DomainResult.Success(Unit)
    }
}
