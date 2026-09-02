package com.sucharu.sucharupro.domain.model.order

import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.common.toMoney
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderDomainTest {

    // ========================================================================
    // A. Inquiry Tests
    // ========================================================================

    @Test
    fun test01_validInquiry_instantiatesSuccessfully() {
        val requirement = InquiryRequirement(
            itemId = "inq-item-01",
            productName = "Visiting Card",
            description = "300 GSM Art Card, Matte Lamination, 4 Color Print",
            quantity = 1000,
            unit = "Pcs",
            size = "3.25 x 2.0 inch",
            paperMaterial = "Art Card",
            gsm = 300,
            colorSpecification = "4 Color (CMYK)",
            printingMethod = "Offset",
            finishing = "Matte Lamination",
            isDesignRequired = false
        )

        val inquiry = Inquiry(
            inquiryId = "inq-001",
            inquiryNumber = "INQ-000001",
            customerId = "cus-001",
            status = InquiryStatusType.NEW,
            source = InquirySource.DIRECT_VISIT,
            items = listOf(requirement),
            contactPhone = "+880 1711-234567",
            contactPerson = "Md. Abdullah Rahman",
            notes = "Customer needs urgent sample check",
            createdAt = "2026-08-15T10:00:00Z",
            updatedAt = "2026-08-15T10:00:00Z"
        )

        assertEquals("inq-001", inquiry.inquiryId)
        assertEquals("INQ-000001", inquiry.inquiryNumber)
        assertEquals("cus-001", inquiry.customerId)
        assertEquals(InquiryStatusType.NEW, inquiry.status)
        assertEquals(1, inquiry.totalItemsCount)
        assertEquals(1000, inquiry.totalQuantity)
    }

    @Test(expected = IllegalArgumentException::class)
    fun test02_inquiryWithBlankCustomerId_throwsException() {
        Inquiry(
            inquiryId = "inq-001",
            inquiryNumber = "INQ-000001",
            customerId = "   ",
            createdAt = "2026-08-15T10:00:00Z",
            updatedAt = "2026-08-15T10:00:00Z"
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun test03_inquiryRequirementWithZeroQuantity_throwsException() {
        InquiryRequirement(
            itemId = "inq-item-01",
            productName = "Banner",
            description = "PVC Banner with Eyelets",
            quantity = 0
        )
    }

    @Test
    fun test04_inquiryStatus_transitions() {
        assertTrue(InquiryStatusType.NEW.canTransitionTo(InquiryStatusType.IN_PROGRESS))
        assertTrue(InquiryStatusType.NEW.canTransitionTo(InquiryStatusType.QUOTED))
        assertTrue(InquiryStatusType.NEW.canTransitionTo(InquiryStatusType.CANCELLED))
        assertTrue(InquiryStatusType.IN_PROGRESS.canTransitionTo(InquiryStatusType.QUOTED))
        assertTrue(InquiryStatusType.QUOTED.canTransitionTo(InquiryStatusType.CONVERTED))
        assertFalse(InquiryStatusType.CONVERTED.canTransitionTo(InquiryStatusType.NEW))
    }

    // ========================================================================
    // B. Quotation & Revision Tests
    // ========================================================================

    @Test
    fun test05_quotationItem_pricingArithmetic() {
        val item = QuotationItem(
            itemId = "qt-item-01",
            description = "Custom Book Printing (100 Pages, 80 GSM Offset)",
            quantity = 500,
            unit = "Copies",
            unitPrice = 120.toMoney(),
            discount = 1000.toMoney(),
            notes = "Special author discount applied"
        )

        assertEquals("৳ 60,000", item.lineGrossTotal.formatted())
        assertEquals("৳ 59,000", item.lineSubtotal.formatted()) // (500 * 120) - 1000 = 59000
    }

    @Test
    fun test06_quotationRevision_subtotalAndDiscount() {
        val item1 = QuotationItem(
            itemId = "qt-item-01",
            description = "Visiting Cards (1000 Pcs)",
            quantity = 1,
            unit = "Box",
            unitPrice = 850.toMoney()
        )
        val item2 = QuotationItem(
            itemId = "qt-item-02",
            description = "Letterhead Pad (80 GSM, 100 Leaves x 10 Pads)",
            quantity = 10,
            unit = "Pads",
            unitPrice = 250.toMoney()
        )

        val revision = QuotationRevision(
            revisionId = "rev-001-v1",
            quotationId = "qt-001",
            revisionNumber = 1,
            items = listOf(item1, item2),
            discount = 150.toMoney(),
            paymentTerms = PaymentTerms.DEFAULT,
            deliveryRequirement = DeliveryRequirement.DEFAULT_PICKUP,
            createdAt = "2026-08-15T11:00:00Z"
        )

        // Item 1: 850, Item 2: 2500 -> Subtotal: 3350
        assertEquals("৳ 3,350", revision.subtotal.formatted())
        // Total: 3350 - 150 = 3200
        assertEquals("৳ 3,200", revision.totalAmount.formatted())
        assertEquals(11, revision.totalQuantity)
    }

    @Test
    fun test07_quotationRevisionHistory_immutabilityAndLinking() {
        val rev1 = QuotationRevision(
            revisionId = "rev-001-v1",
            quotationId = "qt-001",
            revisionNumber = 1,
            items = listOf(
                QuotationItem(
                    itemId = "it-1",
                    description = "Brochure Print (200 GSM Art Paper)",
                    quantity = 1000,
                    unitPrice = 15.toMoney()
                )
            ),
            revisionReason = "Initial commercial offer",
            createdAt = "2026-08-15T11:00:00Z"
        )

        val rev2 = QuotationRevision(
            revisionId = "rev-001-v2",
            quotationId = "qt-001",
            revisionNumber = 2,
            items = listOf(
                QuotationItem(
                    itemId = "it-1",
                    description = "Brochure Print (170 GSM Art Paper - Negotiated)",
                    quantity = 1000,
                    unitPrice = 12.5.toMoney()
                )
            ),
            revisionReason = "Customer requested paper downgrade to 170 GSM for cost saving",
            createdAt = "2026-08-15T14:00:00Z",
            previousRevisionId = "rev-001-v1"
        )

        val quotation = Quotation(
            quotationId = "qt-001",
            quotationNumber = "QT-000001",
            customerId = "cus-002",
            inquiryId = "inq-001",
            currentRevisionNumber = 2,
            revisions = listOf(rev1, rev2),
            status = QuotationStatusType.APPROVED,
            approvedAt = "2026-08-15T15:00:00Z",
            approvedBy = "Tanvir Ahmed",
            approvedRevisionId = "rev-001-v2",
            createdAt = "2026-08-15T11:00:00Z",
            updatedAt = "2026-08-15T15:00:00Z"
        )

        assertEquals(2, quotation.revisionCount)
        assertEquals("rev-001-v2", quotation.currentRevision?.revisionId)
        assertEquals("৳ 12,500", quotation.totalAmount.formatted())
        assertTrue(quotation.isApproved)
    }

    @Test
    fun test08_quotationStatus_transitions() {
        assertTrue(QuotationStatusType.DRAFT.canTransitionTo(QuotationStatusType.SENT))
        assertTrue(QuotationStatusType.SENT.canTransitionTo(QuotationStatusType.NEGOTIATION))
        assertTrue(QuotationStatusType.SENT.canTransitionTo(QuotationStatusType.APPROVED))
        assertTrue(QuotationStatusType.SENT.canTransitionTo(QuotationStatusType.REJECTED))
        assertTrue(QuotationStatusType.SENT.canTransitionTo(QuotationStatusType.EXPIRED))
        assertTrue(QuotationStatusType.NEGOTIATION.canTransitionTo(QuotationStatusType.APPROVED))
        assertFalse(QuotationStatusType.APPROVED.canTransitionTo(QuotationStatusType.DRAFT))
    }

    // ========================================================================
    // C. Order & Snapshot Tests
    // ========================================================================

    @Test
    fun test09_orderSnapshotFromApprovedQuotation_success() {
        val revApproved = QuotationRevision(
            revisionId = "rev-002-v1",
            quotationId = "qt-002",
            revisionNumber = 1,
            items = listOf(
                QuotationItem(
                    itemId = "item-01",
                    description = "Packaging Box (Duplex Board 350 GSM)",
                    quantity = 2000,
                    unitPrice = 35.toMoney()
                )
            ),
            discount = 2000.toMoney(),
            paymentTerms = PaymentTerms(
                type = PaymentTermType.PARTIAL_ADVANCE,
                advancePercentage = 50
            ),
            deliveryRequirement = DeliveryRequirement(
                deliveryType = DeliveryType.BUSINESS_DELIVERY,
                address = "Tejgaon I/A, Dhaka",
                contactPhone = "+880 1819-000111"
            ),
            createdAt = "2026-08-15T12:00:00Z"
        )

        val quotation = Quotation(
            quotationId = "qt-002",
            quotationNumber = "QT-000002",
            customerId = "cus-003",
            currentRevisionNumber = 1,
            revisions = listOf(revApproved),
            status = QuotationStatusType.APPROVED,
            approvedAt = "2026-08-15T14:00:00Z",
            approvedBy = "Manager",
            approvedRevisionId = "rev-002-v1",
            createdAt = "2026-08-15T12:00:00Z",
            updatedAt = "2026-08-15T14:00:00Z"
        )

        val order = Order.fromApprovedQuotation(
            orderId = "ord-001",
            orderNumber = "ORD-000001",
            quotation = quotation,
            revision = revApproved,
            priority = OrderPriority.HIGH,
            confirmedBy = "Sales Desk",
            timestamp = "2026-08-15T14:30:00Z"
        )

        assertEquals("ord-001", order.orderId)
        assertEquals("ORD-000001", order.orderNumber)
        assertEquals("cus-003", order.customerId)
        assertEquals("qt-002", order.quotationId)
        assertEquals("rev-002-v1", order.approvedQuotationRevisionId)
        assertEquals(OrderStatusType.CONFIRMED, order.status)
        assertEquals(OrderPriority.HIGH, order.priority)
        assertEquals(1, order.items.size)
        // Gross: 70000, Discount: 2000 -> Total: 68000
        assertEquals("৳ 70,000", order.subtotal.formatted())
        assertEquals("৳ 68,000", order.totalAmount.formatted())
        assertEquals(JobHandoffStatus.READY_FOR_JOB, order.jobHandoffStatus)
        assertEquals(PaymentTermType.PARTIAL_ADVANCE, order.paymentTerms.type)
        assertEquals(50, order.paymentTerms.advancePercentage)
    }

    @Test(expected = IllegalArgumentException::class)
    fun test10_orderFromUnapprovedQuotation_throwsException() {
        val draftRevision = QuotationRevision(
            revisionId = "rev-003-v1",
            quotationId = "qt-003",
            revisionNumber = 1,
            items = listOf(
                QuotationItem(
                    itemId = "item-01",
                    description = "Poster",
                    quantity = 100,
                    unitPrice = 50.toMoney()
                )
            ),
            createdAt = "2026-08-15T10:00:00Z"
        )

        val quotation = Quotation(
            quotationId = "qt-003",
            quotationNumber = "QT-000003",
            customerId = "cus-001",
            currentRevisionNumber = 1,
            revisions = listOf(draftRevision),
            status = QuotationStatusType.DRAFT, // Not approved!
            createdAt = "2026-08-15T10:00:00Z",
            updatedAt = "2026-08-15T10:00:00Z"
        )

        Order.fromApprovedQuotation(
            orderId = "ord-002",
            orderNumber = "ORD-000002",
            quotation = quotation,
            revision = draftRevision,
            timestamp = "2026-08-15T10:30:00Z"
        )
    }

    @Test
    fun test11_orderStatus_transitions() {
        assertTrue(OrderStatusType.PENDING.canTransitionTo(OrderStatusType.CONFIRMED))
        assertTrue(OrderStatusType.CONFIRMED.canTransitionTo(OrderStatusType.IN_PRODUCTION))
        assertTrue(OrderStatusType.IN_PRODUCTION.canTransitionTo(OrderStatusType.READY))
        assertTrue(OrderStatusType.READY.canTransitionTo(OrderStatusType.DELIVERED))
        assertTrue(OrderStatusType.CONFIRMED.canTransitionTo(OrderStatusType.ON_HOLD))
        assertTrue(OrderStatusType.ON_HOLD.canTransitionTo(OrderStatusType.CONFIRMED))
        assertTrue(OrderStatusType.CONFIRMED.canTransitionTo(OrderStatusType.CANCELLED))
        assertFalse(OrderStatusType.DELIVERED.canTransitionTo(OrderStatusType.PENDING))
    }

    // ========================================================================
    // D. Commercial Supporting Types & Validation
    // ========================================================================

    @Test
    fun test12_paymentTerms_validationAndDefaults() {
        val fullAdvance = PaymentTerms.DEFAULT
        assertEquals(PaymentTermType.FULL_ADVANCE, fullAdvance.type)
        assertEquals(100, fullAdvance.advancePercentage)
        assertEquals(0, fullAdvance.dueDays)

        val custom = PaymentTerms(
            type = PaymentTermType.CREDIT,
            advancePercentage = 25,
            dueDays = 30,
            customDescription = "25% Advance, Net 30 days"
        )
        assertEquals(25, custom.advancePercentage)
        assertEquals(30, custom.dueDays)
    }

    @Test(expected = IllegalArgumentException::class)
    fun test13_paymentTermsWithInvalidPercentage_throwsException() {
        PaymentTerms(
            type = PaymentTermType.PARTIAL_ADVANCE,
            advancePercentage = 150 // Invalid > 100
        )
    }

    @Test
    fun test14_deliveryRequirement_types() {
        val pickup = DeliveryRequirement.DEFAULT_PICKUP
        assertEquals(DeliveryType.PICKUP, pickup.deliveryType)

        val courier = DeliveryRequirement(
            deliveryType = DeliveryType.COURIER,
            requiredDate = "2026-08-20",
            address = "Chittagong GPO Branch",
            contactName = "Mr. Karim",
            contactPhone = "01811-223344",
            instructions = "Send via Sundarban Courier"
        )
        assertEquals(DeliveryType.COURIER, courier.deliveryType)
        assertEquals("2026-08-20", courier.requiredDate)
    }

    // ========================================================================
    // E. Boundary Verification Tests
    // ========================================================================

    @Test
    fun test15_orderStatusType_strictCommercialSeparationFromProductionStages() {
        val statuses = OrderStatusType.entries.map { it.name }
        // Verify commercial states exist
        assertTrue(statuses.contains("PENDING"))
        assertTrue(statuses.contains("CONFIRMED"))
        assertTrue(statuses.contains("IN_PRODUCTION"))
        assertTrue(statuses.contains("READY"))
        assertTrue(statuses.contains("DELIVERED"))
        assertTrue(statuses.contains("ON_HOLD"))
        assertTrue(statuses.contains("CANCELLED"))

        // Strictly verify NO production stages exist inside OrderStatusType
        assertFalse(statuses.contains("PRINTING"))
        assertFalse(statuses.contains("LAMINATION"))
        assertFalse(statuses.contains("FOLDING"))
        assertFalse(statuses.contains("BINDING"))
        assertFalse(statuses.contains("QC"))
        assertFalse(statuses.contains("PACKAGING"))
    }
}
