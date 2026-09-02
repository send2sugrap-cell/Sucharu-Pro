# SUCHARU PRO — POSTGRESQL PRODUCTION OPERATIONS RUNBOOK
**Document**: `docs/postgresql-production-runbook.md`  
**Stage**: `INFRA-02 → STEP 03`  
**Classification**: Production Operational Runbook  

---

## 1. Routine Lifecycle Operations

### A. Starting Database Services
```bash
docker compose -f deploy/docker-compose.yml up -d postgres
```
Verify readiness probe:
```bash
curl -f http://localhost:8080/health/readiness
```

### B. Stopping Services Gracefully
```bash
docker compose -f deploy/docker-compose.yml stop backend
# Wait for active in-flight transactions to drain (15s)
docker compose -f deploy/docker-compose.yml stop postgres
```

### C. Executing Database Migrations
```bash
./gradlew.bat flywayMigrate -Dflyway.url=jdbc:postgresql://$DATABASE_HOST:$DATABASE_PORT/$DATABASE_NAME \
  -Dflyway.user=$MIGRATION_USER -Dflyway.password=$MIGRATION_PASSWORD
```

---

## 2. Emergency Incident Response

### D. Connection Pool Exhaustion (`DATABASE_POOL_EXHAUSTED`)
1. **Symptoms**: Spike in acquisition timeouts (`Timeout acquiring PostgreSQL connection`).
2. **Immediate Action**:
   - Inspect active queries:
     ```sql
     SELECT pid, now() - query_start AS duration, query, state 
     FROM pg_stat_activity 
     WHERE state != 'idle' 
     ORDER BY duration DESC;
     ```
   - Terminate rogue long-running transactions:
     ```sql
     SELECT pg_terminate_backend(<pid>);
     ```
   - Scale `DATABASE_POOL_SIZE` or tune connection timeouts if traffic is legitimately high.

### E. Health Check Failure (`DATABASE_HEALTH_FAILURE`)
1. Verify PostgreSQL container status: `docker ps --filter name=sucharu_postgres`.
2. Inspect server error logs: `docker logs --tail 100 sucharu_postgres`.
3. Check disk space and memory constraints on the database host.

### F. Migration Failure on Deployment (`DATABASE_MIGRATION_FAILED`)
> [!CAUTION]
> NEVER edit an already-applied migration script directly in production.
1. Check `flyway_schema_history` table for the failing record:
   ```sql
   SELECT * FROM flyway_schema_history WHERE success = false;
   ```
2. Repair schema lock if migration was interrupted:
   ```bash
   ./gradlew.bat flywayRepair
   ```
3. Prepare a forward-fix migration (`V<timestamp>__fix_issue.sql`) and re-run.

---

## 3. Backup & Disaster Recovery Procedures

### G. Manual Logical Backup
```bash
pg_dump -h $DATABASE_HOST -p $DATABASE_PORT -U $DATABASE_USER -d $DATABASE_NAME \
  -F c -b -v -f "/backups/sucharu_pro_backup_$(date +%Y%m%d_%H%M%S).dump"
```

### H. Restore to Fresh Database Instance
> [!WARNING]
> Restoring a database overwrites existing data. Perform only on recovery target.
```bash
# 1. Create fresh database target
createdb -h $DATABASE_HOST -p $DATABASE_PORT -U postgres sucharu_pro_restored

# 2. Restore schema and data
pg_restore -h $DATABASE_HOST -p $DATABASE_PORT -U postgres -d sucharu_pro_restored -v "/backups/target_backup.dump"

# 3. Verify row counts and integrity
# Run verification probe:
SELECT table_name, count FROM (
  SELECT 'customers' AS table_name, count(*) AS count FROM customers
  UNION ALL
  SELECT 'orders', count(*) FROM orders
) t;
```
