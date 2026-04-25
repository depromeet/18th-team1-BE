# 디렉토리 구조 가이드

## 전체 패키지 구조

```
com.firstpenguin.app/
├── global/                          # 전역 공통 모듈
│   ├── exception/
│   │   ├── ErrorCode.kt             # 에러 코드 enum (HTTP status + code + message)
│   │   ├── CustomException.kt       # 비즈니스 예외 기본 클래스
│   │   └── GlobalExceptionHandler.kt # @RestControllerAdvice 전역 예외 처리
│   └── response/
│       └── ErrorResponse.kt         # 에러 응답 DTO
│
└── domain/                          # 도메인별 비즈니스 로직
    └── {domain}/                    # 예: user, emotion, record
        ├── controller/
        │   └── {Domain}Controller.kt
        ├── usecase/                  # Facade — 여러 Service를 조합
        │   └── {Domain}UseCase.kt
        ├── service/
        │   └── {Domain}Service.kt
        ├── repository/
        │   └── {Domain}Repository.kt
        └── dto/
            ├── {Domain}Request.kt
            └── {Domain}Response.kt
```

## 레이어별 역할

| 레이어 | 클래스 | 역할 |
|--------|--------|------|
| Controller | `{Domain}Controller` | HTTP 요청/응답 처리, 입력값 검증 (`@Valid`) |
| UseCase | `{Domain}UseCase` | 여러 Service를 조합하는 Facade, 트랜잭션 경계 |
| Service | `{Domain}Service` | 단일 도메인 비즈니스 로직 |
| Repository | `{Domain}Repository` | jOOQ를 활용한 DB 접근 |

> 의존 방향: `Controller → UseCase → Service → Repository`
>
> Controller는 UseCase만 참조한다. Service끼리 직접 참조하지 않는다.

## global 패키지

비즈니스 도메인과 무관한 공통 코드를 담는다.

- **exception**: `ErrorCode`, `CustomException`, `GlobalExceptionHandler`
- **response**: 공통 응답 DTO (현재 `ErrorResponse`)

## 에러 코드 카테고리 규칙

| prefix | 카테고리 |
|--------|----------|
| `C` | Common (공통) |
| `A` | Auth (인증/인가) |
| `U` | User (사용자) |
| 추가 필요 시 팀 합의 후 확장 | |

예시:
```kotlin
// ErrorCode.kt에 추가
USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "사용자를 찾을 수 없습니다"),
```

예외 사용:
```kotlin
throw CustomException(ErrorCode.USER_NOT_FOUND)
```

## jOOQ 생성 코드

```
app/build/generated-sources/jooq/
└── com/firstpenguin/app/jooq/     # 자동 생성 — 직접 수정 금지
```

## 명명 규칙

- 파일명: `{Domain}` prefix 사용 (예: `UserController`, `UserUseCase`)
- DTO: `data class` 사용
- Request DTO: `{Action}{Domain}Request` (예: `CreateUserRequest`)
- Response DTO: `{Domain}Response` 또는 `{Action}{Domain}Response`
