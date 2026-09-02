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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.domain.model.qc.governance.QcImprovementAction
import com.sucharu.sucharupro.domain.model.qc.governance.QcImprovementActionStatus

/**
 * Detailed view and lifecycle management panel for an individual Improvement Action (Module 06 Step 10).
 */
@Composable
fun QcImprovementActionDetailsScreen(
    action: QcImprovementAction,
    onApprove: () -> Unit = {},
    onStart: () -> Unit = {},
    onComplete: () -> Unit = {},
    onVerify: () -> Unit = {},
    onReject: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Text(
            text = "Improvement Action Details",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = action.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = action.description, style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(12.dp))

                Text(text = "Type: ${action.actionType.defaultLabel}", fontSize = 14.sp)
                Text(text = "Priority: ${action.priority.defaultLabel}", fontSize = 14.sp)
                Text(text = "Status: ${action.status.defaultLabel}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(text = "Proposed By: ${action.proposedByName ?: action.proposedBy}", fontSize = 13.sp)
                if (!action.ownerId.isNullOrBlank()) {
                    Text(text = "Owner: ${action.ownerName ?: action.ownerId}", fontSize = 13.sp)
                }
                if (!action.dueDate.isNullOrBlank()) {
                    Text(text = "Due Date: ${action.dueDate}", fontSize = 13.sp)
                }
                if (action.status == QcImprovementActionStatus.VERIFIED) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Effectiveness: ${action.effectiveness.defaultLabel}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (!action.verificationNotes.isNullOrBlank()) {
                        Text(text = "Verification: ${action.verificationNotes}", fontSize = 13.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Lifecycle Actions
        if (!action.isTerminal) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (action.status) {
                    QcImprovementActionStatus.PROPOSED -> {
                        Button(onClick = onApprove, modifier = Modifier.weight(1f)) { Text("Approve") }
                        OutlinedButton(onClick = onReject, modifier = Modifier.weight(1f)) { Text("Reject") }
                    }
                    QcImprovementActionStatus.APPROVED,
                    QcImprovementActionStatus.ASSIGNED -> {
                        Button(onClick = onStart, modifier = Modifier.weight(1f)) { Text("Start Action") }
                    }
                    QcImprovementActionStatus.IN_PROGRESS -> {
                        Button(onClick = onComplete, modifier = Modifier.weight(1f)) { Text("Complete") }
                    }
                    QcImprovementActionStatus.COMPLETED -> {
                        Button(onClick = onVerify, modifier = Modifier.weight(1f)) { Text("Verify Effectiveness") }
                    }
                    else -> {}
                }
            }
        }
    }
}
