# 월말 결산 구현 계획

## 목표

월말 결산은 사용자가 해당 월에 추천받은 문장을 기준으로 한 달의 장르와 감정 흐름을 보여주는 기능이다.

별도 지시가 없다면 월말 결산에서 말하는 "문장"은 일기 작성에 사용한 문장이 아니라 추천받은 문장이다.
최신 dev 기준으로 추천받은 문장의 source table은 `recommendation_quotes`다.

관련 이슈: #103

## 최신 dev 기준

추천 도메인 테이블명은 V48 migration 이후 아래 이름을 사용한다.

| 구분 | 최신 테이블 |
|------|-------------|
| 추천 요청 | `recommendations` |
| 추천받은 문장 | `recommendation_quotes` |
| 추천 요청 시 입력한 태그 | `recommendation_tags` |

책 장르 source column은 현재 `books.category`다.
다만 도메인, DTO, service/repository 메서드, API 응답에서는 `category`가 아니라 `genre` 용어를 사용한다.
`category`는 현재 DB 컬럼명으로만 취급한다.

## API

아래 API 경로는 서버 context-path `/api`를 포함한 공개 경로로 표기한다.

```text
GET /api/monthly-settlements?year=2026&month=3
Authorization: Bearer {accessToken}
```

### 응답

```json
{
  "year": 2026,
  "month": 3,
  "sharedQuoteCount": 27,
  "mostFrequentGenre": "추리/미스터리 소설",
  "monthlyBooks": [
    {
      "bookId": 1,
      "title": "홍학의 자리",
      "author": "정해연",
      "bookCoverImageUrl": "https://cdn.example.com/book-cover-placeholder.png",
      "genre": "추리/미스터리 소설"
    },
    {
      "bookId": 2,
      "title": "살인자의 기억법",
      "author": "김영하",
      "bookCoverImageUrl": "https://cdn.example.com/book-cover-placeholder.png",
      "genre": "추리/미스터리 소설"
    },
    {
      "bookId": 3,
      "title": "칵테일, 러브, 좀비",
      "author": "조예은",
      "bookCoverImageUrl": "https://cdn.example.com/book-cover-placeholder.png",
      "genre": "추리/미스터리 소설"
    }
  ],
  "emotionTags": [
    {
      "tagId": 10,
      "label": "무기력한",
      "count": 5
    },
    {
      "tagId": 11,
      "label": "공허한",
      "count": 4
    },
    {
      "tagId": 12,
      "label": "우울한",
      "count": 3
    },
    {
      "tagId": 13,
      "label": "신나는",
      "count": 2
    },
    {
      "tagId": 14,
      "label": "심심한",
      "count": 1
    }
  ],
  "recommendationMessage": "무기력한 3월을 보내셨군요. 이 감정과 유사한 문장이 담긴 책을 추천해요.",
  "monthlyBook": {
    "quoteId": 101,
    "bookId": 1,
    "quoteContent": "어떤 기억은 아물지 않습니다. 오히려 그 기억만 남기고 다른 모든 것이 서서히 마모됩니다.",
    "title": "소년이 온다",
    "author": "한강",
    "bookCoverImageUrl": "https://cdn.example.com/book-cover-placeholder.png",
    "genre": "소설"
  }
}
```

| 필드 | 설명 |
|------|------|
| `sharedQuoteCount` | 해당 월에 추천받은 문장 수 |
| `mostFrequentGenre` | 추천받은 문장 기준으로 가장 많이 등장한 책 장르 |
| `monthlyBooks` | `mostFrequentGenre`에 해당하는 책 3권. 같은 장르의 전체 책 후보가 3권 미만이면 가능한 만큼 반환 |
| `emotionTags` | 추천 요청 시 입력한 감정 태그 통계. 최소 1개, 최대 10개 |
| `recommendationMessage` | 1등 감정과 월을 사용한 결산 문장 |
| `monthlyBook` | 1등 감정과 유사한 문장을 가진 책 1권 |

## 집계 기준

월 범위는 `recommendations.recommendation_date` 기준으로 판단한다.

```text
recommendation_date >= year-month-01
recommendation_date < next-month-01
```

기본 추천 문장 source는 다음과 같다.

```text
recommendations
-> recommendation_quotes
-> quotes
-> books
```

`recommendations.quote_id`는 사용자가 선택한 최종 문장이다.
월말 결산의 "함께한 문장"은 추천받은 후보 문장 전체이므로 `recommendation_quotes`만 기준으로 집계한다.

## 1. 함께한 문장

`sharedQuoteCount`는 해당 월 `recommendation_quotes` row 수다.

같은 quote가 여러 번 추천되면 여러 번 함께한 문장으로 센다.
같은 책의 여러 문장이 추천된 경우에도 문장 기준으로 각각 센다.

