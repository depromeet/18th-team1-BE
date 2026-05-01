# 로그인 및 인증 흐름

## 전체 흐름

현재 인증은 OAuth 로그인, refresh token 쿠키, access token 응답 조합으로 동작한다.
첫 OAuth 로그인 성공 시 URL에 access token을 붙이지 않고, HttpOnly refresh token 쿠키만 내려준다.
프론트는 로그인 callback 화면에서 refresh API를 호출해 access token을 발급받는다.

1. 프론트 카카오 로그인 버튼은 백엔드 OAuth 시작 URL로 브라우저를 이동시킨다.

```text
GET /api/oauth2/authorization/kakao
```

2. 백엔드는 카카오 로그인 페이지로 redirect한다.
3. 카카오는 로그인 성공 후 백엔드 callback URL로 redirect한다.

```text
/api/login/oauth2/code/kakao
```

4. 백엔드는 OAuth 사용자 정보를 조회하고 사용자를 생성하거나 갱신한다.
5. 백엔드는 refresh token을 발급해 `Set-Cookie`로 내려주고, 프론트 callback URL로 redirect한다.

```text
local: http://localhost:3000/login/kakao/callback
dev:   https://hiking.monster/login/kakao/callback
```

6. 프론트 callback 화면은 refresh API를 호출한다.

```ts
const response = await fetch("/api/auth/refresh", {
  method: "POST",
  credentials: "include",
});

const { accessToken } = await response.json();
```

7. 프론트는 이후 보호 API 요청에 access token을 `Authorization` 헤더로 보낸다.

```text
Authorization: Bearer {accessToken}
```

## 프론트에서 호출할 URL

프론트와 백엔드가 같은 공개 도메인에서 경로 기반 라우팅을 사용하면 프론트는 상대 경로를 사용한다.

```ts
window.location.href = "/api/oauth2/authorization/kakao";
```

로컬에서 백엔드와 프론트를 각각 띄우면 백엔드 origin을 직접 지정한다.

```ts
window.location.href = "http://localhost:8080/api/oauth2/authorization/kakao";
```

## 카카오 개발자 Redirect URI

카카오 개발자 페이지에는 프론트 callback URL이 아니라 카카오가 백엔드로 돌아올 URL을 등록한다.

경로 기반 라우팅을 사용하는 dev 환경:

```text
https://hiking.monster/api/login/oauth2/code/kakao
```

로컬:

```text
http://localhost:8080/api/login/oauth2/code/kakao
```

## refresh token 쿠키

refresh token은 `refresh_token` 쿠키로 내려간다.

| 속성 | 값 |
|------|----|
| `HttpOnly` | `true` |
| `Secure` | local `false`, dev `true` |
| `SameSite` | `Lax` |
| `Path` | `/api/auth` |
| `Max-Age` | `auth.jwt.refresh-token-expiration` |

`SameSite=Lax`는 `hiking.monster`와 `api.hiking.monster`처럼 같은 site의 서브도메인 구조에서도 사용할 수 있다.
프론트와 백엔드가 완전히 다른 site를 쓰는 경우에만 `SameSite=None; Secure`가 필요하다.

## access token 갱신

프론트는 access token이 필요할 때 refresh API를 호출한다.

```text
POST /api/auth/refresh
Cookie: refresh_token={refreshToken}
```

응답:

```json
{
  "accessToken": "..."
}
```

refresh API는 refresh token을 회전한다.
즉, 기존 refresh token을 검증한 뒤 새 access token과 새 refresh token 쿠키를 발급한다.

## 로그아웃

프론트는 로그아웃 시 refresh token 쿠키를 포함해 logout API를 호출한다.

```ts
await fetch("/api/auth/logout", {
  method: "POST",
  credentials: "include",
});
```

백엔드는 저장된 refresh token hash를 삭제하고 refresh token 쿠키를 만료시킨다.

## JWT payload

JWT는 HS256으로 서명한다.
서명 키는 `auth.jwt.secret`을 SHA-256으로 해시한 바이트 배열을 사용한다.

access token payload:

| claim | 값 |
|-------|----|
| `sub` | 사용자 ID 문자열 |
| `jti` | 매번 새로 생성하는 UUID |
| `iat` | 발급 시각 |
| `exp` | 만료 시각 |
| `type` | `access` |

refresh token payload:

