package com.haeyaji.be.meeting.domain;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * 초대 URL 토큰 생성 (MEET-2). 256비트 난수 → URL-safe Base64 43자 (share_token varchar(64) 이내).
 */
public final class ShareTokenGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32;

    private ShareTokenGenerator() {
    }

    public static String generate() {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
