package com.sucharu.sucharupro.vendor

import com.sucharu.sucharupro.data.datasource.FakeVendorAddressDataSource
import com.sucharu.sucharupro.data.datasource.FakeVendorCapabilityDataSource
import com.sucharu.sucharupro.data.datasource.FakeVendorContactDataSource
import com.sucharu.sucharupro.data.datasource.FakeVendorProfileDataSource
import com.sucharu.sucharupro.data.repository.VendorAddressRepositoryImpl
import com.sucharu.sucharupro.data.repository.VendorCapabilityRepositoryImpl
import com.sucharu.sucharupro.data.repository.VendorContactRepositoryImpl
import com.sucharu.sucharupro.data.repository.VendorProfileRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class VendorProfileRepositoryTest {

    @Test
    fun `VendorProfileRepository save, retrieve and optimistic concurrency conflict`() = runBlocking {
        val repo = VendorProfileRepositoryImpl(FakeVendorProfileDataSource())

        val p1 = VendorProfile(
            vendorId = "vnd_001",
            projectId = "p_001",
            displayName = "Press A"
        )
        val saveRes1 = repo.saveProfile(p1)
        assertTrue(saveRes1 is DomainResult.Success)
        val saved1 = (saveRes1 as DomainResult.Success).data
        assertEquals(1L, saved1.version)

        val retrieved = repo.findByVendorId("p_001", "vnd_001")
        assertTrue(retrieved is DomainResult.Success)
        assertEquals("Press A", (retrieved as DomainResult.Success).data.displayName)

        // Valid update
        val updatedRes = repo.saveProfile(saved1.copy(displayName = "Press A Updated"))
        assertTrue(updatedRes is DomainResult.Success)
        val updated = (updatedRes as DomainResult.Success).data
        assertEquals(2L, updated.version)

        // Stale update should fail
        val staleRes = repo.saveProfile(saved1.copy(displayName = "Stale Update"))
        assertTrue(staleRes is DomainResult.Error)
        assertTrue((staleRes as DomainResult.Error).message.contains("conflict"))
    }

    @Test
    fun `VendorContactRepository CRUD and status update`() = runBlocking {
        val repo = VendorContactRepositoryImpl(FakeVendorContactDataSource())

        val contact = VendorContact(
            contactId = "cnt_1",
            vendorId = "vnd_1",
            projectId = "p_1",
            name = "Anisur Rahman",
            isPrimary = true,
            active = true
        )
        val createRes = repo.createContact(contact)
        assertTrue(createRes is DomainResult.Success)

        val list = repo.listByVendor("p_1", "vnd_1")
        assertTrue(list is DomainResult.Success)
        assertEquals(1, (list as DomainResult.Success).data.size)

        // Deactivate contact
        val statusRes = repo.updateStatus("p_1", "cnt_1", false, "admin")
        assertTrue(statusRes is DomainResult.Success)
        assertFalse((statusRes as DomainResult.Success).data.active)

        val activeOnly = repo.listByVendor("p_1", "vnd_1", activeOnly = true)
        assertTrue(activeOnly is DomainResult.Success)
        assertEquals(0, (activeOnly as DomainResult.Success).data.size)
    }

    @Test
    fun `VendorAddressRepository CRUD and filtering`() = runBlocking {
        val repo = VendorAddressRepositoryImpl(FakeVendorAddressDataSource())

        val addr = VendorAddress(
            addressId = "addr_1",
            vendorId = "vnd_1",
            projectId = "p_1",
            addressLine1 = "Road 5, Dhanmondi",
            city = "Dhaka",
            country = "Bangladesh"
        )
        repo.createAddress(addr)

        val found = repo.findById("p_1", "addr_1")
        assertTrue(found is DomainResult.Success)
        assertEquals("Road 5, Dhanmondi", (found as DomainResult.Success).data.addressLine1)
    }

    @Test
    fun `VendorCapabilityRepository uniqueness and lookup`() = runBlocking {
        val repo = VendorCapabilityRepositoryImpl(FakeVendorCapabilityDataSource())

        val cap = VendorCapability(
            capabilityId = "cap_1",
            vendorId = "vnd_1",
            projectId = "p_1",
            capabilityType = CapabilityType.LAMINATION,
            displayName = "Thermal & Gloss Lamination",
            status = CapabilityStatus.ACTIVE
        )
        val createRes = repo.createCapability(cap)
        assertTrue(createRes is DomainResult.Success)

        assertTrue(repo.existsByVendorAndType("p_1", "vnd_1", CapabilityType.LAMINATION))
        assertFalse(repo.existsByVendorAndType("p_1", "vnd_1", CapabilityType.FOILING))

        // Duplicate capability for same vendor should fail
        val dupCap = cap.copy(capabilityId = "cap_2")
        val dupRes = repo.createCapability(dupCap)
        assertTrue(dupRes is DomainResult.Error)

        val vendors = repo.listVendorsByCapability("p_1", CapabilityType.LAMINATION)
        assertTrue(vendors is DomainResult.Success)
        assertEquals(listOf("vnd_1"), (vendors as DomainResult.Success).data)
    }
}
