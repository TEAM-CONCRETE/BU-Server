package com.concrete.buildup.domain.employee.dto;

import com.concrete.buildup.domain.contract.enums.EmpType;
import com.concrete.buildup.global.util.MaskingUtil;
import lombok.Builder;
import lombok.Getter;

/**
 * 사원 목록 조회 응답 DTO
 *
 * <p>현장에 소속된 근로자 목록을 반환합니다.</p>
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Getter
@Builder
public class EmployeeListResponseDto {

    /**
     * 근로자 ID
     */
    private final Long employeeId;

    /**
     * 근로자 이름
     */
    private final String name;

    /**
     * 주민등록번호 (마스킹 처리됨)
     */
    private final String residentId;

    /**
     * 근로자 유형 (DAILY/PERMANENT)
     */
    private final String empType;

    /**
     * JPQL 생성자 호출용 생성자
     *
     * <p>주민등록번호는 자동으로 마스킹 처리됩니다.</p>
     *
     * @param employeeId 근로자 ID
     * @param name 근로자 이름
     * @param residentNum 주민등록번호 (원본)
     * @param empType 근로자 유형
     */
    public EmployeeListResponseDto(Long employeeId, String name, String residentNum, EmpType empType) {
        this.employeeId = employeeId;
        this.name = name;
        this.residentId = MaskingUtil.maskResidentNumber(residentNum);
        this.empType = empType != null ? empType.name() : null;
    }
}