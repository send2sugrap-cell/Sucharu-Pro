package com.sucharu.sucharupro.domain.validation.communication.vendor.document

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.communication.vendor.document.VendorDocumentType
import com.sucharu.sucharupro.domain.model.user.UserRole
import org.junit.Assert.assertTrue
import org.junit.Test

class VendorDocumentAuthorizationValidatorTest {

    // ─────────────────────────────────────────────
    // validateCreateRequest
    // ─────────────────────────────────────────────

    @Test
    fun createRequest_adminRole_anyDocumentType_succeeds() {
        VendorDocumentType.entries.forEach { type ->
            val result = VendorDocumentAuthorizationValidator.validateCreateRequest(UserRole.ADMIN, type)
            assertTrue("ADMIN should create $type", result is DomainResult.Success)
        }
    }

    @Test
    fun createRequest_managerRole_anyDocumentType_succeeds() {
        VendorDocumentType.entries.forEach { type ->
            val result = VendorDocumentAuthorizationValidator.validateCreateRequest(UserRole.MANAGER, type)
            assertTrue("MANAGER should create $type", result is DomainResult.Success)
        }
    }

    @Test
    fun createRequest_staffRole_anyDocumentType_succeeds() {
        VendorDocumentType.entries.forEach { type ->
            val result = VendorDocumentAuthorizationValidator.validateCreateRequest(UserRole.STAFF, type)
            assertTrue("STAFF should create $type", result is DomainResult.Success)
        }
    }

    @Test
    fun createRequest_customerRole_alwaysDenied() {
        VendorDocumentType.entries.forEach { type ->
            val result = VendorDocumentAuthorizationValidator.validateCreateRequest(UserRole.CUSTOMER, type)
            assertTrue("CUSTOMER must be denied for $type", result is DomainResult.Error)
        }
    }

    @Test
    fun createRequest_vendorRole_alwaysDenied() {
        VendorDocumentType.entries.forEach { type ->
            val result = VendorDocumentAuthorizationValidator.validateCreateRequest(UserRole.VENDOR, type)
            assertTrue("VENDOR must be denied for $type", result is DomainResult.Error)
        }
    }

    @Test
    fun createRequest_accountsRole_financeDocuments_succeed() {
        val financeTypes = listOf(
            VendorDocumentType.TAX_DOCUMENT,
            VendorDocumentType.VAT_DOCUMENT,
            VendorDocumentType.TIN_CERTIFICATE,
            VendorDocumentType.BIN_CERTIFICATE,
            VendorDocumentType.BANK_INFORMATION,
            VendorDocumentType.BANK_CERTIFICATE,
            VendorDocumentType.INVOICE_SUPPORTING_DOCUMENT
        )
        financeTypes.forEach { type ->
            val result = VendorDocumentAuthorizationValidator.validateCreateRequest(UserRole.ACCOUNTS, type)
            assertTrue("ACCOUNTS should be allowed for finance doc $type", result is DomainResult.Success)
        }
    }

    @Test
    fun createRequest_accountsRole_nonFinanceDocuments_denied() {
        val nonFinanceTypes = listOf(
            VendorDocumentType.QUALITY_CERTIFICATE,
            VendorDocumentType.COMPLIANCE_CERTIFICATE,
            VendorDocumentType.DELIVERY_SUPPORTING_DOCUMENT,
            VendorDocumentType.CONTRACT,
            VendorDocumentType.OTHER
        )
        nonFinanceTypes.forEach { type ->
            val result = VendorDocumentAuthorizationValidator.validateCreateRequest(UserRole.ACCOUNTS, type)
            assertTrue("ACCOUNTS must be denied for non-finance doc $type", result is DomainResult.Error)
        }
    }

    @Test
    fun createRequest_qcInspectorRole_qcDocuments_succeed() {
        val qcTypes = listOf(
            VendorDocumentType.QUALITY_CERTIFICATE,
            VendorDocumentType.PRODUCT_CERTIFICATE,
            VendorDocumentType.COMPLIANCE_CERTIFICATE,
            VendorDocumentType.SAFETY_CERTIFICATE
        )
        qcTypes.forEach { type ->
            val result = VendorDocumentAuthorizationValidator.validateCreateRequest(UserRole.QC_INSPECTOR, type)
            assertTrue("QC_INSPECTOR should be allowed for QC doc $type", result is DomainResult.Success)
        }
    }

    @Test
    fun createRequest_qcInspectorRole_nonQcDocuments_denied() {
        val nonQcTypes = listOf(
            VendorDocumentType.TAX_DOCUMENT,
            VendorDocumentType.BANK_INFORMATION,
            VendorDocumentType.DELIVERY_SUPPORTING_DOCUMENT,
            VendorDocumentType.OTHER
        )
        nonQcTypes.forEach { type ->
            val result = VendorDocumentAuthorizationValidator.validateCreateRequest(UserRole.QC_INSPECTOR, type)
            assertTrue("QC_INSPECTOR must be denied for non-QC doc $type", result is DomainResult.Error)
        }
    }

