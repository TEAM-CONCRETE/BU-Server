package com.concrete.buildup.domain.safetydoc.dto;

import com.concrete.buildup.domain.safetydoc.enums.SafetyEducationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SafetyEducationSignatureResponse {

    private Long safetyEducationLogId;
    private SafetyEducationStatus status;
    private String pdfUrl;
    private String pdfHash;
    private LocalDateTime signedAt;
}
