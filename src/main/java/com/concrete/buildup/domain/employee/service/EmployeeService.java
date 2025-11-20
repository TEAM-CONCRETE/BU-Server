package com.concrete.buildup.domain.employee.service;

import com.concrete.buildup.domain.contract.enums.EmpType;
import com.concrete.buildup.domain.employee.dto.EmployeeListResponseDto;
import com.concrete.buildup.domain.employee.dto.EmployeePageResponseDto;
import com.concrete.buildup.domain.employee.repository.EmployeeQueryRepository;
import com.concrete.buildup.domain.site.repository.SiteRepository;
import com.concrete.buildup.global.exception.BusinessException;
import com.concrete.buildup.global.exception.errorcode.EmployeeErrorCode;
import com.concrete.buildup.global.exception.errorcode.SiteErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사원 관리 Service
 *
 * <p>사원 목록 조회 등의 비즈니스 로직을 처리합니다.</p>
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmployeeService {

    private final EmployeeQueryRepository employeeQueryRepository;
    private final SiteRepository siteRepository;

    /**
     * 현장 기반 사원 목록 조회
     *
     * @param siteId 현장 ID
     * @param empTypeStr 근로자 유형 필터 (DAILY/PERMANENT, nullable)
     * @param name 이름 검색어 (nullable)
     * @param page 페이지 번호 (1부터 시작)
     * @param size 페이지 크기
     * @return 사원 목록 (페이징)
     */
    public EmployeePageResponseDto getEmployeesBySite(
            Long siteId,
            String empTypeStr,
            String name,
            int page,
            int size
    ) {
        log.info("사원 목록 조회: siteId={}, empType={}, name={}, page={}, size={}",
                siteId, empTypeStr, name, page, size);

        // 페이지 파라미터 검증
        if (page < 1) {
            throw new BusinessException(EmployeeErrorCode.INVALID_PAGE_NUMBER);
        }
        if (size < 1 || size > 100) {
            throw new BusinessException(EmployeeErrorCode.INVALID_PAGE_SIZE);
        }

        // 현장 존재 여부 확인
        if (!siteRepository.existsById(siteId)) {
            throw new BusinessException(SiteErrorCode.SITE_NOT_FOUND);
        }

        // EmpType 변환
        EmpType empType = parseEmpType(empTypeStr);

        // 페이지 번호 변환 (1-based → 0-based)
        Pageable pageable = PageRequest.of(page - 1, size);

        // 사원 목록 조회
        Page<EmployeeListResponseDto> employeePage = employeeQueryRepository.findBySiteId(
                siteId, empType, name, pageable
        );

        log.info("사원 목록 조회 완료: totalCount={}", employeePage.getTotalElements());

        return EmployeePageResponseDto.from(employeePage);
    }

    /**
     * 문자열을 EmpType enum으로 변환
     */
    private EmpType parseEmpType(String empTypeStr) {
        if (empTypeStr == null || empTypeStr.isBlank()) {
            return null;
        }

        try {
            return EmpType.valueOf(empTypeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("잘못된 empType 값: {}", empTypeStr);
            return null;
        }
    }
}