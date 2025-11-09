package com.concrete.buildup.domain.contract.service;

import com.concrete.buildup.domain.contract.entity.Contract;
import com.concrete.buildup.domain.contract.entity.ContractDetail;
import com.concrete.buildup.global.service.PdfService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 계약서 PDF 생성 서비스
 *
 * <p>계약서 전용 PDF 생성 기능을 제공합니다.</p>
 * <p>공통 PDF 생성 로직은 {@link PdfService}를 사용합니다.</p>
 *
 * <p>주요 기능:</p>
 * <ul>
 *   <li>계약서 초안 PDF 생성 (v1): Contract와 ContractDetail 데이터를 HTML 템플릿에 반영하여 PDF 생성</li>
 *   <li>PDF에 서명 이미지 스탬핑 (v2, v3): 기존 PDF에 서명 이미지를 지정된 위치에 삽입</li>
 * </ul>
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContractPdfService {

    private final PdfService pdfService;

    /**
     * 계약서 초안 PDF 생성 (v1)
     *
     * <p>Contract와 ContractDetail의 모든 데이터를 HTML 템플릿에 반영하여 PDF를 생성합니다.</p>
     *
     * @param contract 계약 엔티티
     * @param detail 계약 상세 엔티티
     * @return PDF 바이트 배열
     */
    public byte[] generateContractPdf(Contract contract, ContractDetail detail) {
        log.info("계약서 PDF 생성 시작: contractId={}", contract.getId());

        // 공통 PDF 서비스를 사용하여 템플릿 렌더링 및 PDF 변환
        Map<String, Object> variables = Map.of(
                "contract", contract,
                "detail", detail
        );

        return pdfService.generatePdfFromTemplate("contract/contract-pdf", variables);
    }

    /**
     * PDF에 서명 이미지 스탬핑 (v2, v3)
     *
     * <p>기존 PDF에 서명 이미지를 지정된 위치에 삽입합니다.</p>
     *
     * @param originalPdfBytes 원본 PDF 바이트 배열
     * @param signatureImageBytes 서명 이미지 바이트 배열
     * @param x 서명 X 좌표 (PDF pt 단위)
     * @param y 서명 Y 좌표 (PDF pt 단위)
     * @param width 서명 이미지 너비 (PDF pt 단위)
     * @param height 서명 이미지 높이 (PDF pt 단위)
     * @return 서명이 스탬핑된 PDF 바이트 배열
     */
    public byte[] stampSignatureOnPdf(
            byte[] originalPdfBytes,
            byte[] signatureImageBytes,
            BigDecimal x,
            BigDecimal y,
            BigDecimal width,
            BigDecimal height
    ) {
        log.info("계약서 PDF에 서명 스탬핑 시작: x={}, y={}, width={}, height={}", x, y, width, height);

        // 공통 PDF 서비스를 사용하여 이미지 스탬핑
        return pdfService.stampImageOnPdf(originalPdfBytes, signatureImageBytes, x, y, width, height);
    }
}
