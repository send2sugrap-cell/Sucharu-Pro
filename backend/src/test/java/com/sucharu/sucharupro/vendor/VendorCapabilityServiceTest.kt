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

class VendorCapabilityServiceTest {

    private lateinit var vendorRepo: VendorRepositoryImpl
    private lateinit var profileRepo: VendorProfileRepositoryImpl
    private lateinit var contactRepo: VendorContactRepositoryImpl
    private lateinit var addressRepo: VendorAddressRepositoryImpl
    private lateinit var capabilityRepo: VendorCapabilityRepositoryImpl

    private lateinit var vendorService: VendorServiceImpl
    private lateinit var profileService: VendorProfileServiceImpl
    private lateinit var contactService: VendorContactServiceImpl
    private lateinit var addressService: VendorAddressServiceImpl
    private lateinit var capabilityService: VendorCapabilityServiceImpl

    @Before
    fun setUp() {
        val fakeVendorDs = FakeVendorDataSource()
        val fakeProfileDs = FakeVendorProfileDataSource()
        val fakeContactDs = FakeVendorContactDataSource()
        val fakeAddressDs = FakeVendorAddressDataSource()
        val fakeCapDs = FakeVendorCapabilityDataSource()

        vendorRepo = VendorRepositoryImpl(fakeVendorDs)
        profileRepo = VendorProfileRepositoryImpl(fakeProfileDs)
        contactRepo = VendorContactRepositoryImpl(fakeContactDs)
        addressRepo = VendorAddressRepositoryImpl(fakeAddressDs)
        capabilityRepo = VendorCapabilityRepositoryImpl(fakeCapDs)

        vendorService = VendorServiceImpl(vendorRepo)
        profileService = VendorProfileServiceImpl(vendorRepo, profileRepo)
        contactService = VendorContactServiceImpl(vendorRepo, contactRepo)
        addressService = VendorAddressServiceImpl(vendorRepo, addressRepo)
        capabilityService = VendorCapabilityServiceImpl(vendorRepo, capabilityRepo)
    }

    @Test
    fun `cannot add capability or contact or address to archived vendor`() = runBlocking {
        // Create an archived vendor
        val vRes = vendorService.createVendor(
            projectId = "p_01",
            vendorName = "Old Press",
            vendorType = VendorType.MATERIAL_SUPPLIER,
            vendorCategory = VendorCategory.PAPER_SUPPLIER,
            createdBy = "admin"
        )
        assertTrue(vRes is DomainResult.Success)
        val vendor = (vRes as DomainResult.Success).data
        vendorService.updateStatus("p_01", vendor.vendorId, VendorStatus.ARCHIVED, "admin")

        // Try adding profile
        val profRes = profileService.updateProfile("p_01", vendor.vendorId, "New Name")
        assertTrue(profRes is DomainResult.Error)
        assertTrue((profRes as DomainResult.Error).message.contains("archived"))

        // Try adding contact
        val cntRes = contactService.createContact("p_01", vendor.vendorId, "Contact 1")
        assertTrue(cntRes is DomainResult.Error)
        assertTrue((cntRes as DomainResult.Error).message.contains("archived"))

        // Try adding address
        val addrRes = addressService.createAddress("p_01", vendor.vendorId, "Address 1")
        assertTrue(addrRes is DomainResult.Error)
        assertTrue((addrRes as DomainResult.Error).message.contains("archived"))

        // Try adding capability
        val capRes = capabilityService.createCapability("p_01", vendor.vendorId, CapabilityType.PRINTING)
        assertTrue(capRes is DomainResult.Error)
        assertTrue((capRes as DomainResult.Error).message.contains("archived"))
    }

    @Test
    fun `vendor profile creation and modification workflow`() = runBlocking {
        val vRes = vendorService.createVendor(
            projectId = "p_01",
            vendorName = "Karim Graphics",
            vendorType = VendorType.SERVICE_PROVIDER,
            vendorCategory = VendorCategory.CTP_PREPRESS
        )
        val vendorId = (vRes as DomainResult.Success).data.vendorId

        val updateRes = profileService.updateProfile(
            projectId = "p_01",
            vendorId = vendorId,
            displayName = "Karim CTP & Graphics",
            legalName = "Karim Graphics Ltd.",
            contactPerson = "Karim Mia",
            primaryPhone = "+8801700112233",
            email = "karim@graphics.com",
            website = "https://karimgraphics.com"
        )
        assertTrue(updateRes is DomainResult.Success)
        val profile = (updateRes as DomainResult.Success).data
        assertEquals("Karim CTP & Graphics", profile.displayName)
        assertEquals("Karim Mia", profile.contactPerson)

        val fetched = profileService.getProfile("p_01", vendorId)
        assertTrue(fetched is DomainResult.Success)
        assertEquals("karim@graphics.com", (fetched as DomainResult.Success).data.email)
    }

    @Test
    fun `capability registration, uniqueness and verification workflow`() = runBlocking {
        val vRes = vendorService.createVendor(
            projectId = "p_01",
            vendorName = "Apex Binding Works",
            vendorType = VendorType.SERVICE_PROVIDER,
            vendorCategory = VendorCategory.FINISHING
        )
        val vendorId = (vRes as DomainResult.Success).data.vendorId

        // Add PERFECT_BINDING capability
        val cap1 = capabilityService.createCapability(
            projectId = "p_01",
            vendorId = vendorId,
            capabilityType = CapabilityType.PERFECT_BINDING,
            displayName = "6-Clamp Perfect Binding Machine"
        )
        assertTrue(cap1 is DomainResult.Success)

        // Add HARD_BINDING capability
        val cap2 = capabilityService.createCapability(
            projectId = "p_01",
            vendorId = vendorId,
            capabilityType = CapabilityType.HARD_BINDING,
            displayName = "Case Making & Hard Cover Binding"
        )
        assertTrue(cap2 is DomainResult.Success)

        // Check hasCapability
        assertTrue(capabilityService.hasCapability("p_01", vendorId, CapabilityType.PERFECT_BINDING))
        assertTrue(capabilityService.hasCapability("p_01", vendorId, CapabilityType.HARD_BINDING))
        assertFalse(capabilityService.hasCapability("p_01", vendorId, CapabilityType.SPOT_UV))

        // Suspend capability
        val cap1Id = (cap1 as DomainResult.Success).data.capabilityId
        val suspendRes = capabilityService.updateStatus("p_01", cap1Id, CapabilityStatus.SUSPENDED, "admin")
        assertTrue(suspendRes is DomainResult.Success)

        // Now hasCapability should return false because it's suspended
        assertFalse(capabilityService.hasCapability("p_01", vendorId, CapabilityType.PERFECT_BINDING))

        // Attempting duplicate PERFECT_BINDING should fail
        val dupCap = capabilityService.createCapability(
            projectId = "p_01",
            vendorId = vendorId,
            capabilityType = CapabilityType.PERFECT_BINDING
        )
        assertTrue(dupCap is DomainResult.Error)
    }
}
