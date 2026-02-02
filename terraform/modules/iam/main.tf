locals {
  name_prefix = "${var.project_name}-${var.environment}"
}

# Fargate Pod Execution Role
resource "aws_iam_role" "fargate_pod_execution" {
  name = "${local.name_prefix}-fargate-pod-execution-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "eks-fargate-pods.amazonaws.com"
        }
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "fargate_pod_execution" {
  policy_arn = "arn:aws:iam::aws:policy/AmazonEKSFargatePodExecutionRolePolicy"
  role       = aws_iam_role.fargate_pod_execution.name
}

# Policy for Fargate pods to access ECR
resource "aws_iam_role_policy" "fargate_ecr_access" {
  name = "${local.name_prefix}-fargate-ecr-access"
  role = aws_iam_role.fargate_pod_execution.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "ecr:GetAuthorizationToken",
          "ecr:BatchCheckLayerAvailability",
          "ecr:GetDownloadUrlForLayer",
          "ecr:BatchGetImage"
        ]
        Resource = "*"
      }
    ]
  })
}

# Policy for Fargate pods to access Secrets Manager
resource "aws_iam_role_policy" "fargate_secrets_access" {
  name = "${local.name_prefix}-fargate-secrets-access"
  role = aws_iam_role.fargate_pod_execution.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "secretsmanager:GetSecretValue",
          "secretsmanager:DescribeSecret"
        ]
        Resource = "arn:aws:secretsmanager:*:*:secret:${var.project_name}/*"
      }
    ]
  })
}

# Policy for CloudWatch Logs
resource "aws_iam_role_policy" "fargate_cloudwatch" {
  name = "${local.name_prefix}-fargate-cloudwatch"
  role = aws_iam_role.fargate_pod_execution.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "logs:CreateLogStream",
          "logs:CreateLogGroup",
          "logs:DescribeLogStreams",
          "logs:PutLogEvents"
        ]
        Resource = "*"
      }
    ]
  })
}

# GitLab CI/CD Role (for deployment)
resource "aws_iam_role" "gitlab_ci" {
  name = "${local.name_prefix}-gitlab-ci-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          AWS = "arn:aws:iam::${data.aws_caller_identity.current.account_id}:root"
        }
      }
    ]
  })
}

resource "aws_iam_role_policy" "gitlab_ci" {
  name = "${local.name_prefix}-gitlab-ci-policy"
  role = aws_iam_role.gitlab_ci.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "eks:DescribeCluster",
          "eks:ListClusters"
        ]
        Resource = "*"
      },
      {
        Effect = "Allow"
        Action = [
          "ecr:GetAuthorizationToken",
          "ecr:BatchCheckLayerAvailability",
          "ecr:GetDownloadUrlForLayer",
          "ecr:BatchGetImage",
          "ecr:PutImage",
          "ecr:InitiateLayerUpload",
          "ecr:UploadLayerPart",
          "ecr:CompleteLayerUpload"
        ]
        Resource = "*"
      }
    ]
  })
}

data "aws_caller_identity" "current" {}
