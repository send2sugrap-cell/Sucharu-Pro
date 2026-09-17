package com.sucharu.sucharupro.data.api.server

import com.sucharu.sucharupro.data.api.model.ValidationException
import com.sucharu.sucharupro.data.auth.model.ProvisionAdminRequestDto

/**
 * Top-level package helper to parse initial Owner/Admin provisioning DTOs safely.
 */
fun parseProvisionAdminRequestDto(body: Any?): ProvisionAdminRequestDto {
    if (body is ProvisionAdminRequestDto) return body
    val map = if (body is Map<*, *>) {
        body.entries.associate { (it.key?.toString() ?: "") to (it.value) }
    } else emptyMap()

    if (map.isNotEmpty()) {
        val password = (map["password"] as? String)?.ifBlank { null }
            ?: throw ValidationException("Missing 'password' parameter.")
        val identifier = (map["identifier"] as? String)?.trim()?.ifBlank { null }
            ?: (map["username"] as? String)?.trim()?.ifBlank { null }
            ?: "admin_owner"
        val username = (map["username"] as? String)?.trim()?.ifBlank { null } ?: identifier
        val email = (map["email"] as? String)?.trim()?.ifBlank { null }
        val phone = (map["phone"] as? String)?.trim()?.ifBlank { null }
        val displayName = (map["displayName"] as? String)?.trim()?.ifBlank { null } ?: "System Owner Admin"
        val requestedProjectId = (map["requestedProjectId"] as? String)?.trim()?.ifBlank { null } ?: "TENANT-001"

        return ProvisionAdminRequestDto(
            identifier = identifier,
            password = password,
            username = username,
            email = email,
            phone = phone,
            displayName = displayName,
            requestedProjectId = requestedProjectId
        )
    }
    throw ValidationException("Request body must be a valid ProvisionAdminRequestDto.")
}
