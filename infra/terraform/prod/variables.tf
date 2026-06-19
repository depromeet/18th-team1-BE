variable "project_id" {
  description = "GCP 프로젝트 ID"
  type        = string
}

variable "region" {
  description = "GCP 리전"
  type        = string
  default     = "asia-northeast3"
}

variable "zone" {
  description = "GCP 존"
  type        = string
  default     = "asia-northeast3-a"
}

variable "env" {
  description = "환경 (dev, prod)"
  type        = string
  default     = "prod"
}

variable "service_name" {
  description = "서비스 이름"
  type        = string
  default     = "firstpenguin"
}

variable "machine_type" {
  description = "VM 머신 타입"
  type        = string
  default     = "e2-standard-2"
}

variable "boot_disk_size_gb" {
  description = "VM boot disk 크기(GB)"
  type        = number
  default     = 50
}

variable "boot_disk_type" {
  description = "VM boot disk 타입"
  type        = string
  default     = "pd-ssd"
}

variable "postgres_disk_size_gb" {
  description = "PostgreSQL 데이터 디스크 크기(GB)"
  type        = number
  default     = 100
}

variable "postgres_disk_type" {
  description = "PostgreSQL 데이터 디스크 타입"
  type        = string
  default     = "pd-ssd"
}

variable "vm_deletion_protection" {
  description = "VM 삭제 보호 활성화 여부"
  type        = bool
  default     = true
}

variable "public_subnet_cidr" {
  description = "public subnet CIDR"
  type        = string
  default     = "10.1.1.0/24"
}

variable "private_subnet_cidr" {
  description = "private subnet CIDR"
  type        = string
  default     = "10.1.10.0/24"
}

variable "ssh_source_ranges" {
  description = "SSH 접근 허용 CIDR 목록"
  type        = list(string)
  default     = ["0.0.0.0/0"]
}

variable "app_source_ranges" {
  description = "8080 애플리케이션 접근 허용 CIDR 목록"
  type        = list(string)
  default     = ["0.0.0.0/0"]
}

variable "postgres_source_ranges" {
  description = "5432 PostgreSQL 접근 허용 CIDR 목록"
  type        = list(string)
  default     = ["0.0.0.0/0"]
}

variable "icmp_source_ranges" {
  description = "ICMP 접근 허용 CIDR 목록"
  type        = list(string)
  default     = ["0.0.0.0/0"]
}

variable "internal_source_ranges" {
  description = "VPC 내부 통신 허용 CIDR 목록"
  type        = list(string)
  default     = ["10.1.0.0/16"]
}

variable "ssh_public_keys" {
  description = "VM 인스턴스에 등록할 SSH 공개키 목록"
  type = list(object({
    user = string
    key  = string
  }))
  default = []
}

variable "gcs_cors_origins" {
  description = "GCS 이미지 버킷 CORS origin 목록"
  type        = list(string)
  default     = ["https://senti.today"]
}
