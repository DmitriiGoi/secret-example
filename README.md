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

2. **Apply the Secret**:
   ```bash
   kubectl apply -f k8s/secret.yaml
   ```

3. **Deploy the App (choose one)**:
   - For Env Var method: `kubectl apply -f k8s/deployment-env.yaml`
   - For Volume method: `kubectl apply -f k8s/deployment-volume.yaml`
   - For ConfigTree method: `kubectl apply -f k8s/deployment-configtree.yaml`
   - For Spring Cloud method: `kubectl apply -f k8s/rbac.yaml` then `kubectl apply -f k8s/deployment-spring-cloud.yaml`
   - For Sidecar method: `kubectl apply -f k8s/deployment-sidecar.yaml` (Note: requires a running Vault instance and Kubernetes Auth method configured in Vault)

4. **Verify**:
   Once the pod is running, you can access the `/secrets` endpoint to see the injected values.
   Note: You may need to use `kubectl get pods -n secret-example` to find the pod name or IP.
   ```bash
   kubectl exec --stdin --tty secret-example-configtree-bddc9b94c-jrs4r -- curl localhost:8080/secrets
   ```
   ```json
   {
       "secret-config-tree (from env)": "secret value for config tree",
       "secret-volume (from volume)": "secret value for volume",
       "secret-spring-k8s-api (from Spring Cloud K8s)": "secret value for spring k8s api",
       "secret-env-variable (from property/env)": "secret value for env variable",
       "secret-config-tree (from configtree)": "secret value for config tree",
       "secret-volume (from configtree)": "secret value for volume",
       "secret-vault (from sidecar)": "File not found or unreadable: /etc/secrets/vault-secret.txt"
   }
   ```

## Files in this example:
- `src/main/java/com/example/secretexample/SecretController.java`: Java code reading secrets.
- `k8s/namespace.yaml`: Definition of the Kubernetes Namespace.
- `k8s/secret.yaml`: Definition of the Kubernetes Secret.
- `k8s/deployment-env.yaml`: Injection via Environment Variables.
- `k8s/deployment-volume.yaml`: Injection via Volume Mount.
- `k8s/deployment-configtree.yaml`: Injection via ConfigTree volume mount.
- `k8s/deployment-spring-cloud.yaml`: Injection via Spring Cloud Kubernetes.
- `k8s/rbac.yaml`: RBAC for Spring Cloud Kubernetes.
- `k8s/deployment-sidecar.yaml`: Sidecar container example.
