package com.concrete.buildup.domain.employee.dto;

import com.concrete.buildup.domain.auth.entity.Employee;
import com.concrete.buildup.domain.contract.entity.Contract;
import com.concrete.buildup.global.util.MaskingUtil;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사원 상세 조회 응답 DTO
 *
 * <p>Figma 모달의 모든 필드를 포함:</p>
 * <ul>
 *   <li>기본 정보 (이름, 주민번호, 연락처, 이메일, 주소)</li>
 *   <li>근무 정보 (입사일/퇴사일)</li>
 *   <li>사원 구분 (상용직/일용직)</li>
 *   <li>비상 연락망</li>
 * </ul>
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EmployeeDetailResponseDto {

    /**
     * 사원 ID
     */
    private Long employeeId;

    /**
     * 사원 이름
     */
    private String name;

    /**
     * 주민등록번호 (마스킹 처리됨)
     * 예: "204205-4******"
     */
    private String residentId;

    /**
     * 사원 구분
     * - DAILY: 일용직
     * - PERMANENT: 상용직
     */
    private String empType;

    /**
     * 연락처
     */
    private String phone;

    /**
     * 이메일
     */
    private String email;

    /**
     * 주소
     */
    private String address;

    /**
     * 비상 연락망
     */
    private String emergencyContact;

    /**
     * 입사일 (yyyy-MM-dd)
     */
    private String joinedAt;

    /**
     * 퇴사일 (yyyy-MM-dd)
     */
    private String leftAt;

    /**
     * Employee 엔티티를 DTO로 변환
     *
     * <p>최근 계약 정보가 있으면 입사일/퇴사일을 포함합니다.</p>
     *
     * @param employee Employee 엔티티
     * @param latestContract 최근 계약 (optional)
     * @return EmployeeDetailResponseDto
     */
    public static EmployeeDetailResponseDto from(Employee employee, Contract latestContract) {
        if (employee == null) {
            return null;
        }

        return EmployeeDetailResponseDto.builder()
            .employeeId(employee.getId())
            .name(employee.getEmpName())
            .residentId(MaskingUtil.maskResidentNumber(employee.getResidentNum()))
            .empType(employee.getEmpType())
            .phone(employee.getUser().getPhone())
            .email(employee.getUser().getEmail())
            .address(employee.getEmpAddress())
            .emergencyContact(employee.getSubPhone())
            .joinedAt(latestContract != null && latestContract.getEmployeeStartDate() != null
                ? latestContract.getEmployeeStartDate().toString()
                : null)
            .leftAt(latestContract != null && latestContract.getEmployeeEndDate() != null
                ? latestContract.getEmployeeEndDate().toString()
                : null)
            .build();
    }
}
