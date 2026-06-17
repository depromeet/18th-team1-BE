# 회원 탈퇴 정책

## 목표

회원 탈퇴는 사용자 row와 서비스 데이터를 즉시 삭제하지 않는다.
스크랩, 추천, 월간 정산, 분석 로그처럼 `user_id`로 연결된 도메인 데이터는 모두 보존한다.

사용자 계정과 OAuth 인증 계정은 분리해서 관리한다.
`users.id`는 서비스 내부 사용자 식별자로 유지하고, OAuth provider 식별자는 `oauth_accounts`에서 관리한다.

탈퇴 신청 후 30일 동안은 같은 OAuth 로그인으로 탈퇴를 자동 취소할 수 있다.
30일이 지난 뒤에는 탈퇴 확정 처리에서 공개 식별 정보와 OAuth 연결 정보를 최소한으로 마스킹한다.

## 단계별 적용 범위

첫 번째 PR에서는 다음만 적용한다.

- 회원 탈퇴 정책 문서화
- `oauth_accounts` 테이블 생성 Flyway 추가

두 번째 코드 변경 PR에서 처리할 범위는 다음과 같다.

- OAuth 로그인 조회/생성/갱신 로직 변경
- 탈퇴 요청 API와 탈퇴 취소 로직 구현
- `users` 상태와 탈퇴 유예 컬럼 추가
- 기존 `users` OAuth 데이터를 `oauth_accounts`로 이관한 뒤 `users` OAuth 관련 컬럼 정리

테이블 생성과 코드 변경을 분리하면 기존 로그인 로직에 영향을 주지 않고 스키마를 먼저 배포할 수 있다.
다만 `oauth_accounts`를 읽는 코드가 들어가기 전까지 새 테이블은 사용되지 않는다.
기존 데이터 이관은 Flyway에 넣지 않고 운영 반영 시 수동 SQL로 처리한다.

## 테이블 구조

### users

`users`는 서비스 내부 사용자와 프로필 상태를 관리한다.
최종 구조에서는 OAuth provider 식별자를 직접 들지 않는다.

```text
users
- id
- nickname
- profile_image_id
- status
- withdrawal_requested_at
- withdrawal_due_at
- deleted_at
- created_at
- updated_at
```

### oauth_accounts

`oauth_accounts`는 외부 OAuth 계정과 서비스 사용자의 연결을 관리한다.
프로젝트 DB 규칙에 따라 DB foreign key는 만들지 않고, `user_id`를 느슨한 참조로 둔다.

```text
oauth_accounts
- id
- user_id
- provider
- provider_id
- email
- provider_display_name
- last_login_at
- disconnected_at
- created_at
- updated_at
```

활성 OAuth 계정은 `disconnected_at IS NULL`인 row다.
탈퇴 확정 또는 계정 연결 해제 후에는 `disconnected_at`을 채워 기존 연결을 끊는다.

## oauth_accounts 제약

동일한 OAuth 계정이 동시에 여러 사용자에게 연결되지 않도록 활성 계정 기준 unique index를 둔다.

```sql
CREATE UNIQUE INDEX oauth_accounts_provider_provider_id_active_uidx
ON oauth_accounts (provider, provider_id)
WHERE disconnected_at IS NULL;
```

한 사용자가 같은 provider 계정을 여러 개 활성 연결하지 않도록 user-provider 기준 active unique도 둔다.

```sql
CREATE UNIQUE INDEX oauth_accounts_user_provider_active_uidx
ON oauth_accounts (user_id, provider)
WHERE disconnected_at IS NULL;
```

이 구조에서는 탈퇴 확정 후 같은 OAuth 계정으로 새 userId 가입이 가능하다.
기존 연결 row의 `disconnected_at`이 채워지면 active unique 대상에서 빠지기 때문이다.

## 사용자 상태

`users.status`는 다음 상태를 사용한다.

| 상태 | 의미 | 인증 가능 여부 |
|------|------|----------------|
| `ACTIVE` | 정상 사용자 | 가능 |
| `BLOCKED` | 차단 사용자 | 불가 |
| `WITHDRAWAL_REQUESTED` | 탈퇴 신청 후 30일 취소 가능 기간 | 일반 API 불가, OAuth 로그인 시 자동 취소 |
| `DELETED` | 탈퇴 확정 사용자 | 불가 |

`WITHDRAWAL_REQUESTED`는 아직 탈퇴가 확정되지 않은 상태다.
이 기간에는 사용자 닉네임과 OAuth 연결을 유지한다.

`DELETED`는 탈퇴가 확정된 상태다.
이 상태에서는 사용자의 공개 식별 정보와 OAuth 연결 정보를 마스킹한다.

## 탈퇴 유예 컬럼

탈퇴 유예 기간 관리를 위해 `users`에 다음 컬럼을 추가한다.
이 컬럼 추가는 다음 코드 변경 PR에서 진행한다.

