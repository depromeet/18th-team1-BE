# 문장 스크랩 API

## 목표

로그인 사용자가 문장을 스크랩하거나 스크랩을 취소할 수 있게 한다.
발견탭의 `isScrapped` 값은 이 API가 저장한 `quote_scraps` 데이터를 기준으로 계산한다.
스크랩은 추천 이력, 추천받은 사용자, 태그 조합이 아니라 문장 자체를 기준으로 한다.

관련 이슈: #100

## 구현 범위

이번 작업에서는 문장 카드에서 공통으로 사용할 단건 스크랩 생성/취소 API를 구현한다.
발견탭 UI가 토글 형태이더라도 서버 API는 `toggle` 하나로 합치지 않는다.
프론트엔드는 현재 `isScrapped` 값에 따라 생성 또는 취소 API를 선택해서 호출한다.

```text
isScrapped=false -> PUT /api/quotes/{quoteId}/scrap
isScrapped=true  -> DELETE /api/quotes/{quoteId}/scrap
```

마이페이지 스크랩 모아보기에서 선택 삭제를 지원하기 위한 다중 취소 API도 함께 구현한다.
이 API는 발견탭 단건 API를 수정하지 않고 별도 엔드포인트로 제공한다.

```text
POST /api/quote-scraps/bulk-delete
```

마이페이지 스크랩 모아보기 목록 API도 함께 구현한다.

```text
GET /api/my-page/scrapped-quotes?cursor={cursor}&limit={limit}
```

## API

아래 API 경로는 서버 context-path `/api`를 포함한 공개 경로로 표기한다.

```text
PUT /api/quotes/{quoteId}/scrap
DELETE /api/quotes/{quoteId}/scrap
Authorization: Bearer {accessToken}
```

두 API 모두 본문 없이 호출하고, 성공 시 `204 No Content`를 반환한다.
성공 응답에 내려줄 본문이 없으므로 `200 OK` 대신 `204 No Content`를 사용한다.

`PUT`은 로그인 사용자의 문장 스크랩 리소스를 원하는 최종 상태로 맞추는 요청이다.
같은 요청을 여러 번 보내도 최종 상태는 "스크랩됨"으로 같으므로 멱등성을 가진다.

## 스크랩 상태 설정

```text
PUT /api/quotes/{quoteId}/scrap
```

정책:

- 로그인 사용자의 `userId`와 path의 `quoteId`로 스크랩 상태를 설정한다.
- 존재하지 않는 문장은 `QUOTE_NOT_FOUND`로 처리한다.
- 이미 스크랩한 문장을 다시 요청해도 성공 처리한다.
- 중복 생성 방지는 `quote_scraps(user_id, quote_id)` unique index와 `ON CONFLICT DO NOTHING`으로 처리한다.
- 같은 문장이 여러 추천 맥락에 노출되더라도 같은 로그인 사용자에게는 하나의 스크랩으로 취급한다.

응답:

```text
204 No Content
```

프론트엔드는 이 요청이 성공하면 같은 화면에 있는 동일 `quoteId` 카드들을 모두 스크랩 상태로 갱신한다.

## 스크랩 취소

```text
DELETE /api/quotes/{quoteId}/scrap
```

정책:

- 로그인 사용자의 `userId`와 path의 `quoteId`에 해당하는 스크랩을 삭제한다.
- 존재하지 않는 문장은 `QUOTE_NOT_FOUND`로 처리한다.
- 스크랩하지 않은 문장을 취소해도 성공 처리한다.

응답:

```text
204 No Content
```

프론트엔드는 이 요청이 성공하면 같은 화면에 있는 동일 `quoteId` 카드들을 모두 스크랩 해제 상태로 갱신한다.

## 마이페이지 스크랩 목록

마이페이지 스크랩 모아보기에서는 로그인 사용자가 스크랩한 문장과 해당 문장의 책 정보를 조회한다.
화면 상단에서 내가 스크랩한 문장 개수도 함께 보여줘야 하므로 목록 응답에 `totalCount`를 포함한다.

예정 API:

```text
GET /api/my-page/scrapped-quotes?cursor={cursor}&limit={limit}
Authorization: Bearer {accessToken}
```

쿼리 파라미터:

| 이름 | 필수 | 설명 |
|------|------|------|
| `cursor` | N | 다음 페이지 조회용 커서. 첫 페이지에서는 생략 |
| `limit` | N | 조회 개수. 기본값은 10, 최대 50 |

응답:

