package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.VendorProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

class FakeVendorProfileDataSource : VendorProfileDataSource {

    private val profiles = ConcurrentHashMap<String, VendorProfile>()
    private val flows = ConcurrentHashMap<String, MutableStateFlow<VendorProfile?>>()

    private fun key(projectId: String, vendorId: String): String = "$projectId:$vendorId"

    override fun observeProfile(projectId: String, vendorId: String): Flow<VendorProfile?> {
        val k = key(projectId, vendorId)
        return flows.getOrPut(k) { MutableStateFlow(profiles[k]) }.asStateFlow()
    }

    override suspend fun findByVendorId(projectId: String, vendorId: String): DomainResult<VendorProfile> {
        val profile = profiles[key(projectId, vendorId)]
        return if (profile != null) {
            DomainResult.Success(profile)
        } else {
            DomainResult.Error(NoSuchElementException("Vendor profile not found for vendor '$vendorId'."))
        }
    }

    override suspend fun saveProfile(profile: VendorProfile): DomainResult<VendorProfile> {
        val k = key(profile.projectId, profile.vendorId)
        val existing = profiles[k]
        val saved = if (existing == null) {
            profile.copy(version = 1L)
        } else {
            if (existing.version != profile.version) {
                return DomainResult.Error(IllegalStateException("Optimistic concurrency conflict on vendor profile: '${profile.vendorId}'."))
            }
            profile.copy(version = existing.version + 1L, updatedAt = System.currentTimeMillis())
        }
        profiles[k] = saved
        flows[k]?.value = saved
        return DomainResult.Success(saved)
    }
}
