package com.sucharu.sucharupro.ui.features.customer.details

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.core.validation.CustomerValidation
import com.sucharu.sucharupro.domain.model.customer.Customer
import com.sucharu.sucharupro.domain.model.customer.CustomerAddress
import com.sucharu.sucharupro.ui.components.AppButton
import com.sucharu.sucharupro.ui.components.AppCard
import com.sucharu.sucharupro.ui.components.AppOutlinedButton
import com.sucharu.sucharupro.ui.components.LoadingIndicator
import com.sucharu.sucharupro.ui.components.SectionHeader
import com.sucharu.sucharupro.ui.features.customer.components.CustomerStatusBadge
import com.sucharu.sucharupro.ui.features.customer.components.CustomerTypeBadge
import com.sucharu.sucharupro.ui.features.customer.details.components.CustomerActivitySection
import com.sucharu.sucharupro.ui.features.customer.details.components.CustomerFollowUpDialog
import com.sucharu.sucharupro.ui.features.customer.details.components.CustomerHistorySection
import com.sucharu.sucharupro.ui.features.customer.details.components.CustomerNoteDialog
import com.sucharu.sucharupro.ui.features.customer.details.components.CustomerNotesSection
import com.sucharu.sucharupro.ui.features.customer.details.components.CustomerRelationshipOverview
import com.sucharu.sucharupro.ui.theme.spacing

