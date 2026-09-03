package com.sucharu.sucharupro.domain.service.affiliate

import com.sucharu.sucharupro.data.api.model.affiliate.*
import com.sucharu.sucharupro.data.datasource.affiliate.FakeAffiliateDataSource
import com.sucharu.sucharupro.data.datasource.affiliate.FakeAffiliateProfileDataSource
import com.sucharu.sucharupro.data.repository.affiliate.AffiliateProfileRepositoryImpl
import com.sucharu.sucharupro.data.repository.affiliate.AffiliateRepositoryImpl
import com.sucharu.sucharupro.domain.model.affiliate.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.concurrent.Executors

class AffiliateProfileConcurrencyTest {

    private lateinit var fakeAffiliateDataSource: FakeAffiliateDataSource
    private lateinit var fakeProfileDataSource: FakeAffiliateProfileDataSource
    private lateinit var affiliateRepo: AffiliateRepositoryImpl
    private lateinit var profileRepo: AffiliateProfileRepositoryImpl
    private lateinit var service: AffiliateProfileServiceImpl

    private val tenantId = "tenant-concurrent"
    private val affiliateId = "aff-concurrent-001"

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
                    userId = "user-concurrent",
                    displayName = "Concurrent Partner",
                    affiliateCode = "CONCURRENT2026",
                    status = AffiliateStatus.ACTIVE
                )
            )
        }
    }

    @Test
    fun `concurrent profile updates and audit trail append safely without corruption`() = runBlocking {
        val dispatcher = Executors.newFixedThreadPool(4).asCoroutineDispatcher()
        val numThreads = 10

        val jobs = (1..numThreads).map { i ->
            CoroutineScope(dispatcher).async {
                val req = UpsertAffiliateProfileRequestDto(
                    displayName = "Partner Update $i",
                    legalName = "Legal Name $i",
                    contactEmail = "partner$i@concurrent.com"
                )
                service.upsertProfile(tenantId, affiliateId, req, "actor-$i", "ADMIN")
            }
        }
        jobs.awaitAll()

        val finalProfile = service.getProfileByAffiliateId(tenantId, affiliateId)
        assertNotNull(finalProfile)

        val audits = service.listAuditRecords(tenantId, affiliateId)
        assertEquals(numThreads, audits.size)
        audits.forEach { audit ->
            assertNotNull(audit.recordHash)
            assertNotNull(audit.chainHash)
        }
    }
}
