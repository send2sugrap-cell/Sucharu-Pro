package com.sucharu.sucharupro.domain.service.affiliate

import com.sucharu.sucharupro.domain.model.affiliate.*
import org.junit.Assert.*
import org.junit.Test

class AffiliateProfileValidationEngineTest {

    @Test
    fun `evaluateCompleteness scores required fields deterministically`() {
        val emptyProfile = AffiliateOperationalProfile(
            tenantId = "tenant-001",
            affiliateId = "aff-100",
            displayName = "New Partner"
        )
        val initialCompleteness = AffiliateProfileValidationEngine.evaluateCompleteness(emptyProfile)
        assertTrue(initialCompleteness.score in 10..30)
        assertFalse(initialCompleteness.isComplete)
        assertTrue(initialCompleteness.missingFields.contains("contactEmail"))
        assertTrue(initialCompleteness.missingFields.contains("country"))

        val fullProfile = AffiliateOperationalProfile(
            tenantId = "tenant-001",
            affiliateId = "aff-100",
            displayName = "Full Partner",
            legalName = "Full Partner Inc",
            businessType = AffiliateBusinessType.BUSINESS,
            businessDescription = "Leading software consultancy",
            contactEmail = "contact@fullpartner.com",
            contactPhone = "+15551234567",
            website = "https://fullpartner.com",
            addressLine1 = "100 Market St",
            city = "San Francisco",
            region = "CA",
            country = "United States",
            postalCode = "94105",
            taxIdOrGst = "US-EIN-99887766",
            taxInformationReference = "W9-DOC-2026"
        )
        val doc = AffiliateDocumentReference(
            tenantId = "tenant-001",
            documentId = "doc-1",
            affiliateId = "aff-100",
            documentType = AffiliateDocumentType.BUSINESS_REGISTRATION,
            storageReference = "gs://bucket/doc.pdf",
            fileName = "doc.pdf",
            status = AffiliateDocumentStatus.VERIFIED
        )
        val fullCompleteness = AffiliateProfileValidationEngine.evaluateCompleteness(fullProfile, listOf(doc))
        assertEquals(100, fullCompleteness.score)
        assertTrue(fullCompleteness.isComplete)
        assertTrue(fullCompleteness.missingFields.isEmpty())
    }

    @Test
    fun `validateProfileStateTransition enforces state machine rules`() {
        // INCOMPLETE -> SUBMITTED is valid
        AffiliateProfileValidationEngine.validateProfileStateTransition(
            AffiliateProfileStatus.INCOMPLETE,
            AffiliateProfileStatus.SUBMITTED
        )

        // SUBMITTED -> UNDER_REVIEW is valid
        AffiliateProfileValidationEngine.validateProfileStateTransition(
            AffiliateProfileStatus.SUBMITTED,
            AffiliateProfileStatus.UNDER_REVIEW
        )

        // UNDER_REVIEW -> VERIFIED is valid
        AffiliateProfileValidationEngine.validateProfileStateTransition(
            AffiliateProfileStatus.UNDER_REVIEW,
            AffiliateProfileStatus.VERIFIED
        )

        // VERIFIED -> INCOMPLETE is invalid
        assertThrows(IllegalStateException::class.java) {
            AffiliateProfileValidationEngine.validateProfileStateTransition(
                AffiliateProfileStatus.VERIFIED,
                AffiliateProfileStatus.INCOMPLETE
            )
        }
    }

    @Test
    fun `validateVerificationStateTransition enforces workflow constraints`() {
        AffiliateProfileValidationEngine.validateVerificationStateTransition(
            AffiliateVerificationStatus.SUBMITTED,
            AffiliateVerificationStatus.VERIFIED
        )

        assertThrows(IllegalStateException::class.java) {
            AffiliateProfileValidationEngine.validateVerificationStateTransition(
                AffiliateVerificationStatus.REJECTED,
                AffiliateVerificationStatus.VERIFIED
            )
        }
    }

    @Test
    fun `computeAuditChainHash maintains cryptographic chain integrity`() {
        val firstHash = AffiliateProfileValidationEngine.computeAuditChainHash(
            previousChainHash = null,
            recordHash = "hash123"
        )
        assertNotNull(firstHash)

        val secondHash = AffiliateProfileValidationEngine.computeAuditChainHash(
            previousChainHash = firstHash,
            recordHash = "hash456"
        )
        assertNotNull(secondHash)
        assertNotEquals(firstHash, secondHash)
    }
}
