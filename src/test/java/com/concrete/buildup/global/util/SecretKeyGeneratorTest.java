package com.concrete.buildup.global.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SecretKeyGenerator 단위 테스트
 *
 * @author Build-Up Team
 * @since 1.0
 */
@DisplayName("SecretKeyGenerator 테스트")
class SecretKeyGeneratorTest {

    @Test
    @DisplayName("관리자 시크릿키 생성 - 정상 포맷 검증")
    void generateManagerSecretKey_ShouldReturnValidFormat() {
        // given
        String corpName = "CONCRETE";
        Long siteId = 1L;

        // when
        String secretKey = SecretKeyGenerator.generateManagerSecretKey(corpName, siteId);

        // then
        assertThat(secretKey).isNotNull();
        assertThat(secretKey).matches("^[A-Z]{4}-\\d+-[A-Z]{3}-\\d{4}$");
        assertThat(secretKey).startsWith("CONC-1-");
    }

    @Test
    @DisplayName("근로자 시크릿키 생성 - 정상 포맷 검증")
    void generateEmployeeSecretKey_ShouldReturnValidFormat() {
        // given
        String corpName = "CONCRETE";
        Long siteId = 1L;

        // when
        String secretKey = SecretKeyGenerator.generateEmployeeSecretKey(corpName, siteId);

        // then
        assertThat(secretKey).isNotNull();
        assertThat(secretKey).matches("^[A-Z]{4}-\\d+-[A-Z]{3}-\\d{4}$");
        assertThat(secretKey).startsWith("CONC-1-");
    }

    @Test
    @DisplayName("기업명 prefix 검증 - 생성된 시크릿키에 기업명 포함 확인")
    void secretKey_ShouldContainCorpNamePrefix() {
        // given
        String corpName = "CONCRETE";
        Long siteId = 1L;

        // when
        String secretKey = SecretKeyGenerator.generateManagerSecretKey(corpName, siteId);

        // then
        assertThat(secretKey).startsWith("CONC-1-");
    }

    @Test
    @DisplayName("시크릿키 유니크성 검증 - 여러번 생성 시 모두 다른 값")
    void generateSecretKey_ShouldBeUnique() {
        // given
        String corpName = "CONCRETE";
        Long siteId = 1L;
        Set<String> generatedKeys = new HashSet<>();
        int generateCount = 100;

        // when
        for (int i = 0; i < generateCount; i++) {
            String managerKey = SecretKeyGenerator.generateManagerSecretKey(corpName, siteId);
            String employeeKey = SecretKeyGenerator.generateEmployeeSecretKey(corpName, siteId);
            generatedKeys.add(managerKey);
            generatedKeys.add(employeeKey);
        }

        // then
        assertThat(generatedKeys).hasSize(generateCount * 2);
    }

    @Test
    @DisplayName("시크릿키 포맷 - prefix-siteId-random-number 형식")
    void secretKeyFormat_ShouldFollowPattern() {
        // given
        String corpName = "TEST COMPANY";
        Long siteId = 999L;
        Pattern pattern = Pattern.compile("^[A-Z]{4}-999-[A-Z]{3}-\\d{4}$");

        // when
        String managerKey = SecretKeyGenerator.generateManagerSecretKey(corpName, siteId);
        String employeeKey = SecretKeyGenerator.generateEmployeeSecretKey(corpName, siteId);

        // then
        assertThat(pattern.matcher(managerKey).matches()).isTrue();
        assertThat(pattern.matcher(employeeKey).matches()).isTrue();
    }

    @Test
    @DisplayName("관리자 시크릿키 생성 - siteId가 null이면 IllegalArgumentException 발생")
    void generateManagerSecretKey_WithNullSiteId_ShouldThrowException() {
        // given
        String corpName = "CONCRETE";
        Long siteId = null;

        // when & then
        assertThatThrownBy(() -> SecretKeyGenerator.generateManagerSecretKey(corpName, siteId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("siteId must not be null");
    }

    @Test
    @DisplayName("근로자 시크릿키 생성 - siteId가 null이면 IllegalArgumentException 발생")
    void generateEmployeeSecretKey_WithNullSiteId_ShouldThrowException() {
        // given
        String corpName = "CONCRETE";
        Long siteId = null;

        // when & then
        assertThatThrownBy(() -> SecretKeyGenerator.generateEmployeeSecretKey(corpName, siteId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("siteId must not be null");
    }
}
