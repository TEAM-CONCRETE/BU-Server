package com.concrete.buildup.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자 정보 조회 응답 DTO
 *
 * <p>인증된 사용자의 기본 정보 및 역할별 추가 정보를 반환합니다.</p>
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "사용자 정보 조회 응답")
public class UserInfoResponse {

    @Schema(description = "사용자 ID", example = "testuser001")
    private String userId;

    @Schema(description = "사용자 역할 (EMPLOYEE, MANAGER, CORPORATION)", example = "EMPLOYEE")
    private String role;

    @Schema(description = "이름", example = "홍길동")
    private String name;

    @Schema(description = "전화번호", example = "010-1234-5678")
    private String phone;

    @Schema(description = "이메일", example = "test@example.com")
    private String email;

    @Schema(description = "역할별 추가 정보 (Employee/Manager/Corporation)")
    private Object additionalInfo;

    /**
     * 근로자 추가 정보 DTO
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "근로자 추가 정보")
    public static class EmployeeInfo {

        @Schema(description = "근로자 ID", example = "1")
        private Long employeeId;

        @Schema(description = "근로자 이름", example = "홍길동")
        private String empName;

        @Schema(description = "주민등록번호 (마스킹)", example = "950101-*******")
        private String residentNum;

        @Schema(description = "비상 연락망", example = "010-9876-5432")
        private String emergencyPhone;

        @Schema(description = "주소", example = "서울시 강남구")
        private String empAddress;

        @Schema(description = "근로자 유형", example = "DAILY")
        private String empType;

        @Schema(description = "연결된 현장 정보")
        private SiteInfo site;
    }

    /**
     * 현장 관리자 추가 정보 DTO
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "현장 관리자 추가 정보")
    public static class ManagerInfo {

        @Schema(description = "관리자 ID", example = "1")
        private Long managerId;

        @Schema(description = "관리자 이름", example = "김관리")
        private String managerName;

        @Schema(description = "관리 중인 현장 정보")
        private SiteInfo site;
    }

    /**
     * 기업 추가 정보 DTO
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "기업 추가 정보")
    public static class CorporationInfo {

        @Schema(description = "기업 ID", example = "1")
        private Long corporationId;

        @Schema(description = "회사명", example = "주식회사 건설")
        private String corpName;

        @Schema(description = "본사 주소", example = "서울시 강남구")
        private String corpAddress;

        @Schema(description = "대표자 이름", example = "이대표")
        private String corpCeoName;
    }

    /**
     * 현장 정보 DTO
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "현장 정보")
    public static class SiteInfo {

        @Schema(description = "현장 ID", example = "1")
        private Long siteId;

        @Schema(description = "현장명", example = "OO아파트 신축공사")
        private String siteName;

        @Schema(description = "현장 주소", example = "서울시 강남구")
        private String siteAddress;
    }
}