package com.sucharu.sucharupro.ui.features.orders.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.domain.model.customer.Customer
import com.sucharu.sucharupro.ui.components.AppTextField
import com.sucharu.sucharupro.ui.theme.spacing

/**
 * Form field for selecting an existing customer from a searchable selection dialog.
 */
@Composable
fun CustomerSelectorField(
    selectedCustomerId: String,
    selectedCustomerName: String?,
    availableCustomers: List<Customer>,
    onCustomerSelected: (Customer) -> Unit,
    errorMessage: String? = null,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    var isDialogOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val displayText = if (selectedCustomerId.isNotBlank()) {
        if (!selectedCustomerName.isNullOrBlank()) {
            "$selectedCustomerName ($selectedCustomerId)"
        } else {
            selectedCustomerId
        }
    } else {
        ""
    }

    Column(modifier = modifier.fillMaxWidth()) {
        AppTextField(
            value = displayText,
            onValueChange = {},
            label = "Customer *",
            placeholder = "Select an existing customer",
            readOnly = true,
            enabled = enabled,
            errorMessage = errorMessage,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            trailingIcon = {
                TextButton(
                    onClick = { if (enabled) isDialogOpen = true },
                    enabled = enabled
                ) {
                    Text("Select")
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled) { isDialogOpen = true }
        )
    }

    if (isDialogOpen) {
        val filteredCustomers = remember(searchQuery, availableCustomers) {
            if (searchQuery.isBlank()) {
                availableCustomers
            } else {
                val q = searchQuery.trim().lowercase()
                availableCustomers.filter { c ->
                    c.displayName.lowercase().contains(q) ||
                    c.customerId.lowercase().contains(q) ||
                    c.primaryPhone.contains(q) ||
                    c.customerCode.lowercase().contains(q)
                }
            }
        }

        AlertDialog(
            onDismissRequest = {
                isDialogOpen = false
                searchQuery = ""
            },
            title = {
                Text(
                    text = "Select Customer",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
                ) {
                    AppTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = "Search name, ID, phone...",
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))

                    if (filteredCustomers.isEmpty()) {
                        Text(
                            text = "No customers found.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = MaterialTheme.spacing.medium)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 300.dp)
                        ) {
                            items(filteredCustomers, key = { it.customerId }) { customer ->
                                val isSelected = customer.customerId == selectedCustomerId

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onCustomerSelected(customer)
                                            isDialogOpen = false
                                            searchQuery = ""
                                        }
                                        .padding(vertical = MaterialTheme.spacing.small),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = customer.displayName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${customer.customerId} • ${customer.primaryPhone}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        isDialogOpen = false
                        searchQuery = ""
                    }
                ) {
                    Text("Close")
                }
            }
        )
    }
}
