# MODULE 12 — STEP 02 IMPLEMENTATION REPORT
## Vendor Profile, Services & Capability Management

---

### Executive Summary

- **Module**: 12 — Vendor Management
- **Step**: 02 of 10 — Vendor Profile, Services & Capability Management
- **Status**: **COMPLETED & VERIFIED (100% Green)**
- **Total Test Suite**: **3,077 tests executed across `:core` and `:backend` (0 failures, 0 errors, 0 skipped)**
- **Artifacts Generated**:
  - Domain Models & Enums (`VendorProfile`, `VendorContact`, `VendorAddress`, `VendorCapability`, `ContactType`, `AddressType`, `CapabilityType`, `CapabilityStatus`)
  - Validators (`VendorProfileValidator`, `VendorContactValidator`, `VendorAddressValidator`, `VendorCapabilityValidator`)
  - Domain Repositories & Domain Services (`VendorProfileService`, `VendorContactService`, `VendorAddressService`, `VendorCapabilityService`)
  - Flyway Migration `V20260916__create_vendor_profile_and_capabilities.sql` (with `FORCE ROW LEVEL SECURITY`)
  - Data Sources (`Postgres...` and `Fake...` implementations with optimistic locking)
  - REST DTOs (`VendorDtos.kt`), Use Cases (`BackendUseCases.kt`), and REST Router endpoints (`BackendRouter.kt`)
  - Comprehensive Test Suite (`VendorProfileDomainTest`, `VendorProfileValidatorTest`, `VendorProfileRepositoryTest`, `VendorCapabilityServiceTest`, `VendorProfileTenantIsolationTest`, `VendorProfileSecurityEdgeTest`)

---

### 1. Architectural Architecture & Invariants Enforced

1. **Clean Aggregate Boundary**:
   - `Vendor` aggregate root retains master identity (`vendorCode`, `status`, `vendorName`).
   - `VendorProfile` extends business profile attributes (legal name, contact person, phones, email, website, tax ID, registration number, notes) without duplicating master lifecycle status or vendor codes.
   - `VendorContact` manages multiple typed contact persons with designation, phone, email, and primary flags.
   - `VendorAddress` manages structured physical/operating addresses across types (`OFFICE`, `FACTORY`, `WAREHOUSE`, `REGISTERED`, `BILLING`, `DELIVERY`).
   - `VendorCapability` manages specialized technical/production capabilities (`PRINTING`, `CTP`, `PLATE_MAKING`, `LAMINATION`, `FOILING`, `SPOT_UV`, `DIE_CUTTING`, `PERFORATION`, `FOLDING`, `BINDING`, `PERFECT_BINDING`, `SADDLE_STITCH`, `HARD_BINDING`, `PACKAGING`, `CUTTING`, `TRIMMING`, `TRANSPORT`, `DELIVERY`, `MAINTENANCE`, `OTHER`).

2. **Tenant Isolation & Security**:
   - Database tables `vendor_profiles`, `vendor_contacts`, `vendor_addresses`, and `vendor_capabilities` have foreign key constraints back to `vendors(project_id, vendor_id)` with `ON DELETE CASCADE`.
   - PostgreSQL `FORCE ROW LEVEL SECURITY` with restrictive policies ensuring strict multi-tenant isolation.
   - RBAC enforced via `BackendAuthorizationPolicy`: `ADMIN`, `MANAGER`, and `STAFF` have access to vendor operations; `CUSTOMER` and `AFFILIATE` are strictly forbidden (403).

3. **Optimistic Concurrency & Auditability**:
   - Every mutation updates `version = version + 1`, `updated_at`, and `updated_by`.
   - Stale concurrent modifications are rejected with concurrency conflict exceptions.

4. **Business Validation Rules**:
   - Archived vendors cannot receive new profiles, contacts, addresses, or capabilities.
   - Duplicate active capabilities for the same `(project_id, vendor_id, capability_type)` are strictly rejected.
   - Quick lookup helper `hasCapability(projectId, vendorId, capabilityType)` returns active, selectable capability status for downstream job assignment (Step 04).

---

### 2. REST API Endpoints Implemented

| Method | Route | Description |
|---|---|---|
| `GET` | `/api/v1/vendors/{vendorId}/profile` | Retrieve vendor business profile |
| `PUT` | `/api/v1/vendors/{vendorId}/profile` | Upsert/update vendor business profile |
| `GET` | `/api/v1/vendors/{vendorId}/contacts` | List contacts for a vendor |
| `POST` | `/api/v1/vendors/{vendorId}/contacts` | Create a new vendor contact |
| `PUT` | `/api/v1/vendors/{vendorId}/contacts/{contactId}` | Update contact details |
| `PATCH` | `/api/v1/vendors/{vendorId}/contacts/{contactId}/status` | Activate / deactivate a contact |
| `GET` | `/api/v1/vendors/{vendorId}/addresses` | List addresses for a vendor |
| `POST` | `/api/v1/vendors/{vendorId}/addresses` | Create a new vendor address |
| `PUT` | `/api/v1/vendors/{vendorId}/addresses/{addressId}` | Update address details |
| `PATCH` | `/api/v1/vendors/{vendorId}/addresses/{addressId}/status` | Activate / deactivate an address |
| `GET` | `/api/v1/vendors/{vendorId}/capabilities` | List capabilities of a vendor |
| `POST` | `/api/v1/vendors/{vendorId}/capabilities` | Register a new capability for a vendor |
| `GET` | `/api/v1/vendors/{vendorId}/capabilities/{capabilityId}` | Retrieve a specific capability record |
| `PATCH` | `/api/v1/vendors/{vendorId}/capabilities/{capabilityId}` | Update capability details / status |
| `GET` | `/api/v1/vendor-capabilities/{capabilityType}/vendors` | Discover vendors possessing a specific capability |

---

### 3. Verification & Test Metrics

- **Core Module Tests**: 2,977 PASSED
- **Backend Module Tests**: 100 PASSED
- **Total Tests**: **3,077 PASSED (0 failures, 0 errors, 0 skipped)**
- **Build Outcome**: `:core:jar`, `:backend:jar` built cleanly with zero compilation errors.