    @Test
    fun createRequest_warehouseRole_warehouseDocuments_succeed() {
        val warehouseTypes = listOf(
            VendorDocumentType.DELIVERY_SUPPORTING_DOCUMENT,
            VendorDocumentType.ADDRESS_PROOF,
            VendorDocumentType.SAFETY_CERTIFICATE
        )
        warehouseTypes.forEach { type ->
            val result = VendorDocumentAuthorizationValidator.validateCreateRequest(UserRole.WAREHOUSE, type)
            assertTrue("WAREHOUSE should be allowed for $type", result is DomainResult.Success)
        }
    }

    @Test
    fun createRequest_warehouseRole_nonWarehouseDocuments_denied() {
        val nonWarehouseTypes = listOf(
            VendorDocumentType.TAX_DOCUMENT,
            VendorDocumentType.QUALITY_CERTIFICATE,
            VendorDocumentType.CONTRACT,
            VendorDocumentType.OTHER
        )
        nonWarehouseTypes.forEach { type ->
            val result = VendorDocumentAuthorizationValidator.validateCreateRequest(UserRole.WAREHOUSE, type)
            assertTrue("WAREHOUSE must be denied for $type", result is DomainResult.Error)
        }
    }

    // ─────────────────────────────────────────────
    // validateSubmitDocument
    // ─────────────────────────────────────────────

    @Test
    fun submitDocument_customerRole_alwaysDenied() {
        val result = VendorDocumentAuthorizationValidator.validateSubmitDocument(
            callerRole = UserRole.CUSTOMER,
            targetVendorId = "ven-001"
        )
        assertTrue(result is DomainResult.Error)
    }

