package com.concrete.buildup.domain.auth.controller;

import com.concrete.buildup.domain.auth.dto.UserExistsResponse;
import com.concrete.buildup.domain.auth.service.AuthService;
import com.concrete.buildup.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증/인가 컨트롤러
 *
 * <p>회원가입, 로그인, 아이디 중복 확인 등 인증 관련 API를 제공합니다.</p>
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Validated
@Tag(name = "Auth", description = "인증/인가 API")
public class AuthController {

    private final AuthService authService;

    /**
     * 아이디 중복 확인 API
     *
     * <p>회원가입 시 사용자 ID가 이미 사용 중인지 확인합니다.</p>
     *
     * @param userId 확인할 사용자 ID
     * @return UserExistsResponse - exists: true(사용 중), false(사용 가능)
     */
    @Operation(
            summary = "아이디 중복 확인",
            description = "회원가입 시 사용자 ID가 이미 존재하는지 확인합니다. " +
                    "exists가 true면 이미 사용 중인 아이디이고, false면 사용 가능한 아이디입니다."
    )
    @GetMapping("/exists")
    public ResponseEntity<ApiResponse<UserExistsResponse>> checkUserIdExists(
            @Parameter(description = "확인할 사용자 ID", required = true, example = "testuser123")
            @RequestParam @NotBlank(message = "사용자 ID는 필수입니다") String userId
    ) {
        log.info("아이디 중복 확인 API 호출: userId={}", userId);

        UserExistsResponse response = authService.checkUserIdExists(userId);

        return ResponseEntity.ok(
                ApiResponse.success(response, "아이디 중복 확인이 완료되었습니다")
        );
    }
}
