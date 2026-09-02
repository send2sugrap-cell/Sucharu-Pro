# User Account Security & Password Lifecycle Specification

## 1. Executive Summary
This document specifies the security controls governing user accounts, credential storage, password policy enforcement, historical password tracking, brute-force mitigation, and single-use verification tokens within **Sucharu Pro — Commercial Printing ERP**.

---

## 2. Password Security & Hashing Architecture

### 2.1 Algorithm Standard
- **Key Derivation Function**: PBKDF2 with HMAC-SHA256 (or Argon2id compliant backend hashing abstraction).
- **Salt Generation**: High-entropy 128-bit (16-byte) cryptographically secure random salt generated per password instance.
- **Work Factor / Iterations**: Minimum 100,000 iterations to resist off-line GPU hardware-accelerated dictionary and rainbow table attacks.
- **Storage Specification**: Password hashes are stored alongside their hex-encoded salt, algorithm identifier, and iteration count in `user_identities.password_hash` and `password_history.password_hash`.

### 2.2 Password History & Anti-Reuse Policy
- **History Lookback Depth**: Configurable (default: last 5 passwords tracked in `password_history`).
- **Validation Execution**: When a password change request occurs (`/api/v1/auth/password/change`), `UserIdentityService` fetches the historical password entries for the user and verifies the proposed new password against every recorded hash using constant-time comparison.
- **Rejection behavior**: If a match is discovered, a `400 Bad Request` (`PASSWORD_RECENTLY_USED`) is returned immediately, and an audit event is logged.

### 2.3 Password Reset & Verification Tokens
- **Token Generation**: URL-safe high-entropy strings generated using `TokenGenerator.generateSecureToken(32)`.
- **Hashed Storage**: Only SHA-256 hashes of verification tokens are stored in `user_verification_tokens.token_hash`. The raw token is delivered exclusively to the requesting client/communication channel.
- **Single-Use & Expiration**: Tokens expire after a strict TTL (default: 15-60 minutes). Attempting to reuse an already consumed (`USED`) or expired (`EXPIRED`) token results in immediate rejection and invalidation.

---

## 3. Account Protection & Brute-Force Safeguards

### 3.1 Lockout Thresholds
- **Max Failed Login Attempts**: 5 consecutive failed attempts within a 15-minute rolling window.
- **Automated State Transition**: Upon exceeding the threshold, account status transitions automatically from `ACTIVE` to `LOCKED`.
- **Notification & Audit**: Triggers `ACCOUNT_LOCKED` security audit event and notification to user security contact.

### 3.2 Administrative Account Status Management
Administrators with `ADMIN_MANAGE_USERS` capability can transition account statuses using `/api/v1/admin/users/{userId}/status`.
- Mandatory administrative audit reason parameter.
- Transition validity checks via `AccountStatus.isValidTransitionTo(targetStatus)`.
- Immediate invalidation of all active user sessions upon lock, suspension, or deactivation.
