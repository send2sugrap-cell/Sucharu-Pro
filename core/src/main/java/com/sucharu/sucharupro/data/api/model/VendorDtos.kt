package com.sucharu.sucharupro.data.api.model

import com.sucharu.sucharupro.domain.model.vendor.*
import com.sucharu.sucharupro.domain.model.vendorportal.*

/**
 * Data Transfer Objects for Vendor Management REST APIs (Module 12 Steps 01 & 02).
 */

// =========================================================================
// 1. VENDOR MASTER DTOS (Step 01)
// =========================================================================

data class CreateVendorRequestDto(
    val vendorName: String,
    val vendorCode: String? = null,
    val legalName: String? = null,
    val vendorType: String? = null,
    val vendorCategory: String? = null,
    val status: String? = null,
    val primaryContactName: String? = null,
    val primaryPhone: String? = null,
    val primaryEmail: String? = null,
    val notes: String? = null
)

data class UpdateVendorRequestDto(
    val vendorName: String,
    val legalName: String? = null,
    val vendorType: String? = null,
    val vendorCategory: String? = null,
    val primaryContactName: String? = null,
    val primaryPhone: String? = null,
    val primaryEmail: String? = null,
    val notes: String? = null
)

data class UpdateVendorStatusRequestDto(
    val status: String
)

data class VendorSummaryDto(
    val vendorId: String,
    val vendorCode: String,
    val vendorName: String,
    val legalName: String? = null,
    val vendorType: String,
    val vendorCategory: String,
    val status: String,
    val primaryPhone: String? = null,
    val primaryEmail: String? = null,
    val createdAt: Long
)

data class VendorDetailDto(
    val vendorId: String,
    val projectId: String,
    val vendorCode: String,
    val vendorName: String,
    val legalName: String? = null,
    val vendorType: String,
    val vendorCategory: String,
    val status: String,
    val primaryContactName: String? = null,
    val primaryPhone: String? = null,
    val primaryEmail: String? = null,
    val notes: String? = null,
    val createdBy: String,
    val updatedBy: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val version: Long
)

fun Vendor.toDetailDto(): VendorDetailDto = VendorDetailDto(
    vendorId = vendorId,
    projectId = projectId,
    vendorCode = vendorCode,
    vendorName = vendorName,
    legalName = legalName,
    vendorType = vendorType.name,
    vendorCategory = vendorCategory.name,
    status = status.name,
    primaryContactName = primaryContactName,
    primaryPhone = primaryPhone,
    primaryEmail = primaryEmail,
    notes = notes,
    createdBy = createdBy,
    updatedBy = updatedBy,
    createdAt = createdAt,
    updatedAt = updatedAt,
    version = version
)

fun Vendor.toSummaryDto(): VendorSummaryDto = VendorSummaryDto(
    vendorId = vendorId,
    vendorCode = vendorCode,
    vendorName = vendorName,
    legalName = legalName,
    vendorType = vendorType.name,
    vendorCategory = vendorCategory.name,
    status = status.name,
    primaryPhone = primaryPhone,
    primaryEmail = primaryEmail,
    createdAt = createdAt
)

// =========================================================================
// 2. VENDOR PROFILE DTOS (Step 02)
// =========================================================================

data class UpdateVendorProfileRequestDto(
    val displayName: String,
    val legalName: String? = null,
    val contactPerson: String? = null,
    val primaryPhone: String? = null,
    val alternatePhone: String? = null,
    val email: String? = null,
    val website: String? = null,
    val taxId: String? = null,
    val businessRegistrationNumber: String? = null,
    val notes: String? = null
)

data class VendorProfileDto(
    val vendorId: String,
    val projectId: String,
    val displayName: String,
    val legalName: String? = null,
    val contactPerson: String? = null,
    val primaryPhone: String? = null,
    val alternatePhone: String? = null,
    val email: String? = null,
    val website: String? = null,
    val taxId: String? = null,
    val businessRegistrationNumber: String? = null,
    val notes: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val createdBy: String? = null,
    val updatedBy: String? = null,
    val version: Long
)

fun VendorProfile.toDto(): VendorProfileDto = VendorProfileDto(
    vendorId = vendorId,
    projectId = projectId,
    displayName = displayName,
    legalName = legalName,
    contactPerson = contactPerson,
    primaryPhone = primaryPhone,
    alternatePhone = alternatePhone,
    email = email,
    website = website,
    taxId = taxId,
    businessRegistrationNumber = businessRegistrationNumber,
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt,
    createdBy = createdBy,
    updatedBy = updatedBy,
    version = version
)

// =========================================================================
// 3. VENDOR CONTACT DTOS (Step 02)
// =========================================================================

data class CreateVendorContactRequestDto(
    val name: String,
    val contactType: String? = null,
    val designation: String? = null,
    val phone: String? = null,
    val alternatePhone: String? = null,
    val email: String? = null,
    val notes: String? = null,
    val isPrimary: Boolean = false
)

data class UpdateVendorContactRequestDto(
    val name: String,
    val contactType: String? = null,
    val designation: String? = null,
    val phone: String? = null,
    val alternatePhone: String? = null,
    val email: String? = null,
    val notes: String? = null,
    val isPrimary: Boolean = false
)

data class UpdateVendorContactStatusRequestDto(
    val active: Boolean
)

data class VendorContactDto(
    val contactId: String,
    val vendorId: String,
    val projectId: String,
    val contactType: String,
    val name: String,
    val designation: String? = null,
    val phone: String? = null,
    val alternatePhone: String? = null,
    val email: String? = null,
    val notes: String? = null,
    val isPrimary: Boolean,
    val active: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val version: Long
)

fun VendorContact.toDto(): VendorContactDto = VendorContactDto(
    contactId = contactId,
    vendorId = vendorId,
    projectId = projectId,
    contactType = contactType.name,
    name = name,
    designation = designation,
    phone = phone,
    alternatePhone = alternatePhone,
    email = email,
    notes = notes,
    isPrimary = isPrimary,
    active = active,
    createdAt = createdAt,
    updatedAt = updatedAt,
    version = version
)

// =========================================================================
// 4. VENDOR ADDRESS DTOS (Step 02)
// =========================================================================

data class CreateVendorAddressRequestDto(
    val addressLine1: String,
    val addressType: String? = null,
    val addressLine2: String? = null,
    val city: String = "Dhaka",
    val district: String? = null,
    val postalCode: String? = null,
    val country: String = "Bangladesh",
    val notes: String? = null,
    val isPrimary: Boolean = false
)

data class UpdateVendorAddressRequestDto(
    val addressLine1: String,
    val addressType: String? = null,
    val addressLine2: String? = null,
    val city: String = "Dhaka",
    val district: String? = null,
    val postalCode: String? = null,
    val country: String = "Bangladesh",
    val notes: String? = null,
    val isPrimary: Boolean = false
)

data class UpdateVendorAddressStatusRequestDto(
    val active: Boolean
)

data class VendorAddressDto(
    val addressId: String,
    val vendorId: String,
    val projectId: String,
    val addressType: String,
    val addressLine1: String,
    val addressLine2: String? = null,
    val city: String,
    val district: String? = null,
    val postalCode: String? = null,
    val country: String,
    val notes: String? = null,
    val isPrimary: Boolean,
    val active: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val version: Long
)

fun VendorAddress.toDto(): VendorAddressDto = VendorAddressDto(
    addressId = addressId,
    vendorId = vendorId,
    projectId = projectId,
    addressType = addressType.name,
    addressLine1 = addressLine1,
    addressLine2 = addressLine2,
    city = city,
    district = district,
    postalCode = postalCode,
    country = country,
    notes = notes,
    isPrimary = isPrimary,
    active = active,
    createdAt = createdAt,
    updatedAt = updatedAt,
    version = version
)

// =========================================================================
// 5. VENDOR CAPABILITY DTOS (Step 02)
// =========================================================================

data class CreateVendorCapabilityRequestDto(
    val capabilityType: String,
    val displayName: String? = null,
    val status: String? = null,
    val notes: String? = null
)

data class UpdateVendorCapabilityRequestDto(
    val displayName: String? = null,
    val status: String? = null,
    val notes: String? = null
)

data class UpdateVendorCapabilityStatusRequestDto(
    val status: String
)

data class VendorCapabilityDto(
    val capabilityId: String,
    val vendorId: String,
    val projectId: String,
    val capabilityType: String,
    val displayName: String,
    val status: String,
    val notes: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val version: Long
)

fun VendorCapability.toDto(): VendorCapabilityDto = VendorCapabilityDto(
    capabilityId = capabilityId,
    vendorId = vendorId,
    projectId = projectId,
    capabilityType = capabilityType.name,
    displayName = displayName,
    status = status.name,
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt,
    version = version
)

// =========================================================================
// 6. VENDOR SERVICE RATE DTOS (Step 03)
// =========================================================================

data class VendorServiceRateTierDto(
    val tierId: String,
    val minimumQuantity: Double,
    val maximumQuantity: Double? = null,
    val rateAmount: Double
)

fun VendorServiceRateTier.toDto(): VendorServiceRateTierDto = VendorServiceRateTierDto(
    tierId = tierId,
    minimumQuantity = minimumQuantity.toDouble(),
    maximumQuantity = maximumQuantity?.toDouble(),
    rateAmount = rateAmount.amount.toDouble()
)

data class CreateVendorServiceRateTierRequestDto(
    val minimumQuantity: Double,
    val maximumQuantity: Double? = null,
    val rateAmount: Double
)

data class CreateVendorServiceRateRequestDto(
    val capabilityType: String,
    val serviceName: String,
    val rateCode: String? = null,
    val pricingMethod: String = "PER_UNIT",
    val unitOfMeasure: String = "PIECE",
    val rateAmount: Double,
    val currency: String = "BDT",
    val minimumQuantity: Double = 0.0,
    val maximumQuantity: Double? = null,
    val effectiveFrom: Long = System.currentTimeMillis(),
    val effectiveTo: Long? = null,
    val status: String = "ACTIVE",
    val tiers: List<CreateVendorServiceRateTierRequestDto> = emptyList(),
    val notes: String? = null
)

data class UpdateVendorServiceRateStatusRequestDto(
    val status: String
)

data class VendorServiceRateDto(
    val rateId: String,
    val projectId: String,
    val vendorId: String,
    val capabilityType: String,
    val rateCode: String,
    val serviceName: String,
    val pricingMethod: String,
    val unitOfMeasure: String,
    val rateAmount: Double,
    val currency: String,
    val minimumQuantity: Double,
    val maximumQuantity: Double? = null,
    val effectiveFrom: Long,
    val effectiveTo: Long? = null,
    val status: String,
    val tiers: List<VendorServiceRateTierDto> = emptyList(),
    val notes: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val version: Long
)

fun VendorServiceRate.toDto(): VendorServiceRateDto = VendorServiceRateDto(
    rateId = rateId,
    projectId = projectId,
    vendorId = vendorId,
    capabilityType = capabilityType.name,
    rateCode = rateCode,
    serviceName = serviceName,
    pricingMethod = pricingMethod.name,
    unitOfMeasure = unitOfMeasure.name,
    rateAmount = rateAmount.amount.toDouble(),
    currency = currency,
    minimumQuantity = minimumQuantity.toDouble(),
    maximumQuantity = maximumQuantity?.toDouble(),
    effectiveFrom = effectiveFrom,
    effectiveTo = effectiveTo,
    status = status.name,
    tiers = tiers.map { it.toDto() },
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt,
    version = version
)

data class ResolveVendorRateRequestDto(
    val capabilityType: String,
    val pricingMethod: String? = null,
    val unitOfMeasure: String? = null,
    val effectiveDate: Long = System.currentTimeMillis()
)

data class EstimateVendorCostRequestDto(
    val quantity: Double,
    val areaSqFt: Double? = null,
    val weightKg: Double? = null,
    val durationHours: Double? = null
)

data class EstimateVendorCostResponseDto(
    val rateId: String,
    val estimatedCost: Double,
    val formatted: String,
    val currency: String
)

// =========================================================================
// 7. VENDOR WORK ORDER DTOS (Step 04)
// =========================================================================

data class VendorWorkOrderRateSnapshotDto(
    val sourceRateId: String? = null,
    val pricingMethod: String,
    val unitOfMeasure: String,
    val currency: String,
    val baseRate: Double,
    val resolvedUnitRate: Double,
    val tierMetadata: String? = null,
    val quantityBasis: Double,
    val resolvedAt: Long
)

fun VendorWorkOrderRateSnapshot.toDto(): VendorWorkOrderRateSnapshotDto = VendorWorkOrderRateSnapshotDto(
    sourceRateId = sourceRateId,
    pricingMethod = pricingMethod.name,
    unitOfMeasure = unitOfMeasure.name,
    currency = currency,
    baseRate = baseRate.amount.toDouble(),
    resolvedUnitRate = resolvedUnitRate.amount.toDouble(),
    tierMetadata = tierMetadata,
    quantityBasis = quantityBasis.toDouble(),
    resolvedAt = resolvedAt
)

data class CreateVendorWorkOrderRequestDto(
    val vendorId: String,
    val capabilityType: String,
    val title: String,
    val description: String? = null,
    val quantity: Double,
    val unitOfMeasure: String = "PIECE",
    val pricingMethod: String = "PER_UNIT",
    val unitRate: Double? = null,
    val serviceRateId: String? = null,
    val sourceReferenceId: String? = null,
    val sourceReferenceType: String? = null,
    val scheduledStartAt: Long? = null,
    val scheduledDueAt: Long? = null,
    val priority: String = "NORMAL",
    val notes: String? = null
)

data class UpdateVendorWorkOrderRequestDto(
    val title: String? = null,
    val description: String? = null,
    val quantity: Double? = null,
    val scheduledStartAt: Long? = null,
    val scheduledDueAt: Long? = null,
    val priority: String? = null,
    val notes: String? = null
)

data class AssignVendorWorkOrderRequestDto(
    val vendorId: String,
    val capabilityType: String,
    val unitRate: Double? = null
)

data class ChangeVendorWorkOrderStatusRequestDto(
    val status: String,
    val reason: String? = null
)

data class VendorWorkOrderDto(
    val workOrderId: String,
    val projectId: String,
    val workOrderNumber: String,
    val vendorId: String,
    val capabilityType: String,
    val serviceRateId: String? = null,
    val sourceReferenceId: String? = null,
    val sourceReferenceType: String? = null,
    val title: String,
    val description: String? = null,
    val quantity: Double,
    val unitOfMeasure: String,
    val pricingMethod: String,
    val rateSnapshot: VendorWorkOrderRateSnapshotDto,
    val currency: String,
    val estimatedAmount: Double,
    val formattedEstimatedAmount: String,
    val scheduledStartAt: Long? = null,
    val scheduledDueAt: Long? = null,
    val priority: String,
    val status: String,
    val notes: String? = null,
    val createdAt: Long,
    val createdBy: String,
    val updatedAt: Long,
    val updatedBy: String,
    val version: Long
)

fun VendorWorkOrder.toDto(): VendorWorkOrderDto = VendorWorkOrderDto(
    workOrderId = workOrderId,
    projectId = projectId,
    workOrderNumber = workOrderNumber,
    vendorId = vendorId,
    capabilityType = capabilityType.name,
    serviceRateId = serviceRateId,
    sourceReferenceId = sourceReferenceId,
    sourceReferenceType = sourceReferenceType,
    title = title,
    description = description,
    quantity = quantity.toDouble(),
    unitOfMeasure = unitOfMeasure.name,
    pricingMethod = pricingMethod.name,
    rateSnapshot = rateSnapshot.toDto(),
    currency = currency,
    estimatedAmount = estimatedAmount.amount.toDouble(),
    formattedEstimatedAmount = estimatedAmount.formatted(),
    scheduledStartAt = scheduledStartAt,
    scheduledDueAt = scheduledDueAt,
    priority = priority,
    status = status.name,
    notes = notes,
    createdAt = createdAt,
    createdBy = createdBy,
    updatedAt = updatedAt,
    updatedBy = updatedBy,
    version = version
)

data class VendorWorkOrderAuditDto(
    val auditId: String,
    val projectId: String,
    val workOrderId: String,
    val eventType: String,
    val actorId: String,
    val correlationId: String? = null,
    val occurredAt: Long,
    val details: String? = null
)

fun VendorWorkOrderAuditEvent.toDto(): VendorWorkOrderAuditDto = VendorWorkOrderAuditDto(
    auditId = auditId,
    projectId = projectId,
    workOrderId = workOrderId,
    eventType = eventType,
    actorId = actorId,
    correlationId = correlationId,
    occurredAt = occurredAt,
    details = details
)

// =========================================================================
// 8. VENDOR PURCHASE ORDER DTOS (Step 05)
// =========================================================================

data class VendorPurchaseOrderItemDto(
    val itemId: String? = null,
    val purchaseOrderId: String? = null,
    val vendorServiceRateId: String? = null,
    val capabilityType: String? = null,
    val itemDescription: String,
    val itemCode: String? = null,
    val quantity: Double,
    val unitOfMeasure: String = "PIECE",
    val unitRate: Double,
    val pricingMethod: String = "PER_UNIT",
    val currency: String = "BDT",
    val discount: Double = 0.0,
    val taxAmount: Double = 0.0,
    val lineTotal: Double = 0.0,
    val formattedLineTotal: String = "",
    val expectedDeliveryDate: Long? = null,
    val notes: String? = null,
    val sourceWorkOrderId: String? = null,
    val version: Long = 1L
)

fun VendorPurchaseOrderItem.toDto(): VendorPurchaseOrderItemDto = VendorPurchaseOrderItemDto(
    itemId = itemId,
    purchaseOrderId = purchaseOrderId,
    vendorServiceRateId = vendorServiceRateId,
    capabilityType = capabilityType?.name,
    itemDescription = itemDescription,
    itemCode = itemCode,
    quantity = quantity.toDouble(),
    unitOfMeasure = unitOfMeasure.name,
    unitRate = unitRate.amount.toDouble(),
    pricingMethod = pricingMethod.name,
    currency = currency,
    discount = discount.amount.toDouble(),
    taxAmount = taxAmount.amount.toDouble(),
    lineTotal = lineTotal.amount.toDouble(),
    formattedLineTotal = lineTotal.formatted(),
    expectedDeliveryDate = expectedDeliveryDate,
    notes = notes,
    sourceWorkOrderId = sourceWorkOrderId,
    version = version
)

fun VendorPurchaseOrderItemDto.toDomain(purchaseOrderId: String): VendorPurchaseOrderItem {
    val unit = runCatching { UnitOfMeasure.valueOf(unitOfMeasure.uppercase()) }.getOrElse { UnitOfMeasure.PIECE }
    val pricing = runCatching { PricingMethod.valueOf(pricingMethod.uppercase()) }.getOrElse { PricingMethod.PER_UNIT }
    val cap = capabilityType?.let { runCatching { CapabilityType.valueOf(it.uppercase()) }.getOrNull() }
    val qty = java.math.BigDecimal.valueOf(quantity)
    val rate = com.sucharu.sucharupro.domain.model.common.Money(unitRate)
    val disc = com.sucharu.sucharupro.domain.model.common.Money(discount)
    val tax = com.sucharu.sucharupro.domain.model.common.Money(taxAmount)
    val calculatedTotal = (rate * qty) + tax - disc

    return VendorPurchaseOrderItem(
        itemId = itemId?.trim()?.ifBlank { "poi_${java.util.UUID.randomUUID().toString().take(12)}" } ?: "poi_${java.util.UUID.randomUUID().toString().take(12)}",
        purchaseOrderId = purchaseOrderId,
        vendorServiceRateId = vendorServiceRateId,
        capabilityType = cap,
        itemDescription = itemDescription,
        itemCode = itemCode,
        quantity = qty,
        unitOfMeasure = unit,
        unitRate = rate,
        pricingMethod = pricing,
        currency = currency,
        discount = disc,
        taxAmount = tax,
        lineTotal = if (calculatedTotal.isNegative()) com.sucharu.sucharupro.domain.model.common.Money.ZERO else calculatedTotal,
        expectedDeliveryDate = expectedDeliveryDate,
        notes = notes,
        sourceWorkOrderId = sourceWorkOrderId,
        version = version
    )
}

data class CreateVendorPurchaseOrderRequestDto(
    val vendorId: String,
    val requestedBy: String? = null,
    val items: List<VendorPurchaseOrderItemDto>,
    val expectedDeliveryDate: Long? = null,
    val deliveryLocation: String? = null,
    val currency: String = "BDT",
    val taxAmount: Double = 0.0,
    val discountAmount: Double = 0.0,
    val notes: String? = null,
    val sourceReferenceType: String? = null,
    val sourceReferenceId: String? = null
)

data class UpdateVendorPurchaseOrderRequestDto(
    val items: List<VendorPurchaseOrderItemDto>? = null,
    val expectedDeliveryDate: Long? = null,
    val deliveryLocation: String? = null,
    val taxAmount: Double? = null,
    val discountAmount: Double? = null,
    val notes: String? = null
)

data class ChangeVendorPurchaseOrderStatusRequestDto(
    val status: String,
    val reason: String? = null
)

data class ReviseVendorPurchaseOrderRequestDto(
    val updatedItems: List<VendorPurchaseOrderItemDto>,
    val reason: String
)

data class VendorPurchaseOrderRevisionDto(
    val revisionId: String,
    val projectId: String,
    val purchaseOrderId: String,
    val revisionNumber: Int,
    val previousTotalAmount: Double,
    val formattedPreviousTotal: String,
    val newTotalAmount: Double,
    val formattedNewTotal: String,
    val changeSummary: String,
    val revisedBy: String,
    val revisedAt: Long
)

fun VendorPurchaseOrderRevision.toDto(): VendorPurchaseOrderRevisionDto = VendorPurchaseOrderRevisionDto(
    revisionId = revisionId,
    projectId = projectId,
    purchaseOrderId = purchaseOrderId,
    revisionNumber = revisionNumber,
    previousTotalAmount = previousTotalAmount.amount.toDouble(),
    formattedPreviousTotal = previousTotalAmount.formatted(),
    newTotalAmount = newTotalAmount.amount.toDouble(),
    formattedNewTotal = newTotalAmount.formatted(),
    changeSummary = changeSummary,
    revisedBy = revisedBy,
    revisedAt = revisedAt
)

data class VendorPurchaseOrderAuditDto(
    val auditId: String,
    val projectId: String,
    val purchaseOrderId: String,
    val eventType: String,
    val actorId: String,
    val correlationId: String? = null,
    val occurredAt: Long,
    val details: String? = null
)

fun VendorPurchaseOrderAuditEvent.toDto(): VendorPurchaseOrderAuditDto = VendorPurchaseOrderAuditDto(
    auditId = auditId,
    projectId = projectId,
    purchaseOrderId = purchaseOrderId,
    eventType = eventType,
    actorId = actorId,
    correlationId = correlationId,
    occurredAt = occurredAt,
    details = details
)

data class VendorPurchaseOrderDto(
    val purchaseOrderId: String,
    val projectId: String,
    val orderNumber: String,
    val vendorId: String,
    val status: String,
    val orderDate: Long,
    val requestedBy: String,
    val approvedBy: String? = null,
    val approvedAt: Long? = null,
    val issuedBy: String? = null,
    val issuedAt: Long? = null,
    val expectedDeliveryDate: Long? = null,
    val deliveryLocation: String? = null,
    val currency: String,
    val subtotal: Double,
    val formattedSubtotal: String,
    val taxAmount: Double,
    val formattedTaxAmount: String,
    val discountAmount: Double,
    val formattedDiscountAmount: String,
    val totalAmount: Double,
    val formattedTotalAmount: String,
    val notes: String? = null,
    val sourceReferenceType: String? = null,
    val sourceReferenceId: String? = null,
    val items: List<VendorPurchaseOrderItemDto>,
    val createdAt: Long,
    val createdBy: String,
    val updatedAt: Long,
    val updatedBy: String,
    val version: Long
)

fun VendorPurchaseOrder.toDto(): VendorPurchaseOrderDto = VendorPurchaseOrderDto(
    purchaseOrderId = purchaseOrderId,
    projectId = projectId,
    orderNumber = orderNumber,
    vendorId = vendorId,
    status = status.name,
    orderDate = orderDate,
    requestedBy = requestedBy,
    approvedBy = approvedBy,
    approvedAt = approvedAt,
    issuedBy = issuedBy,
    issuedAt = issuedAt,
    expectedDeliveryDate = expectedDeliveryDate,
    deliveryLocation = deliveryLocation,
    currency = currency,
    subtotal = subtotal.amount.toDouble(),
    formattedSubtotal = subtotal.formatted(),
    taxAmount = taxAmount.amount.toDouble(),
    formattedTaxAmount = taxAmount.formatted(),
    discountAmount = discountAmount.amount.toDouble(),
    formattedDiscountAmount = discountAmount.formatted(),
    totalAmount = totalAmount.amount.toDouble(),
    formattedTotalAmount = totalAmount.formatted(),
    notes = notes,
    sourceReferenceType = sourceReferenceType,
    sourceReferenceId = sourceReferenceId,
    items = items.map { it.toDto() },
    createdAt = createdAt,
    createdBy = createdBy,
    updatedAt = updatedAt,
    updatedBy = updatedBy,
    version = version
)

// =========================================================================
// 9. VENDOR DELIVERY RECEIPT DTOS (Step 06)
// =========================================================================

data class VendorDeliveryReceiptItemDto(
    val receiptItemId: String = "",
    val deliveryReceiptId: String = "",
    val purchaseOrderId: String = "",
    val purchaseOrderItemId: String,
    val itemDescription: String = "",
    val itemCode: String? = null,
    val orderedQuantity: Double = 0.0,
    val previouslyReceivedQuantity: Double = 0.0,
    val receivedQuantity: Double = 0.0,
    val acceptedQuantity: Double = 0.0,
    val rejectedQuantity: Double = 0.0,
    val damagedQuantity: Double = 0.0,
    val shortQuantity: Double = 0.0,
    val excessQuantity: Double = 0.0,
    val unitOfMeasure: String = "PIECE",
    val unitRate: Double = 0.0,
    val taxAmount: Double = 0.0,
    val lineTotal: Double = 0.0,
    val remarks: String? = null,
    val version: Long = 1L
)

fun VendorDeliveryReceiptItem.toDto(): VendorDeliveryReceiptItemDto = VendorDeliveryReceiptItemDto(
    receiptItemId = receiptItemId,
    deliveryReceiptId = deliveryReceiptId,
    purchaseOrderId = purchaseOrderId,
    purchaseOrderItemId = purchaseOrderItemId,
    itemDescription = itemDescription,
    itemCode = itemCode,
    orderedQuantity = orderedQuantity.toDouble(),
    previouslyReceivedQuantity = previouslyReceivedQuantity.toDouble(),
    receivedQuantity = receivedQuantity.toDouble(),
    acceptedQuantity = acceptedQuantity.toDouble(),
    rejectedQuantity = rejectedQuantity.toDouble(),
    damagedQuantity = damagedQuantity.toDouble(),
    shortQuantity = shortQuantity.toDouble(),
    excessQuantity = excessQuantity.toDouble(),
    unitOfMeasure = unitOfMeasure.name,
    unitRate = unitRate.amount.toDouble(),
    taxAmount = taxAmount.amount.toDouble(),
    lineTotal = lineTotal.amount.toDouble(),
    remarks = remarks,
    version = version
)

data class CreateVendorDeliveryReceiptRequestDto(
    val purchaseOrderId: String,
    val vendorDeliveryReference: String? = null,
    val warehouseId: String? = null,
    val remarks: String? = null,
    val items: List<VendorDeliveryReceiptItemDto> = emptyList()
)

data class UpdateVendorDeliveryReceiptRequestDto(
    val vendorDeliveryReference: String? = null,
    val warehouseId: String? = null,
    val remarks: String? = null,
    val items: List<VendorDeliveryReceiptItemDto>? = null
)

data class InspectVendorDeliveryReceiptRequestDto(
    val items: List<VendorDeliveryReceiptItemDto>,
    val remarks: String? = null
)

data class VendorDeliveryReceiptStatusChangeRequestDto(
    val reason: String? = null,
    val remarks: String? = null
)

data class VendorDeliveryReceiptDto(
    val deliveryReceiptId: String,
    val projectId: String,
    val tenantId: String,
    val receiptNumber: String,
    val purchaseOrderId: String,
    val vendorId: String,
    val vendorDeliveryReference: String? = null,
    val receiptDate: Long,
    val receivedAt: Long? = null,
    val receivedBy: String,
    val status: String,
    val warehouseId: String? = null,
    val remarks: String? = null,
    val items: List<VendorDeliveryReceiptItemDto> = emptyList(),
    val createdAt: Long,
    val createdBy: String,
    val updatedAt: Long,
    val updatedBy: String,
    val version: Long
)

fun VendorDeliveryReceipt.toDto(): VendorDeliveryReceiptDto = VendorDeliveryReceiptDto(
    deliveryReceiptId = deliveryReceiptId,
    projectId = projectId,
    tenantId = tenantId,
    receiptNumber = receiptNumber,
    purchaseOrderId = purchaseOrderId,
    vendorId = vendorId,
    vendorDeliveryReference = vendorDeliveryReference,
    receiptDate = receiptDate,
    receivedAt = receivedAt,
    receivedBy = receivedBy,
    status = status.name,
    warehouseId = warehouseId,
    remarks = remarks,
    items = items.map { it.toDto() },
    createdAt = createdAt,
    createdBy = createdBy,
    updatedAt = updatedAt,
    updatedBy = updatedBy,
    version = version
)

data class VendorDeliveryReceiptAuditDto(
    val auditId: String,
    val projectId: String,
    val deliveryReceiptId: String,
    val purchaseOrderId: String,
    val eventType: String,
    val actorId: String,
    val correlationId: String? = null,
    val occurredAt: Long,
    val details: String? = null
)

