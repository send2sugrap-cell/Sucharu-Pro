package com.sucharu.sucharupro.ui.features.task

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.task.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TaskDetailsViewModel(
    private val repository: TaskRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TaskUiState(isLoading = true))
    val uiState: StateFlow<TaskUiState> = _uiState.asStateFlow()

    fun loadTaskDetails(
        projectId: String = "PRJ-DEFAULT",
        taskId: String,
        actorUserId: String = "USR-ADMIN",
        callerRole: UserRole = UserRole.ADMIN
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val taskRes = repository.getTask(projectId, taskId, actorUserId, callerRole)
            if (taskRes is com.sucharu.sucharupro.domain.model.common.DomainResult.Success) {
                val task = taskRes.data
                val assignHist = repository.getAssignmentHistory(projectId, taskId, actorUserId, callerRole).getOrDefault(emptyList())
                val progHist = repository.getProgressHistory(projectId, taskId, actorUserId, callerRole).getOrDefault(emptyList())
                val actHist = repository.getActivityHistory(projectId, taskId, actorUserId, callerRole).getOrDefault(emptyList())

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    selectedTask = task,
                    assignmentHistory = assignHist,
                    progressHistory = progHist,
                    activityHistory = actHist
                )
            } else if (taskRes is com.sucharu.sucharupro.domain.model.common.DomainResult.Error) {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = taskRes.message)
            }
        }
    }

    fun acknowledgeTask(projectId: String, taskId: String, actorUserId: String, callerRole: UserRole) {
        viewModelScope.launch {
            val res = repository.acknowledgeTask(projectId, taskId, actorUserId, callerRole)
            if (res is com.sucharu.sucharupro.domain.model.common.DomainResult.Success) {
                loadTaskDetails(projectId, taskId, actorUserId, callerRole)
            }
        }
    }

    fun startTask(projectId: String, taskId: String, actorUserId: String, callerRole: UserRole) {
        viewModelScope.launch {
            val res = repository.startTask(projectId, taskId, actorUserId, callerRole)
            if (res is com.sucharu.sucharupro.domain.model.common.DomainResult.Success) {
                loadTaskDetails(projectId, taskId, actorUserId, callerRole)
            }
        }
    }

    fun completeTask(projectId: String, taskId: String, note: String, actorUserId: String, callerRole: UserRole) {
        viewModelScope.launch {
            val res = repository.completeTask(projectId, taskId, note, actorUserId, callerRole)
            if (res is com.sucharu.sucharupro.domain.model.common.DomainResult.Success) {
                loadTaskDetails(projectId, taskId, actorUserId, callerRole)
            }
        }
    }

    fun verifyTask(projectId: String, taskId: String, note: String, actorUserId: String, callerRole: UserRole) {
        viewModelScope.launch {
            val res = repository.verifyTask(projectId, taskId, note, actorUserId, callerRole)
            if (res is com.sucharu.sucharupro.domain.model.common.DomainResult.Success) {
                loadTaskDetails(projectId, taskId, actorUserId, callerRole)
            }
        }
    }
}
