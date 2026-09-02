package com.sucharu.sucharupro.data.event.integration.realtime

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Thread-safe registry managing real-time topic subscriptions with multi-tenant isolation (INFRA-04 Step 03).
 */
class RealTimeSubscriptionRegistry {

    // Keyed by topic string
    private val sessionsByTopic = ConcurrentHashMap<String, CopyOnWriteArrayList<RealTimeClientSession>>()
    // Keyed by sessionId
    private val activeSessions = ConcurrentHashMap<String, RealTimeClientSession>()

    /**
     * Registers an active client connection session.
     */
    fun registerSession(session: RealTimeClientSession) {
        activeSessions[session.sessionId] = session
    }

    /**
     * Unregisters a client session upon disconnect.
     */
    fun unregisterSession(sessionId: String) {
        val session = activeSessions.remove(sessionId) ?: return
        for (topic in session.subscribedTopics) {
            sessionsByTopic[topic]?.remove(session)
        }
    }

    /**
     * Subscribes a client session to a topic with strict tenant security validation.
     */
    fun subscribe(sessionId: String, topic: String): Boolean {
        val session = activeSessions[sessionId] ?: return false

        // Validate topic starts with tenant.<projectId>
        val expectedPrefix = "tenant.${session.projectId}."
        if (!topic.startsWith(expectedPrefix)) {
            // Cross-tenant subscription attempt strictly denied
            return false
        }

        // Block security or auth internals from real-time streaming
        if (topic.contains(".auth.") || topic.contains(".security.") || topic.contains(".session.")) {
            return false
        }

        session.subscribedTopics.add(topic)
        sessionsByTopic.computeIfAbsent(topic) { CopyOnWriteArrayList() }.add(session)
        return true
    }

    /**
     * Unsubscribes a client session from a topic.
     */
    fun unsubscribe(sessionId: String, topic: String) {
        val session = activeSessions[sessionId] ?: return
        session.subscribedTopics.remove(topic)
        sessionsByTopic[topic]?.remove(session)
    }

    /**
     * Returns all authorized active sessions subscribed to a given topic.
     */
    fun getSubscribersForTopic(topic: String): List<RealTimeClientSession> {
        return sessionsByTopic[topic]?.toList() ?: emptyList()
    }

    /**
     * Clears all sessions (for testing).
     */
    fun clear() {
        sessionsByTopic.clear()
        activeSessions.clear()
    }
}
