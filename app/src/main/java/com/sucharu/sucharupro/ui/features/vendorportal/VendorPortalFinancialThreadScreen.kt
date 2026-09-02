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
import com.sucharu.sucharupro.data.api.model.VendorPortalFinancialMessageDto
import com.sucharu.sucharupro.data.api.model.VendorPortalFinancialMessageRequest
import com.sucharu.sucharupro.data.api.model.VendorPortalFinancialThreadDto
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorPortalFinancialThreadScreen(
    thread: VendorPortalFinancialThreadDto,
    messages: List<VendorPortalFinancialMessageDto>,
    onSendMessage: (VendorPortalFinancialMessageRequest) -> Unit,
    onBackClick: () -> Unit = {}
) {
    var messageInput by remember { mutableStateOf("") }

    val bgGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF0F172A), Color(0xFF020617))
    )
    val timeFormat = SimpleDateFormat("HH:mm, dd MMM", Locale.getDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(thread.subject, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Context: ${thread.contextType} (${thread.contextId})", fontSize = 11.sp, color = Color(0xFF94A3B8))
                    }
                },
                navigationIcon = {
                    TextButton(onClick = onBackClick) {
                        Text("← Back", color = Color(0xFF94A3B8), fontWeight = FontWeight.SemiBold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        containerColor = Color(0xFF020617)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(bgGradient)
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Message list
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(messages) { msg ->
                    val isVendor = msg.senderRole.uppercase().contains("VENDOR")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isVendor) Arrangement.End else Arrangement.Start
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isVendor) Color(0xFF1E3A8A) else Color(0xFF1E293B)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth(0.85f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${msg.senderId} (${msg.senderRole})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (isVendor) Color(0xFF93C5FD) else Color(0xFFA78BFA)
                                    )
                                    Text(
                                        text = timeFormat.format(Date(msg.timestamp)),
                                        fontSize = 10.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = msg.content,
                                    fontSize = 13.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Post message input row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = messageInput,
                    onValueChange = { messageInput = it },
                    placeholder = { Text("Write a message or reply...", color = Color(0xFF94A3B8)) },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF3B82F6),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                Button(
                    onClick = {
                        if (messageInput.isNotBlank()) {
                            onSendMessage(VendorPortalFinancialMessageRequest(content = messageInput))
                            messageInput = ""
                        }
                    },
                    enabled = messageInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Send")
                }
            }
        }
    }
}
