# Sucharu Pro ERP — Rollback & Disaster Recovery Runbook

**Document Version**: `1.0.0`  
**Milestone**: `INFRA-05 — Production Backend Runtime, API Edge & External Integration Platform`  
**Classification**: `Restricted / Release Engineering`  

---

## 1. Rollback Philosophy
Sucharu Pro distinguishes between **Application Rollback** and **Database Migration Rollback**:
1. **Application Code**: Versioned, immutable Docker images and fat JARs allow instantaneous rollback to previous tags.
2. **Database Schema**: Forward-compatible migrations (*Expand-Contract pattern*). Destructive database schema rollbacks are strictly prohibited in production without full point-in-time recovery (PITR).

---

## 2. Application Rollback Procedure (Zero Downtime)

### Step 1: Identify Last Known Good Image/Tag
```bash
docker images | grep sucharu-backend
# E.g. sucharu-backend:1.0.0-PROD (new, failing) vs sucharu-backend:0.9.9-PROD (previous stable)
```

### Step 2: Update Image Tag in Environment
```bash
sed -i 's/sucharu-backend:1.0.0/sucharu-backend:0.9.9/g' deploy/docker-compose.production.yml
```

### Step 3: Rolling Update with Healthcheck Validation
```bash
docker compose -f deploy/docker-compose.production.yml up -d --no-deps backend
```

### Step 4: Validate Health & Readiness
```bash
curl -f http://localhost:8080/ready || exit 1
```

---

## 3. Database Migration Disaster Recovery
If an unrecoverable schema issue occurs:
1. Stop backend service.
2. Perform Point-In-Time Recovery (PITR) or restore from latest pre-deployment backup dump.
3. Validate Flyway history table:
   ```sql
   SELECT installed_rank, version, description, success FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;
   ```
4. Re-verify Row-Level Security (RLS) enforcement across all tables:
   ```sql
   SELECT relname, relrowsecurity, relforcerowsecurity FROM pg_class WHERE relnamespace = 'public'::regnamespace;
   ```
5. Resume backend service with matching compatible release version.
