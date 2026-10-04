# CHANGE REQUEST TEMPLATE — SUCHARU PRO ERP

## 1. Change Identification
* **Change ID:** `CR-YYYYMMDD-NNN`
* **Title:** [Brief Change Title]
* **Requestor:** [Name / Role]
* **Date:** YYYY-MM-DD
* **Classification:** [Change Request / Bug Fix / Security Fix / Performance / Infrastructure]

---

## 2. Request & Business Justification
* **What is changing?** [Clear description of the requested modification]
* **Why is it changing?** [Business justification or defect description]

---

## 3. Scope & Boundaries
* **In Scope (Files/Modules Allowed to Change):**
  - [Module / File Path 1]
  - [Module / File Path 2]
* **Explicitly Out of Scope (Prohibited Changes):**
  - [Module / File Path 1]
  - [Module / File Path 2]

---

## 4. Baseline Dependencies & Impacted Layers
* **Protected Baseline Areas Affected:** [None / List affected baseline areas]
* **Impacted Architectural Layers:**
  - [ ] UI / Jetpack Compose
  - [ ] ViewModel / State
  - [ ] API / Backend Router
  - [ ] Backend Domain Service
  - [ ] Repository / DataSource
  - [ ] Database / Flyway DDL / RLS
  - [ ] GCP Cloud Infrastructure
  - [ ] AI Agent / Orchestration / Gemini LLM
  - [ ] Security / Auth / RBAC

---

## 5. Implementation Evidence
* **Git Commit SHA:** `[Commit Hash]`
* **Files Changed:**
  - [File 1]
  - [File 2]
* **Build Command Execution:** `gradle_build("app:assembleDebug")` → `BUILD SUCCESSFUL`
* **Test Suite Execution:** [Test Name] → `PASS`
* **Runtime Verification Evidence:** [Cloud Run Health Probe / Physical Device Logcat Evidence]

---

## 6. Regression Evidence
* **Affected Baseline Journeys Re-Tested:**
  - [ ] Customer Journey
  - [ ] Admin Journey
  - [ ] Affiliate Journey
  - [ ] AI Journey
* **Regression Result:** `PASS`
* **Production Mutations:** `ZERO (0)`

---

## 7. Final Change Approval
* **Final Change Status:** `VERIFIED` / `VERIFIED_WITH_GAPS` / `BLOCKED` / `REJECTED`
* **Approved By:** [Lead Architect / Auditor Name]
* **Date:** YYYY-MM-DD
