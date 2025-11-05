package com.concrete.buildup.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * AWS S3 설정 클래스
 * S3 클라이언트 및 Presigned URL 생성기를 Bean으로 등록합니다.
 *
 * spring.cloud.aws.s3.enabled가 true로 설정되어 있을 때만 활성화됩니다.
 * 개발 환경에서 S3 자격 증명 없이도 애플리케이션을 실행할 수 있습니다.
 */
@Configuration
@ConditionalOnProperty(
        name = "spring.cloud.aws.s3.enabled",
        havingValue = "true",
        matchIfMissing = false
)
public class S3Config {

    @Value("${spring.cloud.aws.credentials.access-key}")
    private String accessKey;

    @Value("${spring.cloud.aws.credentials.secret-key}")
    private String secretKey;

    @Value("${spring.cloud.aws.region.static}")
    private String region;

    /**
     * S3Client Bean
     * S3와의 기본적인 통신을 위한 클라이언트
     */
    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)
                ))
                .build();
    }

    /**
     * S3Presigner Bean
     * Presigned URL 생성을 위한 클라이언트
     * 클라이언트가 직접 S3에 업로드할 수 있는 임시 URL을 발급합니다.
     */
    @Bean
    public S3Presigner s3Presigner() {
        return S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)
                ))
                .build();
    }
}