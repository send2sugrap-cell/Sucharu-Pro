package com.sucharu.sucharupro.domain.model.product

/**
 * Product type classification for Sucharu Pro.
 *
 * Sucharu Pro handles a full range of printing, design, production, and distribution services.
 * This enum defines the broad category of a product or service, which determines:
 *  - Whether physical inventory (stock quantity) applies
 *  - How the product flows through the system (job vs. stock dispatch)
 *  - Pricing model (per-unit, per-job, fixed, variable)
 *  - Whether a challan is required for delivery
 *
 * Examples:
 *  - FINISHED_PRODUCT: Quran Sharif, Qaida, Ampara, Calendar, Diary, Gift Item
 *  - PRINTING_JOB: Visiting Cards, Brochures, Banners, Packaging Boxes (custom production)
 *  - GIFT_PROMOTIONAL: Corporate Gifts, Branded Items, Promotional Merchandise
 *  - SERVICE: Logo Design, Artwork Preparation, Digital File Supply, Consultancy
 *  - CUSTOM_JOB: Complex multi-part orders that don't fit standard categories
 */
enum class ProductType(
    val defaultLabel: String,
    /**
     * Whether this product type maintains physical stock quantity.
     * FINISHED_PRODUCT and GIFT_PROMOTIONAL typically have stock.
     * SERVICE and CUSTOM_JOB typically do not.
     */
    val hasPhysicalStock: Boolean,
    /**
     * Whether a production job is created for this type.
     * PRINTING_JOB and CUSTOM_JOB always require a production job.
     * FINISHED_PRODUCT may or may not (could be dispatched directly from stock).
     */
    val requiresProductionJob: Boolean
) {
    /**
     * A stocked, ready-to-dispatch physical product.
     * Examples: Quran Sharif, Qaida, Ampara, Tajwid, Calendar, Diary, Information Book.
     * Inventory is tracked by quantity. Dispatched via Challan.
     */
    FINISHED_PRODUCT(
        defaultLabel = "Finished Product",
        hasPhysicalStock = true,
        requiresProductionJob = false
    ),

    /**
     * A custom print job produced to order.
     * Examples: Visiting Cards, Brochures, Banners, Leaflets, Packaging Boxes, Cash Memo.
     * Goes through the 13-stage production workflow.
     * May or may not result in stocked inventory after completion.
     */
    PRINTING_JOB(
        defaultLabel = "Printing Job",
        hasPhysicalStock = false,
        requiresProductionJob = true
    ),

    /**
     * A gift or promotional merchandise item.
     * Examples: Corporate Gifts, Branded Pen Sets, Promotional USB Drives, Trophies.
     * May have stock. Dispatched via Challan.
     */
    GIFT_PROMOTIONAL(
        defaultLabel = "Gift / Promotional",
        hasPhysicalStock = true,
        requiresProductionJob = false
    ),

    /**
     * A non-physical service with no stock.
     * Examples: Logo Design, Artwork Preparation, Digital File Supply, Print Consultancy.
     * No physical stock or challan required.
     */
    SERVICE(
        defaultLabel = "Service",
        hasPhysicalStock = false,
        requiresProductionJob = false
    ),

    /**
     * A complex or composite order that may combine multiple types.
     * Examples: Multi-part packaging system, Hybrid product + printing order.
     */
    CUSTOM_JOB(
        defaultLabel = "Custom Job",
        hasPhysicalStock = false,
        requiresProductionJob = true
    )
}
