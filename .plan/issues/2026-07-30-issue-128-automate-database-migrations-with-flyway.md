# 2026-07-30 — Automate Database Migrations With Flyway

- Date: 2026-07-30
- GitHub Issue: #128
- Status: Complete

## Goal

Run ordered PostgreSQL schema migrations automatically when Spring Boot starts.
Support both the existing production database and a new empty database.

## Non-goals

- Provision PostgreSQL or manage database backups.
- Add a separate migration service or Gradle Flyway plugin.
- Rewrite existing schema history into many speculative versions.

## Context / Constraints

- Production already has the schema accumulated in `sql/schema.sql`.
- Hibernate schema generation is disabled.
- Existing production data must remain intact; its current schema and seed are
  baselined through V2.
- Flyway migrations become immutable after deployment.

## Approach (Checklist)
- [x] **Step 0: Recon** (Inspect existing code, locate files)
- [x] **Step 1: Implementation** (Add Flyway and version existing SQL)
- [x] **Step 2: Tests** (Run migration/static checks and Gradle tests)
- [x] **Step 3: Rollout / Rollback** (Document baseline and rollback boundary)

## Validation
- **Commands to run:** `./gradlew test`, `./gradlew build`
- **Expected output:** Flyway dependency resolves; application tests remain green; migration files have valid ordered names.

## Risks & Rollback
- **Risks:** Existing production schema may differ from the V2 baseline. `baseline-on-migrate` trusts that schema and starts applying at V3.
- **Rollback steps:** Revert application/config changes before first Flyway deployment. After a migration runs, restore the database from backup or add a forward corrective migration; never edit an applied migration.

## Open Questions

- Confirm production schema and profile seed match the V2 baseline before first deployment.
