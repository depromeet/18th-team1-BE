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
  default     = "dev"
}

variable "service_name" {
  description = "서비스 이름"
  type        = string
  default     = "firstpenguin"
}

variable "machine_type" {
  description = "VM 머신 타입"
  type        = string
  default     = "e2-medium"
}
