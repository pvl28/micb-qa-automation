# learning-lab: PostgreSQL + RabbitMQ (local, synthetic)

**This module does not test micb.md and does not connect to any bank system.**
The public website gives no access to a database or message broker, so these skills are practised here
on a tiny local pipeline with synthetic data:

```
RatePublisher --(JSON, persistent)--> exchange "rates" --> queue "rates.snapshots"
                                                              |
                                                     RateConsumer (manual ack)
                                                     |                    \
                                   valid: INSERT ... ON CONFLICT DO NOTHING   invalid: reject
                                          into PostgreSQL rate_snapshot        -> DLX -> "rates.snapshots.dlq"
```

## What the tests check

| Test | Type | Checks |
|---|---|---|
| `RateSnapshotTest` | unit, no Docker | validation rules (buy ≤ sell, positive, ISO code) |
| `RatePipelineIT.validMessageIsStored` | integration | message → row with exact values |
| `duplicateMessageIsStoredOnce` | integration | idempotent consumer (same message twice → one row) |
| `invalidMessageGoesToDlq` / `malformedJsonGoesToDlq` | integration | rejected messages go to the dead-letter queue |
| `databaseConstraintProtectsData` | integration | DB `CHECK` constraint as a second line of defence |

Integration tests use **Testcontainers** (real PostgreSQL 16 and RabbitMQ 3.13 in throw-away Docker
containers). Without Docker they are reported as **skipped**, never as passed.

## Run

```bash
cd learning-lab
mvn test
```

Optional manual exploration: `docker compose up -d`, then RabbitMQ UI at http://localhost:15672
(guest/guest) and `psql -h localhost -U lab lab` (password `lab`, local sandbox only).
