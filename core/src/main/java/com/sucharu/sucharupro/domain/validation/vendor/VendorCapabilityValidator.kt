package com.sucharu.sucharupro.domain.validation.vendor

import com.sucharu.sucharupro.domain.model.vendor.CapabilityStatus
import com.sucharu.sucharupro.domain.model.vendor.VendorCapability

/**
 * Domain validator for VendorCapability entity (Module 12 Step 02).
 */
object VendorCapabilityValidator {

    fun validate(capability: VendorCapability): VendorValidationResult {
        val errors = mutableListOf<String>()

        if (capability.capabilityId.isBlank()) {
            errors.add("capabilityId cannot be blank")
        }
        if (capability.vendorId.isBlank()) {
            errors.add("vendorId cannot be blank")
        }
        if (capability.projectId.isBlank()) {
            errors.add("projectId cannot be blank")
        }
        if (capability.displayName.isBlank()) {
            errors.add("displayName cannot be blank")
        } else if (capability.displayName.length > 150) {
            errors.add("displayName exceeds maximum length of 150 characters")
        }

        capability.notes?.let {
            if (it.length > 1000) errors.add("notes exceed maximum length of 1000 characters")
        }

        return if (errors.isEmpty()) VendorValidationResult(true) else VendorValidationResult(false, errors)
    }

    fun validateStatusTransition(currentStatus: CapabilityStatus, newStatus: CapabilityStatus): VendorValidationResult {
        return VendorValidationResult(true)
    }
}
