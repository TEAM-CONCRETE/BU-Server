package com.concrete.buildup.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 근로자 회원가입 1단계 응답 DTO (회원가입 완료)
 *
 * <p>회원가입이 완료되었으며, 추가로 상세 정보를 입력할 수 있습니다.</p>
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "근로자 회원가입 1단계 응답 (회원가입 완료)")
public class SignUpPhase1Response {

    @Schema(description = "사용자 정보")
    private UserInfo user;

    @Schema(description = "근로자 기본 정보")
    private ProfileInfo profile;

    @Schema(description = "추가 정보 입력을 위한 프로필 토큰 (UUID)", example = "550e8400-e29b-41d4-a716-446655440000")
    private String profileToken;

    @Schema(description = "프로필 토큰 만료 시간", example = "2025-11-06T15:30:00")
    private LocalDateTime profileTokenExpiresAt;

    @Schema(description = "다음 단계 안내 메시지", example = "회원가입이 완료되었습니다. 추가 정보를 입력하시면 더 많은 기능을 이용할 수 있습니다.")
    private String nextStepMessage;

    /**
     * 사용자 정보
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "사용자 정보")
    public static class UserInfo {
        @Schema(description = "사용자 ID (PK)", example = "1")
        private Long id;

        @Schema(description = "로그인 ID", example = "testuser001")
        private String userId;

        @Schema(description = "역할", example = "EMPLOYEE")
        private String role;
    }

    /**
     * 근로자 기본 정보
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "근로자 기본 정보")
    public static class ProfileInfo {
        @Schema(description = "근로자 ID (PK)", example = "1")
        private Long employeeId;

        @Schema(description = "근로자 이름", example = "홍길동")
        private String empName;
    }

    /**
     * User와 Employee로부터 응답 생성
     *
     * @param userId 사용자 ID
     * @param userPk 사용자 PK
     * @param roleName 역할명
     * @param employeeId 근로자 ID
     * @param empName 근로자 이름
     * @param profileToken 프로필 토큰
     * @param profileTokenExpiresAt 토큰 만료 시간
     * @return SignUpPhase1Response
     */
    public static SignUpPhase1Response of(
            Long userPk,
            String userId,
            String roleName,
            Long employeeId,
            String empName,
            String profileToken,
            LocalDateTime profileTokenExpiresAt
    ) {
        UserInfo userInfo = UserInfo.builder()
                .id(userPk)
                .userId(userId)
                .role(roleName)
                .build();

        ProfileInfo profileInfo = ProfileInfo.builder()
                .employeeId(employeeId)
                .empName(empName)
                .build();

        return SignUpPhase1Response.builder()
                .user(userInfo)
                .profile(profileInfo)
                .profileToken(profileToken)
                .profileTokenExpiresAt(profileTokenExpiresAt)
                .nextStepMessage("회원가입이 완료되었습니다. 추가 정보를 입력하시면 더 많은 기능을 이용할 수 있습니다.")
                .build();
    }
}
