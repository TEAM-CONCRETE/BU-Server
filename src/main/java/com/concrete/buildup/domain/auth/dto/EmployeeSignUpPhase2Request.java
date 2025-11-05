package com.concrete.buildup.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

/**
 * 근로자 회원가입 2단계 요청 DTO
 *
 * <p>상세 개인정보 및 동의 사항을 입력받습니다.</p>
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "근로자 회원가입 2단계 요청")
public class EmployeeSignUpPhase2Request {

    @NotBlank(message = "등록 토큰은 필수입니다")
    @Schema(description = "1단계 완료 시 발급받은 등록 토큰", example = "550e8400-e29b-41d4-a716-446655440000", required = true)
    private String registrationToken;

    @NotBlank(message = "주민등록번호는 필수입니다")
    @Pattern(regexp = "^\\d{6}-[1-4]\\d{6}$", message = "주민등록번호 형식이 올바르지 않습니다 (예: 990101-1234567)")
    @Schema(description = "주민등록번호 (암호화되어 저장됨)", example = "990101-1234567", required = true)
    private String residentNum;

    @NotBlank(message = "휴대폰 번호는 필수입니다")
    @Pattern(regexp = "^010\\d{8}$", message = "휴대폰 번호 형식이 올바르지 않습니다 (예: 01012345678)")
    @Schema(description = "휴대폰 번호 (숫자만, 하이픈 없이)", example = "01012345678", required = true)
    private String phone;

    @Email(message = "이메일 형식이 올바르지 않습니다")
    @Schema(description = "이메일 주소 (선택)", example = "test@example.com", required = false)
    private String email;

    @Size(max = 255, message = "주소는 255자 이하여야 합니다")
    @Schema(description = "주소 (선택)", example = "서울시 강남구 테헤란로 123", required = false)
    private String empAddress;

    @Pattern(regexp = "^\\d{11}$", message = "비상연락망 형식이 올바르지 않습니다 (예: 01087654321)")
    @Schema(description = "비상연락망 (숫자만, 하이픈 없이, 선택)", example = "01087654321", required = false)
    private String emergencyPhone;

    @NotNull(message = "서비스 이용약관 동의는 필수입니다")
    @AssertTrue(message = "서비스 이용약관에 동의해야 합니다")
    @Schema(description = "서비스 이용약관 동의", example = "true", required = true)
    private Boolean agreeTerms;

    @NotNull(message = "개인정보 처리방침 동의는 필수입니다")
    @AssertTrue(message = "개인정보 처리방침에 동의해야 합니다")
    @Schema(description = "개인정보 처리방침 동의", example = "true", required = true)
    private Boolean agreePrivacy;
}
