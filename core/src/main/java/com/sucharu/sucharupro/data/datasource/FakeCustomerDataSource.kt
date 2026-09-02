package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.common.toMoney
import com.sucharu.sucharupro.domain.model.customer.Customer
import com.sucharu.sucharupro.domain.model.customer.CustomerActivity
import com.sucharu.sucharupro.domain.model.customer.CustomerActivityType
import com.sucharu.sucharupro.domain.model.customer.CustomerAddress
import com.sucharu.sucharupro.domain.model.customer.CustomerAddressType
import com.sucharu.sucharupro.domain.model.customer.CustomerCreditProfile
import com.sucharu.sucharupro.domain.model.customer.CustomerNote
import com.sucharu.sucharupro.domain.model.customer.CustomerStatusType
import com.sucharu.sucharupro.domain.model.customer.CustomerType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import java.util.UUID

/**
 * In-memory data source for Customer Management in Sucharu Pro.
 *
 * Pre-populated with 10 realistic sample customers representing all 6 canonical
 * customer types and diverse operational lifecycle statuses.
 */
class FakeCustomerDataSource : CustomerDataSource {

    private val _customers = MutableStateFlow<List<Customer>>(initialSampleCustomers())
    private val customersFlow = _customers.asStateFlow()

    private val _notes = MutableStateFlow<List<CustomerNote>>(initialSampleNotes())
    private val _activities = MutableStateFlow<List<CustomerActivity>>(initialSampleActivities())

    override fun observeCustomers(): Flow<List<Customer>> = customersFlow

    override suspend fun fetchCustomers(): DomainResult<List<Customer>> {
        return DomainResult.Success(_customers.value)
    }

    override suspend fun fetchCustomerById(customerId: String): DomainResult<Customer> {
        val found = _customers.value.find { it.customerId == customerId }
        return if (found != null) {
            DomainResult.Success(found)
        } else {
            DomainResult.Error(message = "Customer with ID '$customerId' not found.")
        }
    }

    override suspend fun insertCustomer(customer: Customer): DomainResult<Customer> {
        if (_customers.value.any { it.customerId == customer.customerId }) {
            return DomainResult.Error(message = "Customer with ID '${customer.customerId}' already exists.")
        }
        val timestamp = customer.createdAt
        val updatedCustomer = customer.copy(lastActivityAt = timestamp)
        _customers.value = listOf(updatedCustomer) + _customers.value

        // Record customer creation activity
        insertCustomerActivity(
            CustomerActivity(
                id = "act-${UUID.randomUUID().toString().take(8)}",
                customerId = customer.customerId,
                type = CustomerActivityType.CUSTOMER_CREATED,
                description = "Customer profile created: ${customer.displayName}",
                timestamp = timestamp,
                actorName = "Admin / Onboarding"
            )
        )

        return DomainResult.Success(updatedCustomer)
    }

    override suspend fun updateCustomer(customer: Customer): DomainResult<Customer> {
        val currentList = _customers.value
        val index = currentList.indexOfFirst { it.customerId == customer.customerId }
        if (index == -1) {
            return DomainResult.Error(message = "Cannot update: Customer '${customer.customerId}' does not exist.")
        }
        val timestamp = customer.updatedAt
        val updatedCustomer = customer.copy(lastActivityAt = timestamp)
        val updatedList = currentList.toMutableList()
        updatedList[index] = updatedCustomer
        _customers.value = updatedList

        // Record update activity
        insertCustomerActivity(
            CustomerActivity(
                id = "act-${UUID.randomUUID().toString().take(8)}",
                customerId = customer.customerId,
                type = CustomerActivityType.CUSTOMER_UPDATED,
                description = "Customer information updated: ${customer.displayName}",
                timestamp = timestamp,
                actorName = "Admin / Operations"
            )
        )

        return DomainResult.Success(updatedCustomer)
    }

