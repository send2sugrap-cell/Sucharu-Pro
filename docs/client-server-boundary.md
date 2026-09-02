# SUCHARU PRO — CLIENT-SERVER PERSISTENCE BOUNDARY SPECIFICATION
**Document**: `docs/client-server-boundary.md`  
**Stage**: `INFRA-02 → STEP 04`  
**Classification**: System Integration Specification  

---

## 1. Architectural Boundary Rules

1. **No Direct Database Connectivity from Android**:
   - The Android mobile application package (`.apk` / `.aab`) must never contain JDBC connection strings, database usernames, or PostgreSQL master passwords.
   - All persistence interactions must traverse the backend API over HTTPS.

2. **Unified Product Ecosystem**:
   - Customer Portal, Affiliate Portal, and future Staff/Manager/Admin workspaces interact with a single, unified backend API platform.
   - Role permissions govern endpoint accessibility.

3. **In-Memory Testing Preservation**:
   - In-memory `FakeDataSource` fixtures remain fully available for local Android UI preview, unit tests, and offline mocking.

4. **Future AI Agent Compatibility**:
   - Future AI tools (e.g. n8n workflows, Sucharu AI Agent) will call the same authenticated API endpoints and execute through the same Use Case and Repository layers, ensuring consistent auditability and tenant isolation.
