package com.sucharu.sucharupro.domain.validation.vendor

import com.sucharu.sucharupro.domain.model.vendor.VendorAddress

/**
 * Domain validator for VendorAddress entity (Module 12 Step 02).
 */
object VendorAddressValidator {

    fun validate(address: VendorAddress): VendorValidationResult {
        val errors = mutableListOf<String>()

        if (address.addressId.isBlank()) {
            errors.add("addressId cannot be blank")
        }
        if (address.vendorId.isBlank()) {
            errors.add("vendorId cannot be blank")
        }
        if (address.projectId.isBlank()) {
            errors.add("projectId cannot be blank")
        }
        if (address.addressLine1.isBlank()) {
            errors.add("addressLine1 cannot be blank")
        } else if (address.addressLine1.length > 255) {
            errors.add("addressLine1 exceeds maximum length of 255 characters")
        }

        address.addressLine2?.let {
            if (it.length > 255) errors.add("addressLine2 exceeds maximum length of 255 characters")
        }

        if (address.city.isBlank()) {
            errors.add("city cannot be blank")
        } else if (address.city.length > 100) {
            errors.add("city exceeds maximum length of 100 characters")
        }

        address.district?.let {
            if (it.length > 100) errors.add("district exceeds maximum length of 100 characters")
        }

        address.postalCode?.let {
            if (it.length > 20) errors.add("postalCode exceeds maximum length of 20 characters")
        }

        if (address.country.isBlank()) {
            errors.add("country cannot be blank")
        } else if (address.country.length > 100) {
            errors.add("country exceeds maximum length of 100 characters")
        }

        address.notes?.let {
            if (it.length > 1000) errors.add("notes exceed maximum length of 1000 characters")
        }

        return if (errors.isEmpty()) VendorValidationResult(true) else VendorValidationResult(false, errors)
    }
}
