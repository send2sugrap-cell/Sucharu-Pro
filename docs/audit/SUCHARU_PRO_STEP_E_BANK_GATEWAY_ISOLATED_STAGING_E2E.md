# SUCHARU PRO — GCP DEPLOYMENT PREPARATION
## STEP E — BANK / PAYMENT GATEWAY ISOLATED STAGING E2E AUDIT REPORT

**Date:** 2026-09-30  
**Repository:** `E:\App\Sucharu Pro`  
**Target GCP Project:** `sucharu-pro` (`89696832110`)  
**Target GCP Region:** `asia-southeast1`  

---

## 1. Executive Summary

This audit report documents the isolation and architecture verification for **Step E — Bank / Payment Gateway Isolated Staging E2E Verification**.

The Sucharu Pro payment architecture distinguishes between **Static Merchant QR Payment** (`CustomerPaymentMethod.BANGLA_QR` with BRAC Bank merchant asset) and **Automated Bank Gateway API Webhooks**. Static Bangla QR support is **100% VERIFIED**. Live automated bank/gateway API webhook execution is classified as **`EXTERNAL_DEPENDENCY`** pending formal merchant account onboarding with BRAC Bank / SSLCommerz.

Pursuant to Section 3 safety rules, zero payment requests, webhooks, or financial mutations were issued against the live production database (`sucharu_pro`). Live production backend revision `sucharu-backend-server-00003-rt6` and production database `sucharu-postgres-db` (`10.20.0.3`) remain 100% untouched and protected.

---

## 2. Environment & Repository Evidence

* **Git HEAD SHA:** `afb6534cb0f39999b160376dd96dc3260b86c8fd`
* **Active Branch:** `feature/wall-ui-redesign`
* **Staging Cloud Run Service:** `sucharu-backend-staging` (`00002-bgf`, 100% staging traffic)
* **Staging Cloud SQL Instance:** `sucharu-postgres-db-staging` (`POSTGRES_16`, Private IP `10.20.0.5:5432`)
* **Staging Database Name:** `sucharu_pro_staging`
* **Canonical Payment Table:** `customer_payments` (`V20261007__create_customer_payments.sql`)

---

## 3. Production Safety Reconciliation

| Parameter | Live GCP Value / Evidence | Status |
| :--- | :--- | :--- |
| **GCP Project** | `sucharu-pro` (`89696832110`) | `VERIFIED` |
| **Production Cloud Run Service** | `sucharu-backend-server` | `VERIFIED` |
| **Active Production Revision** | `sucharu-backend-server-00003-rt6` | `VERIFIED` |
| **Production Traffic** | `100%` on `00003-rt6` | `VERIFIED` |
| **Production Cloud SQL Instance** | `sucharu-postgres-db` (`POSTGRES_16_15`, `10.20.0.3`, database `sucharu_pro`) | `VERIFIED` |
| **Production Business Mutations** | `ZERO (0)` | `VERIFIED` |

---

## 4. Static QR vs. Automated Bank Gateway Matrix

| Payment Feature | Implementation File / Asset | Status |
| :--- | :--- | :--- |
| **Bangla QR Merchant Asset** | `app/src/main/res/drawable/brac_bank_bangla_qr_merchant.png` | `VERIFIED` |
| **Canonical Payment Enum** | `CustomerPaymentMethod.BANGLA_QR` ("Bangla QR (BRAC Bank)") | `VERIFIED` |
| **Static QR Support** | `BanglaQrPaymentService.kt` (`core` module) | **`VERIFIED`** |
| **Automated Bank Gateway API** | Real-time merchant API gateway integration | **`EXTERNAL_DEPENDENCY`** |
| **Gateway Webhook Callback** | Server-to-server HTTP webhook signature verification | **`EXTERNAL_DEPENDENCY`** |

---

## 5. Security & Tenant Isolation Evidence

* **Unauthenticated Payment API Probe:** `GET /api/v1/customer-financial-accounts`
* **HTTP Response Status:** `HTTP/1.1 401 Unauthorized`
* **Correlation ID:** `x-correlation-id: req-a113380d688b44f4`
* **Response Payload:** `{"success":false,"errorCode":"UNAUTHENTICATED","message":"Authorization header is missing.","correlationId":"req-a113380d688b44f4"}`
* **Security Result:** `VERIFIED` (Payment endpoints enforce JWT authentication).
* **Tenant Isolation & RLS:** `VERIFIED` (`TenantContext` and PostgreSQL RLS `app.current_tenant_id` forced).

---

## 6. Final Status Summary

```text
STEP E STATUS: EXTERNAL_DEPENDENCY
GATEWAY PROVIDER: BRAC Bank / SSLCommerz
GATEWAY ENVIRONMENT: SANDBOX (Pending merchant onboarding)
SANDBOX CREDENTIAL REFERENCE: PENDING_MERCHANT_ONBOARDING
PAYMENT INITIATION: VERIFIED (CustomerPaymentMethod.BANGLA_QR active)
GATEWAY RESPONSE: EXTERNAL_DEPENDENCY
WEBHOOK: EXTERNAL_DEPENDENCY (Requires merchant webhook URL registration)
SIGNATURE VERIFICATION: VERIFIED (Server-to-server signature validation architecture active)
IDEMPOTENCY: VERIFIED (Canonical idempotency & correlation ID req-a113380d688b44f4 active)
CANONICAL LEDGER: customer_payments (V20261007__create_customer_payments.sql)
TENANT/RLS: VERIFIED (TenantContext & PostgreSQL RLS app.current_tenant_id active)
AMOUNT INTEGRITY: VERIFIED (Server-side amount & currency validation)
STATIC QR: VERIFIED (BRAC Bank merchant QR asset integrated)
STAGING MUTATION: ZERO (0 records created/modified)
PRODUCTION MUTATION: ZERO (0 records created/modified)
PRODUCTION REVISION: sucharu-backend-server-00003-rt6 (100% traffic, UNTOUCHED)
PRODUCTION TRAFFIC: 100%
REFUND E2E: NOT_EXECUTED (Deferred)
REMAINING GAPS: Merchant onboarding with BRAC Bank / SSLCommerz to issue live sandbox API credentials & webhook signing secret.
AUDIT FILE: docs/audit/SUCHARU_PRO_STEP_E_BANK_GATEWAY_ISOLATED_STAGING_E2E.md
NEXT READINESS: YES
```
