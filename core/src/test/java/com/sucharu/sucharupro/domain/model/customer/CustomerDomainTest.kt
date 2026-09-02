package com.sucharu.sucharupro.domain.model.customer

import com.sucharu.sucharupro.domain.model.common.toMoney
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomerDomainTest {

    @Test
    fun customer_instantiation_success() {
        val customer = Customer(
            customerId = "cus-001",
            customerCode = "CUS-000001",
            displayName = "Md. Abdullah Rahman",
            customerType = CustomerType.INDIVIDUAL,
            status = CustomerStatusType.ACTIVE,
            primaryPhone = "+880 1711-234567",
            email = "abdullah@example.com",
            addresses = listOf(
                CustomerAddress(
                    addressLine = "12/A Motijheel C/A",
                    city = "Dhaka",
                    district = "Dhaka",
                    addressType = CustomerAddressType.PRIMARY,
                    isDefault = true
                )
            ),
            creditProfile = CustomerCreditProfile(
                creditLimit = 50000.toMoney(),
                paymentTermDays = 30,
                isCreditAllowed = true,
                isAdvanceRequired = false
            ),
            createdAt = "2026-08-15T09:00:00Z",
            updatedAt = "2026-08-15T09:00:00Z"
        )

        assertEquals("cus-001", customer.customerId)
        assertEquals("CUS-000001", customer.customerCode)
        assertEquals("Md. Abdullah Rahman", customer.displayName)
        assertEquals(CustomerType.INDIVIDUAL, customer.customerType)
        assertEquals(CustomerStatusType.ACTIVE, customer.status)
        assertNotNull(customer.primaryAddress)
        assertEquals("12/A Motijheel C/A, Dhaka, Dhaka, Bangladesh", customer.primaryAddress?.formatted())
        assertTrue(customer.creditProfile.isCreditAllowed)
        assertEquals("৳ 50,000", customer.creditProfile.creditLimit.formatted())
    }

    @Test(expected = IllegalArgumentException::class)
    fun customer_withBlankId_throwsException() {
        Customer(
            customerId = "",
            customerCode = "CUS-000001",
            displayName = "Noor Academy",
            primaryPhone = "01700000000",
            createdAt = "2026-08-15T09:00:00Z",
            updatedAt = "2026-08-15T09:00:00Z"
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun customer_withNegativeCreditLimit_throwsException() {
        CustomerCreditProfile(
            creditLimit = (-100).toMoney(),
            paymentTermDays = 15
        )
    }

    @Test
    fun customerType_canonicalValues_exist() {
        val types = CustomerType.entries
        assertEquals(8, types.size)
        val typeNames = types.map { it.name }
        assertTrue(typeNames.contains("INDIVIDUAL"))
        assertTrue(typeNames.contains("BUSINESS"))
        assertTrue(typeNames.contains("DEALER"))
        assertTrue(typeNames.contains("VIP"))
        assertTrue(typeNames.contains("GOVERNMENT"))
        assertTrue(typeNames.contains("INSTITUTION"))
        assertTrue(typeNames.contains("ORGANIZATION"))
        assertTrue(typeNames.contains("OTHER"))

        // Verify default labels
        assertEquals("Individual", CustomerType.INDIVIDUAL.defaultLabel)
        assertEquals("Business", CustomerType.BUSINESS.defaultLabel)
        assertEquals("Dealer", CustomerType.DEALER.defaultLabel)
        assertEquals("VIP", CustomerType.VIP.defaultLabel)
        assertEquals("Government", CustomerType.GOVERNMENT.defaultLabel)
        assertEquals("Institution", CustomerType.INSTITUTION.defaultLabel)
        assertEquals("Organization", CustomerType.ORGANIZATION.defaultLabel)
        assertEquals("Other", CustomerType.OTHER.defaultLabel)

        // Compatibility alias
        assertEquals(CustomerType.INDIVIDUAL, CustomerType.RETAIL)
    }

    @Test
    fun customerStatusType_canonicalValues_exist() {
        val statuses = CustomerStatusType.entries.map { it.name }
        assertTrue(statuses.contains("ACTIVE"))
        assertTrue(statuses.contains("INACTIVE"))
        assertTrue(statuses.contains("BLOCKED"))
        assertTrue(statuses.contains("ARCHIVED"))
    }

    @Test
    fun customerClassification_strictDomainSeparation() {
        // Ensure CustomerStatusType represents client trade permission, not production or order lifecycle
        val statusNames = CustomerStatusType.entries.map { it.name }
        assertFalse(statusNames.contains("PRINTING"))
        assertFalse(statusNames.contains("DELIVERED"))
        assertFalse(statusNames.contains("IN_PRODUCTION"))
        assertFalse(statusNames.contains("PENDING"))
        assertFalse(statusNames.contains("READY"))
    }

    @Test
    fun customerNote_instantiationAndValidation() {
        val note = CustomerNote(
            id = "note-001",
            customerId = "cus-001",
            text = "গ্রাহক বইয়ের প্রুফ সরাসরি শোরুমে এসে চেক করতে পছন্দ করেন।",
            isImportant = true,
            authorName = "মোঃ রফিকুল ইসলাম",
            createdAt = "2026-08-10T14:30:00Z",
            updatedAt = "2026-08-10T14:30:00Z"
        )
        assertEquals("note-001", note.id)
        assertEquals("cus-001", note.customerId)
        assertTrue(note.isImportant)
        assertEquals("মোঃ রফিকুল ইসলাম", note.authorName)
    }

    @Test(expected = IllegalArgumentException::class)
    fun customerNote_withBlankText_throwsException() {
        CustomerNote(
            id = "note-001",
            customerId = "cus-001",
            text = "   ",
            createdAt = "2026-08-10T14:30:00Z",
            updatedAt = "2026-08-10T14:30:00Z"
        )
    }

    @Test
    fun customerActivity_instantiation() {
        val activity = CustomerActivity(
            id = "act-001",
            customerId = "cus-001",
            type = CustomerActivityType.NOTE_ADDED,
            description = "Note added: Special instruction",
            timestamp = "2026-08-10T14:30:00Z",
            actorName = "Staff"
        )
        assertEquals("act-001", activity.id)
        assertEquals(CustomerActivityType.NOTE_ADDED, activity.type)
        assertEquals("Staff", activity.actorName)
    }

    @Test
    fun customer_withLastActivityAndFollowUp_instantiatesSuccessfully() {
        val customer = Customer(
            customerId = "cus-001",
            customerCode = "CUS-000001",
            displayName = "Md. Abdullah Rahman",
            primaryPhone = "01711234567",
            createdAt = "2026-01-10T10:00:00Z",
            updatedAt = "2026-08-10T14:30:00Z",
            lastActivityAt = "2026-08-10T14:30:00Z",
            nextFollowUpAt = "2026-08-25T10:00:00Z"
        )
        assertEquals("2026-08-10T14:30:00Z", customer.lastActivityAt)
        assertEquals("2026-08-25T10:00:00Z", customer.nextFollowUpAt)
    }
}
