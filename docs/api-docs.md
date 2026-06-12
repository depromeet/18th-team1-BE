# API 문서와 Swagger

## 목표

Springdoc OpenAPI를 사용해 백엔드 API를 Swagger UI에서 확인하고 테스트한다.
모든 공개 API 경로는 서버 context-path `/api`를 포함한다.

관련 이슈: #18

## 접근 경로

```text
Swagger UI: /api/swagger-ui.html
Swagger UI: /api/swagger-ui/index.html
OpenAPI JSON: /api/v3/api-docs
```

prefix 없는 루트 경로는 사용하지 않는다.

```text
/v3/api-docs        -> 사용하지 않음
/swagger-ui.html    -> 사용하지 않음
```

## 서버 prefix

애플리케이션은 공통 context-path를 사용한다.

```yaml
server:
  servlet:
    context-path: /api
```

OpenAPI server URL도 `/api`로 설정한다.
Swagger에서 보이는 API path는 context-path 내부 경로를 기준으로 표시될 수 있으므로, 외부에서 직접 호출할 때는 `/api` prefix를 포함한다.

## 인증

OpenAPI에는 HTTP Bearer JWT 인증 스키마를 등록한다.

```text
Authorization: Bearer {accessToken}
```

Swagger UI에서 보호 API를 테스트할 때는 우상단 Authorize 버튼에 access token을 입력한다.
dev 환경에서는 `GET /api/auth/dev-token`으로 개발용 access token을 발급받을 수 있다.

## OAuth 로그인 시작 경로

OAuth 로그인 시작 경로는 Spring Security가 제공한다.

```text
GET /api/oauth2/authorization/kakao
GET /api/oauth2/authorization/google
```

현재 OpenAPI customizer는 Kakao 로그인 시작 경로를 명시적으로 문서에 추가한다.
Google 로그인 흐름과 callback 정책은 `docs/auth-flow.md`를 기준으로 관리한다.

## 정렬과 노출

Swagger UI는 operation과 tag를 알파벳순으로 정렬한다.

```yaml
springdoc:
  swagger-ui:
    operations-sorter: alpha
    tags-sorter: alpha
```

환경별 Swagger 노출 여부는 profile 설정에서 `springdoc.api-docs.enabled`, `springdoc.swagger-ui.enabled`로 제어한다.
운영 환경에서 Swagger를 닫아야 하면 prod profile에서 두 값을 `false`로 둔다.
