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
 * Spring Security와 JWT 필터를 포함한 핵심 플로우를 테스트합니다.
 *
 * 테스트 전략:
 * - 통합 테스트: 핵심 E2E 플로우만 (회원가입→로그인→API 호출, Token 재발급)
 * - 단위 테스트: AuthServiceTest에서 비즈니스 로직 검증
 * - 슬라이스 테스트: AuthControllerTest에서 Controller 레이어 검증
 */
@SpringBootTest(properties = {
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("인증/인가 통합 테스트 - 핵심 플로우")
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
    @DisplayName("Refresh Token으로 Access Token 재발급 플로우")
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
}