```sql
ALTER TABLE users
ADD COLUMN withdrawal_requested_at TIMESTAMP,
ADD COLUMN withdrawal_due_at TIMESTAMP;
```

`withdrawal_due_at`은 다음 기준으로 계산한다.

```text
withdrawal_due_at = withdrawal_requested_at + 30 days
```

30일 기준은 요청 시각으로부터 정확히 30일 뒤로 본다.
예를 들어 `2026-06-17 10:00:00`에 탈퇴를 요청하면
`2026-07-17 10:00:00`부터 탈퇴 확정 대상이 된다.

향후 스케줄러 조회를 위해 다음 인덱스를 둔다.

```sql
CREATE INDEX users_withdrawal_due_idx
ON users (withdrawal_due_at)
WHERE status = 'WITHDRAWAL_REQUESTED';
```

## 탈퇴 요청

탈퇴 요청 시 사용자는 즉시 로그아웃 처리된다.
하지만 30일 동안 탈퇴 취소가 가능하므로 공개 프로필과 OAuth 연결은 아직 마스킹하지 않는다.

처리 내용:

```text
users.status = WITHDRAWAL_REQUESTED
users.withdrawal_requested_at = now()
users.withdrawal_due_at = now() + 30 days
users.updated_at = now()
```

유지하는 값:

```text
users.nickname
users.profile_image_id
users.deleted_at = null
oauth_accounts.provider
oauth_accounts.provider_id
oauth_accounts.email
oauth_accounts.provider_display_name
oauth_accounts.disconnected_at = null
```

`deleted_at`은 탈퇴 요청 시점에 채우지 않는다.
현재 닉네임 unique index가 `WHERE deleted_at IS NULL` 조건을 사용하므로,
탈퇴 유예 기간에 `deleted_at`을 채우면 닉네임 점유가 풀려 복구 시 충돌이 생길 수 있다.

## 토큰 정책

탈퇴 요청 즉시 해당 사용자의 refresh token을 모두 삭제한다.
현재 기기만 로그아웃하는 것이 아니라 `user_id` 기준으로 전체 refresh token을 삭제한다.

```text
DELETE FROM refresh_tokens
WHERE user_id = {userId}
```

응답에는 refresh token cookie 만료 헤더를 내려준다.

access token은 JWT라서 서버에서 물리적으로 삭제할 수 없다.
대신 최상단 인증 로직에서 사용자 상태를 매 요청마다 확인하고,
`ACTIVE`가 아닌 사용자는 보호 API 접근을 막는다.

refresh token 회전 API도 사용자 상태를 검사해야 한다.
저장된 refresh token이 남아 있거나 경계 상황이 있더라도
`WITHDRAWAL_REQUESTED`, `BLOCKED`, `DELETED` 사용자는 access token을 재발급받을 수 없어야 한다.

## 탈퇴 취소

탈퇴 신청 후 30일 안에 같은 OAuth 계정으로 로그인하면 탈퇴를 자동 취소한다.
별도의 확인 화면은 두지 않는다.

OAuth 로그인 시 `oauth_accounts(provider, provider_id)`로 활성 OAuth 계정을 조회한다.
조회된 OAuth 계정의 `user_id`로 `users`를 조회한다.
사용자가 `WITHDRAWAL_REQUESTED`이고 `withdrawal_due_at > now()`이면 다음처럼 복구한다.

```text
users.status = ACTIVE
users.withdrawal_requested_at = null
users.withdrawal_due_at = null
users.updated_at = now()
oauth_accounts.last_login_at = now()
oauth_accounts.updated_at = now()
```

복구 후에는 일반 OAuth 로그인과 동일하게 refresh token을 새로 발급한다.
30일 유예 기간 동안 닉네임과 프로필 정보는 유지되므로 복구 시 별도 데이터 복원이 필요 없다.

## 탈퇴 확정

30일이 지난 탈퇴 신청 사용자는 탈퇴 확정 대상이다.
탈퇴 확정 처리는 향후 스케줄러에서 구현한다.
이번 PR과 다음 코드 변경 PR 범위에는 스케줄러 구현을 포함하지 않는다.

탈퇴 확정 시 도메인 데이터는 삭제하지 않는다.
`users` row도 삭제하지 않고, 사용자 공개 식별 정보와 OAuth 연결만 마스킹한다.

처리 내용:

```text
users.status = DELETED
users.deleted_at = now()
users.nickname = '탈퇴한사용자'
users.profile_image_id = null
users.withdrawal_requested_at = null
users.withdrawal_due_at = null
users.updated_at = now()

oauth_accounts.provider_id = withdrawn:{oauthAccountId}
oauth_accounts.email = null
oauth_accounts.provider_display_name = null
oauth_accounts.disconnected_at = now()
oauth_accounts.updated_at = now()
```

