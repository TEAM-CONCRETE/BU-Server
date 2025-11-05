package com.concrete.buildup.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원가입 응답 DTO
 *
 * <p>회원가입 완료 후 반환되는 정보입니다.</p>
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "회원가입 응답")
public class SignUpResponse {

    @Schema(description = "사용자 정보")
    private UserInfo user;

    @Schema(description = "프로필 정보")
    private ProfileInfo profile;

    @Schema(description = "연락처 검증 필요 여부")
    private VerificationInfo verificationRequired;

    @Schema(description = "시크릿키 처리 결과")
    private LinkingInfo linking;

    /**
     * 사용자 기본 정보
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "사용자 기본 정보")
    public static class UserInfo {
        @Schema(description = "사용자 ID (PK)", example = "1301")
        private Long id;

        @Schema(description = "로그인 ID", example = "worker001")
        private String userId;

        @Schema(description = "역할", example = "EMPLOYEE")
        private String role;

        @Schema(description = "휴대폰 번호", example = "01012345678")
        private String phone;

        @Schema(description = "이메일", example = "me@ex.com")
        private String email;
    }

    /**
     * 근로자 프로필 정보
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "근로자 프로필 정보")
    public static class ProfileInfo {
        @Schema(description = "근로자 ID (PK)", example = "5011")
        private Long employeeId;

        @Schema(description = "이름", example = "김철수")
        private String empName;

        @Schema(description = "주소", example = "서울 강남구 테헤란로 123")
        private String empAddress;

        @Schema(description = "비상 연락망", example = "01098765432")
        private String emergencyPhone;
    }

    /**
     * 연락처 검증 필요 여부
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "연락처 검증 필요 여부")
    public static class VerificationInfo {
        @Schema(description = "휴대폰 검증 필요", example = "true")
        private Boolean phone;

        @Schema(description = "이메일 검증 필요", example = "false")
        private Boolean email;
    }

    /**
     * 시크릿키 처리 결과
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "시크릿키 처리 결과")
    public static class LinkingInfo {
        @Schema(description = "시크릿키 사용 여부", example = "true")
        private Boolean secretKeyUsed;

        @Schema(description = "연동된 현장 정보")
        private SiteInfo siteLinked;
    }

    /**
     * 현장 정보
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "현장 정보")
    public static class SiteInfo {
        @Schema(description = "현장 ID", example = "77")
        private Long siteId;

        @Schema(description = "현장명", example = "이천 A현장")
        private String siteName;
    }
}
