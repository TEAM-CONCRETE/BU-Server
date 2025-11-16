package com.concrete.buildup.domain.payroll.repository;

import com.concrete.buildup.domain.contract.enums.PayPeriod;
import com.concrete.buildup.domain.payroll.entity.Payroll;
import com.concrete.buildup.domain.payroll.enums.PayStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 급여 정보 Repository
 */
@Repository
public interface PayrollRepository extends JpaRepository<Payroll, Long> {

    /**
     * 근로자 ID와 급여 기간으로 급여 조회
     *
     * 센티널 값 적용:
     * - salaryWeek: 주급이 아닌 경우 0
     * - salaryDay: 일급이 아닌 경우 해당 월의 1일
     */
    @Query("SELECT p FROM Payroll p WHERE p.employeeId = :employeeId " +
            "AND p.salaryYear = :year AND p.salaryMonth = :month " +
            "AND p.payCycle = :payCycle " +
            "AND p.salaryWeek = :week " +
            "AND p.salaryDay = :day")
    Optional<Payroll> findByEmployeeAndPeriod(
            @Param("employeeId") Long employeeId,
            @Param("year") Integer year,
            @Param("month") Integer month,
            @Param("payCycle") PayPeriod payCycle,
            @Param("week") Integer week,
            @Param("day") LocalDate day
    );

    /**
     * 근로자 ID로 급여 목록 조회 (최신순)
     */
    List<Payroll> findByEmployeeIdOrderByCreatedAtDesc(Long employeeId);

    /**
     * 기업 ID로 급여 목록 조회
     */
    List<Payroll> findByCorporationId(Long corporationId);

    /**
     * 지급 상태로 급여 목록 조회
     */
    List<Payroll> findByPayStatus(PayStatus payStatus);

    /**
     * 연도와 월로 급여 목록 조회
     */
    List<Payroll> findBySalaryYearAndSalaryMonth(Integer year, Integer month);

    /**
     * S3 키로 급여 조회
     */
    Optional<Payroll> findByS3Key(String s3Key);

    /**
     * 근로자와 연월로 급여 목록 조회
     */
    List<Payroll> findByEmployeeIdAndSalaryYearAndSalaryMonth(
            Long employeeId, Integer year, Integer month
    );
}