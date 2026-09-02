package com.sucharu.sucharupro.domain.model.design

/**
 * Standard categorized reasons for requesting a proof revision in Sucharu Pro ERP.
 */
enum class RevisionReason(val defaultLabel: String) {
    TEXT_CHANGE("Text & Copy Change"),
    LAYOUT_CHANGE("Layout & Composition"),
    IMAGE_CHANGE("Image & Graphics Replacement"),
    COLOR_CHANGE("Color & Separation Adjustment"),
    SIZE_CHANGE("Dimensions & Scaling"),
    CONTENT_CORRECTION("Content & Typo Correction"),
    SPECIFICATION_CHANGE("Print Specification Change"),
    OTHER("Other Adjustments")
}
