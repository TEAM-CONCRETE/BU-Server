package com.concrete.buildup.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 근로자 회원가입 1단계 응답 DTO
 *
 * <p>1단계 완료 후 2단계 진행을 위한 등록 토큰을 반환합니다.</p>
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "근로자 회원가입 1단계 응답")
public class SignUpPhase1Response {

    @Schema(description = "2단계 진행을 위한 등록 토큰 (UUID)", example = "550e8400-e29b-41d4-a716-446655440000")
    private String registrationToken;

    @Schema(description = "등록 토큰 만료 시간", example = "2025-11-06T15:30:00")
    private LocalDateTime expiresAt;

    @Schema(description = "등록된 사용자 ID", example = "testuser001")
    private String userId;

    @Schema(description = "등록된 근로자 이름", example = "홍길동")
    private String empName;

    @Schema(description = "다음 단계 안내 메시지", example = "2단계에서 상세 정보를 입력해주세요. 토큰은 30분간 유효합니다.")
    private String nextStepMessage;

    /**
     * TemporaryRegistration 엔티티로부터 응답 생성
     *
     * @param registrationToken 등록 토큰
     * @param expiresAt 만료 시간
     * @param userId 사용자 ID
     * @param empName 근로자 이름
     * @return SignUpPhase1Response
     */
    public static SignUpPhase1Response of(String registrationToken, LocalDateTime expiresAt, String userId, String empName) {
        return SignUpPhase1Response.builder()
                .registrationToken(registrationToken)
                .expiresAt(expiresAt)
                .userId(userId)
                .empName(empName)
                .nextStepMessage("2단계에서 상세 정보를 입력해주세요. 토큰은 30분간 유효합니다.")
                .build();
    }
}
