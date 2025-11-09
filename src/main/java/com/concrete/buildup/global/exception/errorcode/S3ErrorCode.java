package com.concrete.buildup.global.exception.errorcode;

import com.concrete.buildup.global.exception.BaseErrorCode;
import com.concrete.buildup.global.exception.ErrorCategory;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * S3 관련 에러 코드 (900-999)
 * 파일 업로드/다운로드 등 S3 관련 작업에서 발생하는 에러를 정의합니다.
 */
@Getter
@RequiredArgsConstructor
public enum S3ErrorCode implements BaseErrorCode {

    // 400 Bad Request
    INVALID_FILE_EXTENSION(HttpStatus.BAD_REQUEST, 900, "지원하지 않는 파일 형식입니다."),
    FILE_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, 901, "파일 크기가 제한을 초과했습니다."),
    INVALID_RESOURCE_TYPE(HttpStatus.BAD_REQUEST, 902, "올바르지 않은 리소스 타입입니다."),

    // 404 Not Found
    FILE_NOT_FOUND(HttpStatus.NOT_FOUND, 904, "파일을 찾을 수 없습니다."),

    // 500 Internal Server Error
    PRESIGNED_URL_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 950, "Presigned URL 생성에 실패했습니다."),
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 951, "파일 업로드에 실패했습니다."),
    FILE_DOWNLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 952, "파일 다운로드에 실패했습니다."),
    FILE_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 953, "파일 삭제에 실패했습니다."),
    S3_CONNECTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 954, "S3 연결에 실패했습니다.");

    private final HttpStatus httpStatus;
    private final int codeNumber;
    private final String message;

    @Override
    public String getCode() {
        return ErrorCategory.COMMON.generate(codeNumber);
    }
}
