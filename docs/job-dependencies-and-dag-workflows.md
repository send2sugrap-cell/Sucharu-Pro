# Job Dependencies & DAG Workflows

## 1. Directed Acyclic Graph (DAG) Model
Workflows in Sucharu Pro can compose multiple background jobs into structured execution graphs. Downstream jobs remain in `PENDING` status until all prerequisite upstream dependencies are satisfied.

```
       [Order Placed]
             |
             v
   +-------------------+
   | 1. Generate PDF   |
   +---------+---------+
             |
      +------+------+
      |             |
      v             v
+-----------+ +-----------+
| 2. QC Spec| | 3. Reserve|
| Check     | | Inventory |
+-----+-----+ +-----+-----+
      |             |
      +------+------+
             |
             v
   +-------------------+
   | 4. Dispatch Order |
   +-------------------+
```

## 2. Dependency Cycle Prevention
- `DependencyCycleDetector` performs depth-first cycle detection (DFS) before adding any dependency edge to `job_dependencies`.
- If a dependency creates a cycle (e.g. `A -> B -> A`), the submission is rejected immediately with an `IllegalArgumentException`.

## 3. Dependency Satisfaction
- When an upstream job reaches `SUCCEEDED`, `JobExecutionEngine` notifies `JobDependencyManager`.
- `JobDependencyManager` checks if all dependencies for downstream jobs are satisfied.
- Once all prerequisites succeed, the downstream job is transitioned from `PENDING` to `QUEUED` with `available_at = NOW()`.