    override suspend fun setCustomerStatus(
        customerId: String,
        status: CustomerStatusType
    ): DomainResult<Customer> {
        val currentList = _customers.value
        val existing = currentList.find { it.customerId == customerId }
            ?: return DomainResult.Error(message = "Customer '$customerId' not found.")

        val timestamp = "2026-08-15T12:00:00Z"
        val updated = existing.copy(
            status = status,
            updatedAt = timestamp,
            lastActivityAt = timestamp
        )
        val index = currentList.indexOf(existing)
        val updatedList = currentList.toMutableList()
        updatedList[index] = updated
        _customers.value = updatedList

        // Record status change activity
        insertCustomerActivity(
            CustomerActivity(
                id = "act-${UUID.randomUUID().toString().take(8)}",
                customerId = customerId,
                type = CustomerActivityType.STATUS_CHANGED,
                description = "Status changed to ${status.defaultLabel}",
                timestamp = timestamp,
                actorName = "Management"
            )
        )

        return DomainResult.Success(updated)
    }

    override fun observeCustomerNotes(customerId: String): Flow<List<CustomerNote>> {
        return _notes.map { allNotes ->
            allNotes.filter { it.customerId == customerId }
                .sortedWith(compareByDescending<CustomerNote> { it.isImportant }.thenByDescending { it.createdAt })
        }
    }

    override suspend fun insertCustomerNote(note: CustomerNote): DomainResult<CustomerNote> {
        _notes.value = listOf(note) + _notes.value
        touchCustomerLastActivity(note.customerId, note.createdAt)

        insertCustomerActivity(
            CustomerActivity(
                id = "act-${UUID.randomUUID().toString().take(8)}",
                customerId = note.customerId,
                type = CustomerActivityType.NOTE_ADDED,
                description = if (note.isImportant) "[Important] Note added: ${note.text.take(40)}..." else "Note added: ${note.text.take(40)}...",
                timestamp = note.createdAt,
                actorName = note.authorName ?: "Staff"
            )
        )
        return DomainResult.Success(note)
    }

    override suspend fun updateCustomerNote(note: CustomerNote): DomainResult<CustomerNote> {
        val currentNotes = _notes.value
        val index = currentNotes.indexOfFirst { it.id == note.id && it.customerId == note.customerId }
        if (index == -1) {
            return DomainResult.Error(message = "Note with ID '${note.id}' not found for customer '${note.customerId}'.")
        }
        val updatedList = currentNotes.toMutableList()
        updatedList[index] = note
        _notes.value = updatedList
        touchCustomerLastActivity(note.customerId, note.updatedAt)

        insertCustomerActivity(
            CustomerActivity(
                id = "act-${UUID.randomUUID().toString().take(8)}",
                customerId = note.customerId,
                type = CustomerActivityType.NOTE_UPDATED,
                description = "Note updated: ${note.text.take(40)}...",
                timestamp = note.updatedAt,
                actorName = note.authorName ?: "Staff"
            )
        )
        return DomainResult.Success(note)
    }

    override suspend fun deleteCustomerNote(noteId: String, customerId: String): DomainResult<Unit> {
        val currentNotes = _notes.value
        val found = currentNotes.find { it.id == noteId && it.customerId == customerId }
            ?: return DomainResult.Error(message = "Note with ID '$noteId' not found for customer '$customerId'.")

        _notes.value = currentNotes.filterNot { it.id == noteId && it.customerId == customerId }
        val timestamp = "2026-08-15T12:00:00Z"
        touchCustomerLastActivity(customerId, timestamp)

        insertCustomerActivity(
            CustomerActivity(
                id = "act-${UUID.randomUUID().toString().take(8)}",
                customerId = customerId,
                type = CustomerActivityType.NOTE_DELETED,
                description = "Note deleted: ${found.text.take(30)}...",
                timestamp = timestamp,
                actorName = "Staff"
            )
        )
        return DomainResult.Success(Unit)
    }

    override suspend fun toggleImportantNote(noteId: String, customerId: String): DomainResult<CustomerNote> {
        val currentNotes = _notes.value
        val index = currentNotes.indexOfFirst { it.id == noteId && it.customerId == customerId }
        if (index == -1) {
            return DomainResult.Error(message = "Note with ID '$noteId' not found for customer '$customerId'.")
        }
        val existing = currentNotes[index]
        val toggled = existing.copy(
            isImportant = !existing.isImportant,
            updatedAt = "2026-08-15T12:00:00Z"
        )
        val updatedList = currentNotes.toMutableList()
        updatedList[index] = toggled
        _notes.value = updatedList
        return DomainResult.Success(toggled)
    }

