package com.sucharu.sucharupro.ui.features.machine

import com.sucharu.sucharupro.domain.machine.MachineEquipment
import com.sucharu.sucharupro.domain.machine.MachineStatus
import com.sucharu.sucharupro.domain.machine.MachineType

/**
 * UI State for Machine Registry Screen.
 */
data class MachineRegistryUiState(
    val machines: List<MachineEquipment> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val selectedTypeFilter: MachineType? = null,
    val selectedStatusFilter: MachineStatus? = null,
    val searchQuery: String = ""
) {
    val filteredMachines: List<MachineEquipment>
        get() = machines.filter { m ->
            (selectedTypeFilter == null || m.type == selectedTypeFilter) &&
            (selectedStatusFilter == null || m.status == selectedStatusFilter) &&
            (searchQuery.isBlank() ||
                    m.name.contains(searchQuery, ignoreCase = true) ||
                    m.assetCode.contains(searchQuery, ignoreCase = true) ||
                    (m.manufacturer != null && m.manufacturer.contains(searchQuery, ignoreCase = true)))
        }
}