fun VendorDeliveryReceiptAuditEvent.toDto(): VendorDeliveryReceiptAuditDto = VendorDeliveryReceiptAuditDto(
    auditId = auditId,
    projectId = projectId,
    deliveryReceiptId = deliveryReceiptId,
    purchaseOrderId = purchaseOrderId,
    eventType = eventType,
    actorId = actorId,
    correlationId = correlationId,
    occurredAt = occurredAt,
    details = details
)

data class VendorPurchaseOrderReceivingSummaryDto(
    val purchaseOrderId: String,
    val projectId: String,
    val totalOrderedQuantity: Double,
    val totalReceivedQuantity: Double,
    val totalAcceptedQuantity: Double,
    val totalRejectedQuantity: Double,
    val totalDamagedQuantity: Double,
    val totalShortQuantity: Double,
    val remainingReceivableQuantity: Double,
    val receiptCount: Int,
    val isFullyReceived: Boolean,
    val lastReceiptDate: Long? = null
)

fun VendorPurchaseOrderReceivingSummary.toDto(): VendorPurchaseOrderReceivingSummaryDto = VendorPurchaseOrderReceivingSummaryDto(
    purchaseOrderId = purchaseOrderId,
    projectId = projectId,
    totalOrderedQuantity = totalOrderedQuantity.toDouble(),
    totalReceivedQuantity = totalReceivedQuantity.toDouble(),
    totalAcceptedQuantity = totalAcceptedQuantity.toDouble(),
    totalRejectedQuantity = totalRejectedQuantity.toDouble(),
    totalDamagedQuantity = totalDamagedQuantity.toDouble(),
    totalShortQuantity = totalShortQuantity.toDouble(),
    remainingReceivableQuantity = remainingReceivableQuantity.toDouble(),
    receiptCount = receiptCount,
    isFullyReceived = isFullyReceived,
    lastReceiptDate = lastReceiptDate
)

// ============================================================================
// SECTION 10: VENDOR INVOICE & 3-WAY MATCHING DTOs (Module 12 Step 07)
// ============================================================================

data class VendorInvoiceItemDto(
    val itemId: String,
    val invoiceId: String,
    val purchaseOrderItemId: String,
    val deliveryReceiptItemId: String? = null,
    val description: String,
    val quantity: Double,
    val unitOfMeasure: String,
    val unitPrice: Double,
    val taxRate: Double,
    val taxAmount: Double,
    val discountAmount: Double,
    val lineTotal: Double,
    val sequence: Int,
    val version: Long
)

data class CreateVendorInvoiceItemRequestDto(
    val purchaseOrderItemId: String,
    val deliveryReceiptItemId: String? = null,
    val description: String = "",
    val quantity: Double,
    val unitPrice: Double,
    val taxRate: Double = 0.0,
    val taxAmount: Double = 0.0,
    val discountAmount: Double = 0.0
)

data class CreateVendorInvoiceRequestDto(
    val vendorId: String,
    val purchaseOrderId: String,
    val vendorInvoiceNumber: String,
    val invoiceDate: Long = System.currentTimeMillis(),
    val receivedDate: Long = System.currentTimeMillis(),
    val currency: String = "BDT",
    val shippingAmount: Double = 0.0,
    val otherCharges: Double = 0.0,
    val notes: String? = null,
    val items: List<CreateVendorInvoiceItemRequestDto> = emptyList()
)

data class UpdateVendorInvoiceRequestDto(
    val vendorInvoiceNumber: String? = null,
    val invoiceDate: Long? = null,
    val shippingAmount: Double? = null,
    val otherCharges: Double? = null,
    val notes: String? = null,
    val items: List<CreateVendorInvoiceItemRequestDto>? = null
)

data class VendorInvoiceStatusChangeRequestDto(
    val reason: String? = null,
    val notes: String? = null
)

data class ResolveInvoiceExceptionRequestDto(
    val resolutionNotes: String
)

data class VendorInvoiceMatchLineDto(
    val matchLineId: String,
    val matchId: String,
    val invoiceItemId: String,
    val purchaseOrderItemId: String,
    val deliveryReceiptItemId: String? = null,
    val description: String,
    val orderedQuantity: Double,
    val receivedQuantity: Double,
    val invoicedQuantity: Double,
    val orderedUnitPrice: Double,
    val invoicedUnitPrice: Double,
    val quantityVariance: Double,
    val priceVariance: Double,
    val amountVariance: Double,
    val matchStatus: String,
    val exceptionReason: String? = null
)

data class VendorInvoiceMatchDto(
    val matchId: String,
    val projectId: String,
    val invoiceId: String,
    val purchaseOrderId: String,
    val matchStatus: String,
    val matchedAt: Long,
    val matchedBy: String,
    val subtotalVariance: Double,
    val quantityVariance: Double,
    val priceVariance: Double,
    val taxVariance: Double,
    val totalVariance: Double,
    val currencyMismatch: Boolean,
    val vendorMismatch: Boolean,
    val unmatchedLineCount: Int,
    val exceptionCount: Int,
    val lines: List<VendorInvoiceMatchLineDto> = emptyList(),
    val version: Long
)

data class VendorInvoiceExceptionDto(
    val exceptionId: String,
    val projectId: String,
    val invoiceId: String,
    val matchId: String,
    val exceptionType: String,
    val description: String,
    val resolved: Boolean,
    val resolvedBy: String? = null,
    val resolvedAt: Long? = null,
    val resolutionNotes: String? = null,
    val createdAt: Long
)

data class VendorInvoiceAuditDto(
    val auditId: String,
    val projectId: String,
    val invoiceId: String,
    val eventType: String,
    val actorId: String,
    val correlationId: String? = null,
    val occurredAt: Long,
    val details: String? = null
)

data class VendorInvoiceDto(
    val invoiceId: String,
    val projectId: String,
    val tenantId: String,
    val vendorId: String,
    val purchaseOrderId: String,
    val invoiceNumber: String,
    val vendorInvoiceNumber: String,
    val invoiceDate: Long,
    val receivedDate: Long,
    val currency: String,
    val subtotal: Double,
    val taxAmount: Double,
    val discountAmount: Double,
    val shippingAmount: Double,
    val otherCharges: Double,
    val totalAmount: Double,
    val notes: String? = null,
    val status: String,
    val matchStatus: String,
    val items: List<VendorInvoiceItemDto> = emptyList(),
    val createdAt: Long,
    val createdBy: String,
    val updatedAt: Long,
    val updatedBy: String,
    val version: Long
)

fun VendorInvoiceItem.toDto(): VendorInvoiceItemDto = VendorInvoiceItemDto(
    itemId = itemId,
    invoiceId = invoiceId,
    purchaseOrderItemId = purchaseOrderItemId,
    deliveryReceiptItemId = deliveryReceiptItemId,
    description = description,
    quantity = quantity.toDouble(),
    unitOfMeasure = unitOfMeasure.name,
    unitPrice = unitPrice.amount.toDouble(),
    taxRate = taxRate.toDouble(),
    taxAmount = taxAmount.amount.toDouble(),
    discountAmount = discountAmount.amount.toDouble(),
    lineTotal = lineTotal.amount.toDouble(),
    sequence = sequence,
    version = version
)

fun VendorInvoice.toDto(): VendorInvoiceDto = VendorInvoiceDto(
    invoiceId = invoiceId,
    projectId = projectId,
    tenantId = tenantId,
    vendorId = vendorId,
    purchaseOrderId = purchaseOrderId,
    invoiceNumber = invoiceNumber,
    vendorInvoiceNumber = vendorInvoiceNumber,
    invoiceDate = invoiceDate,
    receivedDate = receivedDate,
    currency = currency,
    subtotal = subtotal.amount.toDouble(),
    taxAmount = taxAmount.amount.toDouble(),
    discountAmount = discountAmount.amount.toDouble(),
    shippingAmount = shippingAmount.amount.toDouble(),
    otherCharges = otherCharges.amount.toDouble(),
    totalAmount = totalAmount.amount.toDouble(),
    notes = notes,
    status = status.name,
    matchStatus = matchStatus.name,
    items = items.map { it.toDto() },
    createdAt = createdAt,
    createdBy = createdBy,
    updatedAt = updatedAt,
    updatedBy = updatedBy,
    version = version
)

fun VendorInvoiceMatchLine.toDto(): VendorInvoiceMatchLineDto = VendorInvoiceMatchLineDto(
    matchLineId = matchLineId,
    matchId = matchId,
    invoiceItemId = invoiceItemId,
    purchaseOrderItemId = purchaseOrderItemId,
    deliveryReceiptItemId = deliveryReceiptItemId,
    description = description,
    orderedQuantity = orderedQuantity.toDouble(),
    receivedQuantity = receivedQuantity.toDouble(),
    invoicedQuantity = invoicedQuantity.toDouble(),
    orderedUnitPrice = orderedUnitPrice.amount.toDouble(),
    invoicedUnitPrice = invoicedUnitPrice.amount.toDouble(),
    quantityVariance = quantityVariance.toDouble(),
    priceVariance = priceVariance.amount.toDouble(),
    amountVariance = amountVariance.amount.toDouble(),
    matchStatus = matchStatus.name,
    exceptionReason = exceptionReason
)

fun VendorInvoiceMatch.toDto(): VendorInvoiceMatchDto = VendorInvoiceMatchDto(
    matchId = matchId,
    projectId = projectId,
    invoiceId = invoiceId,
    purchaseOrderId = purchaseOrderId,
    matchStatus = matchStatus.name,
    matchedAt = matchedAt,
    matchedBy = matchedBy,
    subtotalVariance = subtotalVariance.amount.toDouble(),
    quantityVariance = quantityVariance.toDouble(),
    priceVariance = priceVariance.amount.toDouble(),
    taxVariance = taxVariance.amount.toDouble(),
    totalVariance = totalVariance.amount.toDouble(),
    currencyMismatch = currencyMismatch,
    vendorMismatch = vendorMismatch,
    unmatchedLineCount = unmatchedLineCount,
    exceptionCount = exceptionCount,
    lines = lines.map { it.toDto() },
    version = version
)

fun VendorInvoiceException.toDto(): VendorInvoiceExceptionDto = VendorInvoiceExceptionDto(
    exceptionId = exceptionId,
    projectId = projectId,
    invoiceId = invoiceId,
    matchId = matchId,
    exceptionType = exceptionType.name,
    description = description,
    resolved = resolved,
    resolvedBy = resolvedBy,
    resolvedAt = resolvedAt,
    resolutionNotes = resolutionNotes,
    createdAt = createdAt
)

fun VendorInvoiceAuditEvent.toDto(): VendorInvoiceAuditDto = VendorInvoiceAuditDto(
    auditId = auditId,
    projectId = projectId,
    invoiceId = invoiceId,
    eventType = eventType,
    actorId = actorId,
    correlationId = correlationId,
    occurredAt = occurredAt,
    details = details
)

// ============================================================================
// SECTION 11: VENDOR QUALITY, REJECTIONS & DISPUTES (MODULE 12 STEP 08)
// ============================================================================

data class VendorQualityInspectionItemDto(
    val inspectionItemId: String,
    val inspectionId: String,
    val purchaseOrderItemId: String? = null,
    val deliveryReceiptItemId: String? = null,
    val itemDescription: String,
    val receivedQuantity: Double,
    val acceptedQuantity: Double,
    val rejectedQuantity: Double,
    val conditionalQuantity: Double,
    val defectCount: Int,
    val defectRate: Double,
    val inspectionResult: String,
    val notes: String? = null,
    val version: Long
)

data class CreateVendorQualityInspectionItemRequestDto(
    val purchaseOrderItemId: String? = null,
    val deliveryReceiptItemId: String? = null,
    val itemDescription: String,
    val receivedQuantity: Double,
    val acceptedQuantity: Double = 0.0,
    val rejectedQuantity: Double = 0.0,
    val conditionalQuantity: Double = 0.0,
    val defectCount: Int = 0,
    val inspectionResult: String = "ACCEPTED",
    val notes: String? = null
)

data class CreateVendorQualityInspectionRequestDto(
    val vendorId: String,
    val purchaseOrderId: String? = null,
    val deliveryReceiptId: String? = null,
    val inspectionReference: String,
    val inspectionType: String = "RECEIVING_INSPECTION",
    val receivedQuantity: Double,
    val notes: String? = null,
    val items: List<CreateVendorQualityInspectionItemRequestDto> = emptyList()
)

data class UpdateVendorQualityInspectionRequestDto(
    val notes: String? = null,
    val overallResult: String? = null
)

data class CompleteVendorQualityInspectionRequestDto(
    val status: String,
    val overallResult: String,
    val acceptedQuantity: Double,
    val rejectedQuantity: Double,
    val conditionalQuantity: Double = 0.0
)

data class VendorQualityInspectionDto(
    val inspectionId: String,
    val projectId: String,
    val tenantId: String,
    val vendorId: String,
    val purchaseOrderId: String? = null,
    val deliveryReceiptId: String? = null,
    val inspectionReference: String,
    val inspectionType: String,
    val inspectionStatus: String,
    val inspectedBy: String? = null,
    val inspectionStartedAt: Long? = null,
    val inspectionCompletedAt: Long? = null,
    val receivedQuantity: Double,
    val acceptedQuantity: Double,
    val rejectedQuantity: Double,
    val conditionalQuantity: Double,
    val overallResult: String? = null,
    val notes: String? = null,
    val items: List<VendorQualityInspectionItemDto> = emptyList(),
    val createdAt: Long,
    val createdBy: String,
    val updatedAt: Long,
    val updatedBy: String,
    val version: Long
)

data class CreateVendorDefectRequestDto(
    val inspectionItemId: String? = null,
    val vendorId: String,
    val defectType: String = "QUALITY_DEFECT",
    val severity: String = "MEDIUM",
    val description: String,
    val quantityAffected: Double = 0.0,
    val evidenceReference: String? = null
)

data class VendorDefectDto(
    val defectId: String,
    val projectId: String,
    val tenantId: String,
    val inspectionId: String,
    val inspectionItemId: String? = null,
    val vendorId: String,
    val defectType: String,
    val severity: String,
    val description: String,
    val quantityAffected: Double,
    val detectedAt: Long,
    val detectedBy: String,
    val evidenceReference: String? = null,
    val status: String,
    val resolutionReference: String? = null,
    val createdAt: Long,
    val version: Long
)

data class CreateVendorRejectionRequestDto(
    val vendorId: String,
    val purchaseOrderId: String? = null,
    val deliveryReceiptId: String? = null,
    val deliveryReceiptItemId: String? = null,
    val inspectionId: String? = null,
    val rejectionReference: String,
    val rejectionType: String = "QUALITY_REJECTION",
    val rejectionReason: String,
    val rejectedQuantity: Double,
    val rejectedValue: Double = 0.0,
    val disposition: String = "RETURN_TO_VENDOR",
    val replacementRequired: Boolean = false,
    val returnRequired: Boolean = true,
    val creditRequired: Boolean = false,
    val notes: String? = null
)

data class UpdateVendorRejectionRequestDto(
    val rejectionReason: String,
    val rejectedQuantity: Double,
    val rejectedValue: Double = 0.0,
    val disposition: String,
    val replacementRequired: Boolean,
    val returnRequired: Boolean,
    val creditRequired: Boolean,
    val notes: String? = null
)

data class ResolveRejectionRequestDto(
    val resolutionNotes: String
)

data class VendorRejectionDto(
    val rejectionId: String,
    val projectId: String,
    val tenantId: String,
    val vendorId: String,
    val purchaseOrderId: String? = null,
    val deliveryReceiptId: String? = null,
    val deliveryReceiptItemId: String? = null,
    val inspectionId: String? = null,
    val rejectionReference: String,
    val rejectionType: String,
    val rejectionReason: String,
    val rejectedQuantity: Double,
    val rejectedValue: Double,
    val status: String,
    val disposition: String,
    val replacementRequired: Boolean,
    val returnRequired: Boolean,
    val creditRequired: Boolean,
    val notes: String? = null,
    val vendorResponse: String? = null,
    val vendorResponseAt: Long? = null,
    val resolutionNotes: String? = null,
    val resolvedAt: Long? = null,
    val resolvedBy: String? = null,
    val createdAt: Long,
    val createdBy: String,
    val updatedAt: Long,
    val updatedBy: String,
    val version: Long
)

data class CreateVendorDisputeRequestDto(
    val vendorId: String,
    val purchaseOrderId: String? = null,
    val deliveryReceiptId: String? = null,
    val invoiceId: String? = null,
    val inspectionId: String? = null,
    val rejectionId: String? = null,
    val disputeReference: String,
    val disputeType: String = "QUALITY",
    val priority: String = "MEDIUM",
    val subject: String,
    val description: String,
    val disputedQuantity: Double = 0.0,
    val disputedAmount: Double = 0.0,
    val vendorResponseDueAt: Long? = null
)

data class UpdateVendorDisputeRequestDto(
    val priority: String,
    val subject: String,
    val description: String,
    val disputedQuantity: Double = 0.0,
    val disputedAmount: Double = 0.0,
    val vendorResponseDueAt: Long? = null
)

data class AssignDisputeRequestDto(
    val assignedTo: String
)

data class VendorResponseRequestDto(
    val vendorResponse: String
)

data class EscalateDisputeRequestDto(
    val reason: String
)

data class ProposeResolutionRequestDto(
    val proposal: String
)

data class ResolveDisputeRequestDto(
    val resolution: String
)

data class VendorDisputeDto(
    val disputeId: String,
    val projectId: String,
    val tenantId: String,
    val vendorId: String,
    val purchaseOrderId: String? = null,
    val deliveryReceiptId: String? = null,
    val invoiceId: String? = null,
    val inspectionId: String? = null,
    val rejectionId: String? = null,
    val disputeReference: String,
    val disputeType: String,
    val priority: String,
    val status: String,
    val subject: String,
    val description: String,
    val disputedQuantity: Double,
    val disputedAmount: Double,
    val raisedBy: String,
    val assignedTo: String? = null,
    val vendorResponseDueAt: Long? = null,
    val vendorResponse: String? = null,
    val vendorResponseAt: Long? = null,
    val resolutionProposal: String? = null,
    val resolution: String? = null,
    val resolvedAt: Long? = null,
    val resolvedBy: String? = null,
    val closedAt: Long? = null,
    val closedBy: String? = null,
    val createdAt: Long,
    val createdBy: String,
    val updatedAt: Long,
    val updatedBy: String,
    val version: Long
)

data class VendorDisputeEventDto(
    val eventId: String,
    val projectId: String,
    val tenantId: String,
    val disputeId: String,
    val eventType: String,
    val actorId: String,
    val notes: String? = null,
    val payloadJson: String? = null,
    val occurredAt: Long
)

data class AddEvidenceRequestDto(
    val sourceType: String,
    val sourceId: String,
    val fileReference: String,
    val fileName: String,
    val fileType: String,
    val description: String? = null,
    val checksum: String? = null
)

data class VendorQualityEvidenceDto(
    val evidenceId: String,
    val projectId: String,
    val tenantId: String,
    val sourceType: String,
    val sourceId: String,
    val fileReference: String,
    val fileName: String,
    val fileType: String,
    val description: String? = null,
    val uploadedBy: String,
    val uploadedAt: Long,
    val checksum: String? = null
)

data class VendorQualityAuditDto(
    val auditId: String,
    val projectId: String,
    val tenantId: String,
    val entityType: String,
    val entityId: String,
    val eventType: String,
    val actorId: String,
    val correlationId: String? = null,
    val occurredAt: Long,
    val details: String? = null
)

// Extension mappers
fun VendorQualityInspectionItem.toDto(): VendorQualityInspectionItemDto = VendorQualityInspectionItemDto(
    inspectionItemId = inspectionItemId,
    inspectionId = inspectionId,
    purchaseOrderItemId = purchaseOrderItemId,
    deliveryReceiptItemId = deliveryReceiptItemId,
    itemDescription = itemDescription,
    receivedQuantity = receivedQuantity.toDouble(),
    acceptedQuantity = acceptedQuantity.toDouble(),
    rejectedQuantity = rejectedQuantity.toDouble(),
    conditionalQuantity = conditionalQuantity.toDouble(),
    defectCount = defectCount,
    defectRate = defectRate.toDouble(),
    inspectionResult = inspectionResult.name,
    notes = notes,
    version = version
)

fun VendorQualityInspection.toDto(): VendorQualityInspectionDto = VendorQualityInspectionDto(
    inspectionId = inspectionId,
    projectId = projectId,
    tenantId = tenantId,
    vendorId = vendorId,
    purchaseOrderId = purchaseOrderId,
    deliveryReceiptId = deliveryReceiptId,
    inspectionReference = inspectionReference,
    inspectionType = inspectionType.name,
    inspectionStatus = inspectionStatus.name,
    inspectedBy = inspectedBy,
    inspectionStartedAt = inspectionStartedAt,
    inspectionCompletedAt = inspectionCompletedAt,
    receivedQuantity = receivedQuantity.toDouble(),
    acceptedQuantity = acceptedQuantity.toDouble(),
    rejectedQuantity = rejectedQuantity.toDouble(),
    conditionalQuantity = conditionalQuantity.toDouble(),
    overallResult = overallResult?.name,
    notes = notes,
    items = items.map { it.toDto() },
    createdAt = createdAt,
    createdBy = createdBy,
    updatedAt = updatedAt,
    updatedBy = updatedBy,
    version = version
)

fun VendorDefect.toDto(): VendorDefectDto = VendorDefectDto(
    defectId = defectId,
    projectId = projectId,
    tenantId = tenantId,
    inspectionId = inspectionId,
    inspectionItemId = inspectionItemId,
    vendorId = vendorId,
    defectType = defectType.name,
    severity = severity.name,
    description = description,
    quantityAffected = quantityAffected.toDouble(),
    detectedAt = detectedAt,
    detectedBy = detectedBy,
    evidenceReference = evidenceReference,
    status = status,
    resolutionReference = resolutionReference,
    createdAt = createdAt,
    version = version
)

fun VendorRejection.toDto(): VendorRejectionDto = VendorRejectionDto(
    rejectionId = rejectionId,
    projectId = projectId,
    tenantId = tenantId,
    vendorId = vendorId,
    purchaseOrderId = purchaseOrderId,
    deliveryReceiptId = deliveryReceiptId,
    deliveryReceiptItemId = deliveryReceiptItemId,
    inspectionId = inspectionId,
    rejectionReference = rejectionReference,
    rejectionType = rejectionType,
    rejectionReason = rejectionReason,
    rejectedQuantity = rejectedQuantity.toDouble(),
    rejectedValue = rejectedValue.amount.toDouble(),
    status = status.name,
    disposition = disposition.name,
    replacementRequired = replacementRequired,
    returnRequired = returnRequired,
    creditRequired = creditRequired,
    notes = notes,
    vendorResponse = vendorResponse,
    vendorResponseAt = vendorResponseAt,
    resolutionNotes = resolutionNotes,
    resolvedAt = resolvedAt,
    resolvedBy = resolvedBy,
    createdAt = createdAt,
    createdBy = createdBy,
    updatedAt = updatedAt,
    updatedBy = updatedBy,
    version = version
)

fun VendorDispute.toDto(): VendorDisputeDto = VendorDisputeDto(
    disputeId = disputeId,
    projectId = projectId,
    tenantId = tenantId,
    vendorId = vendorId,
    purchaseOrderId = purchaseOrderId,
    deliveryReceiptId = deliveryReceiptId,
    invoiceId = invoiceId,
    inspectionId = inspectionId,
    rejectionId = rejectionId,
    disputeReference = disputeReference,
    disputeType = disputeType.name,
    priority = priority.name,
    status = status.name,
    subject = subject,
    description = description,
    disputedQuantity = disputedQuantity.toDouble(),
    disputedAmount = disputedAmount.amount.toDouble(),
    raisedBy = raisedBy,
    assignedTo = assignedTo,
    vendorResponseDueAt = vendorResponseDueAt,
    vendorResponse = vendorResponse,
    vendorResponseAt = vendorResponseAt,
    resolutionProposal = resolutionProposal,
    resolution = resolution,
    resolvedAt = resolvedAt,
    resolvedBy = resolvedBy,
    closedAt = closedAt,
    closedBy = closedBy,
    createdAt = createdAt,
    createdBy = createdBy,
    updatedAt = updatedAt,
    updatedBy = updatedBy,
    version = version
)

fun VendorDisputeEvent.toDto(): VendorDisputeEventDto = VendorDisputeEventDto(
    eventId = eventId,
    projectId = projectId,
    tenantId = tenantId,
    disputeId = disputeId,
    eventType = eventType.name,
    actorId = actorId,
    notes = notes,
    payloadJson = payloadJson,
    occurredAt = occurredAt
)

fun VendorQualityEvidence.toDto(): VendorQualityEvidenceDto = VendorQualityEvidenceDto(
    evidenceId = evidenceId,
    projectId = projectId,
    tenantId = tenantId,
    sourceType = sourceType,
    sourceId = sourceId,
    fileReference = fileReference,
    fileName = fileName,
    fileType = fileType,
    description = description,
    uploadedBy = uploadedBy,
    uploadedAt = uploadedAt,
    checksum = checksum
)

fun VendorQualityAuditEvent.toDto(): VendorQualityAuditDto = VendorQualityAuditDto(
    auditId = auditId,
    projectId = projectId,
    tenantId = tenantId,
    entityType = entityType,
    entityId = entityId,
    eventType = eventType,
    actorId = actorId,
    correlationId = correlationId,
    occurredAt = occurredAt,
    details = details
)

// ============================================================================
// SECTION 12 — VENDOR PERFORMANCE, EVALUATION & COMPLIANCE (Step 09)
// ============================================================================

data class CreateVendorPerformanceKpiRequestDto(
    val code: String,
    val name: String,
    val description: String,
    val kpiType: String = "OPERATIONAL",
    val measurementMethod: String = "AUTOMATED",
    val targetValue: Double,
    val minimumAcceptableValue: Double? = null,
    val maximumAcceptableValue: Double? = null,
    val unit: String = "%",
    val direction: String = "HIGHER_IS_BETTER",
    val weight: Double = 1.0,
    val effectiveFrom: Long? = null,
    val effectiveTo: Long? = null
)

data class UpdateVendorPerformanceKpiRequestDto(
    val name: String? = null,
    val description: String? = null,
    val targetValue: Double? = null,
    val minimumAcceptableValue: Double? = null,
    val maximumAcceptableValue: Double? = null,
    val weight: Double? = null,
    val status: String? = null,
    val effectiveTo: Long? = null
)

data class VendorPerformanceKpiDto(
    val kpiId: String,
    val projectId: String,
    val tenantId: String,
    val code: String,
    val name: String,
    val description: String,
    val kpiType: String,
    val measurementMethod: String,
    val targetValue: Double,
    val minimumAcceptableValue: Double?,
    val maximumAcceptableValue: Double?,
    val unit: String,
    val direction: String,
    val weight: Double,
    val status: String,
    val effectiveFrom: Long,
    val effectiveTo: Long?,
    val version: Long,
    val createdAt: Long,
    val createdBy: String,
    val updatedAt: Long,
    val updatedBy: String?
)

data class CreateVendorPerformanceMeasurementRequestDto(
    val kpiId: String,
    val periodStart: Long,
    val periodEnd: Long,
    val actualValue: Double,
    val numerator: Double,
    val denominator: Double,
    val unit: String = "%",
    val sampleSize: Int = 1,
    val confidenceState: String = "SUFFICIENT_DATA"
)

data class VendorPerformanceMeasurementDto(
    val measurementId: String,
    val projectId: String,
    val tenantId: String,
    val vendorId: String,
    val kpiId: String,
    val kpiCode: String,
    val periodStart: Long,
    val periodEnd: Long,
    val actualValue: Double,
    val numerator: Double,
    val denominator: Double,
    val unit: String,
    val sampleSize: Int,
    val confidenceState: String,
    val calculationVersion: String,
    val measuredAt: Long,
    val measuredBy: String
)

data class GenerateVendorPerformanceScorecardRequestDto(
    val periodType: String = "MONTHLY",
    val periodStart: Long,
    val periodEnd: Long,
    val notes: String? = null
)

data class VendorPerformanceScorecardItemDto(
    val itemId: String,
    val scorecardId: String,
    val kpiId: String,
    val kpiCode: String,
    val kpiName: String,
    val kpiType: String,
    val weight: Double,
    val direction: String,
    val targetValue: Double,
    val actualValue: Double,
    val normalizedScore: Double,
    val weightedScore: Double,
    val numerator: Double,
    val denominator: Double,
    val unit: String,
    val sampleSize: Int,
    val confidenceState: String
)

data class VendorPerformanceScorecardDto(
    val scorecardId: String,
    val projectId: String,
    val tenantId: String,
    val vendorId: String,
    val periodType: String,
    val periodStart: Long,
    val periodEnd: Long,
    val overallScore: Double,
    val rating: String,
    val riskLevel: String,
    val dataCompleteness: Double,
    val sampleSize: Int,
    val calculationVersion: String,
    val status: String,
    val items: List<VendorPerformanceScorecardItemDto>,
    val notes: String?,
    val version: Long,
    val generatedAt: Long,
    val generatedBy: String,
    val approvedAt: Long?,
    val approvedBy: String?
)

data class VendorEvaluationCriterionDto(
    val criterionId: String,
    val evaluationId: String,
    val name: String,
    val category: String,
    val weight: Double,
    val score: Double,
    val comments: String?
)

data class CreateVendorEvaluationRequestDto(
    val scorecardId: String? = null,
    val periodType: String = "MONTHLY",
    val periodStart: Long,
    val periodEnd: Long,
    val evaluatorComments: String? = null,
    val criteria: List<VendorEvaluationCriterionDto> = emptyList()
)

data class SubmitVendorEvaluationRequestDto(
    val comments: String? = null
)

