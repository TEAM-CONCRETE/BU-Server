package com.concrete.buildup.domain.contract.entity;

import com.concrete.buildup.domain.contract.enums.ContractState;
import com.concrete.buildup.domain.contract.enums.EmpType;
import com.concrete.buildup.global.exception.BusinessException;
import com.concrete.buildup.global.exception.errorcode.ContractErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Contract 엔티티의 최종 PDF 덮어쓰기 방지 테스트
 */
class ContractFinalPdfTest {

    @Test
    @DisplayName("최종 PDF를 처음 설정할 때는 정상적으로 저장된다")
    void updateFinalPdf_FirstTime_Success() {
        // given
        Contract contract = Contract.builder()
                .employeeId(1L)
                .corporationId(1L)
                .managerId(1L)
                .empType(EmpType.DAILY)
                .contractState(ContractState.EMPLOYEE_SIGNING_PENDING)
                .employeeStartDate(LocalDate.now())
                .writtenAt(LocalDateTime.now())
                .build();

        String pdfUrl = "https://s3.amazonaws.com/contracts/1/v3.pdf";
        String pdfHash = "abc123hash";

        // when
        contract.updateFinalPdf(pdfUrl, pdfHash);

        // then
        assertThat(contract.getFinalPdfUrl()).isEqualTo(pdfUrl);
        assertThat(contract.getFinalPdfHash()).isEqualTo(pdfHash);
        assertThat(contract.getPdfGeneratedAt()).isNotNull();
    }

    @Test
    @DisplayName("이미 최종 PDF가 설정된 경우 덮어쓰기를 시도하면 예외가 발생한다")
    void updateFinalPdf_AlreadySet_ThrowsException() {
        // given
        Contract contract = Contract.builder()
                .employeeId(1L)
                .corporationId(1L)
                .managerId(1L)
                .empType(EmpType.DAILY)
                .contractState(ContractState.EMPLOYEE_SIGNING_PENDING)
                .employeeStartDate(LocalDate.now())
                .writtenAt(LocalDateTime.now())
                .build();

        String firstPdfUrl = "https://s3.amazonaws.com/contracts/1/v3.pdf";
        String firstPdfHash = "abc123hash";
        contract.updateFinalPdf(firstPdfUrl, firstPdfHash);

        String secondPdfUrl = "https://s3.amazonaws.com/contracts/1/v3-new.pdf";
        String secondPdfHash = "def456hash";

        // when & then
        assertThatThrownBy(() -> contract.updateFinalPdf(secondPdfUrl, secondPdfHash))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ContractErrorCode.FINAL_PDF_ALREADY_SET.getMessage())
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ContractErrorCode.FINAL_PDF_ALREADY_SET);

        // 원래 값이 유지되는지 확인
        assertThat(contract.getFinalPdfUrl()).isEqualTo(firstPdfUrl);
        assertThat(contract.getFinalPdfHash()).isEqualTo(firstPdfHash);
    }

    @Test
    @DisplayName("finalPdfUrl만 설정된 경우에도 덮어쓰기가 방지된다")
    void updateFinalPdf_OnlyUrlSet_ThrowsException() {
        // given
        Contract contract = Contract.builder()
                .employeeId(1L)
                .corporationId(1L)
                .managerId(1L)
                .empType(EmpType.DAILY)
                .contractState(ContractState.EMPLOYEE_SIGNING_PENDING)
                .employeeStartDate(LocalDate.now())
                .writtenAt(LocalDateTime.now())
                .build();

        String firstPdfUrl = "https://s3.amazonaws.com/contracts/1/v3.pdf";
        String firstPdfHash = "abc123hash";
        contract.updateFinalPdf(firstPdfUrl, firstPdfHash);

        // when & then
        assertThatThrownBy(() -> contract.updateFinalPdf("new-url", "new-hash"))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ContractErrorCode.FINAL_PDF_ALREADY_SET.getMessage());
    }

    @Test
    @DisplayName("finalPdfHash만 설정된 경우에도 덮어쓰기가 방지된다")
    void updateFinalPdf_OnlyHashSet_ThrowsException() {
        // given
        Contract contract = Contract.builder()
                .employeeId(1L)
                .corporationId(1L)
                .managerId(1L)
                .empType(EmpType.DAILY)
                .contractState(ContractState.EMPLOYEE_SIGNING_PENDING)
                .employeeStartDate(LocalDate.now())
                .writtenAt(LocalDateTime.now())
                .build();

        String firstPdfUrl = "https://s3.amazonaws.com/contracts/1/v3.pdf";
        String firstPdfHash = "abc123hash";
        contract.updateFinalPdf(firstPdfUrl, firstPdfHash);

        // when & then
        assertThatThrownBy(() -> contract.updateFinalPdf("new-url", "new-hash"))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ContractErrorCode.FINAL_PDF_ALREADY_SET.getMessage());
    }
}
