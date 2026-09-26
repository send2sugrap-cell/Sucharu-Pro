package com.sucharu.sucharupro.domain.service.pricing

import com.sucharu.sucharupro.data.repository.ProductPriceRepositoryImpl
import com.sucharu.sucharupro.domain.model.inventory.InventoryUnit
import com.sucharu.sucharupro.domain.model.pricing.ProductPriceConfiguration
import com.sucharu.sucharupro.domain.model.pricing.QuantityPriceTier
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CommercialPricingServiceTest {

    private lateinit var repository: ProductPriceRepositoryImpl
    private lateinit var service: CommercialPricingService

    @Before
    fun setUp() {
        repository = ProductPriceRepositoryImpl()
        service = CommercialPricingService(repository)
    }

    @Test
    fun `mandatoryHistoricalPriceInvariant_orderPriceSnapshotRemainsUnchangedWhenMasterPriceIncreases`() = runBlocking {
        // 1. Setup Master Price Version 1: 1,000 pcs = ৳350 (Delivery: ৳60 => Grand Total: ৳410)
        val v1Config = ProductPriceConfiguration(
            priceConfigId = "PRC-2026-VC-01",
            productId = "PROD-101",
            priceVersion = 1,
            isActive = true,
            basePrice = 350.0,
            baseQuantity = 1000,
            unit = InventoryUnit.PCS,
            minOrderQuantity = 1000,
            insideDhakaDeliveryCharge = 60.0,
            createdAt = "2026-09-26T12:00:00Z",
            updatedAt = "2026-09-26T12:00:00Z",
            createdBy = "ADMIN-001"
        )
        service.createPriceConfig(v1Config)

        // 2. Evaluate commercial price today for Order #ORD-1001
        val evalV1 = service.evaluateCommercialPrice("PROD-101", 1000, deliveryZone = "INSIDE_DHAKA")
        assertEquals(410.0, evalV1.grandTotalAmount, 0.01)

        // 3. Save Order Price Snapshot for Order #ORD-1001
        val savedSnapshot = service.createOrderPriceSnapshot("ORD-1001", evalV1)
        assertEquals(410.0, savedSnapshot.grandTotalAmount, 0.01)
        assertEquals(350.0, savedSnapshot.basePriceSnapshot, 0.01)

        // 4. Update Master Price to Version 2: 1,000 pcs increased to ৳500 (Delivery: ৳60 => Grand Total: ৳560)
        val v2Config = v1Config.copy(
            priceConfigId = "PRC-2026-VC-02",
            priceVersion = 2,
            basePrice = 500.0
        )
        service.createPriceConfig(v2Config)

        // 5. Re-evaluate current commercial price -> New price is ৳560
        val currentEval = service.evaluateCommercialPrice("PROD-101", 1000, deliveryZone = "INSIDE_DHAKA")
        assertEquals(560.0, currentEval.grandTotalAmount, 0.01)

        // 6. Retrieve Historical Order Price Snapshot for Order #ORD-1001
        val historicalSnapshot = service.getOrderPriceSnapshot("ORD-1001")
        assertNotNull(historicalSnapshot)

        // MANDATORY HISTORICAL PRICE INVARIANT PROOF:
        // Order #ORD-1001 grand total MUST REMAIN ৳410 (100% UNCHANGED), NOT ৳560!
        assertEquals("Historical Order Price MUST remain ৳410 even after Master Price increased!", 410.0, historicalSnapshot!!.grandTotalAmount, 0.01)
        assertEquals("Historical Base Price Snapshot MUST remain ৳350!", 350.0, historicalSnapshot.basePriceSnapshot, 0.01)
    }

    @Test
    fun `evaluateCommercialPrice_matchesHigherQuantityTierPriceCorrectly`() = runBlocking {
        val configWithTiers = ProductPriceConfiguration(
            priceConfigId = "PRC-2026-TIERS",
            productId = "PROD-202",
            priceVersion = 1,
            isActive = true,
            basePrice = 1000.0,
            baseQuantity = 1000,
            quantityTiers = listOf(
                QuantityPriceTier("T1", "PRC-2026-TIERS", 1000, 1000.0, 1.0, "2026-09-26T12:00:00Z"),
                QuantityPriceTier("T2", "PRC-2026-TIERS", 5000, 4000.0, 0.8, "2026-09-26T12:00:00Z")
            ),
            insideDhakaDeliveryCharge = 60.0,
            createdAt = "2026-09-26T12:00:00Z",
            updatedAt = "2026-09-26T12:00:00Z",
            createdBy = "ADMIN-001"
        )
        service.createPriceConfig(configWithTiers)

        val eval5000 = service.evaluateCommercialPrice("PROD-202", 5000, deliveryZone = "INSIDE_DHAKA")
        assertEquals(5000, eval5000.matchedTierQuantityBreak)
        assertEquals(4000.0, eval5000.matchedTierPrice!!, 0.01)
        assertEquals(4060.0, eval5000.grandTotalAmount, 0.01)
    }
}
