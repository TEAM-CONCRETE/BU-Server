package com.concrete.buildup.domain.upload.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * S3 업로드 리소스 타입
 * 업로드되는 파일의 유형을 정의합니다.
 */
@Schema(description = """
        S3 업로드 리소스 타입

        ## 서명 업로드 지원 타입
        다음 리소스 타입은 서명 이미지 업로드(Presigned URL)를 지원합니다:
        - `CONTRACT`: 계약서 서명 (기업/근로자)
        - `WORK_REPORT`: 작업일보 서명 (관리자)
        - `SAFETY_DOC`: 안전교육일지 서명 (관리자/참석자)

        ## S3 경로 구조
        `uploads/{resourceType}/{resourceId}/signatures/{signerType}/{timestamp}.png`

        예시:
        - 계약서: `uploads/contracts/123/signatures/CORPORATION/1699000000000.png`
        - 안전교육일지: `uploads/safetydocs/1/signatures/MANAGER/1699000000000.png`
        """)
@Getter
@RequiredArgsConstructor
public enum ResourceType {

    @Schema(description = "계약서 - 기업/근로자 서명 이미지, 계약서 PDF 등")
    CONTRACT("contracts", "계약서"),

    @Schema(description = "작업일보 - 관리자 서명 이미지, 작업일보 PDF 등")
    WORK_REPORT("workreports", "작업일보"),

    @Schema(description = "안전교육일지 - 관리자/참석자 서명 이미지, 안전교육일지 PDF 등")
    SAFETY_DOC("safetydocs", "안전교육일지"),

    @Schema(description = "사원 얼굴 이미지 - 얼굴 인식 등록용 프로필 사진")
    EMPLOYEE_PROFILE("employee-profiles", "사원 얼굴 등록"),

    @Schema(description = "출퇴근 체크 촬영 이미지 - 얼굴 인식 검증용 촬영 사진")
    ATTENDANCE_PROBE("attendance-probes", "출퇴근 체크 촬영");

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