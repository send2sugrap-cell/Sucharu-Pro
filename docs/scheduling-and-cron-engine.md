# Scheduling & Recurring Cron Engine

## 1. Overview
The scheduling engine (`JobScheduler` + `ScheduleCalculator`) runs on the server to execute recurring maintenance tasks, periodic report generation, inventory reconciliation, and sync operations.

## 2. Supported Schedule Types
1. **CRON**: Standard 5-field cron syntax (`minute hour dom month dow`), supporting intervals like `*/15 * * * *` or `0 2 * * *`.
2. **FIXED_INTERVAL**: Recurrences specified in milliseconds, seconds, minutes, or hours (e.g. `every 5 minutes`).
3. **ONE_OFF_DELAYED**: Scheduled execution at an exact epoch timestamp in the future.

## 3. Server-Authoritative Timezone Handling
- Schedules can specify a target timezone (e.g. `Asia/Kolkata` or `UTC`).
- `ScheduleCalculator` resolves next execution timestamps in UTC epoch milliseconds, preventing daylight saving or mobile clock drift errors.
- Active schedules update `last_run_at` and `next_run_at` atomically upon job submission.
