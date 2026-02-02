output "fargate_pod_execution_role_arn" {
  description = "ARN of the Fargate pod execution role"
  value       = aws_iam_role.fargate_pod_execution.arn
}

output "fargate_pod_execution_role_name" {
  description = "Name of the Fargate pod execution role"
  value       = aws_iam_role.fargate_pod_execution.name
}

output "gitlab_ci_role_arn" {
  description = "ARN of the GitLab CI role"
  value       = aws_iam_role.gitlab_ci.arn
}

output "gitlab_ci_role_name" {
  description = "Name of the GitLab CI role"
  value       = aws_iam_role.gitlab_ci.name
}
