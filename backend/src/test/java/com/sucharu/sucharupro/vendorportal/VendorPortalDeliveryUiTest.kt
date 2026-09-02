package com.sucharu.sucharupro.vendorportal

import com.sucharu.sucharupro.data.api.model.*
import org.junit.Assert.*
import org.junit.Test

class VendorPortalDeliveryUiTest {

    @Test
    fun testDeliveryNoticeDtoMappings() {
        val noticeDto = VendorPortalDeliveryNoticeDto(
            noticeId = "asn-1",
            tenantId = "tenant-1",
            projectId = "proj-1",
            vendorId = "vnd-1",
            purchaseOrderId = "po-1",
            orderNumber = "PO-2026-001",
            noticeNumber = "ASN-2026-001",
            status = "DRAFT",
            plannedDeliveryDate = 1750000000L,
            carrierName = "DHL Freight",
            trackingNumber = "DHL-12345",
            vehicleNumber = "DM-11-2233",
            driverName = "Rahim",
            driverPhone = "+8801700000000",
            vendorNotes = "Fragile glass items inside",
            items = listOf(
                VendorPortalDeliveryNoticeItemDto(
                    itemId = "item-1",
                    noticeId = "asn-1",
                    purchaseOrderItemId = "poi-1",
                    itemName = "Glass Bottles 500ml",
                    itemCode = "GB-500",
                    orderedQuantity = 1000.0,
                    previouslyDeliveredQuantity = 200.0,
                    deliveryQuantity = 500.0,
                    unitOfMeasure = "PCS",
                    lotNumber = "LOT-2026-01",
                    packageCount = 50,
                    remarks = "Box 1-50"
                )
            ),
            createdAt = 1740000000L,
            createdBy = "vuser-1",
            updatedAt = 1740000000L,
            updatedBy = "vuser-1",
            submittedAt = null,
            submittedBy = null,
            cancelledAt = null,
            cancelledBy = null,
            cancellationReason = null,
            version = 1L
        )

        assertEquals("ASN-2026-001", noticeDto.noticeNumber)
        assertEquals("DRAFT", noticeDto.status)
        assertEquals(1, noticeDto.items.size)
        assertEquals(500.0, noticeDto.items[0].deliveryQuantity, 0.001)
        assertEquals("DHL Freight", noticeDto.carrierName)
    }

    @Test
    fun testReceivingSummaryDtoMappings() {
        val receivingDto = VendorPortalReceivingSummaryDto(
            purchaseOrderId = "po-1",
            orderNumber = "PO-2026-001",
            vendorId = "vnd-1",
            status = "PARTIALLY_DELIVERED",
            totalOrderedQuantity = 1000.0,
            totalNotifiedQuantity = 1000.0,
            totalReceivedQuantity = 500.0,
            totalAcceptedQuantity = 480.0,
            totalRejectedQuantity = 20.0,
            totalConditionalQuantity = 0.0,
            totalRemainingQuantity = 520.0,
            receiptCount = 1,
            latestReceiptDate = 1740000000L,
            items = listOf(
                VendorPortalReceivingItemSummaryDto(
                    purchaseOrderItemId = "poi-1",
                    itemName = "Glass Bottles 500ml",
                    orderedQuantity = 1000.0,
                    notifiedQuantity = 1000.0,
                    receivedQuantity = 500.0,
                    acceptedQuantity = 480.0,
                    rejectedQuantity = 20.0,
                    conditionalQuantity = 0.0,
                    remainingQuantity = 520.0,
                    unitOfMeasure = "PCS"
                )
            )
        )

        assertEquals(1000.0, receivingDto.totalOrderedQuantity, 0.001)
        assertEquals(480.0, receivingDto.totalAcceptedQuantity, 0.001)
        assertEquals(20.0, receivingDto.totalRejectedQuantity, 0.001)
        assertEquals(520.0, receivingDto.totalRemainingQuantity, 0.001)
    }

    @Test
    fun testQualitySummaryAndResponseDtoMappings() {
        val qualityDto = VendorPortalQualityInspectionSummaryDto(
            inspectionId = "insp-1",
            inspectionNumber = "QI-2026-001",
            deliveryReceiptId = "rcpt-1",
            purchaseOrderId = "po-1",
            vendorId = "vnd-1",
            inspectionDate = 1740000000L,
            status = "COMPLETED",
            overallResult = "FAILED",
            inspectedQuantity = 500.0,
            acceptedQuantity = 480.0,
            rejectedQuantity = 20.0,
            conditionalQuantity = 0.0,
            rejectionId = "rej-1",
            rejectionReason = "20 bottles cracked in transit",
            disposition = "SCRAP",
            replacementRequired = true,
            creditRequired = false,
            correctiveActionRequired = true,
            disputeId = null,
            disputeStatus = null,
            items = listOf(
                VendorPortalQualityItemSummaryDto(
                    inspectionItemId = "qi-item-1",
                    purchaseOrderItemId = "poi-1",
                    itemName = "Glass Bottles 500ml",
                    inspectedQuantity = 500.0,
                    acceptedQuantity = 480.0,
                    rejectedQuantity = 20.0,
                    conditionalQuantity = 0.0,
                    defectCount = 1,
                    remarks = "Cracked bottles separated"
                )
            ),
            defects = listOf(
                VendorPortalDefectSummaryDto(
                    defectId = "def-1",
                    defectCode = "DEF-CRACK",
                    defectCategory = "STRUCTURAL",
                    severity = "CRITICAL",
                    affectedQuantity = 20.0,
                    description = "Cracked body"
                )
            )
        )

        assertEquals("QI-2026-001", qualityDto.inspectionNumber)
        assertEquals("FAILED", qualityDto.overallResult)
        assertTrue(qualityDto.replacementRequired)
        assertEquals(1, qualityDto.defects.size)

        val responseDto = VendorPortalQualityResponseDto(
            responseId = "resp-1",
            tenantId = "tenant-1",
            projectId = "proj-1",
            vendorId = "vnd-1",
            inspectionId = "insp-1",
            rejectionId = "rej-1",
            responseType = "COMMIT_REPLACEMENT",
            comment = "Replacement batch dispatched next Monday",
            correctiveActionPlan = "Added bubble cushioning",
            promisedReplacementDate = 1745000000L,
            evidenceReferences = listOf("cushion_pack.jpg"),
            respondedBy = "vuser-1",
            respondedAt = 1740500000L,
            version = 1L
        )

        assertEquals("COMMIT_REPLACEMENT", responseDto.responseType)
        assertEquals(1, responseDto.evidenceReferences.size)
    }
}
