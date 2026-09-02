package com.sucharu.sucharupro.vendorportal

import com.sucharu.sucharupro.data.datasource.*
import com.sucharu.sucharupro.data.repository.*
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.*
import com.sucharu.sucharupro.domain.model.vendorportal.VendorPortalFinancialThread
import com.sucharu.sucharupro.domain.service.vendor.*
import com.sucharu.sucharupro.domain.service.vendorportal.VendorPortalSettlementServiceImpl
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class VendorPortalSettlementConcurrencyTest {

    private lateinit var portalRepository: VendorPortalSettlementRepositoryImpl
    private lateinit var service: VendorPortalSettlementServiceImpl

    private val tenantId = "TENANT-001"
    private val projectId = "PRJ-001"
    private val vendorId = "VND-CONCUR-01"

    @Before
    fun setup() {
        val portalDataSource = FakeVendorPortalSettlementDataSource()
        portalRepository = VendorPortalSettlementRepositoryImpl(portalDataSource)

        val vendorDs = FakeVendorDataSource()
        val vendorRepo = VendorRepositoryImpl(vendorDs)
        val invoiceDs = FakeVendorInvoiceDataSource()
        val invoiceRepo = VendorInvoiceRepositoryImpl(invoiceDs)
        val poDs = FakeVendorPurchaseOrderDataSource()
        val poRepo = VendorPurchaseOrderRepositoryImpl(poDs)
        val receiptDs = FakeVendorDeliveryReceiptDataSource()
        val receiptRepo = VendorDeliveryReceiptRepositoryImpl(receiptDs)
        val qualityDs = FakeVendorQualityDataSource()
        val qualityRepo = VendorQualityRepositoryImpl(qualityDs)
        val perfDs = FakeVendorPerformanceDataSource()
        val perfRepo = VendorPerformanceRepositoryImpl(perfDs)
        val settlementDs = FakeVendorSettlementDataSource()
        val settlementRepo = VendorSettlementRepositoryImpl(settlementDs)

        val canonicalInvoiceService = VendorInvoiceServiceImpl(vendorRepo, poRepo, receiptRepo, invoiceRepo)
        val analyticsRepo = VendorAnalyticsRepositoryImpl(vendorRepo, poRepo, receiptRepo, invoiceRepo, qualityRepo, perfRepo, settlementRepo)
        val canonicalSettlementService = VendorSettlementServiceImpl(settlementRepo, analyticsRepo, vendorRepo, invoiceRepo)

        runBlocking {
            vendorRepo.createVendor(
                Vendor(
                    vendorId = vendorId,
                    projectId = projectId,
                    vendorCode = "VND-CONCUR",
                    vendorName = "Concurrent Vendor",
                    vendorCategory = VendorCategory.PRINTING,
                    status = VendorStatus.ACTIVE
                )
            )

            portalRepository.saveThread(
                VendorPortalFinancialThread(
                    threadId = "TH-CONCUR-01",
                    tenantId = tenantId,
                    projectId = projectId,
                    vendorId = vendorId,
                    contextType = "SETTLEMENT",
                    contextId = "SETTL-101",
                    subject = "High Volume Messages",
                    createdBy = "system"
                )
            )
        }

        service = VendorPortalSettlementServiceImpl(
            portalRepository = portalRepository,
            canonicalSettlementService = canonicalSettlementService,
            canonicalInvoiceService = canonicalInvoiceService,
            vendorRepository = vendorRepo
        )
    }

    @Test
    fun testConcurrentMessagePosting() = runBlocking {
        val totalMessages = 30

        coroutineScope {
            val jobs = (1..totalMessages).map { i ->
                async {
                    service.postMessage(
                        tenantId = tenantId,
                        projectId = projectId,
                        vendorId = vendorId,
                        threadId = "TH-CONCUR-01",
                        content = "Concurrent message #$i",
                        actorId = "user_$i",
                        actorRole = "VENDOR"
                    )
                }
            }
            jobs.awaitAll()
        }

        val messagesRes = service.listMessages(tenantId, projectId, vendorId, "TH-CONCUR-01")
        assertTrue(messagesRes is DomainResult.Success)
        val messages = (messagesRes as DomainResult.Success).data
        assertEquals(totalMessages, messages.size)

        val threadRes = service.getThreadById(tenantId, projectId, vendorId, "TH-CONCUR-01")
        assertTrue(threadRes is DomainResult.Success)
        assertEquals(totalMessages, (threadRes as DomainResult.Success).data.messageCount)
    }
}
