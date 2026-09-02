package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.datasource.VendorServiceRateDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.CapabilityType
import com.sucharu.sucharupro.domain.model.vendor.PricingMethod
import com.sucharu.sucharupro.domain.model.vendor.RateStatus
import com.sucharu.sucharupro.domain.model.vendor.UnitOfMeasure
import com.sucharu.sucharupro.domain.model.vendor.VendorServiceRate
import com.sucharu.sucharupro.domain.repository.VendorServiceRateRepository
import kotlinx.coroutines.flow.Flow

class VendorServiceRateRepositoryImpl(
    private val dataSource: VendorServiceRateDataSource
) : VendorServiceRateRepository {

    override fun observeRates(projectId: String, vendorId: String): Flow<List<VendorServiceRate>> {
        return dataSource.observeRates(projectId, vendorId)
    }

    override suspend fun findById(projectId: String, rateId: String): DomainResult<VendorServiceRate> {
        return dataSource.findById(projectId, rateId)
    }

    override suspend fun findByRateCode(projectId: String, rateCode: String): DomainResult<VendorServiceRate> {
        return dataSource.findByRateCode(projectId, rateCode)
    }

    override suspend fun listByVendor(projectId: String, vendorId: String, status: RateStatus?): DomainResult<List<VendorServiceRate>> {
        return dataSource.listByVendor(projectId, vendorId, status)
    }

    override suspend fun listByCapability(projectId: String, capabilityType: CapabilityType, status: RateStatus?): DomainResult<List<VendorServiceRate>> {
        return dataSource.listByCapability(projectId, capabilityType, status)
    }

    override suspend fun findApplicableRate(
        projectId: String,
        vendorId: String,
        capabilityType: CapabilityType,
        pricingMethod: PricingMethod?,
        unitOfMeasure: UnitOfMeasure?,
        effectiveDate: Long
    ): DomainResult<VendorServiceRate> {
        return dataSource.findApplicableRate(
            projectId = projectId,
            vendorId = vendorId,
            capabilityType = capabilityType,
            pricingMethod = pricingMethod,
            unitOfMeasure = unitOfMeasure,
            effectiveDate = effectiveDate
        )
    }

    override suspend fun createRate(rate: VendorServiceRate): DomainResult<VendorServiceRate> {
        return dataSource.createRate(rate)
    }

    override suspend fun updateRate(rate: VendorServiceRate): DomainResult<VendorServiceRate> {
        return dataSource.updateRate(rate)
    }

    override suspend fun updateStatus(projectId: String, rateId: String, status: RateStatus, updatedBy: String): DomainResult<VendorServiceRate> {
        return dataSource.updateStatus(projectId, rateId, status, updatedBy)
    }

    override suspend fun getRateHistory(projectId: String, vendorId: String, capabilityType: CapabilityType): DomainResult<List<VendorServiceRate>> {
        return dataSource.getRateHistory(projectId, vendorId, capabilityType)
    }
}
