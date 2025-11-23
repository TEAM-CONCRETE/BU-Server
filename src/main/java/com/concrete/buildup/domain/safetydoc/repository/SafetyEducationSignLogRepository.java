package com.concrete.buildup.domain.safetydoc.repository;

import com.concrete.buildup.domain.contract.enums.SignerRole;
import com.concrete.buildup.domain.safetydoc.entity.SafetyEducationSignLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SafetyEducationSignLogRepository extends JpaRepository<SafetyEducationSignLog, Long> {

    List<SafetyEducationSignLog> findBySafetyEducationLogIdAndIsDeletedFalse(Long logId);

    Optional<SafetyEducationSignLog> findBySafetyEducationLogIdAndSignerRoleAndIsDeletedFalse(
            Long logId, SignerRole signerRole
    );

    Optional<SafetyEducationSignLog> findBySafetyEducationLogIdAndSignerIdAndSignerRoleAndIsDeletedFalse(
            Long logId, Long signerId, SignerRole signerRole
    );

    boolean existsBySafetyEducationLogIdAndSignerRoleAndIsDeletedFalse(Long logId, SignerRole signerRole);
}