data class ReviewVendorEvaluationRequestDto(
    val reviewComments: String
)

data class ApproveVendorEvaluationRequestDto(
    val decision: String = "APPROVED", // APPROVED, CONDITIONALLY_APPROVED, ACTION_REQUIRED
    val comments: String? = null
)

data class RejectVendorEvaluationRequestDto(
    val reason: String
)

data class VendorEvaluationDto(
    val evaluationId: String,
    val projectId: String,
    val tenantId: String,
    val vendorId: String,
    val scorecardId: String?,
    val periodType: String,
    val periodStart: Long,
    val periodEnd: Long,
    val evaluatorId: String,
    val evaluatorName: String,
    val status: String,
    val decision: String?,
    val evaluationScore: Double,
    val rating: String,
    val evaluatorComments: String?,
    val reviewComments: String?,
    val rejectionReason: String?,
    val criteria: List<VendorEvaluationCriterionDto>,
    val submittedAt: Long?,
    val submittedBy: String?,
    val reviewedAt: Long?,
    val reviewedBy: String?,
    val approvedAt: Long?,
    val approvedBy: String?,
    val finalizedAt: Long?,
    val finalizedBy: String?,
    val version: Long,
    val createdAt: Long,
    val createdBy: String,
    val updatedAt: Long,
    val updatedBy: String?
)

data class CreateVendorComplianceRequirementRequestDto(
    val requirementType: String,
    val code: String,
    val name: String,
    val description: String,
    val mandatory: Boolean = true,
    val riskLevel: String = "HIGH",
    val validityDays: Int? = 365
)

data class VendorComplianceRequirementDto(
    val requirementId: String,
    val projectId: String,
    val tenantId: String,
    val requirementType: String,
    val code: String,
    val name: String,
    val description: String,
    val mandatory: Boolean,
    val riskLevel: String,
    val validityDays: Int?,
    val status: String,
    val version: Long,
    val createdAt: Long,
    val createdBy: String,
    val updatedAt: Long,
    val updatedBy: String?
)

data class VendorComplianceEvidenceDto(
    val evidenceId: String,
    val recordId: String,
    val projectId: String,
    val tenantId: String,
    val evidenceType: String,
    val fileName: String,
    val fileUrl: String,
    val checksum: String?,
    val fileSizeBytes: Long,
    val mimeType: String?,
    val uploadedBy: String,
    val uploadedAt: Long
)

data class SubmitVendorComplianceRecordRequestDto(
    val requirementId: String,
    val effectiveDate: Long? = null,
    val expiryDate: Long? = null,
    val notes: String? = null,
    val evidenceList: List<VendorComplianceEvidenceDto> = emptyList()
)

data class VerifyVendorComplianceRecordRequestDto(
    val verified: Boolean = true,
    val rejectionReason: String? = null,
    val notes: String? = null
)

data class VendorComplianceRecordDto(
    val recordId: String,
    val projectId: String,
    val tenantId: String,
    val vendorId: String,
    val requirementId: String,
    val requirementCode: String,
    val requirementName: String,
    val requirementType: String,
    val mandatory: Boolean,
    val effectiveDate: Long,
    val expiryDate: Long?,
    val status: String,
    val riskLevel: String,
    val verificationStatus: String,
    val verifiedBy: String?,
    val verifiedAt: Long?,
    val rejectionReason: String?,
    val notes: String?,
    val evidenceList: List<VendorComplianceEvidenceDto>,
    val version: Long,
    val createdAt: Long,
    val createdBy: String,
    val updatedAt: Long,
    val updatedBy: String?
)

data class CreateVendorCorrectiveActionRequestDto(
    val sourceType: String, // KPI, QUALITY, DISPUTE, EVALUATION, COMPLIANCE
    val sourceId: String? = null,
    val issueDescription: String,
    val rootCause: String? = null,
    val actionPlan: String,
    val assignedTo: String,
    val assignedToName: String,
    val priority: String = "MEDIUM",
    val dueDate: Long
)

data class StartVendorCorrectiveActionRequestDto(
    val notes: String? = null
)

data class VerifyVendorCorrectiveActionRequestDto(
    val verificationNotes: String
)

data class CloseVendorCorrectiveActionRequestDto(
    val notes: String? = null
)

data class VendorCorrectiveActionDto(
    val actionId: String,
    val projectId: String,
    val tenantId: String,
    val vendorId: String,
    val sourceType: String,
    val sourceId: String?,
    val issueDescription: String,
    val rootCause: String?,
    val actionPlan: String,
    val assignedTo: String,
    val assignedToName: String,
    val priority: String,
    val dueDate: Long,
    val status: String,
    val startedAt: Long?,
    val completedAt: Long?,
    val closedAt: Long?,
    val verificationNotes: String?,
    val verifiedBy: String?,
    val verifiedAt: Long?,
    val version: Long,
    val createdAt: Long,
    val createdBy: String,
    val updatedAt: Long,
    val updatedBy: String?
)

data class VendorRiskIndicatorDto(
    val riskId: String,
    val projectId: String,
    val tenantId: String,
    val vendorId: String,
    val riskType: String,
    val severity: String,
    val source: String,
    val sourceId: String?,
    val title: String,
    val description: String,
    val evidenceReference: String?,
    val detectedAt: Long,
    val status: String
)

data class VendorPerformanceTrendPointDto(
    val periodStart: Long,
    val periodEnd: Long,
    val overallScore: Double,
    val qualityScore: Double,
    val deliveryScore: Double,
    val costScore: Double,
    val complianceScore: Double,
    val disputeCount: Int,
    val rating: String
)

data class VendorPerformanceAuditDto(
    val auditId: String,
    val projectId: String,
    val tenantId: String,
    val entityType: String,
    val entityId: String,
    val eventType: String,
    val action: String,
    val actorId: String,
    val actorRole: String?,
    val details: String?,
    val occurredAt: Long
)

fun VendorPerformanceKpi.toDto(): VendorPerformanceKpiDto = VendorPerformanceKpiDto(
    kpiId = kpiId,
    projectId = projectId,
    tenantId = tenantId,
    code = code,
    name = name,
    description = description,
    kpiType = kpiType.name,
    measurementMethod = measurementMethod.name,
    targetValue = targetValue,
    minimumAcceptableValue = minimumAcceptableValue,
    maximumAcceptableValue = maximumAcceptableValue,
    unit = unit,
    direction = direction.name,
    weight = weight,
    status = status.name,
    effectiveFrom = effectiveFrom.toEpochMilli(),
    effectiveTo = effectiveTo?.toEpochMilli(),
    version = version,
    createdAt = createdAt.toEpochMilli(),
    createdBy = createdBy,
    updatedAt = updatedAt.toEpochMilli(),
    updatedBy = updatedBy
)

fun VendorPerformanceMeasurement.toDto(): VendorPerformanceMeasurementDto = VendorPerformanceMeasurementDto(
    measurementId = measurementId,
    projectId = projectId,
    tenantId = tenantId,
    vendorId = vendorId,
    kpiId = kpiId,
    kpiCode = kpiCode,
    periodStart = periodStart.toEpochMilli(),
    periodEnd = periodEnd.toEpochMilli(),
    actualValue = actualValue,
    numerator = numerator,
    denominator = denominator,
    unit = unit,
    sampleSize = sampleSize,
    confidenceState = confidenceState.name,
    calculationVersion = calculationVersion,
    measuredAt = measuredAt.toEpochMilli(),
    measuredBy = measuredBy
)

fun VendorPerformanceScorecardItem.toDto(): VendorPerformanceScorecardItemDto = VendorPerformanceScorecardItemDto(
    itemId = itemId,
    scorecardId = scorecardId,
    kpiId = kpiId,
    kpiCode = kpiCode,
    kpiName = kpiName,
    kpiType = kpiType.name,
    weight = weight,
    direction = direction.name,
    targetValue = targetValue,
    actualValue = actualValue,
    normalizedScore = normalizedScore,
    weightedScore = weightedScore,
    numerator = numerator,
    denominator = denominator,
    unit = unit,
    sampleSize = sampleSize,
    confidenceState = confidenceState.name
)

fun VendorPerformanceScorecard.toDto(): VendorPerformanceScorecardDto = VendorPerformanceScorecardDto(
    scorecardId = scorecardId,
    projectId = projectId,
    tenantId = tenantId,
    vendorId = vendorId,
    periodType = periodType.name,
    periodStart = periodStart.toEpochMilli(),
    periodEnd = periodEnd.toEpochMilli(),
    overallScore = overallScore,
    rating = rating.name,
    riskLevel = riskLevel.name,
    dataCompleteness = dataCompleteness,
    sampleSize = sampleSize,
    calculationVersion = calculationVersion,
    status = status.name,
    items = items.map { it.toDto() },
    notes = notes,
    version = version,
    generatedAt = generatedAt.toEpochMilli(),
    generatedBy = generatedBy,
    approvedAt = approvedAt?.toEpochMilli(),
    approvedBy = approvedBy
)

fun VendorEvaluationCriterion.toDto(): VendorEvaluationCriterionDto = VendorEvaluationCriterionDto(
    criterionId = criterionId,
    evaluationId = evaluationId,
    name = name,
    category = category,
    weight = weight,
    score = score,
    comments = comments
)

fun VendorEvaluation.toDto(): VendorEvaluationDto = VendorEvaluationDto(
    evaluationId = evaluationId,
    projectId = projectId,
    tenantId = tenantId,
    vendorId = vendorId,
    scorecardId = scorecardId,
    periodType = periodType.name,
    periodStart = periodStart.toEpochMilli(),
    periodEnd = periodEnd.toEpochMilli(),
    evaluatorId = evaluatorId,
    evaluatorName = evaluatorName,
    status = status.name,
    decision = decision?.name,
    evaluationScore = evaluationScore,
    rating = rating.name,
    evaluatorComments = evaluatorComments,
    reviewComments = reviewComments,
    rejectionReason = rejectionReason,
    criteria = criteria.map { it.toDto() },
    submittedAt = submittedAt?.toEpochMilli(),
    submittedBy = submittedBy,
    reviewedAt = reviewedAt?.toEpochMilli(),
    reviewedBy = reviewedBy,
    approvedAt = approvedAt?.toEpochMilli(),
    approvedBy = approvedBy,
    finalizedAt = finalizedAt?.toEpochMilli(),
    finalizedBy = finalizedBy,
    version = version,
    createdAt = createdAt.toEpochMilli(),
    createdBy = createdBy,
    updatedAt = updatedAt.toEpochMilli(),
    updatedBy = updatedBy
)

fun VendorComplianceRequirement.toDto(): VendorComplianceRequirementDto = VendorComplianceRequirementDto(
    requirementId = requirementId,
    projectId = projectId,
    tenantId = tenantId,
    requirementType = requirementType.name,
    code = code,
    name = name,
    description = description,
    mandatory = mandatory,
    riskLevel = riskLevel.name,
    validityDays = validityDays,
    status = status.name,
    version = version,
    createdAt = createdAt.toEpochMilli(),
    createdBy = createdBy,
    updatedAt = updatedAt.toEpochMilli(),
    updatedBy = updatedBy
)

fun VendorComplianceEvidence.toDto(): VendorComplianceEvidenceDto = VendorComplianceEvidenceDto(
    evidenceId = evidenceId,
    recordId = recordId,
    projectId = projectId,
    tenantId = tenantId,
    evidenceType = evidenceType.name,
    fileName = fileName,
    fileUrl = fileUrl,
    checksum = checksum,
    fileSizeBytes = fileSizeBytes,
    mimeType = mimeType,
    uploadedBy = uploadedBy,
    uploadedAt = uploadedAt.toEpochMilli()
)

fun VendorComplianceRecord.toDto(): VendorComplianceRecordDto = VendorComplianceRecordDto(
    recordId = recordId,
    projectId = projectId,
    tenantId = tenantId,
    vendorId = vendorId,
    requirementId = requirementId,
    requirementCode = requirementCode,
    requirementName = requirementName,
    requirementType = requirementType.name,
    mandatory = mandatory,
    effectiveDate = effectiveDate.toEpochMilli(),
    expiryDate = expiryDate?.toEpochMilli(),
    status = status.name,
    riskLevel = riskLevel.name,
    verificationStatus = verificationStatus.name,
    verifiedBy = verifiedBy,
    verifiedAt = verifiedAt?.toEpochMilli(),
    rejectionReason = rejectionReason,
    notes = notes,
    evidenceList = evidenceList.map { it.toDto() },
    version = version,
    createdAt = createdAt.toEpochMilli(),
    createdBy = createdBy,
    updatedAt = updatedAt.toEpochMilli(),
    updatedBy = updatedBy
)

fun VendorCorrectiveAction.toDto(): VendorCorrectiveActionDto = VendorCorrectiveActionDto(
    actionId = actionId,
    projectId = projectId,
    tenantId = tenantId,
    vendorId = vendorId,
    sourceType = sourceType,
    sourceId = sourceId,
    issueDescription = issueDescription,
    rootCause = rootCause,
    actionPlan = actionPlan,
    assignedTo = assignedTo,
    assignedToName = assignedToName,
    priority = priority.name,
    dueDate = dueDate.toEpochMilli(),
    status = status.name,
    startedAt = startedAt?.toEpochMilli(),
    completedAt = completedAt?.toEpochMilli(),
    closedAt = closedAt?.toEpochMilli(),
    verificationNotes = verificationNotes,
    verifiedBy = verifiedBy,
    verifiedAt = verifiedAt?.toEpochMilli(),
    version = version,
    createdAt = createdAt.toEpochMilli(),
    createdBy = createdBy,
    updatedAt = updatedAt.toEpochMilli(),
    updatedBy = updatedBy
)

fun VendorRiskIndicator.toDto(): VendorRiskIndicatorDto = VendorRiskIndicatorDto(
    riskId = riskId,
    projectId = projectId,
    tenantId = tenantId,
    vendorId = vendorId,
    riskType = riskType.name,
    severity = severity.name,
    source = source,
    sourceId = sourceId,
    title = title,
    description = description,
    evidenceReference = evidenceReference,
    detectedAt = detectedAt.toEpochMilli(),
    status = status.name
)

fun VendorPerformanceTrendPoint.toDto(): VendorPerformanceTrendPointDto = VendorPerformanceTrendPointDto(
    periodStart = periodStart.toEpochMilli(),
    periodEnd = periodEnd.toEpochMilli(),
    overallScore = overallScore,
    qualityScore = qualityScore,
    deliveryScore = deliveryScore,
    costScore = costScore,
    complianceScore = complianceScore,
    disputeCount = disputeCount,
    rating = rating.name
)

fun VendorPerformanceAuditEvent.toDto(): VendorPerformanceAuditDto = VendorPerformanceAuditDto(
    auditId = auditId,
    projectId = projectId,
    tenantId = tenantId,
    entityType = entityType,
    entityId = entityId,
    eventType = eventType.name,
    action = action,
    actorId = actorId,
    actorRole = actorRole,
    details = details,
    occurredAt = occurredAt.toEpochMilli()
)

// ======================================================================================
// MODULE 12 STEP 10: VENDOR SETTLEMENT, ANALYTICS & INTEGRATION DTOs
// ======================================================================================

data class VendorSettlementAllocationDto(
    val allocationId: String,
    val settlementId: String,
    val payableId: String,
    val invoiceId: String? = null,
    val allocatedAmount: Double,
    val formattedAllocatedAmount: String,
    val currency: String,
    val status: String,
    val createdAt: Long,
    val createdBy: String
)

fun VendorSettlementAllocation.toDto(): VendorSettlementAllocationDto = VendorSettlementAllocationDto(
    allocationId = allocationId,
    settlementId = settlementId,
    payableId = payableId,
    invoiceId = invoiceId,
    allocatedAmount = allocatedAmount.amount.toDouble(),
    formattedAllocatedAmount = allocatedAmount.formatted(),
    currency = currency,
    status = status,
    createdAt = createdAt,
    createdBy = createdBy
)

data class VendorSettlementDto(
    val settlementId: String,
    val projectId: String,
    val tenantId: String,
    val vendorId: String,
    val settlementNumber: String,
    val settlementDate: Long,
    val currency: String,
    val totalAmount: Double,
    val formattedTotalAmount: String,
    val status: String,
    val settlementMethod: String,
    val referenceNumber: String? = null,
    val paymentId: String? = null,
    val notes: String? = null,
    val approvedBy: String? = null,
    val approvedAt: Long? = null,
    val settledAt: Long? = null,
    val allocations: List<VendorSettlementAllocationDto> = emptyList(),
    val createdAt: Long,
    val createdBy: String,
    val updatedAt: Long,
    val updatedBy: String,
    val version: Long
)

fun VendorSettlement.toDto(): VendorSettlementDto = VendorSettlementDto(
    settlementId = settlementId,
    projectId = projectId,
    tenantId = tenantId,
    vendorId = vendorId,
    settlementNumber = settlementNumber,
    settlementDate = settlementDate,
    currency = currency,
    totalAmount = totalAmount.amount.toDouble(),
    formattedTotalAmount = totalAmount.formatted(),
    status = status.name,
    settlementMethod = settlementMethod.name,
    referenceNumber = referenceNumber,
    paymentId = paymentId,
    notes = notes,
    approvedBy = approvedBy,
    approvedAt = approvedAt,
    settledAt = settledAt,
    allocations = allocations.map { it.toDto() },
    createdAt = createdAt,
    createdBy = createdBy,
    updatedAt = updatedAt,
    updatedBy = updatedBy,
    version = version
)

data class CreateVendorSettlementRequestDto(
    val vendorId: String,
    val settlementNumber: String,
    val totalAmount: Double,
    val currency: String = "BDT",
    val settlementMethod: String = "BANK_TRANSFER",
    val referenceNumber: String? = null,
    val notes: String? = null,
    val allocations: List<CreateVendorSettlementAllocationRequestDto> = emptyList()
)

data class CreateVendorSettlementAllocationRequestDto(
    val payableId: String,
    val invoiceId: String? = null,
    val allocatedAmount: Double,
    val currency: String = "BDT"
)

data class SettlementEligibilityResultDto(
    val vendorId: String,
    val payableId: String? = null,
    val status: String,
    val isEligible: Boolean,
    val reasons: List<String>,
    val payableReferences: List<String>,
    val grossPayable: Double,
    val approvedAmount: Double,
    val previouslySettledAmount: Double,
    val creditsAmount: Double,
    val outstandingAmount: Double,
    val currency: String
)

fun SettlementEligibilityResult.toDto(): SettlementEligibilityResultDto = SettlementEligibilityResultDto(
    vendorId = vendorId,
    payableId = payableId,
    status = status.name,
    isEligible = isEligible,
    reasons = reasons,
    payableReferences = payableReferences,
    grossPayable = grossPayable.amount.toDouble(),
    approvedAmount = approvedAmount.amount.toDouble(),
    previouslySettledAmount = previouslySettledAmount.amount.toDouble(),
    creditsAmount = creditsAmount.amount.toDouble(),
    outstandingAmount = outstandingAmount.amount.toDouble(),
    currency = currency
)

data class VendorReconciliationResultDto(
    val reconciliationId: String,
    val vendorId: String,
    val projectId: String,
    val tenantId: String,
    val settlementId: String? = null,
    val payableId: String? = null,
    val paymentId: String? = null,
    val status: String,
    val expectedAmount: Double,
    val settledAmount: Double,
    val paidAmount: Double,
    val ledgerAmount: Double,
    val variance: Double,
    val reasons: List<String>,
    val reconciledAt: Long,
    val reconciledBy: String
)

fun VendorReconciliationResult.toDto(): VendorReconciliationResultDto = VendorReconciliationResultDto(
    reconciliationId = reconciliationId,
    vendorId = vendorId,
    projectId = projectId,
    tenantId = tenantId,
    settlementId = settlementId,
    payableId = payableId,
    paymentId = paymentId,
    status = status.name,
    expectedAmount = expectedAmount.amount.toDouble(),
    settledAmount = settledAmount.amount.toDouble(),
    paidAmount = paidAmount.amount.toDouble(),
    ledgerAmount = ledgerAmount.amount.toDouble(),
    variance = variance.amount.toDouble(),
    reasons = reasons,
    reconciledAt = reconciledAt,
    reconciledBy = reconciledBy
)

data class VendorFinancialSummaryDto(
    val vendorId: String,
    val currency: String,
    val totalPoValue: Double,
    val totalInvoicedValue: Double,
    val totalApprovedPayable: Double,
    val totalSettledAmount: Double,
    val totalOutstandingPayable: Double,
    val averageInvoiceValue: Double,
    val paymentCycleDays: Double,
    val priceVarianceAmount: Double,
    val creditAdjustmentAmount: Double,
    val disputeExposureAmount: Double
)

fun VendorFinancialSummary.toDto(): VendorFinancialSummaryDto = VendorFinancialSummaryDto(
    vendorId = vendorId,
    currency = currency,
    totalPoValue = totalPoValue.amount.toDouble(),
    totalInvoicedValue = totalInvoicedValue.amount.toDouble(),
    totalApprovedPayable = totalApprovedPayable.amount.toDouble(),
    totalSettledAmount = totalSettledAmount.amount.toDouble(),
    totalOutstandingPayable = totalOutstandingPayable.amount.toDouble(),
    averageInvoiceValue = averageInvoiceValue.amount.toDouble(),
    paymentCycleDays = paymentCycleDays,
    priceVarianceAmount = priceVarianceAmount.amount.toDouble(),
    creditAdjustmentAmount = creditAdjustmentAmount.amount.toDouble(),
    disputeExposureAmount = disputeExposureAmount.amount.toDouble()
)

data class VendorOperationalSummaryDto(
    val vendorId: String,
    val orderCount: Int,
    val openOrders: Int,
    val completedOrders: Int,
    val deliveryCount: Int,
    val acceptedQuantity: Double,
    val rejectedQuantity: Double,
    val onTimeDeliveryRate: Double,
    val partialReceiptRate: Double,
    val inspectedQuantity: Double,
    val defectRate: Double,
    val rejectionRate: Double,
    val invoiceCount: Int,
    val matchedInvoiceCount: Int,
    val mismatchRate: Double,
    val openDisputes: Int,
    val resolvedDisputes: Int,
    val assignedJobs: Int,
    val completedJobs: Int,
    val jobCompletionRate: Double
)

fun VendorOperationalSummary.toDto(): VendorOperationalSummaryDto = VendorOperationalSummaryDto(
    vendorId = vendorId,
    orderCount = orderCount,
    openOrders = openOrders,
    completedOrders = completedOrders,
    deliveryCount = deliveryCount,
    acceptedQuantity = acceptedQuantity,
    rejectedQuantity = rejectedQuantity,
    onTimeDeliveryRate = onTimeDeliveryRate,
    partialReceiptRate = partialReceiptRate,
    inspectedQuantity = inspectedQuantity,
    defectRate = defectRate,
    rejectionRate = rejectionRate,
    invoiceCount = invoiceCount,
    matchedInvoiceCount = matchedInvoiceCount,
    mismatchRate = mismatchRate,
    openDisputes = openDisputes,
    resolvedDisputes = resolvedDisputes,
    assignedJobs = assignedJobs,
    completedJobs = completedJobs,
    jobCompletionRate = jobCompletionRate
)

data class VendorQualitySummaryDto(
    val vendorId: String,
    val inspectedQuantity: Double,
    val acceptedQuantity: Double,
    val rejectedQuantity: Double,
    val defectRate: Double,
    val rejectionRate: Double,
    val openDefectsCount: Int,
    val criticalDefectsCount: Int
)

fun VendorQualitySummary.toDto(): VendorQualitySummaryDto = VendorQualitySummaryDto(
    vendorId = vendorId,
    inspectedQuantity = inspectedQuantity,
    acceptedQuantity = acceptedQuantity,
    rejectedQuantity = rejectedQuantity,
    defectRate = defectRate,
    rejectionRate = rejectionRate,
    openDefectsCount = openDefectsCount,
    criticalDefectsCount = criticalDefectsCount
)

data class VendorDeliverySummaryDto(
    val vendorId: String,
    val deliveryReceiptCount: Int,
    val receivedQuantity: Double,
    val acceptedQuantity: Double,
    val rejectedQuantity: Double,
    val onTimeDeliveryRate: Double,
    val delayedDeliveryCount: Int
)

fun VendorDeliverySummary.toDto(): VendorDeliverySummaryDto = VendorDeliverySummaryDto(
    vendorId = vendorId,
    deliveryReceiptCount = deliveryReceiptCount,
    receivedQuantity = receivedQuantity,
    acceptedQuantity = acceptedQuantity,
    rejectedQuantity = rejectedQuantity,
    onTimeDeliveryRate = onTimeDeliveryRate,
    delayedDeliveryCount = delayedDeliveryCount
)

data class VendorInvoiceSummaryDto(
    val vendorId: String,
    val invoiceCount: Int,
    val matchedCount: Int,
    val unmatchedCount: Int,
    val totalInvoiced: Double,
    val totalApproved: Double,
    val exceptionCount: Int
)

fun VendorInvoiceSummary.toDto(): VendorInvoiceSummaryDto = VendorInvoiceSummaryDto(
    vendorId = vendorId,
    invoiceCount = invoiceCount,
    matchedCount = matchedCount,
    unmatchedCount = unmatchedCount,
    totalInvoiced = totalInvoiced.amount.toDouble(),
    totalApproved = totalApproved.amount.toDouble(),
    exceptionCount = exceptionCount
)

data class VendorPerformanceSummaryDto(
    val vendorId: String,
    val latestScore: Double,
    val rating: String,
    val riskLevel: String,
    val scorecardCount: Int,
    val evaluationCount: Int,
    val openCapaCount: Int,
    val resolvedCapaCount: Int
)

fun VendorPerformanceSummary.toDto(): VendorPerformanceSummaryDto = VendorPerformanceSummaryDto(
    vendorId = vendorId,
    latestScore = latestScore,
    rating = rating,
    riskLevel = riskLevel,
    scorecardCount = scorecardCount,
    evaluationCount = evaluationCount,
    openCapaCount = openCapaCount,
    resolvedCapaCount = resolvedCapaCount
)

data class VendorComplianceSummaryDto(
    val vendorId: String,
    val totalRequirements: Int,
    val verifiedCount: Int,
    val pendingCount: Int,
    val expiringSoonCount: Int,
    val expiredCount: Int,
    val complianceScore: Double,
    val criticalRisksCount: Int
)

fun VendorComplianceSummary.toDto(): VendorComplianceSummaryDto = VendorComplianceSummaryDto(
    vendorId = vendorId,
    totalRequirements = totalRequirements,
    verifiedCount = verifiedCount,
    pendingCount = pendingCount,
    expiringSoonCount = expiringSoonCount,
    expiredCount = expiredCount,
    complianceScore = complianceScore,
    criticalRisksCount = criticalRisksCount
)

data class VendorRiskSummaryDto(
    val vendorId: String,
    val overallRiskLevel: String,
    val activeRiskIndicators: List<String>,
    val criticalIssuesCount: Int,
    val unresolvedDisputesCount: Int,
    val overdueCapaCount: Int
)

fun VendorRiskSummary.toDto(): VendorRiskSummaryDto = VendorRiskSummaryDto(
    vendorId = vendorId,
    overallRiskLevel = overallRiskLevel,
    activeRiskIndicators = activeRiskIndicators,
    criticalIssuesCount = criticalIssuesCount,
    unresolvedDisputesCount = unresolvedDisputesCount,
    overdueCapaCount = overdueCapaCount
)

data class Vendor360SummaryDto(
    val vendorId: String,
    val vendorCode: String,
    val vendorName: String,
    val status: String,
    val financial: VendorFinancialSummaryDto,
    val operational: VendorOperationalSummaryDto,
    val quality: VendorQualitySummaryDto,
    val delivery: VendorDeliverySummaryDto,
    val invoice: VendorInvoiceSummaryDto,
    val performance: VendorPerformanceSummaryDto,
    val compliance: VendorComplianceSummaryDto,
    val risk: VendorRiskSummaryDto
)

fun Vendor360Summary.toDto(): Vendor360SummaryDto = Vendor360SummaryDto(
    vendorId = vendorId,
    vendorCode = vendorCode,
    vendorName = vendorName,
    status = status,
    financial = financial.toDto(),
    operational = operational.toDto(),
    quality = quality.toDto(),
    delivery = delivery.toDto(),
    invoice = invoice.toDto(),
    performance = performance.toDto(),
    compliance = compliance.toDto(),
    risk = risk.toDto()
)

data class VendorAnalyticsSnapshotDto(
    val snapshotId: String,
    val vendorId: String,
    val projectId: String,
    val tenantId: String,
    val period: String,
    val startDate: Long,
    val endDate: Long,
    val generatedAt: Long,
    val generatedBy: String,
    val calculationVersion: String,
    val metricsJson: String
)

fun VendorAnalyticsSnapshot.toDto(): VendorAnalyticsSnapshotDto = VendorAnalyticsSnapshotDto(
    snapshotId = snapshotId,
    vendorId = vendorId,
    projectId = projectId,
    tenantId = tenantId,
    period = period.name,
    startDate = startDate,
    endDate = endDate,
    generatedAt = generatedAt,
    generatedBy = generatedBy,
    calculationVersion = calculationVersion,
    metricsJson = metricsJson
)

data class VendorAnalyticsTrendPointDto(
    val periodKey: String,
    val timestamp: Long,
    val poValue: Double,
    val invoicedValue: Double,
    val settledValue: Double,
    val qualityScore: Double,
    val onTimeDeliveryRate: Double,
    val performanceScore: Double
)

fun VendorAnalyticsTrendPoint.toDto(): VendorAnalyticsTrendPointDto = VendorAnalyticsTrendPointDto(
    periodKey = periodKey,
    timestamp = timestamp,
    poValue = poValue,
    invoicedValue = invoicedValue,
    settledValue = settledValue,
    qualityScore = qualityScore,
    onTimeDeliveryRate = onTimeDeliveryRate,
    performanceScore = performanceScore
)

