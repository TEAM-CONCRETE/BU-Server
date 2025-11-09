package com.concrete.buildup.domain.contract.dto;

import com.concrete.buildup.domain.contract.enums.ContractState;
import com.concrete.buildup.domain.contract.enums.EmpType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 계약 목록 조회 검색 조건 DTO
 *
 * <p>GET 요청의 Query Parameter를 바인딩하기 위한 DTO입니다.</p>
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractSearchCondition {

    /**
     * 근로자 ID (선택)
     */
    private Long employeeId;

    /**
     * 근로자 유형 (선택)
     * DAILY: 일용직, PERMANENT: 상용직
     */
    private EmpType empType;

    /**
     * 계약 상태 (선택)
     * DRAFT: 초안, PENDING: 대기중, FULLY_SIGNED: 완전서명
     */
    private ContractState status;

    /**
     * 계약 시작일 범위 시작 (선택)
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate from;

    /**
     * 계약 시작일 범위 종료 (선택)
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate to;

    /**
     * 페이지 번호 (기본값: 1)
     */
    @Min(value = 1, message = "페이지 번호는 1 이상이어야 합니다")
    @Builder.Default
    private int page = 1;

    /**
     * 페이지 크기 (기본값: 20, 최대: 100)
     */
    @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다")
    @Max(value = 100, message = "페이지 크기는 100 이하여야 합니다")
    @Builder.Default
    private int size = 20;

    /**
     * Spring Data Pageable을 위한 zero-based 페이지 인덱스 반환
     *
     * @return 0-based 페이지 인덱스
     */
    public int getPageIndex() {
        return page - 1;
    }
}
