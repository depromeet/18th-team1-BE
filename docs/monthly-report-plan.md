# 월간 리포트 구현 계획

## 목표

월간 리포트는 사용자가 일기를 작성할 때 최종 선택한 문장을 월 단위로 회고하는 기능이다.
리포트에서 말하는 "문장"은 추천 과정에서 노출된 모든 문장이 아니라 `diaries.quote_id`에 저장된 최종 선택 문장이다.

관련 이슈: #103

책 장르 집계는 현재 `books` 테이블에 장르 정보가 없으므로 이번 범위에서 실제 구현하지 않는다.
책 장르 스키마가 추가되면 후속 이슈 #102에서 리포트 장르 집계를 연결한다.

## 리포트 정책

리포트는 종료된 월만 확정한다.
매월 1일 스케줄러가 지난달 리포트를 생성하고, 생성된 결과는 스냅샷으로 저장한다.

현재 월과 미래 월은 확정 리포트 대상이 아니다.
현재 월 데이터를 보여줘야 한다면 월간 리포트가 아니라 별도 실시간 요약 API로 분리한다.

하루 여러 개 일기를 작성하는 기획이 추가되어도 리포트 기준은 바뀌지 않는다.
일기 row 1개를 사용자가 최종 선택한 문장 1개로 보고 집계한다.

## API

아래 API 경로는 서버 context-path `/api`를 포함한 공개 경로로 표기한다.

```text
GET /api/reports/monthly?year=2026&month=5
Authorization: Bearer {accessToken}
```

### 응답

```json
{
  "year": 2026,
  "month": 5,
  "monthlyQuoteCount": 7,
  "totalQuoteCount": 42,
  "mostFrequentBookGenre": "준비중",
  "emotionTags": [
    {
      "tagId": 1,
      "label": "기분좋은",
      "count": 3
    },
    {
      "tagId": 2,
      "label": "행복한",
      "count": 2
    }
  ],
  "monthlyBook": {
    "quoteId": 10,
    "bookId": 3,
    "quoteContent": "가장 중요한 것은 보이지 않는다.",
    "title": "어린 왕자",
    "author": "생텍쥐페리",
    "bookCoverImageUrl": "https://cdn.example.com/book-cover-placeholder.png"
  }
}
```

| 필드 | 설명 |
|------|------|
| `year` | 리포트 년도 |
| `month` | 리포트 월 |
| `monthlyQuoteCount` | 해당 월에 작성한 일기 문장 수 |
| `totalQuoteCount` | 해당 월 말까지 작성한 전체 일기 문장 수 스냅샷 |
| `mostFrequentBookGenre` | v1에서는 장르 스키마가 없어 `"준비중"` 반환 |
| `emotionTags` | 해당 월 일기 문장에 붙은 감정 태그별 개수 |
| `monthlyBook` | 이 달의 책으로 선정된 문장과 책. 후보가 없으면 `null` |

### 빈 월 응답

해당 월에 작성한 일기가 0개인 사용자는 리포트 row를 저장하지 않는다.
조회 시에는 DB row 없이 빈 리포트 응답을 조립한다.

```json
{
  "year": 2026,
  "month": 5,
  "monthlyQuoteCount": 0,
  "totalQuoteCount": 5,
  "mostFrequentBookGenre": "준비중",
  "emotionTags": [],
  "monthlyBook": null
}
```

빈 월이어도 `totalQuoteCount`는 0으로 고정하지 않는다.
해당 월 말까지 사용자가 작성한 전체 일기 문장 수를 계산한다.

## DB

리포트는 월 단위 확정 결과이므로 별도 테이블에 저장한다.
프로젝트 DB 규칙에 따라 Foreign Key는 추가하지 않고 애플리케이션에서 ID로 연결한다.

```sql
CREATE TABLE monthly_reports (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL,
    report_year INT NOT NULL,
    report_month INT NOT NULL,
    monthly_quote_count INT NOT NULL,
    total_quote_count INT NOT NULL,
    top_emotion_tag_id BIGINT,
    top_emotion_tag_label VARCHAR(100),
    selected_quote_id BIGINT,
    selected_quote_content TEXT,
    selected_book_id BIGINT,
    selected_book_title VARCHAR(255),
    selected_book_author VARCHAR(255),
    selected_book_cover_image_url TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX monthly_reports_user_month_uidx
    ON monthly_reports (user_id, report_year, report_month);

CREATE INDEX monthly_reports_user_id_idx
    ON monthly_reports (user_id);
```

