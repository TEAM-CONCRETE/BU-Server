package com.concrete.buildup.domain.employee.controller;

import com.concrete.buildup.domain.employee.dto.MyAttendanceListResponse;
import com.concrete.buildup.domain.employee.dto.MyHomeResponse;
import com.concrete.buildup.domain.employee.service.EmployeeMyService;
import com.concrete.buildup.global.common.ApiResponse;
import com.concrete.buildup.global.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 근로자 본인 정보 조회 컨트롤러
 *
 * <p>근로자가 본인의 출퇴근 내역, 급여 내역 등을 조회하는 API를 제공합니다.</p>
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Slf4j
@RestController
@RequestMapping("/v1/employees/me")
@RequiredArgsConstructor
@Tag(name = "Employee My", description = "근로자 본인 정보 조회 API")
public class EmployeeMyController {

    private final EmployeeMyService employeeMyService;

    /**
     * 본인 출퇴근 내역 조회 API
     *
     * <p>로그인한 근로자 본인의 출퇴근 기록을 조회합니다.</p>
     *
     * @param pageable 페이지네이션 정보 (기본값: page=0, size=20)
     * @return MyAttendanceListResponse - 출퇴근 내역 목록
     */
    @Operation(
            summary = "본인 출퇴근 내역 조회",
            description = """
                로그인한 근로자 본인의 출퇴근 내역을 조회합니다.

                **조회 정보:**
                - 날짜
                - 현장명
                - 출근 시간
                - 퇴근 시간 (퇴근 전이면 null)
                - 근태 상태 (WORKING: 근무중, COMPLETED: 퇴근완료, INCOMPLETE: 미퇴근)
                - 지각 여부

                **조회 범위:** 최근 6개월

                **정렬:** 최신 날짜순 (내림차순)
                """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = MyAttendanceListResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "권한 없음 (EMPLOYEE 역할 필요)"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "근로자 정보를 찾을 수 없음"
            )
    })
    @PreAuthorize("hasRole('EMPLOYEE')")
    @GetMapping("/attendance")
    public ResponseEntity<ApiResponse<MyAttendanceListResponse>> getMyAttendanceList(
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @PageableDefault(size = 20) Pageable pageable
    ) {
        log.info("본인 출퇴근 내역 조회 API 호출: page={}, size={}",
                pageable.getPageNumber(), pageable.getPageSize());

        // JWT에서 userId 추출
        String currentUserId = SecurityUtil.getCurrentUserId();

        MyAttendanceListResponse response = employeeMyService.getMyAttendanceList(currentUserId, pageable);

        log.info("본인 출퇴근 내역 조회 완료: totalElements={}", response.getTotalElements());

        return ResponseEntity.ok(
                ApiResponse.success(response, "출퇴근 내역을 조회했습니다")
        );
    }

    /**
     * 홈 화면 정보 조회 API
     *
     * <p>로그인한 근로자의 홈 화면에 표시할 정보를 조회합니다.</p>
     *
     * @return MyHomeResponse - 홈 화면 정보
     */
    @Operation(
            summary = "홈 화면 정보 조회",
            description = """
                로그인한 근로자의 홈 화면 정보를 조회합니다.

                **조회 정보:**
                - 미결 전자계약 (근로계약서 미서명, 안전교육일지 미서명)
                - 최근 급여 내역 (현장명, 지급일, 실지급액)
                - 금일 근태 (출근/퇴근 시간, 상태, 지각 여부)

                **참고:**
                - 미결 전자계약이 없으면 count=0, items=[]
                - 최근 급여가 없으면 recentSalary=null
                - 금일 출근 기록이 없으면 todayAttendance=null
                """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = MyHomeResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "권한 없음 (EMPLOYEE 역할 필요)"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "근로자 정보를 찾을 수 없음"
            )
    })
    @PreAuthorize("hasRole('EMPLOYEE')")
    @GetMapping("/home")
    public ResponseEntity<ApiResponse<MyHomeResponse>> getMyHome() {
        log.info("홈 화면 정보 조회 API 호출");

        // JWT에서 userId 추출
        String currentUserId = SecurityUtil.getCurrentUserId();

        MyHomeResponse response = employeeMyService.getMyHome(currentUserId);

        log.info("홈 화면 정보 조회 완료: pendingCount={}", response.getPendingContracts().getCount());

        return ResponseEntity.ok(
                ApiResponse.success(response, "홈 화면 정보를 조회했습니다")
        );
    }
}
