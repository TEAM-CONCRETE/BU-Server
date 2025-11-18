package com.concrete.buildup.global.exception.errorcode;

import com.concrete.buildup.global.exception.BaseErrorCode;
import com.concrete.buildup.global.exception.ErrorCategory;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 근태 관리 에러 코드 (6000-6999)
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Getter
@RequiredArgsConstructor
public enum AttendanceErrorCode implements BaseErrorCode {

    // 400 Bad Request
    FACE_VERIFICATION_FAILED(HttpStatus.BAD_REQUEST, 6001, "얼굴 인식 검증에 실패했습니다."),
    FACE_IMAGE_NOT_REGISTERED(HttpStatus.BAD_REQUEST, 6002, "등록된 얼굴 이미지가 없습니다."),
    DUPLICATE_ATTENDANCE(HttpStatus.BAD_REQUEST, 6003, "이미 출퇴근 기록이 존재합니다."),

    // 404 Not Found
    ATTENDANCE_NOT_FOUND(HttpStatus.NOT_FOUND, 6004, "출퇴근 기록을 찾을 수 없습니다."),

    // 500 Internal Server Error
    FACE_API_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, 6005, "얼굴 인식 API 호출 중 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final int codeNumber;
    private final String message;

    @Override
    public String getCode() {
        return ErrorCategory.ATTENDANCE.generate(codeNumber);
    }
}
