package com.concrete.buildup.domain.contract.repository;

import com.concrete.buildup.domain.contract.entity.ContractSignLog;
import com.concrete.buildup.domain.contract.enums.SignerRole;
import com.concrete.buildup.domain.contract.enums.VerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * ContractSignLog Repository
 *
 * <p>계약 서명 로그 엔티티에 대한 데이터 접근 계층입니다.</p>
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Repository
public interface ContractSignLogRepository extends JpaRepository<ContractSignLog, Long> {

    /**
     * 계약 ID로 서명 로그 목록 조회
     *
     * @param contractId 계약 ID
     * @return 서명 로그 목록
     */
    List<ContractSignLog> findByContractId(Long contractId);

    /**
     * 계약 ID와 서명자 역할로 서명 로그 조회
     *
     * @param contractId 계약 ID
     * @param signerRole 서명자 역할
     * @return 서명 로그 (Optional)
     */
    Optional<ContractSignLog> findByContractIdAndSignerRole(Long contractId, SignerRole signerRole);

    /**
     * 계약 ID와 서명자 역할로 서명 로그 존재 여부 확인
     *
     * @param contractId 계약 ID
     * @param signerRole 서명자 역할
     * @return 존재 여부
     */
    boolean existsByContractIdAndSignerRole(Long contractId, SignerRole signerRole);

    /**
     * 서명자 ID로 서명 로그 목록 조회
     *
     * @param signerId 서명자 ID
     * @return 서명 로그 목록
     */
    List<ContractSignLog> findBySignerId(Long signerId);

    /**
     * 검증 상태로 서명 로그 목록 조회
     *
     * @param verificationStatus 검증 상태
     * @return 서명 로그 목록
     */
    List<ContractSignLog> findByVerificationStatus(VerificationStatus verificationStatus);

    /**
     * 계약 ID와 검증 상태로 서명 로그 목록 조회
     *
     * @param contractId 계약 ID
     * @param verificationStatus 검증 상태
     * @return 서명 로그 목록
     */
    List<ContractSignLog> findByContractIdAndVerificationStatus(Long contractId, VerificationStatus verificationStatus);

    // ========== 정렬 쿼리 ==========

    /**
     * 계약 ID로 서명 로그 목록 조회 (서명 시간 내림차순 정렬)
     * 최신 서명부터 조회
     *
     * @param contractId 계약 ID
     * @return 서명 로그 목록 (서명 시간 내림차순)
     */
    List<ContractSignLog> findByContractIdOrderBySignedAtDesc(Long contractId);

    /**
     * 계약 ID로 서명 로그 목록 조회 (서명 시간 오름차순 정렬)
     * 오래된 서명부터 조회 (서명 순서)
     *
     * @param contractId 계약 ID
     * @return 서명 로그 목록 (서명 시간 오름차순)
     */
    List<ContractSignLog> findByContractIdOrderBySignedAtAsc(Long contractId);

    // ========== Fetch Join 쿼리 (N+1 문제 방지) ==========

    /**
     * 계약 ID로 서명 로그 + 계약 정보 조회 (Fetch Join)
     * N+1 문제 방지를 위해 Contract를 함께 조회
     *
     * @param contractId 계약 ID
     * @return 서명 로그 + 계약 정보 목록
     */
    @Query("SELECT csl FROM ContractSignLog csl JOIN FETCH csl.contract WHERE csl.contract.id = :contractId ORDER BY csl.signedAt DESC")
    List<ContractSignLog> findByContractIdWithContract(@Param("contractId") Long contractId);

    /**
     * 서명자 ID로 서명 로그 + 계약 정보 조회 (Fetch Join)
     * N+1 문제 방지를 위해 Contract를 함께 조회
     *
     * @param signerId 서명자 ID
     * @return 서명 로그 + 계약 정보 목록
     */
    @Query("SELECT csl FROM ContractSignLog csl JOIN FETCH csl.contract WHERE csl.signerId = :signerId ORDER BY csl.signedAt DESC")
    List<ContractSignLog> findBySignerIdWithContract(@Param("signerId") Long signerId);
}