    override fun observeCustomerActivities(customerId: String): Flow<List<CustomerActivity>> {
        return _activities.map { allActivities ->
            allActivities.filter { it.customerId == customerId }
                .sortedByDescending { it.timestamp }
        }
    }

    override suspend fun insertCustomerActivity(activity: CustomerActivity): DomainResult<CustomerActivity> {
        _activities.value = listOf(activity) + _activities.value
        return DomainResult.Success(activity)
    }

    override suspend fun setCustomerFollowUp(customerId: String, followUpAt: String?): DomainResult<Customer> {
        val currentList = _customers.value
        val existing = currentList.find { it.customerId == customerId }
            ?: return DomainResult.Error(message = "Customer '$customerId' not found.")

        val timestamp = "2026-08-15T12:00:00Z"
        val updated = existing.copy(
            nextFollowUpAt = followUpAt,
            updatedAt = timestamp,
            lastActivityAt = timestamp
        )
        val index = currentList.indexOf(existing)
        val updatedList = currentList.toMutableList()
        updatedList[index] = updated
        _customers.value = updatedList

        insertCustomerActivity(
            CustomerActivity(
                id = "act-${UUID.randomUUID().toString().take(8)}",
                customerId = customerId,
                type = if (followUpAt != null) CustomerActivityType.FOLLOW_UP_SCHEDULED else CustomerActivityType.FOLLOW_UP_CLEARED,
                description = if (followUpAt != null) "Follow-up scheduled for $followUpAt" else "Follow-up cleared",
                timestamp = timestamp,
                actorName = "Operations"
            )
        )
        return DomainResult.Success(updated)
    }

    private fun touchCustomerLastActivity(customerId: String, timestamp: String) {
        val currentList = _customers.value
        val existing = currentList.find { it.customerId == customerId } ?: return
        val updated = existing.copy(lastActivityAt = timestamp)
        val index = currentList.indexOf(existing)
        val updatedList = currentList.toMutableList()
        updatedList[index] = updated
        _customers.value = updatedList
    }

