package com.sucharu.sucharupro.ui.features.customer.form

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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.domain.model.customer.CustomerStatusType
import com.sucharu.sucharupro.domain.model.customer.CustomerType
import com.sucharu.sucharupro.ui.components.AppButton
import com.sucharu.sucharupro.ui.components.AppCard
import com.sucharu.sucharupro.ui.components.AppOutlinedButton
import com.sucharu.sucharupro.ui.components.AppTextField
import com.sucharu.sucharupro.ui.components.LoadingIndicator
import com.sucharu.sucharupro.ui.components.SectionHeader
import com.sucharu.sucharupro.ui.theme.spacing

/**
 * Customer Form Screen supporting both CREATE and EDIT modes.
 *
 * @param viewModel CustomerFormViewModel managing form state and repository operations.
 * @param onBackClick Invoked when the user navigates back.
 * @param onSaveSuccess Invoked with the customerId upon successful save.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerFormScreen(
    viewModel: CustomerFormViewModel,
    onBackClick: () -> Unit,
    onSaveSuccess: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val formState by viewModel.formState.collectAsState()

    val screenTitle = if (formState.isEditMode) "Edit Customer" else "New Customer"
    val saveButtonText = if (formState.isEditMode) "Save Changes" else "Create Customer"

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = screenTitle,
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
        if (formState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                LoadingIndicator(message = "Loading customer data...", size = 48.dp)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = MaterialTheme.spacing.screenPadding)
                    .padding(vertical = MaterialTheme.spacing.medium),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
            ) {
                // Error Banner if present
                if (!formState.errorMessage.isNullOrBlank()) {
                    AppCard(
                        modifier = Modifier.fillMaxWidth(),
                        colors = androidx.compose.material3.CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        contentPadding = PaddingValues(MaterialTheme.spacing.medium)
                    ) {
                        Text(
                            text = formState.errorMessage.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }

                // Duplicate Customer Warning Banner
                if (!formState.duplicateWarning.isNullOrBlank()) {
                    AppCard(
                        modifier = Modifier.fillMaxWidth(),
                        colors = androidx.compose.material3.CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        ),
                        contentPadding = PaddingValues(MaterialTheme.spacing.large)
                    ) {
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
                        ) {
                            Icon(
                                imageVector = Icons.Default.WarningAmber,
                                contentDescription = "Duplicate Warning",
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(24.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Possible Duplicate Detected",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))
                                Text(
                                    text = formState.duplicateWarning.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
                                ) {
                                    AppButton(
                                        text = "Proceed Anyway",
                                        onClick = { viewModel.acknowledgeDuplicateAndSave(onSaveSuccess) }
                                    )
                                    AppOutlinedButton(
                                        text = "Dismiss",
                                        onClick = { viewModel.dismissDuplicateWarning() }
                                    )
                                }
                            }
                        }
                    }
                }

                // 1. Basic Information Card
                BasicInfoSection(
                    formState = formState,
                    onDisplayNameChange = viewModel::onDisplayNameChange,
                    onCustomerTypeChange = viewModel::onCustomerTypeChange,
                    onStatusChange = viewModel::onStatusChange
                )

                // 2. Contact Information Card
                ContactInfoSection(
                    formState = formState,
                    onPrimaryPhoneChange = viewModel::onPrimaryPhoneChange,
                    onAlternatePhoneChange = viewModel::onAlternatePhoneChange,
                    onEmailChange = viewModel::onEmailChange,
                    onContactPersonChange = viewModel::onContactPersonChange
                )

                // 3. Primary Address Card
                AddressSection(
                    formState = formState,
                    onAddressLineChange = viewModel::onAddressLineChange,
                    onAreaChange = viewModel::onAreaChange,
                    onCityChange = viewModel::onCityChange,
                    onDistrictChange = viewModel::onDistrictChange,
                    onPostalCodeChange = viewModel::onPostalCodeChange,
                    onCountryChange = viewModel::onCountryChange
                )

                // 4. Notes Section
                NotesSection(
                    notes = formState.notes,
                    onNotesChange = viewModel::onNotesChange
                )

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
                ) {
                    AppOutlinedButton(
                        text = "Cancel",
                        onClick = onBackClick,
                        modifier = Modifier.weight(1f),
                        enabled = !formState.isSaving
                    )
                    AppButton(
                        text = saveButtonText,
                        onClick = { viewModel.saveCustomer(onSaveSuccess) },
                        modifier = Modifier.weight(1.5f),
                        isLoading = formState.isSaving,
                        enabled = !formState.isSaving
                    )
                }

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BasicInfoSection(
    formState: CustomerFormState,
    onDisplayNameChange: (String) -> Unit,
    onCustomerTypeChange: (CustomerType) -> Unit,
    onStatusChange: (CustomerStatusType) -> Unit,
    modifier: Modifier = Modifier
) {
    AppCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(MaterialTheme.spacing.large)
    ) {
        SectionHeader(
            title = "Customer Information",
            subtitle = "Identity and classification"
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

        // Display Name (Required)
        AppTextField(
            value = formState.displayName,
            onValueChange = onDisplayNameChange,
            label = "Customer Name *",
            placeholder = "e.g., Bengal Publications Ltd. or মো: আব্দুল্লাহ",
            errorMessage = formState.displayNameError,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            )
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

        // Customer Type Selector
        Text(
            text = "Customer Type *",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall)
        ) {
            CustomerType.entries.forEach { type ->
                val selected = formState.customerType == type
                FilterChip(
                    selected = selected,
                    onClick = { onCustomerTypeChange(type) },
                    label = {
                        Text(
                            text = type.defaultLabel,
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    leadingIcon = if (selected) {
                        {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }

        // Status Selector (In EDIT mode)
        if (formState.isEditMode) {
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
            Text(
                text = "Operational Status",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall)
            ) {
                CustomerStatusType.entries.forEach { status ->
                    val selected = formState.status == status
                    FilterChip(
                        selected = selected,
                        onClick = { onStatusChange(status) },
                        label = {
                            Text(
                                text = status.defaultLabel,
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        leadingIcon = if (selected) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun ContactInfoSection(
    formState: CustomerFormState,
    onPrimaryPhoneChange: (String) -> Unit,
    onAlternatePhoneChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onContactPersonChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    AppCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(MaterialTheme.spacing.large)
    ) {
        SectionHeader(
            title = "Contact Information",
            subtitle = "Phone numbers & communication channels"
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val isTabletOrDesktop = maxWidth >= 600.dp

            if (isTabletOrDesktop) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
                ) {
                    AppTextField(
                        value = formState.primaryPhone,
                        onValueChange = onPrimaryPhoneChange,
                        label = "Primary Phone *",
                        placeholder = "+880 1711-234567",
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        errorMessage = formState.primaryPhoneError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                        modifier = Modifier.weight(1f)
                    )
                    AppTextField(
                        value = formState.alternatePhone,
                        onValueChange = onAlternatePhoneChange,
                        label = "Alternate Phone",
                        placeholder = "Optional secondary number",
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        errorMessage = formState.alternatePhoneError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
                ) {
                    AppTextField(
                        value = formState.email,
                        onValueChange = onEmailChange,
                        label = "Email Address",
                        placeholder = "contact@example.com",
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        errorMessage = formState.emailError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                        modifier = Modifier.weight(1f)
                    )
                    AppTextField(
                        value = formState.contactPersonName,
                        onValueChange = onContactPersonChange,
                        label = "Contact Person",
                        placeholder = "e.g., Tanvir Ahmed (Manager)",
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)) {
                    AppTextField(
                        value = formState.primaryPhone,
                        onValueChange = onPrimaryPhoneChange,
                        label = "Primary Phone *",
                        placeholder = "+880 1711-234567",
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        errorMessage = formState.primaryPhoneError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next)
                    )
                    AppTextField(
                        value = formState.alternatePhone,
                        onValueChange = onAlternatePhoneChange,
                        label = "Alternate Phone",
                        placeholder = "Optional secondary number",
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        errorMessage = formState.alternatePhoneError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next)
                    )
                    AppTextField(
                        value = formState.email,
                        onValueChange = onEmailChange,
                        label = "Email Address",
                        placeholder = "contact@example.com",
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        errorMessage = formState.emailError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next)
                    )
                    AppTextField(
                        value = formState.contactPersonName,
                        onValueChange = onContactPersonChange,
                        label = "Contact Person",
                        placeholder = "e.g., Tanvir Ahmed (Manager)",
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next)
                    )
                }
            }
        }
    }
}

@Composable
private fun AddressSection(
    formState: CustomerFormState,
    onAddressLineChange: (String) -> Unit,
    onAreaChange: (String) -> Unit,
    onCityChange: (String) -> Unit,
    onDistrictChange: (String) -> Unit,
    onPostalCodeChange: (String) -> Unit,
    onCountryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    AppCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(MaterialTheme.spacing.large)
    ) {
        SectionHeader(
            title = "Primary Address",
            subtitle = "Physical and delivery location"
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

        AppTextField(
            value = formState.addressLine,
            onValueChange = onAddressLineChange,
            label = "Address Line",
            placeholder = "House/Plot, Road/Lane, Holding No.",
            singleLine = false,
            maxLines = 3,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next)
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val isTabletOrDesktop = maxWidth >= 600.dp

            if (isTabletOrDesktop) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
                ) {
                    AppTextField(
                        value = formState.area,
                        onValueChange = onAreaChange,
                        label = "Area / Thana",
                        placeholder = "e.g., Motijheel / Uttara",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                        modifier = Modifier.weight(1f)
                    )
                    AppTextField(
                        value = formState.city,
                        onValueChange = onCityChange,
                        label = "City / District",
                        placeholder = "e.g., Dhaka",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
                ) {
                    AppTextField(
                        value = formState.postalCode,
                        onValueChange = onPostalCodeChange,
                        label = "Postal Code",
                        placeholder = "e.g., 1000",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                        modifier = Modifier.weight(1f)
                    )
                    AppTextField(
                        value = formState.country,
                        onValueChange = onCountryChange,
                        label = "Country",
                        placeholder = "Bangladesh",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
                    ) {
                        AppTextField(
                            value = formState.area,
                            onValueChange = onAreaChange,
                            label = "Area / Thana",
                            placeholder = "Motijheel",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                            modifier = Modifier.weight(1f)
                        )
                        AppTextField(
                            value = formState.city,
                            onValueChange = onCityChange,
                            label = "City",
                            placeholder = "Dhaka",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
                    ) {
                        AppTextField(
                            value = formState.postalCode,
                            onValueChange = onPostalCodeChange,
                            label = "Postal Code",
                            placeholder = "1000",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                            modifier = Modifier.weight(1f)
                        )
                        AppTextField(
                            value = formState.country,
                            onValueChange = onCountryChange,
                            label = "Country",
                            placeholder = "Bangladesh",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotesSection(
    notes: String,
    onNotesChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    AppCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(MaterialTheme.spacing.large)
    ) {
        SectionHeader(
            title = "Internal Notes",
            subtitle = "Special printing preferences or remarks"
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

        AppTextField(
            value = notes,
            onValueChange = onNotesChange,
            label = "Notes (Optional)",
            placeholder = "e.g., Preferred paper type, discount agreements, special delivery notes...",
            singleLine = false,
            maxLines = 4,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done)
        )
    }
}
