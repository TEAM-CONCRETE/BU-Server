package com.concrete.buildup.domain.safetydoc.dto;

import com.concrete.buildup.domain.safetydoc.enums.EducationType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(description = "안전교육일지 생성 요청")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateSafetyEducationLogRequest {

    @Schema(
            description = "교육 구분",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "REGULAR",
            allowableValues = {"REGULAR", "HIRING", "WORK_CHANGE", "SPECIAL", "ETC"}
    )
    @NotNull(message = "교육 구분은 필수입니다.")
    private EducationType educationType;

    @Schema(
            description = "교육 과목명 (예: 추락 재해 예방, 전기 안전 등)",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "추락 재해 예방 교육",
            maxLength = 200
    )
    @NotBlank(message = "교육과목은 필수입니다.")
    @Size(max = 200, message = "교육과목은 200자 이내로 입력해주세요.")
    private String educationSubject;

    @Schema(
            description = "교육 내용 상세 (교육에서 다룬 주요 내용을 기술)",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = """
                    1. 추락 재해 현황 및 사례
                    2. 추락 방지 시설 점검 요령
                    3. 개인 보호구 착용 방법
                    4. 비상 시 대응 절차
                    """
    )
    @NotBlank(message = "교육내용은 필수입니다.")
    private String educationContent;

    @Schema(
            description = "교육 실시자 성명 (교육을 진행한 담당자)",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "김안전",
            maxLength = 50
    )
    @NotBlank(message = "교육 실시자 성명은 필수입니다.")
    @Size(max = 50, message = "교육 실시자 성명은 50자 이내로 입력해주세요.")
    private String instructorName;

    @Schema(
            description = "교육 실시 장소",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "현장 사무실 회의실",
            maxLength = 200
    )
    @NotBlank(message = "교육 실시 장소는 필수입니다.")
    @Size(max = 200, message = "교육 실시 장소는 200자 이내로 입력해주세요.")
    private String educationLocation;

    @Schema(
            description = "교육 대상자(근로자) ID 목록. 최소 1명 이상 필수. 근로자 목록 조회 API(/employees)에서 조회한 employeeId를 사용",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "[1, 2, 3, 5, 8]"
    )
    @NotEmpty(message = "교육 대상자는 최소 1명 이상이어야 합니다.")
    private List<Long> attendeeEmployeeIds;
}
