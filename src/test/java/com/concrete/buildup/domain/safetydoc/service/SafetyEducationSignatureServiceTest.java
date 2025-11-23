package com.concrete.buildup.domain.safetydoc.service;

import com.concrete.buildup.domain.auth.entity.Corporation;
import com.concrete.buildup.domain.auth.entity.Employee;
import com.concrete.buildup.domain.auth.entity.Manager;
import com.concrete.buildup.domain.auth.entity.User;
import com.concrete.buildup.domain.auth.repository.ManagerRepository;
import com.concrete.buildup.domain.auth.repository.UserRepository;
import com.concrete.buildup.domain.contract.enums.SignerRole;
import com.concrete.buildup.domain.contract.service.PdfGenerationService;
import com.concrete.buildup.domain.safetydoc.dto.SafetyEducationSignatureRequest;
import com.concrete.buildup.domain.safetydoc.dto.SafetyEducationSignatureResponse;
import com.concrete.buildup.domain.safetydoc.entity.SafetyEducationAttendee;
import com.concrete.buildup.domain.safetydoc.entity.SafetyEducationLog;
import com.concrete.buildup.domain.safetydoc.enums.EducationType;
import com.concrete.buildup.domain.safetydoc.enums.SafetyEducationStatus;
import com.concrete.buildup.domain.safetydoc.repository.SafetyEducationAttendeeRepository;
import com.concrete.buildup.domain.safetydoc.repository.SafetyEducationLogRepository;
import com.concrete.buildup.domain.safetydoc.repository.SafetyEducationSignLogRepository;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * SafetyEducationSignatureService 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SafetyEducationSignatureService 테스트")
class SafetyEducationSignatureServiceTest {

    @Mock
    private SafetyEducationLogRepository safetyEducationLogRepository;

    @Mock
    private SafetyEducationAttendeeRepository attendeeRepository;

    @Mock
    private SafetyEducationSignLogRepository signLogRepository;

    @Mock
    private SiteRepository siteRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ManagerRepository managerRepository;

    @Mock
    private S3Service s3Service;

    @Mock
    private PdfGenerationService pdfGenerationService;

    private SafetyEducationSignatureService signatureService;

    // 테스트 데이터
    private Site site;
    private Manager manager;
    private User user;
    private Corporation corporation;
    private Employee employee;
    private SafetyEducationLog safetyEducationLog;

