package com.concrete.buildup.domain.auth.controller;

import com.concrete.buildup.domain.auth.dto.EmployeeSignUpRequest;
import com.concrete.buildup.domain.auth.dto.SignUpResponse;
import com.concrete.buildup.domain.auth.dto.UserExistsResponse;
import com.concrete.buildup.domain.auth.service.AuthService;
import com.concrete.buildup.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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
@RequestMapping("/auth")
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

    /**
     * 근로자 회원가입 API
     *
     * <p>근로자(Employee) 계정을 생성합니다. users 테이블과 employees 테이블에 동시에 데이터가 저장됩니다.</p>
     *
     * @param request 근로자 회원가입 요청 정보
     * @return SignUpResponse - 생성된 사용자 및 프로필 정보
     */
    @Operation(
            summary = "근로자 회원가입",
            description = "근로자 계정을 생성합니다. users와 employees 테이블에 동시 저장되며, " +
                    "EMPLOYEE 역할이 자동으로 할당됩니다."
    )
    @PostMapping("/register/employee")
    public ResponseEntity<ApiResponse<SignUpResponse>> registerEmployee(
            @Valid @RequestBody EmployeeSignUpRequest request
    ) {
        log.info("근로자 회원가입 API 호출: userId={}", request.getUserId());

        SignUpResponse response = authService.registerEmployee(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "회원가입이 완료되었습니다"));
    }
}
