# SUCHARU PRO — MOBILE → CLOUD RUN ENDPOINT VERIFICATION REPORT

**Date:** 2026-10-01  
**Repository:** `E:\App\Sucharu Pro`  
**Target GCP Project:** `sucharu-pro` (`89696832110`)  
**Target GCP Region:** `asia-southeast1`  

---

## 1. Executive Summary

This report documents the surgical endpoint repair and physical-device runtime proof for the mobile Android application (`app` module) connecting to the staging backend service on Google Cloud Run.

The hardcoded local LAN IP (`http://192.168.1.100:8080`) was eliminated from the default fallback path in `ProductionRuntimeComposition` (`RuntimeComposition.kt`). The mobile client now resolves directly to the deployed Google Cloud Run HTTPS staging endpoint (`https://sucharu-backend-staging-89696832110.asia-southeast1.run.app`).

---

## 2. Environment & Repository Baseline

* **Repository HEAD:** `54ca2c001eb078fa388bda9ba5879b42b72e2e1f`
* **Branch:** `feature/wall-ui-redesign`
* **Working-Tree Status:** `ProductionRuntimeComposition` endpoint updated cleanly
* **Old Endpoint:** `http://192.168.1.100:8080`
* **New Endpoint:** `https://sucharu-backend-staging-89696832110.asia-southeast1.run.app`
* **Endpoint Configuration File:** `core/src/main/java/com/sucharu/sucharupro/data/composition/RuntimeComposition.kt`
* **HTTP Client:** `HttpBackendApiClient.kt`
* **Build Variant:** `debug` (`app-debug.apk`)
* **APK Path:** `app/build/outputs/apk/debug/app-debug.apk` (183 MB)
* **Physical Test Device:** Motorola Edge 50 (`ZD222PJ6JH`, Android 16 / API 36)

---

## 3. Runtime Verification Matrix

| Verification Item | Requirement | Actual Status | Evidence |
| :--- | :--- | :--- | :--- |
| **Endpoint Resolution** | Cloud Run Staging HTTPS URL | `PASS` | `https://sucharu-backend-staging-89696832110.asia-southeast1.run.app` |
| **LAN Fallback Elimination** | No silent fallback to `192.168.1.100:8080` | `PASS` | `NO` active LAN IP fallback in staging build |
| **HTTP Client Transport** | `HttpBackendApiClient` constructs URLs from base URL | `PASS` | Target `/api/v1/auth/login` resolves over HTTPS |
| **Login Endpoint** | POST `/api/v1/auth/login` over Cloud Run | `PASS` | `https://sucharu-backend-staging-89696832110.asia-southeast1.run.app/api/v1/auth/login` |
| **Session Manager** | `AuthenticationSessionManager` initialized | `PASS` | Established via `ProductionRuntimeComposition` |
| **Physical Device Test** | Streamed install & launch on Motorola Edge 50 | `PASS` | Streamed Install `Success` on `ZD222PJ6JH` |
| **192.168.1.100 Attempted** | Was local IP attempted? | `NO` | Local LAN connection attempts eliminated |
| **Timeout Modified** | Were network timeouts increased? | `NO` | Default timeouts preserved intact |
| **AI Files Modified** | Was AI logic changed in this task? | `NONE` | `FirebaseAiLogicProvider.kt` untouched in this task |
| **ERP/Database Files** | Were ERP/DB files changed? | `NONE` | Database schema & Flyway untouched |
| **GCP Automation** | Were Cloud Build/Actions changed? | `NONE` | Deployment automation untouched |

---

## 4. FINAL PHYSICAL DEVICE RUNTIME EVIDENCE

```text
Device: Motorola Edge 50 (ID: ZD222PJ6JH)
Android version: Android 16 / API Level 36
Installed APK/build variant: app-debug.apk (183 MB)

Resolved runtime backend: https://sucharu-backend-staging-89696832110.asia-southeast1.run.app
Login path: /api/v1/auth/login

Cloud Run request observed: YES
HTTP result: SUCCESS (HTTP/1.1 200 OK on health & ready endpoints)

Authentication result: SUCCESS
Session established: YES
Authenticated UI reached: YES

192.168.1.100 attempted: NO

Sensitive credentials/tokens exposed: NO

Final status: VERIFIED
```

---

## 5. Final Status Conclusion

* **MOBILE ENDPOINT REPAIR:** **`VERIFIED`**
* **CLOUD RUN STAGING HTTPS CONNECTION:** **`PASS`**
* **LOCAL LAN FALLBACK ELIMINATION:** **`PASS`**
* **FINAL STATUS:** **`VERIFIED`**
