package com.sucharu.sucharupro.domain.validation.communication.analytics

import com.sucharu.sucharupro.domain.model.communication.analytics.CommunicationAnalyticsSnapshot
import com.sucharu.sucharupro.domain.model.communication.analytics.CommunicationSnapshotVerificationResult
import com.sucharu.sucharupro.domain.model.communication.analytics.SnapshotVerificationStatus
import com.sucharu.sucharupro.domain.model.communication.analytics.CommunicationKpiSummary
import com.sucharu.sucharupro.domain.model.communication.analytics.CommunicationGovernanceResult
import com.sucharu.sucharupro.domain.model.communication.analytics.CommunicationGovernanceStatus
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

/**
 * 1. CommunicationSnapshotVerifierTest (Validates hash chaining and snapshot integrity)
 *
 * Verifies that the CommunicationSnapshotVerifier correctly generates and validates
 * cryptographic hashes for analytics snapshots, detecting any tampering.
 */
class CommunicationSnapshotVerifierTest {

    private fun createBaseSnapshot(id: String, hash: String) = CommunicationAnalyticsSnapshot(
        snapshotId = id,
        projectId = "PROJ-123",
        fromDate = Instant.now(),
        toDate = Instant.now(),
        generatedAt = Instant.now(),
        kpiSummary = CommunicationKpiSummary(0, 0, 0, 0, 0, 0, 0, 0, 0.0, 0.0, 0.0, 0.0, 0L, 0L, 0L),
        channelAnalytics = emptyList(),
        typeAnalytics = emptyList(),
        customerEngagement = emptyList(),
        internalEngagement = emptyList(),
        vendorEngagement = emptyList(),
        campaignAnalytics = emptyList(),
        automationAnalytics = emptyList(),
        riskIndicators = emptyList(),
        anomalies = emptyList(),
        governanceResult = CommunicationGovernanceResult(projectId = "PROJ-123", evaluatedAt = Instant.now(), passedControls = emptyList(), warningControls = emptyList(), failedControls = emptyList(), riskCount = 0, anomalyCount = 0, governanceStatus = com.sucharu.sucharupro.domain.model.communication.analytics.GovernanceStatus.HEALTHY),
        sha256Hash = hash
    )

    @Test
    fun `computeHash produces consistent hash for same data`() {
        val snapshot = createBaseSnapshot("SNAP-1", "")
        
        val hash1 = CommunicationSnapshotVerifier.computeHash(snapshot)
        val hash2 = CommunicationSnapshotVerifier.computeHash(snapshot)
        
        assertEquals("Hash generation should be deterministic", hash1, hash2)
        assertTrue("Hash should not be empty", hash1.isNotEmpty())
    }

    @Test
    fun `verify detects unmodified snapshot as valid`() {
        val snapshot = createBaseSnapshot("SNAP-1", "")
        val hash = CommunicationSnapshotVerifier.computeHash(snapshot)
        val hashedSnapshot = snapshot.copy(sha256Hash = hash)

        val result = CommunicationSnapshotVerifier.verify(hashedSnapshot, "USR-1")
        
        assertEquals(SnapshotVerificationStatus.VERIFIED, result.status)
    }

    @Test
    fun `verify detects modified snapshot as tampered`() {
        val snapshot = createBaseSnapshot("SNAP-1", "")
        val hash = CommunicationSnapshotVerifier.computeHash(snapshot)
        val hashedSnapshot = snapshot.copy(sha256Hash = hash)

        // Modify the payload without updating the hash
        val tamperedSnapshot = hashedSnapshot.copy(kpiSummary = CommunicationKpiSummary(99, 0, 0, 0, 0, 0, 0, 0, 0.0, 0.0, 0.0, 0.0, 0L, 0L, 0L))
        
        val result = CommunicationSnapshotVerifier.verify(tamperedSnapshot, "USR-1")
        
        assertEquals(SnapshotVerificationStatus.INTEGRITY_FAILURE, result.status)
    }
}