data class VendorSettlementAuditEventDto(
    val eventId: String,
    val settlementId: String,
    val vendorId: String,
    val projectId: String,
    val tenantId: String,
    val eventType: String,
    val details: String,
    val actor: String,
    val timestamp: Long
)

fun VendorSettlementAuditEvent.toDto(): VendorSettlementAuditEventDto = VendorSettlementAuditEventDto(
    eventId = eventId,
    settlementId = settlementId,
    vendorId = vendorId,
    projectId = projectId,
    tenantId = tenantId,
    eventType = eventType.name,
    details = details,
    actor = actor,
    timestamp = timestamp
)

// =========================================================================
// 21. VENDOR PORTAL FOUNDATION & SECURE ACCESS DTOS (Module 13 Step 01)
// =========================================================================

data class CreateVendorPortalAccountRequestDto(
    val vendorId: String,
    val portalCode: String,
    val primaryContactEmail: String? = null,
    val primaryContactPhone: String? = null
)

data class UpdateVendorPortalAccountStatusRequestDto(
    val status: String,
    val reason: String? = null
)

data class VendorPortalAccountDto(
    val portalAccountId: String,
    val vendorId: String,
    val tenantId: String,
    val projectId: String,
    val status: String,
    val portalCode: String,
    val primaryContactEmail: String? = null,
    val primaryContactPhone: String? = null,
    val activatedAt: Long? = null,
    val activatedBy: String? = null,
    val suspendedAt: Long? = null,
    val suspendedBy: String? = null,
    val suspensionReason: String? = null,
    val createdAt: Long,
    val createdBy: String,
    val updatedAt: Long,
    val updatedBy: String,
    val version: Long
)

fun VendorPortalAccount.toDto(): VendorPortalAccountDto = VendorPortalAccountDto(
    portalAccountId = portalAccountId,
    vendorId = vendorId,
    tenantId = tenantId,
    projectId = projectId,
    status = status.name,
    portalCode = portalCode,
    primaryContactEmail = primaryContactEmail,
    primaryContactPhone = primaryContactPhone,
    activatedAt = activatedAt,
    activatedBy = activatedBy,
    suspendedAt = suspendedAt,
    suspendedBy = suspendedBy,
    suspensionReason = suspensionReason,
    createdAt = createdAt,
    createdBy = createdBy,
    updatedAt = updatedAt,
    updatedBy = updatedBy,
    version = version
)

data class InviteVendorPortalUserRequestDto(
    val portalAccountId: String,
    val vendorId: String,
    val userId: String,
    val role: String = "VENDOR_OPERATOR",
    val projectScope: String = "*"
)

data class ActivateVendorPortalMembershipRequestDto(
    val invitationToken: String
)

data class UpdateVendorPortalMembershipStatusRequestDto(
    val status: String
)

data class VendorPortalMembershipDto(
    val membershipId: String,
    val portalAccountId: String,
    val vendorId: String,
    val userId: String,
    val tenantId: String,
    val projectScope: String,
    val role: String,
    val roleDisplayName: String,
    val status: String,
    val invitationExpiresAt: Long? = null,
    val activatedAt: Long? = null,
    val lastAccessAt: Long? = null,
    val createdAt: Long,
    val createdBy: String,
    val updatedAt: Long,
    val updatedBy: String,
    val version: Long
)

fun VendorPortalMembership.toDto(): VendorPortalMembershipDto = VendorPortalMembershipDto(
    membershipId = membershipId,
    portalAccountId = portalAccountId,
    vendorId = vendorId,
    userId = userId,
    tenantId = tenantId,
    projectScope = projectScope,
    role = role.name,
    roleDisplayName = role.displayName,
    status = status.name,
    invitationExpiresAt = invitationExpiresAt,
    activatedAt = activatedAt,
    lastAccessAt = lastAccessAt,
    createdAt = createdAt,
    createdBy = createdBy,
    updatedAt = updatedAt,
    updatedBy = updatedBy,
    version = version
)

data class UpdateVendorPortalPolicyRequestDto(
    val vendorId: String? = null,
    val allowRfqSubmission: Boolean = true,
    val allowPoAcknowledgement: Boolean = true,
    val allowInvoiceSubmission: Boolean = true,
    val allowQualityDispute: Boolean = true,
    val requireTwoFactorAuth: Boolean = false,
    val ipWhitelist: String? = null,
    val sessionInactivityTimeoutMinutes: Int = 30,
    val maxActiveSessionsPerUser: Int = 5
)

data class VendorPortalAccessPolicyDto(
    val policyId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String? = null,
    val allowRfqSubmission: Boolean,
    val allowPoAcknowledgement: Boolean,
    val allowInvoiceSubmission: Boolean,
    val allowQualityDispute: Boolean,
    val requireTwoFactorAuth: Boolean,
    val ipWhitelist: String? = null,
    val sessionInactivityTimeoutMinutes: Int,
    val maxActiveSessionsPerUser: Int,
    val createdAt: Long,
    val createdBy: String,
    val updatedAt: Long,
    val updatedBy: String,
    val version: Long
)

fun VendorPortalAccessPolicy.toDto(): VendorPortalAccessPolicyDto = VendorPortalAccessPolicyDto(
    policyId = policyId,
    tenantId = tenantId,
    projectId = projectId,
    vendorId = vendorId,
    allowRfqSubmission = allowRfqSubmission,
    allowPoAcknowledgement = allowPoAcknowledgement,
    allowInvoiceSubmission = allowInvoiceSubmission,
    allowQualityDispute = allowQualityDispute,
    requireTwoFactorAuth = requireTwoFactorAuth,
    ipWhitelist = ipWhitelist,
    sessionInactivityTimeoutMinutes = sessionInactivityTimeoutMinutes,
    maxActiveSessionsPerUser = maxActiveSessionsPerUser,
    createdAt = createdAt,
    createdBy = createdBy,
    updatedAt = updatedAt,
    updatedBy = updatedBy,
    version = version
)

data class VendorPortalSessionDto(
    val sessionId: String,
    val membershipId: String,
    val userId: String,
    val vendorId: String,
    val tenantId: String,
    val projectId: String,
    val ipAddress: String? = null,
    val userAgent: String? = null,
    val status: String,
    val expiresAt: Long,
    val lastActivityAt: Long,
    val createdAt: Long
)

fun VendorPortalSession.toDto(): VendorPortalSessionDto = VendorPortalSessionDto(
    sessionId = sessionId,
    membershipId = membershipId,
    userId = userId,
    vendorId = vendorId,
    tenantId = tenantId,
    projectId = projectId,
    ipAddress = ipAddress,
    userAgent = userAgent,
    status = status.name,
    expiresAt = expiresAt,
    lastActivityAt = lastActivityAt,
    createdAt = createdAt
)

data class VendorPortalAuditEventDto(
    val eventId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val membershipId: String? = null,
    val actorUserId: String,
    val eventType: String,
    val action: String,
    val targetId: String? = null,
    val result: String,
    val details: String,
    val ipAddress: String? = null,
    val correlationId: String? = null,
    val timestamp: Long
)

fun VendorPortalAuditEvent.toDto(): VendorPortalAuditEventDto = VendorPortalAuditEventDto(
    eventId = eventId,
    tenantId = tenantId,
    projectId = projectId,
    vendorId = vendorId,
    membershipId = membershipId,
    actorUserId = actorUserId,
    eventType = eventType.name,
    action = action,
    targetId = targetId,
    result = result,
    details = details,
    ipAddress = ipAddress,
    correlationId = correlationId,
    timestamp = timestamp
)

data class VendorPortalAccessContextDto(
    val userId: String,
    val vendorId: String,
    val vendorCode: String,
    val vendorName: String,
    val membershipId: String,
    val role: String,
    val roleDisplayName: String,
    val tenantId: String,
    val projectScope: String,
    val accountStatus: String,
    val membershipStatus: String,
    val policy: VendorPortalAccessPolicyDto,
    val allowedFeatures: List<String>
)

fun VendorPortalAccessContext.toDto(): VendorPortalAccessContextDto = VendorPortalAccessContextDto(
    userId = userId,
    vendorId = vendorId,
    vendorCode = vendorCode,
    vendorName = vendorName,
    membershipId = membershipId,
    role = role.name,
    roleDisplayName = role.displayName,
    tenantId = tenantId,
    projectScope = projectScope,
    accountStatus = accountStatus.name,
    membershipStatus = membershipStatus.name,
    policy = policy.toDto(),
    allowedFeatures = allowedFeatures
)

// =========================================================================
// 22. VENDOR PORTAL DASHBOARD & WORKSPACE DTOS (Module 13 Step 02)
// =========================================================================

data class VendorPortalKpiDto(
    val key: String,
    val label: String,
    val value: String,
    val numericValue: Double? = null,
    val unit: String? = null,
    val trend: String? = null,
    val status: String = "NORMAL",
    val category: String = "OPERATIONAL"
)

fun VendorPortalKpi.toDto(): VendorPortalKpiDto = VendorPortalKpiDto(
    key = key,
    label = label,
    value = value,
    numericValue = numericValue,
    unit = unit,
    trend = trend,
    status = status,
    category = category
)

data class VendorPortalProfileSummaryDto(
    val vendorId: String,
    val vendorCode: String,
    val vendorName: String,
    val legalName: String? = null,
    val vendorType: String,
    val category: String,
    val status: String,
    val primaryContactName: String? = null,
    val primaryContactEmail: String? = null,
    val primaryContactPhone: String? = null,
    val address: String? = null,
    val portalAccountStatus: String,
    val portalRole: String,
    val projectScope: String,
    val serviceCount: Int = 0,
    val capabilityCount: Int = 0,
    val activeRatesCount: Int = 0
)

fun VendorPortalProfileSummary.toDto(): VendorPortalProfileSummaryDto = VendorPortalProfileSummaryDto(
    vendorId = vendorId,
    vendorCode = vendorCode,
    vendorName = vendorName,
    legalName = legalName,
    vendorType = vendorType,
    category = category,
    status = status,
    primaryContactName = primaryContactName,
    primaryContactEmail = primaryContactEmail,
    primaryContactPhone = primaryContactPhone,
    address = address,
    portalAccountStatus = portalAccountStatus,
    portalRole = portalRole,
    projectScope = projectScope,
    serviceCount = serviceCount,
    capabilityCount = capabilityCount,
    activeRatesCount = activeRatesCount
)

data class VendorPortalOperationalSummaryDto(
    val totalPurchaseOrders: Int,
    val activePurchaseOrders: Int,
    val completedPurchaseOrders: Int,
    val totalWorkOrders: Int,
    val openWorkOrders: Int,
    val completedWorkOrders: Int,
    val totalDeliveries: Int,
    val pendingDeliveries: Int,
    val acceptedDeliveries: Int,
    val onTimeDeliveryRatePercent: Double,
    val poFulfillmentRatePercent: Double
)

fun VendorPortalOperationalSummary.toDto(): VendorPortalOperationalSummaryDto = VendorPortalOperationalSummaryDto(
    totalPurchaseOrders = totalPurchaseOrders,
    activePurchaseOrders = activePurchaseOrders,
    completedPurchaseOrders = completedPurchaseOrders,
    totalWorkOrders = totalWorkOrders,
    openWorkOrders = openWorkOrders,
    completedWorkOrders = completedWorkOrders,
    totalDeliveries = totalDeliveries,
    pendingDeliveries = pendingDeliveries,
    acceptedDeliveries = acceptedDeliveries,
    onTimeDeliveryRatePercent = onTimeDeliveryRatePercent,
    poFulfillmentRatePercent = poFulfillmentRatePercent
)

data class VendorPortalFinancialSummaryDto(
    val totalInvoices: Int,
    val pendingInvoices: Int,
    val approvedInvoices: Int,
    val paidInvoices: Int,
    val disputedInvoices: Int,
    val totalInvoicedAmount: Double,
    val totalPaidAmount: Double,
    val totalOutstandingPayables: Double,
    val currency: String,
    val totalSettlements: Int,
    val pendingSettlementsCount: Int,
    val lastSettlementDate: Long? = null
)

fun VendorPortalFinancialSummary.toDto(): VendorPortalFinancialSummaryDto = VendorPortalFinancialSummaryDto(
    totalInvoices = totalInvoices,
    pendingInvoices = pendingInvoices,
    approvedInvoices = approvedInvoices,
    paidInvoices = paidInvoices,
    disputedInvoices = disputedInvoices,
    totalInvoicedAmount = totalInvoicedAmount.amount.toDouble(),
    totalPaidAmount = totalPaidAmount.amount.toDouble(),
    totalOutstandingPayables = totalOutstandingPayables.amount.toDouble(),
    currency = "BDT",
    totalSettlements = totalSettlements,
    pendingSettlementsCount = pendingSettlementsCount,
    lastSettlementDate = lastSettlementDate
)

data class VendorPortalQualitySummaryDto(
    val totalInspections: Int,
    val passedInspections: Int,
    val failedInspections: Int,
    val overallDefectRatePercent: Double,
    val totalRejections: Int,
    val openRejections: Int,
    val openDisputes: Int,
    val qualityRating: String
)

fun VendorPortalQualitySummary.toDto(): VendorPortalQualitySummaryDto = VendorPortalQualitySummaryDto(
    totalInspections = totalInspections,
    passedInspections = passedInspections,
    failedInspections = failedInspections,
    overallDefectRatePercent = overallDefectRatePercent,
    totalRejections = totalRejections,
    openRejections = openRejections,
    openDisputes = openDisputes,
    qualityRating = qualityRating
)

data class VendorPortalPerformanceSummaryDto(
    val overallScore: Double,
    val qualityScore: Double,
    val deliveryScore: Double,
    val pricingScore: Double,
    val serviceScore: Double,
    val tier: String,
    val evaluationPeriod: String? = null,
    val lastEvaluatedAt: Long? = null
)

fun VendorPortalPerformanceSummary.toDto(): VendorPortalPerformanceSummaryDto = VendorPortalPerformanceSummaryDto(
    overallScore = overallScore,
    qualityScore = qualityScore,
    deliveryScore = deliveryScore,
    pricingScore = pricingScore,
    serviceScore = serviceScore,
    tier = tier,
    evaluationPeriod = evaluationPeriod,
    lastEvaluatedAt = lastEvaluatedAt
)

data class VendorPortalComplianceSummaryDto(
    val complianceRiskLevel: String,
    val activeCertificationsCount: Int,
    val expiringCertificationsCount: Int,
    val expiredCertificationsCount: Int,
    val taxComplianceStatus: String,
    val tradeLicenseStatus: String
)

fun VendorPortalComplianceSummary.toDto(): VendorPortalComplianceSummaryDto = VendorPortalComplianceSummaryDto(
    complianceRiskLevel = complianceRiskLevel,
    activeCertificationsCount = activeCertificationsCount,
    expiringCertificationsCount = expiringCertificationsCount,
    expiredCertificationsCount = expiredCertificationsCount,
    taxComplianceStatus = taxComplianceStatus,
    tradeLicenseStatus = tradeLicenseStatus
)

data class VendorPortalActivitySummaryDto(
    val activityId: String,
    val eventType: String,
    val title: String,
    val description: String,
    val referenceId: String? = null,
    val timestamp: Long,
    val actor: String? = null,
    val category: String = "GENERAL"
)

fun VendorPortalActivitySummary.toDto(): VendorPortalActivitySummaryDto = VendorPortalActivitySummaryDto(
    activityId = activityId,
    eventType = eventType,
    title = title,
    description = description,
    referenceId = referenceId,
    timestamp = timestamp,
    actor = actor,
    category = category
)

data class VendorPortalFeatureVisibilityDto(
    val canViewProfile: Boolean,
    val canViewServices: Boolean,
    val canViewCapabilities: Boolean,
    val canViewRates: Boolean,
    val canViewPurchaseOrders: Boolean,
    val canViewWorkOrders: Boolean,
    val canViewDeliveries: Boolean,
    val canViewInvoices: Boolean,
    val canViewFinancials: Boolean,
    val canViewQuality: Boolean,
    val canViewDisputes: Boolean,
    val canViewSettlements: Boolean,
    val canViewPerformance: Boolean,
    val canViewCompliance: Boolean,
    val canViewAuditTrail: Boolean,
    val canManagePortalUsers: Boolean
)

fun VendorPortalFeatureVisibility.toDto(): VendorPortalFeatureVisibilityDto = VendorPortalFeatureVisibilityDto(
    canViewProfile = canViewProfile,
    canViewServices = canViewServices,
    canViewCapabilities = canViewCapabilities,
    canViewRates = canViewRates,
    canViewPurchaseOrders = canViewPurchaseOrders,
    canViewWorkOrders = canViewWorkOrders,
    canViewDeliveries = canViewDeliveries,
    canViewInvoices = canViewInvoices,
    canViewFinancials = canViewFinancials,
    canViewQuality = canViewQuality,
    canViewDisputes = canViewDisputes,
    canViewSettlements = canViewSettlements,
    canViewPerformance = canViewPerformance,
    canViewCompliance = canViewCompliance,
    canViewAuditTrail = canViewAuditTrail,
    canManagePortalUsers = canManagePortalUsers
)

data class VendorPortalNavigationItemDto(
    val id: String,
    val label: String,
    val route: String,
    val icon: String,
    val badgeCount: Int = 0,
    val isEnabled: Boolean = true,
    val category: String = "MAIN",
    val sortOrder: Int = 0
)

fun VendorPortalNavigationItem.toDto(): VendorPortalNavigationItemDto = VendorPortalNavigationItemDto(
    id = id,
    label = label,
    route = route,
    icon = icon,
    badgeCount = badgeCount,
    isEnabled = isEnabled,
    category = category,
    sortOrder = sortOrder
)

data class VendorPortalDashboardDto(
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val vendorCode: String,
    val vendorName: String,
    val portalRole: String,
    val membershipStatus: String,
    val accountStatus: String,
    val kpis: List<VendorPortalKpiDto>,
    val profile: VendorPortalProfileSummaryDto,
    val operations: VendorPortalOperationalSummaryDto? = null,
    val financials: VendorPortalFinancialSummaryDto? = null,
    val quality: VendorPortalQualitySummaryDto? = null,
    val performance: VendorPortalPerformanceSummaryDto? = null,
    val compliance: VendorPortalComplianceSummaryDto? = null,
    val recentActivities: List<VendorPortalActivitySummaryDto>,
    val featureVisibility: VendorPortalFeatureVisibilityDto,
    val navigationItems: List<VendorPortalNavigationItemDto>,
    val generatedAt: Long
)

fun VendorPortalDashboard.toDto(): VendorPortalDashboardDto = VendorPortalDashboardDto(
    tenantId = tenantId,
    projectId = projectId,
    vendorId = vendorId,
    vendorCode = vendorCode,
    vendorName = vendorName,
    portalRole = portalRole.name,
    membershipStatus = membershipStatus.name,
    accountStatus = accountStatus.name,
    kpis = kpis.map { it.toDto() },
    profile = profile.toDto(),
    operations = operations?.toDto(),
    financials = financials?.toDto(),
    quality = quality?.toDto(),
    performance = performance?.toDto(),
    compliance = compliance?.toDto(),
    recentActivities = recentActivities.map { it.toDto() },
    featureVisibility = featureVisibility.toDto(),
    navigationItems = navigationItems.map { it.toDto() },
    generatedAt = generatedAt
)

data class VendorPortalWorkspaceDto(
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val userId: String,
    val portalRole: String,
    val profile: VendorPortalProfileSummaryDto,
    val featureVisibility: VendorPortalFeatureVisibilityDto,
    val navigationItems: List<VendorPortalNavigationItemDto>
)

fun VendorPortalWorkspace.toDto(): VendorPortalWorkspaceDto = VendorPortalWorkspaceDto(
    tenantId = tenantId,
    projectId = projectId,
    vendorId = vendorId,
    userId = userId,
    portalRole = portalRole.name,
    profile = profile.toDto(),
    featureVisibility = featureVisibility.toDto(),
    navigationItems = navigationItems.map { it.toDto() }
)

// =========================================================================
// 23. VENDOR RFQ / QUOTATION & BID MANAGEMENT DTOS (Step 03)
// =========================================================================

data class CreateVendorRfqItemRequestDto(
    val sequenceNumber: Int,
    val itemCode: String? = null,
    val description: String,
    val requiredCapabilityType: String? = null,
    val quantity: Double,
    val unitOfMeasure: String = "UNIT",
    val targetUnitPrice: Double? = null,
    val targetDeliveryDate: Long? = null,
    val specifications: String? = null,
    val notes: String? = null
)

data class CreateVendorRfqRequestDto(
    val rfqNumber: String? = null,
    val title: String,
    val description: String? = null,
    val responseDeadline: Long,
    val currency: String = "BDT",
    val deliveryRequirements: String? = null,
    val paymentTerms: String? = null,
    val shippingTerms: String? = null,
    val requiredCapabilities: List<String> = emptyList(),
    val items: List<CreateVendorRfqItemRequestDto> = emptyList()
)

data class CancelRfqRequestDto(
    val reason: String
)

data class ExtendDeadlineRequestDto(
    val newDeadline: Long,
    val reason: String
)

data class InviteVendorRequestDto(
    val vendorId: String
)

data class DeclineInvitationRequestDto(
    val reason: String
)

data class CreateVendorQuotationItemRequestDto(
    val rfqItemId: String,
    val quantity: Double,
    val unitPrice: Double,
    val discountAmount: Double = 0.0,
    val taxAmount: Double = 0.0,
    val deliveryLeadTimeDays: Int = 0,
    val notes: String? = null
)

data class CreateVendorQuotationRequestDto(
    val quotationNumber: String? = null,
    val vendorReferenceNumber: String? = null,
    val currency: String = "BDT",
    val validityPeriodDays: Int = 30,
    val paymentTerms: String? = null,
    val deliveryLeadTimeDays: Int = 0,
    val shippingTerms: String? = null,
    val notes: String? = null,
    val items: List<CreateVendorQuotationItemRequestDto> = emptyList()
)

data class WithdrawQuotationRequestDto(
    val reason: String
)

data class RequestRevisionRequestDto(
    val reason: String
)

data class SubmitRevisionRequestDto(
    val reasonForRevision: String,
    val vendorReferenceNumber: String? = null,
    val paymentTerms: String? = null,
    val deliveryLeadTimeDays: Int = 0,
    val shippingTerms: String? = null,
    val notes: String? = null,
    val items: List<CreateVendorQuotationItemRequestDto> = emptyList()
)

data class AskClarificationRequestDto(
    val question: String
)

data class AnswerClarificationRequestDto(
    val answer: String,
    val visibility: String = "PUBLIC_TO_ALL_INVITED"
)

data class EvaluationScoreRequestDto(
    val criterion: String,
    val weightPercent: Double,
    val rawScore: Double,
    val evaluatorNotes: String? = null
)

data class RecordEvaluationRequestDto(
    val quotationId: String,
    val scores: List<EvaluationScoreRequestDto>,
    val decision: String = "UNDER_CONSIDERATION",
    val remarks: String? = null
)

data class AwardRfqRequestDto(
    val winningQuotationId: String,
    val awardReason: String
)

// Response DTOs

data class VendorRfqItemDto(
    val rfqItemId: String,
    val rfqId: String,
    val sequenceNumber: Int,
    val itemCode: String?,
    val description: String,
    val requiredCapabilityType: String?,
    val quantity: Double,
    val unitOfMeasure: String,
    val targetUnitPrice: Double?,
    val targetDeliveryDate: Long?,
    val specifications: String?,
    val notes: String?,
    val version: Long
)

fun VendorRfqItem.toDto(): VendorRfqItemDto = VendorRfqItemDto(
    rfqItemId = rfqItemId,
    rfqId = rfqId,
    sequenceNumber = sequenceNumber,
    itemCode = itemCode,
    description = description,
    requiredCapabilityType = requiredCapabilityType,
    quantity = quantity.toDouble(),
    unitOfMeasure = unitOfMeasure,
    targetUnitPrice = targetUnitPrice?.amount?.toDouble(),
    targetDeliveryDate = targetDeliveryDate,
    specifications = specifications,
    notes = notes,
    version = version
)

data class VendorRfqAwardDecisionDto(
    val awardId: String,
    val rfqId: String,
    val winningVendorId: String,
    val winningQuotationId: String,
    val awardReason: String,
    val awardedAmount: Double,
    val awardedBy: String,
    val awardedAt: Long
)

fun VendorRfqAwardDecision.toDto(): VendorRfqAwardDecisionDto = VendorRfqAwardDecisionDto(
    awardId = awardId,
    rfqId = rfqId,
    winningVendorId = winningVendorId,
    winningQuotationId = winningQuotationId,
    awardReason = awardReason,
    awardedAmount = awardedAmount.amount.toDouble(),
    awardedBy = awardedBy,
    awardedAt = awardedAt
)

data class VendorRfqDto(
    val rfqId: String,
    val tenantId: String,
    val projectId: String,
    val rfqNumber: String,
    val title: String,
    val description: String?,
    val requestedBy: String,
    val issueDate: Long,
    val responseDeadline: Long,
    val currency: String,
    val deliveryRequirements: String?,
    val paymentTerms: String?,
    val shippingTerms: String?,
    val requiredCapabilities: List<String>,
    val status: String,
    val items: List<VendorRfqItemDto>,
    val awardDecision: VendorRfqAwardDecisionDto?,
    val createdAt: Long,
    val createdBy: String,
    val updatedAt: Long,
    val updatedBy: String?,
    val version: Long
)

fun VendorRfq.toDto(): VendorRfqDto = VendorRfqDto(
    rfqId = rfqId,
    tenantId = tenantId,
    projectId = projectId,
    rfqNumber = rfqNumber,
    title = title,
    description = description,
    requestedBy = requestedBy,
    issueDate = issueDate,
    responseDeadline = responseDeadline,
    currency = currency,
    deliveryRequirements = deliveryRequirements,
    paymentTerms = paymentTerms,
    shippingTerms = shippingTerms,
    requiredCapabilities = requiredCapabilities,
    status = status.name,
    items = items.map { it.toDto() },
    awardDecision = awardDecision?.toDto(),
    createdAt = createdAt,
    createdBy = createdBy,
    updatedAt = updatedAt,
    updatedBy = updatedBy,
    version = version
)

data class VendorRfqInvitationDto(
    val invitationId: String,
    val rfqId: String,
    val vendorId: String,
    val projectId: String,
    val tenantId: String,
    val status: String,
    val invitedAt: Long,
    val viewedAt: Long?,
    val acknowledgedAt: Long?,
    val respondedAt: Long?,
    val declineReason: String?,
    val version: Long
)

fun VendorRfqInvitation.toDto(): VendorRfqInvitationDto = VendorRfqInvitationDto(
    invitationId = invitationId,
    rfqId = rfqId,
    vendorId = vendorId,
    projectId = projectId,
    tenantId = tenantId,
    status = status.name,
    invitedAt = invitedAt,
    viewedAt = viewedAt,
    acknowledgedAt = acknowledgedAt,
    respondedAt = respondedAt,
    declineReason = declineReason,
    version = version
)

data class VendorQuotationItemDto(
    val quotationItemId: String,
    val quotationId: String,
    val rfqItemId: String,
    val quantity: Double,
    val unitPrice: Double,
    val discountAmount: Double,
    val taxAmount: Double,
    val lineTotal: Double,
    val deliveryLeadTimeDays: Int,
    val notes: String?,
    val version: Long
)

fun VendorQuotationItem.toDto(): VendorQuotationItemDto = VendorQuotationItemDto(
    quotationItemId = quotationItemId,
    quotationId = quotationId,
    rfqItemId = rfqItemId,
    quantity = quantity.toDouble(),
    unitPrice = unitPrice.amount.toDouble(),
    discountAmount = discountAmount.amount.toDouble(),
    taxAmount = taxAmount.amount.toDouble(),
    lineTotal = lineTotal.amount.toDouble(),
    deliveryLeadTimeDays = deliveryLeadTimeDays,
    notes = notes,
    version = version
)

data class VendorQuotationDto(
    val quotationId: String,
    val rfqId: String,
    val invitationId: String,
    val vendorId: String,
    val projectId: String,
    val tenantId: String,
    val quotationNumber: String,
    val vendorReferenceNumber: String?,
    val revisionNumber: Int,
    val currency: String,
    val validityPeriodDays: Int,
    val paymentTerms: String?,
    val deliveryLeadTimeDays: Int,
    val shippingTerms: String?,
    val notes: String?,
    val subtotal: Double,
    val totalDiscount: Double,
    val totalTax: Double,
    val grandTotal: Double,
    val status: String,
    val items: List<VendorQuotationItemDto>,
    val submittedAt: Long?,
    val submittedBy: String?,
    val createdAt: Long,
    val createdBy: String,
    val updatedAt: Long,
    val updatedBy: String?,
    val version: Long
)

fun VendorQuotation.toDto(): VendorQuotationDto = VendorQuotationDto(
    quotationId = quotationId,
    rfqId = rfqId,
    invitationId = invitationId,
    vendorId = vendorId,
    projectId = projectId,
    tenantId = tenantId,
    quotationNumber = quotationNumber,
    vendorReferenceNumber = vendorReferenceNumber,
    revisionNumber = revisionNumber,
    currency = currency,
    validityPeriodDays = validityPeriodDays,
    paymentTerms = paymentTerms,
    deliveryLeadTimeDays = deliveryLeadTimeDays,
    shippingTerms = shippingTerms,
    notes = notes,
    subtotal = subtotal.amount.toDouble(),
    totalDiscount = totalDiscount.amount.toDouble(),
    totalTax = totalTax.amount.toDouble(),
    grandTotal = grandTotal.amount.toDouble(),
    status = status.name,
    items = items.map { it.toDto() },
    submittedAt = submittedAt,
    submittedBy = submittedBy,
    createdAt = createdAt,
    createdBy = createdBy,
    updatedAt = updatedAt,
    updatedBy = updatedBy,
    version = version
)

