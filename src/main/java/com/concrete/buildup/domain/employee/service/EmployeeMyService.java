package com.concrete.buildup.domain.employee.service;

import com.concrete.buildup.domain.attendance.entity.AttendanceRecord;
import com.concrete.buildup.domain.attendance.enums.AttendanceState;
import com.concrete.buildup.domain.attendance.enums.AttendanceType;
import com.concrete.buildup.domain.attendance.repository.AttendanceRecordRepository;
import com.concrete.buildup.domain.auth.entity.Employee;
import com.concrete.buildup.domain.auth.entity.User;
import com.concrete.buildup.domain.auth.repository.EmployeeRepository;
import com.concrete.buildup.domain.auth.repository.UserRepository;
import com.concrete.buildup.domain.employee.dto.MyAttendanceListResponse;
import com.concrete.buildup.domain.employee.dto.MyAttendanceListResponse.MyAttendanceSummary;
import com.concrete.buildup.domain.site.entity.Site;
import com.concrete.buildup.domain.site.repository.SiteRepository;
import com.concrete.buildup.global.exception.BusinessException;
import com.concrete.buildup.global.exception.errorcode.EmployeeErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 근로자 본인 정보 조회 서비스
 *
 * <p>근로자가 본인의 출퇴근 내역, 급여 내역 등을 조회하는 기능을 제공합니다.</p>
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmployeeMyService {

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final SiteRepository siteRepository;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * 본인 출퇴근 내역 조회
     *
     * <p>JWT에서 추출한 userId로 본인의 출퇴근 기록을 조회합니다.</p>
     * <p>날짜별로 그룹핑하여 출근/퇴근 시간을 하나의 레코드로 반환합니다.</p>
     *
     * @param currentUserId JWT에서 추출한 로그인 ID
     * @param pageable 페이지네이션 정보
     * @return 본인 출퇴근 내역
     */
    public MyAttendanceListResponse getMyAttendanceList(String currentUserId, Pageable pageable) {
        log.info("본인 출퇴근 내역 조회: userId={}", currentUserId);

        // 1. userId(로그인 ID)로 User 조회
        User user = userRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new BusinessException(EmployeeErrorCode.EMPLOYEE_NOT_FOUND));

        // 2. User로 Employee 조회
        Employee employee = employeeRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BusinessException(EmployeeErrorCode.EMPLOYEE_NOT_FOUND));

        Long employeeId = employee.getId();

        // 3. 전체 출퇴근 기록 조회 (최근 6개월)
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusMonths(6);

        List<AttendanceRecord> allRecords = attendanceRecordRepository
                .findByEmployeeIdAndTimestampBetween(employeeId, startTime, endTime);

        // 4. CONFIRMED 상태만 필터링
        List<AttendanceRecord> confirmedRecords = allRecords.stream()
                .filter(record -> record.getState() == AttendanceState.CONFIRMED)
                .sorted(Comparator.comparing(AttendanceRecord::getTimestamp).reversed())
                .toList();

        // 5. 날짜별로 그룹핑하여 출근/퇴근 페어 생성
        Map<LocalDate, Map<Long, List<AttendanceRecord>>> recordsByDateAndSite = confirmedRecords.stream()
                .collect(Collectors.groupingBy(
                        record -> record.getTimestamp().toLocalDate(),
                        LinkedHashMap::new,
                        Collectors.groupingBy(AttendanceRecord::getSiteId)
                ));

        // 6. Site 정보 일괄 조회 (N+1 방지)
        Set<Long> siteIds = confirmedRecords.stream()
                .map(AttendanceRecord::getSiteId)
                .collect(Collectors.toSet());
        Map<Long, Site> siteMap = siteRepository.findAllById(siteIds).stream()
                .collect(Collectors.toMap(Site::getId, site -> site));

        // 7. 날짜+현장별 요약 레코드 생성
        List<MyAttendanceSummary> summaries = new ArrayList<>();
        for (Map.Entry<LocalDate, Map<Long, List<AttendanceRecord>>> dateEntry : recordsByDateAndSite.entrySet()) {
            LocalDate date = dateEntry.getKey();
            for (Map.Entry<Long, List<AttendanceRecord>> siteEntry : dateEntry.getValue().entrySet()) {
                Long siteId = siteEntry.getKey();
                List<AttendanceRecord> records = siteEntry.getValue();

                Site site = siteMap.get(siteId);
                String siteName = site != null ? site.getSiteName() : "알 수 없음";

                // 출근/퇴근 기록 찾기
                AttendanceRecord checkIn = records.stream()
                        .filter(r -> r.getAttendanceType() == AttendanceType.CHECK_IN)
                        .findFirst()
                        .orElse(null);

                AttendanceRecord checkOut = records.stream()
                        .filter(r -> r.getAttendanceType() == AttendanceType.CHECK_OUT)
                        .findFirst()
                        .orElse(null);

                // 출근 기록이 있어야 유효한 데이터
                if (checkIn != null) {
                    String status = determineStatus(checkIn, checkOut);
                    Boolean isLate = determineIsLate(checkIn);

                    summaries.add(MyAttendanceSummary.builder()
                            .attendanceId(checkIn.getId())
                            .date(date)
                            .siteId(siteId)
                            .siteName(siteName)
                            .checkInTime(checkIn.getTimestamp().format(TIME_FORMATTER))
                            .checkOutTime(checkOut != null ? checkOut.getTimestamp().format(TIME_FORMATTER) : null)
                            .status(status)
                            .isLate(isLate)
                            .build());
                }
            }
        }

        // 8. 날짜 내림차순 정렬
        summaries.sort(Comparator.comparing(MyAttendanceSummary::getDate).reversed());

        // 9. 페이지네이션 적용
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), summaries.size());

        List<MyAttendanceSummary> pagedContent = start < summaries.size()
                ? summaries.subList(start, end)
                : Collections.emptyList();

        Page<MyAttendanceSummary> page = new PageImpl<>(pagedContent, pageable, summaries.size());

        log.info("본인 출퇴근 내역 조회 완료: employeeId={}, totalRecords={}", employeeId, summaries.size());

        return MyAttendanceListResponse.from(page);
    }

    /**
     * 근태 상태 결정
     */
    private String determineStatus(AttendanceRecord checkIn, AttendanceRecord checkOut) {
        if (checkOut != null) {
            return "COMPLETED"; // 퇴근 완료
        }

        // 출근만 있는 경우 - 당일이면 근무중, 과거면 미퇴근
        LocalDate today = LocalDate.now();
        LocalDate checkInDate = checkIn.getTimestamp().toLocalDate();

        if (checkInDate.equals(today)) {
            return "WORKING"; // 근무중
        } else {
            return "INCOMPLETE"; // 미퇴근 (과거 데이터)
        }
    }

    /**
     * 지각 여부 결정
     * <p>현재는 단순히 9시 이후 출근을 지각으로 판단합니다.</p>
     * <p>TODO: 계약서의 출근 시간 기준으로 변경 필요</p>
     */
    private Boolean determineIsLate(AttendanceRecord checkIn) {
        LocalTime checkInTime = checkIn.getTimestamp().toLocalTime();
        LocalTime standardTime = LocalTime.of(9, 5); // 9시 5분 기준 (5분 유예)
        return checkInTime.isAfter(standardTime);
    }
}
