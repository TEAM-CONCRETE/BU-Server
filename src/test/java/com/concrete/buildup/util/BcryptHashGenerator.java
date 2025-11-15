package com.concrete.buildup.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.io.Console;

/**
 * BCrypt 해시 생성 유틸리티
 *
 * <p>일회성 비밀번호 해시 생성용 도구입니다. 사용 후에는 삭제하거나 소스 관리에서 제외하는 것을 권장합니다.</p>
 *
 * <p>사용법:</p>
 * <ul>
 *   <li>명령줄 인자로 비밀번호 전달: {@code java BcryptHashGenerator myPassword}</li>
 *   <li>콘솔에서 안전하게 입력: {@code java BcryptHashGenerator} (인자 없이 실행)</li>
 * </ul>
 *
 * @author Build-Up Team
 */
public class BcryptHashGenerator {
    public static void main(String[] args) {
        String password;

        // 명령줄 인자가 있으면 사용, 없으면 콘솔에서 안전하게 입력 받음
        if (args.length > 0) {
            password = args[0];
        } else {
            Console console = System.console();
            if (console == null) {
                System.err.println("에러: 콘솔을 사용할 수 없습니다. IDE에서 실행하거나 명령줄 인자로 비밀번호를 전달하세요.");
                System.err.println("사용법: java BcryptHashGenerator [password]");
                System.exit(1);
                return;
            }

            char[] passwordChars = console.readPassword("비밀번호를 입력하세요: ");
            if (passwordChars == null || passwordChars.length == 0) {
                System.err.println("에러: 비밀번호가 입력되지 않았습니다.");
                System.exit(1);
                return;
            }
            password = new String(passwordChars);
            // 민감 정보 즉시 제거
            java.util.Arrays.fill(passwordChars, ' ');
        }

        // 비밀번호 유효성 검증
        if (password.trim().isEmpty()) {
            System.err.println("에러: 비밀번호는 공백일 수 없습니다.");
            System.exit(1);
            return;
        }

        // BCrypt 해시 생성
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hash = encoder.encode(password);

        // 평문 비밀번호를 출력하지 않고 해시만 출력
        System.out.println("BCrypt 해시:");
        System.out.println(hash);
    }
}