data class VendorQuotationRevisionDto(
    val revisionId: String,
    val quotationId: String,
    val rfqId: String,
    val vendorId: String,
    val tenantId: String,
    val projectId: String,
    val revisionNumber: Int,
    val reasonForRevision: String,
    val snapshotSubtotal: Double,
    val snapshotGrandTotal: Double,
    val itemsSnapshotJson: String,
    val revisedBy: String,
    val revisedAt: Long
)

fun VendorQuotationRevision.toDto(): VendorQuotationRevisionDto = VendorQuotationRevisionDto(
    revisionId = revisionId,
    quotationId = quotationId,
    rfqId = rfqId,
    vendorId = vendorId,
    tenantId = tenantId,
    projectId = projectId,
    revisionNumber = revisionNumber,
    reasonForRevision = reasonForRevision,
    snapshotSubtotal = snapshotSubtotal.amount.toDouble(),
    snapshotGrandTotal = snapshotGrandTotal.amount.toDouble(),
    itemsSnapshotJson = itemsSnapshotJson,
    revisedBy = revisedBy,
    revisedAt = revisedAt
)

data class VendorRfqClarificationDto(
    val clarificationId: String,
    val rfqId: String,
    val vendorId: String,
    val tenantId: String,
    val projectId: String,
    val question: String,
    val askedBy: String,
    val askedAt: Long,
    val answer: String?,
    val answeredBy: String?,
    val answeredAt: Long?,
    val status: String,
    val visibility: String,
    val version: Long
)

fun VendorRfqClarification.toDto(): VendorRfqClarificationDto = VendorRfqClarificationDto(
    clarificationId = clarificationId,
    rfqId = rfqId,
    vendorId = vendorId,
    tenantId = tenantId,
    projectId = projectId,
    question = question,
    askedBy = askedBy,
    askedAt = askedAt,
    answer = answer,
    answeredBy = answeredBy,
    answeredAt = answeredAt,
    status = status.name,
    visibility = visibility.name,
    version = version
)

data class VendorRfqEvaluationScoreDto(
    val criterion: String,
    val weightPercent: Double,
    val rawScore: Double,
    val weightedScore: Double,
    val evaluatorNotes: String?
)

fun VendorRfqEvaluationScore.toDto(): VendorRfqEvaluationScoreDto = VendorRfqEvaluationScoreDto(
    criterion = criterion,
    weightPercent = weightPercent,
    rawScore = rawScore,
    weightedScore = weightedScore,
    evaluatorNotes = evaluatorNotes
)

data class VendorRfqEvaluationDto(
    val evaluationId: String,
    val rfqId: String,
    val quotationId: String,
    val vendorId: String,
    val tenantId: String,
    val projectId: String,
    val evaluatorUserId: String,
    val scores: List<VendorRfqEvaluationScoreDto>,
    val totalScore: Double,
    val decision: String,
    val remarks: String?,
    val evaluatedAt: Long,
    val approvedBy: String?,
    val approvedAt: Long?,
    val version: Long
)

fun VendorRfqEvaluation.toDto(): VendorRfqEvaluationDto = VendorRfqEvaluationDto(
    evaluationId = evaluationId,
    rfqId = rfqId,
    quotationId = quotationId,
    vendorId = vendorId,
    tenantId = tenantId,
    projectId = projectId,
    evaluatorUserId = evaluatorUserId,
    scores = scores.map { it.toDto() },
    totalScore = totalScore,
    decision = decision.name,
    remarks = remarks,
    evaluatedAt = evaluatedAt,
    approvedBy = approvedBy,
    approvedAt = approvedAt,
    version = version
)

data class VendorRfqComparisonItemDto(
    val quotationId: String,
    val vendorId: String,
    val vendorCode: String,
    val vendorName: String,
    val grandTotal: Double,
    val deliveryLeadTimeDays: Int,
    val evaluationScore: Double?,
    val decision: String?,
    val submittedAt: Long?
)

fun VendorRfqComparisonItem.toDto(): VendorRfqComparisonItemDto = VendorRfqComparisonItemDto(
    quotationId = quotationId,
    vendorId = vendorId,
    vendorCode = vendorCode,
    vendorName = vendorName,
    grandTotal = grandTotal.amount.toDouble(),
    deliveryLeadTimeDays = deliveryLeadTimeDays,
    evaluationScore = evaluationScore,
    decision = decision?.name,
    submittedAt = submittedAt
)

data class VendorRfqComparisonSnapshotDto(
    val rfqId: String,
    val rfqNumber: String,
    val title: String,
    val totalInvited: Int,
    val totalBidsReceived: Int,
    val lowestBidAmount: Double?,
    val highestBidAmount: Double?,
    val averageBidAmount: Double?,
    val comparisonItems: List<VendorRfqComparisonItemDto>,
    val generatedAt: Long
)

fun VendorRfqComparisonSnapshot.toDto(): VendorRfqComparisonSnapshotDto = VendorRfqComparisonSnapshotDto(
    rfqId = rfqId,
    rfqNumber = rfqNumber,
    title = title,
    totalInvited = totalInvited,
    totalBidsReceived = totalBidsReceived,
    lowestBidAmount = lowestBidAmount?.amount?.toDouble(),
    highestBidAmount = highestBidAmount?.amount?.toDouble(),
    averageBidAmount = averageBidAmount?.amount?.toDouble(),
    comparisonItems = comparisonItems.map { it.toDto() },
    generatedAt = generatedAt
)

data class VendorRfqAuditEventDto(
    val eventId: String,
    val tenantId: String,
    val projectId: String,
    val rfqId: String,
    val vendorId: String?,
    val quotationId: String?,
    val actorUserId: String,
    val eventType: String,
    val action: String,
    val details: String?,
    val ipAddress: String?,
    val timestamp: Long
)

fun VendorRfqAuditEvent.toDto(): VendorRfqAuditEventDto = VendorRfqAuditEventDto(
    eventId = eventId,
    tenantId = tenantId,
    projectId = projectId,
    rfqId = rfqId,
    vendorId = vendorId,
    quotationId = quotationId,
    actorUserId = actorUserId,
    eventType = eventType.name,
    action = action,
    details = details,
    ipAddress = ipAddress,
    timestamp = timestamp
)

// ============================================================================
// SECTION 24: VENDOR PURCHASE ORDER, WORK ORDER & JOB COLLABORATION (MODULE 13 STEP 04)
// ============================================================================

data class AcknowledgePurchaseOrderRequestDto(
    val acknowledgementType: String = "ACKNOWLEDGED",
    val exceptionDetails: String? = null,
    val declineReason: String? = null,
    val promisedDeliveryDate: Long? = null,
    val comment: String? = null
)

data class VendorPoAcknowledgementDto(
    val acknowledgementId: String,
    val purchaseOrderId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val actorId: String,
    val acknowledgementType: String,
    val exceptionDetails: String?,
    val declineReason: String?,
    val promisedDeliveryDate: Long?,
    val comment: String?,
    val acknowledgedAt: Long,
    val version: Long
)

fun VendorPoAcknowledgement.toDto(): VendorPoAcknowledgementDto = VendorPoAcknowledgementDto(
    acknowledgementId = acknowledgementId,
    purchaseOrderId = purchaseOrderId,
    tenantId = tenantId,
    projectId = projectId,
    vendorId = vendorId,
    actorId = actorId,
    acknowledgementType = acknowledgementType.name,
    exceptionDetails = exceptionDetails,
    declineReason = declineReason,
    promisedDeliveryDate = promisedDeliveryDate,
    comment = comment,
    acknowledgedAt = acknowledgedAt,
    version = version
)

data class AcknowledgeWorkOrderRequestDto(
    val acknowledgementType: String = "ACKNOWLEDGED",
    val exceptionDetails: String? = null,
    val declineReason: String? = null,
    val promisedStartDate: Long? = null,
    val promisedCompletionDate: Long? = null,
    val comment: String? = null
)

data class VendorWoAcknowledgementDto(
    val acknowledgementId: String,
    val workOrderId: String,
    val purchaseOrderId: String?,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val actorId: String,
    val acknowledgementType: String,
    val exceptionDetails: String?,
    val declineReason: String?,
    val promisedStartDate: Long?,
    val promisedCompletionDate: Long?,
    val comment: String?,
    val acknowledgedAt: Long,
    val version: Long
)

fun VendorWoAcknowledgement.toDto(): VendorWoAcknowledgementDto = VendorWoAcknowledgementDto(
    acknowledgementId = acknowledgementId,
    workOrderId = workOrderId,
    purchaseOrderId = purchaseOrderId,
    tenantId = tenantId,
    projectId = projectId,
    vendorId = vendorId,
    actorId = actorId,
    acknowledgementType = acknowledgementType.name,
    exceptionDetails = exceptionDetails,
    declineReason = declineReason,
    promisedStartDate = promisedStartDate,
    promisedCompletionDate = promisedCompletionDate,
    comment = comment,
    acknowledgedAt = acknowledgedAt,
    version = version
)

data class SubmitProgressRequestDto(
    val completedQuantity: Double,
    val remainingQuantity: Double,
    val progressPercentage: Double? = null,
    val statusSummary: String,
    val notes: String? = null,
    val expectedCompletionDate: Long? = null,
    val blockerReferenceId: String? = null
)

data class VendorProgressUpdateDto(
    val progressUpdateId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val workOrderId: String,
    val progressPercentage: Double?,
    val completedQuantity: Double,
    val remainingQuantity: Double,
    val authorizedQuantity: Double,
    val statusSummary: String,
    val notes: String?,
    val expectedCompletionDate: Long?,
    val blockerReferenceId: String?,
    val submittedBy: String,
    val submittedAt: Long,
    val version: Long
)

fun VendorProgressUpdate.toDto(): VendorProgressUpdateDto = VendorProgressUpdateDto(
    progressUpdateId = progressUpdateId,
    tenantId = tenantId,
    projectId = projectId,
    vendorId = vendorId,
    workOrderId = workOrderId,
    progressPercentage = progressPercentage,
    completedQuantity = completedQuantity.toDouble(),
    remainingQuantity = remainingQuantity.toDouble(),
    authorizedQuantity = authorizedQuantity.toDouble(),
    statusSummary = statusSummary,
    notes = notes,
    expectedCompletionDate = expectedCompletionDate,
    blockerReferenceId = blockerReferenceId,
    submittedBy = submittedBy,
    submittedAt = submittedAt,
    version = version
)

data class ReportBlockerRequestDto(
    val workOrderId: String,
    val purchaseOrderId: String? = null,
    val category: String = "OTHER",
    val severity: String = "MEDIUM",
    val title: String,
    val description: String
)

data class ResolveBlockerRequestDto(
    val resolutionNotes: String
)

data class VendorBlockerDto(
    val blockerId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val workOrderId: String,
    val purchaseOrderId: String?,
    val category: String,
    val severity: String,
    val status: String,
    val title: String,
    val description: String,
    val resolutionNotes: String?,
    val reportedBy: String,
    val reportedAt: Long,
    val acknowledgedBy: String?,
    val acknowledgedAt: Long?,
    val resolvedBy: String?,
    val resolvedAt: Long?,
    val version: Long
)

fun VendorBlocker.toDto(): VendorBlockerDto = VendorBlockerDto(
    blockerId = blockerId,
    tenantId = tenantId,
    projectId = projectId,
    vendorId = vendorId,
    workOrderId = workOrderId,
    purchaseOrderId = purchaseOrderId,
    category = category.name,
    severity = severity.name,
    status = status.name,
    title = title,
    description = description,
    resolutionNotes = resolutionNotes,
    reportedBy = reportedBy,
    reportedAt = reportedAt,
    acknowledgedBy = acknowledgedBy,
    acknowledgedAt = acknowledgedAt,
    resolvedBy = resolvedBy,
    resolvedAt = resolvedAt,
    version = version
)

data class CreateCollaborationThreadRequestDto(
    val resourceType: String,
    val resourceId: String,
    val title: String
)

data class VendorCollaborationThreadDto(
    val threadId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val resourceType: String,
    val resourceId: String,
    val title: String,
    val createdBy: String,
    val createdAt: Long,
    val isClosed: Boolean,
    val version: Long
)

fun VendorCollaborationThread.toDto(): VendorCollaborationThreadDto = VendorCollaborationThreadDto(
    threadId = threadId,
    tenantId = tenantId,
    projectId = projectId,
    vendorId = vendorId,
    resourceType = resourceType.name,
    resourceId = resourceId,
    title = title,
    createdBy = createdBy,
    createdAt = createdAt,
    isClosed = isClosed,
    version = version
)

data class PostCollaborationMessageRequestDto(
    val message: String,
    val visibility: String = "VENDOR_VISIBLE",
    val attachmentMetadataJson: String? = null
)

data class VendorCollaborationMessageDto(
    val messageId: String,
    val threadId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val authorId: String,
    val authorName: String?,
    val isInternalAuthor: Boolean,
    val message: String,
    val visibility: String,
    val attachmentMetadataJson: String?,
    val createdAt: Long
)

fun VendorCollaborationMessage.toDto(): VendorCollaborationMessageDto = VendorCollaborationMessageDto(
    messageId = messageId,
    threadId = threadId,
    tenantId = tenantId,
    projectId = projectId,
    vendorId = vendorId,
    authorId = authorId,
    authorName = authorName,
    isInternalAuthor = isInternalAuthor,
    message = message,
    visibility = visibility.name,
    attachmentMetadataJson = attachmentMetadataJson,
    createdAt = createdAt
)

data class RegisterCollaborationEvidenceRequestDto(
    val resourceType: String,
    val resourceId: String,
    val fileReference: String,
    val filename: String,
    val mimeType: String,
    val sizeBytes: Long,
    val description: String? = null,
    val visibility: String = "VENDOR_VISIBLE"
)

data class VendorCollaborationEvidenceDto(
    val evidenceId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val resourceType: String,
    val resourceId: String,
    val fileReference: String,
    val filename: String,
    val mimeType: String,
    val sizeBytes: Long,
    val checksum: String?,
    val description: String?,
    val visibility: String,
    val uploadedBy: String,
    val uploadedAt: Long
)

fun VendorCollaborationEvidence.toDto(): VendorCollaborationEvidenceDto = VendorCollaborationEvidenceDto(
    evidenceId = evidenceId,
    tenantId = tenantId,
    projectId = projectId,
    vendorId = vendorId,
    resourceType = resourceType.name,
    resourceId = resourceId,
    fileReference = fileReference,
    filename = filename,
    mimeType = mimeType,
    sizeBytes = sizeBytes,
    checksum = checksum,
    description = description,
    visibility = visibility.name,
    uploadedBy = uploadedBy,
    uploadedAt = uploadedAt
)

data class SubmitCompletionRequestDto(
    val completionNotes: String,
    val finalCompletedQuantity: Double,
    val evidenceReferences: List<String> = emptyList()
)

data class ReviewCompletionRequestDto(
    val approved: Boolean,
    val reviewNotes: String? = null
)

data class VendorCompletionRequestDto(
    val completionRequestId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val workOrderId: String,
    val status: String,
    val completionNotes: String,
    val finalCompletedQuantity: Double,
    val evidenceReferences: List<String>,
    val submittedBy: String,
    val submittedAt: Long,
    val reviewedBy: String?,
    val reviewedAt: Long?,
    val reviewNotes: String?,
    val version: Long
)

fun VendorCompletionRequest.toDto(): VendorCompletionRequestDto = VendorCompletionRequestDto(
    completionRequestId = completionRequestId,
    tenantId = tenantId,
    projectId = projectId,
    vendorId = vendorId,
    workOrderId = workOrderId,
    status = status.name,
    completionNotes = completionNotes,
    finalCompletedQuantity = finalCompletedQuantity.toDouble(),
    evidenceReferences = evidenceReferences,
    submittedBy = submittedBy,
    submittedAt = submittedAt,
    reviewedBy = reviewedBy,
    reviewedAt = reviewedAt,
    reviewNotes = reviewNotes,
    version = version
)

data class VendorPortalPurchaseOrderSummaryDto(
    val purchaseOrderId: String,
    val orderNumber: String,
    val vendorId: String,
    val status: String,
    val orderDate: Long,
    val expectedDeliveryDate: Long?,
    val deliveryLocation: String?,
    val currency: String,
    val totalAmount: Double,
    val acknowledgementStatus: String?,
    val acknowledgedAt: Long?,
    val activeWorkOrdersCount: Int,
    val openBlockersCount: Int
)

fun VendorPortalPurchaseOrderSummary.toDto(): VendorPortalPurchaseOrderSummaryDto = VendorPortalPurchaseOrderSummaryDto(
    purchaseOrderId = purchaseOrderId,
    orderNumber = orderNumber,
    vendorId = vendorId,
    status = status.name,
    orderDate = orderDate,
    expectedDeliveryDate = expectedDeliveryDate,
    deliveryLocation = deliveryLocation,
    currency = currency,
    totalAmount = totalAmount.amount.toDouble(),
    acknowledgementStatus = acknowledgementStatus?.name,
    acknowledgedAt = acknowledgedAt,
    activeWorkOrdersCount = activeWorkOrdersCount,
    openBlockersCount = openBlockersCount
)

data class VendorPortalPurchaseOrderDetailsDto(
    val purchaseOrderId: String,
    val orderNumber: String,
    val vendorId: String,
    val status: String,
    val orderDate: Long,
    val expectedDeliveryDate: Long?,
    val deliveryLocation: String?,
    val currency: String,
    val subtotal: Double,
    val taxAmount: Double,
    val discountAmount: Double,
    val totalAmount: Double,
    val notes: String?,
    val items: List<VendorPurchaseOrderItemDto>,
    val acknowledgement: VendorPoAcknowledgementDto?,
    val relatedWorkOrders: List<VendorPortalWorkOrderSummaryDto>,
    val openBlockers: List<VendorBlockerDto>,
    val evidenceList: List<VendorCollaborationEvidenceDto>
)

fun VendorPortalPurchaseOrderDetails.toDto(): VendorPortalPurchaseOrderDetailsDto = VendorPortalPurchaseOrderDetailsDto(
    purchaseOrderId = purchaseOrderId,
    orderNumber = orderNumber,
    vendorId = vendorId,
    status = status.name,
    orderDate = orderDate,
    expectedDeliveryDate = expectedDeliveryDate,
    deliveryLocation = deliveryLocation,
    currency = currency,
    subtotal = subtotal.amount.toDouble(),
    taxAmount = taxAmount.amount.toDouble(),
    discountAmount = discountAmount.amount.toDouble(),
    totalAmount = totalAmount.amount.toDouble(),
    notes = notes,
    items = items.map { it.toDto() },
    acknowledgement = acknowledgement?.toDto(),
    relatedWorkOrders = relatedWorkOrders.map { it.toDto() },
    openBlockers = openBlockers.map { it.toDto() },
    evidenceList = evidenceList.map { it.toDto() }
)

data class VendorPortalWorkOrderSummaryDto(
    val workOrderId: String,
    val workOrderNumber: String,
    val purchaseOrderId: String?,
    val title: String,
    val capabilityType: String,
    val quantity: Double,
    val unitOfMeasure: String,
    val status: String,
    val priority: String,
    val scheduledStartAt: Long?,
    val scheduledDueAt: Long?,
    val estimatedAmount: Double,
    val currency: String,
    val acknowledgementStatus: String?,
    val latestProgressPercentage: Double?,
    val completionStatus: String?,
    val openBlockersCount: Int
)

fun VendorPortalWorkOrderSummary.toDto(): VendorPortalWorkOrderSummaryDto = VendorPortalWorkOrderSummaryDto(
    workOrderId = workOrderId,
    workOrderNumber = workOrderNumber,
    purchaseOrderId = purchaseOrderId,
    title = title,
    capabilityType = capabilityType.name,
    quantity = quantity.toDouble(),
    unitOfMeasure = unitOfMeasure.name,
    status = status.name,
    priority = priority,
    scheduledStartAt = scheduledStartAt,
    scheduledDueAt = scheduledDueAt,
    estimatedAmount = estimatedAmount.amount.toDouble(),
    currency = currency,
    acknowledgementStatus = acknowledgementStatus?.name,
    latestProgressPercentage = latestProgressPercentage,
    completionStatus = completionStatus?.name,
    openBlockersCount = openBlockersCount
)

data class VendorPortalWorkOrderDetailsDto(
    val workOrderId: String,
    val workOrderNumber: String,
    val purchaseOrderId: String?,
    val title: String,
    val description: String?,
    val capabilityType: String,
    val quantity: Double,
    val unitOfMeasure: String,
    val pricingMethod: String,
    val estimatedAmount: Double,
    val currency: String,
    val status: String,
    val priority: String,
    val scheduledStartAt: Long?,
    val scheduledDueAt: Long?,
    val notes: String?,
    val acknowledgement: VendorWoAcknowledgementDto?,
    val progressUpdates: List<VendorProgressUpdateDto>,
    val blockers: List<VendorBlockerDto>,
    val evidenceList: List<VendorCollaborationEvidenceDto>,
    val completionRequest: VendorCompletionRequestDto?
)

fun VendorPortalWorkOrderDetails.toDto(): VendorPortalWorkOrderDetailsDto = VendorPortalWorkOrderDetailsDto(
    workOrderId = workOrderId,
    workOrderNumber = workOrderNumber,
    purchaseOrderId = purchaseOrderId,
    title = title,
    description = description,
    capabilityType = capabilityType.name,
    quantity = quantity.toDouble(),
    unitOfMeasure = unitOfMeasure.name,
    pricingMethod = pricingMethod.name,
    estimatedAmount = estimatedAmount.amount.toDouble(),
    currency = currency,
    status = status.name,
    priority = priority,
    scheduledStartAt = scheduledStartAt,
    scheduledDueAt = scheduledDueAt,
    notes = notes,
    acknowledgement = acknowledgement?.toDto(),
    progressUpdates = progressUpdates.map { it.toDto() },
    blockers = blockers.map { it.toDto() },
    evidenceList = evidenceList.map { it.toDto() },
    completionRequest = completionRequest?.toDto()
)

// =========================================================================
// 12. VENDOR PORTAL DELIVERY, RECEIVING & QUALITY DTOS (Module 13 Step 05)
// =========================================================================

data class CreateDeliveryNoticeItemRequestDto(
    val purchaseOrderItemId: String,
    val deliveryQuantity: Double,
    val lotNumber: String? = null,
    val packageCount: Int? = null,
    val remarks: String? = null
)

data class CreateDeliveryNoticeRequestDto(
    val purchaseOrderId: String,
    val plannedDeliveryDate: Long,
    val carrierName: String? = null,
    val trackingNumber: String? = null,
    val vehicleNumber: String? = null,
    val driverName: String? = null,
    val driverPhone: String? = null,
    val vendorNotes: String? = null,
    val items: List<CreateDeliveryNoticeItemRequestDto>
)

data class UpdateDeliveryNoticeRequestDto(
    val plannedDeliveryDate: Long,
    val carrierName: String? = null,
    val trackingNumber: String? = null,
    val vehicleNumber: String? = null,
    val driverName: String? = null,
    val driverPhone: String? = null,
    val vendorNotes: String? = null
)

data class CancelDeliveryNoticeRequestDto(
    val reason: String
)

data class VendorPortalDeliveryNoticeItemDto(
    val itemId: String,
    val noticeId: String,
    val purchaseOrderItemId: String,
    val itemName: String,
    val itemCode: String?,
    val orderedQuantity: Double,
    val previouslyDeliveredQuantity: Double,
    val deliveryQuantity: Double,
    val unitOfMeasure: String,
    val lotNumber: String?,
    val packageCount: Int?,
    val remarks: String?
)

fun com.sucharu.sucharupro.domain.model.vendorportal.VendorPortalDeliveryNoticeItem.toDto(): VendorPortalDeliveryNoticeItemDto = VendorPortalDeliveryNoticeItemDto(
    itemId = itemId,
    noticeId = noticeId,
    purchaseOrderItemId = purchaseOrderItemId,
    itemName = itemName,
    itemCode = itemCode,
    orderedQuantity = orderedQuantity.toDouble(),
    previouslyDeliveredQuantity = previouslyDeliveredQuantity.toDouble(),
    deliveryQuantity = deliveryQuantity.toDouble(),
    unitOfMeasure = unitOfMeasure,
    lotNumber = lotNumber,
    packageCount = packageCount,
    remarks = remarks
)

data class VendorPortalDeliveryNoticeDto(
    val noticeId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val purchaseOrderId: String,
    val orderNumber: String,
    val noticeNumber: String,
    val status: String,
    val plannedDeliveryDate: Long,
    val carrierName: String?,
    val trackingNumber: String?,
    val vehicleNumber: String?,
    val driverName: String?,
    val driverPhone: String?,
    val vendorNotes: String?,
    val items: List<VendorPortalDeliveryNoticeItemDto>,
    val createdAt: Long,
    val createdBy: String,
    val updatedAt: Long,
    val updatedBy: String,
    val submittedAt: Long?,
    val submittedBy: String?,
    val cancelledAt: Long?,
    val cancelledBy: String?,
    val cancellationReason: String?,
    val version: Long
)

fun com.sucharu.sucharupro.domain.model.vendorportal.VendorPortalDeliveryNotice.toDto(): VendorPortalDeliveryNoticeDto = VendorPortalDeliveryNoticeDto(
    noticeId = noticeId,
    tenantId = tenantId,
    projectId = projectId,
    vendorId = vendorId,
    purchaseOrderId = purchaseOrderId,
    orderNumber = orderNumber,
    noticeNumber = noticeNumber,
    status = status.name,
    plannedDeliveryDate = plannedDeliveryDate,
    carrierName = carrierName,
    trackingNumber = trackingNumber,
    vehicleNumber = vehicleNumber,
    driverName = driverName,
    driverPhone = driverPhone,
    vendorNotes = vendorNotes,
    items = items.map { it.toDto() },
    createdAt = createdAt,
    createdBy = createdBy,
    updatedAt = updatedAt,
    updatedBy = updatedBy,
    submittedAt = submittedAt,
    submittedBy = submittedBy,
    cancelledAt = cancelledAt,
    cancelledBy = cancelledBy,
    cancellationReason = cancellationReason,
    version = version
)

data class VendorPortalDeliveryAcknowledgementDto(
    val acknowledgementId: String,
    val noticeId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val deliveryReceiptId: String?,
    val acknowledgedBy: String,
    val acknowledgedAt: Long,
    val receivingGate: String?,
    val notes: String?
)

fun com.sucharu.sucharupro.domain.model.vendorportal.VendorPortalDeliveryAcknowledgement.toDto(): VendorPortalDeliveryAcknowledgementDto = VendorPortalDeliveryAcknowledgementDto(
    acknowledgementId = acknowledgementId,
    noticeId = noticeId,
    tenantId = tenantId,
    projectId = projectId,
    vendorId = vendorId,
    deliveryReceiptId = deliveryReceiptId,
    acknowledgedBy = acknowledgedBy,
    acknowledgedAt = acknowledgedAt,
    receivingGate = receivingGate,
    notes = notes
)

data class VendorPortalReceivingItemSummaryDto(
    val purchaseOrderItemId: String,
    val itemName: String,
    val orderedQuantity: Double,
    val notifiedQuantity: Double,
    val receivedQuantity: Double,
    val acceptedQuantity: Double,
    val rejectedQuantity: Double,
    val conditionalQuantity: Double,
    val remainingQuantity: Double,
    val unitOfMeasure: String
)

fun com.sucharu.sucharupro.domain.model.vendorportal.VendorPortalReceivingItemSummary.toDto(): VendorPortalReceivingItemSummaryDto = VendorPortalReceivingItemSummaryDto(
    purchaseOrderItemId = purchaseOrderItemId,
    itemName = itemName,
    orderedQuantity = orderedQuantity.toDouble(),
    notifiedQuantity = notifiedQuantity.toDouble(),
    receivedQuantity = receivedQuantity.toDouble(),
    acceptedQuantity = acceptedQuantity.toDouble(),
    rejectedQuantity = rejectedQuantity.toDouble(),
    conditionalQuantity = conditionalQuantity.toDouble(),
    remainingQuantity = remainingQuantity.toDouble(),
    unitOfMeasure = unitOfMeasure
)

data class VendorPortalReceivingSummaryDto(
    val purchaseOrderId: String,
    val orderNumber: String,
    val vendorId: String,
    val status: String,
    val totalOrderedQuantity: Double,
    val totalNotifiedQuantity: Double,
    val totalReceivedQuantity: Double,
    val totalAcceptedQuantity: Double,
    val totalRejectedQuantity: Double,
    val totalConditionalQuantity: Double,
    val totalRemainingQuantity: Double,
    val receiptCount: Int,
    val latestReceiptDate: Long?,
    val items: List<VendorPortalReceivingItemSummaryDto>
)

fun com.sucharu.sucharupro.domain.model.vendorportal.VendorPortalReceivingSummary.toDto(): VendorPortalReceivingSummaryDto = VendorPortalReceivingSummaryDto(
    purchaseOrderId = purchaseOrderId,
    orderNumber = orderNumber,
    vendorId = vendorId,
    status = status,
    totalOrderedQuantity = totalOrderedQuantity.toDouble(),
    totalNotifiedQuantity = totalNotifiedQuantity.toDouble(),
    totalReceivedQuantity = totalReceivedQuantity.toDouble(),
    totalAcceptedQuantity = totalAcceptedQuantity.toDouble(),
    totalRejectedQuantity = totalRejectedQuantity.toDouble(),
    totalConditionalQuantity = totalConditionalQuantity.toDouble(),
    totalRemainingQuantity = totalRemainingQuantity.toDouble(),
    receiptCount = receiptCount,
    latestReceiptDate = latestReceiptDate,
    items = items.map { it.toDto() }
)

