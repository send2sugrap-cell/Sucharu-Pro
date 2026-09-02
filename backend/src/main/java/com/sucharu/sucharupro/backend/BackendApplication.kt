package com.sucharu.sucharupro.backend

import com.sucharu.sucharupro.backend.composition.ProductionBackendComposition
import com.sucharu.sucharupro.backend.config.BackendConfig
import com.sucharu.sucharupro.backend.server.HttpServerBootstrap
import org.slf4j.LoggerFactory
import java.util.concurrent.CountDownLatch
import kotlin.system.exitProcess

/**
 * Main entry point for the standalone Sucharu Pro JVM Backend Runtime (INFRA-05 Step 01).
 * Completely decoupled from Android runtime, hosting real PostgreSQL connection pooling,
 * Flyway migrations, authentication authority, event store, background workers, and HTTP health probes.
 */
class BackendRuntime(
    val config: BackendConfig = BackendConfig.fromEnvironment()
) {

    private val logger = LoggerFactory.getLogger(BackendRuntime::class.java)
    val composition = ProductionBackendComposition(config)
    val httpServer = HttpServerBootstrap(composition)
    private val shutdownLatch = CountDownLatch(1)

    fun start() {
        logger.info("Starting Sucharu Pro Standalone Backend Application...")
        try {
            // 1. Initialize production composition (DB pool, Flyway, auth authority, workers, API server)
            composition.start()

            // 2. Start HTTP health and operational endpoints
            httpServer.start()

            logger.info("Sucharu Pro Standalone Backend Application started successfully.")
        } catch (e: Exception) {
            logger.error("FATAL: Failed to start backend application", e)
            stop()
            throw e
        }
    }

    fun stop() {
        logger.info("Stopping Sucharu Pro Standalone Backend Application...")
        try {
            httpServer.stop()
            composition.stop()
        } catch (e: Exception) {
            logger.error("Error during backend stop sequence", e)
        } finally {
            shutdownLatch.countDown()
        }
    }

    fun awaitShutdown() {
        shutdownLatch.await()
    }
}

fun main(args: Array<String>) {
    val logger = LoggerFactory.getLogger("BackendMain")
    val config = BackendConfig.fromEnvironment()
    val runtime = BackendRuntime(config)

    // Register JVM graceful shutdown hook
    Runtime.getRuntime().addShutdownHook(Thread({
        logger.info("JVM Shutdown hook triggered. Initiating graceful shutdown...")
        runtime.stop()
    }, "backend-shutdown-hook"))

    try {
        runtime.start()
        logger.info("Backend runtime is actively running. Awaiting termination signal...")
        runtime.awaitShutdown()
    } catch (e: Exception) {
        logger.error("Application terminated due to startup error: {}", e.message)
        exitProcess(1)
    }
}
