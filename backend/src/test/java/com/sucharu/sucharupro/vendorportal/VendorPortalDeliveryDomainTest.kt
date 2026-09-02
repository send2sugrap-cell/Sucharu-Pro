package com.sucharu.sucharupro.vendorportal

import com.sucharu.sucharupro.domain.model.vendorportal.*
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class VendorPortalDeliveryDomainTest {

    @Test
    fun testDeliveryNoticeCreationAndDefaults() {
        val notice = VendorPortalDeliveryNotice(
            noticeId = "asn-001",
            tenantId = "tenant-1",
            projectId = "proj-1",
            vendorId = "vendor-1",
            purchaseOrderId = "po-101",
            orderNumber = "PO-2026-001",
            noticeNumber = "ASN-001",
            status = VendorPortalDeliveryNoticeStatus.DRAFT,
            plannedDeliveryDate = 1700000000000L,
            items = listOf(
                VendorPortalDeliveryNoticeItem(
                    itemId = "item-1",
                    noticeId = "asn-001",
                    tenantId = "tenant-1",
                    purchaseOrderItemId = "poi-1",
                    itemName = "A4 Paper Reams",
                    orderedQuantity = BigDecimal("100"),
                    previouslyDeliveredQuantity = BigDecimal("20"),
                    deliveryQuantity = BigDecimal("50"),
                    unitOfMeasure = "BOX"
                )
            ),
            createdAt = 1699990000000L,
            createdBy = "user-1"
        )

        assertEquals("asn-001", notice.noticeId)
        assertEquals(VendorPortalDeliveryNoticeStatus.DRAFT, notice.status)
        assertEquals(1, notice.items.size)
        assertEquals(BigDecimal("50"), notice.items[0].deliveryQuantity)
    }

    @Test
    fun testQualitySummaryProjectionQuantities() {
        val qualitySummary = VendorPortalQualityInspectionSummary(
            inspectionId = "insp-001",
            inspectionNumber = "QI-001",
            deliveryReceiptId = "rcpt-001",
            purchaseOrderId = "po-101",
            vendorId = "vendor-1",
            inspectionDate = 1700000000000L,
            status = "COMPLETED",
            overallResult = "FAILED",
            inspectedQuantity = BigDecimal("50"),
            acceptedQuantity = BigDecimal("30"),
            rejectedQuantity = BigDecimal("20"),
            conditionalQuantity = BigDecimal.ZERO,
            rejectionId = "rej-001",
            rejectionReason = "Dimensions out of tolerance",
            replacementRequired = true
        )

        assertEquals(BigDecimal("50"), qualitySummary.inspectedQuantity)
        assertEquals(BigDecimal("20"), qualitySummary.rejectedQuantity)
        assertTrue(qualitySummary.replacementRequired)
    }

    @Test
    fun testDeliveryExceptionCreation() {
        val ex = VendorPortalDeliveryException(
            exceptionId = "exc-001",
            tenantId = "tenant-1",
            projectId = "proj-1",
            vendorId = "vendor-1",
            sourceType = "DELIVERY_NOTICE",
            sourceId = "asn-001",
            exceptionType = VendorPortalDeliveryExceptionType.DELIVERY_DELAY,
            severity = VendorPortalDeliveryExceptionSeverity.HIGH,
            status = VendorPortalDeliveryExceptionStatus.OPEN,
            title = "Shipment Delayed",
            description = "Carrier breakdown on highway",
            createdAt = 1700000000000L,
            createdBy = "user-1"
        )

        assertEquals(VendorPortalDeliveryExceptionStatus.OPEN, ex.status)
        assertEquals(VendorPortalDeliveryExceptionSeverity.HIGH, ex.severity)
    }
}
