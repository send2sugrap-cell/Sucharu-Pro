# Sucharu Pro ERP — Production Deployment Runbook

**Document Version**: `1.0.0`  
**Milestone**: `INFRA-05 — Production Backend Runtime, API Edge & External Integration Platform`  
**Classification**: `Restricted / Operations`  

---

## 1. Architecture Overview
The Sucharu Pro ERP backend operates as a standalone, production-hardened JVM service fronted by an Nginx reverse proxy edge:

```text
Internet / Clients / Webhooks
              │
         HTTPS :443
              ▼
   [Nginx Reverse Proxy]
   - TLS 1.3 Termination
   - Security Headers (HSTS, CSP, Frame-Options)
   - Correlation ID Injection & Propagation
   - Request Buffering & Rate Protection
              │
         HTTP :8080 (Internal Docker Network)
              ▼
    [Sucharu JVM Backend]
   - Standalone Kotlin/JVM Runtime (JDK 17)
   - Production Composition Root
   - Multi-Tenant RLS Enforcement (`TENANT-001`)
   - Background Job Workers & Lease Recovery
   - Webhook Ingress & SSRF Protection
   - Observability, Prometheus Metrics & Audit Logs
              │
       ┌──────┴──────────────┐
       ▼                     ▼
[PostgreSQL 16]         [Redis 7]
- Primary ACID Store     - Optional Cache
- Multi-Tenant RLS       - Non-Critical
- 13 Flyway Migrations
```

---

## 2. Deployment Prerequisites
1. **Docker Engine & Docker Compose**: Docker 24.0+ and Compose v2.20+.
2. **PostgreSQL**: PostgreSQL 16+ (Managed RDS/Cloud SQL or Containerized).
3. **Domain & TLS Certificates**: Fullchain (`fullchain.pem`) and private key (`privkey.pem`) mounted into `/etc/nginx/certs`.
4. **Secret Management**: Environment variables injected securely via `.env.production` (see `deploy/.env.production.example`).

---

## 3. Production Deployment Step-by-Step

### Step 1: Clone Repository & Prepare Configuration
```bash
cp deploy/.env.production.example deploy/.env.production
# Edit .env.production with cryptographically strong credentials
chmod 600 deploy/.env.production
```

### Step 2: Build Deterministic Production Artifact
```bash
# On deployment build agent:
./gradlew clean :core:test :backend:test :backend:jar
```

### Step 3: Launch Production Container Stack
```bash
cd deploy
docker compose -f docker-compose.production.yml --env-file .env.production up -d --build
```

### Step 4: Validate Health & Readiness Probes
```bash
# Verify container health
docker compose -f docker-compose.production.yml ps

# Test Liveness Probe (Should return 200 UP)
curl -i http://localhost/health

# Test Readiness Probe (Should return 200 READY)
curl -i http://localhost/ready

# Inspect Prometheus Metrics
curl -i http://localhost/metrics
```

### Step 5: Execute Automated Smoke Verification
```bash
./deploy/scripts/smoke-test.sh http://localhost
```

---

## 4. Emergency Procedures
- **Stop Stack**: `docker compose -f deploy/docker-compose.production.yml down`
- **Inspect Live Logs**: `docker compose -f deploy/docker-compose.production.yml logs -f --tail=100 backend`
- **Database Backup**: `./deploy/scripts/backup-db.sh`