data class VendorPortalDefectSummaryDto(
    val defectId: String,
    val defectCode: String,
    val defectCategory: String,
    val severity: String,
    val affectedQuantity: Double,
    val description: String
)

fun com.sucharu.sucharupro.domain.model.vendorportal.VendorPortalDefectSummary.toDto(): VendorPortalDefectSummaryDto = VendorPortalDefectSummaryDto(
    defectId = defectId,
    defectCode = defectCode,
    defectCategory = defectCategory,
    severity = severity,
    affectedQuantity = affectedQuantity.toDouble(),
    description = description
)

data class VendorPortalQualityItemSummaryDto(
    val inspectionItemId: String,
    val purchaseOrderItemId: String?,
    val itemName: String,
    val inspectedQuantity: Double,
    val acceptedQuantity: Double,
    val rejectedQuantity: Double,
    val conditionalQuantity: Double,
    val defectCount: Int,
    val remarks: String?
)

fun com.sucharu.sucharupro.domain.model.vendorportal.VendorPortalQualityItemSummary.toDto(): VendorPortalQualityItemSummaryDto = VendorPortalQualityItemSummaryDto(
    inspectionItemId = inspectionItemId,
    purchaseOrderItemId = purchaseOrderItemId,
    itemName = itemName,
    inspectedQuantity = inspectedQuantity.toDouble(),
    acceptedQuantity = acceptedQuantity.toDouble(),
    rejectedQuantity = rejectedQuantity.toDouble(),
    conditionalQuantity = conditionalQuantity.toDouble(),
    defectCount = defectCount,
    remarks = remarks
)

data class VendorPortalQualityInspectionSummaryDto(
    val inspectionId: String,
    val inspectionNumber: String,
    val deliveryReceiptId: String,
    val purchaseOrderId: String,
    val vendorId: String,
    val inspectionDate: Long,
    val status: String,
    val overallResult: String,
    val inspectedQuantity: Double,
    val acceptedQuantity: Double,
    val rejectedQuantity: Double,
    val conditionalQuantity: Double,
    val rejectionId: String?,
    val rejectionReason: String?,
    val disposition: String?,
    val replacementRequired: Boolean,
    val creditRequired: Boolean,
    val correctiveActionRequired: Boolean,
    val disputeId: String?,
    val disputeStatus: String?,
    val items: List<VendorPortalQualityItemSummaryDto>,
    val defects: List<VendorPortalDefectSummaryDto>
)

fun com.sucharu.sucharupro.domain.model.vendorportal.VendorPortalQualityInspectionSummary.toDto(): VendorPortalQualityInspectionSummaryDto = VendorPortalQualityInspectionSummaryDto(
    inspectionId = inspectionId,
    inspectionNumber = inspectionNumber,
    deliveryReceiptId = deliveryReceiptId,
    purchaseOrderId = purchaseOrderId,
    vendorId = vendorId,
    inspectionDate = inspectionDate,
    status = status,
    overallResult = overallResult,
    inspectedQuantity = inspectedQuantity.toDouble(),
    acceptedQuantity = acceptedQuantity.toDouble(),
    rejectedQuantity = rejectedQuantity.toDouble(),
    conditionalQuantity = conditionalQuantity.toDouble(),
    rejectionId = rejectionId,
    rejectionReason = rejectionReason,
    disposition = disposition,
    replacementRequired = replacementRequired,
    creditRequired = creditRequired,
    correctiveActionRequired = correctiveActionRequired,
    disputeId = disputeId,
    disputeStatus = disputeStatus,
    items = items.map { it.toDto() },
    defects = defects.map { it.toDto() }
)

data class AcknowledgeQualityInspectionRequestDto(
    val comment: String
)

data class RespondQualityRequestDto(
    val rejectionId: String? = null,
    val responseType: String,
    val comment: String,
    val correctiveActionPlan: String? = null,
    val promisedReplacementDate: Long? = null,
    val evidenceReferences: List<String> = emptyList()
)

data class VendorPortalQualityResponseDto(
    val responseId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val inspectionId: String,
    val rejectionId: String?,
    val responseType: String,
    val comment: String,
    val correctiveActionPlan: String?,
    val promisedReplacementDate: Long?,
    val evidenceReferences: List<String>,
    val respondedBy: String,
    val respondedAt: Long,
    val version: Long
)

fun com.sucharu.sucharupro.domain.model.vendorportal.VendorPortalQualityResponse.toDto(): VendorPortalQualityResponseDto = VendorPortalQualityResponseDto(
    responseId = responseId,
    tenantId = tenantId,
    projectId = projectId,
    vendorId = vendorId,
    inspectionId = inspectionId,
    rejectionId = rejectionId,
    responseType = responseType.name,
    comment = comment,
    correctiveActionPlan = correctiveActionPlan,
    promisedReplacementDate = promisedReplacementDate,
    evidenceReferences = evidenceReferences,
    respondedBy = respondedBy,
    respondedAt = respondedAt,
    version = version
)

data class VendorPortalDeliveryExceptionDto(
    val exceptionId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val sourceType: String,
    val sourceId: String,
    val exceptionType: String,
    val severity: String,
    val status: String,
    val title: String,
    val description: String,
    val requiredVendorAction: String?,
    val dueAt: Long?,
    val resolvedAt: Long?,
    val resolvedBy: String?,
    val resolutionNotes: String?,
    val createdAt: Long,
    val createdBy: String,
    val version: Long
)

fun com.sucharu.sucharupro.domain.model.vendorportal.VendorPortalDeliveryException.toDto(): VendorPortalDeliveryExceptionDto = VendorPortalDeliveryExceptionDto(
    exceptionId = exceptionId,
    tenantId = tenantId,
    projectId = projectId,
    vendorId = vendorId,
    sourceType = sourceType,
    sourceId = sourceId,
    exceptionType = exceptionType.name,
    severity = severity.name,
    status = status.name,
    title = title,
    description = description,
    requiredVendorAction = requiredVendorAction,
    dueAt = dueAt,
    resolvedAt = resolvedAt,
    resolvedBy = resolvedBy,
    resolutionNotes = resolutionNotes,
    createdAt = createdAt,
    createdBy = createdBy,
    version = version
)

data class ResolveDeliveryExceptionRequestDto(
    val resolutionNotes: String
)

data class RegisterDeliveryEvidenceRequestDto(
    val entityType: String,
    val entityId: String,
    val filename: String,
    val fileReference: String,
    val mimeType: String,
    val sizeBytes: Long,
    val description: String? = null
)

data class VendorPortalDeliveryEvidenceDto(
    val evidenceId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val entityType: String,
    val entityId: String,
    val filename: String,
    val fileReference: String,
    val mimeType: String,
    val sizeBytes: Long,
    val description: String?,
    val uploadedBy: String,
    val uploadedAt: Long
)

fun com.sucharu.sucharupro.domain.model.vendorportal.VendorPortalDeliveryEvidence.toDto(): VendorPortalDeliveryEvidenceDto = VendorPortalDeliveryEvidenceDto(
    evidenceId = evidenceId,
    tenantId = tenantId,
    projectId = projectId,
    vendorId = vendorId,
    entityType = entityType,
    entityId = entityId,
    filename = filename,
    fileReference = fileReference,
    mimeType = mimeType,
    sizeBytes = sizeBytes,
    description = description,
    uploadedBy = uploadedBy,
    uploadedAt = uploadedAt
)

data class VendorPortalDeliveryAuditEventDto(
    val eventId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val eventType: String,
    val entityType: String,
    val entityId: String,
    val actorId: String,
    val description: String,
    val previousState: String?,
    val newState: String?,
    val correlationId: String?,
    val createdAt: Long
)

fun com.sucharu.sucharupro.domain.model.vendorportal.VendorPortalDeliveryAuditEvent.toDto(): VendorPortalDeliveryAuditEventDto = VendorPortalDeliveryAuditEventDto(
    eventId = eventId,
    tenantId = tenantId,
    projectId = projectId,
    vendorId = vendorId,
    eventType = eventType.name,
    entityType = entityType,
    entityId = entityId,
    actorId = actorId,
    description = description,
    previousState = previousState,
    newState = newState,
    correlationId = correlationId,
    createdAt = createdAt
)

// =========================================================================
// 19. VENDOR INVOICE, BILLING & PAYMENT WORKSPACE DTOS (Step 06)
// =========================================================================

data class VendorPortalInvoiceSummaryDto(
    val invoiceId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val purchaseOrderId: String,
    val orderNumber: String,
    val invoiceNumber: String,
    val vendorInvoiceNumber: String,
    val invoiceDate: Long,
    val receivedDate: Long,
    val currency: String,
    val subtotal: Double,
    val taxAmount: Double,
    val discountAmount: Double,
    val shippingAmount: Double,
    val otherCharges: Double,
    val totalAmount: Double,
    val approvedAmount: Double,
    val paidAmount: Double,
    val outstandingAmount: Double,
    val status: String,
    val matchStatus: String,
    val paymentStatus: String,
    val exceptionCount: Int,
    val notes: String?,
    val createdAt: Long,
    val updatedAt: Long
)

fun com.sucharu.sucharupro.domain.model.vendorportal.VendorPortalInvoiceSummary.toDto(): VendorPortalInvoiceSummaryDto = VendorPortalInvoiceSummaryDto(
    invoiceId = invoiceId,
    tenantId = tenantId,
    projectId = projectId,
    vendorId = vendorId,
    purchaseOrderId = purchaseOrderId,
    orderNumber = orderNumber,
    invoiceNumber = invoiceNumber,
    vendorInvoiceNumber = vendorInvoiceNumber,
    invoiceDate = invoiceDate,
    receivedDate = receivedDate,
    currency = currency,
    subtotal = subtotal.amount.toDouble(),
    taxAmount = taxAmount.amount.toDouble(),
    discountAmount = discountAmount.amount.toDouble(),
    shippingAmount = shippingAmount.amount.toDouble(),
    otherCharges = otherCharges.amount.toDouble(),
    totalAmount = totalAmount.amount.toDouble(),
    approvedAmount = approvedAmount.amount.toDouble(),
    paidAmount = paidAmount.amount.toDouble(),
    outstandingAmount = outstandingAmount.amount.toDouble(),
    status = status.name,
    matchStatus = matchStatus.name,
    paymentStatus = paymentStatus.name,
    exceptionCount = exceptionCount,
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt
)

data class SubmitVendorInvoiceItemRequestDto(
    val purchaseOrderItemId: String,
    val deliveryReceiptItemId: String? = null,
    val invoicedQuantity: Double,
    val unitPrice: Double? = null,
    val taxAmount: Double? = null,
    val remarks: String? = null
)

data class SubmitVendorInvoiceRequestDto(
    val purchaseOrderId: String,
    val vendorInvoiceNumber: String,
    val invoiceDate: Long = System.currentTimeMillis(),
    val currency: String = "BDT",
    val shippingAmount: Double? = null,
    val otherCharges: Double? = null,
    val notes: String? = null,
    val items: List<SubmitVendorInvoiceItemRequestDto>
)

data class VendorPortalInvoiceSubmissionItemDto(
    val itemId: String,
    val submissionId: String,
    val purchaseOrderItemId: String,
    val deliveryReceiptItemId: String?,
    val itemName: String,
    val itemCode: String?,
    val invoicedQuantity: Double,
    val unitOfMeasure: String,
    val unitPrice: Double,
    val taxAmount: Double,
    val lineTotal: Double,
    val remarks: String?
)

fun com.sucharu.sucharupro.domain.model.vendorportal.VendorPortalInvoiceSubmissionItem.toDto(): VendorPortalInvoiceSubmissionItemDto = VendorPortalInvoiceSubmissionItemDto(
    itemId = itemId,
    submissionId = submissionId,
    purchaseOrderItemId = purchaseOrderItemId,
    deliveryReceiptItemId = deliveryReceiptItemId,
    itemName = itemName,
    itemCode = itemCode,
    invoicedQuantity = invoicedQuantity.toDouble(),
    unitOfMeasure = unitOfMeasure,
    unitPrice = unitPrice.amount.toDouble(),
    taxAmount = taxAmount.amount.toDouble(),
    lineTotal = lineTotal.amount.toDouble(),
    remarks = remarks
)

data class VendorPortalInvoiceSubmissionDto(
    val submissionId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val purchaseOrderId: String,
    val orderNumber: String,
    val vendorInvoiceNumber: String,
    val invoiceDate: Long,
    val currency: String,
    val subtotalAmount: Double,
    val taxAmount: Double,
    val discountAmount: Double,
    val shippingAmount: Double,
    val otherCharges: Double,
    val totalAmount: Double,
    val notes: String?,
    val status: String,
    val canonicalInvoiceId: String?,
    val rejectionReason: String?,
    val items: List<VendorPortalInvoiceSubmissionItemDto>,
    val createdAt: Long,
    val createdBy: String,
    val updatedAt: Long,
    val updatedBy: String,
    val submittedAt: Long?,
    val submittedBy: String?,
    val version: Long
)

fun com.sucharu.sucharupro.domain.model.vendorportal.VendorPortalInvoiceSubmission.toDto(): VendorPortalInvoiceSubmissionDto = VendorPortalInvoiceSubmissionDto(
    submissionId = submissionId,
    tenantId = tenantId,
    projectId = projectId,
    vendorId = vendorId,
    purchaseOrderId = purchaseOrderId,
    orderNumber = orderNumber,
    vendorInvoiceNumber = vendorInvoiceNumber,
    invoiceDate = invoiceDate,
    currency = currency,
    subtotalAmount = subtotalAmount.amount.toDouble(),
    taxAmount = taxAmount.amount.toDouble(),
    discountAmount = discountAmount.amount.toDouble(),
    shippingAmount = shippingAmount.amount.toDouble(),
    otherCharges = otherCharges.amount.toDouble(),
    totalAmount = totalAmount.amount.toDouble(),
    notes = notes,
    status = status.name,
    canonicalInvoiceId = canonicalInvoiceId,
    rejectionReason = rejectionReason,
    items = items.map { it.toDto() },
    createdAt = createdAt,
    createdBy = createdBy,
    updatedAt = updatedAt,
    updatedBy = updatedBy,
    submittedAt = submittedAt,
    submittedBy = submittedBy,
    version = version
)

data class VendorPortalInvoiceMatchLineSummaryDto(
    val matchLineId: String,
    val invoiceItemId: String,
    val purchaseOrderItemId: String,
    val deliveryReceiptItemId: String?,
    val description: String,
    val orderedQuantity: Double,
    val receivedQuantity: Double,
    val acceptedQuantity: Double,
    val invoicedQuantity: Double,
    val orderedUnitPrice: Double,
    val invoicedUnitPrice: Double,
    val quantityVariance: Double,
    val priceVariance: Double,
    val amountVariance: Double,
    val matchStatus: String,
    val exceptionReason: String?
)

fun com.sucharu.sucharupro.domain.model.vendorportal.VendorPortalInvoiceMatchLineSummary.toDto(): VendorPortalInvoiceMatchLineSummaryDto = VendorPortalInvoiceMatchLineSummaryDto(
    matchLineId = matchLineId,
    invoiceItemId = invoiceItemId,
    purchaseOrderItemId = purchaseOrderItemId,
    deliveryReceiptItemId = deliveryReceiptItemId,
    description = description,
    orderedQuantity = orderedQuantity.toDouble(),
    receivedQuantity = receivedQuantity.toDouble(),
    acceptedQuantity = acceptedQuantity.toDouble(),
    invoicedQuantity = invoicedQuantity.toDouble(),
    orderedUnitPrice = orderedUnitPrice.amount.toDouble(),
    invoicedUnitPrice = invoicedUnitPrice.amount.toDouble(),
    quantityVariance = quantityVariance.toDouble(),
    priceVariance = priceVariance.amount.toDouble(),
    amountVariance = amountVariance.amount.toDouble(),
    matchStatus = matchStatus.name,
    exceptionReason = exceptionReason
)

data class VendorPortalInvoiceMatchSummaryDto(
    val matchId: String,
    val invoiceId: String,
    val purchaseOrderId: String,
    val matchStatus: String,
    val matchedAt: Long,
    val subtotalVariance: Double,
    val quantityVariance: Double,
    val priceVariance: Double,
    val taxVariance: Double,
    val totalVariance: Double,
    val currencyMismatch: Boolean,
    val vendorMismatch: Boolean,
    val exceptionCount: Int,
    val lines: List<VendorPortalInvoiceMatchLineSummaryDto>
)

fun com.sucharu.sucharupro.domain.model.vendorportal.VendorPortalInvoiceMatchSummary.toDto(): VendorPortalInvoiceMatchSummaryDto = VendorPortalInvoiceMatchSummaryDto(
    matchId = matchId,
    invoiceId = invoiceId,
    purchaseOrderId = purchaseOrderId,
    matchStatus = matchStatus.name,
    matchedAt = matchedAt,
    subtotalVariance = subtotalVariance.amount.toDouble(),
    quantityVariance = quantityVariance.toDouble(),
    priceVariance = priceVariance.amount.toDouble(),
    taxVariance = taxVariance.amount.toDouble(),
    totalVariance = totalVariance.amount.toDouble(),
    currencyMismatch = currencyMismatch,
    vendorMismatch = vendorMismatch,
    exceptionCount = exceptionCount,
    lines = lines.map { it.toDto() }
)

data class RespondInvoiceRequestDto(
    val exceptionId: String? = null,
    val responseType: String,
    val comment: String,
    val proposedCorrection: String? = null,
    val evidenceReferences: List<String> = emptyList()
)

data class VendorPortalInvoiceResponseDto(
    val responseId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val invoiceId: String,
    val exceptionId: String?,
    val responseType: String,
    val comment: String,
    val proposedCorrection: String?,
    val evidenceReferences: List<String>,
    val respondedBy: String,
    val respondedAt: Long
)

fun com.sucharu.sucharupro.domain.model.vendorportal.VendorPortalInvoiceResponse.toDto(): VendorPortalInvoiceResponseDto = VendorPortalInvoiceResponseDto(
    responseId = responseId,
    tenantId = tenantId,
    projectId = projectId,
    vendorId = vendorId,
    invoiceId = invoiceId,
    exceptionId = exceptionId,
    responseType = responseType.name,
    comment = comment,
    proposedCorrection = proposedCorrection,
    evidenceReferences = evidenceReferences,
    respondedBy = respondedBy,
    respondedAt = respondedAt
)

data class UploadFinancialEvidenceRequestDto(
    val entityType: String,
    val entityId: String,
    val evidenceType: String,
    val filename: String,
    val fileReference: String,
    val mimeType: String = "application/pdf",
    val sizeBytes: Long
)

data class VendorPortalFinancialEvidenceDto(
    val evidenceId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val entityType: String,
    val entityId: String,
    val evidenceType: String,
    val filename: String,
    val fileReference: String,
    val mimeType: String,
    val sizeBytes: Long,
    val uploadedBy: String,
    val uploadedAt: Long
)

fun com.sucharu.sucharupro.domain.model.vendorportal.VendorPortalFinancialEvidence.toDto(): VendorPortalFinancialEvidenceDto = VendorPortalFinancialEvidenceDto(
    evidenceId = evidenceId,
    tenantId = tenantId,
    projectId = projectId,
    vendorId = vendorId,
    entityType = entityType,
    entityId = entityId,
    evidenceType = evidenceType.name,
    filename = filename,
    fileReference = fileReference,
    mimeType = mimeType,
    sizeBytes = sizeBytes,
    uploadedBy = uploadedBy,
    uploadedAt = uploadedAt
)

data class VendorPortalPaymentSummaryDto(
    val settlementId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val settlementNumber: String,
    val settlementDate: Long,
    val currency: String,
    val totalAmount: Double,
    val paymentStatus: String,
    val paymentMethod: String,
    val referenceNumber: String?,
    val relatedInvoiceIds: List<String>,
    val notes: String?,
    val settledAt: Long?
)

fun com.sucharu.sucharupro.domain.model.vendorportal.VendorPortalPaymentSummary.toDto(): VendorPortalPaymentSummaryDto = VendorPortalPaymentSummaryDto(
    settlementId = settlementId,
    tenantId = tenantId,
    projectId = projectId,
    vendorId = vendorId,
    settlementNumber = settlementNumber,
    settlementDate = settlementDate,
    currency = currency,
    totalAmount = totalAmount.amount.toDouble(),
    paymentStatus = paymentStatus.name,
    paymentMethod = paymentMethod,
    referenceNumber = referenceNumber,
    relatedInvoiceIds = relatedInvoiceIds,
    notes = notes,
    settledAt = settledAt
)

data class VendorPortalFinancialKpiSummaryDto(
    val vendorId: String,
    val currency: String,
    val totalInvoiced: Double,
    val totalApproved: Double,
    val totalPaid: Double,
    val totalOutstanding: Double,
    val totalDisputed: Double,
    val totalOnHold: Double,
    val invoiceCount: Int,
    val outstandingInvoiceCount: Int,
    val paidInvoiceCount: Int,
    val lastUpdated: Long
)

fun com.sucharu.sucharupro.domain.model.vendorportal.VendorPortalFinancialKpiSummary.toDto(): VendorPortalFinancialKpiSummaryDto = VendorPortalFinancialKpiSummaryDto(
    vendorId = vendorId,
    currency = currency,
    totalInvoiced = totalInvoiced.amount.toDouble(),
    totalApproved = totalApproved.amount.toDouble(),
    totalPaid = totalPaid.amount.toDouble(),
    totalOutstanding = totalOutstanding.amount.toDouble(),
    totalDisputed = totalDisputed.amount.toDouble(),
    totalOnHold = totalOnHold.amount.toDouble(),
    invoiceCount = invoiceCount,
    outstandingInvoiceCount = outstandingInvoiceCount,
    paidInvoiceCount = paidInvoiceCount,
    lastUpdated = lastUpdated
)

data class VendorPortalFinancialActivityDto(
    val activityId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val entityType: String,
    val entityId: String,
    val eventType: String,
    val title: String,
    val description: String,
    val amount: Double?,
    val actorId: String,
    val timestamp: Long
)

fun com.sucharu.sucharupro.domain.model.vendorportal.VendorPortalFinancialActivity.toDto(): VendorPortalFinancialActivityDto = VendorPortalFinancialActivityDto(
    activityId = activityId,
    tenantId = tenantId,
    projectId = projectId,
    vendorId = vendorId,
    entityType = entityType,
    entityId = entityId,
    eventType = eventType,
    title = title,
    description = description,
    amount = amount?.amount?.toDouble(),
    actorId = actorId,
    timestamp = timestamp
)

// =========================================================================
// 19. VENDOR PORTAL QUALITY, CAPA & DISPUTE WORKSPACE DTOS (Module 13 Step 07)
// =========================================================================

data class VendorPortalQualityCaseDto(
    val caseId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val inspectionId: String?,
    val deliveryReceiptId: String?,
    val purchaseOrderId: String?,
    val rejectionId: String?,
    val caseNumber: String,
    val status: String,
    val title: String,
    val description: String,
    val severity: String,
    val acknowledgedAt: Long?,
    val acknowledgedBy: String?,
    val closedAt: Long?,
    val closedBy: String?,
    val createdAt: Long,
    val createdBy: String,
    val updatedAt: Long,
    val updatedBy: String,
    val version: Long
)

fun com.sucharu.sucharupro.domain.model.vendorportal.VendorPortalQualityCase.toDto(): VendorPortalQualityCaseDto = VendorPortalQualityCaseDto(
    caseId = caseId,
    tenantId = tenantId,
    projectId = projectId,
    vendorId = vendorId,
    inspectionId = inspectionId,
    deliveryReceiptId = deliveryReceiptId,
    purchaseOrderId = purchaseOrderId,
    rejectionId = rejectionId,
    caseNumber = caseNumber,
    status = status.name,
    title = title,
    description = description,
    severity = severity.name,
    acknowledgedAt = acknowledgedAt,
    acknowledgedBy = acknowledgedBy,
    closedAt = closedAt,
    closedBy = closedBy,
    createdAt = createdAt,
    createdBy = createdBy,
    updatedAt = updatedAt,
    updatedBy = updatedBy,
    version = version
)


data class VendorPortalRejectionSummaryDto(
    val rejectionId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val rejectionReference: String,
    val purchaseOrderId: String?,
    val orderNumber: String?,
    val deliveryReceiptId: String?,
    val receiptNumber: String?,
    val inspectionId: String?,
    val rejectionType: String,
    val rejectionReason: String,
    val rejectedQuantity: Double,
    val rejectedValue: Double,
    val status: String,
    val disposition: String,
    val replacementRequired: Boolean,
    val returnRequired: Boolean,
    val creditRequired: Boolean,
    val vendorResponse: String?,
    val vendorResponseAt: Long?,
    val resolutionNotes: String?,
    val resolvedAt: Long?,
    val createdAt: Long
)

fun com.sucharu.sucharupro.domain.model.vendorportal.VendorPortalRejectionSummary.toDto(): VendorPortalRejectionSummaryDto = VendorPortalRejectionSummaryDto(
    rejectionId = rejectionId,
    tenantId = tenantId,
    projectId = projectId,
    vendorId = vendorId,
    rejectionReference = rejectionReference,
    purchaseOrderId = purchaseOrderId,
    orderNumber = orderNumber,
    deliveryReceiptId = deliveryReceiptId,
    receiptNumber = receiptNumber,
    inspectionId = inspectionId,
    rejectionType = rejectionType,
    rejectionReason = rejectionReason,
    rejectedQuantity = rejectedQuantity.toDouble(),
    rejectedValue = rejectedValue.amount.toDouble(),
    status = status.name,
    disposition = disposition.name,
    replacementRequired = replacementRequired,
    returnRequired = returnRequired,
    creditRequired = creditRequired,
    vendorResponse = vendorResponse,
    vendorResponseAt = vendorResponseAt,
    resolutionNotes = resolutionNotes,
    resolvedAt = resolvedAt,
    createdAt = createdAt
)

data class VendorPortalCapaActionDto(
    val actionId: String,
    val capaId: String,
    val tenantId: String,
    val projectId: String,
    val actionNumber: Int,
    val actionType: String,
    val description: String,
    val owner: String,
    val targetDate: Long,
    val status: String,
    val completedAt: Long?,
    val evidenceReferences: List<String>,
    val notes: String?
)

fun com.sucharu.sucharupro.domain.model.vendorportal.VendorPortalCapaAction.toDto(): VendorPortalCapaActionDto = VendorPortalCapaActionDto(
    actionId = actionId,
    capaId = capaId,
    tenantId = tenantId,
    projectId = projectId,
    actionNumber = actionNumber,
    actionType = actionType.name,
    description = description,
    owner = owner,
    targetDate = targetDate,
    status = status.name,
    completedAt = completedAt,
    evidenceReferences = evidenceReferences,
    notes = notes
)

data class VendorPortalCapaPlanDto(
    val capaId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val caseId: String?,
    val inspectionId: String?,
    val rejectionId: String?,
    val capaNumber: String,
    val status: String,
    val priority: String,
    val title: String,
    val rootCause: String,
    val correctiveAction: String,
    val preventiveAction: String,
    val responsiblePerson: String,
    val targetCompletionDate: Long,
    val actualCompletionDate: Long?,
    val affectedQuantity: Double,
    val affectedUnit: String,
    val verificationStatus: String,
    val verifiedBy: String?,
    val verifiedAt: Long?,
    val reviewerComments: String?,
    val actions: List<VendorPortalCapaActionDto>,
    val createdAt: Long,
    val createdBy: String,
    val updatedAt: Long,
    val updatedBy: String,
    val version: Long
)

fun com.sucharu.sucharupro.domain.model.vendorportal.VendorPortalCapaPlan.toDto(): VendorPortalCapaPlanDto = VendorPortalCapaPlanDto(
    capaId = capaId,
    tenantId = tenantId,
    projectId = projectId,
    vendorId = vendorId,
    caseId = caseId,
    inspectionId = inspectionId,
    rejectionId = rejectionId,
    capaNumber = capaNumber,
    status = status.name,
    priority = priority.name,
    title = title,
    rootCause = rootCause,
    correctiveAction = correctiveAction,
    preventiveAction = preventiveAction,
    responsiblePerson = responsiblePerson,
    targetCompletionDate = targetCompletionDate,
    actualCompletionDate = actualCompletionDate,
    affectedQuantity = affectedQuantity.toDouble(),
    affectedUnit = affectedUnit,
    verificationStatus = verificationStatus,
    verifiedBy = verifiedBy,
    verifiedAt = verifiedAt,
    reviewerComments = reviewerComments,
    actions = actions.map { it.toDto() },
    createdAt = createdAt,
    createdBy = createdBy,
    updatedAt = updatedAt,
    updatedBy = updatedBy,
    version = version
)

data class VendorPortalDisputeSummaryDto(
    val disputeId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val disputeReference: String,
    val sourceType: String,
    val sourceId: String,
    val disputeType: String,
    val priority: String,
    val status: String,
    val subject: String,
    val description: String,
    val requestedResolution: String,
    val disputedQuantity: Double,
    val disputedAmount: Double,
    val raisedBy: String,
    val vendorResponse: String?,
    val vendorResponseAt: Long?,
    val resolutionProposal: String?,
    val resolution: String?,
    val resolvedAt: Long?,
    val resolvedBy: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val version: Long
)

