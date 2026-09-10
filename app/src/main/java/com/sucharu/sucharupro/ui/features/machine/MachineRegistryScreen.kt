package com.sucharu.sucharupro.ui.features.machine

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sucharu.sucharupro.domain.machine.MachineEquipment
import com.sucharu.sucharupro.domain.machine.MachineOwnershipType
import com.sucharu.sucharupro.domain.machine.MachineStatus
import com.sucharu.sucharupro.domain.machine.MachineType
import com.sucharu.sucharupro.ui.components.AppButton
import com.sucharu.sucharupro.ui.components.AppCard
import com.sucharu.sucharupro.ui.components.AppOutlinedButton
import com.sucharu.sucharupro.ui.components.AppTextField
import com.sucharu.sucharupro.ui.components.SectionHeader
import com.sucharu.sucharupro.ui.components.StatusBadge

/**
 * Machine Registry & Equipment Foundation Screen (Module 21 Step 01).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MachineRegistryScreen(
    viewModel: MachineRegistryViewModel,
    onBackClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showRegisterDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "EQUIPMENT REGISTRY",
                            color = Color(0xFF9ECAFF),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Module 21 • Machine Master Identity & Status",
                            color = Color(0xFFB7C8D8),
                            fontSize = 11.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showRegisterDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Register Machine",
                            tint = Color(0xFF9ECAFF)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0B132B))
            )
        },
        containerColor = Color(0xFF0B132B),
        modifier = modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Search Input
            AppTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                label = "Search by Name, Code or Manufacturer",
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color(0xFF9ECAFF)
                    )
                },
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            )

            // Filter Row by Type
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                item {
                    FilterChip(
                        selected = uiState.selectedTypeFilter == null,
                        onClick = { viewModel.onTypeFilterChanged(null) },
                        label = { Text("All Types") }
                    )
                }
                items(MachineType.entries) { type ->
                    FilterChip(
                        selected = uiState.selectedTypeFilter == type,
                        onClick = { viewModel.onTypeFilterChanged(type) },
                        label = { Text(type.name.replace("_", " ")) }
                    )
                }
            }

            // Error & Success Banners
            if (uiState.errorMessage != null) {
                Surface(
                    color = Color(0xFF93000A),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Text(
                        text = uiState.errorMessage!!,
                        color = Color(0xFFFFDAD6),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            if (uiState.successMessage != null) {
                Surface(
                    color = Color(0xFF00511A),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Text(
                        text = uiState.successMessage!!,
                        color = Color(0xFFB5F2BD),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Loading / List Content
            if (uiState.isLoading && uiState.machines.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF9ECAFF))
                }
            } else if (uiState.filteredMachines.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No machine records found.",
                        color = Color(0xFFB7C8D8),
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.filteredMachines, key = { it.machineId }) { machine ->
                        MachineCardItem(
                            machine = machine,
                            onStatusChange = { newStatus ->
                                viewModel.changeMachineStatus(machine.machineId, newStatus)
                            }
                        )
                    }
                }
            }
        }
    }

    if (showRegisterDialog) {
        RegisterMachineDialog(
            onDismiss = { showRegisterDialog = false },
            onSubmit = { newMachine ->
                viewModel.registerMachine(newMachine)
                showRegisterDialog = false
            }
        )
    }
}

@Composable
fun MachineCardItem(
    machine: MachineEquipment,
    onStatusChange: (MachineStatus) -> Unit
) {
    var showStatusDialog by remember { mutableStateOf(false) }

    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = machine.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Asset Code: ${machine.assetCode} • Type: ${machine.type.name}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF9ECAFF)
                    )
                }

                StatusBadge(
                    label = machine.status.name,
                    statusColor = when (machine.status) {
                        MachineStatus.AVAILABLE -> com.sucharu.sucharupro.ui.theme.StatusColor(container = Color(0xFF1B3821), content = Color(0xFF4CAF50), border = Color(0xFF4CAF50))
                        MachineStatus.IN_USE -> com.sucharu.sucharupro.ui.theme.StatusColor(container = Color(0xFF0F2B48), content = Color(0xFF2196F3), border = Color(0xFF2196F3))
                        MachineStatus.MAINTENANCE -> com.sucharu.sucharupro.ui.theme.StatusColor(container = Color(0xFF382A13), content = Color(0xFFFF9800), border = Color(0xFFFF9800))
                        MachineStatus.OFFLINE -> com.sucharu.sucharupro.ui.theme.StatusColor(container = Color(0xFF24272C), content = Color(0xFF9E9E9E), border = Color(0xFF9E9E9E))
                        MachineStatus.DECOMMISSIONED -> com.sucharu.sucharupro.ui.theme.StatusColor(container = Color(0xFF3B1816), content = Color(0xFFF44336), border = Color(0xFFF44336))
                    }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (!machine.manufacturer.isNullOrBlank() || !machine.model.isNullOrBlank()) {
                    Text(
                        text = "Manufacturer: ${machine.manufacturer ?: "N/A"} • Model: ${machine.model ?: "N/A"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFB7C8D8)
                    )
                }
                if (!machine.serialNumber.isNullOrBlank()) {
                    Text(
                        text = "Serial No: ${machine.serialNumber}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFB7C8D8)
                    )
                }
                if (!machine.locationReference.isNullOrBlank()) {
                    Text(
                        text = "Location: ${machine.locationReference} • Dept: ${machine.department ?: "N/A"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFB7C8D8)
                    )
                }
                Text(
                    text = "Ownership: ${machine.ownershipType.name.replace("_", " ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF8692A6)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                AppOutlinedButton(
                    text = "Update Status",
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Build, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    onClick = { showStatusDialog = true }
                )
            }
        }
    }

    if (showStatusDialog) {
        AlertDialog(
            onDismissRequest = { showStatusDialog = false },
            title = { Text("Update Machine Status", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MachineStatus.entries.forEach { status ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(4.dp)
                        ) {
                            RadioButton(
                                selected = machine.status == status,
                                onClick = {
                                    onStatusChange(status)
                                    showStatusDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(status.name, color = Color.White)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showStatusDialog = false }) {
                    Text("Close", color = Color(0xFF9ECAFF))
                }
            },
            containerColor = Color(0xFF1C2541)
        )
    }
}

@Composable
fun RegisterMachineDialog(
    onDismiss: () -> Unit,
    onSubmit: (MachineEquipment) -> Unit
) {
    var assetCode by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(MachineType.PRINTING_PRESS) }
    var manufacturer by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var serialNumber by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Register New Machine", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AppTextField(value = assetCode, onValueChange = { assetCode = it }, label = "Asset Code (e.g. EQ-OFFSET-01)")
                AppTextField(value = name, onValueChange = { name = it }, label = "Machine Name *")
                AppTextField(value = manufacturer, onValueChange = { manufacturer = it }, label = "Manufacturer (Optional)")
                AppTextField(value = model, onValueChange = { model = it }, label = "Model (Optional)")
                AppTextField(value = serialNumber, onValueChange = { serialNumber = it }, label = "Serial Number (Optional)")
                AppTextField(value = location, onValueChange = { location = it }, label = "Location Reference (Optional)")
                AppTextField(value = department, onValueChange = { department = it }, label = "Department (Optional)")
            }
        },
        confirmButton = {
            AppButton(
                text = "Register",
                enabled = assetCode.isNotBlank() && name.isNotBlank(),
                onClick = {
                    onSubmit(
                        MachineEquipment(
                            machineId = "MAC-" + java.util.UUID.randomUUID().toString().take(8).uppercase(),
                            tenantId = "TENANT-001",
                            assetCode = assetCode.trim(),
                            name = name.trim(),
                            type = selectedType,
                            manufacturer = manufacturer.trim().ifBlank { null },
                            model = model.trim().ifBlank { null },
                            serialNumber = serialNumber.trim().ifBlank { null },
                            locationReference = location.trim().ifBlank { null },
                            department = department.trim().ifBlank { null }
                        )
                    )
                }
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFFFFB4AB))
            }
        },
        containerColor = Color(0xFF1C2541)
    )
}
