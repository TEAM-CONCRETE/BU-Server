package com.concrete.buildup.domain.safetydoc.service;

import com.concrete.buildup.domain.auth.entity.Employee;
import com.concrete.buildup.domain.auth.entity.Manager;
import com.concrete.buildup.domain.auth.entity.User;
import com.concrete.buildup.domain.auth.repository.EmployeeRepository;
import com.concrete.buildup.domain.auth.repository.ManagerRepository;
import com.concrete.buildup.domain.auth.repository.UserRepository;
import com.concrete.buildup.domain.contract.enums.EmpType;
import com.concrete.buildup.domain.employee.dto.EmployeeListResponseDto;
import com.concrete.buildup.domain.employee.repository.EmployeeQueryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import com.concrete.buildup.domain.safetydoc.dto.*;
import com.concrete.buildup.domain.safetydoc.entity.SafetyEducationAttendee;
import com.concrete.buildup.domain.safetydoc.entity.SafetyEducationLog;
import com.concrete.buildup.domain.safetydoc.enums.SafetyEducationStatus;
import com.concrete.buildup.domain.safetydoc.repository.SafetyEducationAttendeeRepository;
import com.concrete.buildup.domain.safetydoc.repository.SafetyEducationLogRepository;
import com.concrete.buildup.domain.site.entity.Site;
import com.concrete.buildup.domain.site.repository.SiteRepository;
import com.concrete.buildup.domain.upload.service.S3Service;
import com.concrete.buildup.global.exception.BusinessException;
import com.concrete.buildup.global.exception.errorcode.AuthErrorCode;
import com.concrete.buildup.global.exception.errorcode.SafetyDocErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SafetyEducationService {

    private final SafetyEducationLogRepository safetyEducationLogRepository;
    private final SafetyEducationAttendeeRepository attendeeRepository;
    private final SiteRepository siteRepository;
    private final UserRepository userRepository;
    private final ManagerRepository managerRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeQueryRepository employeeQueryRepository;
    private final SafetyEducationPdfService pdfService;
    private final Optional<S3Service> s3Service;

    @Transactional
    public CreateSafetyEducationLogResponse createSafetyEducationLog(
            Long siteId,
            CreateSafetyEducationLogRequest request,
            String currentUserId
    ) {
        // 1. Site 존재 여부 검증
        Site site = siteRepository.findById(siteId)
                .orElseThrow(() -> new BusinessException(SafetyDocErrorCode.SITE_NOT_FOUND));

        // 2. Manager 존재 및 권한 검증
        User user = userRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new BusinessException(AuthErrorCode.USER_NOT_FOUND));

        Manager manager = managerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BusinessException(AuthErrorCode.USER_NOT_FOUND));

        // 3. Manager가 해당 Site에 권한이 있는지 검증
        if (site.getManager() == null || !site.getManager().getId().equals(manager.getId())) {
            throw new BusinessException(SafetyDocErrorCode.MANAGER_NOT_AUTHORIZED);
        }

        // 4. 선택된 근로자 목록 검증
        List<Employee> employees = employeeRepository.findAllById(request.getAttendeeEmployeeIds());
        if (employees.isEmpty()) {
            throw new BusinessException(SafetyDocErrorCode.EMPTY_ATTENDEE_LIST);
        }

        // 5. SafetyEducationLog 엔티티 생성
        SafetyEducationLog safetyEducationLog = SafetyEducationLog.builder()
                .site(site)
                .manager(manager)
                .corporation(site.getCorporation())
                .educationType(request.getEducationType())
                .educationSubject(request.getEducationSubject())
                .educationContent(request.getEducationContent())
                .instructorName(request.getInstructorName())
                .educationLocation(request.getEducationLocation())
                .status(SafetyEducationStatus.DRAFT)
                .build();

        // 6. SafetyEducationAttendee 목록 생성
        for (Employee employee : employees) {
            SafetyEducationAttendee attendee = SafetyEducationAttendee.builder()
                    .employee(employee)
                    .isSigned(false)
                    .build();
            safetyEducationLog.addAttendee(attendee);
        }

        // 7. DB 저장
        safetyEducationLogRepository.save(safetyEducationLog);

        // 8. 초안 PDF 생성 및 S3 업로드
        String pdfUrl = generateAndUploadInitialPdf(safetyEducationLog, site, manager);

        // 9. status를 MANAGER_SIGNING_PENDING으로 변경
        safetyEducationLog.transitionToManagerSigningPending();
        safetyEducationLog.updatePdf(pdfUrl);
        safetyEducationLogRepository.save(safetyEducationLog);

        return CreateSafetyEducationLogResponse.builder()
                .safetyEducationLogId(safetyEducationLog.getId())
                .status(safetyEducationLog.getStatus())
                .pdfUrl(pdfUrl)
                .attendeeCount(employees.size())
                .build();
    }

    /**
     * 현장별 안전교육일지 목록 조회
     *
     * <p>연도/월 필터링 옵션을 지원합니다.</p>
     *
     * @param siteId 현장 ID
     * @param year 연도 (선택)
     * @param month 월 (선택, 1-12)
     * @param currentUserId 현재 로그인한 사용자 ID
     * @return 안전교육일지 목록
     */
    public List<SafetyEducationLogListResponse> getSafetyEducationLogs(
            Long siteId,
            Integer year,
            Integer month,
            String currentUserId) {
        // Site 존재 여부 검증
        Site site = siteRepository.findById(siteId)
                .orElseThrow(() -> new BusinessException(SafetyDocErrorCode.SITE_NOT_FOUND));

        // 권한 검증
        validateManagerAuthorization(site, currentUserId);

        // 안전교육일지 목록 조회
        List<SafetyEducationLog> logs;
        if (year != null && month != null) {
            // 연도/월 필터링
            logs = safetyEducationLogRepository.findBySiteIdAndYearMonth(siteId, year, month);
        } else {
            // 전체 조회
            logs = safetyEducationLogRepository.findBySiteIdAndIsDeletedFalseOrderByCreatedAtDesc(siteId);
        }

        return logs.stream()
                .map(log -> {
                    int totalCount = (int) attendeeRepository.countTotalAttendees(log.getId());
                    int signedCount = (int) attendeeRepository.countSignedAttendees(log.getId());
                    return SafetyEducationLogListResponse.from(log, totalCount, signedCount);
                })
                .collect(Collectors.toList());
    }

    public SafetyEducationLogDetailResponse getSafetyEducationLogDetail(Long siteId, Long logId, String currentUserId) {
        // Site 존재 여부 검증
        Site site = siteRepository.findById(siteId)
                .orElseThrow(() -> new BusinessException(SafetyDocErrorCode.SITE_NOT_FOUND));

        // 권한 검증
        validateManagerAuthorization(site, currentUserId);

        // 안전교육일지 조회
        SafetyEducationLog log = safetyEducationLogRepository.findByIdWithAttendees(logId)
                .orElseThrow(() -> new BusinessException(SafetyDocErrorCode.SAFETY_EDUCATION_LOG_NOT_FOUND));

        // Site 소속 검증 (cross-site access 방지)
        if (log.getSite() == null || !log.getSite().getId().equals(siteId)) {
            throw new BusinessException(SafetyDocErrorCode.SAFETY_EDUCATION_LOG_NOT_FOUND);
        }

        // 참석자 정보 변환
        List<SafetyEducationAttendee> attendees = attendeeRepository.findBySafetyEducationLogIdWithEmployee(logId);
        List<SafetyEducationLogDetailResponse.AttendeeDto> attendeeDtos = attendees.stream()
                .map(a -> SafetyEducationLogDetailResponse.AttendeeDto.builder()
                        .employeeId(a.getEmployee().getId())
                        .empName(a.getEmployee().getEmpName())
                        .empType(a.getEmployee().getEmpType())
                        .isSigned(a.getIsSigned())
                        .signedAt(a.getSignedAt())
                        .build())
                .collect(Collectors.toList());

        int signedCount = (int) attendees.stream().filter(SafetyEducationAttendee::getIsSigned).count();

        return SafetyEducationLogDetailResponse.from(log, attendeeDtos, signedCount);
    }

    public EmployeeListForSafetyEducationResponse getEmployeesForSafetyEducation(
            Long siteId,
            String empTypeFilter,
            String currentUserId
    ) {
        // Site 존재 여부 검증
        Site site = siteRepository.findById(siteId)
                .orElseThrow(() -> new BusinessException(SafetyDocErrorCode.SITE_NOT_FOUND));

        // 권한 검증
        validateManagerAuthorization(site, currentUserId);

        // 해당 현장의 근로자 목록 조회 (Contract 기반)
        EmpType empType = null;
        if (!"ALL".equalsIgnoreCase(empTypeFilter) && empTypeFilter != null) {
            empType = EmpType.valueOf(empTypeFilter.toUpperCase());
        }

        // EmployeeQueryRepository를 사용하여 현장에 소속된 근로자 조회
        Page<EmployeeListResponseDto> employeePage = employeeQueryRepository.findBySiteId(
                siteId, empType, null, PageRequest.of(0, 1000)
        );

        // N+1 해결: 오늘 완료된 안전교육에서 서명한 참석자 ID를 한 번에 조회
        LocalDate today = LocalDate.now();
        Set<Long> signedEmployeeIds = new HashSet<>(
                attendeeRepository.findSignedEmployeeIdsBySiteIdAndDateRange(
                        siteId,
                        LocalDateTime.of(today, LocalTime.MIN),
                        LocalDateTime.of(today, LocalTime.MAX)
                )
        );

        // 각 근로자의 안전교육 이수 여부 확인 (lookup 사용)
        List<EmployeeForSafetyEducationDto> items = employeePage.getContent().stream()
                .map(emp -> {
                    boolean hasSafetyEducation = signedEmployeeIds.contains(emp.getEmployeeId());
                    return EmployeeForSafetyEducationDto.fromDto(emp, hasSafetyEducation);
                })
                .collect(Collectors.toList());

        // 요약 정보 계산
        List<EmployeeListResponseDto> employeeList = employeePage.getContent();
        int permanentCount = (int) employeeList.stream()
                .filter(e -> EmpType.PERMANENT.name().equals(e.getEmpType()))
                .count();
        int dailyCount = (int) employeeList.stream()
                .filter(e -> EmpType.DAILY.name().equals(e.getEmpType()))
                .count();

        EmployeeListForSafetyEducationResponse.EmployeeSummaryDto summary =
                EmployeeListForSafetyEducationResponse.EmployeeSummaryDto.builder()
                        .totalCount(employeeList.size())
                        .permanentCount(permanentCount)
                        .dailyCount(dailyCount)
                        .build();

        return EmployeeListForSafetyEducationResponse.builder()
                .items(items)
                .summary(summary)
                .build();
    }

    public AttendeeSignatureStatusResponse getAttendeeSignatureStatus(Long siteId, Long logId, String currentUserId) {
        // Site 존재 여부 검증
        Site site = siteRepository.findById(siteId)
                .orElseThrow(() -> new BusinessException(SafetyDocErrorCode.SITE_NOT_FOUND));

        // 권한 검증
        validateManagerAuthorization(site, currentUserId);

        // 안전교육일지 조회
        SafetyEducationLog log = safetyEducationLogRepository.findById(logId)
                .orElseThrow(() -> new BusinessException(SafetyDocErrorCode.SAFETY_EDUCATION_LOG_NOT_FOUND));

        // Site 소속 검증 (cross-site access 방지)
        if (log.getSite() == null || !log.getSite().getId().equals(siteId)) {
            throw new BusinessException(SafetyDocErrorCode.SAFETY_EDUCATION_LOG_NOT_FOUND);
        }

        // 참석자 서명 현황 조회
        List<SafetyEducationAttendee> attendees = attendeeRepository.findBySafetyEducationLogIdWithEmployee(logId);

        List<AttendeeSignatureStatusResponse.AttendeeSignatureDto> attendeeDtos = attendees.stream()
                .map(a -> AttendeeSignatureStatusResponse.AttendeeSignatureDto.builder()
                        .employeeId(a.getEmployee().getId())
                        .empName(a.getEmployee().getEmpName())
                        .empType(a.getEmployee().getEmpType())
                        .isSigned(a.getIsSigned())
                        .signedAt(a.getSignedAt())
                        .build())
                .collect(Collectors.toList());

        int signedCount = (int) attendees.stream().filter(SafetyEducationAttendee::getIsSigned).count();
        int unsignedCount = attendees.size() - signedCount;

        return AttendeeSignatureStatusResponse.builder()
                .safetyEducationLogId(logId)
                .totalCount(attendees.size())
                .signedCount(signedCount)
                .unsignedCount(unsignedCount)
                .attendees(attendeeDtos)
                .build();
    }

    private String generateAndUploadInitialPdf(SafetyEducationLog log, Site site, Manager manager) {
        // PDF 생성
        byte[] pdfBytes = pdfService.generateSafetyEducationPdf(
                log,
                site,
                manager.getManagerName(),
                log.getAttendees()
        );

        // S3 업로드
        S3Service service = s3Service.orElseThrow(() ->
                new BusinessException(SafetyDocErrorCode.S3_UPLOAD_FAILED, "S3 서비스가 비활성화되어 있습니다."));

        LocalDate today = LocalDate.now();
        String dateStr = today.format(DateTimeFormatter.ISO_LOCAL_DATE);

        // log.getId()를 사용하여 고유한 S3 키 생성 (동시성 문제 방지)
        // 형식: safety-docs/{siteId}/{date}/SE-{logId}.pdf
        String s3Key = String.format("safety-docs/%d/%s/SE-%d.pdf",
                site.getId(), dateStr, log.getId());

        service.uploadPdf(s3Key, pdfBytes);
        return service.getPdfUrl(s3Key);
    }

    private void validateManagerAuthorization(Site site, String currentUserId) {
        User user = userRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new BusinessException(AuthErrorCode.USER_NOT_FOUND));

        Manager manager = managerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BusinessException(AuthErrorCode.USER_NOT_FOUND));

        if (site.getManager() == null || !site.getManager().getId().equals(manager.getId())) {
            throw new BusinessException(SafetyDocErrorCode.MANAGER_NOT_AUTHORIZED);
        }
    }

}
