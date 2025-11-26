package com.concrete.buildup.domain.site.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 대시보드(홈) 화면 응답 DTO
 *
 * <p>기업 관리자와 현장 관리자에게 현장 관리 정보를 제공합니다.</p>
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Getter
@Builder
@Schema(description = "대시보드 응답 DTO")
public class DashboardResponse {

    @Schema(description = "현장 기본 정보")
    private SiteBasicInfo siteInfo;

    @Schema(description = "인원 현황")
    private WorkforceStatus workforceStatus;

    @Schema(description = "안전 현황")
    private SafetyStatus safetyStatus;

    @Schema(description = "노무 현황")
    private LaborStatus laborStatus;

    /**
     * 현장 기본 정보
     */
    @Getter
    @Builder
    @Schema(description = "현장 기본 정보")
    public static class SiteBasicInfo {

        @Schema(description = "현장 ID", example = "1")
        private Long siteId;

        @Schema(description = "현장명", example = "강남 오피스텔 신축현장")
        private String siteName;

        @Schema(description = "현장 주소", example = "서울특별시 강남구 역삼동 123-45")
        private String siteAddress;

        @Schema(description = "발주처", example = "서울시설공단")
        private String clientName;

        @Schema(description = "시작일", example = "2025-01-01")
        private LocalDate startDate;

        @Schema(description = "종료일", example = "2025-12-31")
        private LocalDate endDate;

        @Schema(description = "진행률 (0.0 ~ 100.0)", example = "45.5")
        private BigDecimal progressRate;

        @Schema(description = "현장 관리자 이름", example = "김현장")
        private String managerName;
    }

    /**
     * 인원 현황
     */
    @Getter
    @Builder
    @Schema(description = "인원 현황")
    public static class WorkforceStatus {

        @Schema(description = "총 근로자 수", example = "50")
        private Integer totalWorkers;

        @Schema(description = "상용직 수", example = "30")
        private Integer permanentWorkers;

        @Schema(description = "일용직 수", example = "20")
        private Integer dailyWorkers;

        @Schema(description = "금일 출근 인원 수", example = "45")
        private Integer todayAttendance;

        @Schema(description = "금일 지각 인원 수", example = "3")
        private Integer todayLateCount;
    }

    /**
     * 안전 현황
     */
    @Getter
    @Builder
    @Schema(description = "안전 현황")
    public static class SafetyStatus {

        @Schema(description = "안전율 (0.0 ~ 100.0)", example = "92.5")
        private BigDecimal safetyRate;

        @Schema(description = "금일 발생 안전 경고 건수", example = "2")
        private Integer todayWarnings;

        @Schema(description = "교육 미이수 인원 수", example = "5")
        private Integer incompletedEducation;

        @Schema(description = "안전점검 완료 건수", example = "10")
        private Integer completedInspections;
    }

    /**
     * 노무 현황
     */
    @Getter
    @Builder
    @Schema(description = "노무 현황")
    public static class LaborStatus {

        @Schema(description = "전체 미결 전자계약 수", example = "3")
        private Integer totalPendingContracts;

        @Schema(description = "미결 전자계약 목록")
        private List<PendingContractItem> pendingContracts;
    }

    /**
     * 미결 전자계약 항목
     */
    @Getter
    @Builder
    @Schema(description = "미결 전자계약 항목")
    public static class PendingContractItem {

        @Schema(description = "계약 ID", example = "101")
        private Long contractId;

        @Schema(description = "계약서 종류 (근로계약서 또는 안전교육일지)", example = "근로계약서")
        private String contractType;

        @Schema(description = "대상자 이름", example = "김철수")
        private String targetName;

        @Schema(description = "계약 상태 (MANAGER_SIGNING_PENDING, EMPLOYEE_SIGNING_PENDING 등)", example = "MANAGER_SIGNING_PENDING")
        private String contractState;
    }
}
