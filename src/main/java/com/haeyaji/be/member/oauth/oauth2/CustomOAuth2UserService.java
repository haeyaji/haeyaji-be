package com.haeyaji.be.member.oauth.oauth2;

import com.haeyaji.be.member.domain.SocialType;
import com.haeyaji.be.member.domain.Member;
import com.haeyaji.be.member.oauth.OAuthAttributes;
import com.haeyaji.be.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final MemberRepository memberRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        SocialType socialType;
        try {
            socialType = SocialType.valueOf(registrationId.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new OAuth2AuthenticationException("지원하지 않는 소셜 로그인, [registrationId]: " + registrationId);
        }

        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();

        OAuthAttributes attrs = OAuthAttributes.of(socialType, userNameAttributeName, oAuth2User.getAttributes());

        Optional<Member> optionalMember = memberRepository.
                findBySocialTypeAndSocialTypeId(attrs.socialType(), attrs.socialTypeId());

        boolean isNewMember = optionalMember.isEmpty();

        Member member;
        if (optionalMember.isPresent()) {
            member = optionalMember.get().update(attrs.email());
        } else {
            try {
                // saveAndFlush로 즉시 INSERT — 플레인 save()는 @UuidGenerator 때문에
                // 실제 INSERT가 flush까지 미뤄져서 여기서 제약위반을 못 잡음
                member = memberRepository.saveAndFlush(attrs.toEntity());
            } catch (DataIntegrityViolationException e) {
                // 동시에 들어온 다른 요청이 이미 만들어놓은 회원을 재조회해서 이어서 진행
                member = memberRepository.findBySocialTypeAndSocialTypeId(attrs.socialType(), attrs.socialTypeId())
                        .orElseThrow(() -> e);
            }
        }

        return new CustomOAuth2User(member, isNewMember, List.of(new SimpleGrantedAuthority(member.getRole().name())), attrs.attributes(), attrs.nameAttributeKey());
    }
}