/**
 * Customer Details / Profile Screen.
 *
 * Displays full profile, contact info, addresses, and metadata for a selected customer.
 *
 * @param viewModel CustomerDetailsViewModel coordinating customer profile data.
 * @param onBackClick Invoked when the user taps back.
 * @param onEditClick Invoked when the user taps Edit customer.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailsScreen(
    viewModel: CustomerDetailsViewModel,
    onBackClick: () -> Unit,
    onEditClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Customer Details",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to customers"
                        )
                    }
                },
                actions = {
                    val state = uiState
                    if (state is CustomerDetailsUiState.Success) {
                        IconButton(onClick = { onEditClick(state.customer.customerId) }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Customer"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = uiState) {
                is CustomerDetailsUiState.Loading -> {
                    CustomerDetailsLoadingView(modifier = Modifier.fillMaxSize())
                }

                is CustomerDetailsUiState.Error -> {
                    CustomerDetailsErrorView(
                        errorMessage = state.errorMessage,
                        onRetry = { viewModel.retry() },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                is CustomerDetailsUiState.NotFound -> {
                    CustomerNotFoundView(
                        customerId = state.customerId,
                        onBackClick = onBackClick,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                is CustomerDetailsUiState.Success -> {
                    CustomerDetailsSuccessContent(
                        state = state,
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Note Editor Dialog
                    CustomerNoteDialog(
                        isVisible = state.isNoteDialogVisible,
                        noteText = state.noteInputText,
                        isImportant = state.isNoteImportantInput,
                        isEditing = state.editingNoteId != null,
                        errorMessage = state.noteErrorMessage,
                        isSaving = state.isNoteSaving,
                        onNoteTextChange = viewModel::onNoteTextChanged,
                        onImportanceChange = viewModel::onNoteImportanceChanged,
                        onSave = viewModel::saveNote,
                        onDismiss = viewModel::onDismissNoteDialog
                    )

                    // Follow-Up Scheduler Dialog
                    CustomerFollowUpDialog(
                        isVisible = state.isFollowUpDialogVisible,
                        followUpInput = state.followUpInput,
                        onFollowUpInputChange = viewModel::onFollowUpInputChanged,
                        onSave = viewModel::saveFollowUp,
                        onClear = viewModel::clearFollowUp,
                        onDismiss = viewModel::onDismissFollowUpDialog
                    )

                    // Lifecycle Status Confirmation Dialog
                    if (state.isStatusConfirmDialogVisible && state.pendingStatus != null) {
                        val isDeactivating = state.pendingStatus == com.sucharu.sucharupro.domain.model.customer.CustomerStatusType.INACTIVE
                        androidx.compose.material3.AlertDialog(
                            onDismissRequest = viewModel::dismissStatusConfirmDialog,
                            title = {
                                Text(
                                    text = if (isDeactivating) "Deactivate Customer?" else "Reactivate Customer?",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            text = {
                                Text(
                                    text = if (isDeactivating) {
                                        "Deactivating '${state.customer.displayName}' will mark them as Inactive. Their historical records, notes, and activity timeline will remain safely preserved and searchable."
                                    } else {
                                        "Reactivating '${state.customer.displayName}' will restore them to active trading status."
                                    },
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            },
                            confirmButton = {
                                AppButton(
                                    text = if (isDeactivating) "Deactivate" else "Reactivate",
                                    onClick = viewModel::confirmStatusChange
                                )
                            },
                            dismissButton = {
                                AppOutlinedButton(
                                    text = "Cancel",
                                    onClick = viewModel::dismissStatusConfirmDialog
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomerDetailsSuccessContent(
    state: CustomerDetailsUiState.Success,
    viewModel: CustomerDetailsViewModel,
    modifier: Modifier = Modifier
) {
    val customer = state.customer

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = MaterialTheme.spacing.screenPadding)
            .padding(vertical = MaterialTheme.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
    ) {
        // 1. Profile Header Card
        ProfileHeaderCard(customer = customer)

        // 2. Relationship Overview Foundation
        CustomerRelationshipOverview()

        // 3. Responsive Layout for Details, Notes, and Activity Sections
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val isTabletOrDesktop = maxWidth >= 600.dp

            if (isTabletOrDesktop) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
                ) {
                    // Left Column: Contact, Address & Metadata
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
                    ) {
                        ContactInfoCard(customer = customer)
                        AddressInfoCard(addresses = customer.addresses)
                        AdditionalMetadataCard(
                            customer = customer,
                            onScheduleFollowUpClick = { viewModel.onOpenFollowUpDialog() },
                            onDeactivateClick = { viewModel.deactivateCustomer() },
                            onReactivateClick = { viewModel.reactivateCustomer() }
                        )
                    }

                    // Right Column: Notes, Activities & Transactional History
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
                    ) {
                        CustomerNotesSection(
                            notes = state.notes,
                            onAddNoteClick = { viewModel.onOpenAddNoteDialog() },
                            onEditNoteClick = { viewModel.onOpenEditNoteDialog(it) },
                            onDeleteNoteClick = { viewModel.deleteNote(it) },
                            onToggleImportantClick = { viewModel.toggleNoteImportance(it) }
                        )
                        CustomerActivitySection(activities = state.activities)
                        CustomerHistorySection(customer = customer)
                    }
                }
            } else {
                // Stacked vertical single-column on mobile
                Column(
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
                ) {
                    ContactInfoCard(customer = customer)
                    AddressInfoCard(addresses = customer.addresses)
                    CustomerNotesSection(
                        notes = state.notes,
                        onAddNoteClick = { viewModel.onOpenAddNoteDialog() },
                        onEditNoteClick = { viewModel.onOpenEditNoteDialog(it) },
                        onDeleteNoteClick = { viewModel.deleteNote(it) },
                        onToggleImportantClick = { viewModel.toggleNoteImportance(it) }
                    )
                    CustomerActivitySection(activities = state.activities)
                    AdditionalMetadataCard(
                        customer = customer,
                        onScheduleFollowUpClick = { viewModel.onOpenFollowUpDialog() },
                        onDeactivateClick = { viewModel.deactivateCustomer() },
                        onReactivateClick = { viewModel.reactivateCustomer() }
                    )
                    CustomerHistorySection(customer = customer)
                }
            }
        }
    }
}

@Composable
private fun ProfileHeaderCard(
    customer: Customer,
    modifier: Modifier = Modifier
) {
    AppCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(MaterialTheme.spacing.large)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Initial Avatar
            val initials = extractInitials(customer.displayName)
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(MaterialTheme.spacing.medium))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = customer.displayName,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "Customer Code: ${customer.customerCode}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CustomerTypeBadge(type = customer.customerType)
                    CustomerStatusBadge(status = customer.status)
                }
            }
        }
    }
}

@Composable
private fun ContactInfoCard(
    customer: Customer,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    AppCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(MaterialTheme.spacing.large)
    ) {
        SectionHeader(
            title = "Contact Information",
            subtitle = "Primary communications & contact person"
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

        // 1. Primary Phone Action Row
        ContactActionRow(
            icon = Icons.Default.Phone,
            label = "Primary Phone",
            value = customer.primaryPhone,
            onCallClick = {
                val sanitized = CustomerValidation.sanitizeForDialer(customer.primaryPhone)
                if (sanitized.isNotBlank()) {
                    try {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$sanitized"))
                        context.startActivity(intent)
                    } catch (_: Exception) {
                        // Safe fallback if dialer is unavailable
                    }
                }
            },
            onCopyClick = {
                clipboardManager.setText(AnnotatedString(customer.primaryPhone))
            },
            callContentDescription = "Call primary phone: ${customer.primaryPhone}",
            copyContentDescription = "Copy primary phone to clipboard"
        )

        // 2. Alternate Phone Action Row (if present)
        val altPhone = customer.alternatePhone
        if (!altPhone.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
            ContactActionRow(
                icon = Icons.Default.Phone,
                label = "Alternate Phone",
                value = altPhone,
                onCallClick = {
                    val sanitized = CustomerValidation.sanitizeForDialer(altPhone)
                    if (sanitized.isNotBlank()) {
                        try {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$sanitized"))
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            // Safe fallback if dialer is unavailable
                        }
                    }
                },
                onCopyClick = {
                    clipboardManager.setText(AnnotatedString(altPhone))
                },
                callContentDescription = "Call alternate phone: $altPhone",
                copyContentDescription = "Copy alternate phone to clipboard"
            )
        }

        // 3. Email Address Action Row or Clean Empty State
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
        val email = customer.email
        if (!email.isNullOrBlank()) {
            ContactActionRow(
                icon = Icons.Default.Email,
                label = "Email Address",
                value = email,
                onEmailClick = {
                    try {
                        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email"))
                        context.startActivity(intent)
                    } catch (_: Exception) {
                        // Safe fallback if mail client is unavailable
                    }
                },
                onCopyClick = {
                    clipboardManager.setText(AnnotatedString(email))
                },
                emailContentDescription = "Send email to $email",
                copyContentDescription = "Copy email to clipboard"
            )
        } else {
            DetailInfoRow(
                icon = Icons.Default.Email,
                label = "Email Address",
                value = "Not provided",
                isPlaceholder = true
            )
        }

        // 4. Contact Person (if present)
        val cpName = customer.contactPersonName
        if (!cpName.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
            DetailInfoRow(
                icon = Icons.Default.Person,
                label = "Contact Person",
                value = cpName
            )
        }
    }
}

@Composable
private fun ContactActionRow(
    icon: ImageVector,
    label: String,
    value: String,
    onCallClick: (() -> Unit)? = null,
    onEmailClick: (() -> Unit)? = null,
    onCopyClick: (() -> Unit)? = null,
    callContentDescription: String = "Call",
    emailContentDescription: String = "Email",
    copyContentDescription: String = "Copy",
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )

        Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            if (onCallClick != null) {
                IconButton(
                    onClick = onCallClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = callContentDescription,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (onEmailClick != null) {
                IconButton(
                    onClick = onEmailClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = emailContentDescription,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (onCopyClick != null) {
                IconButton(
                    onClick = onCopyClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = copyContentDescription,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AddressInfoCard(
    addresses: List<CustomerAddress>,
    modifier: Modifier = Modifier
) {
    AppCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(MaterialTheme.spacing.large)
    ) {
        SectionHeader(
            title = "Addresses",
            subtitle = if (addresses.isEmpty()) "No registered locations" else "${addresses.size} registered locations"
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

        if (addresses.isEmpty()) {
            Text(
                text = "No address recorded for this customer.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
                addresses.forEach { address ->
                    AddressItemCard(address = address)
                }
            }
        }
    }
}

@Composable
private fun AddressItemCard(
    address: CustomerAddress,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    val formattedAddr = address.formatted()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(MaterialTheme.spacing.medium)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )

            Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = address.addressType.defaultLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    if (address.isDefault) {
                        Text(
                            text = "Default",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = address.addressLine,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )

                val details = listOf(address.area, address.city, address.district, address.postalCode, address.country)
                    .filter { it.isNotBlank() }
                    .joinToString(", ")

                if (details.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = details,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(
                onClick = {
                    clipboardManager.setText(AnnotatedString(formattedAddr))
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy address to clipboard",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun AdditionalMetadataCard(
    customer: Customer,
    onScheduleFollowUpClick: () -> Unit,
    onDeactivateClick: () -> Unit,
    onReactivateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AppCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(MaterialTheme.spacing.large)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionHeader(
                title = "Profile & Operations",
                subtitle = "Lifecycle metadata and follow-up target"
            )

            AppOutlinedButton(
                text = if (customer.nextFollowUpAt != null) "Edit Follow-up" else "Schedule Follow-up",
                onClick = onScheduleFollowUpClick
            )
        }

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

        DetailInfoRow(
            icon = Icons.Default.CalendarMonth,
            label = "Registration Date",
            value = customer.createdAt.replace("T", " ").replace("Z", "")
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

        DetailInfoRow(
            icon = Icons.Default.CalendarMonth,
            label = "Last Updated",
            value = customer.updatedAt.replace("T", " ").replace("Z", "")
        )

        val lastAct = customer.lastActivityAt
        if (!lastAct.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
            DetailInfoRow(
                icon = Icons.Default.CalendarMonth,
                label = "Last Operational Activity",
                value = lastAct.replace("T", " ").replace("Z", "")
            )
        }

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

        DetailInfoRow(
            icon = Icons.Default.CalendarMonth,
            label = "Follow-up Target",
            value = customer.nextFollowUpAt ?: "No upcoming follow-up scheduled",
            isPlaceholder = customer.nextFollowUpAt == null
        )

        val notes = customer.notes
        if (!notes.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
            DetailInfoRow(
                icon = Icons.Default.Description,
                label = "Initial Onboarding Notes",
                value = notes
            )
        }

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

        // Safe Lifecycle Account Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            if (customer.status == com.sucharu.sucharupro.domain.model.customer.CustomerStatusType.ACTIVE) {
                AppOutlinedButton(
                    text = "Deactivate Customer",
                    onClick = onDeactivateClick
                )
            } else {
                AppButton(
                    text = "Reactivate Customer",
                    onClick = onReactivateClick
                )
            }
        }
    }
}

@Composable
private fun DetailInfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    isPlaceholder: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )

        Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isPlaceholder) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (isPlaceholder) FontWeight.Normal else FontWeight.Medium
            )
        }
    }
}

@Composable
private fun CustomerDetailsLoadingView(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        LoadingIndicator(
            message = "Loading customer profile...",
            size = 48.dp
        )
    }
}

@Composable
private fun CustomerNotFoundView(
    customerId: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(MaterialTheme.spacing.large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AppCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(MaterialTheme.spacing.xxLarge)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PersonOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
                Text(
                    text = "Customer Not Found",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
                Text(
                    text = "No customer record exists with ID '$customerId'.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))
                AppOutlinedButton(
                    text = "Back to Customer List",
                    onClick = onBackClick
                )
            }
        }
    }
}

@Composable
private fun CustomerDetailsErrorView(
    errorMessage: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(MaterialTheme.spacing.large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AppCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(MaterialTheme.spacing.large)
        ) {
            Text(
                text = "Unable to load customer profile",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))
            AppButton(
                text = "Try Again",
                onClick = onRetry
            )
        }
    }
}

private fun extractInitials(name: String): String {
    val words = name.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
    return when {
        words.isEmpty() -> "C"
        words.size == 1 -> words[0].take(2).uppercase()
        else -> "${words[0].first()}${words[1].first()}".uppercase()
    }
}
