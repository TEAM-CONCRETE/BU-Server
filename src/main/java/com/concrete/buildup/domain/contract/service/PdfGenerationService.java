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
 * PDF 생성 전문 서비스
 *
 * <p>계약서 PDF 생성 및 서명 이미지 스탬핑 기능을 제공합니다.</p>
 * <p>PdfService를 활용하여 계약 도메인에 특화된 PDF 생성 로직을 처리합니다.</p>
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PdfGenerationService {

    private final PdfService pdfService;

    /**
     * 계약서 PDF 생성
     *
     * <p>Contract와 ContractDetail 정보를 기반으로 PDF를 생성합니다.</p>
     *
     * @param contract 계약 엔티티
     * @param contractDetail 계약 상세 엔티티
     * @return PDF 바이트 배열
     */
    public byte[] generateContractPdf(Contract contract, ContractDetail contractDetail) {
        log.info("계약서 PDF 생성 시작: contractId={}", contract.getId());

        // Thymeleaf 템플릿에 전달할 변수 맵 생성
        Map<String, Object> variables = Map.of(
                "contract", contract,
                "detail", contractDetail
        );

        // HTML 템플릿을 PDF로 변환
        byte[] pdfBytes = pdfService.generatePdfFromTemplate("contract/contract-pdf", variables);

        log.info("계약서 PDF 생성 완료: contractId={}, size={} bytes", contract.getId(), pdfBytes.length);

        return pdfBytes;
    }

    /**
     * PDF에 서명 이미지 스탬핑
     *
     * <p>기존 PDF에 서명 이미지를 지정된 위치에 삽입합니다.</p>
     *
     * @param originalPdfBytes 원본 PDF 바이트 배열
     * @param signatureImageBytes 서명 이미지 바이트 배열
     * @param x PDF X 좌표 (pt)
     * @param y PDF Y 좌표 (pt)
     * @param width 이미지 너비 (pt)
     * @param height 이미지 높이 (pt)
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
        log.info("PDF 서명 스탬핑 시작: x={}, y={}, width={}, height={}", x, y, width, height);

        byte[] stampedPdfBytes = pdfService.stampImageOnPdf(
                originalPdfBytes,
                signatureImageBytes,
                x, y, width, height
        );

        log.info("PDF 서명 스탬핑 완료: size={} bytes", stampedPdfBytes.length);

        return stampedPdfBytes;
    }
}
