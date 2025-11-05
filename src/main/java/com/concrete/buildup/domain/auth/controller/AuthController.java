package com.concrete.buildup.domain.auth.controller;

import com.concrete.buildup.domain.auth.dto.*;
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
     * 근로자 회원가입 1단계 API (회원가입 완료)
     *
     * <p>이름, 아이디, 비밀번호, 시크릿키, 약관동의를 입력받아 회원가입을 완료합니다.</p>
     *
     * @param request 1단계 회원가입 요청 정보
     * @return SignUpPhase1Response - 회원가입 완료 정보 및 프로필 토큰
     */
    @Operation(
            summary = "근로자 회원가입 1단계 (회원가입 완료)",
            description = "이름, 아이디, 비밀번호, 시크릿키, 약관동의를 입력받아 회원가입을 완료합니다. " +
                    "프로필 토큰이 발급되며 30분간 유효합니다. 2단계에서 이 토큰을 사용하여 상세 정보를 입력할 수 있습니다."
    )
    @PostMapping("/register/employee/step1")
    public ResponseEntity<ApiResponse<SignUpPhase1Response>> registerEmployeeStep1(
            @Valid @RequestBody EmployeeSignUpPhase1Request request
    ) {
        log.info("근로자 회원가입 1단계 API 호출: userId={}, empName={}", request.getUserId(), request.getEmpName());

        SignUpPhase1Response response = authService.registerEmployeePhase1(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "회원가입이 완료되었습니다"));
    }

    /**
     * 근로자 회원가입 2단계 API (상세 정보 입력)
     *
     * <p>1단계에서 발급받은 프로필 토큰과 함께 상세 정보를 입력합니다.</p>
     *
     * @param request 2단계 회원가입 요청 정보
     * @return SignUpResponse - 업데이트된 사용자 및 프로필 정보
     */
    @Operation(
            summary = "근로자 회원가입 2단계 (상세 정보 입력)",
            description = "1단계에서 발급받은 프로필 토큰과 함께 주민등록번호, 연락처, 주소 등 상세 정보를 입력합니다. " +
                    "근로자 정보가 업데이트됩니다."
    )
    @PostMapping("/register/employee/step2")
    public ResponseEntity<ApiResponse<SignUpResponse>> registerEmployeeStep2(
            @Valid @RequestBody EmployeeSignUpPhase2Request request
    ) {
        log.info("근로자 회원가입 2단계 API 호출: token={}", request.getRegistrationToken());

        SignUpResponse response = authService.registerEmployeePhase2(request);

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(response, "근로자 정보가 업데이트되었습니다"));
    }

    /**
     * 근로자 회원가입 API (단일 단계, 레거시)
     *
     * <p>근로자(Employee) 계정을 생성합니다. users 테이블과 employees 테이블에 동시에 데이터가 저장됩니다.</p>
     * <p>⚠️ 이 API는 레거시입니다. 신규 구현은 /step1, /step2를 사용해주세요.</p>
     *
     * @param request 근로자 회원가입 요청 정보
     * @return SignUpResponse - 생성된 사용자 및 프로필 정보
     */
    @Operation(
            summary = "근로자 회원가입 (레거시)",
            description = "근로자 계정을 생성합니다. users와 employees 테이블에 동시 저장되며, " +
                    "EMPLOYEE 역할이 자동으로 할당됩니다. ⚠️ 레거시 API - 신규 구현은 step1, step2 사용 권장"
    )
    @PostMapping("/register/employee")
    public ResponseEntity<ApiResponse<SignUpResponse>> registerEmployee(
            @Valid @RequestBody EmployeeSignUpRequest request
    ) {
        log.info("근로자 회원가입 API 호출 (레거시): userId={}", request.getUserId());

        SignUpResponse response = authService.registerEmployee(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "회원가입이 완료되었습니다"));
    }

    /**
     * 현장 관리자 회원가입 API
     *
     * <p>현장 관리자(Manager) 계정을 생성합니다. users 테이블과 managers 테이블에 동시에 데이터가 저장됩니다.</p>
     * <p>시크릿키를 통해 현장과 자동으로 연동됩니다.</p>
     *
     * @param request 현장 관리자 회원가입 요청 정보
     * @return SignUpResponse - 생성된 사용자 및 관리자 정보
     */
    @Operation(
            summary = "현장 관리자 회원가입",
            description = "현장 관리자 계정을 생성합니다. users와 managers 테이블에 동시 저장되며, " +
                    "MANAGER 역할이 자동으로 할당됩니다. 시크릿키를 통해 현장과 연동됩니다."
    )
    @PostMapping("/register/manager")
    public ResponseEntity<ApiResponse<SignUpResponse>> registerManager(
            @Valid @RequestBody ManagerSignUpRequest request
    ) {
        log.info("현장 관리자 회원가입 API 호출: userId={}, managerName={}", request.getUserId(), request.getManagerName());

        SignUpResponse response = authService.registerManager(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "현장 관리자 회원가입이 완료되었습니다"));
    }
}
