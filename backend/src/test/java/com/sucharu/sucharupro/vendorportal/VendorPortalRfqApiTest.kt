package com.sucharu.sucharupro.vendorportal

import com.sucharu.sucharupro.data.api.model.*
import com.sucharu.sucharupro.data.api.server.BackendUseCases
import com.sucharu.sucharupro.data.datasource.FakeVendorDataSource
import com.sucharu.sucharupro.data.datasource.FakeVendorQuotationDataSource
import com.sucharu.sucharupro.data.datasource.FakeVendorRfqDataSource
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.data.persistence.postgres.TransactionContext
import com.sucharu.sucharupro.data.persistence.postgres.TransactionManager
import com.sucharu.sucharupro.data.repository.VendorQuotationRepositoryImpl
import com.sucharu.sucharupro.data.repository.VendorRepositoryImpl
import com.sucharu.sucharupro.data.repository.VendorRfqRepositoryImpl
import com.sucharu.sucharupro.domain.model.vendor.Vendor
import com.sucharu.sucharupro.domain.model.vendor.VendorCategory
import com.sucharu.sucharupro.domain.model.vendor.VendorStatus
import com.sucharu.sucharupro.domain.service.vendorportal.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class VendorPortalRfqApiTest {

    private lateinit var rfqRepo: VendorRfqRepositoryImpl
    private lateinit var quoteRepo: VendorQuotationRepositoryImpl
    private lateinit var vendorRepo: VendorRepositoryImpl
    private lateinit var rfqService: VendorRfqService
    private lateinit var quoteService: VendorQuotationService
    private lateinit var evalService: VendorRfqEvaluationService
    private lateinit var useCases: BackendUseCases

    @Before
    fun setup() {
        runBlocking {
            rfqRepo = VendorRfqRepositoryImpl(FakeVendorRfqDataSource())
            quoteRepo = VendorQuotationRepositoryImpl(FakeVendorQuotationDataSource())
            vendorRepo = VendorRepositoryImpl(FakeVendorDataSource())

            vendorRepo.createVendor(
                Vendor(
                    vendorId = "vnd-api-1",
                    projectId = "proj-1",
                    vendorCode = "VND-API",
                    vendorName = "API Matrix Corp",
                    vendorCategory = VendorCategory.PAPER_SUPPLIER,
                    status = VendorStatus.ACTIVE
                )
            )

            rfqService = VendorRfqServiceImpl(rfqRepo, vendorRepo)
            quoteService = VendorQuotationServiceImpl(quoteRepo, rfqRepo)
            evalService = VendorRfqEvaluationServiceImpl(rfqRepo, quoteRepo, vendorRepo)

            val fakeTxManager = object : TransactionManager {
                override suspend fun <T> inTransaction(tenantContext: TenantContext, block: suspend (TransactionContext) -> T): T {
                    throw UnsupportedOperationException("Not required for mock tests")
                }
                override suspend fun <T> inReadOnly(tenantContext: TenantContext, block: suspend (TransactionContext) -> T): T {
                    throw UnsupportedOperationException("Not required for mock tests")
                }
            }

            val customRepoFactory = object : PostgresRepositoryFactory(fakeTxManager) {
                override fun createVendorRfqService(tenantId: String): VendorRfqService = rfqService
                override fun createVendorQuotationService(tenantId: String): VendorQuotationService = quoteService
                override fun createVendorRfqEvaluationService(tenantId: String): VendorRfqEvaluationService = evalService
            }

            useCases = BackendUseCases(fakeTxManager, customRepoFactory)
        }
    }

    @Test
    fun testRfqCreationAndRetrievalViaUseCases() = runBlocking {
        val adminPrincipal = AuthenticatedPrincipal(
            userId = "admin-1",
            projectId = "proj-1",
            username = "admin",
            role = UserRole.ADMIN,
            permissions = setOf(UserPermission.CREATE_VENDOR_RFQ, UserPermission.READ_VENDOR_RFQ)
        )

        val created = useCases.createVendorRfq(
            adminPrincipal,
            CreateVendorRfqRequestDto(
                rfqNumber = "RFQ-API-001",
                title = "Box Manufacturing",
                responseDeadline = System.currentTimeMillis() + 86400000L,
                items = listOf(
                    CreateVendorRfqItemRequestDto(
                        sequenceNumber = 1,
                        description = "Rigid Box",
                        quantity = 500.0
                    )
                )
            )
        )

        assertNotNull(created)
        assertEquals("RFQ-API-001", created.rfqNumber)
        assertEquals(1, created.items.size)

        val retrieved = useCases.getVendorRfqById(adminPrincipal, created.rfqId)
        assertNotNull(retrieved)
        assertEquals("Box Manufacturing", retrieved.title)
    }
}
