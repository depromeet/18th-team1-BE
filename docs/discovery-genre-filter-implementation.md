# 발견탭 장르 필터 구현 정리

관련 이슈: #108

## 배경

발견탭에서 책 장르별로 문장을 필터링해야 한다.
프론트/API 명칭은 `genre`로 맞추지만, 현재 DB 컬럼은 `books.category`로 존재한다.

따라서 이번 구현은 외부 API에서는 `genre`를 사용하고, 서버 내부 조회에서는 `books.category`에 매핑하는 방식으로 진행했다.
DB 컬럼 rename이나 신규 migration은 이번 범위에 포함하지 않았다.

## API 계약

```text
GET /api/discovery/quotes?genre={genre}&cursor={nextCursor}
Authorization: Bearer {accessToken}
```

`genre`는 선택 파라미터다.

- 파라미터 없음: 전체 장르 조회
- `genre=전체`: 전체 장르 조회
- 그 외 지원 장르: `books.category = genre` 조건으로 필터링
- 지원하지 않는 값: `INVALID_INPUT`

지원 장르 목록:

```text
전체
한국소설
일본소설
영미소설
판타지
고전문학
인문
철학
에세이•시
영화•드라마 원작
```

응답의 각 문장에는 책 장르를 `genre` 필드로 포함한다.

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
      "genre": "한국소설",
      "recommendedAt": "2026-06-05T12:34:56",
      "isScrapped": false
    }
  ],
  "nextCursor": "MjAyNi0wNi0wNVQxMjozNDo1NnwxMA",
  "hasNext": true
}
```

## 페이지네이션과의 관계

장르 필터는 커서 페이지네이션 조회 조건에 함께 들어간다.

클라이언트는 장르가 바뀌면 기존 `cursor`를 재사용하지 않고 첫 페이지부터 다시 요청해야 한다.
커서는 같은 정렬 조건과 같은 필터 조건 안에서만 의미가 있다.

예시 흐름:

```text
GET /api/discovery/quotes?genre=한국소설
GET /api/discovery/quotes?genre=한국소설&cursor={nextCursor}

장르 변경 시:
GET /api/discovery/quotes?genre=일본소설
```

## 구현 방식

### DiscoveryGenre

`DiscoveryGenre` enum을 추가해 API에서 허용하는 장르 값을 한 곳에서 관리한다.

`parse` 규칙:

- `null`, blank, `전체`는 `null` 반환
- 지원 장르는 enum으로 변환
- 지원하지 않는 값은 `CustomException(ErrorCode.INVALID_INPUT)` 발생

### BookTable

프로젝트는 jOOQ codegen이 아니라 수동 테이블 매핑을 사용한다.
DB에 `books.category`가 있어도 `BookTable`에 필드가 없으면 Kotlin 코드에서 참조할 수 없다.

그래서 아래 필드를 추가했다.

```kotlin
val CATEGORY = DSL.field(DSL.name("books", "category"), String::class.java)
```

### DiscoveryRepository

repository는 `DiscoveryGenre?`를 받아 조건을 선택적으로 추가한다.

```kotlin
.and(genre?.let { selectedGenre -> BookTable.CATEGORY.eq(selectedGenre.value) } ?: DSL.noCondition())
```

장르가 없으면 `DSL.noCondition()`으로 전체 장르를 조회한다.
장르가 있으면 현재 DB 구조에 맞춰 `books.category = selectedGenre.value` 조건을 추가한다.

### DTO와 Swagger

`DiscoveryQuoteResponse`에 `genre`를 추가했다.
Swagger에는 `genre` request parameter를 추가하고, 고정 허용 값을 `allowableValues`로 명시했다.

## 주요 변경 파일

- `BookTable`
  - `CATEGORY` 필드 추가
- `DiscoveryGenre`
  - 발견탭 장르 enum 및 파싱 규칙 추가
- `DiscoveryController`
  - `genre` request parameter 추가
  - Swagger 허용 장르 목록 추가
- `DiscoveryUseCase`
  - `genre` 문자열 파싱 후 service 전달
- `DiscoveryService`
  - `DiscoveryGenre?` 전달
- `DiscoveryRepository`
  - `books.category` 조건 추가
  - select 필드에 category 추가
- `DiscoveryQuote`
  - `genre` 필드 추가
- `DiscoveryQuoteResponse`
  - `genre` 응답 필드 추가

## 테스트

다음 시나리오를 검증했다.

- 장르가 있으면 repository SQL에 `books.category = ?` 조건이 추가된다.
- 응답 DTO에 `genre`가 매핑된다.
- `genre=전체`이면 전체 장르 조회로 처리한다.
- 유효한 장르는 `DiscoveryGenre`로 파싱되어 service에 전달된다.
- 유효하지 않은 장르는 `INVALID_INPUT`을 발생시킨다.
- 유효한 `cursor`와 `genre`가 함께 들어와도 둘 다 파싱되어 service에 전달된다.

검증 명령:

```bash
cd app && ./gradlew test --tests 'com.firstpenguin.app.domain.discovery.*'
cd app && ./gradlew formatKotlin lintKotlin detekt
```

## 아직 최적화하지 않은 부분

현재는 `books.category` 문자열을 그대로 비교한다.
데이터가 많아지거나 장르 정책이 바뀌면 다음을 검토해야 한다.

- `books.category` 인덱스 존재 여부와 실제 실행 계획
- DB 컬럼명을 `category`에서 `genre`로 rename할지 여부
- 장르 값 오탈자나 다른 표기법을 DB 입력 단계에서 어떻게 막을지
- 응답에 `genre`를 반드시 포함해야 하는지, 필터 UI만으로 충분한지
- 장르 변경 시 기존 cursor 재사용을 클라이언트에서 명확히 막는 UX 처리

