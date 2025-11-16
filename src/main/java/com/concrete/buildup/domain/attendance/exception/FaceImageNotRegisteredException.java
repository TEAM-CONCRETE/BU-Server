package com.concrete.buildup.domain.attendance.exception;

/**
 * 얼굴 이미지 미등록 예외
 *
 * <p>근로자가 얼굴 이미지를 등록하지 않아 출퇴근 검증을 수행할 수 없을 때 발생합니다.</p>
 *
 * @author Build-Up Team
 * @since 1.0
 */
public class FaceImageNotRegisteredException extends RuntimeException {

    /**
     * 얼굴 이미지가 등록되지 않았을 때 발생하는 예외
     *
     * @param employeeId 근로자 ID
     */
    public FaceImageNotRegisteredException(Long employeeId) {
        super(String.format("근로자 ID %d의 얼굴 이미지가 등록되지 않았습니다. 먼저 얼굴을 등록해주세요.", employeeId));
    }

    /**
     * 얼굴 이미지가 등록되지 않았을 때 발생하는 예외 (상세 메시지)
     *
     * @param message 상세 메시지
     */
    public FaceImageNotRegisteredException(String message) {
        super(message);
    }
}