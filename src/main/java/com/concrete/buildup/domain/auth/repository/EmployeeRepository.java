package com.concrete.buildup.domain.auth.repository;

import com.concrete.buildup.domain.auth.entity.Employee;
import com.concrete.buildup.domain.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Employee Repository
 *
 * <p>근로자 엔티티에 대한 데이터 접근 계층입니다.</p>
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    /**
     * User ID로 근로자 조회 (1:1 관계)
     *
     * @param userId User 엔티티 ID
     * @return 근로자 엔티티 (Optional)
     */
    @Query("SELECT e FROM Employee e WHERE e.user.id = :userId")
    Optional<Employee> findByUserId(@Param("userId") Long userId);

    /**
     * 근로자 유형으로 근로자 목록 조회
     *
     * @param empType 근로자 유형 (DAILY/PERMANENT)
     * @return 근로자 목록
     */
    List<Employee> findByEmpType(String empType);

    /**
     * 근로자 이름으로 근로자 목록 조회 (Like 검색)
     *
     * @param empName 근로자 이름
     * @return 근로자 목록
     */
    List<Employee> findByEmpNameContaining(String empName);

    /**
     * Employee ID로 근로자 + User 조회 (Fetch Join)
     * N+1 문제 방지를 위해 User를 함께 조회
     *
     * @param id 근로자 ID
     * @return 근로자 + User (Optional)
     */
    @Query("SELECT e FROM Employee e JOIN FETCH e.user WHERE e.id = :id")
    Optional<Employee> findByIdWithUser(@Param("id") Long id);

    /**
     * User ID로 근로자 + User + Role 조회 (Fetch Join)
     * N+1 문제 방지를 위해 User와 Role을 함께 조회
     *
     * @param userId User 엔티티 ID
     * @return 근로자 + User + Role (Optional)
     */
    @Query("SELECT e FROM Employee e " +
           "JOIN FETCH e.user u " +
           "LEFT JOIN FETCH u.role " +
           "WHERE u.id = :userId")
    Optional<Employee> findByUserIdWithUserAndRole(@Param("userId") Long userId);

    /**
     * User 엔티티로 근로자 조회
     *
     * @param user User 엔티티
     * @return 근로자 엔티티 (Optional)
     */
    Optional<Employee> findByUser(User user);

    /**
     * 전화번호로 근로자 조회 (출퇴근 검증용)
     * User의 phone으로 근로자를 조회합니다.
     *
     * @param phone 전화번호 (하이픈 포함 또는 제외)
     * @return 근로자 + User (Optional)
     */
    @Query("SELECT e FROM Employee e JOIN FETCH e.user u WHERE u.phone = :phone")
    Optional<Employee> findByPhoneWithUser(@Param("phone") String phone);
}