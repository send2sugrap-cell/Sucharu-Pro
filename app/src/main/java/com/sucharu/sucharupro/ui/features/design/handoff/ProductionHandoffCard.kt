package com.sucharu.sucharupro.ui.features.design.handoff

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.domain.model.design.DesignProductionHandoff

/**
 * Minimal summary card showing Production Handoff details or blocked reason (Module 05 Step 05).
 */
@Composable
fun ProductionHandoffCard(
    handoff: DesignProductionHandoff?,
    blockedReason: String? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (handoff != null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "প্রোডাকশন হ্যান্ডঅফ স্থিতি (Production Handoff)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                ProductionHandoffStatusBadge(isAuthorized = handoff != null)
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (handoff != null) {
                Text(
                    text = "Job ID: ${handoff.productionJobId} • Approved Proof Version: ${handoff.proofVersionId}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Approved Artwork Version: ${handoff.artworkVersionId}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Authorized by ${handoff.authorizedByName ?: handoff.authorizedBy} at ${handoff.authorizedAt}",
                    style = MaterialTheme.typography.labelSmall
                )
            } else {
                Text(
                    text = blockedReason ?: "Final approval locking required before handoff authorization.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
