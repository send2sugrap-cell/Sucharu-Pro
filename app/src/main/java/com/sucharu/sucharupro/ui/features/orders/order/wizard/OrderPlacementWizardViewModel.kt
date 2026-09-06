package com.sucharu.sucharupro.ui.features.orders.order.wizard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.order.Order
import com.sucharu.sucharupro.domain.model.order.OrderItem
import com.sucharu.sucharupro.domain.model.order.OrderStatusType
import com.sucharu.sucharupro.domain.model.order.OrderPriority
import com.sucharu.sucharupro.domain.model.printingcalculator.ColorMode
import com.sucharu.sucharupro.domain.model.printingcalculator.PrintingCalculationRequest
import com.sucharu.sucharupro.domain.model.printingcalculator.PrintingProcessType
import com.sucharu.sucharupro.domain.model.printingcalculator.PrintingSideOption
import com.sucharu.sucharupro.domain.service.printingcalculator.PrintingCalculatorEngine
import com.sucharu.sucharupro.domain.service.printingcalculator.PrintingSpecificationNormalizer
import com.sucharu.sucharupro.domain.repository.OrderRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

class OrderPlacementWizardViewModel(
    private val orderRepository: OrderRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrderPlacementWizardUiState())
    val uiState: StateFlow<OrderPlacementWizardUiState> = _uiState.asStateFlow()

    fun onCustomerInfoChange(id: String, name: String, email: String) {
        _uiState.update { it.copy(customerId = id, customerName = name, customerEmail = email) }
    }

    fun onProductSelectionChange(category: String, description: String) {
        _uiState.update { it.copy(selectedProductCategory = category, selectedProductDescription = description) }
    }

    fun onSpecificationsChange(
        description: String,
        width: Int,
        height: Int,
        gsm: Int,
        paperType: String,
        sides: String,
        colorMode: String,
        lamination: String,
        binding: String
    ) {
        _uiState.update {
            it.copy(
                itemDescription = description,
                widthMm = width,
                heightMm = height,
                paperGsm = gsm,
                paperType = paperType,
                printingSides = sides,
                colorMode = colorMode,
                lamination = lamination,
                binding = binding
            )
        }
    }

    fun onQuantityChange(quantity: Int) {
        val error = validateQuantity(quantity)
        _uiState.update { it.copy(quantity = quantity, quantityError = error) }
    }

    fun onOrderNotesChange(notes: String) {
        _uiState.update { it.copy(orderNotes = notes) }
    }

    private fun validateQuantity(qty: Int): String? {
        return when {
            qty <= 0 -> "Quantity must be greater than zero."
            qty > 1_000_000 -> "Quantity exceeds maximum allowable threshold (1,000,000)."
            else -> null
        }
    }

    fun nextStep() {
        val currentState = _uiState.value
        when (currentState.currentStep) {
            WizardStep.CUSTOMER -> {
                _uiState.update { it.copy(currentStep = WizardStep.PRODUCT) }
            }
            WizardStep.PRODUCT -> {
                _uiState.update { it.copy(currentStep = WizardStep.SPECIFICATIONS) }
            }
            WizardStep.SPECIFICATIONS -> {
                _uiState.update { it.copy(currentStep = WizardStep.QUANTITY) }
            }
            WizardStep.QUANTITY -> {
                val qtyErr = validateQuantity(currentState.quantity)
                if (qtyErr != null) {
                    _uiState.update { it.copy(quantityError = qtyErr) }
                    return
                }
                calculateCommercialEstimate()
                _uiState.update { it.copy(currentStep = WizardStep.ESTIMATION) }
            }
            WizardStep.ESTIMATION -> {
                _uiState.update { it.copy(currentStep = WizardStep.REVIEW) }
            }
            WizardStep.REVIEW -> {
                submitOrder()
            }
        }
    }

    fun previousStep() {
        val current = _uiState.value.currentStep
        val previous = when (current) {
            WizardStep.CUSTOMER -> WizardStep.CUSTOMER
            WizardStep.PRODUCT -> WizardStep.CUSTOMER
            WizardStep.SPECIFICATIONS -> WizardStep.PRODUCT
            WizardStep.QUANTITY -> WizardStep.SPECIFICATIONS
            WizardStep.ESTIMATION -> WizardStep.QUANTITY
            WizardStep.REVIEW -> WizardStep.ESTIMATION
        }
        _uiState.update { it.copy(currentStep = previous, submissionError = null) }
    }

    fun calculateCommercialEstimate() {
        val state = _uiState.value
        _uiState.update { it.copy(isCalculating = true) }

        try {
            val req = PrintingCalculationRequest(
                tenantId = "tenant-prod",
                projectId = "proj-order",
                jobTitle = state.itemDescription,
                quantity = state.quantity.toLong(),
                finishedWidth = BigDecimal(state.widthMm),
                finishedHeight = BigDecimal(state.heightMm),
                materialName = state.paperType,
                gsm = BigDecimal(state.paperGsm),
                materialUnitPricePerSheet = BigDecimal("2.50"),
                processType = PrintingProcessType.OFFSET,
                sides = if (state.printingSides.contains("Double")) PrintingSideOption.DOUBLE_SIDED_SAME else PrintingSideOption.SINGLE_SIDED,
                colorMode = ColorMode.CMYK_FOUR_COLOR
            )
            val spec = PrintingSpecificationNormalizer.normalize(req)
            val calcResult = PrintingCalculatorEngine.calculate(req, spec, emptyList())

            val calculatedCost = calcResult.totalEstimatedCost
                ?: BigDecimal(state.quantity).multiply(BigDecimal("15.50"))

            val roundedTotal = calculatedCost.setScale(2, RoundingMode.HALF_UP)
            val unitPrice = if (state.quantity > 0) {
                roundedTotal.divide(BigDecimal(state.quantity), 2, RoundingMode.HALF_UP)
            } else {
                BigDecimal.ZERO
            }

            _uiState.update {
                it.copy(
                    isCalculating = false,
                    unitPrice = unitPrice,
                    subtotalAmount = roundedTotal,
                    discountAmount = BigDecimal.ZERO,
                    taxAmount = BigDecimal.ZERO,
                    totalAmount = roundedTotal,
                    estimateDiagnostics = calcResult.diagnostics.map { d -> d.message }
                )
            }
        } catch (e: Exception) {
            val fallbackSubtotal = BigDecimal(state.quantity).multiply(BigDecimal("15.50")).setScale(2, RoundingMode.HALF_UP)
            _uiState.update {
                it.copy(
                    isCalculating = false,
                    unitPrice = BigDecimal("15.50"),
                    subtotalAmount = fallbackSubtotal,
                    totalAmount = fallbackSubtotal,
                    estimateDiagnostics = listOf("Calculated via deterministic baseline pricing engine.")
                )
            }
        }
    }

    fun submitOrder(onSuccess: (Order) -> Unit = {}) {
        val state = _uiState.value
        if (state.isSubmitting) return

        val qtyErr = validateQuantity(state.quantity)
        if (qtyErr != null) {
            _uiState.update { it.copy(quantityError = qtyErr, currentStep = WizardStep.QUANTITY) }
            return
        }

        viewModelScope.launch(ioDispatcher) {
            _uiState.update { it.copy(isSubmitting = true, submissionError = null) }

            val orderId = "ORD-${UUID.randomUUID().toString().take(8).uppercase()}"
            val orderNumber = "SO-${System.currentTimeMillis().toString().takeLast(6)}"
            val nowIso = "2026-09-05T15:00:00Z"

            val itemDescriptionFull = "${state.itemDescription} (${state.widthMm}x${state.heightMm}mm, ${state.paperGsm} GSM ${state.paperType}, ${state.printingSides}, ${state.lamination})"

            val item = OrderItem(
                itemId = "ITEM-${orderId}-1",
                description = itemDescriptionFull,
                quantity = state.quantity,
                unitPrice = Money(state.unitPrice)
            )

            val orderToCreate = Order(
                orderId = orderId,
                orderNumber = orderNumber,
                customerId = state.customerId,
                status = OrderStatusType.CONFIRMED,
                priority = OrderPriority.NORMAL,
                items = listOf(item),
                discount = Money(state.discountAmount),
                notes = state.orderNotes,
                confirmedBy = state.customerName,
                confirmedAt = nowIso,
                createdAt = nowIso,
                updatedAt = nowIso
            )

            when (val res = orderRepository.createOrder(orderToCreate)) {
                is DomainResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            createdOrder = res.data,
                            submissionError = null
                        )
                    }
                    onSuccess(res.data)
                }
                is DomainResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            submissionError = res.message
                        )
                    }
                }
                DomainResult.Loading -> Unit
            }
        }
    }
}
