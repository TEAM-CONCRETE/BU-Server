package com.concrete.buildup.domain.site.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 안전/작업 문서 목록 응답 DTO (페이지네이션 포함)
 */
@Schema(description = "안전/작업 문서 목록 응답")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SafetyWorkDocumentListResponse {

    @Schema(description = "문서 목록")
    private List<SafetyWorkDocumentDto> content;

    @Schema(description = "현재 페이지 번호 (0부터 시작)", example = "0")
    private int pageNumber;

    @Schema(description = "페이지 크기", example = "20")
    private int pageSize;

    @Schema(description = "전체 항목 수", example = "50")
    private long totalElements;

    @Schema(description = "전체 페이지 수", example = "3")
    private int totalPages;

    @Schema(description = "첫 페이지 여부", example = "true")
    private boolean first;

    @Schema(description = "마지막 페이지 여부", example = "false")
    private boolean last;

    public static SafetyWorkDocumentListResponse from(Page<SafetyWorkDocumentDto> page) {
        return SafetyWorkDocumentListResponse.builder()
                .content(page.getContent())
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }
}
