package com.concrete.buildup.domain.employee.service;

import com.concrete.buildup.domain.contract.enums.EmpType;
import com.concrete.buildup.domain.employee.dto.EmployeeListResponseDto;
import com.concrete.buildup.domain.employee.dto.EmployeePageResponseDto;
import com.concrete.buildup.domain.employee.repository.EmployeeQueryRepository;
import com.concrete.buildup.domain.site.repository.SiteRepository;
import com.concrete.buildup.global.exception.BusinessException;
import com.concrete.buildup.global.exception.errorcode.SiteErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * EmployeeService 단위 테스트
 *
 * @author Build-Up Team
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EmployeeService 테스트")
class EmployeeServiceTest {

    @Mock
    private EmployeeQueryRepository employeeQueryRepository;

    @Mock
    private SiteRepository siteRepository;

    @InjectMocks
    private EmployeeService employeeService;

    @Test
    @DisplayName("사원 목록 조회 성공")
    void getEmployeesBySite_Success() {
        // given
        Long siteId = 1L;
        int page = 1;
        int size = 10;

        List<EmployeeListResponseDto> employees = List.of(
                new EmployeeListResponseDto(1L, "홍길동", "010101-1234567", EmpType.PERMANENT),
                new EmployeeListResponseDto(2L, "김철수", "020202-2345678", EmpType.DAILY)
        );

        Page<EmployeeListResponseDto> employeePage = new PageImpl<>(
                employees,
                PageRequest.of(0, size),
                2
        );

        given(siteRepository.existsById(siteId)).willReturn(true);
        given(employeeQueryRepository.findBySiteId(eq(siteId), eq(null), eq(null), any(Pageable.class)))
                .willReturn(employeePage);

        // when
        EmployeePageResponseDto response = employeeService.getEmployeesBySite(
                siteId, null, null, page, size
        );

        // then
        assertThat(response).isNotNull();
        assertThat(response.getTotalCount()).isEqualTo(2);
        assertThat(response.getPage()).isEqualTo(1);
        assertThat(response.getSize()).isEqualTo(10);
        assertThat(response.getData()).hasSize(2);
        assertThat(response.getData().get(0).getName()).isEqualTo("홍길동");
        assertThat(response.getData().get(0).getResidentId()).isEqualTo("010101-1******");

        verify(siteRepository, times(1)).existsById(siteId);
        verify(employeeQueryRepository, times(1)).findBySiteId(eq(siteId), eq(null), eq(null), any(Pageable.class));
    }

    @Test
    @DisplayName("사원 목록 조회 성공 - empType 필터 적용")
    void getEmployeesBySite_Success_WithEmpTypeFilter() {
        // given
        Long siteId = 1L;
        String empTypeStr = "DAILY";
        int page = 1;
        int size = 10;

        List<EmployeeListResponseDto> employees = List.of(
                new EmployeeListResponseDto(2L, "김철수", "020202-2345678", EmpType.DAILY)
        );

        Page<EmployeeListResponseDto> employeePage = new PageImpl<>(
                employees,
                PageRequest.of(0, size),
                1
        );

        given(siteRepository.existsById(siteId)).willReturn(true);
        given(employeeQueryRepository.findBySiteId(eq(siteId), eq(EmpType.DAILY), eq(null), any(Pageable.class)))
                .willReturn(employeePage);

        // when
        EmployeePageResponseDto response = employeeService.getEmployeesBySite(
                siteId, empTypeStr, null, page, size
        );

        // then
        assertThat(response).isNotNull();
        assertThat(response.getTotalCount()).isEqualTo(1);
        assertThat(response.getData()).hasSize(1);
        assertThat(response.getData().get(0).getEmpType()).isEqualTo("DAILY");

        verify(employeeQueryRepository, times(1)).findBySiteId(eq(siteId), eq(EmpType.DAILY), eq(null), any(Pageable.class));
    }

