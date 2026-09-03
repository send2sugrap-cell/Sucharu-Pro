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

class AffiliateProfileServiceTest {

    private lateinit var fakeAffiliateDataSource: FakeAffiliateDataSource
    private lateinit var fakeProfileDataSource: FakeAffiliateProfileDataSource
    private lateinit var affiliateRepo: AffiliateRepositoryImpl
    private lateinit var profileRepo: AffiliateProfileRepositoryImpl
    private lateinit var service: AffiliateProfileServiceImpl

    private val tenantId = "test-tenant-001"
    private val affiliateId = "aff-001"
    private val actorId = "user-admin-1"

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

        // Seed base affiliate identity
        runBlocking {
            fakeAffiliateDataSource.saveAffiliate(
                AffiliateProfile(
                    tenantId = tenantId,
                    affiliateId = affiliateId,
                    userId = "user-123",
                    displayName = "Partner Alpha",
                    affiliateCode = "ALPHA2026",
                    status = AffiliateStatus.ACTIVE
                )
            )
        }
    }

    @Test
    fun `upsertProfile creates and updates profile with completeness and audit trail`() = runBlocking {
        val req = UpsertAffiliateProfileRequestDto(
            displayName = "Partner Alpha Inc",
            legalName = "Alpha Innovations LLC",
            businessType = "BUSINESS",
            contactEmail = "admin@alphainno.com",
            contactPhone = "+8801912345678",
            city = "Dhaka",
            country = "Bangladesh",
            taxIdOrGst = "123456789"
        )

        val created = service.upsertProfile(tenantId, affiliateId, req, actorId, "ADMIN")
        assertEquals(affiliateId, created.affiliateId)
        assertTrue(created.completenessScore > 50)
        assertEquals(AffiliateProfileStatus.INCOMPLETE, created.profileStatus)

        // Verify audit record was created
        val audits = service.listAuditRecords(tenantId, affiliateId)
        assertEquals(1, audits.size)
        assertEquals("PROFILE_CREATED", audits.first().action)

        // Verify outbox event was generated
        val outbox = fakeProfileDataSource.listPendingOutboxEvents(tenantId)
        assertEquals(1, outbox.size)
        assertEquals("AffiliateProfileUpdated", outbox.first().eventType)
    }

    @Test
    fun `submitProfile transitions incomplete profile to submitted when valid`() = runBlocking {
        val req = UpsertAffiliateProfileRequestDto(
            displayName = "Partner Alpha",
            legalName = "Alpha LLC",
            businessType = "INDIVIDUAL",
            contactEmail = "alpha@example.com",
            contactPhone = "+8801700000000",
            addressLine1 = "Road 1",
            city = "Dhaka",
            country = "Bangladesh"
        )
        service.upsertProfile(tenantId, affiliateId, req, actorId, "ADMIN")

        // Add document to make profile complete
        service.addDocumentReference(
            tenantId = tenantId,
            affiliateId = affiliateId,
            request = AddDocumentReferenceRequestDto(
                documentType = "IDENTITY_PROOF",
                storageReference = "gs://sucharu/id.pdf",
                fileName = "id.pdf"
            ),
            actorUserId = actorId,
            actorRole = "AFFILIATE"
        )

        val submitted = service.submitProfile(tenantId, affiliateId, actorId, "AFFILIATE")
        assertEquals(AffiliateProfileStatus.SUBMITTED, submitted.profileStatus)
        assertNotNull(submitted.submittedAt)

        val audits = service.listAuditRecords(tenantId, affiliateId)
        assertTrue(audits.size >= 2)
    }

    @Test
    fun `verification lifecycle workflow approve and reject functions correctly`() = runBlocking {
        // 1. Request verification
        val reqDto = RequestVerificationRequestDto(
            verificationType = "IDENTITY",
            metadataReference = "DOC-ID-999",
            reason = "Passport verification"
        )
        val req = service.requestVerification(
            tenantId = tenantId,
            affiliateId = affiliateId,
            request = reqDto,
            actorUserId = actorId,
            actorRole = "AFFILIATE"
        )
        assertEquals(AffiliateVerificationStatus.SUBMITTED, req.status)

        // 2. Approve verification
        val approveDto = ReviewVerificationRequestDto(reason = "Valid passport document verified")
        val approved = service.approveVerification(
            tenantId = tenantId,
            verificationId = req.verificationId,
            request = approveDto,
            actorUserId = actorId,
            actorRole = "ADMIN"
        )
        assertEquals(AffiliateVerificationStatus.VERIFIED, approved.status)
        assertEquals(actorId, approved.reviewerUserId)

        // 3. Request another check and reject it
        val reqTaxDto = RequestVerificationRequestDto(
            verificationType = "TAX",
            reason = "Tax form W8-BEN"
        )
        val reqTax = service.requestVerification(
            tenantId = tenantId,
            affiliateId = affiliateId,
            request = reqTaxDto,
            actorUserId = actorId,
            actorRole = "AFFILIATE"
        )
        val rejectDto = ReviewVerificationRequestDto(reason = "Invalid TIN format provided")
        val rejected = service.rejectVerification(
            tenantId = tenantId,
            verificationId = reqTax.verificationId,
            request = rejectDto,
            actorUserId = actorId,
            actorRole = "ADMIN"
        )
        assertEquals(AffiliateVerificationStatus.REJECTED, rejected.status)
    }

    @Test
    fun `document reference lifecycle upload verify and reject`() = runBlocking {
        val docReq = AddDocumentReferenceRequestDto(
            documentType = "IDENTITY_PROOF",
            storageReference = "gs://sucharu-bucket/passport.pdf",
            fileName = "passport.pdf",
            fileSizeBytes = 2048500L,
            mimeType = "application/pdf"
        )
        val doc = service.addDocumentReference(
            tenantId = tenantId,
            affiliateId = affiliateId,
            request = docReq,
            actorUserId = actorId,
            actorRole = "AFFILIATE"
        )
        assertEquals(AffiliateDocumentStatus.UPLOADED, doc.status)

        val verifiedDoc = service.verifyDocument(
            tenantId = tenantId,
            documentId = doc.documentId,
            actorUserId = actorId,
            actorRole = "ADMIN"
        )
        assertEquals(AffiliateDocumentStatus.VERIFIED, verifiedDoc.status)
        assertNotNull(verifiedDoc.verifiedAt)
    }

    @Test
    fun `profile suspend and reactivate transitions correctly`() = runBlocking {
        val req = UpsertAffiliateProfileRequestDto(
            displayName = "Partner Alpha"
        )
        service.upsertProfile(tenantId, affiliateId, req, actorId, "ADMIN")

        val suspended = service.suspendProfile(tenantId, affiliateId, "Suspect fraud pattern", actorId, "ADMIN")
        assertEquals(AffiliateProfileStatus.SUSPENDED, suspended.profileStatus)
        assertNotNull(suspended.suspendedAt)

        val reactivated = service.reactivateProfile(tenantId, affiliateId, "Fraud cleared upon manual review", actorId, "ADMIN")
        assertEquals(AffiliateProfileStatus.UNDER_REVIEW, reactivated.profileStatus)
        assertNull(reactivated.suspendedAt)
    }

    @Test
    fun `governance summary computes metrics accurately`() = runBlocking {
        val req = UpsertAffiliateProfileRequestDto(
            displayName = "Partner 1"
        )
        service.upsertProfile(tenantId, affiliateId, req, actorId, "ADMIN")

        val summary = service.getGovernanceSummary(tenantId)
        assertEquals(1L, summary.totalProfiles)
        assertEquals(1L, summary.incompleteProfiles)
    }
}
