package com.concrete.buildup.domain.auth.entity;

import com.concrete.buildup.global.common.BaseEntity;
import com.concrete.buildup.global.converter.ResidentNumConverter;
import com.concrete.buildup.global.serializer.ResidentNumMaskingSerializer;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.persistence.*;
import lombok.*;

/**
 * 근로자 엔티티
 *
 * 근로자 상세 정보
 *
 * 테이블: employees
 */
@Entity
@Table(name = "employees")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Employee extends BaseEntity {

    /**
     * 사용자 ID (1:1 관계)
     * User 엔티티와 연결
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    /**
     * 근로자 이름
     */
    @Column(name = "emp_name", nullable = false, length = 50)
    private String empName;

    /**
     * 비상 연락망
     */
    @Column(name = "sub_phone", length = 20)
    private String subPhone;

    /**
     * 주민등록번호 (AES-256 암호화, API 응답 시 마스킹)
     */
    @Convert(converter = ResidentNumConverter.class)
    @JsonSerialize(using = ResidentNumMaskingSerializer.class)
    @Column(name = "resident_num", length = 500)
    private String residentNum;

    /**
     * 주소
     */
    @Column(name = "emp_address", length = 255)
    private String empAddress;

    /**
     * 근로자 유형 (DAILY/PERMANENT)
     */
    @Column(name = "emp_type", length = 30)
    private String empType;

    /**
     * 근로자 정보 수정
     */
    public void updateInfo(String empName, String subPhone, String empAddress) {
        if (empName != null) {
            this.empName = empName;
        }
        if (subPhone != null) {
            this.subPhone = subPhone;
        }
        if (empAddress != null) {
            this.empAddress = empAddress;
        }
    }

    /**
     * 근로자 유형 변경
     */
    public void changeEmpType(String empType) {
        this.empType = empType;
    }
}