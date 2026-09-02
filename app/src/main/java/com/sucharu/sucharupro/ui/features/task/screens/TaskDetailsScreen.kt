package com.sucharu.sucharupro.ui.features.task.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.data.model.task.TaskStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.ui.features.task.TaskDetailsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailsScreen(
    taskId: String,
    viewModel: TaskDetailsViewModel,
    onBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(taskId) {
        viewModel.loadTaskDetails(taskId = taskId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Task Details", color = Color.White) },
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
            } else if (uiState.selectedTask == null) {
                Text("Task not found.", color = Color.Gray, modifier = Modifier.align(Alignment.Center))
            } else {
                val task = uiState.selectedTask!!
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2C)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(task.taskNo, color = Color(0xFF6C63FF), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    Text(task.status.defaultLabel, color = Color(0xFF38EF7D), fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(task.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(task.description, color = Color.LightGray, fontSize = 14.sp)

                                Divider(modifier = Modifier.padding(vertical = 12.dp), color = Color.DarkGray)

                                Text("Priority: ${task.priority.name} | Type: ${task.taskType.defaultLabel}", color = Color.Gray, fontSize = 12.sp)
                                Text("Assigned To: ${task.assignedTo ?: "Unassigned"} | Created By: ${task.createdBy}", color = Color.Gray, fontSize = 12.sp)
                                Text("Progress: ${task.progressPercentage}% | Est. Mins: ${task.estimatedMinutes}", color = Color.Gray, fontSize = 12.sp)
                                if (task.blockedReason != null) {
                                    Text("Blocked Reason: ${task.blockedReason}", color = Color(0xFFFF512F), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (task.status == TaskStatus.ASSIGNED) {
                                Button(
                                    onClick = { viewModel.acknowledgeTask("PRJ-DEFAULT", task.taskId, "USR-ADMIN", UserRole.ADMIN) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C63FF)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Acknowledge")
                                }
                            }
                            if (task.status == TaskStatus.ACKNOWLEDGED || task.status == TaskStatus.ASSIGNED) {
                                Button(
                                    onClick = { viewModel.startTask("PRJ-DEFAULT", task.taskId, "USR-ADMIN", UserRole.ADMIN) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B4DB)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Start Task")
                                }
                            }
                            if (task.status == TaskStatus.IN_PROGRESS) {
                                Button(
                                    onClick = { viewModel.completeTask("PRJ-DEFAULT", task.taskId, "Completed", "USR-ADMIN", UserRole.ADMIN) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF11998E)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Complete")
                                }
                            }
                            if (task.status == TaskStatus.COMPLETED) {
                                Button(
                                    onClick = { viewModel.verifyTask("PRJ-DEFAULT", task.taskId, "Verified", "USR-ADMIN", UserRole.ADMIN) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38EF7D)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Verify")
                                }
                            }
                        }
                    }

                    item {
                        Text("Activity History Timeline", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    items(uiState.activityHistory) { event ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF191924)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(event.eventType.name, color = Color(0xFF6C63FF), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text("Actor: ${event.actorId}", color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
