# CLAUDE.md

## 빌드 & 실행

```bash
cd app && ./gradlew build      # 빌드
cd app && ./gradlew test       # 테스트
cd app && ./gradlew bootRun    # 실행
cd app && ./gradlew testClasses # 컴파일 검증만
```

## 프로젝트 구조

```
app/                  — Spring Boot 애플리케이션 (Kotlin)
infra/terraform/      — GCP 인프라 코드 (dev/prod 분리)
docs/                 — 프로젝트 문서 (convention.md, husky.md)
.husky/               — Git Hook 스크립트
.github/              — 이슈/PR 템플릿
```

## Git 규칙

- 브랜치: `타입/#이슈번호/설명` (예: `feat/#10/user-signup`, `fix/#15/token-expired`)
- 커밋: Conventional Commits — 이모지는 commit-msg 훅이 자동 삽입
- PR 대상: `dev` 브랜치
- 머지 순서: feature → dev → main(배포)

## 커밋 타입

feat, fix, perf, refactor, test, docs, style, chore, ci, build, revert

## 코드 스타일

- 언어: Kotlin
- DTO: `data class` 사용
- `else`/`else-if` 지양 → early return
- 메서드 7줄 이내 권장
- 축약어 사용 금지

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
