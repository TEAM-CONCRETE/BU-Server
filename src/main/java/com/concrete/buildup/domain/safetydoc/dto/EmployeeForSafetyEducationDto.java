package com.concrete.buildup.domain.safetydoc.dto;

import com.concrete.buildup.domain.contract.enums.EmpType;
import com.concrete.buildup.domain.employee.dto.EmployeeListResponseDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "안전교육 대상자용 근로자 정보")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeForSafetyEducationDto {

    @Schema(
            description = "근로자(Employee) ID. 안전교육일지 생성 시 attendeeEmployeeIds에 이 값을 사용",
            example = "1"
    )
    private Long employeeId;

    @Schema(description = "근로자 성명", example = "홍길동")
    private String empName;

    @Schema(
            description = "근로자 유형 (PERMANENT: 상용직, DAILY: 일용직)",
            example = "PERMANENT",
            allowableValues = {"PERMANENT", "DAILY"}
    )
    private String empType;

    @Schema(
            description = "주민등록번호 (마스킹 처리됨). 생년월일과 성별 확인용",
            example = "900101-1******"
    )
    private String residentNum;

    @Schema(
            description = "금일 안전교육 이수 여부. true이면 이미 오늘 안전교육을 받은 근로자",
            example = "false"
    )
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
