# MODULE 07 — INVENTORY BOUNDARY REPORT

---

### 1. Database Schema & Product Categories
Canonical table schema: [`V1__canonical_postgresql_schema.sql`](file:///E:/App/Sucharu%20Pro/core/src/main/resources/db/migration/V1__canonical_postgresql_schema.sql#L352-L362)
```sql
CREATE TABLE inventory_products (
    product_id VARCHAR(50) NOT NULL,
    project_id VARCHAR(36) NOT NULL REFERENCES tenants(project_id) ON DELETE RESTRICT,
    product_code VARCHAR(50) NOT NULL,
    product_name VARCHAR(150) NOT NULL,
    category VARCHAR(50) NOT NULL CHECK (category IN ('PAPER', 'INK', 'PLATE', 'CHEMICAL', 'PACKAGING_MATERIAL', 'FINISHED_GOODS', 'SPARE_PARTS')),
    unit VARCHAR(20) NOT NULL DEFAULT 'PCS',
    reorder_level INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 1,
    PRIMARY KEY (project_id, product_id),
    UNIQUE (project_id, product_code)
);
```

---

### 2. Product Classification & Usage
- **Finished Goods**: Products produced by printing workflow execution (Phase 06 Step 01) are registered with `category = 'FINISHED_GOODS'` and default `productType = FINISHED_PRODUCT`.
- **Substrate Paper Stock**: Paper materials used for printing production (Module 19) are registered with `category = 'PAPER'` and stock-tracking enabled (`isStockTracked = true`).

---

### 3. Inventory Stock Formula Invariants
$$\text{Available Stock} = \text{Physical On-Hand Quantity} - (\text{Active Soft Holds} + \text{Active Hard Holds})$$
- Available stock cannot become negative.
- Cancelling or releasing a reservation immediately restores available stock without modifying physical on-hand quantity.
- Physical stock is decremented only upon physical dispatch to the shop floor or delivery fulfillment (Module 08).
