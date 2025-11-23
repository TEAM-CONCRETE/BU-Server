package com.concrete.buildup.domain.employee.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 근로자 홈 화면 응답 DTO
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Schema(description = "근로자 홈 화면 응답")
@Getter
@Builder
public class MyHomeResponse {

    @Schema(description = "미결 전자계약 정보")
    private PendingContractsInfo pendingContracts;

    @Schema(description = "최근 급여 정보 (없으면 null)")
    private RecentSalaryInfo recentSalary;

    @Schema(description = "금일 근태 정보 (없으면 null)")
    private TodayAttendanceInfo todayAttendance;

    /**
     * 미결 전자계약 정보
     */
    @Schema(description = "미결 전자계약 정보")
    @Getter
    @Builder
    public static class PendingContractsInfo {

        @Schema(description = "미결 건수", example = "2")
        private int count;

        @Schema(description = "미결 계약 목록")
        private List<PendingContractItem> items;
    }

    /**
     * 미결 계약 항목
     */
    @Schema(description = "미결 계약 항목")
    @Getter
    @Builder
    public static class PendingContractItem {

        @Schema(description = "계약 유형 (CONTRACT: 근로계약서, SAFETY_EDUCATION: 안전교육일지)", example = "CONTRACT")
        private String type;

        @Schema(description = "계약 ID (type=CONTRACT일 때)", example = "123")
        private Long contractId;

        @Schema(description = "안전교육일지 ID (type=SAFETY_EDUCATION일 때)", example = "456")
        private Long safetyLogId;

        @Schema(description = "현장 ID", example = "1")
        private Long siteId;

        @Schema(description = "현장명", example = "강남 현장")
        private String siteName;
    }

    /**
     * 최근 급여 정보
     */
    @Schema(description = "최근 급여 정보")
    @Getter
    @Builder
    public static class RecentSalaryInfo {

        @Schema(description = "급여 ID", example = "789")
        private Long payrollId;

        @Schema(description = "현장 ID", example = "1")
        private Long siteId;

        @Schema(description = "현장명", example = "강남 현장")
        private String siteName;

        @Schema(description = "지급일", example = "2025-11-15")
        private LocalDate payDate;

        @Schema(description = "실지급액", example = "2850000")
        private BigDecimal netPay;
    }

    /**
     * 금일 근태 정보
     */
    @Schema(description = "금일 근태 정보")
    @Getter
    @Builder
    public static class TodayAttendanceInfo {

        @Schema(description = "현장 ID", example = "1")
        private Long siteId;

        @Schema(description = "현장명", example = "강남 현장")
        private String siteName;

        @Schema(description = "출근 시간", example = "08:55")
        private String checkInTime;

        @Schema(description = "퇴근 시간 (퇴근 전이면 null)", example = "18:10")
        private String checkOutTime;

        @Schema(description = "근태 상태 (WORKING: 근무중, COMPLETED: 퇴근완료)", example = "WORKING")
        private String status;

        @Schema(description = "지각 여부", example = "false")
        private Boolean isLate;
    }
}
