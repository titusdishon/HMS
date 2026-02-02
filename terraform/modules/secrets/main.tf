locals {
  name_prefix = "${var.project_name}-${var.environment}"
}

# Database Credentials Secret
resource "aws_secretsmanager_secret" "db_credentials" {
  name                    = "${var.project_name}/${var.environment}/db-credentials"
  description             = "Database credentials for ${var.project_name} ${var.environment}"
  recovery_window_in_days = 7

  tags = {
    Name = "${local.name_prefix}-db-credentials"
  }
}

resource "aws_secretsmanager_secret_version" "db_credentials" {
  secret_id = aws_secretsmanager_secret.db_credentials.id
  secret_string = jsonencode({
    username = var.db_username
    password = var.db_password
  })
}

# JWT Secret
resource "aws_secretsmanager_secret" "jwt_secret" {
  name                    = "${var.project_name}/${var.environment}/jwt-secret"
  description             = "JWT secret for ${var.project_name} ${var.environment}"
  recovery_window_in_days = 7

  tags = {
    Name = "${local.name_prefix}-jwt-secret"
  }
}

resource "aws_secretsmanager_secret_version" "jwt_secret" {
  secret_id = aws_secretsmanager_secret.jwt_secret.id
  secret_string = jsonencode({
    secret = var.jwt_secret
  })
}

# Application Secrets (combined)
resource "aws_secretsmanager_secret" "app_secrets" {
  name                    = "${var.project_name}/${var.environment}/app-secrets"
  description             = "Application secrets for ${var.project_name} ${var.environment}"
  recovery_window_in_days = 7

  tags = {
    Name = "${local.name_prefix}-app-secrets"
  }
}

resource "aws_secretsmanager_secret_version" "app_secrets" {
  secret_id = aws_secretsmanager_secret.app_secrets.id
  secret_string = jsonencode({
    db_username = var.db_username
    db_password = var.db_password
    jwt_secret  = var.jwt_secret
  })
}
