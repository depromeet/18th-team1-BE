# 문장 스크랩 API

## 목표

로그인 사용자가 문장을 스크랩하거나 스크랩을 취소할 수 있게 한다.
발견탭의 `isScrapped` 값은 이 API가 저장한 `quote_scraps` 데이터를 기준으로 계산한다.

관련 이슈: #100

## API

아래 API 경로는 서버 context-path `/api`를 포함한 공개 경로로 표기한다.

```text
POST /api/quotes/{quoteId}/scraps
DELETE /api/quotes/{quoteId}/scraps
Authorization: Bearer {accessToken}
```

두 API 모두 본문 없이 호출하고, 성공 시 `204 No Content`를 반환한다.

## 스크랩 생성

```text
POST /api/quotes/{quoteId}/scraps
```

정책:

- 로그인 사용자의 `userId`와 path의 `quoteId`로 스크랩을 생성한다.
- 존재하지 않는 문장은 `QUOTE_NOT_FOUND`로 처리한다.
- 이미 스크랩한 문장을 다시 요청해도 성공 처리한다.
- 중복 생성 방지는 `quote_scraps(user_id, quote_id)` unique index와 `ON CONFLICT DO NOTHING`으로 처리한다.

응답:

```text
204 No Content
```

## 스크랩 취소

```text
DELETE /api/quotes/{quoteId}/scraps
```

정책:

- 로그인 사용자의 `userId`와 path의 `quoteId`에 해당하는 스크랩을 삭제한다.
- 존재하지 않는 문장은 `QUOTE_NOT_FOUND`로 처리한다.
- 스크랩하지 않은 문장을 취소해도 성공 처리한다.

응답:

```text
204 No Content
```

## DB

스크랩 저장은 `quote_scraps` 테이블을 사용한다.
이 테이블은 발견탭 조회 API에서 `isScrapped` 계산에도 사용한다.

```sql
CREATE TABLE IF NOT EXISTS quote_scraps (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL,
    quote_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS quote_scraps_user_quote_uidx
    ON quote_scraps (user_id, quote_id);

CREATE INDEX IF NOT EXISTS quote_scraps_quote_id_idx
    ON quote_scraps (quote_id);
```

프로젝트 DB 규칙에 따라 Foreign Key는 만들지 않는다.
문장 존재 여부는 usecase에서 `QuoteService.findQuoteById(quoteId)`로 검증한다.

## 레이어 책임

### `QuoteScrapController`

- 인증된 사용자 ID와 path의 `quoteId`를 받는다.
- 인증 정보가 없으면 `UNAUTHORIZED`를 발생시킨다.
- 성공 시 `204 No Content`를 반환한다.

### `QuoteScrapUseCase`

- `@Transactional` 경계를 가진다.
- 문장 존재 여부를 먼저 확인한다.
- 스크랩 생성 또는 삭제를 service에 위임한다.

### `QuoteScrapService`

- 스크랩 생성/삭제 정책을 표현한다.
- 중복 생성과 없는 row 삭제를 성공 처리한다.

### `QuoteScrapRepository`

- `insertIgnoreDuplicate(userId, quoteId)`는 `ON CONFLICT DO NOTHING`을 사용한다.
- `deleteByUserIdAndQuoteId(userId, quoteId)`는 해당 사용자의 스크랩만 삭제한다.

## 발견탭과의 관계

`GET /api/discovery/quotes` 응답의 `isScrapped`는 로그인 사용자 기준으로 계산한다.

```text
quote_scraps.user_id = 로그인 사용자 ID
quote_scraps.quote_id = 응답 문장 ID
```

따라서 스크랩 생성 후 같은 문장이 발견탭 응답에 포함되면 `isScrapped=true`가 된다.
스크랩 취소 후에는 `isScrapped=false`가 된다.

## 예외와 경계 조건

| 상황 | 처리 |
|------|------|
| access token 없음, 만료, 위조 | 인증 에러 응답 |
| `quoteId` 문장이 없음 | `QUOTE_NOT_FOUND` |
| 이미 스크랩한 문장 생성 요청 | 성공, `204 No Content` |
| 스크랩하지 않은 문장 삭제 요청 | 성공, `204 No Content` |
| 다른 사용자의 스크랩 | 삭제 대상 아님 |

## 테스트

다음 시나리오를 검증한다.

- 문장이 존재하면 스크랩을 생성한다.
- 이미 스크랩한 문장 생성 요청도 성공 처리한다.
- 문장이 존재하면 스크랩을 취소한다.
- 존재하지 않는 스크랩 취소 요청도 성공 처리한다.
- 존재하지 않는 문장에 대한 생성/삭제 요청은 `QUOTE_NOT_FOUND`로 실패한다.
- repository 생성 쿼리에 `ON CONFLICT DO NOTHING`이 포함된다.
- repository 삭제 쿼리는 `user_id`, `quote_id`를 함께 조건으로 사용한다.
