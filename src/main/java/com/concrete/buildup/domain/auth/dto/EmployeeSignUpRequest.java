package com.concrete.buildup.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 근로자 회원가입 요청 DTO
 *
 * <p>근로자 회원가입 시 필요한 정보를 담는 DTO입니다.</p>
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "근로자 회원가입 요청")
public class EmployeeSignUpRequest {

    @Schema(description = "로그인 ID (소문자/숫자 6~20자)", example = "worker001", required = true)
    @NotBlank(message = "로그인 ID는 필수입니다")
    @Size(min = 6, max = 20, message = "로그인 ID는 6~20자 사이여야 합니다")
    @Pattern(regexp = "^[a-z0-9]+$", message = "로그인 ID는 소문자와 숫자만 사용할 수 있습니다")
    private String userId;

    @Schema(description = "비밀번호 (8~20자, 영문/숫자/특수문자 포함, 공백 불가)", example = "Abc123!!", required = true)
    @NotBlank(message = "비밀번호는 필수입니다")
    @Size(min = 8, max = 20, message = "비밀번호는 8~20자 사이여야 합니다")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]+$",
            message = "비밀번호는 영문, 숫자, 특수문자를 포함해야 하며 공백은 사용할 수 없습니다")
    private String password;

    @Schema(description = "비밀번호 확인", example = "Abc123!!", required = true)
    @NotBlank(message = "비밀번호 확인은 필수입니다")
    private String confirmPassword;

    @Schema(description = "이름 (한글/영문 1~50자)", example = "김철수", required = true)
    @NotBlank(message = "이름은 필수입니다")
    @Size(min = 1, max = 50, message = "이름은 1~50자 사이여야 합니다")
    @Pattern(regexp = "^[가-힣a-zA-Z\\s]+$", message = "이름은 한글 또는 영문만 사용할 수 있습니다")
    private String empName;

    @Schema(description = "주민등록번호 (######-#######)", example = "900101-1******", required = true)
    @NotBlank(message = "주민등록번호는 필수입니다")
    @Pattern(regexp = "^\\d{6}-[1-4*]\\d{6}$", message = "주민등록번호 형식이 올바르지 않습니다")
    private String residentNum;

    @Schema(description = "휴대폰 번호 (010으로 시작하는 11자리)", example = "01012345678", required = true)
    @NotBlank(message = "휴대폰 번호는 필수입니다")
    @Pattern(regexp = "^010\\d{8}$", message = "휴대폰 번호는 010으로 시작하는 11자리 숫자여야 합니다")
    private String phone;

    @Schema(description = "이메일", example = "me@ex.com")
    @Email(message = "이메일 형식이 올바르지 않습니다")
    private String email;

    @Schema(description = "주소", example = "서울 강남구 테헤란로 123")
    @Size(max = 255, message = "주소는 255자를 초과할 수 없습니다")
    private String empAddress;

    @Schema(description = "비상 연락망 (11자리 숫자)", example = "01098765432")
    @Pattern(regexp = "^\\d{11}$", message = "비상 연락망은 11자리 숫자여야 합니다")
    private String emergencyPhone;

    @Schema(description = "현장 인증용 시크릿키 (UUID)", example = "aaaaaaaa-aaaa-4aaa-aaaa-aaaaaaaaaaaa")
    private String secretKey;

    @Schema(description = "약관 전체 동의", example = "true", required = true)
    @NotNull(message = "약관 동의는 필수입니다")
    @AssertTrue(message = "약관에 동의해야 합니다")
    private Boolean agreeTerms;

    @Schema(description = "개인정보 수집 동의", example = "true", required = true)
    @NotNull(message = "개인정보 수집 동의는 필수입니다")
    @AssertTrue(message = "개인정보 수집에 동의해야 합니다")
    private Boolean agreePrivacy;

    /**
     * 비밀번호 일치 여부 확인
     *
     * @return 비밀번호가 일치하면 true
     */
    public boolean isPasswordMatching() {
        return password != null && password.equals(confirmPassword);
    }
}
