aws_region         = "us-east-1"
environment        = "dev"
project_name       = "health-system"
vpc_cidr           = "10.0.0.0/16"
availability_zones = ["us-east-1a", "us-east-1b"]
kubernetes_version = "1.29"

# Sensitive variables - provide via CLI or environment variables
# db_username = ""
# db_password = ""
# jwt_secret  = ""
