package com.sucharu.sucharupro.domain.validation.communication.vendor.document

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.communication.vendor.document.VendorDocumentType
import com.sucharu.sucharupro.domain.model.user.UserRole

/**
 * RBAC authorization validator for Vendor Document & Compliance operations (Module 10 Step 06).
 */
object VendorDocumentAuthorizationValidator {

    private val financeDocumentTypes = setOf(
        VendorDocumentType.TAX_DOCUMENT,
        VendorDocumentType.VAT_DOCUMENT,
        VendorDocumentType.TIN_CERTIFICATE,
        VendorDocumentType.BIN_CERTIFICATE,
        VendorDocumentType.BANK_INFORMATION,
        VendorDocumentType.BANK_CERTIFICATE,
        VendorDocumentType.INVOICE_SUPPORTING_DOCUMENT
    )

    private val qcDocumentTypes = setOf(
        VendorDocumentType.QUALITY_CERTIFICATE,
        VendorDocumentType.PRODUCT_CERTIFICATE,
        VendorDocumentType.COMPLIANCE_CERTIFICATE,
        VendorDocumentType.SAFETY_CERTIFICATE
    )

    private val warehouseDocumentTypes = setOf(
        VendorDocumentType.DELIVERY_SUPPORTING_DOCUMENT,
        VendorDocumentType.ADDRESS_PROOF,
        VendorDocumentType.SAFETY_CERTIFICATE
    )

    /**
     * Validates whether [callerRole] may create a document request of [documentType].
     */
    fun validateCreateRequest(
        callerRole: UserRole,
        documentType: VendorDocumentType
    ): DomainResult<Unit> {
        if (callerRole == UserRole.CUSTOMER) {
            return DomainResult.Error(message = "CUSTOMER role is not permitted to request vendor documents.")
        }
        if (callerRole == UserRole.VENDOR) {
            return DomainResult.Error(message = "VENDOR role cannot issue document requests.")
        }
        if (callerRole == UserRole.ACCOUNTS && documentType !in financeDocumentTypes) {
            return DomainResult.Error(message = "ACCOUNTS role may only request financial-related documents.")
        }
        if (callerRole == UserRole.QC_INSPECTOR && documentType !in qcDocumentTypes) {
            return DomainResult.Error(message = "QC_INSPECTOR role may only request quality/compliance certificates.")
        }
        if (callerRole == UserRole.WAREHOUSE && documentType !in warehouseDocumentTypes) {
            return DomainResult.Error(message = "WAREHOUSE role may only request operational/delivery documents.")
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Validates whether [callerRole] may submit a document for [targetVendorId].
     */
    fun validateSubmitDocument(
        callerRole: UserRole,
        targetVendorId: String,
        callerVendorId: String? = null
    ): DomainResult<Unit> {
        if (callerRole == UserRole.CUSTOMER) {
            return DomainResult.Error(message = "CUSTOMER role cannot submit vendor documents.")
        }
        if (callerRole == UserRole.VENDOR) {
            if (callerVendorId.isNullOrBlank() || callerVendorId != targetVendorId) {
                return DomainResult.Error(message = "VENDOR role may only submit documents for its own organization.")
            }
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Validates whether [callerRole] may review/approve/reject a document.
     */
    fun validateReviewAction(
        callerRole: UserRole,
        documentType: VendorDocumentType
    ): DomainResult<Unit> {
        if (callerRole == UserRole.CUSTOMER || callerRole == UserRole.VENDOR) {
            return DomainResult.Error(message = "External actors cannot review or approve vendor documents.")
        }
        if (callerRole == UserRole.ADMIN || callerRole == UserRole.MANAGER) {
            return DomainResult.Success(Unit)
        }
        if (callerRole == UserRole.ACCOUNTS && documentType in financeDocumentTypes) {
            return DomainResult.Success(Unit)
        }
        if (callerRole == UserRole.QC_INSPECTOR && documentType in qcDocumentTypes) {
            return DomainResult.Success(Unit)
        }
        return DomainResult.Error(message = "Role '$callerRole' is not authorized to review document type '${documentType.defaultLabel}'.")
    }

    /**
     * Validates whether [callerRole] may read/view documents for [targetVendorId].
     */
    fun validateReadAccess(
        callerRole: UserRole,
        targetVendorId: String,
        callerVendorId: String? = null
    ): DomainResult<Unit> {
        if (callerRole == UserRole.CUSTOMER) {
            return DomainResult.Error(message = "CUSTOMER role cannot access vendor documents.")
        }
        if (callerRole == UserRole.VENDOR) {
            if (callerVendorId.isNullOrBlank() || callerVendorId != targetVendorId) {
                return DomainResult.Error(message = "VENDOR role may only view its own documents. Cross-vendor access is prohibited.")
            }
        }
        return DomainResult.Success(Unit)
    }
}
