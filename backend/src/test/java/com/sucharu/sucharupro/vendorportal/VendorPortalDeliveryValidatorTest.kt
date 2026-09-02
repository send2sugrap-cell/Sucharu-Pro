package com.sucharu.sucharupro.vendorportal

import com.sucharu.sucharupro.domain.model.vendorportal.*
import com.sucharu.sucharupro.domain.validation.vendorportal.VendorPortalDeliveryValidator
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class VendorPortalDeliveryValidatorTest {

    @Test
    fun testValidDeliveryNoticePassesValidation() {
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

        VendorPortalDeliveryValidator.validateDeliveryNotice(notice)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testDeliveryNoticeExcessQuantityFailsValidation() {
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
                    previouslyDeliveredQuantity = BigDecimal("80"),
                    deliveryQuantity = BigDecimal("50"), // Remaining is only 20!
                    unitOfMeasure = "BOX"
                )
            ),
            createdAt = 1699990000000L,
            createdBy = "user-1"
        )

        VendorPortalDeliveryValidator.validateDeliveryNotice(notice)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testDeliveryNoticeZeroQuantityFailsValidation() {
        val item = VendorPortalDeliveryNoticeItem(
            itemId = "item-1",
            noticeId = "asn-001",
            tenantId = "tenant-1",
            purchaseOrderItemId = "poi-1",
            itemName = "A4 Paper Reams",
            orderedQuantity = BigDecimal("100"),
            previouslyDeliveredQuantity = BigDecimal("0"),
            deliveryQuantity = BigDecimal("0"),
            unitOfMeasure = "BOX"
        )
        VendorPortalDeliveryValidator.validateDeliveryNoticeItem(item)
    }

    @Test
    fun testValidStatusTransitions() {
        VendorPortalDeliveryValidator.validateNoticeStatusTransition(
            VendorPortalDeliveryNoticeStatus.DRAFT,
            VendorPortalDeliveryNoticeStatus.SUBMITTED
        )
        VendorPortalDeliveryValidator.validateNoticeStatusTransition(
            VendorPortalDeliveryNoticeStatus.SUBMITTED,
            VendorPortalDeliveryNoticeStatus.CANCELLED
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun testInvalidStatusTransitionThrows() {
        VendorPortalDeliveryValidator.validateNoticeStatusTransition(
            VendorPortalDeliveryNoticeStatus.DELIVERED,
            VendorPortalDeliveryNoticeStatus.DRAFT
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun testQualityResponseMissingCAPAFails() {
        val resp = VendorPortalQualityResponse(
            responseId = "resp-1",
            tenantId = "tenant-1",
            projectId = "proj-1",
            vendorId = "vendor-1",
            inspectionId = "insp-1",
            responseType = VendorPortalQualityResponseType.PROPOSE_CORRECTIVE_ACTION,
            comment = "We will fix it",
            correctiveActionPlan = null, // required!
            respondedBy = "user-1",
            respondedAt = 1700000000000L
        )
        VendorPortalDeliveryValidator.validateQualityResponse(resp)
    }
}
