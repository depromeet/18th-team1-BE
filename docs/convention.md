# 프로젝트 컨벤션

## 1. 브랜치 전략

Git Flow 기반 + PR 중심 협업 방식을 사용합니다.

### 브랜치 구조

- `main` : 실제 운영 배포 브랜치
- `dev` : 통합 개발 브랜치 (default)
- `feat/#이슈번호/설명` : 기능 개발
- `fix/#이슈번호/설명` : 버그 수정
- `chore/#이슈번호/설명` : 설정, 인프라 등
- `release/*` : 배포 준비
- `hotfix/*` : 운영 긴급 수정

### 브랜치 네이밍 예시

```
chore/#1/init
feat/#10/user-signup
fix/#15/token-expired
```

### 작업 흐름

> 이슈 생성 → 브랜치 생성 → 작업 → PR → 리뷰 → merge(dev) → 배포(main)

- 모든 작업은 **이슈 기반으로 진행**
- 브랜치는 반드시 **이슈와 연결 (트래킹 필수)**

---

## 2. 커밋 컨벤션

### 기본 규칙

- **Conventional Commits 기반**
- 메시지는 **의미 중심 + 간결하게 작성**
- `commit-msg` 훅이 **이모지를 자동으로 prefix** 해줌

### 커밋 타입

| 타입 | 이모지 | 설명 |
|------|--------|------|
| feat | ✨ | 새로운 기능 추가 |
| fix | 🐛 | 버그 수정 |
| perf | ⚡️ | 성능 개선 |
| refactor | ♻️ | 리팩토링 (기능 변화 X) |
| test | ✅ | 테스트 추가/수정 |
| docs | 📝 | 문서 수정 |
| style | 💄 | 코드 포맷팅 (로직 X) |
| chore | 🔧 | 빌드 설정, 패키지 매니저 등 |
| ci | 🔁 | CI 설정 변경 |
| build | 📦 | 빌드 시스템 변경 |
| revert | ⏪ | 커밋 되돌리기 |

### 작성 예시

```
feat: 회원가입 API 추가
```
→ 훅이 자동으로 변환:
```
✨ feat: 회원가입 API 추가
```

### 커밋 제어

- `husky` + 커스텀 `commit-msg` 훅 사용
- 잘못된 커밋 타입 → 커밋 차단

---

## 3. PR 리뷰 룰

### 기본 원칙

- 최소 **1명 이상 Approve 필요**
- 리뷰 요청 후 **24시간 내 응답**
- PR 생성 시 GitHub Actions가 작성자를 제외한 팀원에게 리뷰를 자동 요청
- PR 작성자는 자동으로 assignee에 등록

### 예외 (hotfix)

- `main` hotfix는 **셀프 머지 가능**
- 단, 반드시 팀에 공유
- 문제 발생 시 책임 명확

---

## 4. Issue / PR 템플릿

### 목적

- 작업 맥락 공유
- 리뷰 효율 향상
- 커뮤니케이션 비용 감소

### 이슈 템플릿

- **Feature** (`.github/ISSUE_TEMPLATE/01-feature.yml`) — 적용 완료
- **Fix** (`.github/ISSUE_TEMPLATE/02-fix.yml`) — 적용 완료
- **Refactor** (`.github/ISSUE_TEMPLATE/03-refactor.yml`) — 적용 완료

### PR 템플릿

`.github/pull_request_template.md` — 적용 완료

---

## 5. 코드 스타일

### 기본 원칙

- **가독성 우선**
- **일관성 유지**

### 규칙

- 축약어 사용 ❌
- DTO → `data class` 사용 (Kotlin)
- 메서드 길이 **7줄 이내 권장**
- `else / else-if` 지양 → **early return**
- Builder 패턴 사용
- 추후 필요하면 추가 예정

---

## 6. 기술 스택

### Backend

- Kotlin
- Spring Framework

### Database / Cache

- PostgreSQL
- Redis

### Observability

- Spring Actuator
- Prometheus
- Grafana

### Infra

- GCP (Compute Engine, VPC)
- Docker
- Terraform
- GitHub Actions (CI/CD)

---

## 7. 아키텍처 & 구조

### 구조 방향

- 초기: 단일 모듈
- 이후: 멀티 모듈 확장

### 아키텍처

- Layered Architecture

```
Controller → UseCase(Facade) → Service → Repository
```

---

## 8. 환경 변수 & 시크릿 관리

### Git 서브모듈 기반 설정 관리

- 설정 파일은 private config 레포에서 관리
- 메인 레포에 submodule로 등록 (`secret/`)
- 민감 정보(DB 비밀번호, API 키 등)는 yml에 넣지 않고 환경변수 또는 볼륨 마운트로 주입
- 상세 가이드: `docs/submodule.md`

---

## 9. AI 활용 전략

### 목적

- 생산성 향상
- 코드 품질 개선

### 도구

- 코드 리뷰: CodeRabbit
- 모델 활용: HuggingFace (Emotion Classification)
