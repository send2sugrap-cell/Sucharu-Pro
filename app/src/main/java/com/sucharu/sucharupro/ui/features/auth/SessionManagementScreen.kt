package com.sucharu.sucharupro.ui.features.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.data.auth.model.SessionSummaryDto
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Unified Sucharu Graphics Session & Remote Device Management Screen (INFRA-03 Step 04).
 */
@Composable
fun SessionManagementScreen(
    sessions: List<SessionSummaryDto>,
    onRevokeSession: (String) -> Unit,
    onRevokeAllSessions: () -> Unit,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    modifier: Modifier = Modifier
) {
    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF0B132B), Color(0xFF1C2541), Color(0xFF0B132B))
    )

    val cardBorderGradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFF0061A4), Color(0xFF9ECAFF), Color(0xFF0061A4))
    )

    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundGradient)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("ACTIVE SESSIONS", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF9ECAFF))
                Text("Manage devices signed into your account", fontSize = 12.sp, color = Color(0xFFB7C8D8))
            }

            Button(
                onClick = onRevokeAllSessions,
                enabled = !isLoading && sessions.size > 1,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBA1A1A)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("REVOKE ALL", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        if (errorMessage != null) {
            Surface(
                color = Color(0xFF93000A), shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Text(text = errorMessage, color = Color(0xFFFFDAD6), modifier = Modifier.padding(12.dp))
            }
        }

        if (sessions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No active sessions found.", color = Color(0xFFB7C8D8))
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(sessions, key = { it.sessionId }) { session ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, if (session.isCurrent) cardBorderGradient else Brush.linearGradient(listOf(Color(0xFF384957), Color(0xFF384957))), RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C2541).copy(alpha = 0.85f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = session.deviceName ?: "Unknown Device",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 15.sp
                                    )
                                    if (session.isCurrent) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            color = Color(0xFF0061A4),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "THIS DEVICE",
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text("IP: ${session.clientIp ?: "Unknown"}", color = Color(0xFFB7C8D8), fontSize = 11.sp)
                                Text("Last Active: ${dateFormatter.format(Date(session.lastSeenAt))}", color = Color(0xFFB7C8D8), fontSize = 11.sp)
                            }

                            if (!session.isCurrent) {
                                TextButton(onClick = { onRevokeSession(session.sessionId) }) {
                                    Text("Revoke", color = Color(0xFFFFB4AB), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
