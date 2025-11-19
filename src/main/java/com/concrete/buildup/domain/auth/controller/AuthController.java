package com.concrete.buildup.domain.auth.controller;

import com.concrete.buildup.domain.auth.dto.*;
import com.concrete.buildup.domain.auth.service.AuthService;
import com.concrete.buildup.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
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
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
@Validated
@Tag(name = "Auth", description = "인증/인가 API")
public class AuthController {

    private final AuthService authService;
    private final Environment environment;

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

    /**
     * 로그인 API
     *
     * <p>근로자, 현장 관리자, 기업 관리자 공통 로그인 API입니다.</p>
     * <p>Access Token과 Refresh Token은 모두 HttpOnly 쿠키로 전달됩니다.</p>
     *
     * @param request 로그인 요청 정보
     * @param response HTTP 응답 (쿠키 설정용)
     * @return LoginResponse - 사용자 정보 (토큰은 쿠키로 전달)
     */
    @Operation(
            summary = "로그인",
            description = "근로자, 현장 관리자, 기업 관리자 공통 로그인 API입니다. " +
                    "Access Token과 Refresh Token은 모두 HttpOnly 쿠키로 전달됩니다."
    )
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response
    ) {
        log.info("로그인 API 호출: username={}", request.getUsername());

        // 로그인 처리
        LoginResult loginResult = authService.login(request);

        // 개발 환경 확인
        boolean isProduction = !environment.acceptsProfiles(org.springframework.core.env.Profiles.of("dev", "local"));

        // Access Token을 HttpOnly 쿠키로 설정 (XSS 방어)
        org.springframework.http.ResponseCookie accessTokenCookie = org.springframework.http.ResponseCookie.from("accessToken", loginResult.getLoginResponse().getAccessToken())
                .httpOnly(true)          // XSS 공격 방어
                .secure(isProduction)    // 운영: HTTPS 필수, 개발: HTTP 허용
                .path("/")               // 모든 경로에서 접근 가능
                .maxAge(loginResult.getLoginResponse().getExpiresIn())  // Access Token 만료 시간
                .sameSite(isProduction ? "Strict" : "Lax")  // 개발: Lax, 운영: Strict
                .build();
        response.addHeader("Set-Cookie", accessTokenCookie.toString());

        // Refresh Token을 HttpOnly 쿠키로 설정
        org.springframework.http.ResponseCookie refreshTokenCookie = org.springframework.http.ResponseCookie.from("refreshToken", loginResult.getRefreshToken())
                .httpOnly(true)          // XSS 공격 방어
                .secure(isProduction)    // 운영: HTTPS 필수, 개발: HTTP 허용
                .path("/")               // 모든 경로에서 접근 가능
                .maxAge(loginResult.getRefreshTokenMaxAge())  // 만료 시간 설정
                .sameSite(isProduction ? "Strict" : "Lax")  // 개발: Lax, 운영: Strict
                .build();
        response.addHeader("Set-Cookie", refreshTokenCookie.toString());

        log.debug("Access Token 및 Refresh Token 쿠키 설정 완료");

        return ResponseEntity.ok(
                ApiResponse.success(loginResult.getLoginResponse(), "로그인 성공")
        );
    }

    /**
     * 내 정보 조회 API
     *
     * <p>인증된 사용자의 기본 정보 및 역할별 추가 정보를 조회합니다.</p>
     * <p>JWT Access Token을 통해 사용자를 식별합니다.</p>
     *
     * @return UserInfoResponse - 사용자 정보 및 역할별 추가 정보
     */
    @Operation(
            summary = "내 정보 조회",
            description = "인증된 사용자의 기본 정보 및 역할별 추가 정보를 조회합니다. " +
                    "JWT Access Token이 필요하며, SecurityContext에서 사용자 ID를 추출합니다."
    )
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserInfoResponse>> getMyInfo() {
        // SecurityContext에서 인증된 사용자 ID 추출
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("내 정보 조회 API 호출: userId={}", userId);

        UserInfoResponse response = authService.getMyInfo(userId);

        return ResponseEntity.ok(
                ApiResponse.success(response, "사용자 정보 조회 성공")
        );
    }

    /**
     * 토큰 재발급 API
     *
     * <p>Refresh Token을 사용하여 새로운 Access Token과 Refresh Token을 재발급합니다.</p>
     * <p>Refresh Token은 HttpOnly 쿠키에서 자동으로 읽어옵니다.</p>
     * <p>새로운 Refresh Token은 HttpOnly 쿠키로 전달됩니다 (Refresh Token Rotation).</p>
     *
     * @param request HTTP 요청 (쿠키에서 Refresh Token 읽기)
     * @param response HTTP 응답 (새로운 Refresh Token 쿠키 설정)
     * @return TokenRefreshResponse - 새로운 Access Token
     */
    @Operation(
            summary = "토큰 재발급",
            description = "Refresh Token을 사용하여 새로운 Access Token과 Refresh Token을 재발급합니다. " +
                    "Refresh Token은 HttpOnly 쿠키에서 자동으로 읽어오며, " +
                    "새로운 Refresh Token은 HttpOnly 쿠키로 전달됩니다 (Refresh Token Rotation)."
    )
    @PostMapping("/token/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(
            jakarta.servlet.http.HttpServletRequest request,
            HttpServletResponse response
    ) {
        log.info("토큰 재발급 API 호출");

        // 쿠키에서 Refresh Token 읽기
        String refreshToken = com.concrete.buildup.global.util.CookieUtil.getCookieValue(request, "refreshToken")
                .orElseThrow(() -> {
                    log.warn("Refresh Token 쿠키를 찾을 수 없음");
                    return new com.concrete.buildup.global.exception.BusinessException(
                            com.concrete.buildup.global.exception.errorcode.AuthErrorCode.REFRESH_TOKEN_NOT_FOUND
                    );
                });

        // 토큰 재발급 처리
        LoginResult loginResult = authService.refreshToken(refreshToken);

        // 개발 환경 확인
        boolean isProduction = !environment.acceptsProfiles(org.springframework.core.env.Profiles.of("dev", "local"));

        // 새로운 Access Token을 HttpOnly 쿠키로 설정
        org.springframework.http.ResponseCookie accessTokenCookie = org.springframework.http.ResponseCookie.from("accessToken", loginResult.getLoginResponse().getAccessToken())
                .httpOnly(true)          // XSS 공격 방어
                .secure(isProduction)    // 운영: HTTPS 필수, 개발: HTTP 허용
                .path("/")               // 모든 경로에서 접근 가능
                .maxAge(loginResult.getLoginResponse().getExpiresIn())  // Access Token 만료 시간
                .sameSite(isProduction ? "Strict" : "Lax")  // 개발: Lax, 운영: Strict
                .build();
        response.addHeader("Set-Cookie", accessTokenCookie.toString());

        // 새로운 Refresh Token을 HttpOnly 쿠키로 설정
        org.springframework.http.ResponseCookie newRefreshTokenCookie = org.springframework.http.ResponseCookie.from("refreshToken", loginResult.getRefreshToken())
                .httpOnly(true)          // XSS 공격 방어
                .secure(isProduction)    // 운영: HTTPS 필수, 개발: HTTP 허용
                .path("/")               // 모든 경로에서 접근 가능
                .maxAge(loginResult.getRefreshTokenMaxAge())  // 만료 시간 설정
                .sameSite(isProduction ? "Strict" : "Lax")  // 개발: Lax, 운영: Strict
                .build();
        response.addHeader("Set-Cookie", newRefreshTokenCookie.toString());

        log.debug("새로운 Access Token 및 Refresh Token 쿠키 설정 완료: maxAge={}초", loginResult.getRefreshTokenMaxAge());

        return ResponseEntity.ok(
                ApiResponse.success(loginResult.getLoginResponse(), "토큰 재발급 성공")
        );
    }
}
