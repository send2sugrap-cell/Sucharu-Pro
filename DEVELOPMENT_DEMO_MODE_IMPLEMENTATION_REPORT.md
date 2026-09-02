# DEVELOPMENT DEMO MODE + DEMO OTP IMPLEMENTATION REPORT
## MOBILE APP DEMONSTRATION — SAFE ISOLATED IMPLEMENTATION

### 1. Objective
Establish an isolated, secure, and production-safe **DEVELOPMENT DEMO MODE** for Sucharu Pro ERP.
This allows the owner/developer to inspect the complete mobile UI/UX on physical Android devices without requiring live Hostinger/VPS infrastructure, backend deployments, SMS delivery gateways, or real customer registration.

---

### 2. Architecture & Production Isolation

```
PRODUCTION BUILD (DEMO_MODE = false):
┌─────────────────────────────────────────────────────────────┐
│ Android App (SucharuGraphicsAppShell)                        │
│   ↓ (Strict Network Boundary)                               │
│ ProductionRuntimeComposition                                │
│   ↓ (SUCHARU_API_GATEWAY_URL)                               │
│ HTTPS API Gateway                                           │
│   ↓                                                         │
│ Backend Server                                              │
│   ↓ (Tenant Isolation + RLS)                                │
│ PostgreSQL                                                  │
└─────────────────────────────────────────────────────────────┘

DEVELOPMENT / DEMO BUILD (DEMO_MODE = true):
┌─────────────────────────────────────────────────────────────┐
│ Android App (SucharuGraphicsAppShell)                        │
│   ↓                                                         │
│ DevelopmentDemoRuntimeComposition                           │
│   ↓                                                         │
│ DemoBackendApiClient (In-Memory Offline Client)             │
│   ↓                                                         │
│ DemoOrderFixtures (ord-demo-001 + Sample Data)              │
│ (Zero PostgreSQL access, Zero Network, Zero Credentials)    │
└─────────────────────────────────────────────────────────────┘
```

---

### 3. Demo Build Flag
- Controlled at compile-time via Gradle `buildConfigField` in `app/build.gradle.kts`:
  - `debug`: `buildConfigField("boolean", "DEMO_MODE", "true")`
  - `release`: `buildConfigField("boolean", "DEMO_MODE", "false")`
- The production APK cannot activate Demo Mode at runtime through deep links, query parameters, or local preferences.

---

### 4. Demo Runtime Composition
- **`DevelopmentDemoRuntimeComposition`** (`core/src/main/java/com/sucharu/sucharupro/data/composition/RuntimeComposition.kt`):
  - Implements `AppRuntimeComposition` with `mode = AppRuntimeMode.DEVELOPMENT`.
  - Instantiates `AuthenticationSessionManager` connected exclusively to `DemoBackendApiClient`.
  - Zero connection to PostgreSQL or production API Gateway.

---

### 5. Demo User
- **Identity**:
  - `userId = "USER-DEMO-001"`
  - `username = "demo"`
  - `email = "demo@sucharu.com"`
  - `role = UserRole.CUSTOMER` (Strictly restricted from `ADMIN`, `MANAGER`, `STAFF` roles).
- The demo user exists strictly in memory within the demo client runtime and is **never** written to PostgreSQL or production databases.

---

### 6. Demo OTP & Verification Flow
- **Deterministic OTP**: `123456`
- Accepted **only** inside `DemoBackendApiClient` / `DevelopmentDemoRuntimeComposition`.
- **Production Authentication Safety**:
  - `AuthenticationService` and production backend explicitly reject `123456` for real accounts.
  - No universal OTP or bypass logic exists in production authentication.

---

### 7. Demo Data & Presentation Fixtures
- Leverages `DemoOrderFixtures.demoOrders()`:
  - `ord-demo-001` ("ORD-DEMO-001"): Premium Business Cards with Spot UV, High Priority, Ready for Job Handoff.
  - Complete sample orders with status tracking, items, delivery requirements, and notes.
- In-memory customer profile: `Sucharu Demo Client` (Credit limit: 50,000.00 BDT, Balance: 1,200.00 BDT).
- Canonical unit-test fixtures (`FakeOrderDataSource.defaultSampleOrders()`) remain clean and unpolluted.

---

### 8. Public Home & Navigation Experience
- **Public Home**:
  - Renders all public services, products, offers, gallery, about, FAQ, contact, and AI Assistant.
  - When `isDemoMode == true`: Displays a prominent `DEV DEMO — TRY DEMO (NO REGISTRATION)` card.
  - When `isDemoMode == false`: The demo card is completely hidden.
- **Navigation Safety**:
  - Back button (UI arrow, Android hardware back, Android gesture back) safely navigates from Verification back to Public Home without navigation traps or duplicate backstacks.
  - **Demo Logout**: Tapping `Sign Out` executes `sessionManager.logout()`, clearing the demo session and returning cleanly to Public Home.

---

### 9. Test Verification Results

#### A. Core Security & Isolation Suite (`DevelopmentDemoModeSecurityTest`)
```
DevelopmentDemoModeSecurityTest > testDemoBackendApiClient_returnsRichDemoOrders PASSED
DevelopmentDemoModeSecurityTest > testProductionRuntimeComposition_blocksDirectDatabaseConnections PASSED
DevelopmentDemoModeSecurityTest > testDevelopmentDemoRuntimeComposition_endToEndLifecycle PASSED
DevelopmentDemoModeSecurityTest > testProductionRuntimeComposition_failsFastWhenApiGatewayUrlMissing PASSED
DevelopmentDemoModeSecurityTest > testDemoBackendApiClient_accepts123456_andRejectsOtherOtps PASSED
DevelopmentDemoModeSecurityTest > testProductionAuthenticationService_rejectsUniversalOtp123456 PASSED
```

#### B. Android App Unit Test Suite (`DevelopmentDemoModeUiTest` + All Feature Tests)
```
DevelopmentDemoModeUiTest > testDemoComposition_instantiatesDemoSessionManager PASSED
DevelopmentDemoModeUiTest > testProductionComposition_blocksUnconfiguredGateway PASSED
DevelopmentDemoModeUiTest > testDemoUser_isCustomerRoleOnly PASSED
... (All ViewModels, Navigation, Routing, Security tests passed: 100%)
BUILD SUCCESSFUL in 49s
```

#### C. Full Multi-Module Build & Packaging
```
.\gradlew.bat :core:test :backend:test :backend:jar :app:assembleDebug
BUILD SUCCESSFUL in 4m 38s
47 actionable tasks: 11 executed, 36 up-to-date
```
- Generated APK: `app/build/outputs/apk/debug/app-debug.apk`

---

### 10. Summary of Invariants Preserved
- [x] Production authentication remains untouched and production-hardened.
- [x] No PostgreSQL credentials in Android application.
- [x] No direct PostgreSQL connection from Android client.
- [x] Demo mode accessible only on debug/demo builds.
- [x] Demo OTP `123456` rejected by production authentication.
- [x] Zero regressions across all ERP modules (Modules 00–17).