```json
{
  "totalCount": 37,
  "quotes": [
    {
      "quoteId": 1,
      "bookId": 10,
      "bookCoverImageUrl": "https://cdn.example.com/book-cover.png",
      "content": "새는 알에서 나오려고 투쟁한다.",
      "title": "데미안",
      "author": "헤르만 헤세",
      "scrappedAt": "2026-06-13T14:30:00"
    }
  ],
  "nextCursor": "MjAyNi0wNi0xM1QxNDozMDowMHwx",
  "hasNext": true
}
```

필드:

| 필드 | 설명 |
|------|------|
| `totalCount` | 로그인 사용자가 스크랩한 문장 총 개수 |
| `quotes` | 현재 페이지의 스크랩 문장 목록 |
| `quoteId` | 문장 ID |
| `bookId` | 책 ID |
| `bookCoverImageUrl` | 책 표지 이미지 URL |
| `content` | 문장 내용 |
| `title` | 책 제목 |
| `author` | 책 저자 |
| `scrappedAt` | 사용자가 해당 문장을 스크랩한 시각 |
| `nextCursor` | 다음 페이지 조회용 커서. 다음 페이지가 없으면 `null` |
| `hasNext` | 다음 페이지 존재 여부 |

정책:

- `limit`를 생략하면 10개를 조회하고, 한 번에 최대 50개까지 조회할 수 있다.
- `limit`가 1보다 작거나 50보다 크면 검증 에러로 처리한다.
- `totalCount`는 책 개수가 아니라 스크랩한 문장 개수다.
- 같은 책에서 문장 3개를 스크랩했다면 `totalCount`는 3 증가한다.
- 기본 정렬은 `scrappedAt DESC, quoteId DESC`로 한다.
- 커서는 마지막 항목의 `scrappedAt + quoteId`를 URL-safe Base64로 인코딩한다.
- 다음 페이지 여부를 판단하기 위해 내부 조회는 `limit + 1`개를 가져오고, 응답은 `limit`개만 반환한다.
- 목록 조회는 `quote_scraps`, `quotes`, `books`를 join한다.
- 삭제된 문장이나 삭제된 책은 응답에서 제외한다.
- 첫 페이지와 이후 페이지 모두 `totalCount`를 포함한다.

## 다중 스크랩 취소

마이페이지 스크랩 모아보기에서는 사용자가 여러 스크랩을 선택해 한 번에 취소할 수 있어야 한다.
단건 삭제 API를 여러 번 호출해도 기능은 동작하지만, 선택 삭제 UX에서는 서버가 bulk 삭제를 제공하는 편이 실패 처리와 네트워크 비용 면에서 낫다.

예정 API:

```text
POST /api/quote-scraps/bulk-delete
Authorization: Bearer {accessToken}
Content-Type: application/json
```

요청:

```json
{
  "quoteIds": [1, 2, 3]
}
```

정책:

- 로그인 사용자의 스크랩 중 `quoteIds`에 해당하는 row만 삭제한다.
- 다른 사용자의 스크랩은 삭제하지 않는다.
- 이미 취소됐거나 스크랩하지 않은 문장이 포함되어도 성공 처리한다.
- `quoteIds`는 한 번에 최대 50개까지 허용한다.
- 빈 `quoteIds`, 50개를 초과한 요청, 0 이하 ID가 포함된 요청은 검증 에러로 처리한다.
- 성공 시 `204 No Content`를 반환한다.

`DELETE /api/quote-scraps`에 request body를 싣는 방식은 클라이언트와 중간 프록시, 문서화 도구에서 해석이 흔들릴 수 있으므로 사용하지 않는다.
마이페이지 화면명에 종속된 `DELETE /api/my-page/scrapped-quotes` 대신 스크랩 리소스 기준의 `POST /api/quote-scraps/bulk-delete`로 둔다.

## DB

스크랩 저장은 `quote_scraps` 테이블을 사용한다.
이 테이블은 발견탭 조회 API에서 `isScrapped` 계산에도 사용한다.
추천한 사용자, 추천 기록, 태그 조합은 저장 키에 포함하지 않는다.

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
- 다중 취소 API는 별도 controller 메서드로 추가한다.

### `QuoteScrapUseCase`

- `@Transactional` 경계를 가진다.
- 문장 존재 여부를 먼저 확인한다.
- 스크랩 생성 또는 삭제를 service에 위임한다.
- 목록 API는 `@Transactional(readOnly = true)`로 조회한다.
- 다중 취소 API도 usecase 계층에 `@Transactional`을 둔다.

### `QuoteScrapService`

