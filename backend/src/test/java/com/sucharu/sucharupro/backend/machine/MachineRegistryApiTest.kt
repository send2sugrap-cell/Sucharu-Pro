package com.sucharu.sucharupro.backend.machine

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.ForbiddenException
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.api.model.machine.CreateMachineRequestDto
import com.sucharu.sucharupro.data.api.model.machine.UpdateMachineStatusRequestDto
import com.sucharu.sucharupro.data.api.server.*
import com.sucharu.sucharupro.data.datasource.machine.FakeMachineRegistryDataSource
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.data.persistence.postgres.TransactionContext
import com.sucharu.sucharupro.data.persistence.postgres.TransactionManager
import com.sucharu.sucharupro.data.repository.machine.MachineRegistryRepositoryImpl
import com.sucharu.sucharupro.domain.machine.MachineOwnershipType
import com.sucharu.sucharupro.domain.machine.MachineStatus
import com.sucharu.sucharupro.domain.machine.MachineType
import com.sucharu.sucharupro.domain.repository.machine.MachineRegistryRepository
import com.sucharu.sucharupro.domain.service.machine.MachineRegistryService
import com.sucharu.sucharupro.domain.service.machine.MachineRegistryServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class MachineRegistryApiTest {

    private lateinit var useCases: BackendUseCases
    private lateinit var customFactory: PostgresRepositoryFactory

    private val projectId = "TENANT-001"

    private val staffPrincipal = AuthenticatedPrincipal(
        userId = "staff_01",
        projectId = projectId,
        username = "staff_user",
        role = UserRole.STAFF
    )

    private val customerPrincipal = AuthenticatedPrincipal(
        userId = "customer_01",
        projectId = projectId,
        username = "customer_user",
        role = UserRole.CUSTOMER
    )

    @Before
    fun setup() {
        val machineDs = FakeMachineRegistryDataSource()
        val machineRepo = MachineRegistryRepositoryImpl(machineDs)
        val machineService = MachineRegistryServiceImpl(machineRepo)

        val fakeTxManager = object : TransactionManager {
            override suspend fun <T> inTransaction(tenantContext: TenantContext, block: suspend (TransactionContext) -> T): T {
                throw UnsupportedOperationException("Not required for mock tests")
            }
            override suspend fun <T> inReadOnly(tenantContext: TenantContext, block: suspend (TransactionContext) -> T): T {
                throw UnsupportedOperationException("Not required for mock tests")
            }
        }

        customFactory = object : PostgresRepositoryFactory(fakeTxManager) {
            override fun createMachineRegistryRepository(tenantId: String): MachineRegistryRepository = machineRepo
            override fun createMachineRegistryService(tenantId: String): MachineRegistryService = machineService
        }

        useCases = BackendUseCases(fakeTxManager, customFactory)
    }

    @Test
    fun test01_registerMachine_staff_success() = runBlocking {
        val req = CreateMachineRequestDto(
            assetCode = "EQ-BIND-01",
            name = "Muller Martini Perfect Binder",
            type = MachineType.BINDING,
            manufacturer = "Muller Martini",
            model = "Acoro A7",
            status = MachineStatus.AVAILABLE,
            ownershipType = MachineOwnershipType.COMPANY_OWNED,
            locationReference = "Floor B - Binding Bay",
            department = "BINDING"
        )

        val response = useCases.registerMachine(staffPrincipal, req, customFactory)
        assertNotNull(response.machineId)
        assertEquals("EQ-BIND-01", response.assetCode)
        assertEquals("Muller Martini Perfect Binder", response.name)
        assertEquals(MachineType.BINDING, response.type)
    }

    @Test
    fun test02_registerMachine_customerRole_forbidden() = runBlocking {
        val req = CreateMachineRequestDto(
            assetCode = "EQ-ILLEGAL-01",
            name = "Unauthorized Press Registration",
            type = MachineType.PRINTING_PRESS
        )

        try {
            useCases.registerMachine(customerPrincipal, req, customFactory)
            fail("Expected ForbiddenException for customer registering machine.")
        } catch (e: ForbiddenException) {
            assertTrue(e.message?.contains("Forbidden") == true || e.message?.contains("Access") == true)
        }
    }

    @Test
    fun test03_listMachines_success() = runBlocking {
        val listResponse = useCases.listMachines(staffPrincipal, repositoryFactory = customFactory)
        assertNotNull(listResponse)
        assertTrue(listResponse.machines.isNotEmpty())
    }

    @Test
    fun test04_updateMachineStatus_success() = runBlocking {
        val list = useCases.listMachines(staffPrincipal, repositoryFactory = customFactory)
        val firstMachine = list.machines.first()

        val statusReq = UpdateMachineStatusRequestDto(status = MachineStatus.MAINTENANCE)
        val updated = useCases.updateMachineStatus(staffPrincipal, firstMachine.machineId, statusReq, customFactory)

        assertEquals(MachineStatus.MAINTENANCE, updated.status)
    }
}
