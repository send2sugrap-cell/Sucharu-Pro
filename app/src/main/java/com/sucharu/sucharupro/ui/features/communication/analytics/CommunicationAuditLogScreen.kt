package com.sucharu.sucharupro.ui.features.communication.analytics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.domain.model.communication.analytics.AuditResult
import com.sucharu.sucharupro.domain.model.communication.analytics.CommunicationAuditEvent
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunicationAuditLogScreen(
    auditEvents: List<CommunicationAuditEvent>,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Audit & Compliance Logs") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("Back")
                    }
                }
            )
        }
    ) { padding ->
        if (auditEvents.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("No audit events recorded yet.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(auditEvents) { event ->
                    AuditEventCard(event)
                }
            }
        }
    }
}

@Composable
fun AuditEventCard(event: CommunicationAuditEvent) {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault())
    
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = event.action,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (event.result == AuditResult.FAILURE) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
                Text(
                    text = formatter.format(event.timestamp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Target: ${event.targetType} [${event.targetId}]", style = MaterialTheme.typography.bodyMedium)
            Text("Actor: ${event.actorRole} [${event.actorUserId}]", style = MaterialTheme.typography.bodyMedium)
            
            if (event.newState != null) {
                Text("State Change: ${event.previousState ?: "None"} -> ${event.newState}", style = MaterialTheme.typography.bodySmall)
            }
            if (event.reason != null) {
                Text("Reason: ${event.reason}", style = MaterialTheme.typography.bodySmall)
            }
            if (event.failureDetail != null) {
                Text("Failure: ${event.failureDetail}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
