package com.sucharu.sucharupro.domain.service.affiliate

import com.sucharu.sucharupro.domain.model.affiliate.*
import org.junit.Assert.*
import org.junit.Test

class AffiliateCommandCenterPolicyTest {

    private val tenantId = "TENANT-POL-TEST"

    @Test
    fun testSynthesizeWorkItems_GeneratesExpectedQueueItems() {
        val aff1 = AffiliateProfile(
            affiliateId = "AFF-1",
            tenantId = tenantId,
            userId = "U-1",
            affiliateCode = "COD1",
            displayName = "Aff 1",
            contactEmail = "aff1@test.com",
            status = AffiliateStatus.PENDING,
            agreementReference = null
        )

        val aff2 = AffiliateProfile(
            affiliateId = "AFF-2",
            tenantId = tenantId,
            userId = "U-2",
            affiliateCode = "COD2",
            displayName = "Aff 2",
            contactEmail = "aff2@test.com",
            status = AffiliateStatus.SUSPENDED,
            agreementReference = "AGR-123"
        )

        val items = AffiliateCommandCenterPolicyEngine.synthesizeWorkItems(
            tenantId = tenantId,
            affiliates = listOf(aff1, aff2),
            operationalProfiles = emptyMap(),
            verifications = emptyMap(),
            enrollments = emptyMap(),
            communications = emptyMap()
        )

        assertTrue(items.any { it.affiliateId == "AFF-1" && it.itemType == AffiliateGovernanceWorkItemType.PENDING_REVIEW })
        assertTrue(items.any { it.affiliateId == "AFF-2" && it.itemType == AffiliateGovernanceWorkItemType.SUSPENDED_REVIEW })
        assertTrue(items.any { it.affiliateId == "AFF-1" && it.itemType == AffiliateGovernanceWorkItemType.AGREEMENT_ACCEPTANCE })
    }

    @Test
    fun testAuditChainHash_DeterministicAndChained() {
        val hash1 = AffiliateCommandCenterPolicyEngine.computeAuditRecordHash(
            tenantId = tenantId,
            auditId = "AUD-1",
            affiliateId = "AFF-1",
            workItemId = "WI-1",
            actorUserId = "ADMIN-1",
            action = "APPROVE",
            previousState = "PENDING",
            newState = "ACTIVE",
            correlationId = "CORR-1",
            timestamp = 1000L
        )

        val chainHash1 = AffiliateCommandCenterPolicyEngine.computeAuditChainHash(null, hash1)
        assertNotNull(chainHash1)

        val hash2 = AffiliateCommandCenterPolicyEngine.computeAuditRecordHash(
            tenantId = tenantId,
            auditId = "AUD-2",
            affiliateId = "AFF-1",
            workItemId = null,
            actorUserId = "ADMIN-1",
            action = "SUSPEND",
            previousState = "ACTIVE",
            newState = "SUSPENDED",
            correlationId = "CORR-2",
            timestamp = 2000L
        )

        val chainHash2 = AffiliateCommandCenterPolicyEngine.computeAuditChainHash(chainHash1, hash2)
        assertNotEquals(chainHash1, chainHash2)
    }

    @Test
    fun testSynthesizeHandoffContract_GeneratesValidSealedContract() {
        val aff = AffiliateProfile(
            affiliateId = "AFF-10",
            tenantId = tenantId,
            userId = "U-10",
            affiliateCode = "COD10",
            displayName = "Aff 10",
            contactEmail = "aff10@test.com",
            status = AffiliateStatus.ACTIVE
        )

        val contract = AffiliateCommandCenterPolicyEngine.synthesizeHandoffContract(
            tenantId = tenantId,
            userId = "ADMIN-99",
            affiliates = listOf(aff),
            workItems = emptyList()
        )

        assertEquals("20.05", contract.stepVersion)
        assertEquals("AFFILIATE_ADMINISTRATIVE_COMMAND_CENTER", contract.moduleScope)
        assertTrue(contract.isReadOnly)
        assertNotNull(contract.integritySealHash)
        assertEquals(1L, contract.totalAffiliates)
        assertEquals(1L, contract.activeAffiliates)
    }
}
