package com.concrete.buildup.domain.upload.controller;

import com.concrete.buildup.domain.upload.dto.PresignedUrlRequest;
import com.concrete.buildup.domain.upload.dto.PresignedUrlResponse;
import com.concrete.buildup.domain.upload.service.S3Service;
import com.concrete.buildup.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 파일 업로드 API 컨트롤러
 * S3 Presigned URL 발급 및 파일 업로드 관련 API를 제공합니다.
 *
 * spring.cloud.aws.s3.enabled가 true로 설정되어 있을 때만 활성화됩니다.
 */
@Slf4j
@RestController
@RequestMapping("/v1/uploads")
@RequiredArgsConstructor
@Tag(name = "Upload", description = "파일 업로드 API")
@SecurityRequirement(name = "Bearer Authentication")
@ConditionalOnProperty(
        name = "spring.cloud.aws.s3.enabled",
        havingValue = "true",
        matchIfMissing = false
)
public class UploadController {

    private final S3Service s3Service;

    /**
     * 서명 이미지 업로드를 위한 Presigned URL 발급
     *
     * <p>클라이언트 업로드 플로우:</p>
     * <ol>
     *   <li>이 API를 호출하여 Presigned URL 발급</li>
     *   <li>발급받은 uploadUrl로 PUT 요청하여 파일 업로드</li>
     *   <li>업로드 완료 후 별도의 complete API 호출 (검증용)</li>
     * </ol>
     *
     * <p>업로드 예시 (JavaScript):</p>
     * <pre>
     * // 1. Presigned URL 발급
     * const response = await fetch('/api/v1/uploads/signatures', {
     *   method: 'POST',
     *   headers: { 'Content-Type': 'application/json' },
     *   body: JSON.stringify({
     *     resourceType: 'CONTRACT',
     *     resourceId: '123',
     *     signerRole: 'EMPLOYEE',
     *     fileExtension: 'png'
     *   })
     * });
     * const { uploadUrl } = await response.json();
     *
     * // 2. S3에 직접 업로드
     * await fetch(uploadUrl, {
     *   method: 'PUT',
     *   headers: { 'Content-Type': 'image/png' },
     *   body: imageFile
     * });
     * </pre>
     *
     * @param request Presigned URL 발급 요청 정보
     * @return Presigned URL 응답 (uploadUrl, expiresAt, s3Key, bucket)
     */
    @PostMapping("/signatures")
    @Operation(
            summary = "서명 이미지 업로드를 위한 Presigned URL 발급",
            description = """
                    클라이언트가 S3에 직접 서명 이미지를 업로드할 수 있는 임시 URL을 발급합니다.

                    **사용 방법:**
                    1. 이 API를 호출하여 Presigned URL을 발급받습니다.
                    2. 발급받은 uploadUrl로 PUT 요청하여 파일을 업로드합니다.
                    3. 업로드 완료 후 별도의 complete API를 호출합니다.

                    **주의사항:**
                    - URL은 15분간만 유효합니다.
                    - 지원 파일 형식: png, jpg, jpeg, pdf
                    - 파일명 규칙: uploads/{resourceType}/{resourceId}/{signerRole}.{ext}
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Presigned URL 발급 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PresignedUrlResponse.class),
                            examples = @ExampleObject(
                                    name = "성공 응답",
                                    value = """
                                            {
                                              "success": true,
                                              "message": "Presigned URL이 발급되었습니다.",
                                              "data": {
                                                "uploadUrl": "https://build-up-contracts.s3.ap-northeast-2.amazonaws.com/uploads/contracts/123/EMPLOYEE.png?signature=xxx",
                                                "expiresAt": "2025-11-05T14:25:00",
                                                "s3Key": "uploads/contracts/123/EMPLOYEE.png",
                                                "bucket": "build-up-contracts"
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (필수 파라미터 누락, 지원하지 않는 파일 형식)",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Validation 실패",
                                    value = """
                                            {
                                              "success": false,
                                              "message": "필드 검증에 실패했습니다.",
                                              "data": {
                                                "fileExtension": "지원하지 않는 파일 형식입니다. (png, jpg, jpeg, pdf만 가능)"
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Presigned URL 생성 실패 (S3 연결 오류 등)"
            )
    })
    public ResponseEntity<ApiResponse<PresignedUrlResponse>> generatePresignedUrl(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Presigned URL 발급 요청 정보",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = PresignedUrlRequest.class),
                            examples = @ExampleObject(
                                    name = "계약서 서명 이미지",
                                    value = """
                                            {
                                              "resourceType": "CONTRACT",
                                              "resourceId": "123",
                                              "signerRole": "EMPLOYEE",
                                              "fileExtension": "png"
                                            }
                                            """
                            )
                    )
            )
            @Valid @RequestBody PresignedUrlRequest request) {

        log.info("Presigned URL 발급 요청: resourceType={}, resourceId={}, signerRole={}, fileExtension={}",
                request.getResourceType(), request.getResourceId(), request.getSignerRole(), request.getFileExtension());

        PresignedUrlResponse response = s3Service.generatePresignedUrl(request);

        log.info("Presigned URL 발급 완료: s3Key={}, expiresAt={}",
                response.getS3Key(), response.getExpiresAt());

        return ResponseEntity.ok(
                ApiResponse.success(response, "Presigned URL이 발급되었습니다.")
        );
    }
}