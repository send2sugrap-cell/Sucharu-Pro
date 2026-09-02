package com.sucharu.sucharupro.ui.features.qc.governance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.domain.model.qc.governance.QcAlertSeverity
import com.sucharu.sucharupro.domain.model.qc.governance.QcAlertStatus
import com.sucharu.sucharupro.domain.model.qc.governance.QcQualityAlert

/**
 * Detailed view and action panel for an individual Quality Alert.
 */
@Composable
fun QcQualityAlertDetailsScreen(
    alert: QcQualityAlert,
    onAcknowledge: () -> Unit = {},
    onResolve: () -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Text(
            text = "Quality Alert Details",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (alert.severity == QcAlertSeverity.CRITICAL) Color(0xFFFFEBEE) else MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = alert.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = alert.message, style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(12.dp))

                Text(text = "KPI: ${alert.kpiType.defaultLabel}", fontSize = 14.sp)
                Text(text = "Current Value: ${alert.currentValue} ${alert.kpiType.unit} (Target: ${alert.targetValue} ${alert.kpiType.unit})", fontSize = 14.sp)
                Text(text = "Severity: ${alert.severity.defaultLabel}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(text = "Status: ${alert.status.defaultLabel}", fontSize = 14.sp)
                Text(text = "Detected At: ${alert.detectedAt}", fontSize = 13.sp, color = MaterialTheme.colorScheme.outline)

                if (!alert.notes.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Notes:\n${alert.notes}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Action Buttons
        if (!alert.isTerminal) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (alert.status == QcAlertStatus.DETECTED) {
                    Button(
                        onClick = onAcknowledge,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Acknowledge")
                    }
                }
                Button(
                    onClick = onResolve,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Resolve")
                }
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Dismiss")
                }
            }
        }
    }
}