```sql
CREATE TABLE monthly_report_emotion_tags (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    monthly_report_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    tag_label VARCHAR(100) NOT NULL,
    tag_count INT NOT NULL,
    sort_order INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX monthly_report_emotion_tags_report_tag_uidx
    ON monthly_report_emotion_tags (monthly_report_id, tag_id);
```

## 집계 기준

월 범위는 `YearMonth`로 계산한다.

```text
start = year-month-01 00:00:00
end = next-month-01 00:00:00
```

집계 대상 일기는 다음 조건을 만족해야 한다.

- `diaries.user_id = 요청 사용자 또는 생성 대상 사용자`
- `diaries.created_at >= start`
- `diaries.created_at < end`
- `diaries.deleted_at IS NULL`

### 월 문장 수

`monthlyQuoteCount`는 해당 월 삭제되지 않은 일기 row 수다.
같은 문장을 여러 일기에 사용했다면 여러 번 만난 것으로 센다.

### 전체 문장 수

`totalQuoteCount`는 해당 월 말까지 삭제되지 않은 일기 row 누적 수다.

```text
diaries.created_at < next-month-01 00:00:00
```

### 이 달의 감정태그

감정태그는 문장 메타데이터 기준으로 집계한다.
사용자가 추천 요청 때 선택한 `daily_recommendation_tags`가 아니라, 최종 선택 문장의 `quote_metadata_tags`를 사용한다.

```text
diaries.quote_id
-> quote_metadata.quote_id
-> quote_metadata_tags.quote_metadata_id
-> tags.id
```

`tags.type = 'EMOTION'`인 태그만 집계한다.
한 문장에 감정태그가 여러 개 있으면 각 태그 count를 1씩 증가시킨다.

정렬 기준은 다음 순서로 고정한다.

```text
tag_count DESC
tags.sort_order ASC
tags.id ASC
```

### 이 달의 책

이 달의 책은 가장 많이 등장한 감정태그에 해당하는 문장 후보 중 1개를 리포트 생성 시 랜덤으로 선정한다.
선정된 quote와 book 정보는 `monthly_reports`에 스냅샷으로 저장한다.

리포트를 여러 번 조회하거나 스케줄러가 재실행되어도 이미 저장된 이 달의 책은 바꾸지 않는다.
감정태그 후보가 없으면 `monthlyBook`은 `null`이다.

## 스케줄러

`@EnableScheduling`을 활성화하고 매월 1일 새벽에 지난달 리포트를 생성한다.

```kotlin
@Scheduled(cron = "0 0 3 1 * *", zone = "Asia/Seoul")
```

스케줄러는 다음 순서로 동작한다.

1. 지난달 `YearMonth`를 계산한다.
2. 월 단위 PostgreSQL advisory lock을 획득한다.
3. lock 획득에 실패하면 현재 인스턴스는 작업하지 않고 종료한다.
4. 지난달 일기가 있는 사용자 ID를 page 단위로 조회한다.
5. 사용자별로 월간 리포트를 생성한다.
6. 이미 생성된 `(user_id, report_year, report_month)` row는 건너뛴다.

전체 사용자를 하나의 트랜잭션으로 묶지 않는다.
사용자 1명의 리포트 생성 단위를 하나의 트랜잭션으로 처리해 중간 실패 후 재실행이 가능하게 한다.

## 멱등성과 복구

스케줄러는 여러 번 실행되어도 같은 월 리포트를 중복 생성하면 안 된다.
이를 위해 `(user_id, report_year, report_month)` unique index를 최종 방어선으로 둔다.

리포트 생성 repository는 이미 존재하는 리포트를 덮어쓰지 않는다.
`ON CONFLICT DO NOTHING` 또는 사전 존재 확인 후 skip 방식을 사용한다.

스케줄러 실패로 과거 월에 일기가 있는데 리포트 row가 없다면 조회 API에서 즉시 생성해 복구한다.
이때도 동일한 unique index와 생성 로직을 사용하므로 scheduler와 조회 요청이 동시에 들어와도 중복 생성되지 않아야 한다.

## 구현 순서

