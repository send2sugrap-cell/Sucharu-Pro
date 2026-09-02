package com.sucharu.sucharupro.ui.features.orders.quotation.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.domain.model.order.QuotationItem
import com.sucharu.sucharupro.ui.components.AppButton
import com.sucharu.sucharupro.ui.components.AppCard
import com.sucharu.sucharupro.ui.components.AppOutlinedButton
import com.sucharu.sucharupro.ui.components.AppTextField
import com.sucharu.sucharupro.ui.components.LoadingIndicator
import com.sucharu.sucharupro.ui.features.orders.components.CustomerSelectorField
import com.sucharu.sucharupro.ui.features.orders.components.DetailSectionCard
import com.sucharu.sucharupro.ui.features.orders.quotation.form.components.QuotationDeliveryRequirementFormCard
import com.sucharu.sucharupro.ui.features.orders.quotation.form.components.QuotationItemEditorDialog
import com.sucharu.sucharupro.ui.features.orders.quotation.form.components.QuotationPaymentTermsFormCard
import com.sucharu.sucharupro.ui.theme.spacing

/**
 * Screen providing Form entry for Creating and Editing Commercial Quotations in Sucharu Pro.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuotationFormScreen(
    viewModel: QuotationFormViewModel,
    onBackClick: () -> Unit,
    onSaveSuccess: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.screenTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                LoadingIndicator(message = "Loading quotation data...", size = 48.dp)
            }
        } else {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                val isWide = maxWidth >= 720.dp

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = MaterialTheme.spacing.screenPadding)
                        .padding(vertical = MaterialTheme.spacing.medium),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
                ) {
                    // Error Banner
                    if (!state.errorMessage.isNullOrBlank()) {
                        AppCard(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (state.isImmutableError) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.errorContainer,
                                contentColor = if (state.isImmutableError) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onErrorContainer
                            ),
                            contentPadding = PaddingValues(MaterialTheme.spacing.medium)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
                            ) {
                                if (state.isImmutableError) {
                                    Icon(
                                        imageVector = Icons.Default.WarningAmber,
                                        contentDescription = null
                                    )
                                }
                                Text(
                                    text = state.errorMessage.orEmpty(),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    if (isWide) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
                            ) {
                                CustomerAndInquirySection(
                                    state = state,
                                    viewModel = viewModel
                                )
                                QuotationPaymentTermsFormCard(
                                    paymentTerms = state.paymentTerms,
                                    onPaymentTermsChange = { viewModel.onPaymentTermsChange(it) }
                                )
                                QuotationDeliveryRequirementFormCard(
                                    deliveryRequirement = state.deliveryRequirement,
                                    onDeliveryRequirementChange = { viewModel.onDeliveryRequirementChange(it) }
                                )
                                TermsAndConditionsSection(
                                    state = state,
                                    viewModel = viewModel
                                )
                                NotesSection(
                                    state = state,
                                    viewModel = viewModel
                                )
                            }

                            Column(
                                modifier = Modifier.weight(1.2f),
                                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
                            ) {
                                QuotationItemsSection(
                                    state = state,
                                    viewModel = viewModel
                                )
                                CommercialSummarySection(
                                    state = state,
                                    viewModel = viewModel
                                )
                                ActionButtonsSection(
                                    state = state,
                                    onBackClick = onBackClick,
                                    onSave = { viewModel.saveQuotation(onSaveSuccess) }
                                )
                            }
                        }
                    } else {
                        CustomerAndInquirySection(
                            state = state,
                            viewModel = viewModel
                        )
                        QuotationItemsSection(
                            state = state,
                            viewModel = viewModel
                        )
                        CommercialSummarySection(
                            state = state,
                            viewModel = viewModel
                        )
                        QuotationPaymentTermsFormCard(
                            paymentTerms = state.paymentTerms,
                            onPaymentTermsChange = { viewModel.onPaymentTermsChange(it) }
                        )
                        QuotationDeliveryRequirementFormCard(
                            deliveryRequirement = state.deliveryRequirement,
                            onDeliveryRequirementChange = { viewModel.onDeliveryRequirementChange(it) }
                        )
                        TermsAndConditionsSection(
                            state = state,
                            viewModel = viewModel
                        )
                        NotesSection(
                            state = state,
                            viewModel = viewModel
                        )
                        ActionButtonsSection(
                            state = state,
                            onBackClick = onBackClick,
                            onSave = { viewModel.saveQuotation(onSaveSuccess) }
                        )
                    }
                }
            }
        }
    }

    if (state.isItemDialogOpen) {
        QuotationItemEditorDialog(
            initialItem = state.editingItem,
            onDismiss = { viewModel.closeItemDialog() },
            onSaveItem = { viewModel.saveItem(it) }
        )
    }
}

@Composable
private fun CustomerAndInquirySection(
    state: QuotationFormUiState,
    viewModel: QuotationFormViewModel
) {
    DetailSectionCard(
        title = "Customer & References",
        icon = Icons.Default.Person
    ) {
        CustomerSelectorField(
            selectedCustomerId = state.customerId,
            selectedCustomerName = state.customerName,
            availableCustomers = state.availableCustomers,
            onCustomerSelected = { viewModel.onCustomerSelected(it) },
            errorMessage = state.customerIdError,
            enabled = !state.isImmutableError
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
        ) {
            AppTextField(
                value = state.inquiryId,
                onValueChange = { viewModel.onInquiryIdChange(it) },
                label = "Inquiry Reference (Optional)",
                placeholder = "e.g. inq-101",
                enabled = !state.isImmutableError,
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            AppTextField(
                value = state.validUntil,
                onValueChange = { viewModel.onValidUntilChange(it) },
                label = "Validity Date",
                placeholder = "YYYY-MM-DD",
                enabled = !state.isImmutableError,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }
    }
}

@Composable
private fun QuotationItemsSection(
    state: QuotationFormUiState,
    viewModel: QuotationFormViewModel
) {
    DetailSectionCard(
        title = "Commercial Line Items (${state.items.size})",
        icon = Icons.Default.ShoppingCart,
        trailingContent = {
            if (!state.isImmutableError) {
                AppButton(
                    text = "Add Item",
                    onClick = { viewModel.openAddItemDialog() },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }
        }
    ) {
        if (!state.itemsError.isNullOrBlank()) {
            Text(
                text = state.itemsError.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = MaterialTheme.spacing.small)
            )
        }

        if (state.items.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = "No quotation items added yet. Click 'Add Item' to insert line items and pricing.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(MaterialTheme.spacing.medium)
                )
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
            ) {
                state.items.forEachIndexed { index, item ->
                    QuotationItemRow(
                        index = index,
                        item = item,
                        enabled = !state.isImmutableError,
                        onEdit = { viewModel.openEditItemDialog(index) },
                        onRemove = { viewModel.removeItem(index) }
                    )
                    if (index < state.items.lastIndex) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    }
                }
            }
        }
    }
}

@Composable
private fun QuotationItemRow(
    index: Int,
    item: QuotationItem,
    enabled: Boolean,
    onEdit: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = MaterialTheme.spacing.extraSmall),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${index + 1}. ${item.description}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            val spec = item.specification
            if (!spec.isNullOrBlank()) {
                Text(
                    text = spec,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "${item.quantity} ${item.unit} @ ${item.unitPrice.formatted()}" +
                    (if (item.discount.isPositive()) " (Disc: ${item.discount.formatted()})" else "") +
                    " = ${item.lineSubtotal.formatted()}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }

        if (enabled) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Item",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = onRemove) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Remove Item",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CommercialSummarySection(
    state: QuotationFormUiState,
    viewModel: QuotationFormViewModel
) {
    DetailSectionCard(
        title = "Commercial Pricing Summary",
        icon = Icons.Default.Calculate
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Items Subtotal",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = state.subtotal.formatted(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

        AppTextField(
            value = state.quotationDiscountText,
            onValueChange = { viewModel.onQuotationDiscountChange(it) },
            label = "Quotation Level Discount (৳)",
            placeholder = "0.00",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            errorMessage = state.discountError,
            enabled = !state.isImmutableError,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Final Total Amount",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = state.totalAmount.formatted(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun TermsAndConditionsSection(
    state: QuotationFormUiState,
    viewModel: QuotationFormViewModel
) {
    DetailSectionCard(
        title = "Terms & Conditions",
        icon = Icons.Default.Gavel
    ) {
        AppTextField(
            value = state.termsAndConditions,
            onValueChange = { viewModel.onTermsAndConditionsChange(it) },
            label = "Legal & Commercial Terms",
            placeholder = "Standard terms, delivery schedules, warranty...",
            singleLine = false,
            maxLines = 3,
            enabled = !state.isImmutableError,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun NotesSection(
    state: QuotationFormUiState,
    viewModel: QuotationFormViewModel
) {
    DetailSectionCard(
        title = "Quotation Notes",
        icon = Icons.AutoMirrored.Filled.Notes
    ) {
        AppTextField(
            value = state.notes,
            onValueChange = { viewModel.onNotesChange(it) },
            label = "General Notes",
            placeholder = "Internal quotation remarks or negotiation context (Bangla/English)...",
            singleLine = false,
            maxLines = 3,
            enabled = !state.isImmutableError,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ActionButtonsSection(
    state: QuotationFormUiState,
    onBackClick: () -> Unit,
    onSave: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
    ) {
        AppOutlinedButton(
            text = "Cancel",
            onClick = onBackClick,
            modifier = Modifier.weight(1f),
            enabled = !state.isSaving
        )
        AppButton(
            text = state.saveButtonText,
            onClick = onSave,
            isLoading = state.isSaving,
            enabled = !state.isSaving && !state.isImmutableError,
            modifier = Modifier.weight(1f)
        )
    }
}
