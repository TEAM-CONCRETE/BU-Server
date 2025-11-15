package com.concrete.buildup.domain.attendance.service;

import com.concrete.buildup.domain.attendance.dto.*;
import com.concrete.buildup.domain.attendance.entity.Attendance;
import com.concrete.buildup.domain.attendance.enums.AttendanceType;
import com.concrete.buildup.domain.attendance.exception.AttendanceNotFoundException;
import com.concrete.buildup.domain.attendance.exception.DuplicateAttendanceException;
import com.concrete.buildup.domain.attendance.exception.FaceImageNotRegisteredException;
import com.concrete.buildup.domain.attendance.repository.AttendanceRepository;
import com.concrete.buildup.domain.auth.entity.Employee;
import com.concrete.buildup.domain.auth.entity.User;
import com.concrete.buildup.domain.auth.repository.EmployeeRepository;
import com.concrete.buildup.domain.auth.repository.UserRepository;
import com.concrete.buildup.domain.contract.entity.Contract;
import com.concrete.buildup.domain.contract.entity.ContractDetail;
import com.concrete.buildup.domain.contract.enums.EmpType;
import com.concrete.buildup.domain.contract.repository.ContractRepository;
import com.concrete.buildup.domain.site.entity.Site;
import com.concrete.buildup.domain.site.repository.SiteRepository;
import com.concrete.buildup.domain.upload.service.S3Service;
import com.concrete.buildup.global.exception.BusinessException;
import com.concrete.buildup.global.exception.errorcode.AuthErrorCode;
import com.concrete.buildup.global.exception.errorcode.CommonErrorCode;
import com.concrete.buildup.global.util.MaskingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Comparator;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final ContractRepository contractRepository;
    private final SiteRepository siteRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final FaceSimilarityClient faceSimilarityClient;
    private final S3Service s3Service;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * 근태 현황 조회 (계약 기반 - 출퇴근 기록 없어도 표시)
     *
     * @param siteId 현장 ID
     * @param year 년도
     * @param month 월
     * @param day 일 (optional)
     * @param employmentType 근로자 유형 (REGULAR/DAILY)
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 근태 현황 응답
     */
    public AttendanceListResponseDto getAttendanceRecords(
        Long siteId,
        Integer year,
        Integer month,
        Integer day,
        String employmentType,
        Integer page,
        Integer size
    ) {
        // 1. 입력 검증
        validateEmploymentType(employmentType);
        if (day != null) {
            validateDay(year, month, day);
        }

        // 2. Site 조회 → managerId 획득
        Site site = siteRepository.findById(siteId)
            .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND, "현장을 찾을 수 없습니다."));
        Long managerId = site.getManager().getId();

        // 3. 날짜 범위 계산
        LocalDate startDate;
        LocalDate endDate;

        if (day != null) {
            // 특정 일자 조회
            startDate = LocalDate.of(year, month, day);
            endDate = startDate;
        } else {
            // 월 전체 조회
            startDate = LocalDate.of(year, month, 1);
            endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
        }

        // 4. 근로자 유형 매핑 (REGULAR → PERMANENT, DAILY → DAILY)
        EmpType empType = mapEmploymentType(employmentType);

        // 4. 해당 현장(managerId), 날짜, 근로자 유형에 맞는 활성 계약 조회
        List<Contract> activeContracts;
        if (day != null) {
            activeContracts = contractRepository.findActiveContractsByManagerIdAndEmpTypeAndDate(
                managerId, empType, startDate
            );
        } else {
            // 월 조회의 경우 startDate부터 endDate 사이에 활성화된 계약 조회
            activeContracts = contractRepository.findActiveContractsByManagerIdAndEmpTypeAndDate(
                managerId, empType, startDate
            ).stream()
            .filter(c -> c.getEmployeeStartDate().isBefore(endDate.plusDays(1)))
            .collect(Collectors.toList());
        }

        log.info("Active contracts found: {} for site: {}, date: {}-{}-{}, empType: {}",
            activeContracts.size(), siteId, year, month, day, empType);

        // 5. 각 계약에 대해 근태 데이터 생성
        Map<Long, Employee> employeeCache = new HashMap<>();
        List<AttendanceDetailDto> allRecords = activeContracts.stream()
            .map(contract -> {
                // Employee 정보 조회 (캐싱)
                Employee employee = employeeCache.computeIfAbsent(
                    contract.getEmployeeId(),
                    id -> employeeRepository.findById(id).orElse(null)
                );

                if (employee == null) {
                    log.warn("Employee not found for contract: {}, employeeId: {}",
                        contract.getId(), contract.getEmployeeId());
                    return null;
                }

                // Attendance 조회
                Optional<Attendance> attendanceOpt;
                if (day != null) {
                    attendanceOpt = attendanceRepository
                        .findByEmployeeIdAndSearchDate(contract.getEmployeeId(), startDate)
                        .stream()
                        .findFirst();
                } else {
                    attendanceOpt = Optional.empty(); // 월 조회시에는 복잡하므로 일단 ABSENT 처리
                }

                return convertToDetailDto(employee, attendanceOpt.orElse(null));
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        // 6. 페이징 처리
        int totalRecords = allRecords.size();
        int start = (page - 1) * size;
        int end = Math.min(start + size, totalRecords);

        List<AttendanceDetailDto> pagedRecords = start < totalRecords
            ? allRecords.subList(start, end)
            : Collections.emptyList();

        // 7. Summary 계산
        AttendanceSummaryDto summary = calculateSummary(allRecords);

        // 8. 페이징 정보 구성
        PaginationDto pagination = PaginationDto.builder()
            .currentPage(page)
            .totalPages((int) Math.ceil((double) totalRecords / size))
            .totalRecords((long) totalRecords)
            .pageSize(size)
            .build();

        return AttendanceListResponseDto.builder()
            .summary(summary)
            .records(pagedRecords)
            .pagination(pagination)
            .build();
    }

    /**
     * 근태 요약 정보 계산
     */
    private AttendanceSummaryDto calculateSummary(List<AttendanceDetailDto> records) {
        long normalCount = records.stream()
            .filter(r -> "NORMAL".equals(r.getAttendanceStatus()))
            .count();

        long lateCount = records.stream()
            .filter(r -> "LATE".equals(r.getAttendanceStatus()))
            .count();

        long earlyLeaveCount = records.stream()
            .filter(r -> "EARLY_LEAVE".equals(r.getAttendanceStatus()))
            .count();

        long absentCount = records.stream()
            .filter(r -> "ABSENT".equals(r.getAttendanceStatus()))
            .count();

        return AttendanceSummaryDto.builder()
            .normalAttendance(normalCount)
            .late(lateCount)
            .earlyLeave(earlyLeaveCount)
            .absent(absentCount)
            .build();
    }

    /**
     * Employee와 Attendance를 AttendanceDetailDto로 변환
     */
    private AttendanceDetailDto convertToDetailDto(Employee employee, Attendance attendance) {
        if (attendance != null) {
            // Attendance 기록이 있는 경우
            return AttendanceDetailDto.builder()
                .workerId(employee.getId())
                .workerName(employee.getEmpName())
                .residentNumber(MaskingUtil.maskResidentNumber(employee.getResidentNum()))
                .attendanceStatus(attendance.getAttendanceStatus())
                .checkInTime(formatTime(attendance.getCheckInTime()))
                .checkOutTime(formatTime(attendance.getCheckOutTime()))
                .totalWorkHours(formatHours(attendance.getTotalWorkHour()))
                .nightWorkHours(formatHours(attendance.getNightWorkHour()))
                .overtimeHours(formatHours(attendance.getAdditionalWorkHour()))
                .holidayWorkHours(formatHours(attendance.getHolidayWorkHour()))
                .build();
        } else {
            // Attendance 기록이 없는 경우 → ABSENT로 표시
            return AttendanceDetailDto.builder()
                .workerId(employee.getId())
                .workerName(employee.getEmpName())
                .residentNumber(MaskingUtil.maskResidentNumber(employee.getResidentNum()))
                .attendanceStatus("ABSENT")
                .checkInTime("-")
                .checkOutTime("-")
                .totalWorkHours("-")
                .nightWorkHours("-")
                .overtimeHours("-")
                .holidayWorkHours("-")
                .build();
        }
    }

    /**
     * LocalDateTime을 HH:mm 형식으로 변환
     */
    private String formatTime(java.time.LocalDateTime dateTime) {
        if (dateTime == null) {
            return "-";
        }
        return dateTime.format(TIME_FORMATTER);
    }

    /**
     * BigDecimal 시간을 "N시간" 형식으로 변환
     */
    private String formatHours(BigDecimal hours) {
        if (hours == null || hours.compareTo(BigDecimal.ZERO) == 0) {
            return "-";
        }
        return hours.stripTrailingZeros().toPlainString() + "시간";
    }

    /**
     * 근로자 유형 검증 및 매핑
     *
     * @param employmentType 근로자 유형 문자열 (REGULAR/DAILY)
     * @return EmpType enum
     * @throws IllegalArgumentException 유효하지 않은 근로자 유형인 경우
     */
    private EmpType mapEmploymentType(String employmentType) {
        if (employmentType == null || employmentType.trim().isEmpty()) {
            throw new IllegalArgumentException("근로자 유형은 필수입니다.");
        }

        return switch (employmentType.toUpperCase().trim()) {
            case "REGULAR" -> EmpType.PERMANENT;
            case "DAILY" -> EmpType.DAILY;
            default -> throw new IllegalArgumentException(
                String.format("유효하지 않은 근로자 유형입니다: %s (허용값: REGULAR, DAILY)", employmentType)
            );
        };
    }

    /**
     * 근로자 유형 검증
     *
     * @param employmentType 근로자 유형 문자열
     * @throws IllegalArgumentException 유효하지 않은 근로자 유형인 경우
     */
    private void validateEmploymentType(String employmentType) {
        if (employmentType == null || employmentType.trim().isEmpty()) {
            throw new IllegalArgumentException("근로자 유형은 필수입니다.");
        }

        String normalized = employmentType.toUpperCase().trim();
        if (!normalized.equals("REGULAR") && !normalized.equals("DAILY")) {
            throw new IllegalArgumentException(
                String.format("유효하지 않은 근로자 유형입니다: %s (허용값: REGULAR, DAILY)", employmentType)
            );
        }
    }

    /**
     * 날짜 유효성 검증 (해당 년월의 실제 일수 확인)
     *
     * @param year 년도
     * @param month 월
     * @param day 일
     * @throws IllegalArgumentException 해당 년월에 존재하지 않는 날짜인 경우
     */
    private void validateDay(Integer year, Integer month, Integer day) {
        try {
            LocalDate.of(year, month, day);
        } catch (Exception e) {
            int maxDay = LocalDate.of(year, month, 1).lengthOfMonth();
            throw new IllegalArgumentException(
                String.format("유효하지 않은 날짜입니다: %d년 %d월 %d일 (최대 일수: %d)", year, month, day, maxDay)
            );
        }
    }

    /**
     * 출퇴근 검증 및 기록 (Presigned URL 방식)
     *
     * <p>현장 관리자가 로그인한 공용 태블릿에서 근로자가 얼굴 인식을 통해 출퇴근을 기록합니다.
     * Presigned URL을 통해 S3에 업로드한 이미지를 기반으로 얼굴 인식 검증을 수행하고
     * 출퇴근 기록을 생성합니다.</p>
     *
     * <p>처리 흐름:</p>
     * <ol>
     *   <li>로그인한 현장 관리자의 현장 ID 조회 (SecurityContext)</li>
     *   <li>근로자 정보 조회 및 얼굴 이미지 등록 여부 확인</li>
     *   <li>출퇴근 유형 자동 판단 (당일 마지막 기록 조회)</li>
     *   <li>중복 기록 검증 (같은 날, 같은 유형 체크)</li>
     *   <li>S3 URL 생성 및 Face API 호출</li>
     *   <li>검증 결과 처리 및 출퇴근 기록 저장</li>
     * </ol>
     *
     * @param request 출퇴근 검증 요청 (employeeId, uploadId)
     * @return 출퇴근 검증 응답 (verified, recordId, attendanceType 등)
     * @throws FaceImageNotRegisteredException 얼굴 이미지가 등록되지 않은 경우
     * @throws DuplicateAttendanceException 중복 출퇴근 기록이 존재하는 경우
     */
    @Transactional
    public AttendanceVerificationResponseDto verifyAndRecordAttendance(AttendanceVerificationRequestDto request) {
        // 1. 로그인한 현장 관리자의 현장 ID 조회
        Long siteId = getCurrentManagerSiteId();

        log.info("출퇴근 검증 시작 - phoneNumber: {}, uploadId: {}, siteId: {}",
                 request.getPhoneNumber(), request.getUploadId(), siteId);

        // 2. 전화번호로 근로자 정보 조회
        Employee employee = employeeRepository.findByPhoneWithUser(request.getPhoneNumber())
            .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND,
                "해당 전화번호로 등록된 근로자를 찾을 수 없습니다: " + request.getPhoneNumber()));

        Long employeeId = employee.getId();
        log.info("전화번호로 근로자 조회 성공 - employeeId: {}, empName: {}", employeeId, employee.getEmpName());

        // 3. 얼굴 이미지 등록 여부 확인
        if (employee.getProfileImageUrl() == null || employee.getProfileImageUrl().isBlank()) {
            throw new FaceImageNotRegisteredException(employeeId);
        }

        // 4. 출퇴근 유형 자동 판단
        AttendanceType attendanceType = determineAttendanceType(employeeId);
        log.info("자동 판단된 출퇴근 유형: {}", attendanceType);

        // 5. 중복 기록 검증
        validateDuplicateAttendance(employeeId, attendanceType);

        // 6. S3 URL 생성 (등록된 얼굴 이미지, 촬영된 이미지)
        String registeredImageUrl = buildS3Url(employee.getProfileImageUrl());
        String capturedImageUrl = buildS3Url(request.getUploadId());

        log.info("Face API 호출 - registered: {}, captured: {}",
                 maskUrl(registeredImageUrl), maskUrl(capturedImageUrl));

        // 7. Face API 호출
        FaceSimilarityResponseDto faceApiResponse = faceSimilarityClient.compareFaces(
            registeredImageUrl,
            capturedImageUrl
        );

        // 8. 검증 결과 처리
        return processVerificationResult(
            employee,
            siteId,
            attendanceType,
            faceApiResponse
        );
    }

    /**
     * 로그인한 현장 관리자의 현장 ID 조회
     *
     * <p>SecurityContext에서 로그인한 사용자의 username을 가져와서
     * User를 조회하고, 해당 관리자가 관리하는 현장의 ID를 반환합니다.</p>
     *
     * @return 현장 ID
     * @throws BusinessException 로그인 정보가 없거나 현장을 찾을 수 없는 경우
     */
    private Long getCurrentManagerSiteId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(AuthErrorCode.INVALID_TOKEN, "로그인이 필요합니다.");
        }

        // JWT 토큰의 subject는 username (userId 문자열)
        String username = authentication.getName();

        // username으로 User 조회
        User user = userRepository.findByUserId(username)
            .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND,
                "사용자를 찾을 수 없습니다."));

        // User의 ID로 Manager의 현장 조회
        Site site = siteRepository.findByManagerUserId(user.getId())
            .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND,
                "관리 중인 현장을 찾을 수 없습니다."));

        return site.getId();
    }

    /**
     * 출퇴근 유형 자동 판단
     *
     * <p>당일 마지막 출퇴근 기록을 조회하여 다음 유형을 결정합니다:</p>
     * <ul>
     *   <li>마지막 기록이 없거나 CHECK_OUT인 경우 → CHECK_IN</li>
     *   <li>마지막 기록이 CHECK_IN인 경우 → CHECK_OUT</li>
     * </ul>
     *
     * @param employeeId 근로자 ID
     * @return 자동 판단된 출퇴근 유형
     */
    private AttendanceType determineAttendanceType(Long employeeId) {
        LocalDate today = LocalDate.now();

        List<Attendance> todayRecords = attendanceRepository
            .findByEmployeeIdAndSearchDate(employeeId, today);

        // 오늘 기록이 없으면 출근
        if (todayRecords.isEmpty()) {
            return AttendanceType.CHECK_IN;
        }

        // 마지막 기록 조회 (checkInTime 기준 정렬)
        Attendance lastRecord = todayRecords.stream()
            .max(Comparator.comparing(
                attendance -> attendance.getCheckInTime() != null
                    ? attendance.getCheckInTime()
                    : attendance.getCreatedAt()
            ))
            .orElse(null);

        // 마지막 기록이 퇴근이면 출근, 출근이면 퇴근
        if (lastRecord != null && lastRecord.getCheckOutTime() != null) {
            return AttendanceType.CHECK_IN;
        }

        return AttendanceType.CHECK_OUT;
    }

    /**
     * 중복 출퇴근 기록 검증
     *
     * <p>당일 같은 유형의 출퇴근 기록이 이미 존재하는지 확인합니다.</p>
     *
     * @param employeeId 근로자 ID
     * @param attendanceType 출퇴근 유형
     * @throws DuplicateAttendanceException 중복 기록이 존재하는 경우
     */
    private void validateDuplicateAttendance(Long employeeId, AttendanceType attendanceType) {
        LocalDate today = LocalDate.now();

        List<Attendance> todayRecords = attendanceRepository
            .findByEmployeeIdAndSearchDate(employeeId, today);

        // CHECK_IN: checkInTime이 있는 기록 확인
        if (attendanceType == AttendanceType.CHECK_IN) {
            boolean hasCheckIn = todayRecords.stream()
                .anyMatch(record -> record.getCheckInTime() != null);
            if (hasCheckIn) {
                throw new DuplicateAttendanceException(attendanceType);
            }
        }

        // CHECK_OUT: checkOutTime이 있는 기록 확인
        if (attendanceType == AttendanceType.CHECK_OUT) {
            boolean hasCheckOut = todayRecords.stream()
                .anyMatch(record -> record.getCheckOutTime() != null);
            if (hasCheckOut) {
                throw new DuplicateAttendanceException(attendanceType);
            }
        }
    }

    /**
     * 검증 결과 처리 및 출퇴근 기록 저장
     *
     * @param employee 근로자 엔티티
     * @param siteId 현장 ID
     * @param attendanceType 출퇴근 유형
     * @param faceApiResponse Face API 응답
     * @return 출퇴근 검증 응답
     */
    private AttendanceVerificationResponseDto processVerificationResult(
        Employee employee,
        Long siteId,
        AttendanceType attendanceType,
        FaceSimilarityResponseDto faceApiResponse
    ) {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();

        if (!faceApiResponse.getVerified()) {
            // 검증 실패
            log.warn("얼굴 인식 검증 실패 - employeeId: {}, similarity: {}",
                     employee.getId(), faceApiResponse.getSimilarity());

            return AttendanceVerificationResponseDto.builder()
                .success(true)
                .verified(false)
                .recordId(null)
                .employeeName(employee.getEmpName())
                .attendanceType(attendanceType)
                .timestamp(now)
                .similarityScore(faceApiResponse.getSimilarity())
                .message(String.format("얼굴 인식에 실패했습니다. 유사도: %.2f%%",
                                       faceApiResponse.getSimilarity() * 100))
                .build();
        }

        // 검증 성공 - 출퇴근 기록 저장
        Attendance savedAttendance;

        if (attendanceType == AttendanceType.CHECK_OUT) {
            // 퇴근: 기존 레코드 UPDATE
            LocalDate today = LocalDate.now();
            List<Attendance> todayRecords = attendanceRepository
                .findByEmployeeIdAndSearchDate(employee.getId(), today);

            Attendance existingRecord = todayRecords.stream()
                .filter(record -> record.getCheckInTime() != null && record.getCheckOutTime() == null)
                .findFirst()
                .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND,
                    "출근 기록을 찾을 수 없습니다."));

            existingRecord.setCheckOutTime(now);
            savedAttendance = attendanceRepository.save(existingRecord);

            log.info("퇴근 기록 업데이트 완료 - recordId: {}, employeeId: {}",
                     savedAttendance.getId(), employee.getId());
        } else {
            // 출근: 새 레코드 INSERT
            Attendance attendance = createAttendanceRecord(employee, siteId, attendanceType, now);
            savedAttendance = attendanceRepository.save(attendance);

            log.info("출근 기록 저장 완료 - recordId: {}, employeeId: {}",
                     savedAttendance.getId(), employee.getId());
        }

        return AttendanceVerificationResponseDto.builder()
            .success(true)
            .verified(true)
            .recordId(savedAttendance.getId())
            .employeeName(employee.getEmpName())
            .attendanceType(attendanceType)
            .timestamp(now)
            .similarityScore(faceApiResponse.getSimilarity())
            .message(String.format("%s이 정상적으로 기록되었습니다.", attendanceType.getDescription()))
            .isLate(savedAttendance.getIsLate())
            .build();
    }

    /**
     * Attendance 엔티티 생성
     *
     * @param employee 근로자 엔티티
     * @param siteId 현장 ID
     * @param attendanceType 출퇴근 유형
     * @param timestamp 기록 시각
     * @return Attendance 엔티티
     */
    private Attendance createAttendanceRecord(
        Employee employee,
        Long siteId,
        AttendanceType attendanceType,
        java.time.LocalDateTime timestamp
    ) {
        // 해당 근로자의 활성 계약 조회
        LocalDate today = LocalDate.now();
        Site site = siteRepository.findById(siteId)
            .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND, "현장을 찾을 수 없습니다."));

        Long managerId = site.getManager().getId();

        Contract activeContract = contractRepository.findActiveContractsByManagerIdAndEmpTypeAndDate(
                managerId,
                EmpType.valueOf(employee.getEmpType()),
                today
            )
            .stream()
            .filter(c -> c.getEmployeeId().equals(employee.getId()))
            .findFirst()
            .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND,
                "활성화된 계약을 찾을 수 없습니다. 근로자 ID: " + employee.getId()));

        // 지각 여부 판단 (출근 시에만)
        Boolean isLate = false;
        if (attendanceType == AttendanceType.CHECK_IN) {
            isLate = checkIfLate(activeContract, timestamp);
        }

        Attendance.AttendanceBuilder builder = Attendance.builder()
            .contractId(activeContract.getId())
            .employeeId(employee.getId())
            .siteId(siteId)
            .searchDate(today)
            .empType(employee.getEmpType())
            .empName(employee.getEmpName())
            .residentNum(employee.getResidentNum())
            .attendanceStatus("NORMAL") // 기본 상태, 추후 로직으로 판단 가능
            .isLate(isLate);

        // 출퇴근 유형에 따라 checkInTime 또는 checkOutTime 설정
        if (attendanceType == AttendanceType.CHECK_IN) {
            builder.checkInTime(timestamp);
        } else {
            builder.checkOutTime(timestamp);
        }

        return builder.build();
    }

    /**
     * S3 객체 키를 Presigned GET URL로 변환
     * Face API가 S3 이미지에 접근할 수 있도록 임시 URL을 생성합니다.
     *
     * @param s3Key S3 객체 키 (예: "attendance/1/1/1735708800000.jpg")
     * @return Presigned GET URL (15분 유효)
     */
    private String buildS3Url(String s3Key) {
        return s3Service.generatePresignedGetUrl(s3Key);
    }

    /**
     * 지각 여부 판단
     *
     * <p>계약서상 출근 시간을 기준으로 지각 여부를 판단합니다.</p>
     * <ul>
     *   <li>계약서상 출근 시간부터 +5분까지: 정상 출근 (false)</li>
     *   <li>계약서상 출근 시간 +5분 초과: 지각 (true)</li>
     *   <li>계약서에 출근 시간이 없는 경우: 정상 출근 (false)</li>
     * </ul>
     *
     * @param contract 근로 계약 (ContractDetail 포함)
     * @param checkInTime 실제 출근 시각
     * @return 지각 여부 (true: 지각, false: 정상)
     */
    private Boolean checkIfLate(Contract contract, java.time.LocalDateTime checkInTime) {
        // ContractDetail 조회
        Contract contractWithDetail = contractRepository.findByIdWithDetails(contract.getId())
            .orElse(contract);

        ContractDetail contractDetail = contractWithDetail.getContractDetail();

        // ContractDetail이 없거나 출근 시간이 설정되지 않은 경우 정상 출근으로 처리
        if (contractDetail == null || contractDetail.getWorkStartTime() == null) {
            log.warn("계약서에 출근 시간이 설정되지 않음 - contractId: {}", contract.getId());
            return false;
        }

        LocalTime contractStartTime = contractDetail.getWorkStartTime();
        LocalTime actualCheckInTime = checkInTime.toLocalTime();

        // 출근 시간 기준 +5분까지 허용
        LocalTime allowedLatestTime = contractStartTime.plusMinutes(5);

        // 실제 출근 시간이 허용 시간을 초과했는지 확인
        boolean late = actualCheckInTime.isAfter(allowedLatestTime);

        log.info("지각 판단 - contractId: {}, 계약상 출근시간: {}, 실제 출근시간: {}, 허용시간: {}, 지각여부: {}",
                 contract.getId(), contractStartTime, actualCheckInTime, allowedLatestTime, late);

        return late;
    }

    /**
     * URL 마스킹 (로그용)
     *
     * @param url S3 URL
     * @return 마스킹된 URL
     */
    private String maskUrl(String url) {
        if (url == null || url.length() < 30) {
            return url;
        }
        return url.substring(0, 30) + "***";
    }
}
