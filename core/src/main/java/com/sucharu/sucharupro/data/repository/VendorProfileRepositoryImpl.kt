package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.datasource.VendorProfileDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.VendorProfile
import com.sucharu.sucharupro.domain.repository.VendorProfileRepository
import kotlinx.coroutines.flow.Flow

class VendorProfileRepositoryImpl(
    private val dataSource: VendorProfileDataSource
) : VendorProfileRepository {

    override fun observeProfile(projectId: String, vendorId: String): Flow<VendorProfile?> {
        return dataSource.observeProfile(projectId, vendorId)
    }

    override suspend fun findByVendorId(projectId: String, vendorId: String): DomainResult<VendorProfile> {
        return dataSource.findByVendorId(projectId, vendorId)
    }

    override suspend fun saveProfile(profile: VendorProfile): DomainResult<VendorProfile> {
        return dataSource.saveProfile(profile)
    }
}
