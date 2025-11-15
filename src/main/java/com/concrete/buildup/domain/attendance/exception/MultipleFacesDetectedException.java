package com.concrete.buildup.domain.attendance.exception;

/**
 * 여러 얼굴 검출 예외
 * Face API 응답에서 2명 이상의 얼굴이 검출되었을 때 발생합니다.
 */
public class MultipleFacesDetectedException extends RuntimeException {

    public MultipleFacesDetectedException(int count) {
        super(String.format("%d명의 얼굴이 감지되었습니다. 한 명만 촬영해주세요.", count));
    }

    public MultipleFacesDetectedException(String message) {
        super(message);
    }
}