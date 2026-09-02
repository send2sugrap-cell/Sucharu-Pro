package com.sucharu.sucharupro.domain.model.communication.analytics

import java.time.Instant

/**
 * Possible outcomes of a snapshot integrity verification operation.
 */
enum class SnapshotVerificationStatus {
    /** Hash recomputed and matches the stored hash. Snapshot is untampered. */
    VERIFIED,
    /** Recomputed hash differs from the stored hash — tamper detected. */
    INTEGRITY_FAILURE,
    /** Snapshot with the given ID was not found in the data source. */
    NOT_FOUND,
    /** Snapshot data is malformed or cannot be processed. */
    INVALID,
    /** An unexpected error occurred during verification. */
    ERROR
}

/**
 * Result of a [CommunicationSnapshotVerifier] operation.
 * Contains enough information for UI display and audit recording.
 *
 * Boundary: READ + VERIFY only. Never mutates the original snapshot.
 */
data class CommunicationSnapshotVerificationResult(
    val snapshotId: String,
    val projectId: String,
    val storedHash: String?,
    val recomputedHash: String?,
    val status: SnapshotVerificationStatus,
    val verifiedAt: Instant = Instant.now(),
    val verifiedByUserId: String,
    /** Human-readable explanation of the verification outcome. */
    val explanation: String
)
