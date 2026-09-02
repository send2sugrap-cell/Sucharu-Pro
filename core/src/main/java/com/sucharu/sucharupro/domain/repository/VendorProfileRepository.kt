package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.VendorProfile
import kotlinx.coroutines.flow.Flow

/**
 * Domain Repository interface for VendorProfile (Module 12 Step 02).
 */
interface VendorProfileRepository {
    fun observeProfile(projectId: String, vendorId: String): Flow<VendorProfile?>
    suspend fun findByVendorId(projectId: String, vendorId: String): DomainResult<VendorProfile>
    suspend fun saveProfile(profile: VendorProfile): DomainResult<VendorProfile>
}
