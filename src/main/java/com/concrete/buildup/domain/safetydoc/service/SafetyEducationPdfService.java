package com.concrete.buildup.domain.safetydoc.service;

import com.concrete.buildup.domain.safetydoc.entity.SafetyEducationAttendee;
import com.concrete.buildup.domain.safetydoc.entity.SafetyEducationLog;
import com.concrete.buildup.domain.site.entity.Site;
import com.concrete.buildup.global.exception.BusinessException;
import com.concrete.buildup.global.exception.errorcode.SafetyDocErrorCode;
import com.concrete.buildup.global.service.PdfService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SafetyEducationPdfService {

    private final PdfService pdfService;

    public byte[] generateSafetyEducationPdf(
            SafetyEducationLog safetyLog,
            Site site,
            String managerName,
            List<SafetyEducationAttendee> attendees
    ) {
        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("safetyLog", safetyLog);
            variables.put("site", site);
            variables.put("managerName", managerName != null ? managerName : "");
            variables.put("attendees", attendees != null ? attendees : List.of());

            return pdfService.generatePdfFromTemplate("safetydoc/safetydoc-pdf", variables);
        } catch (Exception e) {
            throw new BusinessException(
                    SafetyDocErrorCode.PDF_GENERATION_FAILED,
                    "안전교육일지 PDF 생성 중 오류가 발생했습니다: " + e.getMessage()
            );
        }
    }
}
