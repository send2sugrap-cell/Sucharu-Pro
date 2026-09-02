package com.sucharu.sucharupro.ui.features.task

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.data.model.task.TaskPriority
import com.sucharu.sucharupro.data.model.task.TaskStatus
import com.sucharu.sucharupro.data.model.task.TaskType
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.task.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TaskListViewModel(
    private val repository: TaskRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TaskUiState(isLoading = true))
    val uiState: StateFlow<TaskUiState> = _uiState.asStateFlow()

    fun loadTasks(
        projectId: String = "PRJ-DEFAULT",
        query: String = "",
        status: TaskStatus? = null,
        priority: TaskPriority? = null,
        taskType: TaskType? = null,
        actorUserId: String = "USR-ADMIN",
        callerRole: UserRole = UserRole.ADMIN
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val res = repository.searchTasks(projectId, query, status, priority, taskType, null, actorUserId, callerRole)) {
                is com.sucharu.sucharupro.domain.model.common.DomainResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        tasks = res.data
                    )
                }
                is com.sucharu.sucharupro.domain.model.common.DomainResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = res.message
                    )
                }
                else -> {}
            }
        }
    }
}
