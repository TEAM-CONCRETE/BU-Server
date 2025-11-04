package com.concrete.buildup.domain.contract.repository;

import com.concrete.buildup.domain.contract.entity.ContractDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * ContractDetail Repository
 *
 * <p>계약 상세 정보 엔티티에 대한 데이터 접근 계층입니다.</p>
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Repository
public interface ContractDetailRepository extends JpaRepository<ContractDetail, Long> {

    /**
     * 계약 ID로 계약 상세 정보 조회
     *
     * @param contractId 계약 ID
     * @return 계약 상세 정보 (Optional)
     */
    Optional<ContractDetail> findByContractId(Long contractId);

    /**
     * 계약 ID로 계약 상세 정보 존재 여부 확인
     *
     * @param contractId 계약 ID
     * @return 존재 여부
     */
    boolean existsByContractId(Long contractId);

    /**
     * 계약 ID로 계약 상세 정보 삭제
     *
     * @param contractId 계약 ID
     */
    void deleteByContractId(Long contractId);
}
