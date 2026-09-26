package com.sucharu.sucharupro.domain.service.erp

import com.sucharu.sucharupro.data.api.model.erp.OrchestrateOrderActionRequestDto
import com.sucharu.sucharupro.data.repository.ErpWorkflowRepositoryImpl
import com.sucharu.sucharupro.data.repository.ProductPriceRepositoryImpl
import com.sucharu.sucharupro.domain.model.erp.ErpWorkflowStatus
import com.sucharu.sucharupro.domain.model.inventory.InventoryUnit
import com.sucharu.sucharupro.domain.model.pricing.ProductPriceConfiguration
import com.sucharu.sucharupro.domain.service.pricing.CommercialPricingService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class ErpWorkflowOrchestrationServiceTest {

    private lateinit var erpRepository: ErpWorkflowRepositoryImpl
    private lateinit var pricingRepository: ProductPriceRepositoryImpl
    private lateinit var pricingService: CommercialPricingService
    private lateinit var orchestrationService: ErpWorkflowOrchestrationService

    @Before
    fun setUp() {
        erpRepository = ErpWorkflowRepositoryImpl()
        pricingRepository = ProductPriceRepositoryImpl()
        pricingService = CommercialPricingService(pricingRepository)
        orchestrationService = ErpWorkflowOrchestrationService(erpRepository, pricingService)
    }

    @Test
    fun `orchestrateCustomerOrderAction_createsOrderAndPreservesPriceSnapshot`() = runBlocking {
        // 1. Setup Form 04 Master Price Version 1: 1,000 pcs = ৳350 (Delivery: ৳60 => Grand Total: ৳410)
        val priceConfig = ProductPriceConfiguration(
            priceConfigId = "PRC-2026-VC-01",
            productId = "PROD-101",
            priceVersion = 1,
            isActive = true,
            basePrice = 350.0,
            baseQuantity = 1000,
            unit = InventoryUnit.PCS,
            minOrderQuantity = 1000,
            insideDhakaDeliveryCharge = 60.0,
            createdAt = "2026-09-26T14:00:00Z",
            updatedAt = "2026-09-26T14:00:00Z",
            createdBy = "ADMIN-001"
        )
        pricingService.createPriceConfig(priceConfig)

        // 2. Customer triggers ORDER_NOW action
        val req = OrchestrateOrderActionRequestDto(
            customerAction = "ORDER_NOW",
            customerId = "CUST-1001",
            productId = "PROD-101",
            orderQuantity = 1000,
            deliveryZone = "INSIDE_DHAKA",
            specialInstructions = "Urgent Eid Order"
        )

        val result = orchestrationService.orchestrateCustomerOrderAction(req)

        assertNotNull(result)
        assertEquals(ErpWorkflowStatus.ORDER_CONFIRMED.name, result.workflowStatus)
        assertEquals(410.0, result.grandTotalAmount, 0.01)

        // 3. Verify Order Price Snapshot
        val savedSnapshot = pricingService.getOrderPriceSnapshot(result.orderId!!)
        assertNotNull(savedSnapshot)
        assertEquals(410.0, savedSnapshot!!.grandTotalAmount, 0.01)

        // 4. Update Form 04 Master Price to Version 2: ৳500
        val v2Config = priceConfig.copy(
            priceConfigId = "PRC-2026-VC-02",
            priceVersion = 2,
            basePrice = 500.0
        )
        pricingService.createPriceConfig(v2Config)

        // 5. Verify Historical Price Snapshot remains UNCHANGED at ৳410!
        val historicalSnapshot = pricingService.getOrderPriceSnapshot(result.orderId!!)
        assertEquals("Historical Order Price Snapshot MUST remain ৳410 even after Master Price increased!", 410.0, historicalSnapshot!!.grandTotalAmount, 0.01)
    }
}
