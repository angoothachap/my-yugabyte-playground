variable "project_id" {
  description = "project id"
}
variable "east_cluster_name" {
  description = "east cluster name"
}
variable "east_region" {
  description = "east region name"
}

provider "google" {
  project = var.project_id
}

# east VPC
resource "google_compute_network" "east_vpc" {
  name                    = "${var.east_cluster_name}-vpc"
  auto_create_subnetworks = "false"
  #region  = var.src_region
  
}

# east Subnet
resource "google_compute_subnetwork" "east_subnet" {
  name          = "${var.east_cluster_name}-subnet"
  region        = var.east_region
  network       = google_compute_network.east_vpc.id
  ip_cidr_range = "11.0.0.0/16" #node

  secondary_ip_range {
    range_name    = "east-cluster-secondary-range-b-name"
    ip_cidr_range = "12.0.0.0/16" #pod
  }
  secondary_ip_range {
    range_name    = "east-cluster-secondary-range-c-name"
    ip_cidr_range = "13.0.0.0/20" #service
  }
   
}#TODO: narrow down CIDR further
