package com.sucharu.sucharupro.domain.validation.returns

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.returns.ReturnItem
import com.sucharu.sucharupro.domain.model.returns.ReturnRequest

/**
 * Validates domain rules for Return Requests (Module 11 Step 01).
 *
 * All operations use the canonical DomainResult<Unit> pattern, consistent
 * with the rest of the validation layer (e.g. DeliveryReturnAuthorizationValidator).
 */
object ReturnDomainValidator {

    /**
     * Validates that the return request belongs to the given context project.
     * Prevents cross-project leakage.
     */
    fun validateProjectIsolation(
        request: ReturnRequest,
        contextProjectId: String
    ): DomainResult<Unit> {
        if (request.projectId != contextProjectId) {
            return DomainResult.Error(
                message = "Project isolation violation: Access denied to Return " +
                    "${request.returnId} from project $contextProjectId."
            )
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Validates that a return item belongs to the given return request.
     * Prevents cross-return item leakage.
     */
    fun validateItemBelongsToReturn(
        item: ReturnItem,
        returnId: String
    ): DomainResult<Unit> {
        if (item.returnId != returnId) {
            return DomainResult.Error(
                message = "Return Item '${item.returnItemId}' does not belong to Return '$returnId'."
            )
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Validates required field presence and basic business constraints for a ReturnRequest.
     *
     * Note: structurally invalid fields (blank IDs, invalid timestamps, version)
     * are already guarded by the data-class init block in ReturnRequest.
     * This validator enforces business-level rules above init-level guards.
     */
    fun validateReturnRequest(request: ReturnRequest): DomainResult<Unit> {
        if (request.projectId.isBlank()) {
            return DomainResult.Error(message = "Project ID is required.")
        }
        if (request.customerId.isBlank()) {
            return DomainResult.Error(message = "Customer ID is required.")
        }
        if (request.returnId.isBlank()) {
            return DomainResult.Error(message = "Return ID is required.")
        }
        if (request.returnNo.isBlank()) {
            return DomainResult.Error(message = "Return Number is required.")
        }
        if (request.requestedBy.isBlank()) {
            return DomainResult.Error(message = "Requested-by user ID is required.")
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Validates required fields and quantity invariants for a ReturnItem.
     */
    fun validateReturnItem(item: ReturnItem): DomainResult<Unit> {
        if (item.returnItemId.isBlank()) {
            return DomainResult.Error(message = "Return Item ID is required.")
        }
        if (item.returnId.isBlank()) {
            return DomainResult.Error(message = "Return ID reference is required on a ReturnItem.")
        }
        if (item.productId.isBlank()) {
            return DomainResult.Error(message = "Product ID is required on a ReturnItem.")
        }
        if (item.requestedQuantity <= 0) {
            return DomainResult.Error(
                message = "Requested quantity must be strictly positive (got ${item.requestedQuantity})."
            )
        }
        if (item.acceptedQuantity < 0) {
            return DomainResult.Error(
                message = "Accepted quantity cannot be negative (got ${item.acceptedQuantity})."
            )
        }
        if (item.rejectedQuantity < 0) {
            return DomainResult.Error(
                message = "Rejected quantity cannot be negative (got ${item.rejectedQuantity})."
            )
        }
        if (item.acceptedQuantity + item.rejectedQuantity > item.requestedQuantity) {
            return DomainResult.Error(
                message = "Sum of accepted (${item.acceptedQuantity}) + rejected (${item.rejectedQuantity}) " +
                    "cannot exceed requested quantity (${item.requestedQuantity})."
            )
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Validates that the return request belongs to the authenticated customer actor.
     * Prevents cross-customer data leakage when accessed by a customer account.
     */
    fun validateCustomerOwnership(
        request: ReturnRequest,
        callerCustomerId: String
    ): DomainResult<Unit> {
        if (request.customerId != callerCustomerId) {
            return DomainResult.Error(
                message = "Customer ownership violation: Access denied. Return '${request.returnId}' " +
                    "belongs to customer '${request.customerId}', but caller is customer '$callerCustomerId'."
            )
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Validates that the return request is in an editable status (REQUESTED only).
     * Returns in UNDER_INSPECTION, APPROVED, REJECTED, etc. cannot be modified.
     */
    fun validateEditableStatus(request: ReturnRequest): DomainResult<Unit> {
        if (request.status != com.sucharu.sucharupro.domain.model.returns.ReturnStatus.REQUESTED) {
            return DomainResult.Error(
                message = "Modification not allowed: Return '${request.returnId}' is in status " +
                    "'${request.status.name}'. Only returns in REQUESTED status can be modified or cancelled."
            )
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Validates that the requested return quantity does not exceed delivered/eligible items.
     */
    fun validateQuantityEligibility(
        requestedQuantity: Int,
        maxEligibleQuantity: Int
    ): DomainResult<Unit> {
        if (requestedQuantity <= 0) {
            return DomainResult.Error(
                message = "Return quantity must be strictly greater than 0 (got $requestedQuantity)."
            )
        }
        if (requestedQuantity > maxEligibleQuantity) {
            return DomainResult.Error(
                message = "Return quantity ($requestedQuantity) cannot exceed delivered quantity ($maxEligibleQuantity)."
            )
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Validates that the source Challan belongs to the expected project and customer.
     */
    fun validateSourceChallanMatch(
        challanProjectId: String,
        requestProjectId: String,
        challanCustomerId: String?,
        requestCustomerId: String
    ): DomainResult<Unit> {
        if (challanProjectId != requestProjectId) {
            return DomainResult.Error(
                message = "Source Challan project mismatch: Challan belongs to project '$challanProjectId' " +
                    "but Return is in project '$requestProjectId'."
            )
        }
        if (challanCustomerId != null && challanCustomerId != requestCustomerId) {
            return DomainResult.Error(
                message = "Source Challan customer mismatch: Challan belongs to customer '$challanCustomerId' " +
                    "but Return is for customer '$requestCustomerId'."
            )
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Validates that the product referenced in the return item matches the source challan item.
     */
    fun validateSourceItemMatch(
        challanItemProductId: String,
        requestItemProductId: String
    ): DomainResult<Unit> {
        if (challanItemProductId != requestItemProductId) {
            return DomainResult.Error(
                message = "Product mismatch: Source Challan item has product '$challanItemProductId' " +
                    "but Return item references product '$requestItemProductId'."
            )
        }
        return DomainResult.Success(Unit)
    }
}

