# Husky 도입 가이드

> 18th-team1-BE 프로젝트의 Git Hook 관리 도구 Husky 사용 가이드

## 1. Husky란?

Husky는 **Git Hook을 쉽게 관리**할 수 있게 해주는 도구다. Git에는 원래 `.git/hooks/` 디렉터리에 훅 스크립트를 둘 수 있지만, 이 폴더는 버전 관리가 안 돼서 팀원들과 공유가 불가능하다. Husky는 훅 스크립트를 **레포지토리 내부(`.husky/`)에 두고 공유**할 수 있게 해준다.

## 2. 왜 써야 하는가?

### 2.1. 커밋 컨벤션 강제
- 팀원마다 커밋 메시지 스타일이 다르면 히스토리가 지저분해진다.
- `commit-msg` 훅에서 메시지 형식을 검사하면 **커밋 시점에 자동으로 차단**된다.
- PR 리뷰에서 "커밋 메시지 고쳐주세요"라는 비생산적인 코멘트가 사라진다.

### 2.2. 브로큰 코드 유입 방지
- `pre-commit`에서 컴파일 검증(`./gradlew testClasses`)만 해도 **빌드가 깨진 상태의 커밋**을 원천 차단할 수 있다.
- `pre-push`에서 전체 테스트를 검증하면, **깨진 코드가 원격에 올라가는 일 자체를 방지**한다.
- CI가 돌아서 실패 알림이 올 때까지 기다릴 필요가 없다 → 로컬에서 먼저 걸린다.

### 2.3. 비밀키 유출 방지
- `gitleaks` 같은 도구를 `pre-commit`에 엮으면 **API 키, 패스워드, 토큰이 실수로 커밋되는 사고**를 막을 수 있다.
- 한 번 git history에 올라간 비밀키는 지우기 매우 어렵다. **사전 차단이 유일한 해결책**이다.

### 2.4. 공유 가능한 훅
- `.husky/` 디렉터리가 레포에 포함되므로, **새 팀원이 `npm install` 한 번으로 모든 훅이 세팅**된다.
- 로컬 환경 차이로 인한 "제 컴퓨터에선 되는데요" 문제를 줄인다.

## 3. 팀원 사용법 (이미 세팅된 레포에 합류할 때)

### 3.1. 사전 조건
- **Node.js 설치 필수** (husky는 npm 패키지)
- 권장 버전: Node.js 20 이상
- 설치 확인:
  ```bash
  node -v
  npm -v
  ```

### 3.2. 초기 설정
레포 클론 후 프로젝트 루트에서:
```bash
npm install
```
끝. `package.json`의 `"prepare": "husky"` 스크립트가 자동으로 `.husky/` 훅들을 활성화한다.

### 3.3. 커밋 작성법
```bash
git add .
git commit -m "feat: 회원가입 API 추가"
```
→ `commit-msg` 훅이 자동으로 이모지를 붙여서 다음과 같이 변환:
```text
✨ feat: 회원가입 API 추가
```

### 3.4. 훅이 실행되는 시점
| 시점 | 훅 | 실행 내용 |
|------|-----|-----------|
| `git commit` 직전 | `pre-commit` | 비밀키 검사 + 컴파일 검증 |
| `git commit` 메시지 작성 후 | `commit-msg` | 타입 검증 + 이모지 자동 삽입 |
| `git push` 직전 | `pre-push` | 전체 테스트 검증 |

### 3.5. 훅이 실패했을 때
- **컴파일 에러**: IDE에서 에러를 먼저 해결하고 다시 커밋
- **테스트 실패**: 원인 파악 후 수정, 절대 `--no-verify`로 우회하지 말 것
- **커밋 타입 오류**: 허용 타입 중 하나를 사용 (feat, fix, refactor 등)

## 4. 초기 세팅 방법 (신규 도입 시)

> 이미 세팅된 레포라면 이 섹션은 건너뛰어도 된다.

프로젝트 루트에서:

```bash
# 1. package.json 생성
npm init -y

# 2. Husky 설치
npm install --save-dev husky

# 3. Husky 초기화 (.husky/ 디렉터리 생성)
npx husky init
```

