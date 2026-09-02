package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.common.toMoney
import com.sucharu.sucharupro.domain.model.order.DeliveryRequirement
import com.sucharu.sucharupro.domain.model.order.JobHandoffStatus
import com.sucharu.sucharupro.domain.model.order.Order
import com.sucharu.sucharupro.domain.model.order.OrderItem
import com.sucharu.sucharupro.domain.model.order.OrderPriority
import com.sucharu.sucharupro.domain.model.order.OrderStatusType
import com.sucharu.sucharupro.domain.model.order.PaymentTermType
import com.sucharu.sucharupro.domain.model.order.PaymentTerms

/**
 * Isolated Demo presentation dataset for rich UI walkthroughs and demo workspaces.
 * Separated from canonical repository test fixtures to prevent test regression.
 */
object DemoOrderFixtures {

    fun demoOrders(): List<Order> = listOf(
        Order(
            orderId = "ord-demo-001",
            orderNumber = "ORD-DEMO-001",
            customerId = "USER-DEMO-001",
            quotationId = "qt-demo-001",
            approvedQuotationRevisionId = "rev-001-v1",
            status = OrderStatusType.READY,
            priority = OrderPriority.HIGH,
            items = listOf(
                OrderItem(
                    itemId = "item-demo-01",
                    description = "Premium Business Cards",
                    specification = "3.25x2.0 in, 4/4 Color + Spot UV, 1000 Pcs",
                    quantity = 1000,
                    unit = "Pcs",
                    unitPrice = 1.20.toMoney(),
                    discount = 100.toMoney()
                )
            ),
            discount = Money.ZERO,
            paymentTerms = PaymentTerms(
                type = PaymentTermType.PARTIAL_ADVANCE,
                advancePercentage = 50
            ),
            deliveryRequirement = DeliveryRequirement.DEFAULT_PICKUP,
            jobHandoffStatus = JobHandoffStatus.READY_FOR_JOB,
            notes = "Demo order for Showcase.",
            confirmedAt = "2026-08-31T10:00:00Z",
            confirmedBy = "Demo Admin",
            createdAt = "2026-08-31T09:00:00Z",
            updatedAt = "2026-08-31T10:30:00Z"
        )
    ) + FakeOrderDataSource.defaultSampleOrders()
}
