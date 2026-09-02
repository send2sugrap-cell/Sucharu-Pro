package com.sucharu.sucharupro.data.integration.client

import com.sucharu.sucharupro.data.integration.model.IntegrationRequest
import com.sucharu.sucharupro.data.integration.model.IntegrationResponse
import com.sucharu.sucharupro.data.integration.security.SsrfProtectionValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URI

/**
 * Interface for executing outbound HTTP requests to external provider APIs (INFRA-05 Step 05).
 */
interface IntegrationHttpClient {
    suspend fun execute(request: IntegrationRequest): IntegrationResponse
}

/**
 * Production-grade JVM implementation of [IntegrationHttpClient].
 * Enforces SSRF validation, connection/read timeouts, maximum response body size, and TLS verification.
 */
class DefaultIntegrationHttpClient(
    private val ssrfValidator: SsrfProtectionValidator = SsrfProtectionValidator(),
    private val defaultConnectTimeoutMs: Int = 5000,
    private val defaultReadTimeoutMs: Int = 10000,
    private val maxResponseBodyBytes: Int = 2 * 1024 * 1024 // 2MB limit
) : IntegrationHttpClient {

    override suspend fun execute(request: IntegrationRequest): IntegrationResponse = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()

        // 1. SSRF Validation
        try {
            ssrfValidator.validateUrl(request.url)
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            return@withContext IntegrationResponse(
                statusCode = 0,
                durationMs = duration,
                isSuccess = false,
                sanitizedError = "SSRF Policy Rejection: ${e.message}"
            )
        }

        var connection: HttpURLConnection? = null
        try {
            val uri = URI(request.url.trim())
            val url = uri.toURL()

            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = request.method.uppercase()
                connectTimeout = request.timeoutMs.toInt().coerceAtMost(defaultConnectTimeoutMs)
                readTimeout = request.timeoutMs.toInt().coerceAtMost(defaultReadTimeoutMs)
                instanceFollowRedirects = false // Prevent blind internal redirects
                useCaches = false
                doInput = true

                // Apply headers
                request.headers.forEach { (k, v) ->
                    setRequestProperty(k, v)
                }
                if (!request.correlationId.isNullOrBlank()) {
                    setRequestProperty("X-Correlation-ID", request.correlationId)
                }

                if (!request.body.isNullOrBlank()) {
                    doOutput = true
                    if (request.headers.none { it.key.equals("Content-Type", ignoreCase = true) }) {
                        setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    }
                }
            }

            // Write body if present
            if (!request.body.isNullOrBlank()) {
                connection.outputStream.use { os ->
                    os.write(request.body.toByteArray(Charsets.UTF_8))
                    os.flush()
                }
            }

            val statusCode = connection.responseCode
            val responseHeaders = mutableMapOf<String, String>()
            connection.headerFields?.forEach { (k, vList) ->
                if (k != null && vList.isNotEmpty()) {
                    responseHeaders[k] = vList.joinToString(", ")
                }
            }

            val stream: InputStream? = if (statusCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream ?: connection.inputStream
            }

            val body = stream?.let { readBoundedStream(it, maxResponseBodyBytes) }
            val duration = System.currentTimeMillis() - startTime

            IntegrationResponse(
                statusCode = statusCode,
                headers = responseHeaders,
                body = body,
                durationMs = duration,
                isSuccess = statusCode in 200..299,
                sanitizedError = if (statusCode !in 200..299) "HTTP Error $statusCode" else null
            )
        } catch (t: Throwable) {
            val duration = System.currentTimeMillis() - startTime
            IntegrationResponse(
                statusCode = 0,
                durationMs = duration,
                isSuccess = false,
                sanitizedError = "Network/Transport Failure: ${t.javaClass.simpleName} - ${t.message}"
            )
        } finally {
            connection?.disconnect()
        }
    }

    private fun readBoundedStream(inputStream: InputStream, maxBytes: Int): String {
        return inputStream.use { stream ->
            val buffer = ByteArray(4096)
            val output = ByteArrayOutputStream()
            var totalRead = 0
            var bytesRead: Int

            while (stream.read(buffer).also { bytesRead = it } != -1) {
                totalRead += bytesRead
                if (totalRead > maxBytes) {
                    throw IllegalStateException("Response body exceeded maximum allowed limit of $maxBytes bytes.")
                }
                output.write(buffer, 0, bytesRead)
            }
            output.toString(Charsets.UTF_8.name())
        }
    }
}
