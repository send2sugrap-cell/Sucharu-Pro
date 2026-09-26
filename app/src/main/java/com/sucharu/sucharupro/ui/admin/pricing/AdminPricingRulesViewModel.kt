package com.sucharu.sucharupro.ui.admin.pricing

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.data.repository.ProductPriceRepositoryImpl
import com.sucharu.sucharupro.domain.model.pricing.CommercialPriceEvaluation
import com.sucharu.sucharupro.domain.model.pricing.OrderPriceSnapshot
import com.sucharu.sucharupro.domain.model.pricing.ProductPriceConfiguration
import com.sucharu.sucharupro.domain.repository.ProductPriceRepository
import com.sucharu.sucharupro.domain.service.pricing.CommercialPricingService
import kotlinx.coroutines.launch

/**
 * ViewModel for Form 04 — Admin Pricing & Commercial Rules Management.
 */
class AdminPricingRulesViewModel(
    repository: ProductPriceRepository = ProductPriceRepositoryImpl()
) : ViewModel() {

    private val service = CommercialPricingService(repository)

    var priceConfigsList = mutableStateListOf<ProductPriceConfiguration>()
        private set

    var activePriceConfig by mutableStateOf<ProductPriceConfiguration>(
        ProductPriceConfiguration(
            priceConfigId = "PRC-2026-VC-01",
            productId = "PROD-101",
            priceVersion = 1,
            isActive = true,
            currency = "BDT",
            basePrice = 350.0,
            baseQuantity = 1000,
            minOrderQuantity = 1000,
            maxOrderQuantity = 100000,
            surchargeAmount = 0.0,
            insideDhakaDeliveryCharge = 60.0,
            outsideDhakaDeliveryCharge = 120.0,
            taxPercentage = 0.0,
            createdAt = "2026-09-26T12:00:00Z",
            updatedAt = "2026-09-26T12:00:00Z",
            createdBy = "ADMIN"
        )
    )
        private set

    var evaluationResult by mutableStateOf<CommercialPriceEvaluation?>(null)
        private set

    var historicalOrderSnapshot by mutableStateOf<OrderPriceSnapshot?>(null)
        private set

    var statusMessage by mutableStateOf<String?>(null)
        private set

    init {
        loadPriceConfigs()
    }

    fun loadPriceConfigs() {
        viewModelScope.launch {
            val list = service.listAllPriceConfigurations()
            priceConfigsList.clear()
            priceConfigsList.addAll(list)
            list.firstOrNull()?.let { activePriceConfig = it }
        }
    }

    fun updateActiveConfig(updated: ProductPriceConfiguration) {
        activePriceConfig = updated
    }

    fun savePriceConfig() {
        viewModelScope.launch {
            val timestamp = "2026-09-26T13:00:00Z"
            val saved = service.createPriceConfig(
                activePriceConfig.copy(
                    updatedAt = timestamp,
                    updatedBy = "ADMIN"
                )
            )
            activePriceConfig = saved
            loadPriceConfigs()
            statusMessage = "✓ Commercial pricing configuration saved!"
        }
    }

    fun runPriceEvaluation(quantity: Int) {
        viewModelScope.launch {
            val eval = service.evaluateCommercialPrice(activePriceConfig.productId, quantity)
            evaluationResult = eval
        }
    }

    fun testHistoricalPriceInvariant() {
        viewModelScope.launch {
            // 1. Evaluate commercial price today
            val eval = service.evaluateCommercialPrice("PROD-101", 1000)

            // 2. Create Order #ORD-TEST-001 Snapshot
            val snapshot = service.createOrderPriceSnapshot("ORD-TEST-001", eval)

            // 3. Update Master Price to Version 2 (Price increased from 350 to 500)
            val v2 = activePriceConfig.copy(
                priceConfigId = "PRC-2026-VC-02",
                priceVersion = 2,
                basePrice = 500.0
            )
            service.createPriceConfig(v2)

            // 4. Retrieve Historical Snapshot for Order #ORD-TEST-001
            val retrievedSnapshot = service.getOrderPriceSnapshot("ORD-TEST-001")
            historicalOrderSnapshot = retrievedSnapshot

            if (retrievedSnapshot != null && retrievedSnapshot.basePriceSnapshot == 350.0 && retrievedSnapshot.grandTotalAmount == eval.grandTotalAmount) {
                statusMessage = "🎉 Historical Price Invariant VERIFIED! Order #ORD-TEST-001 price remains ৳350 even after Master Price increased to ৳500!"
            }
        }
    }
}
