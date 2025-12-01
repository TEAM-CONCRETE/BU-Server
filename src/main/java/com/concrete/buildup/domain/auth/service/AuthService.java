package com.concrete.buildup.domain.auth.service;

import com.concrete.buildup.domain.auth.dto.*;
import com.concrete.buildup.domain.auth.entity.Corporation;
import com.concrete.buildup.domain.auth.entity.Employee;
import com.concrete.buildup.domain.auth.entity.Manager;
import com.concrete.buildup.domain.auth.entity.Role;
import com.concrete.buildup.domain.auth.entity.User;
import com.concrete.buildup.domain.auth.repository.CorporationRepository;
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
import java.util.Optional;
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
    private final CorporationRepository corporationRepository;
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
        Role employeeRole = roleRepository.findByRoleName("ROLE_EMPLOYEE")
                .orElseThrow(() -> {
                    log.error("ROLE_EMPLOYEE 역할을 찾을 수 없습니다");
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
                .role(employeeRole)
                .build();

        User savedUser = userRepository.save(user);
        log.debug("User 저장 완료: id={}, userId={}", savedUser.getId(), savedUser.getUserId());

        // 6. Employee 생성 및 저장 (주민등록번호는 ResidentNumConverter에서 자동 암호화)
        Employee employee = Employee.builder()
                .user(savedUser)
                .empName(request.getEmpName())
                .residentNum(request.getResidentNum())
                .subPhone(request.getEmergencyPhone())
                .empAddress(request.getEmpAddress())
                .build();

        Employee savedEmployee = employeeRepository.save(employee);
        log.debug("Employee 저장 완료: id={}, empName={} (주민등록번호 암호화 완료)",
                savedEmployee.getId(), savedEmployee.getEmpName());

        // 7. Response 생성
        SignUpResponse response = buildSignUpResponse(savedUser, savedEmployee, savedUser.getSiteId());

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
        Role employeeRole = roleRepository.findByRoleName("ROLE_EMPLOYEE")
                .orElseThrow(() -> {
                    log.error("ROLE_EMPLOYEE 역할을 찾을 수 없습니다");
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
                .role(employeeRole)
                .siteId(site != null ? site.getId() : null)  // 현장 ID 저장 (secretKey 대신)
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

        // 4. Employee 조회 및 정보 업데이트 (주민등록번호는 ResidentNumConverter에서 자동 암호화)
        Employee employee = employeeRepository.findByUser(user)
                .orElseThrow(() -> {
                    log.error("Employee를 찾을 수 없습니다: userId={}", user.getUserId());
                    return new BusinessException(AuthErrorCode.USER_NOT_FOUND);
                });

        // Employee 정보 업데이트
        employee.updateProfile(
                request.getResidentNum(),
                request.getEmergencyPhone(),
                request.getEmpAddress()
        );
        log.debug("Employee 정보 업데이트 완료: id={}, empName={} (주민등록번호 암호화 완료)",
                employee.getId(), employee.getEmpName());

        // 5. 프로필 완성 처리
        user.completeProfile();
        log.debug("프로필 완성 처리: userId={}", user.getUserId());

        // 6. Response 생성
        SignUpResponse response = buildSignUpResponse(user, employee, user.getSiteId());

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

        // 5. 역할 조회 (MANAGER)
        Role managerRole = roleRepository.findByRoleName("ROLE_MANAGER")
                .orElseThrow(() -> {
                    log.error("ROLE_MANAGER 역할을 찾을 수 없습니다");
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
                .role(managerRole)
                .siteId(site.getId())  // 현장 ID 저장 (secretKey 대신)
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

        // 10. Site에 Manager 할당
        site.assignManager(savedManager);
        siteRepository.save(site);
        log.debug("Site에 Manager 할당 완료: siteId={}, managerId={}", site.getId(), savedManager.getId());

        // 11. Response 생성
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
     * @param siteId 현장 ID (nullable)
     * @return SignUpResponse
     */
    private SignUpResponse buildSignUpResponse(User user, Employee employee, Long siteId) {
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

        // 현장 연동 정보
        SignUpResponse.SiteInfo siteInfo = null;
        if (siteId != null) {
            siteInfo = siteRepository.findById(siteId)
                    .map(site -> SignUpResponse.SiteInfo.builder()
                            .siteId(site.getId())
                            .siteName(site.getSiteName())
                            .build())
                    .orElse(null);
        }

        SignUpResponse.LinkingInfo linkingInfo = SignUpResponse.LinkingInfo.builder()
                .secretKeyUsed(siteId != null)
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

        // 3. Role 검증 (DB에 직접 삽입 시 role_id가 null일 수 있음)
        if (user.getRole() == null) {
            log.error("사용자의 역할이 설정되지 않음: userId={}", user.getUserId());
            throw new BusinessException(AuthErrorCode.ROLE_NOT_FOUND);
        }

        // 4. Access Token 생성
        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getUserId(),
                user.getRole().getRoleName()
        );
        log.debug("Access Token 생성 완료: userId={}", user.getUserId());

        // 5. Refresh Token 생성 (rememberMe 고려)
        boolean rememberMe = request.getRememberMe() != null && request.getRememberMe();
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUserId(), rememberMe);
        log.debug("Refresh Token 생성 완료: userId={}, rememberMe={}", user.getUserId(), rememberMe);

        // 6. Refresh Token을 SHA-256으로 해시하여 DB에 저장
        String hashedRefreshToken = hashToken(refreshToken);
        long expiration = rememberMe ? refreshTokenRememberMeExpiration : refreshTokenExpiration;
        LocalDateTime refreshTokenExpiresAt = LocalDateTime.now().plusSeconds(expiration / 1000);
        user.updateRefreshToken(hashedRefreshToken, refreshTokenExpiresAt);
        log.debug("Refresh Token 해시 후 DB 저장 완료: userId={}", user.getUserId());

        // 7. 역할별 사용자 이름 조회
        String userName = getUserNameByRole(user);
        log.debug("사용자 이름 조회 완료: userId={}, userName={}", user.getUserId(), userName);

        // 8. 역할별 추가 ID 조회 (employeeId, managerId, siteId)
        // N+1 문제 방지를 위해 최적화된 쿼리 사용
        Long employeeId = null;
        Long managerId = null;
        Long siteId = null;
        Boolean hasRequiredInfo = null;
        Boolean hasProfileImage = null;

        String roleName = user.getRole().getRoleName();
        if ("ROLE_EMPLOYEE".equals(roleName)) {
            // 근로자: User ID로 직접 조회
            Employee employee = employeeRepository.findByUserId(user.getId()).orElse(null);
            if (employee != null) {
                employeeId = employee.getId();

                // 필수 정보 완성 여부 확인
                hasRequiredInfo = isNotEmpty(employee.getResidentNum())
                    && isNotEmpty(user.getPhone())
                    && isNotEmpty(user.getEmail())
                    && isNotEmpty(employee.getEmpAddress())
                    && isNotEmpty(employee.getSubPhone());

                // 프로필 이미지 등록 여부 확인
                hasProfileImage = isNotEmpty(employee.getProfileImageUrl());
            }
            siteId = user.getSiteId();  // 근로자는 User 테이블의 site_id 사용
            log.debug("근로자 ID 조회 완료: userId={}, employeeId={}, siteId={}, hasRequiredInfo={}, hasProfileImage={}",
                      user.getUserId(), employeeId, siteId, hasRequiredInfo, hasProfileImage);
        } else if ("ROLE_MANAGER".equals(roleName)) {
            // 관리자: Manager 조회 후 관리하는 Site 조회
            Manager manager = managerRepository.findByUserId(user.getId()).orElse(null);
            if (manager != null) {
                managerId = manager.getId();
                // Manager가 관리하는 Site 조회
                siteId = siteRepository.findByManagerId(managerId)
                        .map(Site::getId)
                        .orElse(null);
                log.debug("현장 관리자 ID 조회 완료: userId={}, managerId={}, siteId={}",
                          user.getUserId(), managerId, siteId);
            }
        }

        // 9. Response 생성 (Access Token은 HttpOnly 쿠키로 전달)
        LoginResponse loginResponse = LoginResponse.builder()
                .userId(user.getUserId())
                .userName(userName)
                .role(user.getRole().getRoleName())
                .profileCompleted(user.getProfileCompleted())  // 프로필 완성 여부
                .expiresIn(accessTokenExpiration / 1000)  // 초 단위로 변환
                .employeeId(employeeId)  // 근로자인 경우만 값이 있음
                .managerId(managerId)    // 현장 관리자인 경우만 값이 있음
                .siteId(siteId)          // 근로자/관리자인 경우 값이 있음 (User.siteId)
                .hasRequiredInfo(hasRequiredInfo)  // 근로자인 경우만 값이 있음
                .hasProfileImage(hasProfileImage)  // 근로자인 경우만 값이 있음
                .build();

        // 8. LoginResult 생성 (accessToken, refreshToken 포함, 평문)
        LoginResult result = LoginResult.builder()
                .loginResponse(loginResponse)
                .accessToken(accessToken)    // 평문 토큰 (쿠키로 전달용)
                .refreshToken(refreshToken)  // 평문 토큰 (쿠키로 전달용)
                .refreshTokenMaxAge(expiration / 1000)  // 초 단위로 변환
                .build();

        log.info("로그인 성공: userId={}, role={}", user.getUserId(), user.getRole().getRoleName());

        return result;
    }

    /**
     * 내 정보 조회
     *
     * <p>인증된 사용자의 기본 정보 및 역할별 추가 정보를 조회합니다.</p>
     *
     * @param userId 사용자 ID (로그인 ID)
     * @return UserInfoResponse - 사용자 정보 및 역할별 추가 정보
     */
    public UserInfoResponse getMyInfo(String userId) {
        log.info("내 정보 조회 시작: userId={}", userId);

        // 1. userId로 User 조회 (Fetch Join으로 Role도 함께 조회)
        User user = userRepository.findByUserIdWithRole(userId)
                .orElseThrow(() -> {
                    log.warn("사용자를 찾을 수 없음: userId={}", userId);
                    return new BusinessException(AuthErrorCode.USER_NOT_FOUND);
                });

        // 2. Role 검증 (DB에 직접 삽입 시 role_id가 null일 수 있음)
        if (user.getRole() == null) {
            log.error("사용자의 역할이 설정되지 않음: userId={}", user.getUserId());
            throw new BusinessException(AuthErrorCode.ROLE_NOT_FOUND);
        }

        // 3. 역할별 추가 정보 조회
        String roleName = user.getRole().getRoleName();
        Object additionalInfo = null;
        String name = null;

        switch (roleName) {
            case "ROLE_EMPLOYEE":
                // 근로자 정보 조회
                Employee employee = employeeRepository.findByUser(user)
                        .orElseThrow(() -> {
                            log.error("Employee를 찾을 수 없습니다: userId={}", userId);
                            return new BusinessException(AuthErrorCode.USER_NOT_FOUND);
                        });

                name = employee.getEmpName();

                // 현장 정보 조회 (siteId로)
                UserInfoResponse.SiteInfo siteInfo = null;
                if (user.getSiteId() != null) {
                    siteInfo = siteRepository.findById(user.getSiteId())
                            .map(site -> {
                                log.debug("현장 정보 조회 성공: siteId={}, siteName={}", site.getId(), site.getSiteName());
                                return UserInfoResponse.SiteInfo.builder()
                                        .siteId(site.getId())
                                        .siteName(site.getSiteName())
                                        .siteAddress(site.getSiteAddress())
                                        .build();
                            })
                            .orElse(null);
                }

                additionalInfo = UserInfoResponse.EmployeeInfo.builder()
                        .employeeId(employee.getId())
                        .empName(employee.getEmpName())
                        .residentNum(employee.getResidentNum())  // 이미 마스킹 처리됨 (Serializer)
                        .emergencyPhone(employee.getSubPhone())
                        .empAddress(employee.getEmpAddress())
                        .empType(employee.getEmpType())
                        .site(siteInfo)
                        .build();
                break;

            case "ROLE_MANAGER":
                // 현장 관리자 정보 조회
                Manager manager = managerRepository.findByUser(user)
                        .orElseThrow(() -> {
                            log.error("Manager를 찾을 수 없습니다: userId={}", userId);
                            return new BusinessException(AuthErrorCode.USER_NOT_FOUND);
                        });

                name = manager.getManagerName();

                // 현장 정보 조회 (siteId로)
                UserInfoResponse.SiteInfo managerSiteInfo = null;
                if (user.getSiteId() != null) {
                    managerSiteInfo = siteRepository.findById(user.getSiteId())
                            .map(site -> UserInfoResponse.SiteInfo.builder()
                                    .siteId(site.getId())
                                    .siteName(site.getSiteName())
                                    .siteAddress(site.getSiteAddress())
                                    .build())
                            .orElse(null);
                }

                additionalInfo = UserInfoResponse.ManagerInfo.builder()
                        .managerId(manager.getId())
                        .managerName(manager.getManagerName())
                        .site(managerSiteInfo)
                        .build();
                break;

            case "ROLE_CORPORATION":
                // 기업 정보 조회
                Corporation corporation = corporationRepository.findByUserId(user.getId())
                        .orElseThrow(() -> {
                            log.error("Corporation을 찾을 수 없습니다: userId={}", userId);
                            return new BusinessException(AuthErrorCode.USER_NOT_FOUND);
                        });

                name = corporation.getCorpName();

                additionalInfo = UserInfoResponse.CorporationInfo.builder()
                        .corporationId(corporation.getId())
                        .corpName(corporation.getCorpName())
                        .corpAddress(corporation.getCorpAddress())
                        .corpCeoName(corporation.getCorpCeoName())
                        .build();
                break;

            default:
                log.warn("알 수 없는 역할: role={}", roleName);
                break;
        }

        // 3. Response 생성
        UserInfoResponse response = UserInfoResponse.builder()
                .userId(user.getUserId())
                .role(roleName)
                .name(name)
                .phone(user.getPhone())
                .email(user.getEmail())
                .additionalInfo(additionalInfo)
                .build();

        log.info("내 정보 조회 완료: userId={}, role={}", userId, roleName);

        return response;
    }

    /**
     * 토큰 재발급
     *
     * <p>Refresh Token을 검증하고 새로운 Access Token과 Refresh Token을 발급합니다.</p>
     * <p>Refresh Token Rotation 적용: 새로운 Refresh Token 발급 시 기존 토큰 무효화</p>
     *
     * @param refreshToken Refresh Token (평문)
     * @return TokenRefreshResponse - 새로운 Access Token과 만료 시간
     * @throws BusinessException INVALID_REFRESH_TOKEN: 유효하지 않은 토큰
     * @throws BusinessException REFRESH_TOKEN_MISMATCH: DB와 일치하지 않는 토큰
     * @throws BusinessException USER_NOT_FOUND: 사용자를 찾을 수 없음
     */
    @Transactional
    public LoginResult refreshToken(String refreshToken) {
        log.info("토큰 재발급 요청");

        // 1. Refresh Token 검증
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            log.warn("유효하지 않은 Refresh Token");
            throw new BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        // 2. 토큰 타입 확인 (refresh 타입인지 확인)
        String tokenType = jwtTokenProvider.getTokenType(refreshToken);
        if (!"refresh".equals(tokenType)) {
            log.warn("Refresh Token이 아닌 토큰 타입: type={}", tokenType);
            throw new BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        // 3. userId 추출
        String userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        log.debug("토큰에서 userId 추출: userId={}", userId);

        // 4. DB에서 User 조회
        User user = userRepository.findByUserIdWithRole(userId)
                .orElseThrow(() -> {
                    log.warn("사용자를 찾을 수 없음: userId={}", userId);
                    return new BusinessException(AuthErrorCode.USER_NOT_FOUND);
                });

        // 5. Role 검증 (DB에 직접 삽입 시 role_id가 null일 수 있음)
        if (user.getRole() == null) {
            log.error("사용자의 역할이 설정되지 않음: userId={}", user.getUserId());
            throw new BusinessException(AuthErrorCode.ROLE_NOT_FOUND);
        }

        // 6. DB에 저장된 Refresh Token과 비교 (해시 비교)
        String hashedRefreshToken = hashToken(refreshToken);
        if (!hashedRefreshToken.equals(user.getRefreshToken())) {
            log.warn("Refresh Token 불일치: userId={}", userId);
            throw new BusinessException(AuthErrorCode.REFRESH_TOKEN_MISMATCH);
        }

        // 6. Refresh Token 만료 시간 확인
        if (user.getRefreshTokenExpiresAt() == null ||
            LocalDateTime.now().isAfter(user.getRefreshTokenExpiresAt())) {
            log.warn("만료된 Refresh Token: userId={}", userId);
            throw new BusinessException(AuthErrorCode.REFRESH_TOKEN_EXPIRED);
        }

        // 7. 새로운 Access Token 생성
        String newAccessToken = jwtTokenProvider.generateAccessToken(
                user.getUserId(),
                user.getRole().getRoleName()
        );
        log.debug("새로운 Access Token 생성 완료: userId={}", user.getUserId());

        // 8. 기존 Refresh Token의 만료 시간으로 rememberMe 여부 판단
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = user.getRefreshTokenExpiresAt();
        long remainingTime = java.time.Duration.between(now, expiresAt).toMillis();

        // 남은 시간이 일반 만료시간(7일)보다 길면 rememberMe=true로 간주
        boolean isRememberMe = remainingTime > refreshTokenExpiration;
        long expiration = isRememberMe ? refreshTokenRememberMeExpiration : refreshTokenExpiration;

        log.debug("기존 토큰 rememberMe 판단: isRememberMe={}, remainingTime={}ms, threshold={}ms",
                isRememberMe, remainingTime, refreshTokenExpiration);

        // 9. Refresh Token Rotation: 새로운 Refresh Token 생성 (기존 rememberMe 유지)
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(user.getUserId(), isRememberMe);
        String hashedNewRefreshToken = hashToken(newRefreshToken);
        LocalDateTime newRefreshTokenExpiresAt = LocalDateTime.now()
                .plusSeconds(expiration / 1000);
        user.updateRefreshToken(hashedNewRefreshToken, newRefreshTokenExpiresAt);
        log.debug("새로운 Refresh Token 생성 및 DB 저장 완료: userId={}, rememberMe={}",
                user.getUserId(), isRememberMe);

        // 10. 역할별 사용자 이름 조회
        String userName = getUserNameByRole(user);
        log.debug("사용자 이름 조회 완료: userId={}, userName={}", user.getUserId(), userName);

        // 11. 역할별 추가 ID 조회 (employeeId, managerId, siteId)
        // N+1 문제 방지를 위해 최적화된 쿼리 사용
        Long employeeId = null;
        Long managerId = null;
        Long siteId = null;

        String roleName = user.getRole().getRoleName();
        if ("ROLE_EMPLOYEE".equals(roleName)) {
            // 근로자: User로 직접 조회
            employeeId = employeeRepository.findByUser(user)
                    .map(Employee::getId)
                    .orElse(null);
            siteId = user.getSiteId();  // 근로자는 User 테이블의 site_id 사용
            log.debug("근로자 ID 조회 완료: userId={}, employeeId={}, siteId={}",
                      user.getUserId(), employeeId, siteId);
        } else if ("ROLE_MANAGER".equals(roleName)) {
            // 관리자: Manager 조회 후 관리하는 Site 조회
            Manager manager = managerRepository.findByUser(user).orElse(null);
            if (manager != null) {
                managerId = manager.getId();
                // Manager가 관리하는 Site 조회
                siteId = siteRepository.findByManagerId(managerId)
                        .map(Site::getId)
                        .orElse(null);
                log.debug("현장 관리자 ID 조회 완료: userId={}, managerId={}, siteId={}",
                          user.getUserId(), managerId, siteId);
            }
        }

        // 12. Response 생성 (Access Token은 HttpOnly 쿠키로 전달)
        LoginResponse loginResponse = LoginResponse.builder()
                .userId(user.getUserId())
                .userName(userName)
                .role(user.getRole().getRoleName())
                .profileCompleted(user.getProfileCompleted())  // 프로필 완성 여부
                .expiresIn(accessTokenExpiration / 1000)  // 초 단위로 변환
                .employeeId(employeeId)  // 근로자인 경우만 값이 있음
                .managerId(managerId)    // 현장 관리자인 경우만 값이 있음
                .siteId(siteId)          // 근로자/관리자인 경우 값이 있음 (User.siteId)
                .build();

        // 11. LoginResult 생성 (accessToken, refreshToken 포함, 평문)
        LoginResult result = LoginResult.builder()
                .loginResponse(loginResponse)
                .accessToken(newAccessToken)     // 평문 토큰 (쿠키로 전달용)
                .refreshToken(newRefreshToken)  // 평문 토큰 (쿠키로 전달용)
                .refreshTokenMaxAge(expiration / 1000)  // 초 단위로 변환 (rememberMe 반영)
                .build();

        log.info("토큰 재발급 성공: userId={}, rememberMe={}", user.getUserId(), isRememberMe);

        return result;
    }

    /**
     * 역할별 사용자 이름 조회
     *
     * <p>사용자의 역할에 따라 근로자명, 관리자명, 기업명을 조회합니다.</p>
     *
     * @param user 사용자 엔티티
     * @return 사용자 이름 (근로자명, 관리자명, 기업명)
     */
    private String getUserNameByRole(User user) {
        String roleName = user.getRole().getRoleName();

        switch (roleName) {
            case "ROLE_EMPLOYEE":
                return employeeRepository.findByUser(user)
                        .map(Employee::getEmpName)
                        .orElseThrow(() -> {
                            log.error("Employee를 찾을 수 없습니다: userId={}", user.getUserId());
                            return new BusinessException(AuthErrorCode.USER_NOT_FOUND);
                        });

            case "ROLE_MANAGER":
                return managerRepository.findByUser(user)
                        .map(Manager::getManagerName)
                        .orElseThrow(() -> {
                            log.error("Manager를 찾을 수 없습니다: userId={}", user.getUserId());
                            return new BusinessException(AuthErrorCode.USER_NOT_FOUND);
                        });

            case "ROLE_CORPORATION":
                return corporationRepository.findByUserId(user.getId())
                        .map(Corporation::getCorpName)
                        .orElseThrow(() -> {
                            log.error("Corporation을 찾을 수 없습니다: userId={}", user.getUserId());
                            return new BusinessException(AuthErrorCode.USER_NOT_FOUND);
                        });

            default:
                log.warn("알 수 없는 역할: role={}", roleName);
                return user.getUserId();  // 기본값으로 userId 반환
        }
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

    /**
     * 문자열이 null이 아니고 비어있지 않은지 확인
     *
     * @param str 검증할 문자열
     * @return null이 아니고 비어있지 않으면 true
     */
    private static boolean isNotEmpty(String str) {
        return str != null && !str.trim().isEmpty();
    }
}
