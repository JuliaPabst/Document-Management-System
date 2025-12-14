# Kubernetes Deployment with Minikube

This guide explains how to deploy the Document Management System locally using Minikube.

## Prerequisites

### Software
Ensure you have the following installed:
* **Docker Desktop** (ensure it is running)
* **Minikube** ([Download](https://minikube.sigs.k8s.io/docs/start/))
* **kubectl** ([Download](https://kubernetes.io/docs/tasks/tools/))

### System Requirements
* **CPU:** 4 Cores minimum
* **RAM:** 8 GB minimum (allocated to Minikube)
* **Disk:** 20 GB free space

---

## 1. Cluster Setup

Start the Minikube cluster with sufficient resources.

```powershell
# Start cluster with Docker driver (Recommended for Windows)
minikube start --driver=docker --cpus=4 --memory=8192
````

**Check status:**

```powershell
minikube status
kubectl get nodes
```

-----

## 2\. Configuration (Secrets)

Before deploying, you must configure the application secrets.

1.  **Create the secrets file:**

    ```powershell
    Copy-Item k8s/secrets.yaml.template k8s/secrets.yaml
    ```

2.  **Edit `k8s/secrets.yaml`:**
    Open the file and replace all `<placeholder>` values with your actual passwords and keys (Database, RabbitMQ, MinIO, OpenAI API Key, etc.).

-----

## 3\. Build Docker Images

Since Minikube runs in its own environment, you must build the images **inside** Minikube so the cluster can access them.

1.  **Point your terminal to Minikube's Docker daemon:**

    ```powershell
    # This must be run in every new PowerShell window!
    & minikube -p minikube docker-env --shell powershell | Invoke-Expression
    ```

2.  **Build the images:**

    ```powershell
    docker build -t rest:latest ./rest
    docker build -t webui:latest ./webui
    docker build -t workers:latest ./paperlessWorkers
    ```

3.  **Verify:**

    ```powershell
    docker images | Select-String "rest|webui|workers"
    ```

-----

## 4\. Deployment

Use the automated script [deploy.ps1](k8s/deploy.ps1) to deploy all services (Namespace, Secrets, Infrastructure, Apps, Monitoring) in the correct order.

```powershell
# Run the deployment script
.\k8s\deploy.ps1
```

**Check deployment status:**

```powershell
# Wait until all pods are 'Running' (this may take a few minutes)
kubectl get pods -n paperless
```

-----

## 5\. Accessing Services

### Web UI (Nginx)

The application is exposed via NodePort.

```powershell
# Open directly in default browser
minikube service nginx -n paperless

# Or display the URL
minikube service nginx -n paperless --url
```

### Direct Access (Port Forwarding)

If you need to access backend services directly (e.g., for debugging):

```powershell
# REST API (http://localhost:8081)
kubectl port-forward -n paperless svc/rest 8081:8081

# RabbitMQ (http://localhost:15672)
kubectl port-forward -n paperless svc/rabbitmq 15672:15672
```

-----

## 6\. Monitoring (Grafana)

1.  **Access Grafana:**

    ```powershell
    # Open in browser
    minikube service grafana -n paperless
    ```

    *Or via Port-Forward: `kubectl port-forward -n paperless svc/grafana 3002:3002` (http://localhost:3002)*

2.  **Login:**

      * **User:** `admin`
      * **Password:** (The value you set in `secrets.yaml`)

3.  **Dashboard:**
    Go to **Dashboards** → **Paperless - Spring Boot 3.x Statistics**.

-----

## 7\. Cleanup

To remove the deployment and free up resources:

```powershell
# Option A: Delete all resources but keep the cluster running
kubectl delete namespace paperless

# Option B: Stop the cluster (saves battery/resources)
minikube stop

# Option C: Delete everything (WARNING: All data lost)
minikube delete
```

-----

## Troubleshooting Common Issues

**1. Pods show `ImagePullBackOff` or `ErrImagePull`**

  * **Cause:** Minikube cannot find the local Docker images.
  * **Fix:** Ensure you ran the `docker-env` command (Step 3) and rebuilt the images within that specific shell session.

**2. Pods show `CrashLoopBackOff`**

  * **Cause:** Application startup error (often DB connection or missing secrets).
  * **Fix:** Check the logs:
    ```powershell
    kubectl logs -n paperless <pod-name>
    ```

**3. Services not accessible**

  * **Fix:** Run `minikube tunnel` in a separate administrator terminal to ensure network routes are open, or stick to `minikube service <name>` commands.

-----