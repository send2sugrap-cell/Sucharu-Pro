package com.sucharu.sucharupro.domain.model.delivery.pod

/**
 * Recipient relationship classification for delivery acceptance (Module 08 Step 08).
 */
enum class DeliveryProofRecipientType(val defaultLabel: String) {
    PRIMARY_CONTACT("Primary Customer Contact"),
    AUTHORIZED_REPRESENTATIVE("Authorized Company Representative"),
    SECURITY_GUARD("Security Guard / Gatekeeper"),
    RECEPTIONIST("Receptionist / Front Desk"),
    NEIGHBOR("Neighbor / Adjacent Office"),
    OTHER("Other Verified Recipient")
}
