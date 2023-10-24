package io.nuvalence.ds4g.documentmanagement.service.config;

import com.google.cloud.NoCredentials;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.BucketInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.servers.Server;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/** Spring configuration. **/
@OpenAPIDefinition(servers = {@Server(url = "/")})
@EnableWebMvc
@org.springframework.context.annotation.Configuration
@Slf4j
public class Configuration implements WebMvcConfigurer {
    @Value("${management.endpoints.web.cors.allowed-origins}")
    private List<String> allowedOrigins;

    @Value("${management.endpoints.web.cors.allowed-methods}")
    private List<String> allowedMethods;

    @Value("${management.endpoints.web.cors.allowed-headers}")
    private List<String> allowedHeaders;

    @Value("${management.endpoints.web.cors.allow-credentials}")
    private boolean allowCredentials;

    /**
     * CORS configuration.
     * 
     * @param registry URL mappings and their CORS configurations.
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins.toArray(String[]::new))
                .allowedHeaders(allowedHeaders.toArray(String[]::new))
                .allowedMethods(allowedMethods.toArray(String[]::new))
                .allowCredentials(allowCredentials);
    }

    @Bean
    public Storage getGoogleCloudStorage() {
        return StorageOptions.getDefaultInstance().getService();
    }

    /**
     * This bean is used to configure the Google Cloud Storage emulator. It is only
     * used when the
     * emulator is enabled through the application property at
     * document-management.gcloud-storage-
     * emulator.enabled=true.
     * 
     * @param unscannedBucket  bucket for unscanned files
     * @param quarantineBucket bucket for quarantined files
     * @param scannedBucket    bucket for scanned files
     * @param emulatorHost     host of the emulator
     * 
     * @return the Google Cloud Storage emulator Class
     */
    @Primary
    @ConditionalOnProperty(
            name = "document-management.gcloud-storage-emulator.enabled",
            havingValue = "true")
    @Bean
    public Storage getGoogleCloudStorageEmulator(
            @Value("${document-management.google.bucket.unscanned-files}") String unscannedBucket,
            @Value("${document-management.google.bucket.quarantined-files}")
                    String quarantineBucket,
            @Value("${document-management.google.bucket.scanned-files}") String scannedBucket,
            @Value("${document-management.gcloud-storage-emulator.host}") String emulatorHost) {

        log.warn("Using emulated Google Cloud Storage.");

        // Create a storage service that uses the emulator
        Storage storage =
                StorageOptions.newBuilder()
                        .setHost("http://" + emulatorHost)
                        .setCredentials(NoCredentials.getInstance())
                        .build()
                        .getService();

        // Create buckets if they don't exist
        Bucket bucket = storage.get(unscannedBucket);
        if (bucket == null) {
            storage.create(BucketInfo.newBuilder(unscannedBucket).build());
        }
        bucket = storage.get(scannedBucket);
        if (bucket == null) {
            storage.create(BucketInfo.newBuilder(scannedBucket).build());
        }
        bucket = storage.get(quarantineBucket);
        if (bucket == null) {
            storage.create(BucketInfo.newBuilder(quarantineBucket).build());
        }

        return storage;
    }
}
