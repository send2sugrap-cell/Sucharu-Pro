package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.domain.model.common.Money
import java.math.BigDecimal
import java.sql.ResultSet
import java.sql.Timestamp

/**
 * Type-safe Row Mapper helpers for PostgreSQL JDBC ResultSets (INFRA-01 Step 03).
 */
object RowMappers {

    fun ResultSet.getMoney(columnName: String): Money {
        val bd = getBigDecimal(columnName) ?: BigDecimal.ZERO
        return Money(bd)
    }

    fun ResultSet.getTimestampMillis(columnName: String): Long {
        val ts = getTimestamp(columnName)
        return ts?.time ?: 0L
    }

    fun ResultSet.getNullableTimestampMillis(columnName: String): Long? {
        val ts = getTimestamp(columnName)
        return ts?.time
    }

    inline fun <reified E : Enum<E>> ResultSet.getEnumByName(columnName: String, default: E): E {
        val str = getString(columnName) ?: return default
        return try {
            java.lang.Enum.valueOf(E::class.java, str)
        } catch (_: IllegalArgumentException) {
            default
        }
    }

    inline fun <reified E : Enum<E>> ResultSet.getNullableEnumByName(columnName: String): E? {
        val str = getString(columnName) ?: return null
        return try {
            java.lang.Enum.valueOf(E::class.java, str)
        } catch (_: IllegalArgumentException) {
            null
        }
    }
}