    companion object {
        fun initialSampleNotes(): List<CustomerNote> = listOf(
            CustomerNote(
                id = "note-001",
                customerId = "cus-001",
                text = "গ্রাহক বইয়ের প্রুফ সরাসরি শোরুমে এসে চেক করতে পছন্দ করেন।",
                isImportant = true,
                authorName = "মোঃ রফিকুল ইসলাম (বিক্রয় নির্বাহী)",
                createdAt = "2026-08-10T14:30:00Z",
                updatedAt = "2026-08-10T14:30:00Z"
            ),
            CustomerNote(
                id = "note-002",
                customerId = "cus-001",
                text = "Preferred delivery schedule: Morning shift before 12:00 PM.",
                isImportant = false,
                authorName = "Customer Care",
                createdAt = "2026-08-05T11:00:00Z",
                updatedAt = "2026-08-05T11:00:00Z"
            ),
            CustomerNote(
                id = "note-003",
                customerId = "cus-002",
                text = "প্রতিটি চালানের সাথে দুটি কপি ডেলিভারি চালান ও বাংলা ভ্যাট ইনভয়েস সংযুক্ত করতে হবে।",
                isImportant = true,
                authorName = "Accounts Dept",
                createdAt = "2026-08-12T16:00:00Z",
                updatedAt = "2026-08-12T16:00:00Z"
            ),
            CustomerNote(
                id = "note-004",
                customerId = "cus-003",
                text = "বাংলা বাজার পাইকারি বুক এজেন্সি। কুরিয়ার পাঠানোর পূর্বে ফোনে অবহিত করতে হবে।",
                isImportant = true,
                authorName = "Dispatch In-charge",
                createdAt = "2026-08-14T09:45:00Z",
                updatedAt = "2026-08-14T09:45:00Z"
            )
        )

        fun initialSampleActivities(): List<CustomerActivity> = listOf(
            CustomerActivity(
                id = "act-001",
                customerId = "cus-001",
                type = CustomerActivityType.NOTE_ADDED,
                description = "[Important] Note added: গ্রাহক বইয়ের প্রুফ সরাসরি শোরুমে এসে...",
                timestamp = "2026-08-10T14:30:00Z",
                actorName = "মোঃ রফিকুল ইসলাম"
            ),
            CustomerActivity(
                id = "act-002",
                customerId = "cus-001",
                type = CustomerActivityType.CONTACT_UPDATED,
                description = "Alternate phone number updated",
                timestamp = "2026-08-05T10:15:00Z",
                actorName = "Admin"
            ),
            CustomerActivity(
                id = "act-003",
                customerId = "cus-001",
                type = CustomerActivityType.CUSTOMER_CREATED,
                description = "Customer profile created: Md. Abdullah Rahman",
                timestamp = "2026-01-10T10:00:00Z",
                actorName = "System"
            ),
            CustomerActivity(
                id = "act-004",
                customerId = "cus-002",
                type = CustomerActivityType.CUSTOMER_CREATED,
                description = "Customer profile created: Bengal Publications Ltd.",
                timestamp = "2026-02-01T09:00:00Z",
                actorName = "System"
            ),
            CustomerActivity(
                id = "act-005",
                customerId = "cus-003",
                type = CustomerActivityType.CUSTOMER_CREATED,
                description = "Customer profile created: Al-Noor Printing & Book Agency",
                timestamp = "2026-02-15T11:20:00Z",
                actorName = "System"
            )
        )

        fun initialSampleCustomers(): List<Customer> = listOf(
            // 1. INDIVIDUAL - Retail author/client
            Customer(
                customerId = "cus-001",
                customerCode = "CUS-000001",
                displayName = "Md. Abdullah Rahman",
                customerType = CustomerType.INDIVIDUAL,
                status = CustomerStatusType.ACTIVE,
                primaryPhone = "+880 1711-234567",
                alternatePhone = "+880 1819-876543",
                email = "abdullah.rahman@gmail.com",
                addresses = listOf(
                    CustomerAddress(
                        addressLine = "House 42, Road 11, Sector 4",
                        area = "Uttara",
                        city = "Dhaka",
                        district = "Dhaka",
                        postalCode = "1230",
                        addressType = CustomerAddressType.PRIMARY,
                        isDefault = true
                    )
                ),
                creditProfile = CustomerCreditProfile(
                    creditLimit = 25000.toMoney(),
                    paymentTermDays = 15,
                    isCreditAllowed = true,
                    isAdvanceRequired = false
                ),
                notes = "Author for Islamic literature books. Prefers 80 GSM art paper.",
                createdAt = "2026-01-10T10:00:00Z",
                updatedAt = "2026-08-10T14:30:00Z"
            ),

            // 2. BUSINESS - Corporate Publisher
            Customer(
                customerId = "cus-002",
                customerCode = "CUS-000002",
                displayName = "Bengal Publications Ltd.",
                customerType = CustomerType.BUSINESS,
                status = CustomerStatusType.ACTIVE,
                primaryPhone = "+880 1912-345678",
                email = "procurement@bengalpublications.com",
                contactPersonName = "Tanvir Ahmed (Production Manager)",
                addresses = listOf(
                    CustomerAddress(
                        addressLine = "12/A Motijheel C/A",
                        area = "Motijheel",
                        city = "Dhaka",
                        district = "Dhaka",
                        postalCode = "1000",
                        addressType = CustomerAddressType.BILLING,
                        isDefault = true
                    ),
                    CustomerAddress(
                        addressLine = "Plot 84, BSCIC Industrial Area",
                        area = "Tongi",
                        city = "Gazipur",
                        district = "Gazipur",
                        postalCode = "1710",
                        addressType = CustomerAddressType.DELIVERY
                    )
                ),
                creditProfile = CustomerCreditProfile(
                    creditLimit = 150000.toMoney(),
                    paymentTermDays = 30,
                    isCreditAllowed = true,
                    isAdvanceRequired = false
                ),
                notes = "High-volume offset printing client. Orders 10,000+ copies runs.",
                createdAt = "2026-02-01T09:00:00Z",
                updatedAt = "2026-08-12T16:00:00Z"
            ),

            // 3. DEALER - Wholesale Book Dealer
            Customer(
                customerId = "cus-003",
                customerCode = "CUS-000003",
                displayName = "Al-Noor Printing & Book Agency",
                customerType = CustomerType.DEALER,
                status = CustomerStatusType.ACTIVE,
                primaryPhone = "+880 1822-456789",
                email = "alnoor.banglabazar@yahoo.com",
                contactPersonName = "Haji Nurul Islam",
                addresses = listOf(
                    CustomerAddress(
                        addressLine = "34/1 Pari Das Road",
                        area = "Bangla Bazar",
                        city = "Dhaka",
                        district = "Dhaka",
                        postalCode = "1100",
                        addressType = CustomerAddressType.PRIMARY,
                        isDefault = true
                    )
                ),
                creditProfile = CustomerCreditProfile(
                    creditLimit = 300000.toMoney(),
                    paymentTermDays = 45,
                    isCreditAllowed = true,
                    isAdvanceRequired = false
                ),
                affiliateId = "aff-001",
                referralCode = "REF-BANG-01",
                notes = "Bangla Bazar wholesale book distributor for Quran Sharif and Qaida sets.",
                createdAt = "2026-02-15T11:20:00Z",
                updatedAt = "2026-08-14T09:45:00Z"
            ),

            // 4. INSTITUTION - Madrasa & Academy
            Customer(
                customerId = "cus-004",
                customerCode = "CUS-000004",
                displayName = "Darul Uloom Madrasa & Orphanage",
                customerType = CustomerType.INSTITUTION,
                status = CustomerStatusType.ACTIVE,
                primaryPhone = "+880 1733-567890",
                email = "info@darululoom-jatrabari.org",
                contactPersonName = "Mufti Harunur Rashid",
                addresses = listOf(
                    CustomerAddress(
                        addressLine = "Kajla Main Road",
                        area = "Jatrabari",
                        city = "Dhaka",
                        district = "Dhaka",
                        postalCode = "1204",
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
                notes = "Annual syllabus and certificate printing client.",
                createdAt = "2026-03-05T08:30:00Z",
                updatedAt = "2026-08-01T11:15:00Z"
            ),

            // 5. GOVERNMENT - National Board / Ministry
            Customer(
                customerId = "cus-005",
                customerCode = "CUS-000005",
                displayName = "জাতীয় শিক্ষাক্রম ও পাঠ্যপুস্তক বোর্ড (NCTB)",
                customerType = CustomerType.GOVERNMENT,
                status = CustomerStatusType.ACTIVE,
                primaryPhone = "+880 1644-678901",
                email = "procurement@nctb.gov.bd",
                contactPersonName = "Farzana Yasmin (Deputy Director)",
                addresses = listOf(
                    CustomerAddress(
                        addressLine = "69-70 Motijheel C/A",
                        area = "Motijheel",
                        city = "Dhaka",
                        district = "Dhaka",
                        postalCode = "1000",
                        addressType = CustomerAddressType.PRIMARY,
                        isDefault = true
                    )
                ),
                creditProfile = CustomerCreditProfile(
                    creditLimit = Money.ZERO,
                    paymentTermDays = 0,
                    isCreditAllowed = false,
                    isAdvanceRequired = true
                ),
                notes = "Requires formal Challan and VAT invoice with every delivery.",
                createdAt = "2026-03-20T14:10:00Z",
                updatedAt = "2026-07-28T10:00:00Z"
            ),

            // 6. VIP - Premier High-Value Client
            Customer(
                customerId = "cus-006",
                customerCode = "CUS-000006",
                displayName = "City Creative Pack & Print (VIP)",
                customerType = CustomerType.VIP,
                status = CustomerStatusType.ACTIVE,
                primaryPhone = "+880 1555-789012",
                email = "vip@citycreative.com.bd",
                contactPersonName = "Zahidul Karim",
                addresses = listOf(
                    CustomerAddress(
                        addressLine = "Station Road, Kadamtali",
                        area = "Kotwali",
                        city = "Chittagong",
                        district = "Chattogram",
                        postalCode = "4000",
                        addressType = CustomerAddressType.PRIMARY,
                        isDefault = true
                    )
                ),
                creditProfile = CustomerCreditProfile(
                    creditLimit = 100000.toMoney(),
                    paymentTermDays = 15,
                    isCreditAllowed = true,
                    isAdvanceRequired = false
                ),
                notes = "VIP client: Priority press scheduling and specialized rigid box finishing.",
                createdAt = "2026-04-10T16:40:00Z",
                updatedAt = "2026-08-05T12:00:00Z"
            ),

            // 7. DEALER - Inactive regional reseller
            Customer(
                customerId = "cus-007",
                customerCode = "CUS-000007",
                displayName = "Meghna Paper & Stationery Mart",
                customerType = CustomerType.DEALER,
                status = CustomerStatusType.INACTIVE,
                primaryPhone = "+880 1766-890123",
                email = "meghna.bogura@gmail.com",
                contactPersonName = "Shamsul Huq",
                addresses = listOf(
                    CustomerAddress(
                        addressLine = "Boro Gola Bazar",
                        area = "Sadar",
                        city = "Bogura",
                        district = "Bogura",
                        postalCode = "5800",
                        addressType = CustomerAddressType.PRIMARY,
                        isDefault = true
                    )
                ),
                creditProfile = CustomerCreditProfile.DEFAULT_CASH_ONLY,
                notes = "Seasonal calendar reseller. Inactive during non-peak months.",
                createdAt = "2026-01-20T10:00:00Z",
                updatedAt = "2026-06-15T09:00:00Z"
            ),

            // 8. BUSINESS - Blocked due to payment default
            Customer(
                customerId = "cus-008",
                customerCode = "CUS-000008",
                displayName = "Prime Media & Design Studio",
                customerType = CustomerType.BUSINESS,
                status = CustomerStatusType.BLOCKED,
                primaryPhone = "+880 1877-901234",
                email = "finance@primemedia.com",
                contactPersonName = "Kamrul Hassan",
                addresses = listOf(
                    CustomerAddress(
                        addressLine = "Section 10, Block C",
                        area = "Mirpur",
                        city = "Dhaka",
                        district = "Dhaka",
                        postalCode = "1216",
                        addressType = CustomerAddressType.PRIMARY,
                        isDefault = true
                    )
                ),
                creditProfile = CustomerCreditProfile(
                    creditLimit = Money.ZERO,
                    paymentTermDays = 0,
                    isCreditAllowed = false,
                    isAdvanceRequired = true
                ),
                notes = "BLOCKED: Chronic overdue payment. Management approval required before new jobs.",
                createdAt = "2026-02-10T13:00:00Z",
                updatedAt = "2026-08-01T15:30:00Z"
            ),

            // 9. BUSINESS - Soft-deleted / Archived client
            Customer(
                customerId = "cus-009",
                customerCode = "CUS-000009",
                displayName = "Apex Agro Chemical Packaging",
                customerType = CustomerType.BUSINESS,
                status = CustomerStatusType.ARCHIVED,
                primaryPhone = "+880 1988-012345",
                email = "info@apexagro.com.bd",
                contactPersonName = "Shafiqul Alam",
                addresses = listOf(
                    CustomerAddress(
                        addressLine = "Joydebpur Road",
                        area = "Sadar",
                        city = "Gazipur",
                        district = "Gazipur",
                        postalCode = "1700",
                        addressType = CustomerAddressType.PRIMARY,
                        isDefault = true
                    )
                ),
                creditProfile = CustomerCreditProfile.DEFAULT_CASH_ONLY,
                notes = "Company operations ceased. Archived for tax and ledger audit records.",
                createdAt = "2025-11-15T09:00:00Z",
                updatedAt = "2026-05-30T17:00:00Z"
            ),

            // 10. INSTITUTION - Library Trust
            Customer(
                customerId = "cus-010",
                customerCode = "CUS-000010",
                displayName = "Kazi Nazrul Islam Library Trust",
                customerType = CustomerType.INSTITUTION,
                status = CustomerStatusType.ACTIVE,
                primaryPhone = "+880 1799-123456",
                email = "library@nazrultrust.org",
                contactPersonName = "Dr. Rezaul Karim",
                addresses = listOf(
                    CustomerAddress(
                        addressLine = "Zinda Bazar Main Road",
                        area = "Kotwali",
                        city = "Sylhet",
                        district = "Sylhet",
                        postalCode = "3100",
                        addressType = CustomerAddressType.PRIMARY,
                        isDefault = true
                    )
                ),
                creditProfile = CustomerCreditProfile(
                    creditLimit = 40000.toMoney(),
                    paymentTermDays = 20,
                    isCreditAllowed = true,
                    isAdvanceRequired = false
                ),
                notes = "Specializes in hardbound book restoration and reprinting.",
                createdAt = "2026-04-25T11:00:00Z",
                updatedAt = "2026-08-08T13:20:00Z"
            )
        )
    }
}
