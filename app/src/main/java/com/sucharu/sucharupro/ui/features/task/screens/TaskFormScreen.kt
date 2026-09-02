package com.sucharu.sucharupro.ui.features.task.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.data.model.task.TaskPriority
import com.sucharu.sucharupro.data.model.task.TaskType
import com.sucharu.sucharupro.ui.features.task.TaskFormViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskFormScreen(
    viewModel: TaskFormViewModel,
    onTaskCreated: (String) -> Unit = {},
    onBack: () -> Unit = {}
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var assignedTo by remember { mutableStateOf("") }
    var estimatedMinsText by remember { mutableStateOf("60") }

    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create New Task", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1E1E2C))
            )
        },
        containerColor = Color(0xFF12121D)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Task Information", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title", color = Color.Gray) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF6C63FF),
                    unfocusedBorderColor = Color.Gray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description", color = Color.Gray) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF6C63FF),
                    unfocusedBorderColor = Color.Gray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = assignedTo,
                onValueChange = { assignedTo = it },
                label = { Text("Assignee User ID (Optional)", color = Color.Gray) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF6C63FF),
                    unfocusedBorderColor = Color.Gray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = estimatedMinsText,
                onValueChange = { estimatedMinsText = it },
                label = { Text("Estimated Duration (Minutes)", color = Color.Gray) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF6C63FF),
                    unfocusedBorderColor = Color.Gray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )

            if (uiState.errorMessage != null) {
                Text(uiState.errorMessage!!, color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = {
                    val mins = estimatedMinsText.toIntOrNull() ?: 60
                    val assignee = if (assignedTo.isBlank()) null else assignedTo
                    viewModel.createTask(
                        title = title,
                        description = description,
                        taskType = TaskType.GENERAL,
                        priority = TaskPriority.NORMAL,
                        assignedTo = assignee,
                        dueDate = System.currentTimeMillis() + (24 * 60 * 60 * 1000L),
                        estimatedMinutes = mins,
                        onSuccess = onTaskCreated
                    )
                },
                enabled = title.isNotBlank() && !uiState.isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C63FF)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Create Task")
            }
        }
    }
}
