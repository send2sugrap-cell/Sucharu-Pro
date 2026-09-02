package com.sucharu.sucharupro.data.model.task

/**
 * Task priority levels for Sucharu Pro ERP.
 */
enum class TaskPriority(val level: Int, val defaultLabel: String) {
    LOW(1, "Low"),
    NORMAL(2, "Normal"),
    HIGH(3, "High"),
    URGENT(4, "Urgent"),
    CRITICAL(5, "Critical");

    companion object {
        fun fromString(priority: String?): TaskPriority {
            if (priority.isNull_or_blank()) return NORMAL
            return entries.firstOrNull { it.name.equals(priority, ignoreCase = true) } ?: NORMAL
        }
    }
}

private fun String?.isNull_or_blank(): Boolean = this == null || this.trim().isEmpty()
