package com.sucharu.sucharupro.domain.event.model.events

import com.sucharu.sucharupro.domain.event.model.DomainEvent
import com.sucharu.sucharupro.domain.event.model.DomainEventType
import java.math.BigDecimal

/**
 * Emitted when a new customer registers.
 */
data class CustomerRegisteredEvent(
    val customerId: String,
    val customerCode: String,
    val displayName: String,
    val primaryPhone: String,
    val email: String? = null,
    val referringAffiliateId: String? = null,
    override val aggregateVersion: Long = 1L
) : DomainEvent {
    override val eventType: DomainEventType get() = DomainEventType.CUSTOMER_REGISTERED
    override val aggregateId: String get() = customerId
    override val aggregateType: String get() = "CUSTOMER"

    init {
        require(customerId.isNotBlank()) { "customerId cannot be blank" }
        require(customerCode.isNotBlank()) { "customerCode cannot be blank" }
        require(displayName.isNotBlank()) { "displayName cannot be blank" }
    }
}

/**
 * Emitted when a customer verifies their identity/contact info.
 */
data class CustomerVerifiedEvent(
    val customerId: String,
    val verificationMethod: String,
    val verifiedTimestamp: Long = System.currentTimeMillis(),
    override val aggregateVersion: Long
) : DomainEvent {
    override val eventType: DomainEventType get() = DomainEventType.CUSTOMER_VERIFIED
    override val aggregateId: String get() = customerId
    override val aggregateType: String get() = "CUSTOMER"

    init {
        require(customerId.isNotBlank()) { "customerId cannot be blank" }
        require(verificationMethod.isNotBlank()) { "verificationMethod cannot be blank" }
    }
}

/**
 * Emitted when an affiliate creates or shares a new referral link.
 */
data class AffiliateReferralCreatedEvent(
    val referralId: String,
    val affiliateId: String,
    val referredCustomerId: String,
    val referralCode: String,
    override val aggregateVersion: Long = 1L
) : DomainEvent {
    override val eventType: DomainEventType get() = DomainEventType.AFFILIATE_REFERRAL_CREATED
    override val aggregateId: String get() = referralId
    override val aggregateType: String get() = "AFFILIATE"

    init {
        require(referralId.isNotBlank()) { "referralId cannot be blank" }
        require(affiliateId.isNotBlank()) { "affiliateId cannot be blank" }
        require(referredCustomerId.isNotBlank()) { "referredCustomerId cannot be blank" }
        require(referralCode.isNotBlank()) { "referralCode cannot be blank" }
    }
}

/**
 * Emitted when commission is earned by an affiliate from a converted order.
 */
data class AffiliateCommissionGeneratedEvent(
    val commissionId: String,
    val affiliateId: String,
    val orderId: String,
    val commissionAmount: BigDecimal,
    val currency: String = "BDT",
    val commissionRatePercentage: Double,
    override val aggregateVersion: Long = 1L
) : DomainEvent {
    override val eventType: DomainEventType get() = DomainEventType.AFFILIATE_COMMISSION_GENERATED
    override val aggregateId: String get() = commissionId
    override val aggregateType: String get() = "AFFILIATE"

    init {
        require(commissionId.isNotBlank()) { "commissionId cannot be blank" }
        require(affiliateId.isNotBlank()) { "affiliateId cannot be blank" }
        require(orderId.isNotBlank()) { "orderId cannot be blank" }
        require(commissionAmount > BigDecimal.ZERO) { "commissionAmount must be positive" }
        require(commissionRatePercentage > 0.0) { "commissionRatePercentage must be positive" }
    }
}
