package com.sucharu.sucharupro.domain.validation.productionplanning

import com.sucharu.sucharupro.domain.model.commercialcommitment.CommercialCommitment
import com.sucharu.sucharupro.domain.model.commercialcommitment.CommitmentStatus
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.order.Order
import com.sucharu.sucharupro.domain.model.order.OrderItem
import com.sucharu.sucharupro.domain.model.order.OrderStatusType
import com.sucharu.sucharupro.domain.model.productionplanning.ProductionJobSpecification
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class ProductionPlanningValidatorTest {

    private fun createTestOrder(status: OrderStatusType = OrderStatusType.CONFIRMED) = Order(
        orderId = "ORD-001",
        orderNumber = "ORD-2026-001",
        customerId = "CUST-001",
        status = status,
        items = listOf(
            OrderItem(
                itemId = "ITEM-001",
                description = "Art Paper Magazine",
                quantity = 1000,
                unitPrice = Money(50.0)
            )
        ),
        createdAt = "2026-09-01T12:00:00Z",
        updatedAt = "2026-09-01T12:00:00Z"
    )

    private fun createTestSpec(
        finishedWidth: BigDecimal = BigDecimal("210.0000"),
        finishedHeight: BigDecimal = BigDecimal("297.0000"),
        gsm: Int = 150,
        substrateType: String = "ART_PAPER"
    ) = ProductionJobSpecification(
        specId = "SPEC-001",
        jobTitle = "Art Paper Magazine",
        productType = "PRINT_COMMERCIAL",
        orderedQuantity = 1000L,
        plannedQuantity = 1100L,
        finishedWidthMm = finishedWidth,
        finishedHeightMm = finishedHeight,
        substrateType = substrateType,
        substrateGsm = gsm,
        parentSheetWidthMm = BigDecimal("640.0000"),
        parentSheetHeightMm = BigDecimal("900.0000"),
        pressSheetWidthMm = BigDecimal("640.0000"),
        pressSheetHeightMm = BigDecimal("450.0000"),
        printingMethod = "OFFSET",
        colorsFront = 4,
        colorsBack = 4,
        impositionUps = 4,
        specFingerprint = "FINGERPRINT-001"
    )

    @Test
    fun `valid order and item pass validation without blocking diagnostics`() {
        val order = createTestOrder()
        val item = order.items[0]
        val commitment = CommercialCommitment(
            commitmentId = "COMM-001",
            tenantId = "tenant_test",
            projectId = "tenant_test",
            quotationId = "QUO-001",
            quotationVersion = 1,
            customerId = "CUST-001",
            orderId = "ORD-001",
            orderNumber = "ORD-2026-001",
            status = CommitmentStatus.CONVERTED,
            committedQuantity = 1000L,
            approvedUnitPrice = BigDecimal("50.0000"),
            approvedSubtotal = BigDecimal("50000.0000"),
            approvedGrandTotal = BigDecimal("50000.0000"),
            integrityHash = "HASH",
            createdAt = System.currentTimeMillis(),
            createdBy = "user"
        )

        val diagnostics = ProductionPlanningValidator.validateOrderAndItem("tenant_test", order, item, commitment)
        assertTrue(diagnostics.none { it.isBlocking })
    }

    @Test
    fun `cancelled order yields critical blocking diagnostic`() {
        val order = createTestOrder(status = OrderStatusType.CANCELLED)
        val item = order.items[0]
        val diagnostics = ProductionPlanningValidator.validateOrderAndItem("tenant_test", order, item, null)
        assertTrue(diagnostics.any { it.isBlocking && it.code == "ORDER_CANCELLED" })
    }

    @Test
    fun `missing dimensions or missing substrate produces blocking diagnostic`() {
        val invalidSpec = createTestSpec(
            finishedWidth = BigDecimal.ZERO,
            gsm = 0,
            substrateType = ""
        )
        val diagnostics = ProductionPlanningValidator.validateJobSpecification(invalidSpec)
        assertTrue(diagnostics.any { it.isBlocking && it.code == "MISSING_DIMENSIONS" })
        assertTrue(diagnostics.any { it.isBlocking && it.code == "MISSING_SUBSTRATE" })
    }
}
