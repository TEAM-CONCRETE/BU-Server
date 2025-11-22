package com.concrete.buildup.domain.safetydoc.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 교육 구분 열거형
 */
@Getter
@RequiredArgsConstructor
public enum EducationType {
    REGULAR("정기교육"),
    HIRING("채용 시 교육"),
    WORK_CHANGE("작업내용 변경 시 교육"),
    SPECIAL("특별교육"),
    ETC("기타");

    private final String description;
}