    @BeforeEach
    void setUp() {
        signatureService = new SafetyEducationSignatureService(
                safetyEducationLogRepository,
                attendeeRepository,
                signLogRepository,
                siteRepository,
                userRepository,
                managerRepository,
                Optional.of(s3Service),
                pdfGenerationService
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

        employee = Employee.builder()
                .empName("홍길동")
                .empType("PERMANENT")
                .build();
        ReflectionTestUtils.setField(employee, "id", 100L);

        safetyEducationLog = SafetyEducationLog.builder()
                .site(site)
                .manager(manager)
                .corporation(corporation)
                .educationType(EducationType.REGULAR)
                .educationSubject("안전교육")
                .educationContent("안전교육 내용")
                .instructorName("강사")
                .educationLocation("장소")
                .status(SafetyEducationStatus.MANAGER_SIGNING_PENDING)
                .build();
        ReflectionTestUtils.setField(safetyEducationLog, "id", 1L);
    }

    @Nested
    @DisplayName("관리자 서명 처리")
    class ProcessManagerSignature {

        @Test
        @DisplayName("실패 - 잘못된 상태 (이미 서명됨)")
        void fail_invalidStatus() {
            // given
            Long siteId = 1L;
            Long logId = 1L;
            String currentUserId = "manager01";

            // 상태를 MANAGER_SIGNED로 변경
            ReflectionTestUtils.setField(safetyEducationLog, "status", SafetyEducationStatus.MANAGER_SIGNED);

            SafetyEducationSignatureRequest request = SafetyEducationSignatureRequest.builder()
                    .signatureS3Key("uploads/safety-docs/1/MANAGER/12345.png")
                    .clientHash("hash")
                    .build();

            given(safetyEducationLogRepository.findById(logId)).willReturn(Optional.of(safetyEducationLog));

            // when & then
            assertThatThrownBy(() -> signatureService.processManagerSignature(
                    siteId, logId, request, "ip", "device", currentUserId))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", SafetyDocErrorCode.INVALID_SAFETY_EDUCATION_STATUS);
        }

        @Test
        @DisplayName("실패 - 이미 서명한 경우")
        void fail_alreadySigned() {
            // given
            Long siteId = 1L;
            Long logId = 1L;
            String currentUserId = "manager01";

            SafetyEducationSignatureRequest request = SafetyEducationSignatureRequest.builder()
                    .signatureS3Key("uploads/safety-docs/1/MANAGER/12345.png")
                    .clientHash("hash")
                    .build();

            given(safetyEducationLogRepository.findById(logId)).willReturn(Optional.of(safetyEducationLog));
            given(userRepository.findByUserId(currentUserId)).willReturn(Optional.of(user));
            given(managerRepository.findByUserId(user.getId())).willReturn(Optional.of(manager));
            given(signLogRepository.existsBySafetyEducationLogIdAndSignerRoleAndIsDeletedFalse(logId, SignerRole.MANAGER))
                    .willReturn(true);

            // when & then
            assertThatThrownBy(() -> signatureService.processManagerSignature(
                    siteId, logId, request, "ip", "device", currentUserId))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", SafetyDocErrorCode.ALREADY_SIGNED);
        }

        @Test
        @DisplayName("실패 - 안전교육일지가 존재하지 않음")
        void fail_logNotFound() {
            // given
            Long siteId = 1L;
            Long logId = 999L;
            String currentUserId = "manager01";

            SafetyEducationSignatureRequest request = SafetyEducationSignatureRequest.builder()
                    .signatureS3Key("uploads/safety-docs/1/MANAGER/12345.png")
                    .clientHash("hash")
                    .build();

            given(safetyEducationLogRepository.findById(logId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> signatureService.processManagerSignature(
                    siteId, logId, request, "ip", "device", currentUserId))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", SafetyDocErrorCode.SAFETY_EDUCATION_LOG_NOT_FOUND);
        }

        @Test
        @DisplayName("실패 - 안전교육일지가 다른 현장 소속")
        void fail_crossSiteSigning() {
            // given
            Long siteId = 999L;  // 다른 현장 ID
            Long logId = 1L;
            String currentUserId = "manager01";

            SafetyEducationSignatureRequest request = SafetyEducationSignatureRequest.builder()
                    .signatureS3Key("uploads/safety-docs/1/MANAGER/12345.png")
                    .clientHash("hash")
                    .build();

            given(safetyEducationLogRepository.findById(logId)).willReturn(Optional.of(safetyEducationLog));

            // when & then
            assertThatThrownBy(() -> signatureService.processManagerSignature(
                    siteId, logId, request, "ip", "device", currentUserId))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", SafetyDocErrorCode.SITE_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("참석자 서명 처리")
    class ProcessAttendeeSignature {

        @BeforeEach
        void setUpAttendeeTest() {
            // 참석자 서명을 위한 상태 설정
            ReflectionTestUtils.setField(safetyEducationLog, "status", SafetyEducationStatus.MANAGER_SIGNED);
            safetyEducationLog.updatePdf("https://bucket.s3.amazonaws.com/safety-docs/1/2025-01-24/SE-2025-01-24-1-manager-signed.pdf");
        }

        @Test
        @DisplayName("실패 - 잘못된 상태 (관리자 서명 전)")
        void fail_invalidStatus() {
            // given
            Long siteId = 1L;
            Long logId = 1L;
            Long employeeId = 100L;

            // 관리자 서명 전 상태로 변경
            ReflectionTestUtils.setField(safetyEducationLog, "status", SafetyEducationStatus.MANAGER_SIGNING_PENDING);

            SafetyEducationSignatureRequest request = SafetyEducationSignatureRequest.builder()
                    .signatureS3Key("uploads/safety-docs/1/EMPLOYEE/100/12345.png")
                    .clientHash("hash")
                    .build();

            given(safetyEducationLogRepository.findByIdWithAttendees(logId)).willReturn(Optional.of(safetyEducationLog));

            // when & then
            assertThatThrownBy(() -> signatureService.processAttendeeSignature(
                    siteId, logId, employeeId, request, "ip", "device"))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", SafetyDocErrorCode.INVALID_SAFETY_EDUCATION_STATUS);
        }

        @Test
        @DisplayName("실패 - 이미 서명한 참석자")
        void fail_alreadySigned() {
            // given
            Long siteId = 1L;
            Long logId = 1L;
            Long employeeId = 100L;

            SafetyEducationSignatureRequest request = SafetyEducationSignatureRequest.builder()
                    .signatureS3Key("uploads/safety-docs/1/EMPLOYEE/100/12345.png")
                    .clientHash("hash")
                    .build();

            SafetyEducationAttendee attendee = SafetyEducationAttendee.builder()
                    .employee(employee)
                    .isSigned(true) // 이미 서명됨
                    .build();

            given(safetyEducationLogRepository.findByIdWithAttendees(logId)).willReturn(Optional.of(safetyEducationLog));
            given(attendeeRepository.findBySafetyEducationLogIdAndEmployeeIdAndIsDeletedFalse(logId, employeeId))
                    .willReturn(Optional.of(attendee));

            // when & then
            assertThatThrownBy(() -> signatureService.processAttendeeSignature(
                    siteId, logId, employeeId, request, "ip", "device"))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", SafetyDocErrorCode.ALREADY_SIGNED);
        }

        @Test
        @DisplayName("실패 - 참석자가 아닌 경우")
        void fail_notAttendee() {
            // given
            Long siteId = 1L;
            Long logId = 1L;
            Long employeeId = 999L; // 참석자가 아닌 직원 ID

            SafetyEducationSignatureRequest request = SafetyEducationSignatureRequest.builder()
                    .signatureS3Key("uploads/safety-docs/1/EMPLOYEE/999/12345.png")
                    .clientHash("hash")
                    .build();

            given(safetyEducationLogRepository.findByIdWithAttendees(logId)).willReturn(Optional.of(safetyEducationLog));
            given(attendeeRepository.findBySafetyEducationLogIdAndEmployeeIdAndIsDeletedFalse(logId, employeeId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> signatureService.processAttendeeSignature(
                    siteId, logId, employeeId, request, "ip", "device"))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", SafetyDocErrorCode.EMPLOYEE_NOT_AUTHORIZED);
        }

        @Test
        @DisplayName("실패 - 안전교육일지가 다른 현장 소속")
        void fail_crossSiteSigning() {
            // given
            Long siteId = 999L;  // 다른 현장 ID
            Long logId = 1L;
            Long employeeId = 100L;

            SafetyEducationSignatureRequest request = SafetyEducationSignatureRequest.builder()
                    .signatureS3Key("uploads/safety-docs/1/EMPLOYEE/100/12345.png")
                    .clientHash("hash")
                    .build();

            given(safetyEducationLogRepository.findByIdWithAttendees(logId)).willReturn(Optional.of(safetyEducationLog));

            // when & then
            assertThatThrownBy(() -> signatureService.processAttendeeSignature(
                    siteId, logId, employeeId, request, "ip", "device"))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", SafetyDocErrorCode.SITE_NOT_FOUND);
        }
    }
}
