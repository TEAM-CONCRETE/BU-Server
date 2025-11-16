package com.concrete.buildup.domain.attendance.exception;

/**
 * 얼굴 미검출 예외
 * Face API 응답에서 얼굴이 하나도 검출되지 않았을 때 발생합니다.
 */
public class FaceNotDetectedException extends RuntimeException {

    public FaceNotDetectedException() {
        super("얼굴이 감지되지 않았습니다. 다시 촬영해주세요.");
    }

    public FaceNotDetectedException(String message) {
        super(message);
    }
}
