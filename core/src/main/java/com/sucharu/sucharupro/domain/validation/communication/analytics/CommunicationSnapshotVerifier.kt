package com.sucharu.sucharupro.domain.validation.communication.analytics

import com.sucharu.sucharupro.domain.model.communication.analytics.CommunicationAnalyticsSnapshot
import com.sucharu.sucharupro.domain.model.communication.analytics.CommunicationSnapshotVerificationResult
import com.sucharu.sucharupro.domain.model.communication.analytics.SnapshotVerificationStatus
import java.security.MessageDigest
import java.time.Instant

/**
 * Pure, stateless engine for snapshot integrity verification.
 *
 * Verification Boundary:
 * - READS the stored snapshot and recomputes its hash.
 * - NEVER mutates the snapshot or any source record.
 * - Returns a structured [CommunicationSnapshotVerificationResult].
 *
 * Uses the SAME deterministic hash algorithm as [CommunicationAnalyticsRepositoryImpl.createSnapshot].
 * Any change to the hash algorithm must be reflected here simultaneously.
 */
object CommunicationSnapshotVerifier {

    /**
     * Verifies the integrity of [snapshot] by recomputing its SHA-256 hash
     * and comparing it with [snapshot.sha256Hash].
     *
     * @param snapshot The snapshot to verify.
     * @param verifiedByUserId The actor performing the verification (for audit).
     * @return [CommunicationSnapshotVerificationResult] with status and hash details.
     */
    fun verify(
        snapshot: CommunicationAnalyticsSnapshot,
        verifiedByUserId: String
    ): CommunicationSnapshotVerificationResult {
        return try {
            val recomputedHash = computeHash(snapshot)
            val matches = recomputedHash == snapshot.sha256Hash

            CommunicationSnapshotVerificationResult(
                snapshotId = snapshot.snapshotId,
                projectId = snapshot.projectId,
                storedHash = snapshot.sha256Hash,
                recomputedHash = recomputedHash,
                status = if (matches) SnapshotVerificationStatus.VERIFIED
                         else SnapshotVerificationStatus.INTEGRITY_FAILURE,
                verifiedAt = Instant.now(),
                verifiedByUserId = verifiedByUserId,
                explanation = if (matches)
                    "Snapshot hash verified successfully. Data integrity confirmed."
                else
                    "TAMPER DETECTED: Recomputed hash differs from stored hash. Snapshot data may have been modified."
            )
        } catch (e: Exception) {
            CommunicationSnapshotVerificationResult(
                snapshotId = snapshot.snapshotId,
                projectId = snapshot.projectId,
                storedHash = snapshot.sha256Hash,
                recomputedHash = null,
                status = SnapshotVerificationStatus.ERROR,
                verifiedAt = Instant.now(),
                verifiedByUserId = verifiedByUserId,
                explanation = "Verification failed due to an unexpected error: ${e.message}"
            )
        }
    }

    /**
     * Recomputes the SHA-256 hash for [snapshot] using the same deterministic
     * algorithm used at snapshot creation time.
     *
     * IMPORTANT: Keep this algorithm in sync with
     * [CommunicationAnalyticsRepositoryImpl.createSnapshot].
     */
    fun computeHash(snapshot: CommunicationAnalyticsSnapshot): String {
        val rawData = buildString {
            append(snapshot.projectId)
            append("|")
            append(snapshot.fromDate)
            append("|")
            append(snapshot.toDate)
            append("|")
            append(snapshot.kpiSummary.totalCommunications)
            append("|")
            append(snapshot.kpiSummary.deliveryRate)
            append("|")
            append(snapshot.kpiSummary.readRate)
            append("|")
            append(snapshot.governanceResult.governanceStatus)
        }
        val md = MessageDigest.getInstance("SHA-256")
        val hashBytes = md.digest(rawData.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}
