package com.concrete.buildup.domain.site.service;

import com.concrete.buildup.domain.auth.entity.Corporation;
import com.concrete.buildup.domain.auth.entity.User;
import com.concrete.buildup.domain.auth.repository.CorporationRepository;
import com.concrete.buildup.domain.auth.repository.UserRepository;
import com.concrete.buildup.domain.safetydoc.entity.SafetyEducationLog;
import com.concrete.buildup.domain.safetydoc.enums.EducationType;
import com.concrete.buildup.domain.safetydoc.enums.SafetyEducationStatus;
import com.concrete.buildup.domain.safetydoc.repository.SafetyEducationLogRepository;
import com.concrete.buildup.domain.site.dto.SafetyWorkDocumentListResponse;
import com.concrete.buildup.domain.site.dto.SiteCreateRequest;
import com.concrete.buildup.domain.site.dto.SiteCreateResponse;
import com.concrete.buildup.domain.site.entity.Site;
import com.concrete.buildup.domain.site.repository.SiteRepository;
import com.concrete.buildup.domain.workreport.entity.WorkReport;
import com.concrete.buildup.domain.workreport.repository.WorkReportRepository;
import com.concrete.buildup.global.exception.BusinessException;
import com.concrete.buildup.global.exception.errorcode.AuthErrorCode;
import com.concrete.buildup.global.exception.errorcode.SiteErrorCode;
import com.concrete.buildup.global.util.SecurityUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
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

    @Mock
    private SafetyEducationLogRepository safetyEducationLogRepository;

    @Mock
    private WorkReportRepository workReportRepository;

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

    @Nested
    @DisplayName("안전/작업 문서 목록 조회")
    class GetSafetyWorkDocuments {

        @Test
        @DisplayName("성공 - 안전교육일지와 작업일보가 모두 있는 날짜")
        void success_bothDocumentsExist() throws Exception {
            // given
            Long siteId = 1L;
            Pageable pageable = PageRequest.of(0, 20);
            LocalDate date1 = LocalDate.of(2025, 11, 24);

            Corporation corporation = Corporation.builder().corpName("Test Corp").build();
            setId(corporation, 1L);

            Site site = Site.builder()
                    .siteName("테스트 현장")
                    .corporation(corporation)
                    .build();
            setId(site, siteId);

            SafetyEducationLog safetyLog = SafetyEducationLog.builder()
                    .site(site)
                    .educationType(EducationType.REGULAR)
                    .educationSubject("추락 재해 예방")
                    .educationContent("테스트")
                    .instructorName("김안전")
                    .educationLocation("현장")
                    .status(SafetyEducationStatus.COMPLETED)
                    .corporation(corporation)
                    .build();
            setId(safetyLog, 1L);

            WorkReport workReport = WorkReport.builder()
                    .site(site)
                    .workSections("[{\"sectionName\":\"철근\"}]")
                    .corporation(corporation)
                    .build();
            setId(workReport, 1L);

            given(siteRepository.existsById(siteId)).willReturn(true);
            given(safetyEducationLogRepository.findDistinctDatesBySiteId(siteId)).willReturn(List.of(date1));
            given(workReportRepository.findDistinctDatesBySiteId(siteId)).willReturn(List.of(date1));
            given(safetyEducationLogRepository.findBySiteIdAndDate(siteId, date1)).willReturn(List.of(safetyLog));
            given(workReportRepository.findBySiteIdAndDate(siteId, date1)).willReturn(List.of(workReport));

            // when
            SafetyWorkDocumentListResponse response = siteService.getSafetyWorkDocuments(siteId, pageable);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getContent()).hasSize(1);
            assertThat(response.getContent().get(0).getDate()).isEqualTo(date1);
            assertThat(response.getContent().get(0).getSafetyEducationLog()).isNotNull();
            assertThat(response.getContent().get(0).getSafetyEducationLog().getLogId()).isEqualTo(1L);
            assertThat(response.getContent().get(0).getWorkReport()).isNotNull();
            assertThat(response.getContent().get(0).getWorkReport().getWorkReportId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("성공 - 안전교육일지만 있는 날짜")
        void success_onlySafetyLog() throws Exception {
            // given
            Long siteId = 1L;
            Pageable pageable = PageRequest.of(0, 20);
            LocalDate date1 = LocalDate.of(2025, 11, 24);

            Corporation corporation = Corporation.builder().corpName("Test Corp").build();
            setId(corporation, 1L);

            Site site = Site.builder()
                    .siteName("테스트 현장")
                    .corporation(corporation)
                    .build();
            setId(site, siteId);

            SafetyEducationLog safetyLog = SafetyEducationLog.builder()
                    .site(site)
                    .educationType(EducationType.REGULAR)
                    .educationSubject("추락 재해 예방")
                    .educationContent("테스트")
                    .instructorName("김안전")
                    .educationLocation("현장")
                    .status(SafetyEducationStatus.COMPLETED)
                    .corporation(corporation)
                    .build();
            setId(safetyLog, 1L);

            given(siteRepository.existsById(siteId)).willReturn(true);
            given(safetyEducationLogRepository.findDistinctDatesBySiteId(siteId)).willReturn(List.of(date1));
            given(workReportRepository.findDistinctDatesBySiteId(siteId)).willReturn(List.of());
            given(safetyEducationLogRepository.findBySiteIdAndDate(siteId, date1)).willReturn(List.of(safetyLog));
            given(workReportRepository.findBySiteIdAndDate(siteId, date1)).willReturn(List.of());

            // when
            SafetyWorkDocumentListResponse response = siteService.getSafetyWorkDocuments(siteId, pageable);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getContent()).hasSize(1);
            assertThat(response.getContent().get(0).getSafetyEducationLog()).isNotNull();
            assertThat(response.getContent().get(0).getWorkReport()).isNull();
        }

        @Test
        @DisplayName("실패 - 현장이 존재하지 않음")
        void fail_siteNotFound() {
            // given
            Long siteId = 999L;
            Pageable pageable = PageRequest.of(0, 20);

            given(siteRepository.existsById(siteId)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> siteService.getSafetyWorkDocuments(siteId, pageable))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", SiteErrorCode.SITE_NOT_FOUND);
        }

        @Test
        @DisplayName("성공 - 문서가 없는 경우 빈 목록 반환")
        void success_noDocuments() {
            // given
            Long siteId = 1L;
            Pageable pageable = PageRequest.of(0, 20);

            given(siteRepository.existsById(siteId)).willReturn(true);
            given(safetyEducationLogRepository.findDistinctDatesBySiteId(siteId)).willReturn(List.of());
            given(workReportRepository.findDistinctDatesBySiteId(siteId)).willReturn(List.of());

            // when
            SafetyWorkDocumentListResponse response = siteService.getSafetyWorkDocuments(siteId, pageable);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getContent()).isEmpty();
            assertThat(response.getTotalElements()).isZero();
        }
    }
}