# Sucharu Pro — Session Management & Token Rotation

## Session Lifecycle

### 1. Session Creation (Login)
Upon successful credential validation:
1. A unique `sessionId` (`sess_<uuid>`) is generated.
2. A cryptographic refresh token is generated and SHA-256 fingerprinted.
3. An `AuthSession` record is persisted in `auth_sessions` with status `ACTIVE`.
4. Client receives `AuthResponseDto` containing short-lived JWT `accessToken` and raw `refreshToken`.

### 2. Single-Flight Refresh Token Rotation (RFC 6749 / OAuth 2.0 BCP)
When access token expires:
1. Client acquires single-flight mutex (`DirectBackendApiClient.refreshMutex`) to avoid race conditions.
2. Client sends `POST /api/v1/auth/refresh` with `refreshToken`.
3. Server computes SHA-256 hash of incoming token:
   - If hash matches active session `refresh_token_hash`: session is rotated, new token pair is issued, old hash added to consumed history.
   - If hash matches an already consumed token in session history: **REPLAY ATTACK DETECTED**. Entire session chain is immediately REVOKED with reason `Suspected refresh token replay` and audit alert logged.
   - If hash is unknown: rejected with `UNAUTHENTICATED`.

### 3. Session Revocation (Logout & Admin Invalidation)
- **Single Session Logout** (`POST /api/v1/auth/logout`): Marks current session as `REVOKED`. Clears client storage.
- **Logout All Devices** (`POST /api/v1/auth/logout-all`): Revokes all active sessions belonging to the user.
- **Password Change / Security Events**: Automatically revokes all existing active sessions.
