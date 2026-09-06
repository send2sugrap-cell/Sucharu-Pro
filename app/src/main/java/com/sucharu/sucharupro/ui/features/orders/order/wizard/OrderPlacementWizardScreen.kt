package com.sucharu.sucharupro.ui.features.orders.order.wizard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.domain.model.order.Order
import com.sucharu.sucharupro.ui.components.AppButton
import com.sucharu.sucharupro.ui.components.AppCard
import com.sucharu.sucharupro.ui.components.AppOutlinedButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderPlacementWizardScreen(
    viewModel: OrderPlacementWizardViewModel,
    onBackClick: () -> Unit = {},
    onOrderCreated: (Order) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0B132B))
            .padding(16.dp)
    ) {
        // Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    if (state.currentStep == WizardStep.CUSTOMER) {
                        onBackClick()
                    } else {
                        viewModel.previousStep()
                    }
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "ORDER PLACEMENT WIZARD",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF9ECAFF),
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Step ${state.currentStep.stepNumber} of 6 — ${state.currentStep.title}",
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }
            }
            Surface(
                color = Color(0xFF1C2541),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "${(state.currentStep.stepNumber * 100) / 6}%",
                    color = Color(0xFF48CAE4),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        // Progress Bar
        LinearProgressIndicator(
            progress = { state.currentStep.stepNumber / 6f },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = Color(0xFF48CAE4),
            trackColor = Color(0xFF1C2541),
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Main Wizard Body Content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (state.createdOrder != null) {
                OrderSuccessView(
                    order = state.createdOrder!!,
                    onViewOrderDetails = { onOrderCreated(state.createdOrder!!) }
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                ) {
                    if (state.submissionError != null) {
                        Surface(
                            color = Color(0xFF4A0E17),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            Text(
                                text = "Error: ${state.submissionError}",
                                color = Color(0xFFFFB4AB),
                                fontSize = 13.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    when (state.currentStep) {
                        WizardStep.CUSTOMER -> StepCustomerContent(state, viewModel)
                        WizardStep.PRODUCT -> StepProductContent(state, viewModel)
                        WizardStep.SPECIFICATIONS -> StepSpecificationsContent(state, viewModel)
                        WizardStep.QUANTITY -> StepQuantityContent(state, viewModel)
                        WizardStep.ESTIMATION -> StepEstimationContent(state, viewModel)
                        WizardStep.REVIEW -> StepReviewContent(state, viewModel)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action Navigation Footer Buttons
        if (state.createdOrder == null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (state.currentStep != WizardStep.CUSTOMER) {
                    AppOutlinedButton(
                        text = "Back",
                        onClick = { viewModel.previousStep() },
                        enabled = !state.isSubmitting,
                        modifier = Modifier.weight(1f)
                    )
                }

                val buttonText = when (state.currentStep) {
                    WizardStep.REVIEW -> if (state.isSubmitting) "Submitting..." else "Confirm & Place Order"
                    else -> "Continue"
                }

                AppButton(
                    text = buttonText,
                    onClick = {
                        if (state.currentStep == WizardStep.REVIEW) {
                            viewModel.submitOrder(onSuccess = onOrderCreated)
                        } else {
                            viewModel.nextStep()
                        }
                    },
                    enabled = !state.isSubmitting && (state.quantityError == null),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun StepCustomerContent(
    state: OrderPlacementWizardUiState,
    viewModel: OrderPlacementWizardViewModel
) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp)
    ) {
        Column {
            Text("Step 1: Customer Context", fontWeight = FontWeight.Bold, color = Color(0xFF9ECAFF), fontSize = 16.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Select or verify the customer placing this order.", color = Color(0xFFB7C8D8), fontSize = 12.sp)

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = state.customerId,
                onValueChange = { viewModel.onCustomerInfoChange(it, state.customerName, state.customerEmail) },
                label = { Text("Customer ID") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = state.customerName,
                onValueChange = { viewModel.onCustomerInfoChange(state.customerId, it, state.customerEmail) },
                label = { Text("Customer Name / Company") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = state.customerEmail,
                onValueChange = { viewModel.onCustomerInfoChange(state.customerId, state.customerName, it) },
                label = { Text("Contact Email") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
    }
}

@Composable
private fun StepProductContent(
    state: OrderPlacementWizardUiState,
    viewModel: OrderPlacementWizardViewModel
) {
    val categories = listOf(
        "Flyers / Brochures",
        "Business Cards",
        "Posters & Banners",
        "Packaging Boxes",
        "Booklets & Catalogs",
        "Custom Print Job"
    )

    AppCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp)
    ) {
        Column {
            Text("Step 2: Product & Service Selection", fontWeight = FontWeight.Bold, color = Color(0xFF9ECAFF), fontSize = 16.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Select the category for this commercial print requirement.", color = Color(0xFFB7C8D8), fontSize = 12.sp)

            Spacer(modifier = Modifier.height(16.dp))

            categories.forEach { category ->
                val isSelected = state.selectedProductCategory == category
                Surface(
                    onClick = { viewModel.onProductSelectionChange(category, state.selectedProductDescription) },
                    color = if (isSelected) Color(0xFF00497D) else Color(0xFF0B132B).copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(category, color = Color.White, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                        if (isSelected) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF48CAE4))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepSpecificationsContent(
    state: OrderPlacementWizardUiState,
    viewModel: OrderPlacementWizardViewModel
) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp)
    ) {
        Column {
            Text("Step 3: Specification Input", fontWeight = FontWeight.Bold, color = Color(0xFF9ECAFF), fontSize = 16.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Enter physical dimensions, substrate paper, and finishing specifications.", color = Color(0xFFB7C8D8), fontSize = 12.sp)

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = state.itemDescription,
                onValueChange = {
                    viewModel.onSpecificationsChange(
                        it, state.widthMm, state.heightMm, state.paperGsm,
                        state.paperType, state.printingSides, state.colorMode, state.lamination, state.binding
                    )
                },
                label = { Text("Job Description") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = state.widthMm.toString(),
                    onValueChange = {
                        val w = it.toIntOrNull() ?: 0
                        viewModel.onSpecificationsChange(
                            state.itemDescription, w, state.heightMm, state.paperGsm,
                            state.paperType, state.printingSides, state.colorMode, state.lamination, state.binding
                        )
                    },
                    label = { Text("Width (mm)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                OutlinedTextField(
                    value = state.heightMm.toString(),
                    onValueChange = {
                        val h = it.toIntOrNull() ?: 0
                        viewModel.onSpecificationsChange(
                            state.itemDescription, state.widthMm, h, state.paperGsm,
                            state.paperType, state.printingSides, state.colorMode, state.lamination, state.binding
                        )
                    },
                    label = { Text("Height (mm)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = state.paperGsm.toString(),
                    onValueChange = {
                        val gsm = it.toIntOrNull() ?: 0
                        viewModel.onSpecificationsChange(
                            state.itemDescription, state.widthMm, state.heightMm, gsm,
                            state.paperType, state.printingSides, state.colorMode, state.lamination, state.binding
                        )
                    },
                    label = { Text("Paper GSM") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                OutlinedTextField(
                    value = state.printingSides,
                    onValueChange = {
                        viewModel.onSpecificationsChange(
                            state.itemDescription, state.widthMm, state.heightMm, state.paperGsm,
                            state.paperType, it, state.colorMode, state.lamination, state.binding
                        )
                    },
                    label = { Text("Printing Sides") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
        }
    }
}

@Composable
private fun StepQuantityContent(
    state: OrderPlacementWizardUiState,
    viewModel: OrderPlacementWizardViewModel
) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp)
    ) {
        Column {
            Text("Step 4: Quantity & Validation", fontWeight = FontWeight.Bold, color = Color(0xFF9ECAFF), fontSize = 16.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Specify the total production quantity required.", color = Color(0xFFB7C8D8), fontSize = 12.sp)

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = state.quantity.toString(),
                onValueChange = {
                    val q = it.toIntOrNull() ?: 0
                    viewModel.onQuantityChange(q)
                },
                label = { Text("Order Quantity (Units)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = state.quantityError != null,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            if (state.quantityError != null) {
                Text(
                    text = state.quantityError!!,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Preset Batch Suggestions:", color = Color(0xFF9ECAFF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(500, 1000, 2500, 5000).forEach { preset ->
                    AssistChip(
                        onClick = { viewModel.onQuantityChange(preset) },
                        label = { Text("$preset pcs") }
                    )
                }
            }
        }
    }
}

@Composable
private fun StepEstimationContent(
    state: OrderPlacementWizardUiState,
    viewModel: OrderPlacementWizardViewModel
) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp)
    ) {
        Column {
            Text("Step 5: Commercial Price Estimate", fontWeight = FontWeight.Bold, color = Color(0xFF9ECAFF), fontSize = 16.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Calculated using the Smart Printing Calculator Engine.", color = Color(0xFFB7C8D8), fontSize = 12.sp)

            Spacer(modifier = Modifier.height(16.dp))

            if (state.isCalculating) {
                CircularProgressIndicator(color = Color(0xFF48CAE4), modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0B132B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Unit Price:", color = Color(0xFFB7C8D8))
                            Text("৳ ${state.unitPrice}", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Quantity:", color = Color(0xFFB7C8D8))
                            Text("${state.quantity} pcs", color = Color.White)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = Color(0xFF1C2541))
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Estimated Total Amount:", color = Color(0xFF48CAE4), fontWeight = FontWeight.Bold)
                            Text("৳ ${state.totalAmount}", color = Color(0xFF48CAE4), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                    }
                }

                if (state.estimateDiagnostics.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    state.estimateDiagnostics.forEach { diag ->
                        Text("• $diag", color = Color(0xFFB7C8D8), fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun StepReviewContent(
    state: OrderPlacementWizardUiState,
    viewModel: OrderPlacementWizardViewModel
) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp)
    ) {
        Column {
            Text("Step 6: Quotation Review & Confirmation", fontWeight = FontWeight.Bold, color = Color(0xFF9ECAFF), fontSize = 16.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Review complete commercial order details before final submission.", color = Color(0xFFB7C8D8), fontSize = 12.sp)

            Spacer(modifier = Modifier.height(16.dp))

            Surface(color = Color(0xFF0B132B), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("CUSTOMER: ${state.customerName} (${state.customerId})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("JOB: ${state.itemDescription}", color = Color(0xFF9ECAFF), fontSize = 12.sp)
                    Text("SPECS: ${state.widthMm}x${state.heightMm}mm | ${state.paperGsm} GSM | ${state.printingSides}", color = Color(0xFFB7C8D8), fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("QUANTITY: ${state.quantity} pcs @ ৳ ${state.unitPrice} / unit", color = Color.White, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("COMMERCIAL TOTAL: ৳ ${state.totalAmount}", color = Color(0xFF48CAE4), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = state.orderNotes,
                onValueChange = { viewModel.onOrderNotesChange(it) },
                label = { Text("Special Order Notes / Instructions") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
        }
    }
}

@Composable
private fun OrderSuccessView(
    order: Order,
    onViewOrderDetails: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.ShoppingCart,
            contentDescription = null,
            tint = Color(0xFF48CAE4),
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "ORDER CREATED SUCCESSFULLY!",
            fontWeight = FontWeight.Bold,
            color = Color(0xFF9ECAFF),
            fontSize = 18.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Order ID: ${order.orderId}\nOrder Number: ${order.orderNumber}",
            color = Color.White,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Status: ${order.status.name} | Total: ৳ ${order.totalAmount.amount}",
            color = Color(0xFF48CAE4),
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        AppButton(
            text = "View Authoritative Order Details",
            onClick = onViewOrderDetails,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
