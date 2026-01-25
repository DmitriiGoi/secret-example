# Kubernetes Secret Injection Example for Spring Boot

This project demonstrates different ways to inject Kubernetes Secrets into a Spring Boot application.

## Injection Methods

### 1. Environment Variables (Direct)
Secrets can be mapped directly to environment variables in the Deployment manifest.
- **K8s Manifest**: See `k8s/deployment-env.yaml` (the `DB_PASSWORD` part).
- **Spring Boot**: Accessed using `@Value("${DB_PASSWORD}")`.

### 2. Environment Variables (Spring Property Mapping)
Spring Boot automatically maps environment variables to properties (e.g., `APP_API_KEY` to `app.api-key`).
- **K8s Manifest**: See `k8s/deployment-env.yaml` (the `APP_API_KEY` part).
- **Spring Boot**: Accessed using `@Value("${app.api-key}")`.

### 3. Volume Mounts (Files)
Secrets can be mounted as files into the container's file system.
- **K8s Manifest**: See `k8s/deployment-volume.yaml`.
- **Spring Boot**: Read using standard Java File I/O (see `SecretController.java`).

### 4. Volume Mounts (ConfigTree)
Spring Boot can automatically import a directory of files as properties using `configtree`.
- **K8s Manifest**: See `k8s/deployment-configtree.yaml`. It sets `SPRING_CONFIG_IMPORT=configtree:/etc/config/`.
- **Spring Boot**: Accessed using `@Value("${secret-config-tree}")` where `secret-config-tree` is the name of the file in the secret.

### 5. Spring Cloud Kubernetes
Read secrets directly from the Kubernetes API using `spring-cloud-starter-kubernetes-client-config`.
- **K8s Manifest**: See `k8s/deployment-spring-cloud.yaml` and `k8s/rbac.yaml`.
- **Spring Boot**: Properties are automatically mapped from Kubernetes Secrets named after the application (or specified via config).

### 6. Sidecar Container (HashiCorp Vault Agent)
A sidecar container can manage secrets (e.g., fetching from a vault, rotating them) and share them with the main container via a shared volume. This example uses a **Vault Agent** sidecar.
- **K8s Manifest**: See `k8s/deployment-sidecar.yaml`. It includes the Vault Agent container and a `ConfigMap` for its configuration.
- **Spring Boot**: Read from the shared volume `/etc/secrets/vault-secret.txt`.
- **Note**: The main secret for other methods is in `k8s/secret.yaml` with keys `secret-env-variable`, `secret-config-tree`, `secret-spring-k8s-api`, and `secret-volume`.

## How to Build

This project uses [Jib](https://github.com/GoogleContainerTools/jib) to build Docker images without needing a Docker daemon.

To build the image and push it to Docker Hub:
```bash
./mvnw clean compile jib:build
```

To build the image to a local Docker daemon:
```bash
./mvnw clean compile jib:dockerBuild
```

## How to Run

1. **Create the Namespace**:
   ```bash
   kubectl apply -f k8s/namespace.yaml
   ```

2. **Apply the RBAC and Secrets**:
   ```bash
   kubectl apply -f k8s/secret.yaml
   kubectl apply -f k8s/rbac.yaml
   ```

3. **Deploy the Vault Server**:
   ```bash
   kubectl apply -f k8s/vault-server.yaml
   ```

4. **Initialize Vault Secrets**:
   Since the Vault server is running in `-dev` mode, you need to manually enable the Kubernetes authentication and inject the secret:
   ```bash
   # Get the Vault pod name
   $VAULT_POD=$(kubectl get pods -n secret-example -l app=vault -o jsonpath='{.items[0].metadata.name}')

   # Enable Kubernetes Auth and Configure it
   kubectl exec $VAULT_POD -n secret-example -- sh -c "VAULT_TOKEN=root vault auth enable kubernetes"
   kubectl exec $VAULT_POD -n secret-example -- sh -c "VAULT_TOKEN=root vault write auth/kubernetes/config kubernetes_host=https://kubernetes.default.svc:443 disable_iss_validation=true"

   # Write the Policy
   echo 'path "secret/data/myapp/config" { capabilities = ["read"] }' > myapp-policy.hcl
   kubectl cp myapp-policy.hcl secret-example/$VAULT_POD:/tmp/policy.hcl
   kubectl exec $VAULT_POD -n secret-example -- sh -c "VAULT_TOKEN=root vault policy write myapp-policy /tmp/policy.hcl"

   # Create the Role
   kubectl exec $VAULT_POD -n secret-example -- sh -c "VAULT_TOKEN=root vault write auth/kubernetes/role/example-role bound_service_account_names=spring-cloud-k8s-sa bound_service_account_namespaces=secret-example policies=myapp-policy ttl=24h"

   # Inject the Secret
   kubectl exec $VAULT_POD -n secret-example -- sh -c "VAULT_TOKEN=root vault kv put secret/myapp/config password='vault-password-value'"
   ```

5. **Deploy the Unified Application**:
   ```bash
   kubectl apply -f k8s/deployment-all.yaml
   ```

6. **Verify**:
   Once the pod is running, check the logs to see the injected values:
   ```bash
   kubectl logs -l app=secret-example-all -n secret-example -c secret-example
   ```

## Files in this example:
- `src/main/java/com/example/secretexample/SecretLogger.java`: Java code reading and logging secrets.
- `k8s/namespace.yaml`: Definition of the Kubernetes Namespace.
- `k8s/secret.yaml`: Definition of the Kubernetes Secret.
- `k8s/rbac.yaml`: RBAC for Spring Cloud Kubernetes and Vault authentication.
- `k8s/vault-server.yaml`: Vault server deployment.
- `k8s/deployment-all.yaml`: Unified deployment using all 5 injection methods.
- `deprecated/`: Contains individual deployment files for reference.