## 2. 해당 월의 장르

추천받은 문장의 `quote_id`를 `quotes.book_id`로 변환하고, 다시 `books.category`를 읽어 장르를 집계한다.

장르 count는 문장 기준이다.
같은 책의 여러 문장이 추천되면 해당 책의 장르 count가 문장 수만큼 증가한다.

`books.category`가 `NULL`이거나 blank인 책은 장르 집계에서 제외한다.

정렬 기준은 다음 순서로 고정한다.

```text
genre_count DESC
genre ASC
```

집계 가능한 장르가 없으면 `mostFrequentGenre`는 `null`이다.

## 3. 해당 월의 책 3권

`monthlyBooks`는 `mostFrequentGenre`에 속한 책 3권이다.
같은 장르의 전체 책 후보가 3권 미만일 때만 가능한 개수만 반환한다.

우선 해당 월에 추천받은 문장들의 책 중 `mostFrequentGenre`에 해당하는 책을 고른다.
같은 책의 여러 문장이 추천되면 책 ranking count는 해당 문장 수만큼 증가한다.

정렬 기준은 다음 순서로 고정한다.

```text
recommended_quote_count DESC
book_id ASC
```

추천받은 책 후보가 3권 미만이면 부족한 수만큼 같은 장르의 다른 책을 `books` 테이블에서 채운다.
fallback 책은 해당 월 추천 여부와 무관하게 같은 장르의 책 중에서 가져온다.

fallback 정렬 기준은 다음 순서로 고정한다.

```text
book_id ASC
```

같은 책은 중복 반환하지 않는다.
같은 장르의 전체 책이 3권 미만이면 가능한 개수만 반환한다.

## 4. 해당 월의 감정

`emotionTags`는 문장을 추천받을 때 사용자가 입력한 감정 태그를 집계한다.
문장 메타데이터의 감정 태그가 아니라 `recommendation_tags`를 사용한다.

```text
recommendations
-> recommendation_tags
-> tags
```

`tags.type = 'EMOTION'`인 태그만 집계한다.
감정 count는 추천 요청 단위로 증가한다.
해당 추천 요청에서 더보기로 여러 문장을 받았더라도 감정 태그 count를 `recommendation_quotes` row 수만큼 곱하지 않는다.

정렬 기준은 다음 순서로 고정한다.

```text
tag_count DESC
tags.sort_order ASC
tags.id ASC
```

추천 요청은 감정 태그를 최소 1개 이상 받으므로 월말 결산의 감정 통계도 최소 1개를 전제로 한다.
월말 결산 응답에는 위 정렬 기준으로 최대 10개까지 저장하고 응답한다.
1등 감정은 위 정렬 기준의 첫 번째 태그다.

## 5. 결산 문장

1등 감정이 있으면 다음 형식으로 `recommendationMessage`를 만든다.

```text
{1등 감정 label} {month}월을 보내셨군요. 이 감정과 유사한 문장이 담긴 책을 추천해요.
```

예시:

```text
무기력한 3월을 보내셨군요. 이 감정과 유사한 문장이 담긴 책을 추천해요.
```

1등 감정은 월말 결산 생성 대상에서 항상 존재한다고 본다.

## 6. 이달의 책

`monthlyBook`은 1등 감정을 기준으로 찾은 문장 1개와 그 문장의 책이다.

후보 문장은 `quote_metadata_tags` 기준으로 찾는다.
즉, 추천 요청 때 입력한 1등 감정과 같은 tag를 가진 문장 메타데이터를 찾고, 그 문장이 속한 책을 반환한다.

```text
tags.id = top emotion tag id
-> quote_metadata_tags.tag_id
-> quote_metadata.quote_id
-> quotes
-> books
```

후보는 전체 문장 풀에서 찾는다.
해당 월에 이미 추천받은 문장 제외 정책은 이번 범위에 넣지 않는다.

후보가 여러 개면 월말 결산 생성 시 랜덤으로 1개를 선정한다.
선정된 quote와 book 정보는 월말 결산 스냅샷에 저장하고 이후 조회에서 바꾸지 않는다.

1등 감정을 가진 문장 후보가 없으면 `monthlyBook`은 `null`이다.

## DB

월말 결산은 매번 실시간으로 재계산하지 않고 월 단위 스냅샷으로 저장한다.
프로젝트 DB 규칙에 따라 Foreign Key는 추가하지 않는다.
문서와 schema PR에서는 Flyway migration까지만 추가한다.
DTO, table mapping, repository, API, scheduler 구현은 후속 PR로 분리한다.

