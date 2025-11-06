package com.concrete.buildup.domain.auth.service;

import com.concrete.buildup.domain.auth.dto.*;
import com.concrete.buildup.domain.auth.entity.Employee;
import com.concrete.buildup.domain.auth.entity.Manager;
import com.concrete.buildup.domain.auth.entity.Role;
import com.concrete.buildup.domain.auth.entity.User;
import com.concrete.buildup.domain.auth.repository.EmployeeRepository;
import com.concrete.buildup.domain.auth.repository.ManagerRepository;
import com.concrete.buildup.domain.auth.repository.RoleRepository;
import com.concrete.buildup.domain.auth.repository.UserRepository;
import com.concrete.buildup.domain.site.entity.Site;
import com.concrete.buildup.domain.site.repository.SiteRepository;
import com.concrete.buildup.global.exception.BusinessException;
import com.concrete.buildup.global.exception.errorcode.AuthErrorCode;
import com.concrete.buildup.global.exception.errorcode.SiteErrorCode;
import com.concrete.buildup.global.util.AesEncryptionUtil;
import com.concrete.buildup.global.util.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

/**
 * 인증/인가 서비스
 *
 * <p>회원가입, 로그인, 아이디 중복 확인 등 인증 관련 비즈니스 로직을 처리합니다.</p>
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final ManagerRepository managerRepository;
    private final RoleRepository roleRepository;
    private final SiteRepository siteRepository;
    private final PasswordEncoder passwordEncoder;
    private final AesEncryptionUtil aesEncryptionUtil;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    @Value("${jwt.refresh-token-remember-me-expiration}")
    private long refreshTokenRememberMeExpiration;

    /**
     * 아이디 중복 확인
     *
     * @param userId 확인할 사용자 ID
     * @return UserExistsResponse - exists: true(사용 중), false(사용 가능)
     */
    public UserExistsResponse checkUserIdExists(String userId) {
        log.debug("아이디 중복 확인 요청: userId={}", userId);

        boolean exists = userRepository.existsByUserId(userId);

        log.debug("아이디 중복 확인 결과: userId={}, exists={}", userId, exists);

        return UserExistsResponse.builder()
                .exists(exists)
                .build();
    }

    /**
     * 근로자 회원가입
     *
     * @param request 근로자 회원가입 요청 정보
     * @return SignUpResponse - 생성된 사용자 및 프로필 정보
     */
    @Transactional
    public SignUpResponse registerEmployee(EmployeeSignUpRequest request) {
        log.info("근로자 회원가입 시작: userId={}", request.getUserId());

        // 1. 비밀번호 일치 확인
        if (!request.isPasswordMatching()) {
            log.warn("비밀번호 불일치: userId={}", request.getUserId());
            throw new BusinessException(AuthErrorCode.PASSWORD_MISMATCH);
        }

        // 2. 아이디 중복 확인
        if (userRepository.existsByUserId(request.getUserId())) {
            log.warn("중복된 아이디: userId={}", request.getUserId());
            throw new BusinessException(AuthErrorCode.DUPLICATE_USER_ID);
        }

        // 3. 역할 조회 (EMPLOYEE)
        Role employeeRole = roleRepository.findByRoleName("EMPLOYEE")
                .orElseThrow(() -> {
                    log.error("EMPLOYEE 역할을 찾을 수 없습니다");
                    return new BusinessException(AuthErrorCode.ROLE_NOT_FOUND);
                });

        // 4. 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        // 5. User 생성 및 저장
        User user = User.builder()
                .userId(request.getUserId())
                .password(encodedPassword)
                .phone(request.getPhone())
                .email(request.getEmail())
                .secretKey(request.getSecretKey())
                .role(employeeRole)
                .build();

        User savedUser = userRepository.save(user);
        log.debug("User 저장 완료: id={}, userId={}", savedUser.getId(), savedUser.getUserId());

        // 6. Employee 생성 및 저장 (주민등록번호 AES-256-GCM 암호화)
        String encryptedResidentNum = aesEncryptionUtil.encrypt(request.getResidentNum());

        Employee employee = Employee.builder()
                .user(savedUser)
                .empName(request.getEmpName())
                .residentNum(encryptedResidentNum)
                .subPhone(request.getEmergencyPhone())
                .empAddress(request.getEmpAddress())
                .build();

        Employee savedEmployee = employeeRepository.save(employee);
        log.debug("Employee 저장 완료: id={}, empName={} (주민등록번호 암호화 완료)",
                savedEmployee.getId(), savedEmployee.getEmpName());

        // 7. Response 생성
        SignUpResponse response = buildSignUpResponse(savedUser, savedEmployee, request.getSecretKey());

        log.info("근로자 회원가입 완료: userId={}, employeeId={}", savedUser.getUserId(), savedEmployee.getId());

        return response;
    }

    /**
     * 근로자 회원가입 1단계 (회원가입 완료)
     *
     * @param request 1단계 회원가입 요청 정보
     * @return SignUpPhase1Response - 회원가입 완료 정보 및 프로필 토큰
     */
    @Transactional
    public SignUpPhase1Response registerEmployeePhase1(EmployeeSignUpPhase1Request request) {
        log.info("근로자 회원가입 1단계 시작 (회원가입 완료): userId={}, empName={}", request.getUserId(), request.getEmpName());

        // 1. 비밀번호 일치 확인
        if (!request.isPasswordMatching()) {
            log.warn("비밀번호 불일치: userId={}", request.getUserId());
            throw new BusinessException(AuthErrorCode.PASSWORD_MISMATCH);
        }

        // 2. 아이디 중복 확인
        if (userRepository.existsByUserId(request.getUserId())) {
            log.warn("중복된 아이디: userId={}", request.getUserId());
            throw new BusinessException(AuthErrorCode.DUPLICATE_USER_ID);
        }

        // 3. secretKey 검증 (선택적)
        Site site = null;
        if (request.getSecretKey() != null && !request.getSecretKey().isBlank()) {
            site = siteRepository.findByEmployeeSecretKey(request.getSecretKey())
                    .orElseThrow(() -> {
                        log.warn("시크릿키를 찾을 수 없음: secretKey={}", request.getSecretKey());
                        return new BusinessException(SiteErrorCode.SECRET_KEY_NOT_FOUND_OR_EXPIRED);
                    });

            // 시크릿키 유효성 확인
            if (!site.isSecretKeyValid()) {
                log.warn("시크릿키 만료: secretKey={}, siteId={}", request.getSecretKey(), site.getId());
                throw new BusinessException(SiteErrorCode.SECRET_KEY_NOT_FOUND_OR_EXPIRED);
            }

            log.debug("시크릿키 검증 완료: siteId={}, siteName={}", site.getId(), site.getSiteName());
        }

        // 4. 역할 조회 (EMPLOYEE)
        Role employeeRole = roleRepository.findByRoleName("EMPLOYEE")
                .orElseThrow(() -> {
                    log.error("EMPLOYEE 역할을 찾을 수 없습니다");
                    return new BusinessException(AuthErrorCode.ROLE_NOT_FOUND);
                });

        // 4. 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        // 5. User 생성 및 저장 (기본 정보만, phone과 email은 null)
        User user = User.builder()
                .userId(request.getUserId())
                .password(encodedPassword)
                .phone(null)  // 2단계에서 입력
                .email(null)  // 2단계에서 입력
                .secretKey(request.getSecretKey())
                .role(employeeRole)
                .profileCompleted(false)  // 아직 프로필 미완성
                .build();

        User savedUser = userRepository.save(user);
        log.debug("User 저장 완료: id={}, userId={}", savedUser.getId(), savedUser.getUserId());

        // 6. 프로필 완성 토큰 생성 (30분 유효)
        savedUser.generateProfileToken();
        log.debug("프로필 토큰 생성 완료: token={}", savedUser.getProfileToken());

        // 7. Employee 생성 및 저장 (empName만, 나머지는 2단계에서 입력)
        Employee employee = Employee.builder()
                .user(savedUser)
                .empName(request.getEmpName())
                .residentNum(null)  // 2단계에서 입력 및 암호화
                .subPhone(null)  // 2단계에서 입력
                .empAddress(null)  // 2단계에서 입력
                .build();

        Employee savedEmployee = employeeRepository.save(employee);
        log.debug("Employee 저장 완료: id={}, empName={}", savedEmployee.getId(), savedEmployee.getEmpName());

        log.info("근로자 회원가입 1단계 완료 (회원가입 완료): userId={}, employeeId={}", savedUser.getUserId(), savedEmployee.getId());

        // 8. Response 생성
        if (site != null) {
            log.debug("현장 연동 정보 포함: siteId={}, siteName={}", site.getId(), site.getSiteName());
            return SignUpPhase1Response.of(
                    savedUser.getId(),
                    savedUser.getUserId(),
                    savedUser.getRole().getRoleName(),
                    savedEmployee.getId(),
                    savedEmployee.getEmpName(),
                    savedUser.getProfileToken(),
                    savedUser.getProfileTokenExpiresAt(),
                    site.getId(),
                    site.getSiteName()
            );
        } else {
            return SignUpPhase1Response.of(
                    savedUser.getId(),
                    savedUser.getUserId(),
                    savedUser.getRole().getRoleName(),
                    savedEmployee.getId(),
                    savedEmployee.getEmpName(),
                    savedUser.getProfileToken(),
                    savedUser.getProfileTokenExpiresAt()
            );
        }
    }

    /**
     * 근로자 회원가입 2단계 (상세 정보 입력)
     *
     * @param request 2단계 회원가입 요청 정보
     * @return SignUpResponse - 업데이트된 사용자 및 프로필 정보
     */
    @Transactional
    public SignUpResponse registerEmployeePhase2(EmployeeSignUpPhase2Request request) {
        log.info("근로자 상세 정보 입력 시작: token={}", request.getRegistrationToken());

        // 1. 프로필 토큰으로 사용자 조회
        User user = userRepository.findByProfileToken(request.getRegistrationToken())
                .orElseThrow(() -> {
                    log.warn("프로필 토큰을 찾을 수 없음: token={}", request.getRegistrationToken());
                    return new BusinessException(AuthErrorCode.REGISTRATION_TOKEN_NOT_FOUND);
                });

        // 2. 토큰 유효성 확인
        if (!user.isProfileTokenValid()) {
            log.warn("프로필 토큰 만료 또는 무효: token={}, userId={}", request.getRegistrationToken(), user.getUserId());
            throw new BusinessException(AuthErrorCode.REGISTRATION_TOKEN_EXPIRED);
        }

        // 3. User 정보 업데이트 (phone, email)
        user.updateInfo(request.getPhone(), request.getEmail());
        log.debug("User 정보 업데이트 완료: id={}, userId={}", user.getId(), user.getUserId());

        // 4. Employee 조회 및 정보 업데이트 (주민등록번호 AES-256-GCM 암호화)
        Employee employee = employeeRepository.findByUser(user)
                .orElseThrow(() -> {
                    log.error("Employee를 찾을 수 없습니다: userId={}", user.getUserId());
                    return new BusinessException(AuthErrorCode.USER_NOT_FOUND);
                });

        String encryptedResidentNum = aesEncryptionUtil.encrypt(request.getResidentNum());

        // Employee 정보 업데이트
        employee.updateProfile(
                encryptedResidentNum,
                request.getEmergencyPhone(),
                request.getEmpAddress()
        );
        log.debug("Employee 정보 업데이트 완료: id={}, empName={} (주민등록번호 암호화 완료)",
                employee.getId(), employee.getEmpName());

        // 5. 프로필 완성 처리
        user.completeProfile();
        log.debug("프로필 완성 처리: userId={}", user.getUserId());

        // 6. Response 생성
        SignUpResponse response = buildSignUpResponse(user, employee, user.getSecretKey());

        log.info("근로자 상세 정보 입력 완료: userId={}, employeeId={}", user.getUserId(), employee.getId());

        return response;
    }

    /**
     * 현장 관리자 회원가입
     *
     * @param request 현장 관리자 회원가입 요청 정보
     * @return SignUpResponse - 생성된 사용자 및 관리자 정보
     */
    @Transactional
    public SignUpResponse registerManager(ManagerSignUpRequest request) {
        log.info("현장 관리자 회원가입 시작: userId={}, managerName={}", request.getUserId(), request.getManagerName());

        // 1. 비밀번호 일치 확인
        if (!request.isPasswordMatching()) {
            log.warn("비밀번호 불일치: userId={}", request.getUserId());
            throw new BusinessException(AuthErrorCode.PASSWORD_MISMATCH);
        }

        // 2. 아이디 중복 확인
        if (userRepository.existsByUserId(request.getUserId())) {
            log.warn("중복된 아이디: userId={}", request.getUserId());
            throw new BusinessException(AuthErrorCode.DUPLICATE_USER_ID);
        }

        // 3. secretKey로 Site 검증
        Site site = siteRepository.findByManagerSecretKey(request.getSecretKey())
                .orElseThrow(() -> {
                    log.warn("시크릿키를 찾을 수 없음: secretKey={}", request.getSecretKey());
                    return new BusinessException(SiteErrorCode.SECRET_KEY_NOT_FOUND_OR_EXPIRED);
                });

        // 4. 시크릿키 유효성 확인
        if (!site.isSecretKeyValid()) {
            log.warn("시크릿키 만료: secretKey={}, siteId={}", request.getSecretKey(), site.getId());
            throw new BusinessException(SiteErrorCode.SECRET_KEY_NOT_FOUND_OR_EXPIRED);
        }

        // 5. 시크릿키 점유 확인
        if (userRepository.existsBySecretKey(request.getSecretKey())) {
            log.warn("이미 사용 중인 시크릿키: secretKey={}", request.getSecretKey());
            throw new BusinessException(SiteErrorCode.SECRET_KEY_ALREADY_USED);
        }

        // 6. 역할 조회 (MANAGER)
        Role managerRole = roleRepository.findByRoleName("MANAGER")
                .orElseThrow(() -> {
                    log.error("MANAGER 역할을 찾을 수 없습니다");
                    return new BusinessException(AuthErrorCode.ROLE_NOT_FOUND);
                });

        // 7. 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        // 8. User 생성 및 저장
        User user = User.builder()
                .userId(request.getUserId())
                .password(encodedPassword)
                .phone(request.getPhone())
                .email(null)
                .secretKey(request.getSecretKey())
                .role(managerRole)
                .profileCompleted(true)  // 현장 관리자는 프로필 완성 상태로 시작
                .build();

        User savedUser = userRepository.save(user);
        log.debug("User 저장 완료: id={}, userId={}", savedUser.getId(), savedUser.getUserId());

        // 9. Manager 생성 및 저장
        Manager manager = Manager.builder()
                .user(savedUser)
                .managerName(request.getManagerName())
                .build();

        Manager savedManager = managerRepository.save(manager);
        log.debug("Manager 저장 완료: id={}, managerName={}", savedManager.getId(), savedManager.getManagerName());

        // 10. Response 생성
        SignUpResponse response = buildManagerSignUpResponse(savedUser, savedManager, site);

        log.info("현장 관리자 회원가입 완료: userId={}, managerId={}, siteId={}",
                savedUser.getUserId(), savedManager.getId(), site.getId());

        return response;
    }

    /**
     * 회원가입 응답 생성 (근로자)
     *
     * @param user 생성된 사용자
     * @param employee 생성된 근로자
     * @param secretKey 시크릿키 (nullable)
     * @return SignUpResponse
     */
    private SignUpResponse buildSignUpResponse(User user, Employee employee, String secretKey) {
        // User 정보
        SignUpResponse.UserInfo userInfo = SignUpResponse.UserInfo.builder()
                .id(user.getId())
                .userId(user.getUserId())
                .role(user.getRole().getRoleName())
                .phone(user.getPhone())
                .email(user.getEmail())
                .build();

        // Profile 정보
        SignUpResponse.ProfileInfo profileInfo = SignUpResponse.ProfileInfo.builder()
                .employeeId(employee.getId())
                .empName(employee.getEmpName())
                .empAddress(employee.getEmpAddress())
                .emergencyPhone(employee.getSubPhone())
                .build();

        // 연락처 검증 필요 여부
        SignUpResponse.VerificationInfo verificationInfo = SignUpResponse.VerificationInfo.builder()
                .phone(true)  // 항상 휴대폰 검증 필요
                .email(user.getEmail() != null && !user.getEmail().isBlank())  // 이메일이 있으면 검증 필요
                .build();

        // 시크릿키 처리 결과 (현재는 null, 추후 현장 연동 기능 구현 시 확장)
        SignUpResponse.LinkingInfo linkingInfo = SignUpResponse.LinkingInfo.builder()
                .secretKeyUsed(secretKey != null && !secretKey.isBlank())
                .siteLinked(null)  // 추후 현장 연동 구현 시 설정
                .build();

        return SignUpResponse.builder()
                .user(userInfo)
                .profile(profileInfo)
                .verificationRequired(verificationInfo)
                .linking(linkingInfo)
                .build();
    }

    /**
     * 회원가입 응답 생성 (현장 관리자)
     *
     * @param user 생성된 사용자
     * @param manager 생성된 관리자
     * @param site 연결된 현장
     * @return SignUpResponse
     */
    private SignUpResponse buildManagerSignUpResponse(User user, Manager manager, Site site) {
        // User 정보
        SignUpResponse.UserInfo userInfo = SignUpResponse.UserInfo.builder()
                .id(user.getId())
                .userId(user.getUserId())
                .role(user.getRole().getRoleName())
                .phone(user.getPhone())
                .email(user.getEmail())
                .build();

        // Profile 정보 (관리자)
        SignUpResponse.ProfileInfo profileInfo = SignUpResponse.ProfileInfo.builder()
                .managerId(manager.getId())
                .managerName(manager.getManagerName())
                .build();

        // 연락처 검증 필요 여부
        SignUpResponse.VerificationInfo verificationInfo = SignUpResponse.VerificationInfo.builder()
                .phone(true)  // 항상 휴대폰 검증 필요
                .email(false)  // 관리자는 이메일 선택사항
                .build();

        // 시크릿키 처리 결과 (현장 연동 성공)
        SignUpResponse.SiteInfo siteInfo = SignUpResponse.SiteInfo.builder()
                .siteId(site.getId())
                .siteName(site.getSiteName())
                .build();

        SignUpResponse.LinkingInfo linkingInfo = SignUpResponse.LinkingInfo.builder()
                .secretKeyUsed(true)
                .siteLinked(siteInfo)
                .build();

        return SignUpResponse.builder()
                .user(userInfo)
                .profile(profileInfo)
                .verificationRequired(verificationInfo)
                .linking(linkingInfo)
                .build();
    }

    /**
     * 로그인
     *
     * @param request 로그인 요청 정보
     * @return LoginResult - Access Token, 사용자 정보, Refresh Token
     */
    @Transactional
    public LoginResult login(LoginRequest request) {
        log.info("로그인 시도: username={}", request.getUsername());

        // 1. userId로 User 조회 (Fetch Join으로 Role도 함께 조회)
        User user = userRepository.findByUserIdWithRole(request.getUsername())
                .orElseThrow(() -> {
                    log.warn("사용자를 찾을 수 없음: username={}", request.getUsername());
                    return new BusinessException(AuthErrorCode.INVALID_CREDENTIALS);
                });

        // 2. 비밀번호 검증
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("비밀번호 불일치: username={}", request.getUsername());
            throw new BusinessException(AuthErrorCode.INVALID_CREDENTIALS);
        }

        // 3. Access Token 생성
        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getUserId(),
                user.getRole().getRoleName()
        );
        log.debug("Access Token 생성 완료: userId={}", user.getUserId());

        // 4. Refresh Token 생성 (rememberMe 고려)
        boolean rememberMe = request.getRememberMe() != null && request.getRememberMe();
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUserId(), rememberMe);
        log.debug("Refresh Token 생성 완료: userId={}, rememberMe={}", user.getUserId(), rememberMe);

        // 5. Refresh Token을 SHA-256으로 해시하여 DB에 저장
        String hashedRefreshToken = hashToken(refreshToken);
        long expiration = rememberMe ? refreshTokenRememberMeExpiration : refreshTokenExpiration;
        LocalDateTime refreshTokenExpiresAt = LocalDateTime.now().plusSeconds(expiration / 1000);
        user.updateRefreshToken(hashedRefreshToken, refreshTokenExpiresAt);
        log.debug("Refresh Token 해시 후 DB 저장 완료: userId={}", user.getUserId());

        // 6. Response 생성
        LoginResponse loginResponse = LoginResponse.builder()
                .accessToken(accessToken)
                .userId(user.getUserId())
                .role(user.getRole().getRoleName())
                .expiresIn(accessTokenExpiration / 1000)  // 초 단위로 변환
                .build();

        // 7. LoginResult 생성 (refreshToken 포함, 평문)
        LoginResult result = LoginResult.builder()
                .loginResponse(loginResponse)
                .refreshToken(refreshToken)  // 평문 토큰 (쿠키로 전달용)
                .refreshTokenMaxAge(expiration / 1000)  // 초 단위로 변환
                .build();

        log.info("로그인 성공: userId={}, role={}", user.getUserId(), user.getRole().getRoleName());

        return result;
    }

    /**
     * 토큰을 SHA-256으로 해시 처리
     *
     * <p>Refresh Token을 DB에 안전하게 저장하기 위해 SHA-256으로 해시합니다.</p>
     * <p>DB 해킹 시에도 원본 토큰을 알 수 없도록 보호합니다.</p>
     *
     * @param token 원본 토큰
     * @return SHA-256 해시값 (Hex 형식)
     */
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 알고리즘을 찾을 수 없습니다", e);
            throw new RuntimeException("토큰 해시 처리 중 오류 발생", e);
        }
    }
}
