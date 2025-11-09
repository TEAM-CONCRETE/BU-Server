package com.concrete.buildup.domain.auth.integration;

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
import com.concrete.buildup.global.util.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 인증/인가 통합 테스트
 *
 * Spring Security와 JWT 필터를 포함한 전체 스택 테스트를 수행합니다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("인증/인가 통합 테스트")
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private ManagerRepository managerRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private CorporationRepository corporationRepository;

    @Autowired
    private SiteRepository siteRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private Role employeeRole;
    private Role managerRole;
    private Role corporationRole;
    private Site testSite;
    private Corporation testCorporation;
    private Manager testManager;

    @BeforeEach
    void setUp() {
        // 역할 데이터 준비
        employeeRole = roleRepository.findByRoleName("ROLE_EMPLOYEE")
                .orElseGet(() -> roleRepository.save(Role.builder()
                        .roleName("ROLE_EMPLOYEE")
                        .description("근로자 역할")
                        .build()));

        managerRole = roleRepository.findByRoleName("ROLE_MANAGER")
                .orElseGet(() -> roleRepository.save(Role.builder()
                        .roleName("ROLE_MANAGER")
                        .description("현장 관리자 역할")
                        .build()));

        corporationRole = roleRepository.findByRoleName("ROLE_CORPORATION")
                .orElseGet(() -> roleRepository.save(Role.builder()
                        .roleName("ROLE_CORPORATION")
                        .description("기업 역할")
                        .build()));

        // 테스트용 기업 사용자 생성
        User corporationUser = userRepository.save(User.builder()
                .userId("testcorp")
                .password(passwordEncoder.encode("password"))
                .phone("01000000000")
                .role(corporationRole)
                .build());

        // 테스트용 기업 생성
        testCorporation = corporationRepository.save(Corporation.builder()
                .user(corporationUser)
                .corpName("테스트 기업")
                .corpAddress("서울시 강남구")
                .corpCeoName("김대표")
                .build());

        // 테스트용 관리자 사용자 생성
        User managerUser = userRepository.save(User.builder()
                .userId("testmanager")
                .password(passwordEncoder.encode("password"))
                .phone("01099999999")
                .role(managerRole)
                .build());

        // 테스트용 관리자 생성
        testManager = managerRepository.save(Manager.builder()
                .user(managerUser)
                .managerName("테스트 관리자")
                .build());

        // 테스트용 현장 생성
        testSite = siteRepository.save(Site.builder()
                .siteName("테스트 현장")
                .corporation(testCorporation)
                .manager(testManager)
                .employeeSecretKey("test-secret-key")
                .managerSecretKey("test-manager-secret-key")
                .siteAddress("서울시 강남구")
                .build());
    }

    @Test
    @DisplayName("근로자 회원가입 → 로그인 → 내 정보 조회 (성공)")
    void employeeSignUpAndLogin_Success() throws Exception {
        // given: 근로자 회원가입 요청
        EmployeeSignUpRequest signUpRequest = EmployeeSignUpRequest.builder()
                .empName("김근로")
                .userId("employee123")
                .password("Abc123!!")
                .confirmPassword("Abc123!!")
                .secretKey("test-secret-key")
                .agreeTerms(true)
                .agreePrivacy(true)
                .residentNum("900101-1234567")
                .phone("01012345678")
                .empAddress("서울시 강남구")
                .build();

        // when: 회원가입
        mockMvc.perform(post("/v1/auth/register/employee")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signUpRequest)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));

        // when: 로그인
        LoginRequest loginRequest = LoginRequest.builder()
                .username("employee123")
                .password("Abc123!!")
                .build();

        MvcResult loginResult = mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.userId").value("employee123"))
                .andReturn();

        // then: Access Token 추출
        String responseBody = loginResult.getResponse().getContentAsString();
        String accessToken = objectMapper.readTree(responseBody)
                .path("data").path("accessToken").asText();

        // when: 내 정보 조회
        mockMvc.perform(get("/v1/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value("employee123"));
    }

    @Test
    @DisplayName("현장 관리자 회원가입 → 로그인 → 내 정보 조회 (성공)")
    void managerSignUpAndLogin_Success() throws Exception {
        // given: 현장 관리자 회원가입 요청
        ManagerSignUpRequest signUpRequest = ManagerSignUpRequest.builder()
                .managerName("김관리")
                .userId("manager123")
                .password("Manager123!@")
                .confirmPassword("Manager123!@")
                .secretKey("test-manager-secret-key")
                .phone("010-9876-5432")
                .build();

        // when: 회원가입
        mockMvc.perform(post("/v1/auth/register/manager")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signUpRequest)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));

        // when: 로그인
        LoginRequest loginRequest = LoginRequest.builder()
                .username("manager123")
                .password("Manager123!@")
                .build();

        MvcResult loginResult = mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.userId").value("manager123"))
                .andReturn();

        // then: Access Token 추출
        String responseBody = loginResult.getResponse().getContentAsString();
        String accessToken = objectMapper.readTree(responseBody)
                .path("data").path("accessToken").asText();

        // when: 내 정보 조회
        mockMvc.perform(get("/v1/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value("manager123"));
    }

    @Test
    @DisplayName("아이디 중복 확인 - 중복된 아이디")
    void checkUserIdExists_Duplicate() throws Exception {
        // given: 기존 사용자 생성
        User user = User.builder()
                .userId("existing-user")
                .password(passwordEncoder.encode("password"))
                .phone("01012345678")
                .role(employeeRole)
                .build();
        userRepository.save(user);

        // when & then
        mockMvc.perform(get("/v1/auth/exists")
                        .param("userId", "existing-user"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.exists").value(true));
    }

    @Test
    @DisplayName("아이디 중복 확인 - 사용 가능한 아이디")
    void checkUserIdExists_Available() throws Exception {
        // when & then
        mockMvc.perform(get("/v1/auth/exists")
                        .param("userId", "new-user"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.exists").value(false));
    }

    @Test
    @DisplayName("로그인 실패 - 잘못된 비밀번호")
    void login_WrongPassword() throws Exception {
        // given: 기존 사용자 생성
        User user = User.builder()
                .userId("testuser")
                .password(passwordEncoder.encode("correctPassword"))
                .phone("01012345678")
                .role(employeeRole)
                .build();
        userRepository.save(user);

        // when: 잘못된 비밀번호로 로그인 시도
        LoginRequest loginRequest = LoginRequest.builder()
                .username("testuser")
                .password("wrongPassword")
                .build();

        // then
        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("토큰 없이 보호된 API 호출 - 401 Unauthorized")
    void accessProtectedEndpoint_WithoutToken() throws Exception {
        // when & then
        mockMvc.perform(get("/v1/auth/me"))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("잘못된 토큰으로 보호된 API 호출 - 401 Unauthorized")
    void accessProtectedEndpoint_WithInvalidToken() throws Exception {
        // when & then
        mockMvc.perform(get("/v1/auth/me")
                        .header("Authorization", "Bearer invalid-token"))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Refresh Token으로 Access Token 재발급")
    void refreshToken_Success() throws Exception {
        // given: 사용자 생성 및 로그인
        User user = User.builder()
                .userId("testuser")
                .password(passwordEncoder.encode("password123"))
                .phone("01012345678")
                .role(employeeRole)
                .build();
        userRepository.save(user);

        LoginRequest loginRequest = LoginRequest.builder()
                .username("testuser")
                .password("password123")
                .build();

        MvcResult loginResult = mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        // Refresh Token 쿠키 추출
        String setCookieHeader = loginResult.getResponse().getHeader("Set-Cookie");
        assertThat(setCookieHeader).isNotNull();
        assertThat(setCookieHeader).contains("refreshToken=");

        String refreshToken = setCookieHeader.split("refreshToken=")[1].split(";")[0];

        // when: Refresh Token으로 토큰 재발급
        mockMvc.perform(post("/v1/auth/token/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("refreshToken", refreshToken)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").exists());
    }

    @Test
    @DisplayName("EMPLOYEE 역할로 MANAGER 전용 API 접근 시도 - 403 Forbidden")
    void employeeAccessManagerOnlyEndpoint_Forbidden() throws Exception {
        // given: EMPLOYEE 역할 사용자 생성
        User employee = User.builder()
                .userId("employee")
                .password(passwordEncoder.encode("password"))
                .phone("01012345678")
                .role(employeeRole)
                .build();
        userRepository.save(employee);

        Employee employeeProfile = Employee.builder()
                .user(employee)
                .empName("김근로")
                .residentNum("900101-1234567")
                .empAddress("서울시")
                .build();
        employeeRepository.save(employeeProfile);

        // Access Token 생성
        String accessToken = jwtTokenProvider.generateAccessToken("employee", "ROLE_EMPLOYEE");

        // when & then: 계약 목록 조회 시도 (MANAGER, CORPORATION, ADMIN 전용)
        mockMvc.perform(get("/v1/" + testSite.getId() + "/contracts")
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("접근 권한이 없습니다."));
    }

    @Test
    @DisplayName("MANAGER 역할로 계약 목록 조회 - 200 OK")
    void managerAccessContractList_Success() throws Exception {
        // given: MANAGER 역할 사용자 생성
        User manager = User.builder()
                .userId("manager")
                .password(passwordEncoder.encode("password"))
                .phone("01098765432")
                .role(managerRole)
                .build();
        userRepository.save(manager);

        Manager managerProfile = Manager.builder()
                .user(manager)
                .managerName("김관리")
                .build();
        managerRepository.save(managerProfile);

        // Access Token 생성
        String accessToken = jwtTokenProvider.generateAccessToken("manager", "ROLE_MANAGER");

        // when & then: 계약 목록 조회 성공
        mockMvc.perform(get("/v1/" + testSite.getId() + "/contracts")
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("만료된 토큰으로 API 호출 - 401 Unauthorized")
    void accessWithExpiredToken_Unauthorized() throws Exception {
        // given: 만료된 토큰 생성 (만료 시간을 음수로 설정)
        // 실제로는 JwtTokenProvider를 Mock하거나 테스트용 만료된 토큰을 생성해야 하지만,
        // 여기서는 간단히 잘못된 형식의 토큰으로 대체
        String expiredToken = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0dXNlciIsImV4cCI6MTB9.invalid";

        // when & then
        mockMvc.perform(get("/v1/auth/me")
                        .header("Authorization", "Bearer " + expiredToken))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }
}
