package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.domain.model.inventory.InventoryUnit
import com.sucharu.sucharupro.domain.model.pricing.OrderPriceSnapshot
import com.sucharu.sucharupro.domain.model.pricing.ProductPriceConfiguration
import com.sucharu.sucharupro.domain.model.pricing.QuantityPriceTier
import com.sucharu.sucharupro.domain.repository.ProductPriceRepository
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe Repository Implementation for Form 04 Pricing & Commercial Rules.
 */
class ProductPriceRepositoryImpl : ProductPriceRepository {
    private val configsStore = ConcurrentHashMap<String, ProductPriceConfiguration>()
    private val snapshotsStore = ConcurrentHashMap<String, OrderPriceSnapshot>()

    init {
        // Default Sample Pricing Configuration for Visiting Cards (Product PROD-101)
        val p1 = ProductPriceConfiguration(
            priceConfigId = "PRC-2026-VC-01",
            productId = "PROD-101",
            priceVersion = 1,
            isActive = true,
            currency = "BDT",
            basePrice = 350.0,
            baseQuantity = 1000,
            unit = InventoryUnit.PCS,
            minOrderQuantity = 1000,
            maxOrderQuantity = 100000,
            quantityTiers = listOf(
                QuantityPriceTier("T1", "PRC-2026-VC-01", 1000, 350.0, 0.35, "2026-09-26T12:00:00Z"),
                QuantityPriceTier("T2", "PRC-2026-VC-01", 2000, 650.0, 0.325, "2026-09-26T12:00:00Z"),
                QuantityPriceTier("T3", "PRC-2026-VC-01", 5000, 1500.0, 0.30, "2026-09-26T12:00:00Z")
            ),
            surchargeAmount = 0.0,
            insideDhakaDeliveryCharge = 60.0,
            outsideDhakaDeliveryCharge = 120.0,
            taxPercentage = 0.0,
            createdAt = "2026-09-26T12:00:00Z",
            updatedAt = "2026-09-26T12:00:00Z",
            createdBy = "SYSTEM"
        )
        configsStore[p1.priceConfigId] = p1
    }

    override suspend fun createPriceConfiguration(config: ProductPriceConfiguration): ProductPriceConfiguration {
        configsStore[config.priceConfigId] = config
        return config
    }

    override suspend fun updatePriceConfiguration(config: ProductPriceConfiguration): ProductPriceConfiguration {
        configsStore[config.priceConfigId] = config
        return config
    }

    override suspend fun getActivePriceConfigurationByProduct(productId: String): ProductPriceConfiguration? {
        return configsStore.values.firstOrNull { it.productId == productId && it.isActive }
    }

    override suspend fun getPriceConfigurationById(priceConfigId: String): ProductPriceConfiguration? {
        return configsStore[priceConfigId]
    }

    override suspend fun getAllPriceConfigurations(): List<ProductPriceConfiguration> {
        return configsStore.values.toList()
    }

    override suspend fun saveOrderPriceSnapshot(snapshot: OrderPriceSnapshot): OrderPriceSnapshot {
        snapshotsStore[snapshot.orderId] = snapshot
        return snapshot
    }

    override suspend fun getOrderPriceSnapshotByOrder(orderId: String): OrderPriceSnapshot? {
        return snapshotsStore[orderId]
    }
}
