# Terraform dev 인프라

## 목표

dev 환경의 GCP 인프라를 Terraform으로 관리한다.
수동으로 생성한 리소스와 코드 상태가 어긋나지 않도록, 변경 전에는 항상 plan을 확인한다.

관련 이슈: #3, #42, #74

## 작업 디렉터리

```bash
cd infra/terraform/dev
```

dev state는 GCS backend를 사용한다.

```hcl
backend "gcs" {
  bucket = "firstpenguin-tf-state"
  prefix = "terraform/dev"
}
```

`terraform.tfstate`와 `.terraform/` 산출물은 커밋하지 않는다.

## 기본 변수

| 변수 | 기본값 또는 정책 |
|------|------------------|
| `project_id` | `project-10e57bb0-e960-4b16-9fa` |
| `region` | `asia-northeast3` |
| `zone` | `asia-northeast3-a` |
| `env` | `dev` |
| `service_name` | `firstpenguin` |
| `machine_type` | `e2-medium` |

리소스 이름은 `{env}-{service_name}-{resource}` 형식을 따른다.

## 주요 리소스

| 종류 | 이름 |
|------|------|
| VPC | `dev-firstpenguin-vpc` |
| Public Subnet | `dev-firstpenguin-subnet-public` (`10.0.1.0/24`) |
| Private Subnet | `dev-firstpenguin-subnet-private` (`10.0.10.0/24`) |
| Static IP | `dev-firstpenguin-ip-api` |
| VM | `dev-firstpenguin-vm-api` |
| API Service Account | `dev-firstpenguin-api-sa` |
| Image Bucket | `dev-firstpenguin-images` |

VM은 Ubuntu 24.04 LTS, 30GB SSD boot disk를 사용한다.
VM에는 `api-server`, `db-server` network tag를 부여한다.

## 방화벽

| 규칙 | 대상 태그 | 허용 |
|------|-----------|------|
| SSH | `api-server` | TCP 22 |
| App | `api-server` | TCP 8080 |
| PostgreSQL | `db-server` | TCP 5432 |
| ICMP | `api-server` | ICMP |
| Internal | 전체 내부 대역 | TCP/UDP/ICMP, `10.0.0.0/16` |

PostgreSQL 5432는 현재 dev 운영 편의를 위해 열려 있다.
운영 환경에서는 접근 source range를 제한하거나 private 접근 구조로 전환한다.

## SSH 키

VM metadata에는 project SSH key를 차단하고, `ssh_public_keys` 변수로 전달한 키만 등록한다.

```hcl
block-project-ssh-keys = "true"
```

SSH 키를 바꾸면 `terraform.tfvars`를 수정한 뒤 plan을 확인하고 apply한다.

## 이미지 버킷

이미지 버킷은 public object 조회를 전제로 운영한다.

| 항목 | 정책 |
|------|------|
| bucket | `dev-firstpenguin-images` |
| location | `asia-northeast3` |
| uniform bucket-level access | enabled |
| public read | `allUsers roles/storage.objectViewer` |
| API service account | `roles/storage.objectAdmin` |
| CORS origin | `https://dev.senti.today`, `http://localhost:3000` |
| CORS method | `GET`, `PUT` |

presigned URL 발급에는 서비스 계정 서명이 필요하다.
이를 위해 API 서비스 계정에 자기 자신에 대한 `roles/iam.serviceAccountTokenCreator`를 부여하고, 프로젝트 API로 `iamcredentials.googleapis.com`을 활성화한다.

## 실행 순서

변경 전 항상 plan을 먼저 확인한다.

```bash
terraform init
terraform plan
terraform apply
```

원격 state를 사용하므로 동시에 여러 사람이 apply하지 않는다.
apply 결과로 VM 외부 IP, VM 이름, VPC 이름을 output으로 확인할 수 있다.

```bash
terraform output
```

## 주의사항

- `terraform.tfstate`, `terraform.tfstate.backup`, `.terraform/` 산출물은 커밋하지 않는다.
- GCS backend bucket은 Terraform이 관리하는 리소스가 아니라 선행 준비 리소스다.
- public image bucket에는 민감한 이미지를 업로드하지 않는다.
- apply 전에는 변경 대상 리소스가 dev 환경인지 확인한다.
- prod 환경은 별도 디렉터리에서 dev와 분리해 관리한다.
