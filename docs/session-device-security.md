# Session & Device Security Specification

## 1. Overview
This document specifies the session management, device tracking, token revocation, and remote session termination controls for **Sucharu Pro — Commercial Printing ERP**.

---

## 2. Token & Session Architecture

### 2.1 Dual-Token Framework
- **Access Tokens**: Short-lived (15 minutes), signed JSON Web Tokens (JWT) containing server-authoritative principal claims (`userId`, `tenantId`, `role`, `sessionId`).
- **Refresh Tokens**: Long-lived (7 days), opaque cryptographically random tokens stored in database table `user_sessions`. Only token hashes (`SHA-256`) are stored in PostgreSQL.

### 2.2 Device & Session Metadata Tracking
Every session entry in `user_sessions` records:
- `session_id`: Unique UUID identifier.
- `user_id`: Canonical user UUID.
- `tenant_id`: Multi-tenant boundary UUID.
- `device_info`: User-Agent header, OS, client app version.
- `ip_address`: Client IP at creation & last activity.
- `created_at` & `last_seen_at`: Timestamp tracking.
- `status`: Session lifecycle state (`ACTIVE`, `EXPIRED`, `REVOKED`).

---

## 3. Remote Session Revocation & Device Management

### 3.1 Session Listing (`GET /api/v1/auth/sessions`)
Users can view all their active sessions across mobile devices, web sessions, and background clients. The response indicates `isCurrentSession: true` for the active token making the request.

### 3.2 Targeted Session Revocation (`DELETE /api/v1/auth/sessions/{sessionId}`)
Allows users to remotely terminate a specific compromised or lost device session.
- Validates that target session belongs strictly to the requesting user (`enforceUserOwnership`).
- Updates session status in `user_sessions` to `REVOKED` with `revoked_at` timestamp.
- Immediate blacklisting of associated refresh tokens.

### 3.3 Mass Remote Revocation (`POST /api/v1/auth/sessions/revoke-all`)
Allows users or security administrators to terminate all active sessions across all devices simultaneously.
- Option `preserveCurrentSession` (default: `true`) keeps the current session active while invalidating all other devices.
- Triggers mass session invalidation and audit record generation (`SESSIONS_MASS_REVOKED`).

---

## 4. Automatic Session Invalidation Triggers
Sessions are automatically revoked by the server under the following security events:
1. **Password Change**: Changing password invalidates all other active sessions for that account.
2. **Account Status Change**: Transitioning account status to `LOCKED`, `SUSPENDED`, `DEACTIVATED`, or `DELETED` immediately revokes ALL active sessions.
3. **Refresh Token Replay**: Detecting refresh token reuse/replay invalidates the entire session chain and flags the account for security review.
