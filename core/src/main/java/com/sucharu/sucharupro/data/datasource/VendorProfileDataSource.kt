package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.VendorProfile
import kotlinx.coroutines.flow.Flow

interface VendorProfileDataSource {
    fun observeProfile(projectId: String, vendorId: String): Flow<VendorProfile?>
    suspend fun findByVendorId(projectId: String, vendorId: String): DomainResult<VendorProfile>
    suspend fun saveProfile(profile: VendorProfile): DomainResult<VendorProfile>
}
