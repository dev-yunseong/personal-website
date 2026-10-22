# 2026-10-21 — GeoLite2 from a mounted volume only

- Date: 2026-10-21
- GitHub Issue: #195
- Status: Complete

## Goal
Stop the image build from fetching GeoLite2 and make the host volume mount the documented way to supply the City and ASN databases.

## Non-goals
No change to deployment, the resolvers' behavior, or `application.yml` defaults.

## Context / Constraints
The download never worked: MaxMind answers 302 and `curl -fsS` wrote an empty file. Fixing it with `-L` (closed PR #196) would put GeoLite2 into the public `ghcr.io` image once `MAXMIND_LICENCE_KEY` is added, which the GeoLite2 EULA forbids, and immutable tags cannot honour the 30-day deletion rule. Production already reads the databases from a host mount. The `MAXMIND_LICENCE_KEY` repository secret was never set.

## Approach (Checklist)
- [x] **Step 0: Recon** Read Dockerfile, CI workflow, resolvers, `application.yml`, and PR #196 review comment.
- [x] **Step 1: Implementation** Remove the download step and `/geoip` copy from Dockerfile, the secret from CI, and the licence-key wording from resolver Javadoc.
- [x] **Step 2: Docs** Add a README section on mounting the databases at `/app/geoip`.
- [x] **Step 3: Rollout / Rollback** Revert the commit to restore the (non-working) download step.

## Validation
- **Commands to run:** `./gradlew compileJava`; `git diff --check`.
- **Expected output:** Build compiles; only comments change in Java.
- **Result:** See PR.

## Risks & Rollback
- **Risks:** A deployment that relied on the empty `/app/geoip` directory existing sees no difference, because the resolvers already tolerate a missing file.
- **Rollback steps:** Revert the commit; no migration.

## Open Questions
- None.