`nickname` DB 값에는 공백을 넣지 않는다.
현재 nickname check 제약이 공백을 허용하지 않으므로 DB에는 `탈퇴한사용자`로 저장하고,
외부 API 응답에서는 필요한 경우 `탈퇴한 사용자`로 표시한다.

탈퇴 확정 시에도 refresh token은 다시 삭제한다.
스케줄러 지연, 중복 요청, 과거 버그로 남은 토큰이 있더라도 확정 시점에 한 번 더 제거한다.

## OAuth provider_id 처리

`provider_id`는 OAuth provider가 내려주는 외부 사용자 식별자다.
그 자체로 이름이나 이메일은 아니지만, 같은 provider 안에서는 특정 사용자를 안정적으로 다시 식별할 수 있는 값이다.
따라서 서비스 입장에서는 개인정보성 식별자로 취급한다.

탈퇴 유예 기간에는 자동 복구를 위해 원래 `provider_id`를 유지한다.
탈퇴 확정 후에는 같은 OAuth 계정이 기존 탈퇴 row에 다시 연결되지 않도록 OAuth 연결을 끊는다.

연결 해제는 두 값으로 표현한다.

```text
oauth_accounts.disconnected_at = now()
oauth_accounts.provider_id = withdrawn:{oauthAccountId}
```

예시:

```text
탈퇴 전: provider = KAKAO, provider_id = 123456789, disconnected_at = null
탈퇴 후: provider = KAKAO, provider_id = withdrawn:5, disconnected_at = 2026-07-17 10:00:00
```

이렇게 하면 active unique 제약을 유지하면서도 같은 OAuth 계정으로 새 userId 가입이 가능하다.
원래 provider_id는 별도 컬럼이나 로그에 남기지 않는다.
탈퇴 확정 이후에는 기존 userId와 외부 OAuth 계정의 연결을 복구할 수 없어야 한다.

## 프로필 표시 정책

탈퇴 확정 사용자를 외부에 표시할 때는 DB에 저장된 nickname을 그대로 신뢰하지 않는다.
응답 조립 단계에서 사용자 상태가 `DELETED`이면 항상 탈퇴 사용자 표시값으로 마스킹한다.

```json
{
  "id": 10,
  "nickname": "탈퇴한 사용자",
  "profileImageUrl": null,
  "status": "DELETED"
}
```

30일 유예 기간인 `WITHDRAWAL_REQUESTED` 사용자는 아직 탈퇴 확정 전이므로 기존 닉네임을 유지한다.
다만 본인 외의 사용자가 접근하는 공개 프로필 기능에서는 제품 정책에 따라 표시 여부를 별도로 정할 수 있다.

## 스케줄러 설계

탈퇴 확정 스케줄러는 이번 범위에서 구현하지 않는다.
향후 구현 시 다음 조건으로 대상을 조회한다.

```sql
SELECT id
FROM users
WHERE status = 'WITHDRAWAL_REQUESTED'
  AND withdrawal_due_at <= now();
```

확정 update는 상태와 기한 조건을 함께 걸어야 한다.

```sql
UPDATE users
SET ...
WHERE id = ?
  AND status = 'WITHDRAWAL_REQUESTED'
  AND withdrawal_due_at <= now();
```

동시에 사용자가 OAuth 로그인으로 탈퇴 취소를 시도할 수 있으므로,
복구 update도 상태와 기한 조건을 함께 건다.

```sql
UPDATE users
SET ...
WHERE id = ?
  AND status = 'WITHDRAWAL_REQUESTED'
  AND withdrawal_due_at > now();
```

이렇게 하면 경계 시점에서 스케줄러 확정과 로그인 복구가 동시에 실행되어도
하나의 정책만 성공하게 만들 수 있다.

## 구현 시 확인할 것

- `oauth_accounts` repository/table object 추가
- 기존 `users` OAuth 데이터를 운영 반영 시 수동 SQL로 `oauth_accounts`에 이관
- `users_status_check`에 `WITHDRAWAL_REQUESTED` 추가
- `UserStatus` enum에 `WITHDRAWAL_REQUESTED` 추가
- 인증 가능한 상태를 `ACTIVE`로 제한
- refresh token 회전 시 사용자 상태 검사 추가
- 탈퇴 요청 API에서 refresh token 전체 삭제와 cookie 만료 처리
- OAuth 로그인에서 `WITHDRAWAL_REQUESTED` 자동 복구 처리
- 탈퇴 확정 시 `oauth_accounts.disconnected_at` 설정
- 탈퇴 확정 시 `oauth_accounts.provider_id = withdrawn:{oauthAccountId}` 치환
- 탈퇴 확정 사용자 응답 마스킹 처리
- 스케줄러 구현 시 동시성 조건 포함
