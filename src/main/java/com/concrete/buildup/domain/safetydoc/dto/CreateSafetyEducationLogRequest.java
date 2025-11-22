package com.concrete.buildup.domain.safetydoc.dto;

import com.concrete.buildup.domain.safetydoc.enums.EducationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateSafetyEducationLogRequest {

    @NotNull(message = "교육 구분은 필수입니다.")
    private EducationType educationType;

    @NotBlank(message = "교육과목은 필수입니다.")
    @Size(max = 200, message = "교육과목은 200자 이내로 입력해주세요.")
    private String educationSubject;

    @NotBlank(message = "교육내용은 필수입니다.")
    private String educationContent;

    @NotBlank(message = "교육 실시자 성명은 필수입니다.")
    @Size(max = 50, message = "교육 실시자 성명은 50자 이내로 입력해주세요.")
    private String instructorName;

    @NotBlank(message = "교육 실시 장소는 필수입니다.")
    @Size(max = 200, message = "교육 실시 장소는 200자 이내로 입력해주세요.")
    private String educationLocation;

    @NotEmpty(message = "교육 대상자는 최소 1명 이상이어야 합니다.")
    private List<Long> attendeeEmployeeIds;
}
