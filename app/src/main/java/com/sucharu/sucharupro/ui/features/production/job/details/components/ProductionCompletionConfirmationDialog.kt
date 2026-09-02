package com.sucharu.sucharupro.ui.features.production.job.details.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.domain.model.job.ProductionCompletionChecklist
import com.sucharu.sucharupro.domain.model.job.ProductionJob
import com.sucharu.sucharupro.ui.components.AppButton
import com.sucharu.sucharupro.ui.theme.spacing

/**
 * Confirmation dialog for finalizing production completion.
 */
@Composable
fun ProductionCompletionConfirmationDialog(
    job: ProductionJob,
    checklist: ProductionCompletionChecklist?,
    onConfirm: (remarks: String?) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var remarks by remember { mutableStateOf("") }
    var isSubmitted by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isSubmitted) onDismiss() },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DoneAll,
                    contentDescription = null,
                    tint = Color(0xFF2E7D32),
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "উৎপাদন সমাপ্তি নিশ্চিতকরণ",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
            ) {
                Text(
                    text = "আপনি কি নিশ্চিত যে জব '${job.jobNumber}' এর উৎপাদন কাজ সফলভাবে সমাপ্ত হয়েছে এবং জবটি ডেলিভারির জন্য প্রস্তুত?",
                    style = MaterialTheme.typography.bodyMedium
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // Summary Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "পরিকল্পিত পরিমাণ:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${job.quantity} ${job.unit}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Over-production warning
                if (checklist?.isOverProduced == true) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFFFF8E1))
                            .padding(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFF57F17),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "বিজ্ঞপ্তি: +${checklist.overProductionQuantity} ${job.unit} অতিরিক্ত উৎপাদন রেকর্ড হয়েছে।",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFE65100)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = remarks,
                    onValueChange = { remarks = it },
                    label = { Text("সমাপ্তি মন্তব্য (ঐচ্ছিক)") },
                    placeholder = { Text("উদাহরণ: ৫০০ কপি সম্পূর্ণ প্রস্তুত") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    shape = RoundedCornerShape(8.dp)
                )
            }
        },
        confirmButton = {
            AppButton(
                text = "সম্পন্ন নিশ্চিত করুন",
                onClick = {
                    if (!isSubmitted) {
                        isSubmitted = true
                        onConfirm(remarks.trim().ifEmpty { null })
                    }
                },
                enabled = !isSubmitted
            )
        },
        dismissButton = {
            com.sucharu.sucharupro.ui.components.AppOutlinedButton(
                text = "বাতিল",
                onClick = onDismiss,
                enabled = !isSubmitted
            )
        }
    )
}
