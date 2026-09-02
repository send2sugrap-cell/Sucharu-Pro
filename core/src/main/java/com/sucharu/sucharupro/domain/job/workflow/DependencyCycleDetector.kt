package com.sucharu.sucharupro.domain.job.workflow

/**
 * Cycle detection utility for workflow dependency DAGs (INFRA-04 Step 04).
 */
object DependencyCycleDetector {

    /**
     * Checks if adding a directed dependency edge (from child [jobId] -> parent [dependsOnJobId])
     * would introduce a circular cycle given existing dependency edges.
     *
     * @param existingEdges Map where key is child jobId, value is set of parent jobIds it depends on.
     * @param newChildJobId The child job that will depend on [newParentJobId].
     * @param newParentJobId The parent prerequisite job.
     * @return true if a cycle is detected (illegal), false if DAG invariant is preserved.
     */
    fun wouldCreateCycle(
        existingEdges: Map<String, Set<String>>,
        newChildJobId: String,
        newParentJobId: String
    ): Boolean {
        if (newChildJobId == newParentJobId) return true

        // If newParentJobId already directly or transitively depends on newChildJobId, adding this edge causes a cycle.
        val visited = mutableSetOf<String>()
        val queue = ArrayDeque<String>()
        queue.add(newParentJobId)

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (current == newChildJobId) {
                return true // Cycle detected!
            }
            if (visited.add(current)) {
                val parents = existingEdges[current] ?: emptySet()
                for (parent in parents) {
                    if (!visited.contains(parent)) {
                        queue.add(parent)
                    }
                }
            }
        }

        return false
    }
}
