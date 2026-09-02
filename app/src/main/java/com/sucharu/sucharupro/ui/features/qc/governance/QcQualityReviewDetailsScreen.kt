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
import com.sucharu.sucharupro.domain.model.qc.governance.QcQualityReview
import com.sucharu.sucharupro.domain.model.qc.governance.QcQualityReviewStatus

/**
 * Detailed view of a formal Quality Review record.
 */
@Composable
fun QcQualityReviewDetailsScreen(
    review: QcQualityReview,
    onStartReview: () -> Unit = {},
    onCompleteReview: () -> Unit = {},
    onCancelReview: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Text(
            text = "Quality Review Details",
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
                Text(text = review.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Status: ${review.status.defaultLabel}", fontWeight = FontWeight.SemiBold)
                Text(text = "Reviewer: ${review.reviewerName ?: review.reviewerId}")
                Text(text = "Period: ${review.reviewPeriod.startTimestamp} to ${review.reviewPeriod.endTimestamp}")
                Spacer(modifier = Modifier.height(12.dp))

                Text(text = "Major Defects: ${review.majorDefectCount} | Recurring: ${review.recurringDefectCount}")
                Text(text = "Reworks: ${review.reworkCount} | Re-QC Cycles: ${review.reQcCycleCount}")
                Text(text = "Final QC Pass Rate: ${review.finalQcPassRate}%")
                Text(text = "Cost Variance: ${review.costVariance} | Time Variance: ${review.timeVarianceMinutes} min")

                if (!review.recommendations.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Recommendations:\n${review.recommendations}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (!review.reviewNotes.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Notes:\n${review.reviewNotes}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (!review.isTerminal) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (review.status == QcQualityReviewStatus.DRAFT || review.status == QcQualityReviewStatus.SCHEDULED) {
                    Button(onClick = onStartReview, modifier = Modifier.weight(1f)) { Text("Start Review") }
                }
                if (review.status == QcQualityReviewStatus.IN_REVIEW) {
                    Button(onClick = onCompleteReview, modifier = Modifier.weight(1f)) { Text("Complete Review") }
                }
                OutlinedButton(onClick = onCancelReview, modifier = Modifier.weight(1f)) { Text("Cancel") }
            }
        }
    }
}
