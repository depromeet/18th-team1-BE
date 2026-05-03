# 이미지 정책

## 기본 원칙

이미지는 public object로 운영한다.
스토리지에 업로드된 이미지는 인증 없이 public URL로 조회할 수 있으므로 민감한 이미지는 업로드하지 않는다.

DB에는 최종 조회용 public URL을 저장한다.
`images.url`은 서비스가 허용한 스토리지에 업로드된 이미지의 절대 public URL을 의미한다.
presigned URL은 업로드용 임시 URL이므로 DB에 저장하지 않는다.

현재 스토리지는 GCP Cloud Storage(dev 환경)를 사용한다.

## 업로드 흐름

이미지 업로드는 클라이언트 직접 업로드 방식으로 처리한다.

1. 클라이언트가 백엔드에 presigned URL 발급을 요청한다. 요청 시 이미지 타입과 파일 형식을 함께 전달한다.
2. 백엔드는 타입에 따라 경로 prefix를 결정하고, UUID로 파일명을 생성한 뒤 presigned URL과 public URL을 함께 반환한다.
3. 클라이언트는 presigned URL로 스토리지에 파일을 직접 업로드한다. PUT 요청 시 `Content-Type` 헤더를 발급 시 지정한 값과 동일하게 설정해야 한다.
4. 업로드 성공 후 클라이언트는 public URL을 백엔드 저장 API에 전달한다.
5. 백엔드는 public URL을 검증한 뒤 `images.url`에 저장한다.

### Presigned URL 발급 요청

```json
{
  "type": "DIARY_IMAGE",
  "contentType": "image/webp"
}
```

### Presigned URL 발급 응답

| 필드 | 설명 |
|------|------|
| `presignedUrl` | 클라이언트가 스토리지에 파일을 업로드할 때 사용하는 임시 PUT URL |
| `publicUrl` | 업로드 성공 후 DB에 저장하고 API 응답으로 내려줄 조회용 URL |

스토리지 업로드 성공 여부는 클라이언트가 presigned URL 요청의 응답으로 판단한다.
현재 단계에서는 `uploadId`, 임시 업로드 테이블, 스토리지 `HEAD` 검증, 완료 상태 관리는 도입하지 않는다.

## 경로 정책

경로는 이미지 타입에 따라 백엔드가 결정한다. 클라이언트는 경로와 파일명을 지정할 수 없다.
파일명은 백엔드가 UUID로 생성하며, 확장자는 클라이언트가 전달한 `contentType`에서 결정된다.

| ImageType | 경로 prefix |
|-----------|------------|
| `DIARY_IMAGE` | `diary/` |
| `USER_PROFILE_IMAGE` | `user/profile/` |

최종 오브젝트 키 예시: `diary/550e8400-e29b-41d4-a716.webp`

## URL 검증

백엔드는 클라이언트가 전달한 public URL을 그대로 신뢰하지 않는다.
저장 전에 최소한 다음 조건을 검증한다.

- URL 형식이 정상이어야 한다.
- 현재 환경에서 허용한 public base URL 또는 host에 속해야 한다.
- presigned URL처럼 query string이 포함된 값은 저장하지 않는다.

검증에 실패하면 DB에 저장하지 않고 `400 Bad Request`로 응답한다.

환경별 허용 public URL 기준값은 설정으로 관리한다.

```yaml
gcs:
  bucket-name: dev-senti-images
  base-url: https://storage.googleapis.com/dev-senti-images
```

## 파일 정책

기본 허용 파일 정책은 다음과 같다.

| 항목 | 정책 |
|------|------|
| 허용 contentType | `image/jpeg`, `image/png`, `image/webp` |
| 최대 크기 | 5MB |
| 공개 범위 | public |

SVG, GIF, 동영상, 문서 파일은 이미지 업로드 대상에 포함하지 않는다.
허용 범위를 넓혀야 하는 경우에는 보안, 비용, 렌더링 정책을 먼저 검토한다.

## 삭제와 교체

이미지를 교체할 때는 기존 파일명을 덮어쓰기보다 새 public URL을 생성한다.
브라우저와 CDN 캐시로 인해 같은 URL의 이미지가 즉시 갱신되지 않을 수 있기 때문이다.

이미지를 삭제하거나 교체하면 DB 레코드와 스토리지 객체 정리 책임이 함께 발생한다.
스토리지 객체 정리를 즉시 처리할지, 별도 배치나 운영 작업으로 처리할지는 구현 시점에 정한다.

## 변경 가능성

public URL 저장 방식은 단순하지만, CDN 도메인, 버킷 도메인, 스토리지 구조가 바뀌면 기존 DB URL 마이그레이션이 필요할 수 있다.

향후 private bucket 또는 signed read URL이 필요해지면 `images.url`에 public URL을 저장하는 방식 대신 `object_key`를 저장하고 조회 시점에 URL을 생성하는 방식으로 전환을 검토한다.
