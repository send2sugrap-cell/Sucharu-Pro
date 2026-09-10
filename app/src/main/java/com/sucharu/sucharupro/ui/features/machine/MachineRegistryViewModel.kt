package com.sucharu.sucharupro.ui.features.machine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.machine.MachineEquipment
import com.sucharu.sucharupro.domain.machine.MachineStatus
import com.sucharu.sucharupro.domain.machine.MachineType
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.service.machine.MachineRegistryService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel managing Machine Registry Master Identity UI state & operations.
 */
class MachineRegistryViewModel(
    private val service: MachineRegistryService,
    private val tenantId: String = "TENANT-001"
) : ViewModel() {

    private val _uiState = MutableStateFlow(MachineRegistryUiState())
    val uiState: StateFlow<MachineRegistryUiState> = _uiState.asStateFlow()

    init {
        loadMachines()
    }

    fun loadMachines() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val res = service.listMachines(tenantId, _uiState.value.selectedTypeFilter, _uiState.value.selectedStatusFilter)) {
                is DomainResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, machines = res.data) }
                }
                is DomainResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = res.message) }
                }
                DomainResult.Loading -> {}
            }
        }
    }

    fun registerMachine(machine: MachineEquipment) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            when (val res = service.registerMachine(machine)) {
                is DomainResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = "Machine '${res.data.name}' registered successfully!"
                        )
                    }
                    loadMachines()
                }
                is DomainResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = res.message) }
                }
                DomainResult.Loading -> {}
            }
        }
    }

    fun changeMachineStatus(machineId: String, newStatus: MachineStatus) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            when (val res = service.changeMachineStatus(tenantId, machineId, newStatus)) {
                is DomainResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = "Machine status updated to ${newStatus.name}"
                        )
                    }
                    loadMachines()
                }
                is DomainResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = res.message) }
                }
                DomainResult.Loading -> {}
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onTypeFilterChanged(type: MachineType?) {
        _uiState.update { it.copy(selectedTypeFilter = type) }
        loadMachines()
    }

    fun onStatusFilterChanged(status: MachineStatus?) {
        _uiState.update { it.copy(selectedStatusFilter = status) }
        loadMachines()
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
