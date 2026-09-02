package com.sucharu.sucharupro.ui.features.task.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.data.model.task.TaskStatus
import com.sucharu.sucharupro.ui.features.task.TaskBoardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskBoardScreen(
    viewModel: TaskBoardViewModel,
    onSelectTask: (String) -> Unit = {},
    onBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadBoard()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Task Kanban Board", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1E1E2C))
            )
        },
        containerColor = Color(0xFF12121D)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color(0xFF6C63FF))
            } else {
                val boardStatuses = listOf(
                    TaskStatus.ASSIGNED,
                    TaskStatus.ACKNOWLEDGED,
                    TaskStatus.IN_PROGRESS,
                    TaskStatus.BLOCKED,
                    TaskStatus.ON_HOLD,
                    TaskStatus.COMPLETED
                )

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    boardStatuses.forEach { status ->
                        val tasksInStatus = uiState.tasks.filter { it.status == status }
                        Column(
                            modifier = Modifier
                                .width(280.dp)
                                .fillMaxHeight()
                        ) {
                            Text(
                                text = "${status.defaultLabel} (${tasksInStatus.size})",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(tasksInStatus) { task ->
                                    Card(
                                        onClick = { onSelectTask(task.taskId) },
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2C)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(task.taskNo, color = Color(0xFF6C63FF), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(task.title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("Assignee: ${task.assignedTo ?: "Unassigned"}", color = Color.Gray, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
