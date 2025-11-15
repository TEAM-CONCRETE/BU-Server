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
 *   <li>시스템 터미널에서 실행: {@code java -cp build/classes/java/test com.concrete.buildup.util.BcryptHashGenerator}</li>
 *   <li>프롬프트에 비밀번호 입력 (입력 내용은 화면에 표시되지 않음)</li>
 * </ul>
 *
 * <p>보안 참고:</p>
 * <ul>
 *   <li>명령줄 인자로 비밀번호를 전달하는 것은 보안상 허용되지 않습니다 (명령 히스토리에 남을 수 있음)</li>
 *   <li>IDE 터미널에서는 Console이 지원되지 않을 수 있으므로 시스템 터미널에서 실행하세요</li>
 * </ul>
 *
 * @author Build-Up Team
 */
public class BcryptHashGenerator {
    public static void main(String[] args) {
        // 보안상의 이유로 명령줄 인자는 허용하지 않음 (명령 히스토리에 남을 수 있음)
        if (args.length > 0) {
            System.err.println("경고: 보안상의 이유로 명령줄 인자로 비밀번호를 전달할 수 없습니다.");
            System.err.println("비밀번호는 콘솔에서 안전하게 입력해주세요.");
            System.err.println();
        }

        // 콘솔에서 안전하게 입력 받음
        Console console = System.console();
        if (console == null) {
            System.err.println("에러: 콘솔을 사용할 수 없습니다.");
            System.err.println("IDE 터미널에서는 Console이 지원되지 않을 수 있습니다.");
            System.err.println("시스템 터미널에서 직접 실행해주세요:");
            System.err.println("  java -cp build/classes/java/test com.concrete.buildup.util.BcryptHashGenerator");
            System.exit(1);
            return;
        }

        char[] passwordChars = console.readPassword("비밀번호를 입력하세요: ");
        if (passwordChars == null || passwordChars.length == 0) {
            System.err.println("에러: 비밀번호가 입력되지 않았습니다.");
            System.exit(1);
            return;
        }

        String password = new String(passwordChars);
        // 민감 정보 즉시 제거
        java.util.Arrays.fill(passwordChars, ' ');

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
