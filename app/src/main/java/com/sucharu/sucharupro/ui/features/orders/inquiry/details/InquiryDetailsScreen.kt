package com.sucharu.sucharupro.ui.features.orders.inquiry.details

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.domain.model.order.Inquiry
import com.sucharu.sucharupro.domain.model.activity.CommercialEntityType
import com.sucharu.sucharupro.domain.repository.CommercialActivityRepository
import com.sucharu.sucharupro.ui.components.AppButton
import com.sucharu.sucharupro.ui.components.AppCard
import com.sucharu.sucharupro.ui.components.AppOutlinedButton
import com.sucharu.sucharupro.ui.components.LoadingIndicator
import com.sucharu.sucharupro.ui.features.orders.components.CommercialActivityTimeline
import com.sucharu.sucharupro.ui.features.orders.components.DetailSectionCard
import com.sucharu.sucharupro.ui.features.orders.inquiry.components.InquiryStatusBadge
import com.sucharu.sucharupro.ui.features.orders.inquiry.details.components.InquiryRequirementsSection
import com.sucharu.sucharupro.ui.theme.spacing

/**
 * Customer Inquiry Details / Profile Screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InquiryDetailsScreen(
    viewModel: InquiryDetailsViewModel,
    onBackClick: () -> Unit,
    onEditClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Inquiry Details",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to inquiries"
                        )
                    }
                },
                actions = {
                    val currentState = uiState
                    if (currentState is InquiryDetailsUiState.Success) {
                        IconButton(onClick = { onEditClick(currentState.inquiry.inquiryId) }) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Edit,
                                contentDescription = "Edit Inquiry"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = uiState) {
                is InquiryDetailsUiState.Loading -> {
                    InquiryDetailsLoadingView(modifier = Modifier.fillMaxSize())
                }

                is InquiryDetailsUiState.NotFound -> {
                    InquiryNotFoundView(
                        inquiryId = state.inquiryId,
                        onBackClick = onBackClick,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                is InquiryDetailsUiState.Error -> {
                    InquiryDetailsErrorView(
                        errorMessage = state.errorMessage,
                        onRetry = { viewModel.retry() },
                        onBackClick = onBackClick,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                is InquiryDetailsUiState.Success -> {
                    InquiryDetailsContent(
                        inquiry = state.inquiry,
                        activityRepository = viewModel.activityRepository,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun InquiryDetailsContent(
    inquiry: Inquiry,
    activityRepository: CommercialActivityRepository,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        val isTabletOrDesktop = maxWidth >= 600.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(MaterialTheme.spacing.screenPadding),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
        ) {
            if (isTabletOrDesktop) {
                // Tablet/Desktop: 2-column top section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
                ) {
                    InquiryHeaderCard(
                        inquiry = inquiry,
                        modifier = Modifier.weight(1f)
                    )
                    InquiryCustomerCard(
                        inquiry = inquiry,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                // Phone: Stacked
                InquiryHeaderCard(inquiry = inquiry)
                InquiryCustomerCard(inquiry = inquiry)
            }

            // Requirements Section
            DetailSectionCard(
                title = "Inquiry Requirements (${inquiry.totalItemsCount} items, ${inquiry.totalQuantity} total units)",
                icon = Icons.Default.Description
            ) {
                InquiryRequirementsSection(items = inquiry.items)
            }

            // Notes Section if present
            val notes = inquiry.notes
            if (!notes.isNullOrBlank()) {
                DetailSectionCard(
                    title = "Inquiry Notes & Instructions",
                    icon = Icons.Default.Notes
                ) {
                    Text(
                        text = notes,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // ── Activity Timeline ──
            DetailSectionCard(
                title = "Activity History",
                icon = Icons.Default.History
            ) {
                CommercialActivityTimeline(
                    activityRepository = activityRepository,
                    entityType = CommercialEntityType.INQUIRY,
                    entityId = inquiry.inquiryId
                )
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))
        }
    }
}

@Composable
private fun InquiryHeaderCard(
    inquiry: Inquiry,
    modifier: Modifier = Modifier
) {
    DetailSectionCard(
        title = inquiry.inquiryNumber,
        icon = Icons.Default.Assignment,
        trailingContent = {
            InquiryStatusBadge(status = inquiry.status)
        },
        modifier = modifier
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Inquiry ID",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = inquiry.inquiryId,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Created Date",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = inquiry.createdAt.take(10),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Last Updated",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = inquiry.updatedAt.take(10),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun InquiryCustomerCard(
    inquiry: Inquiry,
    modifier: Modifier = Modifier
) {
    DetailSectionCard(
        title = "Customer Reference",
        icon = Icons.Default.Person,
        modifier = modifier
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Customer ID",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = inquiry.customerId,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            val contactPerson = inquiry.contactPerson
            if (!contactPerson.isNullOrBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Contact Person",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = contactPerson,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            val contactPhone = inquiry.contactPhone
            if (!contactPhone.isNullOrBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Contact Phone",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = contactPhone,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Inquiry Source",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = inquiry.source.defaultLabel,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun InquiryDetailsLoadingView(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        LoadingIndicator(
            message = "Loading inquiry details...",
            size = 48.dp
        )
    }
}

@Composable
private fun InquiryNotFoundView(
    inquiryId: String,
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
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
                Text(
                    text = "Inquiry Not Found",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
                Text(
                    text = "No customer inquiry exists with ID: $inquiryId",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))
                AppButton(
                    text = "Back to Inquiries",
                    onClick = onBackClick
                )
            }
        }
    }
}

@Composable
private fun InquiryDetailsErrorView(
    errorMessage: String,
    onRetry: () -> Unit,
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
            contentPadding = PaddingValues(MaterialTheme.spacing.large)
        ) {
            Text(
                text = "Failed to load inquiry",
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
            Row(
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
            ) {
                AppOutlinedButton(
                    text = "Back",
                    onClick = onBackClick,
                    modifier = Modifier.weight(1f)
                )
                AppButton(
                    text = "Retry",
                    onClick = onRetry,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
