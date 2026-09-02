package com.sucharu.sucharupro.data.persistence.postgres

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [PostgresConnectionConfig] and [TenantContext] (INFRA-01 Step 03).
 */
class PostgresConnectionConfigAndTenantTest {

    @Test
    fun `tenant context requires non-blank project ID`() {
        val tenant = TenantContext("PROJECT-101")
        assertEquals("PROJECT-101", tenant.projectId)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `tenant context rejects blank project ID`() {
        TenantContext("   ")
    }

    @Test
    fun `default connection config produces valid JDBC URL`() {
        val config = PostgresConnectionConfig(
            host = "db.internal",
            port = 5432,
            database = "sucharu_erp",
            user = "app_user",
            password = "secret_password"
        )
        val url = config.toJdbcUrl()
        assertTrue(url.contains("jdbc:postgresql://db.internal:5432/sucharu_erp"))
        assertEquals("app_user", config.user)
        assertEquals("secret_password", config.password)
    }

    @Test
    fun `fromUrl factory creates valid config`() {
        val config = PostgresConnectionConfig.fromUrl(
            jdbcUrl = "jdbc:postgresql://localhost:5432/testdb",
            user = "test_user",
            password = "test_password"
        )
        assertNotNull(config)
        assertEquals("test_user", config.user)
    }
}
