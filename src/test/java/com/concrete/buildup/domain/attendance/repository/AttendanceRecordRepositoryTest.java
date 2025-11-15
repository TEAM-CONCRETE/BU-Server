package com.concrete.buildup.domain.attendance.repository;

import com.concrete.buildup.domain.attendance.entity.AttendanceRecord;
import com.concrete.buildup.domain.attendance.enums.AttendanceState;
import com.concrete.buildup.domain.attendance.enums.AttendanceType;
import com.concrete.buildup.global.config.QueryDslConfig;
import com.concrete.buildup.global.util.AesEncryptionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@EnableJpaAuditing
@Import(QueryDslConfig.class)
@DisplayName("AttendanceRecordRepository 테스트")
class AttendanceRecordRepositoryTest {

    @Autowired
    private AttendanceRecordRepository attendanceRecordRepository;

    @MockBean
    private AesEncryptionUtil aesEncryptionUtil;

    private Long testEmployeeId;
    private Long testSiteId;
    private LocalDateTime testTimestamp;

    @BeforeEach
    void setUp() {
        testEmployeeId = 1L;
        testSiteId = 1L;
        testTimestamp = LocalDateTime.now();

        // 테스트 데이터 정리
        attendanceRecordRepository.deleteAll();
    }

    @Test
    @DisplayName("출퇴근 기록 저장 성공")
    void saveAttendanceRecord_Success() {
        // given
        AttendanceRecord record = AttendanceRecord.builder()
            .employeeId(testEmployeeId)
            .siteId(testSiteId)
            .attendanceType(AttendanceType.CHECK_IN)
            .timestamp(testTimestamp)
            .capturedFaceImageUrl("https://s3.example.com/faces/test.jpg")
            .similarityScore(0.95)
            .state(AttendanceState.CONFIRMED)
            .build();

        // when
        AttendanceRecord saved = attendanceRecordRepository.save(record);

        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getEmployeeId()).isEqualTo(testEmployeeId);
        assertThat(saved.getSiteId()).isEqualTo(testSiteId);
        assertThat(saved.getAttendanceType()).isEqualTo(AttendanceType.CHECK_IN);
        assertThat(saved.getState()).isEqualTo(AttendanceState.CONFIRMED);
        assertThat(saved.getSimilarityScore()).isEqualTo(0.95);
    }

    @Test
    @DisplayName("사원의 특정 기간 출퇴근 기록 조회")
    void findByEmployeeIdAndTimestampBetween_Success() {
        // given
        LocalDateTime start = testTimestamp.minusDays(1);
        LocalDateTime end = testTimestamp.plusDays(1);

        createAttendanceRecord(testEmployeeId, testSiteId, AttendanceType.CHECK_IN, testTimestamp);
        createAttendanceRecord(testEmployeeId, testSiteId, AttendanceType.CHECK_OUT, testTimestamp.plusHours(8));
        createAttendanceRecord(testEmployeeId + 1, testSiteId, AttendanceType.CHECK_IN, testTimestamp);

        // when
        List<AttendanceRecord> records = attendanceRecordRepository.findByEmployeeIdAndTimestampBetween(
            testEmployeeId, start, end
        );

        // then
        assertThat(records).hasSize(2);
        assertThat(records).allMatch(r -> r.getEmployeeId().equals(testEmployeeId));
    }

    @Test
    @DisplayName("현장의 특정 기간 출퇴근 기록 조회 (페이징)")
    void findBySiteIdAndTimestampBetween_WithPaging_Success() {
        // given
        LocalDateTime start = testTimestamp.minusDays(1);
        LocalDateTime end = testTimestamp.plusDays(1);

        createAttendanceRecord(1L, testSiteId, AttendanceType.CHECK_IN, testTimestamp);
        createAttendanceRecord(2L, testSiteId, AttendanceType.CHECK_IN, testTimestamp.plusHours(1));
        createAttendanceRecord(3L, testSiteId, AttendanceType.CHECK_IN, testTimestamp.plusHours(2));
        createAttendanceRecord(1L, testSiteId + 1, AttendanceType.CHECK_IN, testTimestamp);

        PageRequest pageRequest = PageRequest.of(0, 10);

        // when
        Page<AttendanceRecord> page = attendanceRecordRepository.findBySiteIdAndTimestampBetween(
            testSiteId, start, end, pageRequest
        );

        // then
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent()).allMatch(r -> r.getSiteId().equals(testSiteId));
    }

    @Test
    @DisplayName("당일 중복 출퇴근 체크 - 기존 기록 존재")
    void findDuplicateCheckInToday_RecordExists() {
        // given
        LocalDate today = testTimestamp.toLocalDate();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);

        createAttendanceRecord(testEmployeeId, testSiteId, AttendanceType.CHECK_IN, testTimestamp);

        // when
        Optional<AttendanceRecord> duplicate = attendanceRecordRepository
            .findFirstByEmployeeIdAndSiteIdAndAttendanceTypeAndTimestampBetweenAndState(
                testEmployeeId,
                testSiteId,
                AttendanceType.CHECK_IN,
                startOfDay,
                endOfDay,
                AttendanceState.CONFIRMED
            );

        // then
        assertThat(duplicate).isPresent();
        assertThat(duplicate.get().getEmployeeId()).isEqualTo(testEmployeeId);
        assertThat(duplicate.get().getAttendanceType()).isEqualTo(AttendanceType.CHECK_IN);
    }

    @Test
    @DisplayName("당일 중복 출퇴근 체크 - 기존 기록 없음")
    void findDuplicateCheckInToday_NoRecord() {
        // given
        LocalDate today = testTimestamp.toLocalDate();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);

        // when
        Optional<AttendanceRecord> duplicate = attendanceRecordRepository
            .findFirstByEmployeeIdAndSiteIdAndAttendanceTypeAndTimestampBetweenAndState(
                testEmployeeId,
                testSiteId,
                AttendanceType.CHECK_IN,
                startOfDay,
                endOfDay,
                AttendanceState.CONFIRMED
            );

        // then
        assertThat(duplicate).isEmpty();
    }

    @Test
    @DisplayName("사원의 최근 출퇴근 기록 조회")
    void findTopByEmployeeIdOrderByTimestampDesc_Success() {
        // given
        createAttendanceRecord(testEmployeeId, testSiteId, AttendanceType.CHECK_IN, testTimestamp.minusHours(10));
        createAttendanceRecord(testEmployeeId, testSiteId, AttendanceType.CHECK_OUT, testTimestamp.minusHours(2));
        AttendanceRecord latest = createAttendanceRecord(testEmployeeId, testSiteId, AttendanceType.CHECK_IN, testTimestamp);

        // when
        Optional<AttendanceRecord> result = attendanceRecordRepository.findTopByEmployeeIdOrderByTimestampDesc(testEmployeeId);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(latest.getId());
        assertThat(result.get().getTimestamp()).isEqualTo(testTimestamp);
    }

    @Test
    @DisplayName("현장의 특정 상태 출퇴근 기록 조회")
    void findBySiteIdAndState_Success() {
        // given
        createAttendanceRecord(1L, testSiteId, AttendanceType.CHECK_IN, testTimestamp, AttendanceState.CONFIRMED);
        createAttendanceRecord(2L, testSiteId, AttendanceType.CHECK_IN, testTimestamp, AttendanceState.PENDING_REVIEW);
        createAttendanceRecord(3L, testSiteId, AttendanceType.CHECK_IN, testTimestamp, AttendanceState.PENDING_REVIEW);

        PageRequest pageRequest = PageRequest.of(0, 10);

        // when
        Page<AttendanceRecord> page = attendanceRecordRepository.findBySiteIdAndState(
            testSiteId, AttendanceState.PENDING_REVIEW, pageRequest
        );

        // then
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).allMatch(r -> r.getState() == AttendanceState.PENDING_REVIEW);
    }

    @Test
    @DisplayName("현장의 PENDING_REVIEW 상태 기록 개수 조회")
    void countBySiteIdAndState_Success() {
        // given
        createAttendanceRecord(1L, testSiteId, AttendanceType.CHECK_IN, testTimestamp, AttendanceState.CONFIRMED);
        createAttendanceRecord(2L, testSiteId, AttendanceType.CHECK_IN, testTimestamp, AttendanceState.PENDING_REVIEW);
        createAttendanceRecord(3L, testSiteId, AttendanceType.CHECK_IN, testTimestamp, AttendanceState.PENDING_REVIEW);
        createAttendanceRecord(4L, testSiteId, AttendanceType.CHECK_IN, testTimestamp, AttendanceState.PENDING_REVIEW);

        // when
        Long count = attendanceRecordRepository.countBySiteIdAndState(testSiteId, AttendanceState.PENDING_REVIEW);

        // then
        assertThat(count).isEqualTo(3);
    }

    private AttendanceRecord createAttendanceRecord(Long employeeId, Long siteId, AttendanceType type, LocalDateTime timestamp) {
        return createAttendanceRecord(employeeId, siteId, type, timestamp, AttendanceState.CONFIRMED);
    }

    private AttendanceRecord createAttendanceRecord(Long employeeId, Long siteId, AttendanceType type, LocalDateTime timestamp, AttendanceState state) {
        AttendanceRecord record = AttendanceRecord.builder()
            .employeeId(employeeId)
            .siteId(siteId)
            .attendanceType(type)
            .timestamp(timestamp)
            .capturedFaceImageUrl("https://s3.example.com/faces/" + employeeId + ".jpg")
            .similarityScore(0.95)
            .state(state)
            .build();
        return attendanceRecordRepository.save(record);
    }
}
