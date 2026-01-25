package com.example.secretexample;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Secret;
import io.kubernetes.client.openapi.models.V1SecretList;
import io.kubernetes.client.util.Config;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

public class K8sSecretInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        try {
            ApiClient client = Config.defaultClient();
            Configuration.setDefaultApiClient(client);
            CoreV1Api api = new CoreV1Api();

            String namespace = "secret-example";
            // Use list as it worked in DebugController
            V1SecretList list = api.listNamespacedSecret(namespace, null, null, null, null, null, null, null, null, null, null, null);
            V1Secret appSecret = list.getItems().stream()
                    .filter(s -> "secret-app".equals(s.getMetadata().getName()))
                    .findFirst()
                    .orElse(null);

            if (appSecret != null && appSecret.getData() != null) {
                Map<String, Object> properties = new HashMap<>();
                appSecret.getData().forEach((k, v) -> properties.put(k, new String(v)));
                
                MapPropertySource source = new MapPropertySource("manual-k8s-secret-source", properties);
                applicationContext.getEnvironment().getPropertySources().addFirst(source);
                System.out.println("[K8sSecretInitializer] Successfully loaded secret-app from API");
            } else {
                System.out.println("[K8sSecretInitializer] secret-app not found in namespace " + namespace);
            }
        } catch (Exception e) {
            System.err.println("[K8sSecretInitializer] Failed to load secrets: " + e.getMessage());
        }
    }
}
