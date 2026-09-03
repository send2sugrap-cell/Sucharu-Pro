package com.sucharu.sucharupro.domain.service.affiliate

import com.sucharu.sucharupro.domain.model.affiliate.*
import org.junit.Assert.*
import org.junit.Test

class AffiliateValidationEngineTest {

    @Test
    fun `test normalizeAndValidateCode sanitizes and enforces format rules`() {
        val valid1 = AffiliateValidationEngine.normalizeAndValidateCode("promo_2026")
        assertEquals("PROMO_2026", valid1)

        val valid2 = AffiliateValidationEngine.normalizeAndValidateCode("  creator-01  ")
        assertEquals("CREATOR-01", valid2)

        assertThrows(IllegalArgumentException::class.java) {
            AffiliateValidationEngine.normalizeAndValidateCode("ab") // too short (<3)
        }

        assertThrows(IllegalArgumentException::class.java) {
            AffiliateValidationEngine.normalizeAndValidateCode("invalid@code!") // special invalid characters
        }

        assertThrows(IllegalArgumentException::class.java) {
            AffiliateValidationEngine.normalizeAndValidateCode("   ")
        }
    }

    @Test
    fun `test generateDefaultAffiliateCode creates deterministic formatted slug`() {
        val code1 = AffiliateValidationEngine.generateDefaultAffiliateCode("John Doe Graphics", "usr-1234")
        assertTrue(code1.startsWith("JOHNDOEG_"))
        assertTrue(code1.endsWith("1234"))
    }

    @Test
    fun `test validateStateTransition allows valid lifecycle steps and rejects invalid ones`() {
        // Valid transitions
        AffiliateValidationEngine.validateStateTransition(AffiliateStatus.PENDING, AffiliateStatus.ACTIVE)
        AffiliateValidationEngine.validateStateTransition(AffiliateStatus.PENDING, AffiliateStatus.REJECTED)
        AffiliateValidationEngine.validateStateTransition(AffiliateStatus.ACTIVE, AffiliateStatus.SUSPENDED)
        AffiliateValidationEngine.validateStateTransition(AffiliateStatus.SUSPENDED, AffiliateStatus.ACTIVE)
        AffiliateValidationEngine.validateStateTransition(AffiliateStatus.ACTIVE, AffiliateStatus.INACTIVE)
        AffiliateValidationEngine.validateStateTransition(AffiliateStatus.INACTIVE, AffiliateStatus.ACTIVE)
        AffiliateValidationEngine.validateStateTransition(AffiliateStatus.ACTIVE, AffiliateStatus.TERMINATED)
        AffiliateValidationEngine.validateStateTransition(AffiliateStatus.SUSPENDED, AffiliateStatus.TERMINATED)

        // Invalid transitions
        assertThrows(IllegalStateException::class.java) {
            AffiliateValidationEngine.validateStateTransition(AffiliateStatus.TERMINATED, AffiliateStatus.ACTIVE)
        }

        assertThrows(IllegalStateException::class.java) {
            AffiliateValidationEngine.validateStateTransition(AffiliateStatus.REJECTED, AffiliateStatus.ACTIVE)
        }
    }

