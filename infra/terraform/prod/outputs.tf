output "vm_external_ip" {
  description = "VM 외부 IP"
  value       = google_compute_address.api.address
}

output "vm_name" {
  description = "VM 인스턴스 이름"
  value       = google_compute_instance.api.name
}

output "vpc_name" {
  description = "VPC 이름"
  value       = google_compute_network.vpc.name
}

output "image_bucket_name" {
  description = "GCS 이미지 버킷 이름"
  value       = google_storage_bucket.images.name
}

output "service_account_email" {
  description = "API 서버 서비스 계정 이메일"
  value       = google_service_account.api.email
}
