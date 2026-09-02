package com.sucharu.sucharupro.ui.features.task.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.ui.features.task.TaskDetailsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskAssignmentScreen(
    taskId: String,
    viewModel: TaskDetailsViewModel,
    onBack: () -> Unit = {}
) {
    var newAssignee by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Assign / Reassign Task", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1E1E2C))
            )
        },
        containerColor = Color(0xFF12121D)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Assignee Selection", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)

            OutlinedTextField(
                value = newAssignee,
                onValueChange = { newAssignee = it },
                label = { Text("Assignee User ID", color = Color.Gray) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF6C63FF),
                    unfocusedBorderColor = Color.Gray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = reason,
                onValueChange = { reason = it },
                label = { Text("Assignment Reason", color = Color.Gray) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF6C63FF),
                    unfocusedBorderColor = Color.Gray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = onBack,
                enabled = newAssignee.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C63FF)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Confirm Assignment")
            }
        }
    }
}
