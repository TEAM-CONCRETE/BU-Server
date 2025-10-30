package com.concrete.buildup.global.common;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 페이징 응답 DTO
 *
 * Spring Data JPA의 Page 객체를 클라이언트 친화적인 형태로 변환합니다.
 *
 * 사용 예시:
 * <pre>
 * // Service에서 Page<Entity> 반환
 * Page<Employee> employeePage = employeeRepository.findAll(pageable);
 *
 * // Controller에서 Page<Response> 변환 후 PageResponse로 래핑
 * Page<EmployeeResponse> responsePage = employeePage.map(EmployeeResponse::from);
 * PageResponse<EmployeeResponse> pageResponse = PageResponse.from(responsePage);
 *
 * return ResponseEntity.ok(ApiResponse.success(pageResponse));
 * </pre>
 */
@Getter
@Builder
public class PageResponse<T> {

    /**
     * 현재 페이지의 데이터 목록
     */
    private final List<T> content;

    /**
     * 현재 페이지 번호 (0부터 시작)
     */
    private final int pageNumber;

    /**
     * 페이지당 데이터 개수
     */
    private final int pageSize;

    /**
     * 전체 데이터 개수
     */
    private final long totalElements;

    /**
     * 전체 페이지 수
     */
    private final int totalPages;

    /**
     * 첫 페이지 여부
     */
    private final boolean first;

    /**
     * 마지막 페이지 여부
     */
    private final boolean last;

    /**
     * 비어있는 페이지 여부
     */
    private final boolean empty;

    /**
     * Spring Data JPA의 Page 객체를 PageResponse로 변환
     *
     * @param page Spring Data JPA Page 객체
     * @return PageResponse
     */
    public static <T> PageResponse<T> from(Page<T> page) {
        return PageResponse.<T>builder()
                .content(page.getContent())
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .empty(page.isEmpty())
                .build();
    }

    /**
     * 빈 PageResponse 생성
     *
     * @return 빈 PageResponse
     */
    public static <T> PageResponse<T> empty() {
        return PageResponse.<T>builder()
                .content(List.of())
                .pageNumber(0)
                .pageSize(0)
                .totalElements(0)
                .totalPages(0)
                .first(true)
                .last(true)
                .empty(true)
                .build();
    }
}
