# 이미지 업로드 흐름과 저장 정책

## 기본 원칙

이미지는 public object로 운영한다.
스토리지에 업로드된 이미지는 인증 없이 public URL로 조회할 수 있으므로 민감한 이미지는 업로드하지 않는다.

DB에는 최종 조회용 public URL을 저장한다.
`images.url`은 서비스가 허용한 스토리지에 업로드된 이미지의 절대 public URL을 의미한다.
presigned URL은 업로드용 임시 URL이므로 DB에 저장하지 않는다.
클라이언트는 public URL을 직접 저장 API에 전달하지 않고, 백엔드가 발급한 `imageId`만 도메인 API에 전달한다.

스토리지 구현은 `cloud.provider` 설정으로 선택한다.

| `cloud.provider` | 구현체 | 용도 |
|------------------|--------|------|
| `gcp` | `GcsStorageService` | dev/prod GCP Cloud Storage |
| `local` 또는 미설정 | `LocalStorageService` | 로컬 개발용 mock URL |

dev/prod 환경은 GCP Cloud Storage를 사용한다.
로컬에서 별도 GCP 자격증명 없이 API 흐름만 확인할 때는 local 구현이 mock 업로드 URL과 mock 조회 URL을 반환한다.

## 업로드 흐름

이미지 업로드는 클라이언트 직접 업로드 방식으로 처리한다.

1. 클라이언트가 백엔드에 presigned URL 발급을 요청한다. 요청 시 이미지 타입과 파일 형식을 함께 전달한다.
2. 백엔드는 타입에 따라 경로 prefix를 결정하고, UUID로 파일명을 생성한다.
3. 백엔드는 GCS 업로드용 presigned URL과 최종 조회용 public URL을 생성한다.
4. 백엔드는 public URL을 `images.url`에 먼저 저장하고, 생성된 `images.id`를 `imageId`로 반환한다.
5. 클라이언트는 presigned URL로 스토리지에 파일을 직접 업로드한다. PUT 요청 시 `Content-Type` 헤더를 발급 시 지정한 값과 동일하게 설정해야 한다.
6. 도메인 생성 API는 클라이언트가 전달한 `imageIds`를 받아 `image_owners`에 owner 연결만 저장한다.

```text
POST /images/presigned-url
  -> GCS presigned URL 생성
  -> images INSERT(publicUrl)
  <- { presignedUrl, imageId }

클라이언트 GCS PUT 업로드

POST /diaries 또는 다른 도메인 생성 API
  body: { ..., imageIds: [123] }
  -> image_owners INSERT(imageId, ownerType, ownerId, sortOrder)
```

### Presigned URL 발급 요청

```json
{
  "type": "DIARY",
  "contentType": "image/webp"
}
```

### Presigned URL 발급 응답

| 필드 | 설명 |
|------|------|
| `presignedUrl` | 클라이언트가 스토리지에 파일을 업로드할 때 사용하는 임시 PUT URL |
| `imageId` | `images` 테이블에 저장된 이미지 ID. 도메인 생성 API 호출 시 전달 |

스토리지 업로드 성공 여부는 클라이언트가 GCS PUT 요청 결과로 판단한다.
현재 응답에는 public URL을 노출하지 않는다.

## 서버 저장 로직

### `ImageService.issue(type, contentType)`

presigned URL 발급 API에서 사용하는 로직이다.

1. `contentType`이 허용 목록에 포함되는지 검증한다.
2. `CloudStorageService.issuePresignedUrl(type, contentType)`으로 presigned URL과 public URL을 생성한다.
3. `ImageRepository.save(publicUrl)`로 `images` row를 생성한다.
4. `presignedUrl`과 `imageId`를 반환한다.

### `ImageService.saveImages(imageIds, ownerType, ownerId)`

도메인 생성 로직에서 가져다 쓰는 공용 연결 로직이다.

1. `imageIds`가 비어 있으면 아무 작업도 하지 않는다.
2. `ImageRepository.saveOwners(ownerType, ownerId, imageIds)`를 호출한다.
3. `image_owners`에는 `image_id`, `owner_type`, `owner_id`, `sort_order`가 저장된다.

`images` row는 presigned URL 발급 시점에 이미 생성되어 있으므로, 도메인 생성 시점에는 `image_owners` 연결만 만든다.

