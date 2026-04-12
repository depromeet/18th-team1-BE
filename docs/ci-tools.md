# CI 코드 품질 도구 선정

> Kotlin + Spring Boot 프로젝트에서 사용하는 CI 코드 품질 도구와 선택 근거

## 선정 도구

| 카테고리 | 도구 | 역할 |
|----------|------|------|
| 코드 스타일 | ktlint | Kotlin 공식 스타일 가이드 기반 포맷팅 강제 |
| 정적 분석 | detekt | 코드 스멜, 복잡도, 잠재적 버그 탐지 |
| 코드 커버리지 | Kover | 테스트 커버리지 측정 및 최소 기준 검증 |
| 의존성 취약점 | Dependabot | 의존성 보안 취약점 자동 탐지 |

## 선정 기준

1. **Kotlin 네이티브** — Java 도구(Checkstyle, PMD, SpotBugs)는 Kotlin 문법을 이해하지 못하므로 Kotlin 전용 도구 사용
2. **가벼움** — Gradle 플러그인으로 바로 적용, 외부 서비스 의존 없음
3. **팀 규모에 적합** — 팀원 3명, 초기 코드베이스에서 과도한 도구 도입은 오버엔지니어링

## 대체재 비교 및 미선택 사유

### ktlint vs diktat vs Spotless

- **diktat**: ktlint 기반 확장이지만 커뮤니티가 작고 업데이트가 느려 장기 유지보수 불안
- **Spotless**: 멀티 언어 포맷터이나 내부적으로 ktlint를 래핑한 것이라 직접 사용이 깔끔
- **ktlint 선택**: Kotlin 공식 스타일 가이드 그대로 강제, zero-config, 자동 수정 지원

### detekt vs Qodana vs CodeClimate

- **Qodana**: JetBrains 제작으로 Kotlin 지원은 우수하나 Docker 기반이라 CI 시간 크게 증가
- **CodeClimate**: Kotlin 지원이 제한적이고 유료
- **detekt 선택**: Kotlin 전용 설계, 100+ 규칙, Gradle 플러그인으로 가볍게 실행

### Kover vs Jacoco

- **Jacoco**: 업계 표준이지만 바이트코드 레벨 측정이라 Kotlin 인라인 함수/코루틴 측정이 부정확할 수 있음
- **Kover 선택**: JetBrains가 Kotlin을 위해 만든 도구, 정확한 측정, 간단한 설정

### Dependabot vs Snyk vs Renovate vs OWASP

- **Snyk**: 상세하지만 무료 티어 제한
- **Renovate**: 설정이 세밀하지만 복잡, 현재 단계에서 불필요
- **OWASP Dependency-Check**: 느리고 오탐이 많음
- **Dependabot 선택**: GitHub 내장, 설정 파일 하나로 활성화, 무료

## SonarCloud 미도입 사유

SonarCloud는 정적 분석 + 커버리지를 통합 대시보드로 제공하는 강력한 도구이다. 그러나:

1. **단독으로 모든 도구를 대체하지 못함**
   - 포맷팅 자동 수정 불가 → ktlint 여전히 필요
   - 커버리지 측정은 못 함 → Jacoco/Kover가 측정한 리포트를 읽어서 시각화할 뿐
   - 의존성 취약점 미지원 → Dependabot 별도 필요
2. **현재 팀 상황에 불필요**
   - 팀원 3명으로 코드 리뷰에서 충분히 품질 관리 가능
   - 코드베이스가 작아 통합 대시보드의 이점이 크지 않음
   - ktlint + detekt + Kover로 각 카테고리를 개별 커버하면 충분

프로젝트가 성숙하고 팀 규모가 커지면 SonarCloud 도입을 재검토한다.

## CI 파이프라인 적용 순서

```
PR 생성 시:
  1. ktlintCheck       → 스타일 위반 시 실패
  2. detekt            → 코드 스멜 발견 시 실패
  3. build + test      → 컴파일 + 테스트
  4. koverVerify       → 커버리지 미달 시 실패
```
