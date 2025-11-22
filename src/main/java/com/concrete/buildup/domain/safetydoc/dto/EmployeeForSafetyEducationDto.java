package com.concrete.buildup.domain.safetydoc.dto;

import com.concrete.buildup.domain.contract.enums.EmpType;
import com.concrete.buildup.domain.employee.dto.EmployeeListResponseDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeForSafetyEducationDto {

    private Long employeeId;
    private String empName;
    private String empType;
    private String residentNum;
    private Boolean hasSafetyEducation;

    public static EmployeeForSafetyEducationDto fromDto(EmployeeListResponseDto dto, boolean hasSafetyEducation) {
        return EmployeeForSafetyEducationDto.builder()
                .employeeId(dto.getEmployeeId())
                .empName(dto.getName())
                .empType(dto.getEmpType())
                .residentNum(dto.getResidentId())
                .hasSafetyEducation(hasSafetyEducation)
                .build();
    }

    public EmpType getEmpTypeEnum() {
        return empType != null ? EmpType.valueOf(empType) : null;
    }
}
