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
