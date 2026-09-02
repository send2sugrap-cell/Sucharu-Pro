package com.sucharu.sucharupro.domain.model.qc

import java.util.UUID

/**
 * Domain entity representing a discrete Pre-Production Quality Control check item (Module 06 Step 02).
 */
data class PreProductionQcItem(
    val itemId: String,
    val qcId: String,
    val category: PreProductionQcCategory,
    val label: String,
    val status: PreProductionItemStatus = PreProductionItemStatus.PENDING,
    val isRequired: Boolean = true,
    val checkedBy: String? = null,
    val checkedByName: String? = null,
    val checkedAt: String? = null,
    val notes: String? = null
) {
    init {
        require(itemId.isNotBlank()) { "Item ID cannot be blank." }
        require(qcId.isNotBlank()) { "QC ID cannot be blank." }
        require(label.isNotBlank()) { "Check item label cannot be blank." }
    }

    /** Indicates whether this item has been evaluated. */
    val isEvaluated: Boolean get() = status != PreProductionItemStatus.PENDING

    companion object {
        /**
         * Creates the 11 canonical Pre-Production QC items for a specific [qcId].
         */
        fun createCanonicalItems(qcId: String): List<PreProductionQcItem> {
            return listOf(
                PreProductionQcItem(
                    itemId = "item-qc-" + UUID.randomUUID().toString(),
                    qcId = qcId,
                    category = PreProductionQcCategory.JOB_SPECIFICATION,
                    label = "জব পরিচয়, টাইপ ও চূড়ান্ত পরিমাণ যাচাই (Job identity & quantity verification)",
                    isRequired = true
                ),
                PreProductionQcItem(
                    itemId = "item-qc-" + UUID.randomUUID().toString(),
                    qcId = qcId,
                    category = PreProductionQcCategory.ARTWORK,
                    label = "সঠিক আর্টওয়ার্ক ফাইল ও ভার্সন যাচাই (Approved artwork file & version)",
                    isRequired = true
                ),
                PreProductionQcItem(
                    itemId = "item-qc-" + UUID.randomUUID().toString(),
                    qcId = qcId,
                    category = PreProductionQcCategory.APPROVED_PROOF,
                    label = "অনুমোদিত প্রুফ কপি ও প্রুফ ভার্সন যাচাই (Approved proof copy & proof version)",
                    isRequired = true
                ),
                PreProductionQcItem(
                    itemId = "item-qc-" + UUID.randomUUID().toString(),
                    qcId = qcId,
                    category = PreProductionQcCategory.FINAL_APPROVAL,
                    label = "চূড়ান্ত অনুমোদন ও লকড স্ট্যাটাস যাচাই (FINAL_LOCKED approval state)",
                    isRequired = true
                ),
                PreProductionQcItem(
                    itemId = "item-qc-" + UUID.randomUUID().toString(),
                    qcId = qcId,
                    category = PreProductionQcCategory.SIZE,
                    label = "প্রিন্ট সাইজ, উইডথ, হাইট ও অরিয়েন্টেশন (Print dimensions & orientation)",
                    isRequired = true
                ),
                PreProductionQcItem(
                    itemId = "item-qc-" + UUID.randomUUID().toString(),
                    qcId = qcId,
                    category = PreProductionQcCategory.CONTENT,
                    label = "কনটেন্ট, টেক্সট ও প্রয়োজনীয় এলিমেন্ট উপস্থিতি (Content & text check)",
                    isRequired = true
                ),
                PreProductionQcItem(
                    itemId = "item-qc-" + UUID.randomUUID().toString(),
                    qcId = qcId,
                    category = PreProductionQcCategory.COLOR,
                    label = "কালার মোড ও স্পট কালার স্পেসিফিকেশন (CMYK/Spot color specification)",
                    isRequired = true
                ),
                PreProductionQcItem(
                    itemId = "item-qc-" + UUID.randomUUID().toString(),
                    qcId = qcId,
                    category = PreProductionQcCategory.BLEED_TRIM_SAFE_AREA,
                    label = "ব্লিড, ট্রিম ও সেফ মার্জিন পরিমাপ (Bleed, trim & safe area margins)",
                    isRequired = true
                ),
                PreProductionQcItem(
                    itemId = "item-qc-" + UUID.randomUUID().toString(),
                    qcId = qcId,
                    category = PreProductionQcCategory.RESOLUTION,
                    label = "রিজোলিউশন ও প্রিন্ট কোয়ালিটি স্পেক (Resolution & output quality)",
                    isRequired = true
                ),
                PreProductionQcItem(
                    itemId = "item-qc-" + UUID.randomUUID().toString(),
                    qcId = qcId,
                    category = PreProductionQcCategory.MATERIAL_SPECIFICATION,
                    label = "কাগজ/মেটেরিয়াল ও জিএসএম স্পেসিফিকেশন (Paper/material & GSM spec)",
                    isRequired = true
                ),
                PreProductionQcItem(
                    itemId = "item-qc-" + UUID.randomUUID().toString(),
                    qcId = qcId,
                    category = PreProductionQcCategory.FINISHING,
                    label = "ফিনিশিং ও পোস্ট-প্রেস স্পেসিফিকেশন (Lamination/Die-cut/Binding spec)",
                    isRequired = true
                )
            )
        }
    }
}
