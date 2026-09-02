package com.sucharu.sucharupro.ui.features.qc.governance

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.domain.model.qc.governance.QcAlertSeverity
import com.sucharu.sucharupro.domain.model.qc.governance.QcQualityAlert

/**
 * Screen listing all quality alerts with status filtering (Module 06 Step 10).
 */
@Composable
fun QcQualityAlertListScreen(
    alerts: List<QcQualityAlert>,
    onAlertClick: (String) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Text(
            text = "Quality Alerts",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(alerts) { alert ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAlertClick(alert.alertId) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (alert.severity == QcAlertSeverity.CRITICAL) Color(0xFFFFEBEE) else MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = alert.title,
                                fontWeight = FontWeight.Bold,
                                color = if (alert.severity == QcAlertSeverity.CRITICAL) Color(0xFFC62828) else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = alert.severity.defaultLabel,
                                fontWeight = FontWeight.Bold,
                                color = if (alert.severity == QcAlertSeverity.CRITICAL) Color(0xFFC62828) else Color(0xFFF57F17),
                                fontSize = 12.sp
                            )
                        }
                        Text(text = alert.message, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Status: ${alert.status.defaultLabel} | Escalation: ${alert.escalationLevel.defaultLabel}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}
