# Git Submodule 가이드

> 설정 파일(application.yml)을 private 레포에서 관리하기 위한 서브모듈 사용 가이드

## 사용 목적

config 레포는 환경별 설정을 코드와 같은 방식으로 추적하기 위해 사용한다.

- 설정 변경 이력을 commit 단위로 남긴다.
- 특정 배포 시점이 어떤 설정 commit을 사용했는지 메인 레포의 submodule pointer로 확인한다.
- 문제가 생겼을 때 설정 변경 diff를 확인하고 이전 commit으로 되돌릴 수 있다.
- GCP, OCI처럼 여러 클라우드 벤더를 함께 사용하므로, 특정 클라우드 Secret Manager에 설정 관리가 종속되지 않게 한다.
- 서버나 팀원이 필요한 설정 값을 직접 확인할 수 있게 하되, 접근 권한은 config 레포 권한으로 제한한다.

현재 config 레포는 민감 설정 접근 범위를 좁히기 위해 organization repo가 아니라 개인 private repository로 관리한다.
organization private repo에 두면 organization 권한이 있는 사람이 설정 값을 볼 수 있으므로, 필요한 팀원과 서버 deploy key/token에만 접근 권한을 부여한다.

## 구조

```
18th-team1-BE (메인 레포 - public)
├── app/src/main/resources/
│   ├── application.yml
│   └── application-local.yml
└── secret/  ← submodule (private)
    ├── application-dev.yml
    └── application-prod.yml
```

- 로컬 환경은 classpath 리소스(`application.yml`, `application-local.yml`)를 사용
- dev/prod 환경은 외부 config 레포의 profile yml을 사용
- 메인 레포에는 config 레포의 **커밋 해시(포인터)**만 저장됨
- config 레포 접근 권한이 없으면 설정 파일을 볼 수 없음
- 이미지에 yml을 포함하지 않고, **서버에서 볼륨 마운트로 주입**

## 접근 권한

config 레포는 private repository이므로 아래 권한이 필요하다.

- 새 팀원은 메인 BE 레포뿐 아니라 config 레포에도 초대되어야 한다.
- 배포 서버는 config 레포 read 권한이 있는 deploy key 또는 token을 사용해야 한다.
- CI에서 submodule checkout을 수행한다면 CI token에도 config 레포 read 권한이 있어야 한다.
- CodeRabbit 같은 외부 리뷰 도구에는 config 레포 접근 권한을 부여하지 않는다.

권한이 없는 환경에서는 GitHub가 private repository를 `Repository not found`처럼 응답할 수 있다.
따라서 외부 리뷰 도구에서 submodule remote 접근 실패 경고가 나올 수 있으며, 이는 config 레포 권한을 의도적으로 제한한 결과다.
병합 전에는 팀원과 배포 서버에서 submodule checkout이 가능한지만 확인한다.

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
# secret/application-dev.yml
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
