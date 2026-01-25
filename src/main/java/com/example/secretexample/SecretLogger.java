package com.example.secretexample;

import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@Component
public class SecretLogger {

    private static final Logger logger = LoggerFactory.getLogger(SecretLogger.class);

    // Injected via environment variable (standard K8s env injection)
    @Value("${SECRET_CONFIG_TREE:Not Set}")
    private String secretConfigTreeFromEnv;

    // Injected via Spring Boot property (backed by env var)
    @Value("${SECRET_ENV_VARIABLE:Not Set}")
    private String secretEnvVariable;

    // Injected via Spring Boot configtree (file name becomes property name)
    @Value("${secret-config-tree:Not Set}")
    private String secretConfigTree;

    @Value("${secret-volume:Not Set}")
    private String secretVolumeFromConfigTree;

    // Injected via Spring Cloud Kubernetes (reads directly from K8s Secret 'secret-app')
    @Value("${secret-spring-k8s-api:Not Set}")
    private String secretSpringK8sApi;

    @EventListener(ApplicationReadyEvent.class)
    public void logSecrets() {
        Map<String, String> secrets = new HashMap<>();
        secrets.put("secret-config-tree (from env)", secretConfigTreeFromEnv);
        secrets.put("secret-env-variable (from property/env)", secretEnvVariable);
        secrets.put("secret-config-tree (from configtree)", secretConfigTree);
        secrets.put("secret-volume (from configtree)", secretVolumeFromConfigTree);
        secrets.put("secret-spring-k8s-api (from Spring Cloud K8s)", secretSpringK8sApi);

        // Reading secret from a mounted file
        secrets.put("secret-volume (from volume)", readFile("/etc/secrets/secret-volume"));

        // Reading secret from a Vault Agent sidecar-mounted volume
        secrets.put("secret-vault (from sidecar)", readFile("/etc/vault-secrets/vault-secret.txt"));

        logger.info("--- Loaded Secrets ---");
        secrets.forEach((key, value) -> logger.info("{}: {}", key, value));
        logger.info("----------------------");
    }

    private String readFile(String path) {
        try {
            return new String(Files.readAllBytes(Paths.get(path))).trim();
        } catch (IOException e) {
            return "File not found or unreadable: " + e.getMessage();
        }
    }
}