### package.json 설정
```json
{
  "name": "team1-be",
  "version": "0.1.0",
  "private": true,
  "devDependencies": {
    "husky": "^9.1.7"
  },
  "scripts": {
    "prepare": "husky"
  }
}
```

## 5. Hook 구성

### 5.1. `commit-msg` — 커밋 메시지 검증 및 이모지 자동 삽입

**역할**
- 커밋 타입 검증 (feat, fix, refactor 등)
- 타입에 맞는 이모지 자동 prefix
- `Co-authored-by` 트레일러 차단 (AI 공동 커밋 방지)

**커밋 타입**

| 타입 | 이모지 | 설명 |
|------|--------|------|
| feat | ✨ | 새로운 기능 추가 |
| fix | 🐛 | 버그 수정 |
| perf | ⚡️ | 성능 개선 |
| refactor | ♻️ | 리팩토링 (기능 변화 X) |
| test | ✅ | 테스트 추가/수정 |
| docs | 📝 | 문서 수정 |
| style | 💄 | 코드 포맷팅, 세미콜론 등 (로직 X) |
| chore | 🔧 | 빌드 설정, 패키지 매니저 등 |
| ci | 🔁 | CI 설정 변경 |
| build | 📦 | 빌드 시스템 변경 |
| revert | ⏪ | 커밋 되돌리기 |

**작성 예시**
```text
feat: 회원가입 API 추가
```
→ 자동으로 변환:
```text
✨ feat: 회원가입 API 추가
```

### 5.2. `pre-commit` — 커밋 직전 검증

**역할**
- 비밀키 스캔 (gitleaks)
- 최소 컴파일 검증 (`./gradlew testClasses`)

**왜 `test`가 아니라 `testClasses`?**
- 전체 테스트는 느리다 → 개발 흐름 방해
- 컴파일만 확인해도 **import 누락, 문법 오류, 타입 오류**는 다 잡힌다
- 전체 테스트는 `pre-push`에서 수행

### 5.3. `pre-push` — 푸시 직전 검증

**역할**
- 전체 테스트 실행 (`./gradlew clean test`)

**왜 push에만?**
- 커밋은 자주 한다 → 빠른 피드백 필요
- 푸시는 덜 한다 → 한 번에 꼼꼼히 검증
- 원격에 올라가는 순간부터 팀 전체에 영향 → 여기서 막는 게 마지막 방어선

## 6. 훅 우회가 필요할 때

정말 급할 때는 `--no-verify` 플래그로 우회할 수 있다:
```bash
git commit --no-verify -m "..."
git push --no-verify
```

**하지만 원칙적으로 금지.** 우회가 필요하다는 건 훅 설정이 잘못됐거나, 진짜 문제가 있다는 신호다. 팀 회의에서 논의 후 설정을 조정하는 게 맞다.

## 7. 주의사항

### 7.1. Node.js 의존성
- Java 프로젝트에 Node 의존성이 들어오는 게 불편할 수 있음
- 하지만 `devDependencies`이고 훅 관리만 하므로 부담은 적음
- 대안: 순수 shell 기반 `pre-commit` 프레임워크도 있지만, husky가 가장 대중적

### 7.2. CI 환경
- CI에서는 `npm ci --ignore-scripts`로 husky 설치를 스킵하는 게 좋음 (불필요)
- `HUSKY=0` 환경변수로도 비활성화 가능

### 7.3. 훅 실행 속도
- `pre-commit`은 **3초 이내**로 유지하는 게 좋음 (넘어가면 개발자가 `--no-verify` 남용)
- 무거운 작업은 `pre-push`로 옮기기

### 7.4. Gradle 실행 경로 주의
- husky 자체는 어떤 프로젝트 구조에서도 동일하게 동작한다. 달라지는 건 **훅 스크립트 안의 Gradle 명령어 경로**뿐이다.
- 현재 프로젝트는 gradle wrapper가 `app/` 하위에 있어서, 훅에서 `cd app && ./gradlew ...` 형태로 이동이 필요하다.
- 만약 추후 멀티모듈로 전환하면서 wrapper를 루트로 옮기면, 훅 스크립트도 `./gradlew ...`로 단순화해야 한다.

## 8. 참고

- [Husky 공식 문서](https://typicode.github.io/husky/)
- [Conventional Commits](https://www.conventionalcommits.org/)
