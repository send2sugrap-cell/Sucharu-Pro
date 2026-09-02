package com.sucharu.sucharupro.data.event.integration.realtime

import com.sucharu.sucharupro.domain.event.boundary.RealTimeEventFrame

/**
 * Active real-time client session (WebSocket or Server-Sent Events).
 */
data class RealTimeClientSession(
    val sessionId: String,
    val userId: String,
    val projectId: String,
    val connectedAt: Long = System.currentTimeMillis(),
    val subscribedTopics: MutableSet<String> = mutableSetOf()
)

/**
 * Real-time event transport sender interface (WebSocket / SSE adapter).
 */
interface RealTimeTransportSender {
    suspend fun sendFrame(session: RealTimeClientSession, frame: RealTimeEventFrame): Boolean
}
