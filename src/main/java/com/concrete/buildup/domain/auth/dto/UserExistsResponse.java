package com.concrete.buildup.domain.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 아이디 중복 확인 응답 DTO
 *
 * <p>사용자 ID 존재 여부를 반환합니다.</p>
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserExistsResponse {

    /**
     * 아이디 존재 여부
     * - true: 이미 사용 중인 아이디
     * - false: 사용 가능한 아이디
     */
    private boolean exists;
}
