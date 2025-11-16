package com.concrete.buildup.domain.site.service;

import com.concrete.buildup.domain.auth.entity.Corporation;
import com.concrete.buildup.domain.auth.entity.User;
import com.concrete.buildup.domain.auth.repository.CorporationRepository;
import com.concrete.buildup.domain.auth.repository.UserRepository;
import com.concrete.buildup.domain.site.dto.SiteCreateRequest;
import com.concrete.buildup.domain.site.dto.SiteCreateResponse;
import com.concrete.buildup.domain.site.entity.Site;
import com.concrete.buildup.domain.site.repository.SiteRepository;
import com.concrete.buildup.global.exception.BusinessException;
import com.concrete.buildup.global.exception.errorcode.AuthErrorCode;
import com.concrete.buildup.global.exception.errorcode.SiteErrorCode;
import com.concrete.buildup.global.util.SecurityUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * SiteService 단위 테스트
 *
 * @author Build-Up Team
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SiteService 테스트")
class SiteServiceTest {

    @Mock
    private SiteRepository siteRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CorporationRepository corporationRepository;

    @Mock
    private SecretKeyService secretKeyService;

    @InjectMocks
    private SiteService siteService;

    /**
     * BaseEntity의 id 필드를 Reflection으로 설정하는 헬퍼 메서드
     */
    private void setId(Object entity, Long id) throws Exception {
        var idField = entity.getClass().getSuperclass().getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(entity, id);
    }

    @Test
    @DisplayName("현장 등록 성공")
    void createSite_Success() throws Exception {
        // given
        String userId = "testuser";
        User mockUser = User.builder().userId(userId).build();
        setId(mockUser, 1L);

        Corporation mockCorporation = Corporation.builder()
                .corpName("CONCRETE")
                .build();
        setId(mockCorporation, 1L);

        SiteCreateRequest request = SiteCreateRequest.builder()
                .siteName("강남 재개발 현장")
                .siteAddress("서울시 강남구")
                .clientName("서울시청")
                .startDate(LocalDate.of(2025, 1, 1))
                .endDate(LocalDate.of(2025, 12, 31))
                .build();

        Site savedSite = Site.builder()
                .siteName(request.getSiteName())
                .siteAddress(request.getSiteAddress())
                .clientName(request.getClientName())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .corporation(mockCorporation)
                .managerSecretKey("CONC-1-ABC-1234")
                .employeeSecretKey("CONC-1-XYZ-5678")
                .build();
        setId(savedSite, 1L);

        SecretKeyService.SecretKeyPair mockKeyPair = new SecretKeyService.SecretKeyPair(
                "CONC-1-ABC-1234",
                "CONC-1-XYZ-5678"
        );

        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            securityUtil.when(SecurityUtil::getCurrentUserId).thenReturn(userId);
            given(userRepository.findByUserId(userId)).willReturn(Optional.of(mockUser));
            given(corporationRepository.findByUserId(1L)).willReturn(Optional.of(mockCorporation));
            given(siteRepository.save(any(Site.class))).willReturn(savedSite);
            given(secretKeyService.generateUniqueSecretKeys(anyString(), anyLong())).willReturn(mockKeyPair);

            // when
            SiteCreateResponse response = siteService.createSite(request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getSiteId()).isEqualTo(1L);
            assertThat(response.getSiteName()).isEqualTo("강남 재개발 현장");
            assertThat(response.getManagerSecretKey()).isEqualTo("CONC-1-ABC-1234");
            assertThat(response.getEmployeeSecretKey()).isEqualTo("CONC-1-XYZ-5678");

            verify(userRepository, times(1)).findByUserId(userId);
            verify(corporationRepository, times(1)).findByUserId(1L);
            verify(siteRepository, times(1)).save(any(Site.class));
            verify(secretKeyService, times(1)).generateUniqueSecretKeys("CONCRETE", 1L);
        }
    }

    @Test
    @DisplayName("현장 등록 실패 - 사용자 없음")
    void createSite_Fail_UserNotFound() {
        // given
        String userId = "nonexistentuser";
        SiteCreateRequest request = SiteCreateRequest.builder()
                .siteName("테스트 현장")
                .build();

        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            securityUtil.when(SecurityUtil::getCurrentUserId).thenReturn(userId);
            given(userRepository.findByUserId(userId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> siteService.createSite(request))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.USER_NOT_FOUND);

            verify(userRepository, times(1)).findByUserId(userId);
            verify(corporationRepository, never()).findByUserId(anyLong());
        }
    }

    @Test
    @DisplayName("현장 등록 실패 - 기업 정보 없음")
    void createSite_Fail_CorporationNotFound() throws Exception {
        // given
        String userId = "testuser";
        User mockUser = User.builder().userId(userId).build();
        setId(mockUser, 1L);

        SiteCreateRequest request = SiteCreateRequest.builder()
                .siteName("테스트 현장")
                .build();

        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            securityUtil.when(SecurityUtil::getCurrentUserId).thenReturn(userId);
            given(userRepository.findByUserId(userId)).willReturn(Optional.of(mockUser));
            given(corporationRepository.findByUserId(1L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> siteService.createSite(request))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", SiteErrorCode.CORPORATION_NOT_FOUND);

            verify(userRepository, times(1)).findByUserId(userId);
            verify(corporationRepository, times(1)).findByUserId(1L);
        }
    }

    @Test
    @DisplayName("현장 등록 실패 - 잘못된 날짜 범위 (종료일이 시작일보다 이전)")
    void createSite_Fail_InvalidDateRange() throws Exception {
        // given
        String userId = "testuser";
        User mockUser = User.builder().userId(userId).build();
        setId(mockUser, 1L);

        Corporation mockCorporation = Corporation.builder()
                .corpName("CONCRETE")
                .build();
        setId(mockCorporation, 1L);

        SiteCreateRequest request = SiteCreateRequest.builder()
                .siteName("테스트 현장")
                .startDate(LocalDate.of(2025, 12, 31))
                .endDate(LocalDate.of(2025, 1, 1))  // 종료일이 시작일보다 이전
                .build();

        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            securityUtil.when(SecurityUtil::getCurrentUserId).thenReturn(userId);
            given(userRepository.findByUserId(userId)).willReturn(Optional.of(mockUser));
            given(corporationRepository.findByUserId(1L)).willReturn(Optional.of(mockCorporation));

            // when & then
            assertThatThrownBy(() -> siteService.createSite(request))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", SiteErrorCode.INVALID_DATE_RANGE);

            verify(siteRepository, never()).save(any(Site.class));
        }
    }
}