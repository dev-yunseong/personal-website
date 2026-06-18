# Testing Workflow

## Why

Validation depth must match behavior risk and blast radius.

## Strategy

1. Verify commands against `.agents/docs/project.md` and repository configuration.
2. Reproduce the failure or establish a baseline when relevant.
3. Add or update the smallest test at the observable behavior boundary.
4. Run focused tests during implementation.
5. Run `./gradlew test` for shared contracts or cross-module changes.
6. Perform manual validation for UI behavior when automation is insufficient.

## Risk-Based Coverage

- Low risk: focused unit or static validation.
- Medium risk: affected package tests plus integration boundary.
- High risk: broad regression suite, migration/rollback checks, and production-like verification.

## Rules

- Test observable behavior, not private implementation details.
- Keep tests deterministic and independent.
- Avoid sleeps, external network reliance, and mutable global state unless explicitly controlled.
- Verify failure paths, boundaries, security behavior, and backward compatibility when relevant.
- Never claim validation that was not run.

## Reporting

State commands run, results, manual checks, skipped checks with reasons, and
known residual risk.
