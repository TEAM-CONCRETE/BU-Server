package com.concrete.buildup.domain.auth.service;

import com.concrete.buildup.domain.auth.dto.LoginResult;
import com.concrete.buildup.domain.auth.dto.UserExistsResponse;
import com.concrete.buildup.domain.auth.dto.UserInfoResponse;
import com.concrete.buildup.domain.auth.entity.Corporation;
import com.concrete.buildup.domain.auth.entity.Employee;
import com.concrete.buildup.domain.auth.entity.Manager;
import com.concrete.buildup.domain.auth.entity.Role;
import com.concrete.buildup.domain.auth.entity.User;
import com.concrete.buildup.domain.auth.repository.CorporationRepository;
import com.concrete.buildup.domain.auth.repository.EmployeeRepository;
import com.concrete.buildup.domain.auth.repository.ManagerRepository;
import com.concrete.buildup.domain.auth.repository.UserRepository;
import com.concrete.buildup.domain.site.entity.Site;
import com.concrete.buildup.domain.site.repository.SiteRepository;
import com.concrete.buildup.global.exception.BusinessException;
import com.concrete.buildup.global.exception.errorcode.AuthErrorCode;
import com.concrete.buildup.global.util.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * AuthService 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 테스트")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private ManagerRepository managerRepository;

    @Mock
    private CorporationRepository corporationRepository;

    @Mock
    private SiteRepository siteRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        // AuthService의 private 필드 값 설정
        ReflectionTestUtils.setField(authService, "accessTokenExpiration", 3600000L);  // 1시간
        ReflectionTestUtils.setField(authService, "refreshTokenExpiration", 604800000L);  // 7일
    }

    /**
     * 토큰을 SHA-256으로 해시 처리 (테스트용 헬퍼 메서드)
     */
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Hash error", e);
        }
    }

    @Test
    @DisplayName("아이디 중복 확인 - 존재하는 아이디")
    void checkUserIdExists_ExistingId() {
        // given
        String userId = "existinguser";
        given(userRepository.existsByUserId(userId)).willReturn(true);

        // when
        UserExistsResponse response = authService.checkUserIdExists(userId);

        // then
        assertThat(response.isExists()).isTrue();
        verify(userRepository).existsByUserId(userId);
    }

    @Test
    @DisplayName("아이디 중복 확인 - 존재하지 않는 아이디 (사용 가능)")
    void checkUserIdExists_AvailableId() {
        // given
        String userId = "newuser";
        given(userRepository.existsByUserId(userId)).willReturn(false);

        // when
        UserExistsResponse response = authService.checkUserIdExists(userId);

        // then
        assertThat(response.isExists()).isFalse();
        verify(userRepository).existsByUserId(userId);
    }

    @Test
    @DisplayName("내 정보 조회 - 근로자(EMPLOYEE)")
    void getMyInfo_Employee() {
        // given
        String userId = "employee001";
        String secretKey = "test-secret-key";

        Role employeeRole = Role.builder()
                .roleName("ROLE_EMPLOYEE")
                .build();

        User user = User.builder()
                .userId(userId)
                .phone("010-1234-5678")
                .email("employee@test.com")
                .role(employeeRole)
                .secretKey(secretKey)
                .build();

        Employee employee = Employee.builder()
                .user(user)
                .empName("홍길동")
                .residentNum("950101-*******")
                .subPhone("010-9876-5432")
                .empAddress("서울시 강남구")
                .empType("DAILY")
                .build();

        Site site = Site.builder()
                .siteName("OO아파트 신축공사")
                .siteAddress("서울시 강남구")
                .build();

        given(userRepository.findByUserIdWithRole(userId)).willReturn(Optional.of(user));
        given(employeeRepository.findByUser(user)).willReturn(Optional.of(employee));
        given(siteRepository.findByEmployeeSecretKey(secretKey)).willReturn(Optional.of(site));

        // when
        UserInfoResponse response = authService.getMyInfo(userId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getRole()).isEqualTo("ROLE_EMPLOYEE");
        assertThat(response.getName()).isEqualTo("홍길동");
        assertThat(response.getPhone()).isEqualTo("010-1234-5678");
        assertThat(response.getEmail()).isEqualTo("employee@test.com");

        UserInfoResponse.EmployeeInfo empInfo = (UserInfoResponse.EmployeeInfo) response.getAdditionalInfo();
        assertThat(empInfo).isNotNull();
        assertThat(empInfo.getEmpName()).isEqualTo("홍길동");
        assertThat(empInfo.getEmpAddress()).isEqualTo("서울시 강남구");
        assertThat(empInfo.getEmpType()).isEqualTo("DAILY");
        assertThat(empInfo.getSite()).isNotNull();
        assertThat(empInfo.getSite().getSiteName()).isEqualTo("OO아파트 신축공사");

        verify(userRepository).findByUserIdWithRole(userId);
        verify(employeeRepository).findByUser(user);
        verify(siteRepository).findByEmployeeSecretKey(secretKey);
    }

    @Test
    @DisplayName("내 정보 조회 - 현장 관리자(MANAGER)")
    void getMyInfo_Manager() {
        // given
        String userId = "manager001";
        String secretKey = "test-manager-key";

        Role managerRole = Role.builder()
                .roleName("ROLE_MANAGER")
                .build();

        User user = User.builder()
                .userId(userId)
                .phone("010-1111-2222")
                .email("manager@test.com")
                .role(managerRole)
                .secretKey(secretKey)
                .build();

        Manager manager = Manager.builder()
                .user(user)
                .managerName("김관리")
                .build();

        Site site = Site.builder()
                .siteName("OO현장")
                .siteAddress("서울시 강남구")
                .build();

        given(userRepository.findByUserIdWithRole(userId)).willReturn(Optional.of(user));
        given(managerRepository.findByUser(user)).willReturn(Optional.of(manager));
        given(siteRepository.findByManagerSecretKey(secretKey)).willReturn(Optional.of(site));

        // when
        UserInfoResponse response = authService.getMyInfo(userId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getRole()).isEqualTo("ROLE_MANAGER");
        assertThat(response.getName()).isEqualTo("김관리");

        UserInfoResponse.ManagerInfo mgrInfo = (UserInfoResponse.ManagerInfo) response.getAdditionalInfo();
        assertThat(mgrInfo).isNotNull();
        assertThat(mgrInfo.getManagerName()).isEqualTo("김관리");
        assertThat(mgrInfo.getSite()).isNotNull();
        assertThat(mgrInfo.getSite().getSiteName()).isEqualTo("OO현장");

        verify(userRepository).findByUserIdWithRole(userId);
        verify(managerRepository).findByUser(user);
        verify(siteRepository).findByManagerSecretKey(secretKey);
    }

    @Test
    @DisplayName("내 정보 조회 - 기업(CORPORATION)")
    void getMyInfo_Corporation() {
        // given
        String userId = "corp001";

        Role corpRole = Role.builder()
                .roleName("ROLE_CORPORATION")
                .build();

        // User 엔티티 생성 (id는 BaseEntity에서 관리되므로 직접 설정하지 않음)
        User user = User.builder()
                .userId(userId)
                .phone("02-1234-5678")
                .email("corp@test.com")
                .role(corpRole)
                .build();

        Corporation corporation = Corporation.builder()
                .user(user)
                .corpName("주식회사 건설")
                .corpAddress("서울시 강남구")
                .corpCeoName("이대표")
                .build();

        given(userRepository.findByUserIdWithRole(userId)).willReturn(Optional.of(user));
        given(corporationRepository.findByUserId(any())).willReturn(Optional.of(corporation));

        // when
        UserInfoResponse response = authService.getMyInfo(userId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getRole()).isEqualTo("ROLE_CORPORATION");
        assertThat(response.getName()).isEqualTo("주식회사 건설");

        UserInfoResponse.CorporationInfo corpInfo = (UserInfoResponse.CorporationInfo) response.getAdditionalInfo();
        assertThat(corpInfo).isNotNull();
        assertThat(corpInfo.getCorpName()).isEqualTo("주식회사 건설");
        assertThat(corpInfo.getCorpAddress()).isEqualTo("서울시 강남구");
        assertThat(corpInfo.getCorpCeoName()).isEqualTo("이대표");

        verify(userRepository).findByUserIdWithRole(userId);
        verify(corporationRepository).findByUserId(any());
    }

    @Test
    @DisplayName("내 정보 조회 - 사용자를 찾을 수 없음")
    void getMyInfo_UserNotFound() {
        // given
        String userId = "nonexistent";
        given(userRepository.findByUserIdWithRole(userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> authService.getMyInfo(userId))
                .isInstanceOf(BusinessException.class);

        verify(userRepository).findByUserIdWithRole(userId);
    }

    @Test
    @DisplayName("토큰 갱신 - 정상 갱신 성공")
    void refreshToken_Success() {
        // given
        String refreshToken = "valid-refresh-token";
        String userId = "testuser";
        String hashedRefreshToken = hashToken(refreshToken);  // 실제 해시 값 사용
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);

        Role role = Role.builder()
                .roleName("EMPLOYEE")
                .build();

        User user = User.builder()
                .userId(userId)
                .role(role)
                .refreshToken(hashedRefreshToken)
                .refreshTokenExpiresAt(expiresAt)
                .build();

        given(jwtTokenProvider.validateToken(refreshToken)).willReturn(true);
        given(jwtTokenProvider.getTokenType(refreshToken)).willReturn("refresh");
        given(jwtTokenProvider.getUserIdFromToken(refreshToken)).willReturn(userId);
        given(userRepository.findByUserIdWithRole(userId)).willReturn(Optional.of(user));
        given(jwtTokenProvider.generateAccessToken(userId, "EMPLOYEE")).willReturn("new-access-token");
        given(jwtTokenProvider.generateRefreshToken(userId, false)).willReturn("new-refresh-token");

        // when
        LoginResult result = authService.refreshToken(refreshToken);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getLoginResponse()).isNotNull();
        assertThat(result.getLoginResponse().getAccessToken()).isEqualTo("new-access-token");
        assertThat(result.getLoginResponse().getUserId()).isEqualTo(userId);
        assertThat(result.getLoginResponse().getRole()).isEqualTo("EMPLOYEE");
        assertThat(result.getRefreshToken()).isEqualTo("new-refresh-token");

        verify(jwtTokenProvider).validateToken(refreshToken);
        verify(jwtTokenProvider).getTokenType(refreshToken);
        verify(jwtTokenProvider).getUserIdFromToken(refreshToken);
        verify(userRepository).findByUserIdWithRole(userId);
        verify(jwtTokenProvider).generateAccessToken(userId, "EMPLOYEE");
        verify(jwtTokenProvider).generateRefreshToken(userId, false);
    }

    @Test
    @DisplayName("토큰 갱신 - 유효하지 않은 토큰")
    void refreshToken_InvalidToken() {
        // given
        String invalidToken = "invalid-token";
        given(jwtTokenProvider.validateToken(invalidToken)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> authService.refreshToken(invalidToken))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.INVALID_REFRESH_TOKEN);

        verify(jwtTokenProvider).validateToken(invalidToken);
    }

    @Test
    @DisplayName("토큰 갱신 - Refresh Token이 아닌 Access Token 사용")
    void refreshToken_NotRefreshToken() {
        // given
        String accessToken = "access-token";
        given(jwtTokenProvider.validateToken(accessToken)).willReturn(true);
        given(jwtTokenProvider.getTokenType(accessToken)).willReturn("access");

        // when & then
        assertThatThrownBy(() -> authService.refreshToken(accessToken))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.INVALID_REFRESH_TOKEN);

        verify(jwtTokenProvider).validateToken(accessToken);
        verify(jwtTokenProvider).getTokenType(accessToken);
    }

    @Test
    @DisplayName("토큰 갱신 - 사용자를 찾을 수 없음")
    void refreshToken_UserNotFound() {
        // given
        String refreshToken = "valid-refresh-token";
        String userId = "nonexistent";

        given(jwtTokenProvider.validateToken(refreshToken)).willReturn(true);
        given(jwtTokenProvider.getTokenType(refreshToken)).willReturn("refresh");
        given(jwtTokenProvider.getUserIdFromToken(refreshToken)).willReturn(userId);
        given(userRepository.findByUserIdWithRole(userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> authService.refreshToken(refreshToken))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.USER_NOT_FOUND);

        verify(jwtTokenProvider).validateToken(refreshToken);
        verify(jwtTokenProvider).getTokenType(refreshToken);
        verify(jwtTokenProvider).getUserIdFromToken(refreshToken);
        verify(userRepository).findByUserIdWithRole(userId);
    }

    @Test
    @DisplayName("토큰 갱신 - Refresh Token 불일치")
    void refreshToken_TokenMismatch() {
        // given
        String refreshToken = "valid-refresh-token";
        String userId = "testuser";
        String differentHashedToken = "different-hashed-token";
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);

        Role role = Role.builder()
                .roleName("EMPLOYEE")
                .build();

        User user = User.builder()
                .userId(userId)
                .role(role)
                .refreshToken(differentHashedToken)
                .refreshTokenExpiresAt(expiresAt)
                .build();

        given(jwtTokenProvider.validateToken(refreshToken)).willReturn(true);
        given(jwtTokenProvider.getTokenType(refreshToken)).willReturn("refresh");
        given(jwtTokenProvider.getUserIdFromToken(refreshToken)).willReturn(userId);
        given(userRepository.findByUserIdWithRole(userId)).willReturn(Optional.of(user));

        // when & then
        assertThatThrownBy(() -> authService.refreshToken(refreshToken))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.REFRESH_TOKEN_MISMATCH);

        verify(jwtTokenProvider).validateToken(refreshToken);
        verify(jwtTokenProvider).getTokenType(refreshToken);
        verify(jwtTokenProvider).getUserIdFromToken(refreshToken);
        verify(userRepository).findByUserIdWithRole(userId);
    }

    @Test
    @DisplayName("토큰 갱신 - 만료된 Refresh Token")
    void refreshToken_ExpiredToken() {
        // given
        String refreshToken = "valid-refresh-token";
        String userId = "testuser";
        String hashedRefreshToken = hashToken(refreshToken);  // 실제 해시 값 사용
        LocalDateTime expiredTime = LocalDateTime.now().minusDays(1);  // 이미 만료됨

        Role role = Role.builder()
                .roleName("EMPLOYEE")
                .build();

        User user = User.builder()
                .userId(userId)
                .role(role)
                .refreshToken(hashedRefreshToken)
                .refreshTokenExpiresAt(expiredTime)
                .build();

        given(jwtTokenProvider.validateToken(refreshToken)).willReturn(true);
        given(jwtTokenProvider.getTokenType(refreshToken)).willReturn("refresh");
        given(jwtTokenProvider.getUserIdFromToken(refreshToken)).willReturn(userId);
        given(userRepository.findByUserIdWithRole(userId)).willReturn(Optional.of(user));

        // when & then
        assertThatThrownBy(() -> authService.refreshToken(refreshToken))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.REFRESH_TOKEN_EXPIRED);

        verify(jwtTokenProvider).validateToken(refreshToken);
        verify(jwtTokenProvider).getTokenType(refreshToken);
        verify(jwtTokenProvider).getUserIdFromToken(refreshToken);
        verify(userRepository).findByUserIdWithRole(userId);
    }
}
