package com.sucharu.sucharupro.vendorportal

import com.sucharu.sucharupro.data.datasource.*
import com.sucharu.sucharupro.data.repository.*
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.vendor.*
import com.sucharu.sucharupro.domain.model.vendorportal.VendorPoAcknowledgementType
import com.sucharu.sucharupro.domain.service.vendor.VendorPurchaseOrderServiceImpl
import com.sucharu.sucharupro.domain.service.vendor.VendorServiceRateServiceImpl
import com.sucharu.sucharupro.domain.service.vendor.VendorWorkOrderServiceImpl
import com.sucharu.sucharupro.domain.service.vendorportal.VendorPortalCollaborationService
import com.sucharu.sucharupro.domain.service.vendorportal.VendorPortalCollaborationServiceImpl
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.util.concurrent.atomic.AtomicInteger

class VendorPortalCollaborationConcurrencyAndIdempotencyTest {

    private lateinit var vendorRepo: VendorRepositoryImpl
    private lateinit var capabilityRepo: VendorCapabilityRepositoryImpl
    private lateinit var rateRepo: VendorServiceRateRepositoryImpl
    private lateinit var poRepo: VendorPurchaseOrderRepositoryImpl
    private lateinit var woRepo: VendorWorkOrderRepositoryImpl
    private lateinit var collabRepo: VendorCollaborationRepositoryImpl
    private lateinit var service: VendorPortalCollaborationService

    private val tenantId = "tenant-concurrent-test"
    private val projectId = "proj-concurrent-test"
    private val vendorId = "vendor-concurrent-1"

    @Before
    fun setup() {
        runBlocking {
            vendorRepo = VendorRepositoryImpl(FakeVendorDataSource())
            capabilityRepo = VendorCapabilityRepositoryImpl(FakeVendorCapabilityDataSource())
            rateRepo = VendorServiceRateRepositoryImpl(FakeVendorServiceRateDataSource())
            poRepo = VendorPurchaseOrderRepositoryImpl(FakeVendorPurchaseOrderDataSource())
            woRepo = VendorWorkOrderRepositoryImpl(FakeVendorWorkOrderDataSource())
            collabRepo = VendorCollaborationRepositoryImpl(FakeVendorCollaborationDataSource())

            vendorRepo.createVendor(
                Vendor(
                    vendorId = vendorId,
                    projectId = projectId,
                    vendorCode = "VND-CONCUR",
                    vendorName = "Concurrent Vendor Corp",
                    vendorCategory = VendorCategory.PRINTING,
                    status = VendorStatus.ACTIVE
                )
            )

            poRepo.createOrder(
                VendorPurchaseOrder(
                    purchaseOrderId = "po-concurrent-1",
                    projectId = projectId,
                    orderNumber = "PO-CONCUR-1",
                    vendorId = vendorId,
                    status = VendorPurchaseOrderStatus.ISSUED,
                    subtotal = Money(BigDecimal("10000")),
                    totalAmount = Money(BigDecimal("10000")),
                    requestedBy = "admin-1"
                )
            )

            poRepo.createOrder(
                VendorPurchaseOrder(
                    purchaseOrderId = "po-idempotent-1",
                    projectId = projectId,
                    orderNumber = "PO-IDEMP-1",
                    vendorId = vendorId,
                    status = VendorPurchaseOrderStatus.ISSUED,
                    subtotal = Money(BigDecimal("10000")),
                    totalAmount = Money(BigDecimal("10000")),
                    requestedBy = "admin-1"
                )
            )

            val rateService = VendorServiceRateServiceImpl(vendorRepo, capabilityRepo, rateRepo)
            val poService = VendorPurchaseOrderServiceImpl(vendorRepo, capabilityRepo, rateService, poRepo)
            val woService = VendorWorkOrderServiceImpl(vendorRepo, capabilityRepo, rateService, woRepo)

            service = VendorPortalCollaborationServiceImpl(
                collaborationRepository = collabRepo,
                vendorPurchaseOrderService = poService,
                vendorWorkOrderService = woService,
                vendorRepository = vendorRepo
            )
        }
    }

    @Test
    fun testConcurrentPoAcknowledgementRequests() = runBlocking {
        val successCount = AtomicInteger(0)
        val coroutinesCount = 10

        coroutineScope {
            val jobs = (1..coroutinesCount).map { i ->
                async(Dispatchers.Default) {
                    val res = service.acknowledgePurchaseOrder(
                        tenantId = tenantId,
                        projectId = projectId,
                        vendorId = vendorId,
                        purchaseOrderId = "po-concurrent-1",
                        ackType = VendorPoAcknowledgementType.ACKNOWLEDGED,
                        comment = "Concurrent call $i",
                        actorId = "vendor-user-$i"
                    )
                    if (res is DomainResult.Success) {
                        successCount.incrementAndGet()
                    }
                }
            }
            jobs.awaitAll()
        }

        assertTrue(successCount.get() >= 1)
    }

    @Test
    fun testIdempotentPoAcknowledgementWithSameInputs() = runBlocking {
        val res1 = service.acknowledgePurchaseOrder(
            tenantId = tenantId,
            projectId = projectId,
            vendorId = vendorId,
            purchaseOrderId = "po-idempotent-1",
            ackType = VendorPoAcknowledgementType.ACKNOWLEDGED,
            comment = "Initial ack",
            actorId = "vendor-user-1"
        )
        assertTrue(res1 is DomainResult.Success)

        val res2 = service.acknowledgePurchaseOrder(
            tenantId = tenantId,
            projectId = projectId,
            vendorId = vendorId,
            purchaseOrderId = "po-idempotent-1",
            ackType = VendorPoAcknowledgementType.ACKNOWLEDGED,
            comment = "Repeated ack",
            actorId = "vendor-user-1"
        )
        assertTrue(res2 is DomainResult.Success)
    }
}
