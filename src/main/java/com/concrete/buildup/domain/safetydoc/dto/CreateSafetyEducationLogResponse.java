package com.concrete.buildup.domain.safetydoc.dto;

import com.concrete.buildup.domain.safetydoc.enums.SafetyEducationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateSafetyEducationLogResponse {

    private Long safetyEducationLogId;
    private SafetyEducationStatus status;
    private String pdfUrl;
    private int attendeeCount;
}