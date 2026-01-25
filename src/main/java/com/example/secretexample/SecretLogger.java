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
import java.util.Map;
import java.util.TreeMap;

@Component
public class SecretLogger {

    private static final Logger logger = LoggerFactory.getLogger(SecretLogger.class);

    // 1. Injected via environment variable
    @Value("${ENV_SECRET:Not Set}")
    private String envSecret;

    // 3. Injected via Spring Boot configtree (multiple files)
    @Value("${configtree-file1:Not Set}")
    private String configTreeFile1;

    @Value("${configtree-file2:Not Set}")
    private String configTreeFile2;

    // 4. Injected via Spring Cloud Kubernetes (reads directly from K8s Secret 'secret-app')
    @Value("${api-secret:Not Set}")
    private String apiSecret;

    @Value("${extra-api-secret:Not Set}")
    private String extraApiSecret;

    @EventListener(ApplicationReadyEvent.class)
    public void logSecrets() {
        Map<String, String> secrets = new TreeMap<>();
        secrets.put("1. Environment variable secret", envSecret);
        secrets.put("2. Volume mounted file secret", readFile("/etc/volume-secret/secret-file.txt"));
        secrets.put("3a. ConfigTree secret (file 1)", configTreeFile1);
        secrets.put("3b. ConfigTree secret (file 2)", configTreeFile2);
        secrets.put("4. From Spring K8s API secret ", apiSecret);
        secrets.put("4b. From Spring K8s API extra secret ", extraApiSecret);
        secrets.put("5. Hashicorp Vault secret (sidecar)", readFile("/etc/vault-secrets/vault-secret.txt"));

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
