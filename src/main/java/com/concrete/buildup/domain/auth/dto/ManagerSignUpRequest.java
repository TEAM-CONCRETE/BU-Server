package com.concrete.buildup.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

/**
 * 현장 관리자 회원가입 요청 DTO
 *
 * <p>현장 관리자 계정 정보를 입력받습니다.</p>
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "현장 관리자 회원가입 요청")
public class ManagerSignUpRequest {

    @NotBlank(message = "이름은 필수입니다")
    @Size(min = 1, max = 50, message = "이름은 1자 이상 50자 이하여야 합니다")
    @Pattern(regexp = "^[가-힣a-zA-Z\\s]+$", message = "이름은 한글, 영문, 공백만 입력 가능합니다")
    @Schema(description = "관리자 이름", example = "김철수", required = true)
    private String managerName;

    @NotBlank(message = "로그인 ID는 필수입니다")
    @Size(min = 6, max = 20, message = "로그인 ID는 6자 이상 20자 이하여야 합니다")
    @Pattern(regexp = "^[a-z0-9]+$", message = "로그인 ID는 영문 소문자와 숫자만 입력 가능합니다")
    @Schema(description = "로그인 ID (영문 소문자, 숫자)", example = "manager001", required = true)
    private String userId;

    @NotBlank(message = "비밀번호는 필수입니다")
    @Size(min = 8, max = 20, message = "비밀번호는 8자 이상 20자 이하여야 합니다")
    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]+$",
            message = "비밀번호는 영문, 숫자, 특수문자(@$!%*#?&)를 포함해야 합니다"
    )
    @Schema(description = "비밀번호 (영문, 숫자, 특수문자 포함)", example = "Manager123!@", required = true)
    private String password;

    @NotBlank(message = "비밀번호 확인은 필수입니다")
    @Schema(description = "비밀번호 확인", example = "Manager123!@", required = true)
    private String confirmPassword;

    @NotBlank(message = "시크릿키는 필수입니다")
    @Size(max = 100, message = "시크릿키는 100자 이하여야 합니다")
    @Schema(description = "현장 관리자용 시크릿키", example = "MANAGER-SECRET-KEY-12345", required = true)
    private String secretKey;

    @NotBlank(message = "전화번호는 필수입니다")
    @Pattern(
            regexp = "^01([0|1|6|7|8|9])-?([0-9]{3,4})-?([0-9]{4})$",
            message = "전화번호 형식이 올바르지 않습니다 (예: 010-1234-5678)"
    )
    @Schema(description = "전화번호", example = "010-1234-5678", required = true)
    private String phone;

    /**
     * 비밀번호 일치 여부 확인
     *
     * @return true if passwords match, false otherwise
     */
    public boolean isPasswordMatching() {
        return password != null && password.equals(confirmPassword);
    }
}
