package com.sucharu.sucharupro.domain.model.communication.vendor.document

/**
 * Non-mutating analysis engine for assessing vendor compliance (Module 10 Step 06).
 *
 * Rules:
 * - Computes compliance percentages and overall status
 * - Detects missing required documents
 * - Identifies expired or expiring soon documents
 * - NEVER mutates vendor, financial, inventory, delivery, or production domains
 */
object VendorComplianceEngine {

    /**
     * Default list of document types required for active vendors.
     */
    val defaultRequiredDocumentTypes: Set<VendorDocumentType> = setOf(
        VendorDocumentType.BUSINESS_LICENSE,
        VendorDocumentType.TRADE_LICENSE,
        VendorDocumentType.TIN_CERTIFICATE,
        VendorDocumentType.BIN_CERTIFICATE,
        VendorDocumentType.BANK_INFORMATION,
        VendorDocumentType.CONTRACT
    )

    /**
     * Calculates compliance metrics for a single vendor within a project.
     */
    fun evaluateVendorCompliance(
        projectId: String,
        vendorId: String,
        documents: List<VendorDocument>,
        requiredTypes: Set<VendorDocumentType> = defaultRequiredDocumentTypes,
        now: Long = System.currentTimeMillis()
    ): VendorComplianceSummary {
        val vendorDocs = documents.filter { it.projectId == projectId && it.vendorId == vendorId }

        val activeDocs = vendorDocs.filter { it.status != VendorDocumentStatus.CANCELLED }

        // Find approved, unexpired docs by type
        val approvedValidDocs = activeDocs.filter { doc ->
            doc.status == VendorDocumentStatus.APPROVED &&
                    doc.verificationStatus == VendorDocumentVerificationStatus.VERIFIED &&
                    (doc.expiryDate == null || doc.expiryDate >= now)
        }

        val approvedDocTypes = approvedValidDocs.map { it.documentType }.toSet()

        val missingTypes = requiredTypes.filter { it !in approvedDocTypes }

        val totalRequired = requiredTypes.size
        val totalApprovedRequired = requiredTypes.count { it in approvedDocTypes }

        val pending = activeDocs.count {
            it.status == VendorDocumentStatus.SUBMITTED || it.status == VendorDocumentStatus.UNDER_REVIEW
        }
        val rejected = activeDocs.count { it.status == VendorDocumentStatus.REJECTED }
        val expired = activeDocs.count {
            it.status == VendorDocumentStatus.EXPIRED || (it.expiryDate != null && it.expiryDate < now)
        }
        val expiringSoon = activeDocs.count {
            it.expiryDate != null && it.expiryDate >= now && (it.expiryDate - now) <= (30L * 24 * 60 * 60 * 1000)
        }

        val compliancePercentage = if (totalRequired > 0) {
            (totalApprovedRequired.toDouble() / totalRequired.toDouble()) * 100.0
        } else {
            100.0
        }

        val overallStatus = when {
            expired > 0 -> VendorComplianceStatus.NON_COMPLIANT
            missingTypes.isEmpty() -> VendorComplianceStatus.COMPLIANT
            pending > 0 -> VendorComplianceStatus.UNDER_REVIEW
            expiringSoon > 0 -> VendorComplianceStatus.EXPIRING
            totalApprovedRequired > 0 -> VendorComplianceStatus.PARTIALLY_COMPLIANT
            else -> VendorComplianceStatus.NON_COMPLIANT
        }

        return VendorComplianceSummary(
            projectId = projectId,
            vendorId = vendorId,
            totalRequiredDocuments = totalRequired,
            submittedDocuments = activeDocs.size,
            approvedDocuments = approvedValidDocs.size,
            pendingDocuments = pending,
            rejectedDocuments = rejected,
            expiredDocuments = expired,
            expiringSoonDocuments = expiringSoon,
            missingRequiredDocumentTypes = missingTypes,
            compliancePercentage = compliancePercentage,
            overallStatus = overallStatus,
            evaluatedAt = now
        )
    }
}
