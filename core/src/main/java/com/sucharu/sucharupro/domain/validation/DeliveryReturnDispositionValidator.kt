package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnDisposition
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnLineCondition

/**
 * Validation rules for return condition and disposition compatibility (Module 08 Step 07).
 */
object DeliveryReturnDispositionValidator {

    fun validateDispositionCompatibility(
        condition: DeliveryReturnLineCondition,
        disposition: DeliveryReturnDisposition
    ): DomainResult<Unit> {
        if (disposition == DeliveryReturnDisposition.PENDING_DECISION) {
            return DomainResult.Success(Unit)
        }

        // DAMAGED or DEFECTIVE items cannot be directly restocked into clean sellable inventory
        if (disposition == DeliveryReturnDisposition.RESTOCK &&
            (condition == DeliveryReturnLineCondition.DAMAGED || condition == DeliveryReturnLineCondition.DEFECTIVE || condition == DeliveryReturnLineCondition.MISSING)
        ) {
            return DomainResult.Error(
                message = "Items in '${condition.defaultLabel}' condition cannot be assigned 'RESTOCK' disposition. Use QUARANTINE, REWORK, or SCRAP instead."
            )
        }

        return DomainResult.Success(Unit)
    }
}
