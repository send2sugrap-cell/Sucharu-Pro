package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.datasource.machine.MachineRegistryDataSource
import com.sucharu.sucharupro.domain.machine.MachineEquipment
import com.sucharu.sucharupro.domain.machine.MachineOwnershipType
import com.sucharu.sucharupro.domain.machine.MachineStatus
import com.sucharu.sucharupro.domain.machine.MachineType
import com.sucharu.sucharupro.domain.model.common.DomainResult
import java.sql.ResultSet

/**
 * PostgreSQL implementation of MachineRegistryDataSource using TransactionManager & RLS.
 */
class PostgresMachineRegistryDataSource(
    private val transactionManager: TransactionManager
) : MachineRegistryDataSource {

    override suspend fun saveMachine(machine: MachineEquipment): DomainResult<MachineEquipment> {
        return try {
            transactionManager.inTransaction(TenantContext(machine.tenantId)) { ctx ->
                val conn = ctx.connection
                val sql = """
                    INSERT INTO machine_registry (
                        machine_id, tenant_id, asset_code, name, machine_type,
                        category, manufacturer, model, serial_number, description,
                        status, is_active, ownership_type, location_reference, department,
                        configuration_metadata, created_at, updated_at, created_by, updated_by
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, to_timestamp(? / 1000.0), to_timestamp(? / 1000.0), ?, ?)
                    ON CONFLICT (machine_id) DO UPDATE SET
                        asset_code = EXCLUDED.asset_code,
                        name = EXCLUDED.name,
                        machine_type = EXCLUDED.machine_type,
                        category = EXCLUDED.category,
                        manufacturer = EXCLUDED.manufacturer,
                        model = EXCLUDED.model,
                        serial_number = EXCLUDED.serial_number,
                        description = EXCLUDED.description,
                        status = EXCLUDED.status,
                        is_active = EXCLUDED.is_active,
                        ownership_type = EXCLUDED.ownership_type,
                        location_reference = EXCLUDED.location_reference,
                        department = EXCLUDED.department,
                        configuration_metadata = EXCLUDED.configuration_metadata,
                        updated_at = EXCLUDED.updated_at,
                        updated_by = EXCLUDED.updated_by
                """.trimIndent()

                conn.prepareStatement(sql).use { ps ->
                    ps.setString(1, machine.machineId)
                    ps.setString(2, machine.tenantId)
                    ps.setString(3, machine.assetCode)
                    ps.setString(4, machine.name)
                    ps.setString(5, machine.type.name)
                    ps.setString(6, machine.category)
                    ps.setString(7, machine.manufacturer)
                    ps.setString(8, machine.model)
                    ps.setString(9, machine.serialNumber)
                    ps.setString(10, machine.description)
                    ps.setString(11, machine.status.name)
                    ps.setBoolean(12, machine.isActive)
                    ps.setString(13, machine.ownershipType.name)
                    ps.setString(14, machine.locationReference)
                    ps.setString(15, machine.department)
                    ps.setString(16, machine.configurationMetadata)
                    ps.setLong(17, machine.createdAt)
                    ps.setLong(18, machine.updatedAt)
                    ps.setString(19, machine.createdBy)
                    ps.setString(20, machine.updatedBy)
                    ps.executeUpdate()
                }
            }
            DomainResult.Success(machine)
        } catch (e: Exception) {
            PostgresErrorTranslator.translate(e, "save machine")
        }
    }

    override suspend fun getMachineById(tenantId: String, machineId: String): DomainResult<MachineEquipment?> {
        return try {
            val result = transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
                val conn = ctx.connection
                val sql = "SELECT * FROM machine_registry WHERE tenant_id = ? AND machine_id = ?"
                conn.prepareStatement(sql).use { ps ->
                    ps.setString(1, tenantId)
                    ps.setString(2, machineId)
                    ps.executeQuery().use { rs ->
                        if (rs.next()) mapMachine(rs) else null
                    }
                }
            }
            DomainResult.Success(result)
        } catch (e: Exception) {
            PostgresErrorTranslator.translate(e, "get machine by id")
        }
    }

    override suspend fun getMachineByAssetCode(tenantId: String, assetCode: String): DomainResult<MachineEquipment?> {
        return try {
            val result = transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
                val conn = ctx.connection
                val sql = "SELECT * FROM machine_registry WHERE tenant_id = ? AND LOWER(asset_code) = LOWER(?)"
                conn.prepareStatement(sql).use { ps ->
                    ps.setString(1, tenantId)
                    ps.setString(2, assetCode)
                    ps.executeQuery().use { rs ->
                        if (rs.next()) mapMachine(rs) else null
                    }
                }
            }
            DomainResult.Success(result)
        } catch (e: Exception) {
            PostgresErrorTranslator.translate(e, "get machine by asset code")
        }
    }

    override suspend fun listMachines(
        tenantId: String,
        type: MachineType?,
        status: MachineStatus?
    ): DomainResult<List<MachineEquipment>> {
        return try {
            val result = transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
                val conn = ctx.connection
                val sql = StringBuilder("SELECT * FROM machine_registry WHERE tenant_id = ?")
                if (type != null) sql.append(" AND machine_type = ?")
                if (status != null) sql.append(" AND status = ?")
                sql.append(" ORDER BY name ASC")

                conn.prepareStatement(sql.toString()).use { ps ->
                    var idx = 1
                    ps.setString(idx++, tenantId)
                    if (type != null) ps.setString(idx++, type.name)
                    if (status != null) ps.setString(idx, status.name)

                    ps.executeQuery().use { rs ->
                        val list = mutableListOf<MachineEquipment>()
                        while (rs.next()) {
                            list.add(mapMachine(rs))
                        }
                        list
                    }
                }
            }
            DomainResult.Success(result)
        } catch (e: Exception) {
            PostgresErrorTranslator.translate(e, "list machines")
        }
    }

    override suspend fun updateMachineStatus(
        tenantId: String,
        machineId: String,
        status: MachineStatus,
        updatedBy: String?
    ): DomainResult<MachineEquipment> {
        return try {
            val result = transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
                val conn = ctx.connection
                val sql = """
                    UPDATE machine_registry SET
                        status = ?,
                        updated_at = CURRENT_TIMESTAMP,
                        updated_by = ?
                    WHERE tenant_id = ? AND machine_id = ?
                    RETURNING *
                """.trimIndent()

                conn.prepareStatement(sql).use { ps ->
                    ps.setString(1, status.name)
                    ps.setString(2, updatedBy)
                    ps.setString(3, tenantId)
                    ps.setString(4, machineId)
                    ps.executeQuery().use { rs ->
                        if (rs.next()) mapMachine(rs) else null
                    }
                }
            }
            if (result != null) {
                DomainResult.Success(result)
            } else {
                DomainResult.Error(message = "Machine not found: $machineId")
            }
        } catch (e: Exception) {
            PostgresErrorTranslator.translate(e, "update machine status")
        }
    }

    private fun mapMachine(rs: ResultSet): MachineEquipment {
        val createdAtTs = rs.getTimestamp("created_at")
        val updatedAtTs = rs.getTimestamp("updated_at")
        return MachineEquipment(
            machineId = rs.getString("machine_id"),
            tenantId = rs.getString("tenant_id"),
            assetCode = rs.getString("asset_code"),
            name = rs.getString("name"),
            type = try { MachineType.valueOf(rs.getString("machine_type")) } catch (_: Exception) { MachineType.OTHER },
            category = rs.getString("category") ?: "PRODUCTION",
            manufacturer = rs.getString("manufacturer"),
            model = rs.getString("model"),
            serialNumber = rs.getString("serial_number"),
            description = rs.getString("description"),
            status = try { MachineStatus.valueOf(rs.getString("status")) } catch (_: Exception) { MachineStatus.AVAILABLE },
            isActive = rs.getBoolean("is_active"),
            ownershipType = try { MachineOwnershipType.valueOf(rs.getString("ownership_type")) } catch (_: Exception) { MachineOwnershipType.COMPANY_OWNED },
            locationReference = rs.getString("location_reference"),
            department = rs.getString("department"),
            configurationMetadata = rs.getString("configuration_metadata"),
            createdAt = createdAtTs?.time ?: System.currentTimeMillis(),
            updatedAt = updatedAtTs?.time ?: System.currentTimeMillis(),
            createdBy = rs.getString("created_by"),
            updatedBy = rs.getString("updated_by")
        )
    }
}
