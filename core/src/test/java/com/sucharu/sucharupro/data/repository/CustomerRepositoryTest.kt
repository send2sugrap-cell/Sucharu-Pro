package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.datasource.FakeCustomerDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.toMoney
import com.sucharu.sucharupro.domain.model.customer.Customer
import com.sucharu.sucharupro.domain.model.customer.CustomerAddress
import com.sucharu.sucharupro.domain.model.customer.CustomerAddressType
import com.sucharu.sucharupro.domain.model.customer.CustomerCreditProfile
import com.sucharu.sucharupro.domain.model.customer.CustomerStatusType
import com.sucharu.sucharupro.domain.model.customer.CustomerType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CustomerRepositoryTest {

    private lateinit var repository: CustomerRepositoryImpl

    @Before
    fun setUp() {
        repository = CustomerRepositoryImpl(FakeCustomerDataSource())
    }

    @Test
    fun test01_getCustomers_returnsSampleCustomers() = runBlocking {
        val customers = repository.getCustomers().first()
        assertTrue(customers.isNotEmpty())
        assertEquals(10, customers.size)
    }

    @Test
    fun test02_getCustomerById_returnsCorrectCustomer() = runBlocking {
        val customer = repository.getCustomerById("cus-001").first()
        assertNotNull(customer)
        assertEquals("cus-001", customer?.customerId)
        assertEquals("Md. Abdullah Rahman", customer?.displayName)
    }

    @Test
    fun test03_unknownCustomerId_returnsNull() = runBlocking {
        val customer = repository.getCustomerById("unknown-999").first()
        assertNull(customer)

        val result = repository.findCustomerById("unknown-999")
        assertTrue(result is DomainResult.Error)
    }

    @Test
    fun test04_addCustomer_addsCustomerSuccessfully() = runBlocking {
        val newCustomer = Customer(
            customerId = "cus-999",
            customerCode = "CUS-000999",
            displayName = "Chowdhury Offset Press",
            customerType = CustomerType.BUSINESS,
            status = CustomerStatusType.ACTIVE,
            primaryPhone = "+880 1711-999888",
            createdAt = "2026-08-15T10:00:00Z",
            updatedAt = "2026-08-15T10:00:00Z"
        )

        val addResult = repository.addCustomer(newCustomer)
        assertTrue(addResult is DomainResult.Success)

        val fetched = repository.getCustomerById("cus-999").first()
        assertNotNull(fetched)
        assertEquals("Chowdhury Offset Press", fetched?.displayName)
    }

    @Test
    fun test05_updateCustomer_updatesExistingCustomer() = runBlocking {
        val existing = repository.getCustomerById("cus-001").first()!!
        val updated = existing.copy(
            displayName = "Md. Abdullah Rahman (Updated)",
            notes = "Updated author preferences"
        )

        val updateResult = repository.updateCustomer(updated)
        assertTrue(updateResult is DomainResult.Success)

        val fetched = repository.getCustomerById("cus-001").first()
        assertEquals("Md. Abdullah Rahman (Updated)", fetched?.displayName)
        assertEquals("Updated author preferences", fetched?.notes)
    }

    @Test
    fun test06_archiveCustomer_changesStatusToArchived() = runBlocking {
        val archiveResult = repository.archiveCustomer("cus-001")
        assertTrue(archiveResult is DomainResult.Success)

        val fetched = repository.getCustomerById("cus-001").first()
        assertNotNull(fetched)
        assertEquals(CustomerStatusType.ARCHIVED, fetched?.status)
    }

    @Test
    fun test07_archivedCustomer_remainsRetrievable() = runBlocking {
        val customer = repository.getCustomerById("cus-009").first()
        assertNotNull(customer)
        assertEquals("Apex Agro Chemical Packaging", customer?.displayName)
        assertEquals(CustomerStatusType.ARCHIVED, customer?.status)
    }

    @Test
    fun test08_search_byDisplayName() = runBlocking {
        val results = repository.searchCustomers("Abdullah").first()
        assertEquals(1, results.size)
        assertEquals("Md. Abdullah Rahman", results[0].displayName)
    }

    @Test
    fun test09_search_byCustomerCode() = runBlocking {
        val results = repository.searchCustomers("CUS-000003").first()
        assertEquals(1, results.size)
        assertEquals("Al-Noor Printing & Book Agency", results[0].displayName)
    }

    @Test
    fun test10_search_byPhone() = runBlocking {
        val results = repository.searchCustomers("1912-345678").first()
        assertEquals(1, results.size)
        assertEquals("Bengal Publications Ltd.", results[0].displayName)
    }

    @Test
    fun test11_search_caseInsensitive() = runBlocking {
        val lowerResults = repository.searchCustomers("bengal").first()
        val upperResults = repository.searchCustomers("BENGAL").first()
        assertEquals(1, lowerResults.size)
        assertEquals(lowerResults.size, upperResults.size)
        assertEquals("Bengal Publications Ltd.", lowerResults[0].displayName)
    }

    @Test
    fun test12_blankSearch_returnsAllCustomers() = runBlocking {
        val results = repository.searchCustomers("   ").first()
        assertEquals(10, results.size)
    }

    @Test
    fun test13_filterByType_returnsMatchingTypes() = runBlocking {
        val dealerResults = repository.filterCustomers(type = CustomerType.DEALER).first()
        assertTrue(dealerResults.isNotEmpty())
        assertTrue(dealerResults.all { it.customerType == CustomerType.DEALER })

        val institutionResults = repository.filterCustomers(type = CustomerType.INSTITUTION).first()
        assertEquals(2, institutionResults.size)
        assertTrue(institutionResults.all { it.customerType == CustomerType.INSTITUTION })
    }

    @Test
    fun test14_filterByStatus_returnsMatchingStatuses() = runBlocking {
        val activeResults = repository.filterCustomers(status = CustomerStatusType.ACTIVE).first()
        assertEquals(7, activeResults.size)
        assertTrue(activeResults.all { it.status == CustomerStatusType.ACTIVE })

        val blockedResults = repository.filterCustomers(status = CustomerStatusType.BLOCKED).first()
        assertEquals(1, blockedResults.size)
        assertEquals("cus-008", blockedResults[0].customerId)

        val archivedResults = repository.filterCustomers(status = CustomerStatusType.ARCHIVED).first()
        assertEquals(1, archivedResults.size)
        assertEquals("cus-009", archivedResults[0].customerId)
    }

    @Test
    fun test15_observeCustomerNotes_returnsNotesForSpecificCustomer() = runBlocking {
        val cus1Notes = repository.observeCustomerNotes("cus-001").first()
        assertEquals(2, cus1Notes.size)
        assertTrue(cus1Notes.all { it.customerId == "cus-001" })
        assertTrue(cus1Notes[0].isImportant) // Important notes ordered first
    }

    @Test
    fun test16_addCustomerNote_addsSuccessfullyAndTouchesLastActivity() = runBlocking {
        val newNote = com.sucharu.sucharupro.domain.model.customer.CustomerNote(
            id = "note-test-99",
            customerId = "cus-001",
            text = "গ্রাহক জরুরি ডেলিভারির জন্য অনুরোধ করেছেন।",
            isImportant = false,
            authorName = "মোঃ রফিক",
            createdAt = "2026-08-15T10:00:00Z",
            updatedAt = "2026-08-15T10:00:00Z"
        )
        val result = repository.addCustomerNote(newNote)
        assertTrue(result is DomainResult.Success)

        val notes = repository.observeCustomerNotes("cus-001").first()
        assertTrue(notes.any { it.id == "note-test-99" })

        // Check activity logged
        val activities = repository.observeCustomerActivities("cus-001").first()
        assertTrue(activities.any { it.type == com.sucharu.sucharupro.domain.model.customer.CustomerActivityType.NOTE_ADDED })
    }

    @Test
    fun test17_updateCustomerNote_updatesExistingNote() = runBlocking {
        val original = repository.observeCustomerNotes("cus-001").first().first { it.id == "note-002" }
        val updated = original.copy(text = "Updated preference: Morning delivery only.")

        val result = repository.updateCustomerNote(updated)
        assertTrue(result is DomainResult.Success)

        val notes = repository.observeCustomerNotes("cus-001").first()
        val fetched = notes.first { it.id == "note-002" }
        assertEquals("Updated preference: Morning delivery only.", fetched.text)
    }

    @Test
    fun test18_deleteCustomerNote_removesNote() = runBlocking {
        val deleteResult = repository.deleteCustomerNote("note-002", "cus-001")
        assertTrue(deleteResult is DomainResult.Success)

        val notes = repository.observeCustomerNotes("cus-001").first()
        assertFalse(notes.any { it.id == "note-002" })
    }

    @Test
    fun test19_toggleImportantNote_invertsImportance() = runBlocking {
        val toggleResult = repository.toggleImportantNote("note-001", "cus-001")
        assertTrue(toggleResult is DomainResult.Success)
        val toggled = (toggleResult as DomainResult.Success).data
        assertFalse(toggled.isImportant) // Was true, now false
    }

    @Test
    fun test20_customerNotesAndActivities_strictCustomerIsolation() = runBlocking {
        // Customer cus-001 must not see cus-002 notes
        val cus1Notes = repository.observeCustomerNotes("cus-001").first()
        val cus2Notes = repository.observeCustomerNotes("cus-002").first()

        assertTrue(cus1Notes.none { it.customerId == "cus-002" })
        assertTrue(cus2Notes.none { it.customerId == "cus-001" })

        // Customer cus-001 must not see cus-002 activities
        val cus1Activities = repository.observeCustomerActivities("cus-001").first()
        val cus2Activities = repository.observeCustomerActivities("cus-002").first()

        assertTrue(cus1Activities.none { it.customerId == "cus-002" })
        assertTrue(cus2Activities.none { it.customerId == "cus-001" })
    }

    @Test
    fun test21_setAndClearFollowUpDate_updatesCustomer() = runBlocking {
        val setResult = repository.setFollowUpDate("cus-001", "2026-08-25")
        assertTrue(setResult is DomainResult.Success)

        val customer = repository.getCustomerById("cus-001").first()
        assertNotNull(customer)
        assertEquals("2026-08-25", customer?.nextFollowUpAt)

        val clearResult = repository.setFollowUpDate("cus-001", null)
        assertTrue(clearResult is DomainResult.Success)

        val clearedCustomer = repository.getCustomerById("cus-001").first()
        assertNull(clearedCustomer?.nextFollowUpAt)
    }

    @Test
    fun test22_deactivateAndReactivateCustomer_lifecycleTransitions() = runBlocking {
        // Deactivate cus-001
        val deactResult = repository.deactivateCustomer("cus-001")
        assertTrue(deactResult is DomainResult.Success)

        var customer = repository.getCustomerById("cus-001").first()
        assertEquals(CustomerStatusType.INACTIVE, customer?.status)

        // Verify notes and activities remain fully preserved
        val notes = repository.observeCustomerNotes("cus-001").first()
        assertTrue(notes.isNotEmpty())

        val activities = repository.observeCustomerActivities("cus-001").first()
        assertTrue(activities.any { it.type == com.sucharu.sucharupro.domain.model.customer.CustomerActivityType.STATUS_CHANGED })

        // Reactivate cus-001
        val reactResult = repository.reactivateCustomer("cus-001")
        assertTrue(reactResult is DomainResult.Success)

        customer = repository.getCustomerById("cus-001").first()
        assertEquals(CustomerStatusType.ACTIVE, customer?.status)
    }

    @Test
    fun test23_findDuplicateCustomer_matchesExistingNormalizedContacts() = runBlocking {
        // cus-001 has phone: "+880 1711-234567" and email: "abdullah.rahman@gmail.com"
        val dupByPhone = repository.findDuplicateCustomer(phone = "01711234567", email = null)
        assertNotNull(dupByPhone)
        assertEquals("cus-001", dupByPhone?.customerId)

        val dupByEmail = repository.findDuplicateCustomer(phone = "01999999999", email = "  ABDULLAH.rahman@gmail.com ")
        assertNotNull(dupByEmail)
        assertEquals("cus-001", dupByEmail?.customerId)

        val noDup = repository.findDuplicateCustomer(phone = "01988888888", email = "unique.new@domain.com")
        assertNull(noDup)
    }

    @Test
    fun test24_findDuplicateCustomer_excludesSelfIdOnEdit() = runBlocking {
        // When editing cus-001, matching cus-001's own phone/email should not trigger duplicate with itself
        val selfDup = repository.findDuplicateCustomer(
            phone = "+880 1711-234567",
            email = "abdullah.rahman@gmail.com",
            excludeCustomerId = "cus-001"
        )
        assertNull(selfDup)
    }
}
