package com.sucharu.sucharupro.domain.service.vendor

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.vendor.*
import com.sucharu.sucharupro.domain.repository.VendorCapabilityRepository
import com.sucharu.sucharupro.domain.repository.VendorRepository
import com.sucharu.sucharupro.domain.repository.VendorServiceRateRepository
import com.sucharu.sucharupro.domain.validation.vendor.VendorServiceRateValidator
import java.math.BigDecimal
import java.util.UUID

interface VendorServiceRateService {
    suspend fun getRateById(projectId: String, rateId: String): DomainResult<VendorServiceRate>
    suspend fun listRatesByVendor(projectId: String, vendorId: String, status: RateStatus? = null): DomainResult<List<VendorServiceRate>>
    suspend fun listRatesByCapability(projectId: String, capabilityType: CapabilityType, status: RateStatus? = RateStatus.ACTIVE): DomainResult<List<VendorServiceRate>>
    suspend fun getRateHistory(projectId: String, vendorId: String, capabilityType: CapabilityType): DomainResult<List<VendorServiceRate>>
    suspend fun resolveApplicableRate(
        projectId: String,
        vendorId: String,
        capabilityType: CapabilityType,
        pricingMethod: PricingMethod? = null,
        unitOfMeasure: UnitOfMeasure? = null,
        effectiveDate: Long = System.currentTimeMillis()
    ): DomainResult<VendorServiceRate>
    suspend fun createRate(
        projectId: String,
        vendorId: String,
        capabilityType: CapabilityType,
        serviceName: String,
        rateCode: String? = null,
        pricingMethod: PricingMethod = PricingMethod.PER_UNIT,
        unitOfMeasure: UnitOfMeasure = UnitOfMeasure.PIECE,
        rateAmount: Money,
        currency: String = "BDT",
        minimumQuantity: BigDecimal = BigDecimal.ZERO,
        maximumQuantity: BigDecimal? = null,
        effectiveFrom: Long = System.currentTimeMillis(),
        effectiveTo: Long? = null,
        status: RateStatus = RateStatus.ACTIVE,
        tiers: List<VendorServiceRateTier> = emptyList(),
        notes: String? = null,
        createdBy: String = "system"
    ): DomainResult<VendorServiceRate>
    suspend fun updateStatus(
        projectId: String,
        rateId: String,
        status: RateStatus,
        updatedBy: String = "system"
    ): DomainResult<VendorServiceRate>
    suspend fun estimateCost(
        projectId: String,
        rateId: String,
        quantity: BigDecimal,
        areaSqFt: BigDecimal? = null,
        weightKg: BigDecimal? = null,
        durationHours: BigDecimal? = null
    ): DomainResult<Money>
}

