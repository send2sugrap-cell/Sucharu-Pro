package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.CapabilityType
import com.sucharu.sucharupro.domain.model.vendor.PricingMethod
import com.sucharu.sucharupro.domain.model.vendor.RateStatus
import com.sucharu.sucharupro.domain.model.vendor.UnitOfMeasure
import com.sucharu.sucharupro.domain.model.vendor.VendorServiceRate
import kotlinx.coroutines.flow.Flow

interface VendorServiceRateDataSource {
    fun observeRates(projectId: String, vendorId: String): Flow<List<VendorServiceRate>>
    suspend fun findById(projectId: String, rateId: String): DomainResult<VendorServiceRate>
    suspend fun findByRateCode(projectId: String, rateCode: String): DomainResult<VendorServiceRate>
    suspend fun listByVendor(projectId: String, vendorId: String, status: RateStatus? = null): DomainResult<List<VendorServiceRate>>
    suspend fun listByCapability(projectId: String, capabilityType: CapabilityType, status: RateStatus? = RateStatus.ACTIVE): DomainResult<List<VendorServiceRate>>
    suspend fun findApplicableRate(
        projectId: String,
        vendorId: String,
        capabilityType: CapabilityType,
        pricingMethod: PricingMethod? = null,
        unitOfMeasure: UnitOfMeasure? = null,
        effectiveDate: Long = System.currentTimeMillis()
    ): DomainResult<VendorServiceRate>
    suspend fun createRate(rate: VendorServiceRate): DomainResult<VendorServiceRate>
    suspend fun updateRate(rate: VendorServiceRate): DomainResult<VendorServiceRate>
    suspend fun updateStatus(projectId: String, rateId: String, status: RateStatus, updatedBy: String): DomainResult<VendorServiceRate>
    suspend fun getRateHistory(projectId: String, vendorId: String, capabilityType: CapabilityType): DomainResult<List<VendorServiceRate>>
}
