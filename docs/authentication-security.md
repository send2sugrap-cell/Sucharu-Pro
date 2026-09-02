# Sucharu Pro — Authentication Security & Cryptography Specification

## Security Controls

### 1. Password Storage & Hashing
- **Algorithm**: `PBKDF2WithHmacSHA256`
- **Iterations**: 65,536 iterations
- **Salt**: 16 bytes (128 bits) cryptographically secure random (`SecureRandom`)
- **Key Length**: 256 bits (32 bytes)
- **Comparison**: Constant-time byte array comparison via `MessageDigest.isEqual` to prevent timing attacks.

### 2. JWT Access Token Security
- **Algorithm**: `HMAC-SHA256` (HS256)
- **Claims**:
  - `sub`: User ID
  - `sid`: Session ID
  - `pid`: Tenant Project ID
  - `usr`: Username
  - `role`: Canonical User Role
  - `perms`: Comma-separated canonical permission set
  - `iss`: Configured Issuer (`sucharu-pro-backend`)
  - `aud`: Configured Audience (`sucharu-pro-clients`)
  - `iat` / `exp` / `jti`: Standard JWT claims
- **Validation**: Strict validation of algorithm, signature, issuer, audience, and expiration before trusting payload.

### 3. Refresh Token Fingerprinting & Storage
- **Generation**: 32-byte (256-bit) URL-safe cryptographically secure random token.
- **Storage**: NEVER stored in plaintext. Hashed with SHA-256 before persistence in `auth_sessions.refresh_token_hash`.
- **Database Exposure Risk**: Even if database records are compromised, raw refresh tokens cannot be extracted.

### 4. Brute-Force & Credential Stuffing Defense
- **Failed Attempt Tracking**: Incremented on each failed password attempt.
- **Account Lockout**: After 5 consecutive failed attempts, account transitions to `LOCKED` status for 15 minutes (`lock_until`).
- **Generic Error Responses**: Identical `UNAUTHENTICATED` / "Invalid credentials." response returned for non-existent users, wrong passwords, and locked accounts to eliminate user enumeration vectors.

### 5. Secret Protection & Sanitization
- **Configuration Redaction**: `AuthConfig.toSafeString()` automatically redacts `jwtSigningSecret` (`[REDACTED]`).
- **Zero Database / Stack Trace Leakage**: Server errors are sanitized to generic `INTERNAL_ERROR` without leaking SQL syntax, table names, or connection strings.
