package com.sucharu.sucharupro.vendorportal

import com.sucharu.sucharupro.data.api.model.*
import org.junit.Assert.*
import org.junit.Test

class VendorPortalCollaborationUiTest {

    @Test
    fun testDtoMappingsForPoSummaryAndDetails() {
        val summary = VendorPortalPurchaseOrderSummaryDto(
            purchaseOrderId = "po-1",
            orderNumber = "PO-2026-001",
            vendorId = "v-1",
            status = "ISSUED",
            orderDate = 1740000000L,
            expectedDeliveryDate = 1750000000L,
            deliveryLocation = "Dhaka Central Hub",
            currency = "BDT",
            totalAmount = 15000.0,
            acknowledgementStatus = "ACKNOWLEDGED",
            acknowledgedAt = 1740000000L,
            activeWorkOrdersCount = 1,
            openBlockersCount = 0
        )
        assertEquals("PO-2026-001", summary.orderNumber)
        assertEquals("ISSUED", summary.status)
        assertEquals(0, summary.openBlockersCount)
    }

    @Test
    fun testDtoMappingsForWoSummaryAndDetails() {
        val summary = VendorPortalWorkOrderSummaryDto(
            workOrderId = "wo-1",
            workOrderNumber = "WO-2026-001",
            purchaseOrderId = "po-1",
            title = "Shirt Stitching Batch 1",
            capabilityType = "STITCHING",
            quantity = 500.0,
            unitOfMeasure = "PCS",
            status = "IN_PROGRESS",
            priority = "MEDIUM",
            scheduledStartAt = 1740000000L,
            scheduledDueAt = 1750000000L,
            estimatedAmount = 25000.0,
            currency = "BDT",
            acknowledgementStatus = "ACKNOWLEDGED",
            latestProgressPercentage = 60.0,
            completionStatus = "NOT_REQUESTED",
            openBlockersCount = 1
        )
        assertEquals("WO-2026-001", summary.workOrderNumber)
        assertEquals(60.0, summary.latestProgressPercentage!!, 0.001)
        assertEquals(1, summary.openBlockersCount)
    }

    @Test
    fun testDtoMappingsForProgressBlockerThreadAndEvidence() {
        val progressDto = VendorProgressUpdateDto(
            progressUpdateId = "up-1",
            tenantId = "t-1",
            projectId = "p-1",
            vendorId = "v-1",
            workOrderId = "wo-1",
            progressPercentage = 50.0,
            completedQuantity = 50.0,
            remainingQuantity = 50.0,
            authorizedQuantity = 100.0,
            statusSummary = "50% cut",
            notes = "On track",
            expectedCompletionDate = 1750000000L,
            blockerReferenceId = null,
            submittedBy = "vendor-rep",
            submittedAt = 1740000000L,
            version = 1L
        )
        assertEquals(50.0, progressDto.progressPercentage!!, 0.001)

        val blockerDto = VendorBlockerDto(
            blockerId = "blk-1",
            tenantId = "t-1",
            projectId = "p-1",
            vendorId = "v-1",
            workOrderId = "wo-1",
            purchaseOrderId = "po-1",
            category = "MATERIAL_UNAVAILABLE",
            severity = "HIGH",
            status = "OPEN",
            title = "Missing thread",
            description = "Need thread",
            resolutionNotes = null,
            reportedBy = "vendor-rep",
            reportedAt = 1740000000L,
            acknowledgedBy = null,
            acknowledgedAt = null,
            resolvedBy = null,
            resolvedAt = null,
            version = 1L
        )
        assertEquals("OPEN", blockerDto.status)
        assertEquals("HIGH", blockerDto.severity)

        val threadDto = VendorCollaborationThreadDto(
            threadId = "th-1",
            tenantId = "t-1",
            projectId = "p-1",
            vendorId = "v-1",
            resourceType = "WORK_ORDER",
            resourceId = "wo-1",
            title = "Discussion",
            createdBy = "vendor-rep",
            createdAt = 1740000000L,
            isClosed = false,
            version = 1L
        )
        assertEquals("WORK_ORDER", threadDto.resourceType)

        val evidenceDto = VendorCollaborationEvidenceDto(
            evidenceId = "ev-1",
            tenantId = "t-1",
            projectId = "p-1",
            vendorId = "v-1",
            resourceType = "WORK_ORDER",
            resourceId = "wo-1",
            fileReference = "gcs://evidence/photo.jpg",
            filename = "photo.jpg",
            mimeType = "image/jpeg",
            sizeBytes = 2048L,
            checksum = "sha256abc",
            description = "Sample photo",
            visibility = "VENDOR_VISIBLE",
            uploadedBy = "vendor-rep",
            uploadedAt = 1740000000L
        )
        assertEquals("photo.jpg", evidenceDto.filename)
        assertEquals("VENDOR_VISIBLE", evidenceDto.visibility)
    }
}
