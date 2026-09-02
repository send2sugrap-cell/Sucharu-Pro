package com.sucharu.sucharupro.ui.features.vendorportal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.data.api.model.VendorRfqClarificationDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorPortalClarificationScreen(
    rfqNumber: String,
    clarifications: List<VendorRfqClarificationDto>,
    onAskQuestion: (String) -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    var newQuestion by remember { mutableStateOf("") }
    val bgGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF0F172A), Color(0xFF020617))
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Clarifications — $rfqNumber",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgGradient)
                .padding(padding)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                // Ask question input
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        OutlinedTextField(
                            value = newQuestion,
                            onValueChange = { newQuestion = it },
                            label = { Text("Ask a question about this RFQ") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF3B82F6),
                                unfocusedBorderColor = Color(0xFF475569)
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                if (newQuestion.isNotBlank()) {
                                    onAskQuestion(newQuestion)
                                    newQuestion = ""
                                }
                            },
                            modifier = Modifier.align(Alignment.End),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                        ) {
                            Text("Submit Question")
                        }
                    }
                }

                if (clarifications.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "No questions asked yet", color = Color(0xFF64748B), fontSize = 16.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(clarifications) { item ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.8f))
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "Q: ${item.question}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                    if (!item.answer.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "A: ${item.answer}",
                                            fontSize = 13.sp,
                                            color = Color(0xFF34D399)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Answered by ${item.answeredBy}",
                                            fontSize = 11.sp,
                                            color = Color(0xFF94A3B8)
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Status: Pending Answer",
                                            fontSize = 12.sp,
                                            color = Color(0xFFF59E0B)
                                        )
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
