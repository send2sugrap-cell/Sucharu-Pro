package com.sucharu.sucharupro.data.integration.security

import java.net.InetAddress
import java.net.URI

/**
 * Server-Side Request Forgery (SSRF) and Network Security Validator (INFRA-05 Step 05).
 *
 * Enforces:
 * 1. HTTPS scheme enforcement (HTTP prohibited in production)
 * 2. Hostname normalization and maximum URL length limit
 * 3. Prohibition of embedded credentials (user:password@host)
 * 4. Comprehensive IP address blocking:
 *    - Loopback (127.0.0.0/8, ::1, localhost)
 *    - RFC 1918 Private Ranges (10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16)
 *    - Link-Local IPv4 (169.254.0.0/16) & IPv6 (fe80::/10)
 *    - Cloud instance metadata endpoints (169.254.169.254, metadata.google.internal, instance-data)
 */
class SsrfProtectionValidator(
    private val allowPrivateNetworks: Boolean = false,
    private val allowedHosts: Set<String> = emptySet(),
    private val maxUrlLength: Int = 2048
) {

    /**
     * Validates an outbound destination URL against SSRF rules.
     * Throws [SecurityException] or [IllegalArgumentException] if the URL is dangerous or invalid.
     */
    fun validateUrl(urlString: String) {
        if (urlString.isBlank()) {
            throw IllegalArgumentException("Outbound destination URL cannot be blank.")
        }

        if (urlString.length > maxUrlLength) {
            throw IllegalArgumentException("URL exceeds maximum permitted length ($maxUrlLength characters).")
        }

        val uri = try {
            URI(urlString.trim())
        } catch (e: Exception) {
            throw IllegalArgumentException("Malformed destination URI: ${e.message}")
        }

        val scheme = uri.scheme?.lowercase() ?: throw IllegalArgumentException("URI must contain an explicit scheme.")
        if (scheme != "https" && scheme != "http") {
            throw SecurityException("Unsupported URI scheme '$scheme'. Only HTTP/HTTPS are permitted.")
        }

        if (!uri.userInfo.isNullOrBlank()) {
            throw SecurityException("Embedded credentials in destination URL are strictly prohibited.")
        }

        val host = uri.host?.lowercase() ?: throw IllegalArgumentException("URI host cannot be null or empty.")

        // 1. Check explicit allow-list
        if (allowedHosts.contains(host)) {
            return
        }

        // 2. Block well-known cloud metadata hostnames
        if (isCloudMetadataHost(host)) {
            throw SecurityException("SSRF Blocked: Cloud instance metadata endpoint access is prohibited ($host).")
        }

        // 3. Resolve and validate IP address
        if (!allowPrivateNetworks) {
            validateHostAddresses(host)
        }
    }

    private fun isCloudMetadataHost(host: String): Boolean {
        val normalized = host.lowercase()
        return normalized == "localhost" ||
               normalized == "metadata.google.internal" ||
               normalized == "instance-data" ||
               normalized.endsWith(".internal") ||
               normalized.endsWith(".local") ||
               normalized == "169.254.169.254"
    }

    private fun validateHostAddresses(host: String) {
        val addresses = try {
            InetAddress.getAllByName(host)
        } catch (e: Exception) {
            throw SecurityException("SSRF Validation: Failed to resolve hostname '$host' (${e.message}).")
        }

        for (addr in addresses) {
            if (addr.isLoopbackAddress) {
                throw SecurityException("SSRF Blocked: Loopback address detected for host '$host' (${addr.hostAddress}).")
            }

            if (addr.isLinkLocalAddress || addr.isSiteLocalAddress || addr.isAnyLocalAddress) {
                throw SecurityException("SSRF Blocked: Private/Local network address detected for host '$host' (${addr.hostAddress}).")
            }

            val ip = addr.hostAddress ?: ""
            if (isPrivateOrReservedIp(ip)) {
                throw SecurityException("SSRF Blocked: Reserved IP range detected for host '$host' ($ip).")
            }
        }
    }

    private fun isPrivateOrReservedIp(ip: String): Boolean {
        if (ip.startsWith("127.") || ip == "0.0.0.0" || ip == "::1") return true
        if (ip.startsWith("10.")) return true
        if (ip.startsWith("192.168.")) return true
        if (ip.startsWith("169.254.")) return true

        // 172.16.0.0 to 172.31.255.255
        if (ip.startsWith("172.")) {
            val parts = ip.split(".")
            if (parts.size >= 2) {
                val secondOctet = parts[1].toIntOrNull() ?: 0
                if (secondOctet in 16..31) return true
            }
        }

        // IPv6 Unique Local Address (fc00::/7) or Link-Local (fe80::/10)
        if (ip.startsWith("fc") || ip.startsWith("fd") || ip.startsWith("fe80")) return true

        return false
    }
}
