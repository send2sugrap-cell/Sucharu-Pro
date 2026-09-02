# SUCHHARU PRO ERP — AUTHENTICATION & REGISTRATION UX GAP-FIX REPORT

**Date:** 2026-08-31  
**Scope:** Public Registration End-to-End Flow + Password Visibility Controls + Android Phonebook/Contacts Picker  
**Target Modules:** `:app`, `:core`, `:backend`  
**Status:** **COMPLETED & VERIFIED** (Full regression passed)

---

## 1. Executive Summary

This targeted application-level gap-fix addressed three critical usability and functional gaps in the Sucharu Pro ERP authentication foundation:
1. **End-to-End Registration Wiring:** Added missing dispatch routes in `BackendRouter` for `/api/v1/auth/register`, `/api/v1/auth/password/recovery/request`, and `/api/v1/auth/password/recovery/confirm`. Wired client session flow, input validation, post-registration state transitions, and auto-navigation to sign-in with clear feedback banners.
2. **Password Visibility Controls:** Implemented independent, state-isolated eye / eye-off toggleable password fields with full accessibility support across `RegisterScreen`, `LoginScreen`, `ResetPasswordScreen`, and `VendorPortalEntryScreen`.
3. **Android Device Phonebook / Contacts Picker:** Implemented a non-intrusive Android contacts picker flow using `ActivityResultContracts.PickContact()` with transient URI access, runtime permission handling, multi-number selection modal dialogs, phone number normalization, and strict data minimization (zero contact scraping or storage of unrelated contacts).

---

## 2. Gap-Fix Details & Implementations

