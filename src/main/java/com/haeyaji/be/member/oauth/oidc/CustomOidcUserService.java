package com.haeyaji.be.member.oauth.oidc;

import com.haeyaji.be.member.domain.SocialType;
import com.haeyaji.be.member.domain.Member;
import com.haeyaji.be.member.oauth.OAuthAttributes;
import com.haeyaji.be.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomOidcUserService extends OidcUserService {

    private final MemberRepository memberRepository;

    @Override
    @Transactional
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest); // id_token 검증 + (설정된 경우) userinfo 병합까지 끝난 상태

        /**
         * OAuth2ClientPropertiesRegistrationAdapter가
         * application.yml에 있는 security:oauth2:client:registration 안의
         * kakao/naver가 key, 나머지 내용이 value인 map으로 바인딩됨
         * 이 map을 순회하면서 각 쌍마다 ClientRegistration객체를 생성
         */

        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        SocialType socialType;
        try {
            socialType = SocialType.valueOf(registrationId.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new OAuth2AuthenticationException("지원하지 않는 소셜 로그인, [registrationId]: " + registrationId);
        }

        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();

        OAuthAttributes attrs = OAuthAttributes.of(socialType, userNameAttributeName, oidcUser.getAttributes());

        Optional<Member> optionalMember = memberRepository.
                findBySocialTypeAndSocialTypeId(attrs.socialType(), attrs.socialTypeId());

        boolean isNewMember = optionalMember.isEmpty();

        Member member = optionalMember
                .map(existingMember -> existingMember.update(attrs.email()))
                // 정보가 바뀐채로 로그인될경우 update, 컬럼이 많아지면 dto 고려. 실제로 값이 바뀌지 않았을 경우에는 update쿼리 x (dirty check)
                .orElseGet(() -> memberRepository.save(attrs.toEntity()));

        return new CustomOidcUser(member, isNewMember, List.of(new SimpleGrantedAuthority(member.getRole().name())), oidcUser.getIdToken(), oidcUser.getUserInfo(), attrs.nameAttributeKey());
    }
}
