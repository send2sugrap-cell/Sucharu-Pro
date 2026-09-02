package com.sucharu.sucharupro.vendor

import com.sucharu.sucharupro.data.datasource.*
import com.sucharu.sucharupro.data.repository.*
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.*
import com.sucharu.sucharupro.domain.service.vendor.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class VendorProfileTenantIsolationTest {

    private lateinit var vendorService: VendorServiceImpl
    private lateinit var profileService: VendorProfileServiceImpl
    private lateinit var contactService: VendorContactServiceImpl
    private lateinit var capabilityService: VendorCapabilityServiceImpl

    @Before
    fun setUp() {
        val fakeVendorDs = FakeVendorDataSource()
        val fakeProfileDs = FakeVendorProfileDataSource()
        val fakeContactDs = FakeVendorContactDataSource()
        val fakeAddressDs = FakeVendorAddressDataSource()
        val fakeCapDs = FakeVendorCapabilityDataSource()

        val vendorRepo = VendorRepositoryImpl(fakeVendorDs)
        val profileRepo = VendorProfileRepositoryImpl(fakeProfileDs)
        val contactRepo = VendorContactRepositoryImpl(fakeContactDs)
        val addressRepo = VendorAddressRepositoryImpl(fakeAddressDs)
        val capabilityRepo = VendorCapabilityRepositoryImpl(fakeCapDs)

        vendorService = VendorServiceImpl(vendorRepo)
        profileService = VendorProfileServiceImpl(vendorRepo, profileRepo)
        contactService = VendorContactServiceImpl(vendorRepo, contactRepo)
        capabilityService = VendorCapabilityServiceImpl(vendorRepo, capabilityRepo)
    }

    @Test
    fun `profile, contact and capability records in Tenant A are completely inaccessible in Tenant B`() = runBlocking {
        // Create vendor and profile in Tenant A
        val vResA = vendorService.createVendor(
            projectId = "tenant_A",
            vendorName = "Dhaka Paper Mart",
            vendorType = VendorType.MATERIAL_SUPPLIER,
            vendorCategory = VendorCategory.PAPER_SUPPLIER
        )
        assertTrue(vResA is DomainResult.Success)
        val vendorIdA = (vResA as DomainResult.Success).data.vendorId

        profileService.updateProfile("tenant_A", vendorIdA, "Dhaka Paper Mart - HQ", email = "dpm@tenantA.com")
        val cntResA = contactService.createContact("tenant_A", vendorIdA, "Abul Kalam", email = "kalam@tenantA.com")
        val capResA = capabilityService.createCapability("tenant_A", vendorIdA, CapabilityType.TRANSPORT)

        // Attempt reading from Tenant B
        val profB = profileService.getProfile("tenant_B", vendorIdA)
        assertTrue(profB is DomainResult.Error)

        val contactsB = contactService.listContacts("tenant_B", vendorIdA)
        assertTrue(contactsB is DomainResult.Success)
        assertEquals(0, (contactsB as DomainResult.Success).data.size)

        val capIdA = (capResA as DomainResult.Success).data.capabilityId
        val capB = capabilityService.getCapabilityById("tenant_B", capIdA)
        assertTrue(capB is DomainResult.Error)

        val hasCapB = capabilityService.hasCapability("tenant_B", vendorIdA, CapabilityType.TRANSPORT)
        assertFalse(hasCapB)
    }
}
