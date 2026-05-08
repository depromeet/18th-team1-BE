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

locals {
  instance_ssh_keys = [
    for ssh_key in var.ssh_public_keys : "${ssh_key.user}:${ssh_key.key}"
  ]
}

# ============================================
# Project APIs
# ============================================
resource "google_project_service" "iam_credentials" {
  service            = "iamcredentials.googleapis.com"
  disable_on_destroy = false
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

  target_tags   = ["api-server"]
  source_ranges = ["0.0.0.0/0"]
}

resource "google_compute_firewall" "allow_app" {
  name    = "${var.env}-${var.service_name}-fw-allow-app"
  network = google_compute_network.vpc.id

  allow {
    protocol = "tcp"
    ports    = ["8080"]
  }

  target_tags   = ["api-server"]
  source_ranges = ["0.0.0.0/0"]
}

resource "google_compute_firewall" "allow_icmp" {
  name    = "${var.env}-${var.service_name}-fw-allow-icmp"
  network = google_compute_network.vpc.id

  allow {
    protocol = "icmp"
  }

  target_tags   = ["api-server"]
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

  allow_stopping_for_update = true

  metadata = merge(
    {
      block-project-ssh-keys = "true"
    },
    length(local.instance_ssh_keys) > 0 ? {
      ssh-keys = join("\n", local.instance_ssh_keys)
    } : {}
  )

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

  service_account {
    email  = google_service_account.api.email
    scopes = ["cloud-platform"]
  }

  tags = ["api-server"]

  labels = {
    env     = var.env
    service = var.service_name
  }
}

# ============================================
# Service Account (이미지 버킷용)
# ============================================
resource "google_service_account" "api" {
  account_id   = "${var.env}-${var.service_name}-api-sa"
  display_name = "${var.env} API Server Service Account"
}

resource "google_service_account_iam_member" "api_sa_token_creator" {
  service_account_id = google_service_account.api.name
  role               = "roles/iam.serviceAccountTokenCreator"
  member             = "serviceAccount:${google_service_account.api.email}"
}

# ============================================
# GCS Image Bucket
# ============================================
resource "google_storage_bucket" "images" {
  name          = "${var.env}-${var.service_name}-images"
  location      = var.region
  force_destroy = false

  uniform_bucket_level_access = true

  cors {
    origin          = ["https://dev.senti.today", "http://localhost:3000"]
    method          = ["GET", "PUT"]
    response_header = ["Content-Type", "Access-Control-Allow-Origin"]
    max_age_seconds = 3600
  }
}

resource "google_storage_bucket_iam_member" "images_public_read" {
  bucket = google_storage_bucket.images.name
  role   = "roles/storage.objectViewer"
  member = "allUsers"
}

resource "google_storage_bucket_iam_member" "images_api_sa_admin" {
  bucket = google_storage_bucket.images.name
  role   = "roles/storage.objectAdmin"
  member = "serviceAccount:${google_service_account.api.email}"
}
