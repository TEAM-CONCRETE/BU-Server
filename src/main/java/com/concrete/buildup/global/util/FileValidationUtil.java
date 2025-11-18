package com.concrete.buildup.global.util;

import com.concrete.buildup.global.exception.BusinessException;
import com.concrete.buildup.global.exception.errorcode.CommonErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

/**
 * 파일 검증 유틸리티
 *
 * <p>업로드되는 파일의 보안 검증을 수행합니다.</p>
 * <ul>
 *   <li>파일 존재 여부 검증</li>
 *   <li>파일 크기 검증</li>
 *   <li>MIME 타입 검증</li>
 *   <li>매직 넘버 검증 (실제 파일 포맷 확인)</li>
 * </ul>
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Slf4j
public class FileValidationUtil {

    /**
     * 최대 파일 크기 (5MB)
     */
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    /**
     * 허용되는 MIME 타입 목록
     */
    private static final List<String> ALLOWED_MIME_TYPES = Arrays.asList(
            "image/jpeg",
            "image/jpg",
            "image/png"
    );

    /**
     * JPEG 매직 넘버: FF D8 FF
     */
    private static final byte[] JPEG_MAGIC_NUMBERS = {
            (byte) 0xFF, (byte) 0xD8, (byte) 0xFF
    };

    /**
     * PNG 매직 넘버: 89 50 4E 47 0D 0A 1A 0A
     */
    private static final byte[] PNG_MAGIC_NUMBERS = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };

    /**
     * 이미지 파일 전체 검증
     *
     * <p>다음을 순차적으로 검증합니다:</p>
     * <ol>
     *   <li>파일 존재 여부</li>
     *   <li>파일 크기 (최대 5MB)</li>
     *   <li>MIME 타입 (image/jpeg, image/png)</li>
     *   <li>매직 넘버 (실제 이미지 파일인지)</li>
     * </ol>
     *
     * @param file 검증할 파일
     * @throws BusinessException 검증 실패 시
     */
    public static void validateImageFile(MultipartFile file) {
        // 1. 파일 존재 여부 검증 (NPE 방지를 위해 가장 먼저 수행)
        validateFileExists(file);

        log.debug("파일 검증 시작: originalFilename={}, size={}, contentType={}",
                file.getOriginalFilename(), file.getSize(), file.getContentType());

        // 2. 파일 크기 검증
        validateFileSize(file);

        // 3. MIME 타입 검증
        validateMimeType(file);

        // 4. 매직 넘버 검증 (실제 파일 포맷)
        validateMagicNumber(file);

        log.debug("파일 검증 완료: {}", file.getOriginalFilename());
    }

    /**
     * 파일 존재 여부 검증
     *
     * @param file 검증할 파일
     * @throws BusinessException 파일이 null이거나 비어있는 경우
     */
    private static void validateFileExists(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            log.warn("파일 검증 실패: 파일이 비어있음");
            throw new BusinessException(
                    CommonErrorCode.INVALID_INPUT_VALUE,
                    "파일이 비어있습니다."
            );
        }
    }

    /**
     * 파일 크기 검증
     *
     * @param file 검증할 파일
     * @throws BusinessException 파일 크기가 5MB를 초과하는 경우
     */
    private static void validateFileSize(MultipartFile file) {
        long fileSize = file.getSize();

        if (fileSize > MAX_FILE_SIZE) {
            log.warn("파일 크기 초과: size={} bytes, max={} bytes", fileSize, MAX_FILE_SIZE);
            throw new BusinessException(
                    CommonErrorCode.INVALID_INPUT_VALUE,
                    String.format("파일 크기가 너무 큽니다. (최대: %.1fMB, 현재: %.1fMB)",
                            MAX_FILE_SIZE / 1024.0 / 1024.0,
                            fileSize / 1024.0 / 1024.0)
            );
        }

        log.debug("파일 크기 검증 통과: {} bytes", fileSize);
    }

    /**
     * MIME 타입 검증
     *
     * @param file 검증할 파일
     * @throws BusinessException 허용되지 않는 MIME 타입인 경우
     */
    private static void validateMimeType(MultipartFile file) {
        String contentType = file.getContentType();

        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            log.warn("허용되지 않는 MIME 타입: {}", contentType);
            throw new BusinessException(
                    CommonErrorCode.INVALID_INPUT_VALUE,
                    String.format("허용되지 않는 파일 형식입니다. (허용: JPG, PNG, 현재: %s)", contentType)
            );
        }

        log.debug("MIME 타입 검증 통과: {}", contentType);
    }

    /**
     * 매직 넘버 검증 (실제 파일 포맷 확인)
     *
     * <p>파일의 실제 바이너리 헤더를 확인하여 진짜 이미지 파일인지 검증합니다.
     * MIME 타입은 클라이언트가 조작할 수 있지만, 매직 넘버는 파일 자체의 구조이므로 더 신뢰할 수 있습니다.</p>
     *
     * @param file 검증할 파일
     * @throws BusinessException 유효하지 않은 이미지 포맷인 경우
     */
    private static void validateMagicNumber(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            // 최대 8바이트 읽기 (PNG 매직 넘버가 8바이트)
            byte[] fileHeader = new byte[8];
            int bytesRead = inputStream.read(fileHeader);

            if (bytesRead < 3) {
                log.warn("파일 헤더를 읽을 수 없음: bytesRead={}", bytesRead);
                throw new BusinessException(
                        CommonErrorCode.INVALID_INPUT_VALUE,
                        "유효하지 않은 이미지 파일입니다."
                );
            }

            // JPEG 검증 (첫 3바이트)
            if (isJpeg(fileHeader)) {
                log.debug("매직 넘버 검증 통과: JPEG");
                return;
            }

            // PNG 검증 (첫 8바이트)
            if (bytesRead >= 8 && isPng(fileHeader)) {
                log.debug("매직 넘버 검증 통과: PNG");
                return;
            }

            // 둘 다 아닌 경우
            log.warn("매직 넘버 검증 실패: 지원되지 않는 포맷. Header: {}",
                    bytesToHex(fileHeader, bytesRead));
            throw new BusinessException(
                    CommonErrorCode.INVALID_INPUT_VALUE,
                    "유효하지 않은 이미지 파일입니다. (JPEG 또는 PNG 파일만 허용)"
            );

        } catch (IOException e) {
            log.error("파일 읽기 오류: {}", e.getMessage(), e);
            throw new BusinessException(
                    CommonErrorCode.INTERNAL_SERVER_ERROR,
                    "파일 검증 중 오류가 발생했습니다."
            );
        }
    }

    /**
     * JPEG 매직 넘버 확인
     *
     * @param fileHeader 파일 헤더 바이트 배열
     * @return JPEG 파일이면 true
     */
    private static boolean isJpeg(byte[] fileHeader) {
        return fileHeader[0] == JPEG_MAGIC_NUMBERS[0] &&
               fileHeader[1] == JPEG_MAGIC_NUMBERS[1] &&
               fileHeader[2] == JPEG_MAGIC_NUMBERS[2];
    }

    /**
     * PNG 매직 넘버 확인
     *
     * @param fileHeader 파일 헤더 바이트 배열
     * @return PNG 파일이면 true
     */
    private static boolean isPng(byte[] fileHeader) {
        for (int i = 0; i < PNG_MAGIC_NUMBERS.length; i++) {
            if (fileHeader[i] != PNG_MAGIC_NUMBERS[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * 바이트 배열을 16진수 문자열로 변환 (디버깅용)
     *
     * @param bytes 바이트 배열
     * @param length 변환할 길이
     * @return 16진수 문자열
     */
    private static String bytesToHex(byte[] bytes, int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(String.format("%02X ", bytes[i]));
        }
        return sb.toString().trim();
    }
}
