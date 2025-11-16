package com.concrete.buildup.domain.attendance.service;

import com.concrete.buildup.domain.auth.entity.Employee;
import com.concrete.buildup.domain.auth.entity.User;
import com.concrete.buildup.domain.auth.repository.EmployeeRepository;
import com.concrete.buildup.domain.auth.repository.UserRepository;
import com.concrete.buildup.global.exception.BusinessException;
import com.concrete.buildup.global.exception.errorcode.AuthErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사원 얼굴 이미지 등록 서비스
 * Presigned URL 방식을 사용하여 클라이언트가 직접 S3에 업로드한 이미지를 등록합니다.
 *
 * 프로세스:
 * 1. 클라이언트: POST /v1/uploads/presign (resourceType=EMPLOYEE_PROFILE) → Presigned URL 발급
 * 2. 클라이언트: PUT {uploadUrl} → S3 직접 업로드
 * 3. 클라이언트: POST /api/attendance/my-face (uploadId 전달)
 * 4. 백엔드: uploadId로 S3 URL 구성 후 DB 저장
 *
 * 얼굴 검출 검증:
 * - 프론트엔드에서 face-api.js를 사용하여 사전 검증
 * - 백엔드에서는 별도의 얼굴 검출 검증 없이 URL만 저장
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class EmployeeFaceService {

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucketName;

    /**
     * 사원 얼굴 이미지 등록 (uploadId 방식)
     *
     * @param userId 로그인 ID (JWT에서 추출한 User.user_id)
     * @param uploadId S3 객체 키 (예: uploads/employee-profiles/123/profile.jpg)
     * @return 등록된 이미지 S3 URL
     * @throws RuntimeException User 또는 Employee가 존재하지 않는 경우
     */
    public String registerEmployeeFaceImage(String userId, String uploadId) {
        log.info("사원 얼굴 이미지 등록 시작 - userId: {}, uploadId: {}", userId, uploadId);

        // 1. User 조회 (로그인 ID로)
        User user = userRepository.findByUserId(userId)
            .orElseThrow(() -> {
                log.error("User를 찾을 수 없음 - userId: {}", userId);
                return new BusinessException(AuthErrorCode.USER_NOT_FOUND);
            });

        // 2. Employee 조회 (User의 PK로)
        Employee employee = employeeRepository.findByUserId(user.getId())
            .orElseThrow(() -> {
                log.error("Employee를 찾을 수 없음 - userPk: {}", user.getId());
                return new BusinessException(AuthErrorCode.USER_NOT_FOUND);
            });

        // 3. uploadId로 S3 URL 구성
        String s3Url = buildS3Url(uploadId);
        log.info("S3 URL 구성 완료: {}", maskUrl(s3Url));

        // 4. 기존 이미지가 있으면 로그 남기기 (S3 삭제는 선택사항)
        if (employee.getProfileImageUrl() != null && !employee.getProfileImageUrl().isEmpty()) {
            log.info("기존 얼굴 이미지 존재 - userId: {}, oldUrl: {}",
                     userId, maskUrl(employee.getProfileImageUrl()));
            // 필요 시 S3 삭제 로직 추가 가능
        }

        // 5. Employee 엔티티 업데이트
        employee.updateProfileImageUrl(s3Url);
        employeeRepository.save(employee);

        log.info("사원 얼굴 이미지 등록 완료 - userId: {}, s3Url: {}", userId, maskUrl(s3Url));
        return s3Url;
    }

    /**
     * uploadId(s3Key)로 S3 URL 구성
     *
     * @param uploadId S3 객체 키
     * @return S3 URL (https://{bucket}.s3.amazonaws.com/{uploadId})
     */
    private String buildS3Url(String uploadId) {
        return String.format("https://%s.s3.amazonaws.com/%s", bucketName, uploadId);
    }

    /**
     * URL 마스킹 (개인정보 보호)
     * 로그에 전체 URL 노출을 방지합니다.
     *
     * @param url S3 URL
     * @return 마스킹된 URL (앞 30자만 표시)
     */
    private String maskUrl(String url) {
        if (url == null || url.length() < 30) {
            return url;
        }
        return url.substring(0, 30) + "***";
    }
}
