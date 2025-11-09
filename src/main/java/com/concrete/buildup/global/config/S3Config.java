package com.concrete.buildup.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * AWS S3 설정 클래스
 * S3 클라이언트 및 Presigned URL 생성기를 Bean으로 등록합니다.
 *
 * <p>자격 증명 방식:</p>
 * <ul>
 *   <li>명시적 키 제공 시: StaticCredentialsProvider 사용</li>
 *   <li>키 미제공 시: DefaultCredentialsProvider 사용 (IAM Role 등 자동 탐색)</li>
 * </ul>
 */
@Configuration
public class S3Config {

    @Value("${spring.cloud.aws.credentials.access-key:}")
    private String accessKey;

    @Value("${spring.cloud.aws.credentials.secret-key:}")
    private String secretKey;

    @Value("${spring.cloud.aws.region.static}")
    private String region;

    /**
     * AWS 자격 증명 프로바이더 생성
     * 명시적 키가 제공되면 StaticCredentialsProvider 사용,
     * 그렇지 않으면 DefaultCredentialsProvider 사용 (IAM Role, 환경 변수 등 자동 탐색)
     */
    private AwsCredentialsProvider credentialsProvider() {
        if (accessKey != null && !accessKey.isEmpty() && secretKey != null && !secretKey.isEmpty()) {
            // 명시적 키 제공 시
            return StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKey, secretKey)
            );
        } else {
            // IAM Role, 환경 변수, EC2 인스턴스 메타데이터 등에서 자동 탐색
            return DefaultCredentialsProvider.create();
        }
    }

    /**
     * S3Client Bean
     * S3와의 기본적인 통신을 위한 클라이언트
     */
    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(credentialsProvider())
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
                .credentialsProvider(credentialsProvider())
                .build();
    }
}