```sql
CREATE TABLE IF NOT EXISTS monthly_settlements (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL,
    settlement_year INT NOT NULL,
    settlement_month INT NOT NULL,
    shared_quote_count INT NOT NULL,
    most_frequent_genre VARCHAR(100),
    top_emotion_tag_id BIGINT NOT NULL,
    top_emotion_tag_label VARCHAR(100) NOT NULL,
    recommendation_message TEXT NOT NULL,
    selected_quote_id BIGINT,
    selected_quote_content TEXT,
    selected_book_id BIGINT,
    selected_book_title VARCHAR(255),
    selected_book_author VARCHAR(255),
    selected_book_cover_image_url TEXT,
    selected_book_genre VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT monthly_settlements_month_check CHECK (settlement_month BETWEEN 1 AND 12),
    CONSTRAINT monthly_settlements_shared_quote_count_check CHECK (shared_quote_count > 0)
);

CREATE UNIQUE INDEX IF NOT EXISTS monthly_settlements_user_month_uidx
    ON monthly_settlements (user_id, settlement_year, settlement_month);

CREATE INDEX IF NOT EXISTS monthly_settlements_user_id_idx
    ON monthly_settlements (user_id);
```

```sql
CREATE TABLE IF NOT EXISTS monthly_settlement_emotion_tags (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    monthly_settlement_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    tag_label VARCHAR(100) NOT NULL,
    tag_count INT NOT NULL,
    sort_order INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT monthly_settlement_emotion_tags_count_check CHECK (tag_count > 0),
    CONSTRAINT monthly_settlement_emotion_tags_sort_order_check CHECK (sort_order BETWEEN 1 AND 10)
);

CREATE UNIQUE INDEX IF NOT EXISTS monthly_settlement_emotion_tags_settlement_tag_uidx
    ON monthly_settlement_emotion_tags (monthly_settlement_id, tag_id);

CREATE UNIQUE INDEX IF NOT EXISTS monthly_settlement_emotion_tags_settlement_sort_uidx
    ON monthly_settlement_emotion_tags (monthly_settlement_id, sort_order);
```

```sql
CREATE TABLE IF NOT EXISTS monthly_settlement_books (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    monthly_settlement_id BIGINT NOT NULL,
    book_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    author VARCHAR(255) NOT NULL,
    book_cover_image_url TEXT NOT NULL,
    genre VARCHAR(100) NOT NULL,
    sort_order INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT monthly_settlement_books_sort_order_check CHECK (sort_order BETWEEN 1 AND 3)
);

CREATE UNIQUE INDEX IF NOT EXISTS monthly_settlement_books_settlement_book_uidx
    ON monthly_settlement_books (monthly_settlement_id, book_id);

CREATE UNIQUE INDEX IF NOT EXISTS monthly_settlement_books_settlement_sort_uidx
    ON monthly_settlement_books (monthly_settlement_id, sort_order);
```

## PR 분리

월말 결산은 한 화면 기능이지만 PR은 위험 단위로 나눈다.

1. 문서 + schema PR
   - 최종 설계 문서 정리
   - 기존 월간 리포트 문서 삭제
   - 월말 결산 스냅샷 테이블 Flyway migration 추가
2. API PR
   - DTO, table mapping, repository, service, usecase, controller 추가
   - `GET /api/monthly-settlements`
   - shared quote count, top genre, monthly books, emotion tags, recommendation message, monthly book 집계 구현
3. scheduler PR
   - 매월 1일 지난달 월말 결산 생성
   - 월 단위 lock
   - 누락 감지와 재처리
   - scheduler 멱등성 검증

## 스케줄러

`@EnableScheduling`을 활성화하고 매월 1일 새벽에 지난달 월말 결산을 생성한다.

```kotlin
@Scheduled(cron = "0 0 3 1 * *", zone = "Asia/Seoul")
```

스케줄러는 다음 순서로 동작한다.

1. 지난달 `YearMonth`를 계산한다.
2. 월 단위 lock을 획득한다.
3. lock 획득에 실패하면 현재 인스턴스는 작업하지 않고 종료한다.
4. 지난달 추천 문장이 있는 사용자 ID를 page 단위로 조회한다.
5. 사용자별로 월말 결산을 생성한다.
6. 이미 생성된 `(user_id, settlement_year, settlement_month)` row는 건너뛴다.

전체 사용자를 하나의 트랜잭션으로 묶지 않는다.
사용자 1명의 월말 결산 생성 단위를 하나의 트랜잭션으로 처리해 중간 실패 후 재실행이 가능하게 한다.

## 멱등성과 누락 복구

스케줄러는 여러 번 실행되어도 같은 월말 결산을 중복 생성하면 안 된다.
`(user_id, settlement_year, settlement_month)` unique index를 최종 방어선으로 둔다.

누락은 다음 조건으로 판단한다.

