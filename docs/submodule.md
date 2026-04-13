# Git Submodule 가이드

> 설정 파일(application.yml)을 private 레포에서 관리하기 위한 서브모듈 사용 가이드

## 구조

```
18th-team1-BE (메인 레포 - public)
└── secret/  ← submodule (private)
    ├── application.yml
    ├── application-dev.yml
    └── application-prod.yml
```

- 메인 레포에는 config 레포의 **커밋 해시(포인터)**만 저장됨
- config 레포 접근 권한이 없으면 설정 파일을 볼 수 없음
- 이미지에 yml을 포함하지 않고, **서버에서 볼륨 마운트로 주입**

## 최초 세팅 (clone 후)

레포를 처음 받은 뒤 submodule을 초기화해야 합니다.

```bash
git clone https://github.com/depromeet/18th-team1-BE.git
cd 18th-team1-BE
git submodule update --init
```

또는 clone 시 한 번에:

```bash
git clone --recurse-submodules https://github.com/depromeet/18th-team1-BE.git
```

> config 레포 접근 권한이 필요합니다. 권한이 없으면 관리자에게 요청하세요.

## 설정 파일 수정

config 레포의 파일을 수정하려면:

```bash
cd secret

# 파일 수정 후
git add .
git commit -m "chore: DB 설정 변경"
git push origin main
```

메인 레포에서 submodule 포인터도 업데이트해야 합니다:

```bash
cd /path/to/18th-team1-BE  # 메인 레포 루트로 이동
git add secret
git commit -m "chore: config submodule 업데이트"
```

## 최신 설정 가져오기

다른 팀원이 config 레포를 업데이트했을 때:

```bash
git submodule update --remote
```

## 배포 시 설정 주입 방식

Docker 이미지에 yml을 포함하지 않고, 서버에서 볼륨 마운트로 주입합니다.

```yaml
# docker-compose-dev.yml
services:
  app:
    image: ghcr.io/depromeet/18th-team1-be/api:latest
    volumes:
      - ./config:/app/config   # 서버의 config 디렉터리를 컨테이너에 마운트
```

서버에서는 config 레포를 deploy key로 clone하여 관리합니다:
```bash
git clone git@github-config:kimseonj/18th-team1-BE-config.git /opt/firstpenguin/app/config
```

## 민감 정보 관리 원칙

| 구분 | 저장 위치 | 예시 |
|------|-----------|------|
| 환경별 설정 | config 레포 (submodule) | DB URL, JPA 설정, 로깅 레벨 |
| 민감 정보 | 서버 `.env` | DB 비밀번호, API 키, JWT secret |

민감 정보는 yml에 placeholder로 작성합니다:

```yaml
# application-dev.yml
spring:
  datasource:
    url: jdbc:postgresql://db:5432/firstpenguin
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
```

실제 값은 서버의 `.env` 파일에서 Docker Compose 환경변수로 주입합니다.

## 주의사항

- config 레포에 **비밀번호, API 키를 직접 넣지 마세요** (placeholder 사용)
- submodule 변경 후 메인 레포에서 **포인터 커밋을 잊지 마세요**
- `.env` 파일은 `.gitignore`에 포함되어 있으므로 git에 올라가지 않습니다
- 서버에서 config 레포 접근은 **deploy key** (read-only)로 제한합니다
