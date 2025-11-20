package com.concrete.buildup.domain.employee.dto;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 사원 목록 페이징 응답 DTO
 *
 * <p>사원 목록 조회 API의 페이징 응답 형식입니다.</p>
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Getter
@Builder
public class EmployeePageResponseDto {

    /**
     * 전체 데이터 개수
     */
    private final long totalCount;

    /**
     * 현재 페이지 번호 (1부터 시작)
     */
    private final int page;

    /**
     * 페이지당 데이터 개수
     */
    private final int size;

    /**
     * 사원 목록
     */
    private final List<EmployeeListResponseDto> data;

    /**
     * Page 객체로부터 응답 DTO 생성
     *
     * @param page Spring Data Page 객체
     * @return 페이징 응답 DTO
     */
    public static EmployeePageResponseDto from(Page<EmployeeListResponseDto> page) {
        return EmployeePageResponseDto.builder()
                .totalCount(page.getTotalElements())
                .page(page.getNumber() + 1)  // 0-based를 1-based로 변환
                .size(page.getSize())
                .data(page.getContent())
                .build();
    }
}