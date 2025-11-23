package com.concrete.buildup.domain.document.controller;

import com.concrete.buildup.domain.document.dto.DocumentUrlResponseDto;
import com.concrete.buildup.domain.document.service.DocumentService;
import com.concrete.buildup.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 문서 조회 Controller
 *
 * <p>근로계약서, 급여명세서 등의 PDF 문서 열람을 위한 Signed URL을 발급합니다.</p>
 */
@Slf4j
@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
@Tag(name = "Document", description = "문서 조회 API")
public class DocumentController {

    private final DocumentService documentService;

    /**
     * 근로계약서 PDF Signed URL 조회
     *
     * @param contractId 계약 ID
     * @return Signed URL 응답
     */
    @GetMapping("/contracts/{contractId}")
    @Operation(
        summary = "근로계약서 PDF 조회",
        description = """
            근로계약서 PDF 파일을 조회하기 위한 Signed URL을 발급합니다.

            **사용 방법:**
            - 발급된 URL을 iframe 또는 PDF 뷰어에서 사용
            - URL은 15분 후 만료됩니다.

            **S3 경로:**
            - contracts/{contractId}/signed_final.pdf
            """
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Signed URL 발급 성공",
            content = @Content(schema = @Schema(implementation = DocumentUrlResponseDto.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "계약 또는 문서를 찾을 수 없음"
        )
    })
    public ResponseEntity<ApiResponse<DocumentUrlResponseDto>> getContractPdf(
            @Parameter(description = "계약 ID", required = true)
            @PathVariable Long contractId
    ) {
        log.info("근로계약서 PDF 조회 API 호출: contractId={}", contractId);

        DocumentUrlResponseDto response = documentService.getContractPdfUrl(contractId);

        return ResponseEntity.ok(ApiResponse.success(response, "근로계약서 PDF URL 발급에 성공했습니다"));
    }

    /**
     * 안전교육일지 PDF Signed URL 조회
     *
     * @param logId 안전교육일지 ID
     * @return Signed URL 응답
     */
    @GetMapping("/safety-education-logs/{logId}")
    @Operation(
        summary = "안전교육일지 PDF 조회 (미리보기/다운로드)",
        description = """
            안전교육일지 PDF 파일을 조회하기 위한 Signed URL을 발급합니다.

            **사용 방법:**
            - 발급된 URL을 iframe 또는 PDF 뷰어에서 사용 (미리보기)
            - 발급된 URL로 직접 다운로드 가능
            - URL은 15분 후 만료됩니다.

            **상태별 PDF 버전:**
            - MANAGER_SIGNING_PENDING: 초안 PDF
            - MANAGER_SIGNED: 관리자 서명 완료 PDF
            - COMPLETED: 최종 PDF (모든 참석자 서명 완료)

            **S3 경로:**
            - safety-docs/{siteId}/{date}/SE-{date}-{logId}.pdf
            - safety-docs/{siteId}/{date}/SE-{date}-{logId}-manager-signed.pdf
            - safety-docs/{siteId}/{date}/SE-{date}-{logId}-final.pdf
            """
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Signed URL 발급 성공",
            content = @Content(schema = @Schema(implementation = DocumentUrlResponseDto.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "안전교육일지 또는 문서를 찾을 수 없음"
        )
    })
    public ResponseEntity<ApiResponse<DocumentUrlResponseDto>> getSafetyEducationLogPdf(
            @Parameter(description = "안전교육일지 ID", required = true)
            @PathVariable Long logId
    ) {
        log.info("안전교육일지 PDF 조회 API 호출: logId={}", logId);

        DocumentUrlResponseDto response = documentService.getSafetyEducationLogPdfUrl(logId);

        return ResponseEntity.ok(ApiResponse.success(response, "안전교육일지 PDF URL 발급에 성공했습니다"));
    }

    /**
     * 급여명세서 PDF Signed URL 조회
     *
     * @param payrollId 급여 ID
     * @return Signed URL 응답
     */
    @GetMapping("/payslips/{payrollId}")
    @Operation(
        summary = "급여명세서 PDF 조회",
        description = """
            급여명세서 PDF 파일을 조회하기 위한 Signed URL을 발급합니다.

            **사용 방법:**
            - 발급된 URL을 iframe 또는 PDF 뷰어에서 사용
            - URL은 15분 후 만료됩니다.

            **S3 경로:**
            - payslips/{payrollId}/payslip.pdf
            """
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Signed URL 발급 성공",
            content = @Content(schema = @Schema(implementation = DocumentUrlResponseDto.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "급여명세서 또는 문서를 찾을 수 없음"
        )
    })
    public ResponseEntity<ApiResponse<DocumentUrlResponseDto>> getPayslipPdf(
            @Parameter(description = "급여 ID", required = true)
            @PathVariable Long payrollId
    ) {
        log.info("급여명세서 PDF 조회 API 호출: payrollId={}", payrollId);

        DocumentUrlResponseDto response = documentService.getPayslipPdfUrl(payrollId);

        return ResponseEntity.ok(ApiResponse.success(response, "급여명세서 PDF URL 발급에 성공했습니다"));
    }

    /**
     * 작업일보 PDF Signed URL 조회
     *
     * @param workReportId 작업일보 ID
     * @return Signed URL 응답
     */
    @GetMapping("/work-reports/{workReportId}")
    @Operation(
        summary = "작업일보 PDF 조회 (미리보기/다운로드)",
        description = """
            작업일보 PDF 파일을 조회하기 위한 Signed URL을 발급합니다.

            **사용 방법:**
            - 발급된 URL을 iframe 또는 PDF 뷰어에서 사용 (미리보기)
            - 발급된 URL로 직접 다운로드 가능
            - URL은 15분 후 만료됩니다.

            **S3 경로:**
            - work-reports/{siteId}/{date}/WR-{workReportId}.pdf
            """
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Signed URL 발급 성공",
            content = @Content(schema = @Schema(implementation = DocumentUrlResponseDto.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "작업일보 또는 문서를 찾을 수 없음"
        )
    })
    public ResponseEntity<ApiResponse<DocumentUrlResponseDto>> getWorkReportPdf(
            @Parameter(description = "작업일보 ID", required = true)
            @PathVariable Long workReportId
    ) {
        log.info("작업일보 PDF 조회 API 호출: workReportId={}", workReportId);

        DocumentUrlResponseDto response = documentService.getWorkReportPdfUrl(workReportId);

        return ResponseEntity.ok(ApiResponse.success(response, "작업일보 PDF URL 발급에 성공했습니다"));
    }
}