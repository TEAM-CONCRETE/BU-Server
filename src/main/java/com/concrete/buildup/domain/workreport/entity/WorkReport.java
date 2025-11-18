package com.concrete.buildup.domain.workreport.entity;

import com.concrete.buildup.domain.auth.entity.Manager;
import com.concrete.buildup.domain.site.entity.Site;
import com.concrete.buildup.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 작업일보 엔티티
 *
 * <p>현장 관리자가 일일 작업 내용을 기록하고 PDF로 생성합니다.
 * 작업 내용 입력 시 즉시 PDF가 생성되며, 별도의 상태 관리는 하지 않습니다.</p>
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Entity
@Table(name = "work_reports")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class WorkReport extends BaseEntity {

    /**
     * 현장 (FK)
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    /**
     * 작성자 (현장 관리자)
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "manager_id", nullable = false)
    private Manager manager;

    /**
     * 작업일자
     */
    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    /**
     * 공정명 (예: 철근공사, 거푸집공사)
     */
    @Column(name = "work_section", nullable = false, length = 100)
    private String workSection;

    /**
     * 투입 인력 수 (명)
     */
    @Column(name = "work_section_employee_num", nullable = false)
    private Integer workSectionEmployeeNum;

    /**
     * 공정별 작업 내용
     *
     * <p>해당 공정에 따른 구체적인 작업 내용을 기록합니다.</p>
     */
    @Column(name = "work_report_context", nullable = false, columnDefinition = "TEXT")
    private String workReportContext;

    /**
     * 생성된 PDF 파일의 S3 URL
     *
     * <p>작업일보 내용 저장 시 즉시 PDF가 생성되며, Public 접근 가능한 S3 URL이 저장됩니다.</p>
     */
    @Column(name = "pdf_url", length = 500)
    private String pdfUrl;

    /**
     * PDF 생성 일시
     */
    @Column(name = "pdf_generated_at")
    private LocalDateTime pdfGeneratedAt;

    /**
     * 투입 자재 목록 (1:N)
     */
    @OneToMany(mappedBy = "workReport", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<WorkReportMaterial> materials = new ArrayList<>();

    // === 비즈니스 로직 ===

    /**
     * 자재 추가 (양방향 연관관계 설정)
     *
     * @param material 추가할 자재
     */
    public void addMaterial(WorkReportMaterial material) {
        this.materials.add(material);
        material.setWorkReport(this);
    }

    /**
     * 자재 제거
     *
     * @param material 제거할 자재
     */
    public void removeMaterial(WorkReportMaterial material) {
        this.materials.remove(material);
        material.setWorkReport(null);
    }

    /**
     * PDF 생성 정보 업데이트
     *
     * @param pdfUrl PDF S3 URL
     * @param pdfGeneratedAt PDF 생성 일시
     */
    public void updatePdf(String pdfUrl, LocalDateTime pdfGeneratedAt) {
        this.pdfUrl = pdfUrl;
        this.pdfGeneratedAt = pdfGeneratedAt;
    }
}
