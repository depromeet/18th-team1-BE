# 발견탭 커서 페이지네이션 구현 정리

관련 이슈: #98

## 배경

발견탭 첫 구현은 페이지네이션을 바로 넣기 어려워 `ORDER BY random()`으로 최대 10개를 조회했다.
이 방식은 무한스크롤에서 다음 목록을 안정적으로 이어갈 수 없고, 요청마다 랜덤 순서가 달라져 중복 방지가 어렵다.

따라서 발견탭 조회를 랜덤 방식에서 최신 추천 이력 기준 커서 페이지네이션으로 전환했다.

## API 계약

```text
GET /api/discovery/quotes?cursor={nextCursor}
Authorization: Bearer {accessToken}
```

첫 페이지는 `cursor` 없이 요청한다.
다음 페이지는 직전 응답의 `nextCursor` 값을 그대로 `cursor`에 전달한다.

응답은 기존 `quotes` 목록에 페이지네이션 메타 정보를 추가했다.

```json
{
  "quotes": [
    {
      "quoteId": 10,
      "bookId": 1,
      "recommendedUserId": 20,
      "content": "새는 알에서 나오려고 투쟁한다.",
      "title": "데미안",
      "author": "헤르만 헤세",
      "bookCoverImageUrl": "https://cdn.example.com/book-cover-placeholder.png",
      "recommendedAt": "2026-06-05T12:34:56",
      "isScrapped": false
    }
  ],
  "nextCursor": "MjAyNi0wNi0wNVQxMjozNDo1NnwxMA",
  "hasNext": true
}
```

`nextCursor`는 서버가 발급한 URL-safe Base64 문자열이다.
클라이언트는 내부 값을 해석하거나 직접 만들지 않고, 응답 값을 그대로 다음 요청에 전달한다.

## 정렬 기준

발견탭의 최신순은 문장 생성 시각이 아니라 추천 이력의 최신 시각이다.

정렬 기준은 아래 순서다.

```sql
ORDER BY recommended_at DESC, quote_id DESC
```

`recommended_at`만 커서로 사용하지 않고 `quote_id`를 함께 넣은 이유는 같은 추천 시각을 가진 문장이 여러 개 있을 수 있기 때문이다.
복합 커서를 사용해야 같은 시각 데이터에서 누락이나 중복 없이 다음 페이지를 이어갈 수 있다.

## 커서 구조

커서 payload는 아래 값을 조합한다.

```text
recommendedAt|quoteId
```

예를 들어 내부 값이 다음과 같다면:

```text
2026-06-05T12:34:56|10
```

이를 URL-safe Base64로 인코딩해 클라이언트에는 아래처럼 내려준다.

```text
MjAyNi0wNi0wNVQxMjozNDo1NnwxMA
```

커서 파싱에 실패하면 `INVALID_INPUT`으로 처리한다.

## 조회 방식

발견탭은 최종 선택이 완료되고 삭제되지 않은 추천 이력만 사용한다.

- `recommendations.quote_id IS NOT NULL`
- `recommendations.deleted_at IS NULL`

`recommendation_quotes`는 추천 후보 목록이므로 발견탭에서 사용하지 않는다.
같은 문장이 여러 번 최종 선택됐으면 최신 추천 이력 1개를 대표 이벤트로 사용한다.
삭제된 문장과 삭제된 책은 제외한다.
로그인 사용자의 스크랩 여부는 `quote_scraps`를 left join해서 계산한다.

커서가 있으면 다음 조건을 추가한다.

```sql
recommended_at < :recommendedAt
OR (recommended_at = :recommendedAt AND quote_id < :quoteId)
```

실제 응답 개수는 10개지만, `hasNext` 판단을 위해 내부에서는 11개를 조회한다.
11개가 조회되면 10개만 응답하고, 10번째 문장의 `recommendedAt + quoteId`로 `nextCursor`를 만든다.

## 주요 변경 파일

- `DiscoveryController`
  - `cursor` request parameter 추가
  - Swagger 설명을 커서 기반 무한스크롤 기준으로 변경
- `DiscoveryUseCase`
  - `cursor` 파싱
  - 11개 조회 후 10개 응답
  - `nextCursor`, `hasNext` 계산
- `DiscoveryService`
  - cursor를 repository로 전달
- `DiscoveryRepository`
  - `random()` 제거
  - 최신순 정렬 추가
  - 복합 커서 조건 추가
- `DiscoveryCursor`
  - `recommendedAt + quoteId` 인코딩/파싱 담당
- `DiscoveryQuotesResponse`
  - `nextCursor`, `hasNext` 추가

## 테스트

다음 시나리오를 검증했다.

- 최종 선택되고 삭제되지 않은 추천 문장만 최신순으로 조회한다.
- 추천 후보 테이블을 조회하지 않는다.
- 커서가 있으면 다음 페이지 조건을 추가한다.
- 응답은 11개 조회 결과 중 10개만 반환한다.
- 11개가 조회되면 `hasNext=true`, `nextCursor`를 반환한다.
- 유효한 커서는 파싱되어 service로 전달된다.
- 잘못된 커서는 `INVALID_INPUT`을 발생시킨다.

검증 명령:

```bash
cd app && ./gradlew test --tests 'com.firstpenguin.app.domain.discovery.*'
cd app && ./gradlew formatKotlin lintKotlin detekt
```

## 아직 최적화하지 않은 부분

현재 쿼리는 최종 선택된 추천 이력에서 `row_number()`로 최신 대표 이벤트를 뽑는다.
데이터가 많아지면 다음 지점을 확인해야 한다.

- `recommended_at`, `quote_id` 기반 정렬/커서 조건에 맞는 인덱스 필요 여부
- 최신 대표 이벤트를 매번 계산하지 않고 별도 materialized view나 캐시로 관리할지 여부
- `nextCursor`에 서명 값을 붙여 클라이언트 조작을 더 강하게 막을지 여부
