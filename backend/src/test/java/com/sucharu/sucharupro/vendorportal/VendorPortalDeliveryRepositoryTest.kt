package com.sucharu.sucharupro.vendorportal

import com.sucharu.sucharupro.data.datasource.FakeVendorPortalDeliveryDataSource
import com.sucharu.sucharupro.data.repository.VendorPortalDeliveryRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendorportal.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class VendorPortalDeliveryRepositoryTest {

    private lateinit var repository: VendorPortalDeliveryRepositoryImpl
    private val tenantId = "tenant-repo-test"
    private val projectId = "proj-repo-test"
    private val vendorId = "vendor-repo-1"

    @Before
    fun setup() {
        val dataSource = FakeVendorPortalDeliveryDataSource()
        repository = VendorPortalDeliveryRepositoryImpl(dataSource)
    }

    @Test
    fun testSaveAndFindDeliveryNotice() = runBlocking {
        val notice = VendorPortalDeliveryNotice(
            noticeId = "asn-r1",
            tenantId = tenantId,
            projectId = projectId,
            vendorId = vendorId,
            purchaseOrderId = "po-1",
            orderNumber = "PO-1",
            noticeNumber = "ASN-1",
            status = VendorPortalDeliveryNoticeStatus.DRAFT,
            plannedDeliveryDate = 1700000000000L,
            items = listOf(
                VendorPortalDeliveryNoticeItem(
                    itemId = "item-r1",
                    noticeId = "asn-r1",
                    tenantId = tenantId,
                    purchaseOrderItemId = "poi-1",
                    itemName = "Steel Rods",
                    orderedQuantity = BigDecimal("50"),
                    previouslyDeliveredQuantity = BigDecimal.ZERO,
                    deliveryQuantity = BigDecimal("25"),
                    unitOfMeasure = "TON"
                )
            ),
            createdAt = 1699990000000L,
            createdBy = "user-1"
        )

        val saveRes = repository.saveDeliveryNotice(notice)
        assertTrue(saveRes is DomainResult.Success)

        val findRes = repository.findDeliveryNoticeById("asn-r1", tenantId)
        assertTrue(findRes is DomainResult.Success)
        val found = (findRes as DomainResult.Success).data
        assertNotNull(found)
        assertEquals("ASN-1", found?.noticeNumber)
        assertEquals(1, found?.items?.size)
    }

    @Test
    fun testSaveAndListExceptions() = runBlocking {
        val ex = VendorPortalDeliveryException(
            exceptionId = "exc-1",
            tenantId = tenantId,
            projectId = projectId,
            vendorId = vendorId,
            sourceType = "DELIVERY_NOTICE",
            sourceId = "asn-1",
            exceptionType = VendorPortalDeliveryExceptionType.QUANTITY_VARIANCE,
            severity = VendorPortalDeliveryExceptionSeverity.MEDIUM,
            status = VendorPortalDeliveryExceptionStatus.OPEN,
            title = "Discrepancy at gate",
            description = "Physical count 24 vs ASN 25",
            createdAt = 1700000000000L,
            createdBy = "inspector-1"
        )

        repository.saveException(ex)

        val listRes = repository.listExceptions(tenantId, projectId, vendorId, null, null)
        assertTrue(listRes is DomainResult.Success)
        val list = (listRes as DomainResult.Success).data
        assertEquals(1, list.size)
        assertEquals("Discrepancy at gate", list[0].title)
    }
}