## 스토리지 구현

### GCP

`cloud.provider=gcp`이면 `GcsStorageService`가 활성화된다.
GCS presigned URL은 Application Default Credentials를 사용해 V4 signed URL로 발급한다.

앱 시작 시점에 `GoogleCredentials.getApplicationDefault()` 결과가 `ServiceAccountSigner`인지 검증한다.
서명을 지원하지 않는 자격증명이면 앱 시작 단계에서 실패시켜, presigned URL 발급 시점의 403 서명 오류를 늦게 발견하지 않도록 한다.

GCP 환경에서는 다음 조건이 필요하다.

- VM 또는 실행 환경에 서비스 계정 자격증명이 연결되어 있어야 한다.
- 해당 서비스 계정이 대상 버킷 object 관리 권한을 가져야 한다.
- IAM Service Account Credentials API가 활성화되어 있어야 한다.

### Local

`cloud.provider=local` 또는 설정이 없으면 `LocalStorageService`가 활성화된다.
이 구현은 실제 스토리지에 업로드 URL을 만들지 않고 아래 형식의 mock URL을 반환한다.

```text
presignedUrl: http://localhost:8080/mock-upload/{objectKey}
publicUrl:    http://localhost:8080/mock-images/{objectKey}
```

로컬 구현은 API 응답과 DB 저장 흐름을 확인하기 위한 용도다.
실제 파일 업로드와 조회를 보장하지 않는다.

## DB 저장 구조

### `images`

| 컬럼 | 설명 |
|------|------|
| `id` | 이미지 ID. presigned URL 발급 응답의 `imageId` |
| `url` | 최종 조회용 public URL |
| `created_at` | 이미지 row 생성 시각 |

### `image_owners`

| 컬럼 | 설명 |
|------|------|
| `image_id` | `images.id` |
| `owner_type` | 이미지를 소유하거나 참조하는 도메인 타입 |
| `owner_id` | 도메인 row ID |
| `sort_order` | 같은 owner 내 이미지 정렬 순서 |

하나의 도메인 owner가 여러 이미지를 가질 수 있으므로 `imageIds`는 리스트로 전달한다.

## 경로 정책

경로는 이미지 타입에 따라 백엔드가 결정한다. 클라이언트는 경로와 파일명을 지정할 수 없다.
파일명은 백엔드가 UUID로 생성하며, 확장자는 클라이언트가 전달한 `contentType`에서 결정된다.

| ImageType | 경로 prefix |
|-----------|------------|
| `DIARY` | `diary/` |
| `USER_PROFILE` | `user/profile/` |
| `REPORT` | `report/` |

최종 오브젝트 키 예시: `diary/550e8400-e29b-41d4-a716.webp`

## URL 정책

백엔드는 클라이언트가 전달한 public URL을 저장하지 않는다.
public URL은 백엔드가 GCS object key와 설정된 base URL로 직접 생성한다.

환경별 public URL 기준값은 설정으로 관리한다.

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

## 현재 구현 범위와 TODO

현재 구현은 presigned URL 발급 시 `images` row를 생성하고, 도메인 생성 로직에서 재사용할 수 있는 `saveImages()` 연결 메서드를 제공한다.

다음 항목은 아직 구현하지 않았다.

- `ImageCleanupScheduler` 기반 orphan image row 정리
- GCS orphan object 정리
- `imageId` 소유권 검증
- `images.created_at` 기반 cleanup 인덱스

presigned URL을 발급받았지만 도메인 생성까지 이어지지 않은 경우 `images` row가 orphan 상태로 남을 수 있다.
추후 cleanup을 추가할 때는 `image_owners`에 연결되지 않고 일정 시간 이상 지난 `images` row를 삭제하는 방식으로 처리한다.

## 변경 가능성

public URL 저장 방식은 단순하지만, CDN 도메인, 버킷 도메인, 스토리지 구조가 바뀌면 기존 DB URL 마이그레이션이 필요할 수 있다.

향후 private bucket 또는 signed read URL이 필요해지면 `images.url`에 public URL을 저장하는 방식 대신 `object_key`를 저장하고 조회 시점에 URL을 생성하는 방식으로 전환을 검토한다.
