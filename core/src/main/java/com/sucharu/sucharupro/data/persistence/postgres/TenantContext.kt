package com.sucharu.sucharupro.data.persistence.postgres

/**
 * Immutable tenant scoping context for all PostgreSQL persistence operations (INFRA-01 Step 03).
 *
 * Guarantees that every database operation is explicitly bound to a tenant/project.
 */
data class TenantContext(
    val projectId: String
) {
    init {
        require(projectId.isNotBlank()) { "Tenant projectId cannot be blank for persistence operations." }
    }
}
