package com.sucharu.sucharupro.ui.features.task.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.sucharu.sucharupro.ui.features.task.TaskDashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDashboardScreen(
    viewModel: TaskDashboardViewModel,
    onNavigateToTasks: () -> Unit = {},
    onNavigateToCreate: () -> Unit = {},
    onNavigateToBoard: () -> Unit = {},
    onSelectTask: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadDashboard()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Task Management Dashboard", color = Color.White) },
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
            } else if (uiState.errorMessage != null) {
                Text(
                    text = uiState.errorMessage ?: "Error loading dashboard",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                val db = uiState.dashboard
                val summary = db?.summary

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = onNavigateToCreate,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C63FF)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("+ New Task")
                            }
                            OutlinedButton(
                                onClick = onNavigateToTasks,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Task List", color = Color.White)
                            }
                            OutlinedButton(
                                onClick = onNavigateToBoard,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Board View", color = Color.White)
                            }
                        }
                    }

                    item {
                        Text(
                            text = "Task Overview",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MetricCard("My Tasks", db?.myTasks?.size?.toString() ?: "0", Color(0xFF4E54C8), Modifier.weight(1f))
                            MetricCard("Team Tasks", db?.teamTasks?.size?.toString() ?: "0", Color(0xFF00B4DB), Modifier.weight(1f))
                            MetricCard("Overdue", summary?.overdueTasks?.toString() ?: "0", Color(0xFFFF416C), Modifier.weight(1f))
                            MetricCard("Urgent", summary?.urgentTasks?.toString() ?: "0", Color(0xFFFF4B2B), Modifier.weight(1f))
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MetricCard("In Progress", summary?.inProgressTasks?.toString() ?: "0", Color(0xFFF7971E), Modifier.weight(1f))
                            MetricCard("Blocked", summary?.blockedTasks?.toString() ?: "0", Color(0xFFFF512F), Modifier.weight(1f))
                            MetricCard("Completed", summary?.completedTasks?.toString() ?: "0", Color(0xFF11998E), Modifier.weight(1f))
                            MetricCard("Completion", String.format("%.0f%%", summary?.completionRate ?: 0.0), Color(0xFF38EF7D), Modifier.weight(1f))
                        }
                    }

                    item {
                        Text(
                            text = "My Active Tasks",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    val activeTasks = db?.myTasks ?: emptyList()
                    if (activeTasks.isEmpty()) {
                        item {
                            Text("No active tasks assigned.", color = Color.Gray, fontSize = 14.sp)
                        }
                    } else {
                        items(activeTasks) { task ->
                            Card(
                                onClick = { onSelectTask(task.taskId) },
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2C)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(task.taskNo, color = Color(0xFF6C63FF), fontWeight = FontWeight.Bold)
                                        Text(task.status.defaultLabel, color = Color(0xFF38EF7D), fontSize = 12.sp)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(task.title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Progress: ${task.progressPercentage}% | Priority: ${task.priority.name}", color = Color.Gray, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricCard(title: String, value: String, accentColor: Color, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2C)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = accentColor)
            Spacer(modifier = Modifier.height(4.dp))
            Text(title, fontSize = 11.sp, color = Color.Gray)
        }
    }
}
