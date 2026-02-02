aws_region         = "us-east-1"
environment        = "prod"
project_name       = "health-system"
vpc_cidr           = "10.2.0.0/16"
availability_zones = ["us-east-1a", "us-east-1b", "us-east-1c"]
kubernetes_version = "1.29"

# Sensitive variables - provide via CLI or environment variables
# db_username = ""
# db_password = ""
# jwt_secret  = ""
