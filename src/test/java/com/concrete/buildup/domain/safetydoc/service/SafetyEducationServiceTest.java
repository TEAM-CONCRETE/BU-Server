package com.concrete.buildup.domain.safetydoc.service;

import com.concrete.buildup.domain.auth.entity.Corporation;
import com.concrete.buildup.domain.auth.entity.Employee;
import com.concrete.buildup.domain.auth.entity.Manager;
import com.concrete.buildup.domain.auth.entity.User;
import com.concrete.buildup.domain.auth.repository.EmployeeRepository;
import com.concrete.buildup.domain.auth.repository.ManagerRepository;
import com.concrete.buildup.domain.auth.repository.UserRepository;
import com.concrete.buildup.domain.employee.repository.EmployeeQueryRepository;
import com.concrete.buildup.domain.safetydoc.dto.*;
import com.concrete.buildup.domain.safetydoc.entity.SafetyEducationAttendee;
import com.concrete.buildup.domain.safetydoc.entity.SafetyEducationLog;
import com.concrete.buildup.domain.safetydoc.enums.EducationType;
import com.concrete.buildup.domain.safetydoc.enums.SafetyEducationStatus;
import com.concrete.buildup.domain.safetydoc.repository.SafetyEducationAttendeeRepository;
import com.concrete.buildup.domain.safetydoc.repository.SafetyEducationLogRepository;
import com.concrete.buildup.domain.site.entity.Site;
import com.concrete.buildup.domain.site.repository.SiteRepository;
import com.concrete.buildup.domain.upload.service.S3Service;
import com.concrete.buildup.global.exception.BusinessException;
import com.concrete.buildup.global.exception.errorcode.SafetyDocErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * SafetyEducationService 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SafetyEducationService 테스트")
class SafetyEducationServiceTest {

    @Mock
    private SafetyEducationLogRepository safetyEducationLogRepository;

    @Mock
    private SafetyEducationAttendeeRepository attendeeRepository;

    @Mock
    private SiteRepository siteRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ManagerRepository managerRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private EmployeeQueryRepository employeeQueryRepository;

    @Mock
    private SafetyEducationPdfService pdfService;

    @Mock
    private S3Service s3Service;

    private SafetyEducationService safetyEducationService;

    // 테스트 데이터
    private Site site;
    private Manager manager;
    private User user;
    private Corporation corporation;
    private Employee employee1;
    private Employee employee2;

    @BeforeEach
    void setUp() {
        safetyEducationService = new SafetyEducationService(
                safetyEducationLogRepository,
                attendeeRepository,
                siteRepository,
                userRepository,
                managerRepository,
                employeeRepository,
                employeeQueryRepository,
                pdfService,
                Optional.of(s3Service)
        );

        // 테스트 데이터 초기화
        corporation = Corporation.builder().build();
        ReflectionTestUtils.setField(corporation, "id", 1L);

        user = User.builder()
                .userId("manager01")
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);

        manager = Manager.builder()
                .managerName("테스트관리자")
                .user(user)
                .build();
        ReflectionTestUtils.setField(manager, "id", 1L);

        site = Site.builder()
                .siteName("테스트현장")
                .manager(manager)
                .corporation(corporation)
                .build();
        ReflectionTestUtils.setField(site, "id", 1L);

        employee1 = Employee.builder()
                .empName("홍길동")
                .empType("PERMANENT")
                .build();
        ReflectionTestUtils.setField(employee1, "id", 100L);

