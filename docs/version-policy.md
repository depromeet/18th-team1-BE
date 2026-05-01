# 기술 버전 선정 기준

> 백엔드 초기 환경 구성 시 Java, Spring Boot, Kotlin, Gradle, CI 코드 품질 도구 버전을 선택한 기준을 정리한다.

## 선정 원칙

- 공식 지원 범위 안에서 현재 JVM/Spring/Kotlin 생태계의 안정적인 최신 라인을 우선 사용한다.
- Gradle wrapper와 Gradle plugin 버전을 고정해 로컬/CI 환경 차이를 줄인다.
- Kotlin 기반 프로젝트이므로 Kotlin 문법과 생태계를 직접 지원하는 도구를 사용한다.
- 초기 프로젝트 단계에서는 레거시 호환성보다 앞으로의 업그레이드 비용과 유지보수성을 우선한다.

## 현재 사용 버전

| 항목 | 버전 | 설정 위치 |
|------|------|-----------|
| Java | 21 | `app/build.gradle.kts`, `app/Dockerfile` |
| Spring Boot | 4.0.5 | `app/build.gradle.kts` |
| Kotlin | 2.2.20 | `app/build.gradle.kts` |
| Gradle | 9.4.1 | `app/gradle/wrapper/gradle-wrapper.properties` |
| kotlinter | 5.3.0 | `app/build.gradle.kts` |
| detekt | 2.0.0-alpha.1 | `app/build.gradle.kts` |
| Kover | 0.9.1 | `app/build.gradle.kts` |

## Java 21 선택 이유

이 프로젝트는 Kotlin을 사용하지만 Kotlin/JVM 백엔드 프로젝트다. Kotlin 소스는 Java 소스로 변환되는 것이 아니라 JVM이 실행할 수 있는 `.class` 바이트코드로 컴파일된다. 따라서 빌드에 사용할 JDK, 테스트 실행 환경, 애플리케이션 런타임 JRE 기준을 선택해야 한다.

Java 21은 다음 이유로 선택했다.

1. **LTS 버전**
   - Java 21은 장기 지원(LTS) 버전이다.
   - 신규 프로젝트에서 최신 기능과 안정적인 지원 기간을 함께 가져갈 수 있다.

2. **Spring Boot 4.x 공식 지원 범위**
   - Spring Boot 4.x는 Java 17 이상을 요구하며, Java 26까지 호환된다.
   - Java 21은 Spring Boot 4.x의 공식 지원 범위 안에 있는 안정적인 LTS 선택지다.

3. **빌드 환경과 런타임 환경 일치**
   - Gradle toolchain은 Java 21을 사용하도록 설정되어 있다.
   - Dockerfile도 빌드 이미지는 `eclipse-temurin:21-jdk`, 실행 이미지는 `eclipse-temurin:21-jre`를 사용한다.
   - 로컬 빌드, CI 빌드, 컨테이너 런타임의 Java 기준을 맞춰 환경 차이를 줄인다.

4. **Java 17 대비 개선점**
   - Virtual Threads가 정식 도입되어 blocking I/O 기반 서버에서 확장성 선택지가 늘었다.
   - 기본 charset이 UTF-8로 고정되어 실행 환경별 인코딩 차이를 줄일 수 있다.
   - GC, 컬렉션 API, 런타임 성능 개선 등 서버 운영에 유리한 변화가 포함되어 있다.

Java 25는 Java 21 다음 LTS이며 향후 업그레이드 후보로 볼 수 있다. 다만 현재는 Spring Boot, Gradle, 컨테이너 이미지, CI 도구 호환성이 넓게 검증된 Java 21을 프로젝트 기준으로 사용한다.

Kotlin의 JVM bytecode target은 Java toolchain과 별개로 명시할 수 있다. 현재는 Java 21 toolchain과 Java 21 컨테이너 런타임을 프로젝트 기준으로 둔다.

## Spring Boot 4.x 선택 이유

Spring Boot 4.x는 현재 Spring 생태계의 주요 stable 라인이다. 신규 백엔드 프로젝트에서 최신 Spring 플랫폼과 의존성 관리 기준을 사용하기 위해 Spring Boot 4.x를 선택했다.

1. **현재 Spring 플랫폼 라인 사용**
   - Spring Boot 4.x는 Spring Framework 7, Spring Security 7, Jackson 3, Tomcat 11 계열을 기반으로 한다.
   - 신규 프로젝트에서 최신 Spring 플랫폼을 기준으로 잡으면 이후 major migration 비용을 줄일 수 있다.

2. **공식 지원 기간**
   - Spring Boot 4.0.x의 OSS 지원 종료일은 2026-12-31이다.
   - Spring Boot 3.x의 마지막 OSS 지원 라인인 3.5.x는 2026-06-30에 지원 종료 예정이다.
   - 신규 환경에서는 더 오래 지원되는 현재 major 라인을 선택하는 것이 유지보수 측면에서 유리하다.

3. **Kotlin 2.2.x와의 방향성**
   - 프로젝트는 Kotlin 2.2.x를 기준으로 설정되어 있다.
   - Spring Boot 4.x / Spring Framework 7 라인은 Kotlin 2.2.x, JSpecify 기반 null-safety 등 최신 Kotlin 지원 방향과 맞다.

4. **현재 의존성 조합**
   - `springdoc-openapi`는 Spring Boot 4 호환 버전인 3.0.3을 사용한다.
   - Jackson Kotlin 모듈, Security, Flyway 등도 현재 Spring Boot 4.x 관리 의존성 조합을 기준으로 검증한다.

