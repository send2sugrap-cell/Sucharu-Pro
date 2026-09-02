# Unified Sucharu Graphics App Shell

## Design Principle

**One Unified Application — One Brand — Many Role Workspaces**

The Sucharu Pro Android client is a **single APK** that serves all user types — Guest, Customer, Affiliate, Staff, Manager, Admin — under a single branded experience.

There are no separate apps per role. The `SucharuGraphicsAppShell` is the single root composable that dispatches to role-appropriate workspace views based on the server-authoritative `AuthenticatedPrincipal`.

---

## Shell Composition

```
SucharuGraphicsAppShell
├── Observes: AppNavigationManager.sessionState
├── Dispatches to:
│   ├── PublicExperienceView          (GUEST / unauthenticated)
│   ├── CustomerWorkspaceShell        (CUSTOMER role)
│   ├── AffiliateWorkspaceShell       (AFFILIATE role)
│   ├── InternalWorkspaceShells
│   │   ├── StaffShell                (STAFF role)
│   │   ├── ManagerShell              (MANAGER role)
│   │   └── AdminShell                (ADMIN role)
│   └── SecurityStateViews
│       ├── VerificationRequiredView
│       ├── AccountUnavailableView
│       ├── SessionExpiredView
│       └── ForbiddenView
└── AI_AGENT: BLOCKED at DeepLinkAuthorizer — never enters any shell
```

---

## Session-Aware Routing

The shell reacts to `NavigationSessionState` transitions:

| State | Shell Rendered |
|---|---|
| `Unauthenticated` | `PublicExperienceView` (with Login/Register entry points) |
| `Authenticated(principal)` | Role-appropriate workspace shell |
| `RequiresVerification` | `SecurityStateViews.VerificationRequiredView` |
| `AccountUnavailable` | `SecurityStateViews.AccountUnavailableView` |
| `SessionExpired` | `SecurityStateViews.SessionExpiredView` |
| `SecurityReview` | `SecurityStateViews.ForbiddenView` |

---

## Back-Stack Security

After logout, the back-stack is **fully cleared**. No authenticated destination is reachable by pressing the back button from the guest/login screen. This is enforced by `AppNavigationManager` on every `NavigationSessionState.Unauthenticated` transition and validated by `LogoutBackStackSecurityTest`.

---

## Branding

All shells share the **Sucharu Graphics** brand identity: typography, colour palette, and logo lockup. Role differences manifest in navigation structure and content — not in separate applications.