    @Test
    @DisplayName("사원 목록 조회 성공 - 이름 검색 적용")
    void getEmployeesBySite_Success_WithNameSearch() {
        // given
        Long siteId = 1L;
        String name = "홍";
        int page = 1;
        int size = 10;

        List<EmployeeListResponseDto> employees = List.of(
                new EmployeeListResponseDto(1L, "홍길동", "010101-1234567", EmpType.PERMANENT)
        );

        Page<EmployeeListResponseDto> employeePage = new PageImpl<>(
                employees,
                PageRequest.of(0, size),
                1
        );

        given(siteRepository.existsById(siteId)).willReturn(true);
        given(employeeQueryRepository.findBySiteId(eq(siteId), eq(null), eq(name), any(Pageable.class)))
                .willReturn(employeePage);

        // when
        EmployeePageResponseDto response = employeeService.getEmployeesBySite(
                siteId, null, name, page, size
        );

        // then
        assertThat(response).isNotNull();
        assertThat(response.getTotalCount()).isEqualTo(1);
        assertThat(response.getData()).hasSize(1);
        assertThat(response.getData().get(0).getName()).isEqualTo("홍길동");

        verify(employeeQueryRepository, times(1)).findBySiteId(eq(siteId), eq(null), eq(name), any(Pageable.class));
    }

    @Test
    @DisplayName("사원 목록 조회 실패 - 현장 없음")
    void getEmployeesBySite_Fail_SiteNotFound() {
        // given
        Long siteId = 999L;
        int page = 1;
        int size = 10;

        given(siteRepository.existsById(siteId)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> employeeService.getEmployeesBySite(siteId, null, null, page, size))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", SiteErrorCode.SITE_NOT_FOUND);

        verify(siteRepository, times(1)).existsById(siteId);
        verify(employeeQueryRepository, never()).findBySiteId(any(), any(), any(), any());
    }

    @Test
    @DisplayName("사원 목록 조회 성공 - 잘못된 empType 무시")
    void getEmployeesBySite_Success_InvalidEmpTypeIgnored() {
        // given
        Long siteId = 1L;
        String empTypeStr = "INVALID";
        int page = 1;
        int size = 10;

        List<EmployeeListResponseDto> employees = List.of(
                new EmployeeListResponseDto(1L, "홍길동", "010101-1234567", EmpType.PERMANENT)
        );

        Page<EmployeeListResponseDto> employeePage = new PageImpl<>(
                employees,
                PageRequest.of(0, size),
                1
        );

        given(siteRepository.existsById(siteId)).willReturn(true);
        given(employeeQueryRepository.findBySiteId(eq(siteId), eq(null), eq(null), any(Pageable.class)))
                .willReturn(employeePage);

        // when
        EmployeePageResponseDto response = employeeService.getEmployeesBySite(
                siteId, empTypeStr, null, page, size
        );

        // then
        assertThat(response).isNotNull();
        assertThat(response.getTotalCount()).isEqualTo(1);

        // 잘못된 empType은 null로 처리됨
        verify(employeeQueryRepository, times(1)).findBySiteId(eq(siteId), eq(null), eq(null), any(Pageable.class));
    }

    @Test
    @DisplayName("사원 목록 조회 성공 - 빈 결과")
    void getEmployeesBySite_Success_EmptyResult() {
        // given
        Long siteId = 1L;
        int page = 1;
        int size = 10;

        Page<EmployeeListResponseDto> emptyPage = new PageImpl<>(
                List.of(),
                PageRequest.of(0, size),
                0
        );

        given(siteRepository.existsById(siteId)).willReturn(true);
        given(employeeQueryRepository.findBySiteId(eq(siteId), eq(null), eq(null), any(Pageable.class)))
                .willReturn(emptyPage);

        // when
        EmployeePageResponseDto response = employeeService.getEmployeesBySite(
                siteId, null, null, page, size
        );

        // then
        assertThat(response).isNotNull();
        assertThat(response.getTotalCount()).isEqualTo(0);
        assertThat(response.getData()).isEmpty();

        verify(employeeQueryRepository, times(1)).findBySiteId(eq(siteId), eq(null), eq(null), any(Pageable.class));
    }
}
