# CD 파이프라인

> dev/main 브랜치 머지 시 자동으로 Docker 이미지 빌드 → GHCR 푸시 → 서버 배포

## 전체 흐름

```
PR merge → dev 또는 main 브랜치
  ↓
GitHub Actions (cd.yml)
  1. 소스코드 checkout
  2. GHCR 로그인
  3. Docker 이미지 빌드 (멀티스테이지)
  4. GHCR에 이미지 push (latest + commit SHA 태그)
  5. SSH로 서버 접속 → 배포 스크립트 실행
```

배포 스크립트 (`deploy-dev.sh`):
```
1. config 레포 pull (설정 파일 최신화)
2. 최신 이미지 pull
3. docker compose로 app 컨테이너 재시작 (DB 유지)
4. 이전 이미지 정리
```

## 구성 요소

### 1. Dockerfile (멀티스테이지 빌드)

```
[1단계 - 빌드]                      [2단계 - 실행]
gradle:8.13-jdk21                   eclipse-temurin:21-jre
├── 소스코드 복사                    ├── jar만 복사
├── gradle build -x test            └── java -jar app.jar
└── jar 생성 (~1GB)                     (~300MB)
```

- 빌드 도구, 소스코드는 최종 이미지에 포함되지 않음
- 테스트는 CI에서 이미 통과했으므로 `-x test`로 스킵
- yml 파일은 이미지에 포함하지 않음 (서버에서 볼륨 마운트)

### 2. 이미지 레지스트리 (GHCR)

| 항목 | 값 |
|------|------|
| 레지스트리 | `ghcr.io` |
| 이미지 경로 | `ghcr.io/depromeet/18th-team1-be/api` |
| 태그 | `latest` + commit SHA |

**GHCR 선택 이유**:
- GitHub Actions에서 `GITHUB_TOKEN`으로 바로 인증 (별도 설정 불필요)
- 클라우드 벤더 종속 없음 (OCI 이전 시에도 변경 불필요)
- public 이미지 저장공간 무제한

### 3. GitHub Environment

브랜치에 따라 환경이 자동 선택됨:

| 브랜치 | Environment | 배포 대상 |
|--------|-------------|-----------|
| dev | `dev` | dev 서버 |
| main | `prod` | prod 서버 |

환경별로 secrets와 variables를 분리하여 동일한 워크플로우로 dev/prod 배포 가능.

## GitHub 설정

### Secrets (Settings → Environments → 환경별 등록)

| Secret | 설명 |
|--------|------|
| `SERVER_HOST` | 서버 외부 IP |
| `SERVER_USER` | SSH 사용자 (ubuntu) |
| `SERVER_SSH_KEY` | 배포용 SSH 개인키 |

### Variables (Settings → Environments → 환경별 등록)

| Variable | dev | prod |
|----------|-----|------|
| `DEPLOY_SCRIPT_PATH` | `/opt/firstpenguin/scripts/deploy-dev.sh` | `/opt/firstpenguin/scripts/deploy-prod.sh` |

### SSH 키 생성 방법

```bash
# 로컬에서 키 생성
ssh-keygen -t ed25519 -C "github-actions-deploy" -f ~/.ssh/deploy_key

# 공개키를 서버에 등록
cat ~/.ssh/deploy_key.pub | ssh ubuntu@{서버IP} "cat >> ~/.ssh/authorized_keys"

# 개인키를 GitHub Secrets에 등록 (SERVER_SSH_KEY)
cat ~/.ssh/deploy_key
```

## 서버 구조

```
/opt/firstpenguin/
├── scripts/
│   └── deploy-dev.sh           ← 배포 스크립트
└── app/
    ├── docker-compose-dev.yml  ← 컨테이너 구성
    ├── .env                    ← DB 환경변수 (비밀번호 등)
    └── config/                 ← config 레포 clone (deploy key로 접근)
        ├── application.yml
        └── application-dev.yml
```

## 서버 초기 세팅 (최초 1회)

```bash
# 1. 디렉터리 생성
sudo mkdir -p /opt/firstpenguin/{scripts,app/config}
sudo chown -R ubuntu:ubuntu /opt/firstpenguin

# 2. 배포 스크립트 배치
cp deploy-dev.sh /opt/firstpenguin/scripts/
chmod +x /opt/firstpenguin/scripts/deploy-dev.sh

# 3. docker-compose-dev.yml 배치
cp docker-compose-dev.yml /opt/firstpenguin/app/

# 4. .env 파일 생성
cat > /opt/firstpenguin/app/.env << 'EOF'
POSTGRES_DB=firstpenguin
POSTGRES_USER=postgres
POSTGRES_PASSWORD={실제 비밀번호}
EOF

# 5. config 레포 clone (deploy key 설정 후)
git clone git@github-config:kimseonj/18th-team1-BE-config.git /opt/firstpenguin/app/config

# 6. DB 먼저 실행
cd /opt/firstpenguin/app
docker compose -f docker-compose-dev.yml up -d db
```

## 배포 확인

```bash
# 컨테이너 상태 확인
docker ps --filter "name=firstpenguin"

# 앱 로그 확인
docker logs firstpenguin-api

# 헬스 체크
curl http://localhost:8080/actuator/health
```

## 롤백

```bash
# 이전 커밋의 이미지로 변경
docker pull ghcr.io/depromeet/18th-team1-be/api:{이전 commit SHA}

cd /opt/firstpenguin/app
docker compose -f docker-compose-dev.yml up -d --force-recreate app
```

commit SHA 태그를 함께 push하기 때문에, 특정 버전으로 언제든 롤백 가능.
