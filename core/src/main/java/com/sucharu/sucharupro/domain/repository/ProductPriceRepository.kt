package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.pricing.OrderPriceSnapshot
import com.sucharu.sucharupro.domain.model.pricing.ProductPriceConfiguration

/**
 * Domain Repository Interface for Form 04 — Pricing & Commercial Rules.
 */
interface ProductPriceRepository {
    suspend fun createPriceConfiguration(config: ProductPriceConfiguration): ProductPriceConfiguration
    suspend fun updatePriceConfiguration(config: ProductPriceConfiguration): ProductPriceConfiguration
    suspend fun getActivePriceConfigurationByProduct(productId: String): ProductPriceConfiguration?
    suspend fun getPriceConfigurationById(priceConfigId: String): ProductPriceConfiguration?
    suspend fun getAllPriceConfigurations(): List<ProductPriceConfiguration>
    suspend fun saveOrderPriceSnapshot(snapshot: OrderPriceSnapshot): OrderPriceSnapshot
    suspend fun getOrderPriceSnapshotByOrder(orderId: String): OrderPriceSnapshot?
}