class VendorServiceRateServiceImpl(
    private val vendorRepository: VendorRepository,
    private val capabilityRepository: VendorCapabilityRepository,
    private val rateRepository: VendorServiceRateRepository
) : VendorServiceRateService {

    override suspend fun getRateById(projectId: String, rateId: String): DomainResult<VendorServiceRate> {
        val pId = projectId.trim()
        val rId = rateId.trim()
        if (pId.isBlank() || rId.isBlank()) {
            return DomainResult.Error(IllegalArgumentException("projectId and rateId cannot be blank."))
        }
        return rateRepository.findById(pId, rId)
    }

    override suspend fun listRatesByVendor(projectId: String, vendorId: String, status: RateStatus?): DomainResult<List<VendorServiceRate>> {
        val pId = projectId.trim()
        val vId = vendorId.trim()
        if (pId.isBlank() || vId.isBlank()) {
            return DomainResult.Error(IllegalArgumentException("projectId and vendorId cannot be blank."))
        }
        return rateRepository.listByVendor(pId, vId, status)
    }

    override suspend fun listRatesByCapability(projectId: String, capabilityType: CapabilityType, status: RateStatus?): DomainResult<List<VendorServiceRate>> {
        val pId = projectId.trim()
        if (pId.isBlank()) {
            return DomainResult.Error(IllegalArgumentException("projectId cannot be blank."))
        }
        return rateRepository.listByCapability(pId, capabilityType, status)
    }

    override suspend fun getRateHistory(projectId: String, vendorId: String, capabilityType: CapabilityType): DomainResult<List<VendorServiceRate>> {
        val pId = projectId.trim()
        val vId = vendorId.trim()
        if (pId.isBlank() || vId.isBlank()) {
            return DomainResult.Error(IllegalArgumentException("projectId and vendorId cannot be blank."))
        }
        return rateRepository.getRateHistory(pId, vId, capabilityType)
    }

    override suspend fun resolveApplicableRate(
        projectId: String,
        vendorId: String,
        capabilityType: CapabilityType,
        pricingMethod: PricingMethod?,
        unitOfMeasure: UnitOfMeasure?,
        effectiveDate: Long
    ): DomainResult<VendorServiceRate> {
        val pId = projectId.trim()
        val vId = vendorId.trim()
        if (pId.isBlank() || vId.isBlank()) {
            return DomainResult.Error(IllegalArgumentException("projectId and vendorId cannot be blank."))
        }

        // Verify vendor exists and is not archived
        val vendor = when (val res = vendorRepository.findById(pId, vId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Loading state"))
        }

        if (vendor.status == VendorStatus.ARCHIVED) {
            return DomainResult.Error(IllegalStateException("Cannot resolve rate for archived vendor '$vId'."))
        }

        return rateRepository.findApplicableRate(
            projectId = pId,
            vendorId = vId,
            capabilityType = capabilityType,
            pricingMethod = pricingMethod,
            unitOfMeasure = unitOfMeasure,
            effectiveDate = effectiveDate
        )
    }

    override suspend fun createRate(
        projectId: String,
        vendorId: String,
        capabilityType: CapabilityType,
        serviceName: String,
        rateCode: String?,
        pricingMethod: PricingMethod,
        unitOfMeasure: UnitOfMeasure,
        rateAmount: Money,
        currency: String,
        minimumQuantity: BigDecimal,
        maximumQuantity: BigDecimal?,
        effectiveFrom: Long,
        effectiveTo: Long?,
        status: RateStatus,
        tiers: List<VendorServiceRateTier>,
        notes: String?,
        createdBy: String
    ): DomainResult<VendorServiceRate> {
        val pId = projectId.trim()
        val vId = vendorId.trim()
        if (pId.isBlank() || vId.isBlank()) {
            return DomainResult.Error(IllegalArgumentException("projectId and vendorId cannot be blank."))
        }

        // 1. Verify vendor exists and is not archived
        val vendor = when (val res = vendorRepository.findById(pId, vId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Loading state"))
        }

        if (vendor.status == VendorStatus.ARCHIVED) {
            return DomainResult.Error(IllegalStateException("Cannot create rate for archived vendor '$vId'."))
        }

        // 2. Verify vendor possesses active capability
        val cap = when (val res = capabilityRepository.findByVendorAndType(pId, vId, capabilityType)) {
            is DomainResult.Success -> res.data
            else -> null
        }

        if (cap == null || !cap.status.isActive) {
            return DomainResult.Error(
                IllegalStateException("Vendor '$vId' does not possess active capability '${capabilityType.name}'.")
            )
        }

        // 3. Check for overlapping active rate periods for same pricing dimension
        if (status == RateStatus.ACTIVE) {
            val existingRates = when (val res = rateRepository.listByVendor(pId, vId, RateStatus.ACTIVE)) {
                is DomainResult.Success -> res.data
                else -> emptyList()
            }
            val hasOverlap = existingRates.any { existing ->
                existing.capabilityType == capabilityType &&
                existing.pricingMethod == pricingMethod &&
                existing.unitOfMeasure == unitOfMeasure &&
                checkPeriodOverlap(effectiveFrom, effectiveTo, existing.effectiveFrom, existing.effectiveTo)
            }
            if (hasOverlap) {
                return DomainResult.Error(
                    IllegalStateException("An active rate already exists for vendor '$vId' in capability '${capabilityType.name}' with overlapping effective dates.")
                )
            }
        }

        val generatedRateCode = rateCode?.trim()?.takeIf { it.isNotBlank() }
            ?: "RATE-${capabilityType.name.take(4)}-${UUID.randomUUID().toString().take(6).uppercase()}"

        val rateId = "rate_${UUID.randomUUID().toString().replace("-", "").take(16)}"

        val mappedTiers = tiers.mapIndexed { idx, t ->
            t.copy(
                tierId = if (t.tierId.isBlank()) "tier_${rateId}_${idx + 1}" else t.tierId,
                projectId = pId,
                rateId = rateId
            )
        }

        val rate = VendorServiceRate(
            rateId = rateId,
            projectId = pId,
            vendorId = vId,
            capabilityType = capabilityType,
            rateCode = generatedRateCode,
            serviceName = serviceName.trim(),
            pricingMethod = pricingMethod,
            unitOfMeasure = unitOfMeasure,
            rateAmount = rateAmount,
            currency = currency.trim().ifBlank { "BDT" },
            minimumQuantity = minimumQuantity,
            maximumQuantity = maximumQuantity,
            effectiveFrom = effectiveFrom,
            effectiveTo = effectiveTo,
            status = status,
            tiers = mappedTiers,
            notes = notes?.trim()?.takeIf { it.isNotBlank() },
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            createdBy = createdBy.trim().ifBlank { "system" },
            updatedBy = createdBy.trim().ifBlank { "system" },
            version = 1L
        )

        val validation = VendorServiceRateValidator.validate(rate)
        if (!validation.isValid) {
            return DomainResult.Error(IllegalArgumentException("Validation failed: ${validation.errorMessage}"))
        }

        return rateRepository.createRate(rate)
    }

    override suspend fun updateStatus(
        projectId: String,
        rateId: String,
        status: RateStatus,
        updatedBy: String
    ): DomainResult<VendorServiceRate> {
        val pId = projectId.trim()
        val rId = rateId.trim()
        if (pId.isBlank() || rId.isBlank()) {
            return DomainResult.Error(IllegalArgumentException("projectId and rateId cannot be blank."))
        }

        val existing = when (val res = rateRepository.findById(pId, rId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Loading state"))
        }

        val transitionVal = VendorServiceRateValidator.validateStatusTransition(existing.status, status)
        if (!transitionVal.isValid) {
            return DomainResult.Error(IllegalArgumentException(transitionVal.errorMessage))
        }

        // If transitioning to ACTIVE, check overlap
        if (status == RateStatus.ACTIVE && existing.status != RateStatus.ACTIVE) {
            val existingRates = when (val res = rateRepository.listByVendor(pId, existing.vendorId, RateStatus.ACTIVE)) {
                is DomainResult.Success -> res.data
                else -> emptyList()
            }
            val hasOverlap = existingRates.any { r ->
                r.rateId != existing.rateId &&
                r.capabilityType == existing.capabilityType &&
                r.pricingMethod == existing.pricingMethod &&
                r.unitOfMeasure == existing.unitOfMeasure &&
                checkPeriodOverlap(existing.effectiveFrom, existing.effectiveTo, r.effectiveFrom, r.effectiveTo)
            }
            if (hasOverlap) {
                return DomainResult.Error(
                    IllegalStateException("Cannot activate rate '$rId': an overlapping active rate already exists.")
                )
            }
        }

        return rateRepository.updateStatus(pId, rId, status, updatedBy.trim().ifBlank { "system" })
    }

    override suspend fun estimateCost(
        projectId: String,
        rateId: String,
        quantity: BigDecimal,
        areaSqFt: BigDecimal?,
        weightKg: BigDecimal?,
        durationHours: BigDecimal?
    ): DomainResult<Money> {
        val pId = projectId.trim()
        val rId = rateId.trim()
        if (pId.isBlank() || rId.isBlank()) {
            return DomainResult.Error(IllegalArgumentException("projectId and rateId cannot be blank."))
        }
        val rate = when (val res = rateRepository.findById(pId, rId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Loading state"))
        }
        return try {
            val cost = VendorServiceRateCalculator.calculateEstimatedCost(
                rate = rate,
                quantity = quantity,
                areaSqFt = areaSqFt,
                weightKg = weightKg,
                durationHours = durationHours
            )
            DomainResult.Success(cost)
        } catch (e: Throwable) {
            DomainResult.Error(IllegalArgumentException(e.message ?: "Failed to calculate estimated cost"))
        }
    }

    private fun checkPeriodOverlap(
        startA: Long,
        endA: Long?,
        startB: Long,
        endB: Long?
    ): Boolean {
        val effectiveEndA = endA ?: Long.MAX_VALUE
        val effectiveEndB = endB ?: Long.MAX_VALUE
        return startA <= effectiveEndB && startB <= effectiveEndA
    }
}
