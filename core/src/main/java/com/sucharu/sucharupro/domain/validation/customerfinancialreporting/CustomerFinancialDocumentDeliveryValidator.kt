package com.sucharu.sucharupro.domain.validation.customerfinancialreporting

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customerfinancialreporting.CustomerFinancialDeliveryStatus
import com.sucharu.sucharupro.domain.model.customerfinancialreporting.CustomerFinancialDocumentDelivery

/**
 * Domain validator for Customer Financial Document Delivery operations (Module 14 Step 11).
 */
object CustomerFinancialDocumentDeliveryValidator {

    fun validateCreation(
        tenantId: String,
        projectId: String,
        customerId: String,
        documentName: String,
        checksum: String,
        fileSize: Long
    ): DomainResult<Unit> {
        if (tenantId.isBlank()) return DomainResult.Error(IllegalArgumentException("Tenant ID cannot be blank."))
        if (projectId.isBlank()) return DomainResult.Error(IllegalArgumentException("Project ID cannot be blank."))
        if (customerId.isBlank()) return DomainResult.Error(IllegalArgumentException("Customer ID cannot be blank."))
        if (documentName.isBlank()) return DomainResult.Error(IllegalArgumentException("Document name cannot be blank."))
        if (checksum.isBlank()) return DomainResult.Error(IllegalArgumentException("Document checksum cannot be blank."))
        if (fileSize < 0) return DomainResult.Error(IllegalArgumentException("File size cannot be negative."))
        return DomainResult.Success(Unit)
    }

    fun validateAccess(delivery: CustomerFinancialDocumentDelivery?): DomainResult<Unit> {
        if (delivery == null) {
            return DomainResult.Error(IllegalArgumentException("Document delivery record not found."))
        }
        if (delivery.isRevoked) {
            return DomainResult.Error(IllegalStateException("Access denied: Document delivery has been revoked."))
        }
        if (delivery.isExpired) {
            return DomainResult.Error(IllegalStateException("Access denied: Document delivery link has expired."))
        }
        if (delivery.deliveryStatus == CustomerFinancialDeliveryStatus.FAILED) {
            return DomainResult.Error(IllegalStateException("Document delivery is in FAILED state."))
        }
        return DomainResult.Success(Unit)
    }

    fun validateRevocation(
        delivery: CustomerFinancialDocumentDelivery?,
        reason: String?
    ): DomainResult<Unit> {
        if (delivery == null) {
            return DomainResult.Error(IllegalArgumentException("Document delivery record not found."))
        }
        if (delivery.isRevoked) {
            return DomainResult.Error(IllegalStateException("Document delivery is already revoked."))
        }
        if (reason.isNullOrBlank()) {
            return DomainResult.Error(IllegalArgumentException("Revocation reason is mandatory."))
        }
        return DomainResult.Success(Unit)
    }
}
