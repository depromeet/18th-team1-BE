# 로그인 및 인증 흐름

## 전체 흐름

현재 인증은 OAuth 로그인, refresh token 쿠키, access token 응답 조합으로 동작한다.
첫 OAuth 로그인 성공 시 URL에 access token을 붙이지 않고, HttpOnly refresh token 쿠키만 내려준다.
프론트는 로그인 callback 화면에서 refresh API를 호출해 access token을 발급받는다.

1. 프론트 OAuth 로그인 버튼은 백엔드 OAuth 시작 URL로 브라우저를 이동시킨다.

```text
GET /api/oauth2/authorization/kakao
GET /api/oauth2/authorization/google
```

2. 백엔드는 provider 로그인 페이지로 redirect한다.
3. provider는 로그인 성공 후 백엔드 callback URL로 redirect한다.

```text
/api/login/oauth2/code/kakao
/api/login/oauth2/code/google
```

4. 백엔드는 OAuth 사용자 정보를 조회하고 사용자를 생성하거나 갱신한다.
5. 백엔드는 refresh token을 발급해 `Set-Cookie`로 내려주고, 프론트 callback URL로 redirect한다.

```text
local: http://localhost:3000/login/callback
dev:   https://hiking.monster/login/callback
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
window.location.href = "/api/oauth2/authorization/google";
```

로컬에서 백엔드와 프론트를 각각 띄우면 백엔드 origin을 직접 지정한다.

```ts
window.location.href = "http://localhost:8080/api/oauth2/authorization/kakao";
window.location.href = "http://localhost:8080/api/oauth2/authorization/google";
```

## 개발용 토큰

개발 환경에서는 Swagger로 보호 API를 테스트할 수 있도록 개발용 access token 발급 API를 제공한다.

```text
GET /api/auth/dev-token
```

이 API는 `app.token.enabled=true`일 때만 bean으로 등록된다.
dev 환경에서는 활성화하고, prod 환경에서는 비활성화한다.

응답:

```json
{
  "accessToken": "..."
}
```

개발용 토큰은 고정 더미 사용자를 생성하거나 갱신한 뒤 access token을 발급한다.
refresh token 쿠키를 발급하는 OAuth 로그인 흐름과는 별개의 Swagger 테스트 편의 기능이다.

## OAuth Redirect URI

OAuth provider 개발자 페이지에는 프론트 callback URL이 아니라 provider가 백엔드로 돌아올 URL을 등록한다.

경로 기반 라우팅을 사용하는 dev 환경:

```text
https://hiking.monster/api/login/oauth2/code/kakao
```

로컬:

```text
http://localhost:8080/api/login/oauth2/code/kakao
```

Google OAuth2도 동일하게 백엔드 callback URL을 등록한다.
Google은 `openid` scope가 포함되면 OIDC 플로우로 처리되므로, Spring Security의 `oidcUserService`에서 사용자를 로드한다.

