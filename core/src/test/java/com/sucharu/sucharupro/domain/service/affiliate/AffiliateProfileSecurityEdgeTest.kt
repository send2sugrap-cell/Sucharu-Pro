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

class AffiliateProfileSecurityEdgeTest {

    private lateinit var fakeAffiliateDataSource: FakeAffiliateDataSource
    private lateinit var fakeProfileDataSource: FakeAffiliateProfileDataSource
    private lateinit var affiliateRepo: AffiliateRepositoryImpl
    private lateinit var profileRepo: AffiliateProfileRepositoryImpl
    private lateinit var service: AffiliateProfileServiceImpl

    private val tenantA = "tenant-aaa"
    private val tenantB = "tenant-bbb"
    private val affA = "aff-aaa-001"

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
                    tenantId = tenantA,
                    affiliateId = affA,
                    userId = "user-a",
                    displayName = "Affiliate Tenant A",
                    affiliateCode = "ALPHA-A",
                    status = AffiliateStatus.ACTIVE
                )
            )
        }
    }

    @Test
    fun `cross-tenant access to profile is strictly isolated`() {
        runBlocking {
            val reqA = UpsertAffiliateProfileRequestDto(
                displayName = "Tenant A Affiliate Legal",
                legalName = "Alpha Corp",
                contactEmail = "alpha@example.com"
            )
            service.upsertProfile(tenantA, affA, reqA, "actor-a", "ADMIN")

            // Tenant B cannot read Tenant A's profile
            val fromTenantB = service.getProfileByAffiliateId(tenantB, affA)
            assertNull("Tenant B must not retrieve Tenant A's profile", fromTenantB)

            // Tenant B audit list is always empty
            val auditsTenantB = service.listAuditRecords(tenantB, affA)
            assertTrue("Tenant B audit list must be empty", auditsTenantB.isEmpty())
        }

        // getProfileCompleteness must throw because no profile exists for Tenant B
        try {
            runBlocking { service.getProfileCompleteness(tenantB, affA) }
            fail("Expected an exception for cross-tenant completeness check")
        } catch (_: Exception) { /* expected: NoSuchElementException or IllegalStateException */ }
    }

    @Test
    fun `cross-tenant verification review is rejected`() {
        val ver = runBlocking {
            val reqDto = RequestVerificationRequestDto(
                verificationType = "IDENTITY",
                reason = "Passport check"
            )
            service.requestVerification(tenantA, affA, reqDto, "actor-a", "AFFILIATE")
        }

        // Tenant B cannot approve Tenant A's verification record
        try {
            runBlocking {
                service.approveVerification(
                    tenantId = tenantB,
                    verificationId = ver.verificationId,
                    request = ReviewVerificationRequestDto(reason = "Unauthorized cross-tenant approval"),
                    actorUserId = "malicious-admin",
                    actorRole = "ADMIN"
                )
            }
            fail("Expected an exception for cross-tenant verification approval")
        } catch (_: Exception) { /* expected: NoSuchElementException */ }
    }

    @Test
    fun `cross-tenant document verification is rejected`() {
        val doc = runBlocking {
            val docReq = AddDocumentReferenceRequestDto(
                documentType = "BUSINESS_REGISTRATION",
                storageReference = "gs://sucharu/tin_cert.pdf",
                fileName = "tin_cert.pdf"
            )
            service.addDocumentReference(tenantA, affA, docReq, "actor-a", "AFFILIATE")
        }

        // Tenant B cannot verify Tenant A's document
        try {
            runBlocking {
                service.verifyDocument(
                    tenantId = tenantB,
                    documentId = doc.documentId,
                    actorUserId = "malicious-admin",
                    actorRole = "ADMIN"
                )
            }
            fail("Expected an exception for cross-tenant document verification")
        } catch (_: Exception) { /* expected: NoSuchElementException */ }
    }

    @Test
    fun `non-existent affiliate profile upsert fails`() {
        try {
            runBlocking {
                service.upsertProfile(
                    tenantA,
                    "non-existent-affiliate",
                    UpsertAffiliateProfileRequestDto(displayName = "Ghost"),
                    "actor-a",
                    "ADMIN"
                )
            }
            fail("Expected an exception for non-existent affiliate")
        } catch (_: Exception) { /* expected: NoSuchElementException or IllegalStateException */ }
    }
}
