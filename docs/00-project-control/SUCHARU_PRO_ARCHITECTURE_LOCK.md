# SUCHARU PRO — ARCHITECTURE LOCK CONTRACT
### Formal Boundary Protection & Architectural Invariants Contract

---

## 1. ARCHITECTURE IDENTITY
- **System**: Sucharu Pro Commercial Printing ERP & Server-Driven Wall Platform
- **Scope**: Modules 00–24 & Forms 01–06
- **Status**: **LOCKED BASELINE**

---

## 2. CANONICAL MODULE BOUNDARY (MODULES 00–24)
There are exactly 25 canonical modules in Sucharu Pro (Modules 00–24).
- **Strict Prohibition**: There is **NO Module 25**.
- **No Module Renumbering**: Module IDs 00 through 24 are fixed and immutable.

---

## 3. CANONICAL FORMS BOUNDARY (FORMS 01–06)
Forms 01–06 represent the presentation, design, offer, commercial pricing, order orchestration, and server-driven publishing layer:
1. **FORM 01 — Content & Product Foundation** (Commit `f60c234`)
2. **FORM 02 — Full Admin Visual Design Studio** (Commit `30098f3` / `a1e4dd3`)
3. **FORM 03 — Offer & Audience Eligibility** (Commit `155eaa0`)
4. **FORM 04 — Pricing & Commercial Rules** (Commit `13dcc27`)
5. **FORM 05 — ERP / Order / Fulfillment Integration** (Commit `ed8a249`)
6. **FORM 06 — Wall / Section / Publishing Control** (Commit `e1334e0`)

---

## 4. CORE DOMAIN BOUNDARIES
- **Product Master Ownership**: `InventoryProduct` (`core/src/main/java/com/sucharu/sucharupro/domain/model/inventory/InventoryProduct.kt`) in Module 07 is the single canonical source of truth for products.
- **Category Master Ownership**: `InventoryProductCategory` (`Module 07`) is the single canonical source of truth for categories.
- **Content Foundation**: `ContentFoundation` (`Form 01`) holds presentation metadata and references `InventoryProduct.id`.
- **Visual Design Studio**: `VisualDesignConfiguration` (`Form 02`) holds visual card dimensions, colors, borders, shadows, typography, and CTA styling.
- **Commercial Pricing**: `ProductPriceConfiguration` (`Form 04`) calculates commercial prices and saves immutable `OrderPriceSnapshot` records.

---

## 5. CRITICAL BUSINESS INVARIANTS

### Invariant A: Offer Eligibility $\neq$ Wall Visibility
$$\text{FORM 03 OFFER ELIGIBILITY} \neq \text{FORM 06 WALL VISIBILITY}$$
- **Offer Eligibility** determines *who can REDEEM an offer* (`isEveryoneEligible`, `isGuestEligible`, `isCustomerEligible`, `isAffiliateEligible`).
- **Wall Visibility** determines *where an offer is DISPLAYED* (`publicGuestWallVisible`, `customerWallVisible`, `affiliateWallVisible`).
- These two systems must NEVER be merged or collapsed into a single Boolean or Enum.

### Invariant B: Historical Price Snapshot Immutability
$$\text{Master Price Version Updates} \neq \text{Historical Order Price Snapshot Changes!}$$
- When a customer creates an order, `CommercialPricingService` saves an immutable `OrderPriceSnapshot`.
- If Master Price in `ProductPriceConfiguration` is updated to Version 2 later, existing `OrderPriceSnapshot` grand totals MUST REMAIN 100% UNCHANGED.

### Invariant C: Canonical Production Pipeline
The 13 canonical production stages are strictly separated from `OrderStatusType`:
```
DESIGN → APPROVAL → QC → ITEM_APPROVAL → CTP → PRINTING → LAMINATION → FOLDING → BINDING → FINAL_QC → PACKAGING → READY → DELIVERED
```

### Invariant D: Inventory Ownership
- Inventory is tracked solely for finished products in Module 07.
- No raw-material inventory ledger or duplicate stock counters may be created.

---

## 6. FORBIDDEN ARCHITECTURAL CHANGES
- ❌ **NO Module 25**.
- ❌ **NO duplicate Product Master** (`GalleryProduct`, `WallProduct`, etc.).
- ❌ **NO duplicate Category Master**.
- ❌ **NO duplicate Pricing Engine** or hardcoded prices in UI Compose code.
- ❌ **NO duplicate Order / Finance Ledgers**.
- ❌ **NO merging of Offer Eligibility and Wall Visibility**.
- ❌ **NO replacing PostgreSQL/Flyway with in-memory stores for production**.

---

## 7. CHANGE APPROVAL RULES
Any proposed modification to locked baselines (Modules 00–24, Forms 01–06) requires:
1. Direct source code evidence proving an unavoidable integration/compile defect.
2. Formal audit justification documenting the exact file, lines, and impact.
3. Minimal surgical patch without altering underlying domain rules.
