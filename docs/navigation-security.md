# Navigation Security

## Core Security Model

Navigation in Sucharu Pro is **server-authoritative**. The client UI is treated as an untrusted surface. All authorization decisions are made against the `AuthenticatedPrincipal` derived from a server-signed JWT — never against locally stored state.

---

## Defense Layers

### Layer 1 — JWT Verification (Backend)
The `AuthenticatedPrincipal` is reconstructed from a short-lived, signed JWT access token. The backend rejects any request with an invalid, expired, or tampered token. The role and permissions embedded in the JWT cannot be altered by the client.

### Layer 2 — DeepLinkAuthorizer (Client)
Before any navigation is performed, `DeepLinkAuthorizer.authorizeDeepLink()` evaluates the target route against the current `AuthenticatedPrincipal`. It:
- Returns `AppDestination.Public.Login` for unauthenticated principals attempting protected routes
- Returns `AppDestination.Security.Forbidden` for role mismatches
- Performs resource ownership checks for parameterized routes

### Layer 3 — CapabilityAwareNavigation (UI Filtering)
Navigation menu items are hidden (not merely disabled) when the principal lacks the required `AuthorizationCapability`. This prevents inadvertent exposure of inaccessible features.

### Layer 4 — Back-Stack Clearing on Logout
On every session termination, `AppNavigationManager` clears the entire navigation back-stack. A logged-out user cannot press the device back button to return to any authenticated screen.

---

## AI_AGENT Machine Principal Isolation

`UserRole.AI_AGENT` is a machine identity used exclusively for server-side automation and n8n agent orchestration.

**AI_AGENT is hard-blocked at Layer 2:**
```
if (principal.role == UserRole.AI_AGENT) {
    return AppDestination.Security.Forbidden
}
```

AI_AGENT principals will never be routed to any human-interactive workspace, regardless of what route they request. This is enforced unconditionally before any role or capability evaluation.

---

## Anti-Spoofing

`DeepLinkAuthorizer` accepts three optional client-supplied parameters:
- `clientSuppliedUserId`
- `clientSuppliedProjectId`
- `clientSuppliedRole`

These parameters are **silently discarded**. They are never used in any authorization decision. All routing authority derives exclusively from `AuthenticatedPrincipal` (which comes from the server-verified JWT).

---

## Session Expiry

When the session expires or is revoked:
1. `AuthenticationSessionManager` detects the expired/revoked state
2. `AppNavigationManager` transitions to `NavigationSessionState.SessionExpired`
3. `SucharuGraphicsAppShell` renders `SecurityStateViews.SessionExpiredView`
4. The back-stack is cleared
5. The user must re-authenticate to access any protected destination
