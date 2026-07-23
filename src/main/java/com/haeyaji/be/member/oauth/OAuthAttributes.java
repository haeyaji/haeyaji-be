package com.haeyaji.be.member.oauth;

import com.haeyaji.be.member.domain.SocialType;
import com.haeyaji.be.member.domain.Member;
import com.haeyaji.be.member.domain.MemberRole;
import lombok.Builder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;

import java.util.Map;

@Builder
public record OAuthAttributes(
        Map<String, Object> attributes,   // 원본 응답 전체
        String nameAttributeKey,          // 식별자 키 (예: "id", "sub")
        SocialType socialType,            // KAKAO | NAVER | GOOGLE
        String socialTypeId,              // 소셜 타입의 고유 ID
        String email
) {

    public static OAuthAttributes of(SocialType socialType, String userNameAttributeName, Map<String, Object> attributes) {
        return switch (socialType) {
            case GOOGLE -> ofGoogle(socialType, userNameAttributeName, attributes);
            case KAKAO -> ofKakao(socialType, userNameAttributeName, attributes);
            case NAVER -> ofNaver(socialType, attributes);
        };
    }

    private static OAuthAttributes ofGoogle(SocialType socialType, String userNameAttributeName, Map<String, Object> attributes) {
        return OAuthAttributes.builder()
                .attributes(attributes)
                .nameAttributeKey(userNameAttributeName)
                .socialType(socialType)
                .socialTypeId(String.valueOf(attributes.get(userNameAttributeName)))
                .email((String) attributes.get("email"))
                .build();
    }

    private static OAuthAttributes ofKakao(SocialType socialType, String userNameAttributeName, Map<String, Object> attributes) {
        return OAuthAttributes.builder()
                .attributes(attributes)
                .nameAttributeKey(userNameAttributeName)
                .socialType(socialType)
                .socialTypeId(String.valueOf(attributes.get(userNameAttributeName)))
                .email((String) attributes.get("email"))
                .build();
    }

    @SuppressWarnings("unchecked")
    private static OAuthAttributes ofNaver(SocialType socialType, Map<String, Object> attributes) {
        // 네이버는 유저의 정보가 response라는 map에 담은 채로 응답한다.
        Map<String, Object> response = (Map<String, Object>) attributes.get("response");
        if (response == null) {
            throw new OAuth2AuthenticationException("네이버 응답에서 response 속성을 찾을 수 없습니다.");
        }

        String userNameAttributeName = "id";

        return OAuthAttributes.builder()
                .attributes(response)
                .nameAttributeKey(userNameAttributeName)
                .socialType(socialType)
                .socialTypeId(String.valueOf(response.get(userNameAttributeName)))
                .email((String) response.get("email"))
                .build();
    }

    public Member toEntity() {
        return Member.builder()
                .socialType(socialType)
                .socialTypeId(socialTypeId)
                .email(email)
                .role(MemberRole.ROLE_USER)
                .build();
    }
}
