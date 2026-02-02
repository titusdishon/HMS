locals {
  name_prefix = "${var.project_name}-${var.environment}"
}

# EKS Cluster
resource "aws_eks_cluster" "main" {
  name     = "${local.name_prefix}-cluster"
  role_arn = aws_iam_role.cluster.arn
  version  = var.kubernetes_version

  vpc_config {
    subnet_ids              = var.private_subnet_ids
    endpoint_private_access = true
    endpoint_public_access  = true
    security_group_ids      = [aws_security_group.cluster.id]
  }

  enabled_cluster_log_types = ["api", "audit", "authenticator", "controllerManager", "scheduler"]

  tags = {
    Name = "${local.name_prefix}-cluster"
  }

  depends_on = [
    aws_iam_role_policy_attachment.cluster_policy,
    aws_iam_role_policy_attachment.cluster_vpc_policy,
  ]
}

# EKS Cluster IAM Role
resource "aws_iam_role" "cluster" {
  name = "${local.name_prefix}-cluster-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "eks.amazonaws.com"
        }
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "cluster_policy" {
  policy_arn = "arn:aws:iam::aws:policy/AmazonEKSClusterPolicy"
  role       = aws_iam_role.cluster.name
}

resource "aws_iam_role_policy_attachment" "cluster_vpc_policy" {
  policy_arn = "arn:aws:iam::aws:policy/AmazonEKSVPCResourceController"
  role       = aws_iam_role.cluster.name
}

# Cluster Security Group
resource "aws_security_group" "cluster" {
  name        = "${local.name_prefix}-cluster-sg"
  description = "Security group for EKS cluster"
  vpc_id      = var.vpc_id

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${local.name_prefix}-cluster-sg"
  }
}

# Fargate Profile for default namespace
resource "aws_eks_fargate_profile" "default" {
  cluster_name           = aws_eks_cluster.main.name
  fargate_profile_name   = "${local.name_prefix}-default"
  pod_execution_role_arn = var.fargate_pod_role_arn
  subnet_ids             = var.private_subnet_ids

  selector {
    namespace = "default"
  }

  selector {
    namespace = var.project_name
  }

  tags = {
    Name = "${local.name_prefix}-fargate-default"
  }
}

# Fargate Profile for kube-system namespace
resource "aws_eks_fargate_profile" "kube_system" {
  cluster_name           = aws_eks_cluster.main.name
  fargate_profile_name   = "${local.name_prefix}-kube-system"
  pod_execution_role_arn = var.fargate_pod_role_arn
  subnet_ids             = var.private_subnet_ids

  selector {
    namespace = "kube-system"
  }

  tags = {
    Name = "${local.name_prefix}-fargate-kube-system"
  }
}

# OIDC Provider for IRSA (IAM Roles for Service Accounts)
data "tls_certificate" "cluster" {
  url = aws_eks_cluster.main.identity[0].oidc[0].issuer
}

resource "aws_iam_openid_connect_provider" "cluster" {
  client_id_list  = ["sts.amazonaws.com"]
  thumbprint_list = [data.tls_certificate.cluster.certificates[0].sha1_fingerprint]
  url             = aws_eks_cluster.main.identity[0].oidc[0].issuer

  tags = {
    Name = "${local.name_prefix}-oidc-provider"
  }
}

# CloudWatch Log Group for EKS
resource "aws_cloudwatch_log_group" "cluster" {
  name              = "/aws/eks/${local.name_prefix}-cluster/cluster"
  retention_in_days = 7

  tags = {
    Name = "${local.name_prefix}-cluster-logs"
  }
}
