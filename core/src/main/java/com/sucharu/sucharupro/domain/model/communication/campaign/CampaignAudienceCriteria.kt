package com.sucharu.sucharupro.domain.model.communication.campaign

import com.sucharu.sucharupro.domain.model.user.UserRole

/**
 * Type-safe targeting criteria for audience resolution (Module 10 Step 07).
 *
 * Security: Prevents filter/SQL injection by strictly holding strongly-typed parameters.
 */
data class CampaignAudienceCriteria(
    val customerIds: Set<String> = emptySet(),
    val vendorIds: Set<String> = emptySet(),
    val roles: Set<UserRole> = emptySet(),
    val departmentIds: Set<String> = emptySet(),
    val teamIds: Set<String> = emptySet(),
    val customerSegments: Set<String> = emptySet(),
    val vendorSegments: Set<String> = emptySet(),
    val activeOnly: Boolean = true,
    val customRecipientIds: Set<String> = emptySet()
)