fun com.sucharu.sucharupro.domain.model.vendorportal.VendorPortalDisputeSummary.toDto(): VendorPortalDisputeSummaryDto = VendorPortalDisputeSummaryDto(
    disputeId = disputeId,
    tenantId = tenantId,
    projectId = projectId,
    vendorId = vendorId,
    disputeReference = disputeReference,
    sourceType = sourceType,
    sourceId = sourceId,
    disputeType = disputeType.name,
    priority = priority.name,
    status = status.name,
    subject = subject,
    description = description,
    requestedResolution = requestedResolution.name,
    disputedQuantity = disputedQuantity.toDouble(),
    disputedAmount = disputedAmount.amount.toDouble(),
    raisedBy = raisedBy,
    vendorResponse = vendorResponse,
    vendorResponseAt = vendorResponseAt,
    resolutionProposal = resolutionProposal,
    resolution = resolution,
    resolvedAt = resolvedAt,
    resolvedBy = resolvedBy,
    createdAt = createdAt,
    updatedAt = updatedAt,
    version = version
)

data class VendorPortalResolutionResponseDto(
    val responseId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val disputeId: String,
    val proposalAction: String,
    val rationale: String,
    val respondedBy: String,
    val respondedAt: Long
)

fun com.sucharu.sucharupro.domain.model.vendorportal.VendorPortalResolutionResponse.toDto(): VendorPortalResolutionResponseDto = VendorPortalResolutionResponseDto(
    responseId = responseId,
    tenantId = tenantId,
    projectId = projectId,
    vendorId = vendorId,
    disputeId = disputeId,
    proposalAction = proposalAction.name,
    rationale = rationale,
    respondedBy = respondedBy,
    respondedAt = respondedAt
)

data class VendorPortalQualityEvidenceDto(
    val evidenceId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val entityType: String,
    val entityId: String,
    val evidenceType: String,
    val filename: String,
    val fileReference: String,
    val sizeBytes: Long,
    val checksum: String?,
    val description: String?,
    val uploadedBy: String,
    val uploadedAt: Long
)

fun com.sucharu.sucharupro.domain.model.vendorportal.VendorPortalQualityEvidence.toDto(): VendorPortalQualityEvidenceDto = VendorPortalQualityEvidenceDto(
    evidenceId = evidenceId,
    tenantId = tenantId,
    projectId = projectId,
    vendorId = vendorId,
    entityType = entityType,
    entityId = entityId,
    evidenceType = evidenceType.name,
    filename = filename,
    fileReference = fileReference,
    sizeBytes = sizeBytes,
    checksum = checksum,
    description = description,
    uploadedBy = uploadedBy,
    uploadedAt = uploadedAt
)

data class VendorPortalQualityActivityDto(
    val activityId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val entityType: String,
    val entityId: String,
    val action: String,
    val actorId: String,
    val details: String?,
    val timestamp: Long
)

fun com.sucharu.sucharupro.domain.model.vendorportal.VendorPortalQualityActivity.toDto(): VendorPortalQualityActivityDto = VendorPortalQualityActivityDto(
    activityId = activityId,
    tenantId = tenantId,
    projectId = projectId,
    vendorId = vendorId,
    entityType = entityType,
    entityId = entityId,
    action = action,
    actorId = actorId,
    details = details,
    timestamp = timestamp
)

data class VendorPortalQualityKpiSummaryDto(
    val vendorId: String,
    val openQualityCases: Int,
    val pendingVendorResponses: Int,
    val activeCapaCount: Int,
    val overdueCapaCount: Int,
    val openDisputesCount: Int,
    val totalInspectionsCount: Int,
    val totalRejectionsCount: Int,
    val totalRejectedQuantity: Double,
    val totalAcceptedQuantity: Double,
    val qualityPassRate: Double
)

fun com.sucharu.sucharupro.domain.model.vendorportal.VendorPortalQualityKpiSummary.toDto(): VendorPortalQualityKpiSummaryDto = VendorPortalQualityKpiSummaryDto(
    vendorId = vendorId,
    openQualityCases = openQualityCases,
    pendingVendorResponses = pendingVendorResponses,
    activeCapaCount = activeCapaCount,
    overdueCapaCount = overdueCapaCount,
    openDisputesCount = openDisputesCount,
    totalInspectionsCount = totalInspectionsCount,
    totalRejectionsCount = totalRejectionsCount,
    totalRejectedQuantity = totalRejectedQuantity.toDouble(),
    totalAcceptedQuantity = totalAcceptedQuantity.toDouble(),
    qualityPassRate = qualityPassRate.toDouble()
)

data class VendorPortalQualityWorkspaceDto(
    val kpiSummary: VendorPortalQualityKpiSummaryDto,
    val recentCases: List<VendorPortalQualityCaseDto>,
    val recentInspections: List<VendorPortalQualityInspectionSummaryDto>,
    val recentRejections: List<VendorPortalRejectionSummaryDto>,
    val activeCapas: List<VendorPortalCapaPlanDto>,
    val activeDisputes: List<VendorPortalDisputeSummaryDto>
)

fun com.sucharu.sucharupro.domain.model.vendorportal.VendorPortalQualityWorkspace.toDto(): VendorPortalQualityWorkspaceDto = VendorPortalQualityWorkspaceDto(
    kpiSummary = kpiSummary.toDto(),
    recentCases = recentCases.map { it.toDto() },
    recentInspections = recentInspections.map { it.toDto() },
    recentRejections = recentRejections.map { it.toDto() },
    activeCapas = activeCapas.map { it.toDto() },
    activeDisputes = activeDisputes.map { it.toDto() }
)

// Request bodies
data class VendorPortalQualityCaseResponseRequest(
    val comment: String,
    val correctiveActionPlan: String? = null,
    val promisedReplacementDate: Long? = null,
    val evidenceReferences: List<String> = emptyList()
)

data class VendorPortalCapaPlanCreateRequest(
    val caseId: String? = null,
    val inspectionId: String? = null,
    val rejectionId: String? = null,
    val title: String,
    val rootCause: String,
    val correctiveAction: String,
    val preventiveAction: String,
    val responsiblePerson: String,
    val targetCompletionDate: Long,
    val priority: String = "MEDIUM",
    val affectedQuantity: Double = 0.0,
    val affectedUnit: String = "PIECE"
)

data class VendorPortalCapaActionCreateRequest(
    val actionType: String = "CORRECTIVE",
    val description: String,
    val owner: String,
    val targetDate: Long,
    val notes: String? = null
)

data class VendorPortalCapaActionCompleteRequest(
    val evidenceReferences: List<String> = emptyList()
)

data class VendorPortalDisputeCreateRequest(
    val sourceType: String,
    val sourceId: String,
    val disputeType: String = "QUALITY",
    val priority: String = "MEDIUM",
    val subject: String,
    val description: String,
    val requestedResolution: String = "REPLACEMENT",
    val disputedQuantity: Double = 0.0,
    val disputedAmount: Double = 0.0
)

data class VendorPortalDisputeResponseRequest(
    val response: String
)

data class VendorPortalResolutionProposalResponseRequest(
    val proposalAction: String,
    val rationale: String
)

data class VendorPortalQualityEvidenceUploadRequest(
    val entityType: String,
    val entityId: String,
    val evidenceType: String = "DOCUMENT",
    val filename: String,
    val fileReference: String,
    val sizeBytes: Long = 0L,
    val checksum: String? = null,
    val description: String? = null
)

// =========================================================================
// 12. VENDOR PERFORMANCE & COMPLIANCE WORKSPACE DTOS (Step 08)
// =========================================================================

data class VendorPortalPerformanceKpiSummaryDto(
    val kpiId: String,
    val code: String,
    val name: String,
    val description: String,
    val kpiType: String,
    val targetValue: Double,
    val actualValue: Double,
    val normalizedScore: Double,
    val weightedScore: Double,
    val weight: Double,
    val unit: String,
    val direction: String,
    val sampleSize: Int,
    val confidenceState: String
)

fun com.sucharu.sucharupro.domain.model.vendorportal.VendorPortalPerformanceKpiSummary.toDto(): VendorPortalPerformanceKpiSummaryDto = VendorPortalPerformanceKpiSummaryDto(
    kpiId = kpiId,
    code = code,
    name = name,
    description = description,
    kpiType = kpiType.name,
    targetValue = targetValue,
    actualValue = actualValue,
    normalizedScore = normalizedScore,
    weightedScore = weightedScore,
    weight = weight,
    unit = unit,
    direction = direction.name,
    sampleSize = sampleSize,
    confidenceState = confidenceState.name
)

data class VendorPortalPerformanceScorecardSummaryDto(
    val scorecardId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val periodType: String,
    val periodStart: Long,
    val periodEnd: Long,
    val overallScore: Double,
    val rating: String,
    val riskLevel: String,
    val dataCompleteness: Double,
    val sampleSize: Int,
    val status: String,
    val notes: String? = null,
    val items: List<VendorPortalPerformanceKpiSummaryDto> = emptyList(),
    val generatedAt: Long,
    val approvedAt: Long? = null
)

fun com.sucharu.sucharupro.domain.model.vendorportal.VendorPortalPerformanceScorecardSummary.toDto(): VendorPortalPerformanceScorecardSummaryDto = VendorPortalPerformanceScorecardSummaryDto(
    scorecardId = scorecardId,
    tenantId = tenantId,
    projectId = projectId,
    vendorId = vendorId,
    periodType = periodType.name,
    periodStart = periodStart,
    periodEnd = periodEnd,
    overallScore = overallScore,
    rating = rating.name,
    riskLevel = riskLevel.name,
    dataCompleteness = dataCompleteness,
    sampleSize = sampleSize,
    status = status.name,
    notes = notes,
    items = items.map { it.toDto() },
    generatedAt = generatedAt,
    approvedAt = approvedAt
)

data class VendorPortalPerformanceTrendPointDto(
    val periodStart: Long,
    val periodEnd: Long,
    val overallScore: Double,
    val qualityScore: Double,
    val deliveryScore: Double,
    val costScore: Double,
    val complianceScore: Double,
    val disputeCount: Int,
    val rating: String
)

fun com.sucharu.sucharupro.domain.model.vendorportal.VendorPortalPerformanceTrendPoint.toDto(): VendorPortalPerformanceTrendPointDto = VendorPortalPerformanceTrendPointDto(
    periodStart = periodStart,
    periodEnd = periodEnd,
    overallScore = overallScore,
    qualityScore = qualityScore,
    deliveryScore = deliveryScore,
    costScore = costScore,
    complianceScore = complianceScore,
    disputeCount = disputeCount,
    rating = rating.name
)

data class VendorPortalPerformanceOverviewDto(
    val vendorId: String,
    val overallScore: Double,
    val rating: String,
    val riskLevel: String,
    val onTimeDeliveryRate: Double,
    val poFulfillmentRate: Double,
    val defectRate: Double,
    val qualityRating: String,
    val totalScorecards: Int,
    val activeEvaluations: Int,
    val openCorrectiveActions: Int,
    val latestPeriodStart: Long? = null,
    val latestPeriodEnd: Long? = null,
    val topStrengths: List<String> = emptyList(),
    val improvementAreas: List<String> = emptyList()
)

fun com.sucharu.sucharupro.domain.model.vendorportal.VendorPortalPerformanceOverview.toDto(): VendorPortalPerformanceOverviewDto = VendorPortalPerformanceOverviewDto(
    vendorId = vendorId,
    overallScore = overallScore,
    rating = rating.name,
    riskLevel = riskLevel.name,
    onTimeDeliveryRate = onTimeDeliveryRate,
    poFulfillmentRate = poFulfillmentRate,
    defectRate = defectRate,
    qualityRating = qualityRating,
    totalScorecards = totalScorecards,
    activeEvaluations = activeEvaluations,
    openCorrectiveActions = openCorrectiveActions,
    latestPeriodStart = latestPeriodStart,
    latestPeriodEnd = latestPeriodEnd,
    topStrengths = topStrengths,
    improvementAreas = improvementAreas
)

data class VendorPortalEvaluationCriterionSummaryDto(
    val criterionId: String,
    val name: String,
    val category: String,
    val weight: Double,
    val score: Double,
    val comments: String? = null
)

fun com.sucharu.sucharupro.domain.model.vendorportal.VendorPortalEvaluationCriterionSummary.toDto(): VendorPortalEvaluationCriterionSummaryDto = VendorPortalEvaluationCriterionSummaryDto(
    criterionId = criterionId,
    name = name,
    category = category,
    weight = weight,
    score = score,
    comments = comments
)

data class VendorPortalEvaluationSummaryDto(
    val evaluationId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val scorecardId: String? = null,
    val periodType: String,
    val periodStart: Long,
    val periodEnd: Long,
    val status: String,
    val decision: String? = null,
    val evaluationScore: Double,
    val rating: String,
    val evaluatorComments: String? = null,
    val reviewComments: String? = null,
    val criteria: List<VendorPortalEvaluationCriterionSummaryDto> = emptyList(),
    val acknowledgedAt: Long? = null,
    val acknowledgedBy: String? = null,
    val finalizedAt: Long? = null,
    val createdAt: Long
)

fun com.sucharu.sucharupro.domain.model.vendorportal.VendorPortalEvaluationSummary.toDto(): VendorPortalEvaluationSummaryDto = VendorPortalEvaluationSummaryDto(
    evaluationId = evaluationId,
    tenantId = tenantId,
    projectId = projectId,
    vendorId = vendorId,
    scorecardId = scorecardId,
    periodType = periodType.name,
    periodStart = periodStart,
    periodEnd = periodEnd,
    status = status.name,
    decision = decision?.name,
    evaluationScore = evaluationScore,
    rating = rating.name,
    evaluatorComments = evaluatorComments,
    reviewComments = reviewComments,
    criteria = criteria.map { it.toDto() },
    acknowledgedAt = acknowledgedAt,
    acknowledgedBy = acknowledgedBy,
    finalizedAt = finalizedAt,
    createdAt = createdAt
)

data class VendorPortalEvaluationResponseDto(
    val responseId: String,
    val evaluationId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val responseType: String,
    val subject: String,
    val remarks: String,
    val proposedRemediation: String? = null,
    val evidenceReferences: List<String> = emptyList(),
    val status: String,
    val submittedBy: String,
    val submittedAt: Long,
    val reviewerFeedback: String? = null,
    val version: Long = 1
)

fun com.sucharu.sucharupro.domain.model.vendorportal.VendorPortalEvaluationResponse.toDto(): VendorPortalEvaluationResponseDto = VendorPortalEvaluationResponseDto(
    responseId = responseId,
    evaluationId = evaluationId,
    tenantId = tenantId,
    projectId = projectId,
    vendorId = vendorId,
    responseType = responseType.name,
    subject = subject,
    remarks = remarks,
    proposedRemediation = proposedRemediation,
    evidenceReferences = evidenceReferences,
    status = status.name,
    submittedBy = submittedBy,
    submittedAt = submittedAt,
    reviewerFeedback = reviewerFeedback,
    version = version
)

data class VendorPortalComplianceRequirementSummaryDto(
    val requirementId: String,
    val requirementType: String,
    val code: String,
    val name: String,
    val description: String,
    val mandatory: Boolean,
    val riskLevel: String,
    val validityDays: Int?
)

fun com.sucharu.sucharupro.domain.model.vendorportal.VendorPortalComplianceRequirementSummary.toDto(): VendorPortalComplianceRequirementSummaryDto = VendorPortalComplianceRequirementSummaryDto(
    requirementId = requirementId,
    requirementType = requirementType.name,
    code = code,
    name = name,
    description = description,
    mandatory = mandatory,
    riskLevel = riskLevel.name,
    validityDays = validityDays
)

data class VendorPortalComplianceEvidenceDto(
    val evidenceId: String,
    val recordId: String? = null,
    val requirementId: String? = null,
    val actionId: String? = null,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val evidenceType: String,
    val fileName: String,
    val fileUrl: String,
    val checksum: String? = null,
    val fileSizeBytes: Long,
    val mimeType: String? = null,
    val description: String? = null,
    val uploadedBy: String,
    val uploadedAt: Long,
    val version: Long
)

fun com.sucharu.sucharupro.domain.model.vendorportal.VendorPortalComplianceEvidence.toDto(): VendorPortalComplianceEvidenceDto = VendorPortalComplianceEvidenceDto(
    evidenceId = evidenceId,
    recordId = recordId,
    requirementId = requirementId,
    actionId = actionId,
    tenantId = tenantId,
    projectId = projectId,
    vendorId = vendorId,
    evidenceType = evidenceType.name,
    fileName = fileName,
    fileUrl = fileUrl,
    checksum = checksum,
    fileSizeBytes = fileSizeBytes,
    mimeType = mimeType,
    description = description,
    uploadedBy = uploadedBy,
    uploadedAt = uploadedAt,
    version = version
)

data class VendorPortalComplianceRecordSummaryDto(
    val recordId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val requirementId: String,
    val requirementCode: String,
    val requirementName: String,
    val requirementType: String,
    val mandatory: Boolean,
    val effectiveDate: Long,
    val expiryDate: Long?,
    val status: String,
    val riskLevel: String,
    val verificationStatus: String,
    val rejectionReason: String? = null,
    val notes: String? = null,
    val daysUntilExpiry: Long? = null,
    val expiryAlertLevel: String,
    val evidenceCount: Int,
    val evidenceList: List<VendorPortalComplianceEvidenceDto> = emptyList()
)

fun com.sucharu.sucharupro.domain.model.vendorportal.VendorPortalComplianceRecordSummary.toDto(): VendorPortalComplianceRecordSummaryDto = VendorPortalComplianceRecordSummaryDto(
    recordId = recordId,
    tenantId = tenantId,
    projectId = projectId,
    vendorId = vendorId,
    requirementId = requirementId,
    requirementCode = requirementCode,
    requirementName = requirementName,
    requirementType = requirementType.name,
    mandatory = mandatory,
    effectiveDate = effectiveDate,
    expiryDate = expiryDate,
    status = status.name,
    riskLevel = riskLevel.name,
    verificationStatus = verificationStatus.name,
    rejectionReason = rejectionReason,
    notes = notes,
    daysUntilExpiry = daysUntilExpiry,
    expiryAlertLevel = expiryAlertLevel.name,
    evidenceCount = evidenceCount,
    evidenceList = evidenceList.map { it.toDto() }
)

data class VendorPortalCertificationExpiryAlertDto(
    val recordId: String,
    val certificationName: String,
    val requirementCode: String,
    val expiryDate: Long,
    val daysRemaining: Long,
    val alertLevel: String,
    val mandatory: Boolean,
    val status: String
)

fun com.sucharu.sucharupro.domain.model.vendorportal.VendorPortalCertificationExpiryAlert.toDto(): VendorPortalCertificationExpiryAlertDto = VendorPortalCertificationExpiryAlertDto(
    recordId = recordId,
    certificationName = certificationName,
    requirementCode = requirementCode,
    expiryDate = expiryDate,
    daysRemaining = daysRemaining,
    alertLevel = alertLevel.name,
    mandatory = mandatory,
    status = status.name
)

data class VendorPortalComplianceOverviewDto(
    val vendorId: String,
    val overallRiskLevel: String,
    val overallComplianceStatus: String,
    val totalRequirements: Int,
    val compliantCount: Int,
    val pendingCount: Int,
    val nonCompliantCount: Int,
    val expiredCertificationsCount: Int,
    val upcomingExpiringCertificationsCount: Int,
    val openCorrectiveActionsCount: Int,
    val complianceRate: Double
)

fun com.sucharu.sucharupro.domain.model.vendorportal.VendorPortalComplianceOverview.toDto(): VendorPortalComplianceOverviewDto = VendorPortalComplianceOverviewDto(
    vendorId = vendorId,
    overallRiskLevel = overallRiskLevel.name,
    overallComplianceStatus = overallComplianceStatus.name,
    totalRequirements = totalRequirements,
    compliantCount = compliantCount,
    pendingCount = pendingCount,
    nonCompliantCount = nonCompliantCount,
    expiredCertificationsCount = expiredCertificationsCount,
    upcomingExpiringCertificationsCount = upcomingExpiringCertificationsCount,
    openCorrectiveActionsCount = openCorrectiveActionsCount,
    complianceRate = complianceRate
)

data class VendorPortalCorrectiveActionSummaryDto(
    val actionId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val sourceType: String,
    val sourceId: String? = null,
    val issueDescription: String,
    val rootCause: String? = null,
    val actionPlan: String,
    val priority: String,
    val dueDate: Long,
    val status: String,
    val startedAt: Long? = null,
    val completedAt: Long? = null,
    val closedAt: Long? = null,
    val isOverdue: Boolean,
    val latestVendorResponse: String? = null,
    val responsesCount: Int
)

fun com.sucharu.sucharupro.domain.model.vendorportal.VendorPortalCorrectiveActionSummary.toDto(): VendorPortalCorrectiveActionSummaryDto = VendorPortalCorrectiveActionSummaryDto(
    actionId = actionId,
    tenantId = tenantId,
    projectId = projectId,
    vendorId = vendorId,
    sourceType = sourceType,
    sourceId = sourceId,
    issueDescription = issueDescription,
    rootCause = rootCause,
    actionPlan = actionPlan,
    priority = priority.name,
    dueDate = dueDate,
    status = status.name,
    startedAt = startedAt,
    completedAt = completedAt,
    closedAt = closedAt,
    isOverdue = isOverdue,
    latestVendorResponse = latestVendorResponse,
    responsesCount = responsesCount
)

data class VendorPortalCorrectiveActionResponseDto(
    val responseId: String,
    val actionId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val remediationNotes: String,
    val rootCauseExplanation: String? = null,
    val progressPercentage: Double,
    val isCompletionRequest: Boolean,
    val evidenceReferences: List<String> = emptyList(),
    val status: String,
    val submittedBy: String,
    val submittedAt: Long,
    val version: Long
)

fun com.sucharu.sucharupro.domain.model.vendorportal.VendorPortalCorrectiveActionResponse.toDto(): VendorPortalCorrectiveActionResponseDto = VendorPortalCorrectiveActionResponseDto(
    responseId = responseId,
    actionId = actionId,
    tenantId = tenantId,
    projectId = projectId,
    vendorId = vendorId,
    remediationNotes = remediationNotes,
    rootCauseExplanation = rootCauseExplanation,
    progressPercentage = progressPercentage,
    isCompletionRequest = isCompletionRequest,
    evidenceReferences = evidenceReferences,
    status = status.name,
    submittedBy = submittedBy,
    submittedAt = submittedAt,
    version = version
)

data class VendorPortalPerformanceComplianceActivityDto(
    val activityId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val eventType: String,
    val entityType: String,
    val entityId: String,
    val actorId: String,
    val actorRole: String? = null,
    val description: String,
    val occurredAt: Long,
    val metadata: Map<String, String> = emptyMap()
)

fun com.sucharu.sucharupro.domain.model.vendorportal.VendorPortalPerformanceComplianceActivity.toDto(): VendorPortalPerformanceComplianceActivityDto = VendorPortalPerformanceComplianceActivityDto(
    activityId = activityId,
    tenantId = tenantId,
    projectId = projectId,
    vendorId = vendorId,
    eventType = eventType.name,
    entityType = entityType,
    entityId = entityId,
    actorId = actorId,
    actorRole = actorRole,
    description = description,
    occurredAt = occurredAt,
    metadata = metadata
)

data class VendorPortalPerformanceWorkspaceDto(
    val overview: VendorPortalPerformanceOverviewDto,
    val complianceOverview: VendorPortalComplianceOverviewDto,
    val recentScorecards: List<VendorPortalPerformanceScorecardSummaryDto> = emptyList(),
    val pendingEvaluations: List<VendorPortalEvaluationSummaryDto> = emptyList(),
    val urgentExpiries: List<VendorPortalCertificationExpiryAlertDto> = emptyList(),
    val openCorrectiveActions: List<VendorPortalCorrectiveActionSummaryDto> = emptyList()
)

fun com.sucharu.sucharupro.domain.model.vendorportal.VendorPortalPerformanceWorkspace.toDto(): VendorPortalPerformanceWorkspaceDto = VendorPortalPerformanceWorkspaceDto(
    overview = overview.toDto(),
    complianceOverview = complianceOverview.toDto(),
    recentScorecards = recentScorecards.map { it.toDto() },
    pendingEvaluations = pendingEvaluations.map { it.toDto() },
    urgentExpiries = urgentExpiries.map { it.toDto() },
    openCorrectiveActions = openCorrectiveActions.map { it.toDto() }
)

// Request bodies
data class VendorPortalEvaluationResponseRequest(
    val subject: String,
    val remarks: String,
    val proposedRemediation: String? = null,
    val evidenceReferences: List<String> = emptyList()
)

data class VendorPortalComplianceEvidenceUploadRequest(
    val recordId: String? = null,
    val requirementId: String? = null,
    val actionId: String? = null,
    val evidenceType: String = "DOCUMENT",
    val fileName: String,
    val fileUrl: String,
    val checksum: String? = null,
    val fileSizeBytes: Long = 0L,
    val mimeType: String? = null,
    val description: String? = null
)

data class VendorPortalCorrectiveActionResponseRequest(
    val remediationNotes: String,
    val rootCauseExplanation: String? = null,
    val progressPercentage: Double = 0.0,
    val evidenceReferences: List<String> = emptyList()
)

data class VendorPortalCorrectiveActionCompletionRequest(
    val completionNotes: String,
    val evidenceReferences: List<String> = emptyList()
)

// =========================================================================
// 21. VENDOR PORTAL SETTLEMENT, RECONCILIATION & FINANCIAL COLLABORATION DTOS (Module 13 Step 09)
// =========================================================================

data class VendorPortalSettlementSummaryDto(
    val settlementId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val settlementNumber: String,
    val settlementDate: Long,
    val currency: String,
    val grossAmount: Double,
    val deductions: Double,
    val credits: Double,
    val netPayable: Double,
    val status: String,
    val settlementMethod: String,
    val maskedPaymentReference: String? = null,
    val notes: String? = null,
    val approvedAt: Long? = null,
    val settledAt: Long? = null,
    val allocationCount: Int = 0,
    val acknowledgementStatus: String
)

fun com.sucharu.sucharupro.domain.model.vendorportal.VendorPortalSettlementSummary.toDto(): VendorPortalSettlementSummaryDto = VendorPortalSettlementSummaryDto(
    settlementId = settlementId,
    tenantId = tenantId,
    projectId = projectId,
    vendorId = vendorId,
    settlementNumber = settlementNumber,
    settlementDate = settlementDate,
    currency = currency,
    grossAmount = grossAmount.amount.toDouble(),
    deductions = deductions.amount.toDouble(),
    credits = credits.amount.toDouble(),
    netPayable = netPayable.amount.toDouble(),
    status = status.name,
    settlementMethod = settlementMethod.name,
    maskedPaymentReference = maskedPaymentReference,
    notes = notes,
    approvedAt = approvedAt,
    settledAt = settledAt,
    allocationCount = allocationCount,
    acknowledgementStatus = acknowledgementStatus.name
)

data class VendorPortalSettlementAllocationProjectionDto(
    val allocationId: String,
    val settlementId: String,
    val payableId: String,
    val invoiceId: String? = null,
    val invoiceNumber: String? = null,
    val purchaseOrderId: String? = null,
    val orderNumber: String? = null,
    val allocatedAmount: Double,
    val currency: String,
    val allocatedAt: Long
)

fun com.sucharu.sucharupro.domain.model.vendorportal.VendorPortalSettlementAllocationProjection.toDto(): VendorPortalSettlementAllocationProjectionDto = VendorPortalSettlementAllocationProjectionDto(
    allocationId = allocationId,
    settlementId = settlementId,
    payableId = payableId,
    invoiceId = invoiceId,
    invoiceNumber = invoiceNumber,
    purchaseOrderId = purchaseOrderId,
    orderNumber = orderNumber,
    allocatedAmount = allocatedAmount.amount.toDouble(),
    currency = currency,
    allocatedAt = allocatedAt
)

data class VendorPortalSettlementAcknowledgementDto(
    val acknowledgementId: String,
    val settlementId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val acknowledgedBy: String,
    val acknowledgedAt: Long,
    val status: String,
    val idempotencyKey: String,
    val discrepancyFlag: Boolean,
    val discrepancyNotes: String? = null,
    val evidenceReferences: List<String> = emptyList()
)

fun com.sucharu.sucharupro.domain.model.vendorportal.VendorPortalSettlementAcknowledgement.toDto(): VendorPortalSettlementAcknowledgementDto = VendorPortalSettlementAcknowledgementDto(
    acknowledgementId = acknowledgementId,
    settlementId = settlementId,
    tenantId = tenantId,
    projectId = projectId,
    vendorId = vendorId,
    acknowledgedBy = acknowledgedBy,
    acknowledgedAt = acknowledgedAt,
    status = status.name,
    idempotencyKey = idempotencyKey,
    discrepancyFlag = discrepancyFlag,
    discrepancyNotes = discrepancyNotes,
    evidenceReferences = evidenceReferences
)

data class VendorPortalReconciliationCaseDto(
    val caseId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val settlementId: String? = null,
    val invoiceId: String? = null,
    val caseNumber: String,
    val subject: String,
    val status: String,
    val claimedAmount: Double,
    val systemAmount: Double,
    val varianceAmount: Double,
    val currency: String,
    val notes: String? = null,
    val createdBy: String,
    val createdAt: Long,
    val updatedAt: Long,
    val events: List<VendorPortalReconciliationEventDto> = emptyList()
)

data class VendorPortalReconciliationEventDto(
    val eventId: String,
    val caseId: String,
    val actorId: String,
    val actorRole: String,
    val action: String,
    val remarks: String,
    val timestamp: Long
)

fun com.sucharu.sucharupro.domain.model.vendorportal.VendorPortalReconciliationEvent.toDto(): VendorPortalReconciliationEventDto = VendorPortalReconciliationEventDto(
    eventId = eventId,
    caseId = caseId,
    actorId = actorId,
    actorRole = actorRole,
    action = action,
    remarks = remarks,
    timestamp = timestamp
)

