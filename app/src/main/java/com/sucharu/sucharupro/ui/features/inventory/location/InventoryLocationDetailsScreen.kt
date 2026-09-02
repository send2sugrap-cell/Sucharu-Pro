package com.sucharu.sucharupro.ui.features.inventory.location

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
import com.sucharu.sucharupro.domain.model.inventory.InventoryLocationStatus

/**
 * Screen displaying complete details for a storage location and its sub-locations (Module 07 Step 02).
 */
@Composable
fun InventoryLocationDetailsScreen(
    viewModel: InventoryLocationDetailsViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Location Details",
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
        } else if (uiState.location == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = uiState.errorMessage ?: "Location not found.",
                    color = MaterialTheme.colorScheme.error
                )
            }
        } else {
            val location = uiState.location!!
            val statusColor = when (location.status) {
                InventoryLocationStatus.ACTIVE -> Color(0xFF2E7D32)
                InventoryLocationStatus.INACTIVE -> Color(0xFFF57F17)
                InventoryLocationStatus.ARCHIVED -> Color(0xFF757575)
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
                            text = location.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = location.status.defaultLabel.uppercase(),
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(text = "Code: ${location.code}", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
                    Text(text = "Type: ${location.type.defaultLabel}", fontSize = 14.sp)
                    Text(text = "Warehouse ID: ${location.warehouseId}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    if (uiState.parentLocation != null) {
                        Text(text = "Parent Location: ${uiState.parentLocation!!.name} (${uiState.parentLocation!!.code})", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    if (location.capacity != null) {
                        Text(text = "Capacity: ${location.capacity} ${location.capacityUnit ?: ""}", fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary)
                    }

                    if (!location.description.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "Description: ${location.description}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Created: ${location.createdAt} by ${location.createdBy}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = "Last Updated: ${location.updatedAt}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                    if (location.archivedAt != null) {
                        Text(
                            text = "Archived At: ${location.archivedAt}",
                            fontSize = 11.sp,
                            color = Color(0xFFC62828)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (!location.isTerminal) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (location.status == InventoryLocationStatus.ACTIVE) {
                        Button(
                            onClick = { viewModel.deactivateLocation("admin-01", "2026-08-17T12:00:00Z") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF57F17)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Deactivate")
                        }
                    } else if (location.status == InventoryLocationStatus.INACTIVE) {
                        Button(
                            onClick = { viewModel.activateLocation("admin-01", "2026-08-17T12:00:00Z") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Activate")
                        }
                    }

                    Button(
                        onClick = { viewModel.archiveLocation("admin-01", "2026-08-17T12:00:00Z") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Archive")
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Sub-Locations / Child Nodes (${uiState.childLocations.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (uiState.childLocations.isEmpty()) {
                Text(
                    text = "No child storage locations registered under this location.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(uiState.childLocations) { child ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = child.name, fontWeight = FontWeight.Bold)
                                    Text(text = child.status.defaultLabel, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                }
                                Text(text = "Code: ${child.code} | Type: ${child.type.defaultLabel}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}
