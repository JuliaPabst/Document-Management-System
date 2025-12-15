# Kubernetes Deployment Script for Document Management System
# This script deploys all services to Minikube in the correct order

Write-Host "=== Deploying Document Management System to Kubernetes ===" -ForegroundColor Cyan
Write-Host ""

# Step 1: Create namespace (optional but recommended)
Write-Host "[1/6] Creating namespace..." -ForegroundColor Yellow
kubectl create namespace paperless --dry-run=client -o yaml | kubectl apply -f -

# Step 2: Create secrets
Write-Host "[2/6] Creating secrets..." -ForegroundColor Yellow
kubectl apply -f k8s/secrets.yaml -n paperless

# Step 3: Create ConfigMaps
Write-Host "[3/6] Creating ConfigMaps..." -ForegroundColor Yellow
kubectl apply -f k8s/nginx-cm0-configmap.yaml -n paperless
kubectl apply -f k8s/prometheus-cm0-configmap.yaml -n paperless
kubectl apply -f k8s/rabbitmq-cm1-configmap.yaml -n paperless
kubectl apply -f k8s/rabbitmq-cm2-configmap.yaml -n paperless

# Step 4: Create PersistentVolumeClaims
Write-Host "[4/6] Creating PersistentVolumeClaims..." -ForegroundColor Yellow
kubectl apply -f k8s/postgres-data-persistentvolumeclaim.yaml -n paperless
kubectl apply -f k8s/rabbitmq-data-persistentvolumeclaim.yaml -n paperless
kubectl apply -f k8s/minio-data-persistentvolumeclaim.yaml -n paperless
kubectl apply -f k8s/prometheus-data-persistentvolumeclaim.yaml -n paperless
kubectl apply -f k8s/grafana-data-persistentvolumeclaim.yaml -n paperless

# Step 5: Deploy infrastructure services (databases, queues, storage)
Write-Host "[5/6] Deploying infrastructure services..." -ForegroundColor Yellow
kubectl apply -f k8s/db-deployment.yaml -n paperless
kubectl apply -f k8s/db-service.yaml -n paperless
kubectl apply -f k8s/rabbitmq-deployment.yaml -n paperless
kubectl apply -f k8s/rabbitmq-service.yaml -n paperless
kubectl apply -f k8s/minio-deployment.yaml -n paperless
kubectl apply -f k8s/minio-service.yaml -n paperless

Write-Host "Waiting for infrastructure to be ready (30s)..." -ForegroundColor Gray
Start-Sleep -Seconds 30

# Step 6: Deploy application services
Write-Host "[6/6] Deploying application services..." -ForegroundColor Yellow
kubectl apply -f k8s/rest-deployment.yaml -n paperless
kubectl apply -f k8s/rest-service.yaml -n paperless
kubectl apply -f k8s/workers-deployment.yaml -n paperless
kubectl apply -f k8s/webui-deployment.yaml -n paperless
kubectl apply -f k8s/webui-service.yaml -n paperless
kubectl apply -f k8s/nginx-deployment.yaml -n paperless
kubectl apply -f k8s/nginx-service.yaml -n paperless

# Deploy monitoring
kubectl apply -f k8s/prometheus-deployment.yaml -n paperless
kubectl apply -f k8s/prometheus-service.yaml -n paperless
kubectl apply -f k8s/grafana-deployment.yaml -n paperless
kubectl apply -f k8s/grafana-service.yaml -n paperless

# Optional: PgAdmin
kubectl apply -f k8s/pgadmin-deployment.yaml -n paperless
kubectl apply -f k8s/pgadmin-service.yaml -n paperless

Write-Host ""
Write-Host "=== Deployment Complete ===" -ForegroundColor Green
Write-Host ""
Write-Host "Check deployment status with:" -ForegroundColor Cyan
Write-Host "  kubectl get pods -n paperless" -ForegroundColor White
Write-Host "  kubectl get svc -n paperless" -ForegroundColor White
Write-Host ""
Write-Host "Access the application:" -ForegroundColor Cyan
Write-Host "  minikube service nginx -n paperless" -ForegroundColor White
Write-Host ""
Write-Host "Access Grafana:" -ForegroundColor Cyan
Write-Host "  minikube service grafana -n paperless" -ForegroundColor White
