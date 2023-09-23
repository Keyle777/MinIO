package fun.keyle.MinIO.config;

import io.minio.MinioClient;
import io.swagger.v3.oas.annotations.media.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Schema(description = "minio配置")
public class MinIoClientConfig {
	@Value("${oss.endpoint}")
    private String url;
    @Value("${oss.access-key}")
    private String accessKey;
    @Value("${oss.secret-key}")
    private String secretKey;

    private static final Logger logger = LoggerFactory.getLogger(MinIoClientConfig.class);
    @Bean
    public MinioClient minioClient() {
        logger.info("Initializing MinioClient...");
        logger.info("URL: {}", url);
        logger.info("Access Key: {}", accessKey);
        logger.info("Secret Key: {}", secretKey);

        try {
            MinioClient minioClient = MinioClient.builder()
                    .endpoint(url)
                    .credentials(accessKey, secretKey)
                    .build();
            logger.info("MinioClient initialized successfully.");
            return minioClient;
        } catch (Exception e) {
            logger.error("Failed to initialize MinioClient. Reason: {}", e.getMessage());
            throw e;
        }
    }
 }
