# User Profile Security & Concurrency Specification

## 1. Overview
This document outlines profile management, demographic data security, tenant isolation, and Optimistic Concurrency Control (OCC) for user profiles in **Sucharu Pro — Commercial Printing ERP**.

---

## 2. Profile Data Model & Multi-Tenant Isolation

### 2.1 Database Schema (`user_profiles`)
```sql
CREATE TABLE IF NOT EXISTS user_profiles (
    profile_id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL UNIQUE REFERENCES user_identities(user_id) ON DELETE CASCADE,
    tenant_id VARCHAR(36) NOT NULL REFERENCES tenants(tenant_id) ON DELETE CASCADE,
    full_name VARCHAR(150) NOT NULL,
    phone_number VARCHAR(30),
    avatar_url VARCHAR(500),
    language VARCHAR(10) DEFAULT 'en',
    timezone VARCHAR(50) DEFAULT 'UTC',
    preferred_currency VARCHAR(10) DEFAULT 'BDT',
    custom_attributes JSONB DEFAULT '{}'::jsonb,
    version INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

### 2.2 PostgreSQL Row-Level Security (RLS)
- RLS policy `user_profiles_tenant_isolation_policy` enforces:
  `tenant_id = current_setting('app.current_project_id')`
- Prevents cross-tenant profile reads, updates, or leakage even if SQL injection occurs at application level.

---

## 3. Optimistic Concurrency Control (OCC)

### 3.1 Motivation
In high-concurrency ERP environments, multiple client applications (Android mobile, Web dashboard, API integrations) may update user profile attributes simultaneously. Without OCC, lost updates (where a slow write overwrites a faster write) would corrupt demographic data.

### 3.2 Protocol Implementation
1. **Read Profile (`GET /api/v1/auth/profile`)**: Returns current `UserProfile` including integer `version` field.
2. **Update Profile (`PUT /api/v1/auth/profile`)**: Request DTO must include current `expectedVersion`.
3. **Database Mutation**:
   ```sql
   UPDATE user_profiles
   SET full_name = ?, phone_number = ?, ..., version = version + 1, updated_at = CURRENT_TIMESTAMP
   WHERE user_id = ? AND version = ? AND tenant_id = current_setting('app.current_project_id');
   ```
4. **OCC Failure**: If no row is updated (indicating `version` mismatch), `UserIdentityService` throws `OptimisticConcurrencyException("Profile update failed due to concurrent modification. Expected version: $expectedVersion")` resulting in a `409 Conflict` HTTP response.
