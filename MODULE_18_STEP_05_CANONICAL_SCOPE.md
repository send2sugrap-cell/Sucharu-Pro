# MODULE 18 → STEP 05: CANONICAL SCOPE SPECIFICATION

**Canonical Module Title**: `Module 18 — Advanced Dynamic Imposition & Gang-Run Optimizer Engine`  
**Canonical Step Title**: `Step 05 — Prepress CTP Output, Plate Imposition Package & Production-Ready Export`  
*(Also anchored as: Pre-Press Marks, Gripper/Gutter/Bleed Allocation & Plate Prep / Pre-Press CTP Plate Output Generation)*  
**Date**: September 2, 2026  
**Status**: **CANONICAL SCOPE AUDITED & LOCKED**

---

## 1. Context & Architectural Positioning

Module 18 Step 05 is the production-prepress output layer sitting directly AFTER:
- **Module 18 Step 01**: Automated Sheet Layout & Single-Job Dynamic Imposition Engine
- **Module 18 Step 02**: Multi-Job Gang-Run Batching & Compatibility Clustering
- **Module 18 Step 03**: Dynamic Nesting, Sheet Utilization & Wastage Minimization
- **Module 18 Step 04**: Signature Layouts, Page Imposition & Work-and-Turn / Tumble

and directly BEFORE:
- **Module 18 Step 06**: Imposition Audit Trail, Production Job Interlock & AI Handoff

The core mission of Step 05 is to convert an approved imposition specification (Single-Job, Gang-Run, Dynamic Nesting, or Multi-Page Signature) into a deterministic, auditable, production-ready **CTP (Computer-to-Plate) / Prepress Output Package Specification**.

---

## 2. Mandatory Core Capabilities

1. **Prepress Marks Placement & Allocation**:
   - Registration Targets (4 corner marks + sheet centerline targets + plate crosshairs).
   - Precision Crop Marks indicating exact page trim lines.
   - Bleed Line Indicators (standard 3.0000mm or custom prepress bleed margin).
   - Color Calibration Bars & Density Step Wedges (Cyan, Magenta, Yellow, Black, Spot Pantone) placed along non-gripper tail margins.
   - Fold Line & Spine Center Indicators for signature publication forms.
   - Plate Identifier Slugs containing Tenant ID, Job ID, Form ID, Side, Separation Color, Output Date/Version, and Screen Ruling (LPI).
   - Gripper Margin Visual Clearance Zones.

2. **Color Separation & Plate Channel Management**:
   - Primary Process Color Plates: Cyan, Magenta, Yellow, Black (CMYK).
   - Spot Color Separation Plates: Dedicated Spot Pantone plates, Varnish Coating plates, Die-Cut Guides.
   - Front Plate vs Back Plate separation tracking.
   - Work-and-Turn / Work-and-Tumble single-plate dual-side registration preservation.

3. **Screening & Output Resolution Configuration**:
   - Supported Output Resolutions: 1200 DPI, 2400 DPI, 2540 DPI, 4000 DPI.
   - Supported Screening Methods: AM Conventional (e.g. 150/175 LPI), FM Stochastic, Hybrid XM.
   - Screen Angles and Dot Shapes (Euclidean, Round, Elliptical).

4. **Physical Geometry & Clearance Validation**:
   - Strict mathematical validation: $\text{Sheet Area} + \text{Marks} + \text{Gripper} + \text{Tail} + \text{Side Margins} \le \text{Plate Dimension}$.
   - Validation against minimum clamp / gripper clearances.

5. **Deterministic Production Package & Cryptographic Integrity**:
   - Versioned, immutable `CtpOutputPackage` entity.
   - Deterministic SHA-256 cryptographic integrity hash calculated across production-critical parameters.

6. **Enterprise Persistence & RBAC**:
   - PostgreSQL schema with `ENABLE / FORCE ROW LEVEL SECURITY`.
   - REST API endpoints under `/api/v1/imposition/ctp/*`.
   - Role-based authorization (`ADMIN`, `MANAGER`, `STAFF`, `AI_AGENT`).

7. **Android Prepress Command Center UI**:
   - Interactive 2D Canvas rendering plate boundaries, sheet margins, registration marks, crop lines, color bars, slugs, and separation previews.
   - Color channel switcher, validation panel, package export, and version history.

---

## 3. Strict Boundary Invariants

- **Module 17 (Production Execution)**: Unchanged and intact.
- **Module 18 Steps 01–04**: Fully preserved.
- **Module 19 (Substrate Stock Reservation)**: FROZEN / ON HOLD. Step 05 emits prepress production output contracts only; it does not mutate stock or reserve inventory.
