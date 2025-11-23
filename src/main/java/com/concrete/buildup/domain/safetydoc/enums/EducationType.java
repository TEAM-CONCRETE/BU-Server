package com.concrete.buildup.domain.safetydoc.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 교육 구분 열거형
 *
 * 산업안전보건법에 따른 안전보건교육 종류
 */
@Schema(description = """
        교육 구분 (산업안전보건법 기준)

        | 값 | 설명 | 교육 시기 |
        |---|---|---|
        | REGULAR | 정기교육 | 매월/분기별 정기 교육 |
        | HIRING | 채용 시 교육 | 신규 채용 시 |
        | WORK_CHANGE | 작업내용 변경 시 교육 | 작업 내용 변경 시 |
        | SPECIAL | 특별교육 | 유해·위험 작업 종사 시 |
        | ETC | 기타 | 기타 안전교육 |
        """)
@Getter
@RequiredArgsConstructor
public enum EducationType {

    @Schema(description = "정기교육 - 매월 또는 분기별로 실시하는 정기 안전보건교육")
    REGULAR("정기교육"),

    @Schema(description = "채용 시 교육 - 신규 채용된 근로자에게 실시하는 교육")
    HIRING("채용 시 교육"),

    @Schema(description = "작업내용 변경 시 교육 - 작업 내용이 변경될 때 실시하는 교육")
    WORK_CHANGE("작업내용 변경 시 교육"),

    @Schema(description = "특별교육 - 유해·위험 작업 종사 근로자 대상 특별 교육")
    SPECIAL("특별교육"),

    @Schema(description = "기타 - 위 분류에 해당하지 않는 기타 안전교육")
    ETC("기타");

    private final String description;
}
