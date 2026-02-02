# Deployment Guide

This guide covers deploying the Health Management System to AWS EKS with Fargate.

## Prerequisites

- AWS CLI configured with appropriate credentials
- Terraform >= 1.5.0
- kubectl >= 1.29
- Docker
- Helm >= 3.0

## Infrastructure Setup

### 1. Initialize Terraform

```bash
cd terraform/environments/dev

# Initialize Terraform
terraform init

# Review the plan
terraform plan -out=tfplan

# Apply the infrastructure
terraform apply tfplan
```

### 2. Terraform Resources Created

| Resource | Purpose |
|----------|---------|
| VPC | Network isolation with public/private subnets |
| EKS Cluster | Managed Kubernetes control plane |
| Fargate Profiles | Serverless compute for pods |
| ECR | Container image registry |
| IAM Roles | Service accounts and permissions |
| Secrets Manager | Database credentials storage |
| ALB Controller | Ingress load balancer management |

### 3. Configure kubectl

```bash
# Get cluster credentials
aws eks update-kubeconfig \
  --name health-system-dev \
  --region us-east-1

# Verify connection
kubectl get nodes
kubectl get pods -A
```

## Application Deployment

### 1. Build and Push Docker Image

```bash
# Login to ECR
aws ecr get-login-password --region us-east-1 | \
  docker login --username AWS --password-stdin \
  <account-id>.dkr.ecr.us-east-1.amazonaws.com

# Build image
docker build -t health-management-system:latest .

# Tag for ECR
docker tag health-management-system:latest \
  <account-id>.dkr.ecr.us-east-1.amazonaws.com/health-management-system:latest

# Push to ECR
docker push <account-id>.dkr.ecr.us-east-1.amazonaws.com/health-management-system:latest
```

### 2. Deploy to Kubernetes

```bash
# Deploy using Kustomize
kubectl apply -k k8s/overlays/dev

# Verify deployment
kubectl get pods -n health-system
kubectl get svc -n health-system
kubectl get ingress -n health-system
```

### 3. Verify Deployment

```bash
# Check pod status
kubectl get pods -n health-system -w

# View logs
kubectl logs -f deployment/health-management-system -n health-system

# Check ingress
kubectl describe ingress health-management-system -n health-system
```

## Environment Configuration

### Development

```yaml
# k8s/overlays/dev/kustomization.yaml
replicas: 1
resources:
  cpu: 256m
  memory: 512Mi
```

### Staging

```yaml
# k8s/overlays/staging/kustomization.yaml
replicas: 2
resources:
  cpu: 512m
  memory: 1Gi
```

### Production

```yaml
# k8s/overlays/prod/kustomization.yaml
replicas: 3
resources:
  cpu: 1000m
  memory: 2Gi
hpa:
  minReplicas: 3
  maxReplicas: 10
```

## Secrets Management

### AWS Secrets Manager

Secrets are stored in AWS Secrets Manager and synced to Kubernetes using External Secrets Operator.

```bash
# Create secret in AWS
aws secretsmanager create-secret \
  --name health-system/dev/database \
  --secret-string '{"username":"app","password":"secret"}'

# Verify ExternalSecret sync
kubectl get externalsecrets -n health-system
kubectl get secrets -n health-system
```

## Monitoring & Logging

### CloudWatch Container Insights

```bash
# Enable Container Insights
aws eks update-cluster-config \
  --name health-system-dev \
  --logging '{"clusterLogging":[{"types":["api","audit","authenticator","controllerManager","scheduler"],"enabled":true}]}'
```

### Application Logs

```bash
# Stream logs
kubectl logs -f -l app=health-management-system -n health-system

# View logs in CloudWatch
aws logs tail /aws/eks/health-system-dev/containers --follow
```

## Scaling

### Manual Scaling

```bash
# Scale deployment
kubectl scale deployment health-management-system \
  --replicas=5 -n health-system
```

### Horizontal Pod Autoscaler

```bash
# Check HPA status
kubectl get hpa -n health-system

# Describe HPA
kubectl describe hpa health-management-system-hpa -n health-system
```

## Rollback

### Kubernetes Rollback

```bash
# View rollout history
kubectl rollout history deployment/health-management-system -n health-system

# Rollback to previous version
kubectl rollout undo deployment/health-management-system -n health-system

# Rollback to specific revision
kubectl rollout undo deployment/health-management-system \
  --to-revision=2 -n health-system
```

## Troubleshooting

### Common Issues

1. **Pods not starting**
   ```bash
   kubectl describe pod <pod-name> -n health-system
   kubectl logs <pod-name> -n health-system --previous
   ```

2. **Database connection issues**
   ```bash
   kubectl exec -it <pod-name> -n health-system -- \
     curl -v telnet://database-host:5432
   ```

3. **Ingress not working**
   ```bash
   kubectl describe ingress health-management-system -n health-system
   kubectl logs -n kube-system -l app.kubernetes.io/name=aws-load-balancer-controller
   ```

### Health Checks

```bash
# Liveness probe
curl http://<alb-dns>/actuator/health/liveness

# Readiness probe
curl http://<alb-dns>/actuator/health/readiness

# Full health details
curl http://<alb-dns>/actuator/health
```

## Cleanup

```bash
# Delete Kubernetes resources
kubectl delete -k k8s/overlays/dev

# Destroy Terraform infrastructure
cd terraform/environments/dev
terraform destroy
```

## Security Considerations

1. **Network Security**
   - Pods run in private subnets only
   - ALB handles SSL termination
   - Security groups restrict traffic

2. **Secrets**
   - Never commit secrets to version control
   - Use AWS Secrets Manager for all credentials
   - Rotate secrets regularly

3. **IAM**
   - Use IRSA (IAM Roles for Service Accounts)
   - Follow principle of least privilege
   - Audit IAM policies regularly

4. **Container Security**
   - Run containers as non-root user
   - Use read-only root filesystem
   - Enable ECR image scanning