        employee2 = Employee.builder()
                .empName("김철수")
                .empType("DAILY")
                .build();
        ReflectionTestUtils.setField(employee2, "id", 101L);
    }

    @Nested
    @DisplayName("안전교육일지 생성")
    class CreateSafetyEducationLog {

        @Test
        @DisplayName("성공 - 정상적인 안전교육일지 생성")
        void success() {
            // given
            Long siteId = 1L;
            String currentUserId = "manager01";
            CreateSafetyEducationLogRequest request = CreateSafetyEducationLogRequest.builder()
                    .educationType(EducationType.REGULAR)
                    .educationSubject("화재 예방 교육")
                    .educationContent("화재 발생 시 대피 요령")
                    .instructorName("안전담당자")
                    .educationLocation("현장 회의실")
                    .attendeeEmployeeIds(Arrays.asList(100L, 101L))
                    .build();

            given(siteRepository.findById(siteId)).willReturn(Optional.of(site));
            given(userRepository.findByUserId(currentUserId)).willReturn(Optional.of(user));
            given(managerRepository.findByUserId(user.getId())).willReturn(Optional.of(manager));
            given(employeeRepository.findAllById(request.getAttendeeEmployeeIds()))
                    .willReturn(Arrays.asList(employee1, employee2));
            given(pdfService.generateSafetyEducationPdf(any(), any(), any(), any()))
                    .willReturn("PDF_BYTES".getBytes());
            given(s3Service.getPdfUrl(anyString())).willReturn("https://bucket.s3.amazonaws.com/safety-docs/1/2025-01-24/SE-2025-01-24-1.pdf");
            given(safetyEducationLogRepository.countBySiteIdAndCreatedAtBetweenAndIsDeletedFalse(anyLong(), any(), any()))
                    .willReturn(0L);
            given(safetyEducationLogRepository.save(any(SafetyEducationLog.class)))
                    .willAnswer(invocation -> {
                        SafetyEducationLog log = invocation.getArgument(0);
                        ReflectionTestUtils.setField(log, "id", 1L);
                        return log;
                    });

            // when
            CreateSafetyEducationLogResponse response = safetyEducationService.createSafetyEducationLog(
                    siteId, request, currentUserId
            );

            // then
            assertThat(response).isNotNull();
            assertThat(response.getSafetyEducationLogId()).isEqualTo(1L);
            assertThat(response.getStatus()).isEqualTo(SafetyEducationStatus.MANAGER_SIGNING_PENDING);
            assertThat(response.getAttendeeCount()).isEqualTo(2);
            assertThat(response.getPdfUrl()).contains("safety-docs");

            verify(safetyEducationLogRepository, times(2)).save(any(SafetyEducationLog.class));
            verify(pdfService).generateSafetyEducationPdf(any(), any(), any(), any());
            verify(s3Service).uploadPdf(anyString(), any(byte[].class));
        }

        @Test
        @DisplayName("실패 - 현장이 존재하지 않음")
        void fail_siteNotFound() {
            // given
            Long siteId = 999L;
            String currentUserId = "manager01";
            CreateSafetyEducationLogRequest request = CreateSafetyEducationLogRequest.builder()
                    .educationType(EducationType.REGULAR)
                    .educationSubject("테스트")
                    .educationContent("내용")
                    .instructorName("강사")
                    .educationLocation("장소")
                    .attendeeEmployeeIds(Arrays.asList(100L))
                    .build();

            given(siteRepository.findById(siteId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> safetyEducationService.createSafetyEducationLog(siteId, request, currentUserId))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", SafetyDocErrorCode.SITE_NOT_FOUND);

            verify(safetyEducationLogRepository, never()).save(any());
        }

        @Test
        @DisplayName("실패 - 관리자 권한 없음")
        void fail_managerNotAuthorized() {
            // given
            Long siteId = 1L;
            String currentUserId = "manager02";

            // 다른 관리자 설정
            User otherUser = User.builder()
                    .userId("manager02")
                    .build();
            ReflectionTestUtils.setField(otherUser, "id", 2L);

            Manager otherManager = Manager.builder()
                    .managerName("다른관리자")
                    .user(otherUser)
                    .build();
            ReflectionTestUtils.setField(otherManager, "id", 2L);

            CreateSafetyEducationLogRequest request = CreateSafetyEducationLogRequest.builder()
                    .educationType(EducationType.REGULAR)
                    .educationSubject("테스트")
                    .educationContent("내용")
                    .instructorName("강사")
                    .educationLocation("장소")
                    .attendeeEmployeeIds(Arrays.asList(100L))
                    .build();

            given(siteRepository.findById(siteId)).willReturn(Optional.of(site));
            given(userRepository.findByUserId(currentUserId)).willReturn(Optional.of(otherUser));
            given(managerRepository.findByUserId(otherUser.getId())).willReturn(Optional.of(otherManager));

            // when & then
            assertThatThrownBy(() -> safetyEducationService.createSafetyEducationLog(siteId, request, currentUserId))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", SafetyDocErrorCode.MANAGER_NOT_AUTHORIZED);
        }

        @Test
        @DisplayName("실패 - 참석자 목록이 비어있음")
        void fail_emptyAttendeeList() {
            // given
            Long siteId = 1L;
            String currentUserId = "manager01";
            CreateSafetyEducationLogRequest request = CreateSafetyEducationLogRequest.builder()
                    .educationType(EducationType.REGULAR)
                    .educationSubject("테스트")
                    .educationContent("내용")
                    .instructorName("강사")
                    .educationLocation("장소")
                    .attendeeEmployeeIds(Arrays.asList(999L)) // 존재하지 않는 직원
                    .build();

            given(siteRepository.findById(siteId)).willReturn(Optional.of(site));
            given(userRepository.findByUserId(currentUserId)).willReturn(Optional.of(user));
            given(managerRepository.findByUserId(user.getId())).willReturn(Optional.of(manager));
            given(employeeRepository.findAllById(request.getAttendeeEmployeeIds()))
                    .willReturn(Collections.emptyList());

            // when & then
            assertThatThrownBy(() -> safetyEducationService.createSafetyEducationLog(siteId, request, currentUserId))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", SafetyDocErrorCode.EMPTY_ATTENDEE_LIST);
        }
    }

    @Nested
    @DisplayName("안전교육일지 목록 조회")
    class GetSafetyEducationLogs {

        @Test
        @DisplayName("성공 - 목록 조회")
        void success() {
            // given
            Long siteId = 1L;
            String currentUserId = "manager01";

            SafetyEducationLog log1 = SafetyEducationLog.builder()
                    .site(site)
                    .manager(manager)
                    .corporation(corporation)
                    .educationType(EducationType.REGULAR)
                    .educationSubject("교육1")
                    .educationContent("내용1")
                    .instructorName("강사1")
                    .educationLocation("장소1")
                    .status(SafetyEducationStatus.COMPLETED)
                    .build();
            ReflectionTestUtils.setField(log1, "id", 1L);

            SafetyEducationLog log2 = SafetyEducationLog.builder()
                    .site(site)
                    .manager(manager)
                    .corporation(corporation)
                    .educationType(EducationType.SPECIAL)
                    .educationSubject("교육2")
                    .educationContent("내용2")
                    .instructorName("강사2")
                    .educationLocation("장소2")
                    .status(SafetyEducationStatus.MANAGER_SIGNED)
                    .build();
            ReflectionTestUtils.setField(log2, "id", 2L);

            given(siteRepository.findById(siteId)).willReturn(Optional.of(site));
            given(userRepository.findByUserId(currentUserId)).willReturn(Optional.of(user));
            given(managerRepository.findByUserId(user.getId())).willReturn(Optional.of(manager));
            given(safetyEducationLogRepository.findBySiteIdAndIsDeletedFalseOrderByCreatedAtDesc(siteId))
                    .willReturn(Arrays.asList(log1, log2));
            given(attendeeRepository.countTotalAttendees(1L)).willReturn(5L);
            given(attendeeRepository.countSignedAttendees(1L)).willReturn(5L);
            given(attendeeRepository.countTotalAttendees(2L)).willReturn(3L);
            given(attendeeRepository.countSignedAttendees(2L)).willReturn(1L);

            // when
            List<SafetyEducationLogListResponse> result = safetyEducationService.getSafetyEducationLogs(siteId, currentUserId);

            // then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getId()).isEqualTo(1L);
            assertThat(result.get(0).getTotalAttendeeCount()).isEqualTo(5);
            assertThat(result.get(0).getSignedAttendeeCount()).isEqualTo(5);
            assertThat(result.get(1).getId()).isEqualTo(2L);
            assertThat(result.get(1).getTotalAttendeeCount()).isEqualTo(3);
            assertThat(result.get(1).getSignedAttendeeCount()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("참석자 서명 현황 조회")
    class GetAttendeeSignatureStatus {

        @Test
        @DisplayName("성공 - 서명 현황 조회")
        void success() {
            // given
            Long siteId = 1L;
            Long logId = 1L;
            String currentUserId = "manager01";

            SafetyEducationLog log = SafetyEducationLog.builder()
                    .site(site)
                    .manager(manager)
                    .corporation(corporation)
                    .educationType(EducationType.REGULAR)
                    .educationSubject("교육")
                    .educationContent("내용")
                    .instructorName("강사")
                    .educationLocation("장소")
                    .status(SafetyEducationStatus.MANAGER_SIGNED)
                    .build();
            ReflectionTestUtils.setField(log, "id", logId);

            SafetyEducationAttendee attendee1 = SafetyEducationAttendee.builder()
                    .employee(employee1)
                    .isSigned(true)
                    .build();

            SafetyEducationAttendee attendee2 = SafetyEducationAttendee.builder()
                    .employee(employee2)
                    .isSigned(false)
                    .build();

            given(siteRepository.findById(siteId)).willReturn(Optional.of(site));
            given(userRepository.findByUserId(currentUserId)).willReturn(Optional.of(user));
            given(managerRepository.findByUserId(user.getId())).willReturn(Optional.of(manager));
            given(safetyEducationLogRepository.findById(logId)).willReturn(Optional.of(log));
            given(attendeeRepository.findBySafetyEducationLogIdWithEmployee(logId))
                    .willReturn(Arrays.asList(attendee1, attendee2));

            // when
            AttendeeSignatureStatusResponse result = safetyEducationService.getAttendeeSignatureStatus(
                    siteId, logId, currentUserId
            );

            // then
            assertThat(result).isNotNull();
            assertThat(result.getSafetyEducationLogId()).isEqualTo(logId);
            assertThat(result.getTotalCount()).isEqualTo(2);
            assertThat(result.getSignedCount()).isEqualTo(1);
            assertThat(result.getUnsignedCount()).isEqualTo(1);
            assertThat(result.getAttendees()).hasSize(2);
        }

        @Test
        @DisplayName("실패 - 안전교육일지가 존재하지 않음")
        void fail_logNotFound() {
            // given
            Long siteId = 1L;
            Long logId = 999L;
            String currentUserId = "manager01";

            given(siteRepository.findById(siteId)).willReturn(Optional.of(site));
            given(userRepository.findByUserId(currentUserId)).willReturn(Optional.of(user));
            given(managerRepository.findByUserId(user.getId())).willReturn(Optional.of(manager));
            given(safetyEducationLogRepository.findById(logId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> safetyEducationService.getAttendeeSignatureStatus(siteId, logId, currentUserId))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", SafetyDocErrorCode.SAFETY_EDUCATION_LOG_NOT_FOUND);
        }
    }
}
