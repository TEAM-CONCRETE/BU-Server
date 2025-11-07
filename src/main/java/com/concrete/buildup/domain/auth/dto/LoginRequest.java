package com.concrete.buildup.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * 로그인 요청 DTO
 *
 * <p>사용자 로그인 정보를 담는 DTO입니다.</p>
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "로그인 요청")
public class LoginRequest {

    @NotBlank(message = "아이디는 필수입니다")
    @Schema(description = "로그인 ID", example = "testuser001", required = true)
    private String username;

    @NotBlank(message = "비밀번호는 필수입니다")
    @Schema(description = "비밀번호", example = "Test123@@", required = true)
    private String password;

    @Builder.Default
    @Schema(description = "자동 로그인 여부 (true: 30일, false: 7일)", example = "false", required = false)
    private Boolean rememberMe = false;
}