| claim | 값 |
|-------|----|
| `sub` | 사용자 ID 문자열 |
| `jti` | 매번 새로 생성하는 UUID |
| `iat` | 발급 시각 |
| `exp` | 만료 시각 |
| `type` | `refresh` |

인증 principal은 사용자 ID만 가진다.
DB에는 refresh token 원문을 저장하지 않고 SHA-256 hash만 저장한다.

## OAuth 사용자 식별자 저장 정책

`users.provider_id`에는 Kakao, Google이 내려준 외부 사용자 ID 원문을 저장한다.
즉 `kakao_...`, `google_...`처럼 provider prefix를 붙이지 않는다.

provider 구분은 별도 `users.provider` 컬럼으로 관리하고, 중복 방지는 `(provider, provider_id)` 복합 unique 제약으로 처리한다.
따라서 서로 다른 provider에서 같은 외부 ID 문자열을 내려주더라도 DB 레벨에서 충돌하지 않는다.

## 인증 에러 응답

에러 응답은 `message`만 내려준다.
프론트가 access token과 refresh token 실패를 구분할 수 있도록 메시지를 토큰 종류별로 분리한다.

| 상황 | HTTP status | message |
|------|-------------|---------|
| 보호 API에 인증 정보가 없음 | `401` | `인증이 필요합니다` |
| access token 만료 | `401` | `Access Token이 만료되었습니다` |
| access token 형식 이상, 서명 불일치, type 불일치 | `401` | `유효하지 않은 Access Token입니다` |
| refresh token 쿠키 없음 | `401` | `Refresh Token이 필요합니다` |
| refresh token 만료 | `401` | `Refresh Token이 만료되었습니다` |
| refresh token 형식 이상, 서명 불일치, type 불일치, DB hash 없음 | `401` | `유효하지 않은 Refresh Token입니다` |

`/api/auth/refresh`는 refresh token 쿠키로 access token을 재발급하는 공개 auth API다.
따라서 만료된 access token 헤더가 같이 오더라도 access token 오류로 먼저 차단하지 않고, refresh token 검증을 진행한다.

## Nginx 경로 기반 라우팅

프론트와 백엔드가 다른 인스턴스에 있어도, 공개 도메인 기준으로 경로 기반 라우팅을 사용하면 프론트 Nginx가 `/api/**` 요청을 백엔드로 proxy 해야 한다.
즉 브라우저는 `https://hiking.monster/api/...`로 요청하고, Nginx가 백엔드 인스턴스의 `8080` 포트로 전달한다.

예시:

```nginx
server {
    listen 443 ssl;
    server_name hiking.monster;

    location /api/ {
        proxy_pass http://BACKEND_PRIVATE_IP_OR_DNS:8080;

        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Forwarded-Host $host;
        proxy_set_header X-Forwarded-Port $server_port;
    }

    location / {
        try_files $uri $uri/ /index.html;
    }
}
```

`proxy_pass` 뒤에 `/`를 붙이지 않아야 `/api` path가 백엔드에 그대로 전달된다.
백엔드는 `server.servlet.context-path: /api`를 사용하므로 `/api` prefix가 유지되어야 한다.

`location /`의 `try_files` 설정은 SPA 라우팅을 위해 필요하다.
이 설정이 없으면 `/login/kakao/callback`에 직접 진입하거나 redirect 되었을 때 Nginx가 정적 파일을 찾지 못해 404를 반환할 수 있다.

이 백엔드 repo에는 프론트 Nginx 설정 파일이 없다.
따라서 현재 repo 변경만으로 Nginx가 설정된 것은 아니며, 프론트 인스턴스의 Nginx 설정에 위 `/api/` proxy와 SPA fallback이 적용되어야 한다.

## 서버 설정 체크리스트

- 프론트 버튼은 `/api/oauth2/authorization/kakao`로 브라우저 이동을 수행한다.
- 카카오 개발자 Redirect URI는 `https://hiking.monster/api/login/oauth2/code/kakao`로 등록한다.
- 백엔드 `auth.oauth2.success-redirect-url`은 `https://hiking.monster/login/kakao/callback`이다.
- 백엔드 dev 설정에는 `server.forward-headers-strategy: framework`가 필요하다.
- 프론트 Nginx는 `/api/`를 백엔드 `8080`으로 proxy 한다.
- 프론트 Nginx는 SPA fallback으로 `/login/kakao/callback`을 프론트 앱에 전달한다.
- callback 화면은 `POST /api/auth/refresh`를 `credentials: "include"`로 호출한다.
