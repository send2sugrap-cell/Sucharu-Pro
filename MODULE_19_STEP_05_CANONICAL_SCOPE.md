# MODULE 19 STEP 05 — CANONICAL SCOPE SPECIFICATION

## Job Cancellation, Revision & Substrate Release Governance

---

### 1. Scope Boundary

Module 19 Step 05 governs reservation release and delta decisions when upstream orders or jobs are:
- Cancelled (Order or Job level)
- Revised (Quantity reduction, increase, or SKU change)
- Rescheduled / Reallocated

### 2. Operational Rules

1. **Non-destructive Evaluation**: Historical reservation decisions and allocations are never overwritten destructively.
2. **Floor Commitment Protection**: Material consumed or operationally committed to active production runs cannot be released.
3. **Delta Re-reservation**: Quantity increases produce an additional requirement that feeds into Step 01 / Step 02 / Step 03 without creating a separate reservation authority.
4. **Segregation of Duties**: Evaluation $\neq$ Approval $\neq$ Execution.

### 3. Cross-Module Authorities

| Domain | Authority | Module 19 Step 05 Boundary |
|---|---|---|
| Order Lifecycle | Module 03 | Reads cancellation/revision triggers |
| Production Lifecycle | Module 17 | Respects floor commitments & status |
| Physical Stock & Ledger | Module 06 | Releases reservations via Step 02 interlock |
| Reservation Governance | Module 19 | Evaluates, approves, and governs release lifecycle |
