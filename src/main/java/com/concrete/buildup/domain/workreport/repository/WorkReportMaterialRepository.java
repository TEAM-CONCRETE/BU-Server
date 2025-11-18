package com.concrete.buildup.domain.workreport.repository;

import com.concrete.buildup.domain.workreport.entity.WorkReportMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 작업일보 자재 Repository
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Repository
public interface WorkReportMaterialRepository extends JpaRepository<WorkReportMaterial, Long> {

    /**
     * 작업일보의 자재 목록 조회
     *
     * @param workReportId 작업일보 ID
     * @return 자재 목록
     */
    List<WorkReportMaterial> findByWorkReportIdAndIsDeletedFalse(Long workReportId);
}