### A. End-to-End Registration Routing & Flow (Gap A)
* **Backend Router Dispatch:**
  - Added HTTP route handlers in [BackendRouter.kt](file:///e:/App/Sucharu%20Pro/core/src/main/java/com/sucharu/sucharupro/data/api/server/BackendRouter.kt) for `POST /api/v1/auth/register`, `POST /api/v1/auth/password/recovery/request`, and `POST /api/v1/auth/password/recovery/confirm`.
  - Integrated with `AuthenticationService` and `UserIdentityService` when present, with secure default responses for test/standalone runtimes.
* **Authentication Service Improvements:**
  - Enhanced duplicate account prevention in [AuthenticationService.kt](file:///e:/App/Sucharu%20Pro/core/src/main/java/com/sucharu/sucharupro/data/auth/service/AuthenticationService.kt) to check both primary and normalized mobile numbers (`+880` vs `01X`) as well as email addresses.
  - Enforced strict public registration role policies (`CUSTOMER` default, `AFFILIATE` with referral code; blocked `ADMIN`, `MANAGER`, `STAFF`, `AI_AGENT`).
* **App Shell & Client Session Feedback:**
  - Updated [SucharuGraphicsAppShell.kt](file:///e:/App/Sucharu%20Pro/app/src/main/java/com/sucharu/sucharupro/ui/shell/SucharuGraphicsAppShell.kt) to handle successful registration and password reset by navigating to the login screen with clear success banners.

### B. Password Visibility Toggle (Gap B)
* **Screen Integration:**
  - Updated [RegisterScreen.kt](file:///e:/App/Sucharu%20Pro/app/src/main/java/com/sucharu/sucharupro/ui/features/auth/RegisterScreen.kt) with independent visibility toggles on `password` and `confirmPassword` fields.
  - Updated [LoginScreen.kt](file:///e:/App/Sucharu%20Pro/app/src/main/java/com/sucharu/sucharupro/ui/features/auth/LoginScreen.kt) with visibility toggle on the `password` field.
  - Updated [ResetPasswordScreen.kt](file:///e:/App/Sucharu%20Pro/app/src/main/java/com/sucharu/sucharupro/ui/features/auth/ResetPasswordScreen.kt) with independent visibility toggles on `newPassword` and `confirmPassword` fields.
  - Updated [VendorPortalEntryScreen.kt](file:///e:/App/Sucharu%20Pro/app/src/main/java/com/sucharu/sucharupro/ui/features/vendorportal/VendorPortalEntryScreen.kt) with visibility toggle on the `password` field.
* **Accessibility & Security:**
  - `rememberSaveable` state preservation across configuration changes and recomposition.
  - Accessible `contentDescription`: `"Show password"` when hidden, `"Hide password"` when visible.
  - Immutable underlying string values (zero transformation or leakage).

### C. Android Native Contacts / Phonebook Picker (Gap C)
* **Contacts Helper & Multi-Number Dialog:**
  - Created [ContactPickerHelper.kt](file:///e:/App/Sucharu%20Pro/app/src/main/java/com/sucharu/sucharupro/ui/features/auth/components/ContactPickerHelper.kt) to extract phone numbers from picked contact URIs.
  - Added `SelectContactPhoneDialog` to prompt the user if a contact has multiple phone numbers (e.g. Mobile, Work, Home).
* **Permission & Fallback:**
  - Added `<uses-permission android:name="android.permission.READ_CONTACTS" />` to [AndroidManifest.xml](file:///e:/App/Sucharu%20Pro/app/src/main/AndroidManifest.xml).
  - Clean runtime permission handling in [RegisterScreen.kt](file:///e:/App/Sucharu%20Pro/app/src/main/java/com/sucharu/sucharupro/ui/features/auth/RegisterScreen.kt). If denied, non-blocking notice is shown and manual entry remains fully available.
* **Privacy & Data Minimization:**
  - Zero bulk reading or uploading of the contact address book.
  - Only the user-selected contact's phone number is extracted, normalized, and placed into the form.

---

## 3. Verification & Test Execution Results

| Test Suite | Module | Test Count | Result |
| :--- | :--- | :--- | :--- |
| `AuthenticationRegistrationEndToEndTest` | `:app` | 5 | **PASSED** |
| `ContactPickerAndValidationTest` | `:app` | 5 | **PASSED** |
| `PasswordVisibilityUnitTest` | `:app` | 3 | **PASSED** |
| `PostgresAuthenticationApiEndToEndTest` | `:app` | 4 | **PASSED** |
| Complete `:app:testDebugUnitTest` | `:app` | All | **PASSED** |
| Full Regression `:core:test` | `:core` | All | **PASSED** |
| Full Regression `:backend:test` | `:backend` | All | **PASSED** |
| Packaging Verification `:backend:jar` | `:backend` | 1 | **PASSED** |

---

## 4. Modified & Created Files Summary

* **Modified Files:**
  - [BackendRouter.kt](file:///e:/App/Sucharu%20Pro/core/src/main/java/com/sucharu/sucharupro/data/api/server/BackendRouter.kt): Added `/api/v1/auth/register`, `/api/v1/auth/password/recovery/request`, and `/api/v1/auth/password/recovery/confirm` routes.
  - [AuthenticationService.kt](file:///e:/App/Sucharu%20Pro/core/src/main/java/com/sucharu/sucharupro/data/auth/service/AuthenticationService.kt): Normalized phone numbers and enhanced duplicate detection across email and phone.
  - [RegisterScreen.kt](file:///e:/App/Sucharu%20Pro/app/src/main/java/com/sucharu/sucharupro/ui/features/auth/RegisterScreen.kt): Added password/confirm-password visibility toggles, contacts picker flow, and client validation feedback.
  - [LoginScreen.kt](file:///e:/App/Sucharu%20Pro/app/src/main/java/com/sucharu/sucharupro/ui/features/auth/LoginScreen.kt): Added password visibility toggle and `successMessage` notification support.
  - [ResetPasswordScreen.kt](file:///e:/App/Sucharu%20Pro/app/src/main/java/com/sucharu/sucharupro/ui/features/auth/ResetPasswordScreen.kt): Added independent password visibility toggles.
  - [VendorPortalEntryScreen.kt](file:///e:/App/Sucharu%20Pro/app/src/main/java/com/sucharu/sucharupro/ui/features/vendorportal/VendorPortalEntryScreen.kt): Added password visibility toggle.
  - [SucharuGraphicsAppShell.kt](file:///e:/App/Sucharu%20Pro/app/src/main/java/com/sucharu/sucharupro/ui/shell/SucharuGraphicsAppShell.kt): Wired success messaging and screen transitions.
  - [AndroidManifest.xml](file:///e:/App/Sucharu%20Pro/app/src/main/AndroidManifest.xml): Declared `android.permission.READ_CONTACTS`.
  - [BusinessCostManagementScreen.kt](file:///e:/App/Sucharu%20Pro/app/src/main/java/com/sucharu/sucharupro/ui/features/cost/BusinessCostManagementScreen.kt): Added missing model import and resolved smart cast warnings.
* **New Files:**
  - [ContactPickerHelper.kt](file:///e:/App/Sucharu%20Pro/app/src/main/java/com/sucharu/sucharupro/ui/features/auth/components/ContactPickerHelper.kt): Extraction helper and `SelectContactPhoneDialog`.
  - [AuthenticationRegistrationEndToEndTest.kt](file:///e:/App/Sucharu%20Pro/app/src/test/java/com/sucharu/sucharupro/ui/auth/AuthenticationRegistrationEndToEndTest.kt): End-to-end registration flow and security test suite.
  - [ContactPickerAndValidationTest.kt](file:///e:/App/Sucharu%20Pro/app/src/test/java/com/sucharu/sucharupro/ui/auth/ContactPickerAndValidationTest.kt): Phone normalization and validation tests.
  - [PasswordVisibilityUnitTest.kt](file:///e:/App/Sucharu%20Pro/app/src/test/java/com/sucharu/sucharupro/ui/auth/PasswordVisibilityUnitTest.kt): Password visual transformation state tests.
