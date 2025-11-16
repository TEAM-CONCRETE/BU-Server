package com.concrete.buildup.domain.payroll.repository;

import com.concrete.buildup.domain.payroll.entity.PayslipItem;
import com.concrete.buildup.domain.payroll.enums.ItemType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 급여 명세 항목 Repository
 */
@Repository
public interface PayslipItemRepository extends JpaRepository<PayslipItem, Long> {

    /**
     * 급여 ID로 항목 목록 조회
     */
    List<PayslipItem> findByPayrollId(Long payrollId);

    /**
     * 급여 ID와 항목 타입으로 조회
     */
    List<PayslipItem> findByPayrollIdAndItemType(Long payrollId, ItemType itemType);

    /**
     * 급여 ID로 항목 삭제
     */
    @Modifying
    @Transactional
    void deleteByPayrollId(Long payrollId);
}
