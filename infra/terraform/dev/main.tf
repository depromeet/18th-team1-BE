terraform {
  required_version = ">= 1.0"

  backend "gcs" {
    bucket = "firstpenguin-tf-state"
    prefix = "terraform/dev"
  }

  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~> 5.0"
    }
  }
}

provider "google" {
  project = var.project_id
  region  = var.region
}

# ============================================
# VPC
# ============================================
resource "google_compute_network" "vpc" {
  name                    = "${var.env}-${var.service_name}-vpc"
  auto_create_subnetworks = false
}

# ============================================
# Subnets
# ============================================
resource "google_compute_subnetwork" "public" {
  name          = "${var.env}-${var.service_name}-subnet-public"
  ip_cidr_range = "10.0.1.0/24"
  region        = var.region
  network       = google_compute_network.vpc.id
}

resource "google_compute_subnetwork" "private" {
  name          = "${var.env}-${var.service_name}-subnet-private"
  ip_cidr_range = "10.0.10.0/24"
  region        = var.region
  network       = google_compute_network.vpc.id
}

# ============================================
# Firewall Rules
# ============================================
resource "google_compute_firewall" "allow_ssh" {
  name    = "${var.env}-${var.service_name}-fw-allow-ssh"
  network = google_compute_network.vpc.id

  allow {
    protocol = "tcp"
    ports    = ["22"]
  }

  source_ranges = ["0.0.0.0/0"]
}

resource "google_compute_firewall" "allow_app" {
  name    = "${var.env}-${var.service_name}-fw-allow-app"
  network = google_compute_network.vpc.id

  allow {
    protocol = "tcp"
    ports    = ["8080"]
  }

  source_ranges = ["0.0.0.0/0"]
}

resource "google_compute_firewall" "allow_icmp" {
  name    = "${var.env}-${var.service_name}-fw-allow-icmp"
  network = google_compute_network.vpc.id

  allow {
    protocol = "icmp"
  }

  source_ranges = ["0.0.0.0/0"]
}

resource "google_compute_firewall" "allow_internal" {
  name    = "${var.env}-${var.service_name}-fw-allow-internal"
  network = google_compute_network.vpc.id

  allow {
    protocol = "tcp"
    ports    = ["0-65535"]
  }

  allow {
    protocol = "udp"
    ports    = ["0-65535"]
  }

  allow {
    protocol = "icmp"
  }

  source_ranges = ["10.0.0.0/16"]
}

resource "google_compute_firewall" "deny_all_ingress" {
  name     = "${var.env}-${var.service_name}-fw-deny-all-ingress"
  network  = google_compute_network.vpc.id
  priority = 65534

  deny {
    protocol = "all"
  }

  source_ranges = ["0.0.0.0/0"]
}

# ============================================
# Static IP
# ============================================
resource "google_compute_address" "api" {
  name   = "${var.env}-${var.service_name}-ip-api"
  region = var.region
}

# ============================================
# VM Instance
# ============================================
resource "google_compute_instance" "api" {
  name         = "${var.env}-${var.service_name}-vm-api"
  machine_type = var.machine_type
  zone         = var.zone

  metadata = {
    block-project-ssh-keys = "true"
  }

  boot_disk {
    initialize_params {
      image = "ubuntu-os-cloud/ubuntu-2404-lts-amd64"
      size  = 30
      type  = "pd-ssd"
    }
  }

  network_interface {
    network    = google_compute_network.vpc.id
    subnetwork = google_compute_subnetwork.public.id

    access_config {
      nat_ip = google_compute_address.api.address
    }
  }

  tags = ["api-server"]

  labels = {
    env     = var.env
    service = var.service_name
  }
}
