# 홈 요약 API

## 목표

홈 화면에서 오늘 일기, 이번 달 일기 목록, 전체 일기 수를 한 번에 조회한다.
이번 달 일기 항목에는 상세 화면 이동을 위한 `diaryId`를 포함한다.

관련 이슈: #68

## API

아래 API 경로는 서버 context-path `/api`를 포함한 공개 경로로 표기한다.

```text
GET /api/home/summary
Authorization: Bearer {accessToken}
```

로그인 사용자만 호출할 수 있다.

## 응답

```json
{
  "todayDiary": {
    "diaryId": 23,
    "createdAt": "2026-05-06",
    "quoteContent": "가장 중요한 것은 보이지 않는다."
  },
  "monthlyDiaries": [
    {
      "diaryId": 23,
      "createdAt": "2026-05-06",
      "quoteContent": "가장 중요한 것은 보이지 않는다."
    }
  ],
  "totalDiaryCount": 42
}
```

| 필드 | 설명 |
|------|------|
| `todayDiary` | 오늘 작성한 일기. 없으면 `null` |
| `monthlyDiaries` | 이번 달 일기 목록 |
| `monthlyDiaries[].diaryId` | 일기 상세 화면 이동에 사용할 일기 ID |
| `monthlyDiaries[].createdAt` | 일기 작성일. 서울 시간 기준 날짜 |
| `monthlyDiaries[].quoteContent` | 일기에 저장된 최종 선택 문장 |
| `totalDiaryCount` | 지금까지 작성한 전체 일기 수 |

## 조회 기준

`monthlyDiaries`는 서버 현재 날짜 기준으로 이번 달 범위를 계산한다.

```text
start = 이번 달 1일
end = 이번 달 마지막 날
```

`todayDiary`는 `monthlyDiaries` 중 `createdAt`이 오늘 날짜와 같은 첫 번째 항목이다.
오늘 작성한 일기가 없으면 `null`을 반환한다.

`totalDiaryCount`는 사용자 기준 전체 일기 수다.
이번 달 범위와 무관하게 누적 개수를 반환한다.

## 상세 화면 이동

홈 요약의 이번 달 일기 항목은 상세 화면으로 이어질 수 있어야 한다.
따라서 각 항목에 `diaryId`를 포함한다.

```text
GET /api/diaries/{diaryId}
```

클라이언트는 `monthlyDiaries[].diaryId` 또는 `todayDiary.diaryId`를 사용해 일기 상세 API를 호출한다.

## 예외와 경계 조건

| 상황 | 처리 |
|------|------|
| access token 없음, 만료, 위조 | 인증 에러 응답 |
| 이번 달 일기 없음 | `monthlyDiaries=[]`, `todayDiary=null` |
| 오늘 일기 없음 | `todayDiary=null` |
| 전체 일기 없음 | `totalDiaryCount=0` |

## 테스트

다음 시나리오를 검증한다.

- 이번 달 일기 목록을 반환한다.
- 오늘 작성한 일기가 있으면 `todayDiary`에 포함한다.
- 오늘 작성한 일기가 없으면 `todayDiary=null`을 반환한다.
- 각 월간 일기 항목에 `diaryId`가 포함된다.
- 전체 일기 수는 월 범위와 무관하게 사용자 전체 기준으로 계산한다.
