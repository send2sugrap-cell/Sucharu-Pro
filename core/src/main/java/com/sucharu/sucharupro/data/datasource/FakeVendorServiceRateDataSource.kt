package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

class FakeVendorServiceRateDataSource : VendorServiceRateDataSource {

    private val rates = ConcurrentHashMap<String, VendorServiceRate>()
    private val flows = ConcurrentHashMap<String, MutableStateFlow<List<VendorServiceRate>>>()

    private fun key(projectId: String, rateId: String): String = "$projectId:$rateId"
    private fun vendorKey(projectId: String, vendorId: String): String = "$projectId:$vendorId"

    private fun updateFlow(projectId: String, vendorId: String) {
        val list = rates.values.filter { it.projectId == projectId && it.vendorId == vendorId }
        flows[vendorKey(projectId, vendorId)]?.value = list
    }

    override fun observeRates(projectId: String, vendorId: String): Flow<List<VendorServiceRate>> {
        val vk = vendorKey(projectId, vendorId)
        val initial = rates.values.filter { it.projectId == projectId && it.vendorId == vendorId }
        return flows.getOrPut(vk) { MutableStateFlow(initial) }.asStateFlow()
    }

    override suspend fun findById(projectId: String, rateId: String): DomainResult<VendorServiceRate> {
        val rate = rates[key(projectId, rateId)]
        return if (rate != null && rate.projectId == projectId) {
            DomainResult.Success(rate)
        } else {
            DomainResult.Error(NoSuchElementException("Vendor service rate '$rateId' not found in project '$projectId'."))
        }
    }

    override suspend fun findByRateCode(projectId: String, rateCode: String): DomainResult<VendorServiceRate> {
        val rate = rates.values.find { it.projectId == projectId && it.rateCode == rateCode }
        return if (rate != null) {
            DomainResult.Success(rate)
        } else {
            DomainResult.Error(NoSuchElementException("Vendor service rate with code '$rateCode' not found."))
        }
    }

    override suspend fun listByVendor(projectId: String, vendorId: String, status: RateStatus?): DomainResult<List<VendorServiceRate>> {
        val list = rates.values
            .filter { it.projectId == projectId && it.vendorId == vendorId && (status == null || it.status == status) }
            .sortedByDescending { it.effectiveFrom }
        return DomainResult.Success(list)
    }

    override suspend fun listByCapability(projectId: String, capabilityType: CapabilityType, status: RateStatus?): DomainResult<List<VendorServiceRate>> {
        val list = rates.values
            .filter { it.projectId == projectId && it.capabilityType == capabilityType && (status == null || it.status == status) }
            .sortedBy { it.rateAmount }
        return DomainResult.Success(list)
    }

    override suspend fun findApplicableRate(
        projectId: String,
        vendorId: String,
        capabilityType: CapabilityType,
        pricingMethod: PricingMethod?,
        unitOfMeasure: UnitOfMeasure?,
        effectiveDate: Long
    ): DomainResult<VendorServiceRate> {
        val matched = rates.values
            .filter { r ->
                r.projectId == projectId &&
                r.vendorId == vendorId &&
                r.capabilityType == capabilityType &&
                r.status == RateStatus.ACTIVE &&
                r.effectiveFrom <= effectiveDate &&
                (r.effectiveTo == null || r.effectiveTo >= effectiveDate) &&
                (pricingMethod == null || r.pricingMethod == pricingMethod) &&
                (unitOfMeasure == null || r.unitOfMeasure == unitOfMeasure)
            }
            .maxByOrNull { it.effectiveFrom }

        return if (matched != null) {
            DomainResult.Success(matched)
        } else {
            DomainResult.Error(
                NoSuchElementException("No active applicable rate found for vendor '$vendorId' in capability '${capabilityType.name}' on date '$effectiveDate'.")
            )
        }
    }

    override suspend fun createRate(rate: VendorServiceRate): DomainResult<VendorServiceRate> {
        val k = key(rate.projectId, rate.rateId)
        if (rates.containsKey(k)) {
            return DomainResult.Error(IllegalStateException("Rate '${rate.rateId}' already exists."))
        }
        if (rates.values.any { it.projectId == rate.projectId && it.rateCode == rate.rateCode }) {
            return DomainResult.Error(IllegalStateException("Rate with code '${rate.rateCode}' already exists in project '${rate.projectId}'."))
        }
        val saved = rate.copy(version = 1L)
        rates[k] = saved
        updateFlow(saved.projectId, saved.vendorId)
        return DomainResult.Success(saved)
    }

    override suspend fun updateRate(rate: VendorServiceRate): DomainResult<VendorServiceRate> {
        val k = key(rate.projectId, rate.rateId)
        val existing = rates[k] ?: return DomainResult.Error(NoSuchElementException("Rate not found."))
        if (existing.version != rate.version) {
            return DomainResult.Error(IllegalStateException("Optimistic concurrency conflict on rate '${rate.rateId}'."))
        }
        val saved = rate.copy(version = existing.version + 1L, updatedAt = System.currentTimeMillis())
        rates[k] = saved
        updateFlow(saved.projectId, saved.vendorId)
        return DomainResult.Success(saved)
    }

    override suspend fun updateStatus(projectId: String, rateId: String, status: RateStatus, updatedBy: String): DomainResult<VendorServiceRate> {
        val k = key(projectId, rateId)
        val existing = rates[k] ?: return DomainResult.Error(NoSuchElementException("Rate not found."))
        val updated = existing.copy(
            status = status,
            updatedBy = updatedBy,
            updatedAt = System.currentTimeMillis(),
            version = existing.version + 1L
        )
        rates[k] = updated
        updateFlow(updated.projectId, updated.vendorId)
        return DomainResult.Success(updated)
    }

    override suspend fun getRateHistory(projectId: String, vendorId: String, capabilityType: CapabilityType): DomainResult<List<VendorServiceRate>> {
        val list = rates.values
            .filter { it.projectId == projectId && it.vendorId == vendorId && it.capabilityType == capabilityType }
            .sortedWith(compareByDescending<VendorServiceRate> { it.effectiveFrom }.thenByDescending { it.version })
        return DomainResult.Success(list)
    }
}
