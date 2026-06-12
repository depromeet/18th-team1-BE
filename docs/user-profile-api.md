# 사용자 프로필 API

## 목표

로그인 사용자가 내 정보를 조회하고, 닉네임 또는 프로필 이미지를 수정할 수 있게 한다.

관련 이슈: #76

## API

아래 API 경로는 서버 context-path `/api`를 포함한 공개 경로로 표기한다.

```text
GET /api/users/me
PATCH /api/users/me
Authorization: Bearer {accessToken}
```

두 API 모두 로그인 사용자만 호출할 수 있다.

## 내 정보 조회

```text
GET /api/users/me
```

응답:

```json
{
  "id": 1,
  "email": "user@example.com",
  "nickname": "책읽는펭귄",
  "profileImageUrl": "https://cdn.example.com/profile.png"
}
```

| 필드 | 설명 |
|------|------|
| `id` | 사용자 ID |
| `email` | OAuth provider에서 제공한 이메일. 제공되지 않으면 `null` |
| `nickname` | 서비스에서 사용하는 사용자 닉네임 |
| `profileImageUrl` | `users.profile_image_id`로 연결된 이미지 URL. 연결된 이미지가 없으면 `null` |

`profileImageUrl`은 `images.url`을 조회해서 응답에 포함한다.
클라이언트는 `profileImageId`가 아니라 조회 가능한 URL만 받는다.

## 내 프로필 수정

```text
PATCH /api/users/me
Content-Type: application/json
```

요청:

```json
{
  "nickname": "새닉네임",
  "profileImageId": 42
}
```

| 필드 | 설명 |
|------|------|
| `nickname` | 변경할 닉네임. `null`이면 변경하지 않음 |
| `profileImageId` | 변경할 프로필 이미지 ID. `null`이면 변경하지 않음 |

`nickname`, `profileImageId` 중 하나 이상은 반드시 포함해야 한다.
두 값이 모두 `null`이면 `INVALID_INPUT`으로 처리한다.

`null` 필드는 변경하지 않는다.
따라서 현재 API로는 프로필 이미지를 제거할 수 없다.

응답은 수정 후 `UserResponse`를 반환한다.

```json
{
  "id": 1,
  "email": "user@example.com",
  "nickname": "새닉네임",
  "profileImageUrl": "https://cdn.example.com/profile.png"
}
```

## 프로필 이미지 변경 흐름

프로필 이미지는 이미지 업로드 공통 흐름을 사용한다.

```text
POST /api/images/presigned-url
body: { "type": "USER_PROFILE", "contentType": "image/webp" }
<- { "presignedUrl": "...", "imageId": 42 }

클라이언트가 presignedUrl로 GCS PUT 업로드

PATCH /api/users/me
body: { "profileImageId": 42 }
```

`profileImageId`는 서버가 발급한 `imageId`여야 한다.
존재하지 않는 이미지 ID를 전달하면 `INVALID_INPUT`으로 처리한다.

자세한 이미지 정책은 `docs/image-policy.md`를 따른다.

## 닉네임 정책

닉네임은 공백 문자열을 허용하지 않는다.
예약 닉네임은 사용할 수 없다.
현재 예약 닉네임은 다음과 같다.

```text
개발자
```

다른 사용자가 이미 사용 중인 닉네임으로 변경하면 `NICKNAME_ALREADY_EXISTS`로 처리한다.
사전 중복 조회를 통과해도 동시에 같은 닉네임으로 변경될 수 있으므로, 최종 방어는 `users.nickname` unique 제약과 `DuplicateKeyException` 변환으로 처리한다.

## OAuth 닉네임과의 관계

OAuth provider에서 내려준 표시 이름은 서비스 닉네임을 덮어쓰지 않는다.
신규 가입 시 서비스 닉네임은 랜덤 닉네임 생성 정책으로 만든다.
provider 표시 이름은 `provider_display_name`에 별도로 저장한다.

기존 사용자가 다시 로그인해도 `users.nickname`은 유지된다.
닉네임 변경은 `PATCH /api/users/me`에서만 수행한다.

## 예외와 경계 조건

| 상황 | 처리 |
|------|------|
| access token 없음, 만료, 위조 | 인증 에러 응답 |
| `nickname`, `profileImageId` 모두 `null` | `INVALID_INPUT` |
| `nickname` blank 또는 예약 닉네임 | `INVALID_INPUT` |
| `nickname` 중복 | `NICKNAME_ALREADY_EXISTS` |
| `profileImageId`가 존재하지 않음 | `INVALID_INPUT` |
| 프로필 이미지 제거 요청 | 현재 미지원. `null`은 변경 없음으로 처리 |

## 테스트

다음 시나리오를 검증한다.

- 내 정보 조회 시 `profileImageId`가 있으면 `profileImageUrl`을 응답한다.
- `PATCH /api/users/me`는 닉네임만 변경할 수 있다.
- `PATCH /api/users/me`는 프로필 이미지만 변경할 수 있다.
- 두 필드가 모두 `null`이면 실패한다.
- 존재하지 않는 `profileImageId`면 실패한다.
- 공백 또는 예약 닉네임이면 실패한다.
- 이미 사용 중인 닉네임이면 실패한다.
- unique 충돌이 발생해도 `NICKNAME_ALREADY_EXISTS`로 변환한다.