## Kotlin 2.2.x 선택 이유

Kotlin은 Spring Boot처럼 버전별 고정 EOL 날짜를 명확히 제공하지 않는다. 대신 최신 stable 버전과 이전 language/API 버전 호환 범위를 기준으로 지원한다.

현재 프로젝트는 Kotlin 기반 백엔드이므로 Kotlin 2.2.x를 기준으로 개발 환경을 구성했다.

- Spring Boot 4.x / Spring Framework 7 라인과의 호환성
- Kotlin 전용 정적 분석 도구(detekt), 코드 스타일 도구(ktlint 계열)와의 호환성
- Java 도구(Checkstyle, PMD, SpotBugs)보다 Kotlin 문법을 정확히 이해하는 품질 도구 구성
- 초기 프로젝트 단계에서 최신 Kotlin 라인을 기준으로 맞춰 추후 Kotlin 업그레이드 비용을 줄이기 위함

## Gradle 9.x 선택 이유

Gradle 9.4.1 wrapper는 Spring Boot 4.x 기반 초기 프로젝트 세팅 시 함께 생성된 값이며, Spring Boot 4.x의 공식 지원 범위 안에 있다. 팀원 로컬 환경과 CI 빌드 환경을 같은 wrapper 기준으로 고정하기 위해 Gradle 9.x를 사용한다.

1. **Spring Boot 4.x의 공식 지원 범위**
   - Spring Boot 4.x는 Gradle 8.14 이상 또는 Gradle 9.x를 명시적으로 지원한다.
   - 현재 프로젝트의 Spring Boot 버전은 4.0.5이므로 Gradle 9.x는 공식 지원 범위 안에 있다.

2. **개발 환경 재현성**
   - Gradle wrapper를 사용하면 팀원 로컬 환경과 CI가 동일한 Gradle 버전으로 빌드된다.
   - 빌드 도구 버전을 고정해 환경별 차이로 발생하는 문제를 줄인다.

3. **초기 환경의 일관성**
   - Spring Boot 4.x, Kotlin 2.2.x, Gradle 9.x 조합으로 초기 환경이 구성되어 있다.
   - 빌드, 테스트, lint, detekt, coverage 검증을 같은 Gradle wrapper 기준으로 실행한다.

## CI 코드 품질 도구 버전 선택 이유

CI 품질 도구의 상세 선정 기준은 `docs/ci-tools.md`를 따른다. 이 문서에서는 버전 선택 이유만 정리한다.

### kotlinter 5.3.0

- ktlint 엔진을 Gradle에서 실행하기 위한 플러그인이다.
- Kotlin 2.2.x 호환을 위해 `jlleitschuh` 계열 플러그인 대신 kotlinter를 사용한다.
- 코드 스타일 검사(`lintKotlin`)와 자동 수정(`formatKotlin`)을 제공한다.

### detekt 2.0.0-alpha.1

- Kotlin 전용 정적 분석 도구다.
- Kotlin 2.2.x 호환을 위해 2.0.0-alpha.1을 사용한다.
- alpha 버전이지만 빌드/검증 도구로만 사용되며 프로덕션 런타임 코드에는 포함되지 않는다.

### Kover 0.9.1

- JetBrains가 제공하는 Kotlin 친화적인 커버리지 도구다.
- Jacoco는 업계 표준이지만 Kotlin 인라인 함수, 코루틴 등에서 측정이 부정확할 수 있어 Kover를 사용한다.
- 현재는 최소 커버리지 기준을 낮게 두고, 추후 테스트 정책이 구체화되면 기준을 조정한다.

## 대안 검토

Spring Boot 3.x는 운영 사례가 많고 안정적인 라인이다. 기존 서비스에서 3.x를 유지하는 것은 레거시 의존성, 마이그레이션 비용, 내부 보안 통제, 상용 지원 여부에 따라 충분히 가능한 선택이다.

이 프로젝트는 신규 환경이므로 기존 레거시 제약보다 다음 기준을 우선했다.

- 현재 Spring major 라인을 기준으로 개발 환경 구성
- Kotlin 2.2.x와 Kotlin 전용 품질 도구를 함께 사용하는 개발 경험
- Gradle 9 wrapper를 통한 로컬/CI 빌드 환경 고정
- OSS 지원 기간과 이후 업그레이드 비용

따라서 이 프로젝트는 신규 백엔드 환경의 기준점으로 Spring Boot 4.x 조합을 선택했다.

## 버전 변경 기준

선택한 버전은 고정된 결론이 아니며, 다음 조건이 생기면 재검토한다.

- 현재 버전에서 보안 취약점이 발견된 경우
- 사용 중인 버전의 OSS 지원 종료가 가까워진 경우
- Kotlin, Gradle, Spring Boot, 품질 도구 간 호환성 문제가 확인된 경우
- 주요 의존성(`springdoc-openapi`, Jackson, Flyway, jOOQ, Security 등)의 지원 범위가 변경된 경우
- 팀원 로컬 개발 환경 또는 CI 환경에서 재현 가능한 빌드 문제가 발생한 경우

## 참고

- Spring Support Policy: https://spring.io/support-policy
- Spring Boot generations API: https://api.spring.io/projects/spring-boot/generations
- Spring Boot system requirements: https://docs.spring.io/spring-boot/system-requirements.html
- Kotlin releases: https://kotlinlang.org/docs/releases.html
- Kotlin evolution principles: https://kotlinlang.org/docs/kotlin-evolution-principles.html
