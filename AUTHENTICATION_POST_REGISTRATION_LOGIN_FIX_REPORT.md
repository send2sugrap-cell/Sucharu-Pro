# SUCHARU PRO ERP — AUTHENTICATION POST-REGISTRATION LOGIN DEFECT FIX REPORT

**Date:** 2026-08-31  
**Scope:** Public Registration Lifecycle, Contact Verification, Account Activation, Mobile Normalization, and Post-Login Routing  
**Target Modules:** `:core`, `:app`, `:backend`  
**Status:** **COMPLETED, VERIFIED & PASSING (100% Regression Passed)**

---

## 1. Executive Summary

This targeted fix resolved the critical blocking issue where a newly registered customer user who received the notice *"Registration successful. Please verify your account."* was unable to log in and received `"Invalid credentials."` when attempting to authenticate with their registered mobile number.

Through comprehensive code tracing, four distinct root causes were pinpointed and rectified across data sources, backend routing, business domain logic, and Compose UI navigation.

---

## 2. Root Cause Analysis & Rectifications

### A. Account Status Invariant vs Public Verification Gap
* **Root Cause:**
  - `AuthenticationService.register` properly created new user accounts in `AccountStatus.PENDING`.
  - `AuthAccount.canAuthenticate` only allows `ACTIVE` (or unlocked `LOCKED`) accounts to log in.
  - The previous `/api/v1/auth/verification/confirm` route required an authenticated `Bearer` token (`securityContext.authenticate`), creating an impossible catch-22 for unverified pending users who had no access token.
  - Furthermore, `UserIdentityService.confirmVerificationToken` only updated profile verification timestamps and did not transition `accountStatus` from `PENDING` to `ACTIVE`.
* **Fix:**
  - Updated `BackendRouter.kt` to allow both public unauthenticated verification confirmation (for pending account activation) and authenticated verification (for logged-in contact changes).
  - Added `verifyAccount` to `AuthenticationService.kt` and updated `UserIdentityService.kt` to consume the single-use token and update `accountStatus` to `AccountStatus.ACTIVE` upon successful verification.
  - Added `resendVerificationToken` endpoint and service method to allow users to request new verification tokens without account enumeration leakage.

### B. Mobile Number Normalization Mismatch in Identifier Lookups
* **Root Cause:**
  - Registration used `CustomerValidation.normalizePhoneNumber` to save local `01X...` numbers to `auth_accounts.phone` (e.g. `+8801712553809` -> `01712553809`).
  - Login and password recovery searched the raw `identifier` string against `(username = ? OR email = ? OR phone = ?)`.
  - When users entered `+8801712553809`, `8801712553809`, or formatted variants like `+880 1712-553809`, lookup failed to find the account record, producing `"Invalid credentials."`.
* **Fix:**
  - Updated `getAccount` in both `PostgresAuthDataSources.kt` and `FakeAuthDataSources.kt` to match against normalized phone numbers in addition to raw identifiers, emails, and usernames.
  - Enhanced `AuthenticationService.login` and `requestPasswordRecovery` to apply canonical phone normalization fallback.

### C. Safe and Actionable Login Error Feedback
* **Root Cause:**
  - Inactive or pending accounts were previously rejected with a generic `"Invalid credentials."` before checking if the password was correct, leaving valid users confused.
* **Fix:**
  - Password verification is now evaluated first to prevent user enumeration / status probing.
  - If password is correct but `accountStatus == AccountStatus.PENDING`, a clear and safe notice is returned: `"Account pending verification. Please verify your account before logging in."`.
  - Deactivated or suspended accounts return status-appropriate errors, and invalid passwords increment failed login counters and return generic `"Invalid credentials."`.

### D. App Shell UI Flow Routing
* **Root Cause:**
  - In `SucharuGraphicsAppShell.kt`, upon receiving `res.data.verificationRequired == true`, the UI previously forced `activeAuthScreenOverride = "login"`.
* **Fix:**
  - When `verificationRequired` is true, the UI routes directly to `activeAuthScreenOverride = "verification"`, rendering `VerificationScreen`.
  - Upon token confirmation, the user's account is activated and navigated to login with `"Account verified successfully! Please sign in."`.
  - Resend token and skip actions are fully wired.

---

## 3. Verification & Test Execution Results

| Test Suite | Module | Test Count | Result |
| :--- | :--- | :--- | :--- |
| `AuthenticationRegistrationEndToEndTest` | `:app` | 5 | **PASSED (100%)** |
| `ContactPickerAndValidationTest` | `:app` | 5 | **PASSED (100%)** |
| `PasswordVisibilityUnitTest` | `:app` | 3 | **PASSED (100%)** |
| Complete `:app:testDebugUnitTest` | `:app` | 92 | **PASSED (100%)** |
| Complete `:core:test` | `:core` | 2,997 | **PASSED (100%)** |
| Complete `:backend:test` | `:backend` | 443 | **PASSED (100%)** |
| `:backend:jar` Packaging | `:backend` | 1 | **PASSED** |
| `:app:assembleDebug` APK Build | `:app` | 1 | **PASSED** |

---

## 4. Modified & Created Files Summary

* **Modified Files:**
  - `PostgresAuthDataSources.kt`: Enhanced `getAccount` SQL query with normalized phone matching.
  - `FakeAuthDataSources.kt`: Enhanced `getAccount` in-memory lookup with normalized phone matching.
  - `UserIdentityService.kt`: Added `AccountStatus.PENDING -> AccountStatus.ACTIVE` transition on contact verification.
  - `AuthenticationService.kt`: Added normalized phone login/recovery resolution, safe pending error message, public `verifyAccount`, and `resendVerificationToken`.
  - `AuthModels.kt`: Added `ResendVerificationRequestDto` and enhanced verification DTOs.
  - `BackendRouter.kt`: Supported public unauthenticated verification confirmation and resend endpoints.
  - `BackendApiClient.kt`: Added `resendVerificationToken` to client interface and `DirectBackendApiClient`.
  - `AuthenticationSessionManager.kt`: Added `confirmVerification` and `resendVerification`.
  - `SucharuGraphicsAppShell.kt`: Wired `VerificationScreen` navigation and token confirmation handling.
  - `PostgresVerificationSecurityTest.kt`: Added account activation and resend tests.
  - `PostgresLoginFlowSecurityTest.kt`: Added multi-format phone login and pending status tests.
  - `AuthenticationRegistrationEndToEndTest.kt`: Updated end-to-end flow to test registration -> unverified login rejection -> single-use token confirmation -> activated multi-format phone login -> `CustomerWorkspace`.
  - `LogoutBackStackSecurityTest.kt` & `SessionExpiryNavigationTest.kt`: Updated stub clients.
