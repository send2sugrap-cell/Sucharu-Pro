package com.sucharu.sucharupro.ui.features.production.job.details.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.domain.model.job.ProductionJobStage
import com.sucharu.sucharupro.domain.model.job.ProductionOperator
import com.sucharu.sucharupro.ui.components.AppButton
import com.sucharu.sucharupro.ui.theme.spacing

/**
 * Dialog allowing an authorized user to assign or reassign an operator to a specific production stage.
 */
@Composable
fun StageOperatorAssignmentDialog(
    stage: ProductionJobStage,
    availableOperators: List<ProductionOperator>,
    isReassignment: Boolean = false,
    onConfirm: (operatorId: String, operatorName: String, notes: String?) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedOperator by remember {
        mutableStateOf(
            availableOperators.find { it.operatorId == stage.assignedUserId }
                ?: availableOperators.firstOrNull()
        )
    }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isReassignment) {
                    "Reassign Stage Operator"
                } else {
                    "Assign Stage Operator"
                }
            )
        },
        text = {
            Column {
                Text(
                    text = "Stage: ${stage.sequence}. ${stage.stageType.defaultLabel} (${stage.stageType.shortCode})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                if (isReassignment && stage.assignedUserName != null) {
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))
                    Text(
                        text = "Current Operator: ${stage.assignedUserName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

                Text(
                    text = "Select Operator:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))

                // Operators List
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth().height(150.dp)
                ) {
                    LazyColumn(modifier = Modifier.padding(MaterialTheme.spacing.extraSmall)) {
                        items(availableOperators) { operator ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedOperator = operator }
                                    .padding(vertical = 4.dp, horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedOperator?.operatorId == operator.operatorId,
                                    onClick = { selectedOperator = operator }
                                )
                                Column(modifier = Modifier.padding(start = 4.dp)) {
                                    Text(
                                        text = operator.operatorName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "Role: ${operator.role.defaultLabel}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Assignment Notes (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            AppButton(
                text = if (isReassignment) "Confirm Reassign" else "Confirm Assign",
                enabled = selectedOperator != null,
                onClick = {
                    val operator = selectedOperator ?: return@AppButton
                    onConfirm(
                        operator.operatorId,
                        operator.operatorName,
                        notes.trim().ifBlank { null }
                    )
                }
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
