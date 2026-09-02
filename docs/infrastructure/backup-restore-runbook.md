# Sucharu Pro ERP — PostgreSQL Backup & Restore Runbook

**Document Version**: `1.0.0`  
**Milestone**: `INFRA-05 — Production Backend Runtime, API Edge & External Integration Platform`  
**Classification**: `Restricted / Database Administration`  

---

## 1. Backup Strategy
- **Backup Type**: Logical custom-format archive (`-Fc`) using `pg_dump`.
- **Frequency**: Daily automated full backup; hourly WAL archiving in managed environments.
- **Retention**: 30 days locally, 90 days off-site in encrypted object storage.
- **Zero Secret Exposure**: Database password must NEVER be placed in script arguments or commit history; always passed via `PGPASSWORD` or `~/.pgpass`.

---

## 2. Automated Backup Execution

### Running Backup (Linux / macOS)
```bash
export PGPASSWORD="your_secure_db_password"
export DB_HOST="postgres-cluster.internal"
export DB_NAME="sucharu_pro_db"
export DB_USER="sucharu_app"
./deploy/scripts/backup-db.sh
```

### Running Backup (Windows PowerShell)
```powershell
$env:PGPASSWORD = "your_secure_db_password"
.\deploy\scripts\backup-db.ps1 -Host "postgres-cluster.internal" -Database "sucharu_pro_db" -User "sucharu_app"
```

---

## 3. Backup Verification
Before archiving, verify dump readability:
```bash
./deploy/scripts/verify-backup.sh backups/sucharu_pro_db_20260825_120000.dump
```

---

## 4. Disaster Recovery & Restore Procedure

### Step 1: Drain Inbound Traffic
Place Nginx into maintenance mode or stop the backend container:
```bash
docker compose -f deploy/docker-compose.production.yml stop backend
```

### Step 2: Restore from Verified Dump
```bash
export PGPASSWORD="your_secure_db_password"
./deploy/scripts/restore-db.sh backups/sucharu_pro_db_20260825_120000.dump
```

### Step 3: Verify PostgreSQL RLS & Schema Integrity
```sql
SELECT tablename, rowsecurity FROM pg_tables WHERE schemaname = 'public';
SELECT count(*) FROM flyway_schema_history;
```

### Step 4: Restart Backend & Verify Readiness
```bash
docker compose -f deploy/docker-compose.production.yml start backend
curl -i http://localhost/ready
```