- 스크랩 생성/삭제 정책을 표현한다.
- 중복 생성과 없는 row 삭제를 성공 처리한다.
- 목록 API에서는 로그인 사용자의 스크랩 목록과 총 개수를 조회한다.
- 다중 취소에서는 요청 사용자의 `quoteIds`만 삭제하도록 repository에 위임한다.

### `QuoteScrapRepository`

- `insertIgnoreDuplicate(userId, quoteId)`는 `ON CONFLICT DO NOTHING`을 사용한다.
- `deleteByUserIdAndQuoteId(userId, quoteId)`는 해당 사용자의 스크랩만 삭제한다.
- 목록 API에서는 `findActiveByUserId(userId, cursor, limit)`와 `countActiveByUserId(userId)`를 추가한다.
- 다중 취소에서는 `deleteByUserIdAndQuoteIds(userId, quoteIds)` 형태로 확장한다.

## 발견탭과의 관계

`GET /api/discovery/quotes` 응답의 `isScrapped`는 로그인 사용자 기준으로 계산한다.

```text
quote_scraps.user_id = 로그인 사용자 ID
quote_scraps.quote_id = 응답 문장 ID
```

따라서 스크랩 생성 후 같은 문장이 발견탭 응답에 포함되면 `isScrapped=true`가 된다.
스크랩 취소 후에는 `isScrapped=false`가 된다.

예를 들어 한 화면에 아래 두 카드가 함께 존재하더라도 스크랩 상태는 동일한 `quoteId=1`에 묶인다.

```text
recommendedUserId=1, quoteId=1
recommendedUserId=2, quoteId=1
```

첫 번째 카드에서 `PUT /api/quotes/1/scrap`을 성공시키면 두 카드 모두 스크랩된 상태로 취급해야 한다.
서버를 다시 조회하면 두 응답 모두 `isScrapped=true`가 되어야 한다.
즉시 화면에 반영하려면 프론트엔드가 요청 성공 후 같은 `quoteId`를 가진 카드들의 로컬 상태를 함께 갱신하거나,
스크랩 상태를 `quoteId` 기준 set/map으로 관리해 각 카드의 상태를 파생시키면 된다.

발견탭 조회가 `recommendedUserId + quoteId` 기준으로 추천 이력을 노출하면 같은 문장이 서로 다른 추천 사용자 카드로 한 화면에 여러 번 등장할 수 있다.
그래도 스크랩 상태는 `quoteId` 기준으로 공유되므로 같은 `quoteId`를 가진 모든 카드는 동일한 `isScrapped` 값을 내려받아야 한다.

## 예외와 경계 조건

| 상황 | 처리 |
|------|------|
| access token 없음, 만료, 위조 | 인증 에러 응답 |
| `quoteId` 문장이 없음 | `QUOTE_NOT_FOUND` |
| 이미 스크랩한 문장 상태 설정 요청 | 성공, `204 No Content` |
| 스크랩하지 않은 문장 삭제 요청 | 성공, `204 No Content` |
| 다른 사용자의 스크랩 | 삭제 대상 아님 |
| 다중 취소 요청에 이미 취소된 문장 포함 | 성공, `204 No Content` |

## 테스트

다음 시나리오를 검증한다.

- 문장이 존재하면 스크랩 상태로 설정한다.
- 이미 스크랩한 문장 상태 설정 요청도 성공 처리한다.
- 문장이 존재하면 스크랩을 취소한다.
- 존재하지 않는 스크랩 취소 요청도 성공 처리한다.
- 존재하지 않는 문장에 대한 생성/삭제 요청은 `QUOTE_NOT_FOUND`로 실패한다.
- repository 생성 쿼리에 `ON CONFLICT DO NOTHING`이 포함된다.
- repository 삭제 쿼리는 `user_id`, `quote_id`를 함께 조건으로 사용한다.

목록 조회와 다중 취소 API는 다음 시나리오를 추가로 검증한다.

- 내가 스크랩한 문장 목록을 `scrappedAt DESC, quoteId DESC` 순서로 조회한다.
- 응답에 책 이미지, 문장, 책 제목, 작가를 포함한다.
- `totalCount`는 로그인 사용자가 스크랩한 문장 개수를 반환한다.
- 커서가 있으면 다음 페이지를 조회한다.
- `limit + 1` 조회로 `hasNext`, `nextCursor`를 계산한다.
- 여러 `quoteId`를 한 번에 삭제한다.
- 요청 사용자의 스크랩만 삭제한다.
- 스크랩하지 않은 문장이 포함되어도 성공 처리한다.
- 빈 `quoteIds` 요청은 검증 에러로 실패한다.
