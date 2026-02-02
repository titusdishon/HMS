terraform {
  backend "s3" {
    # Configure backend via CLI or terraform.tfvars
    # bucket         = "health-system-terraform-state"
    # key            = "terraform.tfstate"
    # region         = "us-east-1"
    # encrypt        = true
    # dynamodb_table = "terraform-locks"
  }
}

provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project     = var.project_name
      Environment = var.environment
      ManagedBy   = "Terraform"
    }
  }
}

# Data sources for EKS authentication
data "aws_eks_cluster" "cluster" {
  name       = module.eks.cluster_name
  depends_on = [module.eks]
}

data "aws_eks_cluster_auth" "cluster" {
  name       = module.eks.cluster_name
  depends_on = [module.eks]
}

provider "kubernetes" {
  host                   = data.aws_eks_cluster.cluster.endpoint
  cluster_ca_certificate = base64decode(data.aws_eks_cluster.cluster.certificate_authority[0].data)
  token                  = data.aws_eks_cluster_auth.cluster.token
}

provider "helm" {
  kubernetes {
    host                   = data.aws_eks_cluster.cluster.endpoint
    cluster_ca_certificate = base64decode(data.aws_eks_cluster.cluster.certificate_authority[0].data)
    token                  = data.aws_eks_cluster_auth.cluster.token
  }
}

# VPC Module
module "vpc" {
  source = "./modules/vpc"

  project_name       = var.project_name
  environment        = var.environment
  vpc_cidr           = var.vpc_cidr
  availability_zones = var.availability_zones
}

# ECR Module
module "ecr" {
  source = "./modules/ecr"

  project_name = var.project_name
  environment  = var.environment
}

# IAM Module
module "iam" {
  source = "./modules/iam"

  project_name = var.project_name
  environment  = var.environment
}

# EKS Module
module "eks" {
  source = "./modules/eks"

  project_name         = var.project_name
  environment          = var.environment
  kubernetes_version   = var.kubernetes_version
  vpc_id               = module.vpc.vpc_id
  private_subnet_ids   = module.vpc.private_subnet_ids
  fargate_pod_role_arn = module.iam.fargate_pod_execution_role_arn
}

# Secrets Module
module "secrets" {
  source = "./modules/secrets"

  project_name = var.project_name
  environment  = var.environment
  db_username  = var.db_username
  db_password  = var.db_password
  jwt_secret   = var.jwt_secret
}

# ALB Controller (Helm)
module "alb_controller" {
  source = "./modules/alb"

  project_name             = var.project_name
  environment              = var.environment
  cluster_name             = module.eks.cluster_name
  cluster_oidc_issuer_url  = module.eks.cluster_oidc_issuer_url
  vpc_id                   = module.vpc.vpc_id

  depends_on = [module.eks]
}