    @Test
    fun submitDocument_vendorRole_ownVendorId_succeeds() {
        val result = VendorDocumentAuthorizationValidator.validateSubmitDocument(
            callerRole = UserRole.VENDOR,
            targetVendorId = "ven-001",
            callerVendorId = "ven-001"
        )
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun submitDocument_vendorRole_differentVendorId_denied() {
        val result = VendorDocumentAuthorizationValidator.validateSubmitDocument(
            callerRole = UserRole.VENDOR,
            targetVendorId = "ven-001",
            callerVendorId = "ven-999"
        )
        assertTrue(result is DomainResult.Error)
    }

    @Test
    fun submitDocument_vendorRole_nullCallerVendorId_denied() {
        val result = VendorDocumentAuthorizationValidator.validateSubmitDocument(
            callerRole = UserRole.VENDOR,
            targetVendorId = "ven-001",
            callerVendorId = null
        )
        assertTrue(result is DomainResult.Error)
    }

    @Test
    fun submitDocument_vendorRole_blankCallerVendorId_denied() {
        val result = VendorDocumentAuthorizationValidator.validateSubmitDocument(
            callerRole = UserRole.VENDOR,
            targetVendorId = "ven-001",
            callerVendorId = "   "
        )
        assertTrue(result is DomainResult.Error)
    }

    @Test
    fun submitDocument_internalRoles_noVendorIdNeeded_succeed() {
        val internalRoles = listOf(UserRole.ADMIN, UserRole.MANAGER, UserRole.STAFF, UserRole.ACCOUNTS)
        internalRoles.forEach { role ->
            val result = VendorDocumentAuthorizationValidator.validateSubmitDocument(
                callerRole = role,
                targetVendorId = "ven-001"
            )
            assertTrue("$role should be allowed to submit without callerVendorId", result is DomainResult.Success)
        }
    }

    // ─────────────────────────────────────────────
    // validateReviewAction
    // ─────────────────────────────────────────────

    @Test
    fun reviewAction_customerRole_alwaysDenied() {
        VendorDocumentType.entries.forEach { type ->
            val result = VendorDocumentAuthorizationValidator.validateReviewAction(UserRole.CUSTOMER, type)
            assertTrue("CUSTOMER must be denied review for $type", result is DomainResult.Error)
        }
    }

    @Test
    fun reviewAction_vendorRole_alwaysDenied() {
        VendorDocumentType.entries.forEach { type ->
            val result = VendorDocumentAuthorizationValidator.validateReviewAction(UserRole.VENDOR, type)
            assertTrue("VENDOR must be denied review for $type", result is DomainResult.Error)
        }
    }

    @Test
    fun reviewAction_adminRole_anyDocument_succeeds() {
        VendorDocumentType.entries.forEach { type ->
            val result = VendorDocumentAuthorizationValidator.validateReviewAction(UserRole.ADMIN, type)
            assertTrue("ADMIN should review $type", result is DomainResult.Success)
        }
    }

    @Test
    fun reviewAction_managerRole_anyDocument_succeeds() {
        VendorDocumentType.entries.forEach { type ->
            val result = VendorDocumentAuthorizationValidator.validateReviewAction(UserRole.MANAGER, type)
            assertTrue("MANAGER should review $type", result is DomainResult.Success)
        }
    }

    @Test
    fun reviewAction_accountsRole_financeDocuments_succeed() {
        val financeTypes = listOf(
            VendorDocumentType.TAX_DOCUMENT,
            VendorDocumentType.BANK_INFORMATION,
            VendorDocumentType.INVOICE_SUPPORTING_DOCUMENT
        )
        financeTypes.forEach { type ->
            val result = VendorDocumentAuthorizationValidator.validateReviewAction(UserRole.ACCOUNTS, type)
            assertTrue("ACCOUNTS should review finance doc $type", result is DomainResult.Success)
        }
    }

    @Test
    fun reviewAction_accountsRole_nonFinanceDocuments_denied() {
        val nonFinanceTypes = listOf(
            VendorDocumentType.QUALITY_CERTIFICATE,
            VendorDocumentType.DELIVERY_SUPPORTING_DOCUMENT,
            VendorDocumentType.OTHER
        )
        nonFinanceTypes.forEach { type ->
            val result = VendorDocumentAuthorizationValidator.validateReviewAction(UserRole.ACCOUNTS, type)
            assertTrue("ACCOUNTS must be denied review for non-finance $type", result is DomainResult.Error)
        }
    }

    @Test
    fun reviewAction_qcInspectorRole_qcDocuments_succeed() {
        val qcTypes = listOf(
            VendorDocumentType.QUALITY_CERTIFICATE,
            VendorDocumentType.PRODUCT_CERTIFICATE,
            VendorDocumentType.COMPLIANCE_CERTIFICATE,
            VendorDocumentType.SAFETY_CERTIFICATE
        )
        qcTypes.forEach { type ->
            val result = VendorDocumentAuthorizationValidator.validateReviewAction(UserRole.QC_INSPECTOR, type)
            assertTrue("QC_INSPECTOR should review $type", result is DomainResult.Success)
        }
    }

    @Test
    fun reviewAction_qcInspectorRole_nonQcDocuments_denied() {
        val nonQcTypes = listOf(
            VendorDocumentType.TAX_DOCUMENT,
            VendorDocumentType.BANK_INFORMATION,
            VendorDocumentType.DELIVERY_SUPPORTING_DOCUMENT
        )
        nonQcTypes.forEach { type ->
            val result = VendorDocumentAuthorizationValidator.validateReviewAction(UserRole.QC_INSPECTOR, type)
            assertTrue("QC_INSPECTOR must be denied review for $type", result is DomainResult.Error)
        }
    }

    // ─────────────────────────────────────────────
    // validateReadAccess
    // ─────────────────────────────────────────────

    @Test
    fun readAccess_customerRole_alwaysDenied() {
        val result = VendorDocumentAuthorizationValidator.validateReadAccess(
            callerRole = UserRole.CUSTOMER,
            targetVendorId = "ven-001"
        )
        assertTrue(result is DomainResult.Error)
    }

    @Test
    fun readAccess_vendorRole_ownVendorId_succeeds() {
        val result = VendorDocumentAuthorizationValidator.validateReadAccess(
            callerRole = UserRole.VENDOR,
            targetVendorId = "ven-001",
            callerVendorId = "ven-001"
        )
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun readAccess_vendorRole_crossVendorAccess_denied() {
        val result = VendorDocumentAuthorizationValidator.validateReadAccess(
            callerRole = UserRole.VENDOR,
            targetVendorId = "ven-001",
            callerVendorId = "ven-002"
        )
        assertTrue(result is DomainResult.Error)
    }

    @Test
    fun readAccess_vendorRole_nullCallerVendorId_denied() {
        val result = VendorDocumentAuthorizationValidator.validateReadAccess(
            callerRole = UserRole.VENDOR,
            targetVendorId = "ven-001",
            callerVendorId = null
        )
        assertTrue(result is DomainResult.Error)
    }

    @Test
    fun readAccess_internalRoles_anyVendor_succeed() {
        val internalRoles = listOf(
            UserRole.ADMIN, UserRole.MANAGER, UserRole.STAFF,
            UserRole.ACCOUNTS, UserRole.QC_INSPECTOR, UserRole.WAREHOUSE
        )
        internalRoles.forEach { role ->
            val result = VendorDocumentAuthorizationValidator.validateReadAccess(
                callerRole = role,
                targetVendorId = "ven-001"
            )
            assertTrue("$role should be allowed to read vendor documents", result is DomainResult.Success)
        }
    }
}
