package com.concrete.buildup.global.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 비밀번호 해시 생성 테스트
 * SQL 삽입용 BCrypt 해시 생성
 */
@DisplayName("비밀번호 해시 생성 테스트")
class PasswordHashGeneratorTest {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    @DisplayName("테스트 계정 비밀번호 해시 생성")
    void generatePasswordHashes() {
        String corpPassword = "corp1234";
        String managerPassword = "manager1234";
        String employeePassword = "employee1234";

        String corpHash = passwordEncoder.encode(corpPassword);
        String managerHash = passwordEncoder.encode(managerPassword);
        String employeeHash = passwordEncoder.encode(employeePassword);

        System.out.println("\n=== 비밀번호 해시 ===");
        System.out.println("corp1234 해시: " + corpHash);
        System.out.println("manager1234 해시: " + managerHash);
        System.out.println("employee1234 해시: " + employeeHash);

        // 검증
        System.out.println("\n=== 해시 검증 ===");
        System.out.println("corp1234 매칭: " + passwordEncoder.matches(corpPassword, corpHash));
        System.out.println("manager1234 매칭: " + passwordEncoder.matches(managerPassword, managerHash));
        System.out.println("employee1234 매칭: " + passwordEncoder.matches(employeePassword, employeeHash));

        // 기존 해시 검증
        String existingManagerHash = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";
        System.out.println("\n=== 기존 해시 검증 ===");
        System.out.println("기존 manager 해시로 manager1234 매칭: " + passwordEncoder.matches(managerPassword, existingManagerHash));
    }
}
