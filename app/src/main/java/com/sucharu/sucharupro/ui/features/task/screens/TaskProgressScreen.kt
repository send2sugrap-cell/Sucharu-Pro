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
fun TaskProgressScreen(
    taskId: String,
    viewModel: TaskDetailsViewModel,
    onBack: () -> Unit = {}
) {
    var progressSlider by remember { mutableStateOf(50f) }
    var note by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Update Progress", color = Color.White) },
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
            Text("Progress: ${progressSlider.toInt()}%", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)

            Slider(
                value = progressSlider,
                onValueChange = { progressSlider = it },
                valueRange = 0f..100f,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF6C63FF),
                    activeTrackColor = Color(0xFF6C63FF)
                )
            )

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Progress Note", color = Color.Gray) },
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
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C63FF)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Progress Update")
            }
        }
    }
}
