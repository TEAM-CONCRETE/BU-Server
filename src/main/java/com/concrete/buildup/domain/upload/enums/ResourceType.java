package com.concrete.buildup.domain.upload.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * S3 업로드 리소스 타입
 * 업로드되는 파일의 유형을 정의합니다.
 */
@Getter
@RequiredArgsConstructor
public enum ResourceType {

    /**
     * 계약서 관련 파일 (서명 이미지, PDF 등)
     */
    CONTRACT("contracts", "계약서"),

    /**
     * 작업일보 관련 파일
     */
    WORK_REPORT("workreports", "작업일보"),

    /**
     * 안전교육일지 관련 파일
     */
    SAFETY_DOC("safetydocs", "안전교육일지");

    /**
     * S3 경로에 사용될 폴더명
     * 예: uploads/contracts/123/...
     */
    private final String folderName;

    /**
     * 리소스 타입에 대한 한글 설명
     */
    private final String description;
}