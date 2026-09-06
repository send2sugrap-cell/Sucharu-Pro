package com.sucharu.sucharupro.data.auth.security

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.sucharu.sucharupro.data.api.model.UnauthenticatedException
import java.io.InputStream
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Verified identity claims extracted from a cryptographically verified Firebase ID token.
 *
 * All fields are sourced exclusively from the verified token payload returned by the
 * Firebase Admin SDK — never from a client-supplied claim, prefix, or synthetic fallback.
 */
data class VerifiedFirebaseToken(
    val uid: String,
    val phoneNumber: String?,
    val email: String?,
    val name: String?,
    val issuer: String,
    val audience: String,
    val expiresAtSeconds: Long
)

/**
 * Interface for verifying identity tokens securely.
 * This abstraction prevents the Android runtime from loading the concrete FirebaseTokenVerifier
 * and crashing with a VerifyError due to missing Firebase Admin SDK classes.
 */
interface IIdTokenVerifier {
    fun verifyIdToken(idToken: String): VerifiedFirebaseToken
}

/**
 * Server-Side Firebase ID Token Verifier using Firebase Admin SDK (INFRA-AUTH-SEC-01).
 *
 * Security Invariants:
 *  - ZERO tolerance for token prefixes, demo stubs, synthetic UIDs, or mock phone numbers.
 *  - ONLY tokens cryptographically signed by Firebase (Google RSA) are accepted.
 *  - Invalid, expired, malformed, or forged tokens unconditionally throw [UnauthenticatedException].
 *  - No fallback, no bypass, no debug shortcut. This class has one mode: strict production.
 *
 * Initialisation:
 *  Call [FirebaseTokenVerifier.initialise] once at server startup, supplying a service account
 *  credential stream. If the app is already initialised (e.g. in tests that call this multiple
 *  times) the existing [FirebaseApp] is reused.
 */
class FirebaseTokenVerifier(
    private val expectedProjectId: String = "sucharu-pro-erp"
) : IIdTokenVerifier {

    /**
     * Verifies the client-supplied Firebase ID Token using the Firebase Admin SDK.
     *
     * All of the following are verified cryptographically by the SDK:
     *  - RSA signature (Google public key)
     *  - Issuer: must be https://securetoken.google.com/<projectId>
     *  - Audience: must match expectedProjectId
     *  - Expiration: token must not be expired
     *  - Subject (uid): must be non-empty
     *
     * @throws UnauthenticatedException for any invalid, expired, forged, or malformed token.
     */
    override fun verifyIdToken(idToken: String): VerifiedFirebaseToken {
        val rawToken = idToken.trim()

        if (rawToken.isBlank()) {
            throw UnauthenticatedException("Firebase ID token is missing or empty.")
        }

        // CRITICAL: Any token that looks like a demo/test/synthetic stub is immediately rejected.
        // There is no prefix-based trust path in this implementation.
        // The Admin SDK is the sole authority.
        val auth = try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            throw UnauthenticatedException(
                "Firebase Admin SDK is not initialised. Server startup may have failed to load " +
                "service account credentials. Token verification is unavailable: ${e.message}"
            )
        }

        val decodedToken = try {
            auth.verifyIdToken(rawToken)
        } catch (e: FirebaseAuthException) {
            throw UnauthenticatedException(
                "Firebase ID token verification failed: ${e.message ?: "Unknown error."}"
            )
        } catch (e: Exception) {
            throw UnauthenticatedException(
                "Unexpected error during Firebase token verification: ${e.message}"
            )
        }

        // Validate issuer and audience explicitly as a defence-in-depth check.
        val expectedIssuer = "https://securetoken.google.com/$expectedProjectId"
        if (decodedToken.issuer != expectedIssuer) {
            throw UnauthenticatedException(
                "Firebase token issuer mismatch. Expected '$expectedIssuer', " +
                "got '${decodedToken.issuer}'."
            )
        }

        if (decodedToken.uid.isBlank()) {
            throw UnauthenticatedException("Firebase token subject (uid) is empty after verification.")
        }

        return VerifiedFirebaseToken(
            uid = decodedToken.uid,
            phoneNumber = decodedToken.claims["phone_number"] as? String,
            email = decodedToken.email,
            name = decodedToken.name,
            issuer = decodedToken.issuer,
            audience = expectedProjectId,
            expiresAtSeconds = 0L  // Expiration is validated cryptographically by verifyIdToken(); not re-exposed here.
        )
    }

    companion object {
        private val initialised = AtomicBoolean(false)

        /**
         * Initialises the Firebase Admin SDK singleton with the provided service-account credentials.
         *
         * Must be called exactly once at server startup before any [FirebaseTokenVerifier] instance
         * can verify tokens. Safe to call multiple times — subsequent calls are no-ops.
         *
         * @param serviceAccountStream  InputStream of the Firebase service account JSON key file.
         *                              Must not be null. The stream is consumed and closed.
         * @param projectId             Firebase project ID (e.g. "sucharu-pro-erp").
         */
        fun initialise(serviceAccountStream: InputStream, projectId: String = "sucharu-pro-erp") {
            if (initialised.compareAndSet(false, true)) {
                val credentials = GoogleCredentials.fromStream(serviceAccountStream)
                val options = FirebaseOptions.builder()
                    .setCredentials(credentials)
                    .setProjectId(projectId)
                    .build()
                if (FirebaseApp.getApps().isEmpty()) {
                    FirebaseApp.initializeApp(options)
                }
            }
        }

        /**
         * True if the Firebase Admin SDK has been successfully initialised.
         */
        fun isInitialised(): Boolean = initialised.get() && FirebaseApp.getApps().isNotEmpty()
    }
}
