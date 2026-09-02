package com.sucharu.sucharupro.data.api.client

/**
 * Secure token storage abstraction for client applications (INFRA-02 Step 04).
 */
interface AuthTokenStorage {
    fun saveToken(token: String)
    fun getToken(): String?
    fun clearToken()
}

/**
 * In-memory token storage implementation for testing and client runtime.
 */
class InMemoryAuthTokenStorage : AuthTokenStorage {
    private var token: String? = null

    override fun saveToken(token: String) {
        this.token = token
    }

    override fun getToken(): String? {
        return token
    }

    override fun clearToken() {
        token = null
    }
}
