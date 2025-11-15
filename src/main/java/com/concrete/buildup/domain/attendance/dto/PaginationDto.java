package com.concrete.buildup.domain.attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaginationDto {
    private Integer currentPage;
    private Integer totalPages;
    private Long totalRecords;
    private Integer pageSize;
}