fun com.sucharu.sucharupro.domain.model.vendorportal.VendorPortalReconciliationCase.toDto(): VendorPortalReconciliationCaseDto = VendorPortalReconciliationCaseDto(
    caseId = caseId,
    tenantId = tenantId,
    projectId = projectId,
    vendorId = vendorId,
    settlementId = settlementId,
    invoiceId = invoiceId,
    caseNumber = caseNumber,
    subject = subject,
    status = status.name,
    claimedAmount = claimedAmount.amount.toDouble(),
    systemAmount = systemAmount.amount.toDouble(),
    varianceAmount = varianceAmount.amount.toDouble(),
    currency = currency,
    notes = notes,
    createdBy = createdBy,
    createdAt = createdAt,
    updatedAt = updatedAt,
    events = events.map { it.toDto() }
)

data class VendorPortalFinancialDisputeDto(
    val disputeId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val settlementId: String? = null,
    val invoiceId: String? = null,
    val disputeNumber: String,
    val category: String,
    val priority: String,
    val status: String,
    val disputedAmount: Double,
    val proposedResolutionAmount: Double? = null,
    val currency: String,
    val reason: String,
    val resolutionNotes: String? = null,
    val createdBy: String,
    val createdAt: Long,
    val updatedAt: Long,
    val resolvedBy: String? = null,
    val resolvedAt: Long? = null,
    val events: List<VendorPortalFinancialDisputeEventDto> = emptyList()
)

data class VendorPortalFinancialDisputeEventDto(
    val eventId: String,
    val disputeId: String,
    val actorId: String,
    val actorRole: String,
    val action: String,
    val remarks: String,
    val timestamp: Long
)

fun com.sucharu.sucharupro.domain.model.vendorportal.VendorPortalFinancialDisputeEvent.toDto(): VendorPortalFinancialDisputeEventDto = VendorPortalFinancialDisputeEventDto(
    eventId = eventId,
    disputeId = disputeId,
    actorId = actorId,
    actorRole = actorRole,
    action = action,
    remarks = remarks,
    timestamp = timestamp
)

fun com.sucharu.sucharupro.domain.model.vendorportal.VendorPortalFinancialDispute.toDto(): VendorPortalFinancialDisputeDto = VendorPortalFinancialDisputeDto(
    disputeId = disputeId,
    tenantId = tenantId,
    projectId = projectId,
    vendorId = vendorId,
    settlementId = settlementId,
    invoiceId = invoiceId,
    disputeNumber = disputeNumber,
    category = category,
    priority = priority,
    status = status.name,
    disputedAmount = disputedAmount.amount.toDouble(),
    proposedResolutionAmount = proposedResolutionAmount?.amount?.toDouble(),
    currency = currency,
    reason = reason,
    resolutionNotes = resolutionNotes,
    createdBy = createdBy,
    createdAt = createdAt,
    updatedAt = updatedAt,
    resolvedBy = resolvedBy,
    resolvedAt = resolvedAt,
    events = events.map { it.toDto() }
)

data class VendorPortalFinancialSettlementEvidenceDto(
    val evidenceId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val entityType: String,
    val entityId: String,
    val evidenceType: String,
    val fileName: String,
    val fileUrl: String,
    val checksum: String? = null,
    val fileSizeBytes: Long = 0L,
    val mimeType: String? = null,
    val description: String? = null,
    val uploadedBy: String,
    val uploadedAt: Long
)

fun com.sucharu.sucharupro.domain.model.vendorportal.VendorPortalFinancialSettlementEvidence.toDto(): VendorPortalFinancialSettlementEvidenceDto = VendorPortalFinancialSettlementEvidenceDto(
    evidenceId = evidenceId,
    tenantId = tenantId,
    projectId = projectId,
    vendorId = vendorId,
    entityType = entityType,
    entityId = entityId,
    evidenceType = evidenceType.name,
    fileName = fileName,
    fileUrl = fileUrl,
    checksum = checksum,
    fileSizeBytes = fileSizeBytes,
    mimeType = mimeType,
    description = description,
    uploadedBy = uploadedBy,
    uploadedAt = uploadedAt
)

data class VendorPortalFinancialThreadDto(
    val threadId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val contextType: String,
    val contextId: String,
    val subject: String,
    val status: String,
    val createdBy: String,
    val createdAt: Long,
    val updatedAt: Long,
    val messageCount: Int
)

fun com.sucharu.sucharupro.domain.model.vendorportal.VendorPortalFinancialThread.toDto(): VendorPortalFinancialThreadDto = VendorPortalFinancialThreadDto(
    threadId = threadId,
    tenantId = tenantId,
    projectId = projectId,
    vendorId = vendorId,
    contextType = contextType,
    contextId = contextId,
    subject = subject,
    status = status,
    createdBy = createdBy,
    createdAt = createdAt,
    updatedAt = updatedAt,
    messageCount = messageCount
)

data class VendorPortalFinancialMessageDto(
    val messageId: String,
    val threadId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val senderId: String,
    val senderRole: String,
    val content: String,
    val evidenceReferences: List<String> = emptyList(),
    val timestamp: Long
)

fun com.sucharu.sucharupro.domain.model.vendorportal.VendorPortalFinancialMessage.toDto(): VendorPortalFinancialMessageDto = VendorPortalFinancialMessageDto(
    messageId = messageId,
    threadId = threadId,
    tenantId = tenantId,
    projectId = projectId,
    vendorId = vendorId,
    senderId = senderId,
    senderRole = senderRole,
    content = content,
    evidenceReferences = evidenceReferences,
    timestamp = timestamp
)

data class VendorPortalFinancialActivityEventDto(
    val activityId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val eventType: String,
    val entityType: String,
    val entityId: String,
    val actorId: String,
    val actorRole: String,
    val description: String,
    val occurredAt: Long,
    val metadata: Map<String, String> = emptyMap()
)

fun com.sucharu.sucharupro.domain.model.vendorportal.VendorPortalFinancialActivityEvent.toDto(): VendorPortalFinancialActivityEventDto = VendorPortalFinancialActivityEventDto(
    activityId = activityId,
    tenantId = tenantId,
    projectId = projectId,
    vendorId = vendorId,
    eventType = eventType.name,
    entityType = entityType,
    entityId = entityId,
    actorId = actorId,
    actorRole = actorRole,
    description = description,
    occurredAt = occurredAt,
    metadata = metadata
)

data class VendorPortalSettlementAnalyticsSummaryDto(
    val vendorId: String,
    val currency: String,
    val totalSettledAmount: Double,
    val totalOutstandingAmount: Double,
    val totalDisputedAmount: Double,
    val totalReconciledAmount: Double,
    val activeDisputeCount: Int,
    val pendingReconciliationCount: Int,
    val averageSettlementCycleDays: Double,
    val disputeResolutionRate: Double
)

fun com.sucharu.sucharupro.domain.model.vendorportal.VendorPortalSettlementAnalyticsSummary.toDto(): VendorPortalSettlementAnalyticsSummaryDto = VendorPortalSettlementAnalyticsSummaryDto(
    vendorId = vendorId,
    currency = currency,
    totalSettledAmount = totalSettledAmount.amount.toDouble(),
    totalOutstandingAmount = totalOutstandingAmount.amount.toDouble(),
    totalDisputedAmount = totalDisputedAmount.amount.toDouble(),
    totalReconciledAmount = totalReconciledAmount.amount.toDouble(),
    activeDisputeCount = activeDisputeCount,
    pendingReconciliationCount = pendingReconciliationCount,
    averageSettlementCycleDays = averageSettlementCycleDays,
    disputeResolutionRate = disputeResolutionRate
)

data class VendorPortalFinancialWorkspaceDto(
    val settlementOverview: List<VendorPortalSettlementSummaryDto> = emptyList(),
    val outstandingBalance: Double,
    val pendingReconciliations: List<VendorPortalReconciliationCaseDto> = emptyList(),
    val openDisputes: List<VendorPortalFinancialDisputeDto> = emptyList(),
    val recentActivity: List<VendorPortalFinancialActivityEventDto> = emptyList(),
    val analytics: VendorPortalSettlementAnalyticsSummaryDto
)

fun com.sucharu.sucharupro.domain.model.vendorportal.VendorPortalFinancialWorkspace.toDto(): VendorPortalFinancialWorkspaceDto = VendorPortalFinancialWorkspaceDto(
    settlementOverview = settlementOverview.map { it.toDto() },
    outstandingBalance = outstandingBalance.amount.toDouble(),
    pendingReconciliations = pendingReconciliations.map { it.toDto() },
    openDisputes = openDisputes.map { it.toDto() },
    recentActivity = recentActivity.map { it.toDto() },
    analytics = analytics.toDto()
)

// Request Bodies

data class VendorPortalSettlementAcknowledgementRequest(
    val status: String = "ACKNOWLEDGED",
    val idempotencyKey: String,
    val discrepancyFlag: Boolean = false,
    val discrepancyNotes: String? = null,
    val evidenceReferences: List<String> = emptyList()
)

data class VendorPortalReconciliationQueryRequest(
    val settlementId: String? = null,
    val invoiceId: String? = null,
    val subject: String,
    val claimedAmount: Double,
    val systemAmount: Double,
    val currency: String = "BDT",
    val notes: String? = null
)

data class VendorPortalReconciliationResponseRequest(
    val remarks: String
)

data class VendorPortalFinancialDisputeCreateRequest(
    val settlementId: String? = null,
    val invoiceId: String? = null,
    val category: String,
    val priority: String = "NORMAL",
    val disputedAmount: Double,
    val proposedResolutionAmount: Double? = null,
    val currency: String = "BDT",
    val reason: String
)

data class VendorPortalFinancialDisputeResponseRequest(
    val remarks: String,
    val proposedResolutionAmount: Double? = null
)

data class VendorPortalFinancialEvidenceUploadRequest(
    val entityType: String,
    val entityId: String,
    val evidenceType: String = "SETTLEMENT_STATEMENT",
    val fileName: String,
    val fileUrl: String,
    val checksum: String? = null,
    val fileSizeBytes: Long = 0L,
    val mimeType: String? = null,
    val description: String? = null
)

data class VendorPortalFinancialMessageRequest(
    val content: String,
    val evidenceReferences: List<String> = emptyList()
)

// ============================================================================
// SECTION 14: VENDOR PORTAL ANALYTICS, NOTIFICATIONS, SEARCH & WORKSPACE DTOs (Module 13 Step 10)
// ============================================================================

data class VendorPortalTrendMetricDto(
    val metricKey: String,
    val label: String,
    val currentValue: Double,
    val previousValue: Double,
    val delta: Double,
    val percentageDelta: Double,
    val direction: String,
    val unit: String
)

data class VendorPortalOperationalAnalyticsDto(
    val activePurchaseOrders: Int,
    val openWorkOrders: Int,
    val completedWorkOrders: Int,
    val pendingDeliveryNotices: Int,
    val onTimeDeliveryRate: Double,
    val poFulfillmentRate: Double,
    val recentActivityCount: Int
)

data class VendorPortalFinancialAnalyticsDto(
    val submittedInvoicesCount: Int,
    val approvedInvoicesCount: Int,
    val paidInvoicesCount: Int,
    val totalOutstandingAmount: Double,
    val totalDisputedAmount: Double,
    val totalSettledAmount: Double,
    val currency: String,
    val paymentTrend: String
)

data class VendorPortalQualityAnalyticsDto(
    val totalInspections: Int,
    val passedQuantity: Double,
    val rejectedQuantity: Double,
    val defectRate: Double,
    val openRejectionCases: Int,
    val activeDisputes: Int,
    val openCapaCount: Int
)

data class VendorPortalPerformanceAnalyticsDto(
    val overallScore: Double,
    val qualityKpi: Double,
    val onTimeDeliveryKpi: Double,
    val fulfillmentKpi: Double,
    val totalEvaluations: Int,
    val performanceRating: String
)

data class VendorPortalComplianceAnalyticsDto(
    val complianceStatus: String,
    val totalCertifications: Int,
    val expiringCertifications: Int,
    val pendingRequirements: Int,
    val overallRiskLevel: String
)

data class VendorPortalCollaborationAnalyticsDto(
    val openBlockers: Int,
    val unreadMessages: Int,
    val pendingAcknowledgements: Int,
    val openDisputes: Int,
    val unresolvedItems: Int
)

data class VendorPortalUnifiedAnalyticsHubDto(
    val vendorId: String,
    val tenantId: String,
    val projectId: String,
    val period: String,
    val operational: VendorPortalOperationalAnalyticsDto,
    val financial: VendorPortalFinancialAnalyticsDto,
    val quality: VendorPortalQualityAnalyticsDto,
    val performance: VendorPortalPerformanceAnalyticsDto,
    val compliance: VendorPortalComplianceAnalyticsDto,
    val collaboration: VendorPortalCollaborationAnalyticsDto,
    val trends: List<VendorPortalTrendMetricDto>,
    val generatedAt: Long
)

data class VendorPortalNotificationDto(
    val notificationId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val category: String,
    val severity: String,
    val status: String,
    val title: String,
    val message: String,
    val relatedEntityType: String?,
    val relatedEntityId: String?,
    val deepLinkTarget: String?,
    val createdAt: Long,
    val readAt: Long?,
    val metadata: Map<String, String>
)

data class VendorPortalNotificationPreferenceDto(
    val preferenceId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val emailEnabled: Boolean,
    val inAppEnabled: Boolean,
    val pushEnabled: Boolean,
    val importantOnlyMode: Boolean,
    val disabledCategories: List<String>,
    val minSeverity: String,
    val updatedAt: Long
)

data class VendorPortalNotificationUnreadCountDto(
    val totalUnread: Int,
    val unreadByCategory: Map<String, Int>,
    val unreadBySeverity: Map<String, Int>
)

data class VendorPortalSearchResultItemDto(
    val resultType: String,
    val entityId: String,
    val title: String,
    val snippet: String,
    val status: String,
    val contextualMetadata: Map<String, String>,
    val timestamp: Long?,
    val deepLinkTarget: String
)

data class VendorPortalSearchResultDto(
    val query: String,
    val totalMatches: Int,
    val page: Int,
    val pageSize: Int,
    val items: List<VendorPortalSearchResultItemDto>
)

data class VendorPortalCrossModuleActivityItemDto(
    val activityId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val sourceModule: String,
    val eventType: String,
    val entityType: String,
    val entityId: String,
    val title: String,
    val description: String,
    val actorId: String,
    val actorRole: String,
    val timestamp: Long,
    val deepLinkTarget: String?
)

data class VendorPortalActivityTimelineDto(
    val items: List<VendorPortalCrossModuleActivityItemDto>,
    val totalCount: Int,
    val page: Int,
    val pageSize: Int
)

data class VendorPortalWorkspaceNavigationSectionDto(
    val sectionId: String,
    val label: String,
    val route: String,
    val badgeCount: Int,
    val isVisible: Boolean,
    val iconName: String,
    val order: Int
)

data class VendorPortalUnifiedWorkspaceSummaryDto(
    val vendorId: String,
    val vendorName: String,
    val activePoCount: Int,
    val pendingInvoiceCount: Int,
    val openDisputeCount: Int,
    val unreadNotificationCount: Int,
    val overallPerformanceScore: Double,
    val complianceStatus: String,
    val navigationSections: List<VendorPortalWorkspaceNavigationSectionDto>
)

// Request DTOs
data class VendorPortalEmitNotificationRequest(
    val category: String,
    val severity: String = "NORMAL",
    val title: String,
    val message: String,
    val relatedEntityType: String? = null,
    val relatedEntityId: String? = null,
    val deepLinkTarget: String? = null,
    val metadata: Map<String, String> = emptyMap(),
    val idempotencyKey: String? = null
)

data class VendorPortalUpdateNotificationPreferencesRequest(
    val emailEnabled: Boolean = true,
    val inAppEnabled: Boolean = true,
    val pushEnabled: Boolean = false,
    val importantOnlyMode: Boolean = false,
    val disabledCategories: List<String> = emptyList(),
    val minSeverity: String = "LOW"
)

// Mappers
fun VendorPortalTrendMetric.toDto(): VendorPortalTrendMetricDto = VendorPortalTrendMetricDto(
    metricKey = metricKey,
    label = label,
    currentValue = currentValue,
    previousValue = previousValue,
    delta = delta,
    percentageDelta = percentageDelta,
    direction = direction.name,
    unit = unit
)

fun VendorPortalOperationalAnalytics.toDto(): VendorPortalOperationalAnalyticsDto = VendorPortalOperationalAnalyticsDto(
    activePurchaseOrders = activePurchaseOrders,
    openWorkOrders = openWorkOrders,
    completedWorkOrders = completedWorkOrders,
    pendingDeliveryNotices = pendingDeliveryNotices,
    onTimeDeliveryRate = onTimeDeliveryRate,
    poFulfillmentRate = poFulfillmentRate,
    recentActivityCount = recentActivityCount
)

fun VendorPortalFinancialAnalytics.toDto(): VendorPortalFinancialAnalyticsDto = VendorPortalFinancialAnalyticsDto(
    submittedInvoicesCount = submittedInvoicesCount,
    approvedInvoicesCount = approvedInvoicesCount,
    paidInvoicesCount = paidInvoicesCount,
    totalOutstandingAmount = totalOutstandingAmount.amount.toDouble(),
    totalDisputedAmount = totalDisputedAmount.amount.toDouble(),
    totalSettledAmount = totalSettledAmount.amount.toDouble(),
    currency = currency,
    paymentTrend = paymentTrend
)

fun VendorPortalQualityAnalytics.toDto(): VendorPortalQualityAnalyticsDto = VendorPortalQualityAnalyticsDto(
    totalInspections = totalInspections,
    passedQuantity = passedQuantity,
    rejectedQuantity = rejectedQuantity,
    defectRate = defectRate,
    openRejectionCases = openRejectionCases,
    activeDisputes = activeDisputes,
    openCapaCount = openCapaCount
)

fun VendorPortalPerformanceAnalytics.toDto(): VendorPortalPerformanceAnalyticsDto = VendorPortalPerformanceAnalyticsDto(
    overallScore = overallScore,
    qualityKpi = qualityKpi,
    onTimeDeliveryKpi = onTimeDeliveryKpi,
    fulfillmentKpi = fulfillmentKpi,
    totalEvaluations = totalEvaluations,
    performanceRating = performanceRating
)

fun VendorPortalComplianceAnalytics.toDto(): VendorPortalComplianceAnalyticsDto = VendorPortalComplianceAnalyticsDto(
    complianceStatus = complianceStatus,
    totalCertifications = totalCertifications,
    expiringCertifications = expiringCertifications,
    pendingRequirements = pendingRequirements,
    overallRiskLevel = overallRiskLevel
)

fun VendorPortalCollaborationAnalytics.toDto(): VendorPortalCollaborationAnalyticsDto = VendorPortalCollaborationAnalyticsDto(
    openBlockers = openBlockers,
    unreadMessages = unreadMessages,
    pendingAcknowledgements = pendingAcknowledgements,
    openDisputes = openDisputes,
    unresolvedItems = unresolvedItems
)

fun VendorPortalUnifiedAnalyticsHub.toDto(): VendorPortalUnifiedAnalyticsHubDto = VendorPortalUnifiedAnalyticsHubDto(
    vendorId = vendorId,
    tenantId = tenantId,
    projectId = projectId,
    period = period.name,
    operational = operational.toDto(),
    financial = financial.toDto(),
    quality = quality.toDto(),
    performance = performance.toDto(),
    compliance = compliance.toDto(),
    collaboration = collaboration.toDto(),
    trends = trends.map { it.toDto() },
    generatedAt = generatedAt
)

fun VendorPortalNotification.toDto(): VendorPortalNotificationDto = VendorPortalNotificationDto(
    notificationId = notificationId,
    tenantId = tenantId,
    projectId = projectId,
    vendorId = vendorId,
    category = category.name,
    severity = severity.name,
    status = status.name,
    title = title,
    message = message,
    relatedEntityType = relatedEntityType,
    relatedEntityId = relatedEntityId,
    deepLinkTarget = deepLinkTarget,
    createdAt = createdAt,
    readAt = readAt,
    metadata = metadata
)

fun VendorPortalNotificationPreference.toDto(): VendorPortalNotificationPreferenceDto = VendorPortalNotificationPreferenceDto(
    preferenceId = preferenceId,
    tenantId = tenantId,
    projectId = projectId,
    vendorId = vendorId,
    emailEnabled = emailEnabled,
    inAppEnabled = inAppEnabled,
    pushEnabled = pushEnabled,
    importantOnlyMode = importantOnlyMode,
    disabledCategories = disabledCategories.map { it.name },
    minSeverity = minSeverity.name,
    updatedAt = updatedAt
)

fun VendorPortalNotificationUnreadCount.toDto(): VendorPortalNotificationUnreadCountDto = VendorPortalNotificationUnreadCountDto(
    totalUnread = totalUnread,
    unreadByCategory = unreadByCategory.mapKeys { it.key.name },
    unreadBySeverity = unreadBySeverity.mapKeys { it.key.name }
)

fun VendorPortalSearchResultItem.toDto(): VendorPortalSearchResultItemDto = VendorPortalSearchResultItemDto(
    resultType = resultType.name,
    entityId = entityId,
    title = title,
    snippet = snippet,
    status = status,
    contextualMetadata = contextualMetadata,
    timestamp = timestamp,
    deepLinkTarget = deepLinkTarget
)

fun VendorPortalSearchResult.toDto(): VendorPortalSearchResultDto = VendorPortalSearchResultDto(
    query = query,
    totalMatches = totalMatches,
    page = page,
    pageSize = pageSize,
    items = items.map { it.toDto() }
)

fun VendorPortalCrossModuleActivityItem.toDto(): VendorPortalCrossModuleActivityItemDto = VendorPortalCrossModuleActivityItemDto(
    activityId = activityId,
    tenantId = tenantId,
    projectId = projectId,
    vendorId = vendorId,
    sourceModule = sourceModule,
    eventType = eventType,
    entityType = entityType,
    entityId = entityId,
    title = title,
    description = description,
    actorId = actorId,
    actorRole = actorRole,
    timestamp = timestamp,
    deepLinkTarget = deepLinkTarget
)

fun VendorPortalActivityTimeline.toDto(): VendorPortalActivityTimelineDto = VendorPortalActivityTimelineDto(
    items = items.map { it.toDto() },
    totalCount = totalCount,
    page = page,
    pageSize = pageSize
)

fun VendorPortalWorkspaceNavigationSection.toDto(): VendorPortalWorkspaceNavigationSectionDto = VendorPortalWorkspaceNavigationSectionDto(
    sectionId = sectionId,
    label = label,
    route = route,
    badgeCount = badgeCount,
    isVisible = isVisible,
    iconName = iconName,
    order = order
)

fun VendorPortalUnifiedWorkspaceSummary.toDto(): VendorPortalUnifiedWorkspaceSummaryDto = VendorPortalUnifiedWorkspaceSummaryDto(
    vendorId = vendorId,
    vendorName = vendorName,
    activePoCount = activePoCount,
    pendingInvoiceCount = pendingInvoiceCount,
    openDisputeCount = openDisputeCount,
    unreadNotificationCount = unreadNotificationCount,
    overallPerformanceScore = overallPerformanceScore,
    complianceStatus = complianceStatus,
    navigationSections = navigationSections.map { it.toDto() }
)

// =============================================================================
// MODULE 13 STEP 11: WORKFLOW ORCHESTRATION & CROSS-MODULE INTEGRATION DTOs
// =============================================================================

data class VendorWorkflowDto(
    val workflowId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val correlationId: String,
    val workflowTitle: String,
    val currentStage: String,
    val status: String,
    val slaStatus: String,
    val rfqId: String? = null,
    val quotationId: String? = null,
    val purchaseOrderId: String? = null,
    val workOrderId: String? = null,
    val deliveryNoticeId: String? = null,
    val invoiceId: String? = null,
    val qualityCaseId: String? = null,
    val settlementId: String? = null,
    val startedAt: Long,
    val completedAt: Long? = null,
    val targetDeliveryAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val version: Long = 1L,
    val metadata: Map<String, String> = emptyMap()
)

data class VendorWorkflowTimelineEventDto(
    val eventId: String,
    val workflowId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val correlationId: String,
    val causationId: String? = null,
    val stage: String,
    val eventType: String,
    val title: String,
    val description: String? = null,
    val sourceModule: String,
    val actorId: String,
    val actorType: String = "VENDOR",
    val occurredAt: Long,
    val metadata: Map<String, String> = emptyMap()
)

data class VendorWorkflowExceptionDto(
    val exceptionId: String,
    val workflowId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val category: String,
    val severity: String,
    val status: String,
    val title: String,
    val description: String,
    val detectedAt: Long,
    val resolvedAt: Long? = null,
    val resolvedBy: String? = null,
    val resolutionNotes: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val version: Long = 1L
)

data class VendorWorkflowNextActionDto(
    val actionId: String,
    val workflowId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val actionType: String,
    val title: String,
    val description: String,
    val requiredRole: String,
    val priority: String,
    val dueAt: Long? = null,
    val deepLinkTarget: String? = null,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val completedBy: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)

data class VendorWorkflowSlaProjectionDto(
    val workflowId: String,
    val milestoneTitle: String,
    val deadline: Long,
    val slaStatus: String,
    val timeRemainingMs: Long,
    val isBreached: Boolean
)

data class VendorWorkflowHubSummaryDto(
    val vendorId: String,
    val tenantId: String,
    val projectId: String,
    val totalActiveWorkflows: Int,
    val completedWorkflows: Int,
    val blockedWorkflows: Int,
    val overdueWorkflows: Int,
    val averageCycleTimeDays: Double,
    val stageBreakdown: Map<String, Int>,
    val recentWorkflows: List<VendorWorkflowDto>,
    val urgentActions: List<VendorWorkflowNextActionDto>
)

data class VendorWorkflowRecordExceptionRequest(
    val category: String,
    val severity: String = "MEDIUM",
    val title: String,
    val description: String
)

data class VendorWorkflowResolveExceptionRequest(
    val resolutionNotes: String
)

data class VendorWorkflowSyncRequest(
    val correlationId: String
)

// --- Step 11 Mappers ---

fun com.sucharu.sucharupro.domain.model.vendorportal.VendorWorkflowItem.toDto(): VendorWorkflowDto = VendorWorkflowDto(
    workflowId = workflowId,
    tenantId = tenantId,
    projectId = projectId,
    vendorId = vendorId,
    correlationId = correlationId,
    workflowTitle = workflowTitle,
    currentStage = currentStage.name,
    status = status.name,
    slaStatus = slaStatus.name,
    rfqId = rfqId,
    quotationId = quotationId,
    purchaseOrderId = purchaseOrderId,
    workOrderId = workOrderId,
    deliveryNoticeId = deliveryNoticeId,
    invoiceId = invoiceId,
    qualityCaseId = qualityCaseId,
    settlementId = settlementId,
    startedAt = startedAt,
    completedAt = completedAt,
    targetDeliveryAt = targetDeliveryAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
    version = version,
    metadata = metadata
)

fun com.sucharu.sucharupro.domain.model.vendorportal.VendorWorkflowTimelineEvent.toDto(): VendorWorkflowTimelineEventDto = VendorWorkflowTimelineEventDto(
    eventId = eventId,
    workflowId = workflowId,
    tenantId = tenantId,
    projectId = projectId,
    vendorId = vendorId,
    correlationId = correlationId,
    causationId = causationId,
    stage = stage.name,
    eventType = eventType,
    title = title,
    description = description,
    sourceModule = sourceModule,
    actorId = actorId,
    actorType = actorType,
    occurredAt = occurredAt,
    metadata = metadata
)

fun com.sucharu.sucharupro.domain.model.vendorportal.VendorWorkflowException.toDto(): VendorWorkflowExceptionDto = VendorWorkflowExceptionDto(
    exceptionId = exceptionId,
    workflowId = workflowId,
    tenantId = tenantId,
    projectId = projectId,
    vendorId = vendorId,
    category = category,
    severity = severity.name,
    status = status.name,
    title = title,
    description = description,
    detectedAt = detectedAt,
    resolvedAt = resolvedAt,
    resolvedBy = resolvedBy,
    resolutionNotes = resolutionNotes,
    createdAt = createdAt,
    updatedAt = updatedAt,
    version = version
)

fun com.sucharu.sucharupro.domain.model.vendorportal.VendorWorkflowNextAction.toDto(): VendorWorkflowNextActionDto = VendorWorkflowNextActionDto(
    actionId = actionId,
    workflowId = workflowId,
    tenantId = tenantId,
    projectId = projectId,
    vendorId = vendorId,
    actionType = actionType.name,
    title = title,
    description = description,
    requiredRole = requiredRole,
    priority = priority.name,
    dueAt = dueAt,
    deepLinkTarget = deepLinkTarget,
    isCompleted = isCompleted,
    completedAt = completedAt,
    completedBy = completedBy,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun com.sucharu.sucharupro.domain.model.vendorportal.VendorWorkflowSlaProjection.toDto(): VendorWorkflowSlaProjectionDto = VendorWorkflowSlaProjectionDto(
    workflowId = workflowId,
    milestoneTitle = milestoneTitle,
    deadline = deadline,
    slaStatus = slaStatus.name,
    timeRemainingMs = timeRemainingMs,
    isBreached = isBreached
)

fun com.sucharu.sucharupro.domain.model.vendorportal.VendorWorkflowHubSummary.toDto(): VendorWorkflowHubSummaryDto = VendorWorkflowHubSummaryDto(
    vendorId = vendorId,
    tenantId = tenantId,
    projectId = projectId,
    totalActiveWorkflows = totalActiveWorkflows,
    completedWorkflows = completedWorkflows,
    blockedWorkflows = blockedWorkflows,
    overdueWorkflows = overdueWorkflows,
    averageCycleTimeDays = averageCycleTimeDays,
    stageBreakdown = stageBreakdown,
    recentWorkflows = recentWorkflows.map { it.toDto() },
    urgentActions = urgentActions.map { it.toDto() }
)




