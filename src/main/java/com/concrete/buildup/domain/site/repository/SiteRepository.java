package com.concrete.buildup.domain.site.repository;

import com.concrete.buildup.domain.site.entity.Site;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 현장 Repository
 */
@Repository
public interface SiteRepository extends JpaRepository<Site, Long> {

    /**
     * 현장 관리자용 시크릿키로 현장 조회
     *
     * @param managerSecretKey 현장 관리자용 시크릿키
     * @return 현장 정보
     */
    Optional<Site> findByManagerSecretKey(String managerSecretKey);

    /**
     * 근로자용 시크릿키로 현장 조회
     *
     * @param employeeSecretKey 근로자용 시크릿키
     * @return 현장 정보
     */
    Optional<Site> findByEmployeeSecretKey(String employeeSecretKey);

    /**
     * 현장 관리자 User ID로 현장 조회
     *
     * @param userId 현장 관리자 User ID
     * @return 현장 정보
     */
    @Query("SELECT s FROM Site s JOIN s.manager m WHERE m.user.id = :userId")
    Optional<Site> findByManagerUserId(@Param("userId") Long userId);

    /**
     * 현장 관리자용 시크릿키 중복 확인
     *
     * @param managerSecretKey 현장 관리자용 시크릿키
     * @return 존재 여부
     */
    boolean existsByManagerSecretKey(String managerSecretKey);

    /**
     * 근로자용 시크릿키 중복 확인
     *
     * @param employeeSecretKey 근로자용 시크릿키
     * @return 존재 여부
     */
    boolean existsByEmployeeSecretKey(String employeeSecretKey);
}
