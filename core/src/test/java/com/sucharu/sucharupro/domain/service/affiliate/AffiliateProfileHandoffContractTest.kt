package com.sucharu.sucharupro.domain.service.affiliate

import com.sucharu.sucharupro.data.api.model.affiliate.*
import com.sucharu.sucharupro.data.datasource.affiliate.FakeAffiliateDataSource
import com.sucharu.sucharupro.data.datasource.affiliate.FakeAffiliateProfileDataSource
import com.sucharu.sucharupro.data.repository.affiliate.AffiliateProfileRepositoryImpl
import com.sucharu.sucharupro.data.repository.affiliate.AffiliateRepositoryImpl
import com.sucharu.sucharupro.domain.model.affiliate.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AffiliateProfileHandoffContractTest {

    private lateinit var fakeAffiliateDataSource: FakeAffiliateDataSource
    private lateinit var fakeProfileDataSource: FakeAffiliateProfileDataSource
    private lateinit var affiliateRepo: AffiliateRepositoryImpl
    private lateinit var profileRepo: AffiliateProfileRepositoryImpl
    private lateinit var service: AffiliateProfileServiceImpl

    private val tenantId = "tenant-handoff"
    private val affiliateId = "aff-handoff-001"

    @Before
    fun setUp() {
        fakeAffiliateDataSource = FakeAffiliateDataSource()
        fakeProfileDataSource = FakeAffiliateProfileDataSource()
        affiliateRepo = AffiliateRepositoryImpl(fakeAffiliateDataSource)
        profileRepo = AffiliateProfileRepositoryImpl(fakeProfileDataSource)
        service = AffiliateProfileServiceImpl(
            profileRepository = profileRepo,
            affiliateRepository = affiliateRepo
        )

        runBlocking {
            fakeAffiliateDataSource.saveAffiliate(
                AffiliateProfile(
                    tenantId = tenantId,
                    affiliateId = affiliateId,
                    userId = "user-handoff",
                    displayName = "Handoff Partner",
                    affiliateCode = "HANDOFF2026",
                    status = AffiliateStatus.ACTIVE
                )
            )
        }
    }

    @Test
    fun `getHandoffContract produces sealed immutable read-only contract with integrity seal`() = runBlocking {
        val req = UpsertAffiliateProfileRequestDto(
            displayName = "Handoff Partner Inc",
            legalName = "Handoff Partner LLC",
            businessType = "BUSINESS",
            contactEmail = "handoff@example.com",
            contactPhone = "+15550001111",
            city = "Austin",
            country = "USA",
            taxIdOrGst = "TX-998877"
        )
        service.upsertProfile(tenantId, affiliateId, req, "actor-admin", "ADMIN")

        // Add verified verification check
        val verReq = RequestVerificationRequestDto(
            verificationType = "IDENTITY",
            reason = "Passport check"
        )
        val ver = service.requestVerification(
            tenantId,
            affiliateId,
            verReq,
            "actor-admin",
            "ADMIN"
        )
        service.approveVerification(
            tenantId = tenantId,
            verificationId = ver.verificationId,
            request = ReviewVerificationRequestDto(reason = "Verified"),
            actorUserId = "actor-admin",
            actorRole = "ADMIN"
        )

        // Add verified document
        val docReq = AddDocumentReferenceRequestDto(
            documentType = "IDENTITY_PROOF",
            storageReference = "gs://sucharu/id.pdf",
            fileName = "id.pdf"
        )
        val doc = service.addDocumentReference(
            tenantId,
            affiliateId,
            docReq,
            "actor-admin",
            "ADMIN"
        )
        service.verifyDocument(
            tenantId = tenantId,
            documentId = doc.documentId,
            actorUserId = "actor-admin",
            actorRole = "ADMIN"
        )

        val contract = service.getHandoffContract(tenantId, affiliateId)
        assertEquals("v1.0.0", contract.contractVersion)
        assertTrue(contract.isReadOnly)
        assertEquals(tenantId, contract.tenantId)
        assertEquals(affiliateId, contract.affiliateId)
        assertEquals(AffiliateProfileStatus.INCOMPLETE, contract.profileStatus)
        assertEquals(1, contract.documentCount)
        assertEquals(1, contract.verificationSummary.size)
        assertTrue(contract.integritySealHash.isNotBlank())

        // Verify forbidden AI actions are specified
        assertTrue(contract.forbiddenAiActions.contains("APPROVE_VERIFICATION"))
        assertTrue(contract.forbiddenAiActions.contains("MUTATE_PROFILE_DATA"))
        assertTrue(contract.forbiddenAiActions.contains("BYPASS_ROW_LEVEL_SECURITY"))
    }
}
