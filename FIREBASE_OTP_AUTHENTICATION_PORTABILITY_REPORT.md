# SUCHARU PRO — FIREBASE OTP AUTHENTICATION & SERVER-PORTABLE AUTH ARCHITECTURE REPORT

## 1. Architectural Summary & Provider Abstraction
Sucharu Pro utilizes a **Provider-Based Authentication Architecture** designed to decouple identity verification providers (such as Firebase Phone Auth) from application business authority, session management, tenant isolation, and PostgreSQL data persistence.

```
[ Android App ]
      │
      ├── AuthenticationProvider (Interface)
      │       ├── FirebaseAuthenticationProvider (Production)
      │       └── DemoAuthenticationProvider (Isolated Demo Mode)
      │
      ▼ (Verification Token: Firebase ID Token / Demo Token)
[ Backend Gateway API ] (Configurable: SUCHARU_API_GATEWAY_URL)
      │
      ├── FirebaseTokenVerifier (Verifies JWT signature, issuer, audience, subject)
      ├── AuthenticationService (Normalizes phone, resolves customer/tenant, verifies session)
      │
      ▼
[ Canonical PostgreSQL Authority ] (Database: Customers, Tenants, RBAC, Active Sessions)
```

### Key Components

1. **`AuthenticationProvider` Interface** ([`AuthenticationProvider.kt`](file:///E:/App/Sucharu%20Pro/core/src/main/java/com/sucharu/sucharupro/data/auth/provider/AuthenticationProvider.kt)):
   - Defines strict contracts for `requestPhoneOtp`, `verifyPhoneOtp`, `getIdentityToken`, `signOut`, and `getProviderId`.
   - UI layers and ViewModels interact exclusively with `AuthenticationProvider`, ensuring Firebase dependencies do not leak into business logic or view rendering.

2. **Production Firebase Provider** ([`FirebaseAuthenticationProvider.kt`](file:///E:/App/Sucharu%20Pro/app/src/main/java/com/sucharu/sucharupro/data/auth/FirebaseAuthenticationProvider.kt)):
   - Leverages official `firebase-auth` SDK (BoM `33.10.0`).
   - Handles phone OTP dispatch, automatic credential retrieval, and verification code sign-in.
   - Obtains a signed Firebase ID token (`getIdToken(forceRefresh = true)`), which is passed to the Sucharu backend for identity resolution.

3. **Isolated Demo Provider** ([`DemoAuthenticationProvider.kt`](file:///E:/App/Sucharu%20Pro/core/src/main/java/com/sucharu/sucharupro/data/auth/provider/DemoAuthenticationProvider.kt)):
   - Activated when `DEMO_MODE = true`.
   - Bypasses external Firebase SDK calls and accepts demo OTP `123456`.
   - Prevents accidental production billing, SMS quota consumption, or test data contamination.

---

## 2. Backend Token Verification & Canonical Authority

Firebase is strictly restricted to initial phone identity verification. The Sucharu Pro backend maintains full, unyielding authority over user accounts, tenant resolution, RBAC, and session token generation.

1. **Token Verification Protocol** ([`FirebaseTokenVerifier.kt`](file:///E:/App/Sucharu%20Pro/core/src/main/java/com/sucharu/sucharupro/data/auth/security/FirebaseTokenVerifier.kt)):
   - Verifies ID token JWT structure, header algorithm (`RS256`), issuer (`https://securetoken.google.com/<PROJECT_ID>`), audience (`<PROJECT_ID>`), and valid non-expired timestamps.
   - Extracts canonical Firebase phone number claim (`phone_number`).

2. **Customer & Tenant Scoping** ([`AuthenticationService.kt`](file:///E:/App/Sucharu%20Pro/core/src/main/java/com/sucharu/sucharupro/data/auth/service/AuthenticationService.kt)):
   - Normalizes phone numbers (e.g. converting `+880 1711-223344` to local canonical format `01711223344` via `CustomerValidation.normalizePhoneNumber`).
   - Resolves or creates the corresponding Customer entity in PostgreSQL.
   - Resolves tenant affiliation, tenant user role (`ADMIN`, `OPERATOR`, `CUSTOMER`), and active session lifecycle.
   - Issues Sucharu JWT Access Token signed by PostgreSQL session authority.

---

## 3. Server Gateway & Infrastructure Portability

1. **Configurable Gateway Endpoint**:
   - Android client dynamically loads API Gateway URL via `SUCHARU_API_GATEWAY_URL` environment/build configuration (defaulting to local/VPS endpoint).
   - Allows hosting server relocation (VPS, AWS, GCP, On-Premise) without requiring any changes to Android source code.

2. **Provider Swap Readiness**:
   - Should Firebase Phone Auth be replaced (e.g., Twilio, AWS SNS, Infobip, or direct SMS gateway), only a new implementation of `AuthenticationProvider` is required.
   - The backend `/api/v1/auth/firebase` endpoint (or vendor-agnostic equivalent `/api/v1/auth/otp-verify`) retains identical downstream customer resolution and PostgreSQL session logic.

---

## 4. Verification & Test Matrix

| Verification Suite | Target Component | Status | Details |
| :--- | :--- | :--- | :--- |
| **Portability Unit Tests** | `FirebaseAuthPortabilityTest.kt` | **PASSED (5/5)** | Tests provider abstraction, token verification, customer/tenant resolution, and server gateway dynamic routing. |
| **Core Module Tests** | `:core:test` | **PASSED** | Validated core data models, security verifiers, and API clients. |
| **App Unit Tests** | `:app:testDebugUnitTest` | **PASSED** | Validated Android app module unit test suite. |
| **Debug Build Assembly** | `:app:assembleDebug` | **PASSED** | Compiled APK debug artifact with Firebase BoM dependencies included. |

---

## 5. Security & Isolation Matrix

- **Zero Business Logic in Firebase**: Firebase Firestore / Realtime Database / Cloud Functions are NOT utilized for ERP business logic.
- **PostgreSQL Authority**: All tenant isolation, RLS rules, order records, and user permissions remain strictly inside PostgreSQL.
- **Isolated Demo Mode**: Demo OTP (`123456`) operates strictly within `DemoAuthenticationProvider` and `DemoBackendApiClient`, fully separated from production Firebase verification.
