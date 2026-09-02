package com.sucharu.sucharupro.ui.features.inventory.warehouse

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.domain.model.inventory.InventoryWarehouseStatus

/**
 * Screen displaying complete warehouse details and its storage locations (Module 07 Step 02).
 */
@Composable
fun InventoryWarehouseDetailsScreen(
    viewModel: InventoryWarehouseDetailsViewModel,
    onNavigateBack: () -> Unit = {},
    onLocationClick: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Back Button & Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Warehouse Details",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            OutlinedButton(onClick = onNavigateBack) {
                Text("Back")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.warehouse == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = uiState.errorMessage ?: "Warehouse not found.",
                    color = MaterialTheme.colorScheme.error
                )
            }
        } else {
            val warehouse = uiState.warehouse!!
            val statusColor = when (warehouse.status) {
                InventoryWarehouseStatus.ACTIVE -> Color(0xFF2E7D32)
                InventoryWarehouseStatus.INACTIVE -> Color(0xFFF57F17)
                InventoryWarehouseStatus.ARCHIVED -> Color(0xFF757575)
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = warehouse.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = warehouse.status.defaultLabel.uppercase(),
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(text = "Code: ${warehouse.code}", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
                    Text(text = "Type: ${warehouse.type.defaultLabel}", fontSize = 14.sp)
                    Text(text = "Project ID: ${warehouse.projectId}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    if (!warehouse.address.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "Address:\n${warehouse.address}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    if (!warehouse.contactPerson.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "Contact: ${warehouse.contactPerson} (${warehouse.contactPhone ?: "N/A"})", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    if (!warehouse.description.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "Description: ${warehouse.description}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Created: ${warehouse.createdAt} by ${warehouse.createdBy}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = "Last Updated: ${warehouse.updatedAt}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                    if (warehouse.archivedAt != null) {
                        Text(
                            text = "Archived At: ${warehouse.archivedAt}",
                            fontSize = 11.sp,
                            color = Color(0xFFC62828)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            if (!warehouse.isTerminal) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (warehouse.status == InventoryWarehouseStatus.ACTIVE) {
                        Button(
                            onClick = { viewModel.deactivateWarehouse("admin-01", "2026-08-17T12:00:00Z") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF57F17)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Deactivate")
                        }
                    } else if (warehouse.status == InventoryWarehouseStatus.INACTIVE) {
                        Button(
                            onClick = { viewModel.activateWarehouse("admin-01", "2026-08-17T12:00:00Z") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Activate")
                        }
                    }

                    Button(
                        onClick = { viewModel.archiveWarehouse("admin-01", "2026-08-17T12:00:00Z") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Archive")
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Storage Locations (${uiState.locations.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (uiState.locations.isEmpty()) {
                Text(
                    text = "No storage locations configured in this warehouse.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(uiState.locations) { loc ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = loc.name, fontWeight = FontWeight.Bold)
                                    Text(text = loc.status.defaultLabel, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                }
                                Text(text = "Code: ${loc.code} | Type: ${loc.type.defaultLabel}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}
