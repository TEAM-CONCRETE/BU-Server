package com.concrete.buildup.domain.attendance.dto;

import com.concrete.buildup.domain.attendance.enums.AttendanceType;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 출퇴근 검증 응답 DTO
 *
 * <p>얼굴 인식 검증 결과와 출퇴근 기록 정보를 담아 반환합니다.</p>
 *
 * <p>success는 API 호출 성공 여부이며, verified는 얼굴 인식 검증 통과 여부입니다.
 * verified가 false인 경우 recordId는 null일 수 있습니다.</p>
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "출퇴근 검증 응답")
public class AttendanceVerificationResponseDto {

    /**
     * API 호출 성공 여부
     */
    @Schema(description = "API 호출 성공 여부", example = "true")
    private Boolean success;

    /**
     * 얼굴 인식 검증 통과 여부
     *
     * <p>true: 등록된 얼굴과 일치하여 출퇴근 기록 완료</p>
     * <p>false: 얼굴 불일치 또는 기타 검증 실패</p>
     */
    @Schema(description = "얼굴 인식 검증 통과 여부", example = "true")
    private Boolean verified;

    /**
     * 출퇴근 기록 ID
     *
     * <p>검증 성공 시 생성된 출퇴근 기록의 ID입니다.
     * verified가 false인 경우 null일 수 있습니다.</p>
     */
    @Schema(description = "출퇴근 기록 ID (검증 성공 시)", example = "123")
    private Long recordId;

    /**
     * 근로자 이름
     */
    @Schema(description = "근로자 이름", example = "홍길동")
    private String employeeName;

    /**
     * 출퇴근 유형 (자동 판단 결과)
     *
     * <p>서버가 당일 마지막 기록을 조회하여 자동으로 판단한 결과입니다.</p>
     * <p>마지막 기록이 없거나 CHECK_OUT인 경우 → CHECK_IN</p>
     * <p>마지막 기록이 CHECK_IN인 경우 → CHECK_OUT</p>
     */
    @Schema(description = "출퇴근 유형 (자동 판단)", example = "CHECK_IN")
    private AttendanceType attendanceType;

    /**
     * 출퇴근 기록 시각
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "출퇴근 기록 시각", example = "2025-01-01T09:00:00")
    private LocalDateTime timestamp;

    /**
     * 얼굴 유사도 점수 (0.0 ~ 1.0)
     *
     * <p>Face API가 반환한 similarity_0_1 값입니다.
     * 0.35 기준으로 검증 통과/실패가 결정됩니다.</p>
     */
    @Schema(description = "얼굴 유사도 점수 (0.0 ~ 1.0)", example = "0.974841")
    private Double similarityScore;

    /**
     * 응답 메시지
     *
     * <p>검증 결과에 대한 설명 메시지입니다.</p>
     * <p>예: "출근이 정상적으로 기록되었습니다", "얼굴 인식에 실패했습니다" 등</p>
     */
    @Schema(description = "응답 메시지", example = "출근이 정상적으로 기록되었습니다")
    private String message;

    /**
     * 지각 여부
     *
     * <p>출근 시에만 판단되며, 계약서상 출근 시간 기준 +5분 초과 시 true입니다.</p>
     * <p>퇴근 기록의 경우 null 또는 false입니다.</p>
     */
    @Schema(description = "지각 여부 (출근 시에만 해당)", example = "false")
    private Boolean isLate;
}