```text
https://hiking.monster/api/login/oauth2/code/google
http://localhost:8080/api/login/oauth2/code/google
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

## OAuth provider 처리

Kakao는 일반 OAuth2 사용자 정보 플로우를 사용한다.
Google은 `openid` scope 포함 시 OIDC 사용자 플로우를 사용한다.

| Provider | Spring principal | 처리 컴포넌트 |
|----------|------------------|---------------|
| Kakao | `OAuth2AuthenticatedUser` | `CustomOAuth2UserService` |
| Google | `OidcAuthenticatedUser` | `CustomOidcUserService` |

로그인 성공 후 `JwtIssueSuccessHandler`는 두 principal 타입을 모두 처리한다.
공통적으로 서비스 사용자 정보를 꺼낸 뒤 refresh token을 발급하고, refresh token cookie와 함께 프론트 callback URL로 redirect한다.

## OAuth 사용자 저장 정책

`users.provider_id`에는 Kakao, Google이 내려준 외부 사용자 ID 원문을 저장한다.
즉 `kakao_...`, `google_...`처럼 provider prefix를 붙이지 않는다.

provider 구분은 별도 `users.provider` 컬럼으로 관리하고, 중복 방지는 `(provider, provider_id)` 복합 unique 제약으로 처리한다.
따라서 서로 다른 provider에서 같은 외부 ID 문자열을 내려주더라도 DB 레벨에서 충돌하지 않는다.

신규 OAuth 사용자는 provider 표시 이름을 서비스 닉네임으로 사용하지 않는다.
서비스 닉네임은 랜덤 닉네임 생성기로 만들고, provider 표시 이름은 `provider_display_name`에 별도로 저장한다.

기존 OAuth 사용자가 다시 로그인하면 다음 값만 갱신한다.

- provider에서 이메일을 제공한 경우 `email`
- `provider_display_name`
- `last_login_at`
- `updated_at`

재로그인 시 `users.nickname`은 덮어쓰지 않는다.
서비스 닉네임 변경은 `PATCH /api/users/me`에서만 수행한다.

## OAuth 생성과 재로그인 흐름

OAuth 로그인은 `find -> create/update` 흐름으로 처리한다.

1. `(provider, provider_id)`로 기존 사용자를 조회한다.
2. 기존 사용자가 있으면 인증 가능한 상태인지 검증한다.
3. `ACTIVE` 사용자면 OAuth 로그인 정보를 갱신하고 사용자 정보를 반환한다.
4. 기존 사용자가 없으면 랜덤 닉네임을 생성해 신규 사용자를 insert한다.
5. insert는 `ON CONFLICT DO NOTHING`을 사용한다.
6. insert 결과가 없으면 같은 OAuth 계정의 동시 첫 로그인 가능성이 있으므로 다시 조회한다.
7. 재조회된 사용자가 있으면 기존 사용자 로그인 흐름으로 수렴한다.
8. 재조회된 사용자가 없으면 닉네임 충돌로 보고 최대 10회까지 닉네임을 다시 생성한다.

10회 안에 사용 가능한 닉네임을 만들지 못하면 `NICKNAME_GENERATION_FAILED`로 처리한다.

OAuth 로그인 시 인증 가능한 사용자 상태는 다음과 같다.

| 사용자 상태 | 처리 |
|-------------|------|
| `ACTIVE` | 로그인 가능 |
| `BLOCKED` | `AUTH_USER_BLOCKED` |
| `DELETED` 또는 `deleted_at` 존재 | `AUTH_USER_DELETED` |

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
| access token의 사용자 ID가 DB에 없음 | `401` | `인증된 사용자를 찾을 수 없습니다` |
| access token의 사용자가 차단됨 | `401` | `차단된 사용자입니다` |
| access token의 사용자가 탈퇴됨 | `401` | `탈퇴한 사용자입니다` |

`/api/auth/refresh`는 refresh token 쿠키로 access token을 재발급하는 공개 auth API다.
따라서 만료된 access token 헤더가 같이 오더라도 access token 오류로 먼저 차단하지 않고, refresh token 검증을 진행한다.

JWT 자체가 유효하더라도 claims의 사용자 ID가 실제 인증 가능한 사용자라는 보장은 없다.
따라서 보호 API 진입 전 `JwtAuthenticator`가 사용자 존재 여부와 상태를 중앙에서 검증한다.

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
이 설정이 없으면 `/login/callback`에 직접 진입하거나 redirect 되었을 때 Nginx가 정적 파일을 찾지 못해 404를 반환할 수 있다.

이 백엔드 repo에는 프론트 Nginx 설정 파일이 없다.
따라서 현재 repo 변경만으로 Nginx가 설정된 것은 아니며, 프론트 인스턴스의 Nginx 설정에 위 `/api/` proxy와 SPA fallback이 적용되어야 한다.

## 서버 설정 체크리스트

- 프론트 버튼은 `/api/oauth2/authorization/{provider}`로 브라우저 이동을 수행한다.
- Kakao Redirect URI는 `https://hiking.monster/api/login/oauth2/code/kakao`로 등록한다.
- Google Redirect URI는 `https://hiking.monster/api/login/oauth2/code/google`로 등록한다.
- 백엔드 `auth.oauth2.success-redirect-url`은 `https://hiking.monster/login/callback`이다.
- 백엔드 dev 설정에는 `server.forward-headers-strategy: framework`가 필요하다.
- 프론트 Nginx는 `/api/`를 백엔드 `8080`으로 proxy 한다.
- 프론트 Nginx는 SPA fallback으로 `/login/callback`을 프론트 앱에 전달한다.
- callback 화면은 `POST /api/auth/refresh`를 `credentials: "include"`로 호출한다.
- dev 환경에서 Swagger 테스트가 필요하면 `app.token.enabled=true` 상태에서 `GET /api/auth/dev-token`을 사용한다.