1. Flyway migration을 추가한다.
   - `monthly_reports`
   - `monthly_report_emotion_tags`
   - unique index와 조회 인덱스
2. report 도메인 모델과 DTO를 만든다.
   - 월간 리포트 응답
   - 감정태그 집계 응답
   - 이 달의 책 응답
3. repository를 만든다.
   - 저장된 리포트 조회
   - 빈 월 판단용 월 일기 수 조회
   - 월말 기준 전체 일기 수 조회
   - 감정태그 집계
   - 이 달의 책 후보 조회
   - 리포트와 태그 집계 저장
4. service를 만든다.
   - 집계 정책
   - top emotion tag 결정
   - 이 달의 책 랜덤 선정
   - 빈 월 응답 생성
5. usecase를 만든다.
   - `@Transactional(readOnly = true)` 조회
   - 누락 리포트 복구 생성은 별도 `@Transactional` 메서드로 분리
6. controller를 추가한다.
   - `GET /api/reports/monthly`
   - `year`, `month` request parameter 검증
7. scheduler를 추가한다.
   - 매월 1일 지난달 리포트 생성
   - advisory lock
   - page 단위 사용자 처리

## 예외와 경계 조건

현재 월과 미래 월은 조회하지 않는다.
잘못된 `year`, `month` 값은 기존 `INVALID_INPUT` 응답을 사용한다.

삭제된 일기는 리포트 생성 대상에서 제외한다.
이미 생성된 리포트는 스냅샷으로 유지한다.
생성 이후 일기를 삭제했을 때 과거 리포트를 재계산할지는 별도 정책으로 남긴다.

문장 메타데이터가 없는 문장은 감정태그 집계에서 제외한다.
해당 월에 문장은 있지만 감정태그가 하나도 없으면 `emotionTags`는 빈 배열이고 `monthlyBook`은 `null`이다.

책 장르는 v1에서 `"준비중"`을 반환한다.
장르 스키마가 추가되면 #102에서 실제 장르 집계로 교체한다.

## 테스트

단위 테스트는 다음 시나리오를 검증한다.

- 해당 월 일기 row 수가 `monthlyQuoteCount`로 집계된다.
- 같은 날 여러 일기가 있어도 일기 row 수 기준으로 집계된다.
- `totalQuoteCount`는 해당 월 말 기준 누적 일기 row 수다.
- 감정태그는 `quote_metadata_tags`와 `tags.type = EMOTION` 기준으로 집계된다.
- 사용자가 선택한 `daily_recommendation_tags`는 리포트 감정태그 집계에 사용하지 않는다.
- top 감정태그는 count, sort order, tag id 순서로 결정된다.
- 이 달의 책은 한 번 저장된 뒤 재조회해도 바뀌지 않는다.
- 빈 월은 DB row 없이 빈 응답을 반환한다.
- 스케줄러가 이미 생성된 월 리포트를 중복 생성하지 않는다.
- 스케줄러 lock 획득 실패 시 작업을 건너뛴다.
- 과거 월 리포트가 누락된 경우 조회 시 생성해 복구한다.

최종 검증은 다음 명령으로 수행한다.

```bash
cd app && ./gradlew testClasses
cd app && ./gradlew test
cd app && ./gradlew lintKotlin
cd app && ./gradlew detekt
```

포맷 수정이 필요한 경우 push 전에 `cd app && ./gradlew formatKotlin`을 실행한다.

## 포트폴리오 어필 포인트

이 기능은 단순 CRUD가 아니라 월 단위 집계 데이터를 안정적으로 확정하는 배치 기능이다.
다음 내용을 구현 의도와 함께 설명하면 백엔드 포트폴리오에서 강점이 된다.

- 리포트 결과를 실시간 계산하지 않고 월말 스냅샷으로 저장한 이유
- `daily_recommendations`가 아니라 `diaries.quote_id`를 기준으로 잡은 이유
- 랜덤으로 뽑는 이 달의 책을 한 번 저장해 결과 일관성을 보장한 점
- unique index와 conflict 처리로 스케줄러 멱등성을 확보한 점
- 다중 서버 환경에서 advisory lock을 고려한 점
- 사용자 단위 트랜잭션으로 중간 실패 후 재실행이 가능하게 한 점
- 0건 월은 저장하지 않고 조회 시 빈 응답으로 처리해 불필요한 row 증가를 피한 점
