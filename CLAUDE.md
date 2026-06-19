# CLAUDE.md

## 빌드 & 실행

```bash
cd app && ./gradlew build      # 빌드
cd app && ./gradlew test       # 테스트
cd app && ./gradlew bootRun    # 실행
cd app && ./gradlew testClasses # 컴파일 검증만
cd app && ./gradlew formatKotlin # 코드 포맷 자동 수정 — push 전 반드시 실행
cd app && ./gradlew lintKotlin   # 포맷 lint 검사 — push 전 반드시 실행
cd app && ./gradlew detekt       # 정적 분석 — push 전 반드시 실행
```

## 프로젝트 구조

```text
app/                  — Spring Boot 애플리케이션 (Kotlin)
infra/terraform/      — GCP 인프라 코드 (dev/prod 분리)
docs/                 — 프로젝트 문서 (convention.md, husky.md)
.husky/               — Git Hook 스크립트
.github/              — 이슈/PR 템플릿
```

## Git 규칙

- 브랜치: `타입/#이슈번호/설명` (예: `feat/#10/user-signup`, `fix/#15/token-expired`)
- GitHub 이슈 생성 시 assignees는 현재 작업자 본인으로 지정한다.
- 커밋: Conventional Commits — 이모지는 commit-msg 훅이 자동 삽입
- 커밋은 구현 범위와 관심사 기준으로 적절히 나눈다. 하나의 커밋은 한 가지 목적을 가져야 하며, 서로 독립적인 기능 구현, 리팩터링, 문서/설정 변경은 가능하면 분리한다.
- PR 대상: `dev` 브랜치
- 머지 순서: feature → dev → main(배포)
- `dev` 브랜치에 직접 커밋하거나 push하지 않는다. 모든 변경은 작업 브랜치에서 커밋하고 PR로 반영한다.

## 설정 파일 규칙

- 환경별 설정과 민감 정보는 `secret` submodule의 profile yml에서 관리한다.
- `app/src/main/resources/application.yml`에는 애플리케이션 공통 설정만 둔다.
- DB 접속 정보, OAuth secret, JWT secret 등 환경별 값은 `app/src/main/resources`에 추가하지 않는다.

## DB / Flyway 규칙

- Flyway migration은 테이블, 컬럼, 인덱스, 체크 제약처럼 필요한 스키마만 정의한다.
- 명시적인 요청이나 팀 합의가 없으면 Foreign Key 등 DB 레벨 연관관계 제약을 만들지 않는다.
- 이미지처럼 여러 도메인이 참조하는 공용 리소스는 `images`와 `image_owners(image_id, owner_type, owner_id, sort_order)` 같은 느슨한 참조 구조를 우선 고려한다.
- jOOQ는 SQL 생성과 실행을 돕는 도구이며 ORM 연관관계 매핑을 만들지 않는다. 애플리케이션 관계는 repository/usecase에서 명시적으로 조회한다.

## 추천 데이터 규칙

- `recommendations.quote_id`는 사용자가 최종 선택한 문장이다.
- `recommendation_quotes`는 추천 1회에서 노출할 후보 문장 목록이며, 사용자의 선택 결과가 아니다.
- 사용자 선택 이력, 통계, 결산, 발견탭처럼 최종 선택 결과를 사용하는 로직은 `recommendations.quote_id IS NOT NULL`을 기준으로 조회한다.
- 삭제된 선택 결과를 제외해야 하는 조회는 `recommendations.deleted_at IS NULL` 조건도 함께 적용한다.
- `recommendation_quotes`는 후보 노출, 선택 유효성 검증 등 후보 자체가 필요한 로직에서만 사용한다.

## 트랜잭션 규칙

- `@Transactional`은 usecase 계층에만 선언한다.
- 기본 호출 흐름은 controller → usecase → service → repository 순서로 구성한다.
- repository는 쿼리 작성과 실행만 담당하며 트랜잭션 경계를 만들지 않는다.
- service에는 도메인 정책과 검증 로직을 두되 `@Transactional`을 선언하지 않는다.
- 외부 연동 컴포넌트에는 `@Transactional`을 선언하지 않는다. 외부 API 호출과 DB 트랜잭션이 같은 경계에 묶이지 않도록 한다.
- 여러 repository 호출을 하나의 작업으로 묶어야 하면 해당 작업을 usecase 메서드로 만들고 그 메서드에 `@Transactional`을 선언한다.

## 커밋 타입

feat, fix, perf, refactor, test, docs, style, chore, ci, build, revert

## 코드 스타일

- 언어: Kotlin
- DTO: `data class` 사용
- `else`/`else-if` 지양 → early return
- 메서드 7줄 이내 권장
- 축약어 사용 금지. 단, jOOQ의 `DSLContext`와 같이 공식 API 타입명에 포함된 널리 쓰이는 기술 약어는 허용한다.

## 금지 사항

- `terraform.tfstate` 커밋 금지
- `.env` 파일 커밋 금지
- `--no-verify` 사용 금지
- 커밋 메시지, PR은 한글로 작성
- push, PR 생성은 명시적으로 요청하기 전까지 하지 말 것 — 커밋까지만 진행

## Terraform

- 작업 디렉토리: `infra/terraform/dev/`
- state: GCS 백엔드 (`firstpenguin-tf-state` 버킷)
- apply 전 반드시 `terraform plan` 확인
- GCP 프로젝트: `project-10e57bb0-e960-4b16-9fa`
- 리전: `asia-northeast3` (서울)

## 네이밍 컨벤션

- 인프라 리소스: `{env}-{service}-{resource}` (예: `dev-firstpenguin-vpc`)
- 방화벽 태그: 역할 기반 (`api-server`, `db-server`)
