package com.sucharu.sucharupro.ui.features.orders.inquiry.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import com.sucharu.sucharupro.domain.model.order.InquiryRequirement
import com.sucharu.sucharupro.domain.model.order.InquirySource
import com.sucharu.sucharupro.ui.components.AppButton
import com.sucharu.sucharupro.ui.components.AppCard
import com.sucharu.sucharupro.ui.components.AppOutlinedButton
import com.sucharu.sucharupro.ui.components.AppTextField
import com.sucharu.sucharupro.ui.components.LoadingIndicator
import com.sucharu.sucharupro.ui.features.orders.components.CustomerSelectorField
import com.sucharu.sucharupro.ui.features.orders.components.DetailSectionCard
import com.sucharu.sucharupro.ui.features.orders.inquiry.form.components.InquiryRequirementEditorDialog
import com.sucharu.sucharupro.ui.theme.spacing

/**
 * Screen providing Form entry for Creating and Editing Customer Inquiries in Sucharu Pro.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun InquiryFormScreen(
    viewModel: InquiryFormViewModel,
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
                LoadingIndicator(message = "Loading inquiry data...", size = 48.dp)
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
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            ),
                            contentPadding = PaddingValues(MaterialTheme.spacing.medium)
                        ) {
                            Text(
                                text = state.errorMessage.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium
                            )
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
                                CustomerSection(
                                    state = state,
                                    viewModel = viewModel
                                )
                                ContactDetailsSection(
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
                                RequirementsSection(
                                    state = state,
                                    viewModel = viewModel
                                )
                                ActionButtonsSection(
                                    state = state,
                                    onBackClick = onBackClick,
                                    onSave = { viewModel.saveInquiry(onSaveSuccess) }
                                )
                            }
                        }
                    } else {
                        CustomerSection(
                            state = state,
                            viewModel = viewModel
                        )
                        ContactDetailsSection(
                            state = state,
                            viewModel = viewModel
                        )
                        RequirementsSection(
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
                            onSave = { viewModel.saveInquiry(onSaveSuccess) }
                        )
                    }
                }
            }
        }
    }

    // Modal requirement dialog
    if (state.isItemDialogOpen) {
        InquiryRequirementEditorDialog(
            initialItem = state.editingItem,
            onDismiss = { viewModel.closeItemDialog() },
            onSaveItem = { viewModel.saveItem(it) }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CustomerSection(
    state: InquiryFormUiState,
    viewModel: InquiryFormViewModel
) {
    DetailSectionCard(
        title = "Customer & Channel",
        icon = Icons.Default.Person
    ) {
        CustomerSelectorField(
            selectedCustomerId = state.customerId,
            selectedCustomerName = state.customerName,
            availableCustomers = state.availableCustomers,
            onCustomerSelected = { viewModel.onCustomerSelected(it) },
            errorMessage = state.customerIdError
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

        Text(
            text = "Inquiry Channel / Source",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall)
        ) {
            InquirySource.values().forEach { src ->
                FilterChip(
                    selected = state.source == src,
                    onClick = { viewModel.onSourceChange(src) },
                    label = { Text(src.defaultLabel) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }
    }
}

@Composable
private fun ContactDetailsSection(
    state: InquiryFormUiState,
    viewModel: InquiryFormViewModel
) {
    DetailSectionCard(
        title = "Contact Information",
        icon = Icons.Default.Phone
    ) {
        AppTextField(
            value = state.contactPerson,
            onValueChange = { viewModel.onContactPersonChange(it) },
            label = "Contact Person",
            placeholder = "e.g. Mr. Masud",
            singleLine = true
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

        AppTextField(
            value = state.contactPhone,
            onValueChange = { viewModel.onContactPhoneChange(it) },
            label = "Contact Phone",
            placeholder = "01XXXXXXXXX",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true
        )
    }
}

@Composable
private fun RequirementsSection(
    state: InquiryFormUiState,
    viewModel: InquiryFormViewModel
) {
    DetailSectionCard(
        title = "Specification Requirements (${state.items.size})",
        icon = Icons.Default.Layers,
        trailingContent = {
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
                    text = "No specification requirements added yet. Click 'Add Item' to specify print requirements.",
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
                    InquiryItemRow(
                        index = index,
                        item = item,
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
private fun InquiryItemRow(
    index: Int,
    item: InquiryRequirement,
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
                text = "${index + 1}. ${item.productName}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = item.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Qty: ${item.quantity} ${item.unit}" +
                    (if (!item.size.isNullOrBlank()) " • Size: ${item.size}" else "") +
                    (if (!item.paperMaterial.isNullOrBlank()) " • Paper: ${item.paperMaterial}" else "") +
                    (if (item.gsm != null) " • ${item.gsm} GSM" else ""),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }

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

@Composable
private fun NotesSection(
    state: InquiryFormUiState,
    viewModel: InquiryFormViewModel
) {
    DetailSectionCard(
        title = "Inquiry Notes",
        icon = Icons.AutoMirrored.Filled.Notes
    ) {
        AppTextField(
            value = state.notes,
            onValueChange = { viewModel.onNotesChange(it) },
            label = "General Notes",
            placeholder = "Enter any customer instructions or commercial notes (Bangla/English supported)...",
            singleLine = false,
            maxLines = 4,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ActionButtonsSection(
    state: InquiryFormUiState,
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
            enabled = !state.isSaving,
            modifier = Modifier.weight(1f)
        )
    }
}