```text
종료된 월이고,
해당 월에 사용자의 recommendation_quotes row가 1개 이상 있고,
monthly_settlements에 해당 user_id/year/month row가 없다.
```

누락 사용자 조회는 `recommendations`와 `recommendation_quotes`를 기준으로 한다.
스케줄러 재실행 시 누락된 사용자만 다시 생성할 수 있어야 한다.

조회 API에서도 과거 월인데 결산 row가 없고 추천 문장이 존재하면 즉시 생성해 복구한다.
이때도 동일한 unique index와 생성 로직을 사용해 scheduler와 조회 요청이 동시에 들어와도 중복 생성되지 않게 한다.

## 현재 월 차단

월말 결산은 종료된 월만 조회할 수 있다.
백엔드에서 `YearMonth.now(ZoneId.of("Asia/Seoul"))` 기준으로 현재 월과 미래 월을 차단한다.

```kotlin
val requestedMonth = YearMonth.of(year, month)
val currentMonth = YearMonth.now(ZoneId.of("Asia/Seoul"))

if (!requestedMonth.isBefore(currentMonth)) {
    throw CustomException(ErrorCode.INVALID_INPUT)
}
```

지난달 이전은 조회 가능하고, 현재 월과 미래 월은 조회 불가다.
에러 코드는 기존 `INVALID_INPUT`을 사용할 수 있지만, API 명확성을 위해 `MONTHLY_SETTLEMENT_NOT_AVAILABLE` 같은 별도 에러 코드 추가도 고려한다.

## 구현 순서

1. 문서 + schema PR에서 Flyway migration을 추가한다.
   - `monthly_settlements`
   - `monthly_settlement_emotion_tags`
   - `monthly_settlement_books`
2. API PR에서 book 매핑을 확인한다.
   - 현재 DB source column은 `books.category`
   - 코드 레벨에서는 `genre` 용어 사용
3. API PR에서 monthly settlement 도메인 모델과 DTO를 만든다.
   - 월말 결산 응답
   - 감정 태그 통계 응답
   - 해당 월의 책 응답
   - 이달의 책 응답
4. API PR에서 repository를 만든다.
   - 저장된 월말 결산 조회
   - 추천 문장 수 조회
   - top genre 조회
   - top genre 책 후보 조회
   - fallback 책 후보 조회
   - 추천 요청 감정 태그 집계
   - top emotion 기반 이달의 책 후보 조회
   - 월말 결산 스냅샷 저장
5. API PR에서 service를 만든다.
   - 집계 정책
   - top genre 결정
   - 월별 책 3권 구성
   - top emotion 결정
   - 결산 문장 생성
   - 이달의 책 랜덤 선정
6. API PR에서 usecase를 만든다.
   - 조회
   - 누락 결산 즉시 생성
7. API PR에서 controller를 추가한다.
   - `GET /api/monthly-settlements`
   - `year`, `month` request parameter 검증
8. scheduler PR에서 scheduler를 추가한다.
   - 매월 1일 지난달 월말 결산 생성
   - 월 단위 lock
   - page 단위 사용자 처리

## 테스트

단위 테스트는 다음 시나리오를 검증한다.

- `sharedQuoteCount`는 `recommendation_quotes` row 수 기준이다.
- `recommendations.quote_id`는 함께한 문장 수 집계에 사용하지 않는다.
- 같은 책의 여러 문장이 추천되면 장르 count는 문장 수만큼 증가한다.
- top genre는 `books.category` 기준으로 결정하되 코드 개념명은 genre를 사용한다.
- top genre 책 3권은 추천받은 책을 우선 사용한다.
- 추천받은 top genre 책이 3권 미만이면 같은 장르의 다른 책으로 채운다.
- 감정 통계는 `recommendation_tags` 중 `EMOTION` 태그 기준이다.
- 감정 통계는 더보기 문장 수만큼 곱하지 않는다.
- 감정 통계는 최소 1개, 최대 10개까지 저장하고 응답한다.
- top emotion은 항상 존재하며, top emotion에 해당하는 문장 후보가 없으면 `monthlyBook`은 `null`이다.
- 이달의 책은 한 번 저장된 뒤 재조회해도 바뀌지 않는다.
- 현재 월과 미래 월은 조회할 수 없다.
- 스케줄러는 이미 생성된 월말 결산을 중복 생성하지 않는다.
- 과거 월 결산이 누락된 경우 조회 시 생성해 복구한다.

최종 검증은 다음 명령으로 수행한다.

```bash
cd app && ./gradlew testClasses
cd app && ./gradlew test
cd app && ./gradlew lintKotlin
cd app && ./gradlew detekt
```

포맷 수정이 필요한 경우 push 전에 `cd app && ./gradlew formatKotlin`을 실행한다.