    @Test
    fun `test evaluateEligibility assesses identity, agreement, account status and tax compliance`() {
        val validProfile = AffiliateProfile(
            affiliateId = "AFF-001",
            tenantId = "TENANT-1",
            userId = "USR-001",
            displayName = "Top Partner Corp",
            affiliateCode = "TOP_PARTNER",
            status = AffiliateStatus.ACTIVE,
            affiliateType = AffiliateType.BUSINESS,
            taxIdOrGst = "GSTIN-12345678",
            verificationState = VerificationState.VERIFIED,
            agreementReference = "AGR-2026-V1",
            agreementAcceptedAt = System.currentTimeMillis()
        )

        val eligibility = AffiliateValidationEngine.evaluateEligibility(validProfile, "ADMIN-1")
        assertTrue(eligibility.isEligible)
        assertTrue(eligibility.identityVerified)
        assertTrue(eligibility.agreementAccepted)
        assertTrue(eligibility.accountActive)
        assertTrue(eligibility.taxCompliant)
        assertTrue(eligibility.businessVerified)
        assertTrue(eligibility.rejectionReasons.isEmpty())

        // Ineligible if unverified and no tax ID for business
        val ineligibleProfile = validProfile.copy(
            verificationState = VerificationState.UNVERIFIED,
            taxIdOrGst = null
        )
        val inel = AffiliateValidationEngine.evaluateEligibility(ineligibleProfile, "ADMIN-1")
        assertFalse(inel.isEligible)
        assertFalse(inel.identityVerified)
        assertFalse(inel.taxCompliant)
        assertEquals(2, inel.rejectionReasons.size)
    }

    @Test
    fun `test cryptographic hash calculation and chaining`() {
        val recordHash1 = AffiliateValidationEngine.computeRecordHash(
            tenantId = "TENANT-1",
            affiliateId = "AFF-001",
            eventType = AffiliateAuditEventType.AFFILIATE_CREATED,
            previousStatus = null,
            newStatus = AffiliateStatus.PENDING,
            actorType = AffiliateActorType.HUMAN,
            actorId = "USR-1",
            actorRole = "STAFF",
            timestamp = 1000L,
            correlationId = "CORR-1",
            reason = "Initial profile creation"
        )
        assertNotNull(recordHash1)
        assertEquals(64, recordHash1.length) // SHA-256 hex length

        val chainHash1 = AffiliateValidationEngine.computeChainHash(null, recordHash1)
        assertNotNull(chainHash1)
        assertEquals(64, chainHash1.length)

        val recordHash2 = AffiliateValidationEngine.computeRecordHash(
            tenantId = "TENANT-1",
            affiliateId = "AFF-001",
            eventType = AffiliateAuditEventType.AFFILIATE_ACTIVATED,
            previousStatus = AffiliateStatus.PENDING,
            newStatus = AffiliateStatus.ACTIVE,
            actorType = AffiliateActorType.HUMAN,
            actorId = "MGR-1",
            actorRole = "MANAGER",
            timestamp = 2000L,
            correlationId = "CORR-2",
            reason = "Approved"
        )
        val chainHash2 = AffiliateValidationEngine.computeChainHash(chainHash1, recordHash2)
        assertNotEquals(chainHash1, chainHash2)
    }

    @Test
    fun `test synthesizeHandoffContract produces sealed contract with strict AI boundaries`() {
        val profile = AffiliateProfile(
            affiliateId = "AFF-001",
            tenantId = "TENANT-1",
            userId = "USR-001",
            displayName = "Creator Pro",
            affiliateCode = "CREATOR_PRO",
            status = AffiliateStatus.ACTIVE,
            affiliateType = AffiliateType.CREATOR,
            verificationState = VerificationState.VERIFIED,
            agreementReference = "AGR-001",
            agreementAcceptedAt = 12345L
        )
        val eligibility = AffiliateValidationEngine.evaluateEligibility(profile, "ADMIN-1")
        val contract = AffiliateValidationEngine.synthesizeHandoffContract(profile, eligibility)

        assertEquals("v1.0.0", contract.contractVersion)
        assertTrue(contract.isReadOnly)
        assertTrue(contract.isEligibleForCommission)
        assertTrue(contract.isEligibleForAttribution)
        assertTrue(contract.allowedAiActions.contains("INSPECT_AFFILIATE_PROFILE"))
        assertTrue(contract.forbiddenAiActions.contains("ACTIVATE_AFFILIATE"))
        assertTrue(contract.forbiddenAiActions.contains("BYPASS_ROW_LEVEL_SECURITY"))
        assertEquals(64, contract.integritySealHash.length)
    }
}
