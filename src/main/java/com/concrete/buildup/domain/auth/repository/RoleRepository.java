package com.concrete.buildup.domain.auth.repository;

import com.concrete.buildup.domain.auth.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Role Repository
 *
 * <p>역할 엔티티에 대한 데이터 접근 계층입니다.</p>
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    /**
     * 역할명으로 역할 조회
     *
     * @param roleName 역할명 (EMPLOYEE, MANAGER, CORPORATION, ADMIN)
     * @return 역할 엔티티 (Optional)
     */
    Optional<Role> findByRoleName(String roleName);

    /**
     * 역할명 존재 여부 확인
     *
     * @param roleName 역할명
     * @return 존재 여부
     */
    boolean existsByRoleName(String roleName);
}