package com.sucharu.sucharupro.domain.model.order

/**
 * Capture channels for customer inquiries in Sucharu Pro.
 */
enum class InquirySource(val defaultLabel: String) {
    /** Customer visited showroom, office, or printing facility in person. */
    DIRECT_VISIT("Direct Visit / Walk-in"),

    /** Inbound phone call inquiry. */
    PHONE_CALL("Phone Call"),

    /** WhatsApp or mobile messaging chat inquiry. */
    WHATSAPP("WhatsApp"),

    /** Email communication inquiry. */
    EMAIL("Email"),

    /** Website or online customer portal. */
    WEBSITE("Website"),

    /** Existing customer or partner referral. */
    REFERRAL("Referral"),

    /** Other miscellaneous channel. */
    OTHER("Other")
}
