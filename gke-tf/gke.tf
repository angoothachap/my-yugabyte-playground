

variable "gke_num_nodes" {
  default     = 1
  description = "number of gke nodes"
}
variable "machine_type" {
  default     = "n2-standard-4"
  description = "GKE node VM type"
}

# GKE east cluster
resource "google_container_cluster" "east" {
  name     = "${var.east_cluster_name}-gke"
  location = var.east_region
  
  # We can't create a cluster with no node pool defined, but we want to only use
  # separately managed node pools. So we create the smallest possible default
  # node pool and immediately delete it.
  remove_default_node_pool = true
  initial_node_count       = 1

  network    = google_compute_network.east_vpc.id
  subnetwork = google_compute_subnetwork.east_subnet.id
  ip_allocation_policy {
    cluster_secondary_range_name  = "east-cluster-secondary-range-b-name" #pod IP range
    services_secondary_range_name = google_compute_subnetwork.east_subnet.secondary_ip_range.1.range_name # service IPs
  }
}

# Separately Managed Node Pool for east GKR cluster
resource "google_container_node_pool" "east_nodes" {
  name       = "${google_container_cluster.east.name}-node-pool"
  location   = var.east_region
  cluster    = google_container_cluster.east.name
  node_count = var.gke_num_nodes

  node_config {
    oauth_scopes = [
      "https://www.googleapis.com/auth/logging.write",
      "https://www.googleapis.com/auth/monitoring",
    ]

    labels = {
      env = var.east_cluster_name
    }

    # preemptible  = true
    machine_type = var.machine_type
    tags         = ["gke-node", "${var.east_cluster_name}-gke"]
    metadata = {
      disable-legacy-endpoints = "true"
    }
    service_account = "yba-sa-owner@${project_id}.iam.gserviceaccount.com"
  }
}

