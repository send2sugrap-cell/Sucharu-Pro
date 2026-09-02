package com.sucharu.sucharupro.backend.server

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import com.sucharu.sucharupro.backend.composition.ProductionBackendComposition
import com.sucharu.sucharupro.data.api.model.ApiErrorResponse
import com.sucharu.sucharupro.data.api.model.ApiSuccessResponse
import com.sucharu.sucharupro.data.api.server.HttpRequest
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import java.io.OutputStream
import java.net.InetSocketAddress
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Lightweight HTTP server bootstrap for standalone backend health, readiness, and REST API endpoints.
 * Provides the network foundation for INFRA-05 Step 01, Step 02, and Step 03 runtime lifecycle.
 */
class HttpServerBootstrap(
    private val composition: ProductionBackendComposition
) {

    private val logger = LoggerFactory.getLogger(HttpServerBootstrap::class.java)
    private var server: HttpServer? = null
    private val isRunning = AtomicBoolean(false)
    private val executor = Executors.newFixedThreadPool(4)

    fun start() {
        if (isRunning.compareAndSet(false, true)) {
            val config = composition.config
            val address = InetSocketAddress(config.serverHost, config.serverPort)
            val httpServer = HttpServer.create(address, 0)

            httpServer.createContext("/health", HealthHandler(composition))
            httpServer.createContext("/health/live", LivenessHandler(composition))
            httpServer.createContext("/health/ready", ReadinessHandler(composition))
            httpServer.createContext("/health/readiness", ReadinessHandler(composition))
            httpServer.createContext("/ready", ReadinessHandler(composition))
            httpServer.createContext("/metrics", MetricsHandler(composition))
            httpServer.createContext("/api", ApiHandler(composition))
            httpServer.createContext("/", RootHandler(composition))

            httpServer.executor = executor
            httpServer.start()
            server = httpServer

            logger.info("HTTP Server bootstrap listening on http://{}:{}", config.serverHost, config.serverPort)
            logger.info("Endpoints active: /health, /health/live, /ready, /metrics, /api/v1/*")
        }
    }

    fun stop() {
        if (isRunning.compareAndSet(true, false)) {
            val graceSec = (composition.config.gracefulShutdownTimeoutMs / 1000).coerceIn(1L, 10L).toInt()
            logger.info("Stopping HTTP server bootstrap with {}s grace period...", graceSec)
            server?.stop(graceSec)
            executor.shutdown()
            logger.info("HTTP server bootstrap stopped.")
        }
    }

    private class HealthHandler(private val composition: ProductionBackendComposition) : HttpHandler {
        override fun handle(exchange: HttpExchange) {
            val report = composition.healthTracker.getHealthReport()
            val isReady = composition.healthTracker.isReady()
            val statusCode = if (isReady) 200 else 503

            val json = buildJsonString(report)
            sendJsonResponse(exchange, statusCode, json)
        }
    }

    private class LivenessHandler(private val composition: ProductionBackendComposition) : HttpHandler {
        override fun handle(exchange: HttpExchange) {
            val isLive = composition.healthTracker.isLive()
            val statusCode = if (isLive) 200 else 503
            val json = """{"status":"${if (isLive) "UP" else "DOWN"}","live":$isLive}"""
            sendJsonResponse(exchange, statusCode, json)
        }
    }

    private class ReadinessHandler(private val composition: ProductionBackendComposition) : HttpHandler {
        override fun handle(exchange: HttpExchange) {
            val isReady = composition.healthTracker.isReady()
            val statusCode = if (isReady) 200 else 503
            val report = composition.healthTracker.getHealthReport()
            val json = buildJsonString(report)
            sendJsonResponse(exchange, statusCode, json)
        }
    }

    private class MetricsHandler(private val composition: ProductionBackendComposition) : HttpHandler {
        override fun handle(exchange: HttpExchange) {
            val output = composition.metricsRegistry.formatPrometheus()
            val bytes = output.toByteArray(Charsets.UTF_8)
            exchange.responseHeaders.set("Content-Type", "text/plain; version=0.0.4; charset=utf-8")
            exchange.responseHeaders.set("Server", "Sucharu-Backend-Runtime/1.0")
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            val os: OutputStream = exchange.responseBody
            os.write(bytes)
            os.close()
        }
    }

    private class ApiHandler(private val composition: ProductionBackendComposition) : HttpHandler {
        override fun handle(exchange: HttpExchange) {
            val method = exchange.requestMethod
            val path = exchange.requestURI.path
            val headers = mutableMapOf<String, String>()
            exchange.requestHeaders.forEach { (k, v) ->
                if (v.isNotEmpty()) headers[k] = v[0]
            }

            val bodyBytes = exchange.requestBody.readBytes()
            val bodyString = if (bodyBytes.isNotEmpty()) String(bodyBytes, Charsets.UTF_8) else null

            val httpRequest = HttpRequest(
                method = method,
                path = path,
                headers = headers,
                body = bodyString,
                clientIp = exchange.remoteAddress?.address?.hostAddress ?: "127.0.0.1"
            )

            val httpResponse = runBlocking {
                composition.apiServer.handle(httpRequest)
            }

            val responseJson = formatResponseJson(httpResponse.body)

            exchange.responseHeaders.set("X-Correlation-ID", httpResponse.correlationId)
            sendJsonResponse(exchange, httpResponse.statusCode, responseJson)
        }
    }

    private class RootHandler(private val composition: ProductionBackendComposition) : HttpHandler {
        override fun handle(exchange: HttpExchange) {
            val rel = composition.config.getReleaseMetadata()
            val json = """
                {
                    "application": "${rel.appName}",
                    "version": "${rel.appVersion}",
                    "buildVersion": "${rel.buildVersion}",
                    "gitRevision": "${rel.gitRevision}",
                    "environment": "${rel.environment}",
                    "buildTimestamp": "${rel.buildTimestamp}",
                    "status": "RUNNING",
                    "health": "/health",
                    "readiness": "/ready",
                    "metrics": "/metrics",
                    "api": "/api/v1"
                }
            """.trimIndent()
            sendJsonResponse(exchange, 200, json)
        }
    }

    companion object {
        private fun sendJsonResponse(exchange: HttpExchange, statusCode: Int, json: String) {
            val bytes = json.toByteArray(Charsets.UTF_8)
            exchange.responseHeaders.set("Content-Type", "application/json; charset=utf-8")
            exchange.responseHeaders.set("Server", "Sucharu-Backend-Runtime/1.0")
            exchange.sendResponseHeaders(statusCode, bytes.size.toLong())
            val os: OutputStream = exchange.responseBody
            os.write(bytes)
            os.close()
        }

        fun formatResponseJson(body: Any): String {
            return when (body) {
                is String -> body
                is Map<*, *> -> @Suppress("UNCHECKED_CAST") buildJsonString(body as Map<String, Any>)
                is ApiSuccessResponse<*> -> {
                    val dataObj = body.data
                    val dataJson = when (dataObj) {
                        null -> "null"
                        is Map<*, *> -> @Suppress("UNCHECKED_CAST") buildJsonString(dataObj as Map<String, Any>)
                        is String -> "\"${dataObj.replace("\"", "\\\"")}\""
                        is Number -> dataObj.toString()
                        is Boolean -> dataObj.toString()
                        else -> {
                            val map = convertToMap(dataObj)
                            buildJsonString(map)
                        }
                    }
                    """{"success":true,"data":$dataJson,"correlationId":"${body.correlationId}"}"""
                }
                is ApiErrorResponse -> {
                    val code = body.errorCode.name
                    val msg = body.message.replace("\"", "\\\"")
                    """{"success":false,"errorCode":"$code","message":"$msg","correlationId":"${body.correlationId}"}"""
                }
                else -> {
                    val map = convertToMap(body)
                    buildJsonString(map)
                }
            }
        }

        private fun convertToMap(obj: Any): Map<String, Any> {
            val map = mutableMapOf<String, Any>()
            val fields = obj.javaClass.declaredFields
            for (f in fields) {
                if (java.lang.reflect.Modifier.isStatic(f.modifiers)) continue
                try {
                    f.isAccessible = true
                    val v = f.get(obj)
                    if (v != null) {
                        map[f.name] = v
                    }
                } catch (_: Exception) {}
            }
            return map
        }

        private fun buildJsonString(map: Map<String, Any>): String {
            val sb = StringBuilder("{")
            val entries = map.entries.toList()
            for (i in entries.indices) {
                val (k, v) = entries[i]
                sb.append("\"").append(k).append("\":")
                appendValue(sb, v)
                if (i < entries.size - 1) sb.append(",")
            }
            sb.append("}")
            return sb.toString()
        }

        @Suppress("UNCHECKED_CAST")
        private fun appendValue(sb: StringBuilder, v: Any?) {
            when (v) {
                null -> sb.append("null")
                is Number -> sb.append(v)
                is Boolean -> sb.append(v)
                is String -> sb.append("\"").append(v.replace("\"", "\\\"")).append("\"")
                is Enum<*> -> sb.append("\"").append(v.name).append("\"")
                is Map<*, *> -> sb.append(buildJsonString(v as Map<String, Any>))
                is Collection<*> -> {
                    sb.append("[")
                    val list = v.toList()
                    for (i in list.indices) {
                        appendValue(sb, list[i])
                        if (i < list.size - 1) sb.append(",")
                    }
                    sb.append("]")
                }
                else -> {
                    val map = convertToMap(v)
                    if (map.isNotEmpty()) {
                        sb.append(buildJsonString(map))
                    } else {
                        sb.append("\"").append(v.toString()).append("\"")
                    }
                }
            }
        }
    }
}
