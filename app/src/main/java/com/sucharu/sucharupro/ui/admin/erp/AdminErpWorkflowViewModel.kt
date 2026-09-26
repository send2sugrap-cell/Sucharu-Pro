package com.sucharu.sucharupro.ui.admin.erp

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.data.api.model.erp.OrchestrateOrderActionRequestDto
import com.sucharu.sucharupro.data.api.model.erp.OrchestratedOrderResultDto
import com.sucharu.sucharupro.data.repository.ErpWorkflowRepositoryImpl
import com.sucharu.sucharupro.data.repository.ProductPriceRepositoryImpl
import com.sucharu.sucharupro.domain.model.erp.ErpWorkflowOrchestration
import com.sucharu.sucharupro.domain.repository.ErpWorkflowRepository
import com.sucharu.sucharupro.domain.service.erp.ErpWorkflowOrchestrationService
import com.sucharu.sucharupro.domain.service.pricing.CommercialPricingService
import kotlinx.coroutines.launch

/**
 * ViewModel for Form 05 — Admin ERP / Order / Fulfillment Integration.
 */
class AdminErpWorkflowViewModel(
    erpRepository: ErpWorkflowRepository = ErpWorkflowRepositoryImpl(),
    pricingService: CommercialPricingService = CommercialPricingService(ProductPriceRepositoryImpl())
) : ViewModel() {

    private val service = ErpWorkflowOrchestrationService(erpRepository, pricingService)

    var orchestrationsList = mutableStateListOf<ErpWorkflowOrchestration>()
        private set

    var lastResult by mutableStateOf<OrchestratedOrderResultDto?>(null)
        private set

    var statusMessage by mutableStateOf<String?>(null)
        private set

    init {
        loadOrchestrations()
    }

    fun loadOrchestrations() {
        viewModelScope.launch {
            val list = service.listAllOrchestrations()
            orchestrationsList.clear()
            orchestrationsList.addAll(list)
        }
    }

    fun testE2eOrderOrchestration() {
        viewModelScope.launch {
            val req = OrchestrateOrderActionRequestDto(
                customerAction = "ORDER_NOW",
                customerId = "CUST-1001",
                productId = "PROD-101",
                orderQuantity = 1000,
                deliveryZone = "INSIDE_DHAKA",
                specialInstructions = "Urgent Eid Order"
            )
            val result = service.orchestrateCustomerOrderAction(req)
            lastResult = result
            loadOrchestrations()
            statusMessage = "🎉 E2E Order Orchestration Passed! Order #${result.orderId} created with price ৳${result.grandTotalAmount.toInt()}!"
        }
    }
}
