# Provider Health Monitoring

## Circuit Breakers & Availability
- Snapshots provider status, success/failure rate, and latency.
- 5 consecutive failures trips circuit state from `CLOSED` to `OPEN` and marks provider as `CRITICAL`.
