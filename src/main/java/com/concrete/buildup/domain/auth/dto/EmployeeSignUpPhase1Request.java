package com.concrete.buildup.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * 근로자 회원가입 1단계 요청 DTO
 *
 * <p>기본 계정 정보를 입력받습니다.</p>
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "근로자 회원가입 1단계 요청")
public class EmployeeSignUpPhase1Request {

    @NotBlank(message = "이름은 필수입니다")
    @Size(min = 1, max = 50, message = "이름은 1자 이상 50자 이하여야 합니다")
    @Pattern(regexp = "^[가-힣a-zA-Z\\s]+$", message = "이름은 한글, 영문, 공백만 입력 가능합니다")
    @Schema(description = "근로자 이름", example = "홍길동", required = true)
    private String empName;

    @NotBlank(message = "로그인 ID는 필수입니다")
    @Size(min = 6, max = 20, message = "로그인 ID는 6자 이상 20자 이하여야 합니다")
    @Pattern(regexp = "^[a-z0-9]+$", message = "로그인 ID는 영문 소문자와 숫자만 입력 가능합니다")
    @Schema(description = "로그인 ID (영문 소문자, 숫자)", example = "testuser001", required = true)
    private String userId;

    @NotBlank(message = "비밀번호는 필수입니다")
    @Size(min = 8, max = 20, message = "비밀번호는 8자 이상 20자 이하여야 합니다")
    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]+$",
            message = "비밀번호는 영문, 숫자, 특수문자(@$!%*#?&)를 포함해야 합니다"
    )
    @Schema(description = "비밀번호 (영문, 숫자, 특수문자 포함)", example = "Test123@@", required = true)
    private String password;

    @NotBlank(message = "비밀번호 확인은 필수입니다")
    @Schema(description = "비밀번호 확인", example = "Test123@@", required = true)
    private String confirmPassword;

    @Size(max = 100, message = "시크릿키는 100자 이하여야 합니다")
    @Schema(description = "현장 연동용 시크릿키 (선택)", example = "SECRET-KEY-12345", required = false)
    private String secretKey;

    /**
     * 비밀번호 일치 여부 확인
     *
     * @return true if passwords match, false otherwise
     */
    public boolean isPasswordMatching() {
        return password != null && password.equals(confirmPassword);
    }
}
