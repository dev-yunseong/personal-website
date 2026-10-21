# 2026-10-21 — Spring Boot 3.5.16 업그레이드와 virtual thread 적용

- Date: 2026-10-21
- GitHub Issue: #219
- Status: 구현 완료, CI 검증 대기

## Goal

Spring Boot 3.5 계열 최신 공개 패치 3.5.16을 적용하고 Java 21 virtual thread를 활성화합니다.

## Non-goals

Spring Boot 4.x 마이그레이션과 운영 배포는 포함하지 않습니다.

## Context / Constraints

Java 21, Spring AI 1.1.2, Gradle 8.14.3을 유지합니다. 3.5 계열은 장기 지원 대상이지만 무료 OSS 지원과 상용 연장 지원은 별개입니다. 공식 GitHub 릴리스에서 3.5.16을 확인했습니다.

## Approach (Checklist)
- [x] **Step 0: Recon**: build.gradle, application.yml, Dockerfile, CI와 저장소 지침을 확인했습니다.
- [x] **Step 1: Implementation**: build.gradle에서 3.5.4를 3.5.16으로 변경하고 application.yml에서 spring.threads.virtual.enabled를 true로 설정합니다.
- [ ] **Step 2: Tests**: GitHub Actions에서 ./gradlew test 결과를 확인합니다. 로컬 git clone은 프록시 연결 오류로 실패했습니다.
- [x] **Step 3: Rollout / Rollback**: 병합 전 CI를 확인하며, 문제가 생기면 변경 커밋을 되돌립니다.

## Validation
- **Commands to run:** ./gradlew test
- **Expected output:** 테스트가 모두 통과합니다. 실제 요청 스레드의 Thread.isVirtual() 확인은 별도 실행 검증이 필요합니다.

## Risks & Rollback
- **Risks:** 관리 의존성 패치에 따른 동작 변화가 있을 수 있습니다. virtual thread는 자동 구성된 MVC 요청 처리와 작업 실행기에 적용되며, 사용자 정의 실행기와 Reactor 스레드를 자동 전환하지 않습니다. Java 21에서는 synchronized 내부의 블로킹으로 pinning이 발생할 수 있습니다.
- **Rollback steps:** 변경 커밋을 revert하거나 spring.threads.virtual.enabled=false로 비활성화합니다.

## Open Questions

로컬 프록시 오류로 전체 테스트를 실행하지 못했습니다. CI 결과와 운영 부하에서의 효과를 확인해야 합니다.
