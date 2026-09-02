package com.sucharu.sucharupro.data.persistence.postgres

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Timestamp
import java.sql.Types

/**
 * Parameterized SQL execution abstraction (INFRA-01 Step 03).
 *
 * Guarantees zero SQL concatenation and type-safe parameter binding.
 */
class SqlExecutor(
    private val connection: Connection
) {

    private fun bindParameters(stmt: PreparedStatement, params: List<Any?>) {
        params.forEachIndexed { index, param ->
            val paramIndex = index + 1
            when (param) {
                null -> stmt.setNull(paramIndex, Types.NULL)
                is String -> stmt.setString(paramIndex, param)
                is Int -> stmt.setInt(paramIndex, param)
                is Long -> stmt.setLong(paramIndex, param)
                is Double -> stmt.setDouble(paramIndex, param)
                is Boolean -> stmt.setBoolean(paramIndex, param)
                is BigDecimal -> stmt.setBigDecimal(paramIndex, param)
                is Timestamp -> stmt.setTimestamp(paramIndex, param)
                is ByteArray -> stmt.setBytes(paramIndex, param)
                else -> stmt.setObject(paramIndex, param)
            }
        }
    }

    suspend fun <T> queryList(
        sql: String,
        params: List<Any?> = emptyList(),
        rowMapper: (ResultSet) -> T
    ): List<T> = withContext(Dispatchers.IO) {
        connection.prepareStatement(sql).use { stmt ->
            bindParameters(stmt, params)
            stmt.executeQuery().use { rs ->
                val resultList = mutableListOf<T>()
                while (rs.next()) {
                    resultList.add(rowMapper(rs))
                }
                resultList
            }
        }
    }

    suspend fun <T> querySingleOrNull(
        sql: String,
        params: List<Any?> = emptyList(),
        rowMapper: (ResultSet) -> T
    ): T? = withContext(Dispatchers.IO) {
        connection.prepareStatement(sql).use { stmt ->
            bindParameters(stmt, params)
            stmt.executeQuery().use { rs ->
                if (rs.next()) {
                    rowMapper(rs)
                } else {
                    null
                }
            }
        }
    }

    suspend fun executeUpdate(
        sql: String,
        params: List<Any?> = emptyList()
    ): Int = withContext(Dispatchers.IO) {
        connection.prepareStatement(sql).use { stmt ->
            bindParameters(stmt, params)
            stmt.executeUpdate()
        }
    }

    suspend fun executeBatch(
        sql: String,
        paramBatches: List<List<Any?>>
    ): IntArray = withContext(Dispatchers.IO) {
        if (paramBatches.isEmpty()) return@withContext intArrayOf()
        connection.prepareStatement(sql).use { stmt ->
            for (batch in paramBatches) {
                bindParameters(stmt, batch)
                stmt.addBatch()
            }
            stmt.executeBatch()
        }
    }
}
