package com.sucharu.sucharupro.domain.model.returns

/**
 * Domain model representing the physical receiving of an APPROVED return request.
 * This is the foundational entity for Module 11 Step 04 – Return Receiving.
 */
data class ReturnReceivingInfo(
    val receivingEventId: String,
    val returnId: String,
    val projectId: String,
    val receiverId: String,
    val receivedAt: Long = System.currentTimeMillis(),
    val approvedQty: Int,
    val actualQty: Int,
    val acceptedQty: Int,
    val rejectedQty: Int,
    val damagedQty: Int,
    val mismatchFlag: Boolean,
    val condition: String? = null,
    val packaging: String? = null,
    val damageNotes: String? = null,
    val version: Long,
    val idempotencyKey: String
) {

}
