package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturn
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnLine

/**
 * High-level business validation for Delivery Return aggregate (Module 08 Step 07).
 */
object DeliveryReturnValidator {

    fun validateReturn(
        ret: DeliveryReturn,
        lines: List<DeliveryReturnLine>
    ): DomainResult<Unit> {
        if (lines.isEmpty()) {
            return DomainResult.Error(message = "A Delivery Return must contain at least one item line.")
        }

        val mismatchedProject = lines.find { it.projectId != ret.projectId }
        if (mismatchedProject != null) {
            return DomainResult.Error(
                message = "Project ID mismatch on return line '${mismatchedProject.returnLineId}'."
            )
        }

        // Validate unique DO line per return
        val duplicateDoLine = lines.groupBy { it.deliveryOrderLineId }.filter { it.value.size > 1 }
        if (duplicateDoLine.isNotEmpty()) {
            return DomainResult.Error(
                message = "Duplicate item line detected for Delivery Order Line '${duplicateDoLine.keys.first()}'."
            )
        }

        // Validate each line
        for (line in lines) {
            val lineRes = DeliveryReturnLineValidator.validateLine(line)
            if (lineRes is DomainResult.Error) return lineRes
        }

        return DomainResult.Success(Unit)
    }

    fun validateImmutableIdentity(
        original: DeliveryReturn,
        updated: DeliveryReturn
    ): DomainResult<Unit> {
        if (original.returnId != updated.returnId) {
            return DomainResult.Error(message = "Return ID is immutable.")
        }
        if (original.projectId != updated.projectId) {
            return DomainResult.Error(message = "Project ID is immutable.")
        }
        if (original.returnNo != updated.returnNo) {
            return DomainResult.Error(message = "Return Number is immutable.")
        }
        if (original.deliveryOrderId != updated.deliveryOrderId) {
            return DomainResult.Error(message = "Delivery Order reference is immutable.")
        }
        if (original.createdBy != updated.createdBy) {
            return DomainResult.Error(message = "Original author is immutable.")
        }
        return DomainResult.Success(Unit)
    }
}

// Extension to retrieve createdBy fallback
private val DeliveryReturn.createdBy: String
    get() = this.requestedBy
