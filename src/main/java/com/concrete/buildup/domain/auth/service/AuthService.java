package com.concrete.buildup.domain.auth.service;

import com.concrete.buildup.domain.auth.dto.EmployeeSignUpRequest;
import com.concrete.buildup.domain.auth.dto.SignUpResponse;
import com.concrete.buildup.domain.auth.dto.UserExistsResponse;
import com.concrete.buildup.domain.auth.entity.Employee;
import com.concrete.buildup.domain.auth.entity.Role;
import com.concrete.buildup.domain.auth.entity.User;
import com.concrete.buildup.domain.auth.repository.EmployeeRepository;
import com.concrete.buildup.domain.auth.repository.RoleRepository;
import com.concrete.buildup.domain.auth.repository.UserRepository;
import com.concrete.buildup.global.exception.BusinessException;
import com.concrete.buildup.global.exception.errorcode.AuthErrorCode;
import com.concrete.buildup.global.util.AesEncryptionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AesEncryptionUtil aesEncryptionUtil;

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
     * 회원가입 응답 생성
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
}
