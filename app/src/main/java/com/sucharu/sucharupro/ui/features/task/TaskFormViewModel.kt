package com.sucharu.sucharupro.ui.features.task

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.data.model.task.TaskPriority
import com.sucharu.sucharupro.data.model.task.TaskType
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.task.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TaskFormViewModel(
    private val repository: TaskRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TaskUiState())
    val uiState: StateFlow<TaskUiState> = _uiState.asStateFlow()

    fun createTask(
        projectId: String = "PRJ-DEFAULT",
        title: String,
        description: String,
        taskType: TaskType,
        priority: TaskPriority,
        assignedTo: String?,
        dueDate: Long?,
        estimatedMinutes: Int,
        actorUserId: String = "USR-ADMIN",
        callerRole: UserRole = UserRole.ADMIN,
        onSuccess: (String) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val res = repository.createTask(
                projectId = projectId,
                title = title,
                description = description,
                taskType = taskType,
                priority = priority,
                assignedTo = assignedTo,
                dueDate = dueDate,
                estimatedMinutes = estimatedMinutes,
                actorUserId = actorUserId,
                callerRole = callerRole
            )
            when (res) {
                is com.sucharu.sucharupro.domain.model.common.DomainResult.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    onSuccess(res.data.taskId)
                }
                is com.sucharu.sucharupro.domain.model.common.DomainResult.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = res.message)
                }
                else -> {}
            }
        }
    }
}
