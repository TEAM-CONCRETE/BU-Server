package com.concrete.buildup.domain.workreport.entity;

import com.concrete.buildup.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * 작업일보 자재 투입 엔티티
 *
 * <p>작업일보에 기록되는 자재의 품명, 규격, 단위 정보를 관리합니다.</p>
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Entity
@Table(name = "work_report_materials")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class WorkReportMaterial extends BaseEntity {

    /**
     * 작업일보 (FK)
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_report_id", nullable = false)
    @Setter
    private WorkReport workReport;

    /**
     * 자재 품명 (예: 레미콘, 철근)
     */
    @Column(name = "material_name", nullable = false, length = 100)
    private String materialName;

    /**
     * 자재 규격 (예: 25-210-12, D13)
     */
    @Column(name = "material_standard", length = 100)
    private String materialStandard;

    /**
     * 자재 단위 (예: m³, ton, EA)
     */
    @Column(name = "material_unit", length = 20)
    private String materialUnit;
}